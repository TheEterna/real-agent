package com.ai.agent.real.application.utils;

import com.ai.agent.real.contract.model.protocol.AgentExecutionEvent;
import org.slf4j.Logger;

import java.util.Optional;
import java.util.function.Consumer;

/**
 * @author han
 * @time 2025/11/5 15:19
 */
public class FunctionUtils {

	public static Consumer<AgentExecutionEvent> defaultOnNext(Logger logger) {
		return evt -> logger.debug("type={}, msg={}...",
				Optional.ofNullable(evt).map(AgentExecutionEvent::getType).orElse(null),
				Optional.ofNullable(evt)
					.map(AgentExecutionEvent::getMessage)
					.map(msg -> AgentUtils.safeHead(msg, 256))
					.orElse(null));
	}

}