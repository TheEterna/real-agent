<template>
  <a-card style="width: 100%; overflow: hidden">
    <div class="tools-header">
      <div class="header-left">
        <a-input-search v-model:value="search" :placeholder="t('tools.search')" style="max-width: 360px"/>
      </div>
      <div class="header-right">
        <a-button size="small" @click="openServerConfig()">配置服务器（预留）</a-button>
      </div>
    </div>
    <!-- Row 2: MCP 区域（两列） -->
    <div class="section section-mcp">
      <div class="section-title">MCP</div>
      <a-row :gutter="16" class="section-body">
        <!-- 左列：client 选择 + servers 列表 -->
        <a-col :span="12" class="col">
          <div class="mcp-left">
            <template v-if="mcpClients.length">
              <a-tabs v-model:activeKey="selectedClient" type="card" class="mcp-tabs">
                <a-tab-pane v-for="client in mcpClients" :key="client" :tab="client">
                  <div class="servers-list" ref="serversListRef">
                    <a-list :data-source="serversByClient[client] || []" :loading="loadingList" :split="true">
                      <template #renderItem="{ item }">
                        <a-list-item>
                          <a-card class="tool-item" hoverable :bordered="true" @click="selectServer(client, item)">
                            <div class="title">
                              <strong>{{ item }}</strong>
                              <a-tag color="geekblue" style="margin-left: 8px">server</a-tag>
                            </div>
                          </a-card>
                        </a-list-item>
                      </template>
                    </a-list>
                  </div>
                </a-tab-pane>
              </a-tabs>
            </template>
          </div>
        </a-col>
        <!-- 右列：该 client/server 下的 MCP 工具列表 -->
        <a-col :span="12" class="col">
          <div class="mcp-right">
            <a-typography-title :level="5" style="margin: 0 0 8px">
              {{ selectedClient ? (selectedClient + ' / ' + (selectedServer || '')) : 'MCP Tools' }}
            </a-typography-title>
            <div class="mcp-tools-list" ref="mcpToolsListRef">
              <a-list :data-source="mcpToolsForSelected" :loading="loadingList" :split="false">
                <template #renderItem="{ item }">
                  <a-list-item>
                    <a-card class="tool-item" hoverable :bordered="false" @click="pick(item)">
                      <div class="title">
                        <strong>{{ item.spec.name }}</strong>
                        <a-tag color="purple" style="margin-left: 8px">MCP</a-tag>
                      </div>
                      <div class="desc" style="margin-top: 8px">{{ item.spec.description || '' }}</div>
                    </a-card>
                  </a-list-item>
                </template>
              </a-list>
            </div>
          </div>
        </a-col>
      </a-row>
    </div>

    <!-- Row 3: System 区域（两列） -->
    <div class="section section-system">
      <div class="section-title">System</div>
      <a-row :gutter="16" class="section-body">
        <!-- 左列：System 列表 -->
        <a-col :span="12" class="col">
          <div class="system-list" ref="systemListRef">
            <a-list :data-source="systemItems" :loading="loadingList" :split="true">
              <template #renderItem="{ item }">
                <a-list-item>
                  <a-card class="tool-item" hoverable :bordered="false" @click="pick(item)">
                    <div class="title">
                      <strong>{{ item.spec.name }}</strong>
                      <a-tag color="blue" style="margin-left: 8px">{{ item.spec.category || 'system' }}</a-tag>
                    </div>
                    <div class="desc" style="margin-top: 8px">{{ item.spec.description || '' }}</div>
                  </a-card>
                </a-list-item>
              </template>
            </a-list>
          </div>
        </a-col>
        <!-- 右列：执行面板 -->
        <a-col :span="12" class="col">
          <div class="executor-panel">
            <a-space direction="vertical" style="width:100%">
              <a-select v-model:value="exec.toolName" :placeholder="t('tools.selectTool')">
                <a-select-option v-for="t in items" :key="t.id" :value="t.spec.name">
                  <template v-if="isMcp(t)">
                    {{ t.spec.name }} ({{ t.spec.mcpToolSpec?.server }} / {{ t.spec.mcpToolSpec?.client }})
                  </template>
                  <template v-else>
                    {{ t.spec.name }} (system)
                  </template>
                </a-select-option>
              </a-select>

              <!-- 基于工具的 inputSchema 动态渲染参数表单；若没有schema，则回退为JSON文本框 -->
              <template v-if="currentSchema">
                <a-form layout="vertical">
                  <template v-for="(prop, key) in (currentSchema.properties || {})" :key="String(key)">
                    <a-form-item :label="renderLabel(String(key), prop)" :required="isRequired(String(key))">
                      <template v-if="prop.type === 'string'">
                        <a-input v-model:value="exec.argsObj[key]" :placeholder="prop.description || ''" allow-clear />
                      </template>
                      <template v-else-if="prop.type === 'number' || prop.type === 'integer'">
                        <a-input-number v-model:value="exec.argsObj[key]" :placeholder="prop.description || ''" :style="{ width: '100%' }" />
                      </template>
                      <template v-else-if="prop.type === 'boolean'">
                        <a-switch v-model:checked="exec.argsObj[key]" />
                      </template>
                      <template v-else-if="prop.type === 'object'">
                        <a-card size="small" :bordered="true" :title="prop.title ? (prop.title + ' ('+ String(key) +')') : String(key)" style="margin-bottom: 8px;">
                          <template v-if="prop.properties">
                            <template v-for="(subProp, subKey) in prop.properties" :key="String(subKey)">
                              <a-form-item :label="renderLabel(String(subKey), subProp)" :required="isRequiredIn(prop, String(subKey))">
                                <template v-if="subProp.type === 'string'">
                                  <a-input v-model:value="exec.argsObj[key][subKey]" :placeholder="subProp.description || ''" allow-clear />
                                </template>
                                <template v-else-if="subProp.type === 'number' || subProp.type === 'integer'">
                                  <a-input-number v-model:value="exec.argsObj[key][subKey]" :placeholder="subProp.description || ''" :style="{ width: '100%' }" />
                                </template>
                                <template v-else-if="subProp.type === 'boolean'">
                                  <a-switch v-model:checked="exec.argsObj[key][subKey]" />
                                </template>
                                <template v-else>
                                  <a-input v-model:value="exec.argsObj[key][subKey]" :placeholder="subProp.description || ''" allow-clear />
                                </template>
                                <div v-if="subProp.description" style="font-size:12px;color:#888;margin-top:4px;">{{ subProp.description }}</div>
                              </a-form-item>
                            </template>
                          </template>
                          <template v-else>
                            <a-textarea v-model:value="exec.argsObj[key]" :rows="4" :placeholder="(prop.description || '') + ' (JSON)'" />
                          </template>
                        </a-card>
                      </template>
                      <template v-else>
                        <a-input v-model:value="exec.argsObj[key]" :placeholder="prop.description || ''" allow-clear />
                      </template>
                      <div v-if="prop.description" style="font-size:12px;color:#888;margin-top:4px;">{{ prop.description }}</div>
                    </a-form-item>
                  </template>
                </a-form>
              </template>
              <template v-else>
                <a-textarea v-model:value="exec.argsText" :rows="8" :placeholder="t('tools.toolArgs')"/>
              </template>
              <a-space>
                <a-button type="primary" :loading="executing" @click="onExecute">{{ t('tools.execute') }}</a-button>
                <a-button @click="onReset">{{ t('tools.reset') }}</a-button>
              </a-space>

              <ToolResultViewer v-if="resultRaw"
                                :tool-name="exec.toolName"
                                :args="safeArgs"
                                :raw="resultRaw"
                                :loading="executing"
              />
            </a-space>
          </div>
        </a-col>
      </a-row>
    </div>

    <!-- 服务器配置 Modal（预留展示） -->
    <a-modal v-model:open="serverConfigVisible" :title="`配置服务器（预留） - ${selectedClient || ''}`" :footer="null">
      <p>以下为当前 client 的 server 列表（来自工具清单推导）。保存与写回尚未接入，当前仅展示。</p>
      <a-list :data-source="currentServers" bordered size="small">
        <template #renderItem="{ item }">
          <a-list-item>
            <a-space>
              <a-tag color="geekblue">server</a-tag>
              <span>{{ item }}</span>
            </a-space>
          </a-list-item>
        </template>
      </a-list>
      <a-divider/>
      <a-input placeholder="新建 server（未接入保存）" disabled/>
      <a-button type="dashed" block disabled style="margin-top:8px">新增（未接入）</a-button>
    </a-modal>
  </a-card>
