package com.ai.agent.real.application.utils;

import com.ai.agent.real.common.constant.*;
import com.ai.agent.real.contract.agent.context.AgentContextAble;
import com.ai.agent.real.contract.model.message.*;
import com.ai.agent.real.contract.model.property.*;
import com.ai.agent.real.contract.model.protocol.*;
import com.ai.agent.real.contract.model.protocol.AgentExecutionEvent.*;
import com.ai.agent.real.contract.tool.AgentTool;
import com.ai.agent.real.contract.tool.IToolService;
import com.fasterxml.jackson.core.type.*;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.exc.*;
import lombok.extern.slf4j.*;
import org.springframework.ai.chat.messages.AssistantMessage.*;
import org.springframework.ai.chat.messages.ToolResponseMessage.*;
import org.springframework.ai.chat.model.*;
import org.springframework.ai.chat.prompt.*;
import org.springframework.ai.model.*;
import reactor.core.publisher.*;

import java.util.*;
import java.util.function.*;
import java.util.stream.*;

import static org.springframework.ai.model.ModelOptionsUtils.*;

/**
 * @author han
 * @time 2025/9/13 21:50
 */
@Slf4j
public class FluxUtils {

	private static final TypeReference<HashMap<String, Object>> MAP_TYPE_REF = new TypeReference<HashMap<String, Object>>() {
	};

	/**
	 * 处理上下文，支持指定工具名称
	 * @param context 上下文
	 * @param agentId Agent ID
	 */
	public static Function<Flux<AgentExecutionEvent>, Flux<AgentExecutionEvent>> handleContext(AgentContextAble context,
			String agentId) {
		return stageFlux -> {
			StringBuilder assistantMessageBuf = new StringBuilder();
			List<AgentMessage> toolMessages = new ArrayList<>();

			return stageFlux.doOnNext(event -> {
				String msg = event.getMessage();
				if (msg == null || msg.isBlank()) {
					// do nothing
				}
				// 处理不同类型的事件
				else if (event.getType() != null) {
					switch (event.getType()) {
						case TOOL: {
							// 收集工具消息，但不立即添加到上下文
							// 使用传入的工具名称，如果没有则使用默认值
							ToolResponse toolResponse = (ToolResponse) event.getData();
							Map<String, Object> stringObjectMap = new ObjectMapper().convertValue(toolResponse,
									new TypeReference<Map<String, Object>>() {
									});
							stringObjectMap.put("arguments", event.getMeta().get("arguments"));
							toolMessages.add(AgentMessage.tool(toolResponse.responseData(), agentId, stringObjectMap));
							break;
						}
						default: {
							// 收集AI回复内容
							assistantMessageBuf.append(msg);
							break;
						}
					}
				}
				else {
					assistantMessageBuf.append(msg);
				}
			}).doOnComplete(() -> {
				// 按正确顺序添加消息到上下文：先AI回复，后工具结果
				// 即使content 为空依然要添加进上下文,因为function calling 要求必须要 如此格式的 assistant 消息存在
				/**
				 * { "role": "assistant", "content": null, "tool_calls": [ { "function": {
				 * "arguments": "{\"location\": \"杭州市\"}", "name": "get_current_weather"
				 * }, "id": "call_e405b0c0cca94b37bee78706", "index": 0, "type":
				 * "function" } ] }
				 */

				// 根据agentId确定消息类型
				AgentMessage assistantMessage = createMessageByAgentType(assistantMessageBuf.toString(), agentId);
				// add function calling

				List<ToolCall> toolCalls = toolMessages.stream().map(toolMessage -> {
					Map<String, Object> metadata = toolMessage.getMetadata();
					log.info("metadata: {}", metadata);
					return new ToolCall(metadata.get("id").toString(), "function", metadata.get("name").toString(),
							ModelOptionsUtils.toJsonString(metadata.get("arguments")));
				}).collect(Collectors.toList());

				// this is a must of function calling
				assistantMessage.setMetadata(Collections.singletonMap("tool_calls", toolCalls));

				context.addMessage(assistantMessage);

				// 然后添加工具消息
				context.addMessages(toolMessages);

			});
		};
	}

