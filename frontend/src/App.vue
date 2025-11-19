<template>

  <a-config-provider
      :theme="{
      token: {
    'colorPrimaryBg': '#f0f7f7',
    'colorPrimaryBgHover': '#e4f1f1',
    'colorPrimaryBorder': '#d8ebea',
    'colorPrimaryBorderHover': '#b8ddd9',
    'colorPrimaryHover': 'rgba(80,200,183,0.9)',
    'colorPrimary': 'rgba(109,184,172,0.9)',
    'colorInfo': '#00bac4',
    'colorSuccess': '#52c41a',
    'colorText': 'rgba(51, 65, 85, 0.9)',
    'colorTextSecondary': 'rgba(51, 65, 85, 0.75)',
    'colorTextTertiary': 'rgba(51, 65, 85, 0.5)',
    'colorTextQuaternary': 'rgba(51, 65, 85, 0.3)'
  },
    }"
  >

    <a-layout v-if="!isStandalone" style="overflow: hidden;min-height: 100vh;height: 100vh;">
      <a-layout-sider
          theme="light"
          :width="300"
          :collapsed-width="0"
          :collapsed="effectiveCollapsed"
          :collapsible="false"
      >
        <div class="flex flex-col bg-primary-50 h-full p-4.5">
          <div class="sider-top">
            <button class="collapse-btn rounded-br-full" @click="toggleCollapse" :title="collapsed ? '展开' : '折叠'" @mouseenter="onMouseEnter" @mouseleave="onMouseLeave">
              <MenuFoldOutlined />
            </button>
            <div class="brand" v-show="!effectiveCollapsed">Real Agent</div>
            <div class="lang" v-show="!effectiveCollapsed">
              <a-select
                  v-model:value="currentLocale"
                  style="width: 100%"
                  @change="changeLanguage"
                  size="small"
              >
                <a-select-option value="zh">中文</a-select-option>
                <a-select-option value="en">English</a-select-option>
              </a-select>
            </div>
          </div>

          <!-- per-page sidebar content -->
          <router-view name="sider" v-show="!effectiveCollapsed" />

          <div class="sider-spacer"></div>

          <div :class="['sider-bottom', { 'sider-bottom--collapsed': effectiveCollapsed }]" v-show="!effectiveCollapsed">
            <a-tooltip :title="t('menu.chat')" placement="right">
              <router-link class="nav-item rounded-4xl" to="/chat" v-slot="{ isActive, href, navigate }">
                <a :href="href" @click="navigate"
                   :class="[
                     'flex items-center gap-2 px-3 py-2 rounded-4xl transition-colors',
                     isActive ? 'bg-primary-100' : 'bg-primary-75/35 hover:bg-primary-900/10'
                   ]">
                  <span class="inline-flex items-center justify-center w-7 h-7 rounded-full" :style="{ backgroundColor: iconBg.chat }"><MessageOutlined /></span>
                  <span class="font-semibold text-slate-700">{{ t('menu.chat') }}</span>
                </a>
              </router-link>
            </a-tooltip>
            <a-tooltip :title="t('menu.dashboard')" placement="right">
              <router-link class="nav-item rounded-4xl" to="/dashboard" v-slot="{ isActive, href, navigate }">
                <a :href="href" @click="navigate"
                   :class="[
                     'flex items-center gap-2 px-3 py-2 rounded-4xl transition-colors',
                     isActive ? 'bg-primary-100' : 'bg-primary-75/35 hover:bg-primary-900/10'
                   ]">
                  <span class="inline-flex items-center justify-center w-7 h-7 rounded-full" :style="{ backgroundColor: iconBg.dashboard }"><DashboardOutlined /></span>
                  <span class="font-semibold text-slate-700">{{ t('menu.dashboard') }}</span>
                </a>
              </router-link>
            </a-tooltip>
            <a-tooltip :title="t('menu.tools')" placement="right">
              <router-link class="nav-item rounded-4xl" to="/tools" v-slot="{ isActive, href, navigate }">
                <a :href="href" @click="navigate"
                   :class="[
                     'flex items-center gap-2 px-3 py-2 rounded-4xl transition-colors',
                     isActive ? 'bg-primary-100' : 'bg-primary-75/35 hover:bg-primary-900/10'
                   ]">
                  <span class="inline-flex items-center justify-center w-7 h-7 rounded-full" :style="{ backgroundColor: iconBg.tools }"><ToolOutlined /></span>
                  <span class="font-semibold text-slate-700">{{ t('menu.tools') }}</span>
                </a>
              </router-link>
            </a-tooltip>
            <a-tooltip :title="t('menu.agents')" placement="right">
              <router-link class="nav-item rounded-4xl" to="/agents" v-slot="{ isActive, href, navigate }">
                <a :href="href" @click="navigate"
                   :class="[
                     'flex items-center gap-2 px-3 py-2 rounded-4xl transition-colors',
                     isActive ? 'bg-primary-100' : 'bg-primary-75/35 hover:bg-primary-900/10'
                   ]">
                  <span class="inline-flex items-center justify-center w-7 h-7 rounded-full" :style="{ backgroundColor: iconBg.agents }"><RobotOutlined /></span>
                  <span class="font-semibold text-slate-700">{{ t('menu.agents') }}</span>
                </a>
              </router-link>
            </a-tooltip>
            <a-tooltip :title="t('menu.workflows')" placement="right">
              <router-link class="nav-item rounded-4xl" to="/workflows" v-slot="{ isActive, href, navigate }">
                <a :href="href" @click="navigate"
                   :class="[
                     'flex items-center gap-2 px-3 py-2 rounded-4xl transition-colors',
                     isActive ? 'bg-primary-100' : 'bg-primary-75/35 hover:bg-primary-900/10'
                   ]">
                  <span class="inline-flex items-center justify-center w-7 h-7 rounded-full" :style="{ backgroundColor: iconBg.workflows }"><ApartmentOutlined /></span>
                  <span class="font-semibold text-slate-700">{{ t('menu.workflows') }}</span>
                </a>
              </router-link>
            </a-tooltip>
            <a-tooltip :title="t('menu.config')" placement="right">
              <router-link class="nav-item rounded-4xl" to="/config" v-slot="{ isActive, href, navigate }">
                <a :href="href" @click="navigate"
                   :class="[
                     'flex items-center gap-2 px-3 py-2 rounded-4xl transition-colors',
                     isActive ? 'bg-primary-100' : 'bg-primary-75/35 hover:bg-primary-900/10'
                   ]">
                  <span class="inline-flex items-center justify-center w-7 h-7 rounded-full" :style="{ backgroundColor: iconBg.config }"><SettingOutlined /></span>
                  <span class="font-semibold text-slate-700">{{ t('menu.config') }}</span>
                </a>
              </router-link>
            </a-tooltip>
            <a-tooltip :title="t('menu.playground')" placement="right">
              <router-link class="nav-item rounded-4xl" to="/playground" v-slot="{ isActive, href, navigate }">
                <a :href="href" @click="navigate"
                   :class="[
                     'flex items-center gap-2 px-3 py-2 transition-colors',
                     isActive ? 'bg-primary-100' : 'bg-primary-75/35 hover:bg-primary-900/10'
                   ]">
                  <span class="inline-flex items-center justify-center w-7 h-7 rounded-full" :style="{ backgroundColor: iconBg.playground }"><ExperimentOutlined /></span>
                  <span class="font-semibold text-slate-700">{{ t('menu.playground') }}</span>
                </a>
              </router-link>
            </a-tooltip>
          </div>
        </div>
      </a-layout-sider>
      <!-- Collapsed floating mini bar (expand button + horizontal icons) -->
      <div v-if="effectiveCollapsed" class="sider-mini" aria-label="quick apps">
        <button class="collapse-btn mini" @click="toggleCollapse" :title="'展开'" @mouseenter="onMouseEnter" @mouseleave="onMouseLeave">
          <MenuUnfoldOutlined />
        </button>
        <div class="mini-icons">
          <a-tooltip :title="t('menu.chat')" placement="top">
            <router-link class="mini-icon" to="/chat"><MessageOutlined /></router-link>
          </a-tooltip>
          <a-tooltip :title="t('menu.dashboard')" placement="top">
            <router-link class="mini-icon" to="/dashboard"><DashboardOutlined /></router-link>
          </a-tooltip>
          <a-tooltip :title="t('menu.tools')" placement="top">
            <router-link class="mini-icon" to="/tools"><ToolOutlined /></router-link>
          </a-tooltip>
          <a-tooltip :title="t('menu.agents')" placement="top">
            <router-link class="mini-icon" to="/agents"><RobotOutlined /></router-link>
          </a-tooltip>
          <a-tooltip :title="t('menu.workflows')" placement="top">
            <router-link class="mini-icon" to="/workflows"><ApartmentOutlined /></router-link>
          </a-tooltip>
          <a-tooltip :title="t('menu.config')" placement="top">
            <router-link class="mini-icon" to="/config"><SettingOutlined /></router-link>
          </a-tooltip>
          <!--        <a-tooltip :title="t('menu.logs')" placement="top">-->
          <!--          <router-link class="mini-icon" to="/logs"><FileTextOutlined /></router-link>-->
          <!--        </a-tooltip>-->
          <a-tooltip :title="t('menu.playground')" placement="top">
            <router-link class="mini-icon" to="/playground"><ExperimentOutlined /></router-link>
          </a-tooltip>
        </div>
      </div>

      <a-layout class="bg-primary-0">
        <a-layout-content style="position: relative; overflow: auto; display: flex;">
          <router-view class="bg-white"/>
        </a-layout-content>
      </a-layout>
    </a-layout>

    <div v-else style="min-height: 100vh; max-height: 100vh;height: 100vh;">
      <router-view />
    </div>
  </a-config-provider>

