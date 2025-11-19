<template>
  <div
    class="param-card bg-white border border-primary-200 rounded-xl p-3 hover:border-primary-300 hover:shadow-sm transition-all"
    :style="{ marginLeft: level * 16 + 'px' }"
  >
    <div class="flex items-center justify-between mb-1.5">
      <div class="flex items-center gap-2">
        <!-- Required 标识 -->
        <span v-if="isRequired" class="text-red-500 font-bold">*</span>
        <span class="text-xs font-bold text-primary-600 uppercase">{{ paramKey }}</span>
        <!-- Description Tooltip -->
        <a-tooltip v-if="getDescription(paramKey)" placement="top" :title="getDescription(paramKey)">
          <InfoCircleOutlined class="w-3 h-3 text-primary-400 hover:text-primary-600 cursor-help" />
        </a-tooltip>
      </div>
      <span class="text-xs px-1.5 py-0.5 rounded bg-primary-100 text-primary-600">{{ getValueType(paramValue) }}</span>
    </div>

    <!-- 值渲染 -->
    <div class="param-value">
      <!-- 简单值 -->
      <template v-if="!isComplexValue(paramValue)">
        <div class="text-sm font-mono" :class="getValueTypeClass(paramValue)">
          <template v-if="typeof paramValue === 'string'">
            <span class="text-primary-400">"</span>
            <span>{{ paramValue }}</span>
            <span class="text-primary-400">"</span>
          </template>
          <template v-else-if="typeof paramValue === 'number'">
            <span class="text-red-600">{{ paramValue }}</span>
          </template>
          <template v-else-if="typeof paramValue === 'boolean'">
            <span class="text-primary-600">{{ paramValue }}</span>
          </template>
          <template v-else-if="paramValue === null">
            <span class="text-gray-400">null</span>
          </template>
        </div>
      </template>

      <!-- 数组值 -->
      <template v-else-if="Array.isArray(paramValue)">
        <div class="space-y-2 mt-2">
          <div class="text-xs text-primary-500 flex items-center gap-1">
            <span>数组长度: {{ paramValue.length }}</span>
            <button
              v-if="!reachedMaxDepth"
              @click="toggleCollapse(`${paramKey}-array`)"
              class="text-primary-400 hover:text-primary-600"
            >
              <CaretUpFilled
                :class="{ 'rotate-180': !collapsed[`${paramKey}-array`] }"
                class="transition-transform"
              />
            </button>
            <span v-if="reachedMaxDepth" class="text-xs text-orange-500">(已达最大展开深度)</span>
          </div>
          <div v-show="!collapsed[`${paramKey}-array`] && !reachedMaxDepth" class="space-y-1">
            <div v-for="(item, index) in paramValue" :key="index">
              <ParameterRenderer
                :param-key="`[${index}]`"
                :param-value="item"
                :level="level + 1"
                :descriptions="{}"
                :required-fields="[]"
                :max-depth="maxDepth"
              />
            </div>
          </div>
          <!-- 深度达到上限时显示简化信息 -->
          <div v-if="reachedMaxDepth" class="text-xs text-gray-500 bg-gray-50 p-2 rounded">
            {{ formatArrayPreview(paramValue) }}
          </div>
        </div>
      </template>

      <!-- 对象值 -->
      <template v-else>
        <div class="space-y-2 mt-2">
          <div class="text-xs text-primary-500 flex items-center gap-1">
            <span>对象属性: {{ Object.keys(paramValue).length }}</span>
            <button
              v-if="!reachedMaxDepth"
              @click="toggleCollapse(`${paramKey}-object`)"
              class="text-primary-400 hover:text-primary-600"
            >
              <CaretUpFilled
                :class="{ 'rotate-180': !collapsed[`${paramKey}-object`] }"
                class="transition-transform"
              />
            </button>
            <span v-if="reachedMaxDepth" class="text-xs text-orange-500">(已达最大展开深度)</span>
          </div>
          <div v-show="!collapsed[`${paramKey}-object`] && !reachedMaxDepth" class="space-y-1">
            <div v-for="(value, key) in paramValue" :key="key">
              <ParameterRenderer
                :param-key="key"
                :param-value="value"
                :level="level + 1"
                :descriptions="getNestedDescriptions(paramKey)"
                :required-fields="getRequiredFields(paramKey)"
                :max-depth="maxDepth"
              />
            </div>
          </div>
          <!-- 深度达到上限时显示简化信息 -->
          <div v-if="reachedMaxDepth" class="text-xs text-gray-500 bg-gray-50 p-2 rounded">
            {{ formatObjectPreview(paramValue) }}
          </div>
        </div>
      </template>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { InfoCircleOutlined, CaretUpFilled } from '@ant-design/icons-vue'

