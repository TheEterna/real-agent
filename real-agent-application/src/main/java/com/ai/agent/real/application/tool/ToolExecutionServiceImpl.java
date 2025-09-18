package com.ai.agent.real.application.tool;

import com.ai.agent.real.common.utils.ToolUtils;
import com.ai.agent.real.contract.protocol.ToolExecuteResult;
import com.ai.agent.real.contract.protocol.ToolLogEntry;
import com.ai.agent.real.contract.spec.AgentContext;
import com.ai.agent.real.contract.spec.AgentTool;
import com.ai.agent.real.contract.service.ToolService;
import com.ai.agent.real.contract.spec.ToolResult;
import io.modelcontextprotocol.client.McpAsyncClient;
import org.springframework.ai.mcp.AsyncMcpToolCallback;
import reactor.core.publisher.Flux;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

public class ToolExecutionServiceImpl implements ToolExecutionService {

    private final ToolService toolService;
    private final List<McpAsyncClient> mcpAsyncClients;

    public ToolExecutionServiceImpl(ToolService toolService, List<McpAsyncClient> mcpAsyncClients) {
        this.toolService = toolService;
        this.mcpAsyncClients = mcpAsyncClients != null ? mcpAsyncClients : List.of();
    }

    @Override
    public ToolExecuteResult execute(String toolName, Map<String, Object> args){
        String traceId = UUID.randomUUID().toString();
        long start = System.currentTimeMillis();
        List<ToolLogEntry> logs = new ArrayList<>();
        try{
            AgentTool tool = resolveToolByName(toolName).orElseThrow(() -> new NoSuchElementException("未知工具: " + toolName));

            AgentContext ctx = new AgentContext();
            ctx.setTraceId(traceId);
            ctx.setToolArgs(args != null ? new HashMap<>(args) : new HashMap<>());
            ctx.setStartTime(null); // 使用 ToolExecuteResult.timing 输出 ISO 字符串

            ToolResult<?> tr = tool.execute(ctx);
            long end = System.currentTimeMillis();

            Map<String,Object> metrics = new HashMap<>();
            metrics.put("latencyMs", tr != null ? tr.getElapsedMs() : (end - start));

            Object resultPayload = (tr != null ? tr.getData() : null);
            ToolExecuteResult res = ToolExecuteResult.ok(toolName, resultPayload)
                    .withLogs(logs)
                    .withMetrics(metrics)
                    .withTraceId(traceId)
                    .withTiming(start, end);
            return res;
        }catch(Exception ex){
            long end = System.currentTimeMillis();
            logs.add(ToolLogEntry.error("Exception: " + ex.getMessage(), isoNow()))
            ;
            Map<String,Object> metrics = Map.of("latencyMs", end - start);
            return ToolExecuteResult.fail(toolName, ex.getMessage())
                    .withLogs(logs)
                    .withMetrics(metrics)
                    .withTraceId(traceId)
                    .withTiming(start, end);
        }
    }

    private Optional<AgentTool> resolveToolByName(String toolName){
        // 1) system tool
        AgentTool sys = toolService.get(toolName);
        if (sys != null) {
            return Optional.of(sys);
        }
        // 2) mcp tools (async clients -> listTools -> to AgentTool)
        try{
            List<AgentTool> mcpTools = Flux.fromIterable(mcpAsyncClients)
                    .flatMap(mcpClient -> mcpClient.listTools()
                            .map(response -> response.tools().stream()
                                    .map(tool -> AsyncMcpToolCallback.builder().mcpClient(mcpClient).tool(tool).prefixedToolName(tool.name()).build())
                                    .map(cb -> ToolUtils.convertToolCallback2AgentTool(cb, mcpClient))
                                    .collect(Collectors.toList())
                            )
                    ).blockLast();
            if (mcpTools != null){
                for (AgentTool t : mcpTools){
                    if (t != null && t.getSpec() != null && toolName.equals(t.getSpec().getName())){
                        return Optional.of(t);
                    }
                }
            }
        }catch (Exception ignore){
        }
        return Optional.empty();
    }

    private static String isoNow(){
        return DateTimeFormatter.ISO_OFFSET_DATE_TIME.withZone(ZoneOffset.UTC).format(Instant.now());
    }
}
