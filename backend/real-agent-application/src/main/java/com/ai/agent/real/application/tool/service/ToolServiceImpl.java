package com.ai.agent.real.application.tool.service;

import com.ai.agent.real.contract.agent.context.AgentContextAble;
import com.ai.agent.real.contract.model.protocol.*;
import com.ai.agent.real.contract.model.protocol.ToolResult.*;
import com.ai.agent.real.contract.tool.AgentTool;
import com.ai.agent.real.application.utils.ToolUtils;
import com.ai.agent.real.contract.tool.IToolService;
import io.modelcontextprotocol.client.*;
import lombok.extern.slf4j.*;
import org.springframework.ai.mcp.*;
import reactor.core.publisher.*;

import java.util.*;
import java.util.concurrent.*;
import java.util.stream.*;

/**
 * extend ToolRegister(该类已经并入ToolService)
 * 基于关键词的工具注册器
 * 实现工具与关键词的绑定，支持Agent根据关键词获取可用工具
 *
 * @author han
 * @time 2025/9/7 01:20
 */

/**
 * tool service include system tool and mcp tool
 *
 * @author han
 * @time 2025/9/16 11:11
 */

@Slf4j
public class ToolServiceImpl implements IToolService {

	/**
	 * async mcp tool clients
	 * @param toolService
	 */
	private final List<McpAsyncClient> mcpAsyncClients;

	/**
	 * sync mcp tools
	 * @param mcpAsyncClients
	 */
	public ToolServiceImpl(List<McpAsyncClient> mcpAsyncClients) {
		this.mcpAsyncClients = mcpAsyncClients;

	}

	@Override
	public List<AgentTool> listNativeSystemTools() {
		return this.list();
	}

	@Override
	public Mono<List<AgentTool>> listAllAgentToolsCachedAsync() {
		return Mono.just(this.list());
	}

	@Override
	public Mono<List<AgentTool>> listAllAgentToolsRefreshAsync() {

		List<AgentTool> tools = new ArrayList<>(this.listNativeSystemTools());

		return Flux.fromIterable(mcpAsyncClients)
			.flatMap(mcpClient -> mcpClient.listTools()
				.map(response -> response.tools()
					.stream()
					.map(tool -> AsyncMcpToolCallback.builder()
						.mcpClient(mcpClient)
						.tool(tool)
						.prefixedToolName(tool.name())
						.build())
					.map(toolCallback -> ToolUtils.convertToolCallback2AgentTool(toolCallback, mcpClient))
					.collect(Collectors.toList())

				))
			// .map(toolCallbacksArray ->
			// ToolUtils.convertToolCallbacks2AgentTools(List.of(toolCallbacksArray)))
			.filter(list -> !list.isEmpty())
			// 使用 reduce 合并所有列表
			.reduce(tools, (accumulator, currentList) -> {
				// 将当前列表的元素添加到累加器中
				tools.addAll(currentList);
				// 返回更新后的累加器
				return accumulator;
			});
	}

	@Override
	public Mono<List<AgentTool>> listAllMCPToolsAsync() {

		List<AgentTool> tools = new ArrayList<>();

		return Flux.fromIterable(mcpAsyncClients)
			.flatMap(mcpClient -> mcpClient.listTools()
				.map(response -> response.tools()
					.stream()
					.map(tool -> AsyncMcpToolCallback.builder()
						.mcpClient(mcpClient)
						.tool(tool)
						.prefixedToolName(tool.name())
						.build())
					.map(toolCallback -> ToolUtils.convertToolCallback2AgentTool(toolCallback, mcpClient))
					.collect(Collectors.toList())

				))
			// .map(toolCallbacksArray ->
			// ToolUtils.convertToolCallbacks2AgentTools(List.of(toolCallbacksArray)))
			.filter(list -> !list.isEmpty())
			// 使用 reduce 合并所有列表
			.reduce(tools, (accumulator, currentList) -> {
				// 将当前列表的元素添加到累加器中
				tools.addAll(currentList);
				// 返回更新后的累加器
				return accumulator;
			});
	}

	private final Map<String, AgentTool> tools = new ConcurrentHashMap<>();

	private void register(AgentTool tool) {
		if (tool == null || tool.getSpec().getName() == null) {
			return;
		}

		tools.put(tool.getSpec().getName(), tool);
	}

	/**
	 * 关键词到工具的映射 key: 关键词, value: 工具名称集合
	 */
	private final Map<String, Set<String>> keywordToTools = new ConcurrentHashMap<>();

	/**
	 * 工具到关键词的映射 key: 工具名称, value: 关键词集合
	 */
	private final Map<String, Set<String>> toolToKeywords = new ConcurrentHashMap<>();

	/**
	 * get tool by name
	 */
	@Override
	public AgentTool getByName(String name) {
		return tools.get(name);
	}

	/**
	 * get tool by id
	 */
	@Override
	public AgentTool getById(String id) {
		return tools.get(id);
	}

	/**
	 * 获取所有已注册的工具
	 */
	public List<AgentTool> list() {
		return new ArrayList<>(tools.values());
	}

	/**
	 * 根据单个关键词获取工具
	 */
	@Override
	public List<AgentTool> getToolsByKeyword(String keyword) {
		return getToolsByKeywords(Set.of(keyword));
	}

	/**
	 * 注册工具并绑定关键词
	 */
	@Override
	public boolean registerToolWithKeywords(AgentTool tool, Set<String> keywords) {
		if (tool == null || keywords == null || keywords.isEmpty()) {
			return false;
		}
		// 先注册工具
		this.register(tool);

		String toolName = tool.getSpec().getName();

		// 建立关键词绑定
		toolToKeywords.put(toolName, new HashSet<>(keywords));

		for (String keyword : keywords) {
			keywordToTools.computeIfAbsent(keyword.toLowerCase(), k -> new HashSet<>()).add(toolName);
		}
		// 所有的 tool 都需要绑定到 * 上
		keywordToTools.computeIfAbsent("*", k -> new HashSet<>()).add(toolName);

		log.info("工具 [{}] 注册成功，绑定关键词: {}", toolName, keywords);
		return true;
	}

	/**
	 * 注册工具并绑定关键词
	 */
	@Override
	public boolean registerToolsWithKeywords(List<AgentTool> tools, Set<String> keywords) {
		if (tools == null || keywords == null || keywords.isEmpty()) {
			return false;
		}

		for (AgentTool tool : tools) {
			this.registerToolWithKeywords(tool, keywords);
		}

		return true;
	}

	/**
	 * execute tool Async
	 * @param toolName
	 * @return
	 */
	@Override
	public Mono<ToolResult> executeToolAsync(String toolName, AgentContextAble agentContext) {

		// DefaultToolCallingManager toolCallingManager =
		// ToolCallingManager.builder().build();

		AgentTool tool = this.getByName(toolName);
		if (tool == null) {
			return Mono.just(ToolResult.error(ToolResultCode.TOOL_NOT_FOUND, "工具不存在", toolName, -1));
		}
		return tool.executeAsync(agentContext);

	}

	/**
	 * 根据关键词获取可用工具
	 */
	@Override
	public List<AgentTool> getToolsByKeywords(Set<String> keywords) {
		Set<String> matchedToolIds = new HashSet<>();

		for (String keyword : keywords) {
			Set<String> tools = keywordToTools.get(keyword.toLowerCase());
			if (tools != null) {
				matchedToolIds.addAll(tools);
			}
		}

		return matchedToolIds.stream().map(this::getById).filter(Objects::nonNull).collect(Collectors.toList());
	}

}