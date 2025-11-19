# Real-Agent 配置管理最佳实践

## 🎯 目标与原则

### 核心目标
1. **安全性** - 保护敏感配置信息
2. **可维护性** - 便于配置的修改和版本管理
3. **可扩展性** - 支持新模块和新环境
4. **一致性** - 统一的配置规范和命名约定
5. **可观测性** - 配置变更的追踪和审计

### 设计原则
1. **配置与代码分离** - 配置不硬编码在代码中
2. **环境隔离** - 不同环境使用独立配置
3. **最小权限** - 只配置必需的权限和访问
4. **默认安全** - 安全配置作为默认选项
5. **向后兼容** - 配置变更不破坏现有功能

## 🔐 安全配置管理

### 1. 敏感信息管理

#### 绝对禁止的做法 ❌
```yaml
# 错误：硬编码敏感信息
spring:
  ai:
    openai:
      api-key: sk-1234567890abcdef  # 绝对不要这样做！
  r2dbc:
    password: secretpassword123    # 绝对不要这样做！
```

#### 推荐的做法 ✅

**方式1：环境变量**
```yaml
spring:
  ai:
    openai:
      api-key: ${DASHSCOPE_API_KEY:}
  r2dbc:
    password: ${DB_PASSWORD:}
```

**方式2：外部配置文件**
```yaml
spring:
  config:
    import: optional:file:./secrets/database.yml
```

**方式3：Spring Cloud Config**
```yaml
spring:
  config:
    import: configserver:http://config-server:8888
```

### 2. 密钥轮换策略

#### 实现配置热更新
```java
@Component
@RefreshScope
public class ApiKeyManager {

    @Value("${spring.ai.openai.api-key}")
    private String apiKey;

    @EventListener(RefreshScopeRefreshedEvent.class)
    public void onConfigRefresh() {
        log.info("API Key configuration refreshed");
        // 重新初始化客户端
        reinitializeClients();
    }
}
```

#### 密钥版本管理
```yaml
# 支持多个 API Key 配置
spring:
  ai:
    openai:
      primary-key: ${DASHSCOPE_API_KEY_PRIMARY:}
      secondary-key: ${DASHSCOPE_API_KEY_SECONDARY:}
      key-rotation:
        enabled: true
        check-interval: 1h
```

### 3. 访问控制配置

```yaml
security:
  # 管理端点访问控制
  management:
    endpoints:
      roles: [ADMIN, MONITOR]
      allowed-ips: [127.0.0.1, 10.0.0.0/8]

  # 配置端点保护
  config:
    encryption:
      enabled: true
      key: ${CONFIG_ENCRYPTION_KEY:}
```

## 🏗️ 模块化配置设计

### 1. 配置文件组织原则

#### 按功能域分组
```
config/
├── core/              # 核心业务逻辑配置
│   ├── agent-config.yml
│   ├── tool-config.yml
│   └── strategy-config.yml
├── infrastructure/    # 基础设施配置
│   ├── database-config.yml
│   ├── ai-model-config.yml
│   └── mcp-config.yml
└── integration/       # 集成配置
    ├── third-party-apis.yml
    └── webhooks.yml
```

#### 配置继承和覆盖策略
```yaml
# base-config.yml (基础配置)
default: &default
  timeout: 30s
  retry:
    max-attempts: 3
    delay: 1s

# specific-config.yml (特定配置)
production:
  <<: *default
  timeout: 60s  # 覆盖基础配置
```

### 2. 配置属性类设计

#### 使用类型安全的配置类
```java
@ConfigurationProperties(prefix = "agent")
@Validated
@Data
public class AgentProperties {

    @Valid
    @NotNull
    private React react = new React();

    @Valid
    @NotNull
    private Action action = new Action();

    @Data
    public static class React {
        @Positive
        @Max(50)
        private int maxIterations = 10;

        @DurationMin(seconds = 1)
        @DurationMax(minutes = 10)
        private Duration thinkingTimeout = Duration.ofSeconds(30);
    }

    @Data
    public static class Action {
        @NotNull
        @Pattern(regexp = "AUTO|REQUIRE_APPROVAL|DISABLED")
        private String approvalMode = "AUTO";

        @Positive
        private int maxConcurrentExecutions = 5;
    }
}
```

