package com.ai.agent.real.contract.model.terminal;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 终端命令执行结果
 *
 * @author han
 * @time 2025/10/23 23:26
 */
public class TerminalCommandResult {

	/**
	 * 执行是否成功
	 */
	private boolean success;

	/**
	 * 输出内容
	 */
	private String output;

	/**
	 * 结构化数据
	 */
	private Object data;

	/**
	 * 错误信息
	 */
	private String error;

	/**
	 * 建议信息
	 */
	private String suggestion;

	/**
	 * 元数据
	 */
	private Metadata metadata;

	/**
	 * 渲染类型枚举
	 *
	 *
	 * /** 元数据
	 */
	@Data
	public static class Metadata {

		/**
		 * 执行时间(毫秒)
		 */
		private Long executionTime;

		/**
		 * 退出码
		 */
		private Integer exitCode;

		/**
		 * 警告信息
		 */
		private List<String> warnings;

		/**
		 * 服务器时间
		 */
		private String serverTime;

		/**
		 * 扩展信息
		 */
		private Map<String, Object> extra;

	}

	// Constructors
	public TerminalCommandResult() {
	}

	public TerminalCommandResult(boolean success, String output) {
		this.success = success;
		this.output = output;
	}

	public static TerminalCommandResult success(String output) {
		return new TerminalCommandResult(true, output);
	}

	public static TerminalCommandResult error(String error) {
		TerminalCommandResult result = new TerminalCommandResult();
		result.setSuccess(false);
		result.setError(error);
		return result;
	}

	public static TerminalCommandResult error(String error, String suggestion) {
		TerminalCommandResult result = error(error);
		result.setSuggestion(suggestion);
		return result;
	}

	// Getters and Setters
	public boolean isSuccess() {
		return success;
	}

	public void setSuccess(boolean success) {
		this.success = success;
	}

	public String getOutput() {
		return output;
	}

	public void setOutput(String output) {
		this.output = output;
	}

	public Object getData() {
		return data;
	}

	public void setData(Object data) {
		this.data = data;
	}

	public String getError() {
		return error;
	}

	public void setError(String error) {
		this.error = error;
	}

	public String getSuggestion() {
		return suggestion;
	}

	public void setSuggestion(String suggestion) {
		this.suggestion = suggestion;
	}

	public Metadata getMetadata() {
		return metadata;
	}

	public void setMetadata(Metadata metadata) {
		this.metadata = metadata;
	}

}