package com.ai.agent.real.entity.agent.context.reactplus;

import com.ai.agent.real.contract.agent.context.AgentContextAble;
import com.ai.agent.real.contract.model.callback.ToolApprovalCallback;
import com.ai.agent.real.contract.model.logging.Traceable;
import com.ai.agent.real.contract.model.message.AgentMessage;
import com.ai.agent.real.entity.agent.context.ReActAgentContext;
import org.springframework.ai.model.ModelOptionsUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * ReActPlusAgentContext 类定义了 ReActPlus 的上下文信息。
 *
 * @author han
 * @time 2025/10/28 23:38
 */
public class ReActPlusAgentContext implements AgentContextAble<ReActPlusAgentContextMeta> {

	/**
	 * 组合的追踪信息对象（推荐使用）
	 */
	private Traceable trace;

	/**
	 * 工具执行的参数
	 */
	private Map<String, Object> toolArgs;

	/**
	 * 对话历史记录 - 使用结构化消息列表
	 */
	private List<AgentMessage> messageHistory;

	/**
	 * 当前迭代轮次
	 */
	private int currentIteration;

	private String task;

	/**
	 * 任务完成状态
	 */
	private AtomicBoolean taskCompleted;

	/**
	 * 工具审批回调（用于工具执行需要审批时通知上层）
	 */
	private ToolApprovalCallback toolApprovalCallback;

	/**
	 * meta 元数据
	 */
	private ReActPlusAgentContextMeta meta;

	/**
	 * 构造函数
	 */
	public ReActPlusAgentContext(Traceable trace) {
		this.trace = trace;
		this.toolArgs = Map.of();
		this.messageHistory = new CopyOnWriteArrayList<>();
		this.currentIteration = 0;
		this.taskCompleted = new AtomicBoolean(false);
		this.toolApprovalCallback = ToolApprovalCallback.NOOP;
		this.meta = null;
	}

	/**
	 * @return Get all historical messages of this agent
	 */
	@Override
	public List<AgentMessage> getMessageHistory() {
		return List.of();
	}

	/**
	 * 添加消息到对话历史
	 * @param message
	 */
	@Override
	public ReActAgentContext addMessage(AgentMessage message) {
		return null;
	}

	/**
	 * 添加多条消息到对话历史
	 * @param messages
	 */
	@Override
	public ReActAgentContext addMessages(List<AgentMessage> messages) {
		return null;
	}

	/**
	 * @return
	 */
	@Override
	public int getCurrentIteration() {
		return 0;
	}

	/**
	 * @param currentIteration
	 * @return
	 */
	@Override
	public ReActAgentContext setCurrentIteration(int currentIteration) {
		return null;
	}

	/**
	 * 获取任务完成状态
	 */
	@Override
	public boolean isTaskCompleted() {
		return false;
	}

	/**
	 * 设置任务完成状态
	 * @param taskCompleted
	 */
	@Override
	public void setTaskCompleted(Boolean taskCompleted) {
		this.taskCompleted.set(taskCompleted);
	}

	@Override
	public void setTaskCompleted(AtomicBoolean taskCompleted) {
		this.taskCompleted = taskCompleted;
	}

	@Override
	public AtomicBoolean getTaskCompleted() {
		return taskCompleted;
	}

	/**
	 * @return
	 */
	@Override
	public Map<String, Object> getToolArgs() {
		return Map.of();
	}

	/**
	 * 获取结构化工具参数
	 * @param toolArgsClass
	 * @return 结构化工具参数
	 */
	@Override
	public <T> T getStructuralToolArgs(Class<T> toolArgsClass) {
		return ModelOptionsUtils.mapToClass(toolArgs, toolArgsClass);
	}

	/**
	 * @param toolArgs
	 */
	@Override
	public void setToolArgs(Map<String, Object> toolArgs) {

	}

	/**
	 * @return
	 */
	@Override
	public String getSessionId() {
		return "";
	}

	/**
	 * @param sessionId
	 * @return
	 */
	@Override
	public Traceable setSessionId(String sessionId) {
		return null;
	}

	/**
	 * @return
	 */
	@Override
	public String getTurnId() {
		return "";
	}

	/**
	 * @param turnId
	 * @return
	 */
	@Override
	public Traceable setTurnId(String turnId) {
		return null;
	}

	/**
	 * @return
	 */
	@Override
	public LocalDateTime getStartTime() {
		return null;
	}

	/**
	 * @param startTime
	 * @return
	 */
	@Override
	public Traceable setStartTime(LocalDateTime startTime) {
		return null;
	}

	/**
	 * @return
	 */
	@Override
	public LocalDateTime getEndTime() {
		return null;
	}

	/**
	 * @param endTime
	 * @return
	 */
	@Override
	public Traceable setEndTime(LocalDateTime endTime) {
		return null;
	}

	/**
	 * @return
	 */
	@Override
	public String getSpanId() {
		return "";
	}

	/**
	 * @param spanId
	 * @return
	 */
	@Override
	public Traceable setSpanId(String spanId) {
		return null;
	}

	/**
	 * @return
	 */
	@Override
	public String getNodeId() {
		return "";
	}

	/**
	 * @param nodeId
	 * @return
	 */
	@Override
	public Traceable setNodeId(String nodeId) {
		return null;
	}

	/**
	 * @return
	 */
	@Override
	public String getAgentId() {
		return "";
	}

	/**
	 * @param agentId
	 * @return
	 */
	@Override
	public Traceable setAgentId(String agentId) {
		return null;
	}

	@Override
	public String getTask() {
		return task;
	}

	@Override
	public void setTask(String task) {
		this.task = task;
	}

	@Override
	public ToolApprovalCallback getToolApprovalCallback() {
		return toolApprovalCallback;
	}

	@Override
	public void setToolApprovalCallback(ToolApprovalCallback toolApprovalCallback) {
		this.toolApprovalCallback = toolApprovalCallback;
	}

	@Override
	public void setMessageHistory(List<AgentMessage> messageHistory) {
		this.messageHistory = messageHistory;
	}

	/**
	 * @return
	 */
	@Override
	public ReActPlusAgentContextMeta getMetadata() {
		return this.meta;
	}

	/**
	 * @param metadata
	 */
	@Override
	public void setMetadata(Object metadata) {

		this.meta = (ReActPlusAgentContextMeta) metadata;
	}

}
