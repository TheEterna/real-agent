<template>
  <div class="role-select">
    <header class="hero">
      <div class="hero-main">
        <div class="hero-actions">
          <a-button type="default" shape="circle" @click="goBack" :title="'返回 Playground'">
            <template #icon>
              <ArrowLeftOutlined />
            </template>
          </a-button>
        </div>
        <div class="hero-content">
          <div class="hero-title">选择一个角色</div>
          <div class="hero-subtitle">选择您喜欢的 AI 角色，开始沉浸式的对话体验。</div>
        </div>
      </div>
    </header>

    <main class="workspace">
        <a-spin size="large" class="loading" tip="加载角色中..." v-if="loading" />
        <a-alert v-else-if="error" type="error" :message="error" show-icon />
        <a-empty v-else-if="roles.length === 0" description="暂无可用角色" />

        <a-list
            v-else
            :data-source="roles"
            :grid="{ gutter: [16, 20], column: column }"
            class="role-list"
        >
          <template #renderItem="{ item }">
            <a-list-item>
              <a-card
                  hoverable
                  class="role-card"
                  @click="enter(item)"
                  :aria-label="`选择角色：${item.name}`"
                  tabindex="0"
                  @keydown.enter="enter(item)"
              >
                <div class="role-item">
                  <div class="role-avatar">
                    <img
                        :src="item.avatar || fallbackAvatar"
                        alt="角色头像"
                    />
                  </div>
                  <div class="role-info">
                    <div class="role-name">{{ item.name }}</div>
                    <div class="role-desc">{{ item.desc || '这个角色正等你创造故事。' }}</div>
                  </div>
                </div>
              </a-card>
            </a-list-item>
          </template>
        </a-list>
    </main>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ArrowLeftOutlined } from '@ant-design/icons-vue'
import { useRoleStore, type Role } from '@/stores/roleStore'
import useBreakpoint from "ant-design-vue/es/_util/hooks/useBreakpoint";

const router = useRouter()
const goBack = () => router.push('/playground')

const roleStore = useRoleStore()
const roles = computed(() => roleStore.roles)
const loading = computed(() => roleStore.loading)
const error = computed(() => roleStore.error || '')
const fallbackAvatar = '/default-avatar.png'

onMounted(() => {
  if (!roleStore.loaded) {
    roleStore.loadRoles().catch(err => {
      console.error('[RoleSelect] 加载角色失败', err)
    })
  }
})

// 更精细的响应式列数控制
const breakpoint = useBreakpoint()
const column = computed(() => {
  const screens = breakpoint.value
  if (screens.xxxl) return 4
  if (screens.xxl) return 4
  if (screens.xl) return 3
  if (screens.lg) return 3
  if (screens.md) return 3
  if (screens.sm) return 2
  if (screens.xs) return 1
  return 3
})

function enter(role: Role) {
  router.push(`/playground/role-play-agent/${role.id}`)
}

</script>

<style scoped lang="scss">
.role-select {
  width: 100%;
  background: linear-gradient( #ffffff 100%,  #d2d7ff 50%, #ffebeb 0%);
  display: flex;
  flex-direction: column;
  height: 100%;
  overflow: hidden;
}

// Hero 区域样式
.hero {
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(10px);
  border-bottom: 1px solid rgba(255, 255, 255, 0.2);
  padding: 24px;
  margin-bottom: 24px;
}

.hero-main {
  max-width: 1200px;
  margin: 0 auto;
  display: flex;
  align-items: center;
  gap: 24px;
}

.hero-actions {
  flex-shrink: 0;
}

.hero-content {
  flex: 1;
  text-align: center;
}

.hero-title {
  font-size: 32px;
  font-weight: 700;
  color: #1a1a1a;
  margin-bottom: 8px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.hero-subtitle {
  font-size: 16px;
  color: #666;
  line-height: 1.5;
}

// 工作区域
.workspace {
  flex: 1;
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 24px 24px;
  width: 100%;
  text-align: center;
  overflow: auto;
}


.loading {
  margin: 100px auto;

}

.role-list {
  text-align: center;
}

// 角色卡片样式
.role-card {
  cursor: pointer;
  transition: all 0.3s ease;
  height: 210px; // 固定高度确保一致性
  border-radius: 16px;
  border: 1px solid rgba(255, 255, 255, 0.2);
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(10px);
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.1);

  &:hover {
    transform: translateY(-4px);
    box-shadow: 0 8px 30px rgba(0, 0, 0, 0.15);
    border-color: rgba(102, 126, 234, 0.3);
  }

  :deep(.ant-card-body) {
    padding: 20px;
    height: 100%;
    display: flex;
    flex-direction: column;
  }
}

.role-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  height: 100%;
  gap: 16px;
}

.role-avatar {
  flex-shrink: 0;
  width: 80px;
  height: 80px;
  border-radius: 50%;
  overflow: hidden;
  border: 3px solid rgba(102, 126, 234, 0.2);
  transition: border-color 0.3s ease;

  img {
    width: 100%;
    height: 100%;
    object-fit: cover;
  }
}

.role-card:hover .role-avatar {
  border-color: rgba(102, 126, 234, 0.5);
}

.role-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  min-height: 0; // 允许内容收缩
}

.role-name {
  font-size: 16px;
  font-weight: 600;
  color: #1a1a1a;
  margin-bottom: 8px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  width: 100%;
}

.role-desc {
  font-size: 13px;
  color: #666;
  line-height: 1.4;
  display: -webkit-box;
  -webkit-line-clamp: 3; // 最多显示3行
  -webkit-box-orient: vertical;
  overflow: hidden;
  text-overflow: ellipsis;
  flex: 1;
  width: 100%;
}

// 响应式设计
@media (max-width: 768px) {
  .hero {
    padding: 16px;
    margin-bottom: 16px;
  }

  .hero-main {
    flex-direction: column;
    gap: 16px;
    text-align: center;
  }

  .hero-title {
    font-size: 24px;
  }

  .hero-subtitle {
    font-size: 14px;
  }

  .workspace {
    padding: 0 16px 16px;
  }

  .role-card {
    height: 180px;
  }

  .role-avatar {
    width: 60px;
    height: 60px;
  }

  .role-name {
    font-size: 14px;
  }

  .role-desc {
    font-size: 12px;
    -webkit-line-clamp: 2;
  }
}

@media (max-width: 480px) {
  .hero {
    padding: 12px;
  }

  .workspace {
    padding: 0 12px 12px;
  }

  .role-card {
    height: 160px;
  }

  .role-avatar {
    width: 50px;
    height: 50px;
  }
}
</style>
