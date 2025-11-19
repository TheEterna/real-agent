package com.ai.agent.real.web.ws;

import com.ai.agent.real.web.service.OmniRealtimeStreamHub;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.*;

import java.net.URI;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class VoiceStreamWebSocketHandler implements WebSocketHandler {

	private final OmniRealtimeStreamHub hub;

	@Override
	public Mono<Void> handle(WebSocketSession session) {
		URI uri = session.getHandshakeInfo().getUri();
		Map<String, List<String>> params = org.springframework.web.util.UriComponentsBuilder.fromUri(uri)
			.build()
			.getQueryParams();
		String sessionId = first(params, "sessionId");
		String roleId = first(params, "roleId");
		if (sessionId == null || sessionId.isBlank()) {
			log.warn("[ws] missing sessionId");
			return session.close();
		}

		log.info("[ws] open sid={}, roleId={}", sessionId, roleId);

		// Outbound: convert Hub SSE-style events to JSON text messages over WS
		Flux<WebSocketMessage> outbound = hub.subscribe(sessionId, roleId).map(sse -> {
			String event = sse.event();
			String data = sse.data();
			String json = '{' + "\"event\":\"" + escapeJson(event) + "\",\"data\":\"" + escapeJson(data) + "\"}";
			return session.textMessage(json);
		}).doOnError(err -> log.warn("[ws] error sid={}, err={}", sessionId, err.toString()));

		// Inbound: receive PCM and control texts
		Mono<Void> inbound = session.receive()
			.doOnSubscribe(s -> hub.ensure(sessionId, roleId))
			.flatMap(msg -> handleMessage(sessionId, msg))
			.doOnError(err -> log.warn("[ws] error sid={}, err={}", sessionId, err.toString()))
			.then();

		return Mono.when(session.send(outbound), inbound).doFinally(sig -> {
			log.info("[ws] close sid={}, signal={}", sessionId, sig);
			hub.close(sessionId);
		});
	}

	private Mono<Void> handleMessage(String sessionId, WebSocketMessage msg) {
		if (msg.getType() == WebSocketMessage.Type.BINARY) {
			byte[] bytes = new byte[msg.getPayload().readableByteCount()];
			msg.getPayload().read(bytes);
			log.debug("[ws] received audio data: {} bytes for session {}", bytes.length, sessionId);
			hub.appendPcm(sessionId, bytes);
		}
		else if (msg.getType() == WebSocketMessage.Type.TEXT) {
			String text = msg.getPayloadAsText();
			if ("ping".equalsIgnoreCase(text)) {
				// ignore
			}
			else if ("close".equalsIgnoreCase(text)) {
				hub.close(sessionId);
			}

		}
		return Mono.empty();
	}

	private String first(Map<String, List<String>> q, String key) {
		List<String> vs = q.get(key);
		return (vs == null || vs.isEmpty()) ? null : vs.get(0);
	}

	private String escapeJson(String s) {
		if (s == null)
			return "";
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < s.length(); i++) {
			char c = s.charAt(i);
			switch (c) {
				case '"':
					sb.append("\\\"");
					break;
				case '\\':
					sb.append("\\\\");
					break;
				case '\n':
					sb.append("\\n");
					break;
				case '\r':
					sb.append("\\r");
					break;
				case '\t':
					sb.append("\\t");
					break;
				default:
					if (c < 0x20) {
						sb.append(String.format("\\u%04x", (int) c));
					}
					else {
						sb.append(c);
					}
			}
		}
		return sb.toString();
	}

}
