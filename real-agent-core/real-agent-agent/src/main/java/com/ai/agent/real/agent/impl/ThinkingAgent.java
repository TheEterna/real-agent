package com.ai.agent.real.agent.impl;

import com.ai.agent.real.common.protocol.*;
import com.ai.agent.real.contract.protocol.*;
import com.ai.agent.real.contract.service.*;
import com.ai.agent.real.contract.spec.*;
import com.ai.agent.real.agent.Agent;

import com.ai.agent.real.common.utils.*;
import com.ai.agent.real.common.protocol.AgentExecutionEvent.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import reactor.core.publisher.*;

import java.util.*;

import static com.ai.agent.real.common.constant.NounConstants.THINKING_AGENT_ID;

/**
 * 思考Agent - 负责ReAct框架中的思考(Thinking)阶段
 * 分析当前情况，决定下一步行动策略
 * 
 * @author han
 * @time 2025/9/9 02:50
 */
@Slf4j
public class ThinkingAgent extends Agent {

    public static final String AGENT_ID = THINKING_AGENT_ID;
    
    private final String SYSTEM_PROMPT = """
            ## Thought Agent 提示词
                        
            ### 角色定义
            你是一个基于ReAct框架的Thought Agent，专注于任务拆解、推理决策和行动规划。你的核心职责是通过"思考-行动-观察"循环，逐步推进任务完成，确保每一步行动都有明确的逻辑支撑。
                        
            ### 核心能力
            1. **任务分析**：将用户的原始需求拆解为可执行的子任务，明确每个子任务的目标和优先级
            2. **推理决策**：基于当前上下文和历史交互，判断下一步需要执行的操作类型（思考、调用工具、直接回答等）
            3. **行动规划**：为复杂任务制定分阶段执行计划，根据执行结果动态调整策略
            4. **反思修正**：持续评估行动效果，识别偏差并及时修正，避免无效循环
                        
            ### 工作流程
            1. **接收输入**：获取用户查询、历史交互记录和工具返回结果
            2. **思考过程**：
               - 分析当前任务状态（已完成/进行中/未开始）
               - 评估已有信息是否足够推进任务
               - 确定是否需要调用工具（搜索/计算/操作等）或直接回答
            3. **输出格式**：
               - 首先明确当前思考结论（下一步行动方向）
               - 若需要调用工具，指定工具类型及参数
               - 若无需工具，直接生成应答内容
               - 附加简要推理依据（为什么选择此行动）
                        
            ### 关键规则
            - 保持思考过程的连贯性，避免跳步或重复推理
            - 当信息不足时，优先选择调用合适的工具补充信息，而非猜测
            - 对于复杂任务，每次只推进一个明确的子目标，逐步积累成果
            - 若连续三次行动未取得进展，主动向用户请求澄清或补充信息
            - 始终使用用户指定的工作语言进行思考和输出
                        
            ### 示例场景
            当接收到"分析近五年全球气温变化趋势"的查询时：
            1. 思考：需要获取近五年全球气温数据→判断需调用数据查询工具
            2. 行动：指定工具类型为"数据检索"，参数为"全球气温变化，时间范围：近五年"
            3. 观察：接收工具返回的数据→进入下一轮思考（分析数据并可视化）
                        
            请根据具体任务需求，在每次交互中清晰展现思考逻辑与行动决策，确保任务推进的可追溯性和有效性。
            """;
    

    /**
     * 构造函数
     */
    public ThinkingAgent(ChatModel chatModel,
                          ToolService toolService,
                         ToolApprovalMode toolApprovalMode) {
        super(AGENT_ID,
                "ReActAgentStrategy-ThinkingAgent",
                "ReAct框架里的思考agent",
                chatModel,
                toolService,
                Set.of("ReActAgentStrategy", "thinking", "思考"),
                toolApprovalMode);
        this.setCapabilities(new String[]{"ReActAgentStrategy", "thinking", "思考"});
    }

    @Override
    public AgentResult execute(String task, AgentContext context) {
        try {
            log.debug("ThinkingAgent开始分析任务: {}", task);
            
            // 构建思考提示
            String thinkingPrompt = buildThinkingPrompt(task, context);
            

            Prompt prompt = AgentUtils.buildPromptWithContextAndTools(
                    this.availableTools,
                    context,
                    SYSTEM_PROMPT,
                    thinkingPrompt
            );


            // 调用LLM进行思考
            var response = chatModel.call(prompt);
            String thinking = response.getResult().getOutput().getText();
            
            log.debug("ThinkingAgent思考结果: {}", thinking);
            
            return AgentResult.success(thinking, AGENT_ID);
            
        } catch (Exception e) {
            log.error("ThinkingAgent执行异常", e);
            return AgentResult.failure("思考过程出现异常: " + e.getMessage(), AGENT_ID);
        }
    }

    /**
     * Agent的唯一标识符
     */
    @Override
    public String getAgentId() {
        return AGENT_ID;
    }

    @Override
    public Flux<AgentExecutionEvent> executeStream(String task, AgentContext context) {
        try {

            log.debug("ThinkingAgent开始流式分析任务: {}", task);
            
            // 构建思考提示
            String thinkingPrompt = buildThinkingPrompt(task, context);

            Prompt prompt = AgentUtils.buildPromptWithContextAndTools(
                    this.availableTools,
                    context,
                    SYSTEM_PROMPT,
                    thinkingPrompt
            );

            // 使用通用的工具支持方法
            return FluxUtils.executeWithToolSupport(
                    chatModel,
                    prompt,
                    context,
                    AGENT_ID,
                    toolService,
                    toolApprovalMode,
                    EventType.THINKING
            )
            .doOnNext(content -> log.debug("ThinkingAgent流式输出: {}", content))
            .doOnError(e -> log.error("ThinkingAgent流式执行异常", e))
            .onErrorResume(e -> {
                // handle error
                return Flux.just(AgentExecutionEvent.error("ThinkingAgent流式执行异常"));
            })
            .doOnComplete(() -> {
                afterHandle(context);
            })
            .doFinally(signalType -> {
                // after handle
                log.debug("ThinkingAgent流式分析结束，信号类型: {}", signalType);
            });


        } catch (Exception e) {
            log.error("ThinkingAgent流式执行异常", e);
            return Flux.error(e);
        }
    }
    
    /**
     * 构建思考提示词
     */
    private String buildThinkingPrompt(String task, AgentContext context) {
        StringBuilder promptBuilder = new StringBuilder();
        
        promptBuilder.append("请分析以下任务的当前状态：\n\n");
        promptBuilder.append("原始任务: ").append(task).append("\n\n");

        
        promptBuilder.append("请基于以上信息进行思考分析，并决定下一步的行动策略。");

        return promptBuilder.toString();
    }
}
