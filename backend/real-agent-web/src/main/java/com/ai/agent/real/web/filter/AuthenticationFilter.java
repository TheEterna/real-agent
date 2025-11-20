package com.ai.agent.real.web.filter;

import com.ai.agent.real.application.service.auth.TokenService;
import com.ai.agent.real.contract.model.auth.UserContextHolder;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

/**
 * 认证过滤器
 */
@Component
public class AuthenticationFilter implements WebFilter {

	private final TokenService tokenService;

	public AuthenticationFilter(TokenService tokenService) {
		this.tokenService = tokenService;
	}

	@Override
	public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
		String path = exchange.getRequest().getPath().value();

		// 公开接口，跳过认证
		if (isPublicPath(path)) {
			return chain.filter(exchange);
		}

		// 提取 Token
		String token = extractToken(exchange.getRequest());

		if (token == null) {
			// Token 不存在，允许匿名访问
			return chain.filter(exchange);
		}

		// 验证 Token 并获取用户信息
		return tokenService.validateToken(token).flatMap(user -> {
			// 将用户信息注入到 Reactor Context
			return chain.filter(exchange).contextWrite(ctx -> UserContextHolder.setUser(ctx, user));
		})
			.switchIfEmpty(
					// Token 无效，允许匿名访问
					chain.filter(exchange));
	}

	/**
	 * 提取 Token
	 */
	private String extractToken(ServerHttpRequest request) {
		String bearerToken = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
		if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
			return bearerToken.substring(7);
		}
		return null;
	}

	/**
	 * 判断是否是公开路径
	 */
	private boolean isPublicPath(String path) {
		return path.startsWith("/api/auth/") || path.startsWith("/api/public/") || path.equals("/")
				|| path.startsWith("/actuator/");
	}

}
