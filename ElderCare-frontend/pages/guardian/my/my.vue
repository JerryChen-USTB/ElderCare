<template>
  <view class="my-container">
    <!-- 顶部用户信息卡片 -->
    <view class="user-card">
      <view class="user-info">
        <view class="avatar-wrapper">
          <image 
            :src="getAvatarUrl(avatarUrl)" 
            class="avatar" 
            @error="handleAvatarError" 
            mode="aspectFill"  
          />
          <view class="avatar-edit-mask">
            <image src="/static/icons/camera.png" class="camera-icon" />
          </view>
        </view>
        <view class="info">
          <text class="name">{{ userName }}</text>
          <text class="phone">{{ phoneNumber }}</text>
        </view>
        <image src="/static/icons/edit.png" class="edit-icon" @click="navigateToProfile" />
      </view>
    </view>

    <!-- 功能列表区域 -->
    <view class="function-list">
      <!-- 个人资料 -->
      <view class="list-item personal-info" @click="navigateToProfile">
        <view class="item-left">
          <image class="item-icon" src="/static/icons/profile.png"></image>
          <text class="function-label">个人资料</text>
        </view>
        <image class="arrow-icon" src="/static/arrow_right.png"></image>
      </view>
      
      <!-- 账号与安全 -->
      <view class="list-item account-security" @click="navigateToSecurity">
        <view class="item-left">
          <image class="item-icon" src="/static/icons/security.png"></image>
          <text class="function-label">账号与安全</text>
        </view>
        <image class="arrow-icon" src="/static/arrow_right.png"></image>
      </view>
      
      <!-- 消息通知 -->
      <view class="list-item notification-setting">
        <view class="item-left">
          <image class="item-icon" src="/static/icons/notification.png"></image>
          <text>接受新消息通知</text>
        </view>
        <switch 
          :checked="isReceiveNotification" 
          @change="(e) => isReceiveNotification = e.detail.value"
          color="#63b530" 
        />
      </view>
      
      <!-- 关于我们 -->
      <view class="list-item about-us" @click="navigateToAbout">
        <view class="item-left">
          <image class="item-icon" src="/static/icons/about.png"></image>
          <text class="function-label">关于我们</text>
        </view>
        <image class="arrow-icon" src="/static/arrow_right.png"></image>
      </view>
      
      <!-- 帮助与反馈 -->
      <view class="list-item help-feedback" @click="navigateToFeedback">
        <view class="item-left">
          <image class="item-icon" src="/static/icons/feedback.png"></image>
          <text class="function-label">帮助与反馈</text>
        </view>
        <image class="arrow-icon" src="/static/arrow_right.png"></image>
      </view>
    </view>
    
    <!-- 退出登录 -->
    <view class="logout-wrapper">
      <button class="logout-btn" @click="logout">退出登录</button>
    </view>

    <!-- 修改了 role 属性为 'guardian' -->
    <custom-tabbar :current="1" :role="'guardian'" />
    
    <!-- 紧急求助弹窗组件 -->
    <emergency-modal 
      :show="showEmergencyModal"
      :title="emergencyTitle"
      :content="emergencyContent"
      :data="emergencyData"
      @close="closeEmergencyModal"
    />
  </view>
</template>

<script>
import config from '@/utils/config.js'
import CustomTabbar from '@/components/custom-tabbar.vue'
import request from '@/utils/request.js'
import EmergencyModal from '@/components/emergency-modal.vue'
import EmergencyModalMixin from '@/mixins/emergency-modal.js'

