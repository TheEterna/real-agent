<template>
  <a-card :title="title" :loading="loading" size="small">
    <a-tabs v-model:activeKey="activeKey">
      <a-tab-pane key="summary" :tab="t('tools.result.summary')">
        <a-descriptions bordered size="small" :column="1">
          <a-descriptions-item :label="t('tools.result.status')">
            <a-badge :status="statusBadge.status" :text="statusBadge.text" />
          </a-descriptions-item>
          <a-descriptions-item :label="t('tools.result.toolName')">{{ toolName || '-' }}</a-descriptions-item>
          <a-descriptions-item :label="'turnId'">{{ norm.turnId || '-' }}</a-descriptions-item>
          <a-descriptions-item :label="t('tools.result.duration')">{{ norm.timing?.durationMs ?? '-' }} ms</a-descriptions-item>
          <a-descriptions-item :label="t('tools.result.message')">{{ norm.message || '-' }}</a-descriptions-item>
        </a-descriptions>
      </a-tab-pane>
      <a-tab-pane key="args" :tab="t('tools.result.args')">
        <pre class="code">{{ pretty(args) }}</pre>
      </a-tab-pane>
      <a-tab-pane key="result" :tab="t('tools.result.result')">
        <template v-if="norm.result?.type==='json'">
          <pre class="code">{{ pretty(norm.result.value) }}</pre>
        </template>
        <template v-else-if="norm.result?.type==='text'">
          <MarkdownViewer :message="String(norm.result.value ?? '')" />
        </template>
        <template v-else>
          <pre class="code">{{ pretty(norm.rawData) }}</pre>
        </template>
      </a-tab-pane>
      <a-tab-pane key="logs" :tab="t('tools.result.logs')">
        <a-empty v-if="!norm.logs || !norm.logs.length" />
        <a-list v-else :data-source="norm.logs">
          <template #renderItem="{ item }">
            <a-list-item>
              <a-tag :color="logColor(item.level)">{{ item.level || 'INFO' }}</a-tag>
              <span style="margin-left:8px">{{ item.ts || '' }}</span>
              <div style="margin-top:4px">{{ item.message }}</div>
            </a-list-item>
          </template>
        </a-list>
      </a-tab-pane>
      <a-tab-pane key="metrics" :tab="t('tools.result.metrics')">
        <a-empty v-if="!norm.metrics || !Object.keys(norm.metrics).length" />
        <a-descriptions v-else bordered size="small" :column="1">
          <a-descriptions-item v-for="(v,k) in norm.metrics" :key="k" :label="k">{{ v }}</a-descriptions-item>
        </a-descriptions>
      </a-tab-pane>
      <a-tab-pane key="raw" :tab="t('tools.result.raw')">
        <pre class="code">{{ pretty(raw) }}</pre>
      </a-tab-pane>
    </a-tabs>
  </a-card>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import MarkdownViewer from './MarkdownViewer.vue'

interface NormalizedResult {
  ok: boolean
  message?: string
  result?: { type: 'text'|'json'|'binary'|'unknown', value: unknown }
  logs?: Array<{ level?: string; message: string; ts?: string }>
  metrics?: Record<string, unknown>
  turnId?: string
  timing?: { startTime?: string; endTime?: string; durationMs?: number }
  rawData?: unknown
}

const props = defineProps<{
  toolName?: string
  args?: unknown
  raw: unknown
  loading?: boolean
}>()

const { t } = useI18n()
const activeKey = ref('summary')

const title = computed(() => t('tools.result.title'))

function isResponseResult(resp: unknown): resp is { code: unknown; message: unknown; startTime: unknown; data?: unknown }{
  return !!resp && typeof resp === 'object' && 'code' in (resp as any) && 'message' in (resp as any) && 'startTime' in (resp as any)
}

function normalize(raw: unknown): NormalizedResult{
  // 若为统一包装，取 data 继续
  const body = isResponseResult(raw) ? raw.data : raw
  const norm: NormalizedResult = { ok: true, rawData: body }
  // 经验性探测字段
  if (body && typeof body === 'object'){
    const anyBody = body as Record<string, unknown>
    if ('ok' in anyBody) norm.ok = Boolean((anyBody as any).ok)
    if ('message' in anyBody) norm.message = String((anyBody as any).message ?? '')
    if ('turnId' in anyBody) norm.turnId = String((anyBody as any).turnId ?? '')
    const start = (anyBody as any).startTime || (anyBody as any).start || undefined
    const end = (anyBody as any).endTime || (anyBody as any).end || undefined
    const dur = (anyBody as any).durationMs || (anyBody as any).duration || undefined
    if (start || end || dur){
      norm.timing = { startTime: start, endTime: end, durationMs: dur }
    }
    if (Array.isArray((anyBody as any).logs)) norm.logs = (anyBody as any).logs
    if ((anyBody as any).metrics) norm.metrics = (anyBody as any).metrics
    // 结果推断
    const r = (anyBody as any).result ?? (anyBody as any).data ?? (anyBody as any).output
    if (r !== undefined){
      if (typeof r === 'string') norm.result = { type: 'text', value: r as string }
      else if (typeof r === 'object') norm.result = { type: 'json', value: r as object }
      else norm.result = { type: 'unknown', value: r }
    }
  }
  // 若未检测到结果，则以原始 raw 展示
  if (!('result' in (norm as any))) {
    norm.result = { type: 'json', value: isResponseResult(raw) ? raw.data : raw }
  }
  return norm
}

const norm = computed(() => normalize(props.raw))

const statusBadge = computed(() => ({
  status: (props.loading ? 'processing' : (norm.value.ok ? 'success' : 'error')),
  text: (props.loading ? t('tools.result.running') : (norm.value.ok ? t('tools.result.success') : t('tools.result.failed')))
}))

function pretty(v: unknown){
  try{ return JSON.stringify(v, null, 2) }catch{ return String(v as any) }
}

function logColor(level?: string){
  const l = (level||'').toUpperCase()
  if (l==='ERROR') return 'red'
  if (l==='WARN' || l==='WARNING') return 'orange'
  return 'blue'
}
</script>

<style scoped>
.code {
  white-space: pre-wrap;
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, "Liberation Mono", "Courier New", monospace;
  font-size: 12px;
}
</style>
