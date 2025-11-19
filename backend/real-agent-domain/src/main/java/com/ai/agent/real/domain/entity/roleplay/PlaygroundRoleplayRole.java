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
 * 角色扮演角色实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "roleplay_roles", schema = "playground")
public class PlaygroundRoleplayRole implements Persistable<Long> {

	@Id
	private Long id;

	@Column("voice")
	private VoiceEnum voice;

	@Column("name")
	private String name;

	@Column("avatar_url")
	private String avatarUrl;

	@Column("description")
	private String description;

	@Column("status")
	private Integer status;

	@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
	@Column("created_at")
	private LocalDateTime createdAt;

	@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
	@Column("updated_at")
	private LocalDateTime updatedAt;

	@Transient
	private boolean isNew = true;

	// 便于业务层使用的JSON转换字段
	@Transient
	private Map<String, Object> traitsJson;

	@Transient
	private Map<String, Object> scriptsJson;

	@Override
	public Long getId() {
		return id;
	}

	@Override
	@Transient
	public boolean isNew() {
		return isNew;
	}

	/**
	 * 角色音色枚举
	 */
	public enum VoiceEnum {

		CHERRY("Cherry"), ETHAN("Ethan"), NOFISH("Nofish"), JENNIFER("Jennifer"), RYAN("Ryan"), KATERINA("Katerina"),
		ELIAS("Elias"), JADA("Jada"), DYLAN("Dylan"), SUNNY("Sunny"), LI("li"), MARCUS("Marcus"), ROY("Roy"),
		PETER("Peter"), ROCKY("Rocky"), KIKI("Kiki"), ERIC("Eric");

		private final String value;

		VoiceEnum(String value) {
			this.value = value;
		}

		public String getValue() {
			return value;
		}

	}

}
