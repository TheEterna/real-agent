package com.ai.agent.real.contract.model.logging;

import java.time.*;
import java.util.UUID;

/**
 * TraceInfo 值对象：承载追踪/审计相关字段。 作为组合到业务上下文中的独立载体，便于模块解耦与后续扩展。
 */
public class TraceInfo implements Traceable {

	/** 会话ID */
	protected UUID sessionId;

	/** 跟踪ID */
	protected UUID turnId;

	/** 起始时间戳（毫秒） */
	protected OffsetDateTime startTime;

	/** 截止时间戳（毫秒） */
	protected OffsetDateTime endTime;

	protected String messageId;

	protected String agentId;

	@Override
	public UUID getSessionId() {
		return sessionId;
	}

	@Override
	public Traceable setSessionId(UUID sessionId) {
		this.sessionId = sessionId;
		return this;
	}

	@Override
	public UUID getTurnId() {
		return turnId;
	}

	@Override
	public Traceable setTurnId(UUID turnId) {
		this.turnId = turnId;
		return this;
	}

	@Override
	public OffsetDateTime getStartTime() {
		return startTime;
	}

	@Override
	public Traceable setStartTime(OffsetDateTime startTime) {
		this.startTime = startTime;
		return this;
	}

	@Override
	public OffsetDateTime getEndTime() {
		return endTime;
	}

	@Override
	public Traceable setEndTime(OffsetDateTime endTime) {
		this.endTime = endTime;
		return this;
	}

	@Override
	public String getMessageId() {
		return messageId;
	}

	@Override
	public Traceable setMessageId(String messageId) {
		this.messageId = messageId;
		return this;
	}

	@Override
	public String getAgentId() {
		return agentId;
	}

	@Override
	public Traceable setAgentId(String agentId) {
		this.agentId = agentId;
		return this;
	}

}
