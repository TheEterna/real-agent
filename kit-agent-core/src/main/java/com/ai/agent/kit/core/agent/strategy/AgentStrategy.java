package com.ai.agent.kit.core.agent.strategy;

import com.ai.agent.kit.common.spec.*;
import com.ai.agent.kit.core.agent.Agent;
import com.ai.agent.kit.core.agent.communication.AgentContext;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * Agent策略接口
 * 定义了不同Agent协作模式的统一规范，支持流式和非流式执行
 * 
 * @author han
 * @time 2025/9/6 23:40
 */
public interface AgentStrategy {

    /**
     * 获取策略名称
     */
    default String getStrategyName() {
        return this.getClass().getSimpleName();
    }

    /**
     * 获取策略描述
     */
    default String getDescription() {
        return "Agent策略: " + getStrategyName();
    }

    /**
     * 流式执行策略（推荐）
     * 返回实时的执行进度和中间结果
     * 
     * @param task 任务描述
     * @param agents 可用的Agent列表
     * @param context 执行上下文
     * @return 流式执行结果
     */
    Flux<AgentExecutionEvent> executeStream(String task, List<Agent> agents, AgentContext context);

    /**
     * 同步执行策略（兼容旧版本）
     * 
     * @param task 任务描述
     * @param agents 可用的Agent列表
     * @param context 工具执行上下文
     * @return 执行结果
     */
    AgentResult execute(String task, List<Agent> agents, AgentContext context);


}
