package com.ai.agent.real.contract.service;

import com.ai.agent.real.contract.protocol.*;
import com.ai.agent.real.contract.spec.*;
import reactor.core.publisher.*;

import java.util.*;

/**
 * @author han
 * @time 2025/9/15 14:37
 */

public interface ToolService {

	/**
	 * 列出所有本地系统工具
	 */
	List<AgentTool> listNativeSystemTools();

	/**
	 * 异步列出所有缓存的智能体工具
	 */
	Mono<List<AgentTool>> listAllAgentToolsCachedAsync();

	/**
	 * 异步列出所有刷新后的智能体工具
	 */
	Mono<List<AgentTool>> listAllAgentToolsRefreshAsync();

	/**
	 * 异步列出所有MCP工具
	 */
	Mono<List<AgentTool>> listAllMCPToolsAsync();

	/**
	 * 根据id获取工具
	 */
	AgentTool getById(String id);

	/**
	 * 根据名称获取工具
	 */
	AgentTool getByName(String name);

	/**
	 * 获取所有注册的工具
	 */
	List<AgentTool> list();

	/**
	 * 根据关键词获取可用工具
	 */
	List<AgentTool> getToolsByKeywords(Set<String> keywords);

	/**
	 * 根据单个关键词获取工具
	 */
	List<AgentTool> getToolsByKeyword(String keyword);

	/**
	 * 注册工具并绑定关键词
	 */
	boolean registerToolWithKeywords(AgentTool tool, Set<String> keywords);

	/**
	 * 注册工具s并绑定关键词
	 */
	boolean registerToolsWithKeywords(List<AgentTool> tools, Set<String> keywords);

	/**
	 * execute tool
	 * @param toolId
	 * @param agentContext
	 * @return
	 */
	Mono<ToolResult<Object>> executeToolAsync(String toolId, AgentContext agentContext);

}
