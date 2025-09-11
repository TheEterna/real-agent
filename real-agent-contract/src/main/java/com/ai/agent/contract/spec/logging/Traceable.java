package com.ai.agent.contract.spec.logging;

import java.time.*;

public interface Traceable {


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

    String getNodeId();
    Traceable setNodeId(String nodeId);

//    String getParentSpanId();
//    Traceable setParentSpanId(String parentSpanId);


    String getAgentId();
    Traceable setAgentId(String agentId);

}
