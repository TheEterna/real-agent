package com.ai.agent.real.web.controller.agent;

import com.ai.agent.real.common.constant.NounConstants;
import com.ai.agent.real.common.utils.CommonUtils;
import com.ai.agent.real.contract.agent.IAgentStrategy;
import com.ai.agent.real.contract.agent.context.AgentContextAble;
import com.ai.agent.real.contract.agent.service.IAgentTurnManagerService;
import com.ai.agent.real.contract.dto.ChatRequest;
import com.ai.agent.real.contract.model.interaction.InteractionResponse;
import com.ai.agent.real.contract.model.logging.TraceInfo;
import com.ai.agent.real.contract.model.protocol.AgentExecutionEvent;
import com.ai.agent.real.contract.model.protocol.ResponseResult;
import com.ai.agent.real.contract.user.ISessionService;
import com.ai.agent.real.contract.model.context.reactplus.ReActPlusAgentContext;
import com.ai.agent.real.contract.model.context.reactplus.ReActPlusAgentContextMeta;
import com.ai.agent.real.contract.model.auth.UserContextHolder;
import com.ai.agent.real.contract.user.SessionDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;

import static com.ai.agent.real.contract.model.protocol.ResponseResult.success;

/**
 * @author: han
 * @time: 2025/10/22 16:46
 */

@Slf4j
@RestController
@RequestMapping("/api/agent/chat")
public class ReActPlusAgentController {

	private final IAgentTurnManagerService agentSessionManagerService;

	private final IAgentStrategy reActPlusAgentStrategy;

	private final ISessionService sessionService;

	public ReActPlusAgentController(IAgentTurnManagerService agentSessionManagerService,
			@Qualifier("reActPlusAgentStrategy") IAgentStrategy reActPlusAgentStrategy,
			ISessionService sessionService) {

		this.agentSessionManagerService = agentSessionManagerService.of(reActPlusAgentStrategy);
		this.reActPlusAgentStrategy = reActPlusAgentStrategy;
		this.sessionService = sessionService;
	}

	/**
	 * 执行ReActPlus任务（流式响应） 使用AgentSessionHub管理会话，支持工具审批中断和恢复
	 */
	@PostMapping(value = "/react-plus/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
	public Flux<ServerSentEvent<AgentExecutionEvent>> executeReActPlusStream(@RequestBody ChatRequest request) {
		log.info("收到ReAct-Plus流式执行请求: sessionId={}, message={}", request.getSessionId(), request.getMessage());

		return UserContextHolder.getUserId().flatMapMany(userId -> {
			log.info("收到ReAct-Plus流式执行请求: sessionId={}, message={}", request.getSessionId(), request.getMessage());
			return Mono.justOrEmpty(request.getSessionId())
				.filter(id -> !Objects.isNull(id))
				.switchIfEmpty(

						this.sessionService
							.createSessionWithAiTitle("", NounConstants.REACT_PLUS, userId, request.getMessage())
							.map(SessionDTO::getId)

				) // 传入
				.flatMapMany(sessionId -> {
					request.setSessionId(sessionId);

					// 3. 创建并执行 Agent 上下文
					UUID turnId = UUID.randomUUID();

					AgentContextAble<ReActPlusAgentContextMeta> context = new ReActPlusAgentContext(
							new TraceInfo().setSessionId(request.getSessionId())
								.setTurnId(turnId)
								.setStartTime(OffsetDateTime.now()));
					context.setTask(request.getMessage());

					return agentSessionManagerService.subscribe(turnId.toString(), request.getMessage(), context)
						.doOnError(error -> log.error("ReAct执行异常: sessionId={}", request.getSessionId(), error))
						.doOnComplete(() -> {
							context.setEndTime(OffsetDateTime.now());
							log.info("ReActPlus任务执行完成: sessionId={}", request.getSessionId());
						});
				});
		}).switchIfEmpty(Flux.error(new IllegalAccessException("未登录或用户凭证无效"))); // 安全兜底

	}

	/**
	 * 处理用户交互响应（通用接口） 支持所有类型的交互响应：工具审批、缺少信息、用户确认等
	 *
	 * 前端调用此接口来响应工具审批请求，触发恢复执行
	 */
	@PostMapping("/react-plus/interaction_response")
	public Mono<ResponseResult<String>> handleInteractionResponse(@RequestBody InteractionResponse response) {

		return Mono.fromSupplier(() -> {
			// return reActPlusAgentStrategy.handleInteractionResponse(response);
			agentSessionManagerService.handleInteractionResponse(response.getTurnId(), response);
			return ResponseResult.success("交互成功", "交互成功");
		});

	}

}
