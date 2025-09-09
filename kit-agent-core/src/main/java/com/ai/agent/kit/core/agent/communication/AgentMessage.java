package com.ai.agent.kit.core.agent.communication;

import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Agent系统的消息类，扩展Spring AI的Message接口
 * 支持Agent间的对话历史记录和上下文管理
 * 
 * @author han
 * @time 2025/9/9 12:05
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class AgentMessage implements Message {
    
    /**
     * 消息类型枚举，扩展Spring AI的MessageType
     */
    public enum AgentMessageType {
        // Spring AI标准类型
        SYSTEM("system"),
        USER("user"), 
        ASSISTANT("assistant"),
        TOOL("tool"),
        
        // Agent系统扩展类型
        THINKING("thinking"),    // 思考阶段消息
        ACTION("action"),       // 行动阶段消息
        OBSERVING("observing"), // 观察阶段消息
        REFLECTION("reflection"), // 反思消息
        COLLABORATION("collaboration"), // 协作消息
        ERROR("error"),      // 错误消息
        DONE("done");         // 结束消息

        private final String value;
        
        AgentMessageType(String value) {
            this.value = value;
        }
        
        public String getValue() {
            return value;
        }
        
        /**
         * 转换为Spring AI的MessageType
         */
        public MessageType toSpringAIMessageType() {
            switch (this) {
                case SYSTEM: return MessageType.SYSTEM;
                case USER: return MessageType.USER;
                case TOOL: return MessageType.TOOL;
                case ASSISTANT: return MessageType.ASSISTANT;
                default: return MessageType.ASSISTANT;
            }
        }
    }
    
    /**
     * 消息内容
     */
    private String content;
    
    /**
     * 消息类型
     */
    private AgentMessageType messageType;
    
    /**
     * 发送者Agent ID
     */
    private String senderId;
    
    /**
     * 接收者Agent ID（可选）
     */
    private String receiverId;
    
    /**
     * 消息时间戳
     */
    private LocalDateTime timestamp;
    
    /**
     * 迭代轮次（用于ReAct框架）
     */
    private Integer iteration;
    
    /**
     * 消息元数据
     */
    private Map<String, Object> metadata;
    
    /**
     * 构造函数 - 创建基本消息
     */
    public AgentMessage(String content, AgentMessageType messageType, String senderId) {
        this.content = content;
        this.messageType = messageType;
        this.senderId = senderId;
        this.timestamp = LocalDateTime.now();
        this.metadata = new HashMap<>();
    }
    
    /**
     * 构造函数 - 创建带迭代信息的消息（用于ReAct）
     */
    public AgentMessage(String content, AgentMessageType messageType, String senderId, Integer iteration) {
        this(content, messageType, senderId);
        this.iteration = iteration;
    }
    
    // ========== Spring AI Message接口实现 ==========
    
    @Override
    public String getText() {
        return content;
    }
    
    @Override
    public Map<String, Object> getMetadata() {
        if (metadata == null) {
            metadata = new HashMap<>();
        }
        // 添加Agent系统特有的元数据
        metadata.put("senderId", senderId);
        metadata.put("timestamp", timestamp);
        if (iteration != null) {
            metadata.put("iteration", iteration);
        }
        if (receiverId != null) {
            metadata.put("receiverId", receiverId);
        }
        return metadata;
    }
    
    @Override
    public MessageType getMessageType() {
        return messageType.toSpringAIMessageType();
    }
    
    /**
     * 获取Agent系统的消息类型
     */
    public AgentMessageType getAgentMessageType() {
        return messageType;
    }
    
    // ========== 便捷的静态工厂方法 ==========
    
    /**
     * 创建系统消息
     */
    public static AgentMessage system(String content) {
        return new AgentMessage(content, AgentMessageType.SYSTEM, "system");
    }
    
    /**
     * 创建用户消息
     */
    public static AgentMessage user(String content, String userId) {
        return new AgentMessage(content, AgentMessageType.USER, userId);
    }
    
    /**
     * 创建助手消息
     */
    public static AgentMessage assistant(String content, String agentId) {
        return new AgentMessage(content, AgentMessageType.ASSISTANT, agentId);
    }
    
    /**
     * 创建思考消息
     */
    public static AgentMessage thinking(String content, String agentId, Integer iteration) {
        return new AgentMessage(content, AgentMessageType.THINKING, agentId, iteration);
    }
    
    /**
     * 创建行动消息
     */
    public static AgentMessage action(String content, String agentId, Integer iteration) {
        return new AgentMessage(content, AgentMessageType.ACTION, agentId, iteration);
    }

    /**
     * 创建结束消息
     */
    public static AgentMessage done(String content, String agentId, Integer iteration) {
        return new AgentMessage(content, AgentMessageType.DONE, agentId, iteration);
    }

    /**
     * 创建观察消息
     */
    public static AgentMessage observing(String content, String agentId, Integer iteration) {
        return new AgentMessage(content, AgentMessageType.OBSERVING, agentId, iteration);
    }
    
    /**
     * 创建错误消息
     */
    public static AgentMessage error(String content, String agentId) {
        return new AgentMessage(content, AgentMessageType.ERROR, agentId);
    }
    
    /**
     * 创建工具消息
     */
    public static AgentMessage tool(String content, String toolId) {
        return new AgentMessage(content, AgentMessageType.TOOL, toolId);
    }
    
    // ========== 辅助方法 ==========
    
    /**
     * 检查是否为ReAct相关消息
     */
    public boolean isReActMessage() {
        return messageType == AgentMessageType.THINKING || 
               messageType == AgentMessageType.ACTION ||
               messageType == AgentMessageType.OBSERVING;
    }
    
    /**
     * 获取消息的简短描述
     */
    public String getShortDescription() {
        String shortContent = content.length() > 50 ? content.substring(0, 50) + "..." : content;
        return String.format("[%s@%s] %s", messageType.getValue(), senderId, shortContent);
    }
    
    /**
     * 转换为字符串格式（兼容原有的StringBuilder格式）
     */
    public String toFormattedString() {
        StringBuilder sb = new StringBuilder();
        
        if (iteration != null) {
            sb.append(getMessageTypeDisplayName()).append(iteration).append(": ");
        } else {
            sb.append("[").append(messageType.getValue()).append("] ");
        }
        
        sb.append(content);
        
        return sb.toString();
    }
    
    /**
     * 获取消息类型的显示名称
     */
    private String getMessageTypeDisplayName() {
        switch (messageType) {
            case THINKING: return "思考";
            case ACTION: return "行动";
            case OBSERVING: return "观察";
            case SYSTEM: return "系统";
            case USER: return "用户";
            case ASSISTANT: return "助手";
            case TOOL: return "工具";
            case ERROR: return "错误";
            default: return messageType.getValue();
        }
    }
    
    @Override
    public String toString() {
        return toFormattedString();
    }
}
