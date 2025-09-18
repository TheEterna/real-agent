package com.ai.agent.real.web.config;

import com.ai.agent.real.agent.impl.*;
import com.ai.agent.real.agent.strategy.*;
import com.ai.agent.real.contract.service.*;
import org.springframework.ai.chat.model.*;
import org.springframework.context.annotation.*;

/**
 * @author han
 * @time 2025/9/18 20:31
 */

@Configuration
public class AgentBeanConfig {





    @Bean
    public ThinkingAgent thinkingAgent(ChatModel chatModel, ToolService toolService) {
        return new ThinkingAgent(chatModel, toolService);
    }

    @Bean
    public ActionAgent actionAgent(ChatModel chatModel, ToolService toolService) {
        return new ActionAgent(chatModel, toolService);
    }

    @Bean
    public ObservationAgent observationAgent(ChatModel chatModel, ToolService toolService) {
        return new ObservationAgent(chatModel, toolService);
    }

    @Bean
    public FinalAgent finalAgent(ChatModel chatModel, ToolService toolService) {
        return new FinalAgent(chatModel, toolService);
    }
    @Bean
    public ReActAgentStrategy reactAgentStrategy(ThinkingAgent thinkingAgent, ActionAgent actionAgent, ObservationAgent observationAgent, FinalAgent finalAgent) {
        return new ReActAgentStrategy(thinkingAgent, actionAgent, observationAgent, finalAgent);
    }
}
