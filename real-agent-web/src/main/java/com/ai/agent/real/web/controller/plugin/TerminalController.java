package com.ai.agent.real.web.controller.plugin;

import com.ai.agent.real.contract.plugin.terminal.ICommandService;
import com.ai.agent.real.contract.plugin.terminal.ITerminalService;
import com.ai.agent.real.contract.model.protocol.ResponseResult;
import com.ai.agent.real.contract.model.terminal.TerminalCommandRequest;
import com.ai.agent.real.contract.model.terminal.TerminalCommandResult;
import com.ai.agent.real.domain.entity.plguin.TerminalCommand;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * 终端命令API控制器
 *
 * @author han
 * @time 2025/10/23 23:39
 */
@RestController
@RequestMapping("/api/terminal")
public class TerminalController {

	private final ITerminalService terminalService;

	private final ICommandService commandService;

	public TerminalController(ITerminalService terminalService, ICommandService commandService) {
		this.terminalService = terminalService;
		this.commandService = commandService;
	}

	/**
	 * 执行命令 - 网关接口
	 * @param request 命令请求
	 * @return 执行结果
	 */
	@PostMapping("/gateway")
	public Mono<ResponseResult<TerminalCommandResult>> executeCommand(@RequestBody TerminalCommandRequest request) {
		return terminalService.execute(request).map(ResponseResult::success);
	}

	/**
	 * 获取所有命令配置
	 * @return 命令列表
	 */
	@GetMapping("/commands")
	public Mono<ResponseResult<List<TerminalCommand>>> getAllCommands(@RequestParam(required = false) String category,
			@RequestParam(required = false) Boolean enabled) {
		if (category != null && !category.isEmpty()) {
			return commandService.getCommandsByCategory(category).collectList().map(ResponseResult::success);
		}
		return commandService.getAllCommands().collectList().map(ResponseResult::success);
	}

	/**
	 * 获取可见的命令（用于前端显示）
	 * @return 命令列表
	 */
	@GetMapping("/commands/visible")
	public Mono<ResponseResult<List<TerminalCommand>>> getVisibleCommands() {
		return commandService.getVisibleCommands().collectList().map(ResponseResult::success);
	}

	/**
	 * 获取单个命令详情
	 * @param name 命令名称
	 * @return 命令详情
	 */
	@GetMapping("/commands/{name}")
	public Mono<ResponseResult<TerminalCommand>> getCommand(@PathVariable String name) {
		return commandService.getCommandByName(name)
			.map(ResponseResult::success)
			.switchIfEmpty(Mono.just(ResponseResult.notFound()));
	}

	/**
	 * 更新命令（管理员）
	 * @param name 命令名称
	 * @param command 命令配置
	 * @return 更新后的命令
	 */
	@PutMapping("/commands/{name}")
	public Mono<ResponseResult<TerminalCommand>> updateCommand(@PathVariable String name,
			@RequestBody TerminalCommand command) {
		return commandService.updateCommand(name, command)
			.map(result -> ResponseResult.success("命令更新成功", result))
			.onErrorResume(e -> Mono.just(ResponseResult.error(e.getMessage())));
	}

	/**
	 * 删除命令（管理员）
	 * @param name 命令名称
	 * @return 删除结果
	 */
	@DeleteMapping("/commands/{name}")
	public Mono<ResponseResult<Void>> deleteCommand(@PathVariable String name) {
		return commandService.deleteCommand(name)
			.then(Mono.just(ResponseResult.<Void>success("命令删除成功", null)))
			.onErrorResume(e -> Mono.just(ResponseResult.error(e.getMessage())));
	}

	/**
	 * 启用/禁用命令（管理员）
	 * @param name 命令名称
	 * @param enabled 是否启用
	 * @return 更新后的命令
	 */
	@PatchMapping("/commands/{name}/toggle")
	public Mono<ResponseResult<TerminalCommand>> toggleCommand(@PathVariable String name,
			@RequestParam boolean enabled) {
		return commandService.toggleCommand(name, enabled)
			.map(result -> ResponseResult.success(enabled ? "命令已启用" : "命令已禁用", result))
			.onErrorResume(e -> Mono.just(ResponseResult.error(e.getMessage())));
	}

}
