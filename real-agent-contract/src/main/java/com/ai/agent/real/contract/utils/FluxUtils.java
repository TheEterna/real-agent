package com.ai.agent.real.contract.utils;

import com.ai.agent.real.common.constant.*;
import com.ai.agent.real.contract.model.*;
import com.ai.agent.real.contract.model.context.*;
import com.ai.agent.real.contract.model.message.*;
import com.ai.agent.real.contract.model.property.*;
import com.ai.agent.real.contract.model.protocol.*;
import com.ai.agent.real.contract.model.protocol.AgentExecutionEvent.*;
import com.ai.agent.real.contract.service.*;
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
	public static Function<Flux<AgentExecutionEvent>, Flux<AgentExecutionEvent>> handleContext(AgentContext context,
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
					switch (event.getType().toString()) {
						case "TOOL": {
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
					log.info("metadata: {}", metadata.get("arguments").toString());
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
			default:
				return AgentMessage.assistant(content, agentId);
		}
	}

	/**
	 * 将工具执行结果转换为事件，不直接写入上下文。 上下文的写入由 handleContext 方法统一管理，确保正确的消息顺序。 - 成功：返回 TOOL 事件（data
	 * 作为主内容，message 作为次要说明）。 - 失败：返回 ERROR 事件。 - 当 toolName == task_done 且 markTaskDone 为
	 * true 时，设置 context.setTaskCompleted(true)。
	 */
	public static Mono<AgentExecutionEvent> mapToolResultToEvent(Mono<ToolResult<Object>> resultMono,
			AgentContext context, String toolId, String toolCallId, String toolName) {
		return resultMono.map(toolResult -> {
			if (toolResult != null && toolResult.isOk()) {
				String dataStr = String.valueOf(toolResult.getData());

				if (NounConstants.TASK_DONE.equals(toolId)) {
					context.setTaskCompleted(true);
					return AgentExecutionEvent.done(context, dataStr);
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
	public static Flux<AgentExecutionEvent> stage(Flux<AgentExecutionEvent> stageFlux, AgentContext context,
			String agentId, Consumer<AgentExecutionEvent> onNext, Runnable onComplete) {
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
			AgentContext context, String agentId, ToolService toolService, ToolApprovalMode toolApprovalMode,
			EventType eventType) {

		return chatModel.stream(prompt).doOnSubscribe(subscription -> {
			log.debug("开始流式调用LLM，agentId: {}, eventType: {}", agentId, eventType);
			log.debug("Prompt消息数量: {}, 工具数量: {}", prompt.getInstructions().size(),
					prompt.getOptions() != null ? "有工具配置" : "无工具配置");
		}).concatMap(response -> {
			log.debug("收到ChatResponse: metadata={}, hasResult={}", response.getMetadata(),
					response.getResult() != null);

			// 检查是否是空的generations
			if (response.getResults() != null && response.getResults().isEmpty()) {
				log.warn("收到空的generations列表，这可能表明LLM拒绝了请求或提示词有问题");
				log.warn("ChatResponse详情: {}", response);
				return Flux.empty();
			}

			// 防御性编程：检查response.getResult()是否为null
			if (response.getResult() == null) {
				log.debug("收到空结果的流式响应，跳过处理");
				return Flux.empty();
			}

			// 进一步检查getOutput()是否为null
			if (response.getResult().getOutput() == null) {
				log.debug("收到空输出的流式响应，跳过处理");
				return Flux.empty();
			}

			String content = response.getResult().getOutput().getText();

			// 处理文本内容
			Flux<AgentExecutionEvent> contentFlux = Flux.empty();
			if (content != null && !content.trim().isEmpty()) {
				AgentExecutionEvent event = createEventByType(context, content, eventType);
				contentFlux = Flux.just(event);
			}

			// 处理工具调用
			Flux<AgentExecutionEvent> toolFlux = Flux.empty();
			if (ToolUtils.hasToolCallingNative(response)) {
				toolFlux = Flux.fromIterable(response.getResult().getOutput().getToolCalls())
					.concatMap(toolCall -> executeToolCall(toolCall, context, toolService, toolApprovalMode));
			}

			// 合并内容和工具调用结果
			return Flux.concat(contentFlux, toolFlux);
		});
	}

	/**
	 * 根据事件类型创建相应的事件
	 */
	private static AgentExecutionEvent createEventByType(AgentContext context, String content, EventType eventType) {
		switch (eventType) {
			case THINKING:
				return AgentExecutionEvent.thinking(context, content);
			case ACTING:
				return AgentExecutionEvent.action(context, content);
			case OBSERVING:
				return AgentExecutionEvent.observing(context, content);
			default:
				return AgentExecutionEvent.partialResult(context, content);
		}
	}

	/**
	 * 执行单个工具调用
	 */
	private static Flux<AgentExecutionEvent> executeToolCall(ToolCall toolCall, AgentContext context,
			ToolService toolService, ToolApprovalMode toolApprovalMode) {

		String toolName = toolCall.name();
		String toolCallId = toolCall.id();
		log.info(toolCall.arguments());
		Map<String, Object> args = jsonToMap(toolCall.arguments(), OBJECT_MAPPER);
		String toolId = "";
		AgentTool tool = toolService.getByName(toolName);
		if (tool == null) {
			log.error("工具 no exist: toolName={}", toolName);
			return Flux.just(AgentExecutionEvent.error("工具不存在: " + toolName));
		}
		toolId = tool.getId();

		switch (toolApprovalMode) {
			case AUTO:
				log.warn("工具是否执行自动未实现: {}", toolName);
				return Flux.empty();

			case REQUIRE_APPROVAL:
				log.warn("工具执行需要审批（未实现）: {}, toolId: {}", toolName, toolId);
				return Flux.empty();

			case DISABLED:
			default:
				context.setToolArgs(args);
				return mapToolResultToEvent(toolService.executeToolAsync(toolName, context), context, toolId,
						toolCallId, toolName)
					.flux();
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

}
