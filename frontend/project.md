## function calling

### easy example:

1. first request:
   
   ```json
    messages: [
        {
            "role": "user",
            "content": "What is the weather like in Beijing?"
        }
    ],
    "tools": [
        {
            "name": "get_current_weather",
            "arguments": {
                "location": "Beijing, China",
                "unit": "celsius"
            }
        }
    ]
   ```

2. first response (md, different model have different response content, only have a common format, such as qwen, the content of field is null, but some model are not empty, fuck
   make it difficult for font-end rendering to achieve universality
   ):
   
   ```json
    {
        "role": "assistant",
        "content": null or (use get_current_weather function to achieve this task),
        "tool_calls": [
            {
                "id": "call_123",
                "type": "function",
                "function": {
                    "name": "get_current_weather",
                    "arguments": "{\"location\": \"Beijing, China\", \"unit\": \"celsius\"}"
                }
            }
        ]
    }
   ```

after response, we need to send a second request or do some things to get the result of the function call
3. second request (need to carry the tool call result):
add the following message

```json
    {
      "role": "tool",
      "content": "25 degrees celsius",
      "tool_call_id": "call_123"
    }
```

4. 回复, 结束对话 finish dialog

然后很重要的, springAI 默认封装了2-4, 使用 returnDirect = true 封装2-3, 所以我们难以通过 结构化的校验来监听tool的调用, 什么意思呢?

正常使用, 不加 returnDirect = true, 进入步骤1, 相当于步骤2-3都是不可见的, 直接返回回答结果,示例: 今天北京天气为28度
加 returnDirect = true, 进入步骤1, 直接给你返回

```json
{
    [
        {
            "id": call_ef31a0f166f7423b88c5f9, 
            "name": 完成任务, 
            "responseData": "{
                "ok":true,
                "message": null,
                "code":null,
                "elapsedMs":0,
                "data":"task_done",
                "meta":{}
            }"
        }
    ], 
  "messageType": "TOOL",
  "metadata": {
    "messageType": "TOOL"
  }
}
```

springAi 进行了 封装, 封装代码如下:

```java
    /**
     * Build a list of {@link Generation} from the tool execution result, useful for
     * sending the tool execution result to the client directly.
     */
    static List<Generation> buildGenerations(ToolExecutionResult toolExecutionResult) {
        List<Message> conversationHistory = toolExecutionResult.conversationHistory();
        List<Generation> generations = new ArrayList<>();
        if (conversationHistory
            .get(conversationHistory.size() - 1) instanceof ToolResponseMessage toolResponseMessage) {
            toolResponseMessage.getResponses().forEach(response -> {
                AssistantMessage assistantMessage = new AssistantMessage(response.responseData());
                Generation generation = new Generation(assistantMessage,
                        ChatGenerationMetadata.builder()
                            .metadata(METADATA_TOOL_ID, response.id())
                            .metadata(METADATA_TOOL_NAME, response.name())
                            .finishReason(FINISH_REASON)
                            .build());
                generations.add(generation);
            });
        }
        return generations;
    }
```

可使用optionsBuilder.internalToolExecutionEnabled(false); 使用原生方式, 不进行任何封装

## 项目架构设计

### 模块划分

本项目采用 DDD（领域驱动设计）分层架构，各模块职责如下：

#### real-agent-domain（领域层）

- **职责**：核心业务实体和数据访问
- **包含内容**：
  - `entity/` - 实体类（User, PlaygroundRoleplayRole, PlaygroundRoleplaySession, PlaygroundRoleplaySessionMessage）
  - `repository/` - 数据访问接口（R2DBC Repository）
- **技术栈**：Spring Data R2DBC, Reactive Streams
- **依赖关系**：仅依赖 real-agent-contract

#### real-agent-application（应用层）

- **职责**：业务逻辑编排和服务实现
- **包含内容**：
  - `service/` - 业务服务类（PlaygroundRoleplayRoleService, PlaygroundRoleplaySessionService, PlaygroundRoleplaySessionMessageService）
  - `dto/` - 数据传输对象（Request/Response DTO）
- **技术栈**：Spring Boot, Reactor
- **依赖关系**：依赖 real-agent-domain, real-agent-common, real-agent-contract

#### real-agent-common（通用层）

- **职责**：通用工具和常量定义
- **包含内容**：
  - `util/` - 工具类（JsonUtils, StringUtils 等）
  - `constant/` - 常量定义
  - `exception/` - 自定义异常类
- **技术栈**：Java 基础库, Jackson
- **依赖关系**：无外部依赖

#### real-agent-contract（契约层）

- **职责**：接口规范和抽象定义
- **包含内容**：
  - `api/` - 接口定义
  - `model/` - 抽象模型
  - `enums/` - 枚举定义
- **技术栈**：Java 接口
- **依赖关系**：无外部依赖

#### real-agent-core（核心层）

- **职责**：系统核心功能模块
- **说明**：该模块为系统核心，暂不涉及角色扮演功能开发

