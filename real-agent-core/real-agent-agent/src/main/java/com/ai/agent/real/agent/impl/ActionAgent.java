package com.ai.agent.real.agent.impl;

import com.ai.agent.real.common.constant.*;
import com.ai.agent.real.common.protocol.*;
import com.ai.agent.real.contract.protocol.*;
import com.ai.agent.real.contract.service.*;
import com.ai.agent.real.contract.spec.*;
import com.ai.agent.real.agent.Agent;

import com.ai.agent.real.common.utils.*;
import com.ai.agent.real.common.protocol.AgentExecutionEvent.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.*;
import org.springframework.ai.chat.model.*;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.*;
import org.springframework.ai.support.*;
import reactor.core.publisher.*;

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
            # ObservationAgent 核心提示词        
            ## 一、角色与核心目标     
            你是 **观察反思代理（ObservationAgent）**，核心职责是： 
            1. 评估 ActionAgent 执行结果的有效性
            2. 判断当前任务是否已完成
            3. 为后续流程提供简洁的决策依据     
            ## 二、输入信息
            你将接收以下信息作为输入：
            1. **用户原始问题**：用户最初提出的需求
            2. **thinkagent 推理结论**：推理过程和决策依据
            3. **actionagent 执行结果**：实际执行的行动和输出
            4. **历史交互记录**：之前的对话和操作历史
            ## 三、核心评估准则
            ### 1. 任务完成判断标准
            任务完成的条件包括：
            * ✅ 用户的问题已得到明确回答
            * ✅ 用户的需求已被满足
            * ✅ 所有必要的信息已被提供
            * ✅ 用户没有进一步的问题或需求
             
            ### 2. 执行结果评估
            评估 actionagent 的执行结果：
            * 输出是否准确回答了用户问题
            * 信息是否完整和有用
            * 格式是否清晰易读
            * 是否符合用户的预期
                             
            ## 四、输出格式要求   
            ### 标准输出格式         
                观察结果: [简要描述执行结果的有效性]
                任务状态: [完成/未完成]
                下一步行动: [具体建议或"任务已完成"]
            ### 详细说明
            1. **观察结果**（100 字以内）：简洁评估执行结果是否有效
            2. **任务状态**：明确标注 "完成" 或 "未完成"
            3. **下一步行动**：
            * 如任务完成：输出 "任务已完成"
            * 如任务未完成：提供具体的下一步建议
            
            ## 五、特殊情况处理
            ### 1. 任务完成的情况
            当判断任务已完成时，直接输出且需要调用工具使得任务终止：
                观察结果: 用户问题已得到充分回答...
                任务状态: 完成
                下一步行动: 任务已完成
                
            ### 2. 需要继续的情况
            当判断任务未完成时，提供具体建议：
                观察结果: [具体问题描述]
                任务状态: 未完成
                下一步行动: [具体的改进建议] 
            ## 六、重要注意事项
            1. **简洁原则**：所有输出必须简洁明了，避免冗长分析
            2. **避免重复**：不要重复 已经输出的内容
            3. **明确性**：任务状态必须明确标注 "完成" 或 "未完成"
            4. **可操作性**：下一步行动建议必须具体可行
            5. **程序友好**：确保输出格式清晰，便于程序处理和退出
             
       
                 
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
