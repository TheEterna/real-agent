package com.ai.agent.real.tool.system.impl.MathEvalTool;

import com.ai.agent.real.contract.exception.*;
import com.ai.agent.real.contract.protocol.*;
import com.ai.agent.real.contract.spec.*;
import org.springframework.ai.tool.annotation.*;

public class MathEvalTool implements AgentTool {
    private final ToolSpec spec = new ToolSpec()
            .setName("math_eval")
            .setDescription("Evaluate math expression: + - * / () and Math.*")
            .setCategory("utility");


    /**
     * 获取工具的唯一标识, 如果重复, 会抛出异常
     *
     * @return 工具的名称
     */
    @Override
    public String getId() {
        return "math.eval";
    }

    @Override
    public ToolSpec getSpec(){ return spec; }

    @Override
    @Tool(description = "数学计算", name = "数学计算")
    public ToolResult<?> execute(AgentContext ctx) throws ToolException {
//        long start = System.currentTimeMillis();
//        String expr = String.valueOf(.getOrDefault("expr",""));
//        if(expr.isEmpty()) {
//            return ToolResult.error("INVALID_ARG","expr is required");
//        }
//        try{
//            ScriptEngine engine = new ScriptEngineManager().getEngineByName("JavaScript");
//            Object val = engine.eval(expr, new SimpleBindings(Map.of("Math", Math.class)));
//            return ToolResult.ok(Map.of("result", val), System.currentTimeMillis()-start);
//        }catch(Exception e){
//            return ToolResult.error("EVAL_ERROR", e.getMessage());
//        }
        return ToolResult.ok("暂未开发", 0, getId());
    }


}
