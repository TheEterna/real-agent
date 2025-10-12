/**
 * Agent 配置属性
 *
 * <p>配置前缀: real-agent
 *
 * <p>示例配置:
 * <pre>
 * real-agent:
 *   action:
 *     approval-mode: AUTO
 *     execution-timeout: 120s
 * </pre>
 *
 * @author han
 * @time 2025/10/12 18:34
 */
package com.ai.agent.real.web.config.properties;

import com.ai.agent.real.contract.service.*;
import com.ai.agent.real.contract.model.property.*;
import lombok.*;
import org.springframework.boot.context.properties.*;
import org.springframework.stereotype.*;
import org.springframework.validation.annotation.*;

import javax.validation.*;
import javax.validation.constraints.*;
import java.time.*;

@Data
@Component
@ConfigurationProperties(prefix = "real-agent")
@Validated
public class RealAgentProperties implements IPropertyService {

	/**
	 * Action 相关配置
	 */
	@NotNull
	@Valid
	private Tool tool;

	@NotNull
	@Valid
	private Context context;

	@Data
	public static class Tool {

		/**
		 * 审批模式：AUTO | REQUIRE_APPROVAL | DISABLED
		 */
		@NotNull
		private ToolApprovalMode approvalMode = ToolApprovalMode.DISABLED;

		/**
		 * 执行超时时间，默认 120 秒
		 */
		@NotNull
		private Duration executionTimeout = Duration.ofSeconds(120);

	}

	@Data
	public static class Context {

		/**
		 * 压缩模式：DISABLED | ZIP | CRAZY_ZIP
		 */
		@NotNull
		private ContextZipMode zipMode = ContextZipMode.DISABLED;

	}

	/**
	 * 获取 Agent 执行超时时间
	 */
	@Override
	public Duration getAgentExecutionTimeout() {
		return tool.getExecutionTimeout();
	}

	/**
	 * 获取工具审批模式
	 */
	@Override
	public ToolApprovalMode getToolApprovalMode() {
		return tool.getApprovalMode();
	}

	/**
	 * 获取上下文压缩模式
	 */
	@Override
	public ContextZipMode getContextZipMode() {
		return context.getZipMode();
	}

}