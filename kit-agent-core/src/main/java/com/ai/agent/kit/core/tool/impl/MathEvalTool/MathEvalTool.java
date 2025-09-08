package com.ai.agent.kit.core.tool.impl.MathEvalTool;

import com.ai.agent.kit.common.exception.*;
import com.ai.agent.kit.common.spec.*;
import com.ai.agent.kit.core.agent.communication.*;
import com.ai.agent.kit.core.tool.model.*;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.SimpleBindings;
import java.util.Map;

public class MathEvalTool implements AgentTool {
    private final ToolSpec spec = new ToolSpec()
            .setName("数学计算")
            .setDescription("Evaluate math expression: + - * / () and Math.*")
            .setCategory("utility");


    /**
     * 获取工具的唯一标识, 如果重复, 会抛出异常
     *
     * @return 工具的名称
     */
    @Override
    public String Id() {
        return "math.eval";
    }

    @Override
    public ToolSpec getSpec(){ return spec; }

    @Override
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
        return null;
    }
}
