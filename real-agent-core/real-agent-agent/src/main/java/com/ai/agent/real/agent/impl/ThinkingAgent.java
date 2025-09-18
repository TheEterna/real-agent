package com.ai.agent.real.agent.impl;

import com.ai.agent.real.contract.service.*;
import com.ai.agent.real.contract.spec.*;
import com.ai.agent.real.agent.Agent;

import com.ai.agent.real.common.utils.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import reactor.core.publisher.*;

import java.util.*;

/**
 * 思考Agent - 负责ReAct框架中的思考(Thinking)阶段
 * 分析当前情况，决定下一步行动策略
 * 
 * @author han
 * @time 2025/9/9 02:50
 */
@Slf4j
public class ThinkingAgent extends Agent {

    public static final String AGENT_ID = "ThinkingAgent";
    
    private final String SYSTEM_PROMPT = """
            你是一个专门负责思考和分析的AI助手。
            你的职责是：
            1. 分析当前任务的执行情况和上下文
            2. 评估已有的信息和执行结果
            3. 判断任务是否已经完成
            4. 决定下一步需要采取的行动策略
            
            思考时请遵循以下原则：
            - 基于事实进行分析，避免臆测
            - 考虑所有可用的工具和选项
            - 如果信息不足，说明需要获取更多信息
            - 不要过度臆想, 基于上下文和基本事实进行深度分析即可
            """;
    

    /**
     * 构造函数
     */
    public ThinkingAgent(ChatModel chatModel,
                          ToolService toolService) {
        super(AGENT_ID,
                "ReActAgentStrategy-ThinkingAgent",
                "ReAct框架里的思考agent",
                chatModel,
                toolService,
                Set.of("ReActAgentStrategy", "thinking", "思考"));
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

            // 流式调用LLM
            return chatModel.stream(prompt)
                    .concatMap(response -> {
                        String content = response.getResult().getOutput().getText();
                        List<AgentExecutionEvent> events = new ArrayList<>();


                        if (ToolUtils.hasToolCallingNative(response)) {
                            events.add(AgentExecutionEvent.tool(context, content));
                        } else if (!content.trim().isEmpty()) {
                            log.debug("ThinkingAgent流式输出: {}", content);
                            events.add(AgentExecutionEvent.thinking(context, content));
                        }


                        return Flux.fromIterable(events);
                    })
                    .doOnNext(content -> log.debug("ThinkingAgent流式输出: {}", content))
                    .doOnError(e -> log.error("ThinkingAgent流式执行异常", e))
                    .onErrorResume(e -> {
                        // handle error
                        return Flux.just(AgentExecutionEvent.error("ThinkingAgent流式执行异常"));
                    })
                    .doOnComplete(() -> {
                        log.debug("ThinkingAgent流式分析完成");
                        // after handle
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
