package com.ai.agent.real.web.controller;

import com.ai.agent.real.contract.model.protocol.*;
import com.ai.agent.real.domain.entity.roleplay.PlaygroundRoleplaySessionMessage;
import com.ai.agent.real.application.dto.*;
import com.ai.agent.real.application.service.*;
import com.ai.agent.real.common.utils.JsonUtils;
import com.ai.agent.real.domain.entity.roleplay.PlaygroundRoleplayRole;
import com.ai.agent.real.domain.repository.roleplay.PlaygroundRoleplayRoleRepository;
import com.ai.agent.real.domain.repository.roleplay.PlaygroundRoleplaySessionMessageRepository;
import com.ai.agent.real.domain.repository.roleplay.PlaygroundRoleplaySessionRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.validation.Valid;
import java.util.*;
import java.time.LocalDateTime;

/**
 * @author han
 * @time 2025/9/28 05:37
 */
@Slf4j
@RestController
@RequestMapping("/api/sessions/{sessionCode}/messages")
public class PlaygroundRoleplaySessionMessageController {

	private final PlaygroundRoleplaySessionMessageService messageService;

	// 以下用于 SSE 直连模型与存储
	private final PlaygroundRoleplaySessionRepository sessionRepository;

	private final PlaygroundRoleplaySessionMessageRepository messageRepository;

	private final PlaygroundRoleplayRoleRepository roleRepository;

	private final ChatModel chatModel;

	public PlaygroundRoleplaySessionMessageController(PlaygroundRoleplaySessionMessageService messageService,
			PlaygroundRoleplaySessionRepository sessionRepository,
			PlaygroundRoleplaySessionMessageRepository messageRepository,
			PlaygroundRoleplayRoleRepository roleRepository, ChatModel chatModel) {
		this.messageService = messageService;
		this.sessionRepository = sessionRepository;
		this.messageRepository = messageRepository;
		this.roleRepository = roleRepository;
		this.chatModel = chatModel;
	}

	/**
	 * 添加消息
	 */
	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public Mono<ResponseResult<PlaygroundRoleplaySessionMessage>> addMessage(@PathVariable String sessionCode,
                                                                             @Valid @RequestBody MessageCreateRequest request) {
		return messageService.addMessage(sessionCode, request).map(ResponseResult::success);
	}

	/**
	 * 查询消息历史
	 */
	@GetMapping
	public Mono<ResponseResult<List<PlaygroundRoleplaySessionMessage>>> getMessages(@PathVariable String sessionCode,
			@RequestParam(required = false) String messageType, @RequestParam(defaultValue = "50") int limit,
			@RequestParam(defaultValue = "0") int offset) {
		if (messageType != null && !messageType.trim().isEmpty()) {
			return messageService.getSessionMessagesByType(sessionCode, messageType)
				.collectList()
				.map(ResponseResult::success);
		}

		if (limit > 0 && offset >= 0) {
			return messageService.getSessionMessages(sessionCode, limit, offset)
				.collectList()
				.map(ResponseResult::success);
		}

		return messageService.getSessionMessages(sessionCode).collectList().map(ResponseResult::success);
	}

	/**
	 * 统计消息数量
	 */
	@GetMapping("/count")
	public Mono<ResponseResult<Long>> getMessageCount(@PathVariable String sessionCode) {
		return messageService.countSessionMessages(sessionCode).map(ResponseResult::success);
	}

	/**
	 * SSE 流式对话（POST），会先保存用户消息，再按 token 流式推送助手回复，最后保存助手消息
	 */
	@PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
	public Flux<ServerSentEvent<String>> streamMessage(@PathVariable String sessionCode,
			@Valid @RequestBody MessageCreateRequest request) {
		// 只支持用户文本消息触发流式
		if (request.getMessageType() != PlaygroundRoleplaySessionMessage.MessageType.USER_TEXT
				|| request.getRole() != PlaygroundRoleplaySessionMessage.MessageRole.USER) {
			return Flux.error(new IllegalArgumentException("仅支持 USER_TEXT/USER 触发流式回复"));
		}

		return sessionRepository.findBySessionCode(sessionCode)
			.switchIfEmpty(Mono.error(new IllegalArgumentException("会话不存在: " + sessionCode)))
			.flatMapMany(session -> {
				// 1) 保存用户消息
				PlaygroundRoleplaySessionMessage userMsg = PlaygroundRoleplaySessionMessage.builder()
					.sessionId(session.getId())
					.messageType(request.getMessageType())
					.role(request.getRole())
					.content(request.getContent())
					.payloadStr(JsonUtils
						.mapToJsonString(request.getPayload() != null ? request.getPayload() : Collections.emptyMap()))
					.assetUri(request.getAssetUri())
					.createdAt(LocalDateTime.now())
					.isNew(true)
					.build();

				Mono<ServerSentEvent<String>> ackEvent = messageRepository.save(userMsg)
					.thenReturn(ServerSentEvent.builder("user_saved").event("ack").build());

				// 2) 构造 Prompt
				Mono<String> systemPromptMono = roleRepository.findById(session.getRoleId())
					.map(PlaygroundRoleplayRole::getDescription)
					.map(desc -> (desc == null || desc.isBlank()) ? "你是一个友善、简洁的AI助手。" : desc);

				// 3) 模型流式生成
				Flux<ServerSentEvent<String>> streamFlux = systemPromptMono.flatMapMany(sys -> {
					SystemMessage sysMsg = new SystemMessage(sys);
					UserMessage usrMsg = new UserMessage(Optional.ofNullable(request.getContent()).orElse(""));
					Prompt prompt = new Prompt(List.of(sysMsg, usrMsg));

					StringBuilder sb = new StringBuilder();

					return chatModel.stream(prompt)
						.map(ChatResponse::getResult)
						.map(res -> res.getOutput().getText())
						.filter(Objects::nonNull)
						.map(text -> {
							sb.append(text);
							return ServerSentEvent.builder(text).event("delta").build();
						})
						.concatWith(Flux.defer(() -> {
							// 4) 保存助手完整消息
							PlaygroundRoleplaySessionMessage aiMsg = PlaygroundRoleplaySessionMessage.builder()
								.sessionId(session.getId())
								.messageType(PlaygroundRoleplaySessionMessage.MessageType.ASSISTANT_TEXT)
								.role(PlaygroundRoleplaySessionMessage.MessageRole.ASSISTANT)
								.content(sb.toString())
								.payloadStr(JsonUtils.mapToJsonString(Collections.emptyMap()))
								.assetUri(null)
								.createdAt(LocalDateTime.now())
								.isNew(true)
								.build();
							return messageRepository.save(aiMsg)
								.thenMany(Flux.just(ServerSentEvent.builder("[DONE]").event("done").build()));
						}))
						.onErrorResume(e -> {
							log.error("SSE 流式生成失败", e);
							return Flux.just(ServerSentEvent.builder("抱歉，生成失败。请稍后再试。").event("error").build());
						});

				});

				return Flux.concat(ackEvent, streamFlux);
			});
	}

}
