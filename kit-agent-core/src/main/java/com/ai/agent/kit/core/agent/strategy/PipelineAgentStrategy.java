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
//
///**
// * 流水线Agent策略实现
// * Agent按顺序处理任务的不同阶段，前一个Agent的输出作为后一个Agent的输入
// *
// * @author han
// * @time 2025/9/7 00:10
// */
//@Slf4j
//public class PipelineAgentStrategy implements AgentStrategy {
//
//    @Override
//    public String getStrategyName() {
//        return "Pipeline";
//    }
//
//    @Override
//    public String getDescription() {
//        return "Agent按顺序处理任务的不同阶段，形成处理流水线";
//    }
//
//    @Override
//    public AgentResult execute(String task, List<Agent> agents, AgentContext context) {
//        if (agents == null || agents.size() < 2) {
//            return AgentResult.failure("流水线策略需要至少2个Agent", "PipelineStrategy");
//        }
//
//        // 筛选能够处理该任务的Agent
//        List<Agent> capableAgents = agents.stream()
//                .filter(agent -> agent.canHandle(task))
////                .sorted((a1, a2) -> Double.compare(a2.getConfidenceScore(task), a1.getConfidenceScore(task)))
//                .toList();
//
//        if (capableAgents.size() < 2) {
//            return AgentResult.failure("流水线策略需要至少2个能处理该任务的Agent", "PipelineStrategy");
//        }
//
//        log.info("启动流水线策略，参与Agent数量: {}", capableAgents.size());
//
//        try {
//            LocalDateTime startTime = LocalDateTime.now();
//            String currentInput = task;
//            List<AgentResult> stageResults = new ArrayList<>();
//
//            // 按顺序执行每个Agent
//            for (int i = 0; i < capableAgents.size(); i++) {
//                Agent agent = capableAgents.get(i);
//                log.info("流水线第{}阶段，Agent [{}] 开始处理", i + 1, agent.getAgentName());
//
//                try {
//                    AgentResult stageResult = agent.execute(currentInput, context);
//                    stageResults.add(stageResult);
//
//                    if (!stageResult.isSuccess()) {
//                        log.warn("流水线第{}阶段失败，Agent [{}]: {}",
//                                i + 1, agent.getAgentName(), stageResult.getErrorMessage());
//
//                        // 如果某个阶段失败，可以选择继续或终止
//                        // 这里选择继续，但标记为部分成功
//                        currentInput = "前一阶段处理失败: " + stageResult.getErrorMessage() + "\n原始任务: " + currentInput;
//                    } else {
//                        // 成功则将结果作为下一阶段的输入
//                        currentInput = stageResult.getResult();
//                        log.info("流水线第{}阶段完成，Agent [{}]", i + 1, agent.getAgentName());
//                    }
//
//                } catch (Exception e) {
//                    log.error("流水线第{}阶段异常，Agent [{}]", i + 1, agent.getAgentName(), e);
//                    AgentResult errorResult = AgentResult.failure("执行异常: " + e.getMessage(), agent.getAgentId());
//                    stageResults.add(errorResult);
//                    currentInput = "前一阶段异常: " + e.getMessage() + "\n原始任务: " + currentInput;
//                }
//            }
//
//            LocalDateTime endTime = LocalDateTime.now();
//
//            // 整合流水线结果
//            AgentResult finalResult = integratePipelineResults(stageResults, task, capableAgents);
//            finalResult.setStartTime(startTime)
//                      .setEndTime(endTime)
//                      .setExecutionTime(java.time.Duration.between(startTime, endTime).toMillis());
//
//            log.info("流水线策略执行完成，最终结果: {}", finalResult.isSuccess() ? "成功" : "失败");
//
//            return finalResult;
//
//        } catch (Exception e) {
//            log.error("流水线策略执行异常", e);
//            return AgentResult.failure("流水线执行异常: " + e.getMessage(), "PipelineStrategy");
//        }
//    }
//
//
//    /**
//     * 整合流水线结果
//     */
//    private AgentResult integratePipelineResults(List<AgentResult> stageResults, String originalTask, List<Agent> agents) {
//        if (stageResults.isEmpty()) {
//            return AgentResult.failure("没有任何阶段结果", "PipelineStrategy");
//        }
//
//        // 检查是否有成功的阶段
//        List<AgentResult> successResults = stageResults.stream()
//                .filter(AgentResult::isSuccess)
//                .toList();
//
//        boolean overallSuccess = !successResults.isEmpty();
//
//        // 构建最终结果
//        StringBuilder resultBuilder = new StringBuilder();
//        resultBuilder.append("=== 流水线处理结果 ===\n");
//        resultBuilder.append("原始任务: ").append(originalTask).append("\n\n");
//
//        for (int i = 0; i < stageResults.size(); i++) {
//            AgentResult stageResult = stageResults.get(i);
//            Agent agent = agents.get(i);
//
//            resultBuilder.append("阶段 ").append(i + 1).append(" [").append(agent.getAgentName()).append("]: ");
//            if (stageResult.isSuccess()) {
//                resultBuilder.append("✓ 成功\n");
//                resultBuilder.append(stageResult.getResult()).append("\n\n");
//            } else {
//                resultBuilder.append("✗ 失败\n");
//                resultBuilder.append("错误: ").append(stageResult.getErrorMessage()).append("\n\n");
//            }
//        }
//
//        // 如果最后一个阶段成功，使用其结果作为主要输出
//        AgentResult lastResult = stageResults.get(stageResults.size() - 1);
//        if (lastResult.isSuccess()) {
//            resultBuilder.append("=== 最终输出 ===\n");
//            resultBuilder.append(lastResult.getResult());
//        }
//
//        // 创建元数据
//        Map<String, Object> metadata = new HashMap<>();
//        metadata.put("pipelineAgents", agents.stream().map(Agent::getAgentId).toList());
//        metadata.put("stageCount", stageResults.size());
//        metadata.put("successStages", successResults.size());
//        metadata.put("stageResults", stageResults.stream().map(r -> Map.of(
//            "agentId", r.getAgentId(),
//            "success", r.isSuccess()
//        )).toList());
//
//        // 计算整体置信度
//
//        if (overallSuccess) {
//            return AgentResult.success(
//                    resultBuilder.toString(),
//                    "PipelineStrategy"
//            ).setMetadata(metadata);
//        } else {
//            return AgentResult.failure(
//                    "流水线处理失败:\n" + resultBuilder.toString(),
//                    "PipelineStrategy"
//            ).setMetadata(metadata);
//        }
//    }
//
//
//
//}
