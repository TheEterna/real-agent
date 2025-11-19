package com.ai.agent.real.contract.model.interaction;

import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 交互响应 用户对交互请求的响应
 *
 * 设计目的： 1. 记录用户的选择和输入 2. 提供反馈信息给 Agent 3. 支持附加数据传递
 *
 * @author han
 * @time 2025/10/22 17:00
 */
@Data
@Accessors(chain = true)
public class InteractionResponse {

	/**
	 * 会话ID
	 */
	private String sessionId;

	/**
	 * 会话ID
	 */
	private String turnId;

	/**
	 * 选中的选项ID
	 */
	private String selectedOptionId;

	/**
	 * 用户提供的数据（如输入的文本、上传的文件等）
	 */
	private Map<String, Object> data = new HashMap<>();

}
