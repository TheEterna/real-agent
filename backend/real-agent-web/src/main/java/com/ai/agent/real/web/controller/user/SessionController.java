package com.ai.agent.real.web.controller.user;

import com.ai.agent.real.contract.model.auth.UserContextHolder;
import com.ai.agent.real.contract.model.protocol.ResponseResult;
import com.ai.agent.real.contract.user.ISessionService;
import com.ai.agent.real.contract.user.SessionDTO;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;

/**
 * 会话控制器
 */
@RestController
@RequestMapping("/api/sessions")
@RequiredArgsConstructor
public class SessionController {

	private final ISessionService sessionService;

	/**
	 * 获取当前用户的所有会话
	 */
	@GetMapping
	public Mono<ResponseResult<List<SessionDTO>>> getCurrentUserSessions() {
		return UserContextHolder.getUserId()
			.flatMap(userId -> sessionService.getSessionsByUserId(userId).collectList())
			.map(sessions -> ResponseResult.success(sessions))
			.switchIfEmpty(Mono.just(ResponseResult.unauthorized()));
	}

	/**
	 * 删除会话
	 */
	@DeleteMapping("/{id}")
	public Mono<ResponseResult<Object>> deleteSession(@PathVariable UUID id) {
		return UserContextHolder.getUserId()
			.flatMap(userId -> sessionService.isSessionBelongsToUser(id, userId))
			.flatMap(exists -> {
				if (exists) {
					return sessionService.deleteSession(id).then(Mono.just(ResponseResult.success("删除成功", null)));
				}
				else {
					return Mono.just(ResponseResult.notFound());
				}
			})
			.switchIfEmpty(Mono.just(ResponseResult.unauthorized()));
	}

	/**
	 * 创建会话请求对象
	 */
	@Data
	public static class CreateSessionRequest {

		private String title;

		private String type;

	}

}