package com.ai.agent.real.contract.service.terminal;

import com.ai.agent.real.contract.model.terminal.TerminalCommandRequest;
import com.ai.agent.real.contract.model.terminal.TerminalCommandResult;
import reactor.core.publisher.Mono;

/**
 * 终端命令处理器接口
 * <p>
 * 所有命令处理器必须实现此接口
 * </p>
 *
 * @author Real Agent Team
 * @since 2025-01-23
 */
public interface TerminalCommandHandler {

	/**
	 * 获取处理器名称
	 * <p>
	 * 必须与数据库中的handler字段对应
	 * </p>
	 * @return 处理器名称
	 */
	String getName();

	/**
	 * 获取支持的命令名称
	 * @return 命令名称
	 */
	String getCommandName();

	/**
	 * 执行命令
	 * @param request 命令请求
	 * @return 执行结果
	 */
	Mono<TerminalCommandResult> execute(TerminalCommandRequest request);

	/**
	 * 验证命令参数
	 * @param request 命令请求
	 * @return 验证结果，如果验证失败返回错误信息
	 */
	default Mono<String> validate(TerminalCommandRequest request) {
		return Mono.empty(); // 默认通过验证
	}

	/**
	 * 获取命令帮助信息
	 * @return 帮助信息
	 */
	String getHelp();

	/**
	 * 判断是否支持该命令
	 * @param commandName 命令名称
	 * @return 是否支持
	 */
	default boolean supports(String commandName) {
		return getCommandName().equalsIgnoreCase(commandName);
	}

}
