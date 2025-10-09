package com.ai.agent.real.tool.system.impl;

import com.ai.agent.real.contract.exception.*;
import com.ai.agent.real.contract.protocol.*;
import com.ai.agent.real.contract.protocol.ToolResult.*;
import com.ai.agent.real.contract.spec.*;
import lombok.*;
import lombok.extern.slf4j.*;
import org.springframework.ai.tool.annotation.*;

import static com.ai.agent.real.common.constant.NounConstants.*;

/**
 * 任务完成工具 用于标记任务已完成并提供最终结果，替代文本匹配的任务完成判断
 *
 * @author han
 * @time 2025/9/9 01:15
 */
@Slf4j
public class TaskDoneTool implements AgentTool {

	private final String id = "task_done";

	private final ToolSpec spec = new ToolSpec().setName(TASK_DONE)
		.setDescription("当目前的返回结果足以满足用户需求, 则调用此工具标记任务已完成")
		.setCategory("system")
		.setInputSchemaClass(TaskDoneToolDto.class);

	/**
	 * 获取工具的唯一标识, 如果重复, 会抛出异常
	 * @return 工具的名称
	 */
	@Override
	public String getId() {
		return id;
	}

	@Override
	public ToolSpec getSpec() {
		return spec;
	}

	/**
	 * 执行工具的操作。
	 * @param ctx 上下文
	 * @return 工具执行结果
	 * @throws ToolException 工具执行异常
	 */

	@Override
	public ToolResult<?> execute(AgentContext<Object> ctx) throws ToolException {
		long start = System.currentTimeMillis();
		try {
			String finishContent = ctx.getStructuralToolArgs(TaskDoneToolDto.class).getFinishContent();

			long elapsed = System.currentTimeMillis() - start;

			return ToolResult.ok(finishContent, elapsed, getId());

		}
		catch (Exception e) {
			return ToolResult.error(ToolResultCode.TOOL_EXECUTION_ERROR, e.getMessage(), getId());
		}
	}

	/**
	 * must be public, otherwise jackson cannot compile the class
	 */

	@Data
	@NoArgsConstructor
	public static class TaskDoneToolDto {

		@ToolParam(required = true, description = "任务完成时需要传递的内容, 也许是需要输出的内容, 或许是被要求输出的内容")
		String finishContent;

	}

}
