package com.ai.agent.real.application.agent.strategy;

import com.ai.agent.real.application.agent.impl.ActionAgent;
import com.ai.agent.real.application.agent.impl.FinalAgent;
import com.ai.agent.real.application.agent.impl.ObservationAgent;
import com.ai.agent.real.application.agent.impl.ThinkingAgent;
import com.ai.agent.real.application.utils.AgentUtils;
import com.ai.agent.real.application.utils.FluxUtils;
import com.ai.agent.real.common.utils.*;
import com.ai.agent.real.contract.agent.Agent;
import com.ai.agent.real.contract.agent.AgentResult;
import com.ai.agent.real.contract.agent.AgentStrategy;
import com.ai.agent.real.contract.agent.context.AgentContextAble;
import com.ai.agent.real.contract.agent.context.ResumePoint;
import com.ai.agent.real.contract.model.callback.ToolApprovalCallback;
import com.ai.agent.real.contract.model.message.*;
import com.ai.agent.real.contract.model.protocol.*;
import com.ai.agent.real.contract.service.ToolService;
import com.ai.agent.real.entity.agent.context.ReActAgentContext;
import lombok.extern.slf4j.*;
import org.springframework.ai.chat.messages.ToolResponseMessage.ToolResponse;
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

	private final ToolService toolService;

	public ReActAgentStrategy(ThinkingAgent thinkingAgent, ActionAgent actionAgent, ObservationAgent observationAgent,
			FinalAgent finalAgent, ToolService toolService) {
		this.thinkingAgent = thinkingAgent;
		this.actionAgent = actionAgent;
		this.observationAgent = observationAgent;
		this.finalAgent = finalAgent;
		this.toolService = toolService;
	}

	/**
	 * 流式执行策略 返回实时的执行进度和中间结果
	 * @param task 任务描述
	 * @param agents 可用的Agent列表
	 * @param context 执行上下文
	 * @return 流式执行结果
	 */
	@Override
	public Flux<AgentExecutionEvent> executeStream(String task, List<Agent> agents, AgentContextAble context) {
		return executeStream(task, agents, (ReActAgentContext) context, ToolApprovalCallback.NOOP);
	}

	/**
	 * 流式执行策略（带工具审批回调）
	 * @param task 任务描述
	 * @param agents 可用的Agent列表
	 * @param context 执行上下文
	 * @param approvalCallback 工具审批回调
	 * @return 流式执行结果
	 */
	private Flux<AgentExecutionEvent> executeStream(String task, List<Agent> agents, ReActAgentContext context,
			ToolApprovalCallback approvalCallback) {
		log.debug("ReActAgentStrategy executeStream task: {}", task);

		// 设置上下文
		context.setTurnId(CommonUtils.getTraceId("ReAct"));
		// 避免覆盖上游传入的 sessionId，仅在为空时设置默认
		if (context.getSessionId() == null || context.getSessionId().isBlank()) {
			context.setSessionId("default");
		}
		context.addMessage(AgentMessage.user(task, "user"));

		// 设置工具审批回调到上下文
		context.setToolApprovalCallback(approvalCallback);

		return Flux.concat(
				// 发送开始事件（携带上下文trace信息）
				Flux.just(AgentExecutionEvent.progress(context, "ReAct任务开始执行", null)),

				// 执行ReAct循环
				Flux.range(1, MAX_ITERATIONS)
					.concatMap(iteration -> executeReActIteration(task, context, iteration, approvalCallback))
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
	 * 从交互请求后恢复执行 注意：AgentSessionHub 已经根据用户选择的动作做了分发，这里只需要执行工具或继续迭代
	 * @param resumePoint 恢复点
	 * @param approvalCallback 工具审批回调
	 * @return 流式执行结果
	 */
	@Override
	public Flux<AgentExecutionEvent> resumeFromToolApproval(ResumePoint resumePoint,
			ToolApprovalCallback approvalCallback) {
		log.info("从交互请求后恢复执行: resumeId={}, iteration={}, stage={}", resumePoint.getResumeId(),
				resumePoint.getCurrentIteration(), resumePoint.getPausedStage());

		ReActAgentContext context = (ReActAgentContext) resumePoint.getContext();
		String task = resumePoint.getOriginalTask();
		int iteration = resumePoint.getCurrentIteration();

		// 获取交互请求信息
		var interactionRequest = resumePoint.getInteractionRequest();
		if (interactionRequest == null) {
			log.warn("恢复点缺少交互请求信息，直接继续下一轮迭代");
			return continueNextIteration(task, context, iteration, approvalCallback);
		}

		// 获取用户响应
		var userResponse = resumePoint.getUserResponse();
		if (userResponse == null) {
			log.warn("恢复点缺少用户响应信息，直接继续下一轮迭代");
			return continueNextIteration(task, context, iteration, approvalCallback);
		}

		// 根据交互类型处理
		switch (interactionRequest.getType()) {
			case TOOL_APPROVAL:
				return resumeFromToolApprovalInternal(resumePoint, task, context, iteration, approvalCallback);
			case MISSING_INFO:
			case USER_INPUT:
				// 用户已提供信息，继续执行
				return continueNextIteration(task, context, iteration, approvalCallback);
			default:
				log.warn("不支持的交互类型: {}", interactionRequest.getType());
				return continueNextIteration(task, context, iteration, approvalCallback);
		}
	}

	/**
	 * 从工具审批后恢复执行（内部方法）
	 */
	private Flux<AgentExecutionEvent> resumeFromToolApprovalInternal(ResumePoint resumePoint, String task,
			ReActAgentContext context, int iteration, ToolApprovalCallback approvalCallback) {

		// 获取工具信息
		String toolName = (String) resumePoint.getInteractionRequest().getContext().get("toolName");
		@SuppressWarnings("unchecked")
		Map<String, Object> toolArgs = (Map<String, Object>) resumePoint.getInteractionRequest()
			.getContext()
			.get("toolArgs");
		String toolCallId = resumePoint.getResumeId();

		log.info("工具审批通过，开始执行工具: {}", toolName);

		// 设置工具参数到上下文
		context.setToolArgs(toolArgs);

		// 执行工具并获取结果
		Flux<AgentExecutionEvent> toolExecutionFlux = FluxUtils
			.mapToolResultToEvent(toolService.executeToolAsync(toolName, context), context, toolName, // toolId
					toolCallId, toolName)
			.flux();

		// 将工具结果添加到上下文，然后继续执行观察阶段和后续迭代
		return Flux.concat(
				// 1. 执行工具并推送结果
				toolExecutionFlux.doOnNext(event -> {
					if (event.getType() == AgentExecutionEvent.EventType.TOOL) {
						// 将工具结果添加到上下文
						ToolResponse toolResponse = (ToolResponse) event.getData();
						context.addMessage(AgentMessage.tool(toolResponse.responseData(), "action",
								Map.of("id", toolResponse.id(), "name", toolResponse.name(), "arguments", toolArgs)));
					}
				}),

				// 2. 执行观察阶段
				Flux.defer(() -> {
					ReActAgentContext observingCtx = AgentUtils.createAgentContext(context, ObservationAgent.AGENT_ID);
					return FluxUtils.stage(observationAgent.executeStream(task, observingCtx), context,
							ObservationAgent.AGENT_ID, evt -> log.debug("[EVT/OBSERVE/RESUME] type={}", evt.getType()),
							() -> log.info("观察阶段结束（恢复后）"));
				}),

				// 3. 继续执行剩余的迭代
				Flux.range(iteration + 1, MAX_ITERATIONS - iteration)
					.concatMap(nextIter -> executeReActIteration(task, context, nextIter, approvalCallback))
					.takeUntil(event -> context.isTaskCompleted()),

				// 4. 最终总结
				Flux.defer(() -> finalAgent
					.executeStream(task, AgentUtils.createAgentContext(context, FinalAgent.AGENT_ID))
					.transform(FluxUtils.handleContext(context, FinalAgent.AGENT_ID))))
			.concatWith(Flux.just(AgentExecutionEvent.completed()));
	}

	/**
	 * 继续下一轮迭代（通用方法）
	 */
	private Flux<AgentExecutionEvent> continueNextIteration(String task, ReActAgentContext context, int iteration,
			ToolApprovalCallback approvalCallback) {
		log.info("继续执行剩余迭代: currentIteration={}", iteration);

		return Flux.concat(
				// 继续执行剩余的迭代
				Flux.range(iteration + 1, MAX_ITERATIONS - iteration)
					.concatMap(nextIter -> executeReActIteration(task, context, nextIter, approvalCallback))
					.takeUntil(event -> context.isTaskCompleted()),

				// 最终总结
				Flux.defer(() -> finalAgent
					.executeStream(task, AgentUtils.createAgentContext(context, FinalAgent.AGENT_ID))
					.transform(FluxUtils.handleContext(context, FinalAgent.AGENT_ID))))
			.concatWith(Flux.just(AgentExecutionEvent.completed()));
	}

	/**
	 * 执行单次ReAct迭代
	 */
	private Flux<AgentExecutionEvent> executeReActIteration(String task, ReActAgentContext context, int iteration,
			ToolApprovalCallback approvalCallback) {
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
					ReActAgentContext thinkingCtx = AgentUtils.createAgentContext(context, ThinkingAgent.AGENT_ID);
					log.debug("[ITERATION {}] 构建思考阶段上下文 | {}", iteration, AgentUtils.snapshot(thinkingCtx));
					// 注意：思考阶段不需要工具审批回调，因为它不执行工具
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

				// 2. 行动阶段（封装：上下文合并 + 日志回调 + 工具审批回调）
				Flux.defer(() -> {
					// 此时 context 已包含思考阶段写回的历史
					ReActAgentContext actionCtx = AgentUtils.createAgentContext(context, ActionAgent.AGENT_ID);
					log.debug("[ITERATION {}] 构建行动阶段上下文 | {}", iteration, AgentUtils.snapshot(actionCtx));
					// 注意：行动阶段需要传递工具审批回调
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
					ReActAgentContext observingCtx = AgentUtils.createAgentContext(context, ObservationAgent.AGENT_ID);
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
	public AgentResult execute(String task, List<Agent> agents, AgentContextAble context) {
		throw new UnsupportedOperationException("ReActAgentStrategy不支持同步execute方法，请使用executeStream方法");
	}

}
