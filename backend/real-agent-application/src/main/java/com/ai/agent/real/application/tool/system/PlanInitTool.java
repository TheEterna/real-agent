package com.ai.agent.real.application.tool.system;

import com.ai.agent.real.common.utils.CommonUtils;
import com.ai.agent.real.contract.agent.context.AgentContextAble;
import com.ai.agent.real.common.exception.ToolException;
import com.ai.agent.real.contract.model.protocol.ToolResult;
import com.ai.agent.real.contract.tool.AgentTool;
import com.ai.agent.real.contract.tool.ToolSpec;
import com.ai.agent.real.contract.model.context.reactplus.ReActPlusAgentContextMeta;
import com.ai.agent.real.contract.model.context.reactplus.TaskModeMeta;
import lombok.Data;
import org.springframework.ai.tool.annotation.ToolParam;

import java.util.List;
import java.util.stream.IntStream;

import static com.ai.agent.real.common.constant.NounConstants.PLAN_INIT;

/**
 * a tool of init or updating plan
 *
 * @author han
 * @time 2025/10/30 02:07
 */
public class PlanInitTool implements AgentTool {

	private final ToolSpec spec = new ToolSpec().setName(PLAN_INIT)
		.setDescription(
				"""
						用于创建初始计划，用于复杂任务的预处理
						示例：一个 Web 开发任务的 Plan 结构

						假设用户要求“开发一个简单的用户注册和登录的全栈网站”。

						Agent 初始调用该工具，返回结果如下

						```JSON
						{
						  "goal": "有注册、登录的简单全栈Web程序开发",
						  "phases": [
						    {
						      "id": 1,
						      "title": "项目初始化和环境搭建",
						      "description": "此阶段负责搭建项目的基础开发环境和代码结构。主要任务包括：1) 选择合适的技术栈（如前端React/Vue，后端Node.js/Python/Java，数据库MySQL/MongoDB）；2) 创建项目目录结构和版本控制仓库；3) 配置开发环境（安装必要的开发工具、包管理器、依赖库）；4) 设置基础的项目配置文件（如package.json、webpack.config.js、数据库配置等）；5) 搭建基础的前后端项目框架，确保开发环境能够正常运行。",
						      "isParallel": false
						    },
						    {
						      "id": 2,
						      "title": "设计数据库结构和用户认证逻辑以及接口文档，前后端交互规范",
						      "description": "此阶段专注于项目的设计工作，为后续开发提供清晰的指导。主要任务包括：1) 设计用户数据模型，定义用户表结构（包含用户名、邮箱、密码哈希、创建时间、状态等字段）；2) 设计用户认证逻辑，包括密码加密策略（如使用bcrypt）、JWT令牌机制、会话管理方案；3) 制定API接口规范，定义注册、登录、注销等接口的请求参数、响应格式、状态码；4) 创建详细的接口文档，包含接口说明、请求示例、响应示例；5) 确定前后端交互规范，包括数据格式（JSON）、错误处理机制、状态码定义、跨域处理方案等。",
						      "isParallel": false
						    },
						    {
						      "id": 3,
						      "title": "实现后端 API（注册、登录、会话管理）和实现前端界面（注册表单、登录表单）",
						      "description": "此阶段为并行开发阶段，前后端团队同时进行开发工作。后端开发任务：1) 实现用户注册API，包括数据验证、密码加密、用户创建；2) 实现用户登录API，包括身份验证、JWT令牌生成；3) 实现会话管理功能，包括令牌验证、刷新令牌、注销功能；4) 实现用户信息查询和更新API；5) 添加输入验证、错误处理、日志记录等功能。前端开发任务：1) 创建用户注册表单界面，包含用户名、邮箱、密码等输入框和验证逻辑；2) 创建用户登录表单界面，包含登录方式选择、记住密码功能；3) 实现表单验证逻辑，确保输入数据的有效性；4) 开发前后端交互逻辑，调用后端API完成用户认证；5) 实现用户状态管理，处理登录状态、令牌存储、页面跳转等。",
						      "isParallel": true
						    },
						    {
						      "id": 4,
						      "title": "联调测试与最终交付",
						      "description": "此阶段负责项目的集成测试和最终交付。主要任务包括：1) 进行前后端联调测试，确保所有API接口正常工作；2) 测试用户注册、登录、注销等核心功能的完整性和稳定性；3) 进行安全性测试，验证密码加密、令牌机制、输入验证等安全措施；4) 进行性能测试，评估系统在不同负载下的响应性能；5) 修复测试中发现的bug和问题；6) 完善项目文档，包括部署指南、用户手册、API文档等；7) 准备项目交付物，包括源代码、可执行文件、部署脚本等；8) 部署应用程序到生产环境，确保系统能够正常运行。",
						      "isParallel": false
						    }
						  ]
						}
						```

						""")
		.setCategory("system")
		.setInputSchemaClass(PlanInitTool.PlanInitToolDto.class);

