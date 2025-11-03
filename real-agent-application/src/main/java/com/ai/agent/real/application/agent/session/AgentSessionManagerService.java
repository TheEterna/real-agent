package com.ai.agent.real.application.agent.session;

import com.ai.agent.real.common.utils.CommonUtils;
import com.ai.agent.real.contract.agent.AgentStrategy;
import com.ai.agent.real.contract.agent.context.AgentContextAble;
import com.ai.agent.real.contract.agent.service.IAgentSessionManagerService;
import com.ai.agent.real.contract.model.callback.ToolApprovalCallback;

import com.ai.agent.real.contract.agent.context.ResumePoint;
import com.ai.agent.real.contract.model.interaction.*;
import com.ai.agent.real.contract.model.protocol.AgentExecutionEvent;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Component;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Agent会话管理中心 负责管理每个会话的Sink、上下文和待审批工具
 *
 * 核心职责： 1. 管理SSE连接的Sink生命周期 2. 维护会话上下文和执行状态 3. 支持工具审批中断和恢复
 *
 * @author han
 * @time 2025/10/22 15:45
 */
@Slf4j
@Component
public class AgentSessionManagerService implements IAgentSessionManagerService {

	private final AgentStrategy agentStrategy;

	/**
	 * 会话状态映射表 Key: sessionId Value: SessionState
	 */
	private final Map<String, SessionState> sessions;

	public AgentSessionManagerService(AgentStrategy agentStrategy) {
		this.agentStrategy = agentStrategy;
		this.sessions = new ConcurrentHashMap<>();
	}

	public AgentSessionManagerService(AgentStrategy agentStrategy, Map<String, SessionState> sessions) {
		this.agentStrategy = agentStrategy;
		this.sessions = sessions;
	}

	/**
	 * 订阅会话的SSE流
	 * @param sessionId 会话ID
	 * @param message 用户消息
	 * @param context 执行上下文
	 * @return SSE事件流
	 */
	@Override
	public Flux<ServerSentEvent<AgentExecutionEvent>> subscribe(String sessionId, String message,
			AgentContextAble context) {
		log.info("[AgentSessionHub] 创建会话订阅: sessionId={}, message={}", sessionId, message);

		// 创建或获取会话状态
		SessionState state = ensureSession(sessionId, context);

		// 开始执行Agent任务
		startAgentExecution(state, message, context);

		// 返回SSE流
		return state.getSink()
			.asFlux()
			.doOnCancel(() -> log.debug("[AgentSessionHub] 订阅取消: sessionId={}", sessionId))
			.doOnTerminate(() -> log.debug("[AgentSessionHub] 订阅终止: sessionId={}", sessionId))
			.doFinally(signal -> {
				log.info("[AgentSessionHub] SSE连接关闭: sessionId={}, signal={}", sessionId, signal);
				// 注意：不在这里清理会话，因为可能需要工具审批后恢复
			});
	}

	/**
	 * 请求用户交互（通用方法）
	 * @param sessionId 会话ID
	 * @param request 交互请求
	 */
	@Override
	public void requestInteraction(String sessionId, InteractionRequest request) {
		log.info("[AgentSessionHub] 请求用户交互: sessionId={}, type={}, requestId={}", sessionId, request.getType(),
				request.getRequestId());

		SessionState state = sessions.get(sessionId);
		if (state == null) {
			log.warn("[AgentSessionHub] 会话不存在，无法请求交互: sessionId={}", sessionId);
			return;
		}

		// 创建恢复点
		ResumePoint resumePoint = new ResumePoint();
		resumePoint.setResumeId(request.getRequestId());
		resumePoint.setSessionId(sessionId);
		resumePoint.setCurrentIteration(state.getContext().getCurrentIteration());
		resumePoint.setPausedStage(ResumePoint.ReActStage.ACTION);
		resumePoint.setInteractionRequest(request);
		resumePoint.setContext(state.getContext()); // TODO 注意：这里应该深拷贝，但为了简化先直接引用
		resumePoint.setOriginalTask(state.getContext().getTask());
		resumePoint.setCreatedAt(LocalDateTime.now());

		// 保存到会话状态
		state.setResumePoint(resumePoint);

		// 推送交互请求事件到前端
		AgentExecutionEvent interactionEvent = AgentExecutionEvent.interaction(state.getContext(), request);

		state.getSink().tryEmitNext(toSSE(interactionEvent));

		log.info("[AgentSessionHub] 交互请求已发送到前端: sessionId={}, requestId={}", sessionId, request.getRequestId());
	}