#### 配置元数据生成
```java
@ConfigurationProperties(prefix = "tool.system")
@Data
public class ToolSystemProperties {

    /**
     * 是否启用自动工具发现
     */
    @DefaultValue("true")
    private boolean autoDiscovery = true;

    /**
     * 工具执行超时时间
     */
    @DefaultValue("30s")
    private Duration executionTimeout = Duration.ofSeconds(30);
}
```

### 3. 条件化配置

#### 基于环境的条件配置
```java
@Configuration
@Profile("!test")
public class ProductionMcpConfig {

    @Bean
    @ConditionalOnProperty(name = "mcp.client.enabled", havingValue = "true")
    public McpClient mcpClient() {
        return new McpClientImpl();
    }
}

@Configuration
@Profile("test")
public class TestMcpConfig {

    @Bean
    public McpClient mcpClient() {
        return new MockMcpClient();
    }
}
```

#### 基于功能开关的配置
```yaml
features:
  voice:
    enabled: ${FEATURE_VOICE_ENABLED:false}
  omni:
    enabled: ${FEATURE_OMNI_ENABLED:false}
  collaborative-agents:
    enabled: ${FEATURE_COLLABORATIVE_AGENTS_ENABLED:false}
```

```java
@ConditionalOnProperty(name = "features.voice.enabled", havingValue = "true")
@Configuration
public class VoiceConfiguration {
    // 语音功能配置
}
```

## 🌍 环境管理策略

### 1. 环境分层策略

#### 四层环境架构
```
development → testing → staging → production
    ↓           ↓         ↓          ↓
   dev.yml   test.yml   staging.yml prod.yml
```

#### 配置继承关系
```yaml
# application.yml (基础)
spring:
  application:
    name: real-agent-web

# application-base.yml (共同基础)
common: &common
  agent:
    react:
      max-iterations: 10

# application-dev.yml (开发)
spring:
  profiles:
    include: base
<<: *common
agent:
  react:
    max-iterations: 5  # 开发环境减少迭代

# application-prod.yml (生产)
spring:
  profiles:
    include: base
<<: *common
real-agent:
  action:
    tools:
      approval-mode: REQUIRE_APPROVAL  # 生产环境需要审批
```

### 2. 配置验证机制

#### 环境特定配置验证
```java
@Component
@Profile("prod")
public class ProductionConfigValidator implements ApplicationRunner {

    @Value("${spring.ai.openai.api-key:}")
    private String apiKey;

    @Value("${security.ssl.enabled:false}")
    private boolean sslEnabled;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        validateProductionConfig();
    }

    private void validateProductionConfig() {
        Assert.hasText(apiKey, "API key must be configured in production");
        Assert.isTrue(sslEnabled, "SSL must be enabled in production");
        // 其他生产环境配置验证
    }
}
```

#### 配置完整性检查
```java
@Component
public class ConfigIntegrityChecker {

    @Autowired
    private AgentProperties agentProperties;

    @PostConstruct
    public void checkConfigIntegrity() {
        // 检查配置的一致性和完整性
        validateAgentConfiguration();
        validateToolConfiguration();
        validateDatabaseConfiguration();
    }
}
```

### 3. 配置版本管理

#### Git 配置管理
```gitignore
# .gitignore
/config/secrets/
/config/local/
*.env
application-local.yml
```

#### 配置版本标记
```yaml
# 在配置文件中添加版本信息
config:
  version: "1.2.0"
  last-updated: "2024-01-15T10:30:00Z"
  changelog:
    - "Added MCP connection pooling"
    - "Updated database timeout settings"
```

## 📊 配置监控与可观测性

### 1. 配置变更追踪

