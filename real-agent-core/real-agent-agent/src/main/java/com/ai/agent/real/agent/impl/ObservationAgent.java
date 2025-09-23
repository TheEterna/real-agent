package com.ai.agent.real.agent.impl;

import com.ai.agent.real.common.constant.*;
import com.ai.agent.real.contract.protocol.*;
import com.ai.agent.real.contract.service.*;
import com.ai.agent.real.contract.spec.*;
import com.ai.agent.real.agent.Agent;

import com.ai.agent.real.common.utils.*;
import com.ai.agent.real.contract.spec.AgentExecutionEvent.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.*;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.*;

import static com.ai.agent.real.common.constant.NounConstants.OBSERVATION_AGENT_ID;

/**
 * 观察Agent - 负责ReAct框架中的观察(Observation)阶段
 * 分析工具执行结果，总结执行效果，为下一轮思考提供输入
 * 
 * @author han
 * @time 2025/9/9 03:00
 */
@Slf4j
public class ObservationAgent extends Agent {
    
    public static final String AGENT_ID = OBSERVATION_AGENT_ID;
    
    private final String SYSTEM_PROMPT = """
            # ReAct框架 - ObservationAgent 核心提示词
            ## 一、角色与核心目标
            你是 ReAct 框架的 **观察与决策代理（ObservationAgent）**，核心职责是承接下游行动执行代理（actionAgent）输出的“行动指令”及该指令的**实际执行结果**，结合用户原始需求、推理逻辑与历史上下文，完成两大核心任务： \s
            1. 生成 **客观、结构化、可复用** 的观察结果（验证行动是否达成目标）； \s
            2. 基于观察结果与用户需求，**判断对话是否满足终止条件**——若已解决用户所有问题，输出完整最终答案并结束对话；若仍需补充信息/继续行动，明确提示后续方向（如请求补充参数、触发reasonAgent重新推理）。
                      
                      
            ## 二、你必须接收的上下文输入
            在执行观察与决策前，需先确认以下输入信息是否完整；若缺失，需向用户/上游代理请求补充： \s
            1. **用户原始问题**：需解决的核心需求（如“分析2024年中国新能源汽车出口量同比增速，并对比欧洲市场占比”）； \s
            2. **actionAgent 行动指令**：完整的行动ID、工具名称、目标、预期观察结果（如A001调用“权威数据搜索”获取出口量）； \s
            3. **行动实际执行结果**：工具调用后的真实输出（如“搜索返回：2024年中国新能源汽车出口量520万辆，2023年为400万辆”“工具调用失败：数据源访问超时”）； \s
            4. **历史上下文记录**：已完成的“行动-观察-决策”闭环（如“[A001→观察1：获取出口量]→[reasonAgent推理2：需欧洲进口数据]→[A002→观察2：欧洲数据缺失]”）； \s
            5. **reasonAgent 推理结论**：与当前行动对应的推理逻辑（如“需出口量数据计算增速，需欧洲数据计算占比”）； \s
            6. **可用工具列表**：同actionAgent的工具库（用于判断是否存在替代工具解决执行失败问题）。
                      
                      
            ## 三、核心功能模块（双核心：观察生成 + 结束判断）
            ### 模块1：观察结果生成（基础功能）
            基于“行动指令的预期目标”与“实际执行结果”，生成结构化观察内容，需包含以下要素： \s
            - **有效性验证**：明确行动是否达成`action_goal`（如“达成：获取到2024年出口量及2023年同期数据”“未达成：仅返回2024年出口量，缺失2023年数据”）； \s
            - **关键信息提取**：从执行结果中提取与用户问题直接相关的数据/内容（结构化呈现，如表格、键值对，避免冗余）； \s
            - **异常标注**：若执行结果存在问题（如数据不完整、工具报错、数据来源非权威），需明确标注问题类型及影响（如“数据来源为非官方博客，可能存在误差，影响增速计算的准确性”“工具调用超时，未获取任何数据”）； \s
            - **关联行动目标**：将观察结果与`action_goal`直接绑定（如“当前观察结果已满足A001的‘获取出口量官方数据’目标，但未满足‘用于计算同比增速’的衍生需求（缺失2023年数据）”）。
                      
                      
            ### 模块2：对话结束判断（核心扩展功能）
            基于“观察结果”与“用户原始问题”，按以下**终止条件优先级**判断是否结束对话，若不满足则明确后续方向： \s
                      
            | 终止条件类型                | 判断标准                                                                 | 处理逻辑                                                                 |
            |-----------------------------|--------------------------------------------------------------------------|--------------------------------------------------------------------------|
            | 1. 需求完全满足             | 观察结果已覆盖用户问题的所有子需求，数据完整、权威，可直接推导最终答案。 | 输出**结构化最终答案**（整合所有关键数据+结论），标注“对话终止”。         |
            | 2. 需求部分满足，需补充信息 | 部分子需求已解决，但缺少关键参数/数据（如“已知出口量，缺少欧洲市场具体范围”）。 | 输出“部分观察结果”，同时发起**参数请求**（明确缺失参数及作用），提示“需补充后继续”。 |
            | 3. 行动执行失败，可替代解决 | 当前行动未达成目标，但存在替代工具/方案（如“海关总署搜索失败，可尝试汽车工业协会工具”）。 | 输出“行动失败观察”，关联`可用工具列表`给出**替代行动建议**，触发reasonAgent重新推理。 |
            | 4. 行动执行失败，无替代方案 | 目标无法达成（如“无任何工具可获取2024年欧洲进口数据，且无公开数据源”）。 | 输出“目标无法达成说明”（明确障碍），整理**现有部分结论**，标注“对话终止（受限于工具/数据源）”。 |
            | 5. 数据不充分，需继续行动   | 观察结果仅部分满足子需求，需进一步行动（如“已知出口量，需继续获取欧洲进口量”）。 | 输出“当前观察结果”，明确“需继续执行XX行动（如调用工具获取欧洲数据）”，触发actionAgent生成新指令。 |
                      
                      
            ## 四、核心准则（不可违背）
            1. **观察客观性**：仅基于“实际执行结果”生成观察内容，不编造数据、不主观推测（如执行结果未提2023年数据，不可标注“2023年数据约为XX”）； \s
            2. **结束判断严谨性**：必须对照“用户原始问题的所有子需求”判断终止——如用户问“增速+占比”，仅获取增速数据不可结束对话； \s
            3. **异常透明化**：若执行结果存在“数据模糊（如‘约500万辆’）”“来源非权威（如个人博客）”“格式错误”等问题，需在观察中明确标注风险，不可隐瞒； \s
            4. **上下文复用**：若历史观察已包含某数据（如前序观察已获取“欧盟27国范围”），后续判断中需复用该信息，不重复请求； \s
            5. **答案结构化**：若判断“对话结束”，最终答案需按“问题拆解→关键数据→结论”的逻辑组织，避免碎片化（如分点/表格呈现增速、占比计算过程）。
                      
                     
                                       
            ## 五、优化指引（持续提升有效性）
            1. **数据提取精准化**：从执行结果中优先提取“数值+单位+来源+时间”四要素，避免模糊表述（如将“约500万”标注为“500万辆（估算值）”）； \s
            2. **结束判断全面化**：每次判断前需“拆解用户问题子需求”并逐一核对（如用户问“增速+占比+top3出口国”，需确认三个子需求是否均满足）； \s
            3. **异常处理落地化**：若执行结果异常（如数据冲突），需在`next_step`中给出具体解决方案（如“搜索结果显示2024年出口量有520万和530万两个数据，建议调用‘国家统计局API’交叉验证”）； \s
            4. **答案可读性优化**：结束对话时的`final_answer`需“用户友好”——避免专业术语堆砌，用“分点+公式+来源标注”呈现，确保非专业用户可理解。
            
            ## 六、核心中的核心: 工具结果即为唯一真理, 所以工具返回的结果一定能达到对应任务的预期目标
            """;

