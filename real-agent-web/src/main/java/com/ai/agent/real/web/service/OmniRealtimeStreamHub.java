package com.ai.agent.real.web.service;

import com.alibaba.dashscope.audio.omni.OmniRealtimeCallback;
import com.alibaba.dashscope.audio.omni.OmniRealtimeConfig;
import com.alibaba.dashscope.audio.omni.OmniRealtimeConversation;
import com.alibaba.dashscope.audio.omni.OmniRealtimeModality;
import com.alibaba.dashscope.audio.omni.OmniRealtimeParam;
import com.google.gson.JsonObject;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.codec.*;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;
import reactor.core.publisher.Mono;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Realtime streaming session hub for Route B:
 * - Manage Omni realtime conversations per sessionId
 * - Receive PCM frames (16k, mono, 16-bit LE) via WebSocket and forward to SDK
 * - Rely on enableTurnDetection(true) and input audio transcription
 * - Broadcast events to SSE subscribers with OmniSseEvent + SsePayloads
 * @author han
 * @time 2025/9/26 1:38
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OmniRealtimeStreamHub {

    private final OmniProperties properties;
    // 精简版本：去掉持久化服务

    private final Map<String, SessionState> sessions = new ConcurrentHashMap<>();

    public Flux<ServerSentEvent<String>> subscribe(String sessionId, String roleId) {
        SessionState st = ensureSession(sessionId, roleId);
        return st.out.asFlux()
                .doOnCancel(() -> log.debug("[hub] subscriber cancel sessionId={}", sessionId))
                .doOnTerminate(() -> log.debug("[hub] subscriber terminated sessionId={}", sessionId))
                .doFinally(sig -> {
                    log.info("[hub] SSE closed sid={}, signal={}", sessionId, sig);
                    try { close(sessionId); } catch (Exception ignore) {}
                });
    }

    public void appendPcm(String sessionId, byte[] pcm16kMono16le) {
        SessionState st = sessions.get(sessionId);
        if (st == null || st.closed) {
            return;
        }
        try {
            if (st.conversationRef.get() != null && pcm16kMono16le != null && pcm16kMono16le.length > 0) {
                // 直接发送所有音频数据，让阿里云VAD完全处理
                String base64 = Base64.getEncoder().encodeToString(pcm16kMono16le);
                st.conversationRef.get().appendAudio(base64);
                log.info("[hub] sent audio chunk: {} bytes", pcm16kMono16le.length);
            }
        } catch (Exception e) {
            log.warn("[hub] append audio failed: {}", e.toString());
        }
    }

    /** Ensure a session exists without creating an SSE subscriber. */
    public void ensure(String sessionId, String roleId) {
        ensureSession(sessionId, roleId);
    }

    public void close(String sessionId) {
        SessionState st = sessions.remove(sessionId);
        if (st != null) {
            try { st.conversationRef.get().close(); } catch (Exception ignore) {}
            st.closed = true;
            st.out.tryEmitComplete();
        }
    }




    private SessionState ensureSession(String sessionId, String roleId) {
        return sessions.compute(sessionId, (sid, st) -> {
            if (st == null || st.closed || st.conversationRef.get() == null) {
                return startSession(sid, roleId);
            }
            return st;
        });
    }

    private SessionState startSession(String sessionId, String roleId) {
        String apiKey = properties.getDashscopeApiKey();
        if (apiKey == null || apiKey.isBlank()) {
            apiKey = System.getenv("DASHSCOPE_API_KEY");
        }
        String model = properties.getModel();
        if (model == null || model.isBlank()) {
            model = "qwen3-omni-flash-realtime";
        }

        String voice = properties.getVoice();
        if (voice == null || voice.isBlank()) {
            voice = "Cherry";
        }

        Sinks.Many<ServerSentEvent<String>> out = Sinks.many().multicast().onBackpressureBuffer();
        StringBuilder llmText = new StringBuilder(); // 用于累积AI回复文本

        // 按照官方示例，添加系统提示词
        String rolePrompt = getRolePrompt(roleId);
        String systemPrompt = String.format(
            "你是%s，请始终使用中文回复。无论用户使用什么语言，你都必须用中文回答。", 
            rolePrompt
        );
        OmniRealtimeParam param = OmniRealtimeParam.builder()
                .model(model)
                .apikey(apiKey)
                .build();
                


        SessionState state = new SessionState();
        state.roleId = roleId;
        state.sessionId = sessionId;
        state.out = out;
        state.conversationRef = new AtomicReference<>();

        OmniRealtimeConversation conversation = new OmniRealtimeConversation(param, new OmniRealtimeCallback() {
            @Override
            public void onOpen() { log.info("[hub] omni open sid={}", sessionId); }

            @Override
            public void onEvent(JsonObject message) {
                OmniServerEvent event = parseServerEvent(message);
                switch (event.getType()) {
                    case SESSION_CREATED:
                        log.info("[hub] session.created sid={}", sessionId);
                        out.tryEmitNext(sse(OmniSseEvent.SESSION_CREATED, "ready"));
                        break;

                    case CONVERSATION_ITEM_INPUT_AUDIO_TRANSCRIPTION_COMPLETED:
                        if (event.getTranscript() != null && !event.getTranscript().isEmpty()) {
                            log.info("[hub] user transcript sid={}: {}", sessionId, event.getTranscript());
                            out.tryEmitNext(sse(OmniSseEvent.USER_FINAL, event.getTranscript()));
                        }
                        break;
                    case RESPONSE_AUDIO_TRANSCRIPT_DELTA:
                    case RESPONSE_TEXT_DELTA:
                        if (event.getDelta() != null) {
                            // accumulate AI transcript as textual content
                            llmText.append(event.getDelta());
                            out.tryEmitNext(sse(OmniSseEvent.LLM_DELTA, event.getDelta()));
                        }
                        break;
                    case INPUT_AUDIO_BUFFER_SPEECH_STARTED:
                        // VAD检测到用户开始说话，发送打断信号给前端
                        log.info("[hub] VAD speech started - user interruption detected sid={}", sessionId);
                        out.tryEmitNext(sse(OmniSseEvent.USER_INTERRUPT, "speech_started"));
                        break;
                    case INPUT_AUDIO_BUFFER_SPEECH_STOPPED:
                        log.info("[hub] VAD speech stopped - user interruption detected sid={}", sessionId);
                        break;
                    case INPUT_AUDIO_BUFFER_COMMITTED:
                        // Buffer committed by server - VAD will handle response automatically
                        log.info("[hub] audio buffer committed sid={} - waiting for VAD to trigger response", sessionId);
                        break;
                    case RESPONSE_AUDIO_DELTA:
                        String base64 = event.getDelta();
                        if (base64 == null && event.getAudio() != null) {
                            base64 = event.getAudio().getDelta();
                        }
                        if (base64 != null && !base64.isEmpty()) {
                            out.tryEmitNext(sse(OmniSseEvent.AUDIO_DELTA, base64));
                        }
                        break;
                        case RESPONSE_DONE:
                            log.info("[hub] response done sid={}", sessionId);
                            llmText.setLength(0); // 清空等待下次响应
                            out.tryEmitNext(sse(OmniSseEvent.DONE, "done"));
                            break;
                    default:
                        // ignore others
                        log.error("whats up");
                }

            }

            @Override
            public void onClose(int code, String reason) {
                try { out.tryEmitComplete(); } catch (Exception ignore) {}
                state.closed = true;
                sessions.remove(sessionId);
            }
        });

        state.conversationRef.set(conversation);


        // assign conversation into state before any network activity so callbacks can reference it safely

        try {
            conversation.connect();

            // 完全按照官方示例配置
            OmniRealtimeConfig cfg = OmniRealtimeConfig.builder()
                    .modalities(Arrays.asList(OmniRealtimeModality.AUDIO, OmniRealtimeModality.TEXT))
                    .voice(voice)
                    .enableTurnDetection(true)
                    .enableInputAudioTranscription(true)
                    .InputAudioTranscription("gummy-realtime-v1")
                    // 降低敏感度，减少噪音干扰
                    .turnDetectionSilenceDurationMs(1000)  // 增加静音时间到1秒
                    .turnDetectionThreshold(0.3f)          // 降低阈值，更容易检测到语音
                    .build();

            conversation.updateSession(cfg);




        } catch (Exception e) {
            log.error("[hub] open omni session failed", e);
            out.tryEmitNext(sse(OmniSseEvent.ERROR, e.getMessage() == null ? "unknown" : e.getMessage()));
            // Mark state unusable and remove so next subscribe recreates cleanly
            state.closed = true;
            sessions.remove(sessionId);
        }

        // Note: do NOT put into sessions here; insertion is handled by computeIfAbsent in ensureSession()

        // 移除idle cleanup，交给WebSocket和阿里云处理连接生命周期

        return state;
    }



    private String getRolePrompt(String roleId) {
        if (roleId == null) {
            return "智能助手";
        }
        switch (roleId.toLowerCase()) {
            case "conan":
                return "名侦探柯南，擅长推理和解谜，说话简洁有逻辑";
            case "detective":
                return "福尔摩斯，观察入微的大侦探，善于分析和推理";
            default:
                return "智能助手";
        }
    }

    private ServerSentEvent<String> sse(String event, String data) {
        return ServerSentEvent.builder(data).event(event).build();
    }

    private OmniServerEvent parseServerEvent(JsonObject message) {
        String typeStr = message != null && message.has("type") ? message.get("type").getAsString() : null;
        OmniServerEvent evt = new OmniServerEvent();
        evt.setType(OmniServerEventType.fromType(typeStr));
        if (message != null) {
            if (message.has("delta") && message.get("delta").isJsonPrimitive()) {
                evt.setDelta(message.get("delta").getAsString());
            }
            if (message.has("audio") && message.get("audio").isJsonObject()) {
                var audioObj = message.getAsJsonObject("audio");
                OmniServerEvent.AudioPayload ap = new OmniServerEvent.AudioPayload();
                if (audioObj.has("delta") && audioObj.get("delta").isJsonPrimitive()) {
                    ap.setDelta(audioObj.get("delta").getAsString());
                }
                evt.setAudio(ap);
            }
            // transcript for input audio transcription completed
            if (message.has("transcript") && message.get("transcript").isJsonPrimitive()) {
                evt.setTranscript(message.get("transcript").getAsString());
            }
            // server session id from session.created -> session.id
            if (evt.getType() == OmniServerEventType.SESSION_CREATED
                    && message.has("session") && message.get("session").isJsonObject()) {
                var sessionObj = message.getAsJsonObject("session");
                if (sessionObj.has("id") && sessionObj.get("id").isJsonPrimitive()) {
                    evt.setSessionId(sessionObj.get("id").getAsString());
                }
            }
        }
        return evt;
    }

    @Data
    private static class SessionState {
        String sessionId;
        String roleId;
        AtomicReference<OmniRealtimeConversation> conversationRef;
        Sinks.Many<ServerSentEvent<String>> out;
        boolean closed;
        // 只保留核心必要字段，去掉voice、pcm、serverSessionId、voiceSessionId、persistenceReady
    }
}
