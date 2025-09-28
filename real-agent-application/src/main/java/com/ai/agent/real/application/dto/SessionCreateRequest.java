package com.ai.agent.real.application.dto;

import com.ai.agent.real.domain.entity.roleplay.*;
import lombok.Data;

import javax.validation.constraints.NotNull;

/**
 * 会话创建请求DTO
 */
@Data
public class SessionCreateRequest {
    
    @NotNull(message = "用户ID不能为空")
    private Long userId;
    
    @NotNull(message = "角色ID不能为空")
    private Long roleId;
    
}
