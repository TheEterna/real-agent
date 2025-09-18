package com.ai.agent.real.contract.protocol;

import java.io.Serializable;

public class Timing implements Serializable {
    private static final long serialVersionUID = 1L;
    private String startTime;
    private String endTime;
    private Long durationMs;

    public Timing() {}

    public Timing(String startTime, String endTime, Long durationMs) {
        this.startTime = startTime;
        this.endTime = endTime;
        this.durationMs = durationMs;
    }

    public String getStartTime() { return startTime; }
    public void setStartTime(String startTime) { this.startTime = startTime; }
    public String getEndTime() { return endTime; }
    public void setEndTime(String endTime) { this.endTime = endTime; }
    public Long getDurationMs() { return durationMs; }
    public void setDurationMs(Long durationMs) { this.durationMs = durationMs; }
}
