package com.ai.agent.real.agent.impl;

import com.ai.agent.real.contract.service.*;
import com.ai.agent.real.contract.spec.*;
import com.ai.agent.real.agent.Agent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import reactor.core.publisher.*;

import java.util.*;

/**
 * 文档生成专家Agent
 * 专门处理技术文档、API文档、用户手册等文档生成任务
 * 
 * @author han
 * @time 2025/9/7 00:20
 */
@Slf4j
public class DocumentationAgent extends Agent {


    private static final String SYSTEM_PROMPT = """
            你是一位专业的技术文档专家，擅长编写清晰、准确、易懂的技术文档。
            你的专长包括：
            1. API文档编写和维护
            2. 用户手册和操作指南
            3. 技术规范和设计文档
            4. 代码注释和文档生成
            5. 项目README和说明文档
            
            请确保文档结构清晰、内容准确、格式规范，便于读者理解和使用。
            使用Markdown格式输出，包含适当的标题、列表、代码块等元素。
            """;

    public DocumentationAgent(ChatModel chatModel, ToolService toolService) {
        super("documentation-agent",
                "文档生成专家",
                "专门处理各类技术文档生成任务", chatModel,
                toolService,
                Set.of("文档", "说明", "手册", "指南", "readme", "api文档", "注释",
                "规范", "文档生成", "documentation", "manual", "guide",
                "comment", "specification", "doc", "md", "markdown"));
        this.setCapabilities(new String[]{"文档生成", "API文档", "用户手册", "技术规范", "代码注释"});
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
     * @param task    任务描述
     * @param context 工具执行上下文
     * @return 执行结果
     */
    @Override
    public AgentResult execute(String task, AgentContext context) {
        return null;
    }
}