#### 配置变更事件监听
```java
@Component
public class ConfigChangeListener {

    private static final Logger logger = LoggerFactory.getLogger(ConfigChangeListener.class);

    @EventListener
    public void handleConfigRefresh(RefreshScopeRefreshedEvent event) {
        logger.info("Configuration refreshed: {}", event.getName());
        // 记录配置变更到审计日志
        auditConfigChange(event);
    }

    @EventListener
    public void handleEnvironmentChange(EnvironmentChangeEvent event) {
        logger.info("Environment changed: {}", event.getKeys());
        // 通知相关组件配置已变更
        notifyConfigChange(event.getKeys());
    }
}
```

#### 配置差异检测
```java
@Component
public class ConfigDiffTracker {

    private Map<String, Object> lastKnownConfig = new HashMap<>();

    @Scheduled(fixedRate = 300000) // 每5分钟检查一次
    public void trackConfigChanges() {
        Map<String, Object> currentConfig = getCurrentConfig();
        Map<String, Object> changes = detectChanges(lastKnownConfig, currentConfig);

        if (!changes.isEmpty()) {
            logger.info("Configuration changes detected: {}", changes);
            publishConfigChangeEvent(changes);
        }

        lastKnownConfig = currentConfig;
    }
}
```

### 2. 配置健康检查

#### 配置健康指示器
```java
@Component
public class ConfigHealthIndicator implements HealthIndicator {

    @Autowired
    private Environment environment;

    @Override
    public Health health() {
        try {
            Health.Builder builder = Health.up();

            // 检查关键配置项
            checkRequiredProperties(builder);
            checkDatabaseConnection(builder);
            checkApiConnectivity(builder);

            return builder.build();
        } catch (Exception e) {
            return Health.down()
                .withDetail("error", e.getMessage())
                .build();
        }
    }

    private void checkRequiredProperties(Health.Builder builder) {
        String apiKey = environment.getProperty("spring.ai.openai.api-key");
        if (StringUtils.hasText(apiKey)) {
            builder.withDetail("api-key", "configured");
        } else {
            builder.withDetail("api-key", "missing").down();
        }
    }
}
```

### 3. 配置指标监控

#### 配置相关指标
```java
@Component
public class ConfigMetrics {

    private final MeterRegistry meterRegistry;
    private final Counter configRefreshCounter;
    private final Timer configLoadTime;

    public ConfigMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        this.configRefreshCounter = Counter.builder("config.refresh.total")
            .description("Total number of config refreshes")
            .register(meterRegistry);
        this.configLoadTime = Timer.builder("config.load.duration")
            .description("Configuration load time")
            .register(meterRegistry);
    }

    @EventListener
    public void onConfigRefresh(RefreshScopeRefreshedEvent event) {
        configRefreshCounter.increment(
            Tags.of("source", event.getName())
        );
    }
}
```

## 🚀 性能优化配置

### 1. 配置缓存策略

#### 配置属性缓存
```java
@Configuration
@EnableCaching
public class ConfigCacheConfiguration {

    @Bean
    public CacheManager configCacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();
        cacheManager.setCaffeine(Caffeine.newBuilder()
            .maximumSize(1000)
            .expireAfterWrite(10, TimeUnit.MINUTES)
            .recordStats());
        return cacheManager;
    }
}

@Service
public class ConfigService {

    @Cacheable(value = "configCache", key = "#configKey")
    public String getConfigValue(String configKey) {
        return environment.getProperty(configKey);
    }

    @CacheEvict(value = "configCache", allEntries = true)
    public void clearConfigCache() {
        // 清除配置缓存
    }
}
```

### 2. 懒加载配置

#### 条件化 Bean 创建
```java
@Configuration
public class LazyConfigurationLoader {

    @Bean
    @Lazy
    @ConditionalOnProperty(name = "mcp.client.enabled", havingValue = "true")
    public McpClient mcpClient() {
        // 只在需要时创建 MCP 客户端
        return new McpClientImpl();
    }

    @Bean
    @Lazy
    @ConditionalOnProperty(name = "features.voice.enabled", havingValue = "true")
    public VoiceService voiceService() {
        // 只在启用语音功能时创建
        return new VoiceServiceImpl();
    }
}
```

### 3. 配置预加载策略

