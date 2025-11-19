package com.ai.agent.real.contract.dto;

import com.ai.agent.real.domain.entity.roleplay.PlaygroundRoleplaySessionMessage;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.util.Map;

/**
 * 消息创建请求DTO
 */
@Data
public class MessageCreateRequestDto {

	@NotNull(message = "消息类型不能为空")
	private PlaygroundRoleplaySessionMessage.MessageType messageType;

	@NotNull(message = "角色不能为空")
	private PlaygroundRoleplaySessionMessage.MessageRole role;

	private String content;

	private Map<String, Object> payload;

	private String assetUri;

}
