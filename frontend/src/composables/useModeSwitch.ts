import {ref, computed, watch} from 'vue'
import {useRouter, useRoute} from 'vue-router'
import {notification} from 'ant-design-vue'

// 支持的输入模式
export type InputMode = 'geek' | 'multimodal' | 'default'

// 模式配置
const MODE_CONFIG = {
    geek: {
        name: '🤖 极客模式',
        description: '终端风格界面，代码高亮，极客体验',
        themeClass: 'theme-geek'
    },
    multimodal: {
        name: '⚡ 多模态模式',
        description: '现代化界面，支持多种输入方式',
        themeClass: 'theme-react-plus'
    },
    default: {
        name: '默认模式',
        description: '命令行风格，快速执行',
        themeClass: 'theme-react-plus'
    }
} as const

export function useModeSwitch() {
    const router = useRouter()
    const route = useRoute()

    // 当前模式状态
    const currentMode = ref<InputMode>('multimodal')

    // 从 URL 参数读取模式
    const getModeFromRoute = (): InputMode => {
        const modeParam = route.query.mode as string
        if (modeParam && ['geek', 'multimodal', 'default'].includes(modeParam)) {
            return modeParam as InputMode
        }
        return 'multimodal' // 默认模式
    }

    // 同步模式状态与 URL
    const syncModeFromRoute = () => {
        const routeMode = getModeFromRoute()
        if (routeMode !== currentMode.value) {
            currentMode.value = routeMode
        }
    }

    // 切换模式
    const switchMode = async (mode: InputMode) => {
        if (mode === currentMode.value) {
            return
        }

        try {
            // 构建新的查询参数
            const newQuery = {...route.query}

            // 设置或移除模式参数
            newQuery.mode = mode

            // 更新 URL（保持当前路径和其他查询参数）
            await router.push({
                path: route.path,
                query: newQuery
            })

            // 更新本地状态
            currentMode.value = mode

            // 显示切换成功提示
            notification.success({
                message: '模式切换成功',
                description: `已切换到 ${MODE_CONFIG[mode].name}`,
                duration: 2,
                placement: 'bottomRight'
            })

        } catch (error) {
            console.error('模式切换失败:', error)
            notification.error({
                message: '模式切换失败',
                description: error instanceof Error ? error.message : '未知错误',
                duration: 3,
                placement: 'bottomRight'
            })
        }
    }

    // 获取当前模式配置
    const currentModeConfig = computed(() => MODE_CONFIG[currentMode.value])

    // 获取当前主题类名
    const currentThemeClass = computed(() => currentModeConfig.value.themeClass)

    // 判断是否为特定模式
    const isGeekMode = computed(() => currentMode.value === 'geek')
    const isMultimodalMode = computed(() => currentMode.value === 'multimodal')

    // 监听路由变化，同步模式状态
    watch(
        () => route.query.mode,
        () => {
            syncModeFromRoute()
        },
        {immediate: true}
    )

    // 初始化时同步模式
    syncModeFromRoute()

    return {
        // 状态
        currentMode,
        currentModeConfig,
        currentThemeClass,

        // 计算属性
        isGeekMode,
        isMultimodalMode,

        // 方法
        switchMode,
        syncModeFromRoute,
        getModeFromRoute,

        // 配置
        MODE_CONFIG
    }
}