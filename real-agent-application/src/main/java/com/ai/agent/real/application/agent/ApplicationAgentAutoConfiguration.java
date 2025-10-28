package com.ai.agent.real.application.agent;

import com.ai.agent.real.application.agent.dispatcher.DefaultAgentDispatcher;
import com.ai.agent.real.application.agent.impl.ActionAgent;
import com.ai.agent.real.application.agent.impl.FinalAgent;
import com.ai.agent.real.application.agent.impl.ObservationAgent;
import com.ai.agent.real.application.agent.impl.ThinkingAgent;
import com.ai.agent.real.application.agent.session.AgentSessionManagerService;
import com.ai.agent.real.application.agent.strategy.ReActAgentStrategy;
import com.ai.agent.real.contract.agent.AgentStrategy;
import com.ai.agent.real.contract.agent.IAgentDispatcher;
import com.ai.agent.real.contract.agent.context.AgentMemory;
import com.ai.agent.real.contract.agent.context.AgentSessionConfig;
import com.ai.agent.real.contract.agent.service.IAgentSessionManagerService;
import com.ai.agent.real.contract.model.property.ContextZipMode;
import com.ai.agent.real.contract.model.property.ToolApprovalMode;
import com.ai.agent.real.contract.service.IPropertyService;
import com.ai.agent.real.contract.service.ToolService;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import java.util.Map;

/**
 * @author han
 * @time 2025/10/29 01:51
 */
public class ApplicationAgentAutoConfiguration {

	/**
	 * register default agent session manager
	 */
	@Bean
	public IAgentSessionManagerService agentSessionManagerService(AgentStrategy agentStrategy) {
		return new AgentSessionManagerService(agentStrategy);
	}

	/**
	 * 注册默认的Agent调度器
	 * @param agentStrategyMap Agent策略映射
	 * @return DefaultAgentDispatcher实例
	 */
	@Bean
	public IAgentDispatcher defaultAgentDispatcher(Map<String, AgentStrategy> agentStrategyMap) {
		return new DefaultAgentDispatcher(agentStrategyMap);
	}

	/**
	 * =================== Context part ===========================
	 */
	@Bean
	public AgentMemory agentMemory(IPropertyService realAgentProperties) {
		ContextZipMode zipMode = realAgentProperties.getContextZipMode();
		AgentSessionConfig defaultSessionConfig = new AgentSessionConfig(zipMode);
		return AgentMemory.builder().build(defaultSessionConfig);
	}

	/**
	 * =================== Agent part ===========================
	 */
	@Bean
	public ThinkingAgent thinkingAgent(ChatModel chatModel, ToolService toolService, IPropertyService propertyService) {
		ToolApprovalMode mode = propertyService.getToolApprovalMode();
		return new ThinkingAgent(chatModel, toolService, mode);
	}

	@Bean
	public ActionAgent actionAgent(ChatModel chatModel, ToolService toolService, IPropertyService propertyService) {
		ToolApprovalMode mode = propertyService.getToolApprovalMode();
		return new ActionAgent(chatModel, toolService, mode);
	}

	@Bean
	public ObservationAgent observationAgent(ChatModel chatModel, ToolService toolService,
			IPropertyService propertyService) {
		ToolApprovalMode mode = propertyService.getToolApprovalMode();
		return new ObservationAgent(chatModel, toolService, mode);
	}

	@Bean
	public FinalAgent finalAgent(ChatModel chatModel, ToolService toolService, IPropertyService propertyService) {
		return new FinalAgent(chatModel, toolService);
	}

	@Bean("reactAgentStrategy")
	public AgentStrategy reactAgentStrategy(ThinkingAgent thinkingAgent, ActionAgent actionAgent,
			ObservationAgent observationAgent, FinalAgent finalAgent, ToolService toolService) {
		return new ReActAgentStrategy(thinkingAgent, actionAgent, observationAgent, finalAgent, toolService);
	}

	@Bean
	@Primary
	public AgentStrategy reactPlusAgentStrategy(ThinkingAgent thinkingAgent, ActionAgent actionAgent,
			ObservationAgent observationAgent, FinalAgent finalAgent, ToolService toolService) {

		// TODO: 暂时使用ReActAgentStrategy
		return new ReActAgentStrategy(thinkingAgent, actionAgent, observationAgent, finalAgent, toolService);
	}

}
