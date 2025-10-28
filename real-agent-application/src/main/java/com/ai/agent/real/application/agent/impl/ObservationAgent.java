package com.ai.agent.real.application.agent.impl;

import com.ai.agent.real.application.utils.AgentUtils;
import com.ai.agent.real.application.utils.FluxUtils;
import com.ai.agent.real.contract.agent.Agent;
import com.ai.agent.real.common.agent.context.ReActAgentContext;
import com.ai.agent.real.contract.model.property.*;
import com.ai.agent.real.contract.model.protocol.*;
import com.ai.agent.real.contract.model.protocol.AgentExecutionEvent.*;
import com.ai.agent.real.contract.service.*;
import lombok.*;
import lombok.extern.slf4j.*;
import org.springframework.ai.chat.model.*;
import org.springframework.ai.chat.prompt.*;
import reactor.core.publisher.*;

import java.util.*;

import static com.ai.agent.real.common.constant.NounConstants.*;

/**
 * 观察Agent - 负责ReAct框架中的观察(Observation)阶段 分析工具执行结果，总结执行效果，为下一轮思考提供输入
 *
 * @author han
 * @time 2025/9/9 03:00
 */
@Slf4j
public class ObservationAgent extends Agent {

	public static final String AGENT_ID = OBSERVATION_AGENT_ID;

	private final String SYSTEM_PROMPT = """
			## 角色定义
			你是一个超级智能体肯布罗伯特的观察模块，专注于分析工具执行结果并提供清晰的反馈。你的核心职责是接收完整的上下文信息（用户问题、思考过程、工具执行结果），进行有效分析，并以自然流畅的方式呈现观察结果，为后续的思考过程提供决策依据。

			## 核心能力
			1. **结果解析**：准确理解各种工具的执行输出，提取关键信息
			2. **上下文整合**：结合用户问题和思考过程，分析工具结果的相关性和有效性
			3. **信息提炼**：从复杂结果中提炼出与问题直接相关的关键信息
			4. **自然表达**：以自然语言流畅地呈现观察结果，避免技术术语堆砌

			## 工作流程
			1. **接收上下文**：获取完整的上下文信息（用户问题、思考过程、工具执行结果）
			2. **综合分析**：
			   - 理解用户的原始问题和需求
			   - 分析思考过程的逻辑和目标
			   - 验证工具执行结果的完整性和有效性
			   - 评估结果是否满足思考过程中设定的目标
			   - 识别关键信息、模式或异常
			3. **观察呈现**：
			   - 以自然语言描述观察到的结果
			   - 关联用户问题和思考过程进行分析
			   - 突出重要发现和关键数据
			   - 指出任何限制或需要进一步探索的问题
			   - 保持描述的客观性和准确性

			## 输出原则
			1. **自然流畅**：使用自然的口语化表达，避免机械感
			2. **简洁明了**：重点突出，避免冗余描述
			3. **信息完整**：确保所有重要信息都被涵盖
			4. **逻辑清晰**：按照合理的结构组织观察结果
			5. **上下文相关**：与用户问题和思考过程保持紧密关联

			## 重要注意事项
			1. **保持一致性**：确保观察结果与用户问题和思考过程保持逻辑一致
			2. **错误处理**：当工具执行失败时，清晰描述错误情况但避免技术细节过载
			3. **相关性优先**：优先呈现与用户问题直接相关的信息
			4. **简洁原则**：只包含与当前任务相关的重要信息，避免信息过载


			""";

	public ObservationAgent(ChatModel chatModel, ToolService toolService, ToolApprovalMode toolApprovalMode) {

		super(AGENT_ID, "ReActAgentStrategy-ObservationAgent",
				"负责ReAct框架中的观察(Observation)阶段，分析工具执行结果，总结执行效果，为下一轮思考提供输入", chatModel, toolService,
				Set.of("ReActAgentStrategy", "观察", "Observation"), toolApprovalMode);
		this.setCapabilities(new String[] { "ReActAgentStrategy", "观察", "Observation" });
	}

	@SneakyThrows
	@Override
	public Flux<AgentExecutionEvent> executeStream(String task, ReActAgentContext context) {
		log.debug("ObservationAgent开始流式观察分析: {}", task);

		// 构建观察提示
		// String observationPrompt = buildObservationPrompt(task, context);
		String observationPrompt = null;

		Prompt prompt = AgentUtils.buildPromptWithContextAndTools(null, context, SYSTEM_PROMPT, observationPrompt);

		// 使用通用的工具支持方法
		return FluxUtils
			.executeWithToolSupport(chatModel, prompt, context, AGENT_ID, toolService, toolApprovalMode,
					EventType.OBSERVING)

			.doOnComplete(() -> {
				afterHandle(context);
			})
			.doFinally(signalType -> {
				log.debug("ObservationAgent 执行结束，信号类型: {}", signalType);
			});
	}

	/**
	 * 构建观察提示词
	 */
	private String buildObservationPrompt(String task, ReActAgentContext context) {
		StringBuilder promptBuilder = new StringBuilder();

		promptBuilder.append("""
					请基于上下文，分析工具执行结果并提供观察反馈：
				""");

		return promptBuilder.toString();
	}

}
