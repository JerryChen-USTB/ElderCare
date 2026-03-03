<template>
  <view class="tts-test-container">
    <!-- 标题 -->
    <view class="header">
      <text class="title">🎙️ 语音合成测试</text>
      <text class="subtitle">阿里云百炼 TTS 接入测试</text>
    </view>

    <!-- 输入表单 -->
    <view class="form-container">
      <!-- 文本输入 -->
      <view class="form-group">
        <text class="label">输入文本</text>
        <textarea
          class="text-input"
          v-model="formData.text"
          placeholder="请输入要转换为语音的文字..."
          :maxlength="500"
          :auto-height="true"
        />
        <text class="char-count">{{ formData.text.length }}/500</text>
      </view>

      <!-- 音色选择 -->
      <view class="form-group">
        <text class="label">选择音色</text>
        <picker 
          mode="selector" 
          :range="voiceOptions" 
          range-key="label"
          :value="selectedVoiceIndex"
          @change="onVoiceChange"
        >
          <view class="picker-display">
            <text>{{ voiceOptions[selectedVoiceIndex].label }}</text>
            <text class="arrow">▼</text>
          </view>
        </picker>
      </view>

      <!-- 语言选择 -->
      <view class="form-group">
        <text class="label">语言类型</text>
        <picker 
          mode="selector" 
          :range="languageOptions" 
          range-key="label"
          :value="selectedLanguageIndex"
          @change="onLanguageChange"
        >
          <view class="picker-display">
            <text>{{ languageOptions[selectedLanguageIndex].label }}</text>
            <text class="arrow">▼</text>
          </view>
        </picker>
      </view>

      <!-- 快捷文本示例 -->
      <view class="form-group">
        <text class="label">快捷示例</text>
        <view class="quick-examples">
          <view 
            class="example-btn" 
            v-for="(example, index) in examples" 
            :key="index"
            @click="selectExample(example)"
          >
            {{ example.name }}
          </view>
        </view>
      </view>

      <!-- 操作按钮 -->
      <view class="button-group">
        <button 
          class="btn btn-primary" 
          @click="synthesize"
          :disabled="loading || !formData.text"
        >
          <text v-if="!loading">🎵 开始合成</text>
          <text v-else>⏳ 合成中...</text>
        </button>
        <button class="btn btn-secondary" @click="clearForm">
          🗑️ 清空
        </button>
      </view>
    </view>

    <!-- 音频播放器 -->
    <view class="audio-player" v-if="audioUrl">
      <view class="player-header">
        <text class="player-title">🔊 音频播放器</text>
        <text class="status" :class="{ playing: isPlaying }">
          {{ isPlaying ? '播放中' : '已暂停' }}
        </text>
      </view>
      
      <!-- 音频组件 -->
      <audio 
        :src="audioUrl" 
        :id="audioId"
        @play="onPlay"
        @pause="onPause"
        @ended="onEnded"
        @error="onError"
      />
      
      <!-- 播放控制 -->
      <view class="player-controls">
        <button class="control-btn" @click="playAudio" v-if="!isPlaying">
          ▶️ 播放
        </button>
        <button class="control-btn" @click="pauseAudio" v-else>
          ⏸️ 暂停
        </button>
        <button class="control-btn" @click="stopAudio">
          ⏹️ 停止
        </button>
      </view>
      
      <!-- 音频URL -->
      <view class="audio-url">
        <text class="url-label">音频地址：</text>
        <text class="url-value" @click="copyUrl">{{ audioUrl }}</text>
      </view>
    </view>

    <!-- 结果信息 -->
    <view class="result-info" v-if="resultMessage">
      <text :class="{ 'success-msg': isSuccess, 'error-msg': !isSuccess }">
        {{ resultMessage }}
      </text>
    </view>
  </view>
</template>

<script>
import config from '@/utils/config.js'

