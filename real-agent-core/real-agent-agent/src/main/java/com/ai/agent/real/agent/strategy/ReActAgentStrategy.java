package com.ai.agent.real.agent.strategy;

import com.ai.agent.real.agent.*;
import com.ai.agent.real.agent.impl.*;
import com.ai.agent.real.common.constant.*;
import com.ai.agent.real.common.utils.*;
import com.ai.agent.real.contract.spec.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.*;
import reactor.core.publisher.*;

import java.util.*;
import java.util.concurrent.atomic.*;
import java.time.*;

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


    public ReActAgentStrategy(ChatModel chatModel, ToolRegistry toolRegistry) {
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
            // 发送开始事件（携带上下文trace信息）
            Flux.just(AgentExecutionEvent.executing(context, "ReAct任务开始执行")),
            
            // 执行ReAct循环
            Flux.range(1, MAX_ITERATIONS)
                .concatMap(iteration -> executeReActIteration(task, context, iteration)
//                    .doOnNext(event -> {
//                        // 依据上下文的任务完成标记进行短路，而非等待 DONE 事件
//                        if () {
//                            taskCompleted.set(true);
//                        }
//                    }))
                .takeUntil(this::isTaskDoneEvent)


        )
        .onErrorResume(error -> {
            log.error("ReAct流式执行异常", error);
            return Flux.just(AgentExecutionEvent.error(error));
        })
        // 收尾：始终保留 FinalAgent 总结，然后由 FinalAgent 阶段发出 DONE/DONEWITHWARNING
        .concatWith(Flux.defer(() ->
            finalAgent.executeStream(task, createAgentContext(context, FinalAgent.AGENT_ID))
                .transform(FluxUtils.handleContext(context, FinalAgent.AGENT_ID))
                .concatWith(Flux.just(
                    (taskCompleted.get() || context.isTaskCompleted())
                        ? AgentExecutionEvent.done(NounConstants.TASK_DONE)
                        : AgentExecutionEvent.doneWithWarning("达到最大迭代次数 " + MAX_ITERATIONS + "，任务未完成")
                ))
        ))
        .doOnComplete(() -> log.info("ReAct任务执行完成，上下文: {}", context)));
    }
    
    /**
     * 检查事件是否为任务完成事件
     */
    private boolean isTaskDoneEvent(AgentExecutionEvent event) {
        if (event == null || event.getType() == null) {
            return false;
        }
        return "DONE".equals(event.getType().toString());
//        return "DONE".equals(event.getType().toString()) ||
//               (event.getMessage() != null && event.getMessage().contains(TASK_DONE));
    }
    
    /**
     * 执行单次ReAct迭代
     */
    private Flux<AgentExecutionEvent> executeReActIteration(String task, AgentContext context, int iteration) {
        log.debug("ReAct循环第{}轮开始", iteration);
        context.setCurrentIteration(iteration);
        
        return Flux.concat(
            // 发送进度事件（使用executing并携带trace信息，避免SSE字段为空）
            Flux.just(AgentExecutionEvent.executing(context,
                "ReAct循环第" + iteration + "轮，进度：" + ((double) iteration / MAX_ITERATIONS))),
            
            // 1. 思考阶段
            thinkingAgent.executeStream(task, createAgentContext(context, ThinkingAgent.AGENT_ID))
                .transform(FluxUtils.handleContext(context, ThinkingAgent.AGENT_ID))
                    .doOnComplete(() -> log.info("思考阶段结束: {}", context.getConversationHistory())),

            // 2. 行动阶段 
            actionAgent.executeStream(task, createAgentContext(context, ActionAgent.AGENT_ID))
                .transform(FluxUtils.handleContext(context, ActionAgent.AGENT_ID))
                .doOnComplete(() -> log.info("行动阶段结束: {}", context.getConversationHistory())),

            // 3. 观察阶段（独立第三阶段，未完成时才执行）
            observationAgent.executeStream(task, createAgentContext(context, ObservationAgent.AGENT_ID))
                    .transform(FluxUtils.handleContext(context, ObservationAgent.AGENT_ID))
                    .doOnComplete(() -> log.info("观察阶段结束: {}", context.getConversationHistory()))

                // Flux.defer(() -> {
            //     if (context.isTaskCompleted()) {
            //         return Flux.empty();
            //     }
            //     return observationAgent.executeStream(task, createAgentContext(context, ObservationAgent.AGENT_ID))
            //         .transform(FluxUtils.handleContext(context, ObservationAgent.AGENT_ID));
            // })
        );
    }

    /**
     * 为Agent创建独立的执行上下文副本
     */
    private AgentContext createAgentContext(AgentContext originalContext, String agentId) {
        AgentContext newContext = new AgentContext();
        // 独立的 TraceInfo：逐字段复制，避免共享同一个 TraceInfo 对象
        newContext.setSessionId(originalContext.getSessionId());
        newContext.setTraceId(originalContext.getTraceId());
        newContext.setSpanId(originalContext.getSpanId());
        // start/end time 由各 Agent 生命周期自行设置，这里不复制 endTime
        newContext.setEndTime(null);

        // 复制对话历史与参数（浅拷贝集合内容即可）
        newContext.addMessages(originalContext.getConversationHistory());
        newContext.setToolArgs(originalContext.getToolArgs());
        newContext.setCurrentIteration(originalContext.getCurrentIteration());
        newContext.setTaskCompleted(originalContext.isTaskCompleted());

        // 为新上下文设置独立的 Agent 与 node 标识
        newContext.setAgentId(agentId);
        newContext.setNodeId(CommonUtils.getNodeId());
        newContext.setStartTime(LocalDateTime.now());

        return newContext;
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
