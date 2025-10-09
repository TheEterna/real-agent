package com.ai.agent.real.tool.system.impl.TimeNowTool;

import com.ai.agent.real.contract.exception.*;
import com.ai.agent.real.contract.protocol.*;
import com.ai.agent.real.contract.protocol.ToolResult.*;
import com.ai.agent.real.contract.spec.*;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.Map;

public class TimeNowTool implements AgentTool {

	private final ToolSpec spec = new ToolSpec().setName("time_now")
		.setDescription("获取当前时间, 可以指定时区和格式")
		.setCategory("utility")
		.setInputSchemaClass(TimeNowToolDto.class);

	/**
	 * 获取工具的唯一标识, 如果重复, 会抛出异常
	 * @return 工具的名称
	 */
	@Override
	public String getId() {
		return "time.now";
	}

	@Override
	public ToolSpec getSpec() {
		return spec;
	}

	/**
	 * 执行工具的操作。
	 * @param ctx 上下文
	 * @return 工具执行结果
	 * @throws ToolException 工具执行异常
	 */
	@Override
	public ToolResult<Map<String, Object>> execute(AgentContext<Object> ctx) throws ToolException {
		long start = System.currentTimeMillis();
		try {
			ZonedDateTime now = ZonedDateTime.now(ZoneId.of(ctx.getStructuralToolArgs(TimeNowToolDto.class).zone));
			String s = now.format(DateTimeFormatter.ofPattern(ctx.getStructuralToolArgs(TimeNowToolDto.class).pattern));
			return ToolResult.ok(Map.of("time", s, "epochMs", now.toInstant().toEpochMilli()),
					System.currentTimeMillis() - start, getId());
		}
		catch (Exception e) {
			return ToolResult.error(ToolResultCode.TOOL_EXECUTION_ERROR, e.getMessage(), getId());
		}
	}

	public static class TimeNowToolDto {

		String zone = "Asia/Shanghai";

		String pattern = "yyyy-MM-dd HH:mm:ss";

	}

}
