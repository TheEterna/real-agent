package com.ai.agent.real.contract.spec;

import com.ai.agent.real.contract.spec.logging.*;

/**
 * Agent执行事件
 * 用于流式执行中传递实时状态和结果
 */
public class AgentExecutionEvent extends TraceInfo {

    private final EventType type;
    private final String message;
    private final Object data;

    private AgentExecutionEvent(EventType type, String message, Object data, Traceable traceInfo) {
        this.type = type;
        this.message = message;
        this.data = data;
        if (traceInfo != null) {
            this.setSessionId(traceInfo.getSessionId());
            this.setTraceId(traceInfo.getTraceId());
            this.setStartTime(traceInfo.getStartTime());
            this.setEndTime(traceInfo.getEndTime());
            this.setSpanId(traceInfo.getSpanId());
            this.setNodeId(traceInfo.getNodeId());
            this.setAgentId(traceInfo.getAgentId());
        }
    }

    public static AgentExecutionEvent started(String message) {
        return new AgentExecutionEvent(EventType.STARTED, message, null, null);
    }

    public static AgentExecutionEvent progress(String message, double progress) {
        return new AgentExecutionEvent(EventType.PROGRESS, message, progress, null);
    }

    public static AgentExecutionEvent agentSelected(Traceable traceInfo, String message) {
        return new AgentExecutionEvent(EventType.AGENT_SELECTED, message, null, traceInfo);
    }

    public static AgentExecutionEvent thinking(Traceable traceInfo, String thought) {
        return new AgentExecutionEvent(EventType.THINKING, thought, null, traceInfo);
    }

    public static AgentExecutionEvent action(Traceable traceInfo, String action) {
        return new AgentExecutionEvent(EventType.ACTING, action, null, traceInfo);
    }
    public static AgentExecutionEvent tool(Traceable traceInfo, String action) {
        return new AgentExecutionEvent(EventType.TOOL, action, null, traceInfo);
    }

    public static AgentExecutionEvent observing(Traceable traceInfo, String observation) {
        return new AgentExecutionEvent(EventType.OBSERVING, observation, null, traceInfo);
    }
    public static AgentExecutionEvent executing(Traceable traceInfo, String execution) {
        return new AgentExecutionEvent(EventType.EXECUTING, execution, null, traceInfo);
    }

    public static AgentExecutionEvent partialResult(Traceable traceInfo, String result) {
        return new AgentExecutionEvent(EventType.PARTIAL_RESULT, result, null, traceInfo);
    }

    public static AgentExecutionEvent collaborating(Traceable traceInfo, String message) {
        return new AgentExecutionEvent(EventType.COLLABORATING, message, null, traceInfo);
    }

    public static AgentExecutionEvent doneWithWarning(String message) {
        return new AgentExecutionEvent(EventType.DONEWITHWARNING, message, null, null);
    }

    public static AgentExecutionEvent done(String message) {
        return new AgentExecutionEvent(EventType.DONE, message, null, null);
    }

    public static AgentExecutionEvent error(Throwable error) {
        return new AgentExecutionEvent(EventType.ERROR, error.getMessage(), null, null);
    }
    public static AgentExecutionEvent error(String errorMessage) {
        return new AgentExecutionEvent(EventType.ERROR, errorMessage, null, null);
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


    public enum EventType {
        STARTED,        // 开始执行
        PROGRESS,       // 执行进度
        AGENT_SELECTED, // Agent选择
        THINKING,       // Agent思考中
        ACTING,         // Agent执行行动
        OBSERVING,      // Agent观察结果
        COLLABORATING,  // Agent协作
        PARTIAL_RESULT, // 部分结果
        DONE,      // 执行完成
        EXECUTING,      // 执行中
        ERROR,          // 执行错误
        TOOL,          // 工具调用
        DONEWITHWARNING; // 执行完成，有警告


    }

}
