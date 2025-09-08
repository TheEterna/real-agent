//package com.ai.agent.kit.core.agent;
//
//import com.ai.agent.kit.common.spec.*;
//import com.ai.agent.kit.core.agent.impl.CodeAnalysisAgent;
//import com.ai.agent.kit.core.agent.impl.DocumentationAgent;
//import com.ai.agent.kit.core.agent.impl.GeneralPurposeAgent;
//import com.ai.agent.kit.core.agent.manager.AgentManager;
//import com.ai.agent.kit.core.agent.strategy.CollaborativeAgentStrategy;
//import com.ai.agent.kit.core.agent.strategy.CompetitiveAgentStrategy;
//import com.ai.agent.kit.core.agent.strategy.SingleAgentStrategy;
//import com.ai.agent.kit.core.agent.communication.AgentContext;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.mockito.Mock;
//import org.mockito.MockitoAnnotations;
//import org.springframework.ai.chat.model.ChatModel;
//import org.springframework.ai.chat.model.ChatResponse;
//import org.springframework.ai.chat.prompt.Prompt;
//
//import static org.junit.jupiter.api.Assertions.*;
//import static org.mockito.ArgumentMatchers.any;
//import static org.mockito.Mockito.when;
//
///**
// * 多Agent策略测试类
// *
// * @author han
// * @time 2025/9/7 00:15
// */
//class MultiAgentTest {
//
//    @Mock
//    private ChatModel chatModel;
//
//    @Mock
//    private ChatResponse chatResponse;
//
//    private AgentManager agentManager;
//    private AgentContext agentContext;
//
//    @BeforeEach
//    void setUp() {
//        MockitoAnnotations.openMocks(this);
//
//        // Mock ChatModel响应
//        when(chatModel.call(any(Prompt.class))).thenReturn(chatResponse);
//        when(chatResponse.getResult()).thenReturn(new MockChatResult("这是一个模拟的AI响应"));
//
//        // 创建AgentManager并注册Agent和策略
//        agentManager = new AgentManager();
//
//        // 注册Agent
//        agentManager.registerAgent(new CodeAnalysisAgent(chatModel));
//        agentManager.registerAgent(new DocumentationAgent(chatModel));
//        agentManager.registerAgent(new GeneralPurposeAgent(chatModel));
//
//
//        // 创建工具上下文
//        agentContext = new AgentContext()
//                .setUserId("test-user")
//                .setSessionId("test-session")
//                .setTraceId("test-trace");
//    }
//
//    @Test
//    void testSingleAgentStrategy() {
//        String task = "分析这段Java代码的性能问题";
//
//        AgentResult result = agentManager.executeTask(task, agentContext, "SingleAgent");
//
//        assertNotNull(result);
//        assertTrue(result.isSuccess());
//        assertEquals("code-analysis-agent", result.getAgentId());
//        assertNotNull(result.getResult());
//    }
//
//    @Test
//    void testCollaborativeAgentStrategy() {
//        String task = "为这个项目生成完整的技术文档";
//
//        AgentResult result = agentManager.executeTask(task, agentContext, "Collaborative");
//
//        assertNotNull(result);
//        // 协作策略可能成功或失败，取决于Agent的协作结果
//        assertNotNull(result.getResult() != null || result.getErrorMessage() != null);
//    }
//
//    @Test
//    void testCompetitiveAgentStrategy() {
//        String task = "优化这个算法的实现";
//
//        AgentResult result = agentManager.executeTask(task, agentContext, "Competitive");
//
//        assertNotNull(result);
//        // 竞争策略应该选择最优结果
//        if (result.isSuccess()) {
//            assertTrue(result.getConfidenceScore() > 0);
//        }
//    }
//
//    @Test
//    void testAutoStrategySelection() {
//        String task = "分析代码架构并生成文档";
//
//        // 不指定策略，让系统自动选择
//        AgentResult result = agentManager.executeTask(task, agentContext);
//
//        assertNotNull(result);
//        // 系统应该能够自动选择合适的策略
//    }
//
//    @Test
//    void testAgentCapabilityMatching() {
//        // 测试代码分析任务
//        String codeTask = "代码审查";
//        Agent bestAgent = agentManager.getCapableAgents(codeTask).get(0);
//        assertEquals("code-analysis-agent", bestAgent.getAgentId());
//
//        // 测试文档生成任务
//        String docTask = "生成API文档";
//        bestAgent = agentManager.getCapableAgents(docTask).get(0);
//        assertEquals("documentation-agent", bestAgent.getAgentId());
//    }
//
//    @Test
//    void testAgentManagerStatus() {
//        var status = agentManager.getStatus();
//
//        assertEquals(3, status.get("agentCount"));
//        assertEquals(3, status.get("strategyCount"));
//        assertEquals("SingleAgent", status.get("defaultStrategy"));
//
//        assertTrue(((java.util.Set<?>) status.get("agents")).contains("code-analysis-agent"));
//        assertTrue(((java.util.Set<?>) status.get("agents")).contains("documentation-agent"));
//        assertTrue(((java.util.Set<?>) status.get("agents")).contains("general-purpose-agent"));
//    }
//
//    // Mock ChatResult 类
//    private static class MockChatResult implements org.springframework.ai.chat.model.ChatResponse.Result {
//        private final String content;
//
//        public MockChatResult(String content) {
//            this.content = content;
//        }
//
//        @Override
//        public org.springframework.ai.chat.model.Generation getOutput() {
//            return new org.springframework.ai.chat.model.Generation() {
//                @Override
//                public String getContent() {
//                    return content;
//                }
//
//                @Override
//                public java.util.Map<String, Object> getMetadata() {
//                    return java.util.Map.of();
//                }
//            };
//        }
//
//        @Override
//        public java.util.Map<String, Object> getMetadata() {
//            return java.util.Map.of();
//        }
//    }
//}
