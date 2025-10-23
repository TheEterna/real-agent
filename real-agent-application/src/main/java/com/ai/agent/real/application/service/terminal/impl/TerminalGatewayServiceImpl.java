package com.ai.agent.real.application.service.terminal.impl;

import com.ai.agent.real.application.service.terminal.TerminalGatewayService;
import com.ai.agent.real.contract.model.terminal.TerminalCommandRequest;
import com.ai.agent.real.contract.model.terminal.TerminalCommandResult;
import com.ai.agent.real.contract.service.terminal.TerminalCommandHandler;
import com.ai.agent.real.contract.service.terminal.TerminalCommandService;
import com.ai.agent.real.domain.entity.TerminalCommand;
import com.ai.agent.real.domain.entity.TerminalHistory;
import com.ai.agent.real.domain.repository.TerminalHistoryRepository;
import com.ai.agent.real.domain.repository.TerminalSessionRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 终端命令网关服务实现
 *
 * @author Real Agent Team
 * @since 2025-01-23
 */
@Service
public class TerminalGatewayServiceImpl implements TerminalGatewayService {

	private static final Logger logger = LoggerFactory.getLogger(TerminalGatewayServiceImpl.class);

	private final TerminalCommandService commandService;

	private final Map<String, TerminalCommandHandler> handlers;

	private final TerminalHistoryRepository historyRepository;

	private final TerminalSessionRepository sessionRepository;

	private final ObjectMapper objectMapper;

	public TerminalGatewayServiceImpl(TerminalCommandService commandService,
			List<TerminalCommandHandler> handlerList, TerminalHistoryRepository historyRepository,
			TerminalSessionRepository sessionRepository, ObjectMapper objectMapper) {
		this.commandService = commandService;
		this.historyRepository = historyRepository;
		this.sessionRepository = sessionRepository;
		this.objectMapper = objectMapper;

		// 构建处理器映射
		this.handlers = handlerList.stream()
			.collect(Collectors.toMap(TerminalCommandHandler::getCommandName, Function.identity()));

		logger.info("Registered {} terminal command handlers: {}", handlers.size(), handlers.keySet());
	}

	@Override
	public Mono<TerminalCommandResult> execute(TerminalCommandRequest request) {
		long startTime = System.currentTimeMillis();

		return commandService.getCommandByName(request.getCommand())
			.switchIfEmpty(Mono.error(new IllegalArgumentException("未知命令: " + request.getCommand())))
			.flatMap(command -> validateAndExecute(request, command, startTime))
			.doOnSuccess(result -> logger.info("Command executed successfully: {} in {}ms", request.getCommand(),
					result.getMetadata() != null ? result.getMetadata().getExecutionTime()
							: System.currentTimeMillis() - startTime))
			.doOnError(error -> logger.error("Command execution failed: {} - {}", request.getCommand(),
					error.getMessage()));
	}

	private Mono<TerminalCommandResult> validateAndExecute(TerminalCommandRequest request, TerminalCommand command,
			long startTime) {
		// 1. 权限检查
		return checkPermission(command.getName(), getUserId(request))
			.flatMap(hasPermission -> {
				if (!hasPermission) {
					return Mono.just(TerminalCommandResult.error("权限不足，该命令需要更高权限",
							"请联系管理员获取相应权限"));
				}

				// 2. 连接状态检查
				return checkConnection(command.getName(), request.getSessionId());
			})
			.flatMap(hasConnection -> {
				if (!hasConnection) {
					return Mono.just(TerminalCommandResult.error("需要先连接到服务器",
							"请使用 /connect 命令连接到远程服务器"));
				}

				// 3. 查找处理器
				TerminalCommandHandler handler = handlers.get(command.getName());
				if (handler == null) {
					return Mono.just(TerminalCommandResult.error("命令处理器未找到: " + command.getHandler(),
							"请联系管理员检查系统配置"));
				}

				// 4. 参数验证
				return handler.validate(request).flatMap(validationError -> {
					if (validationError != null && !validationError.isEmpty()) {
						return Mono.just(TerminalCommandResult.error("参数验证失败: " + validationError));
					}

					// 5. 执行命令
					return handler.execute(request).flatMap(result -> {
						// 6. 记录历史
						return recordHistory(request, result, startTime).thenReturn(result);
					});
				});
			})
			.onErrorResume(error -> {
				// 记录错误历史
				TerminalCommandResult errorResult = TerminalCommandResult.error(error.getMessage());
				return recordHistory(request, errorResult, startTime).thenReturn(errorResult);
			});
	}

