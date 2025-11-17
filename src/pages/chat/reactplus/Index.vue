<script setup lang="ts">
import {computed, h, nextTick, onMounted, onUnmounted, ref, watch} from 'vue'
import {InputMode, useModeSwitch} from '@/composables/useModeSwitch'
import {EventType, UIMessage} from '@/types/events'
import {useChatStore} from '@/stores/chatStore'
import ThinkingMessage from '@/components/messages/ThinkingMessage.vue'
import ToolApprovalMessage from '@/components/messages/ToolApprovalMessage.vue'
import {useSSE} from '@/composables/useSSE'
import {notification} from 'ant-design-vue'
import ErrorMessage from '@/components/messages/ErrorMessage.vue'
import {
  ArrowDownOutlined,
  CodeOutlined,
  FileTextOutlined,
  LoadingOutlined,
  PaperClipOutlined,
  RobotOutlined,
  SendOutlined,
  SettingOutlined,
  ThunderboltOutlined
} from '@ant-design/icons-vue'
import {Attachment} from '@/types/attachment'
import {TemplateItem} from '@/types/template'

// GSAP动画库
import {gsap} from 'gsap'
// 样式引入
import {NotificationType} from '@/types/notification'
import Terminal from '@/components/terminal/Terminal.vue'
import {useRoute, useRouter} from "vue-router";
import ToolMessage from "@/components/messages/ToolMessage.vue";
import {generateSimplePlan, generateTestPlan} from "@/utils/planTestData";
import PlanWidget from '@/components/PlanWidget.vue'
import CommonMessage from "@/components/messages/CommonMessage.vue";

import {messages as testMessages} from '@/stores/message'

const isDevelopment = (import.meta as any).env?.DEV ?? false

// 共享状态（会话/Agent 选择）
const chat = useChatStore()

console.log('Current mode:', chat)
const inputMessage = ref('')
const attachments = ref<Attachment[]>([])
const router = useRouter()
const route = useRoute()

const {
  currentMode,
  currentModeConfig,
  currentThemeClass,
  isGeekMode,
  isMultimodalMode,
  switchMode
} = useModeSwitch()

// 🖥️ 终端界面状态管理
const terminalRef = ref<InstanceType<typeof Terminal>>()
const terminalReady = ref(false)


// 工具审批状态管理
const pendingApprovals = ref<Map<string, any>>(new Map())
const approvalResults = ref<Map<string, any>>(new Map())

// UI状态管理
const isLoading = computed(() => taskStatus.value.is('running'))
const chatContent = ref<HTMLElement>()
const showScrollButton = ref(false)


// DOM引用
const appContainer = ref<HTMLElement>()
const messageElements = ref<HTMLElement[]>([])

// 发送可用状态
const canSend = computed(() => inputMessage.value.trim().length > 0 && !isLoading.value)

// 附件约束
const MAX_FILES = 4
const MAX_FILE_SIZE = 2 * 1024 * 1024 // 2MB
const MAX_TOTAL_SIZE = 20 * 1024 * 1024 // 20MB
const allowedExts = new Set([
  '.txt', '.md', '.markdown', '.java', '.kt', '.scala', '.py', '.go', '.js', '.mjs', '.cjs', '.ts', '.tsx',
  '.json', '.yml', '.yaml', '.xml', '.html', '.css', '.scss', '.less', '.vue', '.svelte', '.c', '.cpp', '.h', '.hpp',
  '.cs', '.rs', '.php', '.rb', '.swift', '.m', '.mm', '.sql', '.sh', '.bat', '.ps1', '.ini', '.conf', '.log', '.pdf'
])

const isAllowedFile = (f: File) => {
  if (f.type.startsWith('image/')) return true
  if (f.type === 'application/pdf' || f.type === 'text/plain' || f.type === 'application/json' || f.type === 'text/markdown') return true
  const dot = f.name.lastIndexOf('.')
  const ext = dot >= 0 ? f.name.slice(dot).toLowerCase() : ''
  return allowedExts.has(ext)
}

const bytes = (n: number) => Math.round(n / 1024)
const totalSize = () => attachments.value.reduce((s, a) => s + a.size, 0)

