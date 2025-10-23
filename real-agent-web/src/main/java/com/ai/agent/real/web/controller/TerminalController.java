package com.ai.agent.real.web.controller;

import com.ai.agent.real.application.service.terminal.TerminalGatewayService;
import com.ai.agent.real.contract.model.terminal.TerminalCommandRequest;
import com.ai.agent.real.contract.model.terminal.TerminalCommandResult;
import com.ai.agent.real.contract.service.terminal.TerminalCommandService;
import com.ai.agent.real.domain.entity.TerminalCommand;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * 终端命令API控制器
 *
 * @author Real Agent Team
 * @since 2025-01-23
 */
@RestController
@RequestMapping("/api/terminal")
public class TerminalController {

	private final TerminalGatewayService gatewayService;

	private final TerminalCommandService commandService;

	public TerminalController(TerminalGatewayService gatewayService, TerminalCommandService commandService) {
		this.gatewayService = gatewayService;
		this.commandService = commandService;
	}

	/**
	 * 执行命令 - 网关接口
	 * @param request 命令请求
	 * @return 执行结果
	 */
	@PostMapping("/gateway")
	public Mono<TerminalCommandResult> executeCommand(@RequestBody TerminalCommandRequest request) {
		return gatewayService.execute(request);
	}

	/**
	 * 获取所有命令配置
	 * @return 命令列表
	 */
	@GetMapping("/commands")
	public Flux<TerminalCommand> getAllCommands(@RequestParam(required = false) String category,
			@RequestParam(required = false) Boolean enabled) {
		if (category != null && !category.isEmpty()) {
			return commandService.getCommandsByCategory(category);
		}
		return commandService.getAllCommands();
	}

	/**
	 * 获取可见的命令（用于前端显示）
	 * @return 命令列表
	 */
	@GetMapping("/commands/visible")
	public Flux<TerminalCommand> getVisibleCommands() {
		return commandService.getVisibleCommands();
	}

	/**
	 * 获取单个命令详情
	 * @param name 命令名称
	 * @return 命令详情
	 */
	@GetMapping("/commands/{name}")
	public Mono<TerminalCommand> getCommand(@PathVariable String name) {
		return commandService.getCommandByName(name);
	}

	/**
	 * 创建新命令（管理员）
	 * @param command 命令配置
	 * @return 创建的命令
	 */
	@PostMapping("/commands")
	public Mono<TerminalCommand> createCommand(@RequestBody TerminalCommand command) {
		return commandService.createCommand(command);
	}

	/**
	 * 更新命令（管理员）
	 * @param name 命令名称
	 * @param command 命令配置
	 * @return 更新后的命令
	 */
	@PutMapping("/commands/{name}")
	public Mono<TerminalCommand> updateCommand(@PathVariable String name, @RequestBody TerminalCommand command) {
		return commandService.updateCommand(name, command);
	}

	/**
	 * 删除命令（管理员）
	 * @param name 命令名称
	 * @return 删除结果
	 */
	@DeleteMapping("/commands/{name}")
	public Mono<Void> deleteCommand(@PathVariable String name) {
		return commandService.deleteCommand(name);
	}

	/**
	 * 启用/禁用命令（管理员）
	 * @param name 命令名称
	 * @param enabled 是否启用
	 * @return 更新后的命令
	 */
	@PatchMapping("/commands/{name}/toggle")
	public Mono<TerminalCommand> toggleCommand(@PathVariable String name, @RequestParam boolean enabled) {
		return commandService.toggleCommand(name, enabled);
	}

}
