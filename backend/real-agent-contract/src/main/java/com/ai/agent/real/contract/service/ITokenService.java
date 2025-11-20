package com.ai.agent.real.contract.service;

import com.ai.agent.real.contract.model.auth.UserContext;
import com.ai.agent.real.domain.entity.user.User;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * @author han
 * @time 2025/11/21 01:56
 */
public interface ITokenService {

	Mono<TokenPair> generateTokenPair(User user);

	/**
	 * 生成访问令牌和刷新令牌（带设备信息）
	 */
	Mono<TokenPair> generateTokenPair(User user, String ipAddress, String deviceInfo);

	/**
	 * 存储访问令牌到 Redis
	 */
	Mono<Boolean> storeAccessToken(String token, UserContext userContext);

	/**
	 * 存储刷新令牌到 Redis
	 */
	Mono<Boolean> storeRefreshToken(String token, UserContext userContext);

	/**
	 * 添加到在线用户列表
	 */
	Mono<Boolean> addToOnlineUsers(UUID userId, String token);

	Mono<UserContext> validateToken(String token);

	/**
	 * 验证刷新令牌并检查设备信息
	 */
	Mono<UserContext> validateRefreshToken(String token, String ipAddress, String deviceInfo);

	Mono<TokenPair> refreshAccessToken(String refreshToken);

	/**
	 * 刷新访问令牌（带设备信息验证）
	 */
	Mono<TokenPair> refreshAccessToken(String refreshToken, String ipAddress, String deviceInfo);

	Mono<Void> logout(String token);

	/**
	 * Token 对
	 */
	public record TokenPair(String accessToken, String refreshToken, Long expiresIn) {
	}

}
