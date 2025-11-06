package com.ai.agent.real.application.agent.dispatcher;

import com.ai.agent.real.contract.agent.IAgentStrategy;
import com.ai.agent.real.contract.agent.IAgentDispatcher;

import java.util.Map;

/**
 * @author han
 * @time 2025/10/27 20:35
 */
public class DefaultAgentDispatcher implements IAgentDispatcher {

	private Map<String, IAgentStrategy> agentStrategyMap;

	public DefaultAgentDispatcher(Map<String, IAgentStrategy> agentStrategyMap) {
		this.agentStrategyMap = agentStrategyMap;
	}

	/**
	 * 根据name去匹配Agent策略
	 * @param name 匹配用的 name
	 */
	@Override
	public IAgentStrategy getAgentStrategyByName(String name) {
		if (!agentStrategyMap.containsKey(name.trim())) {
			throw new RuntimeException("IAgentStrategy " + name + " not found");
		}

		return agentStrategyMap.get(name.trim());
	}

}
