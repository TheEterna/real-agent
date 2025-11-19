package com.ai.agent.real.web.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Jackson配置类 统一配置JSON序列化和反序列化规则
 *
 * @author han
 * @time 2025/9/11 01:25
 */
@Configuration
public class JacksonConfig {

	/**
	 * 配置全局ObjectMapper 支持Java 8时间类型序列化
	 */
	@Bean
	@Primary
	public ObjectMapper objectMapper() {
		ObjectMapper mapper = new ObjectMapper();

		// 注册Java 8时间模块
		mapper.registerModule(new JavaTimeModule());

		// 禁用时间戳格式，使用ISO-8601格式
		mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

		// 忽略未知属性，提高兼容性
		mapper.configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

		// 允许空对象序列化
		mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);

		return mapper;
	}

}
