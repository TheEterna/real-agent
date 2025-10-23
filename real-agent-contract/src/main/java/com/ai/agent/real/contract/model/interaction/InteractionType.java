package com.ai.agent.real.contract.model.interaction;

/**
 * 交互请求类型 定义了所有可能的用户交互场景
 *
 * 设计目的： 1. 统一管理所有中断场景 2. 支持扩展新的交互类型 3. 便于前端根据类型渲染不同的UI
 *
 * @author han
 * @time 2025/10/22 17:00
 */
public enum InteractionType {

	/**
	 * 工具审批 场景：Agent 请求执行某个工具，需要用户审批
	 */
	TOOL_APPROVAL("工具审批", "Agent 请求执行工具，需要您的审批"),

	/**
	 * 缺少信息 场景：执行过程中缺少必要信息（如 API Key、配置项等）
	 */
	MISSING_INFO("缺少信息", "执行需要您提供额外信息"),

	/**
	 * 用户确认 场景：执行危险操作前需要用户确认（如删除文件、发送邮件等）
	 */
	USER_CONFIRMATION("用户确认", "请确认是否执行该操作"),

	/**
	 * 用户选择 场景：多个方案供用户选择
	 */
	USER_CHOICE("用户选择", "请选择一个方案"),

	/**
	 * 用户输入 场景：需要用户输入文本或数据
	 */
	USER_INPUT("用户输入", "请输入所需信息"),

	/**
	 * 自定义交互 场景：其他自定义的交互场景
	 */
	CUSTOM("自定义交互", "需要您的响应");

	private final String displayName;

	private final String description;

	InteractionType(String displayName, String description) {
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
