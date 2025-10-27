package com.ai.agent.real.application;

import com.ai.agent.real.application.agent.dispatcher.DefaultAgentDispatcher;
import com.ai.agent.real.application.agent.impl.ActionAgent;
import com.ai.agent.real.application.agent.impl.FinalAgent;
import com.ai.agent.real.application.agent.impl.ObservationAgent;
import com.ai.agent.real.application.agent.impl.ThinkingAgent;
import com.ai.agent.real.application.agent.strategy.*;
import com.ai.agent.real.application.plugin.PluginBeanConfiguration;
import com.ai.agent.real.application.tool.system.TaskDoneTool;
import com.ai.agent.real.common.constant.NounConstants;
import com.ai.agent.real.contract.agent.AgentStrategy;
import com.ai.agent.real.contract.agent.IAgentDispatcher;
import com.ai.agent.real.contract.model.context.AgentMemory;
import com.ai.agent.real.contract.model.context.AgentSessionConfig;
import com.ai.agent.real.contract.model.property.ContextZipMode;
import com.ai.agent.real.contract.model.property.ToolApprovalMode;
import com.ai.agent.real.contract.service.IPropertyService;
import com.ai.agent.real.contract.service.ToolService;
import com.ai.agent.real.application.tool.service.ToolServiceImpl;
import com.ai.agent.real.contract.tool.AgentTool;
import io.modelcontextprotocol.client.McpAsyncClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.ai.agent.real.common.constant.NounConstants.TASK_DONE;

/**
 * 应用层自动配置类 作为整个应用模块的唯一配置入口，负责注册所有需要的Bean
 *
 * @author han
 * @time 2025/10/27 21:16
 */
@Configuration
@Import(PluginBeanConfiguration.class)
@ComponentScan(basePackages = "com.ai.agent.real.application.service")
public class ApplicationAutoConfiguration {

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

	@Bean
	public ReActAgentStrategy reactAgentStrategy(ThinkingAgent thinkingAgent, ActionAgent actionAgent,
			ObservationAgent observationAgent, FinalAgent finalAgent, ToolService toolService) {
		return new ReActAgentStrategy(thinkingAgent, actionAgent, observationAgent, finalAgent, toolService);
	}

	/**
	 * =============================== 注册工具服务 ===============================
	 */

	@Bean
	public ToolService toolService(List<McpAsyncClient> mcpAsyncClients) {

		ToolService toolService = new ToolServiceImpl(mcpAsyncClients);

		// custom handle tool register

		toolService.registerToolWithKeywords(new TaskDoneTool(), Set.of(TASK_DONE));

		// register mcp tools
		List<AgentTool> agentToolList = toolService.listAllMCPToolsAsync().block();

		toolService.registerToolsWithKeywords(agentToolList, Set.of(NounConstants.MCP));

		return toolService;
	}

}