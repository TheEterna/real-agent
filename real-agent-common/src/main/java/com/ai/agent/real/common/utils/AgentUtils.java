package com.ai.agent.real.common.utils;

import com.ai.agent.real.contract.spec.*;
import com.ai.agent.real.contract.spec.message.*;
import org.springframework.ai.chat.messages.*;
import org.springframework.ai.chat.prompt.*;
import org.springframework.ai.model.tool.*;
import org.springframework.ai.support.*;

import java.util.*;
import java.util.stream.*;

/**
 * @author han
 * @time 2025/9/10 23:28
 */

public class AgentUtils {
    /**
     * 将AgentMessage列表转换为Spring AI消息列表
     * @param agentMessages AgentMessage列表
     * @return Spring AI消息列表
     */

    public static List<Message> toSpringAiMessages(List<AgentMessage> agentMessages) {
        return agentMessages.stream()
                .map(agentMessage -> {
                    switch (agentMessage.getAgentMessageType()) {
                        case SYSTEM:
                            return new SystemMessage(agentMessage.getText());
                        case USER:
                            return new UserMessage(agentMessage.getText());
                        case ASSISTANT:
                            return new AssistantMessage(agentMessage.getText());
//                        case TOOL:
//                            // TODO 工具调用 SPRINGAI maybe already achieved
//                            Object toolResponse = agentMessage.getMetadata().get("toolResponse");
//                            if (toolResponse != null) {
//                                return new ToolResponseMessage(toolResponse.toString());
//                            } else {
//                                return new ToolResponseMessage(agentMessage.getText());
//                            }
                        default:
                            return new AssistantMessage(agentMessage.getText());
                    }
                })
                .collect(Collectors.toList());
    }




    public static Prompt buildPromptWithContextAndTools(
            List<AgentTool> availableTools,
            AgentContext context,
            String systemPrompt,
            String userPrompt) {

        // 配置工具调用选项, 使用原生function calling
        var optionsBuilder = DefaultToolCallingChatOptions.builder();
        if (availableTools != null && !availableTools.isEmpty()) {
            optionsBuilder.toolCallbacks(ToolUtils.convertAgentTool2ToolCallback(availableTools));
            optionsBuilder.internalToolExecutionEnabled(false);
        }
        var options = optionsBuilder.build();

        // 构建消息
        List<AgentMessage> conversationHistory = context.getConversationHistory();
        List<Message> messages = new ArrayList<>();
        messages.add(new SystemMessage(systemPrompt));
        messages.addAll(AgentUtils.toSpringAiMessages(conversationHistory));
        messages.add(new UserMessage(userPrompt));

        return new Prompt(messages, options);
    }

}