	/**
	 * 处理用户响应（通用方法）
	 * @param sessionId 会话ID
	 * @param response 用户响应
	 */
	@Override
	public void handleInteractionResponse(String sessionId, InteractionResponse response) {
		log.info("[AgentSessionHub] 处理用户响应: sessionId={}, requestId={}, option={}", sessionId, response.getRequestId(),
				response.getSelectedOptionId());

		SessionState state = sessions.get(sessionId);
		if (state == null) {
			log.warn("[AgentSessionHub] 会话不存在: sessionId={}", sessionId);
			return;
		}

		ResumePoint resumePoint = state.getResumePoint();
		if (resumePoint == null || !resumePoint.getResumeId().equals(response.getRequestId())) {
			log.warn("[AgentSessionHub] 恢复点不匹配: sessionId={}, expected={}, actual={}", sessionId,
					resumePoint != null ? resumePoint.getResumeId() : "null", response.getRequestId());
			return;
		}

		// 保存用户响应
		resumePoint.setUserResponse(response);

		// 根据用户选择的动作决定如何恢复
		InteractionOption selectedOption = resumePoint.getInteractionRequest()
			.getOptions()
			.stream()
			.filter(opt -> opt.getOptionId().equals(response.getSelectedOptionId()))
			.findFirst()
			.orElse(null);

		if (selectedOption == null) {
			log.warn("[AgentSessionHub] 无效的选项ID: {}", response.getSelectedOptionId());
			return;
		}

		log.info("[AgentSessionHub] 用户选择动作: {}", selectedOption.getAction());

		// 根据动作类型恢复执行
		switch (selectedOption.getAction()) {
			case APPROVE_AND_EXECUTE:
				resumeWithExecution(state, resumePoint);
				break;
			case RETRY_WITH_FEEDBACK:
				resumeWithRetry(state, resumePoint, response.getFeedback());
				break;
			case REJECT_WITH_REASON:
				resumeWithRejection(state, resumePoint, response.getFeedback());
				break;
			case TERMINATE:
				terminateSession(state, resumePoint, response.getFeedback());
				break;
			case PROVIDE_INFO:
				resumeWithProvidedInfo(state, resumePoint, response.getData());
				break;
			case SKIP:
				resumeWithSkip(state, resumePoint);
				break;
			default:
				log.warn("[AgentSessionHub] 不支持的动作类型: {}", selectedOption.getAction());
		}

		// 清除恢复点
		state.setResumePoint(null);
	}

	/**
	 * 暂停会话执行，等待工具审批（便捷方法） 此方法会被 ToolApprovalCallback 调用
	 * @param sessionId 会话ID
	 * @param toolCallId 工具调用ID
	 * @param toolName 工具名称
	 * @param toolArgs 工具参数
	 * @param context 执行上下文
	 */
	@Override
	public void pauseForToolApproval(String sessionId, String toolCallId, String toolName, Map<String, Object> toolArgs,
			AgentContextAble context) {
		log.info("[AgentSessionHub] 暂停执行等待工具审批: sessionId={}, toolName={}", sessionId, toolName);

		// 构建交互请求
		InteractionRequest request = new InteractionRequest();
		request.setRequestId(toolCallId);
		request.setSessionId(sessionId);
		request.setType(InteractionType.TOOL_APPROVAL);
		request.setTitle("工具执行审批");
		request.setMessage(String.format("Agent 请求执行工具: %s", toolName));
		request.addContext("toolName", toolName);
		request.addContext("toolArgs", toolArgs);

		// 定义四个选项
		request.addOption(new InteractionOption().setOptionId("approve")
			.setLabel("同意执行")
			.setDescription("直接执行该工具")
			.setAction(InteractionAction.APPROVE_AND_EXECUTE)
			.setDefault(false));

		request.addOption(new InteractionOption().setOptionId("retry")
			.setLabel("重新执行")
			.setDescription("让 Agent 重新思考并选择其他方案")
			.setAction(InteractionAction.RETRY_WITH_FEEDBACK)
			.setRequiresInput(true)
			.setInputPrompt("请提供反馈（可选）"));

		request.addOption(new InteractionOption().setOptionId("reject")
			.setLabel("拒绝并说明理由")
			.setDescription("拒绝执行并提供反馈给 Agent")
			.setAction(InteractionAction.REJECT_WITH_REASON)
			.setRequiresInput(true)
			.setInputPrompt("请说明拒绝理由"));

		request.addOption(new InteractionOption().setOptionId("terminate")
			.setLabel("拒绝并终止对话")
			.setDescription("拒绝执行并结束整个任务")
			.setAction(InteractionAction.TERMINATE)
			.setDangerous(true)
			.setRequiresInput(true)
			.setInputPrompt("请说明终止原因（可选）"));

		// 调用通用的交互请求方法
		requestInteraction(sessionId, request);
	}

