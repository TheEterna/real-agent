package com.ai.agent.real.application.agent.strategy;

import com.ai.agent.real.application.agent.item.FinalAgent;
import com.ai.agent.real.application.agent.item.reactplus.*;
import com.ai.agent.real.application.service.ContextManager;
import com.ai.agent.real.application.utils.AgentUtils;
import com.ai.agent.real.application.utils.FluxUtils;
import com.ai.agent.real.application.utils.FunctionUtils;
import com.ai.agent.real.common.utils.CommonUtils;
import com.ai.agent.real.contract.agent.Agent;
import com.ai.agent.real.contract.agent.AgentResult;
import com.ai.agent.real.contract.agent.IAgentStrategy;
import com.ai.agent.real.contract.agent.context.AgentContextAble;
import com.ai.agent.real.contract.agent.context.ResumePoint;
import com.ai.agent.real.contract.agent.service.IAgentTurnManagerService;
import com.ai.agent.real.contract.dto.ChatResponse;
import com.ai.agent.real.contract.model.interaction.InteractionResponse;
import com.ai.agent.real.contract.model.message.AgentMessage;
import com.ai.agent.real.contract.model.protocol.AgentExecutionEvent;
import com.ai.agent.real.contract.model.protocol.ResponseResult;
import com.ai.agent.real.entity.agent.context.reactplus.AgentMode;
import com.ai.agent.real.entity.agent.context.reactplus.ReActPlusAgentContext;
import com.ai.agent.real.entity.agent.context.reactplus.ReActPlusAgentContextMeta;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * ReAct 增强版 融合 PAE，COT，ReAct 框架 TODO: 未完成，初始化阶段
 *
 * @author han
 * @time 2025/11/3 23:23
 */
@Slf4j
public class ReActPlusAgentStrategy implements IAgentStrategy {

	private final int MAX_ITERATIONS = 50; // 最大迭代次数，默认50，可自行调整

	private final TaskAnalysisAgent taskAnalysisAgent;

	private final PlanInitAgent planInitAgent;

	private final ThoughtAgent thoughtAgent;

	private final ThinkingPlusAgent thinkingPlusAgent;

	private final ActionPlusAgent actionPlusAgent;

	private final FinalAgent finalAgent;

	private final ContextManager contextManager;

	public ReActPlusAgentStrategy(TaskAnalysisAgent taskAnalysisAgent, PlanInitAgent planInitAgent,
			ThoughtAgent thoughtAgent, ThinkingPlusAgent thinkingPlusAgent, ActionPlusAgent actionPlusAgent,
			FinalAgent finalAgent, ContextManager contextManager) {
		this.taskAnalysisAgent = taskAnalysisAgent;
		this.planInitAgent = planInitAgent;
		this.thoughtAgent = thoughtAgent;
		this.thinkingPlusAgent = thinkingPlusAgent;
		this.actionPlusAgent = actionPlusAgent;
		this.finalAgent = finalAgent;
		this.contextManager = contextManager;
	}