	@Override
	public Mono<Boolean> checkPermission(String commandName, String userId) {
		return commandService.getCommandByName(commandName).map(command -> {
			String permission = command.getPermission();

			switch (permission) {
			case "public":
				return true;
			case "user":
				return userId != null && !userId.isEmpty();
			case "admin":
				// TODO: 实际项目中需要检查用户角色
				return userId != null && !userId.isEmpty();
			case "system":
				return false; // 系统命令用户不能直接执行
			default:
				return false;
			}
		}).defaultIfEmpty(false);
	}

	@Override
	public Mono<Boolean> checkConnection(String commandName, String sessionId) {
		return commandService.getCommandByName(commandName).flatMap(command -> {
			if (!command.getNeedsConnection()) {
				return Mono.just(true);
			}

			// TODO: 检查会话的连接状态
			// 这里简化处理，实际项目中需要检查session中的连接状态
			return sessionRepository.findById(sessionId)
				.map(session -> session.getStatus().equals("active"))
				.defaultIfEmpty(false);
		});
	}

	/**
	 * 记录命令执行历史
	 */
	private Mono<Void> recordHistory(TerminalCommandRequest request, TerminalCommandResult result, long startTime) {
		return Mono.fromCallable(() -> {
			TerminalHistory history = new TerminalHistory();
			history.setId("hist-" + UUID.randomUUID().toString());
			history.setSessionId(request.getSessionId());
			history.setUserId(getUserId(request));
			history.setCommandName(request.getCommand());
			history.setOriginalCommand(buildOriginalCommand(request));
			history.setParsedCommand(toJson(request));
			history.setContext(toJson(request.getContext()));
			history.setResult(toJson(result));
			history.setExecutionTime((int) (System.currentTimeMillis() - startTime));
			history.setExitCode(result.isSuccess() ? 0 : 1);
			history.setErrorMessage(result.getError());
			history.setTimestamp(LocalDateTime.now());

			return history;
		}).flatMap(historyRepository::save).then();
	}

	/**
	 * 构建原始命令字符串
	 */
	private String buildOriginalCommand(TerminalCommandRequest request) {
		StringBuilder sb = new StringBuilder("/").append(request.getCommand());

		// 添加位置参数
		if (request.getArgs() != null) {
			for (String arg : request.getArgs()) {
				sb.append(" ").append(arg);
			}
		}

		// 添加选项参数
		if (request.getOptions() != null) {
			request.getOptions().forEach((key, value) -> sb.append(" --").append(key).append(" ").append(value));
		}

		// 添加标记参数
		if (request.getFlags() != null) {
			request.getFlags().forEach((key, value) -> {
				if (value) {
					sb.append(" --").append(key);
				}
			});
		}

		return sb.toString();
	}

	/**
	 * 获取用户ID
	 */
	private String getUserId(TerminalCommandRequest request) {
		return request.getContext() != null && request.getContext().getUser() != null
				? request.getContext().getUser().getId() : "anonymous";
	}

	/**
	 * 对象转JSON
	 */
	private String toJson(Object obj) {
		try {
			return objectMapper.writeValueAsString(obj);
		}
		catch (JsonProcessingException e) {
			logger.warn("Failed to convert object to JSON: {}", e.getMessage());
			return "{}";
		}
	}

}