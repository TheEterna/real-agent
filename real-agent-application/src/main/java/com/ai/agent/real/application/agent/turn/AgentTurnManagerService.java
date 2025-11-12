package com.ai.agent.real.application.agent.turn;

import com.ai.agent.real.common.utils.CommonUtils;
import com.ai.agent.real.contract.agent.IAgentStrategy;
import com.ai.agent.real.contract.agent.context.AgentContextAble;
import com.ai.agent.real.contract.agent.service.IAgentTurnManagerService;

import com.ai.agent.real.contract.model.interaction.*;
import com.ai.agent.real.contract.model.protocol.AgentExecutionEvent;
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
 * <p>
 * 核心职责： 1. 管理SSE连接的Sink生命周期 2. 维护会话上下文和执行状态 3. 支持工具审批中断和恢复
 *
 * @author han
 * @time 2025/10/22 15:45
 */
@Slf4j
public class AgentTurnManagerService implements IAgentTurnManagerService {

	private final IAgentStrategy agentStrategy;

	/**
	 * 会话状态映射表 Key: turnId Value: TurnState
	 */
	private final Map<String, TurnState> turns;

	public AgentTurnManagerService() {
		this.agentStrategy = null;
		this.turns = new ConcurrentHashMap<>();
	}

	public AgentTurnManagerService(IAgentStrategy agentStrategy) {
		this.agentStrategy = agentStrategy;
		this.turns = new ConcurrentHashMap<>();
	}

	public AgentTurnManagerService(IAgentStrategy agentStrategy, Map<String, TurnState> turns) {
		this.agentStrategy = agentStrategy;
		this.turns = turns;
	}

	/**
	 * 订阅会话的SSE流
	 * @param turnId 会话ID
	 * @param message 用户消息
	 * @param context 执行上下文
	 * @return SSE事件流
	 */
	@Override
	public Flux<ServerSentEvent<AgentExecutionEvent>> subscribe(String turnId, String message,
			AgentContextAble context) {
		log.info("创建会话订阅: turnId={}, message={}", turnId, message);

		// 创建或获取会话状态
		TurnState state = ensureTurn(turnId, context);

		// 开始执行Agent任务
		startAgentExecution(state, message, context);

		// 返回SSE流
		return state.getSink()
			.asFlux()
			.doOnCancel(() -> log.debug("订阅取消: turnId={}", turnId))
			.doOnTerminate(() -> log.debug("订阅终止: turnId={}", turnId))
			.doFinally(signal -> {
				log.info("SSE连接关闭: turnId={}, signal={}", turnId, signal);
				// 注意：不在这里清理会话，因为可能需要工具审批后恢复
			});
	}

	/**
	 * 处理用户响应（通用方法）
	 * @param turnId 会话ID
	 * @param response 用户响应
	 */
	@Override
	public void handleInteractionResponse(String turnId, InteractionResponse response) {
		log.info("处理用户交互响应: turnId={}, requestId={}, option={}", turnId, response.getRequestId(),
				response.getSelectedOptionId());

		TurnState state = turns.get(turnId);

		if (state == null) {
			log.error("not have the turn: {}", turnId);
			return;
		}
		// 先把 这个 Sink.One 审批的阻塞点给空掉，以免影响该 turn 下的交互
		Sinks.One<InteractionResponse> approvalGate = state.getPendingApproval();
		state.setPendingApproval(null);
		if (approvalGate != null) {
			approvalGate.tryEmitValue(response);
		}
		else {
			log.warn("[ApprovalGate] resolve but gate not found: {}", turnId);
		}

	}

