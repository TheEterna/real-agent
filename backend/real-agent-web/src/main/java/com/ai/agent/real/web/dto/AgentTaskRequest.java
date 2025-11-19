package com.ai.agent.real.web.dto;

import lombok.Data;

/**
 * Agent任务请求DTO
 *
 * @author han
 * @time 2025/9/7 00:35
 */
@Data
public class AgentTaskRequest {

	/**
	 * 任务描述
	 */
	private String task;

	/**
	 * 指定使用的策略名称（可选）
	 */
	private String strategy;

	/**
	 * 用户ID（可选）
	 */
	private String userId;

	/**
	 * 任务超时时间（毫秒，可选）
	 */
	private Long timeoutMs;

	/**
	 * 任务优先级（可选）
	 */
	private Integer priority;

	/**
	 * 额外的任务参数（可选）
	 */
	private java.util.Map<String, Object> parameters;

}
