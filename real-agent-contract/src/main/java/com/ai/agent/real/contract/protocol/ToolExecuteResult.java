package com.ai.agent.real.contract.protocol;

import java.io.Serializable;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class ToolExecuteResult implements Serializable {
    private static final long serialVersionUID = 1L;

    private boolean ok;
    private String message;
    private String toolName;
    private Object result;
    private List<ToolLogEntry> logs = new ArrayList<>();
    private Map<String, Object> metrics = new HashMap<>();
    private String traceId;
    private Timing timing;

    public static ToolExecuteResult ok(String toolName, Object result){
        ToolExecuteResult r = new ToolExecuteResult();
        r.ok = true;
        r.toolName = toolName;
        r.result = result;
        return r;
    }

    public static ToolExecuteResult fail(String toolName, String message){
        ToolExecuteResult r = new ToolExecuteResult();
        r.ok = false;
        r.toolName = toolName;
        r.message = message;
        return r;
    }

    public ToolExecuteResult withLogs(List<ToolLogEntry> logs){
        if (logs != null) this.logs = logs; return this;
    }
    public ToolExecuteResult addLog(ToolLogEntry log){ this.logs.add(log); return this; }
    public ToolExecuteResult withMetrics(Map<String,Object> metrics){ if (metrics!=null) this.metrics = metrics; return this; }
    public ToolExecuteResult putMetric(String k, Object v){ this.metrics.put(k,v); return this; }
    public ToolExecuteResult withTraceId(String traceId){ this.traceId = traceId; return this; }
    public ToolExecuteResult withTiming(long startMs, long endMs){
        Timing t = new Timing(
                formatIso(startMs),
                formatIso(endMs),
                endMs >= startMs ? (endMs - startMs) : null
        );
        this.timing = t; return this;
    }

    private static String formatIso(long epochMs){
        return DateTimeFormatter.ISO_OFFSET_DATE_TIME.withZone(ZoneOffset.UTC).format(Instant.ofEpochMilli(epochMs));
    }

    public boolean isOk() { return ok; }
    public void setOk(boolean ok) { this.ok = ok; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public String getToolName() { return toolName; }
    public void setToolName(String toolName) { this.toolName = toolName; }
    public Object getResult() { return result; }
    public void setResult(Object result) { this.result = result; }
    public List<ToolLogEntry> getLogs() { return logs; }
    public void setLogs(List<ToolLogEntry> logs) { this.logs = logs; }
    public Map<String, Object> getMetrics() { return metrics; }
    public void setMetrics(Map<String, Object> metrics) { this.metrics = metrics; }
    public String getTraceId() { return traceId; }
    public void setTraceId(String traceId) { this.traceId = traceId; }
    public Timing getTiming() { return timing; }
    public void setTiming(Timing timing) { this.timing = timing; }
}
