//package com.ai.agent.kit.core.agent.strategy;
//
//import com.ai.agent.kit.core.agent.Agent;
//import com.ai.agent.kit.common.spec.AgentResult;
//import com.ai.agent.kit.core.agent.communication.AgentContext;
//import lombok.extern.slf4j.Slf4j;
//
//import java.time.LocalDateTime;
//import java.util.List;
//
///**
// * 单Agent策略实现
// * 选择最适合的单个Agent来处理任务
// *
// * @author han
// * @time 2025/9/6 23:55
// */
//@Slf4j
//public class SingleAgentStrategy implements AgentStrategy {
//
//    @Override
//    public String getStrategyName() {
//        return "SingleAgent";
//    }
//
//    @Override
//    public String getDescription() {
//        return "选择最适合的单个Agent处理任务";
//    }
//
//    @Override
//    public AgentResult execute(String task, List<Agent> agents, AgentContext context) {
//        if (agents == null || agents.isEmpty()) {
//            return AgentResult.failure("没有可用的Agent", "SingleAgentStrategy");
//        }
//
//        // 选择最适合的Agent
//        Agent bestAgent = selectBestAgent(task, agents);
//        if (bestAgent == null) {
//            return AgentResult.failure("没有找到适合处理该任务的Agent", "SingleAgentStrategy");
//        }
//
//        log.info("选择Agent [{}] 处理任务: {}", bestAgent.getAgentName(), task);
//
//        try {
//            LocalDateTime startTime = LocalDateTime.now();
//            AgentResult result = bestAgent.execute(task, context);
//            LocalDateTime endTime = LocalDateTime.now();
//
//            // 设置执行时间信息
//            result.setStartTime(startTime)
//                  .setEndTime(endTime)
//                  .setExecutionTime(java.time.Duration.between(startTime, endTime).toMillis());
//
//            log.info("Agent [{}] 执行完成，结果: {}", bestAgent.getAgentName(),
//                    result.isSuccess() ? "成功" : "失败");
//
//            return result;
//        } catch (Exception e) {
//            log.error("Agent [{}] 执行任务时发生异常", bestAgent.getAgentName(), e);
//            return AgentResult.failure("执行异常: " + e.getMessage(), bestAgent.getAgentId());
//        }
//    }
//
//    @Override
//    public boolean isApplicable(String task, List<Agent> agents) {
//        return agents != null && !agents.isEmpty() &&
//               agents.stream().anyMatch(agent -> agent.canHandle(task));
//    }
//
//    @Override
//    public int getPriority() {
//        return 1; // 基础策略，优先级较低
//    }
//
//    /**
//     * 选择最适合的Agent
//     */
//    private Agent selectBestAgent(String task, List<Agent> agents) {
//        Agent bestAgent = null;
//        double maxConfidence = 0.0;
//
////        for (Agent agent : agents) {
////            if (agent.canHandle(task)) {
////                double confidence = agent.getConfidenceScore(task);
////                if (confidence > maxConfidence) {
////                    maxConfidence = confidence;
////                    bestAgent = agent;
////                }
////            }
////        }
//
//        return bestAgent;
//    }
//}
