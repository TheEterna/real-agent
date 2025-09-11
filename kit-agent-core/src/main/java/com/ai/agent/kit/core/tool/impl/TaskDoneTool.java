package com.ai.agent.kit.core.tool.impl;

import com.ai.agent.contract.exception.ToolException;
import com.ai.agent.contract.spec.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;

import static com.ai.agent.kit.common.constant.NounConstants.TASK_DONE;

/**
 * 任务完成工具
 * 用于标记任务已完成并提供最终结果，替代文本匹配的任务完成判断
 * 
 * @author han
 * @time 2025/9/9 01:15
 */
@Slf4j
public class TaskDoneTool implements AgentTool {

    private final ToolSpec spec = new ToolSpec()
            .setName("task_done")
            .setDescription("标记任务已完成")
            .setCategory("system");

    @Override
    public String Id() {
        return "task_done";
    }

    @Override
    public ToolSpec getSpec() {
        return spec;
    }

    @Override
    @Tool(description = "标记任务已完成", name = "完成任务", returnDirect = true)
    public ToolResult<String> execute(AgentContext ctx) throws ToolException {

        return ToolResult.ok(TASK_DONE, 0, Id());
    }

}
