package com.ai.agent.real.application.service.auth;

import com.ai.agent.real.contract.service.IAuthService;
import com.ai.agent.real.contract.service.ITokenService;
import com.ai.agent.real.domain.entity.user.User;
import com.ai.agent.real.domain.repository.user.UserRepository;
import com.ai.agent.real.contract.model.auth.PasswordUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.OffsetDateTime;

/**
 * 认证服务
 */
@Slf4j
@Service
public class AuthService implements IAuthService {

	private final UserRepository userRepository;

	private final TokenService tokenService;

	public AuthService(UserRepository userRepository, TokenService tokenService) {
		this.userRepository = userRepository;
		this.tokenService = tokenService;
	}

	/**
	 * 用户登录（兼容旧版本）
	 */
	@Override
	public Mono<LoginResult> login(String externalId, String password) {
		return login(externalId, password, null, null);
	}

	/**
	 * 用户登录（带设备信息）
	 */
	@Override
	public Mono<LoginResult> login(String externalId, String password, String ipAddress, String deviceInfo) {
		return userRepository.findByExternalId(externalId).flatMap(user -> {
			// 验证密码
			if (!PasswordUtil.matches(password, user.getPasswordHash())) {
				return Mono.error(new AuthenticationException("用户名或密码错误"));
			}

			// 检查用户状态
			if (user.getStatus() == null || user.getStatus() != 1) {
				return Mono.error(new AuthenticationException("用户已被禁用"));
			}

			// 生成 Token（传递设备信息）
			return tokenService.generateTokenPair(user, ipAddress, deviceInfo)
				.map(tokenPair -> new LoginResult(user, tokenPair.accessToken(), tokenPair.refreshToken(),
						tokenPair.expiresIn()));
		}).switchIfEmpty(Mono.error(new AuthenticationException("用户名或密码错误")));
	}

	/**
	 * 用户注册
	 */
	@Override
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
				.createdTime(OffsetDateTime.now())
				.updatedTime(OffsetDateTime.now())
				.build();

			// 3. 保存用户
			return userRepository.save(user);
		});
	}

	/**
	 * 刷新 Token（兼容旧版本）
	 */
	@Override
	public Mono<ITokenService.TokenPair> refreshToken(String refreshToken) {
		return refreshToken(refreshToken, null, null);
	}

	/**
	 * 刷新 Token（带设备信息验证）
	 */
	@Override
	public Mono<ITokenService.TokenPair> refreshToken(String refreshToken, String ipAddress, String deviceInfo) {
		return tokenService.refreshAccessToken(refreshToken, ipAddress, deviceInfo);
	}

	/**
	 * 登出
	 */
	@Override
	public Mono<Void> logout(String token) {
		return tokenService.logout(token);
	}

}