	/**
	 * 流式执行策略（推荐） 返回实时的执行进度和中间结果
	 * @param userInput 任务描述
	 * @param agents 可用的Agent列表
	 * @param context 执行上下文
	 * @return 流式执行结果
	 */
	@Override
	public Flux<AgentExecutionEvent> executeStreamWithInteraction(String userInput, List<Agent> agents,
			AgentContextAble context) {
		log.debug("ReActPlus starting!!!");
		if (context != null && !StringUtils.hasText(context.getTurnId())) {
			context.setTurnId(CommonUtils.getTraceId("ReActPlus"));
		}

		// 避免覆盖上游传入的 sessionId，仅在为空时设置默认
		if (context.getSessionId() == null || context.getSessionId().isBlank()) {
			context.setSessionId("default");
		}
		// fixme: 这里的 userId 后面可能要修复一下
		context.addMessage(AgentMessage.user(userInput, "user"));
		AtomicInteger iterationCount = new AtomicInteger(50);

		return Flux.concat(
				// 发射 STARTED 事件，告知前端任务开始执行
				Flux.defer(() -> Flux.just(AgentExecutionEvent.started(context, "ReActPlus 任务开始执行"))),
				Flux.defer(() -> Flux.just(AgentExecutionEvent.progress(context, "正在催促小二分配资源...", null))),
				// 任务分析，通过调用工具，将分析后的数据状态等放到 context 里
				Flux.defer(() -> executeTaskAnalysisAgent(userInput, context)),
				// 模式选择
				Flux.defer(() -> {
					ReActPlusAgentContextMeta metadata = (ReActPlusAgentContextMeta) context.getMetadata();
					switch (metadata.getAgentMode()) {
						case DIRECT: {
							iterationCount.set(0);
							return Flux.empty();
						}
						case SIMPLE: {
							return Flux.empty();
						}
						case THOUGHT: {
							return executeThoughtAgent(userInput, context);
						}
						case PLAN: {
							return executePlanAgent(userInput, context);
						}
						default: {
							return Flux.empty();
						}
					}
				}), Flux.defer(() -> {

					return Flux.range(1, iterationCount.get()).doOnNext(iteration -> {
						// 在每轮迭代前管理上下文大小
						contextManager.manageContextSize(context);

						// 记录上下文使用情况
						if (iteration % 5 == 0) { // 每 5 轮记录一次
							log.info("迭代 {}/50, 上下文使用: {}", iteration, contextManager.getContextUsage(context));
						}

						// 记录当前迭代次数到上下文
						context.setCurrentIteration(iteration);
					}).concatMap(iteration -> {
						// 在每次迭代开始前检查是否已完成
						if (context.isTaskCompleted()) {
							log.info("任务已完成，跳过第 {} 次迭代", iteration);
							return Flux.empty();
						}

						return Flux.concat(
								Flux.just(AgentExecutionEvent.progress(context,
										String.format("开始第 %d 轮思考-行动循环...", iteration), null)),
								executeReActPlusIteration(userInput, context));
					})
						// 结束条件：收到DONE事件（由task_done工具触发）
						// 已由上下文标记任务完成（例如ActionAgent调用task_done后设置的标记）
						.takeUntil(event -> {
							boolean isCompleted = context.isTaskCompleted();
							if (isCompleted) {
								log.info("检测到任务完成标记，准备结束迭代循环");
							}
							return isCompleted;
						});
				}), Flux.defer(() -> executeFinalAgent(userInput, context))

					.concatWith(Flux.just(AgentExecutionEvent.completed()))
					.doOnComplete(() -> log.info("ReActPlus任务执行完成，上下文: {}", context)));

	}

