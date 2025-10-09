package com.ai.agent.real.web.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/**
 * Web相关配置类
 *
 * @author han
 * @since 2025/9/17
 */
@Configuration
public class WebConfig {

	/**
	 * 提供RestClient.Builder bean，用于支持需要该依赖的组件（如阿里云DashScope）
	 * @return RestClient.Builder实例
	 */
	@Bean
	public RestClient.Builder restClientBuilder() {
		return RestClient.builder();
	}

}