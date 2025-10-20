package com.ai.agent.real.contract.model.agent;

import com.ai.agent.real.contract.model.*;
import com.ai.agent.real.contract.model.context.*;
import com.ai.agent.real.contract.model.protocol.*;
import reactor.core.publisher.*;

import java.util.*;

/**
 * Agent策略接口 定义了不同Agent协作模式的统一规范
 *
 * 支持两种模式： 1. 流式执行：通过 executeStream() 返回事件流 2. Sinks 扩展：通过 getEventSink() 允许外部向事件流注入事件
 *
 * Sinks 设计思想： - Strategy 不仅是事件的生产者，也应该暴露 Sink 让其他模块能推送事件 - 这样可以实现强大的扩展性，例如 MCP 工具的
 * elicitation 请求可以直接注入事件流 - 遵循 Reactive 编程范式，背靠 Sinks 而不是单纯的流
 *
 * @author han
 * @time 2025/9/6 23:40
 */
public interface AgentStrategy {

	/**
	 * 获取策略名称
	 */
	default String getStrategyName() {
		return this.getClass().getSimpleName();
	}

	/**
	 * 获取策略描述
	 */
	default String getDescription() {
		return "Agent策略: " + getStrategyName();
	}

	/**
	 * 流式执行策略（推荐） 返回实时的执行进度和中间结果
	 * @param task 任务描述
	 * @param agents 可用的Agent列表
	 * @param context 执行上下文
	 * @return 流式执行结果
	 */
	Flux<AgentExecutionEvent> executeStream(String task, List<Agent> agents, AgentContext context);

	/**
	 * 同步执行策略（兼容旧版本）
	 * @param task 任务描述
	 * @param agents 可用的Agent列表
	 * @param context 工具执行上下文
	 * @return 执行结果
	 */
	AgentResult execute(String task, List<Agent> agents, AgentContext context);

	/**
	 * 获取事件 Sink（可选实现，基于 turnId）
	 *
	 * 允许外部模块向事件流注入事件，实现强大的扩展性 例如：MCP 工具可以通过此 Sink 发送 TOOL_ELICITATION 事件
	 *
	 * 注意： - 使用 turnId（单轮对话ID）而不是 sessionId - WebFlux 异步环境下 ThreadLocal 失效，必须显式传递 turnId -
	 * 每个 turnId 对应一个独立的 Sink，互不干扰
	 * @param turnId 单轮对话 ID
	 * @return 该 turn 对应的事件 Sink，如果不支持则返回 null
	 */
	default Sinks.Many<AgentExecutionEvent> getEventSink(String turnId) {
		return null;
	}

}
