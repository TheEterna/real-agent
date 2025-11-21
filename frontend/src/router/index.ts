import { createRouter, createWebHistory, RouteRecordRaw } from 'vue-router'
import { useAuthStore } from '@/stores/authStore'

const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/pages/Login.vue'),
    meta: { standalone: true, requiresAuth: false }
  },
  { path: '/', redirect: '/chat' },
  {
    path: '/chat',
    components: {
      default: () => import('@/pages/chat/ChatGateway.vue'),
      sider: () => import('@/pages/chat/ChatSider.vue')
    },
    meta: { requiresAuth: true }
  },
  { path: '/dashboard', component: () => import('@/pages/Dashboard.vue'), meta: { requiresAuth: true } },
  { path: '/tools', component: () => import('@/pages/Tools.vue'), meta: { requiresAuth: true } },
  { path: '/agents', component: () => import('@/pages/Agents.vue'), meta: { requiresAuth: true } },
  { path: '/workflows', component: () => import('@/pages/Workflows.vue'), meta: { requiresAuth: true } },
  { path: '/config', component: () => import('@/pages/Config.vue'), meta: { requiresAuth: true } },
  { path: '/playground', component: () => import('@/pages/playground/Playground.vue'), meta: { requiresAuth: true } },
  { path: '/playground/data-lab', component: () => import('@/pages/playground/DataLab.vue'), meta: { hideHeader: true, requiresAuth: true } },
  { path: '/playground/role-play-agent', component: () => import('@/pages/playground/role-play-agent/RoleSelect.vue'), meta: { hideHeader: true, requiresAuth: true } },
  { path: '/playground/role-play-agent/:roleId', component: () => import('@/pages/playground/role-play-agent/Index.vue'), meta: { hideHeader: true, requiresAuth: true } },
  { path: '/SseTest', component: () => import('@/pages/SseTest.vue'), meta: { requiresAuth: true } },
  // 🏺 青花瓷冰裂纹效果演示
  { path: '/demo/crackle', component: () => import('@/pages/demo/CrackleDemo.vue'), meta: { hideHeader: true } },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

// 全局路由守卫 - 鉴权检查
router.beforeEach((to, from, next) => {
  // 检查路由是否需要认证
  const requiresAuth = to.matched.some(record => record.meta.requiresAuth !== false)

  // 从 authStore 获取认证状态
  const authStore = useAuthStore()
  const hasToken = authStore.isAuthenticated

  if (requiresAuth && !hasToken) {
    // 需要认证但没有token，跳转到登录页
    next({ name: 'Login', query: { redirect: to.fullPath } })
  } else if (to.name === 'Login' && hasToken) {
    // 已登录用户访问登录页，跳转到主页
    next({ path: '/' })
  } else {
    next()
  }
})

export default router

