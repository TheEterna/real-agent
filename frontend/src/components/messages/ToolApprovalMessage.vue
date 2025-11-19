<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { gsap } from 'gsap'
import { notification } from 'ant-design-vue'
import ParameterRenderer from './ParameterRenderer.vue'
import { CaretUpFilled } from '@ant-design/icons-vue';
import {UIMessage} from "@/types/events.js";
const props = defineProps<{
  message: UIMessage
}>()

const emit = defineEmits<{
  approved: [result: any]
  rejected: [reason: string]
  error: [error: Error]
  retryRequested: [params: any]
  terminateRequested: [reason: string]
}>()


interface ToolSpec {
  name?: string
  description?: string
  category?: string
  inputSchema?: string
}

interface ToolApproval {
  toolName?: string
  args?: Record<string, any>
  callId?: string
  startTime?: Date
  toolSchema?: string | ToolSpec  // 后端传来的 schema
}

type ApprovalAction = 'approve' | 'reject'


// DOM 引用
const cardRef = ref<HTMLElement>()
const actionZoneRef = ref<HTMLElement>()

// 状态管理
const executing = ref(false)
const completed = ref(false)
const executionSuccess = ref(false)
const executionError = ref(false)
const currentAction = ref<ApprovalAction | null>(null)

// UI 状态
const showDetails = ref(false)
const showRawParams = ref(false)
const showReasonModal = ref(false)
const rejectionReason = ref('')
const terminateAfterReject = ref(false)


const approval = ref<ToolApproval>({
  toolName: props.message.data?.toolName,
  args: props.message.data?.args,
  callId: props.message.data?.toolCallId,
  startTime: props.message.startTime,
  toolSchema: props.message.meta?.toolSchema as ToolSpec
})

// 解析 toolSchema
const toolSpec = computed<ToolSpec | null>(() => {
  try {
    const schema = approval.value?.toolSchema
    if (!schema) return null

    // 如果已经是对象
    if (typeof schema === 'object') {
      return schema as ToolSpec
    }

    // 如果是字符串，尝试解析
    if (typeof schema === 'string') {
      return JSON.parse(schema) as ToolSpec
    }

    return null
  } catch (error) {
    console.warn('Failed to parse toolSchema:', error)
    return null
  }
})

// 解析 inputSchema 中的参数描述 - 支持嵌套结构
const paramDescriptions = computed<Record<string, any>>(() => {
  try {
    const inputSchema = toolSpec.value?.inputSchema
    if (!inputSchema) return {}

    const schema = typeof inputSchema === 'string' ? JSON.parse(inputSchema) : inputSchema

    // 递归解析嵌套的 Schema 结构
    const parseProperties = (properties: any, path: string = ''): Record<string, any> => {
      const descriptions: Record<string, any> = {}

      for (const [key, value] of Object.entries(properties)) {
        const prop = value as any
        const currentPath = path ? `${path}.${key}` : key

        if (prop?.description) {
          descriptions[currentPath] = prop.description
        }

        // 如果有嵌套的 properties，递归解析
        if (prop?.properties) {
          Object.assign(descriptions, parseProperties(prop.properties, currentPath))
          // 保存整个属性结构以便子组件使用
          descriptions[key] = {
            description: prop.description,
            properties: parseProperties(prop.properties),
            required: prop.required || [] // 添加required字段信息
          }
        }

        // 处理数组类型中的 items
        if (prop?.items?.properties) {
          Object.assign(descriptions, parseProperties(prop.items.properties, `${currentPath}[]`))
        }
      }

      return descriptions
    }

    const properties = schema?.properties || {}
    return parseProperties(properties)
  } catch (error) {
    console.warn('Failed to parse inputSchema:', error)
    return {}
  }
})

// 获取根级别的必填字段
const getRootRequiredFields = (): string[] => {
  try {
    const inputSchema = toolSpec.value?.inputSchema
    if (!inputSchema) return []

    const schema = typeof inputSchema === 'string' ? JSON.parse(inputSchema) : inputSchema
    return Array.isArray(schema?.required) ? schema.required : []
  } catch (error) {
    console.warn('Failed to parse required fields:', error)
    return []
  }
}

