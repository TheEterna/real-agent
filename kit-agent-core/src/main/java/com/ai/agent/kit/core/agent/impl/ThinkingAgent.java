package com.ai.agent.kit.core.agent.impl;

import com.ai.agent.contract.spec.*;

import com.ai.agent.contract.spec.message.*;
import com.ai.agent.kit.common.utils.*;
import com.ai.agent.kit.core.agent.Agent;
import com.ai.agent.contract.spec.AgentContext;

import com.ai.agent.kit.core.tool.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.*;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.DefaultToolCallingChatOptions;
import org.springframework.ai.support.*;
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
            """;
    

    /**
     * 构造函数
     */
    public ThinkingAgent(ChatModel chatModel,
                          ToolRegistry toolRegistry) {
        super(AGENT_ID,
                "ReActAgentStrategy-ThinkingAgent",
                "ReAct框架里的思考agent",
                chatModel,
                toolRegistry,
                Set.of("ReActAgentStrategy", "thinking", "思考"));
        this.setCapabilities(new String[]{"ReActAgentStrategy", "thinking", "思考"});
    }

    @Override
    public AgentResult execute(String task, AgentContext context) {
        try {
            log.debug("ThinkingAgent开始分析任务: {}", task);
            
            // 构建思考提示
            String thinkingPrompt = buildThinkingPrompt(task, context);
            
            // 配置工具调用选项
            var optionsBuilder = DefaultToolCallingChatOptions.builder();
            if (availableTools != null && !availableTools.isEmpty()) {
                optionsBuilder.toolCallbacks(ToolCallbacks.from(availableTools));
            }
            var options = optionsBuilder.build();
            

            // 构建消息
            List<AgentMessage> conversationHistory = context.getConversationHistory();
            List<Message> messages = new ArrayList<>();
            messages.add(new SystemMessage(SYSTEM_PROMPT));
            messages.addAll(conversationHistory);
            messages.add(new UserMessage(thinkingPrompt));

            // 调用LLM进行思考
            var response = chatModel.call(new Prompt(messages, options));
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
            // pre handle
            preHandle(context);
            log.debug("ThinkingAgent开始流式分析任务: {}", task);
            
            // 构建思考提示
            String thinkingPrompt = buildThinkingPrompt(task, context);


            // 配置工具调用选项
            var optionsBuilder = DefaultToolCallingChatOptions.builder();
            if (availableTools != null && !availableTools.isEmpty()) {
                optionsBuilder.toolCallbacks(ToolCallbacks.from(availableTools.toArray()));
            }
            var options = optionsBuilder.build();


            // 构建消息
            // 构建消息
            List<AgentMessage> conversationHistory = context.getConversationHistory();
            List<Message> messages = new ArrayList<>();
            messages.add(new SystemMessage(SYSTEM_PROMPT));
            messages.addAll(AgentMessageUtils.toSpringAiMessages(conversationHistory));
            messages.add(new UserMessage(thinkingPrompt));



            // 流式调用LLM
            return chatModel.stream(new Prompt(messages, options))
                    .map(response -> response.getResult().getOutput().getText())
                    .filter(content -> content != null && !content.trim().isEmpty())
                    .doOnNext(content -> log.debug("ThinkingAgent流式输出: {}", content))
                    .map(content -> {
                        return AgentExecutionEvent.thinking(context, content);
                    })
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
