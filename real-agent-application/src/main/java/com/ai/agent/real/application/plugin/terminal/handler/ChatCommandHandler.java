package com.ai.agent.real.application.plugin.terminal.handler;

import com.ai.agent.real.contract.model.terminal.TerminalCommandRequest;
import com.ai.agent.real.contract.model.terminal.TerminalCommandResult;
import com.ai.agent.real.contract.plugin.terminal.TerminalCommandHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * Chat命令处理器 - AI对话
 *
 * @author Real Agent Team
 * @since 2025-01-23
 */
@Slf4j
public class ChatCommandHandler implements TerminalCommandHandler {

	private final ChatModel chatModel;

	public ChatCommandHandler(ChatModel chatModel) {
		this.chatModel = chatModel;
	}

	@Override
	public String getName() {
		return "ChatCommandHandler";
	}

	@Override
	public String getCommandName() {
		return "chat";
	}

	@Override
	public Mono<TerminalCommandResult> execute(TerminalCommandRequest request) {
		// 获取用户消息
		String message = String.join(" ", request.getArguments());


		// 调用AI模型
		return Mono.fromCallable(() -> {
			long startTime = System.currentTimeMillis();

			Prompt prompt = new Prompt(message);
			String response = chatModel.call(prompt).getResult().getOutput().getText();

			TerminalCommandResult result = TerminalCommandResult.success(response);

			// 设置元数据
			TerminalCommandResult.Metadata metadata = new TerminalCommandResult.Metadata();
			metadata.setExecutionTime(System.currentTimeMillis() - startTime);
			metadata.setExitCode(0);
			result.setMetadata(metadata);

			return result;
		}).onErrorResume(error -> {
			return Mono.just(TerminalCommandResult.error("AI服务调用失败: " + error.getMessage(), "请检查AI服务配置或稍后重试"));
		});
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
		return """
				chat - 与AI助手对话

				用法:
				  chat <消息内容>

				示例:
				  /chat 你好
				  /chat 帮我写一个Python函数
				  /chat "如何学习Vue3框架？"

				注意:
				  - 需要配置AI服务（OpenAI/通义千问等）
				  - 消息内容如包含空格，建议使用引号包裹
				""";
	}

}