// 计算属性
const statusClass = computed(() => {
  if (completed.value) return 'bg-primary-100 text-primary-700'
  if (executing.value) return 'bg-yellow-100 text-yellow-700'
  if (executionSuccess.value) return 'bg-green-100 text-green-700'
  if (executionError.value) return 'bg-red-100 text-red-700'
  return 'bg-gray-100 text-gray-600'
})

const statusDotClass = computed(() => {
  if (completed.value) return 'bg-primary-500'
  if (executing.value) return 'bg-yellow-500 animate-pulse'
  if (executionSuccess.value) return 'bg-green-500'
  if (executionError.value) return 'bg-red-500'
  return 'bg-gray-400'
})

const statusText = computed(() => {
  if (completed.value) return '已完成'
  if (executing.value) return '执行中'
  if (executionSuccess.value) return '执行成功'
  if (executionError.value) return '执行失败'
  return '等待审批'
})

// 工具函数
const formatJSON = (obj: any) => {
  try {
    return JSON.stringify(obj, null, 2)
  } catch {
    return String(obj)
  }
}

const copyToClipboard = async (text: string) => {
  try {
    await navigator.clipboard.writeText(text)
    notification.success({
      message: '已复制到剪贴板',
      duration: 2
    })
  } catch {
    notification.error({
      message: '复制失败',
      duration: 2
    })
  }
}
// 交互函数
const toggleDetails = () => {
  showDetails.value = !showDetails.value
}

const toggleParamsView = () => {
  showRawParams.value = !showRawParams.value
}

// 执行函数
const executeAction = async (action: ApprovalAction, handler: () => Promise<void>) => {
  if (executing.value || completed.value) return

  executing.value = true
  currentAction.value = action
  executionSuccess.value = false
  executionError.value = false

  try {
    await handler()
  } catch (error: any) {
    executionError.value = true
    emit('error', error)
  } finally {
    executing.value = false
    currentAction.value = null
  }
}

// 事件处理
const onApproveExecution = async () => {
  await executeAction('approve', async () => {
    // todo 模拟执行过程, 后续这里会变成真正的工具接口调用，
    await new Promise(resolve => setTimeout(resolve, 2000))

    executionSuccess.value = true
    completed.value = true

    emit('approved', {
      success: true,
      action: 'approve'
    })
  })
}

const onRejectWithReason = () => {
  showReasonModal.value = true
}

const submitRejection = async () => {
  if (!rejectionReason.value.trim()) {
    notification.warning({
      message: '请输入拒绝理由',
      duration: 2
    })
    return
  }

  await executeAction('reject', async () => {

    completed.value = true

    emit('rejected', rejectionReason.value)

    // 如果选择了终止对话
    if (terminateAfterReject.value) {
      emit('terminateRequested', `用户拒绝工具执行: ${rejectionReason.value}`)
    }

    showReasonModal.value = false
  })
}

// 操作区域退出动画
const onActionZoneBeforeLeave = (el: Element) => {
  const element = el as HTMLElement
  // 保存原始高度，避免高度塌陷
  element.style.height = `${element.offsetHeight}px`
}

const onActionZoneLeave = (el: Element, done: () => void) => {
  const element = el as HTMLElement

  // 使用 GSAP 创建自然的退出动画
  gsap.to(element, {
    height: 0,
    opacity: 0,
    paddingTop: 0,
    paddingBottom: 0,
    marginTop: 0,
    marginBottom: 0,
    duration: 0.4,
    ease: "power2.inOut",
    onComplete: () => {
      done()
    }
  })

  // 同时让按钮稍微缩小并淡出
  const buttons = element.querySelectorAll('button')
  gsap.to(buttons, {
    scale: 0.95,
    opacity: 0,
    duration: 0.3,
    ease: "power2.in"
  })
}

