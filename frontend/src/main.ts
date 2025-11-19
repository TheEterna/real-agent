import { createApp } from 'vue'
import { createPinia } from 'pinia'
import piniaPluginPersistedstate from 'pinia-plugin-persistedstate'
import Antd from 'ant-design-vue'
import 'ant-design-vue/dist/reset.css'
import '@/styles/index.scss'
import App from './App.vue'
import router from './router'
import i18n from './i18n'
import { getTwoToneColor, setTwoToneColor } from '@ant-design/icons-vue';

setTwoToneColor('#b8ddd9');
getTwoToneColor();

// monaco work 处理
import { getUseMonaco } from 'vue-renderer-markdown'
getUseMonaco()

const pinia = createPinia()
pinia.use(piniaPluginPersistedstate)

const app = createApp(App)
app.use(pinia)
app.use(router)
app.use(Antd)
app.use(i18n)
app.mount('#app')
