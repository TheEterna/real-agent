package com.ai.agent.real.contract.user;

import com.ai.agent.real.domain.entity.user.Session;
import lombok.Data;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * 会话数据传输对象
 */
@Data
public class SessionDTO {

	/**
	 * 主键
	 */
	private UUID id;

	/**
	 * 会话的标题
	 */
	private String title;

	/**
	 * 会话的类型
	 */
	private String type;

	/**
	 * 所属用户ID
	 */
	private UUID userId;

	/**
	 * 会话创建时间
	 */
	private OffsetDateTime startTime;

	/**
	 * 从Session实体转换为SessionDTO
	 */
	public static SessionDTO fromEntity(Session session) {
		SessionDTO dto = new SessionDTO();
		dto.setId(session.getId());
		dto.setTitle(session.getTitle());
		dto.setType(session.getType());
		dto.setUserId(session.getUserId());
		dto.setStartTime(session.getStartTime());
		return dto;
	}

	/**
	 * 从SessionDTO转换为Session实体
	 */
	public com.ai.agent.real.domain.entity.user.Session toEntity() {
		return com.ai.agent.real.domain.entity.user.Session.builder()
			.id(this.id)
			.title(this.title)
			.type(this.type)
			.userId(this.userId)
			.startTime(this.startTime)
			.build();
	}

}