export default {
  components: { CustomTabbar, EmergencyModal },
  mixins: [EmergencyModalMixin],
  data() {
    return {
      isReceiveNotification: true,
      // 原有变量（未修改）
      userName: '加载中...',
      phoneNumber: '加载中...', // 新增：手机号变量
      userId: null,
      avatarUrl: '' // 头像URL（相对路径），由getAvatarUrl方法处理
    }
  },
  onLoad() {
    this.initUserInfo();
  },
  methods: {
    // 原有方法（未修改）
    initUserInfo() {
      const userInfo = uni.getStorageSync('userInfo')
      if (userInfo && userInfo.id) {
        this.userId = userInfo.id
        this.getGuardianInfo() // 改为调用获取完整信息的方法
      } else {
        this.userName = '请登录'
        this.phoneNumber = '请登录'
      }
    },

    // 原有方法（修改头像处理逻辑）
    async getGuardianInfo() {
      try {
        const guardian = await request.get(`/api/guardian/info/${this.userId}`)
        
        // 直接保存相对路径，由getAvatarUrl方法统一处理
        this.avatarUrl = guardian?.avatarUrl || ''
        
        // 原有姓名和手机号处理
        this.userName = guardian?.name || '未知姓名'
        this.phoneNumber = this.maskPhone(guardian?.phone)
      } catch (error) {
        console.error('获取监护人信息失败：', error)
        this.userName = '加载失败'
        this.phoneNumber = '加载失败'
        // 错误时清空头像URL，让getAvatarUrl返回默认头像
        this.avatarUrl = ''
      }
    },
    
    // 获取头像URL（添加BASE_URL和时间戳避免缓存）
    getAvatarUrl(avatarUrl) {
      if (avatarUrl && avatarUrl.trim() !== '') {
        // 如果是相对路径，添加后端基础URL
        // 支持 /uploads/ 和 /upload/ 两种格式
        if (avatarUrl.startsWith('/uploads/') || avatarUrl.startsWith('/upload/')) {
          // 如果已经带有时间戳，直接添加基础URL
          if (avatarUrl.includes('?t=')) {
            return config.API_BASE_URL + avatarUrl;
          }
          // 否则添加时间戳避免缓存
          const fullUrl = config.API_BASE_URL + avatarUrl + '?t=' + Date.now();
          return fullUrl;
        }
        return avatarUrl;
      }
      // 默认头像（Spring Boot静态资源自动映射）
      return config.API_BASE_URL + '/uploads/avatars/default-avatar.png';
    },
		
    // 原有方法（未修改）
    maskPhone(phone) {
      // 1. 统一转为字符串，去除首尾空格
      const strPhone = String(phone).trim();  
      // 2. 处理空值或非11位的情况
      if (!strPhone || strPhone.length !== 11) {  
        return strPhone || '未绑定'; // 空值返回'未绑定'，否则返回原始值（如短号、带符号）
      }
      // 3. 11位手机号脱敏
      return strPhone.replace(/^(\d{3})(\d{4})(\d{4})$/, '$1****$3');  
    },

    // 原有方法（未修改）
    navigateToProfile() {
      uni.navigateTo({
        url: '/pages/guardian/my/profile'
      })
    },

    // 原有方法（未修改）
    navigateToSecurity() {
      uni.navigateTo({
        url: '/pages/guardian/my/safe'
      })
    },

    // 原有方法（未修改）
    navigateToAbout() {
      uni.navigateTo({
        url: '/pages/guardian/my/about'
      })
    },

    // 原有方法（未修改）
    navigateToFeedback() {
      uni.navigateTo({
        url: '/pages/guardian/my/feedback'
      })
    },

    // 原有方法（未修改）
    logout() {
      uni.showModal({
        title: '确认退出',
        content: '你确定要退出登录吗？',
        confirmText: '退出',
        cancelText: '返回',
        confirmColor: '#FF0000',
        cancelColor: '#000000',
        success: function (res) {
          if (res.confirm) {
            uni.showToast({
              title: '退出登录成功',
              icon:'success'
            })
            uni.redirectTo({
              url: '/pages/login/login'
            })
          }
        }
      })
    },

    // 头像加载失败处理
    handleAvatarError() {
      console.warn('头像加载失败，将显示默认头像');
      // 清空avatarUrl，让getAvatarUrl返回默认头像
      this.avatarUrl = '';
      // 强制刷新视图
      this.$forceUpdate();
    },

    // 新增：字体大小选择方法
    setTextSize(size) {
      this.textSize = size;
    },
  }
}
</script>

<style scoped>
/* 整体容器 - 原有样式（未修改） */
.my-container {
  padding: 20rpx;
  padding-bottom: 140rpx; /* 原120rpx，增加20rpx避免与tabbar紧贴导致回弹 */
  background-color: #f8f9fa;
  min-height: 100vh;
  overflow-y: auto; /* 明确设置垂直滚动，避免body和容器双重滚动 */
  -webkit-overflow-scrolling: touch; /* 优化移动端滚动流畅度 */
}

