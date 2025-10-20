package com.ai.agent.real.interaction.elicitation;

import io.modelcontextprotocol.spec.McpSchema.*;
import org.springaicommunity.mcp.annotation.*;

import java.util.*;

/**
 * @author han
 * @time 2025/10/17 13:13
 */

public class ElicitationObject {

	@McpElicitation(clients = "*")
	public ElicitResult elicit(ElicitRequest request) {
		// 模拟处理elicitation请求
		return ElicitResult.builder().content(Map.of("result", "模拟处理elicitation请求")).build();
	}

}
