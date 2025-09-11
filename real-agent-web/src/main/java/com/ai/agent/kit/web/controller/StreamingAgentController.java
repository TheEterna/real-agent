//package com.ai.agent.kit.web.controller;
//
//import com.ai.agent.contract.spec.*;
//
//import com.ai.agent.kit.core.agent.Agent;
//import com.ai.agent.contract.spec.AgentContext;
//import com.ai.agent.kit.core.agent.manager.AgentManager;
//import com.ai.agent.kit.core.agent.strategy.*;
//import com.ai.agent.kit.core.tool.*;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.ai.chat.model.*;
//import org.springframework.context.annotation.*;
//import org.springframework.http.MediaType;
//import org.springframework.web.bind.annotation.*;
//import reactor.core.publisher.Flux;
//
//import java.time.Duration;
//import java.util.List;
//import java.util.UUID;
//
///**
// * 流式Agent执行控制器
// * 提供Server-Sent Events (SSE) 的实时执行反馈
// *
// * @author han
// * @time 2025/9/8 15:45
// */
//@Slf4j
//@RestController
//@RequestMapping("/api/agent/stream")
//public class StreamingAgentController {
//
//    @Bean
//    public ReActAgentStrategy reAct(ChatModel chatModel, ToolRegistry toolRegistry) {
//        return new ReActAgentStrategy(chatModel, toolRegistry);
//    }
//
//
//    private final AgentManager agentManager;
//    private final AgentStrategy strategy;
//
//    public StreamingAgentController(AgentManager agentManager, ReActAgentStrategy agentStrategy) {
//        this.agentManager = agentManager;
//        this.strategy = agentStrategy;
//    }
//
//
//    /**
//     * 流式执行Agent任务
//     * 返回Server-Sent Events流，提供实时执行反馈
//     */
//    @PostMapping(value = "/execute", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
//    public Flux<String> executeTaskStream(@RequestBody StreamTaskRequest request) {
//        log.info("开始流式执行任务: {}", request.getTask());
//
//        // 创建执行上下文
//        AgentContext context = new AgentContext()
//                .setUserId(request.getUserId() != null ? request.getUserId() : "anonymous")
//                .setSessionId(UUID.randomUUID().toString())
//                .setTraceId(UUID.randomUUID().toString());
//
//        // 获取可用Agent
//        List<Agent> agents = agentManager.getAllAgents();
//
//        // 选择策略
//
//        // 执行流式任务
//        return strategy.executeStream(request.getTask(), agents, context)
//                .map(this::formatEventForSSE)
//                .doOnNext(event -> log.debug("发送SSE事件: {}", event))
//                .doOnComplete(() -> log.info("流式任务执行完成"))
//                .doOnError(error -> log.error("流式任务执行失败", error))
//                .onErrorResume(error -> Flux.just(formatErrorForSSE(error.getMessage())));
//    }
//
//    /**
//     * 获取Agent状态流
//     * 用于监控Agent的实时状态
//     */
//    @GetMapping(value = "/status", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
//    public Flux<String> getAgentStatusStream() {
//        return Flux.interval(Duration.ofSeconds(5))
//                .map(tick -> {
//                    var status = agentManager.getStatus();
//                    return formatStatusForSSE(status);
//                })
//                .doOnNext(status -> log.debug("发送状态更新: {}", status));
//    }
//
//    /**
//     * 获取可用的流式策略列表
//     */
//    @GetMapping("/strategies")
//    public List<String> getAvailableStrategies() {
//        return List.of("single", "collaborative");
//    }
//
//
//    /**
//     * 格式化执行事件为SSE格式
//     */
//    private String formatEventForSSE(AgentExecutionEvent event) {
//        return String.format("""
//                event: %s
//                data: {
//                data:   "type": "%s",
//                data:   "message": "%s",
//                data:   "timestamp": %d,
//                data:   "agentId": "%s"
//                data: }
//
//                """,
//                event.getType().name().toLowerCase(),
//                event.getType().name(),
//                escapeJson(event.getMessage()),
//                event.getTimestamp(),
//                event.getAgentId() != null ? event.getAgentId() : ""
//        );
//    }
//
//    /**
//     * 格式化错误为SSE格式
//     */
//    private String formatErrorForSSE(String errorMessage) {
//        return String.format("""
//                event: error
//                data: {
//                data:   "type": "ERROR",
//                data:   "message": "%s",
//                data:   "timestamp": %d
//                data: }
//
//                """,
//                escapeJson(errorMessage),
//                System.currentTimeMillis()
//        );
//    }
//
//    /**
//     * 格式化状态为SSE格式
//     */
//    private String formatStatusForSSE(Object status) {
//        return String.format("""
//                event: status
//                data: {
//                data:   "type": "STATUS_UPDATE",
//                data:   "data": %s,
//                data:   "timestamp": %d
//                data: }
//
//                """,
//                status.toString(),
//                System.currentTimeMillis()
//        );
//    }
//
//    /**
//     * 转义JSON字符串
//     */
//    private String escapeJson(String str) {
//        if (str == null) {
//            return "";
//        }
//        return str.replace("\"", "\\\"")
//                  .replace("\n", "\\n")
//                  .replace("\r", "\\r")
//                  .replace("\t", "\\t");
//    }
//
//    /**
//     * 流式任务请求DTO
//     */
//    public static class StreamTaskRequest {
//        private String task;
//        private String strategy;
//        private String userId;
//
//        // Getters and Setters
//        public String getTask() { return task; }
//        public void setTask(String task) { this.task = task; }
//
//        public String getStrategy() { return strategy; }
//        public void setStrategy(String strategy) { this.strategy = strategy; }
//
//        public String getUserId() { return userId; }
//        public void setUserId(String userId) { this.userId = userId; }
//    }
//}
