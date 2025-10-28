package com.ai.agent.real.application.utils;

import com.ai.agent.real.common.utils.*;
import com.ai.agent.real.entity.agent.context.ReActAgentContext;
import com.ai.agent.real.contract.model.logging.*;
import com.ai.agent.real.contract.model.message.*;
import com.ai.agent.real.contract.tool.AgentTool;
import io.micrometer.common.util.*;
import lombok.extern.slf4j.*;
import org.springframework.ai.chat.messages.*;
import org.springframework.ai.chat.messages.AssistantMessage.*;
import org.springframework.ai.chat.messages.ToolResponseMessage.*;
import org.springframework.ai.chat.prompt.*;
import org.springframework.ai.model.tool.*;

import java.time.*;
import java.util.*;
import java.util.stream.*;

import static com.ai.agent.real.common.constant.NounConstants.*;

/**
 * @author han
 * @time 2025/9/10 23:28
 */
@Slf4j
public class AgentUtils {

	/**
	 * 将AgentMessage列表转换为Spring AI消息列表
	 * @param agentMessages AgentMessage列表
	 * @return Spring AI消息列表
	 */

	public static List<Message> toSpringAiMessages(List<AgentMessage> agentMessages) {
		log.debug("开始转换AgentMessage列表，总数: {}", agentMessages.size());

		return agentMessages.stream().map(agentMessage -> {
			log.debug("处理AgentMessage: type={}, text={}, metadata={}", agentMessage.getAgentMessageType(),
					agentMessage.getText() != null
							? agentMessage.getText().substring(0, Math.min(50, agentMessage.getText().length())) + "..."
							: "null",
					agentMessage.getMetadata());

			switch (agentMessage.getMessageType()) {
				case SYSTEM:
					log.debug("创建SystemMessage");
					return new SystemMessage(agentMessage.getText());
				case USER:
					log.debug("创建UserMessage");
					return UserMessage.builder().text(agentMessage.getText()).build();
				case ASSISTANT:
					log.debug("创建AssistantMessage");

					/**
					 *
					 * public ToolCall(String id, String type, String name, String
					 * arguments) { this.id = id; this.type = type; this.name = name;
					 * this.arguments = arguments; }
					 */

					return AssistantMessage.builder()
						.content(agentMessage.getText())
						.toolCalls((List<ToolCall>) agentMessage.getMetadata().get("tool_calls"))
						.build();
				case TOOL:
					// log.debug("开始处理TOOL类型消息");
					Map<String, Object> metadata = agentMessage.getMetadata();
					// log.debug("TOOL消息metadata: {}", metadata);
					String id = metadata.get("id").toString();
					String toolName = metadata.get("name").toString();
					String responseData = metadata.get("responseData").toString();
					ToolResponseMessage toolResponseMessage = ToolResponseMessage.builder()
						.responses(List.of(new ToolResponse(id, toolName, responseData)))
						.build();
					log.info("开始处理TOOL类型消息，id: {}, toolName: {}, responseData: {}", id, toolName, responseData);
					log.debug("成功创建ToolResponseMessage");
					return toolResponseMessage;
				// return new AssistantMessage("调用工具" + toolName + "，结果：" +
				// metadata.get("responseData").toString());

				default:
					log.debug("使用默认AssistantMessage处理未知类型: {}", agentMessage.getAgentMessageType());
					return new AssistantMessage(agentMessage.getText());
			}
		}).collect(Collectors.toList());
	}

	/**
	 * build prompt with context and tools, enable function calling
	 * @param availableTools available tools
	 * @param context agent context
	 * @param systemPrompt system prompt
	 * @param userPrompt user prompt
	 * @return Prompt
	 * @throws NoSuchMethodException
	 */
	public static Prompt buildPromptWithContextAndTools(List<AgentTool> availableTools, ReActAgentContext context,
			String systemPrompt, String userPrompt) throws NoSuchMethodException {

		log.debug("开始构建Prompt，可用工具数量: {}, 对话历史数量: {}", availableTools != null ? availableTools.size() : 0,
				context.getMessageHistory().size());

		// 配置工具调用选项, 使用原生function calling
		var optionsBuilder = DefaultToolCallingChatOptions.builder();
		if (availableTools != null && !availableTools.isEmpty()) {
			log.debug("配置工具调用选项，工具列表: {}",
					availableTools.stream()
						.map((agentTool -> agentTool.getSpec().getName()))
						.collect(Collectors.toList()));
			optionsBuilder.toolCallbacks(ToolUtils.convertAgentTool2ToolCallback(availableTools));
			optionsBuilder.internalToolExecutionEnabled(false);
		}

		var options = optionsBuilder.build();

		// 构建消息
		List<Message> messages = buildConversation(availableTools, context, systemPrompt, userPrompt);

		return new Prompt(messages, options);
	}

