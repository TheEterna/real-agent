package com.ai.agent.real.web.persistence.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("voice_session")
public class VoiceSession {
    @Id
    private String id; // voiceSessionId (UUID)

    private String appSessionId; // FK to app_session.id

    private String serverSessionId; // Omni server session id (sess_*)

    private LocalDateTime createdAt;
}
