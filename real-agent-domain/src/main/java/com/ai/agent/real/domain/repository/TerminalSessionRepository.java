package com.ai.agent.real.domain.repository;

import com.ai.agent.real.domain.entity.TerminalSession;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

/**
 * 终端会话Repository
 *
 * @author Real Agent Team
 * @since 2025-01-23
 */
@Repository
public interface TerminalSessionRepository extends R2dbcRepository<TerminalSession, String> {

	/**
	 * 根据用户ID查找所有会话
	 * @param userId 用户ID
	 * @return 会话列表
	 */
	Flux<TerminalSession> findByUserIdOrderByLastActivityDesc(String userId);

	/**
	 * 根据用户ID和状态查找会话
	 * @param userId 用户ID
	 * @param status 会话状态
	 * @return 会话列表
	 */
	Flux<TerminalSession> findByUserIdAndStatus(String userId, String status);

	/**
	 * 查找用户的活跃会话
	 * @param userId 用户ID
	 * @return 活跃会话列表
	 */
	@Query("SELECT * FROM terminal_sessions WHERE user_id = :userId AND status = 'active' ORDER BY last_activity DESC")
	Flux<TerminalSession> findActiveSessionsByUserId(String userId);

	/**
	 * 查找超时的会话
	 * @param timeoutThreshold 超时阈值时间
	 * @return 超时会话列表
	 */
	@Query("SELECT * FROM terminal_sessions WHERE status = 'active' AND last_activity < :timeoutThreshold")
	Flux<TerminalSession> findTimeoutSessions(LocalDateTime timeoutThreshold);

	/**
	 * 更新会话的最后活动时间
	 * @param sessionId 会话ID
	 * @param lastActivity 最后活动时间
	 * @return 更新结果
	 */
	@Query("UPDATE terminal_sessions SET last_activity = :lastActivity WHERE session_id = :sessionId")
	Mono<Void> updateLastActivity(String sessionId, LocalDateTime lastActivity);

	/**
	 * 更新会话状态
	 * @param sessionId 会话ID
	 * @param status 新状态
	 * @return 更新结果
	 */
	@Query("UPDATE terminal_sessions SET status = :status WHERE session_id = :sessionId")
	Mono<Void> updateStatus(String sessionId, String status);

}
