package com.ai.agent.kit.core.agent.impl;

import com.ai.agent.contract.exception.*;
import com.ai.agent.contract.spec.*;

import com.ai.agent.contract.spec.message.*;
import com.ai.agent.kit.common.utils.*;
import com.ai.agent.kit.core.agent.Agent;
import com.ai.agent.contract.spec.AgentContext;

import com.ai.agent.kit.core.tool.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.*;
import org.springframework.ai.chat.messages.AssistantMessage.*;
import org.springframework.ai.chat.model.*;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.DefaultToolCallingChatOptions;
import org.springframework.ai.support.*;
import reactor.core.publisher.*;

import java.util.*;

import static com.ai.agent.kit.common.constant.NounConstants.TASK_DONE;
import static org.springframework.ai.model.tool.ToolExecutionResult.FINISH_REASON;

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
                "ReActAgentStrategy-ActionAgent",
                "负责ReAct框架中的行动(Acting)阶段，执行思考阶段的行动指令",
                chatModel,
                toolRegistry,
                Set.of("ReActAgentStrategy", "行动", "Action"));
        this.setCapabilities(new String[]{"ReActAgentStrategy", "行动", "Action"});
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
//    private boolean isTaskDone(ChatResponse response) {
//        return response.getResult().getMetadata().containsKey(FINISH_REASON)
//                && response.getResult().getMetadata().get(METADATA_TOOL_NAME).equals(TASK_DONE)
//    }

    @Override
    public Flux<AgentExecutionEvent> executeStream(String task, AgentContext context) {
        try {

            log.debug("ActionAgent开始流式执行行动: {}", task);

            // 构建行动提示
            String actionPrompt = buildActionPrompt(task);


            Prompt prompt = AgentUtils.buildPromptWithContextAndTools(
                    this.availableTools,
                    context,
                    SYSTEM_PROMPT,
                    actionPrompt
            );

            return chatModel.stream(prompt)
                    .concatMap(response -> {
                        String content = response.getResult().getOutput().getText();
                        List<AgentExecutionEvent> events = new ArrayList<>();


                        if (ToolUtils.hasTaskDone(response)) {
                            events.add(AgentExecutionEvent.done(content));
                        } else if (ToolUtils.hasToolCalling(response)) {
                            events.add(AgentExecutionEvent.tool(context, content));
                        } else if (!content.trim().isEmpty()) {
                            log.debug("ActionAgent流式输出: {}", content);
                            events.add(AgentExecutionEvent.action(context, content));
                        }


                        return Flux.fromIterable(events);
                    })
                    .filter(Objects::nonNull) // 过滤掉 null 事件
                    .doOnNext(content -> log.debug("ActionAgent流式输出: {}", content))
                    .doOnError(e -> log.error("ActionAgent流式执行异常", e))
                    .doOnComplete(() -> {
                        afterHandle(context);
                    })
                    .onErrorResume(e -> {
                        log.error("ActionAgent流式执行异常", e);
                        return Flux.just(AgentExecutionEvent.error("ActionAgent流式执行异常: " + e.getMessage()));
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
    


}