const pushFilesWithValidation = (files: File[]) => {
  // 数量限制
  if (attachments.value.length + files.length > MAX_FILES) {
    notification.error({message: '超出附件数量上限', description: `最多支持 ${MAX_FILES} 个附件`})
    return
  }
  // 校验每个文件
  let added: Attachment[] = []
  for (const f of files) {
    if (!isAllowedFile(f)) {
      notification.error({message: '不支持的文件类型', description: `${f.name}`})
      continue
    }
    if (f.size > MAX_FILE_SIZE) {
      notification.error({
        message: '文件过大',
        description: `${f.name} 大小 ${bytes(f.size)}KB，单个上限为 ${bytes(MAX_FILE_SIZE)}KB`
      })
      continue
    }
    const after = totalSize() + added.reduce((s, a) => s + a.size, 0) + f.size
    if (after > MAX_TOTAL_SIZE) {
      notification.error({message: '超过总大小限制', description: `当前合计将超过 ${bytes(MAX_TOTAL_SIZE)}KB`})
      continue
    }
    added.push(new Attachment(f.name, f.size, f))
  }
  if (added.length) attachments.value.push(...added)
}

// 滚动相关
const scrollToBottom = () => {
  if (chatContent.value) {
    chatContent.value.scrollTo({top: chatContent.value.scrollHeight, behavior: 'smooth'})
    return
  }
  // 兜底：如果未绑定到可滚动容器，则滚动窗口
  const doc = document.scrollingElement || document.documentElement
  window.scrollTo({top: doc.scrollHeight, behavior: 'smooth'})
}

const updateScrollButtonVisibility = () => {
  if (!chatContent.value) {
    // 兜底：检查窗口滚动
    const threshold = 80
    const distance = document.documentElement.scrollHeight - (window.scrollY + window.innerHeight)
    showScrollButton.value = distance > threshold
    return
  }
  const el = chatContent.value
  const threshold = 80
  const distance = el.scrollHeight - (el.scrollTop + el.clientHeight)
  showScrollButton.value = distance > threshold
}

// 增强的通知处理
const handleDoneNotice = (node: {
  text: string;
  startTime: Date;
  title: string;
  nodeId?: string,
  type: NotificationType
}) => {
  const key = `done-${node.startTime.getTime()}-${Math.random().toString(36).slice(2, 8)}`

  const onClick = () => locateByNode(node.nodeId)

  const notificationConfig = {
    message: node.text,
    key,
    duration: 5,
    onClick,
    style: {
      borderRadius: '8px',
      backdropFilter: 'blur(10px)',
    }
  }

  switch (node.type) {
    case NotificationType.SUCCESS:
      notification.success({...notificationConfig, message: `✅ ${node.text}`})
      break
    case NotificationType.ERROR:
      notification.error({...notificationConfig, message: `❌ ${node.text}`})
      break
    case NotificationType.WARNING:
      notification.warning({...notificationConfig, message: `⚠️ ${node.text}`})
      break
    case NotificationType.INFO:
      notification.info({...notificationConfig, message: `ℹ️ ${node.text}`})
      break
    default:
      notification.info({...notificationConfig, message: `🔔 ${node.text}`})
      break
  }
}

// 使用带自定义处理器的 useSSE
let {
  messages,
  nodeIndex,
  connectionStatus,
  taskStatus,
  progress,
  executeReAct,
  executeReActPlus,
  handleEvent,
  updateMessage
} = useSSE({
  onDoneNotice: handleDoneNotice,
  enableDefaultHandlers: true,  // 启用默认处理器
  handlers: {
    // 自定义工具审批事件处理器
    onToolApproval: (event: any, context: any) => {
      const approvalId = event.nodeId || `approval-${Date.now()}`

      // 存储审批请求
      pendingApprovals.value.set(approvalId, {
        toolName: event.data?.toolName,
        args: event.data?.args,
        callId: event.data?.callId,
        riskLevel: event.data?.riskLevel || 'medium',
        expectedResult: event.data?.expectedResult,
        startTime: new Date(),
        nodeId: approvalId
      })

      // 创建审批消息
      const approvalMessage: UIMessage = {
        nodeId: approvalId,
        sessionId: event.sessionId,
        type: EventType.TOOL_APPROVAL,
        sender: 'System',
        message: '需要您的审批才能执行工具',
        startTime: new Date(),
        meta: event.meta
      }

      messages.value.push(approvalMessage)
      context.scrollToBottom()

      // 返回 false 表示跳过默认处理器（我们已经自定义处理了）
      return false
    }
  }
})

// 工具审批处理函数
const handleToolApproved = (approvalId: string, result: any) => {
  approvalResults.value.set(approvalId, {status: 'approved', result, startTime: new Date()})
  pendingApprovals.value.delete(approvalId)

  notification.success({
    message: '工具执行已批准',
    description: '工具将继续执行，请等待结果...',
    duration: 3
  })
}

