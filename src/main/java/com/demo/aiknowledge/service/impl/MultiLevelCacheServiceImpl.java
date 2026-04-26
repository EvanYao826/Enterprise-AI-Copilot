package com.demo.aiknowledge.service.impl;

import com.demo.aiknowledge.config.CacheConfig;
import com.demo.aiknowledge.service.CacheService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Expiry;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.index.qual.NonNegative;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 多级缓存服务实现
 * 第一级：Caffeine本地缓存（毫秒级响应）
 * 第二级：Redis分布式缓存（秒级响应）
 *
 * 缓存策略：先查本地，再查Redis，最后回源
 */
@Service
@Primary
@Slf4j
public class MultiLevelCacheServiceImpl implements CacheService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    // 本地缓存管理器（按缓存名称隔离）
    private final Map<String, Cache<String, Object>> localCaches = new ConcurrentHashMap<>();

    // 缓存统计信息
    private final Map<String, CacheStats> cacheStatsMap = new ConcurrentHashMap<>();

    // 缓存击穿防护锁
    private final Map<String, Object> lockMap = new ConcurrentHashMap<>();

    // 热点数据标识（永不过期）
    private final Set<String> hotDataKeys = ConcurrentHashMap.newKeySet();

    public MultiLevelCacheServiceImpl(
            RedisTemplate<String, Object> redisTemplate,
            StringRedisTemplate stringRedisTemplate,
            ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.stringRedisTemplate = stringRedisTemplate;
        this.objectMapper = objectMapper;
        initializeLocalCaches();
    }

    /**
     * 初始化本地缓存
     */
    private void initializeLocalCaches() {
        // 初始化各个缓存类型的本地缓存
        for (String cacheName : getCacheNames()) {
            Cache<String, Object> localCache = createLocalCache(cacheName);
            localCaches.put(cacheName, localCache);
            log.info("Initialized local cache for: {}", cacheName);
        }
    }

    /**
     * 创建本地缓存实例
     */
    private Cache<String, Object> createLocalCache(String cacheName) {
        Caffeine<Object, Object> builder = Caffeine.newBuilder();

        // 根据缓存类型配置不同参数
        switch (cacheName) {
            case CacheConfig.CacheConstants.CACHE_AI_ANSWER:
                // 问答缓存：高频访问，中等大小
                builder.maximumSize(1000)
                       .expireAfterWrite(10, TimeUnit.MINUTES)  // 本地缓存10分钟
                       .recordStats();
                break;
            case CacheConfig.CacheConstants.CACHE_USER_SESSION:
                // 用户会话：中等频率，较小大小
                builder.maximumSize(500)
                       .expireAfterWrite(5, TimeUnit.MINUTES)
                       .recordStats();
                break;
            case CacheConfig.CacheConstants.CACHE_DOC_METADATA:
                // 文档元数据：低频访问，较小大小
                builder.maximumSize(200)
                       .expireAfterWrite(2, TimeUnit.MINUTES)
                       .recordStats();
                break;
            case CacheConfig.CacheConstants.CACHE_CONVERSATION_CONTEXT:
                // 对话上下文：高频访问，中等大小
                builder.maximumSize(300)
                       .expireAfter(new Expiry<String, Object>() {
                           @Override
                           public long expireAfterCreate(@NonNull String key, @NonNull Object value, long currentTime) {
                               // 根据上下文长度动态设置过期时间
                               return TimeUnit.MINUTES.toNanos(15);
                           }
                           @Override
                           public long expireAfterUpdate(@NonNull String key, @NonNull Object value, long currentTime, @NonNegative long currentDuration) {
                               // 更新后延长过期时间
                               return currentDuration + TimeUnit.MINUTES.toNanos(5);
                           }
                           @Override
                           public long expireAfterRead(@NonNull String key, @NonNull Object value, long currentTime, @NonNegative long currentDuration) {
                               // 读取后延长过期时间（热点数据）
                               return currentDuration + TimeUnit.MINUTES.toNanos(2);
                           }
                       })
                       .recordStats();
                break;
            default:
                builder.maximumSize(100)
                       .expireAfterWrite(5, TimeUnit.MINUTES)
                       .recordStats();
        }

        return builder.build();
    }

    /**
     * 获取本地缓存实例
     */
    private Cache<String, Object> getLocalCache(String cacheName) {
        return localCaches.computeIfAbsent(cacheName, this::createLocalCache);
    }

    @Override
    public void set(String cacheName, String key, Object value, long ttl, TimeUnit timeUnit) {
        String fullKey = getFullKey(cacheName, key);

        try {
            // 1. 设置本地缓存（使用较短的TTL）
            long localTtl = Math.min(ttl, TimeUnit.MINUTES.toSeconds(10)); // 本地缓存最多10分钟
            getLocalCache(cacheName).put(key, new CacheEntry(value, System.currentTimeMillis(), localTtl));

            // 2. 设置Redis缓存
            redisTemplate.opsForValue().set(fullKey, value, ttl, timeUnit);

            // 3. 如果是热点数据，标记为永不过期（本地）
            if (isHotData(cacheName, key)) {
                hotDataKeys.add(getHotDataKey(cacheName, key));
            }

            recordCachePut(cacheName);
            log.debug("Multi-level cache set - name: {}, key: {}, ttl: {} {}", cacheName, key, ttl, timeUnit);

        } catch (Exception e) {
            log.error("Failed to set multi-level cache - name: {}, key: {}", cacheName, key, e);
            // 降级：只设置本地缓存
            getLocalCache(cacheName).put(key, value);
        }
    }

    @Override
    public void set(String cacheName, String key, Object value) {
        set(cacheName, key, value, getDefaultTtl(cacheName), TimeUnit.SECONDS);
    }

    @Override
    public <T> T get(String cacheName, String key, Class<T> clazz) {
        // 检查是否为热点数据（永不过期）
        if (isHotData(cacheName, key)) {
            log.debug("Hot data access - name: {}, key: {}", cacheName, key);
        }

        // 1. 先查本地缓存
        Cache<String, Object> localCache = getLocalCache(cacheName);
        Object localValue = localCache.getIfPresent(key);

        if (localValue != null) {
            if (localValue instanceof CacheEntry) {
                CacheEntry entry = (CacheEntry) localValue;
                // 检查本地缓存是否过期
                if (!entry.isExpired()) {
                    recordCacheHit(cacheName);
                    log.debug("Local cache hit - name: {}, key: {}", cacheName, key);
                    return clazz.cast(entry.getValue());
                } else {
                    // 本地缓存过期，清除
                    localCache.invalidate(key);
                }
            } else {
                recordCacheHit(cacheName);
                log.debug("Local cache hit (simple) - name: {}, key: {}", cacheName, key);
                return clazz.cast(localValue);
            }
        }

        // 2. 查Redis缓存（防止缓存击穿）
        String fullKey = getFullKey(cacheName, key);
        Object lock = lockMap.computeIfAbsent(fullKey, k -> new Object());

        synchronized (lock) {
            try {
                // 双重检查
                localValue = localCache.getIfPresent(key);
                if (localValue != null) {
                    if (localValue instanceof CacheEntry) {
                        CacheEntry entry = (CacheEntry) localValue;
                        if (!entry.isExpired()) {
                            return clazz.cast(entry.getValue());
                        }
                    } else {
                        return clazz.cast(localValue);
                    }
                }

                // 查询Redis
                Object redisValue = redisTemplate.opsForValue().get(fullKey);

                if (redisValue != null) {
                    // Redis命中，回填本地缓存
                    long localTtl = Math.min(getDefaultTtl(cacheName), TimeUnit.MINUTES.toSeconds(10));
                    
                    // 处理Redis反序列化的类型转换问题
                    Object finalValue = redisValue;
                    if (redisValue instanceof LinkedHashMap) {
                        // 将LinkedHashMap转换为目标类型
                        try {
                            finalValue = objectMapper.convertValue(redisValue, clazz);
                            log.debug("Converted LinkedHashMap to {} - name: {}, key: {}", clazz.getSimpleName(), cacheName, key);
                        } catch (Exception e) {
                            log.error("Failed to convert LinkedHashMap to {} - name: {}, key: {}", clazz.getSimpleName(), cacheName, key, e);
                            // 转换失败，使用原始值（可能会在后续cast时失败，但至少有日志）
                        }
                    }
                    
                    localCache.put(key, new CacheEntry(finalValue, System.currentTimeMillis(), localTtl));

                    recordCacheHit(cacheName);
                    log.debug("Redis cache hit - name: {}, key: {}", cacheName, key);
                    return clazz.cast(finalValue);
                }

                // 缓存未命中
                recordCacheMiss(cacheName);
                log.debug("Cache miss - name: {}, key: {}", cacheName, key);
                return null;

            } catch (Exception e) {
                log.error("Failed to get multi-level cache - name: {}, key: {}", cacheName, key, e);
                recordCacheMiss(cacheName);
                return null;
            } finally {
                lockMap.remove(fullKey);
            }
        }
    }

    @Override
    public Boolean setIfAbsent(String cacheName, String key, Object value, long ttl, TimeUnit timeUnit) {
        String fullKey = getFullKey(cacheName, key);

        try {
            // 使用Redis的SETNX
            Boolean result = redisTemplate.opsForValue().setIfAbsent(fullKey, value, ttl, timeUnit);

            if (result != null && result) {
                // Redis设置成功，同步到本地缓存
                long localTtl = Math.min(ttl, TimeUnit.MINUTES.toSeconds(10));
                getLocalCache(cacheName).put(key, new CacheEntry(value, System.currentTimeMillis(), localTtl));
                recordCachePut(cacheName);
            }

            return result;

        } catch (Exception e) {
            log.error("Failed to set if absent - name: {}, key: {}", cacheName, key, e);
            return false;
        }
    }

    @Override
    public <T> T getAndSet(String cacheName, String key, Object value, Class<T> clazz) {
        String fullKey = getFullKey(cacheName, key);
        Object lock = lockMap.computeIfAbsent(fullKey, k -> new Object());

        synchronized (lock) {
            try {
                // 先获取旧值
                T oldValue = get(cacheName, key, clazz);

                // 设置新值
                set(cacheName, key, value);

                return oldValue;

            } finally {
                lockMap.remove(fullKey);
            }
        }
    }

    @Override
    public void delete(String cacheName, String key) {
        String fullKey = getFullKey(cacheName, key);

        try {
            // 1. 删除本地缓存
            getLocalCache(cacheName).invalidate(key);

            // 2. 删除Redis缓存
            redisTemplate.delete(fullKey);

            // 3. 移除热点数据标记
            hotDataKeys.remove(getHotDataKey(cacheName, key));

            log.debug("Multi-level cache deleted - name: {}, key: {}", cacheName, key);

        } catch (Exception e) {
            log.error("Failed to delete multi-level cache - name: {}, key: {}", cacheName, key, e);
        }
    }

    @Override
    public void delete(String cacheName, List<String> keys) {
        keys.forEach(key -> delete(cacheName, key));
    }

    @Override
    public boolean exists(String cacheName, String key) {
        // 先检查本地缓存
        Cache<String, Object> localCache = getLocalCache(cacheName);
        Object localValue = localCache.getIfPresent(key);

        if (localValue != null) {
            if (localValue instanceof CacheEntry) {
                return !((CacheEntry) localValue).isExpired();
            }
            return true;
        }

        // 检查Redis
        String fullKey = getFullKey(cacheName, key);
        try {
            Boolean exists = redisTemplate.hasKey(fullKey);
            return exists != null && exists;
        } catch (Exception e) {
            log.error("Failed to check cache existence - name: {}, key: {}", cacheName, key, e);
            return false;
        }
    }

    @Override
    public Long getExpire(String cacheName, String key) {
        String fullKey = getFullKey(cacheName, key);
        try {
            return redisTemplate.getExpire(fullKey, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.error("Failed to get cache expire - name: {}, key: {}", cacheName, key, e);
            return null;
        }
    }

    @Override
    public void expire(String cacheName, String key, long ttl, TimeUnit timeUnit) {
        String fullKey = getFullKey(cacheName, key);
        try {
            redisTemplate.expire(fullKey, ttl, timeUnit);
            log.debug("Cache expired - name: {}, key: {}, ttl: {} {}", cacheName, key, ttl, timeUnit);
        } catch (Exception e) {
            log.error("Failed to set cache expire - name: {}, key: {}", cacheName, key, e);
        }
    }

    @Override
    public Set<String> keys(String cacheName, String pattern) {
        String fullPattern = getFullKey(cacheName, pattern);
        try {
            return redisTemplate.keys(fullPattern);
        } catch (Exception e) {
            log.error("Failed to get cache keys - name: {}, pattern: {}", cacheName, pattern, e);
            return Collections.emptySet();
        }
    }

    @Override
    public void clear(String cacheName) {
        try {
            // 1. 清空本地缓存
            Cache<String, Object> localCache = getLocalCache(cacheName);
            localCache.invalidateAll();

            // 2. 清空Redis缓存
            Set<String> keys = keys(cacheName, "*");
            if (!keys.isEmpty()) {
                redisTemplate.delete(keys);
            }

            // 3. 清理热点数据标记
            hotDataKeys.removeIf(key -> key.startsWith(cacheName + ":"));

            log.info("Multi-level cache cleared - name: {}, cleared entries: {}", cacheName, keys.size());

        } catch (Exception e) {
            log.error("Failed to clear cache - name: {}", cacheName, e);
        }
    }

    @Override
    public Map<String, Object> getStats(String cacheName) {
        CacheStats stats = cacheStatsMap.computeIfAbsent(cacheName, k -> new CacheStats());
        Map<String, Object> result = stats.toMap();

        // 添加本地缓存统计
        Cache<String, Object> localCache = getLocalCache(cacheName);
        com.github.benmanes.caffeine.cache.stats.CacheStats localStats = localCache.stats();

        result.put("localHitRate", localStats.hitRate());
        result.put("localRequestCount", localStats.requestCount());
        result.put("localHitCount", localStats.hitCount());
        result.put("localMissCount", localStats.missCount());
        result.put("localEvictionCount", localStats.evictionCount());
        result.put("localSize", localCache.estimatedSize());

        return result;
    }

    @Override
    public List<String> getCacheNames() {
        return Arrays.asList(
                CacheConfig.CacheConstants.CACHE_AI_ANSWER,
                CacheConfig.CacheConstants.CACHE_USER_SESSION,
                CacheConfig.CacheConstants.CACHE_DOC_METADATA,
                CacheConfig.CacheConstants.CACHE_CONVERSATION_CONTEXT,
                CacheConfig.CacheConstants.CACHE_VECTOR_SEARCH
        );
    }

    @Override
    public double getHitRate(String cacheName) {
        CacheStats stats = cacheStatsMap.get(cacheName);
        return stats != null ? stats.getHitRate() : 0.0;
    }

    @Override
    public long getSize(String cacheName) {
        try {
            Set<String> keys = keys(cacheName, "*");
            return keys != null ? keys.size() : 0;
        } catch (Exception e) {
            log.error("Failed to get cache size - name: {}", cacheName, e);
            return 0;
        }
    }

    @Override
    public void setBatch(String cacheName, Map<String, Object> entries, long ttl, TimeUnit timeUnit) {
        entries.forEach((key, value) -> set(cacheName, key, value, ttl, timeUnit));
    }

    @Override
    public <T> Map<String, T> getBatch(String cacheName, List<String> keys, Class<T> clazz) {
        Map<String, T> result = new HashMap<>();
        keys.forEach(key -> {
            T value = get(cacheName, key, clazz);
            if (value != null) {
                result.put(key, value);
            }
        });
        return result;
    }

    @Override
    public Long increment(String cacheName, String key, long delta) {
        String fullKey = getFullKey(cacheName, key);
        try {
            return redisTemplate.opsForValue().increment(fullKey, delta);
        } catch (Exception e) {
            log.error("Failed to increment cache - name: {}, key: {}", cacheName, key, e);
            return null;
        }
    }

    @Override
    public Long decrement(String cacheName, String key, long delta) {
        String fullKey = getFullKey(cacheName, key);
        try {
            return redisTemplate.opsForValue().decrement(fullKey, delta);
        } catch (Exception e) {
            log.error("Failed to decrement cache - name: {}, key: {}", cacheName, key, e);
            return null;
        }
    }

    @Override
    public void hset(String cacheName, String key, String field, Object value) {
        String fullKey = getFullKey(cacheName, key);
        try {
            redisTemplate.opsForHash().put(fullKey, field, value);
            log.debug("Hash set - name: {}, key: {}, field: {}", cacheName, key, field);
        } catch (Exception e) {
            log.error("Failed to set hash field - name: {}, key: {}, field: {}", cacheName, key, field, e);
        }
    }

    @Override
    public <T> T hget(String cacheName, String key, String field, Class<T> clazz) {
        String fullKey = getFullKey(cacheName, key);
        try {
            Object value = redisTemplate.opsForHash().get(fullKey, field);
            return value != null ? clazz.cast(value) : null;
        } catch (Exception e) {
            log.error("Failed to get hash field - name: {}, key: {}, field: {}", cacheName, key, field, e);
            return null;
        }
    }

    @Override
    public Map<String, Object> hgetAll(String cacheName, String key) {
        String fullKey = getFullKey(cacheName, key);
        try {
            Map<Object, Object> hash = redisTemplate.opsForHash().entries(fullKey);
            Map<String, Object> result = new HashMap<>();
            hash.forEach((k, v) -> result.put(k.toString(), v));
            return result;
        } catch (Exception e) {
            log.error("Failed to get all hash fields - name: {}, key: {}", cacheName, key, e);
            return Collections.emptyMap();
        }
    }

    @Override
    public void hdelete(String cacheName, String key, List<String> fields) {
        String fullKey = getFullKey(cacheName, key);
        try {
            Object[] fieldArray = fields.toArray();
            redisTemplate.opsForHash().delete(fullKey, fieldArray);
            log.debug("Hash fields deleted - name: {}, key: {}, fields: {}", cacheName, key, fields);
        } catch (Exception e) {
            log.error("Failed to delete hash fields - name: {}, key: {}, fields: {}", cacheName, key, fields, e);
        }
    }

    @Override
    public void lpush(String cacheName, String key, List<Object> values, long ttl, TimeUnit timeUnit) {
        String fullKey = getFullKey(cacheName, key);
        try {
            redisTemplate.opsForList().rightPushAll(fullKey, values);
            redisTemplate.expire(fullKey, ttl, timeUnit);
            log.debug("List pushed - name: {}, key: {}, size: {}", cacheName, key, values.size());
        } catch (Exception e) {
            log.error("Failed to push list - name: {}, key: {}", cacheName, key, e);
        }
    }

    @Override
    public <T> List<T> lrange(String cacheName, String key, long start, long end, Class<T> clazz) {
        String fullKey = getFullKey(cacheName, key);
        try {
            List<Object> values = redisTemplate.opsForList().range(fullKey, start, end);
            if (values == null) {
                return Collections.emptyList();
            }
            return values.stream()
                    .map(clazz::cast)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Failed to get list range - name: {}, key: {}", cacheName, key, e);
            return Collections.emptyList();
        }
    }

    @Override
    public void sadd(String cacheName, String key, Set<Object> values, long ttl, TimeUnit timeUnit) {
        String fullKey = getFullKey(cacheName, key);
        try {
            redisTemplate.opsForSet().add(fullKey, values.toArray());
            redisTemplate.expire(fullKey, ttl, timeUnit);
            log.debug("Set added - name: {}, key: {}, size: {}", cacheName, key, values.size());
        } catch (Exception e) {
            log.error("Failed to add set - name: {}, key: {}", cacheName, key, e);
        }
    }

    @Override
    public <T> Set<T> smembers(String cacheName, String key, Class<T> clazz) {
        String fullKey = getFullKey(cacheName, key);
        try {
            Set<Object> values = redisTemplate.opsForSet().members(fullKey);
            if (values == null) {
                return Collections.emptySet();
            }
            return values.stream()
                    .map(clazz::cast)
                    .collect(Collectors.toSet());
        } catch (Exception e) {
            log.error("Failed to get set members - name: {}, key: {}", cacheName, key, e);
            return Collections.emptySet();
        }
    }

    // 私有辅助方法

    /**
     * 获取完整的缓存键（添加前缀）
     */
    private String getFullKey(String cacheName, String key) {
        String prefix = getCachePrefix(cacheName);
        return prefix + key;
    }

    /**
     * 获取缓存键前缀
     */
    private String getCachePrefix(String cacheName) {
        switch (cacheName) {
            case CacheConfig.CacheConstants.CACHE_AI_ANSWER:
                return CacheConfig.CacheConstants.KEY_AI_ANSWER;
            case CacheConfig.CacheConstants.CACHE_USER_SESSION:
                return CacheConfig.CacheConstants.KEY_USER_SESSION;
            case CacheConfig.CacheConstants.CACHE_DOC_METADATA:
                return CacheConfig.CacheConstants.KEY_DOC_METADATA;
            case CacheConfig.CacheConstants.CACHE_CONVERSATION_CONTEXT:
                return CacheConfig.CacheConstants.KEY_CONVERSATION_CONTEXT;
            case CacheConfig.CacheConstants.CACHE_VECTOR_SEARCH:
                return CacheConfig.CacheConstants.KEY_VECTOR_SEARCH;
            default:
                return "cache:" + cacheName + ":";
        }
    }

    /**
     * 获取默认过期时间（秒）
     */
    private long getDefaultTtl(String cacheName) {
        switch (cacheName) {
            case CacheConfig.CacheConstants.CACHE_AI_ANSWER:
                return CacheConfig.CacheConstants.TTL_AI_ANSWER;
            case CacheConfig.CacheConstants.CACHE_USER_SESSION:
                return CacheConfig.CacheConstants.TTL_USER_SESSION;
            case CacheConfig.CacheConstants.CACHE_DOC_METADATA:
                return CacheConfig.CacheConstants.TTL_DOC_METADATA;
            case CacheConfig.CacheConstants.CACHE_CONVERSATION_CONTEXT:
                return CacheConfig.CacheConstants.TTL_CONVERSATION_CONTEXT;
            case CacheConfig.CacheConstants.CACHE_VECTOR_SEARCH:
                return CacheConfig.CacheConstants.TTL_VECTOR_SEARCH;
            default:
                return 3600; // 默认1小时
        }
    }

    /**
     * 获取热点数据键
     */
    private String getHotDataKey(String cacheName, String key) {
        return cacheName + ":" + key;
    }

    /**
     * 判断是否为热点数据
     */
    private boolean isHotData(String cacheName, String key) {
        // 这里可以添加热点数据判断逻辑
        // 例如：访问频率超过阈值的数据
        return hotDataKeys.contains(getHotDataKey(cacheName, key));
    }

    /**
     * 记录缓存命中
     */
    private void recordCacheHit(String cacheName) {
        CacheStats stats = cacheStatsMap.computeIfAbsent(cacheName, k -> new CacheStats());
        stats.recordHit();
    }

    /**
     * 记录缓存未命中
     */
    private void recordCacheMiss(String cacheName) {
        CacheStats stats = cacheStatsMap.computeIfAbsent(cacheName, k -> new CacheStats());
        stats.recordMiss();
    }

    /**
     * 记录缓存设置
     */
    private void recordCachePut(String cacheName) {
        CacheStats stats = cacheStatsMap.computeIfAbsent(cacheName, k -> new CacheStats());
        stats.recordPut();
    }

    /**
     * 缓存条目（带过期时间）
     */
    private static class CacheEntry {
        private final Object value;
        private final long timestamp;
        private final long ttlSeconds;

        public CacheEntry(Object value, long timestamp, long ttlSeconds) {
            this.value = value;
            this.timestamp = timestamp;
            this.ttlSeconds = ttlSeconds;
        }

        public Object getValue() {
            return value;
        }

        public boolean isExpired() {
            return System.currentTimeMillis() - timestamp > ttlSeconds * 1000;
        }
    }

    /**
     * 缓存统计信息
     */
    private static class CacheStats {
        private long hits = 0;
        private long misses = 0;
        private long puts = 0;
        private long evictions = 0;

        public void recordHit() {
            hits++;
        }

        public void recordMiss() {
            misses++;
        }

        public void recordPut() {
            puts++;
        }

        public void recordEviction() {
            evictions++;
        }

        public double getHitRate() {
            long total = hits + misses;
            return total > 0 ? (double) hits / total : 0.0;
        }

        public Map<String, Object> toMap() {
            Map<String, Object> stats = new HashMap<>();
            stats.put("hits", hits);
            stats.put("misses", misses);
            stats.put("puts", puts);
            stats.put("evictions", evictions);
            stats.put("hitRate", getHitRate());
            stats.put("total", hits + misses);
            return stats;
        }
    }
}