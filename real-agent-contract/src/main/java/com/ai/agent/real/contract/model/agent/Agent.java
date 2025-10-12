package com.ai.agent.real.contract.model.agent;

import com.ai.agent.real.contract.model.*;
import com.ai.agent.real.contract.model.context.*;
import com.ai.agent.real.contract.model.property.*;
import com.ai.agent.real.contract.model.protocol.*;
import com.ai.agent.real.contract.service.*;
import lombok.*;
import lombok.extern.slf4j.*;
import org.springframework.ai.chat.model.*;
import reactor.core.publisher.*;

import java.time.*;
import java.util.*;

/**
 * Agent 抽象基类，定义了所有Agent的基本行为规范
 *
 * @author han
 * @time 2025/9/5 10:32
 */
@Slf4j
@Data
public abstract class Agent {

	/**
	 * Agent的唯一标识符
	 */
	protected String agentId;

	/**
	 * Agent的名称
	 */
	protected String agentName;

	/**
	 * Agent的描述信息
	 */
	protected String description;

	/**
	 * 关键词列表，用于描述该Agent的能力
	 */
	protected Set<String> keywords;

	protected String SYSTEM_PROMPT;

	protected List<AgentTool> availableTools;

	/**
	 * 聊天模型实例
	 */
	protected ChatModel chatModel;

	/**
	 * Agent的专业领域/能力标签
	 */
	protected String[] capabilities;

	protected ToolService toolService;

	protected ToolApprovalMode toolApprovalMode;

	public Agent() {

	}

	/**
	 * 构造函数
	 */
	protected Agent(String agentId, String agentName, String description, ChatModel chatModel, ToolService toolService,
			Set<String> keywords, ToolApprovalMode toolApprovalMode) {
		this.agentId = agentId;
		this.agentName = agentName;
		this.description = description;
		this.chatModel = chatModel;
		this.toolService = toolService;
		this.keywords = keywords;
		// 需要使用 toolService 去判断有没有工具
		this.availableTools = toolService.getToolsByKeywords(this.keywords);
		this.toolApprovalMode = toolApprovalMode != null ? toolApprovalMode : ToolApprovalMode.AUTO;

		// if (toolList.isEmpty()) {
		// log.warn("Agent[{}] 没有可用的工具", agentId);
		// } else {
		// // 构造 tool 提示词
		// this.SYSTEM_PROMPT += "你可以根据场景判断是否使用工具, 你可以使用以下工具来处理任务：\n";
		// for (AgentTool tool : toolList) {
		// this.SYSTEM_PROMPT += "- " + tool.getSpec().getName() + ": " +
		// tool.getSpec().getDescription() + "\n";
		// }
		// }
	}

	/**
	 * 构造函数
	 */
	protected Agent(String agentId, String agentName, String description, ChatModel chatModel, ToolService toolService,
			Set<String> keywords) {
		this.agentId = agentId;
		this.agentName = agentName;
		this.description = description;
		this.chatModel = chatModel;
		this.toolService = toolService;
		this.keywords = keywords;
		// 需要使用 toolService 去判断有没有工具
		this.availableTools = toolService.getToolsByKeywords(this.keywords);
		this.toolApprovalMode = ToolApprovalMode.AUTO;

		// if (toolList.isEmpty()) {
		// log.warn("Agent[{}] 没有可用的工具", agentId);
		// } else {
		// // 构造 tool 提示词
		// this.SYSTEM_PROMPT += "你可以根据场景判断是否使用工具, 你可以使用以下工具来处理任务：\n";
		// for (AgentTool tool : toolList) {
		// this.SYSTEM_PROMPT += "- " + tool.getSpec().getName() + ": " +
		// tool.getSpec().getDescription() + "\n";
		// }
		// }
	}

	/**
	 * 流式执行任务
	 * @param task 任务描述
	 * @param context 执行上下文
	 * @return 流式执行结果
	 */
	public abstract Flux<AgentExecutionEvent> executeStream(String task, AgentContext context);

	/**
	 * after handle of executeStream method
	 */
	protected void afterHandle(AgentContext context) {
		context.setEndTime(LocalDateTime.now());
	}

