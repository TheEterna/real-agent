package com.ai.agent.real.contract.model.context.reactplus;

import com.ai.agent.real.contract.agent.context.AgentContextAble;
import com.ai.agent.real.contract.model.logging.Traceable;
import com.ai.agent.real.contract.model.message.AgentMessage;
import org.springframework.ai.model.ModelOptionsUtils;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

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
	private AtomicInteger currentIteration;

	private String task;

	/**
	 * 任务完成状态
	 */
	private AtomicBoolean taskCompleted;

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
		this.currentIteration = new AtomicInteger(0);
		this.taskCompleted = new AtomicBoolean(false);
		this.meta = new ReActPlusAgentContextMeta();
	}

	/**
	 * @return Get all historical messages of this agent
	 */
	@Override
	public List<AgentMessage> getMessageHistory() {
		return this.messageHistory;
	}

	/**
	 * 添加消息到对话历史
	 * @param message
	 */
	@Override
	public ReActPlusAgentContext addMessage(AgentMessage message) {
		message.setContent(message.getText());
		this.messageHistory.add(message);
		return this;
	}

	/**
	 * 添加多条消息到对话历史
	 * @param messages
	 */
	@Override
	public ReActPlusAgentContext addMessages(List<AgentMessage> messages) {
		this.messageHistory.addAll(messages);
		return this;
	}

	/**
	 * @return
	 */
	@Override
	public int getCurrentIteration() {
		return this.currentIteration.get();
	}

	/**
	 * @param currentIteration
	 * @return
	 */
	@Override
	public void setCurrentIteration(int currentIteration) {
		this.currentIteration.set(currentIteration);
	}

	/**
	 * 获取任务完成状态
	 */
	@Override
	public boolean isTaskCompleted() {
		return this.taskCompleted.get();
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
		return this.taskCompleted;
	}

	/**
	 * @return
	 */
	@Override
	public Map<String, Object> getToolArgs() {
		return this.toolArgs;
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
		this.toolArgs = toolArgs;
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

	@Override
	public Traceable getTrace() {
		return this.trace;
	}

	/**
	 * @param metadata
	 */
	@Override
	public void setMetadata(Object metadata) {

		this.meta = (ReActPlusAgentContextMeta) metadata;
	}

}
