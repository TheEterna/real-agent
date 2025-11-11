package com.ai.agent.real.contract.agent;

import com.ai.agent.real.contract.agent.context.AgentContextAble;
import com.ai.agent.real.contract.model.property.*;
import com.ai.agent.real.contract.model.protocol.*;
import com.ai.agent.real.contract.tool.AgentTool;
import com.ai.agent.real.contract.tool.IToolService;
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
	 * 关键词列表，筛选工具
	 */
	protected Set<String> keywords;

	protected String systemPrompt;

	protected List<AgentTool> availableTools;

	/**
	 * 聊天模型实例
	 */
	protected ChatModel chatModel;

	/**
	 * Agent的专业领域/能力标签
	 */
	protected String[] capabilities;

	protected IToolService toolService;

	protected ToolApprovalMode toolApprovalMode;

	public Agent() {

	}

	/**
	 * 构造函数
	 */
	protected Agent(String agentId, String agentName, String description, ChatModel chatModel, IToolService toolService,
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
	}

	/**
	 * 构造函数
	 */
	protected Agent(String agentId, String agentName, String description, ChatModel chatModel, IToolService toolService,
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

	}

	/**
	 * 流式执行任务
	 * @param task 任务描述
	 * @param context 执行上下文
	 * @return 流式执行结果
	 */
	public abstract Flux<AgentExecutionEvent> executeStream(String task, AgentContextAble context);

	/**
	 * after handle of executeStream method
	 */
	protected void afterHandle(AgentContextAble context) {
		context.setEndTime(LocalDateTime.now());
	}

}
