package com.ai.agent.real.contract.service.terminal;

import com.ai.agent.real.domain.entity.TerminalCommand;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * 终端命令配置服务接口
 *
 * @author Real Agent Team
 * @since 2025-01-23
 */
public interface TerminalCommandService {

	/**
	 * 获取所有命令
	 * @return 命令列表
	 */
	Flux<TerminalCommand> getAllCommands();

	/**
	 * 获取可见的命令（启用且不隐藏）
	 * @return 命令列表
	 */
	Flux<TerminalCommand> getVisibleCommands();

	/**
	 * 根据命令名称查找
	 * @param name 命令名称
	 * @return 命令配置
	 */
	Mono<TerminalCommand> getCommandByName(String name);

	/**
	 * 根据类别查找命令
	 * @param category 命令类别
	 * @return 命令列表
	 */
	Flux<TerminalCommand> getCommandsByCategory(String category);

	/**
	 * 创建新命令
	 * @param command 命令配置
	 * @return 创建的命令
	 */
	Mono<TerminalCommand> createCommand(TerminalCommand command);

	/**
	 * 更新命令
	 * @param name 命令名称
	 * @param command 命令配置
	 * @return 更新后的命令
	 */
	Mono<TerminalCommand> updateCommand(String name, TerminalCommand command);

	/**
	 * 删除命令
	 * @param name 命令名称
	 * @return 删除结果
	 */
	Mono<Void> deleteCommand(String name);

	/**
	 * 启用/禁用命令
	 * @param name 命令名称
	 * @param enabled 是否启用
	 * @return 更新后的命令
	 */
	Mono<TerminalCommand> toggleCommand(String name, boolean enabled);

}
