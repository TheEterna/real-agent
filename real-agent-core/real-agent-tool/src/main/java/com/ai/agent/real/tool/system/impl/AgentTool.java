//package com.ai.agent.real.tool.system.impl;
//
//import com.ai.agent.kit.common.exception.*;
//import com.ai.agent.real.core.agent.communication.*;
//import com.ai.agent.real.core.agent.strategy.ReActAgentStrategy;
//import com.ai.agent.kit.common.spec.ToolResult;
//import com.ai.agent.real.tool.model.*;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.ai.chat.model.ChatModel;
//
//import java.util.*;
//
///**
// * Agent工具实现
// * 将Agent能力包装为标准工具接口，集成到现有工具框架中
// *
// * @author han
// * @time 2025/9/7 01:05
// */
//@Slf4j
//public class AgentTool implements AgentTool {
//
//    private final String agentId;
//    private final String agentName;
//    private final ChatModel chatModel;
//    private final ReActAgentStrategy reactFramework;
//    private final ToolSpec spec = new ToolSpec()
//            .setName("Agent工具")
//            .setDescription("Agent工具, 可以执行任意任务")
//            .setCategory("utility");
//
//    public AgentTool(String agentId, String agentName, ChatModel chatModel) {
//        this.agentId = agentId;
//        this.agentName = agentName;
//        this.chatModel = chatModel;
//        this.reactFramework = new ReActAgentStrategy(chatModel, new ArrayList<>());
//    }
//
//
//    /**
//     * 判断是否能处理指定任务
//     */
//    public boolean canHandle(String task) {
//        // 基于关键词匹配判断能力
//        return task != null && !task.trim().isEmpty();
//    }
//
//    public String getAgentId() {
//        return agentId;
//    }
//
//    public String getAgentName() {
//        return agentName;
//    }
//
//    /**
//     * 获取工具的唯一标识, 如果重复, 会抛出异常
//     *
//     * @return 工具的名称
//     */
//    @Override
//    public String Id() {
//        return agentId;
//    }
//
//    /**
//     * 获取工具的规范。
//     *
//     * @return 工具的规范
//     */
//    @Override
//    public ToolSpec getSpec() {
//        return spec;
//    }
//
//    /**
//     * 执行工具的操作。
//     *
//     * @param ctx  工具上下文
//     * @param toolArgs 工具参数
//     * @return 工具执行结果
//     * @throws ToolException 工具执行异常
//     */
//    @Override
//    public ToolResult<?> execute(AgentContext ctx, Map<String, Object> toolArgs) throws ToolException {
//        long startTime = System.currentTimeMillis();
//
//        try {
//            log.info("Agent[{}] 开始执行任务: {}", agentId, toolArgs.get("task"));
//
//            // 使用ReAct框架执行任务
//            String result = reactFramework.execute(toolArgs.get("task").toString(), ctx, chatModel);
//
//            long elapsed = System.currentTimeMillis() - startTime;
//
//            return ToolResult.ok(result, elapsed)
//                    .putMeta("agentId", agentId)
//                    .putMeta("agentName", agentName)
//                    .putMeta("taskType", "agent_execution");
//
//        } catch (Exception e) {
//            log.error("Agent[{}] 执行失败: {}", agentId, e.getMessage(), e);
//            return ToolResult.error("AGENT_EXECUTION_ERROR", e.getMessage());
//        }
//    }
//}
