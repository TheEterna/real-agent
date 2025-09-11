package com.ai.agent.kit.core.config;

import com.ai.agent.kit.core.agent.Agent;
import com.ai.agent.kit.core.agent.impl.*;
import com.ai.agent.kit.core.agent.manager.AgentManager;
import com.ai.agent.kit.core.agent.strategy.AgentStrategy;
//import com.ai.agent.kit.core.agent.strategy.CollaborativeAgentStrategy;
//import com.ai.agent.kit.core.agent.strategy.CompetitiveAgentStrategy;
//import com.ai.agent.kit.core.agent.strategy.SingleAgentStrategy;
import com.ai.agent.kit.core.tool.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Agent自动配置类
 * 
 * @author han
 * @time 2025/9/7 00:05
 */
@Slf4j
@Configuration
@EnableConfigurationProperties(SystemProperties.class)
//@ConditionalOnProperty(prefix = SYSTEM_PROPERTY_PREFIX, name = "enabled", havingValue = "true", matchIfMissing = true)
public class AgentAutoConfiguration {

    /**
     * Agent管理器Bean
     */
    @Bean
    public AgentManager agentManager(
                                   List<Agent> agents, 
                                   List<AgentStrategy> strategies,
                                   SystemProperties properties) {
        AgentManager manager = new AgentManager();
        
        // 注册所有Agent
        for (Agent agent : agents) {
            manager.registerAgent(agent);
        }
        
        // 注册所有策略
        for (AgentStrategy strategy : strategies) {
            manager.registerStrategy(strategy);
        }
        
        // 设置默认策略
        if (properties.getDefaultStrategy() != null) {
            manager.setDefaultStrategy(properties.getDefaultStrategy());
        }
        
        log.info("AgentManager初始化完成，注册Agent数量: {}, 策略数量: {}", 
                agents.size(), strategies.size());
        
        return manager;
    }

//    @Bean
//    public CollaborativeAgentStrategy collaborativeAgentStrategy() {
//        return new CollaborativeAgentStrategy();
//    }
//
//    @Bean
//    public CompetitiveAgentStrategy competitiveAgentStrategy() {
//        return new CompetitiveAgentStrategy();
//    }

    /**
     * 默认Agent Bean
     */
    @Bean
    public CodeAnalysisAgent codeAnalysisAgent(ChatModel chatModel, ToolRegistry toolRegistry) {
        return new CodeAnalysisAgent(chatModel, toolRegistry);
    }

    @Bean
    public DocumentationAgent documentationAgent(ChatModel chatModel, ToolRegistry toolRegistry) {
        return new DocumentationAgent(chatModel, toolRegistry);
    }

    @Bean
    public CodeGenerationAgent codeGenerationAgent(ChatModel chatModel, ToolRegistry toolRegistry) {
        return new CodeGenerationAgent(chatModel, toolRegistry);
    }

    @Bean
    public GeneralPurposeAgent generalPurposeAgent(ChatModel chatModel, ToolRegistry toolRegistry) {
        return new GeneralPurposeAgent(chatModel, toolRegistry);
    }
}