</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRoute } from 'vue-router'
import { useI18n } from 'vue-i18n'
import {
  MenuUnfoldOutlined,
  MenuFoldOutlined,
  MessageOutlined,
  DashboardOutlined,
  ToolOutlined,
  RobotOutlined,
  ApartmentOutlined,
  SettingOutlined,
  FileTextOutlined,
  ExperimentOutlined,
} from '@ant-design/icons-vue'
import { getRandomGlassColor } from '@/utils/colorUtils'

const route = useRoute()
const { t, locale } = useI18n()

const currentLocale = ref(locale.value || 'zh')
const collapsed = ref(false)

const changeLanguage = (lang: string) => {
  locale.value = lang
}

const getCurrentMenu = () => {
  const path = route.path
  if (path === '/') return 'chat'
  return path.substring(1)
}

const isStandalone = computed(() => Boolean(route.meta && (route.meta as any).standalone))
// 悬停自动展开：折叠状态下鼠标移入暂时展开
const effectiveCollapsed = computed(() => collapsed.value )

const onMouseEnter = () => {
  if (!collapsed.value) return
}

const onMouseLeave = () => {
  if (!collapsed.value) return
}

const toggleCollapse = () => {
  // 点击切换时取消悬停展开，避免视觉抖动
  collapsed.value = !collapsed.value
}

