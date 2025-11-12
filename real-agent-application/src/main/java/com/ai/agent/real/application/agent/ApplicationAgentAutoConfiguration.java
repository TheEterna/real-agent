package com.ai.agent.real.application.agent;

import com.ai.agent.real.application.agent.dispatcher.DefaultAgentDispatcher;
import com.ai.agent.real.application.agent.item.ActionAgent;
import com.ai.agent.real.application.agent.item.FinalAgent;
import com.ai.agent.real.application.agent.item.ObservationAgent;
import com.ai.agent.real.application.agent.item.ThinkingAgent;
import com.ai.agent.real.application.agent.item.reactplus.*;
import com.ai.agent.real.application.agent.turn.AgentTurnManagerService;
import com.ai.agent.real.application.agent.strategy.ReActAgentStrategy;
import com.ai.agent.real.application.agent.strategy.ReActPlusAgentStrategy;
import com.ai.agent.real.application.service.ContextManager;
import com.ai.agent.real.contract.agent.IAgentStrategy;
import com.ai.agent.real.contract.agent.IAgentDispatcher;
import com.ai.agent.real.contract.agent.context.AgentMemory;
import com.ai.agent.real.contract.agent.context.AgentSessionConfig;
import com.ai.agent.real.contract.agent.service.IAgentTurnManagerService;
import com.ai.agent.real.contract.model.property.ContextZipMode;
import com.ai.agent.real.contract.model.property.ToolApprovalMode;
import com.ai.agent.real.contract.service.IPropertyService;
import com.ai.agent.real.contract.tool.IToolService;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Qualifier;
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
	public IAgentTurnManagerService agentTurnManagerService() {
		return new AgentTurnManagerService();
	}

	/**
	 * 注册默认的Agent调度器
	 * @param agentStrategyMap Agent策略映射
	 * @return DefaultAgentDispatcher实例
	 */
	@Bean
	public IAgentDispatcher defaultAgentDispatcher(Map<String, IAgentStrategy> agentStrategyMap) {
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
	public ThinkingAgent thinkingAgent(ChatModel chatModel, IToolService toolService,
			IPropertyService propertyService) {
		ToolApprovalMode mode = propertyService.getToolApprovalMode();
		return new ThinkingAgent(chatModel, toolService, mode);
	}

	@Bean
	public ActionAgent actionAgent(ChatModel chatModel, IToolService toolService, IPropertyService propertyService) {
		ToolApprovalMode mode = propertyService.getToolApprovalMode();
		return new ActionAgent(chatModel, toolService, mode);
	}

	@Bean
	public ObservationAgent observationAgent(ChatModel chatModel, IToolService toolService,
			IPropertyService propertyService) {
		ToolApprovalMode mode = propertyService.getToolApprovalMode();
		return new ObservationAgent(chatModel, toolService, mode);
	}

	@Bean
	public FinalAgent finalAgent(ChatModel chatModel, IToolService toolService, IPropertyService propertyService) {
		return new FinalAgent(chatModel, toolService);
	}

	@Bean
	// TODO: 待完善
	public TaskAnalysisAgent taskAnalysisAgent(ChatModel chatModel, IToolService toolService,
			IPropertyService propertyService, IAgentTurnManagerService agentTurnManagerService) {
		return new TaskAnalysisAgent(chatModel, toolService, propertyService.getToolApprovalMode(),
				agentTurnManagerService);
	}

	@Bean
	// TODO: 待完善
	public PlanInitAgent planInitAgent(ChatModel chatModel, IToolService toolService,
			IPropertyService propertyService) {
		return new PlanInitAgent(chatModel, toolService);
	}

	@Bean
	// TODO: 待完善
	public ThoughtAgent thoughtAgent(ChatModel chatModel, IToolService toolService, IPropertyService propertyService) {
		return new ThoughtAgent(chatModel, toolService);
	}

	@Bean
	public ThinkingPlusAgent thinkingPlusAgent(ChatModel chatModel, IToolService toolService,
			IPropertyService propertyService) {
		ToolApprovalMode mode = propertyService.getToolApprovalMode();
		return new ThinkingPlusAgent(chatModel, toolService, mode);
	}

	@Bean
	public ActionPlusAgent actionPlusAgent(ChatModel chatModel, IToolService toolService,
			IPropertyService propertyService) {
		ToolApprovalMode mode = propertyService.getToolApprovalMode();
		return new ActionPlusAgent(chatModel, toolService, mode);
	}

	/**
	 * =================== Agent Strategy part ===========================
	 */
	@Bean("reActPlusAgentStrategy")
	@Primary
	public IAgentStrategy reActPlusAgentStrategy(TaskAnalysisAgent taskAnalysisAgent, PlanInitAgent planInitAgent,
			ThoughtAgent thoughtAgent, ThinkingPlusAgent thinkingPlusAgent, ActionPlusAgent actionPlusAgent,
			FinalAgent finalAgent, ContextManager contextManager) {

		return new ReActPlusAgentStrategy(taskAnalysisAgent, planInitAgent, thoughtAgent, thinkingPlusAgent,
				actionPlusAgent, finalAgent, contextManager);
	}

	@Bean("reActAgentStrategy")
	public IAgentStrategy reactAgentStrategy(ThinkingAgent thinkingAgent, ActionAgent actionAgent,
			ObservationAgent observationAgent, FinalAgent finalAgent, IToolService toolService) {
		return new ReActAgentStrategy(thinkingAgent, actionAgent, observationAgent, finalAgent, toolService);
	}

}
