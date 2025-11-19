package com.ai.agent.real.contract.agent.service;

import com.ai.agent.real.contract.agent.IAgentStrategy;
import com.ai.agent.real.contract.agent.context.AgentContextAble;
import com.ai.agent.real.contract.model.interaction.*;
import com.ai.agent.real.contract.model.protocol.AgentExecutionEvent;
import lombok.Data;
import org.springframework.http.codec.ServerSentEvent;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.time.LocalDateTime;

/**
 * Agent会话管理中心 负责管理每个会话的Sink、上下文和待审批工具
 *
 * 核心职责： 1. 管理SSE连接的Sink生命周期 2. 维护会话上下文和执行状态 3. 支持工具审批中断和恢复
 *
 * @author han
 * @time 2025/10/22 15:45
 */
public interface IAgentTurnManagerService {

	/**
	 * 订阅会话的SSE流
	 * @param sessionId 会话ID
	 * @param message 用户消息
	 * @param context 执行上下文
	 * @return SSE事件流
	 */
	Flux<ServerSentEvent<AgentExecutionEvent>> subscribe(String sessionId, String message, AgentContextAble context);

	/**
	 * 处理用户响应（通用方法）
	 * @param turnId 会话ID
	 * @param response 用户响应
	 */
	void handleInteractionResponse(String turnId, InteractionResponse response);

	/**
	 * 关闭会话
	 * @param turnId 会话ID
	 */
	void closeTurn(String turnId);

	/**
	 * deep copy
	 */
	IAgentTurnManagerService of(IAgentStrategy agentStrategy);

	TurnState getTurnState(String turnId);

	/**
	 * 会话状态
	 */
	@Data
	class TurnState {

		/**
		 * 会话ID
		 */
		private String turnId;

		/**
		 * SSE推送Sink
		 */
		private Sinks.Many<ServerSentEvent<AgentExecutionEvent>> sink;

		/**
		 * 以后可能会扩展成List
		 */
		private Sinks.One<InteractionResponse> pendingApproval;

		/**
		 * 执行上下文
		 */
		private AgentContextAble<?> context;

		/**
		 * 当前执行的订阅句柄
		 */
		private Disposable currentExecution;

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
