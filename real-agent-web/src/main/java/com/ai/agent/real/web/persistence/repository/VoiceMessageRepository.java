package com.ai.agent.real.web.persistence.repository;

import com.ai.agent.real.web.persistence.entity.VoiceMessage;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;

public interface VoiceMessageRepository extends ReactiveCrudRepository<VoiceMessage, Long> {
}
