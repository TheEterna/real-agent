package com.ai.agent.real.application.service.Impl;

import com.ai.agent.real.application.dto.*;
import com.ai.agent.real.common.constant.RoleplayConstants;
import com.ai.agent.real.domain.entity.roleplay.*;
import com.ai.agent.real.common.utils.*;
import com.ai.agent.real.domain.repository.roleplay.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.LocalDateTime;
import java.util.Collections;

/**
 * 角色扮演会话消息服务层 (R2DBC 响应式)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PlaygroundRoleplaySessionMessageServiceImpl
		implements com.ai.agent.real.application.service.PlaygroundRoleplaySessionMessageService {

	private final PlaygroundRoleplaySessionMessageRepository messageRepository;

	private final PlaygroundRoleplaySessionRepository sessionRepository;

	private final PlaygroundRoleplayRoleRepository roleRepository;

	private final ChatModel chatModel;

	/**
	 * 添加消息
	 */
	@Override
	public Mono<PlaygroundRoleplaySessionMessage> addMessage(String sessionCode, MessageCreateRequest request) {
		return sessionRepository.findBySessionCode(sessionCode)
			.switchIfEmpty(Mono.error(new IllegalArgumentException("会话不存在: " + sessionCode)))
			.flatMap(session -> {
				if (session.getStatus() != RoleplayConstants.SessionStatus.ACTIVE) {
					return Mono.error(new IllegalArgumentException("会话已结束，无法添加消息: " + sessionCode));
				}

				return Mono.just(PlaygroundRoleplaySessionMessage.builder()
					.sessionId(session.getId())
					.messageType(request.getMessageType())
					.role(request.getRole())
					.content(request.getContent())
					.payloadStr(JsonUtils
						.mapToJsonString(request.getPayload() != null ? request.getPayload() : Collections.emptyMap()))
					.assetUri(request.getAssetUri())
					.createdAt(LocalDateTime.now())
					.isNew(true)
					.build());
			})
			.flatMap(messageRepository::save)
			// 如果是用户文本消息，则调用大模型生成助手回复并保存（异步串联，不改变返回值）
			.flatMap(savedUserMessage -> {
				boolean isUserText = savedUserMessage
					.getMessageType() == PlaygroundRoleplaySessionMessage.MessageType.USER_TEXT
						&& savedUserMessage.getRole() == PlaygroundRoleplaySessionMessage.MessageRole.USER;
				if (!isUserText) {
					return Mono.just(savedUserMessage);
				}

				Mono<Void> generateAndSave = sessionRepository.findById(savedUserMessage.getSessionId())
					.flatMap(sess -> roleRepository.findById(sess.getRoleId()).map(role -> new Object[] { sess, role }))
					.flatMap(tuple -> {
						PlaygroundRoleplaySessionMessage userMsg = savedUserMessage;
						String systemPrompt = tuple[1] != null ? ((PlaygroundRoleplayRole) tuple[1]).getDescription()
								: null;
						if (systemPrompt == null || systemPrompt.isBlank()) {
							systemPrompt = "你是一个友善、简洁的AI助手。";
						}

						String userContent = userMsg.getContent() != null ? userMsg.getContent() : "";
						SystemMessage sys = new SystemMessage(systemPrompt);
						UserMessage usr = new UserMessage(userContent);
						Prompt prompt = new Prompt(java.util.List.of(sys, usr));

						return Mono.fromCallable(() -> chatModel.call(prompt))
							.subscribeOn(Schedulers.boundedElastic())
							.map(ChatResponse::getResult)
							.map(result -> result.getOutput().getText())
							.onErrorResume(e -> {
								log.error("生成助手回复失败: sessionId={}", userMsg.getSessionId(), e);
								return Mono.just("抱歉，我暂时无法回复，请稍后再试。");
							})
							.flatMap(aiContent -> {
								PlaygroundRoleplaySessionMessage aiMessage = PlaygroundRoleplaySessionMessage.builder()
									.sessionId(userMsg.getSessionId())
									.messageType(PlaygroundRoleplaySessionMessage.MessageType.ASSISTANT_TEXT)
									.role(PlaygroundRoleplaySessionMessage.MessageRole.ASSISTANT)
									.content(aiContent)
									.payloadStr(JsonUtils.mapToJsonString(Collections.emptyMap()))
									.assetUri(null)
									.createdAt(LocalDateTime.now())
									.isNew(true)
									.build();
								return messageRepository.save(aiMessage).then();
							});
					})
					.onErrorResume(e -> {
						// 不中断主流程，仅记录错误
						log.error("自动生成助手消息失败: sessionId={}", savedUserMessage.getSessionId(), e);
						return Mono.empty();
					});

				return generateAndSave.thenReturn(savedUserMessage);
			})
			.map(this::convertJsonFields)
			.doOnSuccess(savedMessage -> log.info("添加消息成功: sessionCode={}, messageType={}", sessionCode,
					savedMessage.getMessageType()))
			.doOnError(error -> log.error("添加消息失败: sessionCode={}, messageType={}", sessionCode,
					request.getMessageType(), error));
	}

	/**
	 * 查询会话消息历史
	 */
	@Override
	public Flux<PlaygroundRoleplaySessionMessage> getSessionMessages(String sessionCode) {
		return sessionRepository.findBySessionCode(sessionCode)
			.switchIfEmpty(Mono.error(new IllegalArgumentException("会话不存在: " + sessionCode)))
			.flatMapMany(session -> messageRepository.findBySessionIdOrderBySeq(session.getId()))
			.map(this::convertJsonFields);
	}

	/**
	 * 查询会话消息历史（分页）
	 */
	@Override
	public Flux<PlaygroundRoleplaySessionMessage> getSessionMessages(String sessionCode, int limit, int offset) {
		return sessionRepository.findBySessionCode(sessionCode)
			.switchIfEmpty(Mono.error(new IllegalArgumentException("会话不存在: " + sessionCode)))
			.flatMapMany(session -> messageRepository.findBySessionIdOrderByCreatedAtAscWithPaging(session.getId(),
					limit, offset))
			.map(this::convertJsonFields);
	}

	/**
	 * 根据消息类型查询消息
	 */
	@Override
	public Flux<PlaygroundRoleplaySessionMessage> getSessionMessagesByType(String sessionCode, String messageType) {
		return sessionRepository.findBySessionCode(sessionCode)
			.switchIfEmpty(Mono.error(new IllegalArgumentException("会话不存在: " + sessionCode)))
			.flatMapMany(
					session -> messageRepository.findBySessionIdAndMessageTypeOrderBySeq(session.getId(), messageType))
			.map(this::convertJsonFields);
	}

	/**
	 * 统计会话消息数量
	 */
	@Override
	public Mono<Long> countSessionMessages(String sessionCode) {
		return sessionRepository.findBySessionCode(sessionCode)
			.switchIfEmpty(Mono.error(new IllegalArgumentException("会话不存在: " + sessionCode)))
			.flatMap(session -> messageRepository.countBySessionId(session.getId()));
	}

	/**
	 * 批量添加消息（用于导入历史对话）
	 */
	@Override
	public Flux<PlaygroundRoleplaySessionMessage> batchAddMessages(String sessionCode,
			Flux<MessageCreateRequest> messageRequests) {
		return sessionRepository.findBySessionCode(sessionCode)
			.switchIfEmpty(Mono.error(new IllegalArgumentException("会话不存在: " + sessionCode)))
			.flatMapMany(session -> {
				if (session.getStatus() != RoleplayConstants.SessionStatus.ACTIVE) {
					return Flux.error(new IllegalArgumentException("会话已结束，无法添加消息: " + sessionCode));
				}
				return messageRequests
					.map(request -> PlaygroundRoleplaySessionMessage.builder()
						.sessionId(session.getId())
						.messageType(request.getMessageType())
						.role(request.getRole())
						.content(request.getContent())
						.payloadStr(JsonUtils.mapToJsonString(
								request.getPayload() != null ? request.getPayload() : Collections.emptyMap()))
						.assetUri(request.getAssetUri())
						.createdAt(LocalDateTime.now())
						.isNew(true)
						.build())
					.flatMap(messageRepository::save)
					.map(this::convertJsonFields);
			})
			.doOnNext(savedMessage -> log.debug("批量添加消息: sessionCode={}", sessionCode))
			.doOnComplete(() -> log.info("批量添加消息完成: sessionCode={}", sessionCode))
			.doOnError(error -> log.error("批量添加消息失败: sessionCode={}", sessionCode, error));
	}

}
