package com.ai.agent.real.application.service.context;

import com.ai.agent.real.contract.service.context.TurnResumeStrategy;
import com.ai.agent.real.domain.entity.context.AgentMessage;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 只保留最后一条消息的摘要策略
 */
@Component
public class LastMessageResumeStrategy implements TurnResumeStrategy {

    @Override
    public String generateResume(List<AgentMessage> messages) {
        if (messages == null || messages.isEmpty()) {
            return "";
        }
        // 获取最后一条消息
        AgentMessage lastMessage = messages.get(messages.size() - 1);
        return lastMessage.getMessage();
    }
}
