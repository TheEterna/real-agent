package com.ai.agent.real.contract.service;

import com.ai.agent.real.contract.model.agent.*;
import com.ai.agent.real.contract.model.context.*;
import com.ai.agent.real.contract.model.protocol.*;
import reactor.core.publisher.*;

import java.util.*;

/**
 * agent service
 * @author han
 * @time 2025/10/13 2:16
 */
public interface IAgentService {

    /**
     * 流式执行策略（推荐）
     * @param task 任务描述
     * @param agents 可用的Agent列表
     * @param context 执行上下文
     * @return 流式执行结果
     */
    Flux<AgentExecutionEvent> executeStream(String task, List<Agent> agents, AgentContext context);
}
