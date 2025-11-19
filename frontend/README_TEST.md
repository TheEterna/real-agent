# 测试代码说明

## 已创建的测试文件

### 1. ToolApprovalSimpleTest ✅ 推荐

**文件:** `real-agent-web/src/test/java/com/ai/agent/real/web/controller/agent/ToolApprovalSimpleTest.java`

**说明:** 工具审批机制的简化版单元测试，专注于 API 接口的参数验证和基本功能测试。

**测试覆盖:**
- ✅ 参数验证（缺少 sessionId、requestId、selectedOptionId）
- ✅ 空字符串参数验证
- ✅ 正常请求（同意、拒绝、终止）
- ✅ SSE 流接口连通性测试
- ✅ 自动生成 sessionId 测试

**运行方式:**
```bash
# Maven
mvn test -Dtest=ToolApprovalSimpleTest

# Gradle
./gradlew test --tests ToolApprovalSimpleTest

# IDE
右键点击类名 -> Run 'ToolApprovalSimpleTest'
```

**特点:**
- 无编译警告
- 运行速度快
- 易于理解和维护
- 适合 CI/CD 集成

---

### 2. ReActPlusAgentControllerTest ⚠️ 集成测试

**文件:** `real-agent-web/src/test/java/com/ai/agent/real/web/controller/agent/ReActPlusAgentControllerTest.java`

**说明:** 完整的端到端集成测试，测试真实的工具审批暂停/恢复流程。

**测试覆盖:**
- 基础流程测试（启动对话并接收事件）
- 工具审批流程测试（完整的暂停和恢复）
- 拒绝工具执行测试
- 终止对话测试
- 参数验证测试
- 自动生成 sessionId 测试

**注意事项:**
- ⚠️ 有一些泛型类型警告（已添加 @SuppressWarnings，不影响功能）
- ⚠️ 需要完整的 Spring 上下文和 AI 服务
- ⚠️ 运行时间较长
- ⚠️ 可能需要配置 API Key

**运行方式:**
```bash
# Maven
mvn test -Dtest=ReActPlusAgentControllerTest

# IDE
右键点击类名 -> Run 'ReActPlusAgentControllerTest'
```

---

## 测试文档

### TOOL_APPROVAL_TEST_GUIDE.md

**位置:** `docs/TOOL_APPROVAL_TEST_GUIDE.md`

**内容:**
- 测试文件说明
- 手动测试指南（cURL、Postman）
- 前端测试示例
- 测试场景说明
- 常见问题排查
- 性能测试建议

---

## 快速开始

### 1. 运行简单测试

```bash
cd real-agent/real-agent-web
mvn test -Dtest=ToolApprovalSimpleTest
```

### 2. 手动测试（cURL）

```bash
# 启动对话
curl -N -X POST http://localhost:8080/api/agent/chat/react-plus/stream \
  -H "Content-Type: application/json" \
  -d '{"sessionId":"test-001","message":"帮我查询杭州的天气","userId":"test-user"}'

# 提交审批（在另一个终端）
curl -X POST http://localhost:8080/api/agent/chat/react-plus/interaction_response \
  -H "Content-Type: application/json" \
  -d '{"sessionId":"test-001","requestId":"call_abc123","selectedOptionId":"approve"}'
```

### 3. 浏览器测试

打开浏览器 Console，粘贴以下代码:

```javascript
// 连接 SSE 流
const es = new EventSource('/api/agent/chat/react-plus/stream?sessionId=test-001&message=你好');
es.onmessage = (e) => console.log(JSON.parse(e.data));

// 提交审批（需要先从 SSE 事件中获取 requestId）
fetch('/api/agent/chat/react-plus/interaction_response', {
  method: 'POST',
  headers: {'Content-Type': 'application/json'},
  body: JSON.stringify({
    sessionId: 'test-001',
    requestId: 'call_xxx',  // 从 TOOL_APPROVAL 事件中获取
    selectedOptionId: 'approve'
  })
}).then(r => r.json()).then(console.log);
```

---

## 测试建议

### 开发阶段
1. 先运行 `ToolApprovalSimpleTest` 验证基本功能
2. 使用 cURL 或 Postman 进行接口测试
3. 使用浏览器 Console 测试前端集成

### CI/CD 阶段
1. 只运行 `ToolApprovalSimpleTest`（快速、稳定）
2. 定期运行 `ReActPlusAgentControllerTest`（完整验证）

### 上线前
1. 完整运行所有测试
2. 进行压力测试
3. 验证真实场景

---

## 常见问题

### Q: 为什么有两个测试文件？

A: 
- `ToolApprovalSimpleTest`: 快速、稳定的单元测试，适合日常开发和 CI/CD
- `ReActPlusAgentControllerTest`: 完整的集成测试，适合上线前的完整验证

### Q: 集成测试有警告怎么办？

A: 这些是泛型类型和空指针检查的警告，已添加 `@SuppressWarnings`，不影响功能。这是由于 Spring WebTestClient 的 API 设计导致的，是正常现象。

### Q: 如何跳过测试？

A:
```bash
# Maven
mvn clean install -DskipTests

# Gradle
./gradlew build -x test
```

### Q: 测试失败怎么排查？

A:
1. 查看控制台输出的错误信息
2. 检查 `logs/application.log` 日志
3. 确认服务是否正常启动
4. 确认数据库连接是否正常
5. 确认 AI 服务配置是否正确

---

## 相关文档

- `TOOL_APPROVAL_USAGE.md` - 前端集成指南
- `TOOL_APPROVAL_SUMMARY.md` - 实现总结
- `TOOL_APPROVAL_TEST_GUIDE.md` - 详细测试指南

---

## 总结

✅ **推荐使用 `ToolApprovalSimpleTest` 进行日常测试**

这个测试文件:
- 无编译警告
- 运行速度快（< 10秒）
- 覆盖核心功能
- 易于维护

如需完整的端到端测试，可以使用 `ReActPlusAgentControllerTest`，但需要注意配置和运行时间。
