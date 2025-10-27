package com.ai.agent.real.application.service;

import com.ai.agent.real.common.entity.roleplay.PlaygroundRoleplayRole;
import com.ai.agent.real.contract.dto.RoleCreateRequestDto;
import reactor.core.publisher.*;

/**
 * @author han
 * @time 2025/9/28 12:01
 */

public interface PlaygroundRoleplayRoleService {

	/**
	 * 创建角色
	 */
	Mono<PlaygroundRoleplayRole> createRole(RoleCreateRequestDto request);

	/**
	 * 更新角色
	 */
	Mono<PlaygroundRoleplayRole> updateRole(Long id, RoleCreateRequestDto request);

	/**
	 * 删除角色
	 */
	Mono<Void> deleteRole(Long id);

	/**
	 * 查找所有角色
	 */
	Flux<PlaygroundRoleplayRole> findAllRoles();

	/**
	 * 查找启用的角色
	 */
	Flux<PlaygroundRoleplayRole> findActiveRoles();

	/**
	 * 启用/禁用角色
	 */
	Mono<PlaygroundRoleplayRole> toggleRoleStatus(Long id);

	/**
	 * 根据ID查找角色
	 */
	Mono<PlaygroundRoleplayRole> findById(Long id);

}