	/**
	 * 根据Agent类型创建相应的消息
	 */
	private static AgentMessage createMessageByAgentType(String content, String agentId) {
		switch (agentId) {
			case NounConstants.THINKING_AGENT_ID:
				return AgentMessage.thinking(content, agentId);
			case NounConstants.ACTION_AGENT_ID:
				return AgentMessage.action(content, agentId);
			case NounConstants.OBSERVATION_AGENT_ID:
				return AgentMessage.observing(content, agentId);
			case NounConstants.FINAL_AGENT_ID:
				return AgentMessage.completed(content, agentId);
			case NounConstants.TASK_ANALYSIS_AGENT_ID:
				return AgentMessage.taskAnalysis(content, agentId);
			case NounConstants.THOUGHT_AGENT_ID:
				return AgentMessage.thought(content, agentId);
			default:
				return AgentMessage.assistant(content, agentId);
		}
	}

	/**
	 * 将工具执行结果转换为事件，不直接写入上下文。 上下文的写入由 handleContext 方法统一管理，确保正确的消息顺序。 - 成功：返回 TOOL 事件（data
	 * 作为主内容，message 作为次要说明）。 - 失败：返回 ERROR 事件。 - 当 toolName == task_done 且 markTaskDone 为
	 * true 时，设置 context.setTaskCompleted(true)。
	 */
	public static Mono<AgentExecutionEvent> mapToolResultToEvent(Mono<ToolResult> resultMono, AgentContextAble context,
			String toolId, String toolCallId, String toolName) {
		return resultMono.map(toolResult -> {
			if (toolResult != null && toolResult.isOk()) {
				String dataStr = String.valueOf(toolResult.getData());

				switch (toolId) {
					case NounConstants.TASK_DONE: {
						context.setTaskCompleted(true);
						return AgentExecutionEvent.done(context, dataStr);
					}
					case NounConstants.PLAN_INIT: {
						return AgentExecutionEvent.initPlan(context, dataStr, toolResult.getData());
					}
					case NounConstants.PLAN_UPDATE: {
						return AgentExecutionEvent.updatePlan(context, dataStr, toolResult.getData());
					}
					case NounConstants.PLAN_ADVANCE: {
						return AgentExecutionEvent.advancePlan(context, dataStr, toolResult.getData());
					}
					case NounConstants.TASK_ANALYSIS: {
						return AgentExecutionEvent.taskAnalysis(context, dataStr, toolResult.getData());
					}
				}

				ToolResponse toolResponse = new ToolResponse(toolCallId, toolName, dataStr);
				// 创建TOOL事件，工具名称通过消息传递给 agentContext
				return AgentExecutionEvent.tool(context, toolResponse, toolName,
						Map.of("arguments", ModelOptionsUtils.toJsonString(context.getToolArgs())));
			}
			else if (toolResult != null) {
				String errorMessage = toolResult.getMessage();
				return AgentExecutionEvent.error("tool executed faild: " + errorMessage);
			}
			else {
				return AgentExecutionEvent.error("tool executed faild: 未知错误");
			}
		}).onErrorResume(ex -> Mono.just(AgentExecutionEvent.error("tool executed faild: " + ex.getMessage())));
	}

	/**
	 * 将一个阶段的Flux进行统一封装： - 应用上下文合并（handleContext） - 绑定阶段级日志回调（由调用方提供，避免在通用模块内直接依赖日志实现）
	 */
	public static Flux<AgentExecutionEvent> stage(Flux<AgentExecutionEvent> stageFlux, AgentContextAble context,
			String agentId, Consumer<AgentExecutionEvent> onNext, Runnable onComplete) {
		context.setCurrentIteration(context.getCurrentIteration() + 1);
		Flux<AgentExecutionEvent> wrapped = stageFlux.transform(FluxUtils.handleContext(context, agentId));
		if (onNext != null) {
			wrapped = wrapped.doOnNext(onNext);
		}
		if (onComplete != null) {
			wrapped = wrapped.doOnComplete(onComplete);
		}
		return wrapped;
	}

