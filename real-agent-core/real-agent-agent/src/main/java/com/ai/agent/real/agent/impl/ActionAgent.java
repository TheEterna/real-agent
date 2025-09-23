package com.ai.agent.real.agent.impl;

import com.ai.agent.real.common.constant.*;
import com.ai.agent.real.contract.protocol.*;
import com.ai.agent.real.contract.service.*;
import com.ai.agent.real.contract.spec.*;
import com.ai.agent.real.agent.Agent;

import com.ai.agent.real.common.utils.*;
import com.ai.agent.real.contract.spec.AgentExecutionEvent.*;
import com.ai.agent.real.contract.spec.message.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.*;
import org.springframework.ai.chat.messages.AssistantMessage.*;
import org.springframework.ai.chat.model.*;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.*;
import org.springframework.ai.model.tool.*;
import org.springframework.ai.support.*;
import reactor.core.publisher.*;
import reactor.core.scheduler.Schedulers;

import java.util.*;

import com.fasterxml.jackson.databind.ObjectMapper;

import static com.ai.agent.real.common.constant.NounConstants.ACTION_AGENT_ID;

/**
 * 行动Agent - 负责ReAct框架中的行动(Acting)阶段
 * 基于思考结果执行具体的工具调用和操作
 *
 * @author han
 * @time 2025/9/9 02:55
 */
@Slf4j
public class ActionAgent extends Agent {

    public static final String AGENT_ID = ACTION_AGENT_ID;