	/**
	 * 判断当前Agent是否能够处理指定任务
	 * @param task 任务描述
	 * @return 是否能够处理
	 */
	public boolean canHandle(String task) {
		if (task == null || task.trim().isEmpty()) {
			return false;
		}
		if (keywords.contains("*")) {
			return true;
		}

		String lowerTask = task.toLowerCase();

		for (String keyword : keywords) {
			if (lowerTask.contains(keyword)) {
				return true;
			}
		}

		return false;
	}

	// /**
	// * 处理接收到的消息
	// */
	// protected void handleMessage(AgentMessage message) {
	// log.info("Agent {} received message: {}", agentId, message.getContent());
	//
	// try {
	// switch (message.getMessageType()) {
	// case REQUEST:
	// handleRequestMessage(message);
	// break;
	// case RESPONSE:
	// handleResponseMessage(message);
	// break;
	// case NOTIFICATION:
	// handleNotificationMessage(message);
	// break;
	// case COLLABORATION:
	// handleCollaborationMessage(message);
	// break;
	// default:
	// log.warn("Unknown message type: {}", message.getMessageType());
	// }
	// } catch (Exception e) {
	// log.error("Error handling message: {}", e.getMessage(), e);
	// // 发送错误响应
	// if (message.getMessageType() == REQUEST) {
	// sendErrorResponse(message, e.getMessage());
	// }
	// }
	// }
	// /**
	// * 处理响应消息
	// */
	// protected void handleResponseMessage(AgentMessage message) {
	// log.debug("Handling response message from {}", message.getSenderId());
	// // 子类可以重写此方法来处理响应
	// }
	//
	// /**
	// * 处理通知消息
	// */
	// protected void handleNotificationMessage(AgentMessage message) {
	// log.debug("Handling notification message from {}", message.getSenderId());
	// // 子类可以重写此方法来处理通知
	// }
	//
	// /**
	// * 处理协作消息
	// */
	// protected void handleCollaborationMessage(AgentMessage message) {
	// log.debug("Handling collaboration message from {}", message.getSenderId());
	// // 子类可以重写此方法来处理协作请求
	// }
	// /**
	// * 处理请求消息
	// */
	// protected void handleRequestMessage(AgentMessage message) {
	// log.debug("Handling request message from {}", message.getSenderId());
	//
	// // 检查是否能处理该请求
	// String content = message.getContent();
	// if (canHandle(content)) {
	// try {
	// // 创建临时工具上下文
	// AgentContext context = new AgentContext()
	// .setUserId("system")
	// .setSessionId(message.getTaskId())
	// .setTraceId(message.getMessageId());
	//
	// // 执行任务
	// AgentResult result = execute(content, context);
	//
	// // 发送响应
	// AgentMessage response = AgentMessage.createResponse(
	// agentId, message.getSenderId(),
	// result.isSuccess() ? result.getResult() : result.getErrorMessage(),
	// message.getTaskId()
	// );
	// response.getMetadata().put("success", result.isSuccess());
	//// response.getMetadata().put("confidence", result.getConfidenceScore());
	//
	//
	// } catch (Exception e) {
	// sendErrorResponse(message, e.getMessage());
	// }
	// } else {
	// // 无法处理，发送拒绝响应
	// AgentMessage response = AgentMessage.createResponse(
	// agentId, message.getSenderId(),
	// "无法处理该请求: " + content,
	// message.getTaskId()
	// );
	// response.getMetadata().put("success", false);
	// response.getMetadata().put("reason", "capability_mismatch");
	// }
	// }
	//
	//
	//
	//
	// /**
	// * 发送错误响应
	// */
	// private void sendErrorResponse(AgentMessage originalMessage, String errorMessage) {
	// AgentMessage errorResponse = AgentMessage.createResponse(
	// agentId, originalMessage.getSenderId(),
	// "处理错误: " + errorMessage,
	// originalMessage.getTaskId()
	// );
	// errorResponse.getMetadata().put("success", false);
	// errorResponse.getMetadata().put("error", errorMessage);
	// errorResponse.getMetadata().put("error_code", "AGENT_ERROR");
	// }

}
