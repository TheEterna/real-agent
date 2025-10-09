package com.ai.agent.real.domain.entity.roleplay;

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

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 角色扮演会话实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("playground_roleplay_sessions")
public class PlaygroundRoleplaySession implements Persistable<Long> {

	public static class PlaygroundRoleplaySessionBuilder {

		private boolean isNew = true;

	}

	@Id
	private Long id;

	@Column("session_code")
	private String sessionCode;

	@Column("user_id")
	private Long userId;

	@Column("role_id")
	private Long roleId;

	@Column("status")
	private Integer status;

	@Column("summary")
	private String summary;

	@Column("metadata")
	private String metadataStr;

	@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
	@Column("created_at")
	private LocalDateTime createdAt;

	@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
	@Column("ended_at")
	private LocalDateTime endedAt;

	@Transient
	private boolean isNew = true;

	@Transient
	private Map<String, Object> metadata;

	@Override
	public Long getId() {
		return id;
	}

	@Override
	@Transient
	public boolean isNew() {
		return isNew;
	}

}
