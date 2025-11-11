package com.ai.agent.real.application.utils;

import com.ai.agent.real.contract.tool.AgentTool;
import com.ai.agent.real.entity.agent.context.reactplus.AgentMode;
import com.ai.agent.real.entity.agent.context.reactplus.ReActPlusAgentContextMeta;
import com.ai.agent.real.entity.agent.context.reactplus.TaskModeMeta;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

import static com.ai.agent.real.common.constant.NounConstants.ENVIRONMENTS_TAG;
import static com.ai.agent.real.common.constant.NounConstants.TAG_TOOLS;

/**
 * @author han
 * @time 2025/10/9 12:50
 */

@Slf4j
public class PromptUtils {

	/**
	 * 在指定标签里增加内容
	 * @param prompt rendered prompt
	 * @param tag Designated tags
	 * @param content The content to be added
	 * @return
	 */
	public static String addContentInTag(String prompt, String tag, String content) {
		// 1. 组装tag
		String startTag = "<" + tag + ">";
		String endTag = "</" + tag + ">";

		// 2. 获取标签的起始索引
		int startIdx = prompt.indexOf(startTag);

		// 3. 获取标签的结束索引
		int endIdx = prompt.indexOf(endTag, startIdx + startTag.length());
		if (startIdx == -1 || endIdx == -1) {
			// 如果找不到标签，直接返回原提示词
			log.warn("not to find the {} tag", tag);
			return prompt + "\n" + startTag + content + endTag;
		}
		// extract original content between tags
		String originalContent = prompt.substring(startIdx + startTag.length(), endIdx);

		// combine new content with tools content
		String newContent = originalContent + "\n" + content;

		// 3. 替换标签内容
		return prompt.substring(0, startIdx + startTag.length()) + newContent + prompt.substring(endIdx);
	}

	/**
	 * @param prompt rendered prompt
	 * @param availableTools avble tools
	 * @return
	 */
	public static String renderToolList(String prompt, List<AgentTool> availableTools) {
		// 1. if available tools is empty, return original system prompt directly
		if (availableTools == null || availableTools.isEmpty()) {
			return prompt;
		}

		// 2. 构建工具列表字符串
		StringBuilder stringBuilder = new StringBuilder();

		for (int i = 0; i < availableTools.size(); i++) {
			AgentTool tool = availableTools.get(i);
			// 3. 构建工具列表字符串
			stringBuilder.append(i + 1)
				.append(". ")
				.append(tool.getSpec().getName())
				.append(": ")
				.append(tool.getSpec().getDescription())
				.append("\n")
				.append("参数: \n")
				.append(tool.getSpec().getInputSchema())
				.append("\n");
		}
		String toolsContent = stringBuilder.toString().strip();

		// 3. handle <Tools>标签内容
		return addContentInTag(prompt, TAG_TOOLS, toolsContent);
	}

	/**
	 * 渲染 meta 到 environments，根据 agentMode 不同使用 不同的渲染逻辑
	 * @param meta ReActPlus AgentContextMeta
	 * @return
	 */
	public static String renderMeta(String prompt, ReActPlusAgentContextMeta meta) {
		// 1. pre handle and prepare data
		if (meta == null) {
			return "";
		}

		AgentMode agentMode = meta.getAgentMode();
		TaskModeMeta taskModeMeta = meta.getTaskModeMeta();
		String note = meta.getNote();
		String realTask = meta.getRealTask();

		StringBuilder envBuilder = new StringBuilder();
		envBuilder.append("## 执行环境信息\n\n");

		// 添加基础信息
		if (realTask != null && !realTask.trim().isEmpty()) {
			envBuilder.append("**核心任务**: ").append(realTask).append("\n\n");
		}

		if (note != null && !note.trim().isEmpty()) {
			envBuilder.append("**任务备注**: ").append(note).append("\n\n");
		}

		// 2. build data: 根据不同的 AgentMode 渲染不同的环境信息
		switch (agentMode) {
			case DIRECT:
				envBuilder.append(renderDirectModeEnv(taskModeMeta));
				break;
			case SIMPLE:
				envBuilder.append(renderSimpleModeEnv(taskModeMeta));
				break;
			case PLAN:
				envBuilder.append(renderPlanModeEnv(taskModeMeta));
				break;
			case THOUGHT:
				envBuilder.append(renderThoughtModeEnv(taskModeMeta));
				break;
			case SOP:
				envBuilder.append(renderSopModeEnv(taskModeMeta));
				break;
			case PLAN_THOUGHT:
				envBuilder.append(renderPlanThoughtModeEnv(taskModeMeta));
				break;
			default:
				log.warn("Unknown AgentMode: {}", agentMode);
				break;
		}
		// 3. handle <Tools>标签内容
		return addContentInTag(prompt, ENVIRONMENTS_TAG, envBuilder.toString());
	}

