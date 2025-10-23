package com.ai.agent.real.domain.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

/**
 * 终端命令执行历史实体
 *
 * @author Real Agent Team
 * @since 2025-01-23
 */
@Table("terminal_history")
public class TerminalHistory {

	@Id
	private String id;

	@Column("session_id")
	private String sessionId;

	@Column("user_id")
	private String userId;

	@Column("command_name")
	private String commandName;

	@Column("original_command")
	private String originalCommand;

	@Column("parsed_command")
	private String parsedCommand; // JSON格式: {command, args, flags, options}

	@Column("context")
	private String context; // JSON格式: {isConnected, currentPath, ...}

	@Column("result")
	private String result; // JSON格式: {success, output, data, metadata}

	@Column("execution_time")
	private Integer executionTime; // 执行时间(ms)

	@Column("exit_code")
	private Integer exitCode;

	@Column("error_message")
	private String errorMessage;

	@Column("timestamp")
	private LocalDateTime timestamp;

	// Constructors
	public TerminalHistory() {
	}

	// Getters and Setters
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

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

	public String getCommandName() {
		return commandName;
	}

	public void setCommandName(String commandName) {
		this.commandName = commandName;
	}

	public String getOriginalCommand() {
		return originalCommand;
	}

	public void setOriginalCommand(String originalCommand) {
		this.originalCommand = originalCommand;
	}

	public String getParsedCommand() {
		return parsedCommand;
	}

	public void setParsedCommand(String parsedCommand) {
		this.parsedCommand = parsedCommand;
	}

	public String getContext() {
		return context;
	}

	public void setContext(String context) {
		this.context = context;
	}

	public String getResult() {
		return result;
	}

	public void setResult(String result) {
		this.result = result;
	}

	public Integer getExecutionTime() {
		return executionTime;
	}

	public void setExecutionTime(Integer executionTime) {
		this.executionTime = executionTime;
	}

	public Integer getExitCode() {
		return exitCode;
	}

	public void setExitCode(Integer exitCode) {
		this.exitCode = exitCode;
	}

	public String getErrorMessage() {
		return errorMessage;
	}

	public void setErrorMessage(String errorMessage) {
		this.errorMessage = errorMessage;
	}

	public LocalDateTime getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(LocalDateTime timestamp) {
		this.timestamp = timestamp;
	}

}
