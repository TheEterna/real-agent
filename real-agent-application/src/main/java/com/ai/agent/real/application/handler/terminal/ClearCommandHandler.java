package com.ai.agent.real.application.handler.terminal;

import com.ai.agent.real.contract.model.terminal.TerminalCommandRequest;
import com.ai.agent.real.contract.model.terminal.TerminalCommandResult;
import com.ai.agent.real.contract.service.terminal.TerminalCommandHandler;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * Clear命令处理器 - 清空终端
 *
 * @author Real Agent Team
 * @since 2025-01-23
 */
@Component
public class ClearCommandHandler implements TerminalCommandHandler {

	@Override
	public String getName() {
		return "ClearCommandHandler";
	}

	@Override
	public String getCommandName() {
		return "clear";
	}

	@Override
	public Mono<TerminalCommandResult> execute(TerminalCommandRequest request) {
		// Clear命令主要由前端处理，后端返回确认信息即可
		TerminalCommandResult result = TerminalCommandResult.success("");
		result.setRenderType(TerminalCommandResult.RenderType.TEXT);
		return Mono.just(result);
	}

	@Override
	public String getHelp() {
		return """
				clear - 清空终端屏幕

				用法:
				  clear
				  cls (别名)

				说明:
				  清空当前终端屏幕的所有内容
				""";
	}

}
