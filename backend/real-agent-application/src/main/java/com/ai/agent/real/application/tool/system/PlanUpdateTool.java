package com.ai.agent.real.application.tool.system;

import com.ai.agent.real.contract.agent.context.AgentContextAble;
import com.ai.agent.real.contract.exception.ToolException;
import com.ai.agent.real.contract.model.protocol.ToolResult;
import com.ai.agent.real.contract.tool.AgentTool;
import com.ai.agent.real.contract.tool.ToolSpec;
import com.ai.agent.real.entity.agent.context.reactplus.ReActPlusAgentContextMeta;
import com.ai.agent.real.entity.agent.context.reactplus.TaskModeMeta;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.ToolParam;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import static com.ai.agent.real.common.constant.NounConstants.PLAN_UPDATE;

/**
 * 当通过 web查询，推理思考，发现先前所列计划与推理结果大相径庭时，需要执行该工具
 *
 * @author han
 * @time 2025/10/31 23:04
 */
@Slf4j
public class PlanUpdateTool implements AgentTool {

	private final ToolSpec spec = new ToolSpec().setName(PLAN_UPDATE)
		.setDescription("经过深度思考及权威认证后发现先前所列计划与推理结果大相径庭时，需要执行该工具进行计划修改")
		.setCategory("system")
		.setInputSchemaClass(PlanUpdateTool.PlanUpdateToolDto.class);

