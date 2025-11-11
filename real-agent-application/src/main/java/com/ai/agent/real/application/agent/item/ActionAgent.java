package com.ai.agent.real.application.agent.item;

import com.ai.agent.real.application.utils.AgentUtils;
import com.ai.agent.real.application.utils.FluxUtils;
import com.ai.agent.real.common.constant.*;
import com.ai.agent.real.contract.agent.Agent;
import com.ai.agent.real.contract.agent.context.AgentContextAble;
import com.ai.agent.real.contract.model.property.*;
import com.ai.agent.real.contract.model.protocol.*;
import com.ai.agent.real.contract.model.protocol.AgentExecutionEvent.*;
import com.ai.agent.real.contract.tool.IToolService;
import lombok.extern.slf4j.*;
import org.springframework.ai.chat.model.*;
import org.springframework.ai.chat.prompt.*;
import reactor.core.publisher.*;

import java.util.*;

import static com.ai.agent.real.common.constant.NounConstants.*;

/**
 * 行动Agent - 负责ReAct框架中的行动(Acting)阶段 基于思考结果执行具体的工具调用和操作
 *
 * @author han
 * @time 2025/9/9 02:55
 */
@Slf4j
public class ActionAgent extends Agent {

	public static final String AGENT_ID = ACTION_AGENT_ID;

	private final String SYSTEM_PROMPT = """
			## 角色定义
				你是一个超级智能体，名字叫做 han, 专注于工具执行, 严格按照前文、思维链 里布置的任务去执行 工具, 当没有工具执行时，且任务无法继续推进或任务已完成, 你直接调用任务结束工具, 来结束任务即可.
				## 输入信息
				你将接收以下信息作为输入：
				1. **用户原始问题**：用户最初提出的需求
				2. 推理结论**：推理过程和决策依据
				3. 执行结果**：工具实际执行的行动和输出
				4. **历史交互记录**：之前的对话和操作历史

				## 核心评估准则
				任务完成的条件包括：
				* ✅ 用户的问题已得到明确回答
				* ✅ 用户的需求已被满足
				* ✅ 所有必要的信息已被提供
				* ✅ 用户没有进一步的问题或需求
				* ✅✅✅ 最核心一点, 当上文给出明确指导要求结束任务, 无论任务是否完成, 都必须调用工具使得任务终止

				## 重要注意事项
				1. **明确性**：要严格遵守 指导, 不要进行臆想和猜测, 严格按照上文需要你执行的工具执行

				## 环境变量

				<TOOLS>
				可用工具集：
				</TOOLS>

				""";

	/**
	 * 构造函数
	 */
	public ActionAgent(ChatModel chatModel, IToolService toolService, ToolApprovalMode toolApprovalMode) {

		super(AGENT_ID, "ReActAgentStrategy-ActionAgent", "负责ReAct框架中的行动(Acting)阶段，执行思考阶段的行动指令", chatModel, toolService,
				Set.of("ReActAgentStrategy", "行动", "Action", NounConstants.MCP, NounConstants.TASK_DONE),
				toolApprovalMode);
		this.setCapabilities(
				new String[] { "ReActAgentStrategy", "行动", "Action", NounConstants.MCP, NounConstants.TASK_DONE });
		this.setSystemPrompt(SYSTEM_PROMPT);
	}

	@Override
	public Flux<AgentExecutionEvent> executeStream(String task, AgentContextAble context) {
		try {

			log.info("ActionAgent开始流式执行行动: {}", task);

			// 构建行动提示
			String actionPrompt = buildActionPrompt(task);

			Prompt prompt = AgentUtils.buildPromptWithContextAndTools(this.availableTools, context, SYSTEM_PROMPT,
					actionPrompt);

			return FluxUtils
				.executeWithToolSupport(chatModel, prompt, context, AGENT_ID, toolService, toolApprovalMode,
						EventType.ACTING)
				.doFinally(signalType -> {
					afterHandle(context);
					log.debug("ActionAgent流式执行结束，信号类型: {}", signalType);
				});
		}
		catch (Exception e) {
			log.error("ActionAgent流式执行异常", e);
			return Flux.error(e);
		}
	}

	/**
	 * 构建行动提示词 TODO user prompt 需要修改
	 */
	private String buildActionPrompt(String task) {

		StringBuilder promptBuilder = new StringBuilder();

		promptBuilder.append("请基于思考分析的结果，执行具体的行动：\n\n");
		promptBuilder.append("请选择合适的工具并执行相应的行动。");

		return promptBuilder.toString();
	}

}
