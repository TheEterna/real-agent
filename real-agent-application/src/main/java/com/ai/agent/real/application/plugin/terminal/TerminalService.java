package com.ai.agent.real.application.plugin.terminal;

import com.ai.agent.real.application.agent.strategy.ReActAgentStrategy;
import com.ai.agent.real.application.plugin.terminal.handler.DefaultCommandHandler;
import com.ai.agent.real.contract.plugin.terminal.ICommandService;
import com.ai.agent.real.contract.plugin.terminal.ITerminalService;
import com.ai.agent.real.contract.model.terminal.TerminalCommandRequest;
import com.ai.agent.real.contract.model.terminal.TerminalCommandResult;
import com.ai.agent.real.contract.plugin.terminal.TerminalCommandHandler;
import com.ai.agent.real.common.entity.TerminalCommand;
import com.ai.agent.real.domain.repository.ITerminalCommandRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 终端命令网关服务实现
 *
 * @author Real Agent Team
 * @since 2025-01-23
 */
public class TerminalService implements ITerminalService {

	private static final Logger logger = LoggerFactory.getLogger(TerminalService.class);

	private final Map<String, TerminalCommandHandler> handlers;

	private final ICommandService commandService;

	public TerminalService(ICommandService commandService, List<TerminalCommandHandler> handlerList) {

		this.commandService = commandService;
		// 构建处理器映射
		this.handlers = handlerList.stream()
			.collect(Collectors.toMap(TerminalCommandHandler::getCommandName, Function.identity()));

		logger.info("Registered {} terminal command handlers: {}", handlers.size(), handlers.keySet());
	}

	@Override
	public Mono<TerminalCommandResult> execute(TerminalCommandRequest request) {
		long startTime = System.currentTimeMillis();

		return commandService.getCommandByName(request.getCommandName()).switchIfEmpty(Mono.defer(() -> {
			// 记录切换到默认策略的日志
			logger.info("Command '{}' not found, using default strategy: {}", request.getCommandName(),
					ReActAgentStrategy.class.getSimpleName());
			// 返回备用的命令流
			return commandService.getCommandByName(DefaultCommandHandler.class.getSimpleName());
		}))
			.flatMap(command -> validateAndExecute(request, command, startTime))
			.doOnSuccess(result -> logger.info("Command executed successfully: {} in {}ms", request.getCommandName(),
							System.currentTimeMillis() - startTime))
			.doOnError(error -> logger.error("Command execution failed: {} - {}", request.getCommandName(),
					error.getMessage()));
	}

	private Mono<TerminalCommandResult> validateAndExecute(TerminalCommandRequest request, TerminalCommand command,
			long startTime) {

		// 2. 连接状态检查
		return Mono.defer(() -> {

			// 3. 查找处理器
			TerminalCommandHandler handler = handlers.get(command.getName());
			if (handler == null) {
				return Mono.just(TerminalCommandResult.error("命令处理器未找到: " + command.getHandler(), "请联系管理员检查系统配置"));
			}
			// 参数校验，即判断 用户指定的参数 在该命令参数列表中是否存在

			// 具体命令验证
			return handler.validate(request).flatMap(validationError -> {
				if (validationError != null && !validationError.isEmpty()) {
					return Mono.just(TerminalCommandResult.error("参数验证失败: " + validationError));
				}

				// 5. 执行命令
				return handler.execute(request);
			});
		}).onErrorResume(error -> {
			// 记录错误历史
			TerminalCommandResult errorResult = TerminalCommandResult.error(error.getMessage());
			return Mono.just(errorResult);
		});
	}

}