	/**
	 * 关闭会话
	 * @param sessionId 会话ID
	 */
	@Override
	public void closeSession(String sessionId) {
		log.info("[AgentSessionHub] 关闭会话: sessionId={}", sessionId);

		SessionState state = sessions.remove(sessionId);
		if (state != null) {
			// 取消当前执行
			if (state.getCurrentExecution() != null && !state.getCurrentExecution().isDisposed()) {
				state.getCurrentExecution().dispose();
			}

			// 完成Sink
			state.getSink().tryEmitComplete();
			state.setClosed(true);
		}
	}

	/**
	 * only deep copy agentStrategy, ensure to avoid agentStrategy change
	 */
	@Override
	public IAgentSessionManagerService of(AgentStrategy agentStrategy) {
		return new AgentSessionManagerService(agentStrategy, this.sessions);
	}

	/**
	 * 确保会话存在
	 */
	private SessionState ensureSession(String sessionId, AgentContextAble context) {
		return sessions.computeIfAbsent(sessionId, sid -> {
			log.info("[AgentSessionHub] 创建新会话: sessionId={}", sid);
			SessionState state = new SessionState();
			state.setSessionId(sid);
			state.setContext(context);
			state.setSink(Sinks.many().multicast().onBackpressureBuffer());
			state.setClosed(false);
			state.setCreatedAt(LocalDateTime.now());
			return state;
		});
	}

	/**
	 * 开始执行Agent任务
	 */
	private void startAgentExecution(SessionState state, String message, AgentContextAble context) {
		log.info("[AgentSessionHub] 开始执行 Agent: sessionId={}", state.getSessionId());

		// 执行Agent流式任务
		Disposable execution = agentStrategy.executeStream(message, null, context).map(this::toSSE).doOnNext(event -> {
			// 推送事件到Sink
			Sinks.EmitResult result = state.getSink().tryEmitNext(event);
			if (result.isFailure()) {
				log.warn("[AgentSessionHub] 推送事件失败: sessionId={}, result={}", state.getSessionId(), result);
			}
		}).doOnError(error -> {
			log.error("[AgentSessionHub] Agent执行异常: sessionId={}", state.getSessionId(), error);
			state.getSink().tryEmitNext(toSSE(AgentExecutionEvent.error(error)));
		}).doOnComplete(() -> {
			log.info("[AgentSessionHub] Agent任务执行完成: sessionId={}", state.getSessionId());
			// 任务完成后关闭Sink
			state.getSink().tryEmitComplete();
			state.setClosed(true);
			// 清理会话
			sessions.remove(state.getSessionId());
		}).subscribe();

		state.setCurrentExecution(execution);
	}

	/**
	 * 同意并执行：直接执行工具
	 */
	private void resumeWithExecution(SessionState state, ResumePoint resumePoint) {
		log.info("[AgentSessionHub] 同意并执行: resumeId={}", resumePoint.getResumeId());

		// 创建回调（用于后续可能的工具审批）
		ToolApprovalCallback approvalCallback = (sid, tcid, tname, targs, ctx) -> pauseForToolApproval(sid, tcid, tname,
				targs, ctx);

		// 调用 ReActAgentStrategy 的 resumeFromToolApproval 方法
		Disposable execution = agentStrategy.resumeFromToolApproval(resumePoint, approvalCallback)
			.map(this::toSSE)
			.doOnNext(event -> {
				// 推送事件到Sink
				Sinks.EmitResult result = state.getSink().tryEmitNext(event);
				if (result.isFailure()) {
					log.warn("[AgentSessionHub] 推送事件失败: sessionId={}, result={}", state.getSessionId(), result);
				}
			})
			.doOnError(error -> {
				log.error("[AgentSessionHub] 恢复执行异常: sessionId={}", state.getSessionId(), error);
				state.getSink().tryEmitNext(toSSE(AgentExecutionEvent.error(error)));
			})
			.doOnComplete(() -> {
				log.info("[AgentSessionHub] 恢复执行完成: sessionId={}", state.getSessionId());
				// 任务完成后关闭Sink
				state.getSink().tryEmitComplete();
				state.setClosed(true);
				// 清理会话
				sessions.remove(state.getSessionId());
			})
			.subscribe();

		state.setCurrentExecution(execution);
	}

