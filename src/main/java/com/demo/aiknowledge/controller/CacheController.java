package com.demo.aiknowledge.controller;

import com.demo.aiknowledge.config.CacheConfig;
import com.demo.aiknowledge.service.CacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 缓存管理控制器
 * 提供缓存监控和管理功能
 */
@RestController
@RequestMapping("/api/cache")
@RequiredArgsConstructor
@Slf4j
public class CacheController {

    private final CacheService cacheService;

    /**
     * 获取缓存统计信息
     */
    @GetMapping("/stats")
    public Map<String, Object> getCacheStats() {
        Map<String, Object> result = new HashMap<>();

        // 获取所有缓存名称
        List<String> cacheNames = cacheService.getCacheNames();

        // 收集每个缓存的统计信息
        Map<String, Object> cacheStats = new HashMap<>();
        for (String cacheName : cacheNames) {
            Map<String, Object> stats = cacheService.getStats(cacheName);
            stats.put("size", cacheService.getSize(cacheName));
            stats.put("hitRate", cacheService.getHitRate(cacheName));
            cacheStats.put(cacheName, stats);
        }

        result.put("cacheStats", cacheStats);
        result.put("totalCaches", cacheNames.size());
        result.put("timestamp", System.currentTimeMillis());

        return result;
    }

    /**
     * 获取指定缓存的详细信息
     */
    @GetMapping("/{cacheName}/info")
    public Map<String, Object> getCacheInfo(@PathVariable String cacheName) {
        Map<String, Object> result = new HashMap<>();

        result.put("cacheName", cacheName);
        result.put("stats", cacheService.getStats(cacheName));
        result.put("size", cacheService.getSize(cacheName));
        result.put("hitRate", cacheService.getHitRate(cacheName));

        // 获取缓存键（限制前100个）
        Set<String> keys = cacheService.keys(cacheName, "*");
        if (keys != null && !keys.isEmpty()) {
            List<String> keyList = keys.stream().limit(100).toList();
            result.put("keys", keyList);
            result.put("totalKeys", keys.size());
        }

        return result;
    }

    /**
     * 获取缓存值
     */
    @GetMapping("/{cacheName}/key/{key}")
    public Map<String, Object> getCacheValue(
            @PathVariable String cacheName,
            @PathVariable String key) {
        Map<String, Object> result = new HashMap<>();

        // 检查缓存是否存在
        boolean exists = cacheService.exists(cacheName, key);
        result.put("exists", exists);

        if (exists) {
            // 获取缓存值（作为Object返回，前端需要根据类型处理）
            Object value = cacheService.get(cacheName, key, Object.class);
            result.put("value", value);

            // 获取过期时间
            Long expire = cacheService.getExpire(cacheName, key);
            result.put("expireSeconds", expire);
        }

        return result;
    }

    /**
     * 删除缓存键
     */
    @DeleteMapping("/{cacheName}/key/{key}")
    public Map<String, Object> deleteCacheKey(
            @PathVariable String cacheName,
            @PathVariable String key) {
        Map<String, Object> result = new HashMap<>();

        boolean existed = cacheService.exists(cacheName, key);
        cacheService.delete(cacheName, key);

        result.put("success", true);
        result.put("cacheName", cacheName);
        result.put("key", key);
        result.put("existed", existed);

        log.info("Cache key deleted - name: {}, key: {}", cacheName, key);
        return result;
    }

    /**
     * 清空缓存
     */
    @DeleteMapping("/{cacheName}")
    public Map<String, Object> clearCache(@PathVariable String cacheName) {
        Map<String, Object> result = new HashMap<>();

        long sizeBefore = cacheService.getSize(cacheName);
        cacheService.clear(cacheName);

        result.put("success", true);
        result.put("cacheName", cacheName);
        result.put("clearedEntries", sizeBefore);

        log.info("Cache cleared - name: {}, cleared entries: {}", cacheName, sizeBefore);
        return result;
    }

    /**
     * 刷新缓存（删除并重新加载）
     */
    @PostMapping("/{cacheName}/refresh")
    public Map<String, Object> refreshCache(@PathVariable String cacheName) {
        Map<String, Object> result = new HashMap<>();

        long sizeBefore = cacheService.getSize(cacheName);
        cacheService.clear(cacheName);

        result.put("success", true);
        result.put("cacheName", cacheName);
        result.put("clearedEntries", sizeBefore);
        result.put("message", "Cache cleared. New data will be loaded on next request.");

        log.info("Cache refreshed - name: {}, cleared entries: {}", cacheName, sizeBefore);
        return result;
    }

