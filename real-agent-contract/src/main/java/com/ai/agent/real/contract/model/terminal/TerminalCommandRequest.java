package com.ai.agent.real.contract.model.terminal;

import java.util.Map;

/**
 * 终端命令执行请求
 *
 * @author Real Agent Team
 * @since 2025-01-23
 */
public class TerminalCommandRequest {

	/**
	 * 命令名称
	 */
	private String command;

	/**
	 * 位置参数
	 */
	private String[] args;

	/**
	 * 标记参数（布尔类型）
	 */
	private Map<String, Boolean> flags;

	/**
	 * 选项参数（键值对）
	 */
	private Map<String, Object> options;

	/**
	 * 会话ID
	 */
	private String sessionId;

	/**
	 * 执行上下文
	 */
	private TerminalContext context;

	/**
	 * 元数据
	 */
	private Map<String, Object> metadata;

	// Constructors
	public TerminalCommandRequest() {
	}

	// Getters and Setters
	public String getCommand() {
		return command;
	}

	public void setCommand(String command) {
		this.command = command;
	}

	public String[] getArgs() {
		return args;
	}

	public void setArgs(String[] args) {
		this.args = args;
	}

	public Map<String, Boolean> getFlags() {
		return flags;
	}

	public void setFlags(Map<String, Boolean> flags) {
		this.flags = flags;
	}

	public Map<String, Object> getOptions() {
		return options;
	}

	public void setOptions(Map<String, Object> options) {
		this.options = options;
	}

	public String getSessionId() {
		return sessionId;
	}

	public void setSessionId(String sessionId) {
		this.sessionId = sessionId;
	}

	public TerminalContext getContext() {
		return context;
	}

	public void setContext(TerminalContext context) {
		this.context = context;
	}

	public Map<String, Object> getMetadata() {
		return metadata;
	}

	public void setMetadata(Map<String, Object> metadata) {
		this.metadata = metadata;
	}

}
