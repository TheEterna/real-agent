package com.ai.agent.real.contract.service.context;

import com.ai.agent.real.domain.entity.context.AgentMessage;

import java.util.List;

/**
 * Turn 摘要生成策略接口
 * @author: han
 * @time: 2025/11/21 22:12
 */
public interface TurnResumeStrategy {
    
    /**
     * 根据消息列表生成摘要
     * @param messages 消息列表
     * @return 摘要字符串
     */
    String generateResume(List<AgentMessage> messages);
}
