package com.ai.agent.real.domain.repository;

import com.ai.agent.real.domain.entity.TerminalCommand;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * 终端命令配置Repository
 *
 * @author Real Agent Team
 * @since 2025-01-23
 */
@Repository
public interface TerminalCommandRepository extends R2dbcRepository<TerminalCommand, String> {

	/**
	 * 根据命令名称查找
	 * @param name 命令名称
	 * @return 命令配置
	 */
	Mono<TerminalCommand> findByName(String name);

	/**
	 * 根据类别查找所有命令
	 * @param category 命令类别
	 * @return 命令列表
	 */
	Flux<TerminalCommand> findByCategory(String category);

	/**
	 * 查找所有启用的命令
	 * @return 启用的命令列表
	 */
	Flux<TerminalCommand> findByEnabledTrue();

	/**
	 * 查找所有启用且不隐藏的命令
	 * @return 可见的命令列表
	 */
	@Query("SELECT * FROM terminal_commands WHERE enabled = TRUE AND (hidden IS NULL OR hidden = FALSE)")
	Flux<TerminalCommand> findVisibleCommands();

	/**
	 * 根据类别和启用状态查找
	 * @param category 命令类别
	 * @param enabled 是否启用
	 * @return 命令列表
	 */
	Flux<TerminalCommand> findByCategoryAndEnabled(String category, Boolean enabled);

	/**
	 * 根据权限级别查找
	 * @param permission 权限级别
	 * @return 命令列表
	 */
	Flux<TerminalCommand> findByPermission(String permission);

	/**
	 * 检查命令是否存在
	 * @param name 命令名称
	 * @return 是否存在
	 */
	@Query("SELECT EXISTS(SELECT 1 FROM terminal_commands WHERE name = :name)")
	Mono<Boolean> existsByName(String name);

}