	/**
	 * 通用的Agent流式执行包装器，支持工具调用和上下文管理
	 * @param chatModel LLM模型
	 * @param prompt 提示词
	 * @param context 上下文
	 * @param agentId Agent ID
	 * @param toolService 工具服务
	 * @param toolApprovalMode 工具审批模式
	 * @param eventType 事件类型（如THINKING、ACTING、OBSERVING）
	 * @return 流式执行结果
	 */
	public static Flux<AgentExecutionEvent> executeWithToolSupport(ChatModel chatModel, Prompt prompt,
			AgentContextAble context, String agentId, IToolService toolService, ToolApprovalMode toolApprovalMode,
			EventType eventType) {

		return chatModel.stream(prompt).doOnSubscribe(subscription -> {
			log.debug("开始流式调用LLM，agentId: {}, eventType: {}", agentId, eventType);
			log.debug("Prompt消息数量: {}, 工具数量: {}", prompt.getInstructions().size(),
					prompt.getOptions() != null ? "有工具配置" : "无工具配置");
		}).concatMap(response -> {
			log.debug("收到ChatResponse: metadata={}, hasResult={}", response.getMetadata(),
					response.getResult() != null);

			// 检查是否是空的generations
			if (response.getResults().isEmpty()) {
				log.warn("收到空的generations列表，这可能表明LLM拒绝了请求或提示词有问题");
				log.warn("ChatResponse详情: {}", response);
				return Flux.empty();
			}

			String content = response.getResult().getOutput().getText();

			// 处理文本内容
			Flux<AgentExecutionEvent> contentFlux = Flux.empty();
			if (content != null && !content.trim().isEmpty()) {
				AgentExecutionEvent event = AgentExecutionEvent.common(eventType, context, content);
				contentFlux = Flux.just(event);
			}

			// 处理工具调用
			Flux<AgentExecutionEvent> toolFlux = Flux.empty();
			if (ToolUtils.hasToolCallingNative(response)) {
				// 优先使用上下文中的回调，如果没有则使用传入的回调
				toolFlux = Flux.fromIterable(response.getResult().getOutput().getToolCalls())
					.concatMap(toolCall -> executeToolCall(toolCall, context, toolService, toolApprovalMode));
			}

			// 合并内容和工具调用结果
			return Flux.concat(contentFlux, toolFlux);
		})
			// 错误恢复机制：LLM 调用级别的错误处理
			.onErrorResume(error -> {
				log.error("LLM 调用失败，agentId: {}, eventType: {}, error: {}", agentId, eventType, error.getMessage(),
						error);

				// 根据错误类型决定恢复策略
				String errorMsg = buildErrorMessage(error, agentId, eventType);

				// 发送错误事件而不是中断流
				return Flux.just(AgentExecutionEvent.error(errorMsg));
			});
	}

	/**
	 * 构建友好的错误消息
	 */
	private static String buildErrorMessage(Throwable error, String agentId, EventType eventType) {
		String errorType = error.getClass().getSimpleName();
		String errorMsg = error.getMessage() != null ? error.getMessage() : "未知错误";

		// 根据异常类型提供更具体的提示
		if (errorMsg.contains("timeout") || errorMsg.contains("timed out")) {
			return String.format("[%s] LLM 调用超时，请稍后重试或检查网络连接", agentId);
		}
		else if (errorMsg.contains("rate limit") || errorMsg.contains("quota")) {
			return String.format("[%s] API 调用频率限制，请稍后重试", agentId);
		}
		else if (errorMsg.contains("authentication") || errorMsg.contains("unauthorized")) {
			return String.format("[%s] 认证失败，请检查 API Key 配置", agentId);
		}
		else if (errorMsg.contains("connection") || errorMsg.contains("connect")) {
			return String.format("[%s] 网络连接失败，请检查服务地址和网络状态", agentId);
		}
		else {
			return String.format("[%s] LLM 调用异常 (%s): %s", agentId, errorType, errorMsg);
		}
	}

