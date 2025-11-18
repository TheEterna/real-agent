<template>
  <div>
    <h3>列表:</h3>
    <ul>
      <li v-for="item in items" :key="item.id">
        ID: {{ item.id }} | 姓名: {{ item.name }}
      </li>
    </ul>
    <button @click="updateFirstName">更新第一个人的名字</button>
  </div>
</template>

<script setup>
import { ref } from 'vue';

// 1. 创建响应式对象数组
const items = ref([
  { id: 1, name: "Alice" },
  { id: 2, name: "Bob" }
]);

const updateFirstName = () => {
  // 2. 你的操作：修改数组中第一个对象的 'name' 字段
  // 这会 100% 触发响应式更新
  items.value[0].name = "Alice (已更新)";
  console.log("已更新:", items.value[0]);
};
</script>