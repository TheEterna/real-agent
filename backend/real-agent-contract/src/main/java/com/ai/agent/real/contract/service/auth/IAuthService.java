package com.ai.agent.real.contract.service.auth;

import com.ai.agent.real.domain.entity.user.User;
import reactor.core.publisher.Mono;

/**
 * @author han
 * @time 2025/11/21 01:54
 */
public interface IAuthService {

	Mono<LoginResult> login(String externalId, String password);

	/**
	 * 用户登录（带设备信息）
	 */
	Mono<LoginResult> login(String externalId, String password, String ipAddress, String deviceInfo);

	Mono<User> register(String externalId, String password, String nickname, String avatarUrl);

	Mono<ITokenService.TokenPair> refreshToken(String refreshToken);

	/**
	 * 刷新 Token（带设备信息验证）
	 */
	Mono<ITokenService.TokenPair> refreshToken(String refreshToken, String ipAddress, String deviceInfo);

	Mono<Void> logout(String token);

	/**
	 * 登录结果
	 */
	public record LoginResult(User user, String accessToken, String refreshToken, Long expiresIn) {
	}

	/**
	 * 认证异常
	 */
	public static class AuthenticationException extends RuntimeException {

		public AuthenticationException(String message) {
			super(message);
		}

	}

}
