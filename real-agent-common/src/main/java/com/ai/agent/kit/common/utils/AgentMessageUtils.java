package com.ai.agent.kit.common.utils;

import com.ai.agent.contract.spec.message.*;
import com.ai.agent.contract.spec.message.AgentMessage.*;
import org.springframework.ai.chat.messages.*;
import org.springframework.core.type.filter.*;

import java.util.*;
import java.util.stream.*;

/**
 * @author han
 * @time 2025/9/10 23:28
 */

public class AgentMessageUtils {
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
}