/* 用户卡片 - 原有样式（未修改） */
.user-card {
  background: linear-gradient(to right, #63b530, #458a00);
  border-radius: 24rpx;
  padding: 40rpx 30rpx;
  margin-bottom: 30rpx;
  margin-top: 50rpx;
  box-shadow: 0 10rpx 30rpx rgba(101, 162, 63, 0.2);
  color: white;
  position: relative;
  overflow: hidden;
}

.user-card::before {
  content: '';
  position: absolute;
  top: -50rpx;
  right: -50rpx;
  width: 200rpx;
  height: 200rpx;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.1);
}

.user-card::after {
  content: '';
  position: absolute;
  bottom: -60rpx;
  left: -60rpx;
  width: 200rpx;
  height: 200rpx;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.1);
}

.user-info {
  display: flex;
  align-items: center;
  margin-bottom: 40rpx;
  position: relative;
  z-index: 2;
}

.avatar-wrapper {
  position: relative;
  margin-right: 30rpx;
}

.avatar {
  width: 120rpx;
  height: 120rpx;
  border-radius: 50%;
  border: 4rpx solid rgba(255,255,255,0.5);
}

.avatar-edit-mask {
  position: absolute;
  bottom: 0;
  right: 0;
  width: 40rpx;
  height: 40rpx;
  background: #63b530;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 2rpx solid #fff;
}

.camera-icon {
  width: 22rpx;
  height: 22rpx;
}

.info {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.name {
  font-size: 40rpx;
  font-weight: bold;
  margin-bottom: 10rpx;
}

.phone {
  font-size: 30rpx;
  opacity: 0.9;
}

.edit-icon {
  width: 40rpx;
  height: 40rpx;
}

/* 功能列表 - 原有样式（未修改） */
.function-list {
  background-color: #fff;
  border-radius: 24rpx;
  overflow: hidden;
  box-shadow: 0 6rpx 18rpx rgba(0, 0, 0, 0.05);
  margin-bottom: 30rpx;
}

.list-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 30rpx;
  border-bottom: 1rpx solid #f1f1f1;
  position: relative;
}

.list-item:last-child {
  border-bottom: none;
}

.item-left {
  display: flex;
  align-items: center;
}

.item-icon {
  width: 40rpx;
  height: 40rpx;
  margin-right: 20rpx;
}

.function-label {
  font-size: 32rpx;
  color: #333;
}

.arrow-icon {
  width: 30rpx;
  height: 30rpx;
  opacity: 0.5;
}

/* 新增：字体设置相关样式 */
.font-size-setting {
  flex-direction: column;
  align-items: flex-start;
}

.text-size-control {
  display: flex;
  justify-content: space-between;
  width: 100%;
  margin-top: 20rpx;
}

.text-size-option {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 15rpx 20rpx;
  border-radius: 12rpx;
  cursor: pointer;
  transition: all 0.2s;
  border: 1rpx solid #eee;
}

.text-size-option.active {
  background-color: #63b530;
  color: white;
  border-color: #63b530;
  box-shadow: 0 4rpx 12rpx rgba(101, 162, 63, 0.3);
}

.size-label {
  font-size: 24rpx;
  margin-top: 10rpx;
}

.apply-btn-wrapper {
  padding: 0 30rpx 30rpx;
}

.apply-button {
  background-color: #63b530;
  color: white;
  border: none;
  border-radius: 12rpx;
  font-size: 28rpx;
  height: 80rpx;
  line-height: 80rpx;
  width: 100%;
}

/* 退出登录按钮 - 原有样式（未修改） */
.logout-wrapper {
  padding: 0 20rpx;
}

.logout-btn {
  background-color: #fff;
  color: #ff4d4f;
  font-size: 32rpx;
  height: 90rpx;
  line-height: 90rpx;
  border-radius: 16rpx;
  border: none;
  box-shadow: 0 4rpx 12rpx rgba(0, 0, 0, 0.05);
}

/* 开关样式微调 - 原有样式（未修改） */
switch {
  transform: scale(0.9);
}
</style>
