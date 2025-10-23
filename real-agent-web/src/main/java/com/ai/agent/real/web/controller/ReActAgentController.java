package com.ai.agent.real.web.controller;

import com.ai.agent.real.agent.strategy.*;
import com.ai.agent.real.contract.callback.ToolApprovalCallback;
import com.ai.agent.real.contract.model.context.*;
import com.ai.agent.real.contract.model.interaction.InteractionResponse;
import com.ai.agent.real.contract.model.logging.*;
import com.ai.agent.real.contract.model.message.*;
import com.ai.agent.real.contract.model.protocol.*;
import com.ai.agent.real.web.service.AgentSessionHub;
import lombok.Data;
import lombok.extern.slf4j.*;
import org.springframework.http.*;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.*;

import java.time.*;
import java.util.*;

/**
 * Agent对话控制器 提供ReAct框架的Web接口
 *
 * @author han
 * @time 2025/9/10 10:45
 */
@Slf4j
@RestController
@RequestMapping("/api/agent/chat")
@CrossOrigin(origins = "*")
public class ReActAgentController {

	private final AgentSessionHub agentSessionHub;

	public ReActAgentController(AgentSessionHub agentSessionHub) {
		this.agentSessionHub = agentSessionHub;
	}

	/**
	 * 执行ReAct任务（流式响应） 使用AgentSessionHub管理会话，支持工具审批中断和恢复
	 */
	@PostMapping(value = "/react/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
	public Flux<ServerSentEvent<AgentExecutionEvent>> executeReActStream(@RequestBody ChatRequest request) {
		log.info("收到ReAct流式执行请求: sessionId={}, message={}", request.getSessionId(), request.getMessage());

		try {
			// 验证sessionId
			if (request.getSessionId() == null || request.getSessionId().isBlank()) {
				request.setSessionId("session-" + System.currentTimeMillis());
				log.info("未提供sessionId，自动生成: {}", request.getSessionId());
			}

			// 创建执行上下文
			AgentContext context = new AgentContext(new TraceInfo()).setSessionId(request.getSessionId())
				.setTurnId(generateTraceId())
				.setStartTime(LocalDateTime.now());

			// 设置任务到上下文（用于恢复时使用）
			context.setTask(request.getMessage());

			// 创建工具审批回调
			var approvalCallback = (com.ai.agent.real.contract.callback.ToolApprovalCallback) (sessionId, toolCallId,
					toolName, toolArgs,
					ctx) -> agentSessionHub.pauseForToolApproval(sessionId, toolCallId, toolName, toolArgs, ctx);

			// 设置回调到上下文
			context.setToolApprovalCallback(approvalCallback);

			// 通过AgentSessionHub订阅会话
			return agentSessionHub.subscribe(request.getSessionId(), request.getMessage(), context)
				.doOnError(error -> log.error("ReAct执行异常: sessionId={}", request.getSessionId(), error))
				.doOnComplete(() -> {
					context.setEndTime(LocalDateTime.now());
					log.info("ReAct任务执行完成: sessionId={}", request.getSessionId());
				});

		}
		catch (Exception e) {
			log.error("ReAct执行异常", e);
			return Flux.just(ServerSentEvent.<AgentExecutionEvent>builder()
				.event("ERROR")
				.data(AgentExecutionEvent.error(e))
				.build());
		}
	}

	/**
	 * 处理用户交互响应（通用接口） 支持所有类型的交互响应：工具审批、缺少信息、用户确认等
	 */
	@PostMapping("/react/interaction_response")
	public ChatResponse handleInteractionResponse(@RequestBody InteractionResponse response) {
		log.info("收到用户交互响应: sessionId={}, requestId={}, option={}", response.getSessionId(), response.getRequestId(),
				response.getSelectedOptionId());

		try {
			// 验证参数
			if (response.getSessionId() == null || response.getSessionId().isBlank()) {
				return ChatResponse.builder()
					.success(false)
					.message("sessionId不能为空")
					.timestamp(LocalDateTime.now())
					.build();
			}

			if (response.getRequestId() == null || response.getRequestId().isBlank()) {
				return ChatResponse.builder()
					.success(false)
					.message("requestId不能为空")
					.timestamp(LocalDateTime.now())
					.build();
			}

			if (response.getSelectedOptionId() == null || response.getSelectedOptionId().isBlank()) {
				return ChatResponse.builder()
					.success(false)
					.message("selectedOptionId不能为空")
					.timestamp(LocalDateTime.now())
					.build();
			}

			// 调用AgentSessionHub处理响应
			agentSessionHub.handleInteractionResponse(response.getSessionId(), response);

			return ChatResponse.builder()
				.success(true)
				.message("用户响应已处理，正在恢复执行")
				.sessionId(response.getSessionId())
				.timestamp(LocalDateTime.now())
				.build();

		}
		catch (Exception e) {
			log.error("处理用户交互响应异常", e);
			return ChatResponse.builder()
				.success(false)
				.message("处理用户响应失败: " + e.getMessage())
				.sessionId(response.getSessionId())
				.timestamp(LocalDateTime.now())
				.build();
		}
	}

	/**
	 * 获取对话历史
	 */
	@GetMapping("/history/{sessionId}")
	public ChatResponse getChatHistory(@PathVariable String sessionId) {
		// TODO: 实现对话历史存储和查询
		return ChatResponse.builder()
			.success(true)
			.message("对话历史查询功能待实现")
			.sessionId(sessionId)
			.timestamp(LocalDateTime.now())
			.build();
	}

	/**
	 * 清空对话历史
	 */
	@DeleteMapping("/history/{sessionId}")
	public ChatResponse clearChatHistory(@PathVariable String sessionId) {
		// TODO: 实现对话历史清空
		return ChatResponse.builder()
			.success(true)
			.message("对话历史已清空")
			.sessionId(sessionId)
			.timestamp(LocalDateTime.now())
			.build();
	}

	/**
	 * 获取可用的Agent类型
	 */
	@GetMapping("/types")
	public Map<String, Object> getAgentTypes() {
		return Map.of("types", new String[] { "ReActAgentStrategy", "代码编写" }, "default", "ReActAgentStrategy",
				"description", Map.of("ReActAgentStrategy", "基于推理-行动-观察的智能Agent框架", "代码编写", "专门用于代码生成和编程任务的Agent"));
	}

	/**
	 * 生成追踪ID
	 */
	private String generateTraceId() {
		return "trace-" + System.currentTimeMillis() + "-" + Integer.toHexString((int) (Math.random() * 0x10000));
	}

	/**
	 * 聊天请求DTO
	 */
	@Data
	public static class ChatRequest {

		private String message;

		private String userId;

		private String sessionId;

		private String agentType = "ReActAgentStrategy";

	}

	/**
	 * 聊天响应DTO
	 */
	@Data
	public static class ChatResponse {

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

}
