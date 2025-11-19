package com.ai.agent.real.contract.model.property;

/**
 * @author han
 * @time 2025/10/12 22:33
 */
public enum ContextZipMode {

	/**
	 * 不使用任何压缩技术
	 */
	DISABLED,
	/**
	 * 只使用对话的 finalAgent 回复, 或最后一次回复
	 */
	ZIP,
	/**
	 * 大致 landing 思路为 ai 压缩
	 */
	CRAZY_ZIP

}
