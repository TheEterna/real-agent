<template>
  <a-card title="Playground" style="width: 100%">
    <a-list :data-source="apps" :grid="{ gutter: 16, column: 3 }">
      <template #renderItem="{ item }">
        <a-list-item>
          <a-card hoverable @click="go(item.route)">
            <div style="display:flex;gap:12px;align-items:center;">
              <img :src="item.img" alt="cover" style="width:48px;height:48px;border-radius:10px;object-fit:cover;border:1px solid #eef2f7;" />
              <div>
                <div style="font-weight:600;">{{ item.title }}</div>
                <div style="color:#666;margin-top:4px;
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;;">{{ item.description }}</div>
              </div>
            </div>
          </a-card>
        </a-list-item>
      </template>
    </a-list>
  </a-card>
</template>
<script setup lang="ts">
import { useRouter } from 'vue-router'

const router = useRouter()

type PgApp = { id: string; title: string; description: string; route: string; img: string }

// 暂时硬编码一个应用，后续可从后端获取清单
const apps: PgApp[] = [
  {
    id: 'data-lab',
    title: 'Data Lab',
    description: '数据实验室：用最小代价验证数据与可视化的想法',
    route: '/playground/data-lab',
    img: 'data:image/svg+xml;utf8,<svg xmlns="http://www.w3.org/2000/svg" width="96" height="96" viewBox="0 0 96 96"><rect rx="16" ry="16" width="96" height="96" fill="%23f0f7ff"/><g transform="translate(16,16)" fill="%231677ff"><circle cx="16" cy="32" r="6"/><circle cx="32" cy="20" r="6"/><circle cx="48" cy="40" r="6"/><path d="M16 32 L32 20 L48 40" stroke="%231677ff" stroke-width="4" fill="none"/></g></svg>'
  },
  {
    id: 'role-play-agent',
    title: 'Role Play Agent',
    description: '角色扮演智能体（文本/语音）',
    route: '/playground/role-play-agent',
    img: 'data:image/svg+xml;utf8,<svg xmlns="http://www.w3.org/2000/svg" width="96" height="96" viewBox="0 0 96 96"><rect rx="16" ry="16" width="96" height="96" fill="%23fff7f0"/><g transform="translate(20,20)" fill="%23ff7a45"><circle cx="28" cy="16" r="10"/><path d="M8 48c0-11 9-20 20-20s20 9 20 20" stroke="%23ff7a45" stroke-width="4" fill="none" stroke-linecap="round"/></g></svg>'
  },
]

function go(path: string){
  router.push(path)
}
</script>
