# 工具审批机制 - 测试指南

## 测试文件说明

### 1. ToolApprovalSimpleTest.java ✅ 推荐使用

**位置:** `real-agent-web/src/test/java/com/ai/agent/real/web/controller/agent/ToolApprovalSimpleTest.java`

**特点:**
- ✅ 简单直接，专注于 API 接口测试
- ✅ 无复杂的异步处理
- ✅ 快速验证参数验证逻辑
- ✅ 易于理解和维护

**测试用例:**
1. 参数验证 - 缺少 sessionId
2. 参数验证 - 缺少 requestId  
3. 参数验证 - 缺少 selectedOptionId
4. 参数验证 - 空字符串 sessionId
5. 正常请求 - 同意执行
6. 正常请求 - 拒绝执行
7. 正常请求 - 终止对话
8. SSE 流接口 - 基础连通性测试
9. SSE 流接口 - 自动生成 sessionId

**运行方式:**
```bash
# Maven
mvn test -Dtest=ToolApprovalSimpleTest

# IDE
右键点击类名 -> Run 'ToolApprovalSimpleTest'
```

---

### 2. ReActPlusAgentControllerTest.java ⚠️ 集成测试

**位置:** `real-agent-web/src/test/java/com/ai/agent/real/web/controller/agent/ReActPlusAgentControllerTest.java`

**特点:**
- 完整的端到端测试
- 包含 SSE 流处理
- 测试真实的工具审批流程
- 需要完整的 Spring 上下文

**注意事项:**
- 有一些泛型类型警告（已添加 @SuppressWarnings）
- 需要真实的 AI 模型和工具服务
- 运行时间较长
- 可能需要配置 API Key

**测试用例:**
1. 基础流程测试 - 启动对话并接收事件
2. 工具审批流程测试 - 完整的暂停和恢复
3. 拒绝工具执行测试
4. 终止对话测试
5-8. 参数验证测试

---

## 手动测试指南

### 使用 cURL 测试

#### 1. 启动对话（SSE 流）

```bash
curl -N -X POST http://localhost:8080/api/agent/chat/react-plus/stream \
  -H "Content-Type: application/json" \
  -d '0'
```

**预期输出:**
```
event: THINKING
data: {"type":"THINKING","message":"正在思考..."}

event: ACTING
data: {"type":"ACTING","message":"准备调用工具..."}

event: TOOL_APPROVAL
data: {
  "type":"TOOL_APPROVAL",
  "data":{
    "requestId":"call_abc123",
    "context":{
      "toolName":"get_weather",
      "toolArgs":{"city":"杭州"}
    },
    "options":[
      {"optionId":"approve","label":"同意执行"},
      {"optionId":"reject","label":"拒绝并说明理由"},
      {"optionId":"terminate","label":"拒绝并终止对话"}
    ]
  }
}
```

#### 2. 提交审批决策（同意执行）

```bash
curl -X POST http://localhost:8080/api/agent/chat/react-plus/interaction_response \
  -H "Content-Type: application/json" \
  -d '{
    "sessionId": "test-001",
    "requestId": "call_abc123",
    "selectedOptionId": "approve"
  }'
```

**预期响应:**
```json
{
  "success": true,
  "message": "用户响应已处理，正在恢复执行",
  "sessionId": "test-001",
  "timestamp": "2025-11-12T00:00:00"
}
```

#### 3. 提交审批决策（拒绝执行）

```bash
curl -X POST http://localhost:8080/api/agent/chat/react-plus/interaction_response \
  -H "Content-Type: application/json" \
  -d '{
    "sessionId": "test-001",
    "requestId": "call_abc123",
    "selectedOptionId": "reject",
    "feedback": "这个操作太危险了，请使用其他方法"
  }'
```

#### 4. 提交审批决策（终止对话）

```bash
curl -X POST http://localhost:8080/api/agent/chat/react-plus/interaction_response \
  -H "Content-Type: application/json" \
  -d '{
    "sessionId": "test-001",
    "requestId": "call_abc123",
    "selectedOptionId": "terminate",
    "feedback": "我不需要这个功能了"
  }'
```

---

### 使用 Postman 测试

#### 1. 创建 SSE 请求

1. 新建请求，选择 POST 方法
2. URL: `http://localhost:8080/api/agent/chat/react-plus/stream`
3. Headers:
   - `Content-Type: application/json`
4. Body (raw JSON):
```json
{
  "sessionId": "test-001",
  "message": "帮我查询杭州的天气",
  "userId": "test-user"
}
```
5. 点击 Send，观察 SSE 事件流

#### 2. 创建审批响应请求

1. 新建请求，选择 POST 方法
2. URL: `http://localhost:8080/api/agent/chat/react-plus/interaction_response`
3. Headers:
   - `Content-Type: application/json`
4. Body (raw JSON):
```json
{
  "sessionId": "test-001",
  "requestId": "call_abc123",
  "selectedOptionId": "approve"
}
```
5. 点击 Send

---

