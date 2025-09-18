package com.ai.agent.real.web.controller;

import com.ai.agent.real.application.tool.*;
import com.ai.agent.real.contract.protocol.*;
import com.ai.agent.real.contract.service.*;
import com.ai.agent.real.contract.spec.*;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.*;

import java.util.*;

/**
 * @author han
 * @time 2025/9/16 14:09
 */

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class ToolController {

    private final ToolService toolService;
    private final ToolExecutionService toolExecutionService;

    public ToolController(ToolService toolService,
                          ToolExecutionService toolExecutionService) {
        this.toolService = toolService;
        this.toolExecutionService = toolExecutionService;
    }


    @GetMapping("/tools_refresh")
    public Mono<ResponseResult<List<AgentTool>>> listToolsRefresh() {
        return toolService.listAllAgentToolsRefreshAsync()
                .map(ResponseResult::success)
                // 发生异常时，返回错误响应
                .onErrorResume(e -> Mono.just(
                        ResponseResult.error("list Tools failed: " + e.getMessage())
                ));
    }

    @GetMapping("/tools")
    public Mono<ResponseResult<List<AgentTool>>> listTools() {
        return toolService.listAllAgentToolsCachedAsync()
                .map(ResponseResult::success)
                // 发生异常时，返回错误响应
                .onErrorResume(e -> Mono.just(
                        ResponseResult.error("list Tools failed: " + e.getMessage())
                ));
    }

    @PostMapping("/tools/execute")
    public Mono<ResponseResult<ToolExecuteResult>> executeTool(@RequestBody ExecuteToolRequest request){
        if (request == null || request.getToolName() == null || request.getToolName().isBlank()){
            return Mono.just(ResponseResult.paramError("参数错误: toolName 不能为空"));
        }
        return Mono.fromCallable(() -> toolExecutionService.execute(request.getToolName(), request.getArgs()))
                .map(res -> {
                    if (res != null && res.isOk()){
                        return ResponseResult.success(res);
                    } else {
                        ResponseResult<ToolExecuteResult> rr = ResponseResult.error(ResponseResult.ERROR_CODE,
                                "工具执行失败: " + (res != null && res.getMessage() != null ? res.getMessage() : "unknown"));
                        rr.setData(res);
                        return rr;
                    }
                })
                .onErrorResume(e -> Mono.just(ResponseResult.error("工具执行异常: " + e.getMessage())));
    }
}
