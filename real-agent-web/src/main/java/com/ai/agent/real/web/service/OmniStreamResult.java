package com.ai.agent.real.web.service;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * A simple holder for Omni realtime streaming outputs.
 * - textDeltas: incremental text tokens from Omni
 * - audioWav: the final aggregated WAV bytes built from PCM deltas

 * @author han
 * @time 2025/9/25 17:36
 */
public class OmniStreamResult {
    private final Flux<String> textDeltas;
    private final Mono<byte[]> audioWav;

    public OmniStreamResult(Flux<String> textDeltas, Mono<byte[]> audioWav) {
        this.textDeltas = textDeltas;
        this.audioWav = audioWav;
    }

    public Flux<String> getTextDeltas() {
        return textDeltas;
    }

    public Mono<byte[]> getAudioWav() {
        return audioWav;
    }
}
