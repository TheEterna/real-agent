package com.ai.agent.real.application.tool.system;

import com.ai.agent.real.contract.agent.context.AgentContextAble;
import com.ai.agent.real.contract.exception.ToolException;
import com.ai.agent.real.contract.model.protocol.ToolResult;
import com.ai.agent.real.contract.tool.AgentTool;
import com.ai.agent.real.contract.tool.ToolSpec;

import static com.ai.agent.real.common.constant.NounConstants.PLAN_ADVANCE;

/**
 * a tool for advancing plan 调用 该工具时，顺便调用 planUpdate 工具
 *
 * @author han
 * @time 2025/10/30 22:58
 */
public class PlanAdvanceTool implements AgentTool {

	private final ToolSpec spec = new ToolSpec().setName(PLAN_ADVANCE)
		.setDescription("将任务状态从当前阶段推进到下一个阶段，确认当前阶段已完成")
		.setCategory("system")
		.setInputSchemaClass(null);

	/**
	 * 获取工具的唯一标识, 如果重复, 会抛出异常
	 * @return 工具的名称
	 */
	@Override
	public String getId() {
		return PLAN_ADVANCE;
	}

	/**
	 * 获取工具的规范。
	 * @return 工具的规范
	 */
	@Override
	public ToolSpec getSpec() {
		return this.spec;
	}

	/**
	 * execute tool, note: should catch Exception to cast ToolResult
	 * @param ctx 上下文
	 * @return 工具执行结果
	 * @throws ToolException 工具执行异常
	 */
	@Override
	public ToolResult<Object> execute(AgentContextAble<?> ctx) {
		return null;
	}

}
