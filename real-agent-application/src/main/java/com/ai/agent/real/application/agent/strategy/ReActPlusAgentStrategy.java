package com.ai.agent.real.application.agent.strategy;

import com.ai.agent.real.application.agent.item.ActionAgent;
import com.ai.agent.real.application.agent.item.FinalAgent;
import com.ai.agent.real.application.agent.item.reactplus.TaskAnalysisAgent;
import com.ai.agent.real.application.agent.item.ThinkingAgent;
import com.ai.agent.real.application.agent.item.reactplus.ThoughtAgent;
import com.ai.agent.real.common.utils.CommonUtils;
import com.ai.agent.real.contract.agent.Agent;
import com.ai.agent.real.contract.agent.AgentResult;
import com.ai.agent.real.contract.agent.AgentStrategy;
import com.ai.agent.real.contract.agent.context.AgentContextAble;
import com.ai.agent.real.contract.agent.context.ResumePoint;
import com.ai.agent.real.contract.model.callback.ToolApprovalCallback;
import com.ai.agent.real.contract.model.message.AgentMessage;
import com.ai.agent.real.contract.model.protocol.AgentExecutionEvent;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;

import java.util.List;

/**
 * ReAct 增强版 融合 PAE，COT，ReAct 框架 TODO: 未完成，初始化阶段
 *
 * @author han
 * @time 2025/11/3 23:23
 */
@Slf4j
public class ReActPlusAgentStrategy implements AgentStrategy {

	private final TaskAnalysisAgent taskAnalysisAgent;

	private final ThoughtAgent thoughtAgent;

	private final ThinkingAgent thinkingAgent;

	private final ActionAgent actionAgent;

	private final FinalAgent finalAgent;

	public ReActPlusAgentStrategy(TaskAnalysisAgent taskAnalysisAgent, ThoughtAgent thoughtAgent,
			ThinkingAgent thinkingAgent, ActionAgent actionAgent, FinalAgent finalAgent) {
		this.taskAnalysisAgent = taskAnalysisAgent;
		this.thoughtAgent = thoughtAgent;
		this.thinkingAgent = thinkingAgent;
		this.actionAgent = actionAgent;
		this.finalAgent = finalAgent;
	}

	/**
	 * 流式执行策略（推荐） 返回实时的执行进度和中间结果
	 * @param task 任务描述
	 * @param agents 可用的Agent列表
	 * @param context 执行上下文
	 * @return 流式执行结果
	 */
	@Override
	public Flux<AgentExecutionEvent> executeStream(String task, List<Agent> agents, AgentContextAble context) {
		log.debug("ReActPlus starting!!!");
		context.setTurnId(CommonUtils.getTraceId("ReActPlus"));
		// 避免覆盖上游传入的 sessionId，仅在为空时设置默认
		if (context.getSessionId() == null || context.getSessionId().isBlank()) {
			context.setSessionId("default");
		}
		// fixme: 这里的 userId 后面可能要修复一下
		context.addMessage(AgentMessage.user(task, "user"));

		// 设置工具审批回调到上下文
		// context.setToolApprovalCallback(approvalCallback);
		return Flux.concat(Flux.just(AgentExecutionEvent.progress(context, "ReAct任务开始执行", null)))
			.concatWith(Flux.just(AgentExecutionEvent.completed()))
			.doOnComplete(() -> log.info("ReAct任务执行完成，上下文: {}", context));

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

}
