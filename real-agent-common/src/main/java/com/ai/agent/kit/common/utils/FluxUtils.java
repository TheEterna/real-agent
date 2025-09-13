package com.ai.agent.kit.common.utils;

import com.ai.agent.contract.spec.*;
import com.ai.agent.contract.spec.message.*;
import reactor.core.publisher.*;

import java.util.function.*;

/**
 * @author han
 * @time 2025/9/13 21:50
 */

public class FluxUtils {
    public static Function<Flux<AgentExecutionEvent>, Flux<AgentExecutionEvent>> handleContext(AgentContext context, String agentId) {
        return stageFlux -> {
            StringBuilder buf = new StringBuilder();
            return stageFlux
                    .doOnNext(event -> {
                        String msg = event.getMessage();
                        if (msg != null && !msg.trim().isEmpty()) {
                            buf.append(msg);
                        }
                    })
                    .doOnComplete(() -> {
                        if (buf.length() > 0) {
                            context.addMessage(AgentMessage.thinking(buf.toString(), agentId));
                        }
//                        log.info("思考阶段结束: {}", context.getConversationHistory());
                    });
        };
    }

}
