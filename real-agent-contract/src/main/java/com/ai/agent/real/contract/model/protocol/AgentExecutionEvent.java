package com.ai.agent.real.contract.model.protocol;

import com.ai.agent.real.contract.model.logging.*;
import org.springframework.ai.chat.messages.ToolResponseMessage.*;

import java.util.*;

/**
 * Agent执行事件 用于流式执行中传递实时状态和结果
 */
public class AgentExecutionEvent extends TraceInfo {

	private final EventType type;

	/**
	 * message, 当事件类型为tool时, message为 toolName
	 */
	private final String message;

	private final Object data;

	private Map<String, Object> meta;


	private AgentExecutionEvent(EventType type, String message, Object data, Traceable traceInfo) {
		this.type = type;
		this.message = message;
		this.data = data;

		if (traceInfo != null) {
			this.setSessionId(traceInfo.getSessionId());
			this.setTurnId(traceInfo.getTurnId());
			this.setStartTime(traceInfo.getStartTime());
			this.setEndTime(traceInfo.getEndTime());
			this.setSpanId(traceInfo.getSpanId());
			this.setNodeId(traceInfo.getNodeId());
			this.setAgentId(traceInfo.getAgentId());
		}
	}

	public static AgentExecutionEvent common(EventType type, Traceable traceInfo, String message) {
		return new AgentExecutionEvent(type, message, null, traceInfo);
	}

	public static AgentExecutionEvent started(String message) {
		return new AgentExecutionEvent(EventType.STARTED, message, null, null);
	}

	public static AgentExecutionEvent started(Traceable traceInfo, String message) {
		return new AgentExecutionEvent(EventType.STARTED, message, null, traceInfo);
	}

	public static AgentExecutionEvent progress(Traceable traceInfo, String message, Object data) {

		// cause enter each node before entering each node, so we can safely assign values
		// here.
		// cause 进入每个节点前都会对context进行 copy, filter操作, 所以这里可以大胆赋值

		traceInfo.setNodeId(traceInfo.getNodeId());
		traceInfo.setAgentId("progress");
		return new AgentExecutionEvent(EventType.PROGRESS, message, data, traceInfo);
	}

	public static AgentExecutionEvent toolApproval(Traceable traceInfo, String message, Object data) {
		return new AgentExecutionEvent(EventType.TOOL_APPROVAL, message, data, traceInfo);
	}

	/**
	 * 通用交互请求事件 用于请求用户交互（工具审批、缺少信息、用户确认等）
	 */
	public static AgentExecutionEvent interaction(Traceable traceInfo, Object interactionRequest) {
		return new AgentExecutionEvent(EventType.INTERACTION, "用户交互请求", interactionRequest, traceInfo);
	}

	public static AgentExecutionEvent agentSelected(Traceable traceInfo, String message) {
		return new AgentExecutionEvent(EventType.AGENT_SELECTED, message, null, traceInfo);
	}

	public static AgentExecutionEvent initPlan(Traceable traceInfo, String message, Object data) {
		return new AgentExecutionEvent(EventType.INIT_PLAN, message, data, traceInfo);
	}

	public static AgentExecutionEvent updatePlan(Traceable traceInfo, String message, Object data) {
		return new AgentExecutionEvent(EventType.UPDATE_PLAN, message, data, traceInfo);
	}

	public static AgentExecutionEvent advancePlan(Traceable traceInfo, String message, Object data) {
		return new AgentExecutionEvent(EventType.ADVANCE_PLAN, message, data, traceInfo);
	}

	public static AgentExecutionEvent taskAnalysis(Traceable traceInfo, String message, Object data) {
		return new AgentExecutionEvent(EventType.TASK_ANALYSIS, message, data, traceInfo);
	}

	public static AgentExecutionEvent action(Traceable traceInfo, String action) {
		return new AgentExecutionEvent(EventType.ACTION, action, null, traceInfo);
	}

	public static AgentExecutionEvent tool(Traceable traceInfo, ToolResponse toolResponse, String message,
			Map<String, Object> meta) {
		return new AgentExecutionEvent(EventType.TOOL, message, toolResponse, traceInfo) {
			{
				setMeta(meta);
			}
		};
	}

	public static AgentExecutionEvent observing(Traceable traceInfo, String observation) {
		return new AgentExecutionEvent(EventType.OBSERVING, observation, null, traceInfo);
	}

	public static AgentExecutionEvent executing(Traceable traceInfo, String execution) {
		return new AgentExecutionEvent(EventType.EXECUTING, execution, null, traceInfo);
	}

	public static AgentExecutionEvent completed() {
		return new AgentExecutionEvent(EventType.COMPLETED, null, null, null);
	}

	public static AgentExecutionEvent doneWithWarning(Traceable traceInfo, String message) {
		return new AgentExecutionEvent(EventType.DONEWITHWARNING, message, null, null);
	}

	public static AgentExecutionEvent done(Traceable traceInfo, String message) {
		return new AgentExecutionEvent(EventType.DONE, message, null, null);
	}

	public static AgentExecutionEvent error(Throwable error) {
		return new AgentExecutionEvent(EventType.ERROR, error.getMessage(), null, null);
	}

	public static AgentExecutionEvent error(String errorMessage) {
		return new AgentExecutionEvent(EventType.ERROR, errorMessage, null, null);
	}

	// Getters
	public EventType getType() {
		return type;
	}

	public String getMessage() {
		return message;
	}

	public Object getData() {
		return data;
	}

	public Map<String, Object> getMeta() {
		return meta;
	}

	public void setMeta(Map<String, Object> meta) {
		this.meta = meta;
	}

	public enum EventType {

		STARTED, // 开始执行
		PROGRESS, // 执行进度
		AGENT_SELECTED, // Agent选择
		THINKING, // Agent思考中
		ACTION, // Agent执行行动
		ACTING, // 执行行动
		OBSERVING, // Agent观察结果
		COLLABORATING, // Agent协作
		PARTIAL_RESULT, // 部分结果
		DONE, // 执行完成
		EXECUTING, // 执行中
		ERROR, // 执行错误
		TOOL, // 工具调用
		DONEWITHWARNING, // 执行完成，有警告
		TOOL_APPROVAL, // 工具审批
		INTERACTION, // 通用交互请求（工具审批、缺少信息、用户确认等）
		COMPLETED, // 执行完成, all agents completed, notice client close sse connection

		TASK_ANALYSIS, // 任务难度分析

		THOUGHT, // 思维链
		INIT_PLAN, // 初始化 plan
		UPDATE_PLAN, // 更新 plan
		ADVANCE_PLAN, // plan 下一步

	}

}