	/**
	 * 获取工具的唯一标识, 如果重复, 会抛出异常
	 * @return 工具的名称
	 */
	@Override
	public String getId() {
		return PLAN_UPDATE;
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
	 * @throws ToolException 工具执行异常
	 */
	@Override
	public ToolResult<Object> execute(AgentContextAble<?> context) {

		long start = System.currentTimeMillis();

		PlanUpdateTool.PlanUpdateToolDto structuralToolArgs = context
			.getStructuralToolArgs(PlanUpdateTool.PlanUpdateToolDto.class);

		// 1. 获取当前的 meta 数据
		Object metadataObj = context.getMetadata();
		if (metadataObj == null) {
			return ToolResult.error(ToolResult.ToolResultCode.TOOL_EXECUTION_ERROR,
					"Context metadata is null, cannot update plan", PLAN_UPDATE, -1L);
		}

		// 2. 确保是 ReActPlusAgentContextMeta 类型
		if (!(metadataObj instanceof ReActPlusAgentContextMeta meta)) {
			return ToolResult.error(ToolResult.ToolResultCode.TOOL_EXECUTION_ERROR,
					"Invalid metadata type, expected ReActPlusAgentContextMeta", PLAN_UPDATE, -1L);
		}

		// 3. 获取 TaskModeMeta
		TaskModeMeta taskModeMeta = meta.getTaskModeMeta();
		if (taskModeMeta == null) {
			return ToolResult.error(ToolResult.ToolResultCode.TOOL_EXECUTION_ERROR,
					"TaskModeMeta is null, cannot update plan", PLAN_UPDATE, -1L);
		}

		// 4. 获取当前任务阶段列表（创建可修改的副本）
		List<TaskModeMeta.TaskPhase> currentPhases = new ArrayList<>(taskModeMeta.getTaskPhaseList());

		// 5. 处理批量更新操作
		List<PlanUpdateOperation> operations = structuralToolArgs.getPlanUpdateOperations();
		if (operations == null || operations.isEmpty()) {
			log.warn("No update operations provided");
		}

		// 6. 对每个操作进行处理
		for (PlanUpdateOperation operation : operations) {
			String targetId = operation.getTargetId();
			PlanUpdateEndpoint endpoint = operation.getEndpoint();
			List<String> updateContent = operation.getUpdateContent();

			// 6.1 根据 targetId 查找目标阶段的索引位置
			int targetIndex = IntStream.range(0, currentPhases.size())
				.filter(i -> targetId.equals(currentPhases.get(i).getId()))
				.findFirst()
				.orElse(-1);

			// 检查目标是否存在
			if (targetIndex == -1) {
				return ToolResult.error(ToolResult.ToolResultCode.TOOL_EXECUTION_ERROR,
						"Target phase not found: " + targetId, PLAN_UPDATE, -1L);
			}

			// 生成新阶段列表（提前调用，减少重复代码）
			List<TaskModeMeta.TaskPhase> newPhases = createNewPhases(updateContent);
			// （可选）检查新阶段是否为空（根据业务需求）
			if (newPhases.isEmpty()) {
				return ToolResult.error(ToolResult.ToolResultCode.TOOL_EXECUTION_ERROR,
						"Update content is empty for target: " + targetId, PLAN_UPDATE, -1L);
			}

			// 6.2 根据 endpoint 类型执行不同的操作
			switch (endpoint) {
				case BEFORE:
					// 在目标之前插入新内容
					currentPhases.addAll(targetIndex, newPhases);
					break;

				case AFTER:
					// 在目标之后插入新内容（targetIndex+1 为插入位置）
					currentPhases.addAll(targetIndex + 1, newPhases);
					break;

				case SELF:
					// 替换目标本身（先删除再插入）
					currentPhases.remove(targetIndex);
					currentPhases.addAll(targetIndex, newPhases);
					break;

				default:
					return ToolResult.error(ToolResult.ToolResultCode.TOOL_EXECUTION_ERROR,
							"Invalid endpoint type: " + endpoint, PLAN_UPDATE, -1L);
			}
		}

		// 7. 重新计算所有阶段的 index（确保顺序正确）
		for (int i = 0; i < currentPhases.size(); i++) {
			currentPhases.get(i).setIndex(i);
		}

		// 8. 更新 taskModeMeta 的阶段列表
		taskModeMeta.setTaskPhaseList(currentPhases);

		// 9. 更新 context 的 metadata
		// 10. 更新 context 的 metadata
		context.setMetadata(meta);

		return ToolResult.ok(meta, System.currentTimeMillis() - start, getId());
	}

	/**
	 * 根据内容描述列表创建新的任务阶段
	 * @param descriptions 阶段描述列表（每个字符串同时作为 title 和 description）
	 * @return 新创建的任务阶段列表
	 */
	private List<TaskModeMeta.TaskPhase> createNewPhases(List<String> descriptions) {
		return descriptions.stream()
			.map(desc -> new TaskModeMeta.TaskPhase(com.ai.agent.real.common.utils.CommonUtils.generateUuidToken(), // 生成唯一
																													// ID
					desc, // title
					desc, // description（使用相同内容）
					0, // index 将在后续重新计算
					false, // 默认不并行
					TaskModeMeta.TaskStatus.TODO // 默认状态为 TODO
			))
			.toList();
	}

	@Data
	public static class PlanUpdateToolDto {

		/**
		 * 批量更新操作列表 包含多个独立的更新操作，每个操作可以针对不同的目标位置
		 */
		@ToolParam(required = true, description = "批量更新操作列表，每个元素代表一个独立的更新操作，支持同时对多个位置进行修改")
		private List<PlanUpdateOperation> planUpdateOperations;

	}

	@Data
	public static class PlanUpdateOperation {

		/**
		 * 目标计划ID 用于标识需要进行更新操作的目标计划条目
		 */
		@ToolParam(required = true, description = "目标计划的唯一标识符(id)，用于精确定位需要操作的计划条目")
		private String targetId;

		/**
		 * 更新切入点 指定新内容相对于目标条目的插入位置
		 */
		@ToolParam(required = true, description = "更新切入点类型：BEFORE(在目标条目之前插入)、AFTER(在目标条目之后插入)、SELF(替换目标条目本身)")
		private PlanUpdateEndpoint endpoint;

		@ToolParam(required = true, description = "更新的具体内容，根据endpoint类型决定是插入新内容还是替换原有内容")
		private List<String> updateContent;

	}

	/**
	 * 计划更新切入点枚举 定义了更新操作相对于目标条目的位置关系
	 */
	public enum PlanUpdateEndpoint {

		/**
		 * 在目标条目之前插入新内容
		 */
		BEFORE,
		/**
		 * 在目标条目之后插入新内容
		 */
		AFTER,
		/**
		 * 替换目标条目本身
		 */
		SELF

	}

}
