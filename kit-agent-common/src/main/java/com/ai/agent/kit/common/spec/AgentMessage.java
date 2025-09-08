package com.ai.agent.kit.common.spec;

import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Agent间通信消息
 * 
 * @author han
 * @time 2025/9/6 23:50
 */
@Data
@Accessors(chain = true)
public class AgentMessage {

    /**
     * 消息ID
     */
    private String messageId;

    /**
     * 发送者Agent ID
     */
    private String senderId;

    /**
     * 接收者Agent ID
     */
    private String receiverId;

    /**
     * 消息类型
     */
    private MessageType messageType;

    /**
     * 消息内容
     */
    private String content;

    /**
     * 消息元数据
     */
    private Map<String, Object> metadata;

    /**
     * 发送时间
     */
    private LocalDateTime timestamp;

    /**
     * 消息优先级
     */
    private Priority priority = Priority.NORMAL;

    /**
     * 是否需要回复
     */
    private boolean requiresResponse = false;

    /**
     * 关联的任务ID
     */
    private String taskId;

    /**
     * 消息类型枚举
     */
    public enum MessageType {
        REQUEST,        // 请求
        RESPONSE,       // 响应
        NOTIFICATION,   // 通知
        COLLABORATION,  // 协作
        ERROR,          // 错误
        HEARTBEAT       // 心跳
    }

    /**
     * 优先级枚举
     */
    public enum Priority {
        LOW(1),
        NORMAL(2),
        HIGH(3),
        URGENT(4);

        private final int level;

        Priority(int level) {
            this.level = level;
        }

        public int getLevel() {
            return level;
        }
    }

    /**
     * 创建请求消息
     */
    public static AgentMessage createRequest(String senderId, String receiverId, String content, String taskId) {
        return new AgentMessage()
                .setMessageId(generateMessageId())
                .setSenderId(senderId)
                .setReceiverId(receiverId)
                .setMessageType(MessageType.REQUEST)
                .setContent(content)
                .setTaskId(taskId)
                .setTimestamp(LocalDateTime.now())
                .setRequiresResponse(true);
    }

    /**
     * 创建响应消息
     */
    public static AgentMessage createResponse(String senderId, String receiverId, String content, String taskId) {
        return new AgentMessage()
                .setMessageId(generateMessageId())
                .setSenderId(senderId)
                .setReceiverId(receiverId)
                .setMessageType(MessageType.RESPONSE)
                .setContent(content)
                .setTaskId(taskId)
                .setTimestamp(LocalDateTime.now())
                .setRequiresResponse(false);
    }

    /**
     * 创建通知消息
     */
    public static AgentMessage createNotification(String senderId, String receiverId, String content) {
        return new AgentMessage()
                .setMessageId(generateMessageId())
                .setSenderId(senderId)
                .setReceiverId(receiverId)
                .setMessageType(MessageType.NOTIFICATION)
                .setContent(content)
                .setTimestamp(LocalDateTime.now())
                .setRequiresResponse(false);
    }

    /**
     * 生成消息ID
     */
    private static String generateMessageId() {
        return "msg_" + System.currentTimeMillis() + "_" + (int)(Math.random() * 1000);
    }
}
