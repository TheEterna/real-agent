package com.ai.agent.kit.core.tool.impl.HttpRequestTool;

import com.ai.agent.kit.common.exception.*;
import com.ai.agent.kit.common.spec.*;
import com.ai.agent.kit.core.agent.communication.*;
import com.ai.agent.kit.core.tool.model.*;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class HttpRequestTool implements AgentTool {
    private final SandboxPolicy policy;
    private final ToolSpec spec;

    public HttpRequestTool(SandboxPolicy policy) {
        this.policy = policy;
        this.spec = new ToolSpec()
                .setName("进行HTTP请求")
                .setDescription("执行受限的HTTP GET请求")
                .setCategory("network");
    }

    /**
     * 获取工具的唯一标识, 如果重复, 会抛出异常
     *
     * @return 工具的名称
     */
    @Override
    public String Id() {
        return "http.request";
    }

    @Override
    public ToolSpec getSpec() {
        return spec;
    }

    @Override
    public ToolResult<?> execute(AgentContext ctx) throws ToolException {
        long start = System.currentTimeMillis();

        Map<String, Object> args = ctx.getArgs();
        try {
            String urlStr = String.valueOf(args.getOrDefault("url", ""));
            if (urlStr.isEmpty()) {
                return ToolResult.error("INVALID_ARG", "url is required");
            }
            URL url = new URL(urlStr);
            if (!policy.isHostAllowed(url.getHost())) {
                return ToolResult.error("DENY_HOST", "host not allowed");
            }
            String method = String.valueOf(args.getOrDefault("method", "GET")).toUpperCase();
            if (!policy.isMethodAllowed(method)) {
                return ToolResult.error("DENY_METHOD", "method not allowed");
            }

            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod(method);
            conn.setConnectTimeout((int) Math.min(ctx.getTimeoutMs(), 10000));
            conn.setReadTimeout((int) Math.min(ctx.getTimeoutMs(), 15000));
            int code = conn.getResponseCode();
            BufferedReader br = new BufferedReader(new InputStreamReader(
                    (code >= 200 && code < 400) ? conn.getInputStream() : conn.getErrorStream(),
                    StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            String line;
            long max = policy.getMaxResponseBytes();
            long size = 0;
            while ((line = br.readLine()) != null) {
                size += line.getBytes(StandardCharsets.UTF_8).length;
                if (size > max) {
                    break;
                }
                sb.append(line).append('\n');
            }
            br.close();
            return ToolResult.ok(Map.of("status", code, "body", sb.toString()), System.currentTimeMillis() - start);
        } catch (Exception e) {
            return ToolResult.error("HTTP_ERROR", e.getMessage());
        }
    }
}
