package com.ai.agent.real.contract.agent.service;

import com.ai.agent.real.contract.agent.AgentStrategy;
import com.ai.agent.real.contract.agent.context.AgentContextAble;
import com.ai.agent.real.contract.agent.context.ResumePoint;
import com.ai.agent.real.contract.model.interaction.*;
import com.ai.agent.real.contract.model.protocol.AgentExecutionEvent;
import lombok.Data;
import org.springframework.http.codec.ServerSentEvent;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Agent会话管理中心 负责管理每个会话的Sink、上下文和待审批工具
 *
 * 核心职责： 1. 管理SSE连接的Sink生命周期 2. 维护会话上下文和执行状态 3. 支持工具审批中断和恢复
 *
 * @author han
 * @time 2025/10/22 15:45
 */
public interface IAgentSessionManagerService {

	/**
	 * 订阅会话的SSE流
	 * @param sessionId 会话ID
	 * @param message 用户消息
	 * @param context 执行上下文
	 * @return SSE事件流
	 */
	Flux<ServerSentEvent<AgentExecutionEvent>> subscribe(String sessionId, String message, AgentContextAble context);

	/**
	 * 请求用户交互（通用方法）
	 * @param sessionId 会话ID
	 * @param request 交互请求
	 */
	void requestInteraction(String sessionId, InteractionRequest request);

	/**
	 * 处理用户响应（通用方法）
	 * @param sessionId 会话ID
	 * @param response 用户响应
	 */
	void handleInteractionResponse(String sessionId, InteractionResponse response);

	/**
	 * 暂停会话执行，等待工具审批（便捷方法） 此方法会被 ToolApprovalCallback 调用
	 * @param sessionId 会话ID
	 * @param toolCallId 工具调用ID
	 * @param toolName 工具名称
	 * @param toolArgs 工具参数
	 * @param context 执行上下文
	 */
	void pauseForToolApproval(String sessionId, String toolCallId, String toolName, Map<String, Object> toolArgs,
			AgentContextAble context);

	/**
	 * 关闭会话
	 * @param sessionId 会话ID
	 */
	void closeSession(String sessionId);

	/**
	 * deep copy
	 */
	IAgentSessionManagerService of(AgentStrategy agentStrategy);

	/**
	 * 会话状态
	 */
	@Data
	class SessionState {

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
