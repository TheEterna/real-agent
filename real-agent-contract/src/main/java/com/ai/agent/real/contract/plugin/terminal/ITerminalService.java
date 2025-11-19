package com.ai.agent.real.contract.plugin.terminal;

import com.ai.agent.real.contract.model.terminal.TerminalCommandRequest;
import com.ai.agent.real.contract.model.terminal.TerminalCommandResult;
import reactor.core.publisher.Mono;

/**
 * 终端命令网关服务
 * <p>
 * 负责命令的分发、权限验证、历史记录等
 * </p>
 *
 * @author han
 * @time 2025/10/23 23:35
 */
public interface ITerminalService {

	/**
	 * 执行命令
	 * <p>
	 * 完整的命令执行流程： 1. 验证命令是否存在 2. 验证命令参数 3. 分发到对应的处理器 4. 记录执行历史
	 * </p>
	 * @param request 命令请求
	 * @return 执行结果
	 */
	Mono<TerminalCommandResult> execute(TerminalCommandRequest request);

	// /**
	// * TODO: 检查连接状态, 后期等添加了连接服务器功能后，对其进行实现
	// * @param commandName 命令名称
	// * @param sessionId 会话ID
	// * @return 是否满足连接要求
	// */
	// Mono<Boolean> checkConnection(String commandName, String sessionId);

}
