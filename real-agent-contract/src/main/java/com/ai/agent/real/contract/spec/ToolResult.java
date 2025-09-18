package com.ai.agent.real.contract.spec;

import java.util.HashMap;
import java.util.Map;


/**
 * @author han
 * @time 2025/8/30 17:05
 */
public class ToolResult<T> {
    private boolean ok;
    private String message;
    private String code;
    private long elapsedMs;
    private T data;
    private Map<String, Object> meta = new HashMap<>();
    private String toolId;

    public static <T> ToolResult<T> ok(T data, long elapsed, String toolId) {
        ToolResult<T> r = new ToolResult<>();
        r.ok = true;
        r.data = data;
        r.elapsedMs = elapsed;
        r.meta = new HashMap<>();
        r.toolId = toolId;
        return r;
    }

    public static <T> ToolResult<T> ok(T data, long elapsed, int toolId) {
        ToolResult<T> r = new ToolResult<>();
        r.ok = true;
        r.data = data;
        r.elapsedMs = elapsed;
        r.meta = new HashMap<>();
        r.toolId = String.valueOf(toolId);
        return r;
    }

    public static <T> ToolResult<T> error(String code, String message, String toolId) {
        ToolResult<T> r = new ToolResult<>();
        r.ok = false;
        r.code = code;
        r.message = message;
        r.meta = new HashMap<>();
        r.toolId = toolId;
        return r;
    }

    public boolean isOk() {
        return ok;
    }

    public String getMessage() {
        return message;
    }

    public String getCode() {
        return code;
    }

    public long getElapsedMs() {
        return elapsedMs;
    }

    public T getData() {
        return data;
    }

    public ToolResult<T> setElapsedMs(long v) {
        this.elapsedMs = v;
        return this;
    }

    public Map<String, Object> getMeta() {
        return meta;
    }

    public ToolResult<T> setMeta(Map<String, Object> meta) {
        this.meta = meta != null ? meta : new HashMap<>();
        return this;
    }

    public ToolResult<T> putMeta(String key, Object value) {
        if (this.meta == null) {
            this.meta = new HashMap<>();
        }
        this.meta.put(key, value);
        return this;
    }
}
