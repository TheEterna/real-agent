package com.ai.agent.kit.common.utils;

import com.ai.agent.kit.core.tool.model.*;

import java.util.*;

/**
 * @author han
 * @time 2025/9/8 17:05
 */

public class ToolUtils {

    public static String getToolsDescription(List<AgentTool> tools) {
        if (tools.isEmpty()) {
            return "无可用工具";
        }

        StringBuilder desc = new StringBuilder();
        for (AgentTool tool : tools) {
            desc.append("- ").append(tool.getSpec().getName())
                    .append(": ").append(tool.getSpec().getDescription()).append("\n");
        }
        return desc.toString();
    }


}
