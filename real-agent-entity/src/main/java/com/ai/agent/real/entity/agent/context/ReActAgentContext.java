package com.ai.agent.real.entity.agent.context;

import com.ai.agent.real.contract.agent.context.AgentContextAble;
import com.ai.agent.real.contract.model.callback.ToolApprovalCallback;
import com.ai.agent.real.contract.model.logging.*;
import com.ai.agent.real.contract.model.message.*;
import lombok.experimental.*;
import org.springframework.ai.model.*;

import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

/**
 * AgentContext 类定义了工具执行的上下文信息。
 *
 * @author han
 * @time 2025/8/30 17:00
 */
@Accessors(chain = true)
public class ReActAgentContext implements AgentContextAble<Void> {

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
	 * 构造函数
	 */
	public ReActAgentContext(Traceable trace) {
		this.trace = trace;
		this.toolArgs = Map.of();
		this.messageHistory = new CopyOnWriteArrayList<>();
		this.currentIteration = 0;
		this.taskCompleted = new AtomicBoolean(false);
		this.toolApprovalCallback = ToolApprovalCallback.NOOP;
	}

	@Override
	public List<AgentMessage> getMessageHistory() {
		return messageHistory;
	}

	/**
	 * 添加消息到对话历史
	 */
	@Override
	public ReActAgentContext addMessage(AgentMessage message) {
		// message.setContent("[" + message.getSenderId() + "]: " + message.getText());
		message.setContent(message.getText());
		this.messageHistory.add(message);
		return this;
	}

	/**
	 * 添加多条消息到对话历史
	 */
	@Override
	public ReActAgentContext addMessages(List<AgentMessage> messages) {
		this.messageHistory.addAll(messages);
		return this;
	}

	@Override
	public int getCurrentIteration() {
		return currentIteration;
	}

	/**
	 * set current iteration
	 * @param currentIteration 当前迭代轮次
	 * @return agent context
	 */
	@Override
	public void setCurrentIteration(int currentIteration) {
		this.currentIteration = currentIteration;
	}

	/**
	 * 获取任务完成状态
	 */
	@Override
	public boolean isTaskCompleted() {
		return taskCompleted.get();
	}

	/**
	 * 设置任务完成状态
	 */
	@Override
	public void setTaskCompleted(Boolean taskCompleted) {
		this.taskCompleted.set(taskCompleted);
	}

	/**
	 * 设置任务完成状态
	 */
	@Override
	public void setTaskCompleted(AtomicBoolean taskCompleted) {
		this.taskCompleted = taskCompleted;
	}

	@Override
	public AtomicBoolean getTaskCompleted() {
		return taskCompleted;
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
	public Map<String, Object> getToolArgs() {
		return toolArgs;
	}

	/**
	 * 获取结构化工具参数
	 * @param toolArgsClass
	 * @return 结构化工具参数
	 */
	@Override
	public <T> T getStructuralToolArgs(Class<T> toolArgsClass) {
		return AgentContextAble.super.getStructuralToolArgs(toolArgsClass);
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
	public void setToolArgs(Map<String, Object> toolArgs) {
		this.toolArgs = toolArgs;
	}

	@Override
	public void setMessageHistory(List<AgentMessage> messageHistory) {
		this.messageHistory = messageHistory;
	}

    @Override
    public Traceable getTrace() {
        return this.trace;
    }

    /**
	 * 创建一个包含工具参数的 AgentContext 对象
	 * @param toolArgs 工具参数
	 * @return 包含工具参数的 AgentContext 对象
	 */
	public static ReActAgentContext of(Map<String, Object> toolArgs, Traceable trace) {
		ReActAgentContext agentContext = new ReActAgentContext(trace);
		agentContext.setToolArgs(toolArgs);
		return agentContext;
	}

}
