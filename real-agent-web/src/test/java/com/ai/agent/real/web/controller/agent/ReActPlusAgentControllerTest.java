package com.ai.agent.real.web.controller.agent;

import com.ai.agent.real.contract.dto.ChatRequest;
import com.ai.agent.real.contract.model.interaction.InteractionResponse;
import com.ai.agent.real.contract.model.protocol.AgentExecutionEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.test.web.reactive.server.FluxExchangeResult;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.lang.reflect.Type;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ReActPlusAgentController 集成测试 测试工具审批暂停/恢复机制
 *
 * @author han
 * @time 2025/11/12 00:15
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient(timeout = "60000") // 60秒超时
@DisplayName("ReAct-Plus Agent 工具审批测试")
class ReActPlusAgentControllerTest {

	@Autowired
	private WebTestClient webTestClient;

	// 注入的依赖暂未使用，保留供后续扩展
	// @Autowired
	// private IAgentTurnManagerService agentSessionManagerService;
	//
	// @Autowired
	// private ObjectMapper objectMapper;

	private String testSessionId;

	@BeforeEach
	void setUp() {
		testSessionId = "test-session-" + System.currentTimeMillis();
	}

	@Test
	@DisplayName("1. 基础流程测试 - 启动对话并接收事件")
	void testBasicStreamFlow() {
		// 准备请求
		ChatRequest request = new ChatRequest();
		request.setSessionId(testSessionId);
		request.setMessage("你好，请介绍一下自己");
		request.setUserId("test-user");

		// 发起请求并验证响应
		@SuppressWarnings("rawtypes")
		FluxExchangeResult<ServerSentEvent<AgentExecutionEvent>> result = webTestClient.post()
			.uri("/api/agent/chat/react-plus/stream")
			.contentType(MediaType.APPLICATION_JSON)
			.bodyValue(request)
			.exchange()
			.expectStatus()
			.isOk()
			.expectHeader()
			.contentTypeCompatibleWith(MediaType.TEXT_EVENT_STREAM)
			.returnResult(new ParameterizedTypeReference<ServerSentEvent<AgentExecutionEvent>>() {
				@Override
				public Type getType() {
					return super.getType();
				}
			});

		// 验证事件流
		@SuppressWarnings("unchecked")
		Flux<ServerSentEvent<AgentExecutionEvent>> eventFlux = result.getResponseBody();

		StepVerifier.create(eventFlux.take(5)) // 只取前5个事件
			.expectNextMatches(event -> {
				System.out.println("收到事件: " + event.event());
				return event.data() != null;
			})
			.expectNextCount(4)
			.verifyComplete();
	}

	@Test
	@DisplayName("2. 工具审批流程测试 - 完整的暂停和恢复")
	void testToolApprovalFlow() throws Exception {
		// 准备请求 - 触发需要工具调用的任务
		ChatRequest request = new ChatRequest();
		request.setSessionId(testSessionId);
		request.setMessage("帮我查询杭州的天气"); // 这会触发工具调用
		request.setUserId("test-user");

		// 用于收集事件
		List<AgentExecutionEvent> events = new ArrayList<>();
		AtomicReference<String> toolCallRequestId = new AtomicReference<>();

		// 启动 SSE 流（异步执行）
		Thread sseThread = new Thread(() -> {
			webTestClient.post()
				.uri("/api/agent/chat/react-plus/stream")
				.contentType(MediaType.APPLICATION_JSON)
				.bodyValue(request)
				.exchange()
				.expectStatus()
				.isOk()
				.returnResult(ServerSentEvent.class)
				.getResponseBody()
				.doOnNext(event -> {
					AgentExecutionEvent data = (AgentExecutionEvent) event.data();
					events.add(data);
					System.out.println("收到事件: " + data.getType());

					// 捕获工具审批请求
					if (data.getType() == AgentExecutionEvent.EventType.TOOL_APPROVAL) {
						@SuppressWarnings("unchecked")
						java.util.Map<String, Object> dataMap = (java.util.Map<String, Object>) data.getData();
						String requestId = (String) dataMap.get("requestId");
						toolCallRequestId.set(requestId);
						System.out.println("捕获到工具审批请求: " + requestId);
					}
				})
				.blockLast(Duration.ofSeconds(30));
		});

		sseThread.start();

		// 等待工具审批请求出现
		int maxWait = 20; // 最多等待20秒
		int waited = 0;
		while (toolCallRequestId.get() == null && waited < maxWait) {
			Thread.sleep(1000);
			waited++;
		}

		assertThat(toolCallRequestId.get()).as("应该收到工具审批请求").isNotNull();

		System.out.println("准备提交审批决策...");

		// 提交审批决策 - 同意执行
		InteractionResponse response = new InteractionResponse();
		response.setSessionId(testSessionId);
		response.setRequestId(toolCallRequestId.get());
		response.setSelectedOptionId("approve");

		webTestClient.post()
			.uri("/api/agent/chat/react-plus/interaction_response")
			.contentType(MediaType.APPLICATION_JSON)
			.bodyValue(response)
			.exchange()
			.expectStatus()
			.isOk()
			.expectBody()
			.jsonPath("$.success")
			.isEqualTo(true)
			.jsonPath("$.message")
			.isEqualTo("用户响应已处理，正在恢复执行")
			.jsonPath("$.sessionId")
			.isEqualTo(testSessionId);

		System.out.println("审批决策已提交，等待流程完成...");

		// 等待 SSE 流完成
		sseThread.join(30000);

		// 验证事件序列
		assertThat(events).as("应该收到多个事件").hasSizeGreaterThan(3);

		// 验证包含工具审批事件
		boolean hasToolApproval = events.stream()
			.anyMatch(e -> e.getType() == AgentExecutionEvent.EventType.TOOL_APPROVAL);
		assertThat(hasToolApproval).as("应该包含工具审批事件").isTrue();

		// 验证包含工具执行结果事件
		boolean hasToolResult = events.stream().anyMatch(e -> e.getType() == AgentExecutionEvent.EventType.TOOL);
		assertThat(hasToolResult).as("应该包含工具执行结果事件").isTrue();
	}

