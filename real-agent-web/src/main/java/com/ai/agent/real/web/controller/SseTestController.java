package com.ai.agent.real.web.controller;

import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * SSE连接测试控制器
 * 用于验证SSE连接是否正常工作
 * 
 * @author han
 * @time 2025/9/11 01:20
 */
@Slf4j
@RestController
@RequestMapping("/api/test")
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class SseTestController {

    /**
     * 简单的SSE测试接口
     */
    @GetMapping(value = "/sse", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> testSse() {
        log.info("开始SSE测试连接");
        
        return Flux.interval(Duration.ofSeconds(1))
                .take(10)
                .map(sequence -> {
                    String data = String.format("{\"sequence\":%d,\"message\":\"测试消息 %d\",\"timestamp\":\"%s\"}", 
                            sequence, sequence, LocalDateTime.now());
                    log.debug("发送SSE测试数据: {}", data);
                    return ServerSentEvent.<String>builder()
                            .id(String.valueOf(sequence))
                            .event("test-message")
                            .data(data)
                            .build();
                })
                .doOnComplete(() -> log.info("SSE测试连接完成"))
                .doOnError(error -> log.error("SSE测试连接异常", error));
    }

    /**
     * POST方式的SSE测试接口
     */
    @PostMapping(value = "/sse", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> testSsePost(@RequestBody(required = false) String message) {
        log.info("开始POST SSE测试连接，消息: {}", message);
        
        String testMessage = message != null ? message : "默认测试消息";
        
        return Flux.interval(Duration.ofSeconds(1))
                .take(5)
                .map(sequence -> {
                    String data = String.format("{\"sequence\":%d,\"message\":\"%s - %d\",\"timestamp\":\"%s\"}", 
                            sequence, testMessage, sequence, LocalDateTime.now());
                    log.debug("发送POST SSE测试数据: {}", data);
                    return ServerSentEvent.<String>builder()
                            .id(String.valueOf(sequence))
                            .event("test-post-message")
                            .data(data)
                            .build();
                })
                .doOnComplete(() -> log.info("POST SSE测试连接完成"))
                .doOnError(error -> log.error("POST SSE测试连接异常", error));
    }
}
