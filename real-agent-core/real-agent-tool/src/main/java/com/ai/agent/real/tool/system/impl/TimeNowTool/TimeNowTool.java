package com.ai.agent.real.tool.system.impl.TimeNowTool;

import com.ai.agent.real.contract.exception.*;
import com.ai.agent.real.contract.spec.*;
import org.springframework.ai.tool.annotation.*;


import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.Map;

public class TimeNowTool implements AgentTool {
    private final ToolSpec spec = new ToolSpec()
            .setName("time_now")
            .setDescription("获取当前时间, 可以指定时区和格式")
            .setCategory("utility");


    /**
     * 获取工具的唯一标识, 如果重复, 会抛出异常
     *
     * @return 工具的名称
     */
    @Override
    public String Id() {
        return "time.now";
    }

    @Override
    public ToolSpec getSpec(){ return spec; }

    /**
     * 执行工具的操作。
     *
     * @param ctx 上下文
     * @return 工具执行结果
     * @throws ToolException 工具执行异常
     */
    @Override
    @Tool(description = "获取当前时间, 可以指定时区和格式", name = "time_now")
    public ToolResult<?> execute(AgentContext ctx) throws ToolException {
        long start = System.currentTimeMillis();
        Map<String, Object> args = ctx.getToolArgs();
        String zone = String.valueOf(args.getOrDefault("zone","Asia/Shanghai"));
        String pattern = String.valueOf(args.getOrDefault("pattern","yyyy-MM-dd HH:mm:ss"));
        try{
            ZonedDateTime now = ZonedDateTime.now(ZoneId.of(zone));
            String s = now.format(DateTimeFormatter.ofPattern(pattern));
            return ToolResult.ok(Map.of("time", s, "epochMs", now.toInstant().toEpochMilli()), System.currentTimeMillis()-start, Id());
        }catch(Exception e){
            return ToolResult.error("TIME_ERROR", e.getMessage(), Id());
        }
    }
}