export default {
  data() {
    return {
      formData: {
        text: '',
        voice: 'Cherry',
        languageType: 'Chinese'
      },
      voiceOptions: [
        { value: 'Cherry', label: '🍒 Cherry - 甜美女声（推荐中文）' },
        { value: 'Qwen', label: '👨 Qwen - 温和男声（中英文）' },
        { value: 'Dylan', label: '🎩 Dylan - 成熟男声（英文）' },
        { value: 'Jada', label: '💁 Jada - 亲切女声（英文）' },
        { value: 'Sunny', label: '☀️ Sunny - 活力女声（英文）' }
      ],
      languageOptions: [
        { value: 'Chinese', label: '🇨🇳 中文' },
        { value: 'English', label: '🇺🇸 英文' }
      ],
      examples: [
        { name: '中文示例', text: '你好，我是智能语音助手小乐，很高兴为您服务！', language: 'Chinese' },
        { name: '英文示例', text: 'Hello! Today is a wonderful day to build something people love!', language: 'English' },
        { name: '问候语', text: '欢迎使用智慧养老助手系统，我们致力于为老年人提供贴心的服务。', language: 'Chinese' }
      ],
      selectedVoiceIndex: 0,
      selectedLanguageIndex: 0,
      loading: false,
      audioUrl: '',
      audioId: 'tts-audio',
      isPlaying: false,
      resultMessage: '',
      isSuccess: false,
      audioContext: null  // 保存音频上下文实例
    }
  },
  methods: {
    // 音色选择变化
    onVoiceChange(e) {
      this.selectedVoiceIndex = e.detail.value
      this.formData.voice = this.voiceOptions[e.detail.value].value
      console.log('选择音色：', this.formData.voice)
    },

    // 语言选择变化
    onLanguageChange(e) {
      this.selectedLanguageIndex = e.detail.value
      this.formData.languageType = this.languageOptions[e.detail.value].value
      console.log('选择语言：', this.formData.languageType)
    },

    // 选择示例文本
    selectExample(example) {
      this.formData.text = example.text
      this.formData.languageType = example.language
      this.selectedLanguageIndex = this.languageOptions.findIndex(
        lang => lang.value === example.language
      )
      uni.showToast({ title: '已加载示例', icon: 'success', duration: 1000 })
    },

    // 语音合成
    async synthesize() {
      if (!this.formData.text.trim()) {
        uni.showToast({ title: '请输入文本', icon: 'none' })
        return
      }

      this.loading = true
      this.resultMessage = ''
      this.audioUrl = ''

      try {
        console.log('🚀 发送语音合成请求：', this.formData)

        const response = await uni.request({
          url: `${config.API_BASE_URL}/api/tts/synthesize`,
          method: 'POST',
          data: this.formData,
          header: {
            'Content-Type': 'application/json'
          }
        })

        console.log('📥 收到响应：', response.data)

        if (response.data.success) {
          this.audioUrl = response.data.audioUrl
          this.isSuccess = true
          this.resultMessage = '✅ ' + response.data.message
          
          uni.showToast({ title: '合成成功！', icon: 'success' })
          
          // 自动播放
          setTimeout(() => {
            this.playAudio()
          }, 500)
        } else {
          this.isSuccess = false
          this.resultMessage = '❌ ' + response.data.message
          uni.showToast({ title: response.data.message, icon: 'none', duration: 3000 })
        }

      } catch (error) {
        console.error('❌ 请求失败：', error)
        this.isSuccess = false
        this.resultMessage = '❌ 网络错误，请检查后端服务'
        uni.showToast({ title: '网络错误', icon: 'none' })
      } finally {
        this.loading = false
      }
    },

    // 播放音频
    playAudio() {
      // 如果音频上下文存在且处于暂停状态，继续播放
      if (this.audioContext && !this.isPlaying) {
        this.audioContext.play()
        this.isPlaying = true
        console.log('▶️ 继续播放')
        return
      }
      
      // 如果已有音频在播放，先销毁
      if (this.audioContext) {
        this.audioContext.destroy()
      }
      
      // 创建新的音频上下文
      this.audioContext = uni.createInnerAudioContext()
      this.audioContext.src = this.audioUrl
      this.audioContext.play()
      this.isPlaying = true
      console.log('🎵 开始播放新音频')
      
      // 播放结束事件
      this.audioContext.onEnded(() => {
        this.isPlaying = false
        if (this.audioContext) {
          this.audioContext.destroy()
          this.audioContext = null
        }
        console.log('✅ 播放完成')
      })
      
      // 播放错误事件
      this.audioContext.onError((e) => {
        console.error('音频播放错误：', e)
        uni.showToast({ title: '播放失败', icon: 'none' })
        this.isPlaying = false
        if (this.audioContext) {
          this.audioContext.destroy()
          this.audioContext = null
        }
      })
    },

    // 暂停音频
    pauseAudio() {
      if (this.audioContext) {
        this.audioContext.pause()
        this.isPlaying = false
        console.log('⏸️ 音频已暂停')
      }
    },

    // 停止音频
    stopAudio() {
      if (this.audioContext) {
        this.audioContext.stop()
        this.audioContext.destroy()
        this.audioContext = null
        this.isPlaying = false
        console.log('⏹️ 音频已停止')
      }
    },

    // 音频播放事件
    onPlay() {
      this.isPlaying = true
      console.log('🎵 音频开始播放')
    },

    onPause() {
      this.isPlaying = false
      console.log('⏸️ 音频暂停')
    },

    onEnded() {
      this.isPlaying = false
      console.log('✅ 音频播放完成')
    },

    onError(e) {
      console.error('❌ 音频播放错误：', e)
      uni.showToast({ title: '音频播放失败', icon: 'none' })
      this.isPlaying = false
    },

    // 复制URL
    copyUrl() {
      uni.setClipboardData({
        data: this.audioUrl,
        success: () => {
          uni.showToast({ title: '已复制', icon: 'success' })
        }
      })
    },

    // 清空表单
    clearForm() {
      this.formData.text = ''
      this.audioUrl = ''
      this.resultMessage = ''
      this.isPlaying = false
      uni.showToast({ title: '已清空', icon: 'success', duration: 1000 })
    }
  },
  
  // 页面卸载时清理音频资源
  onUnload() {
    if (this.audioContext) {
      this.audioContext.stop()
      this.audioContext.destroy()
      this.audioContext = null
      console.log('🧹 页面卸载，音频资源已清理')
    }
  }
}
</script>

