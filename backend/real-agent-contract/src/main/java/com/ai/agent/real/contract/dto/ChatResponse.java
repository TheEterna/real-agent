package com.ai.agent.real.contract.dto;

import com.ai.agent.real.contract.model.message.AgentMessage;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 聊天响应DTO
 *
 * @author han
 * @time 2025/11/12 21:59
 */
@Data
public class ChatResponse {

	private boolean success;

	private String message;

	private String agentId;

	private String sessionId;

	private LocalDateTime timestamp;

	private List<AgentMessage> conversationHistory;

	public static ChatResponseBuilder builder() {
		return new ChatResponseBuilder();
	}

	public static class ChatResponseBuilder {

		private ChatResponse response = new ChatResponse();

		public ChatResponseBuilder success(boolean success) {
			response.setSuccess(success);
			return this;
		}

		public ChatResponseBuilder message(String message) {
			response.setMessage(message);
			return this;
		}

		public ChatResponseBuilder agentId(String agentId) {
			response.setAgentId(agentId);
			return this;
		}

		public ChatResponseBuilder sessionId(String sessionId) {
			response.setSessionId(sessionId);
			return this;
		}

		public ChatResponseBuilder timestamp(LocalDateTime timestamp) {
			response.setTimestamp(timestamp);
			return this;
		}

		public ChatResponseBuilder conversationHistory(List<AgentMessage> history) {
			response.setConversationHistory(history);
			return this;
		}

		public ChatResponse build() {
			return response;
		}

	}

}
