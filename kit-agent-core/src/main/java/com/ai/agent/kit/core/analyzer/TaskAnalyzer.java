package com.ai.agent.kit.core.analyzer;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.regex.Pattern;

/**
 * 任务分析器
 * 分析任务类型和复杂度，为策略选择和Agent组合提供依据
 * 
 * @author han
 * @time 2025/9/7 01:15
 */
@Slf4j
public class TaskAnalyzer {
    
    /**
     * 任务类型定义
     */
    public enum TaskType {
        CODE_ANALYSIS("代码分析", Arrays.asList("代码", "分析", "审查", "重构", "优化", "bug", "性能")),
        DOCUMENTATION("文档生成", Arrays.asList("文档", "说明", "手册", "readme", "注释", "规范")),
        DATA_RESEARCH("资料搜索", Arrays.asList("搜索", "查找", "资料", "信息", "网络", "参考")),
        CODE_GENERATION("代码生成", Arrays.asList("编写", "生成", "创建", "实现", "开发", "代码")),
        GENERAL_QA("通用问答", Arrays.asList("问题", "解答", "咨询", "帮助", "建议"));
        
        private final String description;
        private final List<String> keywords;
        
        TaskType(String description, List<String> keywords) {
            this.description = description;
            this.keywords = keywords;
        }
        
        public String getDescription() { return description; }
        public List<String> getKeywords() { return keywords; }
    }
    
    /**
     * 复杂度级别
     */
    public enum ComplexityLevel {
        SIMPLE(1, "简单任务"),
        MEDIUM(2, "中等复杂度"),
        COMPLEX(3, "复杂任务"),
        VERY_COMPLEX(4, "极复杂任务");
        
        private final int level;
        private final String description;
        
        ComplexityLevel(int level, String description) {
            this.level = level;
            this.description = description;
        }
        
        public int getLevel() { return level; }
        public String getDescription() { return description; }
    }
    
    /**
     * 任务分析结果
     */
    @Data
    public static class TaskAnalysisResult {
        private String originalTask;
        private Set<TaskType> taskTypes;
        private ComplexityLevel complexity;
        private List<String> extractedKeywords;
        private Map<String, Double> typeConfidence;
        private String recommendedStrategy;
        private List<String> recommendedAgents;
        
        public TaskAnalysisResult(String originalTask) {
            this.originalTask = originalTask;
            this.taskTypes = new HashSet<>();
            this.extractedKeywords = new ArrayList<>();
            this.typeConfidence = new HashMap<>();
            this.recommendedAgents = new ArrayList<>();
        }
    }
    
    /**
     * 分析任务
     */
    public TaskAnalysisResult analyze(String task) {
        log.info("开始分析任务: {}", task);
        
        TaskAnalysisResult result = new TaskAnalysisResult(task);
        
        // 1. 提取关键词
        result.setExtractedKeywords(extractKeywords(task));
        
        // 2. 分析任务类型
        analyzeTaskTypes(task, result);
        
        // 3. 分析复杂度（暂时简化实现）
        result.setComplexity(analyzeComplexity(task));
        
        // 4. 推荐策略
        result.setRecommendedStrategy(recommendStrategy(result));
        
        // 5. 推荐Agent组合
        result.setRecommendedAgents(recommendAgents(result));
        
        log.info("任务分析完成: 类型={}, 复杂度={}, 推荐策略={}", 
                result.getTaskTypes(), result.getComplexity(), result.getRecommendedStrategy());
        
        return result;
    }
    
    /**
     * 提取关键词
     */
    private List<String> extractKeywords(String task) {
        List<String> keywords = new ArrayList<>();
        String lowerTask = task.toLowerCase();
        
        // 简单的关键词提取逻辑
        for (TaskType type : TaskType.values()) {
            for (String keyword : type.getKeywords()) {
                if (lowerTask.contains(keyword)) {
                    keywords.add(keyword);
                }
            }
        }
        
        return keywords;
    }
    
