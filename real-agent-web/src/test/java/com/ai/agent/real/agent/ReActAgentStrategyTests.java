package com.ai.agent.real.agent;

import com.ai.agent.real.agent.impl.*;
import com.ai.agent.real.common.protocol.*;
import com.ai.agent.real.common.spec.logging.*;
import com.ai.agent.real.common.utils.*;
import com.ai.agent.real.contract.spec.*;
import lombok.extern.slf4j.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.*;
import reactor.core.publisher.*;

import java.time.*;
import java.util.*;

/**
 * @author han
 * @time 2025/10/11 9:16
 */
@Slf4j
public class ReActAgentStrategyTests {

	private String generateTraceId() {
		return "trace-" + System.currentTimeMillis() + "-" + Integer.toHexString((int) (Math.random() * 0x10000));
	}

	@Autowired
	private ThinkingAgent thinkingAgent;

	@Autowired
	private ActionAgent actionAgent;

	@Autowired
	private ObservationAgent observationAgent;

	@Autowired
	private FinalAgent finalAgent;

	@Test
	void testSimpleEntireFlow() {

		// 创建执行上下文
		AgentContext context = new AgentContext(new TraceInfo()).setSessionId("1")
			.setTraceId(generateTraceId())
			.setStartTime(LocalDateTime.now());

		String task = "请用中文写一个关于机器学习算法的简介";
		Flux.concat( // 1. 思考阶段（封装：上下文合并 + 日志回调）
				Flux.defer(() -> {
					AgentContext thinkingCtx = AgentUtils.createAgentContext(context, ThinkingAgent.AGENT_ID);
					log.debug("构建思考阶段上下文 | {}", AgentUtils.snapshot(thinkingCtx));
					return FluxUtils.stage(thinkingAgent.executeStream(task, thinkingCtx), context,
							ThinkingAgent.AGENT_ID,
							evt -> log.debug("[思考事件] type={}, msg={}...",
									Optional.ofNullable(evt).map(AgentExecutionEvent::getType).orElse(null),
									Optional.ofNullable(evt)
										.map(AgentExecutionEvent::getMessage)
										.map(msg -> AgentUtils.safeHead(msg, 256))
										.orElse(null)),
							() -> log.info("思考阶段结束: {}", context.getConversationHistory()));
				}),

				// 2. 行动阶段（封装：上下文合并 + 日志回调）
				Flux.defer(() -> {
					// 此时 context 已包含思考阶段写回的历史
					AgentContext actionCtx = AgentUtils.createAgentContext(context, ActionAgent.AGENT_ID);
					return FluxUtils.stage(actionAgent.executeStream(task, actionCtx), context, ActionAgent.AGENT_ID,
							evt -> log.debug("[观察事件] type={}, msg={}...",
									Optional.ofNullable(evt).map(AgentExecutionEvent::getType).orElse(null),
									Optional.ofNullable(evt)
										.map(AgentExecutionEvent::getMessage)
										.map(msg -> AgentUtils.safeHead(msg, 256))
										.orElse(null)),
							() -> log.info("行动阶段结束: {}", context.getConversationHistory()));
				}),

				// 3. 观察阶段（封装：先过滤DONE，再应用上下文合并与日志回调）
				Flux.defer(() -> {
					// 此时 context 已包含行动阶段写回的历史
					AgentContext observingCtx = AgentUtils.createAgentContext(context, ObservationAgent.AGENT_ID);
					log.debug("构建观察阶段上下文 | {}", AgentUtils.snapshot(observingCtx));
					return FluxUtils.stage(observationAgent.executeStream(task, observingCtx), context,
							ObservationAgent.AGENT_ID,
							evt -> log.debug("[观察事件] type={}, msg={}...",
									Optional.ofNullable(evt).map(AgentExecutionEvent::getType).orElse(null),
									Optional.ofNullable(evt)
										.map(AgentExecutionEvent::getMessage)
										.map(msg -> AgentUtils.safeHead(msg, 256))
										.orElse(null)),
							() -> log.info("观察阶段结束: {}", context.getConversationHistory()));
				}));
	}

}
