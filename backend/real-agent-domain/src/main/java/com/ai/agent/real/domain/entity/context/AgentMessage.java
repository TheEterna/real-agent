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
 * 智能体消息实体 对应表 context.messages
 *
 * @author: han
 * @time: 2025/11/21 22:12
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "messages", schema = "context")
public class AgentMessage implements Persistable<UUID> {

	@Id
	private UUID id;

	@Column("turn_id")
	private UUID turnId;

	@Column("session_id")
	private UUID sessionId;

	@Column("type")
	private String type;

	@Column("message")
	private String message;

	@Column("data")
	private Json data;

	@Column("meta")
	private Json meta;

	@Column("start_time")
	private OffsetDateTime startTime;

	@Column("end_time")
	private OffsetDateTime endTime;

	@Column("is_deleted")
	@Builder.Default
	private Boolean isDeleted = false;

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
