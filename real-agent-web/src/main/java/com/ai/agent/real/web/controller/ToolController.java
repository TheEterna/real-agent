package com.ai.agent.real.web.controller;

import com.ai.agent.application.tool.*;
import com.ai.agent.real.contract.spec.*;
import org.springframework.ai.mcp.*;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * @author han
 * @time 2025/9/16 14:09
 */

@RestController
@RequestMapping("/api")
public class ToolController {

    private final ToolService toolService;

    public ToolController(ToolRegistry toolRegistry,
                          AsyncMcpToolCallbackProvider asyncMcpToolCallbackProvider
    ) {
        this.toolService = new ToolService(toolRegistry, asyncMcpToolCallbackProvider);
    }


    @GetMapping("/tools")
    public List<AgentTool> listTool() {
        return toolService.listAllAgentTools();
    }
}
