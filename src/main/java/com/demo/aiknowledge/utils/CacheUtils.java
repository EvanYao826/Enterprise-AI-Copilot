package com.demo.aiknowledge.utils;

import com.demo.aiknowledge.config.CacheConfig;
import com.demo.aiknowledge.service.CacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * 缓存工具类
 * 提供常用的缓存操作方法
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CacheUtils {

    private final CacheService cacheService;

    /**
     * 获取或设置缓存（防止缓存击穿）
     *
     * @param cacheName 缓存名称
     * @param key       缓存键
     * @param supplier  数据提供者（当缓存不存在时调用）
     * @param clazz     返回值类型
     * @return 缓存值
     */
    public <T> T getOrSet(String cacheName, String key, Supplier<T> supplier, Class<T> clazz) {
        return getOrSet(cacheName, key, supplier, clazz, getDefaultTtl(cacheName), TimeUnit.SECONDS);
    }

    /**
     * 获取或设置缓存（防止缓存击穿）
     *
     * @param cacheName 缓存名称
     * @param key       缓存键
     * @param supplier  数据提供者（当缓存不存在时调用）
     * @param clazz     返回值类型
     * @param ttl       过期时间
     * @param timeUnit  时间单位
     * @return 缓存值
     */
    public <T> T getOrSet(String cacheName, String key, Supplier<T> supplier, Class<T> clazz,
                          long ttl, TimeUnit timeUnit) {
        // 1. 尝试从缓存获取
        T cachedValue = cacheService.get(cacheName, key, clazz);
        if (cachedValue != null) {
            log.debug("Cache hit - name: {}, key: {}", cacheName, key);
            return cachedValue;
        }

        log.debug("Cache miss - name: {}, key: {}", cacheName, key);

        // 2. 缓存不存在，执行数据加载
        try {
            T value = supplier.get();
            if (value != null) {
                // 设置缓存
                cacheService.set(cacheName, key, value, ttl, timeUnit);
                log.debug("Cache set - name: {}, key: {}, ttl: {} {}", cacheName, key, ttl, timeUnit);
            }
            return value;
        } catch (Exception e) {
            log.error("Failed to load data for cache - name: {}, key: {}", cacheName, key, e);
            return null;
        }
    }

    /**
     * 获取或设置缓存（带互斥锁，防止缓存击穿）
     *
     * @param cacheName 缓存名称
     * @param key       缓存键
     * @param supplier  数据提供者
     * @param clazz     返回值类型
     * @param lockKey   锁键（用于分布式锁）
     * @return 缓存值
     */
    public <T> T getOrSetWithLock(String cacheName, String key, Supplier<T> supplier,
                                  Class<T> clazz, String lockKey) {
        return getOrSetWithLock(cacheName, key, supplier, clazz, lockKey,
                getDefaultTtl(cacheName), TimeUnit.SECONDS, 5000L);
    }

    /**
     * 获取或设置缓存（带互斥锁，防止缓存击穿）
     *
     * @param cacheName     缓存名称
     * @param key           缓存键
     * @param supplier      数据提供者
     * @param clazz         返回值类型
     * @param lockKey       锁键
     * @param ttl           缓存过期时间
     * @param timeUnit      时间单位
     * @param lockTimeout   锁超时时间（毫秒）
     * @return 缓存值
     */
    public <T> T getOrSetWithLock(String cacheName, String key, Supplier<T> supplier,
                                  Class<T> clazz, String lockKey,
                                  long ttl, TimeUnit timeUnit, long lockTimeout) {
        // 1. 尝试从缓存获取
        T cachedValue = cacheService.get(cacheName, key, clazz);
        if (cachedValue != null) {
            return cachedValue;
        }

        // 2. 尝试获取分布式锁
        String lockCacheName = "cache_lock";
        boolean lockAcquired = false;
        try {
            // 尝试获取锁（使用SETNX实现）
            lockAcquired = acquireLock(lockCacheName, lockKey, lockTimeout);

            if (lockAcquired) {
                // 再次检查缓存（防止其他线程已经加载）
                cachedValue = cacheService.get(cacheName, key, clazz);
                if (cachedValue != null) {
                    return cachedValue;
                }

                // 执行数据加载
                T value = supplier.get();
                if (value != null) {
                    cacheService.set(cacheName, key, value, ttl, timeUnit);
                }
                return value;
            } else {
                // 等待其他线程加载
                Thread.sleep(100);
                return cacheService.get(cacheName, key, clazz);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Cache lock interrupted - name: {}, key: {}", cacheName, key, e);
            return null;
        } finally {
            if (lockAcquired) {
                releaseLock(lockCacheName, lockKey);
            }
        }
    }

    /**
     * 批量获取或设置缓存
     *
     * @param cacheName 缓存名称
     * @param keys      缓存键集合
     * @param supplier  数据提供者（接收缺失的键，返回对应的值）
     * @param clazz     返回值类型
     * @return 键值对集合
     */
    public <T> java.util.Map<String, T> batchGetOrSet(String cacheName, java.util.List<String> keys,
                                                      java.util.function.Function<java.util.List<String>, java.util.Map<String, T>> supplier,
                                                      Class<T> clazz) {
        // 1. 批量获取缓存
        java.util.Map<String, T> cachedValues = cacheService.getBatch(cacheName, keys, clazz);

        // 2. 找出缺失的键
        java.util.List<String> missingKeys = keys.stream()
                .filter(key -> !cachedValues.containsKey(key))
                .toList();

        if (!missingKeys.isEmpty()) {
            // 3. 加载缺失的数据
            java.util.Map<String, T> loadedValues = supplier.apply(missingKeys);

            if (loadedValues != null && !loadedValues.isEmpty()) {
                // 4. 设置缓存
                cacheService.setBatch(cacheName, new java.util.HashMap<>(loadedValues),
                        getDefaultTtl(cacheName), TimeUnit.SECONDS);

                // 5. 合并结果
                cachedValues.putAll(loadedValues);
            }
        }

        return cachedValues;
    }

    /**
     * 刷新缓存（先删除再重新加载）
     *
     * @param cacheName 缓存名称
     * @param key       缓存键
     * @param supplier  数据提供者
     * @param clazz     返回值类型
     * @return 新的缓存值
     */
    public <T> T refresh(String cacheName, String key, Supplier<T> supplier, Class<T> clazz) {
        // 1. 删除旧缓存
        cacheService.delete(cacheName, key);

        // 2. 重新加载数据
        T value = supplier.get();
        if (value != null) {
            cacheService.set(cacheName, key, value);
        }

        return value;
    }

    /**
     * 设置缓存并返回旧值
     *
     * @param cacheName 缓存名称
     * @param key       缓存键
     * @param value     新值
     * @param clazz     返回值类型
     * @return 旧值（如果存在）
     */
    public <T> T getAndSet(String cacheName, String key, T value, Class<T> clazz) {
        T oldValue = cacheService.get(cacheName, key, clazz);
        cacheService.set(cacheName, key, value);
        return oldValue;
    }

    /**
     * 缓存预热
     *
     * @param cacheName 缓存名称
     * @param data      预热数据
     */
    public <T> void warmUp(String cacheName, java.util.Map<String, T> data) {
        if (data != null && !data.isEmpty()) {
            cacheService.setBatch(cacheName, new java.util.HashMap<>(data),
                    getDefaultTtl(cacheName), TimeUnit.SECONDS);
            log.info("Cache warmed up - name: {}, entries: {}", cacheName, data.size());
        }
    }

    /**
     * 获取缓存命中率
     *
     * @param cacheName 缓存名称
     * @return 命中率（0-1）
     */
    public double getHitRate(String cacheName) {
        return cacheService.getHitRate(cacheName);
    }

    /**
     * 获取缓存大小
     *
     * @param cacheName 缓存名称
     * @return 缓存条目数
     */
    public long getSize(String cacheName) {
        return cacheService.getSize(cacheName);
    }

    /**
     * 清空缓存
     *
     * @param cacheName 缓存名称
     */
    public void clear(String cacheName) {
        cacheService.clear(cacheName);
        log.info("Cache cleared - name: {}", cacheName);
    }

    // 私有辅助方法

    private boolean acquireLock(String cacheName, String lockKey, long timeoutMs) {
        // 使用Redis的SETNX实现分布式锁
        String lockValue = String.valueOf(System.currentTimeMillis() + timeoutMs + 1);

        // 先检查是否已存在锁
        String currentValue = cacheService.get(cacheName, lockKey, String.class);
        if (currentValue == null) {
            // 尝试设置锁
            cacheService.set(cacheName, lockKey, lockValue, timeoutMs, TimeUnit.MILLISECONDS);

            // 再次检查是否设置成功（防止并发）
            String newValue = cacheService.get(cacheName, lockKey, String.class);
            if (newValue != null && newValue.equals(lockValue)) {
                return true;
            }
        } else {
            // 检查锁是否已过期
            try {
                long expireTime = Long.parseLong(currentValue);
                if (System.currentTimeMillis() > expireTime) {
                    // 锁已过期，删除并重新获取
                    cacheService.delete(cacheName, lockKey);
                    return acquireLock(cacheName, lockKey, timeoutMs);
                }
            } catch (NumberFormatException e) {
                // 锁值格式错误，删除并重新获取
                cacheService.delete(cacheName, lockKey);
                return acquireLock(cacheName, lockKey, timeoutMs);
            }
        }

        return false;
    }

    private void releaseLock(String cacheName, String lockKey) {
        cacheService.delete(cacheName, lockKey);
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

}