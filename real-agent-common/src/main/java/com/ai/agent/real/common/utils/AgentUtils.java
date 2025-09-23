package com.ai.agent.real.common.utils;

import com.ai.agent.real.contract.spec.*;
import com.ai.agent.real.contract.spec.message.*;
import com.fasterxml.jackson.databind.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.*;
import org.springframework.ai.chat.messages.ToolResponseMessage.*;
import org.springframework.ai.chat.prompt.*;
import org.springframework.ai.model.tool.*;
import org.springframework.ai.support.*;

import java.util.*;
import java.util.stream.*;

/**
 * @author han
 * @time 2025/9/10 23:28
 */
@Slf4j
public class AgentUtils {
    /**
     * 将AgentMessage列表转换为Spring AI消息列表
     * @param agentMessages AgentMessage列表
     * @return Spring AI消息列表
     */

    public static List<Message> toSpringAiMessages(List<AgentMessage> agentMessages) {
        log.debug("开始转换AgentMessage列表，总数: {}", agentMessages.size());
        
        return agentMessages.stream()
                .map(agentMessage -> {
                    log.debug("处理AgentMessage: type={}, text={}, metadata={}", 
                            agentMessage.getAgentMessageType(), 
                            agentMessage.getText() != null ? agentMessage.getText().substring(0, Math.min(50, agentMessage.getText().length())) + "..." : "null",
                            agentMessage.getMetadata());
                    
                    switch (agentMessage.getAgentMessageType()) {
                        case SYSTEM:
                            log.debug("创建SystemMessage");
                            return new SystemMessage(agentMessage.getText());
                        case USER:
                            log.debug("创建UserMessage");
                            return new UserMessage(agentMessage.getText());
                        case ASSISTANT:
                            log.debug("创建AssistantMessage");
                            return new AssistantMessage(agentMessage.getText());
                        case TOOL:
//                            log.debug("开始处理TOOL类型消息");
                            Map<String, Object> metadata = agentMessage.getMetadata();
//                            log.debug("TOOL消息metadata: {}", metadata);
//
//                            // read Object from map
//                            log.debug("成功转换ToolResponse: id={}, name={}, content={}",
//                                    metadata.get("id"), metadata.get("name"),
//                                    metadata.get("responseData") != null ? metadata.get("responseData").toString().substring(0, Math.min(50, metadata.get("responseData").toString().length())) + "..." : "null");
//
                            String toolName = metadata.get("name").toString();
//                            ToolResponseMessage toolResponseMessage = new ToolResponseMessage(List.of(new ToolResponse(, metadata.get("name").toString(), metadata.get("responseData").toString())));
//                            log.debug("成功创建ToolResponseMessage");
//                            return toolResponseMessage;
                            return new AssistantMessage("调用工具" + toolName + "，结果：" + metadata.get("responseData").toString());

                        default:
                            log.debug("使用默认AssistantMessage处理未知类型: {}", agentMessage.getAgentMessageType());
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

        log.debug("开始构建Prompt，可用工具数量: {}, 对话历史数量: {}", 
                availableTools != null ? availableTools.size() : 0,
                context.getConversationHistory().size());

        // 配置工具调用选项, 使用原生function calling
        var optionsBuilder = DefaultToolCallingChatOptions.builder();
        if (availableTools != null && !availableTools.isEmpty()) {
            log.debug("配置工具调用选项，工具列表: {}", 
                    availableTools.stream().map((agentTool -> agentTool.getSpec().getName())).collect(Collectors.toList()));
            optionsBuilder.toolCallbacks(ToolUtils.convertAgentTool2ToolCallback(availableTools));
            optionsBuilder.internalToolExecutionEnabled(false);
        }
        var options = optionsBuilder.build();

        // 构建消息
        List<AgentMessage> conversationHistory = context.getConversationHistory();
        log.debug("对话历史详情:");
        for (int i = 0; i < conversationHistory.size(); i++) {
            AgentMessage msg = conversationHistory.get(i);
            log.debug("  [{}] type={}, sender={}, text={}", 
                    i, msg.getAgentMessageType(), msg.getSenderId(),
                    msg.getText() != null ? msg.getText().substring(0, Math.min(100, msg.getText().length())) + "..." : "null");
        }
        
        List<Message> messages = new ArrayList<>();
        messages.add(new SystemMessage(systemPrompt));
        
        log.debug("开始转换对话历史为Spring AI消息");
        List<Message> convertedMessages = AgentUtils.toSpringAiMessages(conversationHistory);
        messages.addAll(convertedMessages);
        log.debug("转换完成，Spring AI消息总数: {}", convertedMessages.size());
        
        messages.add(new UserMessage(userPrompt));
        
        log.debug("最终消息列表构建完成，总消息数: {}", messages.size());
        for (int i = 0; i < messages.size(); i++) {
            Message msg = messages.get(i);
            log.debug(" [{}] Spring AI消息类型: {}", i, msg.getClass().getSimpleName());
        }

        return new Prompt(messages, options);
    }

}
