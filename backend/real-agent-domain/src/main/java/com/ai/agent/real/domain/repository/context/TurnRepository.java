package com.ai.agent.real.domain.repository.context;

import com.ai.agent.real.domain.entity.context.Turn;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

import java.util.UUID;

/**
 * @author: han
 * @time: 2025/11/21 22:12
 */
@Repository
public interface TurnRepository extends R2dbcRepository<Turn, UUID> {

}