</template>

<script setup lang="ts">
import {computed, onMounted, onUnmounted, ref, watch, nextTick} from 'vue'
import {useI18n} from 'vue-i18n'
import {listTools, executeTool, type ToolListItem} from '@/services/tools'
import ToolResultViewer from '@/components/ToolResultViewer.vue'

const {t} = useI18n()
const items = ref<ToolListItem[]>([])
const loadingList = ref(false)
const search = ref('')

const exec = ref<{ toolName?: string; argsText: string; argsObj: Record<string, any> }>({argsText: '{"": ""}', argsObj: {}})
const executing = ref(false)
const resultRaw = ref<any>(null)

onMounted(async () => {
  try {
    loadingList.value = true
    items.value = await listTools()
    console.log('items', items.value)
  } catch (e) {
    console.warn(e)
  } finally {
    loadingList.value = false
  }
})

// —— 动态表单（基于 inputSchema） ——
const currentTool = computed<ToolListItem | undefined>(() => items.value.find(it => it.spec.name === exec.value.toolName))
const currentSchema = computed<any | null>(() => {
  const raw = currentTool.value?.spec?.inputSchema
  if (!raw) return null
  try { return typeof raw === 'string' ? JSON.parse(raw) : raw } catch { return null }
})
const isRequired = (key: string) => Array.isArray(currentSchema.value?.required) && currentSchema.value.required.includes(key)
const renderLabel = (key: string, prop: any) => {
  const title = prop?.title ? `${prop.title} (${key})` : key
  return title
}