    public ObservationAgent(ChatModel chatModel,
                          ToolService toolService,
                          ToolApprovalMode toolApprovalMode) {

        super(AGENT_ID,
                "ReActAgentStrategy-ObservationAgent",
                "负责ReAct框架中的观察(Observation)阶段，分析工具执行结果，总结执行效果，为下一轮思考提供输入",
                chatModel,
                toolService,
                Set.of("ReActAgentStrategy", "观察", "Observation", NounConstants.TASK_DONE),
                toolApprovalMode);
        this.setCapabilities(new String[]{"ReActAgentStrategy", "观察", "Observation", NounConstants.TASK_DONE});
    }
    

    @Override
    public AgentResult execute(String task, AgentContext context) {
        try {
            log.debug("ObservationAgent开始观察分析: {}", task);
            
            // 构建观察提示
            String observationPrompt = buildObservationPrompt(task, context);
            
            // 构建消息
            List<Message> messages = List.of(
                new SystemMessage(SYSTEM_PROMPT),
                new UserMessage(observationPrompt)
            );
            
            // 调用LLM进行观察分析
            var response = chatModel.call(new Prompt(messages));
            String observation = response.getResult().getOutput().getText();
            
            log.debug("ObservationAgent观察结果: {}", observation);
            
            return AgentResult.success(observation, AGENT_ID);
            
        } catch (Exception e) {
            log.error("ObservationAgent执行异常", e);
            return AgentResult.failure("观察分析出现异常: " + e.getMessage(), AGENT_ID);
        }
    }
    
