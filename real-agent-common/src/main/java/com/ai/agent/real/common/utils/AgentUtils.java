package com.ai.agent.real.common.utils;

import com.ai.agent.real.contract.spec.*;
import com.ai.agent.real.contract.spec.message.*;
import lombok.extern.slf4j.*;
import org.springframework.ai.chat.messages.*;
import org.springframework.ai.chat.messages.AssistantMessage.*;
import org.springframework.ai.chat.messages.ToolResponseMessage.*;
import org.springframework.ai.chat.prompt.*;
import org.springframework.ai.model.tool.*;

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
					ToolResponseMessage toolResponseMessage = new ToolResponseMessage(
							List.of(new ToolResponse(id, toolName, responseData)));
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
	public static Prompt buildPromptWithContextAndTools(List<AgentTool> availableTools, AgentContext context,
			String systemPrompt, String userPrompt) throws NoSuchMethodException {

		log.debug("开始构建Prompt，可用工具数量: {}, 对话历史数量: {}", availableTools != null ? availableTools.size() : 0,
				context.getConversationHistory().size());

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
	public static Prompt buildPromptWithContext(List<AgentTool> availableTools, AgentContext context,
			String systemPrompt, String userPrompt) {

		log.debug("开始构建Prompt, 对话历史数量: {}", context.getConversationHistory().size());

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
	private static List<Message> buildConversation(List<AgentTool> availableTools, AgentContext context,
			String systemPrompt, String userPrompt) {

		// 1. prepare a empty message container and a conversation history
		List<Message> messages = new ArrayList<>();
		List<AgentMessage> conversationHistory = context.getConversationHistory();
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

		messages.add(new UserMessage(userPrompt));

		log.debug("Message list build completed，total message size: {}", messages.size());

		// 3. return the completed message list
		return messages;
	}

}
