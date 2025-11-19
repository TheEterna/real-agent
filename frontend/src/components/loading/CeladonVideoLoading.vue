<template>
  <div
    v-if="visible"
    class="celadon-video-loading"
    :class="{ 'is-playing': isPlaying, 'use-fallback': useFallbackAnimation }"
  >
    <!-- 背景遮罩 -->
    <div class="overlay-backdrop"></div>

    <!-- 后备青花瓷动画 -->
    <div v-if="useFallbackAnimation" class="fallback-animation">
      <div class="celadon-crackle">
        <div class="crack-center"></div>
        <div class="crack-line" v-for="i in 8" :key="i" :style="{ transform: `rotate(${i * 45}deg)` }"></div>
        <div class="crack-particle" v-for="i in 12" :key="`p${i}`"></div>
      </div>
    </div>

    <!-- 视频播放器 -->
    <div v-else class="video-container">
      <video
        ref="videoRef"
        class="crackle-video"
        :src="videoSrc"
        muted
        playsinline
        @loadeddata="onVideoLoaded"
        @ended="onVideoEnded"
        @error="onVideoError"
      />
    </div>

    <!-- 文字叠加层 -->
    <div class="text-overlay" v-if="title || subtitle">
      <h1 v-if="title" class="loading-title">{{ title }}</h1>
      <p v-if="subtitle" class="loading-subtitle">{{ subtitle }}</p>
    </div>

    <!-- 音频播放器 -->
    <audio
      ref="audioRef"
      :src="currentAudioSrc"
      preload="auto"
      @error="onAudioError"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, watch, onMounted, onUnmounted, computed } from 'vue'

interface Props {
  visible?: boolean
  title?: string
  subtitle?: string
  audioIndex?: number  // 0, 1, 2 对应三个音效文件
}

const props = withDefaults(defineProps<Props>(), {
  visible: false,
  title: '切换中',
  subtitle: '...',
  audioIndex: 0
})

const emit = defineEmits<{
  started: []
  ended: []
  error: [error: string]
}>()

// DOM引用
const videoRef = ref<HTMLVideoElement>()
const audioRef = ref<HTMLAudioElement>()

// 状态管理
const isPlaying = ref(false)
const isVideoLoaded = ref(false)
const useFallbackAnimation = ref(false)  // 后备动画标志

// 资源路径
const videoSrc = '/video/bg.mp4'
const audioFiles = [
  '/voice/CeramicCracking-1.mp3',
  '/voice/CeramicCracking-2.mp3',
  '/voice/CeramicCracking-3.mp3'
]

const currentAudioSrc = computed(() => {
  const index = Math.max(0, Math.min(props.audioIndex, audioFiles.length - 1))
  return audioFiles[index]
})

// 事件处理
const onVideoLoaded = () => {
  console.log('🎬 视频加载完成')
  isVideoLoaded.value = true
}

const onVideoEnded = () => {
  console.log('🎬 视频播放结束')
  isPlaying.value = false
  emit('ended')
}

const onVideoError = (error: Event) => {
  console.error('🎬 视频播放错误:', error)
  emit('error', '视频播放失败')
}

const onAudioError = (error: Event) => {
  console.error('🔊 音频播放错误:', error)
  emit('error', '音频播放失败')
}

// 播放控制
const startPlayback = async () => {
  try {
    if (!videoRef.value || !audioRef.value) {
      console.warn('视频或音频元素未准备好，使用后备动画')
      startFallbackAnimation()
      return
    }

    console.log('🐉 开始播放青花瓷破裂效果...')
    isPlaying.value = true
    emit('started')

    // 重置播放位置
    videoRef.value.currentTime = 0
    audioRef.value.currentTime = 0

    // 尝试播放视频（处理浏览器策略）
    try {
      await videoRef.value.play()
      console.log('🎬 视频播放成功')
      useFallbackAnimation.value = false
    } catch (videoError) {
      console.warn('🎬 视频播放失败，使用后备动画:', videoError)
      useFallbackAnimation.value = true
      startFallbackAnimation()
      return
    }

    // 尝试播放音频（可选，如果失败不影响视频）
    try {
      await audioRef.value.play()
      console.log('🔊 音频播放成功')
    } catch (audioError) {
      console.warn('🔊 音频播放失败（忽略）:', audioError)
      // 音频播放失败不影响整体效果
    }

    console.log('🎬🔊 播放开始')
  } catch (error) {
    console.error('播放失败，使用后备动画:', error)
    useFallbackAnimation.value = true
    startFallbackAnimation()
  }
}

