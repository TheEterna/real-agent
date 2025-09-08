package com.ai.agent.kit.core.agent.manager;

import com.ai.agent.kit.core.agent.Agent;
import com.ai.agent.kit.common.spec.AgentResult;
import com.ai.agent.kit.core.agent.strategy.AgentStrategy;
import com.ai.agent.kit.core.agent.communication.AgentContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Agent管理器
 * 负责Agent的注册、管理和策略调度
 * 
 * @author han
 * @time 2025/9/7 00:10
 */
@Slf4j
@Component
public class AgentManager {

    /**
     * 注册的Agent映射表
     */
    private final Map<String, Agent> agents = new ConcurrentHashMap<>();

    /**
     * 注册的策略映射表
     */
    private final Map<String, AgentStrategy> strategies = new ConcurrentHashMap<>();

    /**
     * 默认策略名称
     */
    private String defaultStrategyName = "SingleAgent";

    /**
     * 注册Agent
     */
    public void registerAgent(Agent agent) {
        if (agent == null || agent.getAgentId() == null) {
            throw new IllegalArgumentException("Agent或AgentId不能为空");
        }
        
        agents.put(agent.getAgentId(), agent);
        log.info("注册Agent成功: [{}] - {}", agent.getAgentId(), agent.getAgentName());
    }

    /**
     * 注册策略
     */
    public void registerStrategy(AgentStrategy strategy) {
        if (strategy == null || strategy.getStrategyName() == null) {
            throw new IllegalArgumentException("策略或策略名称不能为空");
        }
        
        strategies.put(strategy.getStrategyName(), strategy);
        log.info("注册策略成功: [{}] - {}", strategy.getStrategyName(), strategy.getDescription());
    }

    /**
     * 获取Agent
     */
    public Agent getAgent(String agentId) {
        return agents.get(agentId);
    }

    /**
     * 获取所有Agent
     */
    public List<Agent> getAllAgents() {
        return new ArrayList<>(agents.values());
    }

    /**
     * 获取策略
     */
    public AgentStrategy getStrategy(String strategyName) {
        return strategies.get(strategyName);
    }

    /**
     * 获取所有策略
     */
    public List<AgentStrategy> getAllStrategies() {
        return new ArrayList<>(strategies.values());
    }

    /**
     * 执行任务 - 使用默认策略
     */
    public AgentResult executeTask(String task, AgentContext context) {
        return executeTask(task, context, null);
    }

    /**
     * 执行任务 - 指定策略
     */
    public AgentResult executeTask(String task, AgentContext context, String strategyName) {
        if (task == null || task.trim().isEmpty()) {
            return AgentResult.failure("任务描述不能为空", "AgentManager");
        }

        if (agents.isEmpty()) {
            return AgentResult.failure("没有可用的Agent", "AgentManager");
        }

        try {
            // 选择策略
            AgentStrategy strategy = selectStrategy(task, strategyName);
            if (strategy == null) {
                return AgentResult.failure("没有找到适合的策略", "AgentManager");
            }

            log.info("使用策略 [{}] 执行任务: {}", strategy.getStrategyName(), task);

            // 执行任务
            return strategy.execute(task, getAllAgents(), context);

        } catch (Exception e) {
            log.error("执行任务异常", e);
            return AgentResult.failure("任务执行异常: " + e.getMessage(), "AgentManager");
        }
    }

    /**
     * 选择合适的策略
     */
    private AgentStrategy selectStrategy(String task, String specifiedStrategyName) {
        // 如果指定了策略名称，直接使用
        if (specifiedStrategyName != null && !specifiedStrategyName.trim().isEmpty()) {
            AgentStrategy strategy = strategies.get(specifiedStrategyName);
            if (strategy != null) {
                return strategy;
            }
            log.warn("指定的策略 [{}] 不可用，将自动选择策略", specifiedStrategyName);
        }

        // 自动选择最适合的策略
        return strategies.values().stream().findFirst().orElse(null);
    }

    /**
     * 获取能够处理指定任务的Agent列表
     */
    public List<Agent> getCapableAgents(String task) {
        return agents.values().stream()
                .filter(agent -> agent.canHandle(task))
//                .sorted((a1, a2) -> Double.compare(a2.getConfidenceScore(task), a1.getConfidenceScore(task)))
                .toList();
    }

    /**
     * 设置默认策略
     */
    public void setDefaultStrategy(String strategyName) {
        if (strategies.containsKey(strategyName)) {
            this.defaultStrategyName = strategyName;
            log.info("设置默认策略: [{}]", strategyName);
        } else {
            log.warn("策略 [{}] 不存在，无法设置为默认策略", strategyName);
        }
    }

    /**
     * 获取管理器状态信息
     */
    public Map<String, Object> getStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("agentCount", agents.size());
        status.put("strategyCount", strategies.size());
        status.put("defaultStrategy", defaultStrategyName);
        status.put("agents", agents.keySet());
        status.put("strategies", strategies.keySet());
        return status;
    }
}
