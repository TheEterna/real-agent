package com.ai.agent.real.contract.model.interaction;

/**
 * 交互动作类型 定义了用户响应后系统应该执行的动作
 *
 * 设计目的： 1. 标准化恢复执行的策略 2. 支持多种恢复路径 3. 便于扩展新的动作类型
 *
 * @author han
 * @time 2025/10/22 17:00
 */
public enum InteractionAction {

	/**
	 * 同意并执行 场景：用户同意，直接执行原计划
	 */
	APPROVE_AND_EXECUTE("同意并执行", "直接执行原计划"),

	/**
	 * 重新执行（带反馈） 场景：用户不满意当前方案，让 Agent 重新思考
	 */
	RETRY_WITH_FEEDBACK("重新执行", "让 Agent 重新思考并选择其他方案"),

	/**
	 * 拒绝并说明理由 场景：用户拒绝，并提供反馈给 Agent
	 */
	REJECT_WITH_REASON("拒绝并说明理由", "拒绝执行并提供反馈给 Agent"),

	/**
	 * 终止对话 场景：用户不想继续，直接结束整个任务
	 */
	TERMINATE("终止对话", "拒绝执行并结束整个任务"),

	/**
	 * 提供信息 场景：用户提供缺失的信息，继续执行
	 */
	PROVIDE_INFO("提供信息", "提供所需信息并继续执行"),

	/**
	 * 跳过当前步骤 场景：跳过当前操作，继续下一步
	 */
	SKIP("跳过", "跳过当前操作，继续下一步"),

	/**
	 * 自定义动作 场景：其他自定义的动作
	 */
	CUSTOM("自定义动作", "执行自定义逻辑");

	private final String displayName;

	private final String description;

	InteractionAction(String displayName, String description) {
		this.displayName = displayName;
		this.description = description;
	}

	public String getDisplayName() {
		return displayName;
	}

	public String getDescription() {
		return description;
	}

}
