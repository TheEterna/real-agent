package com.ai.agent.real.web.config;

import com.ai.agent.real.application.service.auth.AuthService;
import com.ai.agent.real.application.service.auth.TokenService;
import com.ai.agent.real.domain.repository.user.UserRepository;
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

	@Bean
	public AuthService authService(UserRepository userRepository, TokenService tokenService) {
		return new AuthService(userRepository, tokenService);
	}

	/**
	 * 提供RestClient.Builder bean，用于支持需要该依赖的组件（如阿里云DashScope）
	 * @return RestClient.Builder实例
	 */
	@Bean
	public RestClient.Builder restClientBuilder() {
		return RestClient.builder();
	}

}