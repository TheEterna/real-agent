package com.ai.agent.real.web.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Voice pipeline related properties.
 *
 * Prefix: ai.voice
 *
 * Example application.yaml ai: voice: llm-model: qwen-max asr-model: paraformer-v2
 * tts-model: cosyvoice role-voices: conan: longxia detective: longxia
 */
@Component
@ConfigurationProperties(prefix = "ai.voice")
public class VoiceProperties {

	/** LLM model name, e.g. qwen-max */
	private String llmModel = "qwen-max";

	/** ASR model name, e.g. paraformer-v2 */
	private String asrModel = "paraformer-v2";

	/** TTS model name, e.g. cosyvoice */
	private String ttsModel = "cosyvoice";

	/** Default TTS voice if role-specific not found. */
	private String defaultVoice = "longxia";

	/** Mapping roleId -> tts voice name */
	private Map<String, String> roleVoices = new HashMap<>();

	public String getLlmModel() {
		return llmModel;
	}

	public void setLlmModel(String llmModel) {
		this.llmModel = llmModel;
	}

	public String getAsrModel() {
		return asrModel;
	}

	public void setAsrModel(String asrModel) {
		this.asrModel = asrModel;
	}

	public String getTtsModel() {
		return ttsModel;
	}

	public void setTtsModel(String ttsModel) {
		this.ttsModel = ttsModel;
	}

	public String getDefaultVoice() {
		return defaultVoice;
	}

	public void setDefaultVoice(String defaultVoice) {
		this.defaultVoice = defaultVoice;
	}

	public Map<String, String> getRoleVoices() {
		return roleVoices;
	}

	public void setRoleVoices(Map<String, String> roleVoices) {
		this.roleVoices = roleVoices;
	}

	public String resolveVoiceByRole(String roleId) {
		if (roleId == null)
			return defaultVoice;
		return roleVoices.getOrDefault(roleId, defaultVoice);

	}

}