interface Props {
  paramKey: string
  paramValue: any
  level: number
  descriptions: Record<string, any>
  requiredFields?: string[]
  maxDepth?: number
}

const props = withDefaults(defineProps<Props>(), {
  requiredFields: () => [],
  maxDepth: 3
})

// 折叠状态管理
const collapsed = ref<Record<string, boolean>>({})

// 检查是否达到最大深度
const reachedMaxDepth = computed(() => props.level >= props.maxDepth)

// 检查字段是否为必填
const isRequired = computed(() => {
  return props.requiredFields?.includes(props.paramKey) || false
})

// 获取值类型
const getValueType = (value: any): string => {
  if (Array.isArray(value)) return `array[${value.length}]`
  if (value === null) return 'null'
  if (typeof value === 'object') return `object{${Object.keys(value).length}}`
  return typeof value
}

// 判断是否为复杂值
const isComplexValue = (value: any): boolean => {
  return typeof value === 'object' && value !== null
}

// 获取值样式类
const getValueTypeClass = (value: any) => {
  const type = typeof value
  return {
    'text-green-600': type === 'string',
    'text-red-600': type === 'number',
    'text-primary-600': type === 'boolean',
    'text-gray-400': value === null
  }
}

// 获取描述信息
const getDescription = (key: string): string => {
  if (!props.descriptions || typeof props.descriptions !== 'object') return ''

  // 支持嵌套路径，如 migrationConfig.sourceDatabase.host
  const path = key.split('.')
  let current = props.descriptions

  for (const segment of path) {
    if (current && typeof current === 'object' && segment in current) {
      current = current[segment]
    } else {
      return ''
    }
  }

  return typeof current === 'string' ? current : current?.description || ''
}

// 获取嵌套对象的描述信息
const getNestedDescriptions = (parentKey: string): Record<string, any> => {
  if (!props.descriptions || typeof props.descriptions !== 'object') return {}

  // 从父级描述中获取子级描述
  const parentDesc = props.descriptions[parentKey]
  if (parentDesc && typeof parentDesc === 'object' && 'properties' in parentDesc) {
    return parentDesc.properties || {}
  }

  return {}
}

// 获取嵌套对象的必填字段
const getRequiredFields = (parentKey: string): string[] => {
  if (!props.descriptions || typeof props.descriptions !== 'object') return []

  const parentDesc = props.descriptions[parentKey]
  if (parentDesc && typeof parentDesc === 'object' && 'required' in parentDesc) {
    return Array.isArray(parentDesc.required) ? parentDesc.required : []
  }

  return []
}

// 格式化数组预览信息
const formatArrayPreview = (arr: any[]): string => {
  if (arr.length === 0) return '空数组'

  const types = arr.slice(0, 3).map(item => {
    if (typeof item === 'object' && item !== null) {
      return Array.isArray(item) ? 'Array' : 'Object'
    }
    return typeof item
  })

  const preview = types.join(', ')
  return `[${preview}${arr.length > 3 ? ', ...' : ''}]`
}

// 格式化对象预览信息
const formatObjectPreview = (obj: Record<string, any>): string => {
  const keys = Object.keys(obj)
  if (keys.length === 0) return '空对象'

  const preview = keys.slice(0, 3).join(', ')
  return `{ ${preview}${keys.length > 3 ? ', ...' : ''} }`
}

// 切换折叠状态
const toggleCollapse = (key: string) => {
  collapsed.value[key] = !collapsed.value[key]
}
</script>

<style scoped lang="scss">
.param-card {
  border-left: 3px solid transparent;
  transition: all 0.2s ease;
}


/* 不同层级的区分 */
.param-card[style*="margin-left: 16px"] {
  background-color: $primary-color-50;
}

.param-card[style*="margin-left: 32px"] {
  background-color: $primary-color-75;
}

.param-card[style*="margin-left: 48px"] {
  background-color: $primary-color-100;
}

.param-card[style*="margin-left: 32px"]:hover {
  border-left-color: $primary-color-300;
}

.param-card[style*="margin-left: 48px"]:hover {
  border-left-color: $primary-color-300;
}
</style>