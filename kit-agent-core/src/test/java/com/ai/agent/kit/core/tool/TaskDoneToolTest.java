package com.ai.agent.kit.core.tool;

import com.ai.agent.kit.core.tool.impl.TaskDoneTool;
import org.junit.jupiter.api.Test;
import org.springframework.ai.tool.definition.ToolDefinition;

import static org.junit.jupiter.api.Assertions.*;

/**
 * TaskDoneTool测试类
 * 验证Spring AI兼容性和工具定义生成
 */
public class TaskDoneToolTest {

    @Test
    public void testToolDefinitionGeneration() {
        TaskDoneTool tool = new TaskDoneTool();
        
        // 测试ToolDefinition生成
        ToolDefinition definition = tool.getToolDefinition();
        
        assertNotNull(definition);
        assertEquals("task_done", definition.name());
        assertEquals("标记任务已完成并提供最终结果", definition.description());
        assertNotNull(definition.inputSchema());
        
        // 验证JSON Schema包含必要字段
        String schema = definition.inputSchema();
        assertTrue(schema.contains("result"));
        assertTrue(schema.contains("summary"));
        assertTrue(schema.contains("required"));
        
        System.out.println("TaskDoneTool ToolDefinition:");
        System.out.println("Name: " + definition.name());
        System.out.println("Description: " + definition.description());
        System.out.println("Schema: " + schema);
    }

    @Test
    public void testSpringAIToolCall() {
        TaskDoneTool tool = new TaskDoneTool();
        
        // 测试Spring AI工具调用
        String jsonInput = "{\"result\":\"任务已成功完成\",\"summary\":\"测试任务执行总结\"}";
        String result = tool.call(jsonInput);
        
        assertNotNull(result);
        assertTrue(result.contains("任务已成功完成") || result.contains("执行成功"));
        
        System.out.println("Spring AI AgentTool Call Result: " + result);
    }

    @Test
    public void testInvalidInput() {
        TaskDoneTool tool = new TaskDoneTool();
        
        // 测试无效输入
        String invalidInput = "{\"result\":\"\"}";
        String result = tool.call(invalidInput);
        
        assertNotNull(result);
        assertTrue(result.contains("失败") || result.contains("错误"));
        
        System.out.println("Invalid Input Result: " + result);
    }
}
