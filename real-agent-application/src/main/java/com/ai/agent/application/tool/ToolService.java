package com.ai.agent.application.tool;

import com.ai.agent.real.common.utils.*;
import com.ai.agent.real.contract.spec.*;
import org.springaicommunity.mcp.provider.tool.*;
import org.springframework.ai.mcp.*;
import org.springframework.ai.support.*;
import org.springframework.ai.tool.*;

import java.util.*;

/**
 * tool service include system tool and mcp tool
 * @author han
 * @time 2025/9/16 11:11
 */

public class ToolService {


    private final ToolRegistry toolRegistry;

    /**
     * async mcp tools
     * @param toolRegistry
     */
    private final AsyncMcpToolCallbackProvider asyncMcpToolCallbackProvider;

    /**
     * sync mcp tools
     * @param toolRegistry
     */
    public ToolService(ToolRegistry toolRegistry,
                       AsyncMcpToolCallbackProvider asyncMcpToolCallbackProvider) {
        this.toolRegistry = toolRegistry;
        this.asyncMcpToolCallbackProvider = asyncMcpToolCallbackProvider;
    }
    public List<AgentTool> listNativeSystemTools() {
        return toolRegistry.list();
    }

    public List<ToolCallback> listMcpTools() {
        return Arrays.stream(asyncMcpToolCallbackProvider.getToolCallbacks()).toList();
    }

    public List<AgentTool> listAllAgentTools() {
        List<AgentTool> tools = new ArrayList<>();
        tools.addAll(this.listNativeSystemTools());
        tools.addAll(ToolUtils.convertToolCallback2AgentTool(this.listMcpTools()));
        return tools;
    }

    public List<ToolCallback> listAllToolCallback() {
        List<ToolCallback> tools = new ArrayList<>();
        tools.addAll(List.of(ToolUtils.convertAgentTool2ToolCallback(this.listNativeSystemTools())));
        tools.addAll(this.listMcpTools());
        return tools;
    }
}
