package com.ai.agent.real.contract.model.callback;

import com.ai.agent.real.contract.agent.context.AgentContextAble;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * 工具审批回调接口 用于在工具执行需要审批时，通知上层处理
 *
 * 设计目的： 1. 解耦FluxUtils和AgentSessionHub 2. 保持模块依赖关系清晰 3. 支持不同的审批处理策略
 *
 * @author han
 * @time 2025/10/22 15:50
 */
@FunctionalInterface
public interface ToolApprovalCallback {

	/**
	 * 请求工具审批
	 * @param sessionId 会话ID
	 * @param toolCallId 工具调用ID
	 * @param toolName 工具名称
	 * @param toolArgs 工具参数
	 * @param context 执行上下文
	 */
	void requestApproval(String sessionId, String toolCallId, String toolName, Map<String, Object> toolArgs,
			AgentContextAble context);

	/**
	 * 空实现（不需要审批）
	 */
	ToolApprovalCallback NOOP = (sessionId, toolCallId, toolName, toolArgs, context) -> {
		// just log
		final Logger log = LoggerFactory.getLogger(ToolApprovalCallback.class);
		log.debug("No need to approve tool: {}", toolName);
	};

}
