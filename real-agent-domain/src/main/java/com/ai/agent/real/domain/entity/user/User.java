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

import java.time.LocalDateTime;

/**
 * 用户实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("users")
public class User implements Persistable<Long> {

	public static class UserBuilder {

		private boolean isNew = true;

	}

	@Id
	private Long id;

	@Column("external_id")
	private String externalId;

	@Column("nickname")
	private String nickname;

	@Column("avatar_url")
	private String avatarUrl;

	@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
	@Column("created_at")
	private LocalDateTime createdAt;

	@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
	@Column("updated_at")
	private LocalDateTime updatedAt;

	@Transient
	private boolean isNew = true;

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
