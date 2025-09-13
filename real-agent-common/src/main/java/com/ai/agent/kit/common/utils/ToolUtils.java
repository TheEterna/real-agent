package com.ai.agent.kit.common.utils;

import com.ai.agent.contract.exception.*;
import com.ai.agent.contract.spec.*;
import com.ai.agent.contract.spec.message.*;
import com.fasterxml.jackson.databind.*;
import org.springframework.ai.chat.messages.*;
import org.springframework.ai.chat.messages.AssistantMessage.*;
import org.springframework.ai.chat.model.*;

import java.util.*;

import static com.ai.agent.kit.common.constant.NounConstants.TASK_DONE;
import static org.springframework.ai.model.tool.ToolExecutionResult.FINISH_REASON;

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

    public static boolean hasToolCallingNative(ChatResponse response) {

        return response.hasToolCalls();

//        List<ToolCall> toolCalls = response.getResult().getOutput().getToolCalls();
//        // 处理工具调用
//        if (!toolCalls.isEmpty()) {
//            for (ToolCall toolCall : toolCalls) {
//                log.debug("检测到工具调用: {}, 参数: {}", toolCall.name(), toolCall.arguments());
//
//                // 执行工具调用
//                try {
//                    Object toolResult = executeToolCall(toolCall, context);
//                    context.addMessage(AgentMessage.tool(toolResult.toString(), toolCall.id()));
//                    events.add(AgentExecutionEvent.tool(context,
//                        String.format("工具调用: %s\n参数: %s\n结果: %s",
//                            toolCall.name(), toolCall.arguments(), toolResult)));
//                } catch (Exception e) {
//                    log.error("工具调用执行失败: {}", toolCall.name(), e);
//                    String errorMsg = "工具调用失败: " + e.getMessage();
//                    context.addMessage(AgentMessage.tool(errorMsg, toolCall.id()));
//                    events.add(AgentExecutionEvent.error(errorMsg));
//                }
//            }
//        }
    }



    public static boolean hasTaskDone(ChatResponse response) {

        List<Generation> generations = response.getResults();
        List<String> toolCalls = new ArrayList<>();
        for (Generation generation : generations) {
            // 工具调用后的对话历史包含 ToolResponseMessage
            toolCalls.add(generation.getMetadata().get("toolName"));
        }
        return toolCalls.stream().filter(Objects::nonNull).anyMatch(toolCall -> TASK_DONE.equals(toolCall));
    }
//    public static boolean hasTaskDone(ChatResponse response) {
//
//        List<Generation> generations = response.getResults();
//        List<ToolCall> toolCalls = new ArrayList<>();
//        for (Generation generation : generations) {
//            // 工具调用后的对话历史包含 ToolResponseMessage
//            toolCalls.addAll(generation.getOutput().getToolCalls());
//        }
//        return toolCalls.stream().anyMatch(toolCall -> toolCall.name().equals(TASK_DONE));
//    }
    /**
     * check tool call
     * @param response
     * @return
     */
    public static boolean hasToolCalling(ChatResponse response) {
        List<Generation> generations = response.getResults();
        List<Object> toolCalls = new ArrayList<>();
        for (Generation generation : generations) {
            // 工具调用后的对话历史包含 ToolResponseMessage
            Object toolId = generation.getMetadata().get("toolName");
            if (toolId == null) {
                continue;
            }
            toolCalls.add(toolId);
        }
        return !toolCalls.isEmpty();
    }

    /**
     * 执行工具调用
     */
    public Object executeToolCall(ToolCall toolCall, AgentContext context, List<AgentTool> availableTools) throws ToolException {
        String toolName = toolCall.name();
        String arguments = toolCall.arguments();

        // 查找对应的工具
        AgentTool tool = availableTools.stream()
                .filter(t -> toolName.equals(t.getSpec().getName()))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("未找到工具: " + toolName));

        // 将工具参数设置到上下文中
        if (arguments != null && !arguments.trim().isEmpty()) {
            try {
                // 解析JSON参数并设置到context.args中
                ObjectMapper mapper = new ObjectMapper();
                Map<String, Object> argsMap = mapper.readValue(arguments, Map.class);
                context.getArgs().clear();
                context.getArgs().putAll(argsMap);
            } catch (Exception e) {
//                log.warn("解析工具参数失败，使用原始字符串: {}", arguments, e);
                context.getArgs().clear();
                context.getArgs().put("input", arguments);
            }
        } else {
            context.getArgs().clear();
        }

        // 执行工具
        ToolResult result = tool.execute(context);

        // 检查是否是任务完成工具
        if (TASK_DONE.equals(toolName)) {
            context.setTaskCompleted(true);
        }

        return result.getData();
    }

}
