<template>
  <a-card size="small" :title="t('approval.title')" :bordered="true" class="approval-card">
    <a-descriptions :column="1" size="small" bordered>
      <a-descriptions-item :label="t('approval.toolName')">
        {{ approval?.toolName || '-' }}
      </a-descriptions-item>
      <a-descriptions-item :label="t('approval.args')">
        <pre class="code">{{ pretty(approval?.args) }}</pre>
      </a-descriptions-item>
    </a-descriptions>
    <div class="actions">
      <a-space>
        <a-button type="primary" :loading="executing" @click="onApprove">{{ t('approval.approve') }}</a-button>
        <a-button danger @click="onReject" :disabled="executing">{{ t('approval.reject') }}</a-button>
        <a-button @click="openInTools" :disabled="executing">{{ t('approval.openInTools') }}</a-button>
      </a-space>
    </div>
    <div v-if="statusMsg" class="status">{{ statusMsg }}</div>
  </a-card>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRouter } from 'vue-router'
import { approveExecute } from '@/services/tools'

const props = defineProps<{ approval: { toolName?: string; args?: any } }>()
const { t } = useI18n()
const router = useRouter()

const executing = ref(false)
const statusMsg = ref('')

function pretty(v:any){
  try{ return JSON.stringify(v ?? {}, null, 2) }catch{ return String(v) }
}

async function onApprove(){
  if(!props.approval?.toolName){ statusMsg.value = t('approval.noToolName'); return }
  executing.value = true
  statusMsg.value = t('approval.executing')
  try{
    const res = await approveExecute(props.approval.toolName!, props.approval.args || {})
    statusMsg.value = t('approval.done')
    // 可选：在全局事件总线中广播执行结果，或直接依赖后端SSE返回后续消息
    console.debug('approve result', res)
  }catch(e:any){
    statusMsg.value = t('approval.failed') + ': ' + (e?.message || String(e))
  }finally{
    executing.value = false
  }
}

function onReject(){
  statusMsg.value = t('approval.rejected')
}

function openInTools(){
  const q: any = { }
  if (props.approval?.toolName) q.toolName = props.approval.toolName
  if (props.approval?.args) q.args = encodeURIComponent(JSON.stringify(props.approval.args))
  router.push({ name: 'Tools', query: q }).catch(()=>{})
}
</script>

<style scoped>
.approval-card { margin-top: 8px; }
.code { white-space: pre-wrap; font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, "Liberation Mono", "Courier New", monospace; font-size: 12px; }
.actions { margin-top: 12px; }
.status { margin-top: 8px; color: #555; }
</style>
