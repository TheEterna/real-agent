package com.ai.agent.real.contract.model.context;

import com.ai.agent.real.common.constant.*;
import com.ai.agent.real.contract.model.message.*;
import com.ai.agent.real.contract.model.message.AgentMessage.*;
import com.ai.agent.real.contract.model.property.*;
import org.antlr.v4.runtime.misc.*;
import org.springframework.util.*;

import java.util.*;
import java.util.concurrent.*;

/**
 * Agent 记忆管理, 主要也是一个缓存的作用 session 为主键, 其 context 为 value, 当 close the session, 则 remove
 * (需要考虑需不需要等待若干秒后异步去 删除)
 *
 *
 * thought: 这个压缩级别应该是 session 级别的, 个人设置里 可以选择 开启压缩的级别, 选择后, 需要指定需要压缩的会话, 然后这个级别就会生效
 * 这个后面要做持久化的, 需要有单独的 压缩message表 和 压缩 session 表
 *
 * @author han
 * @time 2025/10/13 0:05
 */
public class AgentMemory {

	/**
	 * 缓存 Agent 上下文
	 */
	private ConcurrentHashMap<String, Pair<List<AgentMessage>, AgentSessionConfig>> memory;
	/**
	 * 默认的 session 配置
	 */
	private AgentSessionConfig defaultSessionConfig;

	private AgentMemory(
			ConcurrentHashMap<String, Pair<List<AgentMessage>, AgentSessionConfig>> memory,
			AgentSessionConfig defaultSessionConfig) {
		this.memory = memory;
		this.defaultSessionConfig = defaultSessionConfig;
	}

	public void addTurn(String sessionId, List<AgentMessage> messages, AgentSessionConfig config) {
		Assert.isTrue(Objects.nonNull(config), "config 不能为空");
		// 如果已经有了这个 sessionId, 就add messages 到 原来的 list 中
		if (this.memory.containsKey(sessionId)) {
			List<AgentMessage> oldMessages = this.memory.get(sessionId).a;
			oldMessages.addAll(messages);
		} else {
			this.memory.put(sessionId, new Pair<>(new CopyOnWriteArrayList<>(messages), config));
		}
	}
	public void addTurn(String sessionId, List<AgentMessage> messages) {
		addTurn(sessionId, messages, defaultSessionConfig);
	}

	public List<AgentMessage> getMessageHistory(String sessionId) {

		Assert.isTrue(StringUtils.hasText(sessionId), "sessionId 不存在");
		// TODO, 这里需要 去做 DB 和 缓存, 因为现在的缓存是在 java 内存里的, 不具备 分布式, 也不具备 DDL 等基础功能

		// 目前阶段不存在 DDL, 不会删除, 也不会 GC 掉, 所以 只有第一次会话才会 找不到 该 sessionId
//		Assert.isTrue(this.memory.containsKey(sessionId), "第一次会话, 需要访问数据库");

		// 如果不存在 sessionId, 则返回空列表
		// TODO 接入中间件之后 就应该是去查 Redis 看有没有这个 sessionId, 就去查 Redis, 没有则去 查数据库 返回, 然后 redis里就存一份, DB insert 后进行 Redis 回填
		if (!this.memory.containsKey(sessionId)) {
			this.memory.put(sessionId, new Pair<>(new CopyOnWriteArrayList<>(), defaultSessionConfig));
			return List.of();
		}

		// 去查一下 sessionConfig, 这里在考虑 是否应该在 缓存 里保存全量数据, 还是 保存 当时状态的数据, 这也有个很致命的缺点, 当 用户偏好更换 缓存即一定程度上失效
		// 这里使用 全量
		Pair<List<AgentMessage>, AgentSessionConfig> agentSessionConfigPair = this.memory.get(sessionId);
		List<AgentMessage> messageHistory = agentSessionConfigPair.a;
		AgentSessionConfig config = agentSessionConfigPair.b;
		if (config.getZipMode() == ContextZipMode.DISABLED) {
			return messageHistory;
		} else if (config.getZipMode() == ContextZipMode.ZIP) {
			// 压缩, 只取 每轮对话 的 最后一句
			List<AgentMessage> compressedMessages = new CopyOnWriteArrayList<>();
			messageHistory.stream().filter(message -> NounConstants.FINAL_AGENT_ID.equals(message.getSenderId())
					&& AgentMessageType.COMPLETED.equals(message.getAgentMessageType()))
					.forEach(compressedMessages::add);

			// todo 可以加一些超长截断等操作

			return compressedMessages;
		} else if (config.getZipMode() == ContextZipMode.CRAZY_ZIP) {
			// Crazy 压缩
			// TODO: 需要引入AI , 暂不实现
			throw new UnsupportedOperationException("暂不支持 Crazy 压缩");
		}

		return messageHistory;

	}
	public static Builder builder() {
		return new Builder();
	}

	public static class Builder {

		private ConcurrentHashMap<String, Pair<List<AgentMessage>, AgentSessionConfig>> memory;

		private Builder() {
			this.memory = new ConcurrentHashMap<>();
		}

		public Builder addSession(String sessionId, List<AgentMessage> messages, AgentSessionConfig config) {
			this.memory.put(sessionId, new Pair<>(new CopyOnWriteArrayList<>(messages), config));
			return this;
		}

		public Builder memory(Map<String, Pair<List<AgentMessage>, AgentSessionConfig>> memory) {
			if (this.memory != null) {
				throw new IllegalArgumentException("memory is already set");
			}
			memory.forEach((sessionId, pair) -> {
				this.memory.put(sessionId, new Pair<>(new CopyOnWriteArrayList<>(pair.a), pair.b));
			});
			return this;
		}

		public AgentMemory build(AgentSessionConfig defaultSessionConfig) {
			return new AgentMemory(this.memory, defaultSessionConfig);
		}

	}

}
