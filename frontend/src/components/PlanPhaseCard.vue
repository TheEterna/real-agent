<script setup lang="ts">
import { ref, computed } from 'vue'
import { PlanPhase, PlanPhaseStatus } from '@/types/events'
import { gsap } from 'gsap'
import {
  ClockCircleOutlined,
  RocketOutlined,
  CheckCircleOutlined,
  PauseCircleOutlined,
  CloseCircleOutlined,
  EditOutlined
} from '@ant-design/icons-vue'

interface Props {
  phase: PlanPhase
  isCurrent?: boolean
  editable?: boolean
}

interface Emits {
  click: [phase: PlanPhase]
  edit: [updates: Partial<PlanPhase>]
}

const props = withDefaults(defineProps<Props>(), {
  isCurrent: false,
  editable: true
})

const emit = defineEmits<Emits>()

// 编辑状态
const isEditing = ref(false)
const editForm = ref({
  title: '',
  description: '',
  isParallel: false
})

// 计算状态样式
const phaseStatusClass = computed(() => {
  const statusMap = {
    [PlanPhaseStatus.TODO]: 'status-todo',
    [PlanPhaseStatus.RUNNING]: 'status-running',
    [PlanPhaseStatus.COMPLETED]: 'status-completed',
    [PlanPhaseStatus.PAUSED]: 'status-paused',
    [PlanPhaseStatus.FAILED]: 'status-failed'
  }
  return statusMap[props.phase.status || PlanPhaseStatus.TODO] || 'status-todo'
})

// 计算状态文本
const statusText = computed(() => {
  const textMap = {
    [PlanPhaseStatus.TODO]: '待执行',
    [PlanPhaseStatus.RUNNING]: '执行中',
    [PlanPhaseStatus.COMPLETED]: '已完成',
    [PlanPhaseStatus.PAUSED]: '已暂停',
    [PlanPhaseStatus.FAILED]: '失败'
  }
  return textMap[props.phase.status || PlanPhaseStatus.TODO] || '未知'
})

// 计算状态图标
const statusIcon = computed(() => {
  const iconMap = {
    [PlanPhaseStatus.TODO]: ClockCircleOutlined,
    [PlanPhaseStatus.RUNNING]: RocketOutlined,
    [PlanPhaseStatus.COMPLETED]: CheckCircleOutlined,
    [PlanPhaseStatus.PAUSED]: PauseCircleOutlined,
    [PlanPhaseStatus.FAILED]: CloseCircleOutlined
  }
  return iconMap[props.phase.status || PlanPhaseStatus.TODO] || ClockCircleOutlined
})

// 开始编辑
const startEdit = () => {
  if (!props.editable) return

  editForm.value = {
    title: props.phase.title,
    description: props.phase.description,
    isParallel: props.phase.isParallel
  }
  isEditing.value = true

  // 编辑模式进入动画
  setTimeout(() => {
    gsap.fromTo('.edit-form',
      { height: 0, opacity: 0 },
      { height: 'auto', opacity: 1, duration: 0.3, ease: 'power2.out' }
    )
  }, 50)
}

// 保存编辑
const saveEdit = () => {
  if (!isEditing.value) return

  emit('edit', {
    title: editForm.value.title.trim(),
    description: editForm.value.description.trim(),
    isParallel: editForm.value.isParallel
  })

  // 编辑退出动画
  gsap.to('.edit-form',
    { height: 0, opacity: 0, duration: 0.3, ease: 'power2.in' }
  ).then(() => {
    isEditing.value = false
  })
}

// 取消编辑
const cancelEdit = () => {
  // 编辑退出动画
  gsap.to('.edit-form',
    { height: 0, opacity: 0, duration: 0.3, ease: 'power2.in' }
  ).then(() => {
    isEditing.value = false
  })
}

// 处理卡片点击
const handleClick = () => {
  if (!isEditing.value) {
    emit('click', props.phase)
  }
}

// 格式化描述文本（限制长度）
const formatDescription = (text: string, maxLength: number = 100) => {
  if (text.length <= maxLength) return text
  return text.substring(0, maxLength) + '...'
}

// 计算进度（如果有的话）
const phaseProgress = computed(() => {
  if (props.phase.status === PlanPhaseStatus.COMPLETED) return 100
  if (props.phase.status === PlanPhaseStatus.RUNNING) return 50 // 假设值
  return 0
})
</script>

<template>
  <div
    class="phase-card"
    :class="{
      [phaseStatusClass]: true,
      current: isCurrent,
      clickable: !isEditing,
      editing: isEditing
    }"
    @click="handleClick"
  >
    <!-- 卡片头部 -->
    <div class="card-header">
      <div class="header-left">
        <div class="status-indicator">
          <component :is="statusIcon" class="status-icon" />
          <span class="status-text">{{ statusText }}</span>
        </div>
        <div class="phase-meta">
          <span class="phase-index">阶段 {{ (phase.index || 0) + 1 }}</span>
          <span v-if="phase.isParallel" class="parallel-badge">并行</span>
        </div>
      </div>
      <EditOutlined
        v-if="editable && !isEditing"
        type="text"
        @click.stop="startEdit"
        class="edit-btn header-actions p-2 text-lg rounded-md"
      />
    </div>

    <!-- 卡片内容 -->
    <div class="card-content" v-if="!isEditing">
      <div class="phase-title">{{ phase.title }}</div>
      <div class="phase-description">
        {{ formatDescription(phase.description, 120) }}
      </div>

      <!-- 进度条 -->
      <div class="progress-bar" v-if="phaseProgress > 0">
        <div
          class="progress-fill"
          :style="{ width: `${phaseProgress}%` }"
        ></div>
      </div>
    </div>

    <!-- 编辑表单 -->
    <div class="edit-form" v-if="isEditing">
      <div class="form-group">
        <label class="form-label">阶段标题</label>
        <a-input
          v-model:value="editForm.title"
          placeholder="请输入阶段标题"
          class="form-input"
          :maxlength="50"
          show-count
        />
      </div>

      <div class="form-group">
        <label class="form-label">阶段描述</label>
        <a-textarea
          v-model:value="editForm.description"
          placeholder="请输入阶段详细描述"
          class="form-textarea"
          :rows="3"
          :maxlength="300"
          show-count
        />
      </div>

      <div class="form-group">
        <a-checkbox v-model:checked="editForm.isParallel">
          可并行执行
        </a-checkbox>
      </div>

      <div class="form-actions">
        <a-button type="primary" size="small" @click="saveEdit">
          保存
        </a-button>
        <a-button size="small" @click="cancelEdit">
          取消
        </a-button>
      </div>
    </div>

    <!-- 当前阶段脉冲效果 -->
    <div
      v-if="isCurrent && phase.status === PlanPhaseStatus.RUNNING"
      class="pulse-ring"
    ></div>
  </div>
