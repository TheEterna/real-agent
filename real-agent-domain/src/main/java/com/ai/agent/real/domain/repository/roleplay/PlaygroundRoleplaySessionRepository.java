package com.ai.agent.real.domain.repository.roleplay;

import com.ai.agent.real.common.entity.roleplay.PlaygroundRoleplaySession;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * 角色扮演会话数据访问层 (R2DBC)
 */
@Repository
public interface PlaygroundRoleplaySessionRepository extends ReactiveCrudRepository<PlaygroundRoleplaySession, Long> {

	/**
	 * 根据会话编码查找会话
	 */
	Mono<PlaygroundRoleplaySession> findBySessionCode(String sessionCode);

	/**
	 * 检查会话编码是否存在
	 */
	@Query("SELECT COUNT(*) > 0 FROM playground.roleplay_sessions WHERE session_code = :sessionCode")
	Mono<Boolean> existsBySessionCode(String sessionCode);

	/**
	 * 查找用户的会话列表
	 */
	@Query("SELECT * FROM playground.roleplay_sessions WHERE user_id = :userId ORDER BY created_at DESC")
	Flux<PlaygroundRoleplaySession> findByUserIdOrderByCreatedAtDesc(Long userId);

	/**
	 * 查找用户的会话列表
	 */
	@Query("SELECT * FROM playground.roleplay_sessions WHERE user_id = :userId ORDER BY created_at ASC")
	Flux<PlaygroundRoleplaySession> findByUserIdOrderByCreatedAtAsc(Long userId);

	/**
	 * 查找用户进行中的会话
	 */
	@Query("SELECT * FROM playground.roleplay_sessions WHERE user_id = :userId AND status = 1 ORDER BY created_at DESC")
	Flux<PlaygroundRoleplaySession> findActiveSessionsByUserId(Long userId);

	/**
	 * 查找角色的会话列表
	 */
	@Query("SELECT * FROM playground.roleplay_sessions WHERE role_id = :roleId ORDER BY created_at DESC")
	Flux<PlaygroundRoleplaySession> findByRoleIdOrderByCreatedAtDesc(Long roleId);

}
