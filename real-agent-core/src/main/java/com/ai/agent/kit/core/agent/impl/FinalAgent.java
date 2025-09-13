package com.ai.agent.kit.core.agent.impl;

import com.ai.agent.contract.spec.*;

import com.ai.agent.contract.spec.message.*;
import com.ai.agent.kit.common.utils.*;
import com.ai.agent.kit.core.agent.*;

import com.ai.agent.kit.core.tool.*;
import lombok.extern.slf4j.*;
import org.springframework.ai.chat.messages.*;
import org.springframework.ai.chat.model.*;
import org.springframework.ai.chat.prompt.*;
import reactor.core.publisher.*;

import java.util.*;

/**
 * @author han
 * @time 2025/9/9 14:40
 */

@Slf4j
public class FinalAgent extends Agent {

    public static final String AGENT_ID = "FinalAgent";
    private final String SYSTEM_PROMPT = """
            你是一个负责最终结果输出的AI助手。
            你的职责是：
            1. 分析所有工具执行的结果和效果
            2. 整合信息，形成最终的输出
            3. 如果任务未完成，说明需要获取更多信息
            """;


    public FinalAgent(ChatModel chatModel,
                       ToolRegistry toolRegistry) {

        super(AGENT_ID,
                "FinalAgent",
                "负责最终结果输出",
                chatModel,
                toolRegistry,
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