</template>

<style scoped lang="scss">
@use '@/styles/variables.scss';

.phase-card {
  @include plan-card-base;
  position: relative;
  overflow: hidden;
}

.phase-card.clickable {
  cursor: pointer;
}

.phase-card.clickable:hover {
  transform: translateY(-2px);
  box-shadow: $plan-shadow-md;
}

.phase-card.current {
  border-color: $primary-color-500;
  box-shadow: 0 4px 20px rgba($primary-color-500, 0.15);
}

.phase-card.editing {
  border-color: $plan-status-running;
  box-shadow: 0 4px 20px rgba($plan-status-running, 0.15);
}

/* 状态样式 - 使用 mixin 和变量 */
.phase-card.status-todo {
  border-left: 4px solid $plan-status-todo;
}

.phase-card.status-running {
  @include plan-status-style($plan-status-running, $plan-status-running-bg);
}

.phase-card.status-completed {
  @include plan-status-style($plan-status-completed, $plan-status-completed-bg);
  opacity: 0.8;
}

.phase-card.status-paused {
  @include plan-status-style($plan-status-paused, $plan-status-paused-bg);
}

.phase-card.status-failed {
  @include plan-status-style($plan-status-failed, $plan-status-failed-bg);
}

/* 卡片头部 */
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: $space-md;
}

.header-left {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: $space-xs;
}

.status-indicator {
  display: flex;
  align-items: center;
  gap: $space-xs;
}

.status-icon {
  font-size: 16px;
  color: inherit;
}

/* 不同状态下的图标颜色 - 使用变量 */
.status-todo .status-icon {
  color: $plan-status-todo;
}

.status-running .status-icon {
  color: $plan-status-running;
}

.status-completed .status-icon {
  color: $plan-status-completed;
}

.status-paused .status-icon {
  color: $plan-status-paused;
}

.status-failed .status-icon {
  color: $plan-status-failed;
}

.status-text {
  font-size: 12px;
  font-weight: 600;
  color: $muted-color;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.phase-meta {
  display: flex;
  gap: $space-sm;
  align-items: center;
}

.phase-index {
  font-size: 11px;
  color: lighten($muted-color, 20%);
}

.parallel-badge {
  font-size: 10px;
  background: $jelly-orange-deep;
  color: white;
  padding: 2px $space-xs;
  border-radius: 8px;
  font-weight: 500;
}

.header-actions {
  flex-shrink: 0;
}

.edit-btn {
  color: $primary-color-500;
  background: rgba($primary-color-100, 0.8);
}

.edit-btn:hover {
  color: $primary-color-0;
  background: rgba($primary-color-500, 0.8);
}

/* 卡片内容 */
.card-content {
  display: flex;
  flex-direction: column;
  gap: $space-sm;
}

.phase-title {
  font-size: 14px;
  font-weight: 600;
  color: $text-color;
  line-height: 1.4;
  margin-bottom: $space-xs;
}

.phase-description {
  font-size: 13px;
  color: lighten($text-color, 20%);
  line-height: 1.5;
}

/* 进度条 */
.progress-bar {
  height: 3px;
  background: rgba($border-color, 0.5);
  border-radius: 2px;
  overflow: hidden;
  margin-top: $space-sm;
}

.progress-fill {
  height: 100%;
  background: linear-gradient(90deg, $plan-status-running, lighten($plan-status-running, 10%));
  transition: width 0.3s ease;
  border-radius: 2px;
}

/* 编辑表单 */
.edit-form {
  display: flex;
  flex-direction: column;
  gap: $space-md;
  overflow: hidden;
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: $space-xs;
}

.form-label {
  font-size: 12px;
  color: $muted-color;
  font-weight: 500;
}

.form-input,
.form-textarea {
  font-size: 13px;
}

.form-actions {
  display: flex;
  gap: $space-sm;
  justify-content: flex-end;
  margin-top: $space-sm;
}

/* 脉冲环 */
.pulse-ring {
  position: absolute;
  top: -2px;
  left: -2px;
  right: -2px;
  bottom: -2px;
  border: 2px solid $primary-color-500;
  border-radius: 12px;
  animation: pulse-ring 2s infinite;
  pointer-events: none;
}

@keyframes pulse-ring {
  0% {
    transform: scale(1);
    opacity: 1;
  }
  100% {
    transform: scale(1.04);
    opacity: 0;
  }
}

/* 响应式适配 */
@media (max-width: 480px) {
  .phase-card {
    padding: $space-md;
  }

  .card-header {
    margin-bottom: $space-sm;
  }

  .phase-title {
    font-size: 13px;
  }

  .phase-description {
    font-size: 12px;
  }
}
</style>