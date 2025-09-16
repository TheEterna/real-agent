package com.ai.agent.real.tool.system;

import com.ai.agent.real.contract.spec.*;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.*;
import java.util.stream.Collectors;

/**
 * 基于关键词的工具注册器
 * 实现工具与关键词的绑定，支持Agent根据关键词获取可用工具
 * 
 * @author han
 * @time 2025/9/7 01:20
 */

@Slf4j
public class ToolRegistryImpl implements ToolRegistry {

    private final Map<String, AgentTool> tools = new ConcurrentHashMap<>();

    private void register(AgentTool tool) {
        if (tool == null || tool.getSpec().getName() == null) {
            return;
        }
        tools.put(tool.getSpec().getName(), tool);
    }

    /**
     * 关键词到工具的映射
     * key: 关键词, value: 工具名称集合
     */
    private final Map<String, Set<String>> keywordToTools = new ConcurrentHashMap<>();
    
    /**
     * 工具到关键词的映射
     * key: 工具名称, value: 关键词集合
     */
    private final Map<String, Set<String>> toolToKeywords = new ConcurrentHashMap<>();




    /**
     * get tool by name
     */
    @Override
    public AgentTool get(String name) {
        return tools.get(name);
    }

    /**
     * 获取所有已注册的工具
     */
    public List<AgentTool> list() {
        return new ArrayList<>(tools.values());
    }
    /**
     * 根据单个关键词获取工具
     */
    @Override
    public List<AgentTool> getToolsByKeyword(String keyword) {
        return getToolsByKeywords(Set.of(keyword));
    }


    /**
     * 注册工具并绑定关键词
     */
    @Override
    public void registerWithKeywords(AgentTool tool, Set<String> keywords) {
        // 先注册工具
        this.register(tool);

        String toolName = tool.getSpec().getName();

        // 建立关键词绑定
        toolToKeywords.put(toolName, new HashSet<>(keywords));

        for (String keyword : keywords) {
            keywordToTools.computeIfAbsent(keyword.toLowerCase(), k -> new HashSet<>())
                    .add(toolName);
        }

        log.info("工具 [{}] 注册成功，绑定关键词: {}", toolName, keywords);
    }




    /**
     * 根据关键词获取可用工具
     */
    @Override
    public List<AgentTool> getToolsByKeywords(Set<String> keywords) {
        Set<String> matchedToolNames = new HashSet<>();

        for (String keyword : keywords) {
            Set<String> tools = keywordToTools.get(keyword.toLowerCase());
            if (tools != null) {
                matchedToolNames.addAll(tools);
            }
        }

        return matchedToolNames.stream()
                .map(this::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }
//    /**
//     * 获取工具的关键词
//     */
//    public Set<String> getKeywordsForTool(String toolName) {
//        return toolToKeywords.getOrDefault(toolName, new HashSet<>());
//    }

//    /**
//     * 为已注册的工具添加关键词
//     */
//    public void addKeywordsToTool(String toolName, Set<String> newKeywords) {
//        if (!toolToKeywords.containsKey(toolName)) {
//            log.warn("工具 [{}] 未注册，无法添加关键词", toolName);
//            return;
//        }
//
//        Set<String> existingKeywords = toolToKeywords.get(toolName);
//        existingKeywords.addAll(newKeywords);
//
//        // 更新关键词到工具的映射
//        for (String keyword : newKeywords) {
//            keywordToTools.computeIfAbsent(keyword.toLowerCase(), k -> new HashSet<>())
//                    .add(toolName);
//        }
//
//        log.info("为工具 [{}] 添加关键词: {}", toolName, newKeywords);
//    }

//    /**
//     * 移除工具的关键词
//     */
//    public void removeKeywordsFromTool(String toolName, Set<String> keywordsToRemove) {
//        Set<String> existingKeywords = toolToKeywords.get(toolName);
//        if (existingKeywords == null) {
//            return;
//        }
//
//        existingKeywords.removeAll(keywordsToRemove);
//
//        // 更新关键词到工具的映射
//        for (String keyword : keywordsToRemove) {
//            Set<String> tools = keywordToTools.get(keyword.toLowerCase());
//            if (tools != null) {
//                tools.remove(toolName);
//                if (tools.isEmpty()) {
//                    keywordToTools.remove(keyword.toLowerCase());
//                }
//            }
//        }
//
//        log.info("从工具 [{}] 移除关键词: {}", toolName, keywordsToRemove);
//    }
//    /**
//     * 根据关键词匹配度排序工具
//     */
//    @Override
//    public List<AgentTool> getToolsByKeywordsRanked(Set<String> keywords) {
//        Map<String, Integer> toolScores = new HashMap<>();
//
//        for (String keyword : keywords) {
//            Set<String> tools = keywordToTools.get(keyword.toLowerCase());
//            if (tools != null) {
//                for (String toolName : tools) {
//                    toolScores.merge(toolName, 1, Integer::sum);
//                }
//            }
//        }
//
//        return toolScores.entrySet().stream()
//                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
//                .map(entry -> get(entry.getKey()))
//                .filter(Optional::isPresent)
//                .map(Optional::get)
//                .collect(Collectors.toList());
//    }
//

}
