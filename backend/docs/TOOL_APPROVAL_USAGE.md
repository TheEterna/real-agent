# 工具审批机制 - 使用指南

## 概述

ReAct-Plus Agent 支持工具执行前的人工审批机制。当 AI 需要调用工具时，会暂停执行并等待用户审批，用户可以选择同意、拒绝或终止对话。

---

## API 接口

### 1. 启动对话（SSE 流式接口）

**接口:** `POST /api/agent/chat/react-plus/stream`

**请求体:**
```json
{
  "sessionId": "session-123",  // 可选，不提供会自动生成
  "message": "帮我查询杭州的天气",
  "userId": "user-001"
}
```

**响应:** SSE 事件流

**事件类型:**
- `THINKING`: AI 思考阶段
- `ACTING`: AI 行动阶段  
- `TOOL_APPROVAL`: 工具审批请求（需要用户响应）
- `TOOL`: 工具执行结果
- `OBSERVING`: AI 观察阶段
- `COMPLETED`: 任务完成

---

### 2. 提交审批决策

**接口:** `POST /api/agent/chat/react-plus/interaction_response`

**请求体:**
```json
{
  "sessionId": "session-123",
  "requestId": "call_abc123",      // 来自 TOOL_APPROVAL 事件的 requestId
  "selectedOptionId": "approve",   // 选项: approve / reject / terminate
  "feedback": "请继续执行"          // 可选，拒绝时可提供理由
}
```

**响应:**
```json
{
  "success": true,
  "message": "用户响应已处理，正在恢复执行",
  "sessionId": "session-123",
  "timestamp": "2025-11-12T00:00:00"
}
```

---

## 前端集成示例

### 完整的 JavaScript 客户端

```javascript
class ReActPlusClient {
    constructor(sessionId) {
        this.sessionId = sessionId || `session-${Date.now()}`;
        this.eventSource = null;
        this.messageHandler = null;
    }

    // 1. 连接 SSE 流
    connect(message, onMessage) {
        this.messageHandler = onMessage;
        
        // 发起 POST 请求启动对话
        fetch('/api/agent/chat/react-plus/stream', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                sessionId: this.sessionId,
                message: message
            })
        }).then(response => {
            // 从响应中读取 SSE 流
            const reader = response.body.getReader();
            const decoder = new TextDecoder();
            
            const readStream = () => {
                reader.read().then(({ done, value }) => {
                    if (done) {
                        console.log('SSE 流结束');
                        return;
                    }
                    
                    const text = decoder.decode(value);
                    const lines = text.split('\n');
                    
                    for (const line of lines) {
                        if (line.startsWith('data: ')) {
                            const data = JSON.parse(line.substring(6));
                            this.handleEvent(data);
                        }
                    }
                    
                    readStream();
                });
            };
            
            readStream();
        });
    }

    // 2. 处理不同类型的事件
    handleEvent(event) {
        console.log('收到事件:', event.type, event);
        
        switch (event.type) {
            case 'THINKING':
                this.messageHandler({
                    type: 'thinking',
                    content: event.message
                });
                break;
                
            case 'ACTING':
                this.messageHandler({
                    type: 'acting',
                    content: event.message
                });
                break;
                
            case 'TOOL_APPROVAL':
                // 显示审批对话框
                this.showApprovalDialog(event.data);
                break;
                
            case 'TOOL':
                this.messageHandler({
                    type: 'tool',
                    content: `工具执行完成: ${event.data.name}`,
                    data: event.data
                });
                break;
                
            case 'OBSERVING':
                this.messageHandler({
                    type: 'observing',
                    content: event.message
                });
                break;
                
            case 'COMPLETED':
                this.messageHandler({
                    type: 'completed',
                    content: '任务完成'
                });
                break;
        }
    }

    // 3. 显示审批对话框
    showApprovalDialog(approvalData) {
        const { requestId, context, options } = approvalData;
        const { toolName, toolArgs } = context;
        
        // 构建对话框内容
        const dialogHtml = `
            <div class="approval-dialog">
                <h3>工具执行审批</h3>
                <p>Agent 请求执行工具: <strong>${toolName}</strong></p>
                <pre>${JSON.stringify(toolArgs, null, 2)}</pre>
                
                <div class="options">
                    ${options.map(opt => `
                        <button 
                            class="option-btn ${opt.dangerous ? 'dangerous' : ''}"
                            onclick="handleApproval('${requestId}', '${opt.optionId}', ${opt.requiresInput})"
                        >
                            ${opt.label}
                        </button>
                    `).join('')}
                </div>
            </div>
        `;
        
        // 显示对话框（具体实现根据你的 UI 框架）
        this.messageHandler({
            type: 'approval_request',
            requestId: requestId,
            toolName: toolName,
            toolArgs: toolArgs,
            options: options
        });
    }

    // 4. 提交审批决策
    submitApproval(requestId, optionId, feedback = null) {
        return fetch('/api/agent/chat/react-plus/interaction_response', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                sessionId: this.sessionId,
                requestId: requestId,
                selectedOptionId: optionId,
                feedback: feedback
            })
        }).then(response => response.json());
    }

    // 5. 断开连接
    disconnect() {
        if (this.eventSource) {
            this.eventSource.close();
        }
    }
}

// 使用示例
const client = new ReActPlusClient();

client.connect('帮我查询杭州的天气', (message) => {
    console.log('收到消息:', message);
    
    if (message.type === 'approval_request') {
        // 显示审批 UI
        showApprovalUI(message);
    } else {
        // 显示普通消息
        appendMessage(message.content);
    }
});

// 用户点击审批按钮时
function handleApproval(requestId, optionId, requiresInput) {
    let feedback = null;
    
    if (requiresInput) {
        feedback = prompt('请输入反馈信息:');
    }
    
    client.submitApproval(requestId, optionId, feedback)
        .then(response => {
            console.log('审批提交成功:', response);
            // 关闭审批对话框
            closeApprovalDialog();
        });
}
```

