package com.ai.agent.real.web.controller.agent;

import com.ai.agent.real.common.utils.CommonUtils;
import com.ai.agent.real.contract.agent.IAgentStrategy;
import com.ai.agent.real.entity.agent.context.ReActAgentContext;
import com.ai.agent.real.contract.agent.service.IAgentSessionManagerService;
import com.ai.agent.real.contract.model.callback.ToolApprovalCallback;
import com.ai.agent.real.contract.model.logging.*;
import com.ai.agent.real.contract.model.message.*;
import com.ai.agent.real.contract.model.protocol.*;
import lombok.Data;
import lombok.extern.slf4j.*;
import org.springframework.beans.factory.annotation.Qualifier;
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

	private final IAgentSessionManagerService agentSessionManagerService;

	public ReActAgentController(IAgentSessionManagerService agentSessionManagerService,
			@Qualifier("reActAgentStrategy") IAgentStrategy reActAgentStrategy) {
		this.agentSessionManagerService = agentSessionManagerService.of(reActAgentStrategy);
	}

	/**
	 * 执行ReAct任务（流式响应） 使用AgentSessionHub管理会话，支持工具审批中断和恢复
	 */
	@PostMapping(value = "/react/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
	public Flux<ServerSentEvent<AgentExecutionEvent>> executeReActStream(@RequestBody ChatRequest request) {
		log.info("收到ReAct流式执行请求: sessionId={}, message={}", request.getSessionId(), request.getMessage());

		// 验证sessionId
		if (request.getSessionId() == null || request.getSessionId().isBlank()) {
			request.setSessionId("session-" + System.currentTimeMillis());
			log.info("未提供sessionId，自动生成: {}", request.getSessionId());
		}

		// 创建执行上下文
		ReActAgentContext context = new ReActAgentContext(
                new TraceInfo()
                        .setSessionId(request.getSessionId())
                        .setTurnId(CommonUtils.getTraceId(CommonUtils.getTraceId("ReAct")))
                        .setStartTime(LocalDateTime.now())
        );

		// 设置任务到上下文（用于恢复时使用）
		context.setTask(request.getMessage());

		// 创建工具审批回调
		ToolApprovalCallback approvalCallback = agentSessionManagerService::pauseForToolApproval;

		// 设置回调到上下文
		context.setToolApprovalCallback(approvalCallback);

		// 通过AgentSessionHub订阅会话
		return agentSessionManagerService.subscribe(request.getSessionId(), request.getMessage(), context)
			.doOnError(error -> log.error("ReAct执行异常: sessionId={}", request.getSessionId(), error))
			.doOnComplete(() -> {
				context.setEndTime(LocalDateTime.now());
				log.info("ReAct任务执行完成: sessionId={}", request.getSessionId());
			});

	}

	/**
	 * 处理用户交互响应（通用接口） 支持所有类型的交互响应：工具审批、缺少信息、用户确认等
	 */
	// @PostMapping("/react/interaction_response")
	// public ChatResponse handleInteractionResponse(@RequestBody InteractionResponse
	// response) {
	// log.info("收到用户交互响应: sessionId={}, requestId={}, option={}",
	// response.getSessionId(), response.getRequestId(),
	// response.getSelectedOptionId());
	//
	// try {
	// // 验证参数
	// if (response.getSessionId() == null || response.getSessionId().isBlank()) {
	// return ChatResponse.builder()
	// .success(false)
	// .message("sessionId不能为空")
	// .timestamp(LocalDateTime.now())
	// .build();
	// }
	//
	// if (response.getRequestId() == null || response.getRequestId().isBlank()) {
	// return ChatResponse.builder()
	// .success(false)
	// .message("requestId不能为空")
	// .timestamp(LocalDateTime.now())
	// .build();
	// }
	//
	// if (response.getSelectedOptionId() == null ||
	// response.getSelectedOptionId().isBlank()) {
	// return ChatResponse.builder()
	// .success(false)
	// .message("selectedOptionId不能为空")
	// .timestamp(LocalDateTime.now())
	// .build();
	// }
	//
	// // 调用AgentSessionHub处理响应
	// agentSessionHub.handleInteractionResponse(response.getSessionId(), response);
	//
	// return ChatResponse.builder()
	// .success(true)
	// .message("用户响应已处理，正在恢复执行")
	// .sessionId(response.getSessionId())
	// .timestamp(LocalDateTime.now())
	// .build();
	//
	// }
	// catch (Exception e) {
	// log.error("处理用户交互响应异常", e);
	// return ChatResponse.builder()
	// .success(false)
	// .message("处理用户响应失败: " + e.getMessage())
	// .sessionId(response.getSessionId())
	// .timestamp(LocalDateTime.now())
	// .build();
	// }
	// }

	/**
	 * 聊天请求DTO
	 */
	@Data
	public static class ChatRequest {

		private String message;

		private String userId;

		private String sessionId;

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