	/**
	 * 执行单个工具调用
	 */
	private static Flux<AgentExecutionEvent> executeToolCall(ToolCall toolCall, AgentContextAble context,
			IToolService toolService, ToolApprovalMode toolApprovalMode) {

		String toolName = toolCall.name();
		String toolCallId = toolCall.id();
		log.info("工具调用: toolName={}, toolCallId={}, args={}", toolName, toolCallId, toolCall.arguments());

		try {
			Map<String, Object> args = jsonToMap(toolCall.arguments(), OBJECT_MAPPER);
			String toolId = "";
			AgentTool tool = toolService.getByName(toolName);

			// 工具不存在的错误处理
			if (tool == null) {
				String errorMsg = String.format("工具不存在: %s，请检查工具是否已注册", toolName);
				log.error(errorMsg);
				// 返回错误事件，但不中断整个流
				return Flux.just(AgentExecutionEvent.error(errorMsg));
			}
			toolId = tool.getId();

			switch (toolApprovalMode) {
				case AUTO:
					// TODO: 实现基于权限列表的自动执行逻辑
					log.info("工具自动执行模式（待实现权限检查）: {}", toolName);
					context.setToolArgs(args);
					return mapToolResultToEvent(toolService.executeToolAsync(toolName, context), context, toolId,
							toolCallId, toolName)
						.flux()
						// 工具执行失败的恢复机制
						.onErrorResume(error -> {
							String errorMsg = String.format("工具 [%s] 执行失败: %s", toolName,
									error.getMessage() != null ? error.getMessage() : "未知错误");
							log.error(errorMsg, error);
							return Flux.just(AgentExecutionEvent.error(errorMsg));
						});

				// fixme: 工具审批
				// case REQUIRE_APPROVAL:
				// log.info("工具需要人工审批: {}", toolName);
				// return Flux.just(AgentExecutionEvent.toolApproval(context,
				// new ToolApprovalRequest(toolCallId, toolName, args)));

				case REQUIRE_APPROVAL:
					// 需要审批：调用回调通知上层，并返回空流（暂停执行）
					log.info("工具执行需要审批: toolName={}, toolCallId={}", toolName, toolCallId);

					// 返回空流，暂停当前执行
					// 注意：这里不会继续执行，需要等待审批后通过其他方式恢复
					return Flux.just(AgentExecutionEvent.toolApproval(context, null,
							new ToolApprovalRequest(toolCallId, toolName, args), Map.of("toolSchema", tool.getSpec())));
				case DISABLED:
				default:
					// 禁用审批：直接执行
					log.info("工具执行（无审批）: {}", toolName);
					context.setToolArgs(args);
					return mapToolResultToEvent(toolService.executeToolAsync(toolName, context), context, toolId,
							toolCallId, toolName)
						.flux();
			}
		}
		catch (Exception e) {
			// 捕获参数解析等早期错误
			String errorMsg = String.format("工具调用准备失败 [%s]: %s", toolName,
					e.getMessage() != null ? e.getMessage() : "参数解析错误");
			log.error(errorMsg, e);
			return Flux.just(AgentExecutionEvent.error(errorMsg));
		}
	}

	public static Map<String, Object> jsonToMap(String json, ObjectMapper objectMapper) {
		if (json == null || json.trim().isEmpty()) {
			return Collections.emptyMap();
		}
		try {
			// 尝试直接解析为 Map
			return objectMapper.readValue(json, MAP_TYPE_REF);
		}
		catch (MismatchedInputException e) {
			// 如果是字符串套 JSON，先解析成 String，再解析成 Map
			try {
				String innerJson = objectMapper.readValue(json, String.class);
				return objectMapper.readValue(innerJson, MAP_TYPE_REF);
			}
			catch (Exception ex) {
				throw new RuntimeException("无法解析 JSON 字符串: " + json, ex);
			}
		}
		catch (Exception e) {
			throw new RuntimeException("无效的 JSON: " + json, e);
		}
	}

	public record ToolApprovalRequest(String toolCallId, String toolName, Map<String, Object> args) {
	}

}