	/**
	 * 流式执行策略（推荐） 返回实时的执行进度和中间结果
	 * @param userInput 任务描述
	 * @param turnState turnState
	 * @param context 执行上下文
	 * @return 流式执行结果
	 */
	@Override
	public Flux<AgentExecutionEvent> executeStreamWithInteraction(String userInput,
			IAgentTurnManagerService.TurnState turnState, AgentContextAble context) {
		log.debug("ReActPlus starting!!!");

		if (context == null) {
			return Flux.error(new RuntimeException("context is null"));
		}
		if (!StringUtils.hasText(context.getTurnId())) {
			context.setTurnId(CommonUtils.getTraceId("ReActPlus"));
		}
		// 避免覆盖上游传入的 sessionId，仅在为空时设置默认
		if (context.getSessionId() == null || context.getSessionId().isBlank()) {
			context.setSessionId("default");
		}
		// fixme: 这里的 userId 后面可能要修复一下
		context.addMessage(AgentMessage.user(userInput, "user"));
		AtomicInteger iterationCount = new AtomicInteger(50);

		return Flux.concat(
				// 发射 STARTED 事件，告知前端任务开始执行
				Flux.defer(() -> Flux.just(AgentExecutionEvent.started(context, "ReActPlus 任务开始执行"))),
				Flux.defer(() -> Flux.just(AgentExecutionEvent.progress(context, "正在催促小二分配资源...", null))),
				// 任务分析，通过调用工具，将分析后的数据状态等放到 context 里
				Flux.defer(() -> executeTaskAnalysisAgent(userInput, context)),
				// 模式选择
				Flux.defer(() -> {
					ReActPlusAgentContextMeta metadata = (ReActPlusAgentContextMeta) context.getMetadata();
					AgentMode mode = Optional.ofNullable(metadata.getAgentMode()).orElse(AgentMode.THOUGHT);
					switch (mode) {
						case DIRECT: {
							iterationCount.set(0);
							return Flux.empty();
						}
						case SIMPLE: {
							return Flux.empty();
						}
						case THOUGHT: {
							return executeThoughtAgent(userInput, context);
						}
						case PLAN: {
							return executePlanAgent(userInput, context);
						}
						default: {
							return Flux.empty();
						}
					}
				}), Flux.defer(() -> {

					return Flux.range(1, iterationCount.get()).doOnNext(iteration -> {
						// 在每轮迭代前管理上下文大小
						contextManager.manageContextSize(context);

						// 记录上下文使用情况
						if (iteration % 5 == 0) { // 每 5 轮记录一次
							log.info("迭代 {}/50, 上下文使用: {}", iteration, contextManager.getContextUsage(context));
						}

						// 记录当前迭代次数到上下文
						context.setCurrentIteration(iteration);
					}).concatMap(iteration -> {
						// 在每次迭代开始前检查是否已完成
						if (context.isTaskCompleted()) {
							log.info("任务已完成，跳过第 {} 次迭代", iteration);
							return Flux.empty();
						}

						return Flux.concat(
								Flux.just(AgentExecutionEvent.progress(context,
										String.format("开始第 %d 轮思考-行动循环...", iteration), null)),
								executeReActPlusIteration(userInput, context));
					})
						// 结束条件：收到DONE事件（由task_done工具触发）
						// 已由上下文标记任务完成（例如ActionAgent调用task_done后设置的标记）
						.takeUntil(event -> {
							boolean isCompleted = context.isTaskCompleted();
							if (isCompleted) {
								log.info("检测到任务完成标记，准备结束迭代循环");
							}
							return isCompleted;
						});
				}), Flux.defer(() -> executeFinalAgent(userInput, context))

					.concatWith(Flux.just(AgentExecutionEvent.completed()))
					.doOnComplete(() -> log.info("ReActPlus任务执行完成，上下文: {}", context)));

	}

	@Override
	public ResponseResult<String> handleInteractionResponse(InteractionResponse response) {

		// 0. prepare args
		String sessionId = response.getSessionId();
		String turnId = response.getTurnId();
		String operationId = response.getSelectedOptionId();
		Map<String, Object> data = response.getData();
		log.info("处理交互响应, 会话ID: {}, 轮次ID: {}, 选项ID: {}, 数据: {}", sessionId, turnId, operationId, data.toString());

		// 1. check args
		if (StringUtils.hasText(sessionId))
			return ResponseResult.error("Session ID cannot be blank");
		if (StringUtils.hasText(turnId))
			return ResponseResult.error("TurnId ID cannot be blank");

		// TODO: 2. refer operationId to select logic

		return IAgentStrategy.super.handleInteractionResponse(response);
	}

	/**
	 * 执行 单次 核心部分的 ReActPlus 迭代，只有 thinkingPlus 和 actionPlus 节点
	 * @param userInput 输入
	 * @param context 上下文对象
	 * @return
	 */
	private Flux<AgentExecutionEvent> executeReActPlusIteration(String userInput, AgentContextAble context) {

		return Flux.concat(
				// 1. 思考阶段 - ThinkingPlusAgent
				Flux.defer(() -> {
					log.debug("开始 ThinkingPlus 阶段");

					ReActPlusAgentContext thinkingContext = AgentUtils.createReActPlusAgentContext(context,
							ThinkingPlusAgent.AGENT_ID);

					return Flux.concat(Flux.just(AgentExecutionEvent.progress(context, "正在进行深度思考分析...", null)),
							FluxUtils.stage(thinkingPlusAgent.executeStream(userInput, thinkingContext), context,
									ThinkingPlusAgent.AGENT_ID, FunctionUtils.defaultOnNext(log),
									() -> log.info("ThinkingPlus 阶段结束: {}", context.getMessageHistory())),
							Flux.just(AgentExecutionEvent.progress(context, "深度思考分析结束...", null)));
				}),

				// 2. 行动阶段 - ActionPlusAgent
				Flux.defer(() -> {
					log.debug("开始 ActionPlus 阶段");

					ReActPlusAgentContext actionContext = AgentUtils.createReActPlusAgentContext(context,
							ActionPlusAgent.AGENT_ID);

					return Flux.concat(Flux.just(AgentExecutionEvent.progress(context, "正在执行工具调用...", null)),
							FluxUtils.stage(actionPlusAgent.executeStream(userInput, actionContext), context,
									ActionPlusAgent.AGENT_ID, FunctionUtils.defaultOnNext(log),
									() -> log.info("ActionPlus 阶段结束: {}", context.getMessageHistory())),
							Flux.just(AgentExecutionEvent.progress(context, "工具执行完毕...", null))

				);
				}));

	}

