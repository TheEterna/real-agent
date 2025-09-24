package com.ai.agent.real.agent.impl;

import com.ai.agent.real.agent.*;

import com.ai.agent.real.common.protocol.*;
import com.ai.agent.real.common.utils.*;
import com.ai.agent.real.contract.service.*;
import com.ai.agent.real.contract.spec.*;
import lombok.extern.slf4j.*;
import org.springframework.ai.chat.model.*;
import org.springframework.ai.chat.prompt.*;
import reactor.core.publisher.*;

import java.util.*;

import static com.ai.agent.real.common.constant.NounConstants.FINAL_AGENT_ID;

/**
 * @author han
 * @time 2025/9/9 14:40
 */

@Slf4j
public class FinalAgent extends Agent {

    public static final String AGENT_ID = FINAL_AGENT_ID;
    private final String SYSTEM_PROMPT = """
            # FinalAgent 核心提示词
            ## 一、角色与核心目标
            你是一个负责最终结果输出的AI助手。
            
            ## 三、核心行动准则（不可违背）

            你需要基于当前完整对话上下文，为用户提供最终、清晰且自然的回答。核心任务如下：
                        
            1. **整合信息**：梳理上下文内所有分析过程、得出的结论、关键数据或解决方案，将分散信息汇总为连贯内容，避免遗漏用户问题的核心诉求。
            2. **回应问题**：针对用户最初提出的问题（如需求咨询、问题排查、信息查询等），直接给出明确答案，优先使用上下文已验证的准确结果；若上下文存在多个相关观点，需筛选出逻辑最严谨、依据最充分的内容作为核心回应。
            3. **处理异常情况**：
               - 若上下文存在错误（如数据矛盾、逻辑漏洞、结论偏差），需以“补充说明”的形式温和指出问题所在，并基于合理逻辑或常识修正，避免使用“之前的分析有误”等易暴露多环节处理的表述；
               - 若任务未完全完成或上下文信息不足，需明确告知用户当前可提供的有效信息范围，同时说明无法进一步推进的原因（如关键信息缺失、需求超出当前处理边界），并主动询问是否需要补充相关条件以继续协助；
               - 若超出上下文覆盖范围，需坦诚说明该领域或问题暂无法提供更深入的解答，避免编造信息。
            4. **对话风格**：全程保持与用户“一对一自然对话”的语气，语言简洁易懂、口语化但不失专业，不提及“其他环节”“前期分析”“多轮处理”等可能暴露内部机制的表述，让用户感知是与单一对象持续沟通，而非多角色协作的结果。
                        
            请始终以用户需求为核心，确保回答既有信息完整性，又具备对话的自然流畅感，无需额外解释自身处理逻辑，仅专注于解决用户问题。   
            
            """;


    public FinalAgent(ChatModel chatModel,
                       ToolService toolService) {

        super(AGENT_ID,
                "FinalAgent",
                "负责最终结果输出",
                chatModel,
                toolService,
                Set.of("*"));
        this.setCapabilities(new String[]{"close"});
    }
    /**
     * 流式执行任务
     *
     * @param task    任务描述
     * @param context 执行上下文
     * @return 流式执行结果
     */
    @Override
    public Flux<AgentExecutionEvent> executeStream(String task, AgentContext context) {
        try {

            log.debug("FinalAgent开始流式执行行动: {}", task);

            // 构建消息
            Prompt prompt = AgentUtils.buildPromptWithContextAndTools(
                    this.availableTools,
                    context,
                    SYSTEM_PROMPT,
                    "你将对会执行的结果进行整合，形成最终的输出"
            );

            // 流式调用LLM
            return chatModel.stream(prompt)
                    .map(response -> response.getResult().getOutput().getText())
                    .filter(content -> content != null && !content.trim().isEmpty())
                    .doOnNext(content -> {
                        log.debug("FinalAgent流式输出: {}", content);
                    })
                    .map(content ->  AgentExecutionEvent.executing(context, content))
                    .doOnError(e -> {
                        // handle error
                        log.error("FinalAgent流式执行异常", e);
                    })
                    .onErrorResume(e -> {
                        // handle error
                        return Flux.just(AgentExecutionEvent.error("FinalAgent流式执行异常"));
                    })
                    .doOnComplete(() -> {
                        // after handle
                        afterHandle(context);
                    })

                    .concatWith(Flux.just(AgentExecutionEvent.executing(context, "\n")));

        } catch (Exception e) {
            log.error("FinalAgent流式执行异常", e);
            return Flux.error(e);
        }
    }



    /**
     * 执行任务（同步版本，兼容旧接口）
     *
     * @param task    任务描述
     * @param context 工具执行上下文
     * @return 执行结果
     */
    @Override
    public AgentResult execute(String task, AgentContext context) {
        return null;
    }
}
