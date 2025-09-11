package com.ai.agent.contract.spec;

import lombok.*;
import lombok.experimental.*;

import java.util.Map;

/**
 * ToolSpec 类定义了工具的规范。
 * @author han
 * @time 2025/8/30 17:02
 */
@Data
@Accessors(chain = true)
public class ToolSpec {

    /**
     * 工具的名称，用于唯一标识工具
     */
    private String name;

    /**
     * 工具的描述，用于提供工具的详细信息
     */
    private String description;

    /**
     * 工具的分类标识，用于对工具进行分类管理
     */
    private String category;

    /**
     * 工具输入参数的Schema定义，用于描述工具所需的输入参数结构和类型
     * key为参数名，value为参数的相关信息（如类型、描述等）
     */
    private Map<String, Object> inputSchema;

    /**
     * 工具输出参数的Schema定义，用于描述工具返回的输出参数结构和类型
     * key为参数名，value为参数的相关信息（如类型、描述等）
     */
    private Map<String, Object> outputSchema;


}
