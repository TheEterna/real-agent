package com.ai.agent.kit.core;

import com.ai.agent.contract.spec.*;
import com.ai.agent.kit.core.agent.*;
import com.ai.agent.kit.core.agent.strategy.AgentStrategy;
import com.ai.agent.kit.core.analyzer.TaskAnalyzer;
import com.ai.agent.contract.spec.AgentContext;
import com.ai.agent.kit.core.tool.ToolRegistry;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 智能编排器
 * 实现三层架构的核心协调逻辑：任务分析 -> 策略选择 -> Agent组合 -> 工具调用
 * 
 * @author han
 * @time 2025/9/7 01:18
 */
@Slf4j
public class SmartOrchestrator  {
    
    private final TaskAnalyzer taskAnalyzer;
    private final Map<String, AgentStrategy> strategies;
    private final Map<String, Agent> agents;
    private final ToolRegistry toolRegistry;
    
    public SmartOrchestrator(ToolRegistry toolRegistry) {
        this.taskAnalyzer = new TaskAnalyzer();
        this.strategies = new ConcurrentHashMap<>();
        this.agents = new ConcurrentHashMap<>();
        this.toolRegistry = toolRegistry;
    }
    
    /**
     * 执行任务的主入口
     * 按照三层架构流程：分析 -> 策略选择 -> Agent组合 -> 执行
     */
    public AgentResult executeTask(String task, AgentContext context) {
        return executeTask(task, context, null);
    }

    /**
     * 执行任务（可指定策略）
     */
    public AgentResult executeTask(String task, AgentContext context, String specifiedStrategy) {

        log.info("开始执行任务: {}", task);

        // 第一步：任务分析
        TaskAnalyzer.TaskAnalysisResult analysis = taskAnalyzer.analyze(task);
        log.info("任务分析完成: 类型={}, 复杂度={}", analysis.getTaskTypes(), analysis.getComplexity());

        // 第二步：策略选择
        String strategyName = Optional.ofNullable(specifiedStrategy).orElse(analysis.getRecommendedStrategy());
        AgentStrategy strategy = strategies.get(strategyName);


        if (strategy == null) {
//                return ToolResult.error("STRATEGY_NOT_FOUND", "未找到策略: " + strategyName);
            return AgentResult.failure("STRATEGY_NOT_FOUND", "未找到策略: " + strategyName);
        }

        // 第三步：Agent选择和组合
        List<Agent> selectedAgents = selectAgents(analysis);
        if (selectedAgents.isEmpty()) {
            return AgentResult.failure("NO_CAPABLE_AGENTS", "未找到能处理该任务的Agent");
        }

        log.info("选择策略: {}, 选择Agent: {}", strategyName,
                selectedAgents.stream().map(Agent::getAgentId).collect(Collectors.toList()));

        // 第四步：执行策略
        AgentResult result = strategy.execute(task, selectedAgents, context);

        return result;
            

    }
    
    /**
     * 根据任务分析结果选择合适的Agent
     */
    private List<Agent> selectAgents(TaskAnalyzer.TaskAnalysisResult analysis) {
        List<Agent> selectedAgents = new ArrayList<>();
        
        // 1. 根据推荐的Agent列表选择
        for (String agentId : analysis.getRecommendedAgents()) {
            Agent agent = agents.get(agentId);
            if (agent != null) {
                selectedAgents.add(agent);
            }
        }
        
        // 2. 如果推荐列表为空或找不到Agent，根据关键词匹配
        if (selectedAgents.isEmpty()) {
            selectedAgents = agents.values().stream()
                    .filter(agent -> agent.canHandle(analysis.getOriginalTask()))

                    .collect(Collectors.toList());
        }
        
        // 3. 确保至少有一个通用Agent作为兜底
        if (selectedAgents.isEmpty()) {
            Agent generalAgent = agents.get("general-purpose-agent");
            if (generalAgent != null) {
                selectedAgents.add(generalAgent);
            }
        }
        
        return selectedAgents;
    }
    
    /**
     * 注册策略
     */
    public void registerStrategy(AgentStrategy strategy) {
        strategies.put(strategy.getStrategyName(), strategy);
        log.info("注册策略: {}", strategy.getStrategyName());
    }
    

    /**
     * 获取可用策略列表
     */
    public List<AgentStrategy> getAvailableStrategies() {
        return new ArrayList<>(strategies.values());
    }
    
    /**
     * 获取可用Agent列表
     */
    public List<Agent> getAvailableAgents() {
        return new ArrayList<>(agents.values());
    }


}
