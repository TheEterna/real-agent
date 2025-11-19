package com.ai.agent.real.contract.model.protocol;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * @author han
 * @time 2025/8/30 17:05 Execute tool request payload
 */
public class ExecuteToolRequest implements Serializable {

	private static final long serialVersionUID = 1L;

	private String toolName;

	private Map<String, Object> args = new HashMap<>();

	public ExecuteToolRequest() {
	}

	public ExecuteToolRequest(String toolName, Map<String, Object> args) {
		this.toolName = toolName;
		this.args = (args != null ? args : new HashMap<>());
	}

	public String getToolName() {
		return toolName;
	}

	public void setToolName(String toolName) {
		this.toolName = toolName;
	}

	public Map<String, Object> getArgs() {
		return args;
	}

	public void setArgs(Map<String, Object> args) {
		this.args = (args != null ? args : new HashMap<>());
	}

}