const handleToolRejected = (approvalId: string, reason: string) => {
  approvalResults.value.set(approvalId, {status: 'rejected', reason, startTime: new Date()})
  pendingApprovals.value.delete(approvalId)

  notification.warning({
    message: '工具执行已拒绝',
    description: reason,
    duration: 3
  })
}

const handleToolError = (approvalId: string, error: Error) => {
  approvalResults.value.set(approvalId, {status: 'error', error: error.message, startTime: new Date()})

  notification.error({
    message: '工具执行失败',
    description: error.message,
    duration: 5
  })
}


const handleToolTerminateRequested = (approvalId: string, reason: string) => {
  approvalResults.value.set(approvalId, {status: 'terminated', reason, startTime: new Date()})
  pendingApprovals.value.delete(approvalId)

  notification.warning({
    message: '对话已终止',
    description: reason,
    duration: 6
  })

  // 终止当前任务和连接
  if (taskStatus.value.is('running')) {
    taskStatus.value.set('completed')
  }
  connectionStatus.value.set('disconnected')

  // 添加系统消息通知用户对话已终止
  messages.value.push({
    type: EventType.SYSTEM,
    sender: 'System',
    message: `**对话已终止**

${reason}

您可以开始新的对话或选择其他会话继续。`,
    startTime: new Date(),
    nodeId: `terminate-${Date.now()}`
  })

  // 滚动到底部显示终止消息
  nextTick(() => {
    scrollToBottom()
  })
}


const handleErrorCopied = (success: boolean) => {
  if (success) {
    notification.success({
      message: '错误信息已复制',
      description: '错误详情已复制到剪贴板',
      duration: 2
    })
  } else {
    notification.error({
      message: '复制失败',
      description: '无法访问剪贴板，请手动选择文本复制',
      duration: 3
    })
  }
}

const locateByNode = (nodeId?: string) => {
  if (nodeId && chatContent.value) {
    const target = document.getElementById('msg-' + nodeId)
    if (target) {
      const container = chatContent.value
      const top = (target as HTMLElement).offsetTop
      container.scrollTo({top: Math.max(0, top - 12), behavior: 'smooth'})
      return
    }
  }
  scrollToBottom()
}

// 会话ID
const sessionId = chat.sessionId

// 发送消息
const sendMessage = async () => {
  if (!inputMessage.value.trim() || isLoading.value) return

  const userMessage: UIMessage = {
    type: EventType.USER,
    sender: '用户',
    message: inputMessage.value,
    startTime: new Date()
  }

  messages.value.push(userMessage)
  const currentMessage = inputMessage.value
  inputMessage.value = ''

  // 滚动到底部
  await nextTick()
  scrollToBottom()

  try {
    await executeReActPlus(currentMessage, sessionId)
  } catch (error) {
    console.error('发送消息失败:', error)
    messages.value.push({
      type: EventType.ERROR,
      sender: 'System',
      message: '发送失败: ' + (error as Error).message,
      startTime: new Date()
    })
    // 出错时手动设置任务状态
    taskStatus.value.set('error')
  } finally {
    // 清空已发送的附件
    attachments.value = []
  }
}


// 会话切换：保存旧会话消息并加载新会话消息
watch(() => chat.sessionId, (newId, oldId) => {
  if (oldId) {
    chat.setSessionMessages(oldId, messages.value)
  }
  const next = chat.getSessionMessages(newId)
  messages.value = next && next.length ? [...next] : []
  nodeIndex.value = {}
  // 清理审批状态
  pendingApprovals.value.clear()
  approvalResults.value.clear()
})

// 消息变化时，更新当前会话的消息，并触碰更新时间
watch(messages, (val, oldVal) => {
  chat.setSessionMessages(sessionId, val)
  chat.touchSession(sessionId)

  // 🐉 GSAP: 为新添加的消息应用入场动画
  if (val.length > oldVal.length) {
    nextTick(() => {
      const messageElements = document.querySelectorAll('.message-wrapper')
      const newMessage = messageElements[messageElements.length - 1] as HTMLElement
      if (newMessage) {
        animateMessageEntry(newMessage)
      }
    })
  }
}, {deep: true})

