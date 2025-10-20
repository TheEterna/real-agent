package com.ai.agent.real.agent.strategy;

import com.ai.agent.real.common.utils.*;
import com.ai.agent.real.contract.model.agent.*;
import com.ai.agent.real.contract.model.protocol.*;
import io.modelcontextprotocol.spec.McpSchema.*;
import lombok.extern.slf4j.*;
import reactor.core.publisher.*;

import java.util.*;
import java.util.concurrent.*;

/**
 * Elicitation服务 - 管理elicitation请求的生命周期
 *
 * 设计思路： 1. 通过 turnId 获取对应的 Sink（由 AgentStrategy 管理） 2. 向该 Sink 注入 TOOL_ELICITATION 事件 3.
 * 事件会自动合并到主 SSE 流中 4. 前端收到事件后显示表单，用户填写 5. 用户提交后，完成 CompletableFuture，结果返回给 MCP 工具
 *
 * 关键要点： - 使用 turnId（单轮对话ID）而不是 sessionId - WebFlux 异步环境下 ThreadLocal 失效，必须显式传递 turnId -
 * 每个 turnId 对应一个独立的 Sink，不会互相干扰
 *
 * 职责： 1. 接收来自MCP的elicitation请求 2. 生成 elicitationId 并管理生命周期 3. 通过 turnId 获取 Sink 并发送事件 4.
 * 等待用户填写schema数据 5. 返回用户填写的结果给MCP工具
 *
 * @author han
 * @time 2025/10/7 3:18
 */
@Slf4j
public class ElicitationService {

	/**
	 * AgentStrategy 实例 通过其 getEventSink(turnId) 方法向事件流注入 elicitation 事件
	 */
	private final AgentStrategy agentStrategy;

	/**
	 * elicitationId -> turnId 映射 用于在用户响应时查找对应的 turnId
	 */
	private final Map<String, String> elicitationToTurn = new ConcurrentHashMap<>();

	/**
	 * 存储等待用户响应的elicitation请求 Key: elicitationId, Value: 等待用户响应的Future
	 */
	private final Map<String, CompletableFuture<ElicitResult>> pendingElicitations = new ConcurrentHashMap<>();

	public ElicitationService(AgentStrategy agentStrategy) {
		this.agentStrategy = agentStrategy;
	}

	/**
	 * 处理来自MCP的elicitation请求
	 * @param elicitRequest MCP elicitation请求
	 * @param turnId 当前轮次的 turnId（必须传递）
	 * @return 响应式的ElicitResult，包含用户填写的数据
	 */
	public Mono<ElicitResult> handleElicitationAsync(ElicitRequest elicitRequest, String turnId) {
		if (turnId == null || turnId.isBlank()) {
			log.error("turnId 为空，无法处理 elicitation 请求");
			return Mono.just(ElicitResult.builder().content(Map.of("error", "turnId 为空")).build());
		}

		String elicitationId = UuidUtils.generateElicitationId();

		log.info("Received elicitation request: id={}, message={}", elicitationId, elicitRequest.message());

		// 创建等待用户响应的Future
		CompletableFuture<ElicitResult> responseFuture = new CompletableFuture<>();

		// 设置超时机制（2分钟）
		responseFuture.orTimeout(2, TimeUnit.MINUTES).exceptionally(throwable -> {
			log.warn("Elicitation request timeout: id={}", elicitationId);
			pendingElicitations.remove(elicitationId);
			return ElicitResult.builder().content(Map.of("error", "用户响应超时")).build();
		});

		pendingElicitations.put(elicitationId, responseFuture);

		// 保存 elicitationId -> turnId 映射
		elicitationToTurn.put(elicitationId, turnId);
		log.debug("elicitationId={} 映射到 turnId={}", elicitationId, turnId);

		// 构建 TOOL_ELICITATION 事件
		AgentExecutionEvent event = AgentExecutionEvent.toolElicitation(null, // traceInfo
																				// 可以根据需要传递
				elicitRequest.message(), elicitRequest.requestedSchema());

		// 通过 turnId 获取对应的 Sink 并推送事件
		// 事件会自动合并到该 turn 的主事件流中
		Sinks.Many<AgentExecutionEvent> turnSink = agentStrategy.getEventSink(turnId);
		if (turnSink != null) {
			Sinks.EmitResult result = turnSink.tryEmitNext(event);
			if (result.isSuccess()) {
				log.info("已向 turnId={} 的 Sink 注入 elicitation 事件: elicitationId={}", turnId, elicitationId);
			}
			else {
				log.error("注入 elicitation 事件失败: turnId={}, elicitationId={}, result={}", turnId, elicitationId, result);
			}
		}
		else {
			log.error("turnId={} 的 Sink 不存在，无法发送 elicitation 事件", turnId);
		}

		// 转换为响应式流
		return Mono.fromFuture(responseFuture);
	}

	/**
	 * 接收用户填写的数据
	 * @param elicitationId elicitation请求ID
	 * @param userData 用户填写的数据
	 * @return 是否成功接收
	 */
	public boolean submitUserResponse(String elicitationId, Map<String, Object> userData) {
		// 移除 Future
		CompletableFuture<ElicitResult> future = pendingElicitations.remove(elicitationId);

		// 移除 elicitationId -> turnId 映射
		String turnId = elicitationToTurn.remove(elicitationId);
		log.debug("移除 elicitationId={} 的映射，turnId={}", elicitationId, turnId);

		if (future == null) {
			log.warn("Elicitation not found or already completed: id={}", elicitationId);
			return false;
		}

		log.info("User submitted response for elicitation: id={}, data={}", elicitationId, userData);

		// 构建结果并完成Future
		ElicitResult result = ElicitResult.builder().content(userData).build();

		future.complete(result);
		return true;
	}

}