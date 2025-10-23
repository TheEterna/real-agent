package com.ai.agent.real.contract.model.interaction;

import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 交互请求 通用的中断点模型，用于请求用户交互
 *
 * 设计目的： 1. 统一所有类型的用户交互请求 2. 提供丰富的上下文信息 3. 支持灵活的选项配置
 *
 * @author han
 * @time 2025/10/22 17:00
 */
@Data
@Accessors(chain = true)
public class InteractionRequest {

	/**
	 * 请求ID（唯一标识）
	 */
	private String requestId;

	/**
	 * 会话ID
	 */
	private String sessionId;

	/**
	 * 交互类型
	 */
	private InteractionType type;

	/**
	 * 标题
	 */
	private String title;

	/**
	 * 提示消息
	 */
	private String message;

	/**
	 * 详细描述（可选）
	 */
	private String details;

	/**
	 * 上下文数据（用于传递额外信息）
	 */
	private Map<String, Object> context = new HashMap<>();

	/**
	 * 可选项列表
	 */
	private List<InteractionOption> options = new ArrayList<>();

	/**
	 * 是否必须响应（如果为 false，可以忽略该请求）
	 */
	private boolean required = true;

	/**
	 * 超时时间（秒） 如果用户在超时时间内未响应，系统可以自动选择默认选项或终止
	 */
	private Integer timeoutSeconds;

	/**
	 * 默认选项ID（超时时自动选择）
	 */
	private String defaultOptionId;

	/**
	 * 创建时间
	 */
	private LocalDateTime createdAt = LocalDateTime.now();

	/**
	 * 添加上下文数据
	 */
	public InteractionRequest addContext(String key, Object value) {
		this.context.put(key, value);
		return this;
	}

	/**
	 * 添加选项
	 */
	public InteractionRequest addOption(InteractionOption option) {
		this.options.add(option);
		return this;
	}

	/**
	 * 获取默认选项
	 */
	public InteractionOption getDefaultOption() {
		if (defaultOptionId != null) {
			return options.stream().filter(opt -> opt.getOptionId().equals(defaultOptionId)).findFirst().orElse(null);
		}
		return options.stream().filter(InteractionOption::isDefault).findFirst().orElse(null);
	}

}
