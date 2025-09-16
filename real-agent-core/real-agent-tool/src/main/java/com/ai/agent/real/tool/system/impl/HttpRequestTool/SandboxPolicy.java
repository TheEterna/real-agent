package com.ai.agent.real.tool.system.impl.HttpRequestTool;

import lombok.*;

import java.util.Set;

@Data
public class SandboxPolicy {
    private Set<String> allowedHosts;
    private Set<String> allowedMethods;
    private long maxResponseBytes = 1_000_000; // 1MB

    public boolean isHostAllowed(String host){
        return allowedHosts==null || allowedHosts.contains(host);
    }
    public boolean isMethodAllowed(String method){
        return allowedMethods==null || allowedMethods.contains(method);
    }
}
