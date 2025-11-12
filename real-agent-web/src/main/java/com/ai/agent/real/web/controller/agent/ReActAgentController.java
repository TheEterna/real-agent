package com.ai.agent.real.web.controller.agent;

import com.ai.agent.real.common.utils.CommonUtils;
import com.ai.agent.real.contract.agent.IAgentStrategy;
import com.ai.agent.real.contract.dto.ChatRequest;
import com.ai.agent.real.entity.agent.context.ReActAgentContext;
import com.ai.agent.real.contract.agent.service.IAgentTurnManagerService;
import com.ai.agent.real.contract.model.logging.*;
import com.ai.agent.real.contract.model.protocol.*;
import lombok.extern.slf4j.*;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.*;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.*;

import java.time.*;

/**
 * Agent对话控制器 提供ReAct框架的Web接口
 *
 * @author han
 * @time 2025/9/10 10:45
 */
@Slf4j
@RestController
@RequestMapping("/api/agent/chat")
@CrossOrigin(origins = "*")
public class ReActAgentController {

	private final IAgentTurnManagerService agentSessionManagerService;

	public ReActAgentController(IAgentTurnManagerService agentSessionManagerService,
			@Qualifier("reActAgentStrategy") IAgentStrategy reActAgentStrategy) {
		this.agentSessionManagerService = agentSessionManagerService.of(reActAgentStrategy);
	}

	/**
	 * 执行ReAct任务（流式响应） 使用AgentSessionHub管理会话，支持工具审批中断和恢复
	 */
	@PostMapping(value = "/react/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
	public Flux<ServerSentEvent<AgentExecutionEvent>> executeReActStream(@RequestBody ChatRequest request) {
		log.info("收到ReAct流式执行请求: sessionId={}, message={}", request.getSessionId(), request.getMessage());

		// 验证sessionId
		if (request.getSessionId() == null || request.getSessionId().isBlank()) {
			request.setSessionId("session-" + System.currentTimeMillis());
			log.info("未提供sessionId，自动生成: {}", request.getSessionId());
		}

		// 创建执行上下文
		ReActAgentContext context = new ReActAgentContext(new TraceInfo().setSessionId(request.getSessionId())
			.setTurnId(CommonUtils.getTraceId(CommonUtils.getTraceId("ReAct")))
			.setStartTime(LocalDateTime.now()));

		// 设置任务到上下文（用于恢复时使用）
		context.setTask(request.getMessage());

		// 通过AgentSessionHub订阅会话
		return agentSessionManagerService.subscribe(request.getSessionId(), request.getMessage(), context)
			.doOnError(error -> log.error("ReAct执行异常: sessionId={}", request.getSessionId(), error))
			.doOnComplete(() -> {
				context.setEndTime(LocalDateTime.now());
				log.info("ReAct任务执行完成: sessionId={}", request.getSessionId());
			});

	}

}
