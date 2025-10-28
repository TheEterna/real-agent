package com.ai.agent.real.contract.agent.context;

import com.ai.agent.real.contract.model.callback.ToolApprovalCallback;
import com.ai.agent.real.contract.model.logging.Traceable;
import com.ai.agent.real.contract.model.message.AgentMessage;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author han
 * @time 2025/10/28 23:05
 */
public interface AgentContextAble extends Traceable {

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

	AgentContextAble setCurrentIteration(int currentIteration);

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
	<T> T getStructuralToolArgs(Class<T> toolArgsClass);

	void setToolArgs(Map<String, Object> toolArgs);

	/**
	 * get original task（just user input）
	 */
	String getTask();

	void setTask(String task);

	/**
	 * @return tool approval callback
	 */
	ToolApprovalCallback getToolApprovalCallback();

	/**
	 * set tool approval callback
	 */
	void setToolApprovalCallback(ToolApprovalCallback toolApprovalCallback);

	void setMessageHistory(List<AgentMessage> messageHistory);

}