    private final String SYSTEM_PROMPT = """
                 # ReAct框架 - actionAgent 核心提示词
                 ## 一、角色与核心目标
                 你是 ReAct 框架的 **行动执行代理（actionAgent）**，核心职责是：承接上游推理代理（reasonAgent）输出的“推理结论”，结合历史上下文与可用工具库，生成 **唯一、可执行、目标导向** 的行动指令。你的行动必须直接服务于“解决用户原始问题”，且为后续“观察阶段（Observation）”提供明确的信息获取/操作依据，不可脱离推理逻辑凭空行动。
                 
                 ## 二、你必须接收的上下文输入
                 在生成行动前，你需先确认以下输入信息是否完整；若缺失，需先向用户/上游代理请求补充：
                 1. **用户原始问题**：需解决的核心需求（如“分析2024年中国新能源汽车出口量同比增速，并对比欧洲市场占比”）；
                 2. **reasonAgent 推理结论**：上游推理给出的“行动必要性说明”（如“当前缺少2024年中国新能源汽车出口量官方数据，需先通过权威数据源获取该数据；同时需获取欧洲市场同期进口量数据以计算占比”）；
                 3. **历史行动-观察记录**：已执行的行动及对应的观察结果（如“[行动1：调用‘国家统计局API’获取2024年新能源汽车出口量] → [观察1：API返回‘2024年出口量520万辆’]”）；
                 4. **可用工具列表**：当前可调用的工具及参数规范（需包含“工具名称、功能描述、必填参数、可选参数、调用格式”，示例：`工具1：【权威数据搜索】- 功能：获取公开权威数据源（如国家统计局、WTO）数据；必填参数：关键词（string）、数据时间范围（start_date/end_date）、数据源类型（enum: 官方/行业报告）；调用格式：{"tool":"权威数据搜索","params":{"keyword":"","start_date":"","end_date":"","data_source":""}}`）。
                 
                 
                 ## 三、核心行动准则（不可违背）
                 1. **行动与推理强绑定**：你的所有行动必须严格匹配 reasonAgent 推理结论中的“行动必要性”——若推理结论明确“需获取A数据”，不可行动为“获取B数据”；若推理结论未提及某工具，不可主动调用该工具。
                 2. **工具调用精准性**：
                    - 工具选择：仅从“可用工具列表”中选择，不可调用未声明的工具；若推理结论指向的工具不在列表中，需输出“当前工具库缺少【目标工具】，无法执行该行动，请补充该工具或调整推理方向”；
                    - 参数完整性：调用工具时必须填写所有“必填参数”，可选参数需根据用户问题优先级补充（如问题涉及“2024年”，需明确时间范围参数，不可留空）；若参数缺失（如用户未提供“具体城市”），需输出“需补充参数：【参数名称】（说明：该参数用于定位XX信息，例：北京）”；
                    - 格式合规性：严格按照“可用工具列表”指定的格式生成行动指令（如JSON、特定关键词包裹），不可出现格式错误（如遗漏引号、参数类型不匹配）。
                 3. **行动目标唯一性**：单次行动仅解决“推理结论中的一个子目标”——若推理结论需“获取A数据+分析A数据”，不可一次行动同时执行，需先执行“获取A数据”，待观察到A数据后，再由reasonAgent判断是否执行“分析A数据”。
                 4. **异常预处理**：
                    - 若工具调用可能存在限制（如API调用次数上限、数据源访问权限），需在行动指令后补充“风险提示：该工具可能存在XX限制，若执行失败请尝试XX替代方案（如切换数据源）”；
                    - 若历史行动中某工具已调用失败（如“搜索工具未找到2024年数据”），不可重复调用相同工具执行相同操作，需输出“历史行动显示【工具名称】执行【操作】失败，建议调整：【替代方案，如更换关键词为‘2024中国新能源汽车出口量 海关总署’】”。
                 5. **行动可验证性**：你的行动指令需明确“如何判断行动成功”——即行动后应获取的“观察结果类型”（如“调用‘权威数据搜索’后，需观察是否返回‘2024年中国新能源汽车出口量的具体数值（单位：万辆）及同比增长率’”），为后续观察阶段提供验证标准。
                 
                
                 
                 ### 2. 特殊场景格式
                 - **参数缺失请求**：当需补充用户信息时，格式为：`{"action_id":"A001","action_type":"参数请求","missing_param":"具体参数名称（如：欧洲市场的具体国家范围）","param_explanation":"该参数的作用（如：需明确是欧盟27国还是包含英国、俄罗斯等）","example":"示例值（如：欧盟27国）"}`
                 - **工具不可用提示**：当推理结论指向的工具未在列表中时，格式为：`{"action_id":"A001","action_type":"工具缺失提示","required_tool":"推理所需工具（如：欧洲汽车工业协会API）","reason":"该工具的必要性（如：需通过该API获取欧洲市场新能源汽车进口量数据）","suggestion":"解决方案（如：1. 补充该工具到工具库；2. 改用‘欧洲统计局搜索工具’替代）"}`
                 
               
                 
                 
                 ## 四、优化指引（持续提升行动有效性）
                 1. **工具优先级**：若多个工具可实现同一目标（如“权威数据搜索”和“海关总署API”均可获取出口数据），优先选择“参数更少、响应更快、数据更权威”的工具；
                 2. **参数精简**：可选参数仅补充“对结果准确性有影响”的内容（如获取“全国出口量”时，无需补充“城市”参数），避免冗余；
                 3. **上下文复用**：若历史观察中已包含部分参数（如用户此前提到“关注欧盟市场”），直接复用该参数，无需重复请求；
                 4. **失败复盘**：若行动执行失败（如工具返回“无数据”），需在下次行动中调整“关键词、数据源、时间范围”等参数（如将关键词从“2024年出口量”改为“2024年新能源汽车出口数据 海关总署”）。
                 
                 使用时，可根据实际“可用工具列表”调整`tool_params`字段的参数名，并根据用户场景（如技术开发、数据分析、日常问答）优化`action_goal`和`expected_observation`的表述颗粒度。
            """;


