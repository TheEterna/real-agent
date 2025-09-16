package com.ai.agent.real.contract.spec;

import java.util.*;
import java.util.stream.*;

/**
 * @author han
 * @time 2025/9/15 14:37
 */

public interface ToolRegistry {


    AgentTool get(String name);

    List<AgentTool> list();

    /**
     * 根据关键词获取可用工具
     */
    List<AgentTool> getToolsByKeywords(Set<String> keywords);


    /**
     * 根据单个关键词获取工具
     */
    List<AgentTool> getToolsByKeyword(String keyword);

    /**
     * 注册工具并绑定关键词
     */
    void registerWithKeywords(AgentTool tool, Set<String> keywords);

//    /**
//     * 根据关键词匹配度排序工具
//     */
//    List<AgentTool> getToolsByKeywordsRanked(Set<String> keywords);
}
