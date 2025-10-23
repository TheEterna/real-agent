package com.ai.agent.real.web.controller;

import com.ai.agent.real.agent.strategy.ReActAgentStrategy;
import com.ai.agent.real.contract.model.context.AgentContext;
import com.ai.agent.real.contract.model.interaction.InteractionResponse;
import com.ai.agent.real.contract.model.logging.TraceInfo;
import com.ai.agent.real.contract.model.message.AgentMessage;
import com.ai.agent.real.contract.model.protocol.AgentExecutionEvent;
import com.ai.agent.real.web.service.AgentSessionHub;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Agent对话控制器 提供ReAct框架的Web接口，支持通用交互机制
 *
 * @author han
 * @time 2025/10/22 17:30
 */
@Slf4j
@RestController
@RequestMapping("/api/agent/chat")
@CrossOrigin(origins = "*")
public class AgentChatController {

	private final ReActAgentStrategy reActAgentStrategy;

	private final AgentSessionHub agentSessionHub;

	public AgentChatController(ReActAgentStrategy reActAgentStrategy, AgentSessionHub agentSessionHub) {
		this.reActAgentStrategy = reActAgentStrategy;
		this.agentSessionHub = agentSessionHub;
	}

}
