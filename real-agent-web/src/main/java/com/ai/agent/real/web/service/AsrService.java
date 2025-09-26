package com.ai.agent.real.web.service;

import com.ai.agent.real.web.config.VoiceProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * ASR via DashScope paraformer-v2 (short audio REST).
 * If no API key configured, return a placeholder text so the pipeline works.
 *
 * @author han
 * @time 2025/9/10 10:45
 */

@Slf4j
@Service
@RequiredArgsConstructor
public class AsrService {

    private final VoiceProperties voiceProperties;

    @Value("${spring.ai.dashscope.api-key}")
    private String dashscopeApiKey;

    private final WebClient webClient = WebClient.builder()
            .baseUrl("https://dashscope.aliyuncs.com")
            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .build();

    private final ObjectMapper objectMapper = new ObjectMapper();

    public Mono<String> transcribe(byte[] audioBytes, String mimeType) {
        if (dashscopeApiKey == null || dashscopeApiKey.isBlank()) {
            log.warn("[asr] No DashScope API key. Returning placeholder.");
            return Mono.just("你好，我是你的语音助手");
        }

        // Prefer to pass base64 data; if mimeType is webm/opus and not supported, we will later add a transcode step.
        String base64 = Base64.getEncoder().encodeToString(audioBytes);

        // NOTE: Endpoint path may vary across DashScope versions. We'll use a commonly documented path and leave TODO to adjust.
        String url = "/api/v1/services/audio/asr/transcription";
        String model = Optional.ofNullable(voiceProperties.getAsrModel()).orElse("paraformer-v2");

        Map<String, Object> audio = new HashMap<>();
        audio.put("format", "auto");
        audio.put("sample_rate", 16000);
        audio.put("content", base64);

        Map<String, Object> input = new HashMap<>();
        input.put("audio", audio);

        Map<String, Object> params = new HashMap<>();
        params.put("language", "zh");
        params.put("enable_itn", true);
        params.put("enable_punctuation", true);

        Map<String, Object> payload = new HashMap<>();
        payload.put("model", model);
        payload.put("input", input);
        payload.put("parameters", params);

        return webClient.post()
                .uri(url)
                .header("Authorization", "Bearer " + dashscopeApiKey)
                .body(BodyInserters.fromValue(payload))
                .retrieve()
                .bodyToMono(String.class)
                .map(this::extractText)
                .doOnError(e -> log.error("[asr] error", e));
//                .onErrorReturn("你好，我是你的语音助手");
    }

    // Very lightweight extraction; TODO: replace with JSON parsing of DashScope response schema
    private String extractText(String json) {
        if (json == null || json.isBlank()) return "";
        try {
            JsonNode root = objectMapper.readTree(json);
            // Common patterns
            String t = textAt(root, "output", "text");
            if (!t.isEmpty()) {
                return t;
            }
            t = textAt(root, "result", "text");

            if (!t.isEmpty()) {
                return t;
            }
            // Fallback: search any field named 'text'
            String any = findFirstTextField(root, "text");
            return any == null ? "" : any;
        } catch (Exception e) {
            return "";
        }
    }

    private String textAt(JsonNode root, String a, String b) {
        JsonNode n = root.path(a).path(b);
        return n.isTextual() ? n.asText() : "";
    }

    private String findFirstTextField(JsonNode node, String fieldName) {
        if (node == null) return null;
        if (node.has(fieldName) && node.get(fieldName).isTextual()) {
            return node.get(fieldName).asText();
        }
        if (node.isObject()) {
            var it = node.fields();
            while (it.hasNext()) {
                var e = it.next();
                String v = findFirstTextField(e.getValue(), fieldName);
                if (v != null && !v.isEmpty()) return v;
            }
        } else if (node.isArray()) {
            for (JsonNode c : node) {
                String v = findFirstTextField(c, fieldName);
                if (v != null && !v.isEmpty()) return v;
            }
        }
        return null;
    }
}

