package com.ai.agent.real.web.controller.auth;

import com.ai.agent.real.application.service.auth.AuthService;
import com.ai.agent.real.entity.auth.UserContextHolder;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * 认证控制器
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

	private final AuthService authService;

	public AuthController(AuthService authService) {
		this.authService = authService;
	}

	/**
	 * 用户登录
	 */
	@PostMapping("/login")
	public Mono<Map<String, Object>> login(@RequestBody LoginRequest request) {
		return authService.login(request.externalId(), request.password())
			.map(result -> Map.of("success", true, "data",
					Map.of("accessToken", result.accessToken(), "refreshToken", result.refreshToken(), "expiresIn",
							result.expiresIn(), "user",
							Map.of("userId", result.user().getId(), "externalId", result.user().getExternalId(),
									"nickname", result.user().getNickname(), "avatarUrl",
									result.user().getAvatarUrl() != null ? result.user().getAvatarUrl() : ""))));
	}

	/**
	 * 用户注册
	 */
	@PostMapping("/register")
	public Mono<Map<String, Object>> register(@RequestBody RegisterRequest request) {
		return authService.register(request.externalId(), request.password(), request.nickname(), request.avatarUrl())
			.map(user -> Map.of("success", true, "data",
					Map.of("userId", user.getId(), "externalId", user.getExternalId())));
	}

	/**
	 * 刷新 Token
	 */
	@PostMapping("/refresh")
	public Mono<Map<String, Object>> refresh(@RequestBody RefreshRequest request) {
		return authService.refreshToken(request.refreshToken())
			.map(tokenPair -> Map.of("success", true, "data",
					Map.of("accessToken", tokenPair.accessToken(), "expiresIn", tokenPair.expiresIn())));
	}

	/**
	 * 登出
	 */
	@PostMapping("/logout")
	public Mono<Map<String, Object>> logout(ServerWebExchange exchange) {
		String token = extractToken(exchange);
		if (token == null) {
			return Mono.just(Map.of("success", false, "message", "未找到 Token"));
		}

		return authService.logout(token).thenReturn(Map.of("success", true, "message", "登出成功"));
	}

	/**
	 * 获取当前用户信息
	 */
	@GetMapping("/me")
	public Mono<Map<String, Object>> getCurrentUser() {
		return UserContextHolder.getUser()
			.map(user -> Map.of("success", true, "data",
					Map.of("userId", user.getUserId(), "externalId", user.getExternalId(), "nickname",
							user.getNickname(), "avatarUrl", user.getAvatarUrl() != null ? user.getAvatarUrl() : "")));
	}

	/**
	 * 提取 Token
	 */
	private String extractToken(ServerWebExchange exchange) {
		String bearerToken = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
		if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
			return bearerToken.substring(7);
		}
		return null;
	}

	/**
	 * 登录请求
	 */
	public record LoginRequest(String externalId, String password) {
	}

	/**
	 * 注册请求
	 */
	public record RegisterRequest(String externalId, String password, String nickname, String avatarUrl) {
	}

	/**
	 * 刷新请求
	 */
	public record RefreshRequest(String refreshToken) {
	}

}