<style scoped>
.tts-test-container {
  min-height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 40rpx 30rpx;
}

/* 标题 */
.header {
  text-align: center;
  margin-bottom: 40rpx;
}

.title {
  display: block;
  font-size: 48rpx;
  font-weight: bold;
  color: #fff;
  margin-bottom: 10rpx;
}

.subtitle {
  display: block;
  font-size: 26rpx;
  color: rgba(255, 255, 255, 0.8);
}

/* 表单容器 */
.form-container {
  background: #fff;
  border-radius: 20rpx;
  padding: 30rpx;
  margin-bottom: 30rpx;
  box-shadow: 0 8rpx 30rpx rgba(0, 0, 0, 0.15);
}

.form-group {
  margin-bottom: 30rpx;
}

.label {
  display: block;
  font-size: 28rpx;
  color: #333;
  font-weight: bold;
  margin-bottom: 15rpx;
}

/* 文本输入 */
.text-input {
  width: 100%;
  min-height: 200rpx;
  background: #f8f9fa;
  border-radius: 12rpx;
  padding: 20rpx;
  font-size: 28rpx;
  border: 2rpx solid #e0e0e0;
  box-sizing: border-box;
}

.char-count {
  display: block;
  text-align: right;
  font-size: 24rpx;
  color: #999;
  margin-top: 10rpx;
}

/* 选择器 */
.picker-display {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: #f8f9fa;
  border-radius: 12rpx;
  padding: 20rpx 25rpx;
  border: 2rpx solid #e0e0e0;
}

.arrow {
  color: #999;
  font-size: 20rpx;
}

/* 快捷示例 */
.quick-examples {
  display: flex;
  gap: 15rpx;
  flex-wrap: wrap;
}

.example-btn {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #fff;
  padding: 15rpx 25rpx;
  border-radius: 30rpx;
  font-size: 24rpx;
}

/* 按钮组 */
.button-group {
  display: flex;
  gap: 20rpx;
  margin-top: 40rpx;
}

.btn {
  flex: 1;
  border-radius: 30rpx;
  font-size: 30rpx;
  padding: 25rpx;
  border: none;
}

.btn-primary {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #fff;
}

.btn-primary[disabled] {
  opacity: 0.6;
}

.btn-secondary {
  background: #f0f0f0;
  color: #666;
}

/* 音频播放器 */
.audio-player {
  background: #fff;
  border-radius: 20rpx;
  padding: 30rpx;
  margin-bottom: 30rpx;
  box-shadow: 0 8rpx 30rpx rgba(0, 0, 0, 0.15);
}

.player-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20rpx;
}

.player-title {
  font-size: 32rpx;
  font-weight: bold;
  color: #333;
}

.status {
  font-size: 24rpx;
  color: #999;
  padding: 8rpx 20rpx;
  border-radius: 20rpx;
  background: #f0f0f0;
}

.status.playing {
  background: #e8f5e9;
  color: #4caf50;
}

.player-controls {
  display: flex;
  gap: 15rpx;
  margin: 20rpx 0;
}

.control-btn {
  flex: 1;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #fff;
  border-radius: 15rpx;
  padding: 20rpx;
  font-size: 26rpx;
  border: none;
}

.audio-url {
  margin-top: 20rpx;
  padding: 15rpx;
  background: #f8f9fa;
  border-radius: 10rpx;
}

.url-label {
  font-size: 24rpx;
  color: #666;
  display: block;
  margin-bottom: 8rpx;
}

.url-value {
  font-size: 22rpx;
  color: #667eea;
  word-break: break-all;
}

/* 结果信息 */
.result-info {
  background: #fff;
  border-radius: 20rpx;
  padding: 25rpx;
  text-align: center;
  box-shadow: 0 8rpx 30rpx rgba(0, 0, 0, 0.15);
}

.success-msg {
  color: #4caf50;
  font-size: 28rpx;
}

.error-msg {
  color: #f44336;
  font-size: 28rpx;
}
</style>
