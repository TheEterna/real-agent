package com.ai.agent.real.contract.model.logging;

import java.time.*;
import java.util.UUID;

public interface Traceable {

	UUID getSessionId();

	Traceable setSessionId(UUID sessionId);

	UUID getTurnId();

	Traceable setTurnId(UUID turnId);

	OffsetDateTime getStartTime();

	Traceable setStartTime(OffsetDateTime startTime);

	OffsetDateTime getEndTime();

	Traceable setEndTime(OffsetDateTime endTime);

	String getMessageId();

	Traceable setMessageId(String messageId);

	String getAgentId();

	Traceable setAgentId(String agentId);

}
