package com.ai.agent.real.contract.model.context;

import com.ai.agent.real.contract.model.interaction.InteractionRequest;
import com.ai.agent.real.contract.model.interaction.InteractionResponse;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * ReAct执行恢复点 用于在工具审批后恢复执行
 *
 * 设计目的： 1. 保存暂停时的执行状态 2. 支持从特定阶段恢复执行 3. 维护上下文连续性
 *
 * @author han
 * @time 2025/10/22 16:30
 */
@Data
public class ResumePoint {

	/**
	 * 恢复点ID（通常是 toolCallId）
	 */
	private String resumeId;

	/**
	 * 会话ID
	 */
	private String sessionId;

	/**
	 * 当前迭代次数
	 */
	private int currentIteration;

	/**
	 * 暂停的阶段
	 */
	private ReActStage pausedStage;

	/**
	 * 交互请求（通用的中断信息）
	 */
	private InteractionRequest interactionRequest;

	/**
	 * 用户响应
	 */
	private InteractionResponse userResponse;

	/**
	 * 执行上下文（深拷贝）
	 */
	private AgentContext context;

	/**
	 * 原始任务描述
	 */
	private String originalTask;

	/**
	 * 创建时间
	 */
	private LocalDateTime createdAt;

	/**
	 * ReAct执行阶段
	 */
	public enum ReActStage {

		/**
		 * 思考阶段
		 */
		THINKING,

		/**
		 * 行动阶段
		 */
		ACTION,

		/**
		 * 观察阶段
		 */
		OBSERVATION

	}

}
