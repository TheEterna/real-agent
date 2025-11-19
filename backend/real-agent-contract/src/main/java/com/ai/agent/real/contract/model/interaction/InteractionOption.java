package com.ai.agent.real.contract.model.interaction;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.HashMap;
import java.util.Map;

/**
 * 交互选项 定义用户可以选择的一个选项
 *
 * 设计目的： 1. 提供灵活的选项配置 2. 支持选项级别的元数据 3. 便于前端渲染不同样式的选项
 *
 * @author han
 * @time 2025/10/22 17:00
 */
@Data
@Accessors(chain = true)
public class InteractionOption {

	/**
	 * 选项ID（唯一标识）
	 */
	private String optionId;

	/**
	 * 显示文本
	 */
	private String label;

	/**
	 * 详细描述
	 */
	private String description;

	/**
	 * 执行动作
	 */
	private InteractionAction action;

	/**
	 * 是否为默认选项
	 */
	private boolean isDefault = false;

	/**
	 * 是否为危险操作（前端可以用红色显示）
	 */
	private boolean isDangerous = false;

	/**
	 * 是否需要用户输入额外信息
	 */
	private boolean requiresInput = false;

	/**
	 * 输入提示（如果 requiresInput 为 true）
	 */
	private String inputPrompt;

	/**
	 * 元数据（用于存储额外信息）
	 */
	private Map<String, Object> metadata = new HashMap<>();

	/**
	 * 添加元数据
	 */
	public InteractionOption addMetadata(String key, Object value) {
		this.metadata.put(key, value);
		return this;
	}

}
