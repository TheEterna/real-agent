package com.ai.agent.real.web.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Configuration properties for Omni Realtime SDK integration.
 *
 * @author han
 * @time 2025/9/25 15:19
 */
@Component
public class OmniProperties {

	@Value("${ai.omni.enabled:false}")
	private boolean enabled;

	@Value("${ai.omni.model:qwen3-omni-flash-realtime}")
	private String model;

	// Default to provider default voice; user can override in yml
	@Value("${ai.omni.voice:Cherry}")
	private String voice;

	// Read DashScope API Key from Spring config if provided; otherwise can fall back to
	// env DASHSCOPE_API_KEY
	@Value("${spring.ai.dashscope.api-key:}")
	private String dashscopeApiKey;

	public boolean isEnabled() {
		return enabled;
	}

	public String getModel() {
		return model;
	}

	public String getVoice() {
		return voice;
	}

	public String getDashscopeApiKey() {
		return dashscopeApiKey;
	}

}
