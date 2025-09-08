//package com.ai.agent.kit.core.agent.strategy;
//
//import com.ai.agent.kit.common.spec.*;
//import com.ai.agent.kit.core.agent.Agent;
//import com.ai.agent.kit.core.agent.communication.AgentContext;
//import lombok.extern.slf4j.Slf4j;
//import reactor.core.publisher.Flux;
//import reactor.core.publisher.Mono;
//
//import java.time.LocalDateTime;
//import java.util.List;
//
///**
// * 流式单Agent策略
// * 提供实时的执行进度反馈和中间结果
// *
// * @author han
// * @time 2025/9/8 15:35
// */
//@Slf4j
//public class StreamingSingleAgentStrategy implements AgentStrategy {
//
//    @Override
//    public String getStrategyName() {
//        return "StreamingSingle";
//    }
//
//    @Override
//    public String getDescription() {
//        return "流式单Agent策略：实时反馈执行进度和中间结果";
//    }
//
//    @Override
//    public Flux<AgentExecutionEvent> executeStream(String task, List<Agent> agents, AgentContext context) {
//        return Flux.create(sink -> {
//            try {
//                // 1. 开始执行
//                sink.next(AgentExecutionEvent.started("开始分析任务和选择Agent"));
//
//                // 2. 选择最适合的Agent
//                Agent bestAgent = selectBestAgent(task, agents);
//                if (bestAgent == null) {
//                    sink.error(new RuntimeException("没有找到适合处理该任务的Agent"));
//                    return;
//                }
//
//                sink.next(AgentExecutionEvent.agentSelected(
//                    bestAgent.getAgentId(),
//                    "选择Agent: " + bestAgent.getAgentName()
//                ));
//
//                // 3. 模拟Agent执行过程中的实时反馈
//                executeAgentWithStreaming(bestAgent, task, context, sink);
//
//            } catch (Exception e) {
//                log.error("流式执行异常", e);
//                sink.error(e);
//            }
//        });
//    }
//
//    @Override
//    public AgentResult execute(String task, List<Agent> agents, AgentContext context) {
//        // 兼容同步执行
//        Agent bestAgent = selectBestAgent(task, agents);
//        if (bestAgent == null) {
//            return AgentResult.failure("没有找到适合处理该任务的Agent", "StreamingSingleAgentStrategy");
//        }
//
//        return bestAgent.execute(task, context);
//    }
//
//    /**
//     * 选择最适合的Agent
//     */
//    private Agent selectBestAgent(String task, List<Agent> agents) {
//        return agents.stream()
//                .filter(agent -> agent.canHandle(task))
//                .findFirst()
//                .orElse(null);
//    }
//
//    /**
//     * 带流式反馈的Agent执行
//     */
//    private void executeAgentWithStreaming(Agent agent, String task, AgentContext context,
//                                         reactor.core.publisher.FluxSink<AgentExecutionEvent> sink) {
//
//        // 模拟ReAct循环的流式反馈
//        Mono.fromCallable(() -> {
//            LocalDateTime startTime = LocalDateTime.now();
//
//            // 思考阶段
//            sink.next(AgentExecutionEvent.thinking(agent.getAgentId(), "正在分析任务需求..."));
//            simulateDelay(500); // 模拟思考时间
//
//            sink.next(AgentExecutionEvent.thinking(agent.getAgentId(), "确定处理策略和所需工具..."));
//            simulateDelay(300);
//
//            // 行动阶段
//            sink.next(AgentExecutionEvent.acting(agent.getAgentId(), "开始执行任务处理逻辑"));
//            simulateDelay(800);
//
//            // 观察阶段
//            sink.next(AgentExecutionEvent.observing(agent.getAgentId(), "分析执行结果..."));
//            simulateDelay(400);
//
//            // 部分结果
//            sink.next(AgentExecutionEvent.partialResult(agent.getAgentId(), "已完成初步分析，正在生成详细结果..."));
//            simulateDelay(600);
//
//            // 实际执行Agent
//            AgentResult result = agent.execute(task, context);
//
//            // 完成
//            sink.next(AgentExecutionEvent.completed(result));
//            sink.complete();
//
//            return result;
//        })
//        .subscribeOn(reactor.core.scheduler.Schedulers.boundedElastic())
//        .subscribe(
//            result -> log.info("Agent执行完成: {}", result.isSuccess()),
//            error -> {
//                log.error("Agent执行失败", error);
//                sink.error(error);
//            }
//        );
//    }
//
//    /**
//     * 模拟延迟（实际实现中应该是真实的处理时间）
//     */
//    private void simulateDelay(long millis) {
//        try {
//            Thread.sleep(millis);
//        } catch (InterruptedException e) {
//            Thread.currentThread().interrupt();
//        }
//    }
//}