---

## Vue 3 集成示例

```vue
<template>
  <div class="chat-container">
    <!-- 消息列表 -->
    <div class="messages">
      <div 
        v-for="msg in messages" 
        :key="msg.id"
        :class="['message', msg.type]"
      >
        {{ msg.content }}
      </div>
    </div>

    <!-- 审批对话框 -->
    <div v-if="approvalRequest" class="approval-dialog">
      <h3>工具执行审批</h3>
      <p>Agent 请求执行工具: <strong>{{ approvalRequest.toolName }}</strong></p>
      <pre>{{ JSON.stringify(approvalRequest.toolArgs, null, 2) }}</pre>
      
      <div class="options">
        <button 
          v-for="opt in approvalRequest.options"
          :key="opt.optionId"
          :class="['option-btn', { dangerous: opt.dangerous }]"
          @click="handleApproval(opt)"
        >
          {{ opt.label }}
        </button>
      </div>
    </div>

    <!-- 输入框 -->
    <div class="input-area">
      <input 
        v-model="userInput" 
        @keyup.enter="sendMessage"
        placeholder="输入消息..."
      />
      <button @click="sendMessage">发送</button>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue';

const sessionId = ref(`session-${Date.now()}`);
const messages = ref([]);
const userInput = ref('');
const approvalRequest = ref(null);
let reader = null;

// 发送消息
async function sendMessage() {
  if (!userInput.value.trim()) return;
  
  const message = userInput.value;
  userInput.value = '';
  
  // 添加用户消息
  messages.value.push({
    id: Date.now(),
    type: 'user',
    content: message
  });

  // 连接 SSE 流
  const response = await fetch('/api/agent/chat/react-plus/stream', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      sessionId: sessionId.value,
      message: message
    })
  });

  reader = response.body.getReader();
  const decoder = new TextDecoder();

  const readStream = async () => {
    const { done, value } = await reader.read();
    if (done) return;

    const text = decoder.decode(value);
    const lines = text.split('\n');

    for (const line of lines) {
      if (line.startsWith('data: ')) {
        const data = JSON.parse(line.substring(6));
        handleEvent(data);
      }
    }

    readStream();
  };

  readStream();
}

// 处理事件
function handleEvent(event) {
  switch (event.type) {
    case 'THINKING':
    case 'ACTING':
    case 'OBSERVING':
      messages.value.push({
        id: Date.now(),
        type: event.type.toLowerCase(),
        content: event.message
      });
      break;

    case 'TOOL_APPROVAL':
      approvalRequest.value = {
        requestId: event.data.requestId,
        toolName: event.data.context.toolName,
        toolArgs: event.data.context.toolArgs,
        options: event.data.options
      };
      break;

    case 'TOOL':
      messages.value.push({
        id: Date.now(),
        type: 'tool',
        content: `工具执行完成: ${event.data.name}`
      });
      break;

    case 'COMPLETED':
      messages.value.push({
        id: Date.now(),
        type: 'system',
        content: '任务完成'
      });
      break;
  }
}

// 处理审批
async function handleApproval(option) {
  let feedback = null;

  if (option.requiresInput) {
    feedback = prompt(option.inputPrompt || '请输入反馈信息:');
    if (feedback === null) return; // 用户取消
  }

  const response = await fetch('/api/agent/chat/react-plus/interaction_response', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      sessionId: sessionId.value,
      requestId: approvalRequest.value.requestId,
      selectedOptionId: option.optionId,
      feedback: feedback
    })
  });

  const result = await response.json();
  console.log('审批提交成功:', result);

  // 关闭审批对话框
  approvalRequest.value = null;
}

onUnmounted(() => {
  if (reader) {
    reader.cancel();
  }
});
</script>

<style scoped>
.approval-dialog {
  position: fixed;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  background: white;
  padding: 2rem;
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0,0,0,0.15);
  z-index: 1000;
}

.option-btn {
  margin: 0.5rem;
  padding: 0.5rem 1rem;
  border: none;
  border-radius: 4px;
  cursor: pointer;
}

.option-btn.dangerous {
  background: #ff4d4f;
  color: white;
}
</style>
```