#### real-agent-web（接入层）

- **职责**：HTTP 接口和 Web 交互
- **包含内容**：
  - `controller/` - REST 控制器
  - `config/` - Web 配置
  - `interceptor/` - 拦截器
- **技术栈**：Spring WebFlux, Spring Security
- **依赖关系**：依赖 real-agent-application, real-agent-common

### 数据库设计

#### 表结构

1. **users** - 用户基础信息表
2. **playground_roleplay_roles** - 角色定义表
3. **playground_roleplay_sessions** - 会话记录表  
4. **playground_roleplay_session_messages** - 会话消息表

#### 技术选型

- **数据库**：MySQL 8.0+
- **连接方式**：R2DBC（响应式数据库连接）
- **ORM**：Spring Data R2DBC
- **JSON 处理**：数据库 JSON 字段 + 应用层转换

### API 设计规范

#### RESTful 接口

```
# 角色管理
GET    /api/roles                    # 查询角色列表
GET    /api/roles/{id}               # 查询单个角色
POST   /api/roles                    # 创建角色
PUT    /api/roles/{id}               # 更新角色
DELETE /api/roles/{id}               # 删除角色

# 会话管理
POST   /api/sessions                 # 创建会话
GET    /api/sessions/{sessionCode}   # 查询会话详情
POST   /api/sessions/{sessionCode}/messages  # 添加消息
GET    /api/sessions/{sessionCode}/messages  # 查询消息历史
PUT    /api/sessions/{sessionCode}/end       # 结束会话
```

#### 响应式编程

- 所有 Service 方法返回 `Mono<T>` 或 `Flux<T>`
- Controller 层支持 WebFlux 响应式接口
- 数据库操作全部异步化

### 开发规范

#### 代码结构

- 实体类使用 `@Builder` 模式
- Repository 继承 `ReactiveCrudRepository`
- Service 使用响应式链式调用
- 统一异常处理和日志记录

#### JSON 字段处理

- 数据库存储：JSON 字段作为 VARCHAR 存储
- 应用层转换：使用 JsonUtils 工具类进行 Map ↔ String 转换
- 实体类设计：提供 `xxxStr` 持久化字段和 `xxx` 业务字段

#### 前后端对接

- 前端：Vue 3 + Axios
- 后端：Spring WebFlux + R2DBC
- 数据格式：统一 JSON 格式，支持响应式流
  
  

## Agent 策略

### ReAct 策略

#### 策略架构设计

    一共四个节点，核心逻辑三个节点 + 一个总结节点，三个核心逻辑节点分别是 Thought（思考）节点，Action（行动）节点，Observation（观察）节点。简单讲就是三个节点循环循环，任务完成就退出，一般讲有一个最大迭代次数 such as  --> for (int i = 0; i < MAX_ITERATION; i++)

#### Thought 设计

1. 为Action做指导，因为Action是工具执行，那指导就是 指导Action去执行什么工具，所以 Thought 的上下文中 就必须要有 工具一系列上下文

2. 其他的就是提示词调整了，很简单，通用来讲就是 思考问题，列出关键点，整理思维链，最主要的是要根据不同的应用场景去定制

#### Action 设计

1. 实际上也没什么好设计的，能操作的空间就只有 一个System Prompt，User Prompt，messages 没必要动，直接使用 项目规范的记忆架构即可，核心的 Tool 提示词，也不由Action操控，**但倒是可以对传过来 description 之类，做一些加工。**

2. 确实 有思路的，之前 一直想做一个 Tool 封装器，来额外暴露一些参数，来控制Tool的行为，比如 是否并发执行，Tool 执行优先级等等。之前的想法是直接拆出一个类，一个模块去做这件事，以一个协调者，参与者的身份介入，但是这个加粗文本的想法提出，确实值得思考，将工具封装能力 赋予 Agent 是不是一个更好的设计呢。

#### Observation 设计

1. 其实 ReAct 的优点就是非常之自然，很好的发挥了 大模型 原有的能力，突破了 工具调用 中断的问题，当面对必须调用外部工具的情况时，举个简单例子： 今天伦敦的天气怎么样？ [思考]（由于我不能获取当前日期，以及最新天气信息，所以我需要调用 日期查看工具和天气查看工具）-> [执行]（"2025-10-09", "晴天"）-> [观察]（今天即2025-10-09的伦敦的天气是晴朗的）
   当思维链 和 执行信息不可见时，就是这样的： 我需要查看今天的日期和天气 ，查看到今天是2025-10-09号，今天伦敦天气是晴朗的。
   这个效果是 超低结构化的，感觉起来就像是单纯设置了 思维链的提示词一样
2. 但这个和 最后的总结节点还是有一些区别的，因为这个观察节点是一个循环里的结论，而总结节点是针对整个任务，以及整个回答链的总结


### git commit 规范 

1. 双语编写：英文在前，中文在后
2. 格式规范：使用 type(scope): description 格式