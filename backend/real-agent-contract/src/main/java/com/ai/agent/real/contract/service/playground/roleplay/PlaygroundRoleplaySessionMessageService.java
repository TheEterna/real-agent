package com.ai.agent.real.contract.service.playground.roleplay;

import com.ai.agent.real.common.utils.*;
import com.ai.agent.real.contract.dto.MessageCreateRequestDto;
import com.ai.agent.real.domain.entity.roleplay.PlaygroundRoleplaySessionMessage;
import reactor.core.publisher.*;

/**
 * @author han
 * @time 2025/9/28 12:01
 */

public interface PlaygroundRoleplaySessionMessageService {

	/**
	 * 添加消息
	 */
	Mono<PlaygroundRoleplaySessionMessage> addMessage(String sessionCode, MessageCreateRequestDto request);

	/**
	 * 查询会话消息历史
	 */
	Flux<PlaygroundRoleplaySessionMessage> getSessionMessages(String sessionCode);

	/**
	 * 查询会话消息历史（分页）
	 */
	Flux<PlaygroundRoleplaySessionMessage> getSessionMessages(String sessionCode, int limit, int offset);

	/**
	 * 根据消息类型查询消息
	 */
	Flux<PlaygroundRoleplaySessionMessage> getSessionMessagesByType(String sessionCode, String messageType);

	/**
	 * 统计会话消息数量
	 */
	Mono<Long> countSessionMessages(String sessionCode);

	/**
	 * 批量添加消息（用于导入历史对话）
	 */
	Flux<PlaygroundRoleplaySessionMessage> batchAddMessages(String sessionCode,
			Flux<MessageCreateRequestDto> messageRequests);

	/**
	 * 转换JSON字段为Map对象
	 */
	default PlaygroundRoleplaySessionMessage convertJsonFields(PlaygroundRoleplaySessionMessage message) {
		message.setPayload(JsonUtils.jsonStringToMap(message.getPayloadStr()));

		return message;
	}

}
