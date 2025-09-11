package com.ai.agent.kit.web.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

/**
 * CORS跨域配置
 * 专门为SSE连接优化的跨域设置
 * 
 * @author han
 * @time 2025/9/11 01:15
 */
@Configuration
public class CorsConfig {

    @Bean
    public CorsWebFilter corsWebFilter() {
        CorsConfiguration corsConfig = new CorsConfiguration();
        
        // 允许所有来源
        corsConfig.addAllowedOriginPattern("*");
        
        // 允许所有HTTP方法
        corsConfig.addAllowedMethod("*");
        
        // 允许所有请求头
        corsConfig.addAllowedHeader("*");
        
        // 允许发送Cookie
        corsConfig.setAllowCredentials(true);
        
        // SSE相关的特殊设置
        corsConfig.addExposedHeader("Cache-Control");
        corsConfig.addExposedHeader("Connection");
        corsConfig.addExposedHeader("Content-Type");
        
        // 预检请求的有效期
        corsConfig.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", corsConfig);
        
        return new CorsWebFilter(source);
    }
}
