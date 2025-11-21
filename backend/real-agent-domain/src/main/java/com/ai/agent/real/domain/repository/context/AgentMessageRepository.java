package com.ai.agent.real.domain.repository.context;

import com.ai.agent.real.domain.entity.context.AgentMessage;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

import java.util.UUID;

/**
 * 
 * @author: han
 * @time: 2025/11/21 22:12
 */
@Repository
public interface AgentMessageRepository extends R2dbcRepository<AgentMessage, UUID> {
    
    Flux<AgentMessage> findBySessionIdOrderByStartTimeAsc(UUID sessionId);
    
    Flux<AgentMessage> findByTurnIdOrderByStartTimeAsc(UUID turnId);
}
