package com.ai.agent.real.contract.model.message;

import com.ai.agent.real.contract.model.logging.*;
import com.fasterxml.jackson.annotation.*;
import lombok.*;
import org.springframework.ai.chat.messages.*;

import java.time.*;
import java.util.*;

/**
 * Agent系统的消息类，扩展Spring AI的Message接口 支持Agent间的对话历史记录和上下文管理
 *
 * @author han
 * @time 2025/9/9 12:05
 */

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class AgentMessage extends TraceInfo implements Message {

	/**
	 * 消息内容
	 */
	private String content;

	/**
	 * 消息类型
	 */
	private AgentMessageType messageType;

	/**
	 * 发送者Agent ID
	 */
	private String senderId;

	/**
	 * 消息时间戳
	 */
	private LocalDateTime timestamp;

	/**
	 * 迭代轮次（用于ReAct框架）
	 */
	private Integer iteration;

	/**
	 * 消息元数据
	 */
	private Map<String, Object> metadata;

	/**
	 * 构造函数 - 创建基本消息
	 */
	public AgentMessage(String content, AgentMessageType messageType, String senderId, Map<String, Object> metadata) {
		this.content = content;
		this.messageType = messageType;
		this.timestamp = LocalDateTime.now();
		this.senderId = senderId;
		this.metadata = metadata;
	}

	/**
	 * 构造函数 - 创建基本消息
	 */
	public AgentMessage(String content, AgentMessageType messageType, String senderId) {
		this.content = content;
		this.messageType = messageType;
		this.timestamp = LocalDateTime.now();
		this.senderId = senderId;
		this.metadata = new HashMap<>();
	}

	@Override
	public String getText() {
		return content;
	}

	@Override
	public Map<String, Object> getMetadata() {
		return metadata;
	}

	@Override
	public MessageType getMessageType() {
		return messageType.toSpringAIMessageType();
	}

	/**
	 * 获取Agent系统的消息类型
	 */
	public AgentMessageType getAgentMessageType() {
		return messageType;
	}

	/**
	 * 创建系统消息
	 */
	public static AgentMessage system(String content) {
		return new AgentMessage(content, AgentMessageType.SYSTEM, "system");
	}

	/**
	 * 创建用户消息
	 */
	public static AgentMessage user(String content, String userId) {
		return new AgentMessage(content, AgentMessageType.USER, userId);
	}

	/**
	 * 创建助手消息
	 */
	public static AgentMessage assistant(String content, String agentId) {
		return new AgentMessage(content, AgentMessageType.ASSISTANT, agentId);
	}

	/**
	 * 创建思考消息
	 */
	public static AgentMessage thinking(String content, String agentId) {
		return new AgentMessage(content, AgentMessageType.THINKING, agentId);
	}

	/**
	 * 创建行动消息
	 */
	public static AgentMessage action(String content, String agentId) {
		return new AgentMessage(content, AgentMessageType.ACTION, agentId);
	}

	/**
	 * 创建结束消息
	 */
	public static AgentMessage completed(String content, String agentId) {
		return new AgentMessage(content, AgentMessageType.COMPLETED, agentId);
	}

	/**
	 * 创建观察消息
	 */
	public static AgentMessage observing(String content, String agentId) {
		return new AgentMessage(content, AgentMessageType.OBSERVING, agentId);
	}

	/**
	 * 创建任务分析消息
	 */
	public static AgentMessage taskAnalysis(String content, String agentId) {
		return new AgentMessage(content, AgentMessageType.TASK_ANALYSIS, agentId);
	}

	/**
	 * 创建思维链消息
	 */
	public static AgentMessage thought(String content, String agentId) {
		return new AgentMessage(content, AgentMessageType.THOUGHT, agentId);
	}

	/**
	 * 创建错误消息
	 */
	public static AgentMessage error(String content, String agentId) {
		return new AgentMessage(content, AgentMessageType.ERROR, agentId);
	}

	/**
	 * 创建工具消息
	 */
	public static AgentMessage tool(String content, String agentId, Map<String, Object> metadata) {
		return new AgentMessage(content, AgentMessageType.TOOL, agentId, metadata);
	}

	/**
	 * 消息类型枚举，扩展Spring AI的MessageType
	 */
	public enum AgentMessageType {

		// Spring AI标准类型
		SYSTEM("system"), USER("user"), ASSISTANT("assistant"), TOOL("tool"),

		// Agent系统扩展类型
		THINKING("thinking"), // 思考阶段消息
		ACTION("action"), // 行动阶段消息
		OBSERVING("observing"), // 观察阶段消息
		ERROR("error"), // 错误消息
		COMPLETED("completed"), // 结束消息
		TASK_ANALYSIS("task_analysis"), // 任务分析消息
		THOUGHT("thought");

		private final String value;

		AgentMessageType(String value) {
			this.value = value;
		}

		public String getValue() {
			return value;
		}

		/**
		 * 转换为Spring AI的MessageType 使用 if-else 替代 switch 以避免潜在的匿名内部类生成（如 AgentMessage$1）
		 */
		public MessageType toSpringAIMessageType() {
			if (this == SYSTEM) {
				return MessageType.SYSTEM;
			}
			else if (this == USER) {
				return MessageType.USER;
			}
			else if (this == TOOL) {
				return MessageType.TOOL;
			}
			else if (this == ASSISTANT) {
				return MessageType.ASSISTANT;
			}
			else {
				return MessageType.ASSISTANT;
			}
		}

	}

}