    /**
     * 构造函数
     */
    public ActionAgent(ChatModel chatModel,
                       ToolService toolService,
                       ToolApprovalMode toolApprovalMode) {

        super(AGENT_ID,
                "ReActAgentStrategy-ActionAgent",
                "负责ReAct框架中的行动(Acting)阶段，执行思考阶段的行动指令",
                chatModel,
                toolService,
                Set.of("ReActAgentStrategy", "行动", "Action", NounConstants.MCP),
                toolApprovalMode);
        this.setCapabilities(new String[]{"ReActAgentStrategy", "行动", "Action", NounConstants.MCP});
    }


    @Override
    public Flux<AgentExecutionEvent> executeStream(String task, AgentContext context) {
        try {

            log.info("ActionAgent开始流式执行行动: {}", task);

            // 构建行动提示
            String actionPrompt = buildActionPrompt(task);


            Prompt prompt = AgentUtils.buildPromptWithContextAndTools(
                    this.availableTools,
                    context,
                    SYSTEM_PROMPT,
                    actionPrompt
            );

            return FluxUtils.executeWithToolSupport(
                    chatModel,
                    prompt,
                    context,
                    AGENT_ID,
                    toolService,
                    toolApprovalMode,
                    EventType.ACTING
            )
                    .doOnNext(content -> log.debug("ActionAgent流式输出: {}", content))
                    .doOnError(e -> log.error("ActionAgent流式执行异常", e))
                    .onErrorResume(e -> {
                        log.error("ActionAgent流式执行异常", e);
                        return Flux.just(AgentExecutionEvent.error("ActionAgent流式执行异常: " + e.getMessage()));
                    })
                    .doOnComplete(() -> {
                        afterHandle(context);
                    })
                    .doFinally(signalType -> {
                        log.debug("ActionAgent流式执行结束，信号类型: {}", signalType);
                    });
        } catch (Exception e) {
            log.error("ActionAgent流式执行异常", e);
            return Flux.error(e);
        }
    }

    /**
     * 构建行动提示词
     */
    private String buildActionPrompt(String task) {

        StringBuilder promptBuilder = new StringBuilder();


        promptBuilder.append("请基于思考分析的结果，执行具体的行动：\n\n");
        promptBuilder.append("原始任务: ").append(task).append("\n\n");


        // 添加可用工具信息
        if (availableTools != null && !availableTools.isEmpty()) {
            promptBuilder.append("可用工具:\n");
            for (AgentTool tool : availableTools) {
                promptBuilder.append("- ").append(tool.getSpec().getName())
                        .append(": ").append(tool.getSpec().getDescription()).append("\n");
            }
            promptBuilder.append("\n");
        }

        promptBuilder.append("请选择合适的工具并执行相应的行动。");

        return promptBuilder.toString();
    }



    private static String argsPreview(Map<String, Object> args, int max) {
        try {
            String s = new ObjectMapper().writeValueAsString(args != null ? args : Collections.emptyMap());
            if (s.length() > max) {
                return s.substring(0, max) + "...";
            }
            return s;
        } catch (Exception e) {
            return "{}";
        }
    }


    @Override
    public AgentResult execute(String task, AgentContext context) {
        try {
            log.debug("ActionAgent开始执行行动: {}", task);

            // 构建行动提示
            String actionPrompt = buildActionPrompt(task);

            // 配置工具调用选项
            var options = DefaultToolCallingChatOptions.builder()
                    .toolCallbacks(ToolCallbacks.from(availableTools.toArray()))
                    .build();

            // 构建消息
            List<Message> messages = List.of(
                    new SystemMessage(SYSTEM_PROMPT),
                    new UserMessage(actionPrompt)
            );

            // 调用LLM执行行动
            var response = chatModel.call(new Prompt(messages, options));
            String action = response.getResult().getOutput().getText();

            log.debug("ActionAgent执行结果: {}", action);

            return AgentResult.success(action, AGENT_ID);

        } catch (Exception e) {
            log.error("ActionAgent执行异常", e);
            return AgentResult.failure("行动执行出现异常: " + e.getMessage(), AGENT_ID);
        }

    }


}
