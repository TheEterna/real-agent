<script setup lang="ts">
import {reactive, ref, onMounted} from 'vue'
import {useRouter} from 'vue-router'
import {message} from 'ant-design-vue'
import {UserOutlined, LockOutlined} from '@ant-design/icons-vue'
import {authApi} from '@/api/auth'
import {useAuthStore} from '@/stores/authStore'
import { gsap } from 'gsap'
import { SendOutlined } from '@ant-design/icons-vue'

const router = useRouter()
const authStore = useAuthStore()
const loading = ref(false)
const registerLoading = ref(false)
const showLoginModal = ref(false)
const showRegisterModal = ref(false)

const features = ['🎨 创意写作', '💻 代码助手', '📊 数据分析', '🌍 多语言翻译', '🧠 逻辑推理']

// 登录表单
const formData = reactive({
  externalId: '',
  password: ''
})

// 注册表单
const registerData = reactive({
  externalId: '',
  password: '',
  nickname: ''
})

// 验证规则
const rules = {
  externalId: [{required: true, message: '请输入用户名', trigger: 'blur'}],
  password: [{required: true, message: '请输入密码', trigger: 'blur'}, {min: 6, message: '密码至少6位', trigger: 'blur'}]
}

const registerRules = {
  externalId: [{required: true, message: '请输入用户名', trigger: 'blur'}, {min: 3, message: '用户名至少3位', trigger: 'blur'}],
  password: [{required: true, message: '请输入密码', trigger: 'blur'}, {min: 6, message: '密码至少6位', trigger: 'blur'}]
}

// 交互处理
const handleInputClick = () => {
  showLoginModal.value = true
}

const switchModal = () => {
  if (showLoginModal.value) {
    showLoginModal.value = false
    showRegisterModal.value = true
  } else {
    showRegisterModal.value = false
    showLoginModal.value = true
  }
}

// 登录逻辑
const handleLogin = async () => {
  loading.value = true
  try {
    const response = await authStore.login({
      externalId: formData.externalId,
      password: formData.password
    })

    if (response.code === 200) {
      message.success('欢迎回来')
      showLoginModal.value = false
      await router.push('/chat')
    } else {
      message.error(response.message || '登录失败')
    }
  } catch (error: any) {
    console.error('登录失败:', error)
    message.error(error.response?.data?.message || error.message || '登录失败，请重试')
  } finally {
    loading.value = false
  }
}

// 注册逻辑
const handleRegister = async () => {
  registerLoading.value = true
  try {
    const response = await authApi.register({
      externalId: registerData.externalId,
      password: registerData.password,
      nickname: registerData.nickname || registerData.externalId
    })

    if (response.code === 200) {
      message.success('注册成功，请登录')
      showRegisterModal.value = false

      // 自动填充并打开登录
      formData.externalId = registerData.externalId
      formData.password = registerData.password
      showLoginModal.value = true

      // 清空注册
      registerData.externalId = ''
      registerData.password = ''
      registerData.nickname = ''
    } else {
      message.error(response.message || '注册失败')
    }
  } catch (error: any) {
    console.error('注册失败:', error)
    message.error(error.response?.data?.message || error.message || '注册失败，请重试')
  } finally {
    registerLoading.value = false
  }
}

