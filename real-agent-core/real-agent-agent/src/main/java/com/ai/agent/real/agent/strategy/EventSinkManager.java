package com.ai.agent.real.agent.strategy;

import com.ai.agent.real.contract.model.protocol.*;
import lombok.extern.slf4j.*;
import reactor.core.publisher.*;

import java.time.*;
import java.util.*;
import java.util.concurrent.*;

/**
 * 事件 Sink 管理器
 *
 * 核心职责： 1. 管理每个 turnId 对应的独立 Sink 2. 在 WebFlux 异步环境下正确隔离不同轮次的事件 3. 自动清理过期的 Sink，防止内存泄漏
 *
 * 设计要点： - 使用 turnId（单轮对话ID）作为 key，不是 sessionId - 每个 turn 有独立的 Sink，互不干扰 - WebFlux 环境下
 * ThreadLocal 失效，必须通过 turnId 显式传递 - 定期清理过期的 Sink（默认 1 小时未使用则清理）
 *
 * 典型流程： 1. ReActAgentStrategy.executeStream() 创建 Sink 2. ElicitationService 通过 turnId 获取
 * Sink 并注入事件 3. Strategy 自动合并内部流和 Sink 的事件 4. 执行完成后清理 Sink
 *
 * @author han
 * @time 2025/10/17 14:13
 */
@Slf4j
public class EventSinkManager {

	/**
	 * turnId -> Sink 映射 每个 turnId 对应一个独立的 Sink
	 */
	private final Map<String, SinkWrapper> turnSinks = new ConcurrentHashMap<>();

	/**
	 * 定时清理器
	 */
	private final ScheduledExecutorService cleanupScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
		Thread thread = new Thread(r, "EventSinkManager-Cleanup");
		thread.setDaemon(true);
		return thread;
	});

	/**
	 * Sink 过期时间（毫秒）
	 */
	private static final long SINK_EXPIRATION_MS = Duration.ofHours(1).toMillis();

	/**
	 * 清理间隔（分钟）
	 */
	private static final long CLEANUP_INTERVAL_MINUTES = 10;

	public EventSinkManager() {
		// 启动定期清理任务
		cleanupScheduler.scheduleAtFixedRate(this::cleanupExpiredSinks, CLEANUP_INTERVAL_MINUTES,
				CLEANUP_INTERVAL_MINUTES, TimeUnit.MINUTES);
		log.info("EventSinkManager 初始化完成，清理间隔: {} 分钟", CLEANUP_INTERVAL_MINUTES);
	}

	/**
	 * 为指定 turnId 创建或获取 Sink
	 * @param turnId 单轮对话 ID
	 * @return 该 turn 对应的 Sink
	 */
	public Sinks.Many<AgentExecutionEvent> getOrCreateSink(String turnId) {
		if (turnId == null || turnId.isBlank()) {
			log.warn("turnId 为空，使用默认 Sink");
			turnId = "default-" + System.currentTimeMillis();
		}

		SinkWrapper wrapper = turnSinks.computeIfAbsent(turnId, k -> {
			log.debug("为 turnId={} 创建新的 Sink", k);
			Sinks.Many<AgentExecutionEvent> sink = Sinks.many().multicast().onBackpressureBuffer();
			return new SinkWrapper(sink, System.currentTimeMillis());
		});

		// 更新最后访问时间
		wrapper.updateLastAccessTime();

		return wrapper.getSink();
	}

	/**
	 * 获取指定 turnId 的 Sink（如果不存在则返回 null）
	 * @param turnId 单轮对话 ID
	 * @return Sink 或 null
	 */
	public Sinks.Many<AgentExecutionEvent> getSink(String turnId) {
		if (turnId == null || turnId.isBlank()) {
			log.warn("turnId 为空");
			return null;
		}

		SinkWrapper wrapper = turnSinks.get(turnId);
		if (wrapper != null) {
			wrapper.updateLastAccessTime();
			return wrapper.getSink();
		}

		log.debug("turnId={} 的 Sink 不存在", turnId);
		return null;
	}

	/**
	 * 移除指定 turnId 的 Sink
	 * @param turnId 单轮对话 ID
	 */
	public void removeSink(String turnId) {
		if (turnId == null || turnId.isBlank()) {
			return;
		}

		SinkWrapper wrapper = turnSinks.remove(turnId);
		if (wrapper != null) {
			log.debug("移除 turnId={} 的 Sink", turnId);
			// 尝试完成 Sink
			wrapper.getSink().tryEmitComplete();
		}
	}

	/**
	 * 清理过期的 Sink
	 */
	private void cleanupExpiredSinks() {
		long now = System.currentTimeMillis();
		int beforeSize = turnSinks.size();

		turnSinks.entrySet().removeIf(entry -> {
			String turnId = entry.getKey();
			SinkWrapper wrapper = entry.getValue();
			long age = now - wrapper.getLastAccessTime();

			if (age > SINK_EXPIRATION_MS) {
				log.info("清理过期 Sink: turnId={}, age={}ms", turnId, age);
				wrapper.getSink().tryEmitComplete();
				return true;
			}
			return false;
		});

		int afterSize = turnSinks.size();
		if (beforeSize != afterSize) {
			log.info("Sink 清理完成: 清理前={}, 清理后={}, 移除={}", beforeSize, afterSize, beforeSize - afterSize);
		}
	}

	/**
	 * Sink 包装类 包含 Sink 本身和最后访问时间
	 */
	private static class SinkWrapper {

		private final Sinks.Many<AgentExecutionEvent> sink;

		private volatile long lastAccessTime;

		public SinkWrapper(Sinks.Many<AgentExecutionEvent> sink, long lastAccessTime) {
			this.sink = sink;
			this.lastAccessTime = lastAccessTime;
		}

		public Sinks.Many<AgentExecutionEvent> getSink() {
			return sink;
		}

		public long getLastAccessTime() {
			return lastAccessTime;
		}

		public void updateLastAccessTime() {
			this.lastAccessTime = System.currentTimeMillis();
		}

	}

}
