package com.ai.agent.real.application.tool.system;

import com.ai.agent.real.contract.agent.context.AgentContextAble;
import com.ai.agent.real.contract.model.protocol.*;
import com.ai.agent.real.contract.tool.AgentTool;
import com.ai.agent.real.contract.tool.ToolSpec;
import com.ai.agent.real.entity.agent.context.ReActAgentContext;
import org.springframework.ai.tool.annotation.*;

public class MathEvalTool implements AgentTool {

	private final ToolSpec spec = new ToolSpec().setName("math_eval")
		.setDescription("Evaluate math expression: + - * / () and Math.*")
		.setCategory("utility");

	/**
	 * 获取工具的唯一标识, 如果重复, 会抛出异常
	 * @return 工具的名称
	 */
	@Override
	public String getId() {
		return "math.eval";
	}

	@Override
	public ToolSpec getSpec() {
		return spec;
	}

	@Override
	@Tool(description = "数学计算", name = "数学计算")
	public ToolResult<Object> execute(AgentContextAble ctx) {
		// long start = System.currentTimeMillis();
		// String expr = String.valueOf(.getOrDefault("expr",""));
		// if(expr.isEmpty()) {
		// return ToolResult.error("INVALID_ARG","expr is required");
		// }
		// try{
		// ScriptEngine engine = new ScriptEngineManager().getEngineByName("JavaScript");
		// Object val = engine.eval(expr, new SimpleBindings(Map.of("Math", Math.class)));
		// return ToolResult.ok(Map.of("result", val), System.currentTimeMillis()-start);
		// }catch(Exception e){
		// return ToolResult.error("EVAL_ERROR", e.getMessage());
		// }
		return ToolResult.ok("暂未开发", 0, getId());
	}

}
