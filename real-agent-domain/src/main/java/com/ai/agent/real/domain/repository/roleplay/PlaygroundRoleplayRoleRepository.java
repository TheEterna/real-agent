package com.ai.agent.real.domain.repository.roleplay;

import com.ai.agent.real.domain.entity.roleplay.PlaygroundRoleplayRole;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

/**
 * 角色扮演角色数据访问层 (R2DBC)
 */
@Repository
public interface PlaygroundRoleplayRoleRepository extends ReactiveCrudRepository<PlaygroundRoleplayRole, Long> {

	/**
	 * 查找启用状态的角色
	 */
	@Query("SELECT * FROM `playground_roleplay_roles` WHERE status = 1 ORDER BY created_at DESC")
	Flux<PlaygroundRoleplayRole> findActiveRoles();

	/**
	 * 根据状态查找角色
	 */
	@Query("SELECT * FROM playground_roleplay_roles WHERE status = :status ORDER BY created_at DESC")
	Flux<PlaygroundRoleplayRole> findByStatusOrderByCreatedAtDesc(Integer status);

}
