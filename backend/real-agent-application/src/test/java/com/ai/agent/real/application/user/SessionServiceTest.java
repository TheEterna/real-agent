// package com.ai.agent.real.application.user;
//
// import com.ai.agent.real.contract.user.SessionDTO;
// import com.ai.agent.real.domain.entity.user.Session;
// import com.ai.agent.real.domain.repository.user.SessionRepository;
// import org.junit.jupiter.api.Test;
// import org.junit.jupiter.api.extension.ExtendWith;
// import org.mockito.InjectMocks;
// import org.mockito.Mock;
// import org.mockito.junit.jupiter.MockitoExtension;
// import reactor.core.publisher.Flux;
// import reactor.core.publisher.Mono;
// import reactor.test.StepVerifier;
//
// import java.time.LocalDateTime;
// import java.util.UUID;
//
// import static org.mockito.ArgumentMatchers.any;
// import static org.mockito.Mockito.*;
//
// @ExtendWith(MockitoExtension.class)
// class SessionServiceTest {
//
// @Mock
// private SessionRepository sessionRepository;
//
// @InjectMocks
// private SessionService sessionService;
//
// @Test
// void createSession_shouldCreateAndReturnSession() {
// // Given
// String title = "Test Session";
// String type = "chat";
// UUID userId = UUID.randomUUID();
// Session session = Session.builder()
// .id(UUID.randomUUID())
// .title(title)
// .type(type)
// .userId(userId)
// .startTime(LocalDateTime.now())
// .build();
//
// when(sessionRepository.save(any(Session.class))).thenReturn(Mono.just(session));
//
// // When
// Mono<SessionDTO> result = sessionService.createSession(title, type, userId);
//
// // Then
// StepVerifier.create(result)
// .expectNextMatches(dto ->
// dto.getTitle().equals(title) &&
// dto.getType().equals(type) &&
// dto.getUserId().equals(userId))
// .verifyComplete();
//
// verify(sessionRepository).save(any(Session.class));
// }
//
// @Test
// void getSessionsByUserId_shouldReturnUserSessions() {
// // Given
// UUID userId = UUID.randomUUID();
// Session session1 = Session.builder()
// .id(UUID.randomUUID())
// .title("Session 1")
// .type("chat")
// .userId(userId)
// .startTime(LocalDateTime.now())
// .build();
// Session session2 = Session.builder()
// .id(UUID.randomUUID())
// .title("Session 2")
// .type("task")
// .userId(userId)
// .startTime(LocalDateTime.now())
// .build();
//
// when(sessionRepository.findByUserIdOrderByStartTimeDesc(userId)).thenReturn(Flux.just(session1,
// session2));
//
// // When
// Flux<SessionDTO> result = sessionService.getSessionsByUserId(userId);
//
// // Then
// StepVerifier.create(result)
// .expectNextCount(2)
// .verifyComplete();
//
// verify(sessionRepository).findByUserId(userId);
// }
//
// @Test
// void deleteSession_shouldCallRepository() {
// // Given
// UUID sessionId = UUID.randomUUID();
// when(sessionRepository.deleteById(sessionId)).thenReturn(Mono.empty());
//
// // When
// Mono<Void> result = sessionService.deleteSession(sessionId);
//
// // Then
// StepVerifier.create(result)
// .verifyComplete();
//
// verify(sessionRepository).deleteById(sessionId);
// }
// }