watch(currentSchema, (schema) => {
  // 初始化/重置 form 模型
  const next: Record<string, any> = {}
  const props = schema?.properties || {}
  Object.keys(props).forEach(k => {
    const p = props[k]
    // 简单默认值：string -> '', number/integer -> undefined, boolean -> false, object -> {}
    if (p?.type === 'string') next[k] = ''
    else if (p?.type === 'number' || p?.type === 'integer') next[k] = undefined
    else if (p?.type === 'boolean') next[k] = false
    else if (p?.type === 'object') next[k] = {}
    else next[k] = ''
  })
  exec.value.argsObj = next
  // 同步生成示例 JSON（方便复制/回退）
  try { exec.value.argsText = JSON.stringify(next, null, 2) } catch {}
}, { immediate: true })

function isRequiredIn(parentProp: any, key: string) {
  const req = parentProp?.required
  return Array.isArray(req) && req.includes(key)
}

function buildArgsFromSchema(): any {
  const schema = currentSchema.value
  if (!schema) return null
  const props = schema.properties || {}
  const out: Record<string, any> = {}
  for (const k of Object.keys(props)) {
    const p = props[k]
    let v = exec.value.argsObj[k]
    if (v === undefined || v === null || v === '') {
      if (isRequired(k)) throw new Error(`缺少必填参数: ${k}`)
      continue
    }
    if (p.type === 'number' || p.type === 'integer') {
      const num = typeof v === 'number' ? v : Number(v)
      if (Number.isNaN(num)) throw new Error(`参数 ${k} 需要为数字`)
      v = num
    } else if (p.type === 'boolean') {
      v = Boolean(v)
    } else if (p.type === 'object') {
      if (p.properties) {
        const childOut: Record<string, any> = {}
        for (const subKey of Object.keys(p.properties)) {
          const subProp = p.properties[subKey]
          let sv = v ? v[subKey] : undefined
          if (sv === undefined || sv === null || sv === '') {
            if (isRequiredIn(p, subKey)) throw new Error(`缺少必填参数: ${k}.${subKey}`)
            continue
          }
          if (subProp.type === 'number' || subProp.type === 'integer') {
            const num2 = typeof sv === 'number' ? sv : Number(sv)
            if (Number.isNaN(num2)) throw new Error(`参数 ${k}.${subKey} 需要为数字`)
            sv = num2
          } else if (subProp.type === 'boolean') {
            sv = Boolean(sv)
          } else if (subProp.type === 'object') {
            // 仅处理两级，若还有更深层，作为原值传递（或可选: 解析字符串为对象）
            if (typeof sv === 'string') {
              try { sv = sv ? JSON.parse(sv) : {} } catch { throw new Error(`参数 ${k}.${subKey} 需要为合法的 JSON 对象`) }
            }
          }
          childOut[subKey] = sv
        }
        v = childOut
      } else {
        // 无属性定义时，接受 JSON 文本
        if (typeof v === 'string') {
          try { v = v ? JSON.parse(v) : {} } catch (e) { throw new Error(`参数 ${k} 需要为合法的 JSON 对象`) }
        }
      }
    }
    out[k] = v
  }
  return out
}

async function onExecute() {
  if (!exec.value.toolName) {
    // 简单提示，结果区显示错误
    resultRaw.value = {ok: false, message: t('tools.noToolSelected')}
    return
  }
  let args: any = {}
  if (currentSchema.value) {
    try { args = buildArgsFromSchema() } catch (e) {
      resultRaw.value = { ok: false, message: (e as Error).message }
      return
    }
  } else {
    try { args = exec.value.argsText ? JSON.parse(exec.value.argsText) : {} } catch (e) {
      resultRaw.value = {ok: false, message: t('tools.invalidJson') + ': ' + (e as Error).message}
      return
    }
  }
  executing.value = true
  try {
    const res = await executeTool({toolName: exec.value.toolName!, args})
    resultRaw.value = res
  } catch (e) {
    resultRaw.value = {ok: false, message: t('tools.execError') + ': ' + (e as Error).toString()}
  } finally {
    executing.value = false
  }
}