    @Override
    public Flux<AgentExecutionEvent> executeStream(String task, AgentContext context) {
        try {

            log.debug("ObservationAgent开始流式观察分析: {}", task);
            
            // 构建观察提示
            String observationPrompt = buildObservationPrompt(task, context);

            Prompt prompt = AgentUtils.buildPromptWithContextAndTools(
                    this.availableTools,
                    context,
                    SYSTEM_PROMPT,
                    observationPrompt
            );

            // 使用通用的工具支持方法
            return FluxUtils.executeWithToolSupport(
                    chatModel,
                    prompt,
                    context,
                    AGENT_ID,
                    toolService,
                    toolApprovalMode,
                    EventType.OBSERVING
            )

            .doOnNext(content -> log.debug("ObservationAgent流式输出: {}", content))
            .doOnError(e -> log.error("ObservationAgent流式执行异常", e))

            .onErrorResume(e -> {
                // handle error
                return Flux.just(AgentExecutionEvent.error("ObservationAgent流式执行异常"));
            })
            .doOnComplete(() -> {
                afterHandle(context);
            })
            .doFinally(signalType -> {
                log.debug("ObservationAgent 执行结束，信号类型: {}", signalType);
            });
        } catch (Exception e) {
            log.error("ObservationAgent流式执行异常", e);
            return Flux.error(e);
        }
    }
    
    /**
     * 构建观察提示词
     */
    private String buildObservationPrompt(String task, AgentContext context) {
        StringBuilder promptBuilder = new StringBuilder();
        
        promptBuilder.append("请观察和分析以下任务的执行结果：\n\n");
        promptBuilder.append("原始任务: ").append(task).append("\n\n");


//        // 添加工具执行结果
//        if (context.getLastToolResult() != null) {
//            promptBuilder.append("工具执行结果:\n");
//            promptBuilder.append(context.getLastToolResult()).append("\n\n");
//        }

        
        promptBuilder.append("请基于以上信息进行观察分析：\n");
        promptBuilder.append("1. 评估执行结果是否符合预期\n");
        promptBuilder.append("2. 识别成功和失败的部分\n");
        promptBuilder.append("3. 分析当前任务的进展状态\n");
        promptBuilder.append("4. 提出下一步的改进建议\n");
        promptBuilder.append("5. 当你认为agent任务完成,且没有问题时，使用{task_done}工具报告任务完成. \n");
        promptBuilder.append("6. 你要明确你的身份, 你需要辅佐思考和行动, 并纠察他们的行为, 当你认为任务已经差不多达到了用户的效果,你就看可以结束该任务. \n");
        promptBuilder.append("7. 你要切记, 你的引导和纠正, 十分重要, 不要为了否定而否定, 不能过度纠察, 而导致回答陷入死循环. \n");

        return promptBuilder.toString();
    }
}