// 根据当前路由设置模式状态
const syncModeFromRoute = () => {
  const path = route.path
  const queryMode = route.query.mode as InputMode

  // 优先使用 URL 查询参数中的模式
  if (queryMode && ['geek', 'multimodal', 'command'].includes(queryMode)) {
    currentMode.value = queryMode
    return
  }

  // fixme 根据路径推断模式
  if (path === '/chat/geek') {
    currentMode.value = 'geek'
  } else if (path === '/chat') {
    currentMode.value = 'multimodal'
  } else {
    currentMode.value = 'multimodal' // 默认
  }
}

// 监听路由变化同步模式
watch(route, () => {
  syncModeFromRoute()
}, {immediate: true})

// 输入区工具栏
const fileInput = ref<HTMLInputElement | null>(null)
const handleUploadClick = () => fileInput.value?.click()
const onFileChange = (e: Event) => {
  const input = e.target as HTMLInputElement
  const files = input.files
  if (!files || files.length === 0) return
  pushFilesWithValidation(Array.from(files))
  input.value = ''
}

const insertCodeBlock = () => {
  const snippet = '\n```javascript\n// 请输入代码\nconsole.log("Hello ReAct+");\n```\n'
  inputMessage.value += snippet
}

const removeAttachment = (name: string) => {
  attachments.value = attachments.value.filter(a => a.name !== name)
}

const onDropFiles = (e: DragEvent) => {
  e.preventDefault()
  const files = e.dataTransfer?.files
  if (!files || files.length === 0) return
  pushFilesWithValidation(Array.from(files))
}

const onPressEnter = (e: KeyboardEvent) => {
  if (e.shiftKey) return
  e.preventDefault()
  sendMessage()
}

const onPaste = (e: ClipboardEvent) => {
  const items = e.clipboardData?.items
  if (!items) return
  const files: File[] = []
  for (const it of items as any) {
    if (it.kind === 'file') {
      const f = it.getAsFile()
      if (f) files.push(f)
    }
  }
  if (files.length) {
    pushFilesWithValidation(files)
  }
}

// ReAct+ 专属模板
const templates: TemplateItem[] = [
  new TemplateItem('智能分析任务', '请对以下问题进行深度分析，包括：\n1. 问题拆解和关键要素识别\n2. 多角度思考和风险评估\n3. 制定执行策略和行动计划\n\n问题描述：\n[请在此处描述您的问题]'),
  new TemplateItem('工具链执行', '请使用相关工具完成以下任务，需要：\n1. 自动选择最适合的工具组合\n2. 按步骤执行并展示中间结果\n3. 对结果进行验证和优化\n\n任务要求：\n[请详细描述任务需求]'),
  new TemplateItem('数据驱动决策', '基于以下数据和背景，帮助我做出最佳决策：\n\n背景信息：\n- 当前状况：\n- 目标期望：\n- 约束条件：\n- 风险考量：\n\n请提供详细的分析过程和建议方案'),
  new TemplateItem('目标导向规划', '请帮我制定实现以下目标的详细计划：\n\n目标：[具体目标]\n时间限制：[时间范围]\n资源情况：[可用资源]\n\n需要包括：里程碑设置、风险缓解、执行策略'),
]

const insertTemplate = (t: string) => {
  inputMessage.value = (inputMessage.value ? inputMessage.value + '\n\n' : '') + t
}

// 渲染Markdown
const resolvePlugin = (p: any) => {
  if (!p) return p
  const cand = (p as any).default ?? p
  if (typeof cand === 'function') return cand
  for (const key of Object.keys(p)) {
    const v = (p as any)[key]
    if (typeof v === 'function') return v
  }
  return cand
}

// 🐉 GSAP 动画系统 - 性能优化版
//  使用 GSAP Context 统一管理所有动画，确保正确清理
let gsapContext: gsap.Context | null = null

const initGSAPAnimations = () => {
  // 使用 GSAP Context 管理所有动画，避免内存泄漏
  if (gsapContext) {
    gsapContext.revert() // 清理旧的动画
  }

  gsapContext = gsap.context(() => {
    // ========== 1. 页面初始化动画 ==========
    if (appContainer.value) {
      // 页面淡入效果 - 只在初始化时执行一次
      gsap.fromTo(appContainer.value,
          {opacity: 0, y: 20},
          {
            opacity: 1,
            y: 0,
            duration: 0.6,
            ease: "power3.out",
            clearProps: "opacity,y" // 动画完成后清除属性
          }
      )
    }

    // ========== 2. 进度指示器 - 优化版 ==========
    const pulseRings = document.querySelectorAll('.pulse-ring')
    const pulseDots = document.querySelectorAll('.pulse-dot')

    // 如果元素存在才执行动画，避免无效的查询
    if (pulseRings.length > 0) {
      gsap.to(pulseRings, {
        scale: 1.01,
        rotation: 2,
        duration: 3,
        ease: "sine.inOut",
        repeat: -1,
        yoyo: true,
        stagger: 0.1 // 添加交错效果，避免所有元素同时动画
      })
    }

    if (pulseDots.length > 0) {
      gsap.to(pulseDots, {
        scale: 1.02,
        rotation: -1,
        duration: 3.5,
        ease: "sine.inOut",
        repeat: -1,
        yoyo: true,
        stagger: 0.15
      })
    }
  })
}

