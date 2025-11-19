package com.ai.agent.real.contract.model.terminal;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 终端命令执行请求
 *
 * @author han
 * @time: 2025/10/23 23:25
 */
@Data
public class TerminalCommandRequest {

	private String rawCommand;

	/**
	 * 命令名称
	 */
	private String commandName;

	/**
	 * 使用的 agent 策略
	 */
	private String agent;

	/**
	 * 参数列表（命令的操作目标，如文件路径、字符串等）
	 */
	private List<String> arguments;

	/**
	 * 选项参数（键值对）
	 */
	private List<String> options;

	/**
	 * 会话ID
	 */
	private String sessionId;

	/**
	 * 元数据
	 */
	private Map<String, Object> metadata;

	// Constructors
	public TerminalCommandRequest() {
	}

}
