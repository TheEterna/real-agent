package com.ai.agent.real.domain.repository;

import com.ai.agent.real.domain.entity.TerminalHistory;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * 终端命令历史Repository
 *
 * @author Real Agent Team
 * @since 2025-01-23
 */
@Repository
public interface TerminalHistoryRepository extends R2dbcRepository<TerminalHistory, String> {

	/**
	 * 根据会话ID查找历史
	 * @param sessionId 会话ID
	 * @return 历史记录列表
	 */
	Flux<TerminalHistory> findBySessionIdOrderByTimestampDesc(String sessionId);

	/**
	 * 根据用户ID查找历史
	 * @param userId 用户ID
	 * @return 历史记录列表
	 */
	Flux<TerminalHistory> findByUserIdOrderByTimestampDesc(String userId);

	/**
	 * 根据命令名称查找历史
	 * @param commandName 命令名称
	 * @return 历史记录列表
	 */
	Flux<TerminalHistory> findByCommandNameOrderByTimestampDesc(String commandName);

	/**
	 * 根据会话ID获取最近N条记录
	 * @param sessionId 会话ID
	 * @param limit 数量限制
	 * @return 历史记录列表
	 */
	@Query("SELECT * FROM terminal_history WHERE session_id = :sessionId ORDER BY timestamp DESC LIMIT :limit")
	Flux<TerminalHistory> findRecentBySessionId(String sessionId, int limit);

	/**
	 * 根据用户ID获取最近N条记录
	 * @param userId 用户ID
	 * @param limit 数量限制
	 * @return 历史记录列表
	 */
	@Query("SELECT * FROM terminal_history WHERE user_id = :userId ORDER BY timestamp DESC LIMIT :limit")
	Flux<TerminalHistory> findRecentByUserId(String userId, int limit);

	/**
	 * 删除指定会话的所有历史
	 * @param sessionId 会话ID
	 * @return 删除数量
	 */
	Mono<Long> deleteBySessionId(String sessionId);

	/**
	 * 统计会话的命令执行次数
	 * @param sessionId 会话ID
	 * @return 执行次数
	 */
	@Query("SELECT COUNT(*) FROM terminal_history WHERE session_id = :sessionId")
	Mono<Long> countBySessionId(String sessionId);

}
