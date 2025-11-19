<template>
  <div class="datalab">
    <div class="topbar">
      <a-button type="default" shape="circle" @click="goBack" :title="'返回 Playground'">
        <template #icon>
          <ArrowLeftOutlined />
        </template>
      </a-button>
    </div>
    <a-card :bordered="false" class="content">
      <a-typography-title :level="4" style="margin-top:0">Data Lab（示例应用）</a-typography-title>
      <a-typography-paragraph>
        这里是一个示例的实践性应用容器。后续可以在此放置你的实验页面、可视化图表、交互表单等内容。
      </a-typography-paragraph>
    </a-card>
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'
import { ArrowLeftOutlined } from '@ant-design/icons-vue'
const router = useRouter()
const goBack = () => router.push('/playground')
</script>

<style scoped lang="scss">
.datalab { display: flex; flex-direction: column; gap: 12px; width: 100%; }
.topbar { display: flex; align-items: center; }
.content { width: 100%; }
</style>
