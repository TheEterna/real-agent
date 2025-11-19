package com.ai.agent.real.application.service.auth;

import com.ai.agent.real.domain.entity.user.User;
import com.ai.agent.real.domain.repository.user.UserRepository;
import com.ai.agent.real.entity.auth.PasswordUtil;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

/**
 * 认证服务
 */
public class AuthService {

	private final UserRepository userRepository;

	private final TokenService tokenService;

	public AuthService(UserRepository userRepository, TokenService tokenService) {
		this.userRepository = userRepository;
		this.tokenService = tokenService;
	}

	/**
	 * 用户登录
	 */
	public Mono<LoginResult> login(String externalId, String password) {
		return userRepository.findByExternalId(externalId).flatMap(user -> {
			// 验证密码
			if (!PasswordUtil.matches(password, user.getPasswordHash())) {
				return Mono.error(new AuthenticationException("用户名或密码错误"));
			}

			// 检查用户状态
			if (user.getStatus() == null || user.getStatus() != 1) {
				return Mono.error(new AuthenticationException("用户已被禁用"));
			}

			// 生成 Token
			return tokenService.generateTokenPair(user)
				.map(tokenPair -> new LoginResult(user, tokenPair.accessToken(), tokenPair.refreshToken(),
						tokenPair.expiresIn()));
		}).switchIfEmpty(Mono.error(new AuthenticationException("用户名或密码错误")));
	}

	/**
	 * 用户注册
	 */
	public Mono<User> register(String externalId, String password, String nickname, String avatarUrl) {
		// 1. 检查用户是否已存在
		return userRepository.existsByExternalId(externalId).flatMap(exists -> {
			if (Boolean.TRUE.equals(exists)) {
				return Mono.error(new AuthenticationException("用户已存在"));
			}

			// 2. 创建用户
			User user = User.builder()
				.externalId(externalId)
				.passwordHash(PasswordUtil.encode(password))
				.nickname(nickname)
				.avatarUrl(avatarUrl)
				.status(1)
				.createdAt(LocalDateTime.now())
				.updatedAt(LocalDateTime.now())
				.build();

			// 3. 保存用户
			return userRepository.save(user);
		});
	}

	/**
	 * 刷新 Token
	 */
	public Mono<TokenService.TokenPair> refreshToken(String refreshToken) {
		return tokenService.refreshAccessToken(refreshToken);
	}

	/**
	 * 登出
	 */
	public Mono<Void> logout(String token) {
		return tokenService.logout(token);
	}

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
