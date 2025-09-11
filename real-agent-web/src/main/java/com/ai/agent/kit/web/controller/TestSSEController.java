package com.ai.agent.kit.web.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * 测试 SSE 连接的简单控制器
 */
@Slf4j
@RestController
@RequestMapping("/test")
@CrossOrigin(origins = "*")
public class TestSSEController {

    /**
     * 简单的 SSE 测试端点
     */
    @GetMapping(value = "/sse", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> testSSE() {
        log.info("收到 SSE 测试请求");
        
        return Flux.interval(Duration.ofSeconds(1))
                .take(5)
                .map(i -> "data: 测试消息 " + i + " - " + LocalDateTime.now() + "\n\n")
                .doOnNext(msg -> log.debug("发送 SSE 消息: {}", msg.trim()))
                .doOnComplete(() -> log.info("SSE 测试完成"));
    }

    /**
     * 简单的 JSON 测试端点
     */
    @GetMapping("/json")
    public String testJson() {
        log.info("收到 JSON 测试请求");
        return "{\"status\":\"ok\",\"message\":\"测试成功\",\"timestamp\":\"" + LocalDateTime.now() + "\"}";
    }
}
