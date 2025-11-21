package com.ai.agent.real.contract.dto;

import lombok.Data;

import java.util.UUID;

/**
 * 聊天请求DTO
 *
 * @author han
 * @time 2025/11/12 21:57
 */
@Data
public class ChatRequest {

	private String message;

	private UUID sessionId;

}
