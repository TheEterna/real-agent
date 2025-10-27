package com.ai.agent.real.application.plugin.terminal.handler;

import com.ai.agent.real.contract.model.terminal.TerminalCommandRequest;
import com.ai.agent.real.contract.model.terminal.TerminalCommandResult;
import com.ai.agent.real.contract.plugin.terminal.TerminalCommandHandler;
import reactor.core.publisher.Mono;

/**
 * @author han
 * @time 2025/10/27 16:51
 */
public class DefaultCommandHandler implements TerminalCommandHandler {

	@Override
	public String getName() {
		return "DefaultCommandHandler";
	}

	@Override
	public String getCommandName() {
		return "default";
	}

	@Override
	public Mono<TerminalCommandResult> execute(TerminalCommandRequest request) {
		return null;
	}

	@Override
	public Mono<String> validate(TerminalCommandRequest request) {

		if (request.getArguments() == null || request.getArguments().isEmpty()) {
			return Mono.just("消息内容不能为空");

		}
		return Mono.empty();
	}

	@Override
	public String getHelp() {
		return "无命令时执行操作，调用指定的agent策略进行任务执行";
	}

}
