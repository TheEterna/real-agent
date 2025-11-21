package com.ai.agent.real.web.controller.agent;

import com.ai.agent.real.common.constant.NounConstants;
import com.ai.agent.real.contract.agent.IAgentStrategy;
import com.ai.agent.real.contract.agent.context.AgentContextAble;
import com.ai.agent.real.contract.agent.service.IAgentTurnManagerService;
import com.ai.agent.real.contract.dto.ChatRequest;
import com.ai.agent.real.contract.model.interaction.InteractionResponse;
import com.ai.agent.real.contract.model.logging.TraceInfo;
import com.ai.agent.real.contract.model.protocol.AgentExecutionEvent;
import com.ai.agent.real.contract.model.protocol.ResponseResult;
import com.ai.agent.real.contract.service.agent.IAgentStorageService;
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


	private final ISessionService sessionService;

	private final IAgentStorageService agentStorageService;

	public ReActPlusAgentController(IAgentTurnManagerService agentSessionManagerService,
			@Qualifier("reActPlusAgentStrategy") IAgentStrategy reActPlusAgentStrategy,
			ISessionService sessionService,
			IAgentStorageService agentStorageService) {

		this.agentSessionManagerService = agentSessionManagerService.of(reActPlusAgentStrategy);
		this.sessionService = sessionService;
		this.agentStorageService = agentStorageService;
	}

	/**
	 * 执行ReActPlus任务（流式响应） 使用AgentSessionHub管理会话，支持工具审批中断和恢复
	 */
	@PostMapping(value = "/react-plus/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
	public Flux<ServerSentEvent<AgentExecutionEvent>> executeReActPlusStream(@RequestBody ChatRequest request) {

		return UserContextHolder.getUserId().flatMapMany(userId -> {
			log.info("收到ReAct-Plus流式执行请求: sessionId={}, message={}", request.getSessionId(), request.getMessage());

            return Mono.justOrEmpty(request.getSessionId())
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

					// 保存用户消息作为第一条消息 (可选，或者在 executeStreamWithInteraction 内部处理，这里先手动保存一下用户提问)
					// 注意：AgentExecutionEvent 中通常包含用户提问，或者我们可以构造一个
					// 这里为了简单，我们依赖 agentSessionManagerService 返回的流中包含的事件
					// 但通常第一条用户消息是输入，可能不在流里，需要手动保存。
					// 让我们先保存 Turn，然后手动保存一条 User Message

					return agentStorageService.startTurn(turnId, null, sessionId) // TODO: parentTurnId logic if needed
						.flatMapMany(turn -> {
							// 保存用户输入消息
							AgentExecutionEvent userEvent = AgentExecutionEvent.common(AgentExecutionEvent.EventType.USER, context, request.getMessage());
							return agentStorageService.saveMessage(sessionId, turnId, userEvent)
								.thenMany(agentSessionManagerService.subscribe(turnId.toString(), request.getMessage(), context));
						})
						.doOnNext(sse -> {
							if (sse.data() != null) {
								agentStorageService.saveMessage(sessionId, turnId, sse.data()).subscribe();
							}
						})
						.doOnError(error -> log.error("ReAct执行异常: sessionId={}", request.getSessionId(), error))
						.doOnComplete(() -> {
							context.setEndTime(OffsetDateTime.now());
							agentStorageService.completeTurn(turnId).subscribe();
						});
				});
		}).switchIfEmpty(Flux.error(new IllegalAccessException("未登录或用户凭证无效"))); // 安全兜底

	}

	/**
	 * 获取会话历史消息
	 */
	@GetMapping("/react-plus/{sessionId}/messages")
	public Mono<ResponseResult<Object>> getSessionMessages(@PathVariable UUID sessionId) {
		return agentStorageService.getSessionMessages(sessionId)
			.collectList()
			.map(ResponseResult::success);
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
