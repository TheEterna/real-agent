package com.ai.agent.real.entity.auth;

import reactor.core.publisher.Mono;
import reactor.util.context.Context;

/**
 * 用户上下文持有者（响应式）
 *
 * @author: han
 * @time: 2025/10/23 13:03
 */
public class UserContextHolder {

	private static final String USER_CONTEXT_KEY = "USER_CONTEXT";

	/**
	 * 设置用户上下文到 Reactor Context
	 */
	public static Context setUser(Context context, UserContext user) {
		return context.put(USER_CONTEXT_KEY, user);
	}

	/**
	 * 从 Reactor Context 获取用户上下文
	 */
	public static Mono<UserContext> getUser() {
		return Mono.deferContextual(ctx -> Mono.justOrEmpty(ctx.getOrEmpty(USER_CONTEXT_KEY)));
	}

	/**
	 * 获取用户 ID
	 */
	public static Mono<Long> getUserId() {
		return getUser().map(UserContext::getUserId);
	}

	/**
	 * 获取外部 ID
	 */
	public static Mono<String> getExternalId() {
		return getUser().map(UserContext::getExternalId);
	}

	/**
	 * 获取昵称
	 */
	public static Mono<String> getNickname() {
		return getUser().map(UserContext::getNickname);
	}

	/**
	 * 判断是否已认证
	 */
	public static Mono<Boolean> isAuthenticated() {
		return getUser().map(UserContext::isAuthenticated).defaultIfEmpty(false);
	}

	/**
	 * 要求用户已认证，否则抛出异常
	 */
	public static Mono<UserContext> requireAuthenticated() {
		return getUser().filter(UserContext::isAuthenticated)
			.switchIfEmpty(Mono.error(new UnauthorizedException("请先登录")));
	}

	/**
	 * 要求用户已认证，返回用户 ID
	 */
	public static Mono<Long> requireUserId() {
		return requireAuthenticated().map(UserContext::getUserId);
	}

	/**
	 * 未认证异常
	 */
	public static class UnauthorizedException extends RuntimeException {

		public UnauthorizedException(String message) {
			super(message);
		}

	}

}
