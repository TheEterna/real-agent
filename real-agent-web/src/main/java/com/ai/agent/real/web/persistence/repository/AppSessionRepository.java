package com.ai.agent.real.web.persistence.repository;

import com.ai.agent.real.web.persistence.entity.AppSession;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;

public interface AppSessionRepository extends ReactiveCrudRepository<AppSession, String> {
}