#### 关键配置预热
```java
@Component
public class ConfigPreloader implements ApplicationReadyEvent {

    @Autowired
    private AgentProperties agentProperties;

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        // 预加载关键配置，避免首次请求延迟
        preloadCriticalConfig();
    }

    private void preloadCriticalConfig() {
        // 触发配置属性的初始化
        agentProperties.getReact().getMaxIterations();
        agentProperties.getAction().getApprovalMode();

        logger.info("Critical configuration preloaded successfully");
    }
}
```

## 🔄 配置自动化管理

### 1. 配置模板化

#### Helm Charts 配置模板
```yaml
# templates/configmap.yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: {{ include "real-agent.fullname" . }}-config
data:
  application.yml: |
    spring:
      profiles:
        active: {{ .Values.environment }}
    real-agent:
      action:
        tools:
          approval-mode: {{ .Values.agent.approvalMode | default "AUTO" }}
    {{- if .Values.database.enabled }}
    spring:
      r2dbc:
        url: {{ .Values.database.url }}
    {{- end }}
```

#### Ansible 配置管理
```yaml
# playbooks/configure-real-agent.yml
- name: Configure Real-Agent
  hosts: real-agent-servers
  vars:
    config_template: "application-{{ environment }}.yml.j2"
  tasks:
    - name: Generate configuration file
      template:
        src: "{{ config_template }}"
        dest: "/opt/real-agent/config/application.yml"
        backup: yes
      notify: restart real-agent

    - name: Validate configuration
      command: java -jar real-agent.jar --spring.config.location=file:./config/ --dry-run
      register: config_validation
      failed_when: config_validation.rc != 0
```

### 2. 配置自动发现

#### 服务发现集成
```java
@Configuration
@ConditionalOnProperty(name = "spring.cloud.discovery.enabled", havingValue = "true")
public class ServiceDiscoveryConfig {

    @Bean
    public DiscoveryClientConfigService configService(DiscoveryClient discoveryClient) {
        return new DiscoveryClientConfigService(discoveryClient);
    }
}

@Service
public class DiscoveryClientConfigService {

    @Scheduled(fixedRate = 60000)
    public void updateServiceEndpoints() {
        // 自动发现服务端点并更新配置
        List<ServiceInstance> mcpServers = discoveryClient.getInstances("mcp-server");
        updateMcpServerConfig(mcpServers);
    }
}
```

### 3. 配置验证自动化

#### CI/CD 配置验证
```yaml
# .github/workflows/config-validation.yml
name: Configuration Validation
on:
  pull_request:
    paths:
      - 'src/main/resources/**/*.yml'
      - 'config/**/*.yml'

jobs:
  validate-config:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v2

      - name: Validate YAML syntax
        run: |
          find . -name "*.yml" -exec yamllint {} \;

      - name: Test configuration loading
        run: |
          ./mvnw test -Dtest=ConfigurationTest

      - name: Security scan
        run: |
          # 扫描配置文件中的敏感信息
          ./scripts/scan-secrets.sh
```

## 📋 配置检查清单

### 开发阶段 ✅

- [ ] 配置文件按模块分离
- [ ] 敏感信息使用环境变量
- [ ] 配置类型安全（使用 @ConfigurationProperties）
- [ ] 配置验证注解完整
- [ ] 默认配置合理
- [ ] 配置文档更新

### 测试阶段 ✅

- [ ] 所有环境配置测试通过
- [ ] 配置加载性能测试
- [ ] 配置验证逻辑测试
- [ ] 配置热更新测试
- [ ] 错误配置处理测试

### 部署阶段 ✅

- [ ] 生产环境配置审查
- [ ] 安全配置检查
- [ ] 性能配置优化
- [ ] 监控配置设置
- [ ] 备份配置策略

### 运维阶段 ✅

- [ ] 配置变更审计
- [ ] 配置监控告警
- [ ] 配置备份恢复
- [ ] 配置文档维护
- [ ] 配置优化调整

---

遵循这些最佳实践，Real-Agent 项目的配置管理将更加安全、可维护和高效，为项目的长期发展奠定坚实基础。