//package com.ai.agent.real.core.agent.strategy;
//
//import com.ai.agent.real.core.agent.Agent;
//import com.ai.agent.kit.common.spec.AgentResult;
//import com.ai.agent.contract.spec.AgentContext;
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
//import java.util.concurrent.TimeUnit;
//
///**
// * 竞争Agent策略实现
// * 多个Agent并行处理同一任务，选择最优结果
// *
// * @author han
// * @time 2025/9/7 00:05
// */
//@Slf4j
//public class CompetitiveAgentStrategy implements AgentStrategy {
//
//    private final ExecutorService executorService = Executors.newCachedThreadPool();
//    private final long timeoutSeconds = 30; // 超时时间
//
//    @Override
//    public String getStrategyName() {
//        return "Competitive";
//    }
//
//    @Override
//    public String getDescription() {
//        return "多个Agent并行处理同一任务，选择最优结果";
//    }
//
//    @Override
//    public AgentResult execute(String task, List<Agent> agents, AgentContext context) {
//        if (agents == null || agents.size() < 2) {
//            return AgentResult.failure("竞争策略需要至少2个Agent", "CompetitiveStrategy");
//        }
//
//        // 筛选能够处理该任务的Agent
//        List<Agent> capableAgents = agents.stream()
//                .filter(agent -> agent.canHandle(task))
//                .toList();
//
//        if (capableAgents.size() < 2) {
//            return AgentResult.failure("竞争策略需要至少2个能处理该任务的Agent", "CompetitiveStrategy");
//        }
//
//        log.info("启动竞争策略，参与竞争的Agent数量: {}", capableAgents.size());
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
//                        log.info("Agent [{}] 开始竞争执行任务", agent.getAgentName());
//                        return agent.execute(task, context);
//                    } catch (Exception e) {
//                        log.error("Agent [{}] 竞争执行异常", agent.getAgentName(), e);
//                        return AgentResult.failure("执行异常: " + e.getMessage(), agent.getAgentId());
//                    }
//                }, executorService);
//
//                futures.add(future);
//            }
//
//            // 等待所有Agent完成或超时
//            CompletableFuture<Void> allFutures = CompletableFuture.allOf(
//                    futures.toArray(new CompletableFuture[0]));
//
//            try {
//                allFutures.get(timeoutSeconds, TimeUnit.SECONDS);
//            } catch (Exception e) {
//                log.warn("部分Agent执行超时，将使用已完成的结果");
//            }
//
//            // 收集已完成的结果
//            List<AgentResult> results = new ArrayList<>();
//            for (CompletableFuture<AgentResult> future : futures) {
//                if (future.isDone() && !future.isCancelled()) {
//                    try {
//                        results.add(future.get());
//                    } catch (Exception e) {
//                        log.error("获取Agent结果异常", e);
//                    }
//                }
//            }
//
//            LocalDateTime endTime = LocalDateTime.now();
//
//            if (results.isEmpty()) {
//                return AgentResult.failure("所有Agent都未能完成任务", "CompetitiveStrategy");
//            }
//
//            // 选择最优结果
//            AgentResult bestResult = selectBestResult(results, task);
//            bestResult.setStartTime(startTime)
//                     .setEndTime(endTime)
//                     .setExecutionTime(java.time.Duration.between(startTime, endTime).toMillis());
//
//            // 添加竞争元数据
//            Map<String, Object> metadata = new HashMap<>();
//            metadata.put("competitorAgents", results.stream().map(AgentResult::getAgentId).toList());
//            metadata.put("completedCount", results.size());
//            metadata.put("totalCount", capableAgents.size());
//            metadata.put("winnerAgent", bestResult.getAgentId());
//            bestResult.setMetadata(metadata);
//
//            log.info("竞争策略执行完成，获胜Agent: [{}]，结果: {}",
//                    bestResult.getAgentId(), bestResult.isSuccess() ? "成功" : "失败");
//
//            return bestResult;
//
//        } catch (Exception e) {
//            log.error("竞争策略执行异常", e);
//            return AgentResult.failure("竞争执行异常: " + e.getMessage(), "CompetitiveStrategy");
//        }
//    }
//
//
//
//    /**
//     * 选择最优结果
//     * 优先级：成功 > 置信度 > 执行时间
//     */
//    private AgentResult selectBestResult(List<AgentResult> results, String task) {
//        // 首先筛选成功的结果
//        List<AgentResult> successResults = results.stream()
//                .filter(AgentResult::isSuccess)
//                .toList();
//
//        if (!successResults.isEmpty()) {
//            // 在成功结果中选择置信度最高的
//            return successResults.stream()
//                    .max((r1, r2) -> {
//                        // 首先比较置信度
////                        int confidenceCompare = Double.compare(r1.getConfidenceScore(), r2.getConfidenceScore());
////                        if (confidenceCompare != 0) {
////                            return confidenceCompare;
////                        }
//                        // 置信度相同时，选择执行时间更短的
//                        return Long.compare(r2.getExecutionTime(), r1.getExecutionTime());
//                    })
//                    .orElse(successResults.get(0));
//        } else {
//            // 如果都失败了，选择错误信息最详细的
//            return results.stream()
//                    .max((r1, r2) -> {
//                        String err1 = r1.getErrorMessage() != null ? r1.getErrorMessage() : "";
//                        String err2 = r2.getErrorMessage() != null ? r2.getErrorMessage() : "";
//                        return Integer.compare(err1.length(), err2.length());
//                    })
//                    .orElse(results.get(0));
//        }
//    }
//}
