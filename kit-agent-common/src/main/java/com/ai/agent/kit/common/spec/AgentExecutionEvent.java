package com.ai.agent.kit.common.spec;

/**
 * Agent执行事件
 * 用于流式执行中传递实时状态和结果
 */
public class AgentExecutionEvent {
    public enum EventType {
        STARTED,        // 开始执行
        PROGRESS,       // 执行进度
        AGENT_SELECTED, // Agent选择
        THINKING,       // Agent思考中
        ACTING,         // Agent执行行动
        OBSERVING,      // Agent观察结果
        COLLABORATING,  // Agent协作
        PARTIAL_RESULT, // 部分结果
        COMPLETED,      // 执行完成
        EXECUTING,      // 执行中
        ERROR          // 执行错误
    }

    private final EventType type;
    private final String message;
    private final Object data;
    private final long timestamp;
    private final String agentId;

    private AgentExecutionEvent(EventType type, String message, Object data, String agentId) {
        this.type = type;
        this.message = message;
        this.data = data;
        this.agentId = agentId;
        this.timestamp = System.currentTimeMillis();
    }

    public static AgentExecutionEvent started(String message) {
        return new AgentExecutionEvent(EventType.STARTED, message, null, null);
    }

    public static AgentExecutionEvent progress(String message, double progress) {
        return new AgentExecutionEvent(EventType.PROGRESS, message, progress, null);
    }

    public static AgentExecutionEvent agentSelected(String agentId, String message) {
        return new AgentExecutionEvent(EventType.AGENT_SELECTED, message, null, agentId);
    }

    public static AgentExecutionEvent thinking(String agentId, String thought) {
        return new AgentExecutionEvent(EventType.THINKING, thought, null, agentId);
    }

    public static AgentExecutionEvent action(String agentId, String action) {
        return new AgentExecutionEvent(EventType.ACTING, action, null, agentId);
    }

    public static AgentExecutionEvent observing(String agentId, String observation) {
        return new AgentExecutionEvent(EventType.OBSERVING, observation, null, agentId);
    }
    public static AgentExecutionEvent executing(String agentId, String execution) {
        return new AgentExecutionEvent(EventType.EXECUTING, execution, null, agentId);
    }

    public static AgentExecutionEvent partialResult(String agentId, String result) {
        return new AgentExecutionEvent(EventType.PARTIAL_RESULT, result, null, agentId);
    }

    public static AgentExecutionEvent collaborating(String agentId, String message) {
        return new AgentExecutionEvent(EventType.COLLABORATING, message, null, agentId);
    }

    public static AgentExecutionEvent completed(String agentId, String message) {
        return new AgentExecutionEvent(EventType.COMPLETED, message, null, agentId);
    }

    public static AgentExecutionEvent error(String message, Throwable error) {
        return new AgentExecutionEvent(EventType.ERROR, message, error, null);
    }

    // Getters
    public EventType getType() {
        return type;
    }

    public String getMessage() {
        return message;
    }

    public Object getData() {
        return data;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public String getAgentId() {
        return agentId;
    }
}
