package com.ai.agent.real.contract.spec;

import com.ai.agent.real.contract.spec.logging.*;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Agent执行结果封装类
 *
 * @author han
 * @time 2025/9/6 23:45
 */
@Data
@Accessors(chain = true)
public class AgentResult {

	/**
	 * 执行是否成功
	 */
	private boolean success;

	/**
	 * 执行结果内容
	 */
	private String result;

	/**
	 * 错误信息（如果执行失败）
	 */
	private String errorMessage;

	/**
	 * 执行耗时（毫秒）
	 */
	private long executionTime;

	/**
	 * 执行开始时间
	 */
	private LocalDateTime startTime;

	/**
	 * 执行结束时间
	 */
	private LocalDateTime endTime;

	/**
	 * 执行Agent的ID
	 */
	private String agentId;

	/**
	 * 追踪信息
	 */
	private Traceable traceInfo;

	/**
	 * 扩展元数据
	 */
	private Map<String, Object> metadata;

	/**
	 * 创建成功结果
	 */
	public static AgentResult success(String result, String agentId) {
		return new AgentResult().setSuccess(true).setResult(result).setAgentId(agentId).setEndTime(LocalDateTime.now());
	}

	/**
	 * 创建失败结果
	 */
	public static AgentResult failure(String errorMessage, String agentId) {
		return new AgentResult().setSuccess(false)
			.setErrorMessage(errorMessage)
			.setAgentId(agentId)
			.setEndTime(LocalDateTime.now());
	}

}
