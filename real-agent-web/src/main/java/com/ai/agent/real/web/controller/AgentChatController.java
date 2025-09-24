package com.ai.agent.real.web.controller;


import com.ai.agent.real.agent.strategy.*;
import com.ai.agent.real.common.spec.logging.*;
import com.ai.agent.real.contract.spec.AgentContext;
import com.ai.agent.real.common.protocol.AgentExecutionEvent;
import com.ai.agent.real.contract.spec.message.AgentMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Agent对话控制器
 * 提供ReAct框架的Web接口
 *
 * @author han
 * @time 2025/9/10 10:45
 */
@Slf4j
@RestController
@RequestMapping("/api/agent/chat")
@CrossOrigin(origins = "*")
public class AgentChatController {

    private final ReActAgentStrategy reActAgentStrategy;

    public AgentChatController(
                               ReActAgentStrategy reActAgentStrategy) {
        this.reActAgentStrategy = reActAgentStrategy;
    }

    /**
     * 执行ReAct任务（流式响应）
     */
    @PostMapping(value = "/react/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<AgentExecutionEvent> executeReActStream(@RequestBody ChatRequest request) {
        log.info("收到ReAct流式执行请求: {}", request.getMessage());

        try {
            // 创建执行上下文
            AgentContext context = new AgentContext(new TraceInfo())
                    .setSessionId(request.getSessionId())
                    .setTraceId(generateTraceId())
                    .setStartTime(LocalDateTime.now());


            // 执行ReAct流式任务
            return reActAgentStrategy.executeStream(request.getMessage(), null, context)
                    // 将流中的异常转换为一个错误事件，避免直接以错误终止连接
                    .onErrorResume(error -> {
                        log.error("ReAct执行异常(流内)", error);
                        return Flux.just(AgentExecutionEvent.error(error));
                    })
                    .doOnError(error -> log.error("ReAct执行异常", error))
                    .doOnComplete(() -> {
                        context.setEndTime(LocalDateTime.now());
                        log.info("ReAct任务执行完成");
                    });

        } catch (Exception e) {
            log.error("ReAct执行异常", e);
            return Flux.just(AgentExecutionEvent.error(e));
        }
    }



    /**
     * 获取对话历史
     */
    @GetMapping("/history/{sessionId}")
    public ChatResponse getChatHistory(@PathVariable String sessionId) {
        // TODO: 实现对话历史存储和查询
        return ChatResponse.builder()
                .success(true)
                .message("对话历史查询功能待实现")
                .sessionId(sessionId)
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * 清空对话历史
     */
    @DeleteMapping("/history/{sessionId}")
    public ChatResponse clearChatHistory(@PathVariable String sessionId) {
        // TODO: 实现对话历史清空
        return ChatResponse.builder()
                .success(true)
                .message("对话历史已清空")
                .sessionId(sessionId)
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * 获取可用的Agent类型
     */
    @GetMapping("/types")
    public Map<String, Object> getAgentTypes() {
        return Map.of(
                "types", new String[]{"ReActAgentStrategy", "代码编写"},
                "default", "ReActAgentStrategy",
                "description", Map.of(
                        "ReActAgentStrategy", "基于推理-行动-观察的智能Agent框架",
                        "代码编写", "专门用于代码生成和编程任务的Agent"
                )
        );
    }

    /**
     * 生成追踪ID
     */
    private String generateTraceId() {
        return "trace-" + System.currentTimeMillis() + "-" +
                Integer.toHexString((int)(Math.random() * 0x10000));
    }

    /**
     * 聊天请求DTO
     */
    public static class ChatRequest {
        private String message;
        private String userId;
        private String sessionId;
        private String agentType = "ReActAgentStrategy";

        // Getters and Setters
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }

        public String getSessionId() { return sessionId; }
        public void setSessionId(String sessionId) { this.sessionId = sessionId; }

        public String getAgentType() { return agentType; }
        public void setAgentType(String agentType) { this.agentType = agentType; }
    }

    /**
     * 聊天响应DTO
     */
    public static class ChatResponse {
        private boolean success;
        private String message;
        private String agentId;
        private String sessionId;
        private LocalDateTime timestamp;
        private java.util.List<AgentMessage> conversationHistory;

        public static ChatResponseBuilder builder() {
            return new ChatResponseBuilder();
        }

        // Getters and Setters
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        public String getAgentId() { return agentId; }
        public void setAgentId(String agentId) { this.agentId = agentId; }

        public String getSessionId() { return sessionId; }
        public void setSessionId(String sessionId) { this.sessionId = sessionId; }

        public LocalDateTime getTimestamp() { return timestamp; }
        public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }

        public java.util.List<AgentMessage> getConversationHistory() { return conversationHistory; }
        public void setConversationHistory(java.util.List<AgentMessage> conversationHistory) {
            this.conversationHistory = conversationHistory;
        }

        public static class ChatResponseBuilder {
            private ChatResponse response = new ChatResponse();

            public ChatResponseBuilder success(boolean success) {
                response.setSuccess(success);
                return this;
            }

            public ChatResponseBuilder message(String message) {
                response.setMessage(message);
                return this;
            }

            public ChatResponseBuilder agentId(String agentId) {
                response.setAgentId(agentId);
                return this;
            }

            public ChatResponseBuilder sessionId(String sessionId) {
                response.setSessionId(sessionId);
                return this;
            }

            public ChatResponseBuilder timestamp(LocalDateTime timestamp) {
                response.setTimestamp(timestamp);
                return this;
            }

            public ChatResponseBuilder conversationHistory(java.util.List<AgentMessage> history) {
                response.setConversationHistory(history);
                return this;
            }

            public ChatResponse build() {
                return response;
            }
        }
    }
}
