package com.ai.agent.real.agent.impl;

import com.ai.agent.real.contract.spec.*;
import com.ai.agent.real.agent.Agent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import reactor.core.publisher.*;

import java.util.*;

/**
 * 代码分析专家Agent
 * 专门处理代码分析、代码审查、架构分析等任务
 * 
 * @author han
 * @time 2025/9/7 00:15
 */
@Slf4j
public class CodeAnalysisAgent extends Agent {

    private static final String SYSTEM_PROMPT = """
            你是一位资深的代码分析专家，拥有20年的软件开发和架构设计经验。
            你的专长包括：
            1. 代码质量分析和改进建议
            2. 架构设计分析和优化
            3. 性能瓶颈识别和解决方案
            4. 安全漏洞检测和修复建议
            5. 代码规范和最佳实践指导
            
            请用专业、详细且易懂的方式分析代码，提供具体的改进建议。
            """;

    public CodeAnalysisAgent(ChatModel chatModel, ToolRegistry toolRegistry) {
        super("code-analysis-agent",
                "代码分析专家",
                "专门处理代码分析和架构分析任务",
                chatModel,
                toolRegistry,
                Set.of("代码", "架构", "分析", "审查", "优化", "重构", "设计模式",
                    "性能", "安全", "漏洞", "规范", "最佳实践", "code", "architecture",
                    "analysis", "review", "optimize", "refactor", "design pattern",
                    "performance", "security", "vulnerability", "standard"));
        this.setCapabilities(new String[]{"代码分析", "架构设计", "性能优化", "安全审查", "代码规范"});
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
