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

		// 1. 提取 Token
		String token = extractToken(exchange.getRequest());

		// 验证 Token 并获取用户信息
		return tokenService.validateToken(token, path).flatMap(user -> {
			// 将用户信息注入到 Reactor Context
			return chain.filter(exchange).contextWrite(ctx -> UserContextHolder.setUser(ctx, user));
		});
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

}
