package com.ai.agent.real.web.controller.auth;

import com.ai.agent.real.application.service.auth.AuthService;
import com.ai.agent.real.contract.model.auth.UserContextHolder;
import com.ai.agent.real.contract.model.protocol.ResponseResult;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.validation.Valid;
import java.util.UUID;

/**
 * 认证控制器
 *
 * @author han
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
	public Mono<ResponseResult<LoginResponse>> login(@Valid @RequestBody LoginRequest request,
			ServerWebExchange exchange) {
		// 提取设备信息
		String ipAddress = extractIpAddress(exchange);
		String deviceInfo = exchange.getRequest().getHeaders().getFirst("User-Agent");

		return authService.login(request.getExternalId(), request.getPassword(), ipAddress, deviceInfo).map(result -> {
			LoginResponse response = LoginResponse.builder()
				.accessToken(result.accessToken())
				.refreshToken(result.refreshToken())
				.expiresIn(result.expiresIn())
				.user(UserInfo.builder()
					.userId(result.user().getId())
					.externalId(result.user().getExternalId())
					.nickname(result.user().getNickname())
					.avatarUrl(result.user().getAvatarUrl())
					.build())
				.build();
			return ResponseResult.success("登录成功", response);
		}).onErrorResume(e -> {
			return Mono.just(ResponseResult.error("登录失败: " + e.getMessage()));
		});
	}

	/**
	 * 用户注册
	 */
	@PostMapping("/register")
	public Mono<ResponseResult<RegisterResponse>> register(@Valid @RequestBody RegisterRequest request) {
		return authService
			.register(request.getExternalId(), request.getPassword(), request.getNickname(), request.getAvatarUrl())
			.map(user -> {
				RegisterResponse response = RegisterResponse.builder()
					.userId(user.getId())
					.externalId(user.getExternalId())
					.build();
				return ResponseResult.success("注册成功", response);
			})
			.onErrorResume(e -> {
				return Mono.just(ResponseResult.error("注册失败: " + e.getMessage()));
			});
	}

	/**
	 * 刷新 Token
	 */
	@PostMapping("/refresh")
	public Mono<ResponseResult<RefreshResponse>> refresh(@Valid @RequestBody RefreshRequest request,
			ServerWebExchange exchange) {
		// 提取设备信息
		String ipAddress = extractIpAddress(exchange);
		String deviceInfo = exchange.getRequest().getHeaders().getFirst("User-Agent");

		return authService.refreshToken(request.getRefreshToken(), ipAddress, deviceInfo).map(tokenPair -> {
			RefreshResponse response = RefreshResponse.builder()
				.accessToken(tokenPair.accessToken())
				.refreshToken(tokenPair.refreshToken())
				.expiresIn(tokenPair.expiresIn())
				.build();
			return ResponseResult.success("刷新成功", response);
		}).onErrorResume(e -> {
			// 如果是设备不匹配的安全异常，返回401
			if (e instanceof SecurityException) {
				return Mono.just(ResponseResult.error(ResponseResult.UNAUTHORIZED_CODE, "设备验证失败: " + e.getMessage()));
			}
			return Mono.just(ResponseResult.error("刷新失败: " + e.getMessage()));
		});
	}

	/**
	 * 登出
	 */
	@PostMapping("/logout")
	public Mono<ResponseResult<Object>> logout(ServerWebExchange exchange) {
		String token = extractToken(exchange);
		if (token == null) {
			return Mono.just(ResponseResult.paramError("未找到 Token"));
		}

		return authService.logout(token).then(Mono.just(ResponseResult.success("登出成功", null))).onErrorResume(e -> {
			return Mono.just(ResponseResult.error("登出失败: " + e.getMessage()));
		});
	}

	/**
	 * 获取当前用户信息
	 */
	@GetMapping("/me")
	public Mono<ResponseResult<CurrentUserResponse>> getCurrentUser() {
		return UserContextHolder.getUserId().map(userId -> {
			CurrentUserResponse response = CurrentUserResponse.builder().userId(userId).build();
			return ResponseResult.success(response);
		}).switchIfEmpty(Mono.just(ResponseResult.unauthorized()));
	}

	// ==================== 辅助方法 ====================

	/**
	 * 提取IP地址（优先从代理头中获取）
	 */
	private String extractIpAddress(ServerWebExchange exchange) {
		// 优先获取代理后的真实IP (X-Forwarded-For)
		String ip = exchange.getRequest().getHeaders().getFirst("X-Forwarded-For");
		if (ip == null || ip.isEmpty()) {
			// 如果没有代理，直接获取远程地址
			var remoteAddress = exchange.getRequest().getRemoteAddress();
			if (remoteAddress != null) {
				ip = remoteAddress.getAddress().getHostAddress();
			}
			else {
				ip = "unknown";
			}
		}
		else {
			// X-Forwarded-For 可能包含多个IP，取第一个
			ip = ip.split(",")[0].trim();
		}
		return ip;
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

	// ==================== 请求/响应 DTO ====================

	/**
	 * 登录请求
	 */
	@Data
	public static class LoginRequest {

		@NotBlank(message = "用户ID不能为空")
		private String externalId;

		@NotBlank(message = "密码不能为空")
		private String password;

	}

	/**
	 * 登录响应
	 */
	@Data
	@lombok.Builder
	public static class LoginResponse {

		private String accessToken;

		private String refreshToken;

		private Long expiresIn;

		private UserInfo user;

	}

	/**
	 * 用户信息
	 */
	@Data
	@lombok.Builder
	public static class UserInfo {

		private UUID userId;

		private String externalId;

		private String nickname;

		private String avatarUrl;

	}

	/**
	 * 注册请求
	 */
	@Data
	public static class RegisterRequest {

		@NotBlank(message = "用户ID不能为空")
		private String externalId;

		@NotBlank(message = "密码不能为空")
		private String password;

		private String nickname;

		private String avatarUrl;

	}

	/**
	 * 注册响应
	 */
	@Data
	@lombok.Builder
	public static class RegisterResponse {

		private UUID userId;

		private String externalId;

	}

	/**
	 * 刷新Token请求
	 */
	@Data
	public static class RefreshRequest {

		@NotBlank(message = "刷新令牌不能为空")
		private String refreshToken;

	}

	/**
	 * 刷新Token响应
	 */
	@Data
	@lombok.Builder
	public static class RefreshResponse {

		private String accessToken;

		private String refreshToken;

		private Long expiresIn;

	}

	/**
	 * 当前用户响应
	 */
	@Data
	@lombok.Builder
	public static class CurrentUserResponse {

		private UUID userId;

	}

}
