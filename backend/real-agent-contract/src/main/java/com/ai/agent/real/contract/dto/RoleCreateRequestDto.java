package com.ai.agent.real.contract.dto;

import com.ai.agent.real.domain.entity.roleplay.PlaygroundRoleplayRole;
import lombok.Data;

import javax.validation.constraints.Size;
import java.util.Map;

/**
 * 角色创建请求DTO
 */
@Data
public class RoleCreateRequestDto {

	private PlaygroundRoleplayRole.VoiceEnum voice;

	@Size(max = 100, message = "角色名称长度不能超过100个字符")
	private String name;

	private String avatarUrl;

	private String description;

	private Map<String, Object> traitsJson;

	private Map<String, Object> scriptsJson;

	private Integer status = 1;

}
