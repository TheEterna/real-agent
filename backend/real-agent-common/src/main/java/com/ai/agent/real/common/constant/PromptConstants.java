package com.ai.agent.real.common.constant;

/**
 * @author han
 * @time 2025/9/9 14:34
 */

public class PromptConstants {

	// 任务未完成
	public static final String TASK_INCOMPLETE = "任务未完成：请继续完善解决方案，确保任务要求得到充分满足。";

	// In plan mode，planAdvance Tool prompt
	public static final String PROMPT_PLAN_MODE_PLAN_ADVANCE_TOOL = """
			- 当 局部任务/子任务/当前任务 结束时，你应该调用 plan_advance 工具来推动任务继续进行 \n
			""";

	// In plan mode，plaUpdate Tool prompt
	public static final String PROMPT_PLAN_MODE_PLAN_UPDATE_TOOL = """
			- 当遭遇信息变更，之前所制订计划与如今需要执行任务不符时，你需要调用 plan_update 工具来进行任务变更，注意： ** 该工具执行时，必须慎重，确保之前计划确实偏离需求、或偏离当前逻辑路线 ** \n
			""";

	// In plan mode，TaskDone Tool prompt
	public static final String PROMPT_PLAN_MODE_TASK_DONE_TOOL = """
			- 当你需要中断任务执行向用户展现阶段性的成果时，你需要调用 task_done 工具，这时可能任务已经结束，那就是正常结束，如果任务未结束就是中断任务，详细举例说明: 1. 全局任务结束时，包括是遭遇异常无法推进 和 顺利完成 \n 2. 遇到一些情况需要和用户交互时， 会调用 task_done 工具 向用户反馈，
			但是这个操作请谨慎执行，如果没有重要的信息或者用户提出某某操作需要确认的话，你应该谨慎在任务未执行完的时候调用该工具
			""";

	// gen title prompt
	public static final String PROMPT_GEN_TITLE = """
			- 你是一个AI助手，你需要根据用户输入的描述，生成一个简短的会话标题，请确保标题不超过15字，请勿添加引号或其他标点符号。
			""";

}
