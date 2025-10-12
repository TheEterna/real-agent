package com.ai.agent.real.agent.impl;

import com.ai.agent.real.contract.model.agent.*;
import com.ai.agent.real.contract.model.context.*;
import com.ai.agent.real.contract.model.protocol.*;
import com.ai.agent.real.contract.service.*;
import lombok.extern.slf4j.*;
import org.springframework.ai.chat.model.*;
import reactor.core.publisher.*;

import java.util.*;

/**
 * 通用Agent 处理各种通用任务，作为兜底Agent使用
 *
 * @author han
 * @time 2025/9/7 00:25
 */
@Slf4j
public class GeneralPurposeAgent extends Agent {

	public final static String AGENT_ID = "general-purpose-agent";

	private final String SYSTEM_PROMPT = """
			你是一个通用的AI助手，能够处理各种类型的任务。
			你具备以下能力：
			1. 问题解答和知识咨询
			2. 文本分析和处理
			3. 逻辑推理和问题解决
			4. 创意思考和建议提供
			5. 任务规划和执行指导
			6. 分析任务并决定是否需要工具

			请根据用户的具体需求，提供准确、有用的回答和建议。
			""";

	public GeneralPurposeAgent(ChatModel chatModel, ToolService toolService) {
		super("general-purpose-agent", "通用助手", "处理各种通用任务的万能Agent", chatModel, toolService,
				Set.of("通用", "问答", "文本", "处理", "逻辑", "推理", "创意思考", "任务", "规划"));
		this.setCapabilities(new String[] { "通用问答", "文本处理", "逻辑推理", "创意思考", "任务规划" });
	}

	/**
	 * 流式执行任务
	 * @param task 任务描述
	 * @param context 执行上下文
	 * @return 流式执行结果
	 */
	@Override
	public Flux<AgentExecutionEvent> executeStream(String task, AgentContext context) {
		return null;
	}

}
