package com.ai.agent.real.application.agent.item.reactplus;

import com.ai.agent.real.application.utils.AgentUtils;
import com.ai.agent.real.application.utils.FluxUtils;
import com.ai.agent.real.application.utils.PromptUtils;
import com.ai.agent.real.common.constant.NounConstants;
import com.ai.agent.real.contract.agent.Agent;
import com.ai.agent.real.contract.agent.context.AgentContextAble;
import com.ai.agent.real.contract.model.property.ToolApprovalMode;
import com.ai.agent.real.contract.model.protocol.AgentExecutionEvent;
import com.ai.agent.real.contract.tool.IToolService;
import com.ai.agent.real.entity.agent.context.reactplus.ReActPlusAgentContextMeta;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import reactor.core.publisher.Flux;

import java.util.Set;

import static com.ai.agent.real.common.constant.NounConstants.*;

/**
 * ActionPlusAgent - ReAct+ 框架中的增强行动 Agent 专注于智能工具选择、精准执行和结果优化，支持复杂任务的高效执行
 *
 * @author han
 * @time 2025/11/5 16:30
 */
@Slf4j
public class ActionPlusAgent extends Agent {

	public static final String AGENT_ID = ACTION_PLUS_AGENT_ID;

	private final String SYSTEM_PROMPT = """

			    ## 角色定义
			    你是一个超级智能体，名字叫做 han, 专注于工具执行, 严格按照前文、思维链 里布置的任务去执行 工具, 当没有工具执行时，且任务无法继续推进或任务已完成, 你直接调用任务结束工具, 来结束任务即可.
			    <首要准则>
			    你要基于上下文中的思考分析结果，选择最合适的工具并精准执行，重点关注执行效率和结果质量
			    </首要准则>## 输入信息
			    你将接收以下信息作为输入：
			    1. **用户原始问题**：用户最初提出的需求
			    2. 推理结论**：推理过程和决策依据
			    3. 执行结果**：工具实际执行的行动和输出
			    4. **历史交互记录**：之前的对话和操作历史



			    ## 智能工具选择
			    - **需求匹配**：根据任务需求精准匹配最合适的工具
			    - **工具组合**：合理组合多个工具以达到最佳执行效果
			    - **参数优化**：为工具调用设置最优参数配置
			    - **备选方案**：当主要工具不可用时快速切换到备选工具
			    ## 核心评估准则
			    任务完成的条件包括：
			    * ✅ 用户的问题已得到明确回答
			    * ✅ 用户的需求已被满足
			    * ✅ 所有必要的信息已被提供
			    * ✅ 用户没有进一步的问题或需求
			    * ✅✅✅ 最核心一点, 当上文给出明确指导要求结束任务, 无论任务是否完成, 都必须调用工具使得任务终止

			    ## 重要注意事项
			    1. **明确性**：要严格遵守 指导, 不要进行臆想和猜测, 严格按照上文需要你执行的工具执行
			    ## 核心能力与职责



			    <TOOLS>
			    可用工具集:
			    </TOOLS>

			    <ENVIRONMENTS>
			    环境变量:
			    </ENVIRONMENTS>


			""";

	public ActionPlusAgent(ChatModel chatModel, IToolService toolService, ToolApprovalMode toolApprovalMode) {

		super(AGENT_ID, AGENT_ID, "ReAct+ 框架中的增强行动执行者，专注于智能工具选择、精准执行和结果优化", chatModel, toolService,
				Set.of("ReActAgentStrategy", "行动", "Action", NounConstants.MCP, NounConstants.TASK_DONE),
				toolApprovalMode);
		this.setCapabilities(new String[] { "工具执行", "智能选择", "ActionPlus" });
	}

	/**
	 * 流式执行任务
	 * @param userInput user input
	 * @param context 执行上下文
	 * @return 流式执行结果
	 */
	@Override
	@SneakyThrows
	public Flux<AgentExecutionEvent> executeStream(String userInput, AgentContextAble context) {
		log.debug("ActionPlusAgent 开始智能工具执行: {}", userInput);

		// 构建行动提示
		// String actionPrompt = buildActionPrompt(context);

		// 比 ReAct 多一步 System Prompt 的 环境变量渲染
		String renderedSystemPrompt = PromptUtils.renderMeta(SYSTEM_PROMPT,
				(ReActPlusAgentContextMeta) context.getMetadata());
		Prompt prompt = AgentUtils.buildPromptWithContextAndTools(this.availableTools, context, renderedSystemPrompt,
				"请基于思考分析的结果，执行具体的行动：请选择合适的工具并执行相应的行动。");

		// // chat 参数配置
		// ChatOptions defaultOptions = chatModel.getDefaultOptions();
		// String model = defaultOptions.getModel();
		// ChatOptions customChatOptions =
		// ChatOptions.builder().model(model).topP(0.4).temperature(0.5).build();
		// prompt = AgentUtils.configurePromptOptions(prompt, customChatOptions);

		return FluxUtils
			.executeWithToolSupport(chatModel, prompt, context, AGENT_ID, toolService, toolApprovalMode,
					AgentExecutionEvent.EventType.ACTING)
			.doFinally(signalType -> {
				afterHandle(context);
				log.debug("ActionPlusAgent 工具执行结束，信号类型: {}", signalType);
			});
	}

}