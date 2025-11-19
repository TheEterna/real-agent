package com.ai.agent.real.contract.agent.context;

import com.ai.agent.real.contract.model.logging.Traceable;
import com.ai.agent.real.contract.model.message.AgentMessage;
import org.springframework.ai.model.ModelOptionsUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * M 代表的是 meta 的 类型 T 代表的是 tool input schema 的类型
 *
 * @author han
 * @time 2025/10/28 23:05
 */
public interface AgentContextAble<M> extends Traceable {

	List<AgentMessage> getMessageHistory();

	/**
	 * 添加消息到对话历史
	 */
	AgentContextAble addMessage(AgentMessage message);

	/**
	 * 添加多条消息到对话历史
	 */
	AgentContextAble addMessages(List<AgentMessage> messages);

	int getCurrentIteration();

	void setCurrentIteration(int currentIteration);

	/**
	 * 获取任务完成状态
	 */
	boolean isTaskCompleted();

	/**
	 * 设置任务完成状态
	 */
	void setTaskCompleted(Boolean taskCompleted);

	/**
	 * 设置任务完成状态
	 */
	void setTaskCompleted(AtomicBoolean taskCompleted);

	AtomicBoolean getTaskCompleted();

	Map<String, Object> getToolArgs();

	/**
	 * 获取结构化工具参数
	 * @return 结构化工具参数
	 */
	default <T> T getStructuralToolArgs(Class<T> toolArgsClass) {
		return ModelOptionsUtils.mapToClass(getToolArgs(), toolArgsClass);
	}

	void setToolArgs(Map<String, Object> toolArgs);

	/**
	 * get original task（just user input）
	 */
	String getTask();

	void setTask(String task);

	void setMessageHistory(List<AgentMessage> messageHistory);

	default void setMetadata(Object metadata) {
		throw new UnsupportedOperationException("not support metadata");
	}

	default M getMetadata() {
		throw new UnsupportedOperationException("not support metadata");
	}

	Traceable getTrace();

	default String getSessionId() {
		return getTrace().getSessionId();
	}

	default Traceable setSessionId(String sessionId) {
		return getTrace().setSessionId(sessionId);
	}

	default String getTurnId() {
		return getTrace().getTurnId();
	}

	default Traceable setTurnId(String turnId) {
		return getTrace().setTurnId(turnId);
	}

	default LocalDateTime getStartTime() {
		return getTrace().getStartTime();
	}

	default Traceable setStartTime(LocalDateTime startTime) {
		return getTrace().setStartTime(startTime);
	}

	default LocalDateTime getEndTime() {
		return getTrace().getEndTime();
	}

	default Traceable setEndTime(LocalDateTime endTime) {
		return getTrace().setEndTime(endTime);
	}

	default String getSpanId() {
		return getTrace().getSpanId();
	}

	default Traceable setSpanId(String spanId) {
		return getTrace().setSpanId(spanId);
	}

	default String getNodeId() {
		return getTrace().getNodeId();
	}

	default Traceable setNodeId(String nodeId) {
		return getTrace().setNodeId(nodeId);
	}

	default String getAgentId() {
		return getTrace().getAgentId();
	}

	default Traceable setAgentId(String agentId) {
		return getTrace().setAgentId(agentId);
	}

}
