package com.ai.agent.real.contract.service;

import com.ai.agent.real.contract.model.protocol.AgentExecutionEvent;
import reactor.core.publisher.Flux;

/**
 * Elicitation 事件提供者接口
 *
 * 用于提供 elicitation 事件流，遵循依赖倒置原则 该接口定义在 contract 模块，避免 core 模块依赖 web 模块
 *
 * @time 2025-01-17
 * @author han
 */
public interface ElicitationEventProvider {

	/**
	 * 获取 elicitation 事件流 当 MCP 工具需要用户输入时，会通过此流发送 TOOL_ELICITATION 事件
	 * @return elicitation 事件流
	 */
	Flux<AgentExecutionEvent> getElicitationEventStream();

}
