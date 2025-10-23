package com.ai.agent.real.domain.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

/**
 * 终端会话实体
 *
 * @author Real Agent Team
 * @since 2025-01-23
 */
@Table("terminal_sessions")
public class TerminalSession {

	@Id
	@Column("session_id")
	private String sessionId;

	@Column("user_id")
	private String userId;

	@Column("start_time")
	private LocalDateTime startTime;

	@Column("last_activity")
	private LocalDateTime lastActivity;

	@Column("status")
	private String status; // active/closed/timeout

	@Column("context")
	private String context; // JSON格式: {isConnected, currentPath, environment}

	@Column("metadata")
	private String metadata; // JSON格式，扩展字段

	// Constructors
	public TerminalSession() {
	}

	// Getters and Setters
	public String getSessionId() {
		return sessionId;
	}

	public void setSessionId(String sessionId) {
		this.sessionId = sessionId;
	}

	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}

	public LocalDateTime getStartTime() {
		return startTime;
	}

	public void setStartTime(LocalDateTime startTime) {
		this.startTime = startTime;
	}

	public LocalDateTime getLastActivity() {
		return lastActivity;
	}

	public void setLastActivity(LocalDateTime lastActivity) {
		this.lastActivity = lastActivity;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public String getContext() {
		return context;
	}

	public void setContext(String context) {
		this.context = context;
	}

	public String getMetadata() {
		return metadata;
	}

	public void setMetadata(String metadata) {
		this.metadata = metadata;
	}

}
