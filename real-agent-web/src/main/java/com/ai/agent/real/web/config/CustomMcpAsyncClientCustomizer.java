package com.ai.agent.real.web.config;

import com.ai.agent.real.agent.strategy.*;
import io.modelcontextprotocol.client.*;
import io.modelcontextprotocol.client.McpClient.*;
import io.modelcontextprotocol.spec.*;
import io.modelcontextprotocol.spec.McpSchema.*;
import lombok.extern.slf4j.*;
import org.springframework.ai.mcp.customizer.*;
import org.springframework.context.annotation.*;
import org.springframework.stereotype.*;
import reactor.core.publisher.*;

import java.time.*;
import java.util.*;

@Slf4j
@Component
public class CustomMcpAsyncClientCustomizer implements McpAsyncClientCustomizer {

	private final List<Root> roots;

	private final ElicitationService elicitationService;

	/**
	 * Session-Turn 追踪器
	 * 用于通过 sessionId 查找当前活跃的 turnId
	 * 解决 Reactor Context 无法跨越 MCP 回调边界的问题
	 */
	private final SessionTurnTracker sessionTurnTracker;

	/**
	 * 使用 @Lazy 延迟注入，避免循环依赖
	 * mcpAsyncClients 在依赖链中，需要延迟获取
	 */
	private final List<McpAsyncClient> mcpAsyncClients;

	public CustomMcpAsyncClientCustomizer(
			ElicitationService elicitationService,
			SessionTurnTracker sessionTurnTracker,
			@Lazy List<McpAsyncClient> mcpAsyncClients) {
		this.roots = List.of(new Root("file://E", "fileDir"));
		this.elicitationService = elicitationService;
		this.sessionTurnTracker = sessionTurnTracker;
		this.mcpAsyncClients = mcpAsyncClients;
	}

	private McpAsyncClient getMcpAsyncClientByName(List<McpAsyncClient> mcpAsyncClients, String name) {
		return mcpAsyncClients.stream()
			.filter(mcpAsyncClient -> mcpAsyncClient.getClientInfo().name().equals(name))
			.findFirst()
			.orElse(null);
	}

	@Override
	public void customize(String name, AsyncSpec spec) {
		// Customize the request timeout configuration
		spec.requestTimeout(Duration.ofSeconds(60));

		// Sets the root URIs that this client can access.
		spec.roots(roots);

		// Sets a custom sampling handler for processing message creation requests.
		spec.sampling((CreateMessageRequest messageRequest) -> {
			// Handle sampling default
			return Mono.just(CreateMessageResult.builder().build());
		});

		// Adds a consumer to be notified when the available tools change, such as tools
		// being added or removed.
		spec.toolsChangeConsumer((List<McpSchema.Tool> tools) -> {
			// Handle tools change
			return Mono.empty();
		});

		// Adds a consumer to be notified when the available resources change, such as
		// resources
		// being added or removed.
		spec.resourcesChangeConsumer((List<McpSchema.Resource> resources) -> {
			// Handle resources change
			return Mono.empty();
		});

		// Adds a consumer to be notified when the available prompts change, such as
		// prompts
		// being added or removed.
		spec.promptsChangeConsumer((List<Prompt> prompts) -> {
			// Handle prompts change
			return Mono.empty();
		});

		// Adds a consumer to be notified when logging messages are received from the
		// server.
		spec.loggingConsumer((McpSchema.LoggingMessageNotification loggingMessageNotification) -> {
			// Handle log messages
			return Mono.fromRunnable(() -> {
				switch (loggingMessageNotification.level()) {
					case DEBUG -> log.debug(loggingMessageNotification.toString());
					case INFO -> log.info(loggingMessageNotification.toString());
					case NOTICE -> log.warn(loggingMessageNotification.toString());
					case WARNING -> log.warn(loggingMessageNotification.toString());
					case ERROR -> log.error(loggingMessageNotification.toString());
					case CRITICAL -> log.error(loggingMessageNotification.toString());
					case ALERT -> log.error(loggingMessageNotification.toString());
					case EMERGENCY -> log.error(loggingMessageNotification.toString());
					default -> log.info(loggingMessageNotification.toString());
				}
			});
		});

		/**
		 * 添加自定义的elicitation处理器, 用于处理工具请求调用大模型的场景
		 *
		 * 当MCP工具需要与用户交互时，会发送ElicitRequest到这里
		 * 
		 * 核心问题：Reactor Context 无法跨越 MCP 回调边界传递 turnId
		 * 
		 * 解决方案：
		 * 1. 从 Reactor Context 获取 sessionId（如果有）
		 * 2. 通过 SessionTurnTracker 查找 sessionId 对应的当前活跃 turnId
		 * 3. 调用 ElicitationService 处理请求
		 * 4. 等待用户填写并返回结果给 MCP 工具
		 */
		spec.elicitation((ElicitRequest elicitationRequest) -> {
			log.info("处理MCP elicitation请求: message={}, schema={}",
				elicitationRequest.message(), elicitationRequest.requestedSchema());

			// 尝试从 Reactor Context 中获取 sessionId
			return Mono.deferContextual(ctx -> {
				String sessionId = ctx.getOrDefault("sessionId", "");
				
				if (sessionId.isBlank()) {
					log.error("无法从 Context 中获取 sessionId，elicitation 请求将失败");
					return Mono.just(ElicitResult.builder()
						.content(Map.of("error", "无法获取 sessionId"))
						.build());
				}

				// 通过 sessionId 查找当前活跃的 turnId
				String turnId = sessionTurnTracker.getCurrentTurnId(sessionId);
				
				if (turnId == null || turnId.isBlank()) {
					log.error("sessionId={} 没有活跃的 turn，无法处理 elicitation 请求", sessionId);
					return Mono.just(ElicitResult.builder()
						.content(Map.of("error", "没有活跃的对话"))
						.build());
				}

				log.info("通过 sessionId={} 找到 turnId={}, 处理 elicitation 请求", sessionId, turnId);

				// 调用ElicitationService处理请求，传递 turnId
				return elicitationService.handleElicitationAsync(elicitationRequest, turnId)
					.doOnSuccess(result -> log.info("Elicitation请求处理成功: sessionId={}, turnId={}, data={}", 
						sessionId, turnId, result.meta()))
					.doOnError(error -> log.error("Elicitation请求处理失败: sessionId={}, turnId={}", 
						sessionId, turnId, error))
					.onErrorReturn(ElicitResult.builder()
						.content(Map.of("error", "处理elicitation请求时发生错误"))
						.build());
			});
		});
	}

}