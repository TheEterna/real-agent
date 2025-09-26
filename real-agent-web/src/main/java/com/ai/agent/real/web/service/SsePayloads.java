package com.ai.agent.real.web.service;

import lombok.*;

/**
 * Simple POJOs for SSE payloads to avoid raw JSON string concatenation.
 * @author han
 * @time 2025/9/26 0:01
 */
public final class SsePayloads {
    private SsePayloads() {}

    @Data
    public static final class Message {
        private String message;
        public Message() {}
        public Message(String message) { this.message = message; }
    }

    @Data
    public static final class Text {
        private String text;
        public Text() {}
        public Text(String text) { this.text = text; }
    }

    @Data
    public static final class AudioUrl {
        private String audio_url;
        public AudioUrl() {}
        public AudioUrl(String audioUrl) { this.audio_url = audioUrl; }
    }

    public static final class Done {
        private boolean ok;
        public Done() {}
        public Done(boolean ok) { this.ok = ok; }
        public boolean isOk() { return ok; }
        public void setOk(boolean ok) { this.ok = ok; }
    }

    @Data
    public static final class Error {
        private String message;
        public Error() {}
        public Error(String message) { this.message = message; }
    }
}
