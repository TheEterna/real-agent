package com.ai.agent.real.contract.agent;

/**
 * Agent 策略分发器
 *
 * @author han
 * @time 2025/10/27 20:29
 */
public interface IAgentDispatcher {

	/**
	 * 根据name去匹配Agent策略
	 * @param name 匹配用的 name
	 */
	AgentStrategy getAgentStrategyByName(String name);

}