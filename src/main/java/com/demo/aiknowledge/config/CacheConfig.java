package com.demo.aiknowledge.config;

import org.springframework.context.annotation.Primary;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.boot.autoconfigure.cache.CacheAutoConfiguration;

/**
 * Redis配置类
 * 提供RedisTemplate和StringRedisTemplate的配置
 * 排除CacheAutoConfiguration以避免缓存管理器冲突
 */
@Configuration
@EnableAutoConfiguration(exclude = {CacheAutoConfiguration.class})
public class CacheConfig {

    /**
     * RedisTemplate 配置
     * 使用@Primary注解确保这是主要的RedisTemplate bean
     */
    @Bean
    @Primary
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory redisConnectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(redisConnectionFactory);

        // 使用 Jackson 序列化器
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        GenericJackson2JsonRedisSerializer serializer = new GenericJackson2JsonRedisSerializer(objectMapper);

        // 设置 key 的序列化器
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());

        // 设置 value 的序列化器
        template.setValueSerializer(serializer);
        template.setHashValueSerializer(serializer);

        template.afterPropertiesSet();
        return template;
    }

    /**
     * StringRedisTemplate 配置
     */
    @Bean
    public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory redisConnectionFactory) {
        StringRedisTemplate template = new StringRedisTemplate();
        template.setConnectionFactory(redisConnectionFactory);
        return template;
    }

    /**
     * 缓存常量定义
     */
    public static class CacheConstants {
        // 缓存名称
        public static final String CACHE_AI_ANSWER = "ai_answer";
        public static final String CACHE_USER_SESSION = "user_session";
        public static final String CACHE_DOC_METADATA = "doc_metadata";
        public static final String CACHE_CONVERSATION_CONTEXT = "conversation_context";
        public static final String CACHE_VECTOR_SEARCH = "vector_search";

        // 缓存键前缀
        public static final String KEY_AI_ANSWER = "ai:answer:";
        public static final String KEY_USER_SESSION = "user:session:";
        public static final String KEY_DOC_METADATA = "doc:metadata:";
        public static final String KEY_CONVERSATION_CONTEXT = "conv:ctx:";
        public static final String KEY_VECTOR_SEARCH = "vector:search:";

        // 过期时间（秒）
        public static final long TTL_AI_ANSWER = 3600; // 1小时
        public static final long TTL_USER_SESSION = 86400; // 24小时
        public static final long TTL_DOC_METADATA = 300; // 5分钟
        public static final long TTL_CONVERSATION_CONTEXT = 1800; // 30分钟
        public static final long TTL_VECTOR_SEARCH = 600; // 10分钟

        // 最大缓存条目数
        public static final int MAX_AI_ANSWER = 10000;
        public static final int MAX_USER_SESSION = 5000;
        public static final int MAX_DOC_METADATA = 1000;
        public static final int MAX_CONVERSATION_CONTEXT = 2000;
        public static final int MAX_VECTOR_SEARCH = 500;
    }
}