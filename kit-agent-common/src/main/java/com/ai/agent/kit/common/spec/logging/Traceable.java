package com.ai.agent.kit.common.spec.logging;

import java.time.*;

public interface Traceable {
    String getUserId();
    Traceable setUserId(String userId);

    String getSessionId();
    Traceable setSessionId(String sessionId);

    String getTraceId();
    Traceable setTraceId(String traceId);

    LocalDateTime getStartTime();
    Traceable setStartTime(LocalDateTime startTime);

    LocalDateTime getEndTime();
    Traceable setEndTime(LocalDateTime endTime);

    String getSpanId();
    Traceable setSpanId(String spanId);

    String getParentSpanId();
    Traceable setParentSpanId(String parentSpanId);
}