const onActionZoneAfterLeave = (el: Element) => {
  const element = el as HTMLElement
  // 清理内联样式
  element.style.height = ''
}

// 生命周期
onMounted(() => {
  // 入场动画
  setTimeout(() => {
    if (cardRef.value) {
      gsap.fromTo(cardRef.value,
        {
          opacity: 0,
          y: 40,
          scale: 0.95
        },
        {
          opacity: 1,
          y: 0,
          scale: 1,
          duration: 0.6,
          ease: "back.out(1.3)"
        }
      )
    }
  }, 100)
})
</script>


<template>
  <div
      ref="cardRef"
      class="tool-approval-container group"
  >
    <!-- 审批卡片 -->
    <div class="approval-card bg-gradient-to-br from-primary-50 via-primary-100/50 to-primary-50 border border-primary-200 rounded-3xl overflow-hidden transition-all duration-200 hover:shadow-lg">

      <!-- 头部区域 -->
      <div class="approval-header flex items-center gap-4 px-4 py-3 bg-primary-50/50 border-b border-primary-200">
        <!-- 工具图标 -->
        <div class="tool-icon-container flex-shrink-0 w-12 h-12 rounded-2xl flex items-center justify-center shadow-3d transform-gpu transition-all duration-300 hover:scale-105 hover:rotate-1">
          <!-- 后端会传回工具icon -->
          <!--          <span>{{ getToolIcon(toolSpec?.name) }}</span>-->
          <svg width="20" height="20" viewBox="0 0 20 20" fill="currentColor" xmlns="http://www.w3.org/2000/svg" class="text-black drop-shadow-sm"><path d="M11.2475 18.25C10.6975 18.25 10.175 18.1455 9.67999 17.9365C9.18499 17.7275 8.74499 17.436 8.35999 17.062C7.94199 17.205 7.50749 17.2765 7.05649 17.2765C6.31949 17.2765 5.63749 17.095 5.01049 16.732C4.38349 16.369 3.87749 15.874 3.49249 15.247C3.11849 14.62 2.93149 13.9215 2.93149 13.1515C2.93149 12.8325 2.97549 12.486 3.06349 12.112C2.62349 11.705 2.28249 11.2375 2.04049 10.7095C1.79849 10.1705 1.67749 9.6095 1.67749 9.0265C1.67749 8.4325 1.80399 7.8605 2.05699 7.3105C2.30999 6.7605 2.66199 6.2875 3.11299 5.8915C3.57499 5.4845 4.10849 5.204 4.71349 5.05C4.83449 4.423 5.08749 3.862 5.47249 3.367C5.86849 2.861 6.35249 2.465 6.92449 2.179C7.49649 1.893 8.10699 1.75 8.75599 1.75C9.30599 1.75 9.82849 1.8545 10.3235 2.0635C10.8185 2.2725 11.2585 2.564 11.6435 2.938C12.0615 2.795 12.496 2.7235 12.947 2.7235C13.684 2.7235 14.366 2.905 14.993 3.268C15.62 3.631 16.1205 4.126 16.4945 4.753C16.8795 5.38 17.072 6.0785 17.072 6.8485C17.072 7.1675 17.028 7.514 16.94 7.888C17.38 8.295 17.721 8.768 17.963 9.307C18.205 9.835 18.326 10.3905 18.326 10.9735C18.326 11.5675 18.1995 12.1395 17.9465 12.6895C17.6935 13.2395 17.336 13.718 16.874 14.125C16.423 14.521 15.895 14.796 15.29 14.95C15.169 15.577 14.9105 16.138 14.5145 16.633C14.1295 17.139 13.651 17.535 13.079 17.821C12.507 18.107 11.8965 18.25 11.2475 18.25ZM7.17199 16.1875C7.72199 16.1875 8.20049 16.072 8.60749 15.841L11.7095 14.059C11.8195 13.982 11.8745 13.8775 11.8745 13.7455V12.3265L7.88149 14.62C7.63949 14.763 7.39749 14.763 7.15549 14.62L4.03699 12.8215C4.03699 12.8545 4.03149 12.893 4.02049 12.937C4.02049 12.981 4.02049 13.047 4.02049 13.135C4.02049 13.696 4.15249 14.213 4.41649 14.686C4.69149 15.148 5.07099 15.511 5.55499 15.775C6.03899 16.05 6.57799 16.1875 7.17199 16.1875ZM7.33699 13.498C7.40299 13.531 7.46349 13.5475 7.51849 13.5475C7.57349 13.5475 7.62849 13.531 7.68349 13.498L8.92099 12.7885L4.94449 10.4785C4.70249 10.3355 4.58149 10.121 4.58149 9.835V6.2545C4.03149 6.4965 3.59149 6.8705 3.26149 7.3765C2.93149 7.8715 2.76649 8.4215 2.76649 9.0265C2.76649 9.5655 2.90399 10.0825 3.17899 10.5775C3.45399 11.0725 3.81149 11.4465 4.25149 11.6995L7.33699 13.498ZM11.2475 17.161C11.8305 17.161 12.3585 17.029 12.8315 16.765C13.3045 16.501 13.6785 16.138 13.9535 15.676C14.2285 15.214 14.366 14.697 14.366 14.125V10.561C14.366 10.429 14.311 10.33 14.201 10.264L12.947 9.538V14.1415C12.947 14.4275 12.826 14.642 12.584 14.785L9.46549 16.5835C10.0045 16.9685 10.5985 17.161 11.2475 17.161ZM11.8745 11.122V8.878L10.01 7.822L8.12899 8.878V11.122L10.01 12.178L11.8745 11.122ZM7.05649 5.8585C7.05649 5.5725 7.17749 5.358 7.41949 5.215L10.538 3.4165C9.99899 3.0315 9.40499 2.839 8.75599 2.839C8.17299 2.839 7.64499 2.971 7.17199 3.235C6.69899 3.499 6.32499 3.862 6.04999 4.324C5.78599 4.786 5.65399 5.303 5.65399 5.875V9.4225C5.65399 9.5545 5.70899 9.659 5.81899 9.736L7.05649 10.462V5.8585ZM15.4385 13.7455C15.9885 13.5035 16.423 13.1295 16.742 12.6235C17.072 12.1175 17.237 11.5675 17.237 10.9735C17.237 10.4345 17.0995 9.9175 16.8245 9.4225C16.5495 8.9275 16.192 8.5535 15.752 8.3005L12.6665 6.5185C12.6005 6.4745 12.54 6.458 12.485 6.469C12.43 6.469 12.375 6.4855 12.32 6.5185L11.0825 7.2115L15.0755 9.538C15.1965 9.604 15.2845 9.692 15.3395 9.802C15.4055 9.901 15.4385 10.022 15.4385 10.165V13.7455ZM12.122 5.3635C12.364 5.2095 12.606 5.2095 12.848 5.3635L15.983 7.195C15.983 7.118 15.983 7.019 15.983 6.898C15.983 6.37 15.851 5.8695 15.587 5.3965C15.334 4.9125 14.9655 4.5275 14.4815 4.2415C14.0085 3.9555 13.4585 3.8125 12.8315 3.8125C12.2815 3.8125 11.803 3.928 11.396 4.159L8.29399 5.941C8.18399 6.018 8.12899 6.1225 8.12899 6.2545V7.6735L12.122 5.3635Z"></path></svg>
        </div>

        <!-- 标题内容 -->
        <div class="header-content flex-1 min-w-0 tool-meta flex flex-col gap-1.5">
          <div class="tool-name-badge inline-flex items-center gap-1.5 bg-primary-100 border border-primary-300 rounded-lg px-2 py-1 self-start">
            <span class="text-xs font-semibold text-primary-700">{{ toolSpec?.name || '未知工具' }}</span>
          </div>
          <div v-if="toolSpec?.description" class="text-xs text-primary-600 leading-relaxed">{{ toolSpec.description }}</div>
        </div>

        <!-- 状态指示器 -->
        <div class="status-panel flex items-center gap-2 px-3 py-1.5 rounded-xl backdrop-blur-sm transition-colors" :class="statusClass">
          <div class="status-dot w-2 h-2 rounded-full" :class="statusDotClass"></div>
          <span class="text-xs font-semibold">{{ statusText }}</span>
        </div>
      </div>

      <!-- 详情折叠触发器 -->
      <button
          @click="toggleDetails"
          class="details-trigger w-full flex items-center justify-between px-4 py-3 bg-primary-50/30 hover:bg-primary-100/50 transition-colors duration-200"
      >
        <span class="text-sm font-medium text-primary-700">{{ showDetails ? '收起执行详情' : '查看执行详情' }}</span>

        <CaretUpFilled class="w-4 h-4 transition-transform duration-200 text-primary-400"
                       :class="{ 'rotate-180': showDetails }"/>
      </button>

      <!-- 详情内容区 -->
      <Transition
          enter-active-class="transition-all duration-300 ease-out"
          leave-active-class="transition-all duration-200 ease-in"
          enter-from-class="opacity-0 max-h-0"
          enter-to-class="opacity-100 max-h-screen"
          leave-from-class="opacity-100 max-h-screen"
          leave-to-class="opacity-0 max-h-0"
      >
        <div v-show="showDetails" class="details-content overflow-hidden">
          <div class="px-4 py-4 space-y-4">

            <!-- 执行参数区域 -->
            <div v-if="approval?.args" class="param-section">
              <div class="section-header flex items-center justify-between">
                <p class="text-lg font-bold text-primary-700 flex items-center gap-1.5">
                  <b>执行参数</b>
                </p>
                <button
                    @click="toggleParamsView"
                    class="text-xs px-2 py-1 rounded-lg border transition-colors"
                    :class="showRawParams
                    ? 'bg-primary-100 border-primary-300 text-primary-700'
                    : 'bg-white border-primary-200 text-primary-600 hover:bg-primary-50'"
                >
                  {{ showRawParams ? '格式化' : '原始' }}
                </button>
              </div>

              <!-- 格式化参数视图 - 嵌套渲染 -->
              <div v-if="!showRawParams" class="params-formatted max-h-96 overflow-y-auto space-y-2 pr-2">
                <div v-for="(value, key) in approval.args" :key="key">
                  <ParameterRenderer
                      :param-key="key"
                      :param-value="value"
                      :level="0"
                      :descriptions="paramDescriptions"
                      :required-fields="getRootRequiredFields()"
                      :max-depth="2"
                  />
                </div>
              </div>

              <!-- 原始 JSON 视图 -->
              <div v-else class="params-raw">
                <div class="code-container relative bg-primary-900 rounded-xl p-4 border border-primary-700 max-h-96 overflow-auto">
                  <pre class="text-xs text-primary-100 font-mono"><code>{{ formatJSON(approval.args) }}</code></pre>
                  <button
                      @click="copyToClipboard(formatJSON(approval.args))"
                      class="absolute top-2 right-2 text-xs px-2 py-1 rounded bg-primary-800 hover:bg-primary-700 text-primary-200 transition-colors"
                  >
                    复制
                  </button>
                </div>
              </div>
            </div>

          </div>
        </div>
      </Transition>

      <!-- 操作按钮区域 -->
      <Transition
          @before-leave="onActionZoneBeforeLeave"
          @leave="onActionZoneLeave"
          @after-leave="onActionZoneAfterLeave"
      >
        <div v-if="!completed" ref="actionZoneRef" class="action-zone px-3.5 py-3 bg-primary-50/30 border-t border-primary-200">
          <div class="action-row flex gap-2.5">
            <!-- 立即执行 -->
            <button
                @click="onApproveExecution"
                :disabled="executing || completed"
                class="flex-1 cursor-pointer flex items-center justify-center gap-2 bg-primary-500 hover:bg-primary-600 active:bg-primary-700 px-3.5 py-2.5 rounded-xl transition-all duration-200 ease-in-out transform hover:scale-[1.02] active:scale-[0.98] disabled:opacity-50 disabled:cursor-not-allowed"
            >
              <div v-if="currentAction === 'approve'" class="w-4 h-4 border-2 border-white border-t-transparent rounded-full animate-spin"></div>
              <span class="text-sm font-medium text-white">立即执行</span>
            </button>

            <!-- 拒绝执行 -->
            <button
                @click="onRejectWithReason"
                :disabled="executing || completed"
                class="flex-1 cursor-pointer flex items-center justify-center gap-2 bg-white hover:bg-red-50 active:bg-red-100 px-3.5 py-2.5 rounded-xl border border-red-300 transition-all duration-200 ease-in-out transform hover:scale-[1.02] active:scale-[0.98] disabled:opacity-50 disabled:cursor-not-allowed"
            >
              <div v-if="currentAction === 'reject'" class="w-4 h-4 border-2 border-red-600 border-t-transparent rounded-full animate-spin"></div>
              <span class="text-sm font-medium text-red-600">拒绝执行</span>
            </button>
          </div>
        </div>
      </Transition>


    </div>

    <!-- 拒绝理由 Modal -->
    <a-modal
        v-model:open="showReasonModal"
        title="请说明拒绝理由"
        ok-text="确认拒绝"
        cancel-text="取消"
        :ok-button-props="{ disabled: !rejectionReason.trim(), loading: currentAction === 'reject', danger: true }"
        :centered="true"
        @ok="submitRejection"
    >
      <a-textarea
          v-model:value="rejectionReason"
          :auto-size="{ minRows: 3, maxRows: 6 }"
          placeholder="请详细说明拒绝执行该工具的具体原因，这将帮助AI更好地理解您的需求..."
          class="mt-4"
      />

      <div class="mt-4">
        <a-checkbox v-model:checked="terminateAfterReject">
          <span class="text-sm">拒绝后终止对话</span>
        </a-checkbox>
        <p class="text-xs text-gray-500 mt-1 ml-6">选中后将终止当前 Agent 对话流程</p>
      </div>
    </a-modal>
  </div>
