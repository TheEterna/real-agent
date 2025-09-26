package com.ai.agent.real.web.service;

import com.ai.agent.real.web.persistence.entity.AppMessage;
import com.ai.agent.real.web.persistence.entity.AppSession;
import com.ai.agent.real.web.persistence.entity.VoiceSession;
import com.ai.agent.real.web.persistence.repository.AppMessageRepository;
import com.ai.agent.real.web.persistence.repository.AppSessionRepository;
import com.ai.agent.real.web.persistence.repository.VoiceSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class VoicePersistenceService {

    private final AppSessionRepository appSessionRepository;
    private final VoiceSessionRepository voiceSessionRepository;
    private final AppMessageRepository appMessageRepository;

    public Mono<Void> ensureAppSession(String appSessionId, String roleId) {
        if (appSessionId == null || appSessionId.isBlank()) return Mono.empty();
        return appSessionRepository.existsById(appSessionId)
                .flatMap(exists -> exists ? Mono.empty() :
                        appSessionRepository.save(AppSession.builder()
                                        .id(appSessionId)
                                        .roleId(roleId)
                                        .createdAt(LocalDateTime.now())
                                        .build())
                                .then())
                .doOnError(e -> log.warn("[persistence] ensureAppSession failed id={}, err={}", appSessionId, e.toString()))
                .onErrorResume(e -> Mono.empty());
    }

    public Mono<Void> ensureVoiceSession(String appSessionId, String serverSessionId, String voiceSessionId) {
        if (voiceSessionId == null || voiceSessionId.isBlank()) return Mono.empty();
        return voiceSessionRepository.existsById(voiceSessionId)
                .flatMap(exists -> exists ? Mono.empty() :
                        voiceSessionRepository.save(VoiceSession.builder()
                                        .id(voiceSessionId)
                                        .appSessionId(appSessionId)
                                        .serverSessionId(serverSessionId)
                                        .createdAt(LocalDateTime.now())
                                        .build())
                                .then())
                .doOnError(e -> log.warn("[persistence] ensureVoiceSession failed id={}, err={}", voiceSessionId, e.toString()))
                .onErrorResume(e -> Mono.empty());
    }

    /**
     * Store user text into app_message under the original appSession.
     */
    public Mono<Void> saveUserText(String appSessionId, String content) {
        if (appSessionId == null || appSessionId.isBlank() || content == null || content.isBlank()) return Mono.empty();
        return appMessageRepository.save(AppMessage.builder()
                        .appSessionId(appSessionId)
                        .direction("USER")
                        .type("TEXT")
                        .content(content)
                        .createdAt(LocalDateTime.now())
                        .build())
                .then()
                .doOnError(e -> log.warn("[persistence] saveUserText failed appSid={}, err={}", appSessionId, e.toString()))
                .onErrorResume(e -> Mono.empty());
    }

    /**
     * Store AI text into app_message under the original appSession.
     */
    public Mono<Void> saveAiText(String appSessionId, String content) {
        if (appSessionId == null || appSessionId.isBlank() || content == null || content.isBlank()) return Mono.empty();
        return appMessageRepository.save(AppMessage.builder()
                        .appSessionId(appSessionId)
                        .direction("AI")
                        .type("TEXT")
                        .content(content)
                        .createdAt(LocalDateTime.now())
                        .build())
                .then()
                .doOnError(e -> log.warn("[persistence] saveAiText failed appSid={}, err={}", appSessionId, e.toString()))
                .onErrorResume(e -> Mono.empty());
    }
}