	/**
	 * 重新执行：让 Agent 重新思考
	 */
	private void resumeWithRetry(SessionState state, ResumePoint resumePoint, String feedback) {
		log.info("[AgentSessionHub] 重新执行: resumeId={}, feedback={}", resumePoint.getResumeId(), feedback);

		// 将用户反馈添加到上下文
		if (feedback != null && !feedback.isBlank()) {
			state.getContext()
				.addMessage(com.ai.agent.real.contract.model.message.AgentMessage.user("用户反馈: " + feedback, "user"));
		}

		// 创建回调
		ToolApprovalCallback approvalCallback = (sid, tcid, tname, targs, ctx) -> pauseForToolApproval(sid, tcid, tname,
				targs, ctx);

		// 从当前迭代重新开始（让 Agent 重新思考）
		Disposable execution = agentStrategy.resumeFromToolApproval(resumePoint, approvalCallback)
			.map(this::toSSE)
			.doOnNext(event -> {
				Sinks.EmitResult result = state.getSink().tryEmitNext(event);
				if (result.isFailure()) {
					log.warn("[AgentSessionHub] 推送事件失败: sessionId={}, result={}", state.getSessionId(), result);
				}
			})
			.doOnError(error -> {
				log.error("[AgentSessionHub] 重新执行异常: sessionId={}", state.getSessionId(), error);
				state.getSink().tryEmitNext(toSSE(AgentExecutionEvent.error(error)));
			})
			.doOnComplete(() -> {
				log.info("[AgentSessionHub] 重新执行完成: sessionId={}", state.getSessionId());
				state.getSink().tryEmitComplete();
				state.setClosed(true);
				sessions.remove(state.getSessionId());
			})
			.subscribe();

		state.setCurrentExecution(execution);
	}

	/**
	 * 拒绝并说明理由：推送拒绝消息，继续下一轮迭代
	 */
	private void resumeWithRejection(SessionState state, ResumePoint resumePoint, String reason) {
		log.info("[AgentSessionHub] 拒绝执行: resumeId={}, reason={}", resumePoint.getResumeId(), reason);

		// 将拒绝理由添加到上下文
		String rejectMessage = "用户拒绝执行工具";
		if (reason != null && !reason.isBlank()) {
			rejectMessage += ": " + reason;
		}
		state.getContext()
			.addMessage(com.ai.agent.real.contract.model.message.AgentMessage.user(rejectMessage, "user"));

		// 推送拒绝消息
		state.getSink().tryEmitNext(toSSE(AgentExecutionEvent.error(rejectMessage)));

		// 创建回调
		ToolApprovalCallback approvalCallback = (sid, tcid, tname, targs, ctx) -> pauseForToolApproval(sid, tcid, tname,
				targs, ctx);

		// 继续执行（跳过当前工具，进入下一轮迭代）
		Disposable execution = agentStrategy.resumeFromToolApproval(resumePoint, approvalCallback)
			.map(this::toSSE)
			.doOnNext(event -> {
				Sinks.EmitResult result = state.getSink().tryEmitNext(event);
				if (result.isFailure()) {
					log.warn("[AgentSessionHub] 推送事件失败: sessionId={}, result={}", state.getSessionId(), result);
				}
			})
			.doOnError(error -> {
				log.error("[AgentSessionHub] 拒绝后继续执行异常: sessionId={}", state.getSessionId(), error);
				state.getSink().tryEmitNext(toSSE(AgentExecutionEvent.error(error)));
			})
			.doOnComplete(() -> {
				log.info("[AgentSessionHub] 拒绝后继续执行完成: sessionId={}", state.getSessionId());
				state.getSink().tryEmitComplete();
				state.setClosed(true);
				sessions.remove(state.getSessionId());
			})
			.subscribe();

		state.setCurrentExecution(execution);
	}

	/**
	 * 终止对话：直接结束任务
	 */
	private void terminateSession(SessionState state, ResumePoint resumePoint, String reason) {
		log.info("[AgentSessionHub] 终止对话: sessionId={}, reason={}", state.getSessionId(), reason);

		// 推送终止消息
		String terminateMessage = "用户终止对话";
		if (reason != null && !reason.isBlank()) {
			terminateMessage += ": " + reason;
		}
		state.getSink().tryEmitNext(toSSE(AgentExecutionEvent.error(terminateMessage)));

		// 关闭 Sink
		state.getSink().tryEmitComplete();
		state.setClosed(true);

		// 清理会话
		sessions.remove(state.getSessionId());

		log.info("[AgentSessionHub] 会话已终止: sessionId={}", state.getSessionId());
	}

