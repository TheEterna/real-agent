package com.ai.agent.real.domain.repository.user;

import com.ai.agent.real.domain.entity.user.Session;
import org.springframework.data.r2dbc.repository.Modifying;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * 会话数据访问层 (R2DBC)
 */
@Repository
public interface SessionRepository extends R2dbcRepository<Session, UUID> {

	/**
	 * 根据用户ID查找会话
	 */
	@Query("SELECT * FROM context.sessions WHERE user_id = :userId ORDER BY updated_time DESC")
	Flux<Session> findByUserIdOrderByUpdatedTimeDesc(UUID userId);

	/**
	 * 根据用户ID和类型查找会话
	 */
	@Query("SELECT * FROM context.sessions WHERE user_id = :userId AND type = :type ORDER BY updated_time DESC")
	Flux<Session> findByUserIdAndType(UUID userId, String type);

	/**
	 * 检查会话是否存在
	 */
	@Query("SELECT COUNT(*) > 0 FROM context.sessions WHERE id = :id AND user_id = :userId")
	Mono<Boolean> existsByIdAndUserId(UUID id, UUID userId);

	@Modifying
	@Query("UPDATE context.sessions SET updated_time = :updatedTime WHERE id = :id")
	Mono<Integer> updateUpdatedTime(UUID id, OffsetDateTime updatedTime);

}