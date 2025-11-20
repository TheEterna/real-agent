package com.ai.agent.real.application.user;

import com.ai.agent.real.common.utils.CommonUtils;
import com.ai.agent.real.contract.user.ISessionService;
import com.ai.agent.real.contract.user.SessionDTO;
import com.ai.agent.real.domain.entity.user.Session;
import com.ai.agent.real.domain.repository.user.SessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.OffsetDateTime;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 会话业务服务
 */
@Service
@RequiredArgsConstructor
public class SessionService implements ISessionService {

	private final SessionRepository sessionRepository;

	private final ChatClient chatClient;

	/**
	 * 创建新会话
	 * @param title 会话标题
	 * @param type 会话类型
	 * @param userId 用户ID
	 * @param firstUserMessage
	 * @return 创建的会话
	 */
	@Override
	public Mono<SessionDTO> createSessionWithAi(String title, String type, UUID userId, String firstUserMessage) {

		// 1. 确定标题来源：如果有传入 title，则直接使用；否则调用 AI 生成
		Mono<String> titleMono;
		if (StringUtils.hasText(title)) {
			titleMono = Mono.just(title);
		}
		else {
			String prompt = "请根据以下用户输入生成一个简短的会话标题（15字以内），直接返回标题内容，不要包含引号或其他标点：\n" + firstUserMessage;
			titleMono = chatClient.prompt()
				.user(prompt)
				.stream()
				.content()
				.collect(Collectors.joining())
				.defaultIfEmpty("新会话") // AI 响应为空时的兜底
				.map(String::trim);
		}

		// 2. 获取最终标题并保存会话
		return titleMono.flatMap(finalTitle -> {
			Session session = Session.builder()
				.id(UUID.randomUUID()) // 根据你的 Entity 配置，如果由 DB 生成 ID，这里可能不需要设置
				.title(finalTitle) // 使用最终确定的标题
				.type(type)
				.userId(userId)
				.startTime(OffsetDateTime.now())
				.isNew(true) // 标记为新记录以强制 INSERT
				.build();
			return sessionRepository.save(session);
		}).map(SessionDTO::fromEntity);
	}

	/**
	 * 根据ID获取会话
	 */
	@Override
	public Mono<SessionDTO> getSessionById(UUID sessionId) {
		return sessionRepository.findById(sessionId).map(SessionDTO::fromEntity);
	}

	/**
	 * 根据用户ID获取会话列表
	 */
	@Override
	public Flux<SessionDTO> getSessionsByUserId(UUID userId) {
		return sessionRepository.findByUserIdOrderByStartTimeDesc(userId).map(SessionDTO::fromEntity);
	}

	/**
	 * 根据用户ID和类型获取会话列表
	 */
	@Override
	public Flux<SessionDTO> getSessionsByUserIdAndType(UUID userId, String type) {
		return sessionRepository.findByUserIdAndType(userId, type).map(SessionDTO::fromEntity);
	}

	/**
	 * 更新会话
	 */
	@Override
	public Mono<SessionDTO> updateSession(SessionDTO sessionDTO) {
		Session session = sessionDTO.toEntity();
		return sessionRepository.save(session).map(SessionDTO::fromEntity);
	}

	/**
	 * 删除会话
	 */
	@Override
	public Mono<Void> deleteSession(UUID id) {
		return sessionRepository.deleteById(id);
	}

	/**
	 * 检查会话是否属于指定用户
	 */
	@Override
	public Mono<Boolean> isSessionBelongsToUser(UUID sessionId, UUID userId) {
		return sessionRepository.existsByIdAndUserId(sessionId, userId);
	}

	/**
	 * 使用 AI 生成标题并创建新会话
	 */
	private Mono<String> createSessionWithAiTitle(UUID userId, String type, String message) {

	}

}