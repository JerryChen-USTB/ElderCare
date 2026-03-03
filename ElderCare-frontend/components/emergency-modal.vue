<template>
  <!-- 紧急求助自定义弹窗 -->
  <view v-if="show" class="emergency-modal-overlay" @click="handleClose">
    <view class="emergency-modal-container" @click.stop>
      <!-- 紧急图标 -->
      <view class="emergency-icon-wrapper">
        <view class="emergency-icon">
          <text class="icon-text">!</text>
        </view>
        <view class="emergency-pulse"></view>
      </view>
      
      <!-- 标题 -->
      <view class="emergency-title">{{ title }}</view>
      
      <!-- 内容 -->
      <view class="emergency-content">{{ content }}</view>
      
      <!-- 知道了按钮 -->
      <view class="emergency-button" @click="handleClose">
        知道了
      </view>
    </view>
  </view>
</template>

<script>
export default {
  name: 'EmergencyModal',
  props: {
    show: {
      type: Boolean,
      default: false
    },
    title: {
      type: String,
      default: '紧急求助通知'
    },
    content: {
      type: String,
      default: '有人向您发出紧急求助'
    },
    data: {
      type: Object,
      default: () => ({})
    }
  },
  methods: {
    handleClose() {
      this.$emit('close')
    }
  }
}
</script>

<style scoped>
/* 紧急求助弹窗样式 */
.emergency-modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: rgba(0, 0, 0, 0.75);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 9999;
  animation: fadeIn 0.3s ease;
}

@keyframes fadeIn {
  from {
    opacity: 0;
  }
  to {
    opacity: 1;
  }
}

.emergency-modal-container {
  width: 600rpx;
  background: linear-gradient(135deg, #fff 0%, #fff5f5 100%);
  border-radius: 40rpx;
  padding: 60rpx 50rpx 50rpx;
  box-shadow: 0 20rpx 60rpx rgba(229, 77, 66, 0.4);
  animation: slideUp 0.4s cubic-bezier(0.34, 1.56, 0.64, 1);
  position: relative;
  border: 4rpx solid #ff4444;
}

@keyframes slideUp {
  from {
    transform: translateY(100rpx);
    opacity: 0;
  }
  to {
    transform: translateY(0);
    opacity: 1;
  }
}

/* 紧急图标区域 */
.emergency-icon-wrapper {
  display: flex;
  justify-content: center;
  align-items: center;
  margin-bottom: 40rpx;
  position: relative;
}

.emergency-icon {
  width: 120rpx;
  height: 120rpx;
  background: linear-gradient(135deg, #ff4444, #e54d42);
  border-radius: 50%;
  display: flex;
  justify-content: center;
  align-items: center;
  box-shadow: 0 10rpx 30rpx rgba(229, 77, 66, 0.5);
  z-index: 2;
  animation: shake 0.5s ease infinite;
}

@keyframes shake {
  0%, 100% {
    transform: translateX(0) rotate(0deg);
  }
  25% {
    transform: translateX(-5rpx) rotate(-5deg);
  }
  75% {
    transform: translateX(5rpx) rotate(5deg);
  }
}

.icon-text {
  color: white;
  font-size: 80rpx;
  font-weight: bold;
  line-height: 1;
}

/* 脉冲动画 */
.emergency-pulse {
  position: absolute;
  width: 120rpx;
  height: 120rpx;
  border-radius: 50%;
  background: rgba(229, 77, 66, 0.4);
  animation: pulse 1.5s ease-out infinite;
}

@keyframes pulse {
  0% {
    transform: scale(1);
    opacity: 1;
  }
  100% {
    transform: scale(2.5);
    opacity: 0;
  }
}

/* 标题 */
.emergency-title {
  font-size: 44rpx;
  font-weight: bold;
  color: #e54d42;
  text-align: center;
  margin-bottom: 30rpx;
  letter-spacing: 2rpx;
}

/* 内容 */
.emergency-content {
  font-size: 32rpx;
  color: #333;
  line-height: 1.8;
  text-align: center;
  margin-bottom: 50rpx;
  padding: 0 20rpx;
  word-break: break-all;
}

/* 知道了按钮 */
.emergency-button {
  width: 100%;
  height: 100rpx;
  background: linear-gradient(135deg, #ff4444, #e54d42);
  border-radius: 50rpx;
  display: flex;
  justify-content: center;
  align-items: center;
  color: white;
  font-size: 36rpx;
  font-weight: bold;
  box-shadow: 0 10rpx 30rpx rgba(229, 77, 66, 0.4);
  transition: all 0.3s ease;
}

.emergency-button:active {
  transform: scale(0.95);
  box-shadow: 0 5rpx 15rpx rgba(229, 77, 66, 0.3);
}
</style>

