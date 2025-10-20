package com.ai.agent.real.agent.strategy;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Session 与 Turn 的追踪器
 * 
 * 核心思想：
 * 在 WebFlux 异步环境下，Reactor Context 无法跨越 MCP 回调边界传递 turnId
 * 
 * 解决方案：
 * - 维护 sessionId -> 当前活跃的 turnId 的映射
 * - 前提假设：同一 session 的请求是串行的（一次只有一个 turn）
 * - 当新 turn 开始时，注册映射
 * - 当 MCP elicit 回调时，通过 sessionId 查找当前 turnId
 * - turn 完成后，清理映射
 * 
 * 适用场景：
 * ✅ 用户逐个发送消息（常见场景）
 * ❌ 同一用户并发多个对话（罕见场景）
 * 
 * 注意：
 * - 如果需要支持同一 session 并发多个 turn，需要使用队列方案
 * - 或者通过 MCP meta 字段传递 turnId
 *
 * @author han
 * @time 2025/10/19 22:47
 */
@Slf4j
@Service
public class SessionTurnTracker {

    /**
     * sessionId -> 当前活跃的 turnId
     * 
     * 假设：同一 session 同时只有一个活跃的 turn
     */
    private final Map<String, String> sessionToTurn = new ConcurrentHashMap<>();

    /**
     * 注册新的 turn
     * 
     * @param sessionId 会话 ID
     * @param turnId 当前轮次 ID
     */
    public void registerTurn(String sessionId, String turnId) {
        if (sessionId == null || sessionId.isBlank()) {
            log.warn("sessionId 为空，无法注册 turn");
            return;
        }

        String previousTurnId = sessionToTurn.put(sessionId, turnId);
        
        if (previousTurnId != null && !previousTurnId.equals(turnId)) {
            log.warn("sessionId={} 已有活跃的 turn={}, 被新 turn={} 覆盖。" +
                "如果这不是预期行为，请检查是否有并发请求。", 
                sessionId, previousTurnId, turnId);
        }
        
        log.debug("注册 turn: sessionId={}, turnId={}", sessionId, turnId);
    }

    /**
     * 获取 session 当前活跃的 turnId
     * 
     * @param sessionId 会话 ID
     * @return 当前活跃的 turnId，如果不存在则返回 null
     */
    public String getCurrentTurnId(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            log.warn("sessionId 为空，无法获取 turnId");
            return null;
        }

        String turnId = sessionToTurn.get(sessionId);
        
        if (turnId == null) {
            log.warn("sessionId={} 没有活跃的 turn，可能 turn 已完成或未注册", sessionId);
        } else {
            log.debug("获取当前 turn: sessionId={}, turnId={}", sessionId, turnId);
        }
        
        return turnId;
    }

    /**
     * 移除 turn 映射
     * 
     * @param sessionId 会话 ID
     * @param turnId 轮次 ID
     */
    public void unregisterTurn(String sessionId, String turnId) {
        if (sessionId == null || sessionId.isBlank()) {
            return;
        }

        String currentTurnId = sessionToTurn.get(sessionId);
        
        // 只有当前活跃的 turn 才能移除
        if (turnId.equals(currentTurnId)) {
            sessionToTurn.remove(sessionId);
            log.debug("移除 turn: sessionId={}, turnId={}", sessionId, turnId);
        } else {
            log.warn("尝试移除非活跃的 turn: sessionId={}, turnId={}, currentTurnId={}", 
                sessionId, turnId, currentTurnId);
        }
    }

    /**
     * 清理指定 session 的映射
     * 
     * @param sessionId 会话 ID
     */
    public void clearSession(String sessionId) {
        if (sessionId != null && !sessionId.isBlank()) {
            sessionToTurn.remove(sessionId);
            log.debug("清理 session: sessionId={}", sessionId);
        }
    }

    /**
     * 获取当前追踪的 session 数量
     */
    public int getSessionCount() {
        return sessionToTurn.size();
    }
}
