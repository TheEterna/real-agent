package com.ai.agent.real.web.controller.playground;

import com.ai.agent.real.application.service.playground.roleplay.PlaygroundRoleplaySessionService;
import com.ai.agent.real.contract.dto.SessionCreateRequestDto;
import com.ai.agent.real.contract.model.protocol.*;
import com.ai.agent.real.domain.entity.roleplay.PlaygroundRoleplaySession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.validation.Valid;
import java.util.Map;

/**
 * PlaygroundRoleplaySessionController
 *
 * @time 2025/9/28 05:37
 */
@Slf4j
@RestController
@RequestMapping("/api/sessions")
@RequiredArgsConstructor
public class PlaygroundRoleplaySessionController {

	private final PlaygroundRoleplaySessionService sessionService;

	/**
	 * 创建会话
	 */
	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public Mono<ResponseResult<PlaygroundRoleplaySession>> createSession(
			@Valid @RequestBody SessionCreateRequestDto request) {
		return sessionService.createSession(request).map(ResponseResult::success);
	}

	/**
	 * 根据会话编码查询会话详情
	 */
	@GetMapping("/{sessionCode}")
	public Mono<ResponseResult<PlaygroundRoleplaySession>> getSession(@PathVariable String sessionCode) {
		return sessionService.findBySessionCode(sessionCode)
			.map(ResponseResult::success)
			.switchIfEmpty(Mono.error(new RuntimeException("会话不存在: " + sessionCode)));
	}

	/**
	 * 查询用户的会话列表
	 */
	@GetMapping("/user/{userId}")
	public Flux<ResponseResult<PlaygroundRoleplaySession>> getUserSessions(@PathVariable Long userId,
			@RequestParam(defaultValue = "false") boolean activeOnly) {
		if (activeOnly) {
			return sessionService.findActiveUserSessions(userId).map(ResponseResult::success);
		}
		return sessionService.findUserSessions(userId).map(ResponseResult::success);
	}

	/**
	 * 结束会话
	 */
	@PutMapping("/{sessionCode}/end")
	public Mono<ResponseResult<PlaygroundRoleplaySession>> endSession(@PathVariable String sessionCode,
			@RequestBody(required = false) Map<String, String> requestBody) {
		String summary = requestBody != null ? requestBody.get("summary") : null;
		return sessionService.endSession(sessionCode, summary).map(ResponseResult::success);
	}

	/**
	 * 更新会话元数据
	 */
	@PutMapping("/{sessionCode}/metadata")
	public Mono<ResponseResult<PlaygroundRoleplaySession>> updateSessionMetadata(@PathVariable String sessionCode,
			@RequestBody Map<String, Object> metadata) {
		return sessionService.updateSessionMetadata(sessionCode, metadata).map(ResponseResult::success);
	}

}
