package com.ai.agent.real.web.persistence.repository;

import com.ai.agent.real.web.persistence.entity.AppMessage;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;

public interface AppMessageRepository extends ReactiveCrudRepository<AppMessage, Long> {
}