    /**
     * 分析任务类型
     */
    private void analyzeTaskTypes(String task, TaskAnalysisResult result) {
        String lowerTask = task.toLowerCase();
        
        for (TaskType type : TaskType.values()) {
            double confidence = calculateTypeConfidence(lowerTask, type);
            result.getTypeConfidence().put(type.name(), confidence);
            
            if (confidence > 0.3) { // 置信度阈值
                result.getTaskTypes().add(type);
            }
        }
        
        // 如果没有匹配到具体类型，默认为通用问答
        if (result.getTaskTypes().isEmpty()) {
            result.getTaskTypes().add(TaskType.GENERAL_QA);
            result.getTypeConfidence().put(TaskType.GENERAL_QA.name(), 0.5);
        }
    }
    
    /**
     * 计算类型置信度
     */
    private double calculateTypeConfidence(String task, TaskType type) {
        double confidence = 0.0;
        int matchCount = 0;
        
        for (String keyword : type.getKeywords()) {
            if (task.contains(keyword)) {
                matchCount++;
                confidence += 0.2; // 每个关键词匹配增加0.2置信度
            }
        }
        
        // 根据匹配关键词数量调整置信度
        if (matchCount > 0) {
            confidence = Math.min(1.0, confidence + (matchCount * 0.1));
        }
        
        return confidence;
    }
    
    /**
     * 分析复杂度
     */
    private ComplexityLevel analyzeComplexity(String task) {
        // 简化的复杂度分析逻辑
        String lowerTask = task.toLowerCase();
        int complexityScore = 0;
        
        // 复杂度指标
        String[] complexIndicators = {"重构", "架构", "系统", "框架", "多个", "复杂", "完整", "全面"};
        String[] mediumIndicators = {"分析", "生成", "创建", "实现", "优化"};
        
        for (String indicator : complexIndicators) {
            if (lowerTask.contains(indicator)) {
                complexityScore += 2;
            }
        }
        
        for (String indicator : mediumIndicators) {
            if (lowerTask.contains(indicator)) {
                complexityScore += 1;
            }
        }
        
        // 根据任务长度调整复杂度
        if (task.length() > 100) {
            complexityScore += 1;
        }
        if (task.length() > 200) {
            complexityScore += 1;
        }
        
        if (complexityScore >= 4) {
            return ComplexityLevel.VERY_COMPLEX;
        }
        if (complexityScore >= 3) {
            return ComplexityLevel.COMPLEX;
        }
        if (complexityScore >= 1) {
            return ComplexityLevel.MEDIUM;
        }
        return ComplexityLevel.SIMPLE;
    }
    
    /**
     * 推荐策略
     */
    private String recommendStrategy(TaskAnalysisResult result) {
        int typeCount = result.getTaskTypes().size();
        ComplexityLevel complexity = result.getComplexity();
        
        // 策略选择逻辑
        if (typeCount == 1 && complexity == ComplexityLevel.SIMPLE) {
            return "SingleAgent";
        } else if (typeCount > 1 || complexity.getLevel() >= 3) {
            return "Collaborative";
        } else if (complexity == ComplexityLevel.MEDIUM) {
            return "Pipeline";
        } else {
            return "SingleAgent";
        }
    }
    
    /**
     * 推荐Agent组合
     */
    private List<String> recommendAgents(TaskAnalysisResult result) {
        List<String> agents = new ArrayList<>();
        
        for (TaskType type : result.getTaskTypes()) {
            switch (type) {
                case CODE_ANALYSIS:
                    agents.add("code-analysis-agent");
                    break;
                case DOCUMENTATION:
                    agents.add("documentation-agent");
                    break;
                case DATA_RESEARCH:
                    agents.add("research-agent");
                    break;
                case CODE_GENERATION:
                    agents.add("code-generation-agent");
                    break;
                case GENERAL_QA:
                    agents.add("general-purpose-agent");
                    break;
            }
        }
        
        // 复杂任务可能需要通用Agent协助
        if (result.getComplexity().getLevel() >= 3 && !agents.contains("general-purpose-agent")) {
            agents.add("general-purpose-agent");
        }
        
        return agents;
    }
}
