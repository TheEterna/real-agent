package com.ai.agent.real.domain.entity.user;

import com.fasterxml.jackson.annotation.JsonFormat;
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
 * 用户实体
 */
@Data
@Builder
@AllArgsConstructor
@Table(name = "users", schema = "app_user")
public class User implements Persistable<UUID> {

	@Id
	private UUID id;

	@Column("external_id")
	private String externalId;

	@Column("password_hash")
	private String passwordHash;

	@Column("nickname")
	private String nickname;

	@Column("avatar_url")
	private String avatarUrl;

	@Column("status")
	private Integer status;

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
		return isNew;
	}

	public User() {
	}

}
