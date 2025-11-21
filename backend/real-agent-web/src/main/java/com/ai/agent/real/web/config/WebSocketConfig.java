package com.ai.agent.real.web.config;

import com.ai.agent.real.web.ws.VoiceStreamWebSocketHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.handler.SimpleUrlHandlerMapping;
import org.springframework.web.reactive.socket.server.support.WebSocketHandlerAdapter;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class WebSocketConfig {

	private final VoiceStreamWebSocketHandler voiceStreamWebSocketHandler;

	public WebSocketConfig(VoiceStreamWebSocketHandler voiceStreamWebSocketHandler) {
		this.voiceStreamWebSocketHandler = voiceStreamWebSocketHandler;
	}

	@Bean
	public SimpleUrlHandlerMapping webSocketMapping() {
		Map<String, Object> map = new HashMap<>();
		map.put("/ws/voice/stream", voiceStreamWebSocketHandler);
		SimpleUrlHandlerMapping mapping = new SimpleUrlHandlerMapping();
		mapping.setOrder(1); // higher priority than annotated controllers
		mapping.setUrlMap(map);
		return mapping;
	}

	@Bean
	public WebSocketHandlerAdapter handlerAdapter() {
		return new WebSocketHandlerAdapter();
	}

}
