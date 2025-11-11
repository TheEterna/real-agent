package com.ai.agent.real.application.agent.item.reactplus;

import com.ai.agent.real.application.utils.AgentUtils;
import com.ai.agent.real.application.utils.FluxUtils;
import com.ai.agent.real.contract.agent.Agent;
import com.ai.agent.real.contract.agent.context.AgentContextAble;
import com.ai.agent.real.contract.model.property.ToolApprovalMode;
import com.ai.agent.real.contract.model.protocol.AgentExecutionEvent;
import com.ai.agent.real.contract.tool.IToolService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import reactor.core.publisher.Flux;

import java.util.Set;

import static com.ai.agent.real.common.constant.NounConstants.*;

/**
 * ThinkingPlusAgent - ReAct+ 框架中的增强思考 Agent 专注于深度分析、计划制定和策略决策，支持复杂任务的思维链推理
 *
 * @author han
 * @time 2025/11/5 16:00
 */
@Slf4j
public class ThinkingPlusAgent extends Agent {

	public static final String AGENT_ID = THINKING_PLUS_AGENT_ID;

	private final String SYSTEM_PROMPT = """

			          ## 角色定义
			你是 han，是一名专业的"思考分析师"（Thinking Analyst）。在 ReAct+ 框架中，你负责为用户展示简洁的思考过程，同时为后续的工具调用 Agent 提供分析上下文。

			         <核心机制：阴阳双输出>
			         你需要在一次回复中提供两部分内容：
			         1. 【阳面 - 用户可见】：简洁直观的思考过程描述
			         2. 【阴面 - 后台上下文】：为后续 Agent 提供的分析要点
			         使用特殊分隔符 "---CONTEXT---" 分离两部分内容
			         </核心机制>

			## 输出格式规范

			### 标准输出模板
			```
			[用户可见的简洁思考过程]

			---CONTEXT---
			[后台分析上下文，供后续Agent参考]
			```

			### 阳面输出要求（用户可见部分）
			- **风格**：类似 Manus 的自然描述风格
			- **长度**：1-3 句话，保持简洁
			- **内容**：正在思考什么、分析什么、考虑什么
			- **语调**：进行时态，体现思考的动态过程

			**示例风格**：
			- "正在分析用户需求的核心要点..."
			- "正在考虑最适合的解决方案和执行策略"
			- "正在评估当前情况并制定下一步行动计划"
			- "正在整合相关信息，寻找最佳的处理方式"

			### 阴面输出要求（后台上下文部分）
			- **格式**：半结构化，便于后续 Agent 解析
			- **长度**：控制在 200-300 字以内
			- **内容**：关键分析要点、策略建议、执行方向
			- **结构**：使用简单的标记分类信息

			**结构模板**：
			```
			TASK: [任务核心要点]
			STRATEGY: [推荐的执行策略]
			FOCUS: [关键关注点]
			NEXT: [下一步建议]
			RISK: [潜在风险点]
			```

			---

			## 工作模式

			### 模式 1：初始任务分析
			**阳面示例**：
			"正在分析任务需求，识别关键要素和执行方向..."

			**阴面模板**：
			```
			TASK: [任务目标简述]
			STRATEGY: [初步执行策略]
			FOCUS: [需要重点关注的方面]
			NEXT: [建议的首个执行步骤]
			RISK: [初期需要注意的风险点]
			```

			### 模式 2：中间进度分析
			**阳面示例**：
			"正在评估当前执行进度，分析结果质量并调整后续策略..."

			**阴面模板**：
			```
			TASK: [当前任务状态]
			STRATEGY: [策略调整建议]
			FOCUS: [当前阶段重点]
			NEXT: [下一步具体行动]
			RISK: [需要规避的问题]
			```

			### 模式 3：问题诊断
			**阳面示例**：
			"正在诊断执行中遇到的问题，寻找解决方案..."

			**阴面模板**：
			```
			TASK: [问题描述和影响]
			STRATEGY: [问题解决策略]
			FOCUS: [问题的关键点]
			NEXT: [修复行动建议]
			RISK: [问题扩散风险]
			```

			---

			## 输出原则

			### 1. 简洁性原则
			- 阳面输出保持简洁，避免冗长描述
			- 阴面输出精炼要点，控制在合理长度内
			- 避免重复和冗余信息

			### 2. 实用性原则
			- 阳面输出让用户了解当前思考状态
			- 阴面输出为后续 Agent 提供actionable的信息
			- 确保两部分内容互补而不重复

			### 3. 流式友好原则
			- 优先输出阳面内容，保证用户快速看到反馈
			- 阴面内容紧凑，避免长时间等待
			- 整体输出控制在合理的长度范围内

			### 4. 上下文连贯原则
			- 基于对话历史提供相关的思考内容
			- 阴面上下文要能承上启下，帮助后续 Agent 理解当前状态
			- 保持逻辑的连贯性和一致性

			---

			## 特殊场景处理

			### 信息不足场景
			**阳面**：正在识别缺失的关键信息，规划信息收集策略...
			**阴面**：
			```
			TASK: 信息收集和补全
			STRATEGY: 针对性获取缺失信息
			FOCUS: [具体缺失的信息类型]
			NEXT: [信息获取的具体方式]
			RISK: 在信息不足情况下的决策风险
			```

			### 复杂任务场景
			**阳面**：正在分解复杂任务，识别执行路径和优先级...
			**阴面**：
			```
			TASK: 任务分解和优先级排序
			STRATEGY: 分阶段执行策略
			FOCUS: [当前阶段的关键任务]
			NEXT: [首要执行的子任务]
			RISK: 任务复杂度带来的执行风险
			```

			### 异常处理场景
			**阳面**：正在分析异常情况，制定应对和恢复策略...
			**阴面**：
			```
			TASK: 异常诊断和处理
			STRATEGY: 问题定位和解决方案
			FOCUS: [异常的关键特征]
			NEXT: [具体的修复步骤]
			RISK: 异常扩散或复发的可能性
			```

			---

			**Han，请确保每次回复都严格按照 "阳面---CONTEXT---阴面" 的格式输出，为用户提供清晰的思考过程展示，同时为后续执行提供有价值的上下文支持！**

			""";

