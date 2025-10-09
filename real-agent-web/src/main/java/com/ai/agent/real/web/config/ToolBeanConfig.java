package com.ai.agent.real.web.config;

import com.ai.agent.real.application.tool.*;
import com.ai.agent.real.common.constant.*;
import com.ai.agent.real.contract.spec.*;
import com.ai.agent.real.contract.service.ToolService;
import com.ai.agent.real.tool.system.impl.*;
import io.modelcontextprotocol.client.*;
import org.springframework.context.annotation.*;

import java.util.*;

import static com.ai.agent.real.common.constant.NounConstants.TASK_DONE;

/**
 * @author han
 * @time 2025/9/18 20:47
 */
@Configuration
public class ToolBeanConfig {

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