</template>

<style scoped lang="scss">
.tool-approval-container {
  max-width: 100%;
}

.approval-card {
  box-shadow:
      0 4px 16px rgba(0, 0, 0, 0.08),
      0 1px 3px rgba(0, 0, 0, 0.05);
}

// 3D立体效果 - 雕栏玉砌风格
.tool-icon-container {
  // 多层次立体背景
  background: linear-gradient(135deg,
      #6b9a98 0%,     // 主色调
      #7eb4b1 25%,    // 亮部
      #5a8785 50%,    // 中间色
      #4a7472 75%,    // 暗部
      #3d605e 100%);  // 最暗部

  // 内嵌投影 + 外部投影
  box-shadow:
      // 外部立体投影 - 主投影
      0 8px 24px rgba(107, 154, 152, 0.4),
        // 外部投影 - 环境光
      0 4px 12px rgba(107, 154, 152, 0.3),
        // 内嵌高光 - 顶部边缘
      inset 0 2px 4px rgba(255, 255, 255, 0.4),
        // 内嵌阴影 - 底部边缘
      inset 0 -2px 4px rgba(0, 0, 0, 0.2),
        // 内嵌侧面高光
      inset 2px 0 6px rgba(255, 255, 255, 0.2),
        // 内嵌侧面阴影
      inset -2px 0 6px rgba(0, 0, 0, 0.1);

  // 边框立体效果
  border: 1px solid rgba(255, 255, 255, 0.3);

  // 3D变换基础
  transform-style: preserve-3d;
  perspective: 1000px;

  // 伪元素 - 创建底面投影
  position: relative;

  &::before {
    content: '';
    position: absolute;
    top: 100%;
    left: 50%;
    transform: translateX(-50%) translateY(2px) rotateX(90deg);
    width: 90%;
    height: 8px;
    background: radial-gradient(ellipse, rgba(107, 154, 152, 0.3) 0%, transparent 70%);
    border-radius: 50%;
    filter: blur(2px);
    z-index: -1;
  }

  // 伪元素 - 创建侧面反光
  &::after {
    content: '';
    position: absolute;
    top: 10%;
    right: 15%;
    width: 20%;
    height: 30%;
    background: linear-gradient(45deg,
        rgba(255, 255, 255, 0.8) 0%,
        rgba(255, 255, 255, 0.4) 50%,
        transparent 100%);
    border-radius: 50%;
    filter: blur(1px);
    pointer-events: none;
  }

  // Hover 3D效果
  &:hover {
    // 增强立体投影
    box-shadow:
        0 12px 32px rgba(107, 154, 152, 0.5),
        0 6px 16px rgba(107, 154, 152, 0.4),
        inset 0 3px 6px rgba(255, 255, 255, 0.5),
        inset 0 -3px 6px rgba(0, 0, 0, 0.25),
        inset 3px 0 8px rgba(255, 255, 255, 0.25),
        inset -3px 0 8px rgba(0, 0, 0, 0.15);

    // 轻微的3D旋转
    transform: scale(1.05) rotateY(5deg) rotateX(-2deg);

    // 增强高光
    &::after {
      opacity: 1.2;
      background: linear-gradient(45deg,
          rgba(255, 255, 255, 0.9) 0%,
          rgba(255, 255, 255, 0.5) 50%,
          transparent 100%);
    }
  }

  // Active 按压效果
  &:active {
    transform: scale(0.98) rotateY(2deg) rotateX(-1deg);
    box-shadow:
        0 4px 16px rgba(107, 154, 152, 0.3),
        0 2px 8px rgba(107, 154, 152, 0.25),
        inset 0 1px 3px rgba(255, 255, 255, 0.3),
        inset 0 -1px 3px rgba(0, 0, 0, 0.15);
  }

  // 图标样式
  span {
    font-size: 1.5rem;
    text-shadow:
        0 1px 2px rgba(0, 0, 0, 0.3),
        0 1px 0 rgba(255, 255, 255, 0.4);
    filter: drop-shadow(0 1px 1px rgba(0, 0, 0, 0.2));
  }
}

// 自定义滚动条样式
.params-formatted,
.code-container {
  scrollbar-width: thin;
  scrollbar-color: rgba(107, 154, 152, 0.3) transparent;

  &::-webkit-scrollbar {
    width: 6px;
    height: 6px; // 添加水平滚动条高度
  }

  &::-webkit-scrollbar-track {
    background: rgba(107, 154, 152, 0.1);
    border-radius: 3px;
  }

  &::-webkit-scrollbar-thumb {
    background: rgba(107, 154, 152, 0.3);
    border-radius: 3px;
    transition: background-color 0.2s ease;

    &:hover {
      background: rgba(107, 154, 152, 0.5);
    }
  }

  &::-webkit-scrollbar-corner {
    background: transparent;
  }
}

// JSON代码容器特殊样式
.code-container {
  &::-webkit-scrollbar-track {
    background: rgba(255, 255, 255, 0.1); // 深色背景下的轨道色
  }

  &::-webkit-scrollbar-thumb {
    background: rgba(255, 255, 255, 0.3); // 深色背景下的滚动块

    &:hover {
      background: rgba(255, 255, 255, 0.5);
    }
  }
}

// 响应式设计
@media (max-width: 768px) {
  .approval-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;
  }

  .tool-icon-container {
    width: 40px;
    height: 40px;
  }

  .status-panel {
    align-self: flex-start;
  }

  .params-formatted,
  .code-container {
    max-height: 20rem; // 移动端降低最大高度
  }
}
</style>
