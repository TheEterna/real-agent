package com.ai.agent.real.application.tool.system;

import com.ai.agent.real.contract.agent.context.AgentContextAble;
import com.ai.agent.real.contract.exception.ToolException;
import com.ai.agent.real.contract.model.protocol.ToolResult;
import com.ai.agent.real.contract.tool.AgentTool;
import com.ai.agent.real.contract.tool.ToolSpec;
import com.ai.agent.real.entity.agent.context.reactplus.ReActPlusAgentContextMeta;
import com.ai.agent.real.entity.agent.context.reactplus.TaskModeMeta;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.ToolParam;

import java.util.List;

import static com.ai.agent.real.common.constant.NounConstants.PLAN_ADVANCE;

/**
 * 计划推进工具 - 用于将任务从当前阶段推进到下一个阶段
 * 调用该工具时，会将当前阶段标记为完成，并移动到下一个待执行阶段
 *
 * @author han
 * @time 2025/10/30 22:58
 */
@Slf4j
public class PlanAdvanceTool implements AgentTool {

	private final ToolSpec spec = new ToolSpec().setName(PLAN_ADVANCE)
		.setDescription("""
				用于推进计划执行，将当前阶段标记为完成并移动到下一阶段

				**使用场景**：
				1. 当前阶段的所有任务已经完成
				2. 已验证当前阶段的交付成果符合要求
				3. 准备开始下一个阶段的工作

				**工具行为**：
				- 将当前阶段状态标记为 DONE（已完成）
				- 自动定位到下一个待执行阶段
				- 将下一阶段状态标记为 RUNNING（执行中）
				- 更新上下文中的当前阶段ID

				**注意事项**：
				- 必须确保当前阶段的工作已完成
				- 如果已经是最后一个阶段，将返回任务全部完成的提示
				- 推进后无法回退，请谨慎使用

				**示例**：
				当完成"数据采集与清洗"阶段后，调用此工具推进到"数据分析"阶段
				""")
		.setCategory("system")
		.setInputSchemaClass(PlanAdvanceToolDto.class);

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

		long start = System.currentTimeMillis();

		try {
			// 1. 获取输入参数
			PlanAdvanceToolDto toolArgs = ctx.getStructuralToolArgs(PlanAdvanceToolDto.class);
			String completionSummary = toolArgs.getCompletionSummary();

			// 2. 获取当前的元数据
			ReActPlusAgentContextMeta meta = (ReActPlusAgentContextMeta) ctx.getMetadata();
			if (meta == null || meta.getTaskModeMeta() == null) {
				return ToolResult.error(ToolResult.ToolResultCode.TOOL_EXECUTION_ERROR, "未找到任务计划信息，请先使用 plan_init 工具初始化计划",
						getId(), System.currentTimeMillis() - start);
			}

			TaskModeMeta taskModeMeta = meta.getTaskModeMeta();
			String currentTaskId = taskModeMeta.getCurrentTaskId();
			List<TaskModeMeta.TaskPhase> taskPhaseList = taskModeMeta.getTaskPhaseList();

			if (taskPhaseList == null || taskPhaseList.isEmpty()) {
				return ToolResult.error(ToolResult.ToolResultCode.TOOL_EXECUTION_ERROR, "任务阶段列表为空", getId(),
						System.currentTimeMillis() - start);
			}

			// 3. 查找当前阶段
			TaskModeMeta.TaskPhase currentPhase = taskModeMeta.getCurrentTask();

			int currentIndex = -1;
			for (int i = 0; i < taskPhaseList.size(); i++) {
				if (taskPhaseList.get(i).getId().equals(currentTaskId)) {
					currentIndex = i;
					break;
				}
			}

			if (currentPhase == null) {
				return ToolResult.error(ToolResult.ToolResultCode.TOOL_EXECUTION_ERROR, "未找到当前执行阶段", getId(),
						System.currentTimeMillis() - start);
			}

			// 4. 标记当前阶段为完成
			currentPhase.setTaskStatus(TaskModeMeta.TaskStatus.DONE);
			log.info("阶段 [{}] 已完成: {}", currentPhase.getTitle(), completionSummary);

			// 5. 查找下一个待执行阶段
			TaskModeMeta.TaskPhase nextPhase = null;
			for (int i = currentIndex + 1; i < taskPhaseList.size(); i++) {
				TaskModeMeta.TaskPhase phase = taskPhaseList.get(i);
				if (phase.getTaskStatus() == TaskModeMeta.TaskStatus.TODO) {
					nextPhase = phase;
					break;
				}
			}

			// 6. 构建返回结果
			StringBuilder resultBuilder = new StringBuilder();
			resultBuilder.append("✅ **阶段推进成功**\n\n");
			resultBuilder.append("**已完成阶段**: ").append(currentPhase.getTitle()).append("\n");
			resultBuilder.append("**完成总结**: ").append(completionSummary).append("\n\n");

			if (nextPhase != null) {
				// 有下一个阶段，推进到下一阶段
				nextPhase.setTaskStatus(TaskModeMeta.TaskStatus.RUNNING);
				taskModeMeta.setCurrentTaskId(nextPhase.getId());

				resultBuilder.append("**下一阶段**: ").append(nextPhase.getTitle()).append("\n");
				resultBuilder.append("**阶段描述**: ").append(nextPhase.getDescription()).append("\n");
				resultBuilder.append("**是否并行**: ").append(nextPhase.isParallel() ? "是" : "否").append("\n\n");

				// 统计进度
				long completedCount = taskPhaseList.stream()
					.filter(p -> p.getTaskStatus() == TaskModeMeta.TaskStatus.DONE)
					.count();
				resultBuilder.append("**整体进度**: ").append(completedCount).append("/").append(taskPhaseList.size())
					.append(" (").append(String.format("%.1f", (completedCount * 100.0 / taskPhaseList.size())))
					.append("%)\n");

				log.info("推进到下一阶段: [{}]", nextPhase.getTitle());
			}
			else {
				// 所有阶段已完成
				resultBuilder.append("🎉 **所有任务阶段已完成！**\n\n");
				resultBuilder.append("**项目目标**: ").append(taskModeMeta.getGoal()).append("\n");
				resultBuilder.append("**总阶段数**: ").append(taskPhaseList.size()).append("\n");
				resultBuilder.append("**状态**: 全部完成\n");

				log.info("所有任务阶段已完成！项目目标: {}", taskModeMeta.getGoal());
			}

			long elapsed = System.currentTimeMillis() - start;
			return ToolResult.ok(resultBuilder.toString(), elapsed, getId());

		}
		catch (Exception e) {
			log.error("计划推进工具执行失败", e);
			return ToolResult.error(ToolResult.ToolResultCode.TOOL_EXECUTION_ERROR, "推进失败: " + e.getMessage(), getId(),
					System.currentTimeMillis() - start);
		}
	}

	@Data
	@NoArgsConstructor
	public static class PlanAdvanceToolDto {

		@ToolParam(required = true, description = """
				**当前阶段完成总结**
				- 简要描述当前阶段已完成的工作内容
				- 说明达成的关键成果和交付物
				- 提及遇到的主要问题及解决方案（如有）
				- 给出对完成质量的评价

				**示例**：
				"已完成数据采集与清洗阶段。采集了近3个月的用户行为日志共计500万条记录，清洗后保留有效数据480万条。处理了缺失值和异常值，输出标准化数据集。数据质量验证通过，准备进入分析阶段。"

				**注意**：
				- 总结要具体明确，包含量化指标
				- 体现阶段目标的达成情况
				- 为后续阶段提供清晰的输入依据
				""")
		private String completionSummary;

	}

}
