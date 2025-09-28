package com.ai.agent.real.domain.entity.roleplay;

import com.fasterxml.jackson.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Builder.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 角色扮演会话消息实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("playground_roleplay_session_messages")
public class PlaygroundRoleplaySessionMessage implements Persistable<Long> {
    
    public static class PlaygroundRoleplaySessionMessageBuilder {
        private boolean isNew = true;
    }
    
    @Id
    private Long id;
    
    @Column("session_id")
    private Long sessionId;

    @Column("message_type")
    private MessageType messageType;

    @Column("role")
    private MessageRole role;
    
    @Column("content")
    private String content;
    
    @Column("payload")
    private String payloadStr;
    
    @Column("asset_uri")
    private String assetUri;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Column("created_at")
    private LocalDateTime createdAt;
    
    @Transient
    @Default
    private boolean isNew = true;
    

    @Transient
    private Map<String, Object> payload;
    
    @Override
    public Long getId() {
        return id;
    }
    
    @Override
    @Transient
    public boolean isNew() {
        return isNew;
    }
    
    public enum MessageType {
        USER_TEXT, ASSISTANT_TEXT, ASSISTANT_AUDIO, EVENT, TOOL_CALL, TOOL_RESULT
    }
    
    public enum MessageRole {
        USER, ASSISTANT, SYSTEM, TOOL
    }
}
