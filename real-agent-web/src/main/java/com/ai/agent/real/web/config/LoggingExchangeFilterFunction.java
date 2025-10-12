package com.ai.agent.real.web.config;

import lombok.extern.slf4j.*;
import org.reactivestreams.*;
import org.springframework.core.io.buffer.*;
import org.springframework.http.*;
import org.springframework.http.client.reactive.*;
import org.springframework.stereotype.*;
import org.springframework.web.reactive.function.client.*;
import reactor.core.publisher.*;

import java.nio.charset.*;

/**
 * WebClient请求体日志拦截器 通过装饰ClientHttpRequest来捕获实际发送的请求体内容
 *
 * @author han
 * @time 2025/10/6 5:10
 */
@Slf4j
@Component
public class LoggingExchangeFilterFunction implements ExchangeFilterFunction {

	@Override
	public Mono<ClientResponse> filter(ClientRequest request, ExchangeFunction next) {
		// 记录请求基本信息
		logRequestBasicInfo(request);

		// 装饰request来捕获body
		ClientRequest decoratedRequest = ClientRequest.from(request).body((outputMessage, context) -> {
			ClientHttpRequest decorated = new LoggingClientHttpRequestDecorator(outputMessage, request);
			return request.body().insert(decorated, context);
		}).build();

		// 处理响应
		return next.exchange(decoratedRequest).flatMap(response -> {
			if (response.statusCode().isError()) {
				return logErrorResponse(request, response);
			}
			else {
				log.info("[WebClient Response] {} {} -> 状态码: {}", request.method(), request.url(),
						response.statusCode());
				return Mono.just(response);
			}
		});
	}

	/**
	 * 记录请求基本信息
	 */
	private void logRequestBasicInfo(ClientRequest request) {
		log.info("========== WebClient 请求开始 ==========");
		log.info("[请求方法] {}", request.method());
		log.info("[请求URL] {}", request.url());

		// 记录请求头（脱敏处理）
		log.info("[请求头]");
		request.headers().forEach((name, values) -> {
			if (name.equalsIgnoreCase("Authorization")) {
				log.info("  {}: {}", name, maskAuthorizationHeader(values));
			}
			else {
				log.info("  {}: {}", name, values);
			}
		});
	}

	/**
	 * 记录错误响应详情
	 */
	private Mono<ClientResponse> logErrorResponse(ClientRequest request, ClientResponse response) {
		log.error("========== WebClient 请求失败 ==========");
		log.error("[请求方法] {}", request.method());
		log.error("[请求URL] {}", request.url());
		log.error("[响应状态码] {}", response.statusCode());
		log.error("[响应状态描述] {}", response.statusCode().value() + " "
				+ HttpStatus.valueOf(response.statusCode().value()).getReasonPhrase());

		// 记录响应头
		log.error("[响应头]");
		response.headers().asHttpHeaders().forEach((name, values) -> {
			log.error("  {}: {}", name, values);
		});

		// 读取并记录错误响应体
		return response.bodyToMono(String.class).defaultIfEmpty("").flatMap(errorBody -> {
			log.error("[错误响应体] {}", errorBody);
			log.error("========================================");

			// 重新构造响应，因为body已经被读取了
			return Mono.just(ClientResponse.from(response).body(errorBody).build());
		}).onErrorResume(e -> {
			log.error("[读取错误响应体失败] {}", e.getMessage());
			log.error("========================================");
			return Mono.just(response);
		});
	}

	/**
	 * 脱敏Authorization头
	 */
	private String maskAuthorizationHeader(java.util.List<String> values) {
		if (values == null || values.isEmpty()) {
			return "[]";
		}

		StringBuilder result = new StringBuilder("[");
		for (int i = 0; i < values.size(); i++) {
			String value = values.get(i);
			if (value.startsWith("Bearer ")) {
				String token = value.substring(7);
				if (token.length() > 10) {
					result.append("Bearer ")
						.append(token, 0, 10)
						.append("****")
						.append(token.substring(token.length() - 4));
				}
				else {
					result.append("Bearer ****");
				}
			}
			else {
				result.append(value);
			}
			if (i < values.size() - 1) {
				result.append(", ");
			}
		}
		result.append("]");
		return result.toString();
	}

	/**
	 * 装饰器：捕获并记录请求体
	 */
	private static class LoggingClientHttpRequestDecorator extends ClientHttpRequestDecorator {

		private final ClientRequest clientRequest;

		public LoggingClientHttpRequestDecorator(ClientHttpRequest delegate, ClientRequest clientRequest) {
			super(delegate);
			this.clientRequest = clientRequest;
		}

		@Override
		public Mono<Void> writeWith(Publisher<? extends DataBuffer> body) {
			Flux<DataBuffer> buffer = Flux.from(body);

			return DataBufferUtils.join(buffer).flatMap(dataBuffer -> {
				byte[] bytes = new byte[dataBuffer.readableByteCount()];
				dataBuffer.read(bytes);
				DataBufferUtils.release(dataBuffer);

				String bodyContent = new String(bytes, StandardCharsets.UTF_8);

				// 记录请求体
				log.info("[请求体] {}", bodyContent);
				log.info("========================================");

				// 重新创建DataBuffer并发送
				DataBuffer newBuffer = getDelegate().bufferFactory().wrap(bytes);
				return super.writeWith(Mono.just(newBuffer));
			});
		}

	}

}
