package com.ai.agent.real.web.dto;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

/**
 * Agent任务响应DTO
 * 
 * @author han
 * @time 2025/9/7 00:36
 */
@Data
@Builder
public class AgentTaskResponse {
    
    /**
     * 任务是否成功执行
     */
    private boolean success;
    
    /**
     * 任务执行结果
     */
    private String result;
    
    /**
     * 错误信息（如果失败）
     */
    private String errorMessage;
    
    /**
     * 执行任务的Agent ID
     */
    private String agentId;
    
    /**
     * 使用的策略名称
     */
    private String strategyUsed;
    
    /**
     * 置信度分数
     */
    private double confidenceScore;
    
    /**
     * 执行时间（毫秒）
     */
    private long executionTimeMs;
    
    /**
     * 任务ID
     */
    private String taskId;
    
    /**
     * 额外的元数据
     */
    private Map<String, Object> metadata;
}
