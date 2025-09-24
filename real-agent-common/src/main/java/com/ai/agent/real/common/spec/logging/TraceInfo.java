package com.ai.agent.real.common.spec.logging;

import com.ai.agent.real.contract.spec.logging.*;

import java.time.*;

/**
 * TraceInfo 值对象：承载追踪/审计相关字段。
 * 作为组合到业务上下文中的独立载体，便于模块解耦与后续扩展。
 */
public class TraceInfo implements Traceable {

    // TODO
    /** 会话ID */
    protected String sessionId;
    /** 跟踪ID */
    protected String traceId;
    /** 起始时间戳（毫秒） */
    protected LocalDateTime startTime;
    /** 截止时间戳（毫秒），0 表示未设置 */
    protected LocalDateTime endTime;
    /** spanId（预留分布式追踪） */
    protected String spanId;
    /** parentSpanId（预留分布式追踪） */
    protected String nodeId;

    protected String agentId;


    @Override
    public String getSessionId() {
        return sessionId;
    }

    @Override
    public Traceable setSessionId(String sessionId) {
        this.sessionId = sessionId;
        return this;
    }

    @Override
    public String getTraceId() {
        return traceId;
    }

    @Override
    public Traceable setTraceId(String traceId) {
        this.traceId = traceId;
        return this;
    }

    @Override
    public LocalDateTime getStartTime() {
        return startTime;
    }



    @Override
    public Traceable setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
        return this;
    }

    @Override
    public LocalDateTime getEndTime() {
        return endTime;
    }


    @Override
    public Traceable setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
        return this;
    }

    @Override
    public String getSpanId() {
        return spanId;
    }

    @Override
    public Traceable setSpanId(String spanId) {
        this.spanId = spanId;
        return this;
    }

    @Override
    public String getNodeId() {
        return nodeId;
    }

    @Override
    public Traceable setNodeId(String nodeId) {
        this.nodeId = nodeId;
        return this;
    }


    @Override
    public String getAgentId() {
        return agentId;
    }

    @Override
    public Traceable setAgentId(String agentId) {
        this.agentId = agentId;
        return this;
    }

}
