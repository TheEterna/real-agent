//package com.ai.agent.kit.core.agent.strategy;
//
//import com.ai.agent.kit.core.agent.Agent;
//import com.ai.agent.kit.common.spec.AgentResult;
//import com.ai.agent.kit.core.agent.communication.AgentContext;
//import lombok.extern.slf4j.Slf4j;
//
//import java.time.LocalDateTime;
//import java.util.ArrayList;
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//import java.util.concurrent.CompletableFuture;
//import java.util.concurrent.ExecutorService;
//import java.util.concurrent.Executors;
//
///**
// * 协作Agent策略实现
// * 多个Agent协同工作，每个Agent处理任务的不同方面，最后整合结果
// *
// * @author han
// * @time 2025/9/7 00:00
// */
//@Slf4j
//public class CollaborativeAgentStrategy implements AgentStrategy {
//
//    private final ExecutorService executorService = Executors.newCachedThreadPool();
//
//    @Override
//    public String getStrategyName() {
//        return "Collaborative";
//    }
//
//    @Override
//    public String getDescription() {
//        return "多个Agent协同工作，分别处理任务的不同方面并整合结果";
//    }
//
//    @Override
//    public AgentResult execute(String task, List<Agent> agents, AgentContext context) {
//        if (agents == null || agents.size() < 2) {
//            return AgentResult.failure("协作策略需要至少2个Agent", "CollaborativeStrategy");
//        }
//
//        // 筛选能够处理该任务的Agent
//        List<Agent> capableAgents = agents.stream()
//                .filter(agent -> agent.canHandle(task))
//                .toList();
//
//        if (capableAgents.isEmpty()) {
//            return AgentResult.failure("没有Agent能够处理该任务", "CollaborativeStrategy");
//        }
//
//        log.info("启动协作策略，参与Agent数量: {}", capableAgents.size());
//
//        try {
//            LocalDateTime startTime = LocalDateTime.now();
//
//            // 并行执行所有capable agents
//            List<CompletableFuture<AgentResult>> futures = new ArrayList<>();
//
//            for (Agent agent : capableAgents) {
//                CompletableFuture<AgentResult> future = CompletableFuture.supplyAsync(() -> {
//                    try {
//                        log.info("Agent [{}] 开始处理任务", agent.getAgentName());
//                        return agent.execute(task, context);
//                    } catch (Exception e) {
//                        log.error("Agent [{}] 执行异常", agent.getAgentName(), e);
//                        return AgentResult.failure("执行异常: " + e.getMessage(), agent.getAgentId());
//                    }
//                }, executorService);
//
//                futures.add(future);
//            }
//
//            // 等待所有Agent完成
//            CompletableFuture<Void> allFutures = CompletableFuture.allOf(
//                    futures.toArray(new CompletableFuture[0]));
//
//            allFutures.join(); // 等待所有任务完成
//
//            // 收集结果
//            List<AgentResult> results = new ArrayList<>();
//            for (CompletableFuture<AgentResult> future : futures) {
//                results.add(future.get());
//            }
//
//            LocalDateTime endTime = LocalDateTime.now();
//
//            // 整合结果
//            AgentResult finalResult = integrateResults(results, task);
//            finalResult.setStartTime(startTime)
//                      .setEndTime(endTime)
//                      .setExecutionTime(java.time.Duration.between(startTime, endTime).toMillis());
//
//            log.info("协作策略执行完成，最终结果: {}", finalResult.isSuccess() ? "成功" : "失败");
//
//            return finalResult;
//
//        } catch (Exception e) {
//            log.error("协作策略执行异常", e);
//            return AgentResult.failure("协作执行异常: " + e.getMessage(), "CollaborativeStrategy");
//        }
//    }
//
//
//
//
//    /**
//     * 整合多个Agent的执行结果
//     */
//    private AgentResult integrateResults(List<AgentResult> results, String task) {
//        List<AgentResult> successResults = results.stream()
//                .filter(AgentResult::isSuccess)
//                .toList();
//
//        if (successResults.isEmpty()) {
//            // 所有Agent都失败了
//            String errorMessages = results.stream()
//                    .map(AgentResult::getErrorMessage)
//                    .reduce("", (a, b) -> a + "; " + b);
//
//            return AgentResult.failure("所有Agent执行失败: " + errorMessages, "CollaborativeStrategy");
//        }
//
//        // 选择置信度最高的结果作为主要结果
//        AgentResult bestResult = successResults.stream()
//                .max((r1, r2) -> Double.compare(r1.getExecutionTime(), r2.getExecutionTime()))
//                .orElse(successResults.get(0));
//
//        // 整合其他成功结果的信息
//        StringBuilder integratedResult = new StringBuilder(bestResult.getResult());
//
//        Map<String, Object> metadata = new HashMap<>();
//        metadata.put("participantAgents", results.stream().map(AgentResult::getAgentId).toList());
//        metadata.put("successCount", successResults.size());
//        metadata.put("totalCount", results.size());
//
//        // 添加其他Agent的补充信息
//        for (AgentResult result : successResults) {
//            if (!result.getAgentId().equals(bestResult.getAgentId())) {
//                integratedResult.append("\n\n[补充信息来自 ").append(result.getAgentId()).append("]: ")
//                              .append(result.getResult());
//            }
//        }
//
//        return AgentResult.success(
//                integratedResult.toString(),
//                "CollaborativeStrategy"
//        ).setMetadata(metadata);
//    }
//
//    /**
//     * 计算整合后的置信度
//     */
////    private double calculateIntegratedConfidence(List<AgentResult> successResults) {
////        if (successResults.isEmpty()) {
////            return 0.0;
////        }
////
////        // 使用加权平均，成功的Agent越多，置信度越高
////        double avgConfidence = successResults.stream()
////                .mapToDouble(AgentResult::getExecutionTime)
////                .average()
////                .orElse(0.0);
////
////        // 协作加成：多个Agent成功会提升整体置信度
////        double collaborationBonus = Math.min(0.2, (successResults.size() - 1) * 0.1);
////
////        return Math.min(1.0, avgConfidence + collaborationBonus);
////    }
//}
