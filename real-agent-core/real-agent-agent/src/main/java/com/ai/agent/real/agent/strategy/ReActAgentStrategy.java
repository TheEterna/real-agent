package com.ai.agent.real.agent.strategy;

import com.ai.agent.real.agent.*;
import com.ai.agent.real.agent.impl.ActionAgent;
import com.ai.agent.real.agent.impl.FinalAgent;
import com.ai.agent.real.agent.impl.ObservationAgent;
import com.ai.agent.real.agent.impl.ThinkingAgent;
import com.ai.agent.real.common.constant.*;
import com.ai.agent.real.common.protocol.*;
import com.ai.agent.real.common.spec.logging.*;
import com.ai.agent.real.common.utils.*;
import com.ai.agent.real.contract.spec.*;
import lombok.extern.slf4j.Slf4j;
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


    public ReActAgentStrategy(ThinkingAgent thinkingAgent, ActionAgent actionAgent, ObservationAgent observationAgent, FinalAgent finalAgent) {
        this.thinkingAgent = thinkingAgent;
        this.actionAgent = actionAgent;
        this.observationAgent = observationAgent;
        this.finalAgent = finalAgent;
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
        // 避免覆盖上游传入的 sessionId，仅在为空时设置默认
        if (context.getSessionId() == null || context.getSessionId().isBlank()) {
            context.setSessionId("1");
        }
        
        AtomicBoolean taskCompleted = new AtomicBoolean(false);
        
        return Flux.concat(
            // 发送开始事件（携带上下文trace信息）
            Flux.just(AgentExecutionEvent.progress(context, "ReAct任务开始执行", null)),
            
            // 执行ReAct循环
            Flux.range(1, MAX_ITERATIONS)
                .concatMap(iteration -> executeReActIteration(task, context, iteration)
                // 结束条件：收到DONE事件 或 已由上下文标记任务完成（例如ObservationAgent调用task_done后设置的标记）
                .takeUntil(event -> context.isTaskCompleted())


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
    }
    
    /**
     * 执行单次ReAct迭代
     */
    private Flux<AgentExecutionEvent> executeReActIteration(String task, AgentContext context, int iteration) {
        log.debug("ReAct循环第{}轮开始", iteration);
        context.setCurrentIteration(iteration);
        // 注意：上下文必须按阶段“延迟创建”，以便每个阶段都能看到上一阶段写回的最新对话历史
        // 否则会出现第一轮 acting/observing 仍拿到空上下文的问题。
        log.info("[CTX/ITER {}] 思考阶段-开始 | {}", iteration, snapshot(context));
        return Flux.concat(
            // 发送进度事件（使用executing并携带trace信息，避免SSE字段为空）
            Flux.just(AgentExecutionEvent.progress(context, "ReAct循环第" + iteration + "轮", null)),
            
            // 1. 思考阶段（封装：上下文合并 + 日志回调）
            Flux.defer(() -> {
                AgentContext thinkingCtx = createAgentContext(context, ThinkingAgent.AGENT_ID);
                log.debug("[CTX/ITER {}] 构建思考阶段上下文 | {}", iteration, snapshot(thinkingCtx));
                return FluxUtils.stage(
                    thinkingAgent.executeStream(task, thinkingCtx),
                    context,
                    ThinkingAgent.AGENT_ID,
                    evt -> log.debug("[EVT/THINK/{}] type={}, msg={}...", iteration, evt != null ? evt.getType() : null, safeHead(evt != null ? evt.getMessage() : null, 256)),
                    () -> log.info("思考阶段结束: {}", context.getConversationHistory())
                );
            }),

            // 2. 行动阶段（封装：上下文合并 + 日志回调）
            Flux.defer(() -> {
                // 此时 context 已包含思考阶段写回的历史
                AgentContext actionCtx = createAgentContext(context, ActionAgent.AGENT_ID);
                log.debug("[CTX/ITER {}] 构建行动阶段上下文 | {}", iteration, snapshot(actionCtx));
                return FluxUtils.stage(
                    actionAgent.executeStream(task, actionCtx),
                    context,
                    ActionAgent.AGENT_ID,
                    evt -> log.debug("[EVT/ACTION/{}] type={}, msg={}...", iteration, evt != null ? evt.getType() : null, safeHead(evt != null ? evt.getMessage() : null, 256)),
                    () -> log.info("行动阶段结束: {}", context.getConversationHistory())
                );
            }),

            // 3. 观察阶段（封装：先过滤DONE，再应用上下文合并与日志回调）
            Flux.defer(() -> {
                // 此时 context 已包含行动阶段写回的历史
                AgentContext observingCtx = createAgentContext(context, ObservationAgent.AGENT_ID);
                log.debug("[CTX/ITER {}] 构建观察阶段上下文 | {}", iteration, snapshot(observingCtx));
                return FluxUtils.stage(
                    observationAgent.executeStream(task, observingCtx)
                        // 观察阶段可能会发出 DONE 事件：为了让 FinalAgent 统一收尾，不把该 DONE 直接透传到前端
                        .filter(evt -> evt == null || evt.getType() == null || !"DONE".equals(evt.getType().toString())),
                    context,
                    ObservationAgent.AGENT_ID,
                    evt -> log.debug("[EVT/OBSERVE/{}] type={}, msg={}...", iteration, evt != null ? evt.getType() : null, safeHead(evt != null ? evt.getMessage() : null, 256)),
                    () -> log.info("观察阶段结束: {}", context.getConversationHistory())
                );
            })

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
        AgentContext newContext = new AgentContext(new TraceInfo());


        // 独立的 TraceInfo：逐字段复制，避免共享同一个 TraceInfo 对象
        newContext.setSessionId(originalContext.getSessionId());
        newContext.setTraceId(originalContext.getTraceId());
        newContext.setSpanId(originalContext.getSpanId());
        // start/end time 由各 Agent 生命周期自行设置，这里不复制 endTime
        newContext.setEndTime(null);

        // 复制对话历史与参数（浅拷贝集合内容），确保不共享可变集合引用
        newContext.setConversationHistory(originalContext.getConversationHistory());
        newContext.setToolArgs(originalContext.getToolArgs());
        newContext.setCurrentIteration(originalContext.getCurrentIteration());
        newContext.setTaskCompleted(originalContext.getTaskCompleted());

        // 为新上下文设置独立的 Agent 与 node 标识
        newContext.setAgentId(agentId);
        newContext.setNodeId(CommonUtils.getNodeId());
        newContext.setStartTime(LocalDateTime.now());

        return newContext;
    }

    /**
     * 打印上下文快照，辅助排查“模型未遵循上下文”的问题。
     */
    private String snapshot(AgentContext ctx) {
        try {
            int msgSize = ctx.getConversationHistory() != null ? ctx.getConversationHistory().size() : 0;
            String lastMsg = "";
            if (msgSize > 0) {
                Object tail = ctx.getConversationHistory().get(msgSize - 1);
                lastMsg = safeHead(String.valueOf(tail), 200);
            }
            String toolArgKeys = "";
            if (ctx.getToolArgs() != null) {
                toolArgKeys = String.join(",", ctx.getToolArgs().toString());
            }
            return String.format("session=%s trace=%s node=%s agent=%s iter=%d done=%s msgs=%d last=%s toolArgKeys=[%s]",
                    ctx.getSessionId(), ctx.getTraceId(), ctx.getNodeId(), ctx.getAgentId(),
                    ctx.getCurrentIteration(), ctx.isTaskCompleted(), msgSize, lastMsg, toolArgKeys);
        } catch (Exception e) {
            return "<snapshot-error>";
        }
    }

    private String safeHead(String s, int max) {
        if (s == null) {
            return "";
        }
        String t = s.replaceAll("\n", " ");
        return t.length() > max ? t.substring(0, max) + "..." : t;
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

        // 不要在非阻塞线程中使用blockLast()，而是直接返回null或抛出UnsupportedOperationException
        // 因为execute方法是同步阻塞的，而executeStream是异步非阻塞的，两者不兼容
        throw new UnsupportedOperationException("ReActAgentStrategy不支持同步execute方法，请使用executeStream方法");
    }






}
