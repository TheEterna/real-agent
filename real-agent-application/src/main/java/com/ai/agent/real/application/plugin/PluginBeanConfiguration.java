package com.ai.agent.real.application.plugin;

import com.ai.agent.real.application.plugin.terminal.CommandService;
import com.ai.agent.real.application.plugin.terminal.TerminalService;
import com.ai.agent.real.application.plugin.terminal.handler.ChatCommandHandler;
import com.ai.agent.real.application.plugin.terminal.handler.ClearCommandHandler;
import com.ai.agent.real.application.plugin.terminal.handler.DefaultCommandHandler;
import com.ai.agent.real.application.plugin.terminal.handler.HelpCommandHandler;
import com.ai.agent.real.contract.plugin.terminal.ICommandService;
import com.ai.agent.real.contract.plugin.terminal.ITerminalService;
import com.ai.agent.real.contract.plugin.terminal.TerminalCommandHandler;
import com.ai.agent.real.domain.repository.ITerminalCommandRepository;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.context.annotation.Bean;

import java.util.List;

/**
 * @author han
 * @time 2025/10/27 23:15
 */

public class PluginBeanConfiguration {

	@Bean
	public TerminalCommandHandler defaultCommandHandler() {
		return new DefaultCommandHandler();
	}

	@Bean
	public TerminalCommandHandler clearCommandHandler() {
		return new ClearCommandHandler();
	}

	@Bean
	public TerminalCommandHandler helpCommandHandler(ICommandService commandService) {
		return new HelpCommandHandler(commandService);
	}

	@Bean
	public TerminalCommandHandler chatCommandHandler(ChatModel chatModel) {
		return new ChatCommandHandler(chatModel);
	}

	/**
	 * 终端命令服务
	 * @param commandRepository 命令仓库
	 * @return CommandService实例
	 */
	@Bean
	public ICommandService commandService(ITerminalCommandRepository commandRepository) {
		return new CommandService(commandRepository);
	}

	/**
	 * 注册终端网关服务
	 * @param handlerList 命令处理器列表
	 * @return TerminalGatewayService实例
	 */
	@Bean
	public ITerminalService terminalService(ICommandService commandService, List<TerminalCommandHandler> handlerList) {
		return new TerminalService(commandService, handlerList);
	}

}
