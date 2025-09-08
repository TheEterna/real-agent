package com.ai.agent.kit.core.agent.communication;

import com.ai.agent.kit.common.spec.logging.*;
import lombok.*;
import lombok.experimental.*;

import java.util.Map;


/**
 * AgentContext 类定义了工具执行的上下文信息。
 * @author han
 * @time 2025/8/30 17:00
 */
@Data
@Accessors(chain = true)
public class AgentContext implements Traceable {

    /**
     * 组合的追踪信息对象（推荐使用）
     */
    private TraceInfo trace;

    /**
     * 工具执行的参数
     */
    private Map<String, Object> args;

    /**
     * 工具执行的超时时间，单位毫秒
     */
    private long timeoutMs = 15000;
    
    /**
     * 对话历史记录
     */
    private StringBuilder conversationHistory = new StringBuilder();
    
    /**
     * 最近的思考结果
     */
    private String lastThinking;
    
    /**
     * 最近的行动结果
     */
    private String lastAction;
    
    /**
     * 最近的工具执行结果
     */
    private String lastToolResult;
    
    /**
     * 当前迭代轮次
     */
    private int currentIteration = 0;

    // ========== 新增字段的getter/setter方法 ==========
    
    public StringBuilder getConversationHistory() {
        return conversationHistory;
    }
    
    public AgentContext setConversationHistory(StringBuilder conversationHistory) {
        this.conversationHistory = conversationHistory;
        return this;
    }
    
    public String getLastThinking() {
        return lastThinking;
    }
    
    public AgentContext setLastThinking(String lastThinking) {
        this.lastThinking = lastThinking;
        return this;
    }
    
    public String getLastAction() {
        return lastAction;
    }
    
    public AgentContext setLastAction(String lastAction) {
        this.lastAction = lastAction;
        return this;
    }
    
    public String getLastToolResult() {
        return lastToolResult;
    }
    
    public AgentContext setLastToolResult(String lastToolResult) {
        this.lastToolResult = lastToolResult;
        return this;
    }
    
    public int getCurrentIteration() {
        return currentIteration;
    }
    
    public AgentContext setCurrentIteration(int currentIteration) {
        this.currentIteration = currentIteration;
        return this;
    }

    public TraceInfo getTrace() {
        return trace;
    }

    public AgentContext setTrace(TraceInfo trace) {
        this.trace = trace;
        return this;
    }

    @Override
    public String getUserId() {
        return trace.getUserId();
    }

    @Override
    public AgentContext setUserId(String userId) {
        if (this.trace == null) {
            this.trace = new TraceInfo();
        }
        this.trace.setUserId(userId);
        return this;
    }

    @Override
    public String getSessionId() {
        return trace.getSessionId();
    }

    @Override
    public AgentContext setSessionId(String sessionId) {
        if (this.trace == null) {
            this.trace = new TraceInfo();
        }
        this.trace.setSessionId(sessionId);
        return this;
    }

    @Override
    public String getTraceId() {
        return trace.getTraceId();
    }

    @Override
    public AgentContext setTraceId(String traceId) {
        if (this.trace == null) {
            this.trace = new TraceInfo();
        }
        this.trace.setTraceId(traceId);
        return this;
    }

    @Override
    public long getStartTime() {
        return trace.getStartTime();
    }

    @Override
    public AgentContext setStartTime(long startTime) {
        if (this.trace == null) {
            this.trace = new TraceInfo();
        }
        this.trace.setStartTime(startTime);
        return this;
    }

    @Override
    public long getEndTime() {
        return trace.getEndTime();
    }

    @Override
    public AgentContext setEndTime(long endTime) {
        if (this.trace == null) {
            this.trace = new TraceInfo();
        }
        this.trace.setEndTime(endTime);
        return this;
    }

    @Override
    public String getSpanId() {
        return trace.getSpanId();
    }

    @Override
    public AgentContext setSpanId(String spanId) {
        if (this.trace == null) {
            this.trace = new TraceInfo();
        }
        this.trace.setSpanId(spanId);
        return this;
    }

    @Override
    public String getParentSpanId() {
        return trace.getParentSpanId();
    }

    @Override
    public AgentContext setParentSpanId(String parentSpanId) {
        if (this.trace == null) {
            this.trace = new TraceInfo();
        }
        this.trace.setParentSpanId(parentSpanId);
        return this;
    }
}
