package com.ai.agent.real.application.service.agent;

import com.ai.agent.real.contract.service.context.TurnResumeStrategy;
import com.ai.agent.real.contract.model.protocol.AgentExecutionEvent;
import com.ai.agent.real.contract.service.agent.IAgentStorageService;
import com.ai.agent.real.contract.user.ISessionService;
import com.ai.agent.real.domain.entity.context.AgentMessage;
import com.ai.agent.real.domain.entity.context.Turn;
import com.ai.agent.real.domain.repository.context.AgentMessageRepository;
import com.ai.agent.real.domain.repository.context.TurnRepository;
import com.ai.agent.real.domain.repository.user.SessionRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.r2dbc.postgresql.codec.Json;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * @author: han
 * @time: 2025/11/21 22:12
 */
@Slf4j
@Service
public class AgentStorageService implements IAgentStorageService {

	private final TurnRepository turnRepository;

	private final AgentMessageRepository agentMessageRepository;

	private final TurnResumeStrategy turnResumeStrategy;

	private final ObjectMapper objectMapper;

	private final SessionRepository sessionRepository;

	public AgentStorageService(TurnRepository turnRepository, AgentMessageRepository agentMessageRepository,
			TurnResumeStrategy turnResumeStrategy, ObjectMapper objectMapper, SessionRepository sessionRepository) {
		this.turnRepository = turnRepository;
		this.agentMessageRepository = agentMessageRepository;
		this.turnResumeStrategy = turnResumeStrategy;
		this.objectMapper = objectMapper;
		this.sessionRepository = sessionRepository;
	}

	/**
	 * 开始一个新的 Turn
	 */
	@Transactional
	@Override
	public Mono<Turn> startTurn(UUID turnId, UUID parentTurnId, UUID sessionId) {
		// 保存 Turn， 更新 session 的 updatedTime
		Turn turn = Turn.builder()
			.id(turnId)
			.sessionId(sessionId)
			.parentTurnId(parentTurnId)
			.startTime(OffsetDateTime.now())
			.isNew(true)
			.build();
		return turnRepository.save(turn)
			// 更新 session 的 updatedTime
			.delayUntil(
					savedTurn -> sessionRepository.updateUpdatedTime(savedTurn.getSessionId(), OffsetDateTime.now()));

	}

	/**
	 * 保存 Agent 消息
	 */
	@Transactional
	@Override
	public Mono<AgentMessage> saveMessage(UUID sessionId, UUID turnId, AgentExecutionEvent event) {
		String content = "";
		Json data = null;

		try {
			if (event.getData() != null) {
				// 如果 data 是 String 类型，直接作为 content
				if (event.getData() instanceof String) {
					content = (String) event.getData();
				}
				else {
					// 否则序列化为 JSON
					String jsonStr = objectMapper.writeValueAsString(event.getData());
					data = Json.of(jsonStr);
					// 对于非 String 类型的数据，也可以考虑提取部分作为 content，或者 content 留空
					// 这里简单处理，如果是 TextEvent，通常 data 就是 String
				}
			}
		}
		catch (JsonProcessingException e) {
			log.error("Failed to serialize event data", e);
		}

		AgentMessage message = AgentMessage.builder()
			.id(UUID.randomUUID())
			.sessionId(sessionId)
			.turnId(turnId)
			.type(event.getType() != null ? event.getType().toString() : "unknown")
			.message(content)
			.data(data)
			.startTime(OffsetDateTime.now())
			.endTime(OffsetDateTime.now())
			.isNew(true)
			.build();

		return agentMessageRepository.save(message);
	}

	/**
	 * 完成 Turn，生成摘要
	 */
	@Transactional
	@Override
	public Mono<Turn> completeTurn(UUID turnId) {
		return turnRepository.findById(turnId)
			.flatMap(turn -> agentMessageRepository.findByTurnIdOrderByStartTimeAsc(turnId)
				.collectList()
				.flatMap(messages -> {
					String resume = turnResumeStrategy.generateResume(messages);
					turn.setResume(resume);
					turn.setEndTime(OffsetDateTime.now());
					turn.setNew(false); // 更新操作
					return turnRepository.save(turn);
				}));
	}

	/**
	 * 获取会话历史消息
	 */
	@Override
	public Flux<AgentMessage> getSessionMessages(UUID sessionId) {
		return agentMessageRepository.findBySessionIdOrderByStartTimeAsc(sessionId);
	}

}