	@Test
	@DisplayName("3. 拒绝工具执行测试")
	void testRejectToolExecution() throws Exception {
		// 准备请求
		ChatRequest request = new ChatRequest();
		request.setSessionId(testSessionId);
		request.setMessage("帮我删除所有文件"); // 危险操作
		request.setUserId("test-user");

		AtomicReference<String> toolCallRequestId = new AtomicReference<>();

		// 启动 SSE 流
		Thread sseThread = new Thread(() -> {
			webTestClient.post()
				.uri("/api/agent/chat/react-plus/stream")
				.contentType(MediaType.APPLICATION_JSON)
				.bodyValue(request)
				.exchange()
				.expectStatus()
				.isOk()
				.returnResult(ServerSentEvent.class)
				.getResponseBody()
				.doOnNext(event -> {
					AgentExecutionEvent data = (AgentExecutionEvent) event.data();
					if (data.getType() == AgentExecutionEvent.EventType.TOOL_APPROVAL) {
						@SuppressWarnings("unchecked")
						java.util.Map<String, Object> dataMap = (java.util.Map<String, Object>) data.getData();
						String requestId = (String) dataMap.get("requestId");
						toolCallRequestId.set(requestId);
					}
				})
				.blockLast(Duration.ofSeconds(30));
		});

		sseThread.start();

		// 等待工具审批请求
		Thread.sleep(5000);

		if (toolCallRequestId.get() != null) {
			// 提交拒绝决策
			InteractionResponse response = new InteractionResponse();
			response.setSessionId(testSessionId);
			response.setRequestId(toolCallRequestId.get());
			response.setSelectedOptionId("reject");
			response.setFeedback("这个操作太危险了，我不同意");

			webTestClient.post()
				.uri("/api/agent/chat/react-plus/interaction_response")
				.contentType(MediaType.APPLICATION_JSON)
				.bodyValue(response)
				.exchange()
				.expectStatus()
				.isOk()
				.expectBody()
				.jsonPath("$.success")
				.isEqualTo(true);
		}

		sseThread.join(30000);
	}

