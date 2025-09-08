//package com.ai.agent.kit.core.agent.strategy;
//
//import com.ai.agent.kit.common.spec.*;
//import com.ai.agent.kit.core.agent.Agent;
//import com.ai.agent.kit.core.agent.communication.AgentContext;
//import lombok.extern.slf4j.Slf4j;
//import reactor.core.publisher.Flux;
//import reactor.core.scheduler.Schedulers;
//
//import java.util.List;
//import java.util.concurrent.CompletableFuture;
//import java.util.stream.Collectors;
//
///**
// * 流式协作策略
// * 多个Agent并行协作，提供实时的协作进度和中间结果
// *
// * @author han
// * @time 2025/9/8 15:40
// */
//@Slf4j
//public class StreamingCollaborativeStrategy implements AgentStrategy {
//
//    @Override
//    public String getStrategyName() {
//        return "StreamingCollaborative";
//    }
//
//    @Override
//    public String getDescription() {
//        return "流式协作策略：多Agent并行协作，实时反馈协作进度";
//    }
//
//    @Override
//    public Flux<AgentExecutionEvent> executeStream(String task, List<Agent> agents, AgentContext context) {
//        return Flux.create(sink -> {
//            try {
//                // 1. 开始协作
//                sink.next(AgentExecutionEvent.started("开始多Agent协作执行"));
//
//                // 2. 筛选能处理任务的Agent
//                List<Agent> capableAgents = agents.stream()
//                        .filter(agent -> agent.canHandle(task))
//                        .limit(3) // 限制协作Agent数量
//                        .collect(Collectors.toList());
//
//                if (capableAgents.isEmpty()) {
//                    sink.error(new RuntimeException("没有找到能处理该任务的Agent"));
//                    return;
//                }
//
//                sink.next(AgentExecutionEvent.progress(
//                    "选择了 " + capableAgents.size() + " 个Agent参与协作",
//                    0.1
//                ));
//
//                // 3. 并行执行多个Agent
//                executeAgentsCollaboratively(capableAgents, task, context, sink);
//
//            } catch (Exception e) {
//                log.error("流式协作执行异常", e);
//                sink.error(e);
//            }
//        });
//    }
//
//    @Override
//    public AgentResult execute(String task, List<Agent> agents, AgentContext context) {
//        // 兼容同步执行 - 简化版本
//        List<Agent> capableAgents = agents.stream()
//                .filter(agent -> agent.canHandle(task))
//                .limit(3)
//                .collect(Collectors.toList());
//
//        if (capableAgents.isEmpty()) {
//            return AgentResult.failure("没有找到能处理该任务的Agent", "StreamingCollaborativeStrategy");
//        }
//
//        // 执行第一个Agent作为主要结果
//        return capableAgents.get(0).execute(task, context);
//    }
//
//    /**
//     * 协作执行多个Agent
//     */
//    private void executeAgentsCollaboratively(List<Agent> agents, String task, AgentContext context,
//                                            reactor.core.publisher.FluxSink<AgentExecutionEvent> sink) {
//
//        // 为每个Agent创建异步执行任务
//        List<CompletableFuture<AgentResult>> futures = agents.stream()
//                .map(agent -> CompletableFuture.supplyAsync(() -> {
//                    // 发送Agent开始执行事件
//                    sink.next(AgentExecutionEvent.agentSelected(
//                        agent.getAgentId(),
//                        "Agent " + agent.getAgentName() + " 开始处理任务"
//                    ));
//
//                    // 模拟协作过程中的实时反馈
//                    simulateCollaborativeExecution(agent, task, sink);
//
//                    // 执行Agent
//                    AgentResult result = agent.execute(task, context);
//
//                    // 发送部分结果
//                    if (result.isSuccess()) {
//                        sink.next(AgentExecutionEvent.partialResult(
//                            agent.getAgentId(),
//                            "Agent " + agent.getAgentName() + " 完成了部分工作"
//                        ));
//                    }
//
//                    return result;
//                }, Schedulers.boundedElastic().createWorker().asExecutor()))
//                .collect(Collectors.toList());
//
//        // 等待所有Agent完成并整合结果
//        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
//                .thenApply(v -> futures.stream()
//                        .map(CompletableFuture::join)
//                        .collect(Collectors.toList()))
//                .thenAccept(results -> {
//                    // 协作完成
//                    sink.next(AgentExecutionEvent.collaborating(
//                        null,
//                        "所有Agent协作完成，正在整合结果..."
//                    ));
//
//                    // 整合结果
//                    AgentResult finalResult = integrateResults(results);
//
//                    // 发送最终结果
//                    sink.next(AgentExecutionEvent.completed(finalResult));
//                    sink.complete();
//                })
//                .exceptionally(throwable -> {
//                    log.error("协作执行失败", throwable);
//                    sink.error(throwable);
//                    return null;
//                });
//    }
//
//    /**
//     * 模拟协作执行过程
//     */
//    private void simulateCollaborativeExecution(Agent agent, String task,
//                                              reactor.core.publisher.FluxSink<AgentExecutionEvent> sink) {
//
//        // 思考阶段
//        sink.next(AgentExecutionEvent.thinking(
//            agent.getAgentId(),
//            agent.getAgentName() + " 正在分析任务的专业领域部分"
//        ));
//
//        simulateDelay(300);
//
//        // 协作沟通
//        sink.next(AgentExecutionEvent.collaborating(
//            agent.getAgentId(),
//            agent.getAgentName() + " 与其他Agent协调分工"
//        ));
//
//        simulateDelay(200);
//
//        // 执行行动
//        sink.next(AgentExecutionEvent.acting(
//            agent.getAgentId(),
//            agent.getAgentName() + " 开始执行专业任务"
//        ));
//    }
//
//    /**
//     * 整合多个Agent的结果
//     */
//    private AgentResult integrateResults(List<AgentResult> results) {
//        List<AgentResult> successResults = results.stream()
//                .filter(AgentResult::isSuccess)
//                .collect(Collectors.toList());
//
//        if (successResults.isEmpty()) {
//            return AgentResult.failure("所有Agent执行失败", "StreamingCollaborativeStrategy");
//        }
//
//        // 选择最好的结果作为主要结果
//        AgentResult bestResult = successResults.get(0);
//
//        // 整合其他成功结果的信息
//        StringBuilder integratedResult = new StringBuilder(bestResult.getResult());
//
//        for (int i = 1; i < successResults.size(); i++) {
//            integratedResult.append("\n\n--- 协作补充 ---\n")
//                           .append(successResults.get(i).getResult());
//        }
//
//        return AgentResult.success(integratedResult.toString(), "StreamingCollaborativeStrategy")
//                .setStartTime(bestResult.getStartTime())
//                .setEndTime(bestResult.getEndTime());
//    }
//
//    /**
//     * 模拟延迟
//     */
//    private void simulateDelay(long millis) {
//        try {
//            Thread.sleep(millis);
//        } catch (InterruptedException e) {
//            Thread.currentThread().interrupt();
//        }
//    }
//}
