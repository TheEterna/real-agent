package com.ai.agent.real.web.config;

import com.ai.agent.real.agent.impl.*;
import com.ai.agent.real.agent.strategy.*;
import com.ai.agent.real.contract.model.context.*;
import com.ai.agent.real.contract.model.property.*;
import com.ai.agent.real.contract.service.*;
import com.ai.agent.real.web.config.properties.*;
import org.springframework.ai.chat.model.*;
import org.springframework.context.annotation.*;

/**
 * @author han
 * @time 2025/9/18 20:31
 */

@Configuration
public class AgentBeanConfig {

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

	@Bean
	public ReActAgentStrategy reactAgentStrategy(ThinkingAgent thinkingAgent, ActionAgent actionAgent,
			ObservationAgent observationAgent, FinalAgent finalAgent) {
		return new ReActAgentStrategy(thinkingAgent, actionAgent, observationAgent, finalAgent);
	}

	/**
	 * =================== Context part ===========================
	 */
	@Bean
	public AgentMemory agentMemory(RealAgentProperties realAgentProperties) {
		ContextZipMode zipMode = realAgentProperties.getContextZipMode();
		AgentSessionConfig defaultSessionConfig = new AgentSessionConfig(zipMode);
		return AgentMemory.builder().build(defaultSessionConfig);
	}



}
