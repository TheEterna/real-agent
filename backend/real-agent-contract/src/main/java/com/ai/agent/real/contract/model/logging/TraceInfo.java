package com.ai.agent.real.contract.model.logging;

import java.time.*;

/**
 * TraceInfo 值对象：承载追踪/审计相关字段。 作为组合到业务上下文中的独立载体，便于模块解耦与后续扩展。
 */
public class TraceInfo implements Traceable {

	/** 会话ID */
	protected String sessionId;

	/** 跟踪ID */
	protected String turnId;

	/** 起始时间戳（毫秒） */
	protected LocalDateTime startTime;

	/** 截止时间戳（毫秒） */
	protected LocalDateTime endTime;

	protected String messageId;

	protected String agentId;

	@Override
	public String getSessionId() {
		return sessionId;
	}

	@Override
	public Traceable setSessionId(String sessionId) {
		this.sessionId = sessionId;
		return this;
	}

	@Override
	public String getTurnId() {
		return turnId;
	}

	@Override
	public Traceable setTurnId(String turnId) {
		this.turnId = turnId;
		return this;
	}

	@Override
	public LocalDateTime getStartTime() {
		return startTime;
	}

	@Override
	public Traceable setStartTime(LocalDateTime startTime) {
		this.startTime = startTime;
		return this;
	}

	@Override
	public LocalDateTime getEndTime() {
		return endTime;
	}

	@Override
	public Traceable setEndTime(LocalDateTime endTime) {
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