	@Test
	@DisplayName("4. 终止对话测试")
	void testTerminateConversation() throws Exception {
		// 准备请求
		ChatRequest request = new ChatRequest();
		request.setSessionId(testSessionId);
		request.setMessage("帮我执行一个复杂任务");
		request.setUserId("test-user");

		AtomicReference<String> toolCallRequestId = new AtomicReference<>();

		// 启动 SSE 流
		Thread sseThread = new Thread(() -> {
			webTestClient.post()
				.uri("/api/agent/chat/react-plus/stream")
				.contentType(MediaType.APPLICATION_JSON)
				.bodyValue(request)
				.exchange()
				.expectStatus()
				.isOk()
				.returnResult(ServerSentEvent.class)
				.getResponseBody()
				.doOnNext(event -> {
					AgentExecutionEvent data = (AgentExecutionEvent) event.data();
					if (data.getType() == AgentExecutionEvent.EventType.TOOL_APPROVAL) {
						@SuppressWarnings("unchecked")
						java.util.Map<String, Object> dataMap = (java.util.Map<String, Object>) data.getData();
						String requestId = (String) dataMap.get("requestId");
						toolCallRequestId.set(requestId);
					}
				})
				.blockLast(Duration.ofSeconds(30));
		});

		sseThread.start();

		// 等待工具审批请求
		Thread.sleep(5000);

		if (toolCallRequestId.get() != null) {
			// 提交终止决策
			InteractionResponse response = new InteractionResponse();
			response.setSessionId(testSessionId);
			response.setRequestId(toolCallRequestId.get());
			response.setSelectedOptionId("terminate");
			response.setFeedback("我改变主意了，不需要这个功能");

			webTestClient.post()
				.uri("/api/agent/chat/react-plus/interaction_response")
				.contentType(MediaType.APPLICATION_JSON)
				.bodyValue(response)
				.exchange()
				.expectStatus()
				.isOk()
				.expectBody()
				.jsonPath("$.success")
				.isEqualTo(true);
		}

		sseThread.join(30000);
	}

	@Test
	@DisplayName("5. 参数验证测试 - 缺少 sessionId")
	void testMissingSessionId() {
		InteractionResponse response = new InteractionResponse();
		response.setSessionId(null); // 缺少 sessionId
		response.setRequestId("test-request-id");
		response.setSelectedOptionId("approve");

		webTestClient.post()
			.uri("/api/agent/chat/react-plus/interaction_response")
			.contentType(MediaType.APPLICATION_JSON)
			.bodyValue(response)
			.exchange()
			.expectStatus()
			.isOk()
			.expectBody()
			.jsonPath("$.success")
			.isEqualTo(false)
			.jsonPath("$.message")
			.isEqualTo("sessionId不能为空");
	}

	@Test
	@DisplayName("6. 参数验证测试 - 缺少 requestId")
	void testMissingRequestId() {
		InteractionResponse response = new InteractionResponse();
		response.setSessionId(testSessionId);
		response.setRequestId(null); // 缺少 requestId
		response.setSelectedOptionId("approve");

		webTestClient.post()
			.uri("/api/agent/chat/react-plus/interaction_response")
			.contentType(MediaType.APPLICATION_JSON)
			.bodyValue(response)
			.exchange()
			.expectStatus()
			.isOk()
			.expectBody()
			.jsonPath("$.success")
			.isEqualTo(false)
			.jsonPath("$.message")
			.isEqualTo("requestId不能为空");
	}

	@Test
	@DisplayName("7. 参数验证测试 - 缺少 selectedOptionId")
	void testMissingSelectedOptionId() {
		InteractionResponse response = new InteractionResponse();
		response.setSessionId(testSessionId);
		response.setRequestId("test-request-id");
		response.setSelectedOptionId(null); // 缺少 selectedOptionId

		webTestClient.post()
			.uri("/api/agent/chat/react-plus/interaction_response")
			.contentType(MediaType.APPLICATION_JSON)
			.bodyValue(response)
			.exchange()
			.expectStatus()
			.isOk()
			.expectBody()
			.jsonPath("$.success")
			.isEqualTo(false)
			.jsonPath("$.message")
			.isEqualTo("selectedOptionId不能为空");
	}

	@Test
	@DisplayName("8. 自动生成 sessionId 测试")
	void testAutoGenerateSessionId() {
		ChatRequest request = new ChatRequest();
		request.setSessionId(null); // 不提供 sessionId
		request.setMessage("测试消息");
		request.setUserId("test-user");

		@SuppressWarnings("rawtypes")
		FluxExchangeResult<ServerSentEvent> result = webTestClient.post()
			.uri("/api/agent/chat/react-plus/stream")
			.contentType(MediaType.APPLICATION_JSON)
			.bodyValue(request)
			.exchange()
			.expectStatus()
			.isOk()
			.returnResult(ServerSentEvent.class);

		// 验证可以正常接收事件
		StepVerifier.create(result.getResponseBody().take(1)).expectNextCount(1).verifyComplete();
	}

}