// 后备 CSS 动画
const startFallbackAnimation = () => {
  isPlaying.value = true
  useFallbackAnimation.value = true
  emit('started')

  console.log('🎨 使用后备青花瓷动画')

  // 播放音频（如果可能）
  if (audioRef.value) {
    audioRef.value.play().catch(e => {
      console.warn('后备动画音频播放失败:', e)
    })
  }

  // 3秒后结束动画
  setTimeout(() => {
    isPlaying.value = false
    useFallbackAnimation.value = false
    emit('ended')
  }, 3000)
}

const stopPlayback = () => {
  console.log('🎬 stopPlayback 被调用')

  if (videoRef.value) {
    videoRef.value.pause()
    videoRef.value.currentTime = 0
  }
  if (audioRef.value) {
    audioRef.value.pause()
    audioRef.value.currentTime = 0
  }

  isPlaying.value = false
  useFallbackAnimation.value = false

  console.log('🎬 播放状态已重置')
}

// 监听 visible 变化
watch(() => props.visible, (newVisible) => {
  console.log('🎬 CeladonVideoLoading visible 变化:', newVisible)

  if (newVisible) {
    if (isVideoLoaded.value) {
      console.log('🎬 视频已加载，开始播放')
      startPlayback()
    } else {
      console.log('🎬 视频未加载，使用后备动画')
      startFallbackAnimation()
    }
  } else if (!newVisible) {
    console.log('🎬 停止播放')
    stopPlayback()
  }
})

// 监听音频文件变化
watch(() => props.audioIndex, () => {
  if (audioRef.value) {
    audioRef.value.load() // 重新加载音频
  }
})

onMounted(() => {
  console.log('🎬 CeladonVideoLoading 组件挂载')

  // 如果组件挂载时就是可见状态
  if (props.visible) {
    console.log('🎬 挂载时即可见，检查视频状态')

    if (videoRef.value && videoRef.value.readyState >= 2) {
      // 视频已加载足够数据
      console.log('🎬 视频已准备好')
      isVideoLoaded.value = true
      startPlayback()
    } else {
      // 视频未准备好，使用后备动画
      console.log('🎬 视频未准备好，使用后备动画')
      startFallbackAnimation()
    }
  }
})

onUnmounted(() => {
  stopPlayback()
})
</script>

<style scoped lang="scss">
/* =================================================================
   🏺 青花瓷视频加载效果 - CELADON VIDEO LOADING
   Philosophy: 现代技术与传统美学的完美融合
   ================================================================= */

.celadon-video-loading {
  position: fixed;
  inset: 0;
  z-index: 99999;  /* 提高z-index确保在最顶层 */
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(0, 0, 0, 0.95);
  transition: all 0.3s ease;

  /* 入场动画 */
  animation: backdropFadeIn 0.4s ease-out;
}

@keyframes backdropFadeIn {
  from {
    opacity: 0;
    backdrop-filter: blur(0px);
  }
  to {
    opacity: 1;
    backdrop-filter: blur(8px);
  }
}

.overlay-backdrop {
  position: absolute;
  inset: 0;
  background: radial-gradient(
    circle at center,
    rgba(91, 138, 138, 0.1) 0%,
    rgba(0, 0, 0, 0.9) 70%
  );
  pointer-events: none;
}

