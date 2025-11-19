<template>
  <div class="crackle-demo-page">
    <div class="demo-content">
      <h1>🏺 青花瓷破裂视频效果演示</h1>
      <p class="subtitle">切换中 · 碎玉落叠 · 现代演绎</p>

      <div class="demo-controls">
        <button @click="triggerAnimation" class="trigger-btn" :disabled="isPlaying">
          {{ isPlaying ? '正在播放...' : '🐉 触发破裂效果' }}
        </button>

        <div class="settings">
          <h3>自定义设置</h3>
          <div class="setting-item">
            <label>标题文字：</label>
            <input v-model="customTitle" placeholder="切换中" />
          </div>
          <div class="setting-item">
            <label>副标题：</label>
            <input v-model="customSubtitle" placeholder="..." />
          </div>
          <div class="setting-item">
            <label>音效选择：</label>
            <select v-model="audioIndex">
              <option :value="0">陶瓷破裂音效 1</option>
              <option :value="1">陶瓷破裂音效 2</option>
              <option :value="2">陶瓷破裂音效 3</option>
            </select>
          </div>
        </div>

        <div class="info-box">
          <h3>💡 实现方式</h3>
          <ul>
            <li><strong>视频播放</strong>: 使用 MP4 视频文件实现破裂动画</li>
            <li><strong>音效同步</strong>: 三种陶瓷破裂音效可选</li>
            <li><strong>文字叠加</strong>: 可自定义标题和副标题</li>
            <li><strong>响应式设计</strong>: 适配各种屏幕尺寸</li>
            <li><strong>青瓷主题</strong>: 保持传统美学风格</li>
          </ul>
        </div>

        <div class="resource-info">
          <h3>📁 资源文件</h3>
          <div class="file-list">
            <div class="file-item">
              <span class="file-type">🎬</span>
              <span class="file-name">495ba642...d4a3.mp4</span>
              <span class="file-desc">破裂动画视频</span>
            </div>
            <div class="file-item">
              <span class="file-type">🔊</span>
              <span class="file-name">CeramicCracking-1.mp3</span>
              <span class="file-desc">陶瓷破裂音效 1</span>
            </div>
            <div class="file-item">
              <span class="file-type">🔊</span>
              <span class="file-name">CeramicCracking-2.mp3</span>
              <span class="file-desc">陶瓷破裂音效 2</span>
            </div>
            <div class="file-item">
              <span class="file-type">🔊</span>
              <span class="file-name">CeramicCracking-3.mp3</span>
              <span class="file-desc">陶瓷破裂音效 3</span>
            </div>
          </div>
        </div>

        <div class="color-reference">
          <h3>🎨 青花瓷配色</h3>
          <div class="color-palette">
            <div class="color-item">
              <div class="color-block" style="background: rgba(91, 138, 138, 1)"></div>
              <span>主色调</span>
            </div>
            <div class="color-item">
              <div class="color-block" style="background: rgba(91, 138, 138, 0.6)"></div>
              <span>光晕效果</span>
            </div>
            <div class="color-item">
              <div class="color-block" style="background: rgba(107, 154, 152, 0.4)"></div>
              <span>边框装饰</span>
            </div>
            <div class="color-item">
              <div class="color-block" style="background: rgba(216, 232, 232, 0.9)"></div>
              <span>文字渐变</span>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 新的视频+音频播放组件 -->
    <CeladonVideoLoading
      :visible="isPlaying"
      :title="customTitle"
      :subtitle="customSubtitle"
      :audio-index="audioIndex"
      @started="onPlaybackStarted"
      @ended="onPlaybackEnded"
      @error="onPlaybackError"
    />
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import CeladonVideoLoading from '@/components/loading/CeladonVideoLoading.vue'

const isPlaying = ref(false)
const customTitle = ref('切换中')
const customSubtitle = ref('...')
const audioIndex = ref(0)

const triggerAnimation = () => {
  console.log('🐉 触发青花瓷破裂效果')
  isPlaying.value = true
}

const onPlaybackStarted = () => {
  console.log('🎬 播放开始')
}

const onPlaybackEnded = () => {
  console.log('🎬 播放结束')
  isPlaying.value = false
}

const onPlaybackError = (error: string) => {
  console.error('❌ 播放错误:', error)
  isPlaying.value = false

  // 可以在这里添加错误提示
  alert(`播放失败: ${error}`)
}
</script>

