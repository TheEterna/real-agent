package com.ai.agent.real.contract.agent.context;

import com.ai.agent.real.contract.model.property.*;
import lombok.*;

/**
 * session 的配置类, 只包含, 影响核心功能的字段
 *
 * though: 这个类的成熟逻辑, 应该是 从 DB 直接读到 对应 session 的 配置, 每次都去读DB即可 优化: 1. 就直接用一个元组去 存在 memory 里
 * 2. 用的话, 直接取就好, 如果 不存在, 则从 DB 读 3. 当 用户修改 个性配置里的 上下文压缩选项时, 会去 memory 查一下 session 存不存在,
 * 如果存在, 则更新 memory 里的压缩配置
 *
 * @author han
 * @time 2025/10/13 0:37
 */
@Data
public class AgentSessionConfig {

	/**
	 * 压缩模式
	 */
	private ContextZipMode zipMode;

	public AgentSessionConfig(ContextZipMode zipMode) {
		this.zipMode = zipMode;
	}

}