.video-container {
  position: relative;
  max-width: 80vw;
  max-height: 80vh;
  border-radius: 12px;
  overflow: hidden;
  box-shadow:
    0 0 40px rgba(91, 138, 138, 0.3),
    0 0 80px rgba(91, 138, 138, 0.1);

  /* 青瓷光晕效果 */
  &::before {
    content: '';
    position: absolute;
    inset: -4px;
    border-radius: 16px;
    padding: 4px;
    background: linear-gradient(45deg,
      rgba(91, 138, 138, 0.6),
      rgba(107, 154, 152, 0.4),
      rgba(91, 138, 138, 0.6)
    );
    background-size: 300% 300%;
    -webkit-mask: linear-gradient(#fff 0 0) content-box, linear-gradient(#fff 0 0);
    -webkit-mask-composite: xor;
    mask-composite: exclude;
    animation: shimmerBorder 3s ease-in-out infinite;
    pointer-events: none;
  }
}

@keyframes shimmerBorder {
  0%, 100% {
    background-position: 0% 50%;
    opacity: 0.6;
  }
  50% {
    background-position: 100% 50%;
    opacity: 1;
  }
}

.crackle-video {
  display: block;
  width: 100%;
  height: 100%;
  object-fit: cover;
  filter: contrast(1.1) brightness(1.05);

  /* 播放时的微妙脉动 */
  .is-playing & {
    animation: videoPulse 4s ease-in-out infinite;
  }
}

@keyframes videoPulse {
  0%, 100% {
    filter: contrast(1.1) brightness(1.05) saturate(1);
  }
  50% {
    filter: contrast(1.15) brightness(1.1) saturate(1.1);
  }
}

.text-overlay {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  text-align: center;
  color: white;
  text-shadow:
    0 0 20px rgba(91, 138, 138, 0.8),
    0 2px 4px rgba(0, 0, 0, 0.6);
  pointer-events: none;

  /* 文字出现动画 */
  animation: textAppear 0.8s ease-out 0.2s both;
}

@keyframes textAppear {
  from {
    opacity: 0;
    transform: translate(-50%, -50%) scale(0.9) translateY(20px);
  }
  to {
    opacity: 1;
    transform: translate(-50%, -50%) scale(1) translateY(0);
  }
}

.loading-title {
  font-size: clamp(1.5rem, 4vw, 3rem);
  font-weight: 300;
  margin: 0 0 0.5rem;
  font-family: 'KaiTi', '楷体', serif;
  letter-spacing: 0.3em;
  background: linear-gradient(
    135deg,
    rgba(255, 255, 255, 1) 0%,
    rgba(216, 232, 232, 0.9) 50%,
    rgba(255, 255, 255, 0.8) 100%
  );
  background-clip: text;
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;

  /* 标题的青光脉动 */
  animation: titleGlow 3s ease-in-out infinite;
}

@keyframes titleGlow {
  0%, 100% {
    text-shadow:
      0 0 20px rgba(91, 138, 138, 0.6),
      0 0 40px rgba(91, 138, 138, 0.3);
  }
  50% {
    text-shadow:
      0 0 30px rgba(91, 138, 138, 0.8),
      0 0 60px rgba(91, 138, 138, 0.4);
  }
}

.loading-subtitle {
  font-size: clamp(0.875rem, 2vw, 1.2rem);
  font-weight: 400;
  margin: 0;
  opacity: 0.9;
  font-family: 'KaiTi', '楷体', serif;
  letter-spacing: 0.2em;
  line-height: 1.6;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .video-container {
    max-width: 95vw;
    max-height: 70vh;
  }

  .text-overlay {
    padding: 0 1rem;
  }

  .loading-title {
    letter-spacing: 0.2em;
  }

  .loading-subtitle {
    letter-spacing: 0.1em;
  }
}

@media (max-width: 480px) {
  .video-container {
    max-width: 100vw;
    max-height: 60vh;
    border-radius: 0;
  }
}

/* 可访问性支持 */
@media (prefers-reduced-motion: reduce) {
  .celadon-video-loading,
  .video-container::before,
  .crackle-video,
  .text-overlay,
  .loading-title {
    animation: none !important;
  }

  .celadon-video-loading {
    transition: opacity 0.2s ease;
  }
}

/* 高对比度支持 */
@media (prefers-contrast: more) {
  .overlay-backdrop {
    background: rgba(0, 0, 0, 0.95);
  }

  .text-overlay {
    text-shadow: 0 2px 8px rgba(0, 0, 0, 0.8);
  }

  .loading-title {
    -webkit-text-fill-color: white;
    background: none;
  }
}

/* ============= 后备青花瓷动画 ============= */
.fallback-animation {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(
    135deg,
    rgba(91, 138, 138, 0.95) 0%,
    rgba(44, 62, 62, 0.9) 50%,
    rgba(91, 138, 138, 0.95) 100%
  );
}

.celadon-crackle {
  position: relative;
  width: 300px;
  height: 300px;
  border-radius: 50%;
  background: radial-gradient(
    circle at center,
    rgba(216, 232, 232, 0.1) 0%,
    rgba(91, 138, 138, 0.3) 50%,
    rgba(44, 62, 62, 0.8) 100%
  );
  animation: crackleExpand 3s ease-out;
}

@keyframes crackleExpand {
  0% {
    transform: scale(0.8);
    opacity: 0.8;
  }
  30% {
    transform: scale(1.1);
    opacity: 1;
  }
  60% {
    transform: scale(1.05);
  }
  100% {
    transform: scale(1.2);
    opacity: 0;
  }
}

.crack-center {
  position: absolute;
  top: 50%;
  left: 50%;
  width: 20px;
  height: 20px;
  background: rgba(216, 232, 232, 0.9);
  border-radius: 50%;
  transform: translate(-50%, -50%);
  animation: centerPulse 3s ease-out;
  box-shadow: 0 0 20px rgba(91, 138, 138, 0.6);
}

@keyframes centerPulse {
  0% {
    transform: translate(-50%, -50%) scale(0);
    opacity: 0;
  }
  10% {
    transform: translate(-50%, -50%) scale(1);
    opacity: 1;
  }
  100% {
    transform: translate(-50%, -50%) scale(1.5);
    opacity: 0;
  }
}

.crack-line {
  position: absolute;
  top: 50%;
  left: 50%;
  width: 2px;
  height: 120px;
  background: linear-gradient(
    to bottom,
    rgba(216, 232, 232, 0.8) 0%,
    rgba(91, 138, 138, 0.6) 50%,
    transparent 100%
  );
  transform-origin: bottom center;
  margin-left: -1px;
  margin-top: -120px;
  animation: crackExpand 3s ease-out;
  box-shadow: 0 0 6px rgba(91, 138, 138, 0.4);
}

.crack-line:nth-child(odd) {
  animation-delay: 0.1s;
}

.crack-line:nth-child(even) {
  animation-delay: 0.2s;
}

@keyframes crackExpand {
  0% {
    height: 0;
    opacity: 0;
  }
  20% {
    height: 60px;
    opacity: 1;
  }
  60% {
    height: 120px;
    opacity: 0.8;
  }
  100% {
    height: 150px;
    opacity: 0;
  }
}

.crack-particle {
  position: absolute;
  width: 6px;
  height: 6px;
  background: rgba(216, 232, 232, 0.8);
  border-radius: 50%;
  animation: particleFloat 3s ease-out;
  box-shadow: 0 0 8px rgba(91, 138, 138, 0.6);
}

.crack-particle:nth-child(n+1) { top: 20%; left: 30%; animation-delay: 0.3s; }
.crack-particle:nth-child(n+2) { top: 30%; left: 70%; animation-delay: 0.4s; }
.crack-particle:nth-child(n+3) { top: 60%; left: 20%; animation-delay: 0.5s; }
.crack-particle:nth-child(n+4) { top: 70%; left: 80%; animation-delay: 0.6s; }
.crack-particle:nth-child(n+5) { top: 40%; left: 50%; animation-delay: 0.4s; }
.crack-particle:nth-child(n+6) { top: 80%; left: 45%; animation-delay: 0.7s; }
.crack-particle:nth-child(n+7) { top: 15%; left: 60%; animation-delay: 0.5s; }
.crack-particle:nth-child(n+8) { top: 50%; left: 15%; animation-delay: 0.6s; }
.crack-particle:nth-child(n+9) { top: 35%; left: 85%; animation-delay: 0.8s; }
.crack-particle:nth-child(n+10) { top: 75%; left: 25%; animation-delay: 0.9s; }
.crack-particle:nth-child(n+11) { top: 25%; left: 40%; animation-delay: 0.7s; }
.crack-particle:nth-child(n+12) { top: 65%; left: 65%; animation-delay: 1.0s; }

@keyframes particleFloat {
  0% {
    transform: scale(0) translateY(0);
    opacity: 0;
  }
  20% {
    transform: scale(1) translateY(-10px);
    opacity: 1;
  }
  60% {
    transform: scale(1.2) translateY(-30px);
    opacity: 0.8;
  }
  100% {
    transform: scale(0.5) translateY(-60px);
    opacity: 0;
  }
}

/* 后备动画状态下的文字样式增强 */
.use-fallback .text-overlay {
  z-index: 10;
}

.use-fallback .loading-title {
  animation: titleShimmer 3s ease-in-out;
}

@keyframes titleShimmer {
  0%, 100% {
    text-shadow:
      0 0 20px rgba(91, 138, 138, 0.6),
      0 0 40px rgba(91, 138, 138, 0.3);
  }
  50% {
    text-shadow:
      0 0 30px rgba(216, 232, 232, 0.8),
      0 0 60px rgba(91, 138, 138, 0.5);
  }
}
</style>
