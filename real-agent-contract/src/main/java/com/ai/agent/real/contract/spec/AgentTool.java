package com.ai.agent.real.contract.spec;

import com.ai.agent.real.contract.exception.*;
import com.ai.agent.real.contract.protocol.*;
import reactor.core.publisher.*;
import reactor.core.scheduler.*;


/**
 * AgentTool 接口定义了一个工具的基本行为。
 * 接口的选择, 而不是抽象类, 是因为 cause 属性改写不强制, 不够优雅。
 * @author han
 * @time 2025/8/30 17:05
 */
public interface AgentTool {

    /**
     * 获取工具的唯一标识, 如果重复, 会抛出异常
     *
     * @return 工具的名称
     */
    String getId();

    /**
     * 获取工具的规范。
     *
     * @return 工具的规范
     */
    ToolSpec getSpec();

    /**
     * execute tool
     *
     * @param ctx  上下文
     * @return 工具执行结果
     * @throws ToolException 工具执行异常
     */
    ToolResult<?> execute(AgentContext<Object> ctx) throws ToolException;

    /**
     * execute tool async
     *
     * @param ctx 上下文
     * @return 工具执行结果
     * @throws ToolException 工具执行异常
     */
    default Mono<? extends ToolResult<?>> executeAsync(AgentContext<Object> ctx) throws ToolException {
        return Mono.fromCallable(() -> (ToolResult<?>) this.execute(ctx))
                .subscribeOn(Schedulers.boundedElastic());
    }



}
