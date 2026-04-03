package com.demo.aiknowledge.service.impl;

import com.demo.aiknowledge.config.CacheConfig;
import com.demo.aiknowledge.service.CacheService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 统一缓存服务实现
 * 基于Redis的缓存服务
 */
@Service
@Slf4j
public class CacheServiceImpl implements CacheService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    public CacheServiceImpl(
            RedisTemplate<String, Object> redisTemplate,
            StringRedisTemplate stringRedisTemplate,
            ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.stringRedisTemplate = stringRedisTemplate;
        this.objectMapper = objectMapper;
    }

    // 缓存命中统计
    private final Map<String, CacheStats> cacheStatsMap = new HashMap<>();

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

    @Override
    public void set(String cacheName, String key, Object value, long ttl, TimeUnit timeUnit) {
        try {
            String fullKey = getFullKey(cacheName, key);
            redisTemplate.opsForValue().set(fullKey, value, ttl, timeUnit);
            recordCachePut(cacheName);
            log.debug("Cache set - name: {}, key: {}, ttl: {} {}", cacheName, key, ttl, timeUnit);
        } catch (Exception e) {
            log.error("Failed to set cache - name: {}, key: {}", cacheName, key, e);
        }
    }

    @Override
    public void set(String cacheName, String key, Object value) {
        // 使用默认过期时间
        set(cacheName, key, value, getDefaultTtl(cacheName), TimeUnit.SECONDS);
    }

    @Override
    public <T> T get(String cacheName, String key, Class<T> clazz) {
        try {
            String fullKey = getFullKey(cacheName, key);
            Object value = redisTemplate.opsForValue().get(fullKey);

            if (value != null) {
                recordCacheHit(cacheName);
                log.debug("Cache hit - name: {}, key: {}", cacheName, key);
                return clazz.cast(value);
            }

            recordCacheMiss(cacheName);
            log.debug("Cache miss - name: {}, key: {}", cacheName, key);
            return null;
        } catch (Exception e) {
            log.error("Failed to get cache - name: {}, key: {}", cacheName, key, e);
            return null;
        }
    }

    @Override
    public void delete(String cacheName, String key) {
        try {
            String fullKey = getFullKey(cacheName, key);
            redisTemplate.delete(fullKey);
            log.debug("Cache deleted - name: {}, key: {}", cacheName, key);
        } catch (Exception e) {
            log.error("Failed to delete cache - name: {}, key: {}", cacheName, key, e);
        }
    }

    @Override
    public void delete(String cacheName, List<String> keys) {
        keys.forEach(key -> delete(cacheName, key));
    }

    @Override
    public boolean exists(String cacheName, String key) {
        try {
            String fullKey = getFullKey(cacheName, key);
            Boolean exists = redisTemplate.hasKey(fullKey);
            return exists != null && exists;
        } catch (Exception e) {
            log.error("Failed to check cache existence - name: {}, key: {}", cacheName, key, e);
            return false;
        }
    }

    @Override
    public Long getExpire(String cacheName, String key) {
        try {
            String fullKey = getFullKey(cacheName, key);
            return redisTemplate.getExpire(fullKey, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.error("Failed to get cache expire - name: {}, key: {}", cacheName, key, e);
            return null;
        }
    }

    @Override
    public void expire(String cacheName, String key, long ttl, TimeUnit timeUnit) {
        try {
            String fullKey = getFullKey(cacheName, key);
            redisTemplate.expire(fullKey, ttl, timeUnit);
            log.debug("Cache expired - name: {}, key: {}, ttl: {} {}", cacheName, key, ttl, timeUnit);
        } catch (Exception e) {
            log.error("Failed to set cache expire - name: {}, key: {}", cacheName, key, e);
        }
    }

    @Override
    public Set<String> keys(String cacheName, String pattern) {
        try {
            String fullPattern = getFullKey(cacheName, pattern);
            return redisTemplate.keys(fullPattern);
        } catch (Exception e) {
            log.error("Failed to get cache keys - name: {}, pattern: {}", cacheName, pattern, e);
            return Collections.emptySet();
        }
    }

    @Override
    public void clear(String cacheName) {
        try {
            Set<String> keys = keys(cacheName, "*");
            if (!keys.isEmpty()) {
                redisTemplate.delete(keys);
            }
            log.info("Cache cleared - name: {}, cleared entries: {}", cacheName, keys.size());
        } catch (Exception e) {
            log.error("Failed to clear cache - name: {}", cacheName, e);
        }
    }

    @Override
    public Map<String, Object> getStats(String cacheName) {
        CacheStats stats = cacheStatsMap.get(cacheName);
        if (stats == null) {
            stats = new CacheStats();
            cacheStatsMap.put(cacheName, stats);
        }
        return stats.toMap();
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
        try {
            String fullKey = getFullKey(cacheName, key);
            return redisTemplate.opsForValue().increment(fullKey, delta);
        } catch (Exception e) {
            log.error("Failed to increment cache - name: {}, key: {}", cacheName, key, e);
            return null;
        }
    }

    @Override
    public Long decrement(String cacheName, String key, long delta) {
        try {
            String fullKey = getFullKey(cacheName, key);
            return redisTemplate.opsForValue().decrement(fullKey, delta);
        } catch (Exception e) {
            log.error("Failed to decrement cache - name: {}, key: {}", cacheName, key, e);
            return null;
        }
    }

    @Override
    public void hset(String cacheName, String key, String field, Object value) {
        try {
            String fullKey = getFullKey(cacheName, key);
            redisTemplate.opsForHash().put(fullKey, field, value);
            log.debug("Hash set - name: {}, key: {}, field: {}", cacheName, key, field);
        } catch (Exception e) {
            log.error("Failed to set hash field - name: {}, key: {}, field: {}", cacheName, key, field, e);
        }
    }

    @Override
    public <T> T hget(String cacheName, String key, String field, Class<T> clazz) {
        try {
            String fullKey = getFullKey(cacheName, key);
            Object value = redisTemplate.opsForHash().get(fullKey, field);
            return value != null ? clazz.cast(value) : null;
        } catch (Exception e) {
            log.error("Failed to get hash field - name: {}, key: {}, field: {}", cacheName, key, field, e);
            return null;
        }
    }

    @Override
    public Map<String, Object> hgetAll(String cacheName, String key) {
        try {
            String fullKey = getFullKey(cacheName, key);
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
        try {
            String fullKey = getFullKey(cacheName, key);
            Object[] fieldArray = fields.toArray();
            redisTemplate.opsForHash().delete(fullKey, fieldArray);
            log.debug("Hash fields deleted - name: {}, key: {}, fields: {}", cacheName, key, fields);
        } catch (Exception e) {
            log.error("Failed to delete hash fields - name: {}, key: {}, fields: {}", cacheName, key, fields, e);
        }
    }

    @Override
    public void lpush(String cacheName, String key, List<Object> values, long ttl, TimeUnit timeUnit) {
        try {
            String fullKey = getFullKey(cacheName, key);
            redisTemplate.opsForList().rightPushAll(fullKey, values);
            redisTemplate.expire(fullKey, ttl, timeUnit);
            log.debug("List pushed - name: {}, key: {}, size: {}", cacheName, key, values.size());
        } catch (Exception e) {
            log.error("Failed to push list - name: {}, key: {}", cacheName, key, e);
        }
    }

    @Override
    public <T> List<T> lrange(String cacheName, String key, long start, long end, Class<T> clazz) {
        try {
            String fullKey = getFullKey(cacheName, key);
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
        try {
            String fullKey = getFullKey(cacheName, key);
            redisTemplate.opsForSet().add(fullKey, values.toArray());
            redisTemplate.expire(fullKey, ttl, timeUnit);
            log.debug("Set added - name: {}, key: {}, size: {}", cacheName, key, values.size());
        } catch (Exception e) {
            log.error("Failed to add set - name: {}, key: {}", cacheName, key, e);
        }
    }

    @Override
    public <T> Set<T> smembers(String cacheName, String key, Class<T> clazz) {
        try {
            String fullKey = getFullKey(cacheName, key);
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

    private String getFullKey(String cacheName, String key) {
        // 根据缓存名称添加前缀
        String prefix = getCachePrefix(cacheName);
        return prefix + key;
    }

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

    private void recordCacheHit(String cacheName) {
        CacheStats stats = cacheStatsMap.computeIfAbsent(cacheName, k -> new CacheStats());
        stats.recordHit();
    }

    private void recordCacheMiss(String cacheName) {
        CacheStats stats = cacheStatsMap.computeIfAbsent(cacheName, k -> new CacheStats());
        stats.recordMiss();
    }

    private void recordCachePut(String cacheName) {
        CacheStats stats = cacheStatsMap.computeIfAbsent(cacheName, k -> new CacheStats());
        stats.recordPut();
    }

    @Override
    public Boolean setIfAbsent(String cacheName, String key, Object value, long ttl, TimeUnit timeUnit) {
        try {
            String fullKey = getFullKey(cacheName, key);
            // 使用Redis的SETNX命令
            Boolean result = redisTemplate.opsForValue().setIfAbsent(fullKey, value, ttl, timeUnit);
            if (result != null && result) {
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
        try {
            String fullKey = getFullKey(cacheName, key);
            // 先获取旧值
            T oldValue = get(cacheName, key, clazz);
            // 设置新值
            set(cacheName, key, value);
            return oldValue;
        } catch (Exception e) {
            log.error("Failed to get and set cache - name: {}, key: {}", cacheName, key, e);
            return null;
        }
    }

    private void recordCacheEviction(String cacheName) {
        CacheStats stats = cacheStatsMap.computeIfAbsent(cacheName, k -> new CacheStats());
        stats.recordEviction();
    }
}