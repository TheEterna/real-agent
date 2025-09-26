package com.ai.agent.real.web.persistence.repository;

import com.ai.agent.real.web.persistence.entity.VoiceSession;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;

public interface VoiceSessionRepository extends ReactiveCrudRepository<VoiceSession, String> {
}
