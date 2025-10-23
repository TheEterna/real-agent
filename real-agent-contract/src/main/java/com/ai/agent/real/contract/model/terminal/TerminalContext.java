package com.ai.agent.real.contract.model.terminal;

import java.util.List;
import java.util.Map;

/**
 * 终端执行上下文
 *
 * @author Real Agent Team
 * @since 2025-01-23
 */
public class TerminalContext {

	/**
	 * 是否连接到远程服务器
	 */
	private boolean isConnected;

	/**
	 * 当前工作目录
	 */
	private String currentPath;

	/**
	 * 用户信息
	 */
	private UserInfo user;

	/**
	 * 环境变量
	 */
	private Map<String, String> environment;

	/**
	 * 命令历史
	 */
	private List<String> history;

	/**
	 * 用户信息
	 */
	public static class UserInfo {

		private String id;

		private String username;

		private List<String> permissions;

		// Getters and Setters
		public String getId() {
			return id;
		}

		public void setId(String id) {
			this.id = id;
		}

		public String getUsername() {
			return username;
		}

		public void setUsername(String username) {
			this.username = username;
		}

		public List<String> getPermissions() {
			return permissions;
		}

		public void setPermissions(List<String> permissions) {
			this.permissions = permissions;
		}

	}

	// Constructors
	public TerminalContext() {
	}

	// Getters and Setters
	public boolean isConnected() {
		return isConnected;
	}

	public void setConnected(boolean connected) {
		isConnected = connected;
	}

	public String getCurrentPath() {
		return currentPath;
	}

	public void setCurrentPath(String currentPath) {
		this.currentPath = currentPath;
	}

	public UserInfo getUser() {
		return user;
	}

	public void setUser(UserInfo user) {
		this.user = user;
	}

	public Map<String, String> getEnvironment() {
		return environment;
	}

	public void setEnvironment(Map<String, String> environment) {
		this.environment = environment;
	}

	public List<String> getHistory() {
		return history;
	}

	public void setHistory(List<String> history) {
		this.history = history;
	}

}
