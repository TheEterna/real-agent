<template>
  <a-row :gutter="[16,16]">
    <a-col :span="6"><a-card><a-statistic :title="t('dashboard.tools')" :value="3" /></a-card></a-col>
    <a-col :span="6"><a-card><a-statistic :title="t('dashboard.agents')" :value="1" /></a-card></a-col>
    <a-col :span="6"><a-card><a-statistic :title="t('dashboard.runs')" :value="0" /></a-card></a-col>
    <a-col :span="6"><a-card><a-statistic :title="t('dashboard.errors')" :value="0" valueStyle="color:#cf1322" /></a-card></a-col>
  </a-row>
</template>
<script setup lang="ts">
import { useI18n } from 'vue-i18n'

const { t } = useI18n()
</script>