	private Flux<AgentExecutionEvent> executeTaskAnalysisAgent(String task, AgentContextAble context) {
		ReActPlusAgentContext taskAnalysisAgentContext = AgentUtils.createReActPlusAgentContext(context,
				TaskAnalysisAgent.AGENT_ID);
		// 注意：思考阶段不需要工具审批回调，因为它不执行工具
		return FluxUtils.stage(taskAnalysisAgent.executeStream(task, taskAnalysisAgentContext), context,
				TaskAnalysisAgent.AGENT_ID, FunctionUtils.defaultOnNext(log), () -> {
					log.info("任务分析阶段结束: {}", context.getMessageHistory());
					if (context.getMetadata() == null) {
						context.setMetadata(new ReActPlusAgentContextMeta(AgentMode.DIRECT));
					}
				});
	}

	private Flux<AgentExecutionEvent> executeThoughtAgent(String task, AgentContextAble context) {
		return Flux.defer(() -> {

			ReActPlusAgentContext thoughtContext = AgentUtils.createReActPlusAgentContext(context,
					ThoughtAgent.AGENT_ID);
			return Flux.concat(Flux.just(AgentExecutionEvent.progress(context, "正在深思...", null)),

					FluxUtils.stage(thoughtAgent.executeStream(task, thoughtContext), context, ThoughtAgent.AGENT_ID,
							FunctionUtils.defaultOnNext(log),
							() -> log.info("thought 阶段结束: {}", context.getMessageHistory())),
					Flux.just(AgentExecutionEvent.progress(context, "思维链生成完毕...", null)));

		});
	}

	private Flux<AgentExecutionEvent> executePlanAgent(String task, AgentContextAble context) {
		return Flux.defer(() -> {

			ReActPlusAgentContext planContext = AgentUtils.createReActPlusAgentContext(context, ThoughtAgent.AGENT_ID);
			return Flux.concat(Flux.just(AgentExecutionEvent.progress(context, "正在规划...", null)),

					FluxUtils.stage(planInitAgent.executeStream(task, planContext), context, PlanInitAgent.AGENT_ID,
							FunctionUtils.defaultOnNext(log),
							() -> log.info("plan init 阶段结束: {}", context.getMessageHistory())),
					Flux.just(AgentExecutionEvent.progress(context, "已完成任务规划...", null))

			);
		});
	}

	private Flux<AgentExecutionEvent> executeFinalAgent(String userInput, AgentContextAble context) {
		return Flux.concat(Flux.just(AgentExecutionEvent.progress(context, "正在生成结果...", null)),
				finalAgent
					.executeStream(userInput, AgentUtils.createReActPlusAgentContext(context, FinalAgent.AGENT_ID))
					.transform(FluxUtils.handleContext(context, FinalAgent.AGENT_ID))
					.concatWith(Flux.defer(() -> {
						if (!context.isTaskCompleted() && context.getCurrentIteration() >= MAX_ITERATIONS) {
							int currentIteration = context.getCurrentIteration();
							String warningMsg = String.format("已达到最大迭代次数（%d/50），但任务未标记为完成。", currentIteration);
							log.warn(warningMsg);
							return Flux.just(AgentExecutionEvent.doneWithWarning(context, warningMsg))
								.concatWith(Flux.just(AgentExecutionEvent.progress(context, "任务执行已结束，但未正式完成", null)));
						}
						// 任务正常完成
						int totalIterations = context.getCurrentIteration();
						String successMsg = String.format("任务成功完成，共执行 %d 轮迭代", totalIterations);
						return Flux.just(AgentExecutionEvent.progress(context, successMsg, null));
					})),
				Flux.just(AgentExecutionEvent.progress(context, "生成结果完毕...", null))

		);
	}

}
