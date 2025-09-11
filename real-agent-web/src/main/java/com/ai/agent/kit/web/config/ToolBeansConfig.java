package com.ai.agent.kit.web.config;

import com.ai.agent.kit.core.tool.*;
import com.ai.agent.kit.core.tool.impl.*;
import com.ai.agent.kit.core.tool.impl.HttpRequestTool.SandboxPolicy;
import com.ai.agent.kit.core.tool.impl.HttpRequestTool.HttpRequestTool;
import com.ai.agent.kit.core.tool.impl.MathEvalTool.MathEvalTool;
import com.ai.agent.kit.core.tool.impl.TimeNowTool.TimeNowTool;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Set;

@Configuration
public class ToolBeansConfig {

//    @Bean
//    public SandboxPolicy sandboxPolicy(){
//        SandboxPolicy policy = new SandboxPolicy();
//        policy.setAllowedHosts(Set.of("api.github.com","httpbin.org","localhost"));
//        policy.setAllowedMethods(Set.of("GET"));
//        policy.setMaxResponseBytes(512_000);
//        return policy;
//    }

    @Bean
//    public ToolRegistry toolRegistry(SandboxPolicy policy){
    public ToolRegistry toolRegistry(){
        ToolRegistry registry = new ToolRegistry();
//        registry.registerWithKeywords(new HttpRequestTool(policy), Set.of("http", "api", "请求"));
//        registry.registerWithKeywords(new MathEvalTool(), Set.of("math", "计算"));
//        registry.registerWithKeywords(new TimeNowTool(), Set.of("time", "时间"));
        registry.registerWithKeywords(new TaskDoneTool(), Set.of("close"));
        return registry;
    }
    @Bean
    public TaskDoneTool taskDoneTool(){
        return new TaskDoneTool();
    }
}
