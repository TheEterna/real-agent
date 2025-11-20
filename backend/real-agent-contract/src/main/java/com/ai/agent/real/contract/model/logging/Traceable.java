package com.ai.agent.real.contract.model.logging;

import java.time.*;

public interface Traceable {

	String getSessionId();

	Traceable setSessionId(String sessionId);

	String getTurnId();

	Traceable setTurnId(String turnId);

	LocalDateTime getStartTime();

	Traceable setStartTime(LocalDateTime startTime);

	LocalDateTime getEndTime();

	Traceable setEndTime(LocalDateTime endTime);

	String getMessageId();

	Traceable setMessageId(String messageId);

	String getAgentId();

	Traceable setAgentId(String agentId);

}
