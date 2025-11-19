package com.ai.agent.real.contract.service;

import com.ai.agent.real.contract.model.property.*;

import java.time.*;

/**
 * 配置服务接口 通过服务模式封装配置访问，更好地控制配置的获取和更新
 *
 * @author han
 * @time 2025/10/12
 */
public interface IPropertyService {

	/**
	 * 获取 Agent 执行超时时间
	 */
	Duration getAgentExecutionTimeout();

	/**
	 * 获取工具审批模式
	 */
	ToolApprovalMode getToolApprovalMode();

	/**
	 * 获取内存压缩模式
	 */
	ContextZipMode getContextZipMode();

}