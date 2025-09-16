package com.ai.agent.real.web.config;

import com.ai.agent.real.tool.system.*;
import com.ai.agent.real.tool.system.impl.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Set;

import static com.ai.agent.real.common.constant.NounConstants.TASK_DONE;

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
//    public ToolRegistryImpl toolRegistry(SandboxPolicy policy){
    public ToolRegistryImpl toolRegistry(){
        ToolRegistryImpl registry = new ToolRegistryImpl();
//        registry.registerWithKeywords(new HttpRequestTool(policy), Set.of("http", "api", "请求"));
//        registry.registerWithKeywords(new MathEvalTool(), Set.of("math", "计算"));
//        registry.registerWithKeywords(new TimeNowTool(), Set.of("time", "时间"));
        registry.registerWithKeywords(new TaskDoneTool(), Set.of(TASK_DONE));
        return registry;
    }
    @Bean
    public TaskDoneTool taskDoneTool(){
        return new TaskDoneTool();
    }
}
