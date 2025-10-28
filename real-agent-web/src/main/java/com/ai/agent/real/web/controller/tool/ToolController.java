package com.ai.agent.real.web.controller.tool;

import com.ai.agent.real.entity.agent.context.ReActAgentContext;
import com.ai.agent.real.contract.model.logging.*;
import com.ai.agent.real.contract.model.protocol.*;
import com.ai.agent.real.contract.service.*;
import com.ai.agent.real.contract.tool.AgentTool;
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

	public ToolController(ToolService toolService) {
		this.toolService = toolService;
	}

	@GetMapping("/tools_refresh")
	public Mono<ResponseResult<List<AgentTool>>> listToolsRefresh() {
		return toolService.listAllAgentToolsRefreshAsync()
			.map(ResponseResult::success)
			// 发生异常时，返回错误响应
			.onErrorResume(e -> Mono.just(ResponseResult.error("list Tools failed: " + e.getMessage())));
	}

	@GetMapping("/tools")
	public Mono<ResponseResult<List<AgentTool>>> listTools() {
		return toolService.listAllAgentToolsCachedAsync()
			.map(ResponseResult::success)
			// 发生异常时，返回错误响应
			.onErrorResume(e -> Mono.just(ResponseResult.error("list Tools failed: " + e.getMessage())));
	}

	@PostMapping("/tools/execute")
	public Mono<ResponseResult<? extends ToolResult<?>>> executeTool(@RequestBody ExecuteToolRequest request) {
		if (request == null || request.getToolName() == null || request.getToolName().isBlank()) {
			return Mono.just(ResponseResult.paramError("参数错误: toolName 不能为空"));
		}
		return toolService
			.executeToolAsync(request.getToolName(), ReActAgentContext.of(request.getArgs(), new TraceInfo()))
			.map(res -> {
				if (res != null && res.isOk()) {
					return ResponseResult.success(res);
				}
				else {
					ResponseResult<ToolResult<?>> responseResult = ResponseResult.error(ResponseResult.ERROR_CODE,
							"tool executed faild: "
									+ (res != null && res.getMessage() != null ? res.getMessage() : "unknown"));
					responseResult.setData(res);
					return responseResult;
				}
			})
			.onErrorResume(e -> Mono.just(ResponseResult.error("工具执行异常: " + e.getMessage())));
	}

	private ToolResult setFieldValue(ToolResult target, String fieldName, Object value) {
		try {
			// ToolResult类中没有setter方法，只能通过反射设置字段值
			java.lang.reflect.Field field = target.getClass().getDeclaredField(fieldName);
			field.setAccessible(true);
			field.set(target, value);
		}
		catch (Exception e) {
			// 如果设置失败，忽略异常
		}
		return target;
	}

}