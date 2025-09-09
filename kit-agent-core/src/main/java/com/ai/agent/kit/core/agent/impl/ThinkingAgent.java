package com.ai.agent.kit.core.agent.impl;

import com.ai.agent.kit.common.spec.*;
import com.ai.agent.kit.core.agent.Agent;
import com.ai.agent.kit.core.agent.communication.AgentContext;
import com.ai.agent.kit.core.agent.communication.AgentMessage;
import com.ai.agent.kit.core.tool.*;
import com.ai.agent.kit.core.tool.model.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.*;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.DefaultToolCallingChatOptions;
import org.springframework.ai.support.*;
import reactor.core.publisher.Flux;

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
            5. 如果任务完成，使用task_done工具提供最终结果
            
            思考时请遵循以下原则：
            - 基于事实进行分析，避免臆测
            - 考虑所有可用的工具和选项
            - 如果信息不足，说明需要获取更多信息
            - 如果任务已完成，明确说明完成情况并调用task_done工具
            """;
    

    /**
     * 构造函数
     */
    public ThinkingAgent(ChatModel chatModel,
                          ToolRegistry toolRegistry) {
        super(AGENT_ID,
                "ReAct-ThinkingAgent",
                "ReAct框架里的思考agent",
                chatModel,
                toolRegistry,
                Set.of("ReAct", "thinking", "思考"));
        this.setCapabilities(new String[]{"ReAct", "thinking", "思考"});
    }

    @Override
    public AgentResult execute(String task, AgentContext context) {
        try {
            log.debug("ThinkingAgent开始分析任务: {}", task);
            
            // 构建思考提示
            String thinkingPrompt = buildThinkingPrompt(task, context);
            
            // 配置工具调用选项
            var options = DefaultToolCallingChatOptions.builder()
                    .toolCallbacks(ToolCallbacks.from(availableTools))
                    .build();
            
            // 构建消息
            List<Message> messages = List.of(
                new SystemMessage(SYSTEM_PROMPT),
                new UserMessage(thinkingPrompt)
            );
            
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
    
    @Override
    public Flux<AgentExecutionEvent> executeStream(String task, AgentContext context) {
        try {
            log.debug("ThinkingAgent开始流式分析任务: {}", task);
            
            // 构建思考提示
            String thinkingPrompt = buildThinkingPrompt(task, context);
            
            // 配置工具调用选项
            var options = DefaultToolCallingChatOptions.builder()
                    .toolCallbacks(ToolCallbacks.from(availableTools))
                    .build();
            
            // 构建消息
            // 构建消息
            List<AgentMessage> conversationHistory = context.getConversationHistory();
            List<Message> messages = new ArrayList<>();
            messages.add(new SystemMessage(SYSTEM_PROMPT));
            messages.addAll(conversationHistory);
            messages.add(new UserMessage(thinkingPrompt));



            // 流式调用LLM
            return chatModel.stream(new Prompt(messages, options))
                    .map(response -> response.getResult().getOutput().getText())
                    .filter(content -> content != null && !content.trim().isEmpty())
                    .doOnNext(content -> log.debug("ThinkingAgent流式输出: {}", content))
                    .map(content -> AgentExecutionEvent.action(AGENT_ID, content))
                    .doOnError(e -> log.error("ThinkingAgent流式执行异常", e))
                    .doOnComplete(() -> log.debug("ThinkingAgent流式分析完成"));

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
        

//        // 添加可用工具信息
//        if (availableTools != null && !availableTools.isEmpty()) {
//            promptBuilder.append("可用工具:\n");
//            for (AgentTool tool : availableTools) {
//                promptBuilder.append("- ").append(tool.getSpec().getName())
//                           .append(": ").append(tool.getSpec().getDescription()).append("\n");
//            }
//            promptBuilder.append("\n");
//        }
        
        promptBuilder.append("请基于以上信息进行思考分析，并决定下一步的行动策略。");
        promptBuilder.append("如果任务已经完成，请调用task_done工具提供最终结果。");
        
        return promptBuilder.toString();
    }
}
