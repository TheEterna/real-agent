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

	public static AgentExecutionEvent started(String message) {
		return new AgentExecutionEvent(EventType.STARTED, message, null, null);
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

	public static AgentExecutionEvent toolElicitation(Traceable traceInfo, String message, Object data) {
		return new AgentExecutionEvent(EventType.TOOL_APPROVAL, message, data, traceInfo);
	}

	public static AgentExecutionEvent agentSelected(Traceable traceInfo, String message) {
		return new AgentExecutionEvent(EventType.AGENT_SELECTED, message, null, traceInfo);
	}

	public static AgentExecutionEvent thinking(Traceable traceInfo, String thought) {
		return new AgentExecutionEvent(EventType.THINKING, thought, null, traceInfo);
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

	public static AgentExecutionEvent partialResult(Traceable traceInfo, String result) {
		return new AgentExecutionEvent(EventType.PARTIAL_RESULT, result, null, traceInfo);
	}

	public static AgentExecutionEvent collaborating(Traceable traceInfo, String message) {
		return new AgentExecutionEvent(EventType.COLLABORATING, message, null, traceInfo);
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

		/**
		 * Agent执行行动
		 */
		ACTION,
		/**
		 * Agent观察结果
		 */
		OBSERVING,
		/**
		 * Agent协作
		 */
		COLLABORATING,
		/**
		 * 部分结果
		 */
		PARTIAL_RESULT,
		/**
		 * 执行完成
		 */
		DONE,

		/**
		 * ==================================== 系统级别事件
		 * ====================================
		 */

		/**
		 * 开始执行
		 */
		STARTED,
		/**
		 * 执行进度
		 */
		PROGRESS,
		/**
		 * Agent选择
		 */
		AGENT_SELECTED,
		/**
		 * Agent思考
		 */
		THINKING,
		/**
		 * 执行
		 */
		EXECUTING,
		/**
		 * 错误
		 */
		ERROR,
		/**
		 * 执行完成，有警告
		 */
		DONEWITHWARNING,
		/**
		 * 执行完成, all agents completed, notice client close sse connection
		 */
		COMPLETED,

		/**
		 * ==================================== 工具相关事件(属于系统事件一种)
		 * ====================================
		 */
		/**
		 * 工具调用
		 */
		TOOL,
		/**
		 * 工具审批
		 */
		TOOL_APPROVAL,

		/**
		 * 工具请求结构化数据, This is a cutting-edge technology, and according to MCP officials,
		 * it may be developed in subsequent versions and adjustments may also be made in
		 * this system
		 */
		TOOL_ELICITATION,
		/**
		 * 工具请求调用大模型,
		 */
		TOOL_SAMPLING,
		/**
		 * 工具日志事件 The protocol follows the standard syslog severity levels specified in
		 * RFC 5424: Level Description Example Use Case debug Detailed debugging
		 * information Function entry/exit points info General informational messages
		 * Operation progress updates notice Normal but significant events Configuration
		 * changes warning Warning conditions Deprecated feature usage error Error
		 * conditions Operation failures critical Critical conditions System component
		 * failures alert Action must be taken immediately Data corruption detected
		 * emergency System is unusable Complete system failure
		 *
		 * 该协议遵循 RFC 5424 中指定的标准 syslog 严重性级别： 水平 描述 示例用例 调试 详细的调试信息 功能入口/出口点 信息 一般信息消息
		 * 运营进度更新 通知 正常但重大事件 配置更改 警告 警告条件 已弃用的功能用法 错误 错误条件 作失败 危急 危急条件 系统组件故障 警报 必须立即采取行动
		 * 检测到数据损坏 紧急 系统无法使用 系统完全故障
		 */
		TOOL_LOG,

	}

}
