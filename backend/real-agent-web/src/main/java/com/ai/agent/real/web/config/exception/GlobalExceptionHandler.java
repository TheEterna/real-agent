package com.ai.agent.real.web.config.exception;

import com.ai.agent.real.contract.model.protocol.ResponseResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import reactor.core.publisher.Mono;

/**
 * @author han
 * @time 2025/10/27 20:57
 */

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(Exception.class)
	public Mono<ResponseResult<Object>> handleException(Exception e) {
		log.error(e.getMessage());
		return Mono.just(ResponseResult.error(e.getMessage()));
	}

}