// ========== 3. 消息出现动画 - 青龙升腾 ==========
const animateMessageEntry = (element: HTMLElement) => {
  // 先清理可能存在的旧动画
  gsap.killTweensOf(element)

  gsap.fromTo(element,
      {
        opacity: 0.9,
        y: 20,
        scale: 0.98
      },
      {
        opacity: 1,
        y: 0,
        scale: 1,
        duration: 0.5,
        ease: "back.out(1.2)",
        clearProps: "all"
      }
  )
}


// ========== 10. 加载点动画 - 简化版 ==========
const setupLoadingDotsAnimation = () => {
  const loadingDots = document.querySelectorAll('.loading-dots span')

  // 简化的加载点动画
  loadingDots.forEach((dot, index) => {
    gsap.to(dot, {
      y: -4,
      duration: 0.6,
      ease: "power2.inOut",
      repeat: -1,
      yoyo: true,
      delay: index * 0.2
    })
  })
}


// ========== 🎨 高级 GSAP 动画系统 - 替代 CSS keyframes ==========

/**
 * 输入容器简化动画
 * 移除复杂的背景位置动画，保留基本的聚焦效果
 */
const setupInputContainerAdvancedAnimations = () => {
  const inputContainer = document.querySelector('.input-container')
  if (!inputContainer) return

  const textarea = inputContainer.querySelector('textarea')
  if (textarea) {
    let focusAnimation: gsap.core.Tween | null = null

    textarea.addEventListener('focus', () => {
      // 简化的聚焦效果
      focusAnimation = gsap.to(inputContainer, {
        borderColor: "rgba(107, 154, 152, 0.3)",
        y: -1,
        duration: 0.3,
        ease: 'power2.out'
      })
    })

    textarea.addEventListener('blur', () => {
      if (focusAnimation) {
        focusAnimation.kill()
      }

      gsap.to(inputContainer, {
        borderColor: "rgba(107, 154, 152, 0.15)",
        y: 0,
        duration: 0.3,
        ease: 'power2.out'
      })
    })
  }
}

/**
 * Textarea 简化动画
 * 移除复杂的光晕效果，保留基本交互反馈
 */
const setupTextareaAdvancedAnimations = () => {
  const textarea = document.querySelector('.input-area textarea')
  if (!textarea) return

  let focusAnimation: gsap.core.Tween | null = null

  textarea.addEventListener('focus', () => {
    // 简化的聚焦效果
    focusAnimation = gsap.to(textarea, {
      scale: 1.001,
      duration: 0.2,
      ease: 'power2.out'
    })
  })

  textarea.addEventListener('blur', () => {
    if (focusAnimation) {
      focusAnimation.kill()
    }

    gsap.to(textarea, {
      scale: 1,
      duration: 0.2,
      ease: 'power2.out'
    })
  })
}

/**
 * 发送按钮简化动画 - 添加防抖优化
 * 移除复杂的呼吸和流光效果，保持简洁的交互反馈
 */
const setupSendButtonAdvancedAnimations = () => {
  const sendButton = document.querySelector('.send-button')
  if (!sendButton) return

  let hoverAnimation: gsap.core.Tween | null = null
  let isAnimating = false

  sendButton.addEventListener('mouseenter', () => {
    // 防抖：如果正在动画中，不重复执行
    if (isAnimating) return

    isAnimating = true
    // 简化的发送按钮悬浮效果
    hoverAnimation = gsap.to(sendButton, {
      y: -1,
      duration: 0.2,
      ease: 'power2.out',
      onComplete: () => {
        isAnimating = false
      }
    })
  })

  sendButton.addEventListener('mouseleave', () => {
    if (hoverAnimation) hoverAnimation.kill()

    gsap.to(sendButton, {
      y: 0,
      duration: 0.2,
      ease: 'power2.out',
      onComplete: () => {
        isAnimating = false
      }
    })
  })
}

