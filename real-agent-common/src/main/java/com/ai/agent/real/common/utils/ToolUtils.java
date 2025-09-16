package com.ai.agent.real.common.utils;

import com.ai.agent.real.common.constant.*;
import com.ai.agent.real.contract.exception.*;
import com.ai.agent.real.contract.spec.*;
import com.fasterxml.jackson.databind.*;
import lombok.*;
import org.springframework.ai.chat.messages.AssistantMessage.*;
import org.springframework.ai.chat.model.*;
import org.springframework.ai.model.*;
import org.springframework.ai.tool.*;
import org.springframework.ai.tool.metadata.*;
import org.springframework.ai.tool.method.*;
import org.springframework.ai.tool.support.*;

import java.lang.reflect.*;
import java.util.*;

import static com.ai.agent.real.common.constant.NounConstants.MCP;

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
        return toolCalls.stream().filter(Objects::nonNull).anyMatch(toolCall -> NounConstants.TASK_DONE.equals(toolCall));
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
                context.getToolArgs().clear();
                context.getToolArgs().putAll(argsMap);
            } catch (Exception e) {
//                log.warn("解析工具参数失败，使用原始字符串: {}", arguments, e);
                context.getToolArgs().clear();
                context.getToolArgs().put("input", arguments);
            }
        } else {
            context.getToolArgs().clear();
        }

        // 执行工具
        ToolResult result = tool.execute(context);

        // 检查是否是任务完成工具
        if (NounConstants.TASK_DONE.equals(toolName)) {
            context.setTaskCompleted(true);
        }

        return result.getData();
    }


    /**
     * convert AgentTool to toolCallback
     * @param availableTools
     * @return
     */
    @SneakyThrows
    public static ToolCallback[] convertAgentTool2ToolCallback(List<AgentTool> availableTools) {
        ToolCallback[] toolCallbacks = new ToolCallback[availableTools.size()];
        for (int i = 0; i < availableTools.size(); i++) {
            AgentTool agentTool = availableTools.get(i);
            Method executeMethod = agentTool.getClass().getMethod("execute", AgentContext.class);
            toolCallbacks[i] = MethodToolCallback.builder()
                    .toolDefinition(
                            ToolDefinitions.builder(executeMethod)
                                    .name(agentTool.getSpec().getName())
                                    .description(agentTool.getSpec().getDescription())
                                    .build())
                        .toolMethod(executeMethod)
                        .toolObject(agentTool)
                        .toolMetadata(ToolMetadata.builder().returnDirect(true).build())
                        .build();
        }
        return toolCallbacks;
    }
    /**
     * convert toolCallback to AgentTool
     * @param toolCallbacks
     * @return
     */
    @SneakyThrows
    public static List<AgentTool> convertToolCallback2AgentTool(List<ToolCallback> toolCallbacks) {
        List<AgentTool> agentTools = new ArrayList<>();
        for (ToolCallback toolCallback : toolCallbacks) {
            AgentTool agentTool = new AgentTool() {
                @Override
                public String Id() {
                    return toolCallback.getToolDefinition().name();
                }

                @Override
                public ToolSpec getSpec() {
                    return new ToolSpec()
                            .setName(toolCallback.getToolDefinition().name())
                            .setDescription(toolCallback.getToolDefinition().description())
                            .setCategory(MCP);
                }

                @Override
                public ToolResult<String> execute(AgentContext ctx) throws ToolException {
                    try {
                        String result = toolCallback.call(ModelOptionsUtils.toJsonString(ctx.getToolArgs()));
                        return ToolResult.ok(result, System.currentTimeMillis(), this.Id());
                    } catch (Exception e) {
                        throw new ToolException("Tool execution failed", e);
                    }
                }

            };
            agentTools.add(agentTool);
        }
        return agentTools;
    }

}
