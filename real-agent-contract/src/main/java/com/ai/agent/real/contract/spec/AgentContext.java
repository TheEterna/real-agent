package com.ai.agent.real.contract.spec;

import com.ai.agent.real.contract.spec.logging.*;
import com.ai.agent.real.contract.spec.message.*;
import lombok.*;
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
@Data
@Accessors(chain = true)
public class AgentContext<T> implements Traceable {

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
	private List<AgentMessage> conversationHistory;

	/**
	 * 当前迭代轮次
	 */
	private int currentIteration = 0;

	/**
	 * 任务完成状态
	 */
	private AtomicBoolean taskCompleted = new AtomicBoolean(false);

	/**
	 * 构造函数
	 */
	public AgentContext(Traceable trace) {
		this.toolArgs = null;
		this.trace = trace;
		this.conversationHistory = new CopyOnWriteArrayList<>();
	}

	public List<AgentMessage> getConversationHistory() {
		return conversationHistory;
	}

	/**
	 * 添加消息到对话历史
	 */
	public AgentContext addMessage(AgentMessage message) {
		message.setContent("[" + message.getSenderId() + "] " + message.getText());
		this.conversationHistory.add(message);
		return this;
	}

	/**
	 * 添加多条消息到对话历史
	 */
	public AgentContext addMessages(List<AgentMessage> messages) {
		this.conversationHistory.addAll(messages);
		return this;
	}

	public int getCurrentIteration() {
		return currentIteration;
	}

	public AgentContext setCurrentIteration(int currentIteration) {
		this.currentIteration = currentIteration;
		return this;
	}

	/**
	 * 获取任务完成状态
	 */
	public boolean isTaskCompleted() {
		return taskCompleted.get();
	}

	/**
	 * 设置任务完成状态
	 */
	public AgentContext setTaskCompleted(AtomicBoolean taskCompleted) {
		this.taskCompleted = taskCompleted;
		return this;
	}

	/**
	 * 设置任务完成状态
	 */
	public AgentContext setTaskCompleted(Boolean taskCompleted) {
		this.taskCompleted.set(taskCompleted);
		return this;
	}

	/**
	 * 链接任务完成标记引用： 将本上下文的 taskCompleted 原子布尔替换为入参上下文的同一引用， 以便多个阶段上下文对任务完成状态进行共享与联动。
	 * 注意：这是引用共享而非值拷贝。
	 */
	public AgentContext linkTaskCompletedFrom(AgentContext<?> other) {
		if (other != null && other.taskCompleted != null) {
			this.taskCompleted = other.taskCompleted;
		}
		return this;
	}

	public Traceable getTrace() {
		return trace;
	}

	public AgentContext setTrace(Traceable trace) {
		this.trace = trace;
		return this;
	}

	@Override
	public String getSessionId() {
		return trace.getSessionId();
	}

	@Override
	public AgentContext setSessionId(String sessionId) {
		this.trace.setSessionId(sessionId);
		return this;
	}

	@Override
	public String getTraceId() {
		return trace.getTraceId();
	}

	@Override
	public AgentContext setTraceId(String traceId) {
		this.trace.setTraceId(traceId);
		return this;
	}

	@Override
	public LocalDateTime getStartTime() {
		return trace.getStartTime();
	}

	@Override
	public AgentContext setStartTime(LocalDateTime startTime) {
		this.trace.setStartTime(startTime);
		return this;
	}

	@Override
	public LocalDateTime getEndTime() {
		return trace.getEndTime();
	}

	@Override
	public AgentContext setEndTime(LocalDateTime endTime) {
		this.trace.setEndTime(endTime);
		return this;
	}

	public boolean isEnd() {
		return trace.getEndTime() != null;
	}

	@Override
	public String getSpanId() {
		return trace.getSpanId();
	}

	@Override
	public AgentContext setSpanId(String spanId) {
		this.trace.setSpanId(spanId);
		return this;
	}

	@Override
	public String getNodeId() {
		return trace.getNodeId();
	}

	@Override
	public Traceable setNodeId(String nodeId) {
		this.trace.setNodeId(nodeId);
		return this;
	}

	@Override
	public String getAgentId() {
		return trace.getAgentId();
	}

	@Override
	public Traceable setAgentId(String agentId) {
		this.trace.setAgentId(agentId);
		return this;
	}

	public Map<String, Object> getToolArgs() {
		return toolArgs;
	}

	/**
	 * 获取结构化工具参数
	 * @return 结构化工具参数
	 */
	public <T> T getStructuralToolArgs(Class<T> toolArgsClass) {
		return ModelOptionsUtils.mapToClass(toolArgs, toolArgsClass);
	}

	public void setToolArgs(Map<String, Object> toolArgs) {
		this.toolArgs = toolArgs;
	}

	/**
	 * 创建一个包含工具参数的 AgentContext 对象
	 * @param toolArgs 工具参数
	 * @return 包含工具参数的 AgentContext 对象
	 */
	public static AgentContext of(Map<String, Object> toolArgs, Traceable trace) {
		AgentContext agentContext = new AgentContext(trace);
		agentContext.setToolArgs(toolArgs);
		return agentContext;
	}

}
