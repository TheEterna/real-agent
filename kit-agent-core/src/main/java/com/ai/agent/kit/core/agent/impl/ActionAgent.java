package com.ai.agent.kit.core.agent.impl;

import com.ai.agent.kit.common.spec.*;
import com.ai.agent.kit.core.agent.Agent;
import com.ai.agent.kit.core.agent.communication.AgentContext;
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
 * 行动Agent - 负责ReAct框架中的行动(Acting)阶段
 * 基于思考结果执行具体的工具调用和操作
 * 
 * @author han
 * @time 2025/9/9 02:55
 */
@Slf4j
public class ActionAgent extends Agent {
    
    public static final String AGENT_ID = "ActionAgent";
    
    private final String SYSTEM_PROMPT = """
            你是一个专门负责执行行动的AI助手。
            你的职责是：
            1. 基于思考分析的结果，执行具体的行动
            2. 调用合适的工具来完成任务
            3. 处理工具调用的参数和配置
            4. 确保行动的准确性和有效性
            5. 如果任务完成，调用task_done工具
            
            执行行动时请遵循以下原则：
            - 严格按照思考阶段的分析结果执行
            - 选择最合适的工具和参数
            - 确保工具调用的参数格式正确
            - 处理可能的执行异常和错误
            - 提供清晰的执行反馈
            """;

    /**
     * 构造函数
     */
    public ActionAgent(ChatModel chatModel,
                       ToolRegistry toolRegistry) {

        super(AGENT_ID,
                "ReAct-ActionAgent",
                "负责ReAct框架中的行动(Acting)阶段，执行思考阶段的行动指令",
                chatModel,
                toolRegistry,
                Set.of("ReAct", "行动", "Action"));
        this.setCapabilities(new String[]{"ReAct", "行动", "Action", "close"});
    }
    

    @Override
    public AgentResult execute(String task, AgentContext context) {
        try {
            log.debug("ActionAgent开始执行行动: {}", task);
            
            // 构建行动提示
            String actionPrompt = buildActionPrompt(task, context);
            
            // 配置工具调用选项
            var options = DefaultToolCallingChatOptions.builder()
                    .toolCallbacks(ToolCallbacks.from(availableTools))
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
    
    @Override
    public Flux<AgentExecutionEvent> executeStream(String task, AgentContext context) {
        try {
            log.debug("ActionAgent开始流式执行行动: {}", task);
            
            // 构建行动提示
            String actionPrompt = buildActionPrompt(task, context);
            
            // 配置工具调用选项
            var options = DefaultToolCallingChatOptions.builder()
                    .toolCallbacks(ToolCallbacks.from(availableTools))
                    .build();
            
            // 构建消息
            List<Message> messages = List.of(
                new SystemMessage(SYSTEM_PROMPT),
                new UserMessage(actionPrompt)
            );
            
            // 流式调用LLM
            return chatModel.stream(new Prompt(messages, options))
                    .map(response -> response.getResult().getOutput().getText())
                    .filter(content -> content != null && !content.trim().isEmpty())
                    .doOnNext(content -> log.debug("ActionAgent流式输出: {}", content))
                    .map(content -> AgentExecutionEvent.acting(AGENT_ID, content))
                    .doOnError(e -> log.error("ActionAgent流式执行异常", e))
                    .doOnComplete(() -> log.debug("ActionAgent流式执行完成"));
                    
        } catch (Exception e) {
            log.error("ActionAgent流式执行异常", e);
            return Flux.error(e);
        }
    }
    
    /**
     * 构建行动提示词
     */
    private String buildActionPrompt(String task, AgentContext context) {
        StringBuilder promptBuilder = new StringBuilder();
        
        promptBuilder.append("请基于思考分析的结果，执行具体的行动：\n\n");
        promptBuilder.append("原始任务: ").append(task).append("\n\n");
        
        // 添加思考结果
        if (context.getLastThinking() != null) {
            promptBuilder.append("思考分析结果:\n");
            promptBuilder.append(context.getLastThinking()).append("\n\n");
        }
        
        // 添加执行历史
        if (context.getConversationHistory() != null && !context.getConversationHistory().isEmpty()) {
            promptBuilder.append("执行历史:\n");
            promptBuilder.append(context.getConversationHistory()).append("\n\n");
        }
        
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
        promptBuilder.append("如果任务已经完成，请调用task_done工具提供最终结果。");
        
        return promptBuilder.toString();
    }
}
