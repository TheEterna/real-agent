package com.ai.agent.kit.core.config;


import lombok.*;
import lombok.experimental.*;
import org.springframework.boot.context.properties.*;
import org.springframework.stereotype.*;

import java.util.*;

import static com.ai.agent.kit.common.constant.SystemConstants.SYSTEM_PROPERTY_PREFIX;

/**
 * 存储所有配置，比如 agent策略的配置，系统的各项配置等
 * @author han
 * @time 2025/9/5 13:18
 */

@ConfigurationProperties(prefix = SYSTEM_PROPERTY_PREFIX)
@Component
@Data
public class SystemProperties {
    /**
     * 默认策略名称
     */
    private String defaultStrategy = "SingleAgent";

    /**
     * 工具超时时间
     */
    private Integer toolTimeout = 30000;

    /**
     * Agent定义
     */
    @Data
    @Accessors(chain = true)
    public static class AgentDefinition {
        private String id;
        private String name;
        private String description;
        private String className;
        private String[] capabilities;
        private Map<String, Object> properties;
        private boolean enabled = true;
    }

    /**
     * 策略定义
     */
    @Data
    @Accessors(chain = true)
    public static class StrategyDefinition {
        private String name;
        private String className;
        private int priority = 1;
        private boolean enabled = true;
        private Map<String, Object> properties;
    }

    /**
     * 消息总线配置
     */
    @Data
    @Accessors(chain = true)
    public static class MessageBusConfig {
        private int queueSize = 1000;
        private int threadPoolSize = 10;
        private long messageTimeoutMs = 30000;
        private boolean enableHistory = true;
        private int maxHistorySize = 1000;
    }

    record AgentProperty(ReActProperty reAct, ReActProperty planAndExecute) {

    }
    record ReAction() {

    }
    record ReActProperty() {

    }
}