// 入场动画
onMounted(() => {
  const tl = gsap.timeline()

  tl.fromTo('.landing-title-animation',
      { opacity: 0, y: 20 },
      { opacity: 1, y: 0, duration: 0.8, ease: 'power2.out' }
  )
      .fromTo('.landing-input-animation',
          { opacity: 0, scale: 0.95 },
          { opacity: 1, scale: 1, duration: 0.6, ease: 'back.out(1.2)' },
          '-=0.4'
      )
      .fromTo('.landing-tags-animation > div',
          { opacity: 0, y: 10 },
          { opacity: 1, y: 0, duration: 0.4, stagger: 0.1, ease: 'power2.out' },
          '-=0.2'
      )
})
</script>
<template>
  <!-- 页面容器：Landing Page 风格 -->
  <div class="min-h-screen flex flex-col bg-white relative overflow-hidden">
    
    <!-- 顶部导航栏 -->
    <header class="
      w-full h-16 
      flex items-center justify-between 
      px-6 md:px-10 
      fixed top-0 left-0 z-10
      bg-white/80 backdrop-blur-md
    ">
      <!-- Logo -->
      <div class="flex items-center gap-2 cursor-pointer">
        <div class="
          w-10 h-10
          flex items-center justify-center
          text-2xl
          bg-gradient-to-br from-primary-50 to-primary-75
          rounded-xl
          border border-primary-200
        ">
          <img src="/logo.png">
        </div>
        <span class="text-lg font-bold text-slate-800 tracking-tight">Real Agent</span>
      </div>

      <!-- 右侧按钮 -->
      <div class="flex items-center gap-3">
        <a-button 
          type="text" 
          class="text-slate-600 hover:text-slate-900 font-medium"
          @click="showRegisterModal = true"
        >
          注册
        </a-button>
        <a-button 
          type="primary" 
          class="
            bg-slate-900 hover:bg-slate-800 
            text-white font-medium 
            rounded-full px-5 h-9
            border-none shadow-none
          "
          @click="showLoginModal = true"
        >
          登录
        </a-button>
      </div>
    </header>

    <!-- 主内容区 -->
    <main class="flex-1 flex flex-col items-center justify-center px-4 w-full max-w-4xl mx-auto mt-16">
      
      <!-- 欢迎标题 -->
      <div class="text-center mb-12 landing-title-animation">
        <h1 class="
          text-4xl md:text-5xl lg:text-6xl 
          font-bold text-slate-900 
          mb-4 tracking-tight
        ">
          有什么可以帮忙的？
        </h1>
        <p class="text-lg text-slate-500 font-normal">
          Real Agent，你的私人 AI 助手
        </p>
      </div>

      <!-- 模拟聊天输入框 -->
      <div 
        class="
          w-full max-w-2xl 
          relative 
          group 
          cursor-text
          landing-input-animation
        "
        @click="handleInputClick"
      >
        <div class="
          w-full h-14 md:h-16 
          bg-white 
          rounded-3xl 
          border border-slate-200 
          shadow-[0_4px_20px_rgba(0,0,0,0.05)]
          flex items-center px-6 
          transition-all duration-300
          group-hover:shadow-[0_8px_30px_rgba(0,0,0,0.08)]
          group-hover:border-slate-300
        ">
          <span class="text-slate-400 text-lg">询问任何问题...</span>
          
          <!-- 发送按钮图标 -->
          <div class="absolute right-4 top-1/2 -translate-y-1/2">
            <div class="
              w-8 h-8 md:w-10 md:h-10 
              bg-slate-100 
              rounded-full flex cursor-pointer
              text-slate-400
              transition-colors duration-300
              group-hover:bg-slate-900 group-hover:text-white
            ">
              <SendOutlined class="rotate-335 m-auto pl-1 text-lg"></SendOutlined>
            </div>
          </div>
        </div>
      </div>

      <!-- 底部功能标签 -->
      <div class="mt-8 flex flex-wrap justify-center gap-3 landing-tags-animation">
        <div v-for="(tag, index) in features" :key="index" class="
          px-4 py-2 
          bg-slate-50 
          rounded-full 
          text-sm text-slate-600 
          border border-slate-100
          cursor-pointer
          transition-colors hover:bg-slate-100
        " @click="showLoginModal = true">
          {{ tag }}
        </div>
      </div>

    </main>

    <!-- 底部版权 -->
    <footer class="w-full py-6 text-center text-xs text-slate-400">
      © 2025 Real Agent. All rights reserved.
    </footer>

    <!-- 登录 Modal -->
    <a-modal
      v-model:open="showLoginModal"
      :footer="null"
      :width="400"
      class="auth-modal"
      centered
    >
      <div class="px-4 py-6">
        <div class="text-center mb-8">
          <h2 class="text-2xl font-bold text-slate-800">欢迎回来</h2>
          <p class="text-slate-500 mt-2">登录以继续使用 Real Agent</p>
        </div>

        <a-form
          :model="formData"
          :rules="rules"
          @finish="handleLogin"
          layout="vertical"
          class="space-y-4"
        >
          <a-form-item label="用户名" name="externalId">
            <a-input
              v-model:value="formData.externalId"
              size="large"
              placeholder="请输入用户名"
              :disabled="loading"
              class="input-custom"
            >
              <template #prefix>
                <UserOutlined class="text-primary-400"/>
              </template>
            </a-input>
          </a-form-item>

          <a-form-item label="密码" name="password">
            <a-input-password
              v-model:value="formData.password"
              size="large"
              placeholder="请输入密码"
              :disabled="loading"
              class="input-custom"
            >
              <template #prefix>
                <LockOutlined class="text-primary-400"/>
              </template>
            </a-input-password>
          </a-form-item>

          <a-form-item class="mb-0 pt-2">
            <a-button
              type="primary"
              html-type="submit"
              size="large"
              block
              :loading="loading"
              class="
                h-11 text-base font-semibold
                rounded-lg border-none
                bg-slate-900 hover:bg-slate-800
                shadow-lg shadow-slate-900/20
              "
            >
              {{ loading ? '登录中...' : '登录' }}
            </a-button>
          </a-form-item>

          <div class="text-center mt-6 text-sm text-slate-600">
            <span>还没有账号？</span>
            <a 
              @click="switchModal" 
              class="text-primary-600 font-medium hover:text-primary-700 cursor-pointer"
            >
              立即注册
            </a>
          </div>
        </a-form>
      </div>
    </a-modal>

    <!-- 注册 Modal -->
    <a-modal
      v-model:open="showRegisterModal"
      :footer="null"
      :width="400"
      class="auth-modal"
      centered
    >
      <div class="px-4 py-6">
        <div class="text-center mb-8">
          <h2 class="text-2xl font-bold text-slate-800">创建账号</h2>
          <p class="text-slate-500 mt-2">开始您的 AI 之旅</p>
        </div>

        <a-form
          :model="registerData"
          :rules="registerRules"
          @finish="handleRegister"
          layout="vertical"
          class="space-y-4"
        >
          <a-form-item label="用户名" name="externalId">
            <a-input
              v-model:value="registerData.externalId"
              size="large"
              placeholder="请输入用户名"
              :disabled="registerLoading"
              class="input-custom"
            />
          </a-form-item>

          <a-form-item label="密码" name="password">
            <a-input-password
              v-model:value="registerData.password"
              size="large"
              placeholder="至少6位"
              :disabled="registerLoading"
              class="input-custom"
            />
          </a-form-item>

          <a-form-item label="昵称" name="nickname">
            <a-input
              v-model:value="registerData.nickname"
              size="large"
              placeholder="可选"
              :disabled="registerLoading"
              class="input-custom"
            />
          </a-form-item>

          <a-form-item class="mb-0 pt-2">
            <a-button
              type="primary"
              html-type="submit"
              size="large"
              block
              :loading="registerLoading"
              class="
                h-11 text-base font-semibold
                rounded-lg border-none
                bg-slate-900 hover:bg-slate-800
                shadow-lg shadow-slate-900/20
              "
            >
              {{ registerLoading ? '注册中...' : '注册' }}
            </a-button>
          </a-form-item>
          
          <div class="text-center mt-6 text-sm text-slate-600">
            <span>已有账号？</span>
            <a 
              @click="switchModal" 
              class="text-primary-600 font-medium hover:text-primary-700 cursor-pointer"
            >
              立即登录
            </a>
          </div>
        </a-form>
      </div>
    </a-modal>

  </div>
</template>

<style scoped lang="scss">
// 覆盖 Ant Design Modal 样式
:deep(.auth-modal .ant-modal-content) {
  @apply rounded-2xl overflow-hidden p-0;
}

:deep(.auth-modal .ant-modal-close) {
  @apply top-4 right-4;
}

// 输入框样式覆盖
:deep(.ant-input),
:deep(.ant-input-password .ant-input) {
  @apply 
    rounded-lg 
    border border-slate-200
    px-3 py-2.5
    text-sm
    bg-slate-50
    transition-all duration-300
    focus:bg-white
    focus:border-primary-500 
    focus:shadow-[0_0_0_3px_rgba(107,154,152,0.1)]
    focus:outline-none;
  
  &::placeholder {
    @apply text-slate-400;
  }
}

:deep(.ant-input-affix-wrapper) {
  @apply 
    rounded-lg
    border border-slate-200
    bg-slate-50
    transition-all duration-300
    focus-within:bg-white
    focus-within:border-primary-500 
    focus-within:shadow-[0_0_0_3px_rgba(107,154,152,0.1)];
}

:deep(.ant-form-item-label > label) {
  @apply font-medium text-slate-700 text-sm;
}
</style>
