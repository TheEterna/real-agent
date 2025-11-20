package com.ai.agent.real.contract.user;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * 会话服务接口
 */
public interface ISessionService {

	/**
	 * 创建新会话
	 * @param title 会话标题
	 * @param type 会话类型
	 * @param userId 用户ID
	 * @param firstUserMessage
	 * @return 创建的会话
	 */
	Mono<SessionDTO> createSessionWithAiTitle(String title, String type, UUID userId, String firstUserMessage);

	/**
	 * 根据ID获取会话
	 */
	Mono<SessionDTO> getSessionById(UUID sessionId);

	/**
	 * 根据用户ID获取会话列表
	 */
	Flux<SessionDTO> getSessionsByUserId(UUID userId);

	/**
	 * 根据用户ID和类型获取会话列表
	 */
	Flux<SessionDTO> getSessionsByUserIdAndType(UUID userId, String type);

	/**
	 * 更新会话
	 */
	Mono<SessionDTO> updateSession(SessionDTO sessionDTO);

	/**
	 * 删除会话
	 */
	Mono<Void> deleteSession(UUID id);

	/**
	 * 检查会话是否属于指定用户
	 */
	Mono<Boolean> isSessionBelongsToUser(UUID sessionId, UUID userId);

}