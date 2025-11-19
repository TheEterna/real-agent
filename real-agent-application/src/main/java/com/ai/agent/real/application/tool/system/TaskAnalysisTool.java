package com.ai.agent.real.application.tool.system;

import com.ai.agent.real.contract.agent.context.AgentContextAble;
import com.ai.agent.real.contract.model.protocol.ToolResult;
import com.ai.agent.real.contract.tool.AgentTool;
import com.ai.agent.real.contract.tool.ToolSpec;
import com.ai.agent.real.entity.agent.context.reactplus.AgentMode;
import com.ai.agent.real.entity.agent.context.reactplus.ReActPlusAgentContextMeta;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.ToolParam;

import java.util.List;

import static com.ai.agent.real.common.constant.NounConstants.TASK_ANALYSIS;

/**
 * @author han
 * @time 2025/11/4 01:18
 */

@Slf4j
public class TaskAnalysisTool implements AgentTool {

	private final ToolSpec spec = new ToolSpec().setName(TASK_ANALYSIS)
		.setDescription(
				"""
						任务难度分析工具，该工具为必须调用且唯一调用工具，用于评估用户任务请求的难度等级，提取核心任务内容，并给出专业的评估理由。
						该工具能够分析任务的复杂程度、所需步骤、依赖关系和执行方式，将任务划分为四个难度等级：Level 1（直接回复）、Level 1（自驱执行）、Level 2（思维链辅助推导）、Level 3（计划执行/Plan-and-Execute）和Level 4（思考规划）。
						同时准确提取用户的核心任务需求，去除冗余信息，形成简洁明确的任务描述.
						""")
		.setCategory("system")
		.setInputSchemaClass(TaskAnalysisTool.TaskAnalysisToolDto.class);

	/**
	 * 获取工具的唯一标识, 如果重复, 会抛出异常
	 * @return 工具的名称
	 */
	@Override
	public String getId() {
		return TASK_ANALYSIS;
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
	 * @param context 上下文
	 * @return 工具执行结果
	 */
	@Override
	public ToolResult execute(AgentContextAble<?> context) {
		long start = System.currentTimeMillis();
		TaskAnalysisTool.TaskAnalysisToolDto taskAnalysisToolDto = context
			.getStructuralToolArgs(TaskAnalysisTool.TaskAnalysisToolDto.class);

		int level = taskAnalysisToolDto.getLevel();
		// ReActPlus 上下文的元数据
		ReActPlusAgentContextMeta metadata = (ReActPlusAgentContextMeta) context.getMetadata();
		if (level == 0) {
			metadata.setAgentMode(AgentMode.DIRECT);
		}
		else if (level == 1) {
			metadata.setAgentMode(AgentMode.SIMPLE);
		}
		else if (level == 2) {
			metadata.setAgentMode(AgentMode.THOUGHT);
		}
		else if (level == 3) {
			metadata.setAgentMode(AgentMode.PLAN);
		}
		else if (level == 4) {
			metadata.setAgentMode(AgentMode.PLAN_THOUGHT);
		}
		metadata.setRealTask(taskAnalysisToolDto.getRealTask());
		metadata.setNote(taskAnalysisToolDto.getNote());
		context.setMetadata(metadata);
		return ToolResult.ok(taskAnalysisToolDto, start - System.currentTimeMillis(), getId());
	}

	@Data
	public static class TaskAnalysisToolDto {

		@ToolParam(required = true,
				description = "任务难度等级标识，为 1-4 的整数，对应自驱执行（1）、思维链辅助推导（2）、计划执行（3）、思考规划（4）四个等级，反映任务的复杂程度、执行步骤及依赖关系。")
		private int level;

		@ToolParam(required = true, description = "用户核心任务描述，以清晰动词开头的简洁语句，提取原始请求中的关键要素与约束条件，去除冗余信息，忠实反映用户真实意图。")
		private String realTask;

		@ToolParam(required = true, description = "任务难度评估依据，简洁专业的文本，明确指出决定难度等级的核心因素（如任务目标清晰度、步骤复杂度、信息整合需求等），不含因果关联词。")
		private String note;

	}

}