    /**
     * 获取缓存配置信息
     */
    @GetMapping("/config")
    public Map<String, Object> getCacheConfig() {
        Map<String, Object> config = new HashMap<>();

        // 缓存常量配置
        Map<String, Object> constants = new HashMap<>();
        constants.put("CACHE_AI_ANSWER", CacheConfig.CacheConstants.CACHE_AI_ANSWER);
        constants.put("CACHE_USER_SESSION", CacheConfig.CacheConstants.CACHE_USER_SESSION);
        constants.put("CACHE_DOC_METADATA", CacheConfig.CacheConstants.CACHE_DOC_METADATA);
        constants.put("CACHE_CONVERSATION_CONTEXT", CacheConfig.CacheConstants.CACHE_CONVERSATION_CONTEXT);
        constants.put("CACHE_VECTOR_SEARCH", CacheConfig.CacheConstants.CACHE_VECTOR_SEARCH);

        // TTL配置
        Map<String, Long> ttls = new HashMap<>();
        ttls.put("AI_ANSWER", CacheConfig.CacheConstants.TTL_AI_ANSWER);
        ttls.put("USER_SESSION", CacheConfig.CacheConstants.TTL_USER_SESSION);
        ttls.put("DOC_METADATA", CacheConfig.CacheConstants.TTL_DOC_METADATA);
        ttls.put("CONVERSATION_CONTEXT", CacheConfig.CacheConstants.TTL_CONVERSATION_CONTEXT);
        ttls.put("VECTOR_SEARCH", CacheConfig.CacheConstants.TTL_VECTOR_SEARCH);

        // 最大条目数配置
        Map<String, Integer> maxEntries = new HashMap<>();
        maxEntries.put("AI_ANSWER", CacheConfig.CacheConstants.MAX_AI_ANSWER);
        maxEntries.put("USER_SESSION", CacheConfig.CacheConstants.MAX_USER_SESSION);
        maxEntries.put("DOC_METADATA", CacheConfig.CacheConstants.MAX_DOC_METADATA);
        maxEntries.put("CONVERSATION_CONTEXT", CacheConfig.CacheConstants.MAX_CONVERSATION_CONTEXT);
        maxEntries.put("VECTOR_SEARCH", CacheConfig.CacheConstants.MAX_VECTOR_SEARCH);

        config.put("constants", constants);
        config.put("ttls", ttls);
        config.put("maxEntries", maxEntries);
        config.put("cacheNames", cacheService.getCacheNames());

        return config;
    }

    /**
     * 获取缓存健康状态
     */
    @GetMapping("/health")
    public Map<String, Object> getCacheHealth() {
        Map<String, Object> health = new HashMap<>();

        List<String> cacheNames = cacheService.getCacheNames();
        Map<String, Boolean> cacheStatus = new HashMap<>();

        for (String cacheName : cacheNames) {
            try {
                // 尝试获取缓存统计信息来检查连接
                cacheService.getStats(cacheName);
                cacheStatus.put(cacheName, true);
            } catch (Exception e) {
                cacheStatus.put(cacheName, false);
                log.warn("Cache health check failed for: {}", cacheName, e);
            }
        }

        health.put("status", "UP");
        health.put("cacheCount", cacheNames.size());
        health.put("cacheStatus", cacheStatus);
        health.put("timestamp", System.currentTimeMillis());

        return health;
    }

    /**
     * 批量操作缓存
     */
    @PostMapping("/batch")
    public Map<String, Object> batchOperation(@RequestBody Map<String, Object> request) {
        Map<String, Object> result = new HashMap<>();

        String operation = (String) request.get("operation");
        String cacheName = (String) request.get("cacheName");
        List<String> keys = (List<String>) request.get("keys");

        if (operation == null || cacheName == null) {
            result.put("success", false);
            result.put("message", "Operation and cacheName are required");
            return result;
        }

        switch (operation.toLowerCase()) {
            case "delete":
                if (keys != null && !keys.isEmpty()) {
                    cacheService.delete(cacheName, keys);
                    result.put("deletedCount", keys.size());
                }
                break;
            case "exists":
                if (keys != null && !keys.isEmpty()) {
                    Map<String, Boolean> existsMap = new HashMap<>();
                    for (String key : keys) {
                        existsMap.put(key, cacheService.exists(cacheName, key));
                    }
                    result.put("exists", existsMap);
                }
                break;
            default:
                result.put("success", false);
                result.put("message", "Unsupported operation: " + operation);
                return result;
        }

        result.put("success", true);
        result.put("operation", operation);
        result.put("cacheName", cacheName);
        return result;
    }
}