package com.ai.agent.kit.common.spec.logging;

public interface Traceable {
    String getUserId();
    Traceable setUserId(String userId);

    String getSessionId();
    Traceable setSessionId(String sessionId);

    String getTraceId();
    Traceable setTraceId(String traceId);

    long getStartTime();
    Traceable setStartTime(long startTime);

    long getEndTime();
    Traceable setEndTime(long endTime);

    String getSpanId();
    Traceable setSpanId(String spanId);

    String getParentSpanId();
    Traceable setParentSpanId(String parentSpanId);
}
