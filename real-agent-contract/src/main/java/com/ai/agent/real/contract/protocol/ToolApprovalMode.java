package com.ai.agent.real.contract.protocol;

/**
 * 工具执行审批模式
 *
 * @author han
 * @time 2025/9/19 1:15
 */
public enum ToolApprovalMode {

	AUTO, REQUIRE_APPROVAL, DISABLED;

	public static ToolApprovalMode from(String v) {
		if (v == null)
			return AUTO;
		String s = v.trim().toUpperCase().replace('-', '_');
		switch (s) {
			case "REQUIRE_APPROVAL":
				return REQUIRE_APPROVAL;
			case "DISABLED":
				return DISABLED;
			default:
				return AUTO;
		}
	}

}
