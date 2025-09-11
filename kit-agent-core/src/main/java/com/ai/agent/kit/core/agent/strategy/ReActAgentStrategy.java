package com.ai.agent.kit.core.agent.strategy;

import com.ai.agent.contract.spec.*;
import com.ai.agent.contract.spec.message.*;
import com.ai.agent.kit.common.utils.*;
import com.ai.agent.kit.core.agent.*;
import com.ai.agent.kit.core.agent.impl.*;
import com.ai.agent.kit.core.tool.*;
import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.databind.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.*;
import reactor.core.publisher.*;

import java.util.*;
import java.util.concurrent.atomic.*;

import static com.ai.agent.kit.common.constant.NounConstants.TASK_DONE;

/**
 * ReAct框架实现：Reasoning and Acting
 * 实现思考-行动-观察的循环推理模式
 * 
 * @author han
 * @time 2025/9/5 12:32
 */
@Slf4j
public class ReActAgentStrategy implements AgentStrategy {

    private static final int MAX_ITERATIONS = 10;
    private final ThinkingAgent thinkingAgent;
    private final ActionAgent actionAgent;
    private final ObservationAgent observationAgent;
    private final FinalAgent finalAgent;
    private final List<AgentTool> availableTools;

    public ReActAgentStrategy(ChatModel chatModel, ToolRegistry toolRegistry) {
        this.availableTools = toolRegistry.list();
        this.thinkingAgent = new ThinkingAgent(chatModel, toolRegistry);
        this.actionAgent = new ActionAgent(chatModel, toolRegistry);
        this.observationAgent = new ObservationAgent(chatModel, toolRegistry);
        this.finalAgent = new FinalAgent(chatModel, toolRegistry);
    }


    


    /**
     * 流式执行策略
     * 返回实时的执行进度和中间结果
     *
     * @param task    任务描述
     * @param agents  可用的Agent列表
     * @param context 执行上下文
     * @return 流式执行结果
     */
    @Override
    public Flux<AgentExecutionEvent> executeStream(String task, List<Agent> agents, AgentContext context) {
        log.debug("ReActAgentStrategy executeStream task: {}", task);
        
        // 设置上下文
        context.setTraceId(CommonUtils.getTraceId());
        context.setSessionId("1");
        
        AtomicBoolean taskCompleted = new AtomicBoolean(false);
        
        return Flux.concat(
            // 发送开始事件
            Flux.just(AgentExecutionEvent.started("ReAct任务开始执行")),
            
            // 执行ReAct循环
            Flux.range(1, MAX_ITERATIONS)
                .concatMap(iteration -> executeReActIteration(task, context, iteration)
                    .doOnNext(event -> {
                        // 检查是否有完成事件
                        if (isTaskDoneEvent(event)) {
                            taskCompleted.set(true);
                        }
                    }))
                .takeUntil(this::isTaskDoneEvent)
                .concatWith(Flux.defer(() -> {
                    // 只有在任务未完成且达到最大迭代次数时才发送警告
                    if (!taskCompleted.get()) {
                        return Flux.just(AgentExecutionEvent.doneWithWarning("达到最大迭代次数 " + MAX_ITERATIONS + "，任务未完成"));
                    }
                    return Flux.empty();
                }))
        )
        .onErrorResume(error -> {
            log.error("ReAct流式执行异常", error);
            return Flux.just(AgentExecutionEvent.error(error));
        })
        .doOnComplete(() -> log.info("ReAct任务执行完成"));
    }
    
    /**
     * 检查事件是否为任务完成事件
     */
    private boolean isTaskDoneEvent(AgentExecutionEvent event) {
        if (event == null || event.getType() == null) {
            return false;
        }
        return "DONE".equals(event.getType().toString()) || 
               (event.getMessage() != null && event.getMessage().contains(TASK_DONE));
    }
    
