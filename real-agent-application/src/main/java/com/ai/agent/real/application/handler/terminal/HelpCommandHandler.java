package com.ai.agent.real.application.handler.terminal;

import com.ai.agent.real.contract.model.terminal.TerminalCommandRequest;
import com.ai.agent.real.contract.model.terminal.TerminalCommandResult;
import com.ai.agent.real.contract.service.terminal.TerminalCommandHandler;
import com.ai.agent.real.contract.service.terminal.TerminalCommandService;
import com.ai.agent.real.domain.entity.TerminalCommand;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Help命令处理器
 *
 * @author Real Agent Team
 * @since 2025-01-23
 */
@Component
public class HelpCommandHandler implements TerminalCommandHandler {

	private final TerminalCommandService commandService;

	public HelpCommandHandler(TerminalCommandService commandService) {
		this.commandService = commandService;
	}

	@Override
	public String getName() {
		return "HelpCommandHandler";
	}

	@Override
	public String getCommandName() {
		return "help";
	}

	@Override
	public Mono<TerminalCommandResult> execute(TerminalCommandRequest request) {
		// 检查是否查询特定命令的帮助
		if (request.getArgs() != null && request.getArgs().length > 0) {
			String commandName = request.getArgs()[0];
			return showCommandHelp(commandName);
		}

		// 显示所有命令列表
		return showAllCommands();
	}

	@Override
	public String getHelp() {
		return """
				help - 显示帮助信息

				用法:
				  help              显示所有可用命令
				  help <命令名>     显示指定命令的详细帮助

				示例:
				  /help
				  /help chat
				  /help connect
				""";
	}

	/**
	 * 显示特定命令的帮助
	 */
	private Mono<TerminalCommandResult> showCommandHelp(String commandName) {
		return commandService.getCommandByName(commandName).map(command -> {
			StringBuilder help = new StringBuilder();
			help.append("\n📋 命令: /").append(command.getName()).append("\n");

			if (command.getAliases() != null && !command.getAliases().isEmpty()) {
				help.append("   别名: ").append(command.getAliases()).append("\n");
			}

			help.append("   描述: ").append(command.getDescription()).append("\n");
			help.append("   用法: ").append(command.getUsage()).append("\n");

			if (command.getExamples() != null && !command.getExamples().isEmpty()) {
				help.append("\n   示例:\n");
				// 解析JSON数组的examples
				String[] examples = parseJsonArray(command.getExamples());
				for (String example : examples) {
					help.append("     ").append(example).append("\n");
				}
			}

			if (command.getParameters() != null && !command.getParameters().isEmpty()) {
				help.append("\n   参数:\n");
				help.append("     (参数详情需要解析parameters JSON字段)\n");
			}

			TerminalCommandResult result = TerminalCommandResult.success(help.toString());
			result.setRenderType(TerminalCommandResult.RenderType.TEXT);
			return result;
		}).switchIfEmpty(Mono.just(TerminalCommandResult.error("未知命令: " + commandName, "使用 /help 查看所有可用命令")));
	}

	/**
	 * 显示所有命令列表
	 */
	private Mono<TerminalCommandResult> showAllCommands() {
		return commandService.getVisibleCommands().collectList().map(commands -> {
			StringBuilder help = new StringBuilder("\n📖 可用命令列表:\n\n");

			// 按类别分组
			Map<String, List<TerminalCommand>> grouped = commands.stream()
				.collect(Collectors.groupingBy(TerminalCommand::getCategory));

			Map<String, String> categoryLabels = new HashMap<>();
			categoryLabels.put("system", "系统控制");
			categoryLabels.put("ai", "AI交互");
			categoryLabels.put("file", "文件操作");
			categoryLabels.put("project", "项目管理");
			categoryLabels.put("connection", "连接管理");

			// 按类别输出命令
			grouped.forEach((category, cmdList) -> {
				String label = categoryLabels.getOrDefault(category, category);
				help.append("  ").append(label).append(":\n");

				cmdList.forEach(cmd -> {
					help.append(String.format("    /%-15s - %s\n", cmd.getName(), cmd.getDescription()));
				});

				help.append("\n");
			});

			help.append("  输入 /help <命令名> 查看详细帮助\n");

			TerminalCommandResult result = TerminalCommandResult.success(help.toString());
			result.setRenderType(TerminalCommandResult.RenderType.TEXT);
			return result;
		});
	}

	/**
	 * 简单解析JSON数组字符串
	 */
	private String[] parseJsonArray(String json) {
		if (json == null || json.isEmpty()) {
			return new String[0];
		}

		// 移除方括号和引号，简单分割
		String cleaned = json.replaceAll("[\\[\\]\"]", "");
		return cleaned.split(",");
	}

}
