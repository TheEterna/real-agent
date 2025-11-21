package com.ai.agent.real.domain.entity.context;

import io.r2dbc.postgresql.codec.Json;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * 对话轮次实体
 * 对应表 context.turns
 * @author: han
 * @time: 2025/11/21 22:12
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "turns", schema = "context")
public class Turn implements Persistable<UUID> {

    @Id
    private UUID id;

    @Column("session_id")
    private UUID sessionId;

    @Column("parent_turn_id")
    private UUID parentTurnId;

    @Column("resume")
    private String resume;

    @Column("meta")
    private Json meta;

    @Column("start_time")
    private OffsetDateTime startTime;

    @Column("end_time")
    private OffsetDateTime endTime;

    @Transient
    @Builder.Default
    private boolean isNew = true;

    @Override
    public UUID getId() {
        return id;
    }

    @Override
    @Transient
    public boolean isNew() {
        return isNew;
    }
}
