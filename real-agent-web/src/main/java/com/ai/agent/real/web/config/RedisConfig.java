package com.ai.agent.real.web.config;

import com.ai.agent.real.contract.infra.redis.IRedisService;
import com.ai.agent.real.infra.redis.RedisService;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Redis 配置类
 *
 * @author: han
 * @time: 2025/10/23 13:13
 */
@Configuration
public class RedisConfig {
    @Bean
    public IRedisService redisService(ReactiveRedisTemplate<String, Object> redisTemplate) {
        return new RedisService(redisTemplate);
    }

	/**
	 * 配置 ReactiveRedisTemplate
	 */
	@Bean
	public ReactiveRedisTemplate<String, Object> reactiveRedisTemplate(
			ReactiveRedisConnectionFactory connectionFactory) {

		// 使用 Jackson2JsonRedisSerializer 来序列化和反序列化 redis 的 value 值
		Jackson2JsonRedisSerializer<Object> serializer = new Jackson2JsonRedisSerializer<>(Object.class);

		// ObjectMapper mapper = new ObjectMapper();
		// mapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
		// mapper.activateDefaultTyping(LaissezFaireSubTypeValidator.instance,
		// ObjectMapper.DefaultTyping.NON_FINAL);
		// serializer.setObjectMapper(mapper);

		// 使用 StringRedisSerializer 来序列化和反序列化 redis 的 key 值
		StringRedisSerializer stringSerializer = new StringRedisSerializer();

		// 配置序列化
		RedisSerializationContext<String, Object> serializationContext = RedisSerializationContext
			.<String, Object>newSerializationContext()
			.key(stringSerializer)
			.value(serializer)
			.hashKey(stringSerializer)
			.hashValue(serializer)
			.build();

		return new ReactiveRedisTemplate<>(connectionFactory, serializationContext);
	}


}
