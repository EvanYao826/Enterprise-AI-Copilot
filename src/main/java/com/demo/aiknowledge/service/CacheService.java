package com.demo.aiknowledge.service;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * 统一缓存服务接口
 * 支持多级缓存：本地缓存 + Redis缓存
 */
public interface CacheService {

    /**
     * 设置缓存值
     *
     * @param cacheName 缓存名称
     * @param key       缓存键
     * @param value     缓存值
     * @param ttl       过期时间
     * @param timeUnit  时间单位
     */
    void set(String cacheName, String key, Object value, long ttl, TimeUnit timeUnit);

    /**
     * 设置缓存值（使用默认过期时间）
     *
     * @param cacheName 缓存名称
     * @param key       缓存键
     * @param value     缓存值
     */
    void set(String cacheName, String key, Object value);


    /**
     * 当键不存在时设置值 (SETNX)
     * @param cacheName 缓存名称
     * @param key       缓存键
     * @param value     缓存值
     * @param ttl       过期时间
     * @param timeUnit  时间单位
     * @return true 如果设置成功，false 如果键已存在
     */
    Boolean setIfAbsent(String cacheName, String key, Object value, long ttl, TimeUnit timeUnit);

    /**
     * 原子性地设置新值并返回旧值 (GETSET)
     * @param cacheName 缓存名称
     * @param key       缓存键
     * @param value     新值
     * @param clazz     返回值类型
     * @return 旧值
     */
    <T> T getAndSet(String cacheName, String key, Object value, Class<T> clazz);

    /**
     * 获取缓存值
     *
     * @param cacheName 缓存名称
     * @param key       缓存键
     * @param clazz     返回值类型
     * @return 缓存值
     */
    <T> T get(String cacheName, String key, Class<T> clazz);

    /**
     * 删除缓存
     *
     * @param cacheName 缓存名称
     * @param key       缓存键
     */
    void delete(String cacheName, String key);

    /**
     * 批量删除缓存
     *
     * @param cacheName 缓存名称
     * @param keys      缓存键集合
     */
    void delete(String cacheName, List<String> keys);

    /**
     * 检查缓存是否存在
     *
     * @param cacheName 缓存名称
     * @param key       缓存键
     * @return 是否存在
     */
    boolean exists(String cacheName, String key);

    /**
     * 获取缓存过期时间
     *
     * @param cacheName 缓存名称
     * @param key       缓存键
     * @return 剩余过期时间（秒）
     */
    Long getExpire(String cacheName, String key);

    /**
     * 设置缓存过期时间
     *
     * @param cacheName 缓存名称
     * @param key       缓存键
     * @param ttl       过期时间
     * @param timeUnit  时间单位
     */
    void expire(String cacheName, String key, long ttl, TimeUnit timeUnit);

    /**
     * 获取缓存键集合
     *
     * @param cacheName 缓存名称
     * @param pattern   匹配模式
     * @return 键集合
     */
    Set<String> keys(String cacheName, String pattern);

    /**
     * 清空缓存
     *
     * @param cacheName 缓存名称
     */
    void clear(String cacheName);

    /**
     * 获取缓存统计信息
     *
     * @param cacheName 缓存名称
     * @return 统计信息
     */
    Map<String, Object> getStats(String cacheName);

    /**
     * 获取所有缓存名称
     *
     * @return 缓存名称集合
     */
    List<String> getCacheNames();

    /**
     * 获取缓存命中率
     *
     * @param cacheName 缓存名称
     * @return 命中率（0-1）
     */
    double getHitRate(String cacheName);

    /**
     * 获取缓存大小
     *
     * @param cacheName 缓存名称
     * @return 缓存条目数
     */
    long getSize(String cacheName);

    /**
     * 批量设置缓存值
     *
     * @param cacheName 缓存名称
     * @param entries   键值对集合
     * @param ttl       过期时间
     * @param timeUnit  时间单位
     */
    void setBatch(String cacheName, Map<String, Object> entries, long ttl, TimeUnit timeUnit);

    /**
     * 批量获取缓存值
     *
     * @param cacheName 缓存名称
     * @param keys      缓存键集合
     * @param clazz     返回值类型
     * @return 键值对集合
     */
    <T> Map<String, T> getBatch(String cacheName, List<String> keys, Class<T> clazz);

    /**
     * 递增计数器
     *
     * @param cacheName 缓存名称
     * @param key       缓存键
     * @param delta     增量
     * @return 递增后的值
     */
    Long increment(String cacheName, String key, long delta);

    /**
     * 递减计数器
     *
     * @param cacheName 缓存名称
     * @param key       缓存键
     * @param delta     减量
     * @return 递减后的值
     */
    Long decrement(String cacheName, String key, long delta);

    /**
     * 设置哈希表字段
     *
     * @param cacheName 缓存名称
     * @param key       哈希表键
     * @param field     字段名
     * @param value     字段值
     */
    void hset(String cacheName, String key, String field, Object value);

    /**
     * 获取哈希表字段
     *
     * @param cacheName 缓存名称
     * @param key       哈希表键
     * @param field     字段名
     * @param clazz     返回值类型
     * @return 字段值
     */
    <T> T hget(String cacheName, String key, String field, Class<T> clazz);

    /**
     * 获取整个哈希表
     *
     * @param cacheName 缓存名称
     * @param key       哈希表键
     * @return 哈希表
     */
    Map<String, Object> hgetAll(String cacheName, String key);

    /**
     * 删除哈希表字段
     *
     * @param cacheName 缓存名称
     * @param key       哈希表键
     * @param fields    字段名集合
     */
    void hdelete(String cacheName, String key, List<String> fields);

    /**
     * 设置列表元素
     *
     * @param cacheName 缓存名称
     * @param key       列表键
     * @param values    元素集合
     * @param ttl       过期时间
     * @param timeUnit  时间单位
     */
    void lpush(String cacheName, String key, List<Object> values, long ttl, TimeUnit timeUnit);

    /**
     * 获取列表元素
     *
     * @param cacheName 缓存名称
     * @param key       列表键
     * @param start     起始位置
     * @param end       结束位置
     * @param clazz     元素类型
     * @return 元素集合
     */
    <T> List<T> lrange(String cacheName, String key, long start, long end, Class<T> clazz);

    /**
     * 设置集合元素
     *
     * @param cacheName 缓存名称
     * @param key       集合键
     * @param values    元素集合
     * @param ttl       过期时间
     * @param timeUnit  时间单位
     */
    void sadd(String cacheName, String key, Set<Object> values, long ttl, TimeUnit timeUnit);

    /**
     * 获取集合元素
     *
     * @param cacheName 缓存名称
     * @param key       集合键
     * @param clazz     元素类型
     * @return 元素集合
     */
    <T> Set<T> smembers(String cacheName, String key, Class<T> clazz);
}