package com.ai.agent.real.contract.service.agent;

import com.ai.agent.real.contract.model.protocol.AgentExecutionEvent;
import com.ai.agent.real.domain.entity.context.AgentMessage;
import com.ai.agent.real.domain.entity.context.Turn;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 *
 * @author han
 * @time 2025/11/21 23:15
 */
public interface IAgentStorageService {
    /**
     * 开始一个新的 Turn
     */
    @Transactional
    Mono<Turn> startTurn(UUID turnId, UUID parentTurnId, UUID sessionId);

    /**
     * 保存 Agent 消息
     */
    @Transactional
    Mono<AgentMessage> saveMessage(UUID sessionId, UUID turnId, AgentExecutionEvent event);

    /**
     * 完成 Turn，生成摘要
     */
    @Transactional
    Mono<Turn> completeTurn(UUID turnId);

    /**
     * 获取会话历史消息
     */
    Flux<AgentMessage> getSessionMessages(UUID sessionId);
}
