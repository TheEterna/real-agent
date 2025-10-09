package com.ai.agent.real.application.service.Impl;

import com.ai.agent.real.application.dto.*;
import com.ai.agent.real.application.service.*;
import com.ai.agent.real.common.constant.RoleplayConstants;
import com.ai.agent.real.common.utils.*;
import com.ai.agent.real.domain.entity.roleplay.PlaygroundRoleplaySession;
import com.ai.agent.real.domain.repository.roleplay.*;
import com.ai.agent.real.domain.repository.user.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.Collections;

/**
 * 角色扮演会话服务层 (R2DBC 响应式)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PlaygroundRoleplaySessionServiceImpl implements PlaygroundRoleplaySessionService {

	private final PlaygroundRoleplaySessionRepository sessionRepository;

	private final UserRepository userRepository;

	private final PlaygroundRoleplayRoleRepository roleRepository;

	/**
	 * 创建会话
	 */
	@Override
	public Mono<PlaygroundRoleplaySession> createSession(SessionCreateRequest request) {
		// 验证用户和角色是否存在
		return userRepository.existsById(request.getUserId()).flatMap(userExists -> {
			if (!userExists) {
				return Mono.error(new IllegalArgumentException("用户不存在: " + request.getUserId()));
			}
			return roleRepository.existsById(request.getRoleId());
		}).flatMap(roleExists -> {
			if (!roleExists) {
				return Mono.error(new IllegalArgumentException("角色不存在: " + request.getRoleId()));
			}

			String sessionCode = UuidUtils.generateSessionCode();
			PlaygroundRoleplaySession session = PlaygroundRoleplaySession.builder()
				.sessionCode(sessionCode)
				.userId(request.getUserId())
				.roleId(request.getRoleId())
				.status(RoleplayConstants.SessionStatus.ACTIVE)
				.metadataStr(JsonUtils.mapToJsonString(Collections.emptyMap()))
				.createdAt(LocalDateTime.now())
				.isNew(true)
				.build();

			return sessionRepository.save(session);
		})
			.map(this::convertJsonFields)
			.doOnSuccess(savedSession -> log.info("创建会话成功: sessionCode={}, userId={}, roleId={}",
					savedSession.getSessionCode(), savedSession.getUserId(), savedSession.getRoleId()))
			.doOnError(error -> log.error("创建会话失败: userId={}, roleId={}", request.getUserId(), request.getRoleId(),
					error));
	}

	/**
	 * 根据会话编码查找会话
	 */
	@Override
	public Mono<PlaygroundRoleplaySession> findBySessionCode(String sessionCode) {
		return sessionRepository.findBySessionCode(sessionCode).map(this::convertJsonFields);
	}

	/**
	 * 查找用户的会话列表
	 */
	@Override
	public Flux<PlaygroundRoleplaySession> findUserSessions(Long userId) {
		return sessionRepository.findByUserIdOrderByCreatedAtAsc(userId).map(this::convertJsonFields);
	}

	/**
	 * 查找用户进行中的会话
	 */
	@Override
	public Flux<PlaygroundRoleplaySession> findActiveUserSessions(Long userId) {
		return sessionRepository.findActiveSessionsByUserId(userId).map(this::convertJsonFields);
	}

	/**
	 * 结束会话
	 */
	@Override
	public Mono<PlaygroundRoleplaySession> endSession(String sessionCode, String summary) {
		return sessionRepository.findBySessionCode(sessionCode)
			.switchIfEmpty(Mono.error(new IllegalArgumentException("会话不存在: " + sessionCode)))
			.flatMap(session -> {
				if (session.getStatus() != RoleplayConstants.SessionStatus.ACTIVE) {
					return Mono.error(new IllegalArgumentException("会话已结束: " + sessionCode));
				}

				session.setStatus(RoleplayConstants.SessionStatus.ENDED);
				session.setSummary(summary);
				session.setEndedAt(LocalDateTime.now());
				session.setNew(false);

				return sessionRepository.save(session);
			})
			.map(this::convertJsonFields)
			.doOnSuccess(endedSession -> log.info("结束会话成功: sessionCode={}", sessionCode))
			.doOnError(error -> log.error("结束会话失败: sessionCode={}", sessionCode, error));
	}

	/**
	 * 更新会话元数据
	 */
	@Override
	public Mono<PlaygroundRoleplaySession> updateSessionMetadata(String sessionCode,
			java.util.Map<String, Object> metadata) {
		return sessionRepository.findBySessionCode(sessionCode)
			.switchIfEmpty(Mono.error(new IllegalArgumentException("会话不存在: " + sessionCode)))
			.map(session -> {
				session.setMetadataStr(JsonUtils.mapToJsonString(metadata));
				session.setNew(false);
				return session;
			})
			.flatMap(sessionRepository::save)
			.map(this::convertJsonFields)
			.doOnSuccess(updatedSession -> log.info("更新会话元数据成功: sessionCode={}", sessionCode))
			.doOnError(error -> log.error("更新会话元数据失败: sessionCode={}", sessionCode, error));
	}

}
