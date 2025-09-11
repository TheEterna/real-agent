package com.ai.agent.contract.spec;

import com.ai.agent.contract.spec.logging.*;
import com.ai.agent.contract.spec.message.*;
import lombok.*;
import lombok.experimental.*;

import java.time.*;
import java.util.*;
import java.util.stream.*;


/**
 * AgentContext 类定义了工具执行的上下文信息。
 * @author han
 * @time 2025/8/30 17:00
 */
@Data
@Accessors(chain = true)
public class AgentContext implements Traceable {

    /**
     * 组合的追踪信息对象（推荐使用）
     */
    private TraceInfo trace;

    /**
     * 工具执行的参数
     */
    private Map<String, Object> args;

    /**
     * 对话历史记录 - 使用结构化消息列表
     */
    private List<AgentMessage> conversationHistory;
    
    /**
     * 当前迭代轮次
     */
    private int currentIteration = 0;
    
    /**
     * 任务完成状态
     */
    private boolean taskCompleted = false;

    /**
     * 构造函数
     */
    public AgentContext() {
        this.args = new HashMap<>();
        this.trace = new TraceInfo();
        this.conversationHistory = new ArrayList<>();
    }



    public List<AgentMessage> getConversationHistory() {
        return conversationHistory;
    }

    
    /**
     * 添加消息到对话历史
     */
    public AgentContext addMessage(AgentMessage message) {

        this.conversationHistory.add(message);
        return this;
    }
    
    /**
     * 添加多条消息到对话历史
     */
    public AgentContext addMessages(List<AgentMessage> messages) {
        this.conversationHistory.addAll(messages);
        return this;
    }
    
//    /**
//     * 获取对话历史的字符串表示（兼容原有代码）
//     */
//    public String getConversationHistoryAsString() {
//        StringBuilder sb = new StringBuilder();
//        for (AgentMessage message : conversationHistory) {
//            sb.append(message.toFormattedString()).append("\n");
//        }
//        return sb.toString();
//    }
    
    /**
     * 获取指定类型的消息
     */
    public List<AgentMessage> getMessagesByType(AgentMessage.AgentMessageType messageType) {
        return conversationHistory.stream()
                .filter(msg -> msg.getAgentMessageType() == messageType)
                .collect(Collectors.toList());
    }
    

    
    /**
     * 获取最近N条消息
     */
    public List<AgentMessage> getRecentMessages(int count) {
        int size = conversationHistory.size();
        int fromIndex = Math.max(0, size - count);
        return new ArrayList<>(conversationHistory.subList(fromIndex, size));
    }
    
    /**
     * 清空对话历史
     */
    public AgentContext clearConversationHistory() {
        this.conversationHistory.clear();
        return this;
    }
    
    public int getCurrentIteration() {
        return currentIteration;
    }
    
    public AgentContext setCurrentIteration(int currentIteration) {
        this.currentIteration = currentIteration;
        return this;
    }
    
    /**
     * 获取任务完成状态
     */
    public boolean isTaskCompleted() {
        return taskCompleted;
    }
    
    /**
     * 设置任务完成状态
     */
    public AgentContext setTaskCompleted(boolean taskCompleted) {
        this.taskCompleted = taskCompleted;
        return this;
    }

    public TraceInfo getTrace() {
        return trace;
    }

    public AgentContext setTrace(TraceInfo trace) {
        this.trace = trace;
        return this;
    }



    @Override
    public String getSessionId() {
        return trace.getSessionId();
    }

    @Override
    public AgentContext setSessionId(String sessionId) {
        this.trace.setSessionId(sessionId);
        return this;
    }

    @Override
    public String getTraceId() {
        return trace.getTraceId();
    }

    @Override
    public AgentContext setTraceId(String traceId) {
        this.trace.setTraceId(traceId);
        return this;
    }

    @Override
    public LocalDateTime getStartTime() {
        return trace.getStartTime();
    }

    @Override
    public AgentContext setStartTime(LocalDateTime startTime) {
        this.trace.setStartTime(startTime);
        return this;
    }

    @Override
    public LocalDateTime getEndTime() {
        return trace.getEndTime();
    }


    @Override
    public AgentContext setEndTime(LocalDateTime endTime) {
        this.trace.setEndTime(endTime);
        return this;
    }
    public boolean isEnd() {
        return trace.getEndTime() != null;
    }
    @Override
    public String getSpanId() {
        return trace.getSpanId();
    }

    @Override
    public AgentContext setSpanId(String spanId) {
        this.trace.setSpanId(spanId);
        return this;
    }

    @Override
    public String getNodeId() {
        return trace.getNodeId();
    }

    @Override
    public Traceable setNodeId(String nodeId) {
        this.trace.setNodeId(nodeId);
        return this;
    }

    @Override
    public String getAgentId() {
        return trace.getAgentId();
    }

    @Override
    public Traceable setAgentId(String agentId) {
        this.trace.setAgentId(agentId);
        return this;
    }


}
