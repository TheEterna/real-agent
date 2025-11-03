package com.ai.agent.real.application.agent.item.reactplus;

import com.ai.agent.real.contract.agent.Agent;
import com.ai.agent.real.contract.agent.context.AgentContextAble;
import com.ai.agent.real.contract.model.protocol.AgentExecutionEvent;
import reactor.core.publisher.Flux;

/**
 * the COT Agent of the ReActPlus framework
 *
 * @author han
 * @time 2025/11/4 01:36
 */
public class ThoughtAgent extends Agent {

	/**
	 * 流式执行任务
	 * @param task 任务描述
	 * @param context 执行上下文
	 * @return 流式执行结果
	 */
	@Override
	public Flux<AgentExecutionEvent> executeStream(String task, AgentContextAble context) {
		return null;
	}

}
