package com.ai.agent.real.application.agent.strategy;

import com.ai.agent.real.application.agent.item.ActionAgent;
import com.ai.agent.real.application.agent.item.FinalAgent;
import com.ai.agent.real.application.agent.item.ObservationAgent;
import com.ai.agent.real.application.agent.item.ThinkingAgent;
import com.ai.agent.real.application.utils.AgentUtils;
import com.ai.agent.real.application.utils.FluxUtils;
import com.ai.agent.real.common.utils.*;
import com.ai.agent.real.contract.agent.Agent;
import com.ai.agent.real.contract.agent.IAgentStrategy;
import com.ai.agent.real.contract.agent.context.AgentContextAble;
import com.ai.agent.real.contract.model.message.*;
import com.ai.agent.real.contract.model.protocol.*;
import com.ai.agent.real.contract.tool.IToolService;
import com.ai.agent.real.contract.model.context.ReActAgentContext;
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
public class ReActAgentStrategy implements IAgentStrategy {

	private static final int MAX_ITERATIONS = 10;

	private final ThinkingAgent thinkingAgent;

	private final ActionAgent actionAgent;

	private final ObservationAgent observationAgent;

	private final FinalAgent finalAgent;

	private final IToolService toolService;

	public ReActAgentStrategy(ThinkingAgent thinkingAgent, ActionAgent actionAgent, ObservationAgent observationAgent,
			FinalAgent finalAgent, IToolService toolService) {
		this.thinkingAgent = thinkingAgent;
		this.actionAgent = actionAgent;
		this.observationAgent = observationAgent;
		this.finalAgent = finalAgent;
		this.toolService = toolService;
	}

	/**
	 * 流式执行策略 返回实时的执行进度和中间结果
	 * @param userInput 用户输入
	 * @param agents 可用的Agent列表
	 * @param context 执行上下文
	 * @return 流式执行结果
	 */
	@Override
	public Flux<AgentExecutionEvent> executeStream(String userInput, List<Agent> agents, AgentContextAble context) {
		return executeStream(userInput, agents, (ReActAgentContext) context);
	}

	/**
	 * 流式执行策略（带工具审批回调）
	 * @param userInput 用户输入
	 * @param agents 可用的Agent列表
	 * @param context 执行上下文
	 * @return 流式执行结果
	 */
	private Flux<AgentExecutionEvent> executeStream(String userInput, List<Agent> agents, ReActAgentContext context) {
		log.debug("ReActAgentStrategy executeStream userInput: {}", userInput);

		// 设置上下文
		context.setTurnId(UUID.randomUUID());
		// 避免覆盖上游传入的 sessionId，仅在为空时设置默认
		if (context.getSessionId() == null) {
			context.setSessionId(UUID.randomUUID());
		}

		// fixme: 这里的 userId 后面可能要修复一下
		context.addMessage(AgentMessage.user(userInput, "user"));

		return Flux.concat(
				// 发送开始事件（携带上下文trace信息）
				Flux.just(AgentExecutionEvent.progress(context, "ReAct任务开始执行", null)),

				// 执行ReAct循环
				Flux.range(1, MAX_ITERATIONS)
					.concatMap(iteration -> executeReActIteration(userInput, context, iteration))
					// 结束条件：收到DONE事件 或
					// 已由上下文标记任务完成（例如ObservationAgent调用task_done后设置的标记）
					.takeUntil(event -> context.isTaskCompleted())
					.onErrorResume(error -> {
						log.error("ReAct流式执行异常", error);
						return Flux.just(AgentExecutionEvent.error(error));
					}),
				// 收尾：始终保留 FinalAgent 总结，然后由 FinalAgent 阶段发出 DONE/DONEWITHWARNING
				Flux.defer(() -> finalAgent
					.executeStream(userInput, AgentUtils.createReActAgentContext(context, FinalAgent.AGENT_ID))
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
	 * 继续下一轮迭代（通用方法）
	 */
	private Flux<AgentExecutionEvent> continueNextIteration(String task, ReActAgentContext context, int iteration) {
		log.info("继续执行剩余迭代: currentIteration={}", iteration);

		return Flux.concat(
				// 继续执行剩余的迭代
				Flux.range(iteration + 1, MAX_ITERATIONS - iteration)
					.concatMap(nextIter -> executeReActIteration(task, context, nextIter))
					.takeUntil(event -> context.isTaskCompleted()),

				// 最终总结
				Flux.defer(() -> finalAgent
					.executeStream(task, AgentUtils.createReActAgentContext(context, FinalAgent.AGENT_ID))
					.transform(FluxUtils.handleContext(context, FinalAgent.AGENT_ID))))
			.concatWith(Flux.just(AgentExecutionEvent.completed()));
	}

	/**
	 * 执行单次ReAct迭代
	 */
	private Flux<AgentExecutionEvent> executeReActIteration(String userInput, ReActAgentContext context,
			int iteration) {
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
					ReActAgentContext thinkingCtx = AgentUtils.createReActAgentContext(context, ThinkingAgent.AGENT_ID);
					log.debug("[ITERATION {}] 构建思考阶段上下文 | {}", iteration, AgentUtils.snapshot(thinkingCtx));
					// 注意：思考阶段不需要工具审批回调，因为它不执行工具
					return FluxUtils.stage(thinkingAgent.executeStream(userInput, thinkingCtx), context,
							ThinkingAgent.AGENT_ID,
							evt -> log.debug("[EVT/THINK/{}] type={}, msg={}...", iteration,
									Optional.ofNullable(evt).map(AgentExecutionEvent::getType).orElse(null),
									Optional.ofNullable(evt)
										.map(AgentExecutionEvent::getMessage)
										.map(msg -> AgentUtils.safeHead(msg, 256))
										.orElse(null)),
							() -> log.info("思考阶段结束: {}", context.getMessageHistory()));
				}),

				// 2. 行动阶段（封装：上下文合并 + 日志回调 + 工具审批回调）
				Flux.defer(() -> {
					// 此时 context 已包含思考阶段写回的历史
					ReActAgentContext actionCtx = AgentUtils.createReActAgentContext(context, ActionAgent.AGENT_ID);
					log.debug("[ITERATION {}] 构建行动阶段上下文 | {}", iteration, AgentUtils.snapshot(actionCtx));
					// 注意：行动阶段需要传递工具审批回调
					return FluxUtils.stage(actionAgent.executeStream(userInput, actionCtx), context,
							ActionAgent.AGENT_ID,
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
					ReActAgentContext observingCtx = AgentUtils.createReActAgentContext(context,
							ObservationAgent.AGENT_ID);
					log.debug("[ITERATION {}] 构建观察阶段上下文 | {}", iteration, AgentUtils.snapshot(observingCtx));
					return FluxUtils.stage(observationAgent.executeStream(userInput, observingCtx), context,
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

}
