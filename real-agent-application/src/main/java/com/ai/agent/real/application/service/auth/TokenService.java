package com.ai.agent.real.application.service.auth;

import com.ai.agent.real.common.utils.CommonUtils;
import com.ai.agent.real.contract.infra.redis.IRedisService;
import com.ai.agent.real.domain.entity.user.User;
import com.ai.agent.real.entity.auth.UserContext;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Token 服务（纯 Redis 方案，不使用 JWT）
 */
@Service
public class TokenService {

	private final IRedisService redisService;

	// Redis Key 前缀
	private static final String TOKEN_PREFIX = "auth:token:";

	private static final String USER_TOKEN_PREFIX = "auth:user:token:";

	private static final String ONLINE_USERS_PREFIX = "auth:online:users";

	// Token 有效期
	private static final Duration ACCESS_TOKEN_DURATION = Duration.ofHours(2); // 2小时

	private static final Duration REFRESH_TOKEN_DURATION = Duration.ofDays(7); // 7天

	public TokenService(IRedisService redisService) {
		this.redisService = redisService;
	}

	/**
	 * 生成访问令牌和刷新令牌
	 */
	public Mono<TokenPair> generateTokenPair(User user) {
		// 生成随机 Token
		String accessToken = CommonUtils.generateUuidToken();
		String refreshToken = CommonUtils.generateUuidToken();

		// 构建用户上下文
		UserContext userContext = UserContext.builder()
			.userId(user.getId())
			.externalId(user.getExternalId())
			.nickname(user.getNickname())
			.avatarUrl(user.getAvatarUrl())
			.build();

		// 将 Token 存储到 Redis
		return storeAccessToken(accessToken, userContext).then(storeRefreshToken(refreshToken, userContext))
			.then(addToOnlineUsers(user.getId(), accessToken))
			.thenReturn(new TokenPair(accessToken, refreshToken, ACCESS_TOKEN_DURATION.toSeconds()));
	}

	/**
	 * 存储访问令牌到 Redis
	 */
	private Mono<Boolean> storeAccessToken(String token, UserContext userContext) {
		String key = TOKEN_PREFIX + token;
		// 存储 Token → UserContext 映射
		return redisService.set(key, userContext, ACCESS_TOKEN_DURATION)
			// 同时记录用户的 Token（用于单点登录控制和管理员踢人）
			.then(redisService.set(USER_TOKEN_PREFIX + userContext.getUserId(), token, ACCESS_TOKEN_DURATION));
	}

	/**
	 * 存储刷新令牌到 Redis
	 */
	private Mono<Boolean> storeRefreshToken(String token, UserContext userContext) {
		String key = TOKEN_PREFIX + token;
		// 存储 Token → UserContext 映射（只存储基本信息）
		UserContext refreshContext = UserContext.builder()
			.userId(userContext.getUserId())
			.externalId(userContext.getExternalId())
			.build();
		return redisService.set(key, refreshContext, REFRESH_TOKEN_DURATION);
	}

	/**
	 * 添加到在线用户列表
	 */
	private Mono<Boolean> addToOnlineUsers(Long userId, String token) {
		// 使用 Hash 存储在线用户：userId → token
		Map<String, Object> userInfo = new HashMap<>();
		userInfo.put("token", token);
		userInfo.put("loginTime", LocalDateTime.now().toString());
		return redisService.set(ONLINE_USERS_PREFIX + ":" + userId, userInfo, ACCESS_TOKEN_DURATION);
	}

	/**
	 * 验证 Token 并获取用户上下文（纯 Redis 方案）
	 */
	public Mono<UserContext> validateToken(String token) {
		// 直接从 Redis 获取用户上下文
		String key = TOKEN_PREFIX + token;
		return redisService.get(key, UserContext.class);
	}

	/**
	 * 刷新访问令牌
	 */
	public Mono<TokenPair> refreshAccessToken(String refreshToken) {
		// 1. 验证刷新令牌
		return validateToken(refreshToken).flatMap(userContext -> {
			// 2. 生成新的访问令牌
			String newAccessToken = CommonUtils.generateUuidToken();

			// 3. 存储新的访问令牌
			return storeAccessToken(newAccessToken, userContext)
				.then(addToOnlineUsers(userContext.getUserId(), newAccessToken))
				.thenReturn(new TokenPair(newAccessToken, refreshToken, ACCESS_TOKEN_DURATION.toSeconds()));
		});
	}

	/**
	 * 登出（直接删除 Token）
	 */
	public Mono<Void> logout(String token) {
		// 1. 从 Redis 获取用户信息
		return validateToken(token).flatMap(userContext -> {
			// 2. 删除 Token
			return redisService.delete(TOKEN_PREFIX + token)
				// 3. 删除用户的 Token 映射
				.then(redisService.delete(USER_TOKEN_PREFIX + userContext.getUserId()))
				// 4. 从在线用户列表移除
				.then(redisService.delete(ONLINE_USERS_PREFIX + ":" + userContext.getUserId()));
		}).then();
	}

	/**
	 * 管理员踢用户下线
	 */
	public Mono<Void> kickUser(Long userId) {
		// 1. 获取用户的 Token
		return redisService.get(USER_TOKEN_PREFIX + userId, String.class).flatMap(token -> {
			// 2. 删除 Token
			return redisService.delete(TOKEN_PREFIX + token)
				// 3. 删除用户的 Token 映射
				.then(redisService.delete(USER_TOKEN_PREFIX + userId))
				// 4. 从在线用户列表移除
				.then(redisService.delete(ONLINE_USERS_PREFIX + ":" + userId));
		}).then();
	}

	/**
	 * 获取所有在线用户（管理员功能）
	 */
	public Flux<Long> getOnlineUsers() {
		// TODO: 实现获取所有在线用户的逻辑
		// 需要使用 Redis SCAN 命令遍历 auth:online:users:* 的 key
		return Flux.empty();
	}

	/**
	 * Token 对
	 */
	public record TokenPair(String accessToken, String refreshToken, Long expiresIn) {
	}

}
