package com.ai.agent.kit.core.agent.impl;

import com.ai.agent.kit.common.spec.*;
import com.ai.agent.kit.core.agent.Agent;
import com.ai.agent.kit.core.agent.communication.*;
import com.ai.agent.kit.core.tool.*;
import com.ai.agent.kit.core.tool.model.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.*;
import org.springframework.ai.chat.prompt.*;
import reactor.core.publisher.*;

import java.time.*;
import java.util.*;

/**
 * 通用Agent
 * 处理各种通用任务，作为兜底Agent使用
 * 
 * @author han
 * @time 2025/9/7 00:25
 */
@Slf4j
public class GeneralPurposeAgent extends Agent {

    public final static String AGENT_ID = "general-purpose-agent";
    private final String SYSTEM_PROMPT = """
            你是一个通用的AI助手，能够处理各种类型的任务。
            你具备以下能力：
            1. 问题解答和知识咨询
            2. 文本分析和处理
            3. 逻辑推理和问题解决
            4. 创意思考和建议提供
            5. 任务规划和执行指导
            6. 分析任务并决定是否需要工具
           
            请根据用户的具体需求，提供准确、有用的回答和建议。
            """;

    public GeneralPurposeAgent(ChatModel chatModel, ToolRegistry toolRegistry) {
        super("general-purpose-agent",
                "通用助手",
                "处理各种通用任务的万能Agent",
                chatModel,
                toolRegistry,
                Set.of("通用", "问答", "文本", "处理", "逻辑", "推理", "创意思考", "任务", "规划"));
        this.setCapabilities(new String[]{"通用问答", "文本处理", "逻辑推理", "创意思考", "任务规划"});
    }


    /**
     * 流式执行任务
     *
     * @param task    任务描述
     * @param context 执行上下文
     * @return 流式执行结果
     */
    @Override
    public Flux<AgentExecutionEvent> executeStream(String task, AgentContext context) {
        return null;
    }

    /**
     * 执行任务（同步版本，兼容旧接口）
     *
     * @param task 任务描述
     * @param context 工具执行上下文
     * @return 执行结果
     */
    public AgentResult execute(String task, AgentContext context) {
        LocalDateTime startTime = LocalDateTime.now();

        try {
            log.info("Agent[{}] 开始执行任务: {}", agentId, task);

            // 1. 获取可用工具
            List<AgentTool> availableTools = this.availableTools;
            log.debug("Agent[{}] 可用工具: {}", agentId,
                    availableTools.stream().map(t -> t.getSpec().getName()).toList());


            // 构建提示词
            String promptText = SYSTEM_PROMPT + "\n\n任务：" + task;
            PromptTemplate promptTemplate = new PromptTemplate(promptText);
            Prompt prompt = promptTemplate.create(Map.of());

            // 调用LLM
            ChatResponse response = chatModel.call(prompt);
            String result = response.getResult().getOutput().getText();

            log.info("通用Agent完成任务处理");

            return AgentResult.success(result, this.agentId)
                    .setStartTime(startTime)
                    .setEndTime(LocalDateTime.now());

        } catch (Exception e) {
            log.error("Agent[{}] 执行失败: {}", agentId, e.getMessage(), e);
            return AgentResult.failure("执行异常: " + e.getMessage(), this.agentId)
                    .setStartTime(startTime)
                    .setEndTime(LocalDateTime.now());        }
    }
}