---

## 工作流程说明

### 1. 正常执行流程
```
前端发起请求 → AI 思考 → AI 行动 → 工具执行 → AI 观察 → 任务完成
```

### 2. 需要审批的流程
```
前端发起请求 → AI 思考 → AI 行动 → 
检测到工具调用 → 发送 TOOL_APPROVAL 事件 → 暂停执行 →
等待用户审批 → 用户提交决策 → 恢复执行 →
执行工具 → AI 观察 → 任务完成
```

---

## 审批选项说明

### approve (同意执行)
- 直接执行该工具
- 执行结果会通过 SSE 推送回前端
- 继续后续的观察和思考流程

### reject (拒绝并说明理由)
- 不执行工具
- 将拒绝理由反馈给 AI
- AI 会重新思考并可能提出新的方案

### terminate (拒绝并终止对话)
- 不执行工具
- 结束整个对话流程
- 关闭 SSE 连接

---

## 注意事项

1. **sessionId 管理**: 每个对话会话需要唯一的 sessionId，前端需要妥善保存
2. **SSE 连接**: 审批期间 SSE 连接保持打开，恢复执行后会继续推送事件
3. **超时处理**: 建议前端设置审批超时机制，避免会话长时间挂起
4. **错误处理**: 审批提交失败时，前端应提示用户重试
5. **并发审批**: 同一会话可能有多个工具需要审批，使用 requestId 区分

---

## 配置工具审批模式

在 Agent 配置中设置工具审批模式:

```java
// application.yml 或代码中配置
toolApprovalMode: REQUIRE_APPROVAL  // AUTO / REQUIRE_APPROVAL / DISABLED
```

- `AUTO`: 自动执行（未来可基于权限列表）
- `REQUIRE_APPROVAL`: 需要人工审批
- `DISABLED`: 禁用工具调用
