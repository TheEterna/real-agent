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
import com.ai.agent.real.contract.agent.AgentStrategy;
import com.ai.agent.real.contract.agent.context.AgentContextAble;
import com.ai.agent.real.contract.agent.context.ResumePoint;
import com.ai.agent.real.contract.model.callback.ToolApprovalCallback;
import com.ai.agent.real.contract.model.message.AgentMessage;
import com.ai.agent.real.contract.model.protocol.AgentExecutionEvent;
import com.ai.agent.real.entity.agent.context.ReActAgentContext;
import com.ai.agent.real.entity.agent.context.reactplus.ReActPlusAgentContextMeta;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Optional;

/**
 * ReAct 增强版 融合 PAE，COT，ReAct 框架 TODO: 未完成，初始化阶段
 *
 * @author han
 * @time 2025/11/3 23:23
 */
@Slf4j
public class ReActPlusAgentStrategy implements AgentStrategy {

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
	public Flux<AgentExecutionEvent> executeStream(String userInput, List<Agent> agents, AgentContextAble context) {
		log.debug("ReActPlus starting!!!");
		context.setTurnId(CommonUtils.getTraceId("ReActPlus"));
		// 避免覆盖上游传入的 sessionId，仅在为空时设置默认
		if (context.getSessionId() == null || context.getSessionId().isBlank()) {
			context.setSessionId("default");
		}
		// fixme: 这里的 userId 后面可能要修复一下
		context.addMessage(AgentMessage.user(userInput, "user"));

		// 设置工具审批回调到上下文
		// context.setToolApprovalCallback(approvalCallback);
		return Flux.concat(
				// 发射 STARTED 事件，告知前端任务开始执行
				Flux.just(AgentExecutionEvent.started(context, "ReActPlus 任务开始执行")),
				Flux.just(AgentExecutionEvent.progress(context, "正在催促小二分配资源...", null)),
				// 任务分析，通过调用工具，将分析后的数据状态等放到 context 里
				Flux.defer(() -> executeTaskAnalysisAgent(userInput, context)),
				// 模式选择
				Flux.defer(() -> {
					ReActPlusAgentContextMeta metadata = (ReActPlusAgentContextMeta) context.getMetadata();
					switch (metadata.getAgentMode()) {
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
				}), Flux.range(1, 50).doOnNext(iteration -> {
					// 在每轮迭代前管理上下文大小
					contextManager.manageContextSize(context);

					// 记录上下文使用情况
					if (iteration % 5 == 0) { // 每 5 轮记录一次
						log.info("迭代 {}/50, 上下文使用: {}", iteration, contextManager.getContextUsage(context));
					}
				})
					.concatMap(iteration -> executeReActPlusIteration(userInput, context,
							context.getToolApprovalCallback()))
					// 结束条件：收到DONE事件
					// 已由上下文标记任务完成（例如ObservationAgent调用task_done后设置的标记）
					.takeUntil(event -> context.isTaskCompleted()),
				Flux.defer(() -> executeFinalAgent(userInput, context))

					.concatWith(Flux.just(AgentExecutionEvent.completed()))
					.doOnComplete(() -> log.info("ReActPlus任务执行完成，上下文: {}", context)));

	}

	/**
	 * 执行 单次 核心部分的 ReActPlus 迭代，只有 thinkingPlus 和 actionPlus 节点
	 * @param userInput 输入
	 * @param context 上下文对象
	 * @param approvalCallback 工具审批回调
	 * @return
	 */
	private Flux<AgentExecutionEvent> executeReActPlusIteration(String userInput, AgentContextAble context,
			ToolApprovalCallback approvalCallback) {

		return Flux.concat(
				// 1. 思考阶段 - ThinkingPlusAgent
				Flux.defer(() -> {
					log.debug("开始 ThinkingPlus 阶段");

					ReActAgentContext thinkingContext = AgentUtils.createAgentContext(context,
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

					ReActAgentContext actionContext = AgentUtils.createAgentContext(context, ActionPlusAgent.AGENT_ID);
					// 设置工具审批回调
					if (approvalCallback != null) {
						actionContext.setToolApprovalCallback(approvalCallback);
					}

					return Flux.concat(Flux.just(AgentExecutionEvent.progress(context, "正在执行工具调用...", null)),
							FluxUtils.stage(actionPlusAgent.executeStream(userInput, actionContext), context,
									ActionPlusAgent.AGENT_ID, FunctionUtils.defaultOnNext(log),
									() -> log.info("ActionPlus 阶段结束: {}", context.getMessageHistory())),
							Flux.just(AgentExecutionEvent.progress(context, "工具执行完毕...", null))

				);
				}));

	}

	/**
	 * 同步执行策略（兼容旧版本）
	 * @param task 任务描述
	 * @param agents 可用的Agent列表
	 * @param context 工具执行上下文
	 * @return 执行结果
	 */
	@Override
	public AgentResult execute(String task, List<Agent> agents, AgentContextAble context) {
		return null;
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
		return null;
	}

	private Flux<AgentExecutionEvent> executeTaskAnalysisAgent(String task, AgentContextAble context) {
		ReActAgentContext taskAnalysisAgentContext = AgentUtils.createAgentContext(context, TaskAnalysisAgent.AGENT_ID);
		// 注意：思考阶段不需要工具审批回调，因为它不执行工具
		return FluxUtils.stage(taskAnalysisAgent.executeStream(task, taskAnalysisAgentContext), context,
				TaskAnalysisAgent.AGENT_ID,
				evt -> log.debug("type={}, msg={}...",
						Optional.ofNullable(evt).map(AgentExecutionEvent::getType).orElse(null),
						Optional.ofNullable(evt)
							.map(AgentExecutionEvent::getMessage)
							.map(msg -> AgentUtils.safeHead(msg, 256))
							.orElse(null)),
				() -> log.info("任务分析阶段结束: {}", context.getMessageHistory()));
	}

	private Flux<AgentExecutionEvent> executeThoughtAgent(String task, AgentContextAble context) {
		return Flux.defer(() -> {

			ReActAgentContext thoughtContext = AgentUtils.createAgentContext(context, ThoughtAgent.AGENT_ID);
			return Flux.concat(Flux.just(AgentExecutionEvent.progress(context, "正在深思...", null)),

					FluxUtils.stage(thoughtAgent.executeStream(task, thoughtContext), context, ThoughtAgent.AGENT_ID,
							FunctionUtils.defaultOnNext(log),
							() -> log.info("thought 阶段结束: {}", context.getMessageHistory())),
					Flux.just(AgentExecutionEvent.progress(context, "思维链生成完毕...", null)));

		});
	}

	private Flux<AgentExecutionEvent> executePlanAgent(String task, AgentContextAble context) {
		return Flux.defer(() -> {

			ReActAgentContext planContext = AgentUtils.createAgentContext(context, ThoughtAgent.AGENT_ID);
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
				finalAgent.executeStream(userInput, AgentUtils.createAgentContext(context, FinalAgent.AGENT_ID))
					.transform(FluxUtils.handleContext(context, FinalAgent.AGENT_ID))
					.concatWith(Flux.defer(() -> {
						if (!context.isTaskCompleted()) {
							return Flux.just(AgentExecutionEvent.doneWithWarning(context, "达到最大迭代次数，但任务未完成"))
								.concatWith(Flux.just(AgentExecutionEvent.progress(context, "达到最大迭代次数，但任务未完成", null)));
						}
						return Flux.empty();
					})),
				Flux.just(AgentExecutionEvent.progress(context, "生成结果完毕...", null))

		);
	}

}