<style scoped lang="scss">
.crackle-demo-page {
  min-height: 100vh;
  background: linear-gradient(135deg, #F8F9FA 0%, #E8F0F0 100%);
  padding: 2rem;
}

.demo-content {
  max-width: 900px;
  margin: 0 auto;
}

h1 {
  font-size: 2.5rem;
  color: rgba(91, 138, 138, 0.9);
  text-align: center;
  margin-bottom: 0.5rem;
  font-family: 'KaiTi', '楷体', serif;
  letter-spacing: 0.3em;
}

.subtitle {
  text-align: center;
  color: rgba(91, 138, 138, 0.6);
  font-size: 1.2rem;
  margin-bottom: 3rem;
  font-family: 'KaiTi', '楷体', serif;
  letter-spacing: 0.2em;
}

.demo-controls {
  background: white;
  border-radius: 12px;
  padding: 2rem;
  box-shadow: 0 4px 16px rgba(91, 138, 138, 0.1);
}

.trigger-btn {
  width: 100%;
  padding: 1.5rem;
  font-size: 1.5rem;
  background: linear-gradient(135deg,
    rgba(91, 138, 138, 0.9),
    rgba(91, 138, 138, 0.7)
  );
  color: white;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.3s ease;
  font-family: 'KaiTi', '楷体', serif;
  letter-spacing: 0.2em;
  margin-bottom: 2rem;

  &:hover:not(:disabled) {
    transform: translateY(-2px);
    box-shadow: 0 8px 24px rgba(91, 138, 138, 0.3);
  }

  &:active:not(:disabled) {
    transform: translateY(0);
  }

  &:disabled {
    opacity: 0.6;
    cursor: not-allowed;
  }
}

.settings {
  margin-bottom: 2rem;
  padding: 1.5rem;
  background: rgba(240, 248, 248, 0.5);
  border-radius: 8px;

  h3 {
    color: rgba(91, 138, 138, 0.9);
    margin-bottom: 1rem;
    font-family: 'KaiTi', '楷体', serif;
  }
}

.setting-item {
  display: flex;
  align-items: center;
  margin-bottom: 1rem;

  label {
    min-width: 100px;
    color: rgba(91, 138, 138, 0.8);
    font-family: 'KaiTi', '楷体', serif;
  }

  input {
    flex: 1;
    padding: 0.5rem 1rem;
    border: 1px solid rgba(91, 138, 138, 0.2);
    border-radius: 6px;
    font-size: 1rem;
    font-family: 'KaiTi', '楷体', serif;

    &:focus {
      outline: none;
      border-color: rgba(91, 138, 138, 0.5);
      box-shadow: 0 0 0 3px rgba(91, 138, 138, 0.1);
    }
  }

  select {
    flex: 1;
    padding: 0.5rem 1rem;
    border: 1px solid rgba(91, 138, 138, 0.2);
    border-radius: 6px;
    font-size: 1rem;
    font-family: 'KaiTi', '楷体', serif;
    background: white;
    cursor: pointer;

    &:focus {
      outline: none;
      border-color: rgba(91, 138, 138, 0.5);
      box-shadow: 0 0 0 3px rgba(91, 138, 138, 0.1);
    }
  }
}

.info-box {
  margin-bottom: 2rem;
  padding: 1.5rem;
  background: rgba(255, 250, 240, 0.5);
  border-radius: 8px;
  border-left: 4px solid rgba(91, 138, 138, 0.5);

  h3 {
    color: rgba(91, 138, 138, 0.9);
    margin-bottom: 1rem;
    font-family: 'KaiTi', '楷体', serif;
  }

  ol, ul {
    margin: 0;
    padding-left: 1.5rem;

    li {
      margin-bottom: 0.5rem;
      color: rgba(91, 138, 138, 0.7);
      line-height: 1.6;

      strong {
        color: rgba(91, 138, 138, 0.9);
        font-weight: 600;
      }
    }
  }
}

.resource-info {
  margin-bottom: 2rem;
  padding: 1.5rem;
  background: rgba(248, 250, 255, 0.5);
  border-radius: 8px;
  border-left: 4px solid rgba(107, 154, 152, 0.5);

  h3 {
    color: rgba(91, 138, 138, 0.9);
    margin-bottom: 1rem;
    font-family: 'KaiTi', '楷体', serif;
  }
}

.file-list {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.file-item {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  padding: 0.75rem;
  background: rgba(255, 255, 255, 0.6);
  border-radius: 6px;
  border: 1px solid rgba(91, 138, 138, 0.1);
  transition: all 0.2s ease;

  &:hover {
    background: rgba(248, 252, 252, 0.8);
    border-color: rgba(91, 138, 138, 0.2);
    transform: translateX(2px);
  }

  .file-type {
    font-size: 1.2rem;
    min-width: 24px;
  }

  .file-name {
    font-family: 'SF Mono', 'JetBrains Mono', Consolas, monospace;
    font-size: 0.875rem;
    color: rgba(91, 138, 138, 0.8);
    min-width: 180px;
    font-weight: 500;
  }

  .file-desc {
    flex: 1;
    color: rgba(91, 138, 138, 0.6);
    font-size: 0.875rem;
  }
}

.color-reference {
  padding: 1.5rem;
  background: rgba(248, 252, 252, 0.5);
  border-radius: 8px;

  h3 {
    color: rgba(91, 138, 138, 0.9);
    margin-bottom: 1rem;
    font-family: 'KaiTi', '楷体', serif;
  }
}

.color-palette {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(120px, 1fr));
  gap: 1rem;
}

.color-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.5rem;
}

.color-block {
  width: 60px;
  height: 60px;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.color-item span {
  font-size: 0.875rem;
  color: rgba(91, 138, 138, 0.7);
  font-family: 'KaiTi', '楷体', serif;
}

@media (max-width: 768px) {
  .crackle-demo-page {
    padding: 1rem;
  }

  h1 {
    font-size: 1.8rem;
  }

  .subtitle {
    font-size: 1rem;
  }

  .trigger-btn {
    font-size: 1.2rem;
    padding: 1rem;
  }

  .setting-item {
    flex-direction: column;
    align-items: flex-start;

    label {
      margin-bottom: 0.5rem;
    }

    input, select {
      width: 100%;
    }
  }

  .file-item {
    flex-direction: column;
    align-items: flex-start;
    gap: 0.5rem;

    .file-name {
      min-width: auto;
      word-break: break-all;
    }
  }
}
</style>
