package com.ai.agent.real.contract.model.property;

/**
 * TODO 1 工具执行审批模式 TODO 2 user can edit their personal tool permission list
 * 用户可以编辑自己的个性化工具权限列表
 *
 * @author han
 * @time 2025/9/19 1:15
 */
public enum ToolApprovalMode {

	/**
	 * 只有面对在权限列表中, 用户开放自动执行权限的工具, 才能自动执行
	 */
	AUTO,
	/**
	 * All tool execution requires user approval 所有工具的执行都有需要用户审批
	 */
	REQUIRE_APPROVAL,
	/**
	 * tool execution approval will be disabled, this is a high risk operation
	 */
	DISABLED

}
