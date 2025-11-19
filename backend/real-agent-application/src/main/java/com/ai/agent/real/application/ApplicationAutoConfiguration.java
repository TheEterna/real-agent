package com.ai.agent.real.application;

import com.ai.agent.real.application.agent.ApplicationAgentAutoConfiguration;
import com.ai.agent.real.application.plugin.ApplicationPluginBeanConfiguration;
import com.ai.agent.real.application.tool.system.*;
import com.ai.agent.real.common.constant.NounConstants;
import com.ai.agent.real.contract.tool.IToolService;
import com.ai.agent.real.application.tool.service.ToolServiceImpl;
import com.ai.agent.real.contract.tool.AgentTool;
import io.modelcontextprotocol.client.McpAsyncClient;
import org.springframework.context.annotation.*;

import java.util.List;
import java.util.Set;

import static com.ai.agent.real.common.constant.NounConstants.*;

/**
 * 应用层自动配置类 作为整个应用模块的唯一配置入口，负责注册所有需要的Bean
 *
 * @author han
 * @time 2025/10/27 21:16
 */
@Configuration
@Import({ ApplicationPluginBeanConfiguration.class, ApplicationAgentAutoConfiguration.class })
@ComponentScan(basePackages = "com.ai.agent.real.application.service")
public class ApplicationAutoConfiguration {

	/**
	 * =============================== 注册工具服务 ===============================
	 */

	@Bean
	public IToolService toolService(List<McpAsyncClient> mcpAsyncClients) {

		IToolService toolService = new ToolServiceImpl(mcpAsyncClients);

		// custom handle tool register

		toolService.registerToolWithKeywords(new TaskDoneTool(), Set.of(TASK_DONE));
		toolService.registerToolWithKeywords(new TaskAnalysisTool(), Set.of(TASK_ANALYSIS, REACT_PLUS_TOOLS));
		toolService.registerToolWithKeywords(new PlanInitTool(), Set.of(PLAN_INIT, PLAN_TOOLS, REACT_PLUS_TOOLS));
		toolService.registerToolWithKeywords(new PlanUpdateTool(), Set.of(PLAN_UPDATE, PLAN_TOOLS, REACT_PLUS_TOOLS));
		toolService.registerToolWithKeywords(new PlanAdvanceTool(), Set.of(PLAN_ADVANCE, PLAN_TOOLS, REACT_PLUS_TOOLS));

		// register mcp tools
		List<AgentTool> agentToolList = toolService.listAllMCPToolsAsync().block();

		toolService.registerToolsWithKeywords(agentToolList, Set.of(NounConstants.MCP));

		return toolService;
	}

}