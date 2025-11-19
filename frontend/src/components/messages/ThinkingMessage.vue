<script setup lang="ts">
import { ref, computed } from 'vue'
import MarkdownViewer from '@/components/MarkdownViewer.vue'
import { CaretUpOutlined } from '@ant-design/icons-vue';

import {
  CheckCircleTwoTone,
} from '@ant-design/icons-vue'

const props = defineProps<{
  content: string
  sender?: string
  startTime?: Date | string
  isThinking?: boolean  // 是否正在思考（默认false表示已完成）
}>()

// 计算是否显示 loading 动画
const showSpinner = computed(() => props.isThinking ?? false)

const isExpanded = ref(false)

const toggleExpand = () => {
  isExpanded.value = !isExpanded.value
}

</script>

<template>
  <div class="group">
    <!-- 折叠头部 -->
    <button
      @click="toggleExpand"
      class="ripple-button cursor-pointer flex items-center bg-primary-50 hover:bg-primary-200 active:bg-primary-300 justify-between leading-1
      px-3.5 py-3 gap-2.5 duration-200 rounded-3xl relative overflow-hidden transition-all ease-in-out transform hover:scale-[1.02] active:scale-[0.98]"
      :aria-expanded="isExpanded"
    >
        <!-- 思考中：显示 spinner -->
      <div
          v-if="showSpinner"
          class="relative w-4 h-4 flex-shrink-0"
        >
          <div class="absolute inset-0 border-2 border-primary-200 dark:border-primary-700 rounded-full"></div>
          <div class="absolute inset-0 border-2 border-primary-500 border-t-transparent rounded-full animate-spin"></div>
      </div>

      <!-- 思考完成：显示完成图标 -->
      <div v-else class="w-5 h-5 flex-shrink-0 text-celadon-dark dark:text-celadon">
        <CheckCircleTwoTone class="text-xl"/>
      </div>

      <span class="text-sm font-medium text-primary-700 dark:text-primary-300">
        {{ showSpinner ? '思考中...' : '思考过程' }}
      </span>


      <CaretUpOutlined class=" flex items-center text-primary-400 dark:text-primary-500 transition-transform duration-200"
                       :class="{ 'rotate-180': isExpanded }" />

    </button>

    <!-- 可展开的内容 -->
    <Transition
      enter-active-class="transition-all duration-300 ease-out"
      leave-active-class="transition-all duration-200 ease-in"
      enter-from-class="opacity-0 max-h-0"
      enter-to-class="opacity-100 max-h-screen"
      leave-from-class="opacity-100 max-h-screen"
      leave-to-class="opacity-0 max-h-0"
    >
      <div v-show="isExpanded" class="overflow-hidden pl-3 pt-3">
        <div class="px-3 pb-3 pt-1 border-primary-400 border-l">
          <!-- 内容区域 -->
          <div class="prose prose-sm max-w-none dark:prose-invert prose-primary">
            <div class="text-primary-700 dark:text-primary-300 leading-relaxed">
              <MarkdownViewer :message="content" />
            </div>
          </div>

        </div>
      </div>
    </Transition>
  </div>
</template>

<style scoped lang="scss">

/* 按钮增强交互效果 */


/* 确保 prose 样式不会影响整体布局 */
.prose {
  font-size: 12px;
}

.prose :where(p):not(:where([class~="not-prose"] *)) {
  margin-top: 0;
  margin-bottom: 0.75rem;
}

.prose :where(p):last-child:not(:where([class~="not-prose"] *)) {
  margin-bottom: 0;
}

.prose :where(ul):not(:where([class~="not-prose"] *)) {
  margin-top: 0.5rem;
  margin-bottom: 0.5rem;
}

.prose :where(li):not(:where([class~="not-prose"] *)) {
  margin-top: 0.25rem;
  margin-bottom: 0.25rem;
}

/* 自定义滚动条（可选） */
.overflow-hidden:hover::-webkit-scrollbar {
  width: 4px;
}

.overflow-hidden:hover::-webkit-scrollbar-track {
  background: transparent;
}

.overflow-hidden:hover::-webkit-scrollbar-thumb {
  background: rgba(156, 163, 175, 0.3);
  border-radius: 2px;
}

.overflow-hidden:hover::-webkit-scrollbar-thumb:hover {
  background: rgba(156, 163, 175, 0.5);
}
</style>
