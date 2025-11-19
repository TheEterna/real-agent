import { createRouter, createWebHistory, RouteRecordRaw } from 'vue-router'

const routes: RouteRecordRaw[] = [
  { path: '/', redirect: '/chat' },
  {
    path: '/chat',
    components: {
      default: () => import('@/pages/chat/ChatGateway.vue'),
      sider: () => import('@/pages/chat/ChatSider.vue')
    }
  },
  { path: '/dashboard', component: () => import('@/pages/Dashboard.vue') },
  { path: '/tools', component: () => import('@/pages/Tools.vue') },
  { path: '/agents', component: () => import('@/pages/Agents.vue') },
  { path: '/workflows', component: () => import('@/pages/Workflows.vue') },
  { path: '/config', component: () => import('@/pages/Config.vue') },
  { path: '/playground', component: () => import('@/pages/playground/Playground.vue') },
  { path: '/playground/data-lab', component: () => import('@/pages/playground/DataLab.vue'), meta: { hideHeader: true }  },
  { path: '/playground/role-play-agent', component: () => import('@/pages/playground/role-play-agent/RoleSelect.vue'), meta: { hideHeader: true }  },
  { path: '/playground/role-play-agent/:roleId', component: () => import('@/pages/playground/role-play-agent/Index.vue'), meta: { hideHeader: true }  },
  { path: '/SseTest', component: () => import('@/pages/SseTest.vue') },
  // 🏺 青花瓷冰裂纹效果演示
  { path: '/demo/crackle', component: () => import('@/pages/demo/CrackleDemo.vue'), meta: { hideHeader: true } },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

export default router
