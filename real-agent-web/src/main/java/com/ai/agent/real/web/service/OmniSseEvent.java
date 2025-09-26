package com.ai.agent.real.web.service;

/**
 * Constants for SSE event names between backend and frontend.
 * @author han
 * @time 2025/9/25 0:01
 */
public final class OmniSseEvent {
    private OmniSseEvent() {}
    public static final String ASR_START = "asr_start";
//    public static final String ASR_PARTIAL = "asr_partial";
    public static final String ASR_FINAL = "asr_final";
    public static final String LLM_START = "llm_start";
    public static final String LLM_DELTA = "llm_delta";
    public static final String LLM_DONE = "llm_done";
    public static final String TTS_START = "tts_start";
    public static final String TTS_READY = "tts_ready";
    public static final String AUDIO_DELTA = "audio_delta";
    public static final String USER_PARTIAL = "user_partial";
    public static final String USER_FINAL = "user_final";
    public static final String SESSION_CREATED = "session_created";
    public static final String APP_SESSION_CREATED = "app_session_created";
    public static final String VOICE_SESSION_CREATED = "voice_session_created";
    public static final String DONE = "done";
    public static final String ERROR = "error";
    // VAD相关事件
    public static final String USER_INTERRUPT = "user_interrupt";
    public static final String USER_SPEECH_STOPPED = "user_speech_stopped";
}