## 前端测试示例

### 使用浏览器 Console 测试

```javascript
// 1. 连接 SSE 流
const eventSource = new EventSource('/api/agent/chat/react-plus/stream?sessionId=test-001&message=帮我查询杭州的天气');

eventSource.onmessage = (event) => {
    const data = JSON.parse(event.data);
    console.log('收到事件:', data.type, data);
    
    if (data.type === 'TOOL_APPROVAL') {
        console.log('需要审批:', data.data);
        // 记录 requestId 用于后续审批
        window.toolRequestId = data.data.requestId;
    }
};

eventSource.onerror = (error) => {
    console.error('SSE 错误:', error);
};

// 2. 提交审批决策
function approveToolExecution() {
    fetch('/api/agent/chat/react-plus/interaction_response', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
            sessionId: 'test-001',
            requestId: window.toolRequestId,
            selectedOptionId: 'approve'
        })
    })
    .then(response => response.json())
    .then(data => console.log('审批结果:', data));
}

// 3. 拒绝工具执行
function rejectToolExecution() {
    fetch('/api/agent/chat/react-plus/interaction_response', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
            sessionId: 'test-001',
            requestId: window.toolRequestId,
            selectedOptionId: 'reject',
            feedback: '这个操作太危险了'
        })
    })
    .then(response => response.json())
    .then(data => console.log('审批结果:', data));
}

// 4. 终止对话
function terminateConversation() {
    fetch('/api/agent/chat/react-plus/interaction_response', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
            sessionId: 'test-001',
            requestId: window.toolRequestId,
            selectedOptionId: 'terminate',
            feedback: '我不需要这个功能了'
        })
    })
    .then(response => response.json())
    .then(data => console.log('审批结果:', data));
}
```

---

## 测试场景

### 场景 1: 正常审批流程

1. 前端发起对话请求
2. AI 开始执行，推送 THINKING、ACTING 事件
3. 检测到工具调用，推送 TOOL_APPROVAL 事件
4. 前端显示审批 UI
5. 用户点击"同意执行"
6. 前端提交审批决策
7. 后端恢复执行，执行工具
8. 推送 TOOL 事件（工具结果）
9. 继续推送 OBSERVING、COMPLETED 事件

### 场景 2: 拒绝工具执行

1-4. 同场景 1
5. 用户点击"拒绝并说明理由"，输入反馈
6. 前端提交拒绝决策
7. 后端将拒绝理由添加到上下文
8. AI 重新思考，可能提出新方案
9. 继续执行或结束

### 场景 3: 终止对话

1-4. 同场景 1
5. 用户点击"拒绝并终止对话"
6. 前端提交终止决策
7. 后端结束会话，关闭 SSE 连接
8. 前端收到连接关闭事件

### 场景 4: 多次审批

1. 对话过程中可能有多个工具需要审批
2. 每次审批都会暂停执行
3. 用户可以分别对每个工具做出决策
4. 使用 requestId 区分不同的审批请求

---

## 常见问题排查

### 1. SSE 连接立即关闭

**可能原因:**
- 服务端异常
- 配置错误
- 网络问题

**排查方法:**
```bash
# 查看服务端日志
tail -f logs/application.log | grep "ReAct-Plus"

# 检查端口是否正常
netstat -an | grep 8080
```

### 2. 审批请求未收到

**可能原因:**
- 工具审批模式未设置为 REQUIRE_APPROVAL
- 工具调用未触发
- 事件推送失败

**排查方法:**
```bash
# 检查配置
grep -r "toolApprovalMode" application.yml

# 查看日志
tail -f logs/application.log | grep "TOOL_APPROVAL"
```

### 3. 审批提交后无响应

**可能原因:**
- sessionId 不匹配
- requestId 不匹配
- 会话已过期

**排查方法:**
```bash
# 查看审批处理日志
tail -f logs/application.log | grep "handleInteractionResponse"

# 检查会话状态
# (需要添加调试接口)
```

---

## 性能测试

### 并发测试

```bash
# 使用 Apache Bench
ab -n 100 -c 10 -p request.json -T application/json \
  http://localhost:8080/api/agent/chat/react-plus/interaction_response

# 使用 wrk
wrk -t4 -c100 -d30s --latency \
  -s post.lua \
  http://localhost:8080/api/agent/chat/react-plus/interaction_response
```

### 压力测试

```bash
# 使用 JMeter
# 创建测试计划，模拟多用户同时发起对话和审批
```

---

## 总结

1. **快速验证:** 使用 `ToolApprovalSimpleTest` 进行单元测试
2. **完整测试:** 使用 `ReActPlusAgentControllerTest` 进行集成测试
3. **手动测试:** 使用 cURL 或 Postman 进行接口测试
4. **前端测试:** 使用浏览器 Console 或实际前端应用测试

建议按照以下顺序进行测试:
1. 单元测试（参数验证）
2. 接口测试（cURL/Postman）
3. 前端集成测试
4. 端到端测试
5. 性能测试
