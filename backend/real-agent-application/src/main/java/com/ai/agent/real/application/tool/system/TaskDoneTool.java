package com.ai.agent.real.application.tool.system;

import com.ai.agent.real.common.exception.ToolException;
import com.ai.agent.real.contract.agent.context.AgentContextAble;
import com.ai.agent.real.contract.model.protocol.*;
import com.ai.agent.real.contract.model.protocol.ToolResult.*;
import com.ai.agent.real.contract.tool.AgentTool;
import com.ai.agent.real.contract.tool.ToolSpec;
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

	private final ToolSpec spec = new ToolSpec().setName(TASK_DONE).setDescription("""
			当目前的返回结果满足用户需求, 则调用此工具标记任务已完成
			## 核心评估准则
			任务完成的条件包括：
			- ✅ 用户的需求或问题已被满足
			- ✅ 所有必要的信息已被提供
			- ✅ 用户没有进一步的问题或需求
			""").setCategory("system").setInputSchemaClass(TaskDoneToolDto.class);

	/**
	 * 获取工具的唯一标识, 如果重复, 会抛出异常
	 * @return 工具的名称
	 */
	@Override
	public String getId() {
		return TASK_DONE;
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
	public ToolResult<Object> execute(AgentContextAble<?> ctx) {
		long start = System.currentTimeMillis();
		try {
			String finishContent = ctx.getStructuralToolArgs(TaskDoneToolDto.class).getFinishContent();

			long elapsed = System.currentTimeMillis() - start;

			return ToolResult.ok(finishContent, elapsed, getId());

		}
		catch (Exception e) {
			return ToolResult.error(ToolResultCode.TOOL_EXECUTION_ERROR, e.getMessage(), getId(),
					System.currentTimeMillis() - start);
		}
	}

	/**
	 * must be public, otherwise jackson cannot compile the class
	 */

	@Data
	@NoArgsConstructor
	public static class TaskDoneToolDto {

		@ToolParam(required = true, description = """
				** 任务完成时需要传递的内容 **
				- 也许是需要输出的内容, 陈述一下任务状态 等等, 比如 任务已完成, 但仍然需要注意该数据由于数据过于异常, 可能是因为用户输入错误, 请注意
				- 或许是被要求输出的内容, 比如 用户要求任务结束时, 输出查询结果
				- 也可能是任务结束的原因, 比如 因要调用身份证查询工具, 但用户未提供该信息, 无法完成任务, 故任务结束
				- 风格要保持精炼，字数要严格把控，控制到70字之内
				""")
		String finishContent;

	}

}
