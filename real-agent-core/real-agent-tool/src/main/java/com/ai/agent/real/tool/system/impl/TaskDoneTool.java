package com.ai.agent.real.tool.system.impl;

import com.ai.agent.real.contract.exception.ToolException;
import com.ai.agent.real.contract.protocol.*;
import com.ai.agent.real.contract.spec.*;
import lombok.extern.slf4j.Slf4j;

import static com.ai.agent.real.common.constant.NounConstants.TASK_DONE;

/**
 * 任务完成工具
 * 用于标记任务已完成并提供最终结果，替代文本匹配的任务完成判断
 * 
 * @author han
 * @time 2025/9/9 01:15
 */
@Slf4j
public class TaskDoneTool implements AgentTool {

    private final String id = "task_done";
    private final ToolSpec spec = new ToolSpec()
            .setName(TASK_DONE)
            .setDescription("当目前的返回结果足以满足用户需求, 则调用此工具标记任务已完成")
            .setCategory("system")
            .setInputSchemaClass(Void.class);

    /**
     * 获取工具的唯一标识, 如果重复, 会抛出异常
     *
     * @return 工具的名称
     */
    @Override
    public String getId() {
        return id;
    }


    @Override
    public ToolSpec getSpec() {
        return spec;
    }

    /**
     * 执行工具的操作。
     *
     * @param ctx 上下文
     * @return 工具执行结果
     * @throws ToolException 工具执行异常
     */


    @Override
    public ToolResult<?> execute(AgentContext<Object> ctx) throws ToolException {

        return ToolResult.ok(TASK_DONE, 0, getId());
    }


}
