package com.ai.agent.real.contract.infra.redis;

import reactor.core.publisher.Mono;

import java.time.Duration;

public interface IRedisService {

	/**
	 * 设置键值对
	 */
	Mono<Boolean> set(String key, Object value);

	/**
	 * 设置键值对，带过期时间
	 */
	Mono<Boolean> set(String key, Object value, Duration timeout);

	/**
	 * 获取值
	 */
	Mono<Object> get(String key);

	/**
	 * 获取值，指定类型
	 */
	<T> Mono<T> get(String key, Class<T> clazz);

	/**
	 * 删除键
	 */
	Mono<Boolean> delete(String key);

	/**
	 * 判断键是否存在
	 */
	Mono<Boolean> hasKey(String key);

	/**
	 * 设置过期时间
	 */
	Mono<Boolean> expire(String key, Duration timeout);

	/**
	 * 获取剩余过期时间
	 */
	Mono<Duration> getExpire(String key);

	/**
	 * 递增
	 */
	Mono<Long> increment(String key);

	/**
	 * 递增指定值
	 */
	Mono<Long> increment(String key, long delta);

	/**
	 * 递减
	 */
	Mono<Long> decrement(String key);

	/**
	 * 递减指定值
	 */
	Mono<Long> decrement(String key, long delta);

}