	/**
	 * 提供信息：将用户提供的信息添加到上下文，继续执行
	 */
	private void resumeWithProvidedInfo(SessionState state, ResumePoint resumePoint, Map<String, Object> data) {
		log.info("[AgentSessionHub] 提供信息: resumeId={}, data={}", resumePoint.getResumeId(), data);

		// 将用户提供的信息添加到上下文
		if (data != null && !data.isEmpty()) {
			// 这里可以根据具体场景处理数据，例如设置到 toolArgs 或其他地方
			state.getContext()
				.addMessage(com.ai.agent.real.contract.model.message.AgentMessage.user("用户提供的信息: " + data, "user"));
		}

		// 创建回调
		ToolApprovalCallback approvalCallback = (sid, tcid, tname, targs, ctx) -> pauseForToolApproval(sid, tcid, tname,
				targs, ctx);

		// 继续执行
		Disposable execution = agentStrategy.resumeFromToolApproval(resumePoint, approvalCallback)
			.map(this::toSSE)
			.doOnNext(event -> {
				Sinks.EmitResult result = state.getSink().tryEmitNext(event);
				if (result.isFailure()) {
					log.warn("[AgentSessionHub] 推送事件失败: sessionId={}, result={}", state.getSessionId(), result);
				}
			})
			.doOnError(error -> {
				log.error("[AgentSessionHub] 提供信息后继续执行异常: sessionId={}", state.getSessionId(), error);
				state.getSink().tryEmitNext(toSSE(AgentExecutionEvent.error(error)));
			})
			.doOnComplete(() -> {
				log.info("[AgentSessionHub] 提供信息后继续执行完成: sessionId={}", state.getSessionId());
				state.getSink().tryEmitComplete();
				state.setClosed(true);
				sessions.remove(state.getSessionId());
			})
			.subscribe();

		state.setCurrentExecution(execution);
	}

	/**
	 * 跳过：跳过当前操作，继续下一步
	 */
	private void resumeWithSkip(SessionState state, ResumePoint resumePoint) {
		log.info("[AgentSessionHub] 跳过当前操作: resumeId={}", resumePoint.getResumeId());

		// 推送跳过消息
		state.getSink().tryEmitNext(toSSE(AgentExecutionEvent.progress(state.getContext(), "用户跳过当前操作", null)));

		// 创建回调
		ToolApprovalCallback approvalCallback = (sid, tcid, tname, targs, ctx) -> pauseForToolApproval(sid, tcid, tname,
				targs, ctx);

		// 继续执行下一步
		Disposable execution = agentStrategy.resumeFromToolApproval(resumePoint, approvalCallback)
			.map(this::toSSE)
			.doOnNext(event -> {
				Sinks.EmitResult result = state.getSink().tryEmitNext(event);
				if (result.isFailure()) {
					log.warn("[AgentSessionHub] 推送事件失败: sessionId={}, result={}", state.getSessionId(), result);
				}
			})
			.doOnError(error -> {
				log.error("[AgentSessionHub] 跳过后继续执行异常: sessionId={}", state.getSessionId(), error);
				state.getSink().tryEmitNext(toSSE(AgentExecutionEvent.error(error)));
			})
			.doOnComplete(() -> {
				log.info("[AgentSessionHub] 跳过后继续执行完成: sessionId={}", state.getSessionId());
				state.getSink().tryEmitComplete();
				state.setClosed(true);
				sessions.remove(state.getSessionId());
			})
			.subscribe();

		state.setCurrentExecution(execution);
	}

	/**
	 * 将AgentExecutionEvent转换为SSE事件
	 */
	private ServerSentEvent<AgentExecutionEvent> toSSE(AgentExecutionEvent event) {
		return ServerSentEvent.<AgentExecutionEvent>builder()
            .id(CommonUtils.getTraceId("sse-"))
			.event(event.getType() != null ? event.getType().toString() : "message")
			.data(event)
			.build();
	}

	/**
	 * 会话状态
	 */
	@Data
	public static class SessionState {

		/**
		 * 会话ID
		 */
		private String sessionId;

		/**
		 * SSE推送Sink
		 */
		private Sinks.Many<ServerSentEvent<AgentExecutionEvent>> sink;

		/**
		 * 执行上下文
		 */
		private AgentContextAble context;

		/**
		 * 当前执行的订阅句柄
		 */
		private Disposable currentExecution;

		/**
		 * 恢复点（用于工具审批后恢复执行）
		 */
		private ResumePoint resumePoint;

		/**
		 * 会话是否已关闭
		 */
		private boolean closed;

		/**
		 * 创建时间
		 */
		private LocalDateTime createdAt;

	}

}
