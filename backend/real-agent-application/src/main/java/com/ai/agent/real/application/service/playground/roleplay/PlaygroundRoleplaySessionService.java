package com.ai.agent.real.application.service.playground.roleplay;

import com.ai.agent.real.common.utils.*;
import com.ai.agent.real.contract.dto.SessionCreateRequestDto;
import com.ai.agent.real.domain.entity.roleplay.PlaygroundRoleplaySession;
import reactor.core.publisher.*;

/**
 * @author han
 * @time 2025/9/28 12:02
 */

public interface PlaygroundRoleplaySessionService {

	/**
	 * 创建会话
	 */
	Mono<PlaygroundRoleplaySession> createSession(SessionCreateRequestDto request);

	/**
	 * 根据会话编码查找会话
	 */
	Mono<PlaygroundRoleplaySession> findBySessionCode(String sessionCode);

	/**
	 * 查找用户的会话列表
	 */
	Flux<PlaygroundRoleplaySession> findUserSessions(Long userId);

	/**
	 * 查找用户进行中的会话
	 */
	Flux<PlaygroundRoleplaySession> findActiveUserSessions(Long userId);

	/**
	 * 结束会话
	 */
	Mono<PlaygroundRoleplaySession> endSession(String sessionCode, String summary);

	/**
	 * 更新会话元数据
	 */
	Mono<PlaygroundRoleplaySession> updateSessionMetadata(String sessionCode, java.util.Map<String, Object> metadata);

	/**
	 * 转换JSON字段为Map对象
	 */
	default PlaygroundRoleplaySession convertJsonFields(PlaygroundRoleplaySession session) {
		session.setMetadata(JsonUtils.jsonStringToMap(session.getMetadataStr()));

		return session;
	}

}
