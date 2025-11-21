//package com.ai.agent.real.web.controller.agent;
//
//import com.ai.agent.real.contract.dto.ChatRequest;
//import com.ai.agent.real.contract.model.interaction.InteractionResponse;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.http.MediaType;
//import org.springframework.test.web.reactive.server.WebTestClient;
//
///**
// * 工具审批机制 - 简化版测试 专注于测试 API 接口的参数验证和基本功能
// *
// * @author han
// * @time 2025/11/12 00:20
// */
//@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
//@AutoConfigureWebTestClient
//@DisplayName("工具审批机制 - 简化测试")
//public class ToolApprovalSimpleTest {
//
//	@Autowired
//	private WebTestClient webTestClient;
//
//	@Test
//	@DisplayName("1. 参数验证 - 缺少 sessionId")
//	void testMissingSessionId() {
//		InteractionResponse response = new InteractionResponse();
//		response.setSessionId(null);
//		response.setRequestId("test-request-id");
//		response.setSelectedOptionId("approve");
//
//		webTestClient.post()
//			.uri("/api/agent/chat/react-plus/interaction_response")
//			.contentType(MediaType.APPLICATION_JSON)
//			.bodyValue(response)
//			.exchange()
//			.expectStatus()
//			.isOk()
//			.expectBody()
//			.jsonPath("$.success")
//			.isEqualTo(false)
//			.jsonPath("$.message")
//			.isEqualTo("sessionId不能为空");
//	}
//
//	@Test
//	@DisplayName("2. 参数验证 - 缺少 requestId")
//	void testMissingRequestId() {
//		InteractionResponse response = new InteractionResponse();
//		response.setSessionId("test-session");
//		response.setRequestId(null);
//		response.setSelectedOptionId("approve");
//
//		webTestClient.post()
//			.uri("/api/agent/chat/react-plus/interaction_response")
//			.contentType(MediaType.APPLICATION_JSON)
//			.bodyValue(response)
//			.exchange()
//			.expectStatus()
//			.isOk()
//			.expectBody()
//			.jsonPath("$.success")
//			.isEqualTo(false)
//			.jsonPath("$.message")
//			.isEqualTo("requestId不能为空");
//	}
//
//	@Test
//	@DisplayName("3. 参数验证 - 缺少 selectedOptionId")
//	void testMissingSelectedOptionId() {
//		InteractionResponse response = new InteractionResponse();
//		response.setSessionId("test-session");
//		response.setRequestId("test-request-id");
//		response.setSelectedOptionId(null);
//
//		webTestClient.post()
//			.uri("/api/agent/chat/react-plus/interaction_response")
//			.contentType(MediaType.APPLICATION_JSON)
//			.bodyValue(response)
//			.exchange()
//			.expectStatus()
//			.isOk()
//			.expectBody()
//			.jsonPath("$.success")
//			.isEqualTo(false)
//			.jsonPath("$.message")
//			.isEqualTo("selectedOptionId不能为空");
//	}
//
//	@Test
//	@DisplayName("4. 参数验证 - 空字符串 sessionId")
//	void testBlankSessionId() {
//		InteractionResponse response = new InteractionResponse();
//		response.setSessionId("");
//		response.setRequestId("test-request-id");
//		response.setSelectedOptionId("approve");
//
//		webTestClient.post()
//			.uri("/api/agent/chat/react-plus/interaction_response")
//			.contentType(MediaType.APPLICATION_JSON)
//			.bodyValue(response)
//			.exchange()
//			.expectStatus()
//			.isOk()
//			.expectBody()
//			.jsonPath("$.success")
//			.isEqualTo(false)
//			.jsonPath("$.message")
//			.isEqualTo("sessionId不能为空");
//	}
//
//	@Test
//	@DisplayName("5. 正常请求 - 同意执行")
//	void testApproveExecution() {
//		InteractionResponse response = new InteractionResponse();
//		response.setSessionId("test-session-" + System.currentTimeMillis());
//		response.setRequestId("test-request-id");
//		response.setSelectedOptionId("approve");
//
//		// 注意：这个测试会失败，因为会话不存在
//		// 但可以验证参数验证通过，进入了业务逻辑
//		webTestClient.post()
//			.uri("/api/agent/chat/react-plus/interaction_response")
//			.contentType(MediaType.APPLICATION_JSON)
//			.bodyValue(response)
//			.exchange()
//			.expectStatus()
//			.isOk()
//			.expectBody()
//			.jsonPath("$.sessionId")
//			.isEqualTo(response.getSessionId());
//	}
//
//	@Test
//	@DisplayName("6. 正常请求 - 拒绝执行")
//	void testRejectExecution() {
//		InteractionResponse response = new InteractionResponse();
//		response.setSessionId("test-session-" + System.currentTimeMillis());
//		response.setRequestId("test-request-id");
//		response.setSelectedOptionId("reject");
//		response.setFeedback("这个操作太危险了");
//
//		webTestClient.post()
//			.uri("/api/agent/chat/react-plus/interaction_response")
//			.contentType(MediaType.APPLICATION_JSON)
//			.bodyValue(response)
//			.exchange()
//			.expectStatus()
//			.isOk()
//			.expectBody()
//			.jsonPath("$.sessionId")
//			.isEqualTo(response.getSessionId());
//	}
//
//	@Test
//	@DisplayName("7. 正常请求 - 终止对话")
//	void testTerminateConversation() {
//		InteractionResponse response = new InteractionResponse();
//		response.setSessionId("test-session-" + System.currentTimeMillis());
//		response.setRequestId("test-request-id");
//		response.setSelectedOptionId("terminate");
//		response.setFeedback("我不需要这个功能了");
//
//		webTestClient.post()
//			.uri("/api/agent/chat/react-plus/interaction_response")
//			.contentType(MediaType.APPLICATION_JSON)
//			.bodyValue(response)
//			.exchange()
//			.expectStatus()
//			.isOk()
//			.expectBody()
//			.jsonPath("$.sessionId")
//			.isEqualTo(response.getSessionId());
//	}
//
//	@Test
//	@DisplayName("8. SSE 流接口 - 基础连通性测试")
//	void testStreamEndpointConnectivity() {
//		ChatRequest request = new ChatRequest();
//		request.setSessionId("test-session-" + System.currentTimeMillis());
//		request.setMessage("你好");
//
//		// 只验证接口可以正常响应，不验证具体内容
//		webTestClient.post()
//			.uri("/api/agent/chat/react-plus/stream")
//			.contentType(MediaType.APPLICATION_JSON)
//			.bodyValue(request)
//			.exchange()
//			.expectStatus()
//			.isOk()
//			.expectHeader()
//			.contentTypeCompatibleWith(MediaType.TEXT_EVENT_STREAM);
//	}
//
//	@Test
//	@DisplayName("9. SSE 流接口 - 自动生成 sessionId")
//	void testStreamAutoGenerateSessionId() {
//		ChatRequest request = new ChatRequest();
//		request.setSessionId(null); // 不提供 sessionId
//		request.setMessage("测试消息");
//
//		webTestClient.post()
//			.uri("/api/agent/chat/react-plus/stream")
//			.contentType(MediaType.APPLICATION_JSON)
//			.bodyValue(request)
//			.exchange()
//			.expectStatus()
//			.isOk()
//			.expectHeader()
//			.contentTypeCompatibleWith(MediaType.TEXT_EVENT_STREAM);
//	}
//
//}