// 随机图标背景色（玻璃色），进入时生成一次即可
const iconBg = {
  chat: getRandomGlassColor(),
  dashboard: getRandomGlassColor(),
  tools: getRandomGlassColor(),
  agents: getRandomGlassColor(),
  workflows: getRandomGlassColor(),
  config: getRandomGlassColor(),
  playground: getRandomGlassColor(),
}
</script>

<style scoped lang="scss">
@use '@/styles/variables.scss' as v;

:deep(.ant-btn){
  span {
    svg {
      display: block !important;
    }
  }
}
.brand{font-weight:700;font-size:18px;padding:8px 8px 0 8px;white-space:nowrap;overflow:hidden;text-overflow:ellipsis;line-height:24px;height:54px}
.sider-top{display:grid;grid-auto-rows:min-content;gap:8px}
.collapse-btn{width:32px;height:32px;border:1px solid #e6eaf0;background:#fff;border-radius:8px;display:flex;align-items:center;justify-content:center;padding:0;cursor:pointer;transition:background-color .2s ease}
.collapse-btn:hover{background:#f6f9ff}
.lang{padding:0 0 8px}
.sider-spacer{flex:1}
.sider-bottom{display: flex; flex-direction: column; gap: 6px; transition: all .25s ease}
.sider-bottom--collapsed{display:flex;gap:8px;padding:6px;border:1px solid #eef2f7;background:#fff;border-radius:16px;justify-content:center;align-items:center}

.nav-item{
  background: rgba(v.$primary-color-75, .35);
}
.nav-item:hover{ background: rgba(v.$primary-color-900, 0.08); }
.nav-item.router-link-active{ background: v.$primary-color-100!important; }

.nav-item__inner{ display:flex; align-items:center; gap: 10px; }
.icon-circle{
  width: 28px; height: 28px;
  border-radius: 999px;
  display:flex; align-items:center; justify-content:center;
}
.nav-label{ font-weight: 600; }


/* Floating mini bar when collapsed */
.sider-mini{position:fixed;left:8px;bottom:12px;z-index:20;display:flex;gap:8px;align-items:center;background:#fff;border:1px solid #eef2f7;border-radius:18px;padding:6px 8px;box-shadow:0 4px 20px rgba(0,0,0,.06);transition:transform .25s ease, opacity .25s ease}
.sider-mini .mini{width:28px;height:28px;border:1px solid #e6eaf0;background:#fff;border-radius:8px;display:flex;align-items:center;justify-content:center;padding:0;cursor:pointer}
.mini-icons{display:flex;gap:8px}
.mini-icon{display:flex;align-items:center;justify-content:center;width:28px;height:28px;border-radius:999px;background:transparent;border:0;color:#222;text-decoration:none;transition:background-color .2s ease}
.mini-icon:hover{background:#f5faff}
.mini-icon.router-link-active{background:#f0f7ff;color:#1677ff}
</style>
