package com.ai.agent.kit.core.tool.impl.TimeNowTool;

import com.ai.agent.kit.common.exception.*;
import com.ai.agent.kit.common.spec.*;
import com.ai.agent.kit.core.agent.communication.*;
import com.ai.agent.kit.core.tool.model.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.Map;

public class TimeNowTool implements AgentTool {
    private final ToolSpec spec = new ToolSpec()
            .setName("获取当前时间")
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

    @Override
    public ToolResult<?> execute(AgentContext ctx, Map<String, Object> args) throws ToolException {
        long start = System.currentTimeMillis();
        String zone = String.valueOf(args.getOrDefault("zone","Asia/Shanghai"));
        String pattern = String.valueOf(args.getOrDefault("pattern","yyyy-MM-dd HH:mm:ss"));
        try{
            ZonedDateTime now = ZonedDateTime.now(ZoneId.of(zone));
            String s = now.format(DateTimeFormatter.ofPattern(pattern));
            return ToolResult.ok(Map.of("time", s, "epochMs", now.toInstant().toEpochMilli()), System.currentTimeMillis()-start);
        }catch(Exception e){
            return ToolResult.error("TIME_ERROR", e.getMessage());
        }
    }
}
