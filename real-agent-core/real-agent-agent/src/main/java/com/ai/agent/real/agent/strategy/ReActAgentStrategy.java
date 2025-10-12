package com.ai.agent.real.agent.strategy;


import com.ai.agent.real.agent.impl.*;
import com.ai.agent.real.common.utils.*;
import com.ai.agent.real.contract.model.*;
import com.ai.agent.real.contract.model.agent.*;
import com.ai.agent.real.contract.model.context.*;
import com.ai.agent.real.contract.model.message.*;
import com.ai.agent.real.contract.model.protocol.*;
import com.ai.agent.real.contract.utils.*;
import lombok.extern.slf4j.*;
import reactor.core.publisher.*;

import java.util.*;

/**
 * ReAct框架实现：Reasoning and Acting 实现思考-行动-观察的循环推理模式
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

	public ReActAgentStrategy(ThinkingAgent thinkingAgent, ActionAgent actionAgent, ObservationAgent observationAgent,
			FinalAgent finalAgent) {
		this.thinkingAgent = thinkingAgent;
		this.actionAgent = actionAgent;
		this.observationAgent = observationAgent;
		this.finalAgent = finalAgent;
	}

	/**
	 * 流式执行策略 返回实时的执行进度和中间结果
	 * @param task 任务描述
	 * @param agents 可用的Agent列表
	 * @param context 执行上下文
	 * @return 流式执行结果
	 */
	@Override
	public Flux<AgentExecutionEvent> executeStream(String task, List<Agent> agents, AgentContext context) {
		log.debug("ReActAgentStrategy executeStream task: {}", task);

		// 设置上下文
		context.setTurnId(CommonUtils.getTraceId());
		// 避免覆盖上游传入的 sessionId，仅在为空时设置默认
		if (context.getSessionId() == null || context.getSessionId().isBlank()) {
			context.setSessionId("default");
		}
		context.addMessage(AgentMessage.user(task, "user"));

		return Flux.concat(
				// 发送开始事件（携带上下文trace信息）
				Flux.just(AgentExecutionEvent.progress(context, "ReAct任务开始执行", null)),

				// 执行ReAct循环
				Flux.range(1, MAX_ITERATIONS)
					.concatMap(iteration -> executeReActIteration(task, context, iteration))
					// 结束条件：收到DONE事件 或
					// 已由上下文标记任务完成（例如ObservationAgent调用task_done后设置的标记）
					.takeUntil(event -> context.isTaskCompleted())
					.onErrorResume(error -> {
						log.error("ReAct流式执行异常", error);
						return Flux.just(AgentExecutionEvent.error(error));
					}),
				// 收尾：始终保留 FinalAgent 总结，然后由 FinalAgent 阶段发出 DONE/DONEWITHWARNING
				Flux.defer(() -> finalAgent
					.executeStream(task, AgentUtils.createAgentContext(context, FinalAgent.AGENT_ID))
					.transform(FluxUtils.handleContext(context, FinalAgent.AGENT_ID))
					.concatWith(Flux.defer(() -> {
						if (!context.isTaskCompleted()) {
							return Flux.just(AgentExecutionEvent.doneWithWarning(context,
									"达到最大迭代次数 " + MAX_ITERATIONS + "，任务未完成"));
						}
						return Flux.empty();
					}))))
			.concatWith(Flux.just(AgentExecutionEvent.completed()))
			.doOnComplete(() -> log.info("ReAct任务执行完成，上下文: {}", context));
	}

	/**
	 * 执行单次ReAct迭代
	 */
	private Flux<AgentExecutionEvent> executeReActIteration(String task, AgentContext context, int iteration) {
		log.debug("ReAct循环第{}轮开始", iteration);
		context.setCurrentIteration(iteration);

		// TODO 我认为在每个循环里都应该有独立的task

		// 注意：上下文必须按阶段“延迟创建”，以便每个阶段都能看到上一阶段写回的最新对话历史
		// 否则会出现第一轮 acting/observing 仍拿到空上下文的问题。
		log.info("[ITERATION {}] 思考阶段-开始 | {}", iteration, AgentUtils.snapshot(context));
		return Flux.concat(
				// 发送进度事件（使用executing并携带trace信息，避免SSE字段为空）
				Flux.just(AgentExecutionEvent.progress(context, "ReAct循环第" + iteration + "轮", null)),

				// 1. 思考阶段（封装：上下文合并 + 日志回调）
				Flux.defer(() -> {
					AgentContext thinkingCtx = AgentUtils.createAgentContext(context, ThinkingAgent.AGENT_ID);
					log.debug("[ITERATION {}] 构建思考阶段上下文 | {}", iteration, AgentUtils.snapshot(thinkingCtx));
					return FluxUtils.stage(thinkingAgent.executeStream(task, thinkingCtx), context,
							ThinkingAgent.AGENT_ID,
							evt -> log.debug("[EVT/THINK/{}] type={}, msg={}...", iteration,
									Optional.ofNullable(evt).map(AgentExecutionEvent::getType).orElse(null),
									Optional.ofNullable(evt)
										.map(AgentExecutionEvent::getMessage)
										.map(msg -> AgentUtils.safeHead(msg, 256))
										.orElse(null)),
							() -> log.info("思考阶段结束: {}", context.getMessageHistory()));
				}),

				// 2. 行动阶段（封装：上下文合并 + 日志回调）
				Flux.defer(() -> {
					// 此时 context 已包含思考阶段写回的历史
					AgentContext actionCtx = AgentUtils.createAgentContext(context, ActionAgent.AGENT_ID);
					log.debug("[ITERATION {}] 构建行动阶段上下文 | {}", iteration, AgentUtils.snapshot(actionCtx));
					return FluxUtils.stage(actionAgent.executeStream(task, actionCtx), context, ActionAgent.AGENT_ID,
							evt -> log.debug("[EVT/ACTION/{}] type={}, msg={}...", iteration,
									Optional.ofNullable(evt).map(AgentExecutionEvent::getType).orElse(null),
									Optional.ofNullable(evt)
										.map(AgentExecutionEvent::getMessage)
										.map(msg -> AgentUtils.safeHead(msg, 256))
										.orElse(null)),
							() -> log.info("行动阶段结束: {}", context.getMessageHistory()));
				}),

				// 3. 观察阶段（封装：先过滤DONE，再应用上下文合并与日志回调）
				Flux.defer(() -> {
					// 此时 context 已包含行动阶段写回的历史
					AgentContext observingCtx = AgentUtils.createAgentContext(context, ObservationAgent.AGENT_ID);
					log.debug("[ITERATION {}] 构建观察阶段上下文 | {}", iteration, AgentUtils.snapshot(observingCtx));
					return FluxUtils.stage(observationAgent.executeStream(task, observingCtx), context,
							ObservationAgent.AGENT_ID,
							evt -> log.debug("[EVT/OBSERVE/{}] type={}, msg={}...", iteration,
									Optional.ofNullable(evt).map(AgentExecutionEvent::getType).orElse(null),
									Optional.ofNullable(evt)
										.map(AgentExecutionEvent::getMessage)
										.map(msg -> AgentUtils.safeHead(msg, 256))
										.orElse(null)),
							() -> log.info("观察阶段结束: {}", context.getMessageHistory()));
				})

		);
	}

	/**
	 * 执行
	 * @param task 任务描述
	 * @param agents 可用的Agent列表
	 * @param context 工具执行上下文
	 * @return 执行结果
	 */
	@Override
	public AgentResult execute(String task, List<Agent> agents, AgentContext context) {
		throw new UnsupportedOperationException("ReActAgentStrategy不支持同步execute方法，请使用executeStream方法");
	}

}
