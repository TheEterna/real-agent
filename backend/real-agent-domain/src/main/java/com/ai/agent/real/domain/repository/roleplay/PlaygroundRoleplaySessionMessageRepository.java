package com.ai.agent.real.domain.repository.roleplay;

import com.ai.agent.real.domain.entity.roleplay.PlaygroundRoleplaySessionMessage;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * 角色扮演会话消息数据访问层 (R2DBC)
 */
@Repository
public interface PlaygroundRoleplaySessionMessageRepository
		extends ReactiveCrudRepository<PlaygroundRoleplaySessionMessage, Long> {

	/**
	 * 根据会话ID查找消息列表（按时间顺序）
	 */
	@Query("SELECT * FROM playground.roleplay_session_messages WHERE session_id = :sessionId ORDER BY created_at")
	Flux<PlaygroundRoleplaySessionMessage> findBySessionIdOrderBySeq(Long sessionId);

	/**
	 * 根据会话ID和消息类型查找消息（按时间顺序）
	 */
	@Query("SELECT * FROM playground.roleplay_session_messages WHERE session_id = :sessionId AND message_type = :messageType ORDER BY created_at")
	Flux<PlaygroundRoleplaySessionMessage> findBySessionIdAndMessageTypeOrderBySeq(Long sessionId, String messageType);

	/**
	 * 统计会话消息数量
	 */
	@Query("SELECT COUNT(*) FROM playground.roleplay_session_messages WHERE session_id = :sessionId")
	Mono<Long> countBySessionId(Long sessionId);

	/**
	 * 查找会话的分页消息（按时间倒序）
	 */
	@Query("SELECT * FROM playground.roleplay_session_messages WHERE session_id = :sessionId ORDER BY created_at ASC LIMIT :limit OFFSET :offset")
	Flux<PlaygroundRoleplaySessionMessage> findBySessionIdOrderByCreatedAtAscWithPaging(Long sessionId, int limit,
			int offset);

}
