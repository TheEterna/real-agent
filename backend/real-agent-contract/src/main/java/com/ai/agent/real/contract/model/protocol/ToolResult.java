package com.ai.agent.real.contract.model.protocol;

import java.util.HashMap;
import java.util.Map;

/**
 * @author han
 * @time 2025/9/11 23:19
 */
public class ToolResult<T> {

	/**
	 * code enum
	 */
	public enum ToolResultCode {

		SUCCESS("200", "执行成功"), TOOL_NOT_FOUND("404", "工具不存在"), TOOL_EXECUTION_ERROR("500", "工具执行错误");

		private final String code;

		private final String message;

		ToolResultCode(String s, String message) {
			this.code = s;
			this.message = message;
		}

		public String getCode() {
			return code;
		}

		public String getMessage() {
			return message;
		}

	}

	private boolean ok;

	private String message;

	private ToolResultCode code;

	private long elapsedMs;

	private T data;

	private Map<String, Object> meta = new HashMap<>();

	private String toolId;

	public static <T> ToolResult<T> ok(T data, long elapsed, String toolId) {
		ToolResult<T> r = new ToolResult<>();
		r.ok = true;
		r.code = ToolResultCode.SUCCESS;
		r.data = data;
		r.elapsedMs = elapsed;
		r.meta = new HashMap<>();
		r.toolId = toolId;
		return r;
	}

	public static <T> ToolResult<T> ok(T data, long elapsed, int toolId) {
		ToolResult<T> r = new ToolResult<>();
		r.ok = true;
		r.data = data;
		r.elapsedMs = elapsed;
		r.meta = new HashMap<>();
		r.toolId = String.valueOf(toolId);
		return r;
	}

	public static <T> ToolResult<T> error(ToolResultCode code, String message, String toolId, long elapsed) {
		ToolResult<T> r = new ToolResult<>();
		r.ok = false;
		r.code = code;
		r.message = message;
		r.elapsedMs = elapsed;
		r.meta = new HashMap<>();
		r.toolId = toolId;
		return r;
	}

	public boolean isOk() {
		return ok;
	}

	public String getMessage() {
		return message;
	}

	public ToolResultCode getCode() {
		return code;
	}

	public long getElapsedMs() {
		return elapsedMs;
	}

	public T getData() {
		return data;
	}

	public ToolResult<T> setElapsedMs(long v) {
		this.elapsedMs = v;
		return this;
	}

	public Map<String, Object> getMeta() {
		return meta;
	}

	public ToolResult<T> setMeta(Map<String, Object> meta) {
		this.meta = meta != null ? meta : new HashMap<>();
		return this;
	}

	public ToolResult<T> putMeta(String key, Object value) {
		if (this.meta == null) {
			this.meta = new HashMap<>();
		}
		this.meta.put(key, value);
		return this;
	}

}
