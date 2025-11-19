# role-play-agent


题目已经list的很详细了
实现点如下：
1. Java + vue 传统前后端架构
2. 利用 AI 做角色扮演，角色扮演的提示词如何来？ 比如 ai 并不了解实时热梗，导致无实时性，so 实现方案最好有且只有唯一，冷缓存角色 + 热搜索 + ai 生成
3. 语音聊天 如何实现，简单实现 浏览器自带转文字传递后端，大模型 输出，tts 文字 -> 语音，这是最快速的方案，效果也最差，微信“按住说话” 比较适合这种实现方式，如果题目中说的语音聊天 是 电话聊天，那就应该直接 调用 云厂商 API， 效果最好，成本最低，最可控，生态最好

以上就是解决方案，据此就能设计出一个基本符合需求的作品

Q&A:
1. actually 我认为这个网页，不具备任何实际生产落地意义，比较适合作为一个AI 工具的附庸，such as 豆包的 “智能体” 广场
2. 基本实现即以上所列三点，据时间长短决定开发功能
3. aliyun，最为普遍，开源较强，模型选择较多，最为熟悉，最为中庸，模型能力比较了解，其dashscope api 的功能也比较契合开发
4. ！！ 应该真正的实现 Agent，这也是我在不断摸索的，简单讲，不只是通过 System Prompt 去简单调控，这并不是真正的智能体，应该深度贯彻角色理念，比如说 一个agent 应该具有自主决策能力，比如 “柯南” 就应该有独有的 api 调用能力，独有的柯南系列知识库，独有的缜密COT Agent 框架，实现真正 role-play 智能体 ！！

### 关键技术： tool/function calling, web, vue/react




