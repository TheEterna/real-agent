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
		boolean isPublicPath = CommonUtils.isPublicPath(path);

		// 1. 提取 Token
		String token = extractToken(exchange.getRequest());

		// 2. 如果没有 Token
		if (token == null) {
			if (isPublicPath) {
				// 公开路径：无 token 直接放行（匿名访问）
				return chain.filter(exchange);
			}
			else {
				// 私有路径：无 token 返回 401
				return makeUnauthorizedResponse(exchange, "Token missing");
			}
		}

		// 3. 有 Token：尝试验证并获取用户信息
		return tokenService.validateToken(token, path).flatMap(user -> {
			// Token 有效：将用户信息注入到 Reactor Context
			return chain.filter(exchange).contextWrite(ctx -> UserContextHolder.setUser(ctx, user));
		})
			.switchIfEmpty(
					// Token 无效或已过期
					isPublicPath ? chain.filter(exchange) // 公开路径：降级为匿名访问
							: makeUnauthorizedResponse(exchange, "Invalid or expired token") // 私有路径：返回
																								// 401
			);
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
