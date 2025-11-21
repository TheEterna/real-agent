<template>
  <div class="login-container bg-primary-100">
    <div class="login-card">
      <div class="login-header">
        <h1 class="login-title">Real Agent</h1>
        <p class="login-subtitle">智能对话助手平台</p>
      </div>

      <a-form
          :model="formData"
          :rules="rules"
          @finish="handleLogin"
          layout="vertical"
          class="login-form"
      >
        <a-form-item label="用户名" name="externalId">
          <a-input
              v-model:value="formData.externalId"
              size="large"
              placeholder="请输入用户名"
              :disabled="loading"
          >
            <template #prefix>
              <UserOutlined />
            </template>
          </a-input>
        </a-form-item>

        <a-form-item label="密码" name="password">
          <a-input-password
              v-model:value="formData.password"
              size="large"
              placeholder="请输入密码"
              :disabled="loading"
          >
            <template #prefix>
              <LockOutlined />
            </template>
          </a-input-password>
        </a-form-item>

        <a-form-item>
          <a-button
              type="primary"
              html-type="submit"
              size="large"
              block
              :loading="loading"
              class="login-button"
          >
            {{ loading ? '登录中...' : '登录' }}
          </a-button>
        </a-form-item>

        <div class="login-footer">
          <span>还没有账号？</span>
          <a @click="showRegisterModal = true" class="register-link">立即注册</a>
        </div>
      </a-form>
    </div>

    <!-- 注册弹窗 -->
    <a-modal
        v-model:open="showRegisterModal"
        title="注册新账号"
        :footer="null"
        :maskClosable="false"
    >
      <a-form
          :model="registerData"
          :rules="registerRules"
          @finish="handleRegister"
          layout="vertical"
      >
        <a-form-item label="用户名" name="externalId">
          <a-input
              v-model:value="registerData.externalId"
              size="large"
              placeholder="请输入用户名"
              :disabled="registerLoading"
          />
        </a-form-item>

        <a-form-item label="密码" name="password">
          <a-input-password
              v-model:value="registerData.password"
              size="large"
              placeholder="请输入密码（至少6位）"
              :disabled="registerLoading"
          />
        </a-form-item>

        <a-form-item label="昵称" name="nickname">
          <a-input
              v-model:value="registerData.nickname"
              size="large"
              placeholder="请输入昵称（可选）"
              :disabled="registerLoading"
          />
        </a-form-item>

        <a-form-item>
          <a-button
              type="primary"
              html-type="submit"
              size="large"
              block
              :loading="registerLoading"
          >
            {{ registerLoading ? '注册中...' : '注册' }}
          </a-button>
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import {reactive, ref} from 'vue'
import {useRouter} from 'vue-router'
import {message} from 'ant-design-vue'
import {UserOutlined, LockOutlined} from '@ant-design/icons-vue'
import {authApi} from '@/api/auth'
import {useAuthStore} from '@/stores/authStore'

const router = useRouter()
const authStore = useAuthStore()
const loading = ref(false)
const registerLoading = ref(false)
const showRegisterModal = ref(false)

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

// 登录表单验证规则
const rules = {
  externalId: [
    {required: true, message: '请输入用户名', trigger: 'blur'}
  ],
  password: [
    {required: true, message: '请输入密码', trigger: 'blur'},
    {min: 6, message: '密码至少6位', trigger: 'blur'}
  ]
}

// 注册表单验证规则
const registerRules = {
  externalId: [
    {required: true, message: '请输入用户名', trigger: 'blur'},
    {min: 3, message: '用户名至少3位', trigger: 'blur'}
  ],
  password: [
    {required: true, message: '请输入密码', trigger: 'blur'},
    {min: 6, message: '密码至少6位', trigger: 'blur'}
  ]
}

// 处理登录
const handleLogin = async () => {
  loading.value = true
  try {
    const response = await authStore.login({
      externalId: formData.externalId,
      password: formData.password
    })

    if (response.code === 200) {
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

// 处理注册
const handleRegister = async () => {
  registerLoading.value = true
  try {
    const response = await authApi.register({
      externalId: registerData.externalId,
      password: registerData.password,
      nickname: registerData.nickname || registerData.externalId
    })

    console.log('[Register] 收到响应:', response)
    console.log('[Register] 响应类型:', typeof response)
    console.log('[Register] response.code:', response.code)
    console.log('[Register] response.data:', response.data)

    if (response.code === 200) {
      message.success('注册成功，请登录')
      showRegisterModal.value = false

      // 填充登录表单
      formData.externalId = registerData.externalId
      formData.password = registerData.password

      // 清空注册表单
      registerData.externalId = ''
      registerData.password = ''
      registerData.nickname = ''
    } else {
      message.error(response.message || '注册失败')
    }
  } catch (error: any) {
    console.error('注册失败:', error)
    console.error('error.response:', error.response)
    console.error('error.response.data:', error.response?.data)
    message.error(error.response?.data?.message || error.message || '注册失败，请重试')
  } finally {
    registerLoading.value = false
  }
}
</script>

<style scoped lang="scss">
.login-container {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
}

.login-card {
  width: 100%;
  max-width: 420px;
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(10px);
  border-radius: 20px;
  padding: 40px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);
}

.login-header {
  text-align: center;
  margin-bottom: 32px;
}

.login-title {
  font-size: 32px;
  font-weight: 700;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  margin-bottom: 8px;
}

.login-subtitle {
  font-size: 14px;
  color: #666;
  margin: 0;
}

.login-form {
  margin-top: 24px;
}

.login-button {
  height: 44px;
  font-size: 16px;
  font-weight: 600;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border: none;
  border-radius: 8px;
  margin-top: 8px;

  &:hover {
    background: linear-gradient(135deg, #5568d3 0%, #6a3f8f 100%);
  }
}

.login-footer {
  text-align: center;
  margin-top: 20px;
  font-size: 14px;
  color: #666;
}

.register-link {
  color: #667eea;
  font-weight: 600;
  cursor: pointer;
  margin-left: 8px;

  &:hover {
    color: #5568d3;
    text-decoration: underline;
  }
}

:deep(.ant-input-affix-wrapper) {
  border-radius: 8px;
}

:deep(.ant-input) {
  border-radius: 8px;
}

:deep(.ant-form-item-label > label) {
  font-weight: 600;
}
</style>
