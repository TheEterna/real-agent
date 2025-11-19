package com.ai.agent.real.contract.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;

/**
 * 会话创建请求DTO
 */
@Data
public class SessionCreateRequestDto {

	@NotNull(message = "用户ID不能为空")
	private Long userId;

	@NotNull(message = "角色ID不能为空")
	private Long roleId;

}