	/**
	 * 关闭会话
	 * @param turnId 会话ID
	 */
	@Override
	public void closeTurn(String turnId) {
		log.info("关闭会话: turnId={}", turnId);

		TurnState state = turns.remove(turnId);
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
	public IAgentTurnManagerService of(IAgentStrategy agentStrategy) {
		return new AgentTurnManagerService(agentStrategy, this.turns);
	}

	@Override
	public TurnState getTurnState(String turnId) {
		return this.turns.get(turnId);
	}

	/**
	 * 确保会话存在
	 */
	private TurnState ensureTurn(String turnId, AgentContextAble context) {
		return turns.computeIfAbsent(turnId, tid -> {
			log.info("创建新会话: turnId={}", tid);
			TurnState state = new TurnState();
			state.setTurnId(tid);
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
	private void startAgentExecution(TurnState state, String message, AgentContextAble context) {
		log.info("开始执行 Agent: turnId={}", state.getTurnId());

		// 执行Agent流式任务
		Disposable execution = agentStrategy.executeStreamWithInteraction(message, state, context)
			.map(this::toSSE)
//			.doOnNext(event -> {
//
//				if (event.data().getType() == AgentExecutionEvent.EventType.TOOL_APPROVAL) {
//					// 创建等待点
//					Sinks.One<InteractionResponse> approvalGate = Sinks.one();
//					state.setPendingApproval(approvalGate);
//				}
//				// 推送事件到Sink
//				Sinks.EmitResult result = state.getSink().tryEmitNext(event);
//				if (result.isFailure()) {
//					log.warn("推送事件失败: turnId={}, result={}", state.getTurnId(), result);
//				}
//			})
			.doOnError(error -> {
				log.error("Agent执行异常: turnId={}", state.getTurnId(), error);
				state.getSink().tryEmitNext(toSSE(AgentExecutionEvent.error(error)));
			})
			.doOnComplete(() -> {
				log.info("Agent任务执行完成: turnId={}", state.getTurnId());
				// 任务完成后关闭Sink
				state.setClosed(true);
				// 清理会话
				state.getSink().tryEmitComplete();
				turns.remove(state.getTurnId());
			})
			.subscribe();

		state.setCurrentExecution(execution);
	}

	/**
	 * 同意并执行：直接执行工具
	 */
	private void resumeWithExecution(TurnState state) {
		log.info("同意并执行");

		// 调用 ReActAgentStrategy 的 resumeFromToolApproval 方法
		// Disposable execution =
		// agentStrategy.resumeFromToolApproval(resumePoint).map(this::toSSE).doOnNext(event
		// -> {
		// // 推送事件到Sink
		// Sinks.EmitResult result = state.getSink().tryEmitNext(event);
		// if (result.isFailure()) {
		// log.warn("推送事件失败: turnId={}, result={}", state.getturnId(), result);
		// }
		// }).doOnError(error -> {
		// log.error("恢复执行异常: turnId={}", state.getturnId(), error);
		// state.getSink().tryEmitNext(toSSE(AgentExecutionEvent.error(error)));
		// }).doOnComplete(() -> {
		// log.info("恢复执行完成: turnId={}", state.getturnId());
		// // 任务完成后关闭Sink
		// state.getSink().tryEmitComplete();
		// state.setClosed(true);
		// // 清理会话
		// turns.remove(state.getTurnId());
		// }).subscribe();

		// state.setCurrentExecution(execution);
	}

	/**
	 * 重新执行：让 Agent 重新思考
	 */
	// private void resumeWithRetry(TurnState state, ResumePoint resumePoint, String
	// feedback) {
	// log.info("重新执行: resumeId={}, feedback={}", resumePoint.getResumeId(), feedback);
	//
	// // 将用户反馈添加到上下文
	// if (feedback != null && !feedback.isBlank()) {
	// state.getContext()
	// .addMessage(com.ai.agent.real.contract.model.message.AgentMessage.user("用户反馈: " +
	// feedback, "user"));
	// }
	//
	// // 从当前迭代重新开始（让 Agent 重新思考）
	// Disposable execution =
	// agentStrategy.resumeFromToolApproval(resumePoint).map(this::toSSE).doOnNext(event
	// -> {
	// Sinks.EmitResult result = state.getSink().tryEmitNext(event);
	// if (result.isFailure()) {
	// log.warn("推送事件失败: sessionId={}, result={}", state.getSessionId(), result);
	// }
	// }).doOnError(error -> {
	// log.error("重新执行异常: sessionId={}", state.getSessionId(), error);
	// state.getSink().tryEmitNext(toSSE(AgentExecutionEvent.error(error)));
	// }).doOnComplete(() -> {
	// log.info("重新执行完成: sessionId={}", state.getSessionId());
	// state.getSink().tryEmitComplete();
	// state.setClosed(true);
	// sessions.remove(state.getSessionId());
	// }).subscribe();
	//
	// state.setCurrentExecution(execution);
	// }

	/**
	 * 拒绝并说明理由：推送拒绝消息，继续下一轮迭代
	 */
	// private void resumeWithRejection(TurnState state,String reason) {
	// log.info("拒绝执行: reason={}", reason);
	//
	// // 将拒绝理由添加到上下文
	// String rejectMessage = "用户拒绝执行工具";
	// if (reason != null && !reason.isBlank()) {
	// rejectMessage += ": " + reason;
	// }
	// state.getContext()
	// .addMessage(com.ai.agent.real.contract.model.message.AgentMessage.user(rejectMessage,
	// "user"));
	//
	// // 推送拒绝消息
	// state.getSink().tryEmitNext(toSSE(AgentExecutionEvent.error(rejectMessage)));
	//
	// // 继续执行（跳过当前工具，进入下一轮迭代）
	// Disposable execution =
	// agentStrategy.resumeFromToolApproval(resumePoint).map(this::toSSE).doOnNext(event
	// -> {
	// Sinks.EmitResult result = state.getSink().tryEmitNext(event);
	// if (result.isFailure()) {
	// log.warn("推送事件失败: sessionId={}, result={}", state.getSessionId(), result);
	// }
	// }).doOnError(error -> {
	// log.error("拒绝后继续执行异常: sessionId={}", state.getSessionId(), error);
	// state.getSink().tryEmitNext(toSSE(AgentExecutionEvent.error(error)));
	// }).doOnComplete(() -> {
	// log.info("拒绝后继续执行完成: sessionId={}", state.getSessionId());
	// state.getSink().tryEmitComplete();
	// state.setClosed(true);
	// sessions.remove(state.getSessionId());
	// }).subscribe();
	//
	// state.setCurrentExecution(execution);
	// }

	/**
	 * 终止对话：直接结束任务
	 */
	private void terminateTurn(TurnState state, String reason) {
		log.info("终止对话: turnId={}, reason={}", state.getTurnId(), reason);

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
		turns.remove(state.getTurnId());

		log.info("会话已终止: turnId={}", state.getTurnId());
	}

	/**
	 * 将AgentExecutionEvent转换为SSE事件
	 */
	public ServerSentEvent<AgentExecutionEvent> toSSE(AgentExecutionEvent event) {
		return ServerSentEvent.<AgentExecutionEvent>builder()
			.id(CommonUtils.getTraceId("sse-"))
			.event(event.getType() != null ? event.getType().toString() : "message")
			.data(event)
			.build();
	}

}