function onReset() {
  if (currentSchema.value) {
    const init: Record<string, any> = {}
    const props = currentSchema.value.properties || {}
    Object.keys(props).forEach(k => {
      const p = props[k]
      if (p?.type === 'string') init[k] = ''
      else if (p?.type === 'number' || p?.type === 'integer') init[k] = undefined
      else if (p?.type === 'boolean') init[k] = false
      else if (p?.type === 'object') init[k] = {}
      else init[k] = ''
    })
    exec.value.argsObj = init
    try { exec.value.argsText = JSON.stringify(init, null, 2) } catch {}
  } else {
    exec.value = {argsText: '{"": ""}', argsObj: {}}
  }
  resultRaw.value = null
}

function pick(item: ToolListItem) {
  exec.value.toolName = item.spec.name
  // 若 schema 存在，可给出示例（此处仅放空模板，后续可增强）
}

const filteredItems = computed(() => {
  const q = (search.value || '').toLowerCase()
  if (!q) return items.value
  return items.value.filter(it => (it.spec.name || '').toLowerCase().includes(q) || (it.spec.description || '').toLowerCase().includes(q))
})

const safeArgs = computed(() => {
  if (currentSchema.value) return exec.value.argsObj
  try { return exec.value.argsText ? JSON.parse(exec.value.argsText) : {} } catch { return {} }
})

function isMcp(item: ToolListItem) {
  return (item.spec.category === 'MCP')
}

const mcpItems = computed(() => filteredItems.value.filter(it => isMcp(it)))
const systemItems = computed(() => filteredItems.value.filter(it => !isMcp(it)))

// -------- MCP 分组：client -> servers -> tools --------
const mcpClients = computed<string[]>(() => {
  const set = new Set<string>()
  for (const it of mcpItems.value) {
    const c = it.spec.mcpToolSpec?.client
    if (c) set.add(c)
  }
  return Array.from(set)
})

const serversByClient = computed<Record<string, string[]>>(() => {
  const map: Record<string, Set<string>> = {}
  for (const it of mcpItems.value) {
    const c = it.spec.mcpToolSpec?.client
    const s = it.spec.mcpToolSpec?.server
    if (!c || !s) continue
    if (!map[c]) map[c] = new Set<string>()
    map[c].add(s)
  }
  const out: Record<string, string[]> = {}
  for (const [k, v] of Object.entries(map)) out[k] = Array.from(v)
  return out
})

const selectedClient = ref<string | undefined>(undefined)
const selectedServer = ref<string | undefined>(undefined)
const serverConfigVisible = ref(false)

// 初始化/联动选中项
onMounted(() => {
  if (!selectedClient.value && mcpClients.value.length) {
    selectedClient.value = mcpClients.value[0]
  }
  if (!selectedServer.value && selectedClient.value) {
    const servers = serversByClient.value[selectedClient.value] || []
    selectedServer.value = servers[0]
  }
})

// 切换 client 时，重置 server
function selectServer(client: string, server: string) {
  selectedClient.value = client
  selectedServer.value = server
}

// 当前选中的 client/server 的 MCP 工具
const mcpToolsForSelected = computed<ToolListItem[]>(() => {
  const c = selectedClient.value
  const s = selectedServer.value
  return mcpItems.value.filter(it => (
      it.spec.mcpToolSpec?.client === c && it.spec.mcpToolSpec?.server === s
  ))
})

function openServerConfig(client?: string) {
  if (client) selectedClient.value = client
  // 默认使用当前选中的 client
  if (!selectedClient.value && mcpClients.value.length) selectedClient.value = mcpClients.value[0]
  serverConfigVisible.value = true
}

const currentServers = computed<string[]>(() => {
  const c = selectedClient.value || ''
  return serversByClient.value[c] || []
})



</script>

<style scoped lang="scss">

.tools-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 8px;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 8px;
}

.tool-item {
  margin-bottom: 16px;
  width: 100%;
}

.section {
  margin-bottom: 16px;
}

.section-title {
  font-weight: 600;
  margin: 0 0 8px;
}

/* 固定每个分区（第二行、第三行）的高度，内部滚动 */
.section-mcp .section-body {
  height: $section-height-mcp;
  border-bottom: 1px solid $border-color;
}

.section-system .section-body {
  height: $section-height-system;
}

.section-body {
  overflow: hidden;
}

.col {
  height: 95%;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.mcp-left, .mcp-right, .system-list, .executor-panel {
  height: 100%;
  overflow: auto;
  padding-right: $space-sm;
}

.servers-list, .mcp-tools-list {
  align-self: auto;
  overflow: auto;
  padding-right: $space-sm;
}

.title {
  font-size: 16px;
  font-weight: 600;
}

.desc {
  color: #666;
  font-size: 14px;
}

.cat {
  color: #1677ff;
}

.meta {
  margin-top: 8px;
}
</style>