/**
 * 工具栏按钮简化动画 - 添加防抖优化
 * 移除复杂的涟漪创建，使用简单的缩放效果
 */
const setupToolbarAdvancedAnimations = () => {
  const toolbarButtons = document.querySelectorAll('.input-toolbar button')

  toolbarButtons.forEach(button => {
    let isAnimating = false

    button.addEventListener('mouseenter', () => {
      if (isAnimating) return

      isAnimating = true
      // 简化的悬浮效果
      gsap.to(button, {
        scale: 1.05,
        duration: 0.2,
        ease: 'power2.out',
        onComplete: () => {
          isAnimating = false
        }
      })
    })

    button.addEventListener('mouseleave', () => {
      gsap.to(button, {
        scale: 1,
        duration: 0.2,
        ease: 'power2.out',
        onComplete: () => {
          isAnimating = false
        }
      })
    })

    button.addEventListener('click', () => {
      // 简化的点击反馈 - 只在不是动画中时执行
      if (!isAnimating) {
        isAnimating = true
        gsap.to(button, {
          scale: 0.95,
          duration: 0.1,
          ease: 'power2.in',
          onComplete: () => {
            gsap.to(button, {
              scale: 1.05,
              duration: 0.1,
              ease: 'power2.out',
              onComplete: () => {
                isAnimating = false
              }
            })
          }
        })
      }
    })
  })
}

/**
 * 附件卡片简化动画
 * 移除复杂的光泽流动，使用简单的悬浮效果
 */
const setupAttachmentAdvancedAnimations = () => {
  const attachmentChips = document.querySelectorAll('.attachment-chip')

  attachmentChips.forEach(chip => {
    chip.addEventListener('mouseenter', () => {
      // 简化的悬浮效果
      gsap.to(chip, {
        y: -2,
        scale: 1.02,
        duration: 0.2,
        ease: 'power2.out'
      })
    })

    chip.addEventListener('mouseleave', () => {
      gsap.to(chip, {
        y: 0,
        scale: 1,
        duration: 0.2,
        ease: 'power2.out'
      })
    })
  })
}


const testInitPlan = () => {
  const plan = generateTestPlan()
  chat.setSessionPlan(sessionId, plan)
  chat.setPlanWidgetMode('ball')
  notification.success({
    message: '测试计划已创建',
    description: '已生成测试计划数据，状态球已显示'
  })
}

const testSimplePlan = () => {
  const plan = generateSimplePlan()
  chat.setSessionPlan(sessionId, plan)
  chat.setPlanWidgetMode('ball')
  notification.success({
    message: '简单计划已创建',
    description: '已生成简单测试计划数据，状态球已显示'
  })
}


// 组件挂载
onMounted(() => {
  // 加载当前会话已存在的消息
  const existing = chat.getSessionMessages(sessionId)
  if (existing && existing.length > 0) {
    messages.value = [...existing]
  } else {
    // 全面的测试数据 - 覆盖所有渲染情况
    messages.value = testMessages
  }

  // 🐉 初始化 GSAP 动画系统 - 简化版
  nextTick(() => {
    // 1. 页面初始化 + 进度指示器
    initGSAPAnimations()


    // 3. 输入相关动画（合并基础和高级动画）
    setupInputContainerAdvancedAnimations()
    setupTextareaAdvancedAnimations()

    // 4. 发送按钮动画（只使用高级版本，避免重复）
    setupSendButtonAdvancedAnimations()

    // 5. 工具栏和附件动画
    setupToolbarAdvancedAnimations()
    setupAttachmentAdvancedAnimations()


    // 7. 加载点动画
    setupLoadingDotsAnimation()

    // 监听滚动，控制下滑按钮显隐
    chatContent.value?.addEventListener('scroll', updateScrollButtonVisibility)
    // 同时监听窗口滚动作为兜底
    window.addEventListener('scroll', updateScrollButtonVisibility)
    updateScrollButtonVisibility()
  })
})

onUnmounted(() => {
  chatContent.value?.removeEventListener('scroll', updateScrollButtonVisibility)
  window.removeEventListener('scroll', updateScrollButtonVisibility)

  // 清理所有 GSAP 动画，避免内存泄漏
  if (gsapContext) {
    gsapContext.revert()
    gsapContext = null
  }

  // 清理全局 GSAP 动画
  gsap.killTweensOf('*')
})
</script>

