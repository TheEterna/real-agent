package com.ai.agent.real.application.service.auth;

import com.ai.agent.real.common.utils.CommonUtils;
import com.ai.agent.real.contract.infra.redis.IRedisService;
import com.ai.agent.real.contract.service.ITokenService;
import com.ai.agent.real.domain.entity.user.User;
import com.ai.agent.real.contract.model.auth.UserContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Token 服务（纯 Redis 方案，不使用 JWT）
 */
@Service
@Slf4j
public class TokenService implements ITokenService {

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
	 * 生成访问令牌和刷新令牌（兼容旧版本，不带设备信息）
	 */
	@Override
	public Mono<TokenPair> generateTokenPair(User user) {
		return generateTokenPair(user, null, null);
	}

	/**
	 * 生成访问令牌和刷新令牌（带设备信息）
	 */
	@Override
	public Mono<TokenPair> generateTokenPair(User user, String ipAddress, String deviceInfo) {
		// 生成随机 Token
		String accessToken = CommonUtils.generateUuidToken();
		String refreshToken = CommonUtils.generateUuidToken();

		// 构建用户上下文
		UserContext userContext = UserContext.builder()
			.userId(user.getId())
			.externalId(user.getExternalId())
			.ipAddress(ipAddress)
			.deviceInfo(deviceInfo)
			.build();

		// 将 Token 存储到 Redis
		return storeAccessToken(accessToken, userContext).then(storeRefreshToken(refreshToken, userContext))
			.then(addToOnlineUsers(user.getId(), accessToken))
			.thenReturn(new TokenPair(accessToken, refreshToken, ACCESS_TOKEN_DURATION.toSeconds()));
	}

	/**
	 * 存储访问令牌到 Redis
	 * @param token
	 * @param userContext
	 */
	@Override
	public Mono<Boolean> storeAccessToken(String token, UserContext userContext) {
		String key = TokenService.TOKEN_PREFIX + token;
		// 存储 Token → UserContext 映射
		return redisService.set(key, userContext, TokenService.ACCESS_TOKEN_DURATION)
			// 同时记录用户的 Token（用于单点登录控制和管理员踢人）
			.then(redisService.set(TokenService.USER_TOKEN_PREFIX + userContext.getUserId(), token,
					TokenService.ACCESS_TOKEN_DURATION));
	}

	/**
	 * 存储刷新令牌到 Redis（包含设备信息用于验证）
	 * @param token
	 * @param userContext
	 */
	@Override
	public Mono<Boolean> storeRefreshToken(String token, UserContext userContext) {
		String key = TOKEN_PREFIX + token;
		// 存储完整的 UserContext，包括设备信息用于后续验证
		return redisService.set(key, userContext, REFRESH_TOKEN_DURATION);
	}

	/**
	 * 添加到在线用户列表
	 * @param userId
	 * @param token
	 */
	@Override
	public Mono<Boolean> addToOnlineUsers(UUID userId, String token) {

		// 使用 Hash 存储在线用户：userId → token
		Map<String, Object> userInfo = new HashMap<>();
		userInfo.put("token", token);
		userInfo.put("loginTime", LocalDateTime.now().toString());
		return redisService.set(TokenService.ONLINE_USERS_PREFIX + ":" + userId, userInfo,
				TokenService.ACCESS_TOKEN_DURATION);
	}

	/**
	 * 验证 Token 并获取用户上下文（纯 Redis 方案）
	 */
	@Override
	public Mono<UserContext> validateToken(String token) {
		// 直接从 Redis 获取用户上下文
		String key = TOKEN_PREFIX + token;

		return redisService.get(key, UserContext.class);
	}

	/**
	 * 验证刷新令牌并检查设备信息
	 */
	@Override
	public Mono<UserContext> validateRefreshToken(String token, String ipAddress, String deviceInfo) {
		return validateToken(token).filter(ctx -> {
			// 如果存储的Token中没有设备信息，则跳过验证（兼容旧数据）
			if (ctx.getIpAddress() == null) {
				return true;
			}
			// 验证IP地址是否匹配
			return ipAddress != null && ipAddress.equals(ctx.getIpAddress());
		}).switchIfEmpty(Mono.error(new SecurityException("IP地址不匹配，可能存在安全风险")));
	}

	/**
	 * 刷新访问令牌（兼容旧版本）
	 */
	@Override
	public Mono<TokenPair> refreshAccessToken(String refreshToken) {
		return refreshAccessToken(refreshToken, null, null);
	}

	/**
	 * 刷新访问令牌（带设备信息验证）
	 */
	@Override
	public Mono<TokenPair> refreshAccessToken(String refreshToken, String ipAddress, String deviceInfo) {
		// 1. 验证刷新令牌并检查设备信息
		return validateRefreshToken(refreshToken, ipAddress, deviceInfo).flatMap(userContext -> {
			// 2. 生成新的访问令牌
			String newAccessToken = CommonUtils.generateUuidToken();

			// 3. 存储新的访问令牌（保留原有的设备信息）
			return storeAccessToken(newAccessToken, userContext)
				.then(addToOnlineUsers(userContext.getUserId(), newAccessToken))
				.thenReturn(new TokenPair(newAccessToken, refreshToken, ACCESS_TOKEN_DURATION.toSeconds()));
		});
	}

	/**
	 * 登出（直接删除 Token）
	 */
	@Override
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

}
