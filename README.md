# real-agent 多Agent策略框架

一个基于Spring AI的多Agent协作框架，支持多种Agent策略模式，能够智能选择最适合的Agent来处理不同类型的任务。

## 🚀 核心特性

### 多Agent策略支持
- **单Agent模式**: 选择最适合的单个Agent处理任务
- **协作模式**: 多个Agent协同工作，整合各自的专业能力
- **竞争模式**: 多个Agent并行处理，选择最优结果
- **流水线模式**: Agent按顺序处理任务的不同阶段

### 专业Agent实现
- **代码分析专家**: 专门处理代码分析、架构设计、性能优化等任务
- **文档生成专家**: 专门处理技术文档、API文档、用户手册等文档生成
- **通用助手**: 处理各种通用任务的万能Agent

### 智能调度机制
- 基于任务内容自动选择最适合的Agent
- 支持置信度评分和能力匹配
- 动态策略选择和优先级管理

## 📋 架构设计

```
┌─────────────────────────────────────────┐
│           Web Controller Layer          │
├─────────────────────────────────────────┤
│         Multi-Agent Strategy Layer      │
├─────────────────────────────────────────┤
│           Agent Management Layer        │
├─────────────────────────────────────────┤
│            Agent Core Layer             │
├─────────────────────────────────────────┤
│           Tool & Framework Layer        │
├─────────────────────────────────────────┤
│            Common & Config Layer        │
└─────────────────────────────────────────┘
```

## 🛠️ 快速开始

### 1. 环境要求
- Java 17+
- Spring Boot 3.4+
- Maven 3.6+

### 2. 配置文件
在 `application.yml` 中配置：

```yaml
kit:
  agent:
    enabled: true
    multi-agent-enabled: true
    default-strategy: "SingleAgent"

spring:
  ai:
    openai:
      api-key: ${OPENAI_API_KEY}
```

### 3. 使用示例

```java
@Autowired
private AgentManager agentManager;

// 执行任务 - 自动选择策略
AgentResult result = agentManager.executeTask("分析这段代码的性能问题", toolContext);

// 执行任务 - 指定策略
AgentResult result = agentManager.executeTask("生成API文档", toolContext, "Collaborative");
```

## 📚 详细文档

### Agent策略说明

#### 1. 单Agent策略 (SingleAgent)
- **适用场景**: 明确的单一任务
- **工作方式**: 选择置信度最高的Agent处理任务
- **优势**: 简单高效，响应快速

#### 2. 协作策略 (Collaborative)
- **适用场景**: 复杂任务需要多种专业能力
- **工作方式**: 多个Agent并行工作，整合结果
- **优势**: 综合多个专家的意见，结果更全面

#### 3. 竞争策略 (Competitive)
- **适用场景**: 需要最优解的任务
- **工作方式**: 多个Agent并行处理，选择最佳结果
- **优势**: 通过竞争获得最优质的输出

#### 4. 流水线策略 (Pipeline)
- **适用场景**: 需要分阶段处理的复杂任务
- **工作方式**: Agent按顺序处理不同阶段
- **优势**: 专业化分工，每个阶段都有专门的Agent处理

### Agent通信机制

框架提供了完整的Agent间通信支持：

```java
// 发送消息
AgentMessage message = AgentMessage.createRequest(
    "sender-agent", "receiver-agent", "请协助分析", "task-123");
messageBus.sendMessage(message);

// 接收消息
AgentMessage received = messageBus.receiveMessage("agent-id");
```

## 🔧 扩展开发

### 自定义Agent

```java
public class CustomAgent extends Agent {
    public CustomAgent(ChatModel chatModel) {
        super("custom-agent", "自定义Agent", "处理特定任务", chatModel);
    }

    @Override
    public AgentResult execute(String task, ToolContext context) {
        // 实现具体的任务处理逻辑
        return AgentResult.success("处理结果", this.agentId);
    }

    @Override
    public boolean canHandle(String task) {
        // 判断是否能处理该任务
        return task.contains("特定关键词");
    }

    @Override
    public double getConfidenceScore(String task) {
        // 返回处理该任务的置信度
        return 0.8;
    }
}
```

### 自定义策略

```java
public class CustomStrategy implements AgentStrategy {
    @Override
    public String getStrategyName() {
        return "Custom";
    }

    @Override
    public AgentResult execute(String task, List<Agent> agents, ToolContext context) {
        // 实现自定义的策略逻辑
        return null;
    }

    @Override
    public boolean isApplicable(String task, List<Agent> agents) {
        // 判断策略是否适用
        return true;
    }

    @Override
    public int getPriority() {
        return 10; // 设置优先级
    }
}
```

## 📊 监控和调试

框架提供了丰富的监控和调试功能：

```java
// 获取管理器状态
Map<String, Object> status = agentManager.getStatus();

// 查看消息历史
List<AgentMessage> history = messageBus.getMessageHistory("agent-id");

// 获取能处理任务的Agent列表
List<Agent> capableAgents = agentManager.getCapableAgents("任务描述");
```

## 🧪 测试

运行测试：
```bash
mvn test
```

查看测试覆盖率：
```bash
mvn test jacoco:report
```

## 📄 许可证

MIT License

## 🤝 贡献

欢迎提交 Issue 和 Pull Request！

# 项目核心逻辑
主要是构建一个 由 提示词驱动的