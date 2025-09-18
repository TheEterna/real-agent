package com.ai.agent.real.contract.protocol;

import java.io.Serializable;

public class ToolLogEntry implements Serializable {
    private static final long serialVersionUID = 1L;
    private String level; // INFO/WARN/ERROR
    private String message;
    private String ts; // ISO-8601 string

    public ToolLogEntry() {}

    public ToolLogEntry(String level, String message, String ts) {
        this.level = level;
        this.message = message;
        this.ts = ts;
    }

    public static ToolLogEntry info(String msg, String ts){ return new ToolLogEntry("INFO", msg, ts); }
    public static ToolLogEntry warn(String msg, String ts){ return new ToolLogEntry("WARN", msg, ts); }
    public static ToolLogEntry error(String msg, String ts){ return new ToolLogEntry("ERROR", msg, ts); }

    public String getLevel() { return level; }
    public void setLevel(String level) { this.level = level; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public String getTs() { return ts; }
    public void setTs(String ts) { this.ts = ts; }
}
