package com.ai.agent.real.infra.redis;

import com.ai.agent.real.contract.infra.redis.IRedisService;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Duration;

/**
 * Redis 响应式服务
 */
public class RedisService implements IRedisService {

	private final ReactiveRedisTemplate<String, Object> redisTemplate;

	public RedisService(ReactiveRedisTemplate<String, Object> redisTemplate) {
		this.redisTemplate = redisTemplate;
	}

	/**
	 * 设置键值对
	 */
	public Mono<Boolean> set(String key, Object value) {
		return redisTemplate.opsForValue().set(key, value);
	}

	/**
	 * 设置键值对，带过期时间
	 */
	public Mono<Boolean> set(String key, Object value, Duration timeout) {
		return redisTemplate.opsForValue().set(key, value, timeout);
	}

	/**
	 * 获取值
	 */
	public Mono<Object> get(String key) {
		return redisTemplate.opsForValue().get(key);
	}

	/**
	 * 获取值，指定类型
	 */
	@SuppressWarnings("unchecked")
	public <T> Mono<T> get(String key, Class<T> clazz) {
		return redisTemplate.opsForValue().get(key).map(value -> (T) value);
	}

	/**
	 * 删除键
	 */
	public Mono<Boolean> delete(String key) {
		return redisTemplate.delete(key).map(count -> count > 0);
	}

	/**
	 * 判断键是否存在
	 */
	public Mono<Boolean> hasKey(String key) {
		return redisTemplate.hasKey(key);
	}

	/**
	 * 设置过期时间
	 */
	public Mono<Boolean> expire(String key, Duration timeout) {
		return redisTemplate.expire(key, timeout);
	}

	/**
	 * 获取剩余过期时间
	 */
	public Mono<Duration> getExpire(String key) {
		return redisTemplate.getExpire(key);
	}

	/**
	 * 递增
	 */
	public Mono<Long> increment(String key) {
		return redisTemplate.opsForValue().increment(key);
	}

	/**
	 * 递增指定值
	 */
	public Mono<Long> increment(String key, long delta) {
		return redisTemplate.opsForValue().increment(key, delta);
	}

	/**
	 * 递减
	 */
	public Mono<Long> decrement(String key) {
		return redisTemplate.opsForValue().decrement(key);
	}

	/**
	 * 递减指定值
	 */
	public Mono<Long> decrement(String key, long delta) {
		return redisTemplate.opsForValue().decrement(key, delta);
	}

}
