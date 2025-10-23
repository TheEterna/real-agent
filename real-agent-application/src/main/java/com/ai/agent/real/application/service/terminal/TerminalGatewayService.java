package com.ai.agent.real.application.service.terminal;

import com.ai.agent.real.contract.model.terminal.TerminalCommandRequest;
import com.ai.agent.real.contract.model.terminal.TerminalCommandResult;
import reactor.core.publisher.Mono;

/**
 * 终端命令网关服务
 * <p>
 * 负责命令的分发、权限验证、历史记录等
 * </p>
 *
 * @author Real Agent Team
 * @since 2025-01-23
 */
public interface TerminalGatewayService {

	/**
	 * 执行命令
	 * <p>
	 * 完整的命令执行流程：
	 * 1. 验证命令是否存在
	 * 2. 检查用户权限
	 * 3. 验证命令参数
	 * 4. 分发到对应的处理器
	 * 5. 记录执行历史
	 * </p>
	 * @param request 命令请求
	 * @return 执行结果
	 */
	Mono<TerminalCommandResult> execute(TerminalCommandRequest request);

	/**
	 * 检查命令权限
	 * @param commandName 命令名称
	 * @param userId 用户ID
	 * @return 是否有权限
	 */
	Mono<Boolean> checkPermission(String commandName, String userId);

	/**
	 * 检查连接状态
	 * @param commandName 命令名称
	 * @param sessionId 会话ID
	 * @return 是否满足连接要求
	 */
	Mono<Boolean> checkConnection(String commandName, String sessionId);

}
