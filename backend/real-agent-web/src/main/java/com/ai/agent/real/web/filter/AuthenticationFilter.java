package com.ai.agent.real.web.filter;

import com.ai.agent.real.application.service.auth.TokenService;
import com.ai.agent.real.common.utils.CommonUtils;
import com.ai.agent.real.contract.model.auth.UserContextHolder;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

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

        // 既没有 token，又是私有api端口
        if (token == null && !CommonUtils.isPublicPath(path)) {
            return makeUnauthorizedResponse(exchange, "Token missing");
        }

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

    /**
     * 构建 401 响应
     */
    private Mono<Void> makeUnauthorizedResponse(ServerWebExchange exchange, String msg) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        String errorMessage = String.format("{\"code\": 401, \"message\": \"Unauthorized: %s\"}", msg);
        byte[] bytes = errorMessage.getBytes(StandardCharsets.UTF_8);
        DataBuffer buffer = response.bufferFactory().wrap(bytes);

        return response.writeWith(Mono.just(buffer));
    }

}