爸，我基于 [src/pages/playground/role-play-agent/README.md](cci:7://file:///e:/%E5%AD%A6%E4%B9%A0/%E9%A1%B9%E7%9B%AE/kit-agent/real-agent-font/src/pages/playground/role-play-agent/README.md:0:0-0:0) 的目标，给出一套“可落地、可演进”的架构设计方案。坚持你的三点核心：Java+Vue 前后端、冷缓存角色 + 热搜索 + AI生成、语音聊天两种实现路径；并深化成“真正的 Agent”（工具/知识/决策/COT/自治）。

总体设计总览
- 前端（Vue）：一个“角色扮演应用”页面，支持文本/语音双模对话、流式输出、工具调用参数表单、对话上下文展示、返回“Playground”按钮。
- 后端（Java/Spring Boot）：会话编排服务（对话管理、Prompt 组装、工具调用编排、角色记忆）、LLM 接入（Aliyun DashScope 等）、热搜索与知识检索（Web 搜索 + 向量库）、语音（STT/TTS）、可观测与风控。
- Agent 能力模型：角色冷缓存（Profile/Persona/规则）+ 热上下文增强（搜索/RAG）+ 工具调用（Function Calling/MCP）+ 反思/计划/多步推理（COT/子任务）+ 记忆（短期/长期）。

目录与模块规划
前端（Vue）
- 路由与页面
  - `src/pages/playground/role-play-agent/Index.vue` 角色扮演界面
  - 统一保留左上角返回按钮（返回 `/playground`）
- 组件建议
  - `components/ChatPanel.vue`：对话窗口、消息项、工具结果渲染
  - `components/ToolArgsForm.vue`：基于 JSON Schema 渲染参数表单（你当前已在 Tools 页面实现了 schema→表单，直接复用思路）
  - `components/AudioBar.vue`：语音输入（录音/停止/上传进度）与 TTS 播放
  - `components/RoleHeader.vue`：角色头像/名称/状态（在线/沉思/调用工具中）
- 状态管理
  - Pinia `stores/rolePlay.ts`：sessionId、messages、pendingToolCall、currentRole、settings（STT/TTS 模式、模型选择）
- 通信协议
  - REST：初始化角色、查询历史、工具清单
  - WebSocket/SSE：LLM 流式输出、工具执行进度、语音任务状态
- 资源
  - 角色封面图/头像存于 `src/assets/role-play-agent/roles/`
  - 统一 UI 变量与 SCSS（你当前项目已迁移 SCSS，延续即可）

后端（Java/Spring Boot）
- 模块划分（建议 Maven 多模块）
  - `role-core`：领域模型、会话/消息、角色配置、记忆服务
  - `role-orchestrator`：对话编排（Prompt 拼装、RAG、工具策略、COT 策略、反思/多步）
  - `llm-adapter`：Aliyun DashScope 接入（文本生成/函数调用/流式）、其他 LLM 适配（可扩展）
  - `retrieval`：热搜索（Bing/Aliyun/自研 MCP 搜索）、RAG（向量库 Milvus/Elastic/KNN，或本地 Faiss）
  - `tools`：工具注册与调用（参考你已有的工具执行框架），支持 MCP/内置工具/HTTP 工具
  - `voice`：STT/TTS（简单模式：浏览器 STT -> 后端 TTS；电话模式：云厂商通话 API 网关）
  - `infra`：认证鉴权、限流熔断、日志埋点、审计记录、配置中心（Spring Cloud Config 可选）
- 数据存储
  - 主库（PostgreSQL/MySQL）保存会话、消息、角色、工具调用日志
  - 向量库（Milvus/pgvector/ES vector）存储角色知识片段与对话长记忆
  - 对象存储（OSS/S3）存音频与大文件
- 可观测与风控
  - 关键事件埋点：请求耗时、LLM Token、工具调用成功率、搜索命中率、TTS 失败率
  - 基于用户/会话的速率限制与防滥用策略

核心流程设计
- 会话编排（一次用户发言到回复）
  1. 前端发送 `POST /api/role-chat/send`（sessionId, roleId, text/voiceId, meta）
  2. 后端接收并做基础清洗与安全检查（敏感词/注入防护）
  3. 角色冷缓存注入：加载角色 Persona/规则/口癖/工具权限/知识域（RoleProfile）
  4. 热搜索/知识增强：
     - 按需触发 Search/RAG，拼接“当下事实/知识摘要”进入 Prompt Context
     - 结合“用户最近对话”形成状态摘要（短记忆）
  5. Prompt 生成：System/Developer/Role、User message、Context（事实/检索/记忆）、工具函数声明
  6. LLM 对话：
     - 优先走 Function Calling（若模型判定需要工具）→ 工具调用 → 结果回注 → 再次续写
     - 无需工具时直接生成回复，支持流式
  7. 反思与连贯性：
     - 可选：对回复进行简短自检（Reflexion），再输出给用户
     - 对成功信息提炼摘要入“长期记忆向量库”（节流）
  8. 返回前端：流式输出 + 工具调用中间态（OBSERVING）+ 完成事件（task_done）
- 角色冷缓存 + 热搜索 + AI 生成
  - 冷缓存（RoleProfile）：角色设定（人物背景/语气/禁忌/边界）、工具能力白名单、专属知识库 ID
  - 热搜索：近实时事件，聚合多源，摘要去重后只留下关键信息（注意引用来源）
  - RAG：角色专属知识库检索（向量召回），拼接到 Prompt 中
- 工具调用（从“真 Agent”角度）
  - 工具来源：内置/HTTP/MCP。沿用你已有通用工具执行框架（有空指针防御；消息顺序修复已完成）
  - 工具路由策略：按角色白名单 + 用户语义触发 + 模型函数调用建议
  - 返回结构统一映射到消息流中（保持与 [Tools.vue](cci:7://file:///e:/%E5%AD%A6%E4%B9%A0/%E9%A1%B9%E7%9B%AE/kit-agent/real-agent-font/src/pages/Tools.vue:0:0-0:0) 的 ToolResultViewer 一致性）

接口与模型草案
- 前端 REST
  - `POST /api/role-chat/session/init` → { sessionId, roleId, roleProfile }
  - `POST /api/role-chat/send` → { sessionId, text | voiceId }（返回 SSE/WebSocket 流）
  - `GET /api/role-chat/history?sessionId=...`
  - `GET /api/role-chat/tools?roleId=...`（展示工具清单与 schema）
  - `POST /api/role-chat/voice/stt`（简单模式可省略，走浏览器 STT）
  - `POST /api/role-chat/voice/tts`（文本->音频，返回音频URL）
- 领域模型（简化）
  - Role: id, name, avatar, persona, toolsWhitelist, knowledgeBaseId
  - Session: id, roleId, userId, createdAt, meta
  - Message: id, sessionId, roleId, sender(user/agent/tool), type(text/audio/tool_result), content, toolName, createdAt
  - ToolSpec: name, description, inputSchema, category, mcpSpec
  - Memory: sessionId, kind(short/long), vectorId, text, embedding

语音聊天两种路径
- 简单“网页按住说话”版（快速上线）
  - 前端：浏览器 MediaRecorder 录音 -> 发送音频到后端 `voice/stt` 或直接本地 STT（若可用）
  - 后端：调用云厂商 ASR（阿里云语音识别 API）→ 文本 | TTS（DashScope/阿里云 TTS）→ 返回音频URL
  - 优点：开发快；缺点：实时性一般、端上生态较弱
- 电话聊天（效果最佳）
  - 直接对接云厂商通话 API（实时 ASR + TTS + 呼叫控制）
  - 前端：仅做状态页；后端：通话事件回调驱动 Agent
  - 优点：质量稳定、可控性强

Prompt/Agent 策略
- System Prompt 模板（角色化）
  - 人设、语气、禁忌、边界、能力、工具列表（函数签名）
- COT/反思/计划
  - “先思考再作答”，必要时才能调用工具；回复前快速自检
- 输出风格
  - 口癖模板 + 内容约束（不虚构/引用来源）
- 角色定制工具
  - 例如“柯南”具备“线索检索工具”、“案情总结器”、“地点周边POI”等（可以挂接你现有 MCP 地图工具）

与当前项目的集成点（很关键）
- 工具体系
  - 直接复用 [src/pages/Tools.vue](cci:7://file:///e:/%E5%AD%A6%E4%B9%A0/%E9%A1%B9%E7%9B%AE/kit-agent/real-agent-font/src/pages/Tools.vue:0:0-0:0) 的“schema→表单”渲染方案，用于调试单个工具
  - 后端保持“工具不存在”的防御性（你已有修复），以及“AI回复→工具结果”的顺序（你已有修复）
- Playground 集成
  - 页面路径：`/playground/role-play-agent`
  - 保留左上返回按钮（统一规范）
  - 列表页 [Playground.vue](cci:7://file:///e:/%E5%AD%A6%E4%B9%A0/%E9%A1%B9%E7%9B%AE/kit-agent/real-agent-font/src/pages/Playground.vue:0:0-0:0) 中加一个条目（已实现图片左侧、卡片结构，可以继续加一个“角色扮演”项）

数据与缓存策略
- 缓存层
  - 角色冷缓存（Redis/本地缓存）
  - 热搜索缓存（按关键词与时间窗缓存，以减少频繁请求）
  - 向量召回结果缓存（TopK + 时间窗）
- 记忆策略
  - 短期记忆：会话摘要保存在关系库
  - 长期记忆：关键事实提炼后入向量库（触发条件：对话结束/用户显式保存/重要性分）

安全与合规
- 限流（用户级/会话级/工具级）
- 内容安全（提示词注入防护、敏感词过滤）
- 工具权限（角色白名单 + 参数校验 + 审计日志）
- 费用可控（按任务类型与长度限额）

分阶段落地计划
- 第1阶段（快速可用）
  - 前端：`Index.vue` + ChatPanel + AudioBar（先只做文本，语音可延后1-2天）
  - 后端：`/session/init`、`/send`，LLM流式；接入1-2个必需工具（搜索/RAG）
  - 角色：1个角色（如“柯南”），冷缓存 + 简单热搜索摘要
- 第2阶段（语音 + 工具编排）
  - 前端：录音/播放，TTS 播放；工具结果在对话中可视化展示
  - 后端：STT/TTS 接口、工具执行日志、RAG 接口化
- 第3阶段（Agent 化）
  - 引入“计划-执行-反思”的最小闭环；可配角色专属工具/知识库
  - 权限/审计/限流完善；可观测仪表盘
- 第4阶段（电话聊天/多角色/市场化）
  - 通话 API、角色商店（清单后端化）、应用模板库

落地文件与路径建议
- 前端
  - `src/pages/playground/role-play-agent/Index.vue`（主页面，含返回按钮）
  - `src/pages/playground/role-play-agent/components/ChatPanel.vue`
  - `src/pages/playground/role-play-agent/components/AudioBar.vue`
  - `src/stores/rolePlay.ts`
- 后端（包结构示例）
  - `com.example.role.core`（domain, repo, service）
  - `com.example.role.orchestrator`（对话编排、Prompt 组装、工具策略）
  - `com.example.role.llm`（DashScopeClient, Streaming）
  - `com.example.role.retrieval`（SearchClient, VectorStore）
  - `com.example.role.tools`（ToolRegistry, ToolExecutor, MCPAdapter）
  - `com.example.role.voice`（STTClient, TTSClient）
  - `com.example.infra`（auth, rate-limit, logging）

你拍个板
- 先按“第1阶段”实施：我在 `src/pages/playground/role-play-agent/` 里创建 `Index.vue` 和基础组件骨架，并在路由 `/playground/role-play-agent` 挂页，保留返回按钮；后端我给出接口契约与伪实现（可先对接 DashScope）。
- 角色你想先做哪个？要不要先做你提到的“柯南”风格角色，我顺手把搜索工具 + 案情梳理工具接上，做出“真 Agent”的感觉。


后端两种落地路径（二选一）

A. Java Spring Boot（与“Java+Vue”保持一致）
POST /api/voice/turn（multipart/form-data: sessionId, roleId, audio）
响应：SSE 流 text/event-stream（事件：asr_start/partial/final → llm_start/delta/done → tts_start/ready → done/error）
技术点：
SSE：WebFlux（Controller 返回 Flux<ServerSentEvent>）
ASR：阿里云 NLS 短语音识别（先可用假数据兜底）
LLM：DashScope（或先返回固定文本演示）
TTS：DashScope/阿里云语音合成（或先返回固定 mp3 演示）
如前端传 webm/opus，后台用 ffmpeg 转 wav pcm16k（先行可 Runtime.exec 调用 ffmpeg）
B. Python FastAPI（最快跑通，复用你现有 trae-agent 仓库）
在 trae-agent 新增 FastAPI 服务（routers/voice.py）
POST /api/voice/turn + EventSourceResponse（SSE）
ASR/LLM/TTS 初期可用假数据/固定 mp3，确认链路后再替换为阿里云/DashScope
优点：起步极快；与当前活跃的 Python 代码更贴近
缺点：与“Java+Vue”目标有偏差，但可作为临时演示/验收版本
接口事件协议（两种实现通用）

事件顺序
asr_start → asr_partial* → asr_final
llm_start → llm_delta* → llm_done
tts_start → tts_ready({ audio_url }) → done
error({ code, message })（任一步失败立即结束）
SSE 每条为：
event: 事件名（如 llm_delta）
data: JSON 字符串（如 {"text":"分片..." }）
前端对接我会做的事

语音模式 UI：替换文字聊天为大圆动效 + 底部按钮（录音/停止/挂断等），和你给的参考一致。
录音：首选 audio/webm;codecs=opus；若你选择仅支持 wav，我会切为 wav 16k 单声道。
上传：停止录音后调用 POST /api/voice/turn 并订阅 SSE。
UI 映射：asr_partial/asr_final 显示“你刚说了…”，llm_delta 实时字幕，tts_ready 自动播放音频，done 结束一轮。
需要你拍板的3件事

后端栈选 A（Java Spring Boot）还是 B（Python FastAPI 临时演示版）？
前端上传音频格式选哪种优先支持？
webm/opus（体积小，需后端 ffmpeg 转码）
wav pcm16k（实现最稳，体积大）
是否先用“假数据”打通链路（ASR/LLM/TTS 分别返回固定内容/固定 mp3），待页面确认后再接阿里云/DashScope？
你一确认，我就：

按你选的后端方案创建接口骨架和事件流（SSE）。
同步把前端语音模式 UI、录音上传、SSE 订阅、播放链路接好。
给你一个可点击“开始语音聊天→说话→听见回复”的最小可运行版本。