    /**
     * 执行单次ReAct迭代
     */
    private Flux<AgentExecutionEvent> executeReActIteration(String task, AgentContext context, int iteration) {
        log.debug("ReAct循环第{}轮开始", iteration);
        context.setCurrentIteration(iteration);
        
        return Flux.concat(
            // 发送进度事件
            Flux.just(AgentExecutionEvent.progress(
                "ReAct循环第" + iteration + "轮",
                (double) iteration / MAX_ITERATIONS
            )),
            
            // 1. 思考阶段
            thinkingAgent.executeStream(task, context)
                .collectList()
                .flatMapMany(thinkingEvents -> {
                    // 收集思考结果
                    String fullThinking = thinkingEvents.stream()
                        .map(AgentExecutionEvent::getMessage)
                        .reduce("", String::concat);
                    context.addMessage(AgentMessage.thinking(fullThinking, ThinkingAgent.AGENT_ID));
                    return Flux.fromIterable(thinkingEvents);
                }),
            
            // 2. 行动阶段
            actionAgent.executeStream(task, context)
                .collectList()
                .flatMapMany(actionEvents -> {
                    // 收集行动结果
                    String fullAction = actionEvents.stream()
                        .map(AgentExecutionEvent::getMessage)
                        .reduce("", String::concat);
                    context.addMessage(AgentMessage.action(fullAction, ActionAgent.AGENT_ID));
                    
                    // 检查任务是否完成
                    if (context.isTaskCompleted()) {
                        // 任务完成，直接返回行动事件和完成事件，跳过观察阶段
                        return Flux.concat(
                            Flux.fromIterable(actionEvents),
                            // 使用final agent结束任务
                            finalAgent.executeStream(task, context)
                                .collectList()
                                .flatMapMany(finalEvents -> {
                                    String endingMessage = finalEvents.stream()
                                        .map(AgentExecutionEvent::getMessage)
                                        .reduce("", String::concat);
                                    context.addMessage(AgentMessage.done(endingMessage, FinalAgent.AGENT_ID));
                                    return Flux.concat(
                                        Flux.fromIterable(finalEvents),
                                        Flux.just(AgentExecutionEvent.done(TASK_DONE))
                                    );
                                })
                        );
                    }
                    
                    // 任务未完成，继续观察阶段
                    return Flux.concat(
                        Flux.fromIterable(actionEvents),
                        // 3. 观察阶段
                        observationAgent.executeStream(task, context)
                            .collectList()
                            .flatMapMany(observationEvents -> {
                                // 收集观察结果
                                String fullObservation = observationEvents.stream()
                                    .map(AgentExecutionEvent::getMessage)
                                    .reduce("", String::concat);
                                context.addMessage(AgentMessage.observing(fullObservation, ObservationAgent.AGENT_ID));
                                return Flux.fromIterable(observationEvents);
                            })
                    );
                })
        );
    }
    





    /**
     * 执行
     *
     * @param task    任务描述
     * @param agents  可用的Agent列表
     * @param context 工具执行上下文
     * @return 执行结果
     */
    @Override
    public AgentResult execute(String task, List<Agent> agents, AgentContext context) {
//        StringBuilder conversationHistory = new StringBuilder();
//        conversationHistory.append("任务: ").append(task).append("\n");
//        conversationHistory.append("可用工具: ").append(getToolsDescription(availableTools)).append("\n\n");
//
//        for (int iteration = 1; iteration <= MAX_ITERATIONS; iteration++) {
//            log.debug("ReAct循环第{}轮开始", iteration);
//
//            // 1. 思考阶段 (Thought)
//            AgentResult thoughtAgentResult = generateThought(conversationHistory.toString(), availableTools);
//            String thought = thoughtAgentResult.getResult();
//            conversationHistory.append("思考").append(iteration).append(": ").append(thought).append("\n");
//
//            // 检查是否完成
//            if (isTaskComplete(thought)) {
//                String finalAnswer = extractFinalAnswer(thought);
//                log.info("ReAct循环在第{}轮完成", iteration);
//                return AgentResult.success(finalAnswer, task);
//            }
//
//            // 2. 行动阶段 (Action)
//            AgentResult actionAgentResult = generateActionWithTools(conversationHistory.toString(), chatModel, availableTools);
//            String action = actionAgentResult.getResult();
//            conversationHistory.append("行动").append(iteration).append(": ").append(action).append("\n");
//
//            // 3. 观察阶段 (Observation)
//            String observation = executeActionWithTools(action, context, availableTools);
//            conversationHistory.append("观察").append(iteration).append(": ").append(observation).append("\n\n");
//        }
//
//        // 达到最大迭代次数，返回当前最佳结果
//        log.warn("ReAct循环达到最大迭代次数{}，强制结束", MAX_ITERATIONS);
//        return generateFinalAnswer(conversationHistory.toString(), chatModel);

        return AgentResult.success(executeStream(task, agents, context).blockLast().getMessage(), "1");
    }






}
