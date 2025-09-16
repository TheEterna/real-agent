package com.ai.agent.real.web.dto;

import lombok.Builder;
import lombok.Data;

import java.util.Set;

/**
 * Agent状态响应DTO
 * 
 * @author han
 * @time 2025/9/7 00:37
 */
@Data
@Builder
public class AgentStatusResponse {
    
    /**
     * 注册的Agent数量
     */
    private int agentCount;
    
    /**
     * 可用的策略数量
     */
    private int strategyCount;
    
    /**
     * 默认策略名称
     */
    private String defaultStrategy;
    
    /**
     * 已注册的Agent ID列表
     */
    private Set<String> agents;
    
    /**
     * 可用的策略名称列表
     */
    private Set<String> strategies;
}
