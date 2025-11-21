package com.ai.agent.real.domain.entity.user;

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
 * 会话实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "sessions", schema = "context")
public class Session implements Persistable<UUID> {

	@Id
	private UUID id;

	@Column("title")
	private String title;

	@Column("type")
	private String type;

	@Column("user_id")
	private UUID userId;

	@Column("created_time")
	private OffsetDateTime createdTime;


	@Column("updated_time")
	private OffsetDateTime updatedTime;

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
		return isNew; // 如果 ID 由数据库生成，保存前 id 为 null 也会被视为新记录，这里保留以支持手动设置 ID 的情况
	}

}