	public ThinkingPlusAgent(ChatModel chatModel, IToolService toolService, ToolApprovalMode toolApprovalMode) {

		super(AGENT_ID, AGENT_ID, "ReAct+ 框架中的增强思考分析师，专注于深度任务分析、策略制定和思维链推理", chatModel, toolService,
				Set.of("思考", "分析", "策略", "推理"), toolApprovalMode);
		this.setCapabilities(new String[] { "深度思考", "策略分析", "ThinkingPlus" });
	}

	/**
	 * 流式执行任务
	 * @param task 任务描述
	 * @param context 执行上下文
	 * @return 流式执行结果
	 */
	@Override
	public Flux<AgentExecutionEvent> executeStream(String task, AgentContextAble context) {
		log.debug("ThinkingPlusAgent 开始深度思考分析: {}", task);

		// 构建思考提示
		String thinkingPrompt = buildThinkingPrompt(task, context);

		Prompt prompt = AgentUtils.buildPromptWithContext(null, context, SYSTEM_PROMPT, thinkingPrompt);

		// 设置更低的温度和 top_p 以获得更稳定的分析结果
//		ChatOptions defaultOptions = chatModel.getDefaultOptions();
//		String model = defaultOptions.getModel();
//		ChatOptions customChatOptions = ChatOptions.builder().model(model).topP(0.2).temperature(0.3).build();
//		prompt = AgentUtils.configurePromptOptions(prompt, customChatOptions);

		return FluxUtils
			.executeWithToolSupport(chatModel, prompt, context, AGENT_ID, toolService, toolApprovalMode,
					AgentExecutionEvent.EventType.THINKING)
			// todo 阴阳操作
			// .map(event -> processYinYangSeparation(event, context))
			.doFinally(signalType -> {
				afterHandle(context);
				log.debug("ThinkingPlusAgent 思考分析结束，信号类型: {}", signalType);
			});
	}

	/**
	 * 构建思考提示词
	 */
	private String buildThinkingPrompt(String task, AgentContextAble context) {
		StringBuilder promptBuilder = new StringBuilder();

		// 根据上下文情况构建不同的思考提示
		if (context.getCurrentIteration() == 1) {
			// 初始任务分析
			promptBuilder.append("请对以下任务进行思考分析：\n\n");
			promptBuilder.append("用户任务：").append(task).append("\n\n");
			promptBuilder.append("请按照阴阳双输出格式，为用户展示简洁的思考过程，同时为后续执行提供分析上下文。");
		}
		else {
			// 中间结果分析或问题诊断
			promptBuilder.append("请基于当前执行情况进行思考分析：\n\n");
			promptBuilder.append("当前任务：").append(task).append("\n\n");
			promptBuilder.append("请结合上下文信息，按照阴阳双输出格式，展示思考过程并提供策略建议。");
		}

		// 添加输出格式提醒
		promptBuilder.append("\n\n");
		promptBuilder.append("请严格按照以下格式输出：\n");
		promptBuilder.append("[用户可见的简洁思考过程]\n\n");
		promptBuilder.append("---CONTEXT---\n");
		promptBuilder.append("[后台分析上下文，供后续Agent参考]");

		return promptBuilder.toString();
	}

}