	/**
	 * 获取工具的唯一标识, 如果重复, 会抛出异常
	 * @return 工具的名称
	 */
	@Override
	public String getId() {
		return PLAN_INIT;
	}

	/**
	 * get the specification of tool
	 * @return 工具的spec
	 */
	@Override
	public ToolSpec getSpec() {
		return this.spec;
	}

	/**
	 * execute tool, note: should catch Exception to cast ToolResult
	 * @param context 上下文
	 * @return 工具执行结果
	 * @throws ToolException 工具执行异常
	 */
	@Override
	public ToolResult<Object> execute(AgentContextAble<?> context) {

		long start = System.currentTimeMillis();

		PlanInitToolDto structuralToolArgs = context.getStructuralToolArgs(PlanInitToolDto.class);
		// 1. 设置 context 的 执行模式，处理 dto 参数
		List<PlanInitTool.Phase> phases = structuralToolArgs.getPhases();

		List<TaskModeMeta.TaskPhase> taskPhaseList = IntStream.range(0, phases.size()).mapToObj(index -> {
			// 通过索引获取当前 phase 元素
			Phase phase = phases.get(index);
			// 构造 TaskPhase，传入当前索引（index）
			return new TaskModeMeta.TaskPhase(CommonUtils.generateUuidToken(), phase.getTitle(), phase.getDescription(),
					index, // 直接使用当前索引作为 index
					phase.isParallel(), TaskModeMeta.TaskStatus.TODO);
		}).toList();
		// 2. 构造设置, 进行最终组装并设置
		ReActPlusAgentContextMeta meta = ReActPlusAgentContextMeta.taskModeMetaBuilder()
			.goal(structuralToolArgs.getGoal())
			.taskPhaseList(taskPhaseList)
			.currentTaskId(taskPhaseList.get(0).getId())
			.build();

		context.setMetadata(meta);

		return ToolResult.ok(meta, start - System.currentTimeMillis(), getId());
	}

	@Data
	public static class PlanInitToolDto {

		@ToolParam(required = true, description = """
				**任务总体目标**
				- 清晰简洁地描述任务的最终目的，一般在20字以内，不能超过25字
				- 作为整个计划执行的核心指引
				- 例如："用户季度消费数据分析及报告生成"
				- 往往一般情况下，总体目标是不会改变的，除非任务执行、权威推理过程中，发现目前确立的总体目标严重失实，你需要对总体目标进行严谨的修正
				""")
		private String goal;

		@ToolParam(required = true, description = """
				**任务阶段列表**
				- 包含计划的所有执行阶段，每个阶段为一个对象
				- 阶段需按执行顺序排列，确保流程连贯性
				- 每个阶段必须包含id（正整数，从1开始递增）和title（阶段标题）
				""")
		private List<Phase> phases;

	}

	@Data
	public static class Phase {

		@ToolParam(required = true, description = """
				**阶段标题**
				- 简要描述该阶段需要完成的工作
				- 例如："数据采集与清洗"、"模型训练与调优"
				""")
		private String title;

		@ToolParam(required = true, description = """
				**阶段任务描述**
				- 详细说明该阶段需要完成的具体工作和交付成果
				- 包含可执行的操作步骤和明确的完成标准
				- 例如："采集近3个月用户行为日志，清洗重复数据和缺失值，输出标准化数据集"
				- 简单描述执行该阶段时需要特别关注的要点、约束或风险
				- 例如："需验证数据源准确性，过滤异常值"、"注意用户隐私数据加密"
				- 用于提示执行过程中的关键考量因素
				""")
		private String description;

		@ToolParam(required = true, description = """
				**是否并行执行**
				- 默认不并行
				- 描述执行该阶段任务时是否可以并行执行，比如 规划做饭任务，可以将 切菜 和 烧水 合并成一个任务，然后并行处理它
				""")
		private boolean isParallel;

	}

}