	/**
	 * build prompt with context but without function calling
	 * @param availableTools available tools
	 * @param context agent context
	 * @param systemPrompt system prompt
	 * @param userPrompt user prompt
	 * @return Prompt
	 */
	public static Prompt buildPromptWithContext(List<AgentTool> availableTools, ReActAgentContext context,
			String systemPrompt, String userPrompt) {

		log.debug("开始构建Prompt, 对话历史数量: {}", context.getMessageHistory().size());

		// 构建消息
		List<Message> messages = buildConversation(availableTools, context, systemPrompt, userPrompt);

		return new Prompt(messages);
	}

	/**
	 * build a message list by system prompt + conversation history + user prompt
	 * @param context agent context
	 * @param systemPrompt
	 * @param userPrompt
	 * @return List<Message> completed message list
	 */
	private static List<Message> buildConversation(List<AgentTool> availableTools, ReActAgentContext context,
			String systemPrompt, String userPrompt) {

		// 1. prepare a empty message container and a conversation history
		List<Message> messages = new ArrayList<>();
		List<AgentMessage> conversationHistory = context.getMessageHistory();
		log.debug("对话历史详情:");

		// 2. concat the message list,logic: system prompt + conversation history + user
		// prompt
		for (int i = 0; i < conversationHistory.size(); i++) {
			AgentMessage msg = conversationHistory.get(i);
			log.debug("[{}] type={}, sender={}, text={}", i, msg.getAgentMessageType(), msg.getSenderId(),
					msg.getText() != null ? msg.getText().substring(0, Math.min(100, msg.getText().length())) + "..."
							: "null");
		}

		String systemPromptWithTools = PromptUtils.renderToolList(systemPrompt, availableTools, TOOLS_TAG);

		messages.add(new SystemMessage(systemPromptWithTools));

		log.debug("开始转换对话历史为Spring AI消息");
		List<Message> convertedMessages = AgentUtils.toSpringAiMessages(conversationHistory);
		messages.addAll(convertedMessages);

		if (StringUtils.isNotBlank(userPrompt)) {
			messages.add(new UserMessage(userPrompt));
		}

		log.debug("Message list build completed，total message size: {}", messages.size());

		// 3. return the completed message list
		return messages;
	}

	/**
	 * 为Agent创建独立的执行上下文副本
	 */
	public static ReActAgentContext createAgentContext(ReActAgentContext originalContext, String agentId) {
		ReActAgentContext newContext = new ReActAgentContext(new TraceInfo());

		// 独立的 TraceInfo：逐字段复制，避免共享同一个 TraceInfo 对象
		newContext.setSessionId(originalContext.getSessionId());
		newContext.setTurnId(originalContext.getTurnId());
		newContext.setSpanId(originalContext.getSpanId());
		// start/end time 由各 Agent 生命周期自行设置，这里不复制 endTime
		newContext.setEndTime(null);

		// 复制对话历史与参数（浅拷贝集合内容），确保不共享可变集合引用
		newContext.setMessageHistory(originalContext.getMessageHistory());
		newContext.setToolArgs(originalContext.getToolArgs());
		newContext.setCurrentIteration(originalContext.getCurrentIteration());
		newContext.setTaskCompleted(originalContext.getTaskCompleted());

		// 为新上下文设置独立的 Agent 与 node 标识
		newContext.setAgentId(agentId);
		newContext.setNodeId(CommonUtils.getNodeId());
		newContext.setStartTime(LocalDateTime.now());

		return newContext;
	}

	/**
	 * 打印上下文快照，辅助排查“模型未遵循上下文”的问题。
	 */
	public static String snapshot(ReActAgentContext ctx) {
		try {
			int msgSize = ctx.getMessageHistory() != null ? ctx.getMessageHistory().size() : 0;
			String lastMsg = "";
			if (msgSize > 0) {
				Object tail = ctx.getMessageHistory().get(msgSize - 1);
				lastMsg = safeHead(String.valueOf(tail), 200);
			}
			String toolArgKeys = "";
			if (ctx.getToolArgs() != null) {
				toolArgKeys = String.join(",", ctx.getToolArgs().toString());
			}
			return String.format(
					"session=%s trace=%s node=%s agent=%s iter=%d done=%s msgs=%d last=%s toolArgKeys=[%s]",
					ctx.getSessionId(), ctx.getTurnId(), ctx.getNodeId(), ctx.getAgentId(), ctx.getCurrentIteration(),
					ctx.isTaskCompleted(), msgSize, lastMsg, toolArgKeys);
		}
		catch (Exception e) {
			return "<snapshot-error>";
		}
	}

	public static String safeHead(String s, int max) {
		if (s == null) {
			return "";
		}
		String t = s.replaceAll("\n", " ");
		return t.length() > max ? t.substring(0, max) + "..." : t;
	}

}