<template>
  <div ref="appContainer" :class="['react-plus-app', currentThemeClass]">
    <!-- Plan 状态侧边栏 - 仅在 reactPlus 页面显示 -->
    <PlanWidget/>
    <!-- 🖥️ 极客模式：终端界面 -->
    <template v-if="isGeekMode">

      <div class="geek-mode-wrapper">
        <!-- 快速模式切换栏 -->
        <div class="geek-mode-header">
          <div class="mode-info">
            <span class="mode-label">🤖 极客模式</span>
            <span class="session-info">Session: {{ sessionId }}</span>
          </div>
          <div class="mode-actions">
            <button
                class="exit-geek-btn"
                @click="() => switchMode('multimodal')"
                title="退出极客模式"
            >
              退出
            </button>
          </div>
        </div>

        <Terminal
            ref="terminalRef"
            :session-id="sessionId"
            class="geek-terminal-interface"
        />
      </div>
    </template>

    <!-- 正常界面 -->
    <template v-else>
      <!-- 主要内容区域 -->
      <div class="main-content flex flex-col h-full min-h-0">

        <!-- 对话区域 -->
        <div class="chat-container flex-1 min-h-0 overflow-y-auto scroll-smooth" ref="chatContent">
          <div
              v-for="(message, index) in messages"
              :key="index"
              :id="message.nodeId ? 'msg-' + message.nodeId : undefined"
              class="message-wrapper"
          >
            <!-- Thinking 消息 - 使用折叠组件 -->
            <ThinkingMessage
                v-if="message.type === EventType.THINKING"
                :content="message.message"
                :sender="message.sender"
                :startTime="message.startTime"
                :is-thinking="!message.endTime"
                class="message-item mb-2.5"
            />
            <!-- 工具调用消息 -->
            <ToolMessage v-else-if="message.type === EventType.TOOL"
                         :message="message"
                         class="message-item mb-2.5"
            ></ToolMessage>

            <!-- 工具审批消息 -->
            <ToolApprovalMessage
                v-else-if="message.type === EventType.TOOL_APPROVAL"
                :message="message"
                @approved="handleToolApproved(message.nodeId!, $event)"
                @rejected="handleToolRejected(message.nodeId!, $event)"
                @error="handleToolError(message.nodeId!, $event)"
                @terminateRequested="handleToolTerminateRequested(message.nodeId!, $event)"
                class="message-item mb-2.5"
            />
            <!-- 错误消息 - 使用专用组件 -->
            <ErrorMessage
                v-else-if="message.type === EventType.ERROR"
                :message="message"
                @copied="handleErrorCopied"
                class="message-item mb-2.5"
            />
            <!-- 普通消息 -->
            <CommonMessage v-else :message="message" class="message-item"/>
          </div>

          <!-- 加载状态 -->
          <div v-if="isLoading" class="loading-indicator">
            <div class="loading-dots">
              <span></span>
              <span></span>
              <span></span>
            </div>
            <span class="loading-text">
                {{ progress?.text || '任务执行...' }}
              </span>
          </div>
        </div>

        <!-- 滚动到底部按钮 -->
        <Transition name="fade">
          <div v-show="showScrollButton" class="scroll-to-bottom" @click="scrollToBottom">
            <a-button type="primary" shape="circle" :icon="h(ArrowDownOutlined)"/>
          </div>
        </Transition>
      </div>

      <div
          class=" w-[830px] sticky bottom-1.5 z-30 px-2 md:px-0  mx-auto
            input-container overflow-hidden rounded-2xl border border-primary-50 backdrop-blur-xl
            shadow-lg transition-colors"
          @dragover.prevent
          @drop="onDropFiles"
      >

        <!-- 📎 附件预览区域 - 仅在有附件时显示 -->

        <div v-if="attachments.length" class="mode-selector flex items-center gap-3 px-5 py-3 flex gap-2 px-4 py-2">
          <div class="flex items-center gap-2">
            <div v-for="attachment in attachments" :key="attachment.name"
                 class="inline-flex items-center gap-1.5 px-2 py-1 bg-white border border-blue-200 rounded-md text-xs shadow-sm">
              <FileTextOutlined class="text-blue-500 text-xs"/>
              <span class="text-blue-700 font-medium truncate max-w-[100px]">{{ attachment.name }}</span>
              <span class="text-blue-400">{{ bytes(attachment.size) }}KB</span>
              <button
                  @click="removeAttachment(attachment.name)"
                  class="text-blue-400 hover:text-red-500 ml-1 font-bold text-sm leading-none"
              >×
              </button>
            </div>
          </div>
        </div>


        <!-- 输入区域（textarea + 发送按钮 + 工具栏） -->
        <div class="input-area relative flex flex-col justify-between px-4 pb-2 bg-transparent w-full">
          <a-textarea
              v-model:value="inputMessage"
              :maxlength="4000"
              :auto-size="{ minRows: 1, maxRows: 2 }"
              placeholder="请输入您的问题..."
              :disabled="isLoading"
              :bordered="false"
              class="w-full text-slate-800 text-sm leading-6 font-normal bg-transparent outline-none focus:outline-none"
              @pressEnter="onPressEnter"
              @paste="onPaste"
          />
          <button
              :disabled="!canSend"
              @click="sendMessage"
              class="send-button absolute right-4 top-1/2 w-10 h-10 -translate-y-1/2 rounded-[50%] font-semibold"
          >
            <SendOutlined class="m-auto text-lg" v-if="!isLoading"/>
            <LoadingOutlined class="m-auto text-lg" v-else/>
          </button>

          <!-- 工具按钮组 -->
          <div class="input-toolbar mt-1 flex items-center gap-1 text-slate-500 text-sm">
            <a-button type="text" size="large" @click="handleUploadClick" :icon="h(PaperClipOutlined)"/>
            <input ref="fileInput" type="file" class="hidden" multiple @change="onFileChange"/>
            <a-button type="text" size="large" @click="insertCodeBlock" :icon="h(CodeOutlined)"/>

            <!-- 模式切换与功能设置下拉菜单 -->
            <a-dropdown placement="topLeft" trigger="click">
              <a-button type="text" size="large" :icon="h(SettingOutlined)" class="hover:text-primary-500"/>
              <template #overlay>
                <a-menu class="min-w-[200px]">
                  <!-- 模式切换组 -->
                  <a-menu-item-group title="模式切换">
                    <a-menu-item
                        key="geek"
                        @click="() => switchMode('geek')"
                        :class="{ 'ant-menu-item-selected': currentMode === 'geek' }"
                    >
                      <template #icon>
                        <RobotOutlined/>
                      </template>
                      极客模式
                    </a-menu-item>
                    <a-menu-item
                        key="multimodal"
                        @click="() => switchMode('multimodal')"
                        :class="{ 'ant-menu-item-selected': currentMode === 'multimodal' }"
                    >
                      <template #icon>
                        <ThunderboltOutlined/>
                      </template>
                      多模态模式
                    </a-menu-item>
                  </a-menu-item-group>

                  <a-menu-divider/>

                  <!-- 计划功能 -->
                  <a-menu-item-group title="计划功能">
                    <a-menu-item
                        key="plan-toggle"
                        @click="chat.togglePlanVisibility"
                        :disabled="!chat.getCurrentPlan()"
                    >
                      <template #icon>📋</template>
                      {{ chat.planVisible ? '隐藏计划' : '显示计划' }}
                    </a-menu-item>
                  </a-menu-item-group>

                  <!-- 开发模式测试功能 -->
                  <template v-if="isDevelopment">
                    <a-menu-divider/>
                    <a-menu-item-group title="开发测试">
                      <a-menu-item key="test-plan" @click="testInitPlan">
                        <template #icon>🧪</template>
                        测试计划
                      </a-menu-item>
                      <a-menu-item key="simple-plan" @click="testSimplePlan">
                        <template #icon>📝</template>
                        简单计划
                      </a-menu-item>
                    </a-menu-item-group>
                  </template>
                </a-menu>
              </template>
            </a-dropdown>
          </div>
        </div>
      </div>


      <!-- 隐藏文件输入 -->
      <input
          type="file"
          ref="fileInput"
          style="display: none"
          multiple
          accept=".txt,.md,.markdown,.java,.kt,.scala,.py,.go,.js,.mjs,.cjs,.ts,.tsx,.json,.yml,.yaml,.xml,.html,.css,.scss,.less,.vue,.svelte,.c,.cpp,.h,.hpp,.cs,.rs,.php,.rb,.swift,.m,.mm,.sql,.sh,.bat,.ps1,.ini,.conf,.log,.pdf,image/*"
          @change="onFileChange"
      />

    </template>

  </div>
</template>

<style scoped lang="scss">
@use './Index.scss' as *;

</style>
