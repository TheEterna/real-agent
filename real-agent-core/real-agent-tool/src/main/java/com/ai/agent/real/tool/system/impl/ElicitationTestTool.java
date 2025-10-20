package com.ai.agent.real.tool.system.impl;

import com.ai.agent.real.contract.model.*;
import com.ai.agent.real.contract.model.context.*;
import com.ai.agent.real.contract.model.protocol.*;
import com.ai.agent.real.contract.model.protocol.ToolResult.*;
import com.fasterxml.jackson.annotation.*;
import lombok.*;
import lombok.extern.slf4j.*;
import reactor.core.publisher.*;

import java.util.*;

/**
 * Elicitation 测试工具
 *
 * 用于测试 MCP elicitation 功能的完整流程： 1. 向用户发送 schema 请求 2. 等待用户填写数据 3. 接收用户数据并继续处理
 *
 * @author 李大飞
 */
@Slf4j
public class ElicitationTestTool implements AgentTool {

	private final String id = "elicitation_test";

	private final ToolSpec spec = new ToolSpec().setName("elicitation_test")
		.setDescription("测试 elicitation 功能，向用户请求输入特定格式的数据")
		.setCategory("test")
		.setInputSchemaClass(ElicitationTestInput.class);

	@Override
	public String getId() {
		return id;
	}

	@Override
	public ToolSpec getSpec() {
		return spec;
	}

	@Override
	public ToolResult<Object> execute(AgentContext ctx) {
		return executeAsync(ctx).block();
	}

	@Override
	public Mono<ToolResult<Object>> executeAsync(AgentContext ctx) {
		long start = System.currentTimeMillis();

		try {
			ElicitationTestInput input = ctx.getStructuralToolArgs(ElicitationTestInput.class);
			log.info("开始执行 elicitation 测试: testType={}", input.getTestType());

			return doElicitationTest(input, ctx)
				.map(result -> ToolResult.ok(result, System.currentTimeMillis() - start, getId()));

		}
		catch (Exception e) {
			log.error("Elicitation 测试工具执行异常", e);
			return Mono.just(ToolResult.error(ToolResultCode.TOOL_EXECUTION_ERROR, e.getMessage(), getId(),
					System.currentTimeMillis() - start));
		}
	}

	/**
	 * 执行具体的 elicitation 测试
	 */
	private Mono<String> doElicitationTest(ElicitationTestInput input, AgentContext ctx) {
		// 根据测试类型创建不同的 schema
		Map<String, Object> schema = createTestSchema(input.getTestType());
		String message = createTestMessage(input.getTestType());

		// 检查上下文中是否有 McpElicitation 实例
		// 注意：这里假设您的工具上下文中有 McpElicitation 实例
		// 如果没有，需要通过其他方式获取 elicitation 功能

		// 模拟 elicitation 请求
		log.info("发送 elicitation 请求: message={}, schema={}", message, schema);

		// 这里需要调用您的 elicitation 功能
		// 由于我们在测试环境中，暂时返回模拟结果
		return Mono.just("Elicitation 测试完成。在实际场景中，这里会显示用户填写的数据。" + "\n请求消息: " + message + "\nSchema: " + schema);
	}

	/**
	 * 根据测试类型创建测试 schema
	 */
	private Map<String, Object> createTestSchema(String testType) {
		Map<String, Object> schema = new HashMap<>();
		schema.put("type", "object");

		Map<String, Object> properties = new HashMap<>();
		List<String> required = new ArrayList<>();

		switch (testType.toLowerCase()) {
			case "simple":
				// 简单文本输入测试
				properties.put("name", Map.of("type", "string", "title", "姓名", "description", "请输入您的姓名"));
				properties.put("message", Map.of("type", "string", "title", "留言", "description", "请输入您想说的话"));
				required.addAll(List.of("name"));
				break;

			case "complex":
				// 复杂表单测试
				properties.put("personal_info",
						Map.of("type", "object", "title", "个人信息", "properties",
								Map.of("name", Map.of("type", "string", "title", "姓名"), "age",
										Map.of("type", "number", "title", "年龄", "minimum", 0, "maximum", 150), "email",
										Map.of("type", "string", "title", "邮箱", "format", "email")),
								"required", List.of("name", "age")));
				properties.put("preferences", Map.of("type", "object", "title", "偏好设置", "properties",
						Map.of("language",
								Map.of("type", "string", "title", "语言", "enum", List.of("中文", "English", "日本語")),
								"notifications", Map.of("type", "boolean", "title", "接收通知"))));
				required.addAll(List.of("personal_info"));
				break;

			case "survey":
				// 问卷调查测试
				properties.put("rating", Map.of("type", "number", "title", "满意度评分", "description", "请为我们的服务打分（1-10分）",
						"minimum", 1, "maximum", 10));
				properties.put("feedback", Map.of("type", "string", "title", "反馈意见", "description", "请分享您的使用体验和建议"));
				properties.put("recommend",
						Map.of("type", "boolean", "title", "是否推荐", "description", "您是否愿意向朋友推荐我们的服务？"));
				required.addAll(List.of("rating", "recommend"));
				break;

			default:
				// 默认测试
				properties.put("input", Map.of("type", "string", "title", "测试输入", "description", "请输入任意内容进行测试"));
				required.add("input");
		}

		schema.put("properties", properties);
		schema.put("required", required);

		return schema;
	}

	/**
	 * 根据测试类型创建测试消息
	 */
	private String createTestMessage(String testType) {
		switch (testType.toLowerCase()) {
			case "simple":
				return "这是一个简单的 elicitation 测试。请填写下面的表单来测试数据传输功能。";
			case "complex":
				return "这是一个复杂表单的 elicitation 测试。请填写详细的个人信息和偏好设置。";
			case "survey":
				return "感谢您参与我们的满意度调查！您的反馈对我们非常重要。";
			default:
				return "欢迎使用 elicitation 功能测试工具！请在下方填写测试数据。";
		}
	}

	/**
	 * 工具输入参数
	 */
	@Data
	@NoArgsConstructor
	@JsonClassDescription("Elicitation 测试工具参数")
	public static class ElicitationTestInput {

		@JsonProperty(required = true)
		@JsonPropertyDescription("测试类型：simple（简单测试）、complex（复杂表单）、survey（问卷调查）、default（默认测试）")
		private String testType = "simple";

		@JsonPropertyDescription("自定义测试消息（可选）")
		private String customMessage;

	}

}