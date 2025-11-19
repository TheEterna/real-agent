package com.ai.agent.real.web.config;

import lombok.extern.slf4j.*;
import org.springframework.boot.web.reactive.function.client.*;
import org.springframework.context.annotation.*;

/**
 * @author han
 * @time 2025/9/22 12:10
 */

@Configuration
@Slf4j
public class WebClientLoggingConfig {

	@Bean
	public WebClientCustomizer loggingWebClientCustomizer() {
		return builder -> builder
			// 打印请求起点（方法、URL、headers）
			// .filter(ExchangeFilterFunctions.())
			// 打印响应状态与headers
			.filter((request, next) -> next.exchange(request).doOnNext(response -> {
				// log.info("[WebClient] {} {} {} 响应状态: {}", request.method(),
				// request.url(), request.body(),
				// response.statusCode());
			}));
		// 打印响应Body（明文字符串）；注意：仅在响应体是文本/JSON时有意义
		// .filter((request, next) -> next.exchange(request)
		// .flatMap(response -> response.bodyToMono(String.class)
		// .defaultIfEmpty("")
		// .doOnNext(body -> {
		// log.debug("[WebClient] {} {} 响应体: {}",
		// request.method(), request.url(), body);
		// })
		// .map(body -> ClientResponse.from(response)
		// .body(body)
		// .build())
		// )
		// );
	}

}