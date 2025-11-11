package com.ai.agent.real.web.controller.agent;

import com.ai.agent.real.common.utils.CommonUtils;
import com.ai.agent.real.contract.agent.IAgentStrategy;
import com.ai.agent.real.contract.agent.context.AgentContextAble;
import com.ai.agent.real.contract.agent.service.IAgentSessionManagerService;
import com.ai.agent.real.contract.model.callback.ToolApprovalCallback;
import com.ai.agent.real.contract.model.interaction.InteractionResponse;
import com.ai.agent.real.entity.agent.context.ReActAgentContext;
import com.ai.agent.real.contract.model.logging.TraceInfo;
import com.ai.agent.real.contract.model.protocol.AgentExecutionEvent;
import com.ai.agent.real.entity.agent.context.reactplus.ReActPlusAgentContext;
import com.ai.agent.real.entity.agent.context.reactplus.ReActPlusAgentContextMeta;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.time.LocalDateTime;

/**
 * @author: han
 * @time: 2025/10/22 16:46
 */

@Slf4j
@RestController
@RequestMapping("/api/agent/chat")
@CrossOrigin(origins = "*")
public class ReActPlusAgentController {

	private final IAgentSessionManagerService agentSessionManagerService;

	public ReActPlusAgentController(IAgentSessionManagerService agentSessionManagerService,
			@Qualifier("reActPlusAgentStrategy") IAgentStrategy reActPlusAgentStrategy) {

		this.agentSessionManagerService = agentSessionManagerService.of(reActPlusAgentStrategy);
	}

	/**
	 * 执行ReActPlus任务（流式响应） 使用AgentSessionHub管理会话，支持工具审批中断和恢复
	 */
	@PostMapping(value = "/react-plus/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
	public Flux<ServerSentEvent<AgentExecutionEvent>> executeReActStream(
			@RequestBody ReActAgentController.ChatRequest request) {
		log.info("收到ReAct-Plus流式执行请求: sessionId={}, message={}", request.getSessionId(), request.getMessage());

		// 验证sessionId
		if (request.getSessionId() == null || request.getSessionId().isBlank()) {
			request.setSessionId("session-" + System.currentTimeMillis());
			log.info("未提供sessionId，自动生成: {}", request.getSessionId());
		}

		// 创建执行上下文
		AgentContextAble<ReActPlusAgentContextMeta> context = new ReActPlusAgentContext(
				new TraceInfo().setSessionId(request.getSessionId())
					.setTurnId(CommonUtils.getTraceId(CommonUtils.getTraceId("ReActPlus")))
					.setStartTime(LocalDateTime.now()));

		// 设置任务到上下文（用于恢复时使用）
		context.setTask(request.getMessage());

		// 创建工具审批回调
		ToolApprovalCallback approvalCallback = (sessionId, toolCallId, toolName, toolArgs,
				ctx) -> agentSessionManagerService.pauseForToolApproval(sessionId, toolCallId, toolName, toolArgs, ctx);
		// 设置回调到上下文
		context.setToolApprovalCallback(approvalCallback);

		// 通过AgentSessionHub订阅会话
		return agentSessionManagerService.subscribe(request.getSessionId(), request.getMessage(), context)
			.doOnError(error -> log.error("ReAct执行异常: sessionId={}", request.getSessionId(), error))
			.doOnComplete(() -> {
				context.setEndTime(LocalDateTime.now());
				log.info("ReActPlus任务执行完成: sessionId={}", request.getSessionId());
			});

	}

	/**
	 * 处理用户交互响应（通用接口） 支持所有类型的交互响应：工具审批、缺少信息、用户确认等
	 *
	 * 前端调用此接口来响应工具审批请求，触发恢复执行
	 */
	@PostMapping("/react-plus/interaction_response")
	public ReActAgentController.ChatResponse handleInteractionResponse(@RequestBody InteractionResponse response) {
		log.info("收到ReAct-Plus用户交互响应: sessionId={}, requestId={}, option={}", response.getSessionId(),
				response.getRequestId(), response.getSelectedOptionId());

		try {
			// 验证参数
			if (response.getSessionId() == null || response.getSessionId().isBlank()) {
				return ReActAgentController.ChatResponse.builder()
					.success(false)
					.message("sessionId不能为空")
					.timestamp(LocalDateTime.now())
					.build();
			}

			if (response.getRequestId() == null || response.getRequestId().isBlank()) {
				return ReActAgentController.ChatResponse.builder()
					.success(false)
					.message("requestId不能为空")
					.timestamp(LocalDateTime.now())
					.build();
			}

			if (response.getSelectedOptionId() == null || response.getSelectedOptionId().isBlank()) {
				return ReActAgentController.ChatResponse.builder()
					.success(false)
					.message("selectedOptionId不能为空")
					.timestamp(LocalDateTime.now())
					.build();
			}

			// 调用AgentSessionManagerService处理响应
			// 这会触发恢复执行，结果会通过SSE流推送给前端
			agentSessionManagerService.handleInteractionResponse(response.getSessionId(), response);

			return ReActAgentController.ChatResponse.builder()
				.success(true)
				.message("用户响应已处理，正在恢复执行")
				.sessionId(response.getSessionId())
				.timestamp(LocalDateTime.now())
				.build();

		}
		catch (Exception e) {
			log.error("处理ReAct-Plus用户交互响应异常", e);
			return ReActAgentController.ChatResponse.builder()
				.success(false)
				.message("处理用户响应失败: " + e.getMessage())
				.sessionId(response.getSessionId())
				.timestamp(LocalDateTime.now())
				.build();
		}
	}

}
