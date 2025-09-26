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
@Table("app_message")
public class AppMessage {
    @Id
    private Long id;

    private String appSessionId; // FK to app_session.id

    private String direction; // USER / AI

    private String type; // TEXT

    private String content; // text content

    private LocalDateTime createdAt;
}
