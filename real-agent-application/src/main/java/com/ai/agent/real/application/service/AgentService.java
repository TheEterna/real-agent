package com.ai.agent.real.application.service;

import com.ai.agent.real.agent.strategy.*;
import com.ai.agent.real.contract.model.agent.*;
import com.ai.agent.real.contract.model.context.*;
import com.ai.agent.real.contract.model.message.*;
import com.ai.agent.real.contract.model.protocol.*;
import com.ai.agent.real.contract.service.*;
import lombok.extern.slf4j.*;
import reactor.core.publisher.*;

import java.time.*;
import java.util.*;

/**
 * @author han
 * @time 2025/10/13 2:17
 */

@Slf4j
public class AgentService implements IAgentService {

    private final ReActAgentStrategy reactAgentStrategy;
    private final AgentMemory agentMemory;

    public AgentService(ReActAgentStrategy reactAgentStrategy,
                        AgentMemory agentMemory) {
        this.reactAgentStrategy = reactAgentStrategy;
        this.agentMemory = agentMemory;
    }

    /**
     * 流式执行策略（推荐）
     *
     * @param task    任务描述
     * @param agents  可用的Agent列表
     * @param context 执行上下文
     * @return 流式执行结果
     */
    @Override
    public Flux<AgentExecutionEvent> executeStream(String task, List<Agent> agents, AgentContext context) {

        // get sessionId to get messageHistory from memory
        String sessionId = context.getSessionId();
        List<AgentMessage> messageHistory = agentMemory.getMessageHistory(sessionId);
        context.setMessageHistory(messageHistory);

        // 调用Agent策略执行流式执行
        return reactAgentStrategy.executeStream(task, agents, context)
                // 将流中的异常转换为一个错误事件，避免直接以错误终止连接
                .onErrorResume(error -> {
                    log.error("ReAct执行异常(流内)", error);
                    return Flux.just(AgentExecutionEvent.error(error));
                })
                .doOnError(error -> log.error("ReAct执行异常", error))
                .doOnComplete(() -> {
                    context.setEndTime(LocalDateTime.now());
                    // 保存上下文
                    agentMemory.addTurn(
                            context.getTrace().getSessionId(),
                            context.getMessageHistory());
                    log.info("ReAct任务执行完成");
                });
    }
}