	/**
	 * 渲染计划模式的环境信息
	 */
	private static String renderPlanModeEnv(TaskModeMeta taskModeMeta) {
		if (taskModeMeta == null) {
			return "**执行模式**: 计划模式 (PLAN)\n\n";
		}

		StringBuilder builder = new StringBuilder();
		builder.append("**执行模式**: 计划模式 (PLAN)\n");
		builder.append("**项目目标**: ").append(taskModeMeta.getGoal()).append("\n");

		if (taskModeMeta.getCurrentTaskId() != null) {
			builder.append("**当前阶段**: ").append(taskModeMeta.getCurrentTaskId()).append("\n");
		}

		// 渲染任务阶段列表
		if (taskModeMeta.getTaskPhaseList() != null && !taskModeMeta.getTaskPhaseList().isEmpty()) {
			builder.append("\n### 📋 执行计划\n");
			for (TaskModeMeta.TaskPhase phase : taskModeMeta.getTaskPhaseList()) {
				String statusIcon = getTaskStatusIcon(phase.getTaskStatus());
				builder.append("**")
					.append(phase.getIndex())
					.append(". ")
					.append(phase.getTitle())
					.append("** ")
					.append(statusIcon)
					.append("\n");
				builder.append("   - 描述: ").append(phase.getDescription()).append("\n");
				builder.append("   - 并行执行: ").append(phase.isParallel() ? "是" : "否").append("\n");
				builder.append("   - 状态: ").append(getTaskStatusText(phase.getTaskStatus())).append("\n\n");
			}
		}

		return builder.toString();
	}

	/**
	 * 渲染思考模式的环境信息
	 */
	private static String renderThoughtModeEnv(TaskModeMeta taskModeMeta) {
		StringBuilder builder = new StringBuilder();
		builder.append("**执行模式**: 思考模式 (THOUGHT)\n");
		builder.append("**执行特点**: 深度思维链推理，专注于复杂问题的分析和策略制定\n\n");

		if (taskModeMeta != null && taskModeMeta.getGoal() != null) {
			builder.append("**思考目标**: ").append(taskModeMeta.getGoal()).append("\n\n");
		}

		return builder.toString();
	}

	/**
	 * 渲染SOP模式的环境信息
	 */
	private static String renderSopModeEnv(TaskModeMeta taskModeMeta) {
		StringBuilder builder = new StringBuilder();
		builder.append("**执行模式**: 标准作业程序模式 (SOP)\n");
		builder.append("**执行特点**: 按照预定义的标准流程执行，确保操作的一致性和规范性\n\n");

		if (taskModeMeta != null && taskModeMeta.getGoal() != null) {
			builder.append("**SOP目标**: ").append(taskModeMeta.getGoal()).append("\n\n");
		}

		return builder.toString();
	}

	/**
	 * 渲染计划-思考混合模式的环境信息
	 */
	private static String renderPlanThoughtModeEnv(TaskModeMeta taskModeMeta) {
		StringBuilder builder = new StringBuilder();
		builder.append("**执行模式**: 计划-思考混合模式 (PLAN_THOUGHT)\n");
		builder.append("**执行特点**: 结合计划执行和深度思考，在执行计划的同时进行动态思维链推理\n\n");

		if (taskModeMeta != null && taskModeMeta.getGoal() != null) {
			builder.append("**混合目标**: ").append(taskModeMeta.getGoal()).append("\n\n");
		}

		return builder.toString();
	}

	/**
	 * 渲染简单模式的环境信息
	 */
	private static String renderSimpleModeEnv(TaskModeMeta taskModeMeta) {
		StringBuilder builder = new StringBuilder();
		builder.append("**执行模式**: 简单模式 (SIMPLE)\n");
		builder.append("**执行特点**: 直接执行，适用于简单明确的任务\n\n");

		return builder.toString();
	}

	/**
	 * 渲染简单模式的环境信息
	 */
	private static String renderDirectModeEnv(TaskModeMeta taskModeMeta) {
		StringBuilder builder = new StringBuilder();
		builder.append("根据用户需求进行态度友好的解答");

		return builder.toString();
	}

	/**
	 * 获取任务状态对应的图标
	 */
	private static String getTaskStatusIcon(TaskModeMeta.TaskStatus status) {
		if (status == null) {
			return "⚪";
		}
		return switch (status) {
			case TODO -> "⚪";
			case RUNNING -> "🔄";
			case DONE -> "✅";
			case FAILED -> "❌";
		};
	}

	/**
	 * 获取任务状态对应的文本
	 */
	private static String getTaskStatusText(TaskModeMeta.TaskStatus status) {
		if (status == null) {
			return "未知";
		}
		return switch (status) {
			case TODO -> "待执行";
			case RUNNING -> "执行中";
			case DONE -> "已完成";
			case FAILED -> "已失败";
		};
	}

}
