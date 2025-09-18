package com.ai.agent.real.agent.impl;

import com.ai.agent.real.common.constant.*;
import com.ai.agent.real.contract.service.*;
import com.ai.agent.real.contract.spec.*;
import com.ai.agent.real.agent.Agent;

import com.ai.agent.real.common.utils.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.*;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import reactor.core.publisher.Flux;

import java.util.*;

/**
 * 观察Agent - 负责ReAct框架中的观察(Observation)阶段
 * 分析工具执行结果，总结执行效果，为下一轮思考提供输入
 * 
 * @author han
 * @time 2025/9/9 03:00
 */
@Slf4j
public class ObservationAgent extends Agent {
    
    public static final String AGENT_ID = "ObservationAgent";
    
    private final String SYSTEM_PROMPT = """
            你是一个专门负责观察和分析的AI助手。
            你的职责是：
            1. 分析工具执行的结果和效果, 是否与任务目标一致
            2. 评估行动是否达到了预期目标
            3. 总结当前任务的进展状态
            4. 识别可能的问题和改进方向
            5. 为下一轮思考提供有价值的观察结果
            6. 当你认为任务完成或达到了预期目标,且没有问题时，使用{task_done}工具报告任务完成

            观察分析时请遵循以下原则：
            - 客观分析执行结果，不添加主观臆测
            - 明确指出成功和失败的部分
            - 识别需要进一步处理的问题
            - 提供建设性的改进建议
            - 总结当前任务的整体进展
            """;

    public ObservationAgent(ChatModel chatModel,
                          ToolService toolService) {

        super(AGENT_ID,
                "ReActAgentStrategy-ObservationAgent",
                "负责ReAct框架中的观察(Observation)阶段，分析工具执行结果，总结执行效果，为下一轮思考提供输入",
                chatModel,
                toolService,
                Set.of("ReActAgentStrategy", "观察", "Observation", NounConstants.TASK_DONE));
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

            // 流式调用LLM
            return chatModel.stream(prompt)
                    .concatMap(response -> {
                        String content = response.getResult().getOutput().getText();
                        List<AgentExecutionEvent> events = new ArrayList<>();


                        if (ToolUtils.hasTaskDoneNative(response)) {
                            events.add(AgentExecutionEvent.done(content));
                        } else if (ToolUtils.hasToolCallingNative(response)) {
                            events.add(AgentExecutionEvent.tool(context, content));
                        } else if (!content.trim().isEmpty()) {
                            log.debug("ActionAgent流式输出: {}", content);
                            events.add(AgentExecutionEvent.observing(context, content));
                        }


                        return Flux.fromIterable(events);
                    })
                    .doOnNext(content -> log.debug("ObservationAgent流式输出: {}", content))
                    .doOnError(e -> log.error("ObservationAgent流式执行异常", e))

                    .onErrorResume(e -> {
                        // handle error
                        return Flux.just(AgentExecutionEvent.error("ObservationAgent流式执行异常"));
                    })
                    .doOnComplete(() -> {
                        log.debug("ObservationAgent流式分析完成");
                        // after handle
                        afterHandle(context);
                    })
                    .concatWith(Flux.just(AgentExecutionEvent.action(context, "\n")));

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
