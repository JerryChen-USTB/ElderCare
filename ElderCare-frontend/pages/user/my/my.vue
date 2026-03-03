<template>
  <view class="my-container">
    <view class="user-info">
      <image 
        :src="getAvatarUrl(userInfo.avatarUrl)" 
        class="avatar" 
        @error="handleImageError"
        @load="handleImageLoad"
      />
      <view class="info">
        <text class="name" :style="{ fontSize: (currentFontSize * 1.2) + 'px' }">{{ userInfo.name || '用户' }}</text>
        <text class="phone" :style="dynamicStyle">{{ formatPhone(userInfo.phone) }}</text>
      </view>
      <image src="/static/icons/edit.png" class="edit-icon" @click="navigateToProfile" />
    </view>
    
    <!-- 个人资料和账号与安全 -->
    <view class="card-group">
      <view class="personal-info" @click="navigateToProfile">
        <text class="function-label" :style="dynamicStyle">个人资料</text>
        <image class="enter-icon" src="/static/icons/enter.png"></image>
      </view>
      <view class="divider-line"></view>
      <view class="account-security" @click="navigateToSecurity">
        <text class="function-label" :style="dynamicStyle">账号与安全</text>
        <image class="enter-icon" src="/static/icons/enter.png"></image>
      </view>
    </view>
    <!-- 接受新消息通知和文本大小 -->
    <view class="card-group">
      <view class="notification-setting">
        <text :style="dynamicStyle">接受新消息通知</text>
        <switch 
          :checked="isReceiveNotification" 
          @change="(e) => isReceiveNotification = e.detail.value"
          color="#E5EDD6"
        />
      </view>
      <view class="divider-line"></view>
      <view class="font-size-setting" @click="showTextSizeModal">
        <text class="function-label" :style="dynamicStyle">文本大小</text>
        <view class="current-size-display">
          <text class="current-size-text" :style="dynamicStyle">{{ getSizeDisplayName(textSize) }}</text>
          <image class="arrow-icon" src="/static/arrow_right.png"></image>
        </view>
      </view>
    </view>
    <!-- 关于我们和帮助与反馈 -->
    <view class="card-group">
      <view class="about-us" @click="navigateToAbout">
        <text class="function-label" :style="dynamicStyle">关于我们</text>
        <image class="arrow-icon" src="/static/arrow_right.png"></image>
      </view>
      <view class="divider-line"></view>
      <view class="help-feedback" @click="navigateToFeedback">
        <text class="function-label" :style="dynamicStyle">帮助与反馈</text>
        <image class="arrow-icon" src="/static/arrow_right.png"></image>
      </view>
    </view>
    <!-- 退出登录 -->
    <button class="logout-btn" @click="logout" :style="dynamicStyle">退出登录</button>
    
    <custom-tabbar :current="1" :role="'user'" />
    
    <!-- 字体大小设置弹窗 -->
    <view v-if="showTextSizePopupFlag" class="popup-overlay" @click="hideTextSizeModal">
      <view class="text-size-modal" @click.stop>
        <view class="modal-header">
          <text class="modal-title">选择文本大小</text>
          <image class="close-icon" src="/static/icons/close.png" @click="hideTextSizeModal"></image>
        </view>
        <view class="text-size-options">
          <view
            class="text-size-option"
            :class="{active: textSize === 'small'}"
            @click="setTextSize('small')"
          >
            <text style="font-size: 12px; color: #4B3425;">A</text>
            <text class="size-label">小</text>
          </view>
          <view
            class="text-size-option"
            :class="{active: textSize === 'normal'}"
            @click="setTextSize('normal')"
          >
            <text style="font-size: 16px; color: #4B3425;">A</text>
            <text class="size-label">标准</text>
          </view>
          <view
            class="text-size-option"
            :class="{active: textSize === 'large'}"
            @click="setTextSize('large')"
          >
            <text style="font-size: 20px; color: #4B3425;">A</text>
            <text class="size-label">大</text>
          </view>
          <view
            class="text-size-option"
            :class="{active: textSize === 'huge'}"
            @click="setTextSize('huge')"
          >
            <text style="font-size: 24px; color: #4B3425;">A</text>
            <text class="size-label">超大</text>
          </view>
        </view>
        <button class="apply-button" @click="applyTextSize">应用</button>
      </view>
    </view>
  </view>
</template>

<script>
import CustomTabbar from '@/components/custom-tabbar.vue'
import request from '@/utils/request.js'

export default {
  components: { CustomTabbar },
  data() {
    return {
      userInfo: {
        name: '',
        phone: '',
        avatarUrl: '',
        userId: null
      },
      isReceiveNotification: true,
      textSize: 'normal',
      showTextSizePopupFlag: false,
      sizeMap: {
        small: 12,
        normal: 16,
        large: 20,
        huge: 24
      }
    }
  },
  computed: {
    // 当前字体大小（像素）
    currentFontSize() {
      return this.sizeMap[this.textSize] || 16;
    },
    // 动态样式对象（rpx单位）
    dynamicStyle() {
      const rpxSize = this.currentFontSize * 2; // px转rpx
      return {
        fontSize: `${rpxSize}rpx`
      };
    }
  },
  onLoad() {
    this.loadUserInfo();
    this.loadTextSizeSettings();
    
    // 监听头像和姓名更新事件
    uni.$on('avatarUpdated', this.handleAvatarUpdated);
    uni.$on('nameUpdated', this.handleNameUpdated);
  },
  
  onShow() {
    // 页面显示时刷新用户信息（处理从其他页面返回的情况）
    this.refreshUserInfo();
  },
  
  onUnload() {
    // 移除事件监听
    uni.$off('avatarUpdated', this.handleAvatarUpdated);
    uni.$off('nameUpdated', this.handleNameUpdated);
  },
  methods: {
    // 加载用户信息
    async loadUserInfo() {
      try {
        // 从本地存储获取用户信息
        const storedUser = uni.getStorageSync('userInfo');
        if (storedUser) {
          this.userInfo.phone = storedUser.phone;
          this.userInfo.userId = storedUser.id;
          
          // 获取老人详细信息
          const elderRes = await request.elderApi.getElderInfo(storedUser.id);
          
          if (elderRes.success && elderRes.elder) {
            this.userInfo.name = elderRes.elder.name || storedUser.phone;
            this.userInfo.avatarUrl = elderRes.elder.avatarUrl || '';
          } else {
            this.userInfo.name = storedUser.phone; // 默认使用手机号作为姓名
          }
        } else {
          // 如果没有存储的用户信息，跳转到登录页
          uni.showToast({
            title: '请先登录',
            icon: 'none'
          });
          setTimeout(() => {
            uni.reLaunch({
              url: '/pages/login/login'
            });
          }, 1500);
        }
      } catch (error) {
        console.error('获取用户信息失败:', error);
        uni.showToast({
          title: '获取用户信息失败',
          icon: 'none'
        });
      }
    },
    
    // 加载字体大小设置
    loadTextSizeSettings() {
      try {
        const savedTextSize = uni.getStorageSync('textSize');
        if (savedTextSize && this.sizeMap[savedTextSize]) {
          this.textSize = savedTextSize;
          console.log('已加载保存的字体大小设置:', savedTextSize);
        }
      } catch (error) {
        console.error('加载字体大小设置失败:', error);
      }
    },
    
    // 格式化手机号显示
    formatPhone(phone) {
      if (!phone) return '';
      return phone.replace(/(\d{3})\d{4}(\d{4})/, '$1****$2');
    },
    
    // 刷新用户信息（轻量级，不显示loading）
    async refreshUserInfo() {
      try {
        const storedUser = uni.getStorageSync('userInfo');
        if (storedUser && storedUser.id) {
          const elderRes = await request.elderApi.getElderInfo(storedUser.id);
          if (elderRes.success && elderRes.elder) {
            const oldAvatarUrl = this.userInfo.avatarUrl;
            const oldName = this.userInfo.name;
            
            this.userInfo.name = elderRes.elder.name || storedUser.phone;
            this.userInfo.avatarUrl = elderRes.elder.avatarUrl || '';
            
            // 如果头像或姓名有变化，强制更新页面
            if (oldAvatarUrl !== this.userInfo.avatarUrl || oldName !== this.userInfo.name) {
              console.log('My页面数据已刷新');
              this.$forceUpdate();
            }
          }
        }
      } catch (error) {
        console.error('My页面刷新用户信息失败:', error);
      }
    },
    
    // 获取头像URL
    getAvatarUrl(avatarUrl) {
      if (avatarUrl && avatarUrl.trim() !== '') {
        // 如果是相对路径，添加后端基础URL
        if (avatarUrl.startsWith('/uploads/')) {
          // 如果已经带有时间戳，直接添加基础URL（避免重复生成时间戳）
          if (avatarUrl.includes('?t=')) {
            return request.BASE_URL + avatarUrl;
          }
          // 否则添加时间戳避免缓存
          const fullUrl = request.BASE_URL + avatarUrl + '?t=' + Date.now();
          return fullUrl;
        }
        return avatarUrl;
      }
      // 默认头像（Spring Boot静态资源自动映射）
      return request.BASE_URL + '/uploads/avatars/default-avatar.png';
    },
    
    // 处理图片加载成功
    handleImageLoad(event) {
      // 图片加载成功，无需额外日志
    },
    
    // 处理图片加载失败
    handleImageError(event) {
      console.error('❌ My页面头像图片加载失败', event);
      
      // 如果当前头像加载失败，尝试重新加载
      if (this.userInfo.avatarUrl && this.userInfo.avatarUrl.includes('/uploads/')) {
        console.log('🔄 My页面尝试重新加载头像...');
        
        // 等待1秒后重新尝试
        setTimeout(() => {
          this.$forceUpdate();
        }, 1000);
      }
    },
    
    // 处理头像更新事件
    handleAvatarUpdated(data) {
      if (data.userId === this.userInfo.userId) {
        this.userInfo.avatarUrl = data.avatarUrl;
        // 强制更新页面
        this.$forceUpdate();
      }
    },
    
    // 处理姓名更新事件
    handleNameUpdated(data) {
      if (data.userId === this.userInfo.userId) {
        console.log('My页面收到姓名更新通知:', data);
        this.userInfo.name = data.name;
        // 强制更新页面
        this.$forceUpdate();
      }
    },
    
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
            // 清除本地存储的用户信息
            uni.removeStorageSync('userInfo');
            uni.removeStorageSync('token');
            
            uni.showToast({
              title: '退出登录成功',
              icon:'success'
            })
            
            // 跳转到登录页
            setTimeout(() => {
              uni.reLaunch({
                url: '/pages/login/login'
              })
            }, 1000);
          } else if (res.cancel) {
            // 用户点击取消，不做处理
          }
        }
      })
    },
    navigateToProfile() {
      uni.navigateTo({
        url: '/pages/user/my/profile'  // 确保路径正确
      })
    },
    navigateToSecurity() {
      uni.navigateTo({
        url: '/pages/user/my/safe'  // 修改路径为正确的安全页面路径
      })
    },
    navigateToAbout() {
      uni.navigateTo({
        url: '/pages/user/my/about'  // 确保路径正确
      })
    },
    navigateToFeedback() {
      uni.navigateTo({
        url: '/pages/user/my/feedback'  // 确保路径正确
      })
    },
    setTextSize(size) {
      this.textSize = size;
    },
    applyTextSize() {
      try {
        // 保存字体大小设置到本地存储
        uni.setStorageSync('textSize', this.textSize);
        
        // 强制更新页面以应用新的字体大小
        this.$forceUpdate();
        
        uni.showToast({
          title: '字体大小已应用',
          icon: 'success'
        });
        
        this.hideTextSizeModal();
        
        console.log('字体大小已设置为:', this.textSize);
      } catch (error) {
        console.error('应用字体大小失败:', error);
        uni.showToast({
          title: '设置失败，请重试',
          icon: 'none'
        });
      }
    },
    showTextSizeModal() {
      this.showTextSizePopupFlag = true;
    },
    hideTextSizeModal() {
      this.showTextSizePopupFlag = false;
    },
    getSizeDisplayName(size) {
      const sizeNames = {
        small: '小',
        normal: '标准',
        large: '大',
        huge: '超大'
      };
      return sizeNames[size] || '标准';
    }
  }
}
</script>

<style scoped>
/* 参考首页UI设计风格 */
.my-container {
  padding-bottom: 120rpx;
  background-color: #F8F4F4;
  padding: 20rpx;
  height: 100vh;
  max-height: 100vh;
  position: relative;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  box-sizing: border-box;
}

/* 强制移除button边框的全局样式 */
button {
  border: none !important;
  outline: none !important;
}

button::after {
  border: none !important;
}

.user-info {
  display: flex;
  align-items: center;
  padding: 60rpx 40rpx;
  background-color: #9AB169;
  color: white;
  border-radius: 32rpx;
  margin-top: 60rpx;
  margin-bottom: 30rpx;
  box-shadow: 0 4rpx 12rpx rgba(0,0,0,0.05);
}

.avatar {
  width: 120rpx;
  height: 120rpx;
  border-radius: 50%;
  border: 4rpx solid rgba(255,255,255,0.3);
  margin-right: 30rpx;
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
  color: white;
}

.phone {
  font-size: 30rpx;
  opacity: 0.9;
  color: white;
}

.edit-icon {
  width: 40rpx;
  height: 40rpx;
  cursor: pointer;
  transition: opacity 0.3s;
}

.edit-icon:active {
  opacity: 0.6;
}


.logout-btn {
  margin: 50rpx 0 0;
  height: 100rpx;
  line-height: 100rpx;
  background-color: #FFF0EC;
  color: #FF824D;
  font-size: 32rpx;
  border-radius: 32rpx;
  border: none !important;
  outline: none !important;
  -webkit-appearance: none;
  appearance: none;
  box-shadow: 0 4rpx 12rpx rgba(0,0,0,0.05);
  font-weight: 600;
}

.card-group {
  background-color: #ffffff;
  border-radius: 32rpx;
  box-shadow: 0 4rpx 12rpx rgba(0,0,0,0.05);
  margin-bottom: 20rpx;
  overflow: hidden;
}

.personal-info,
.account-security,
.notification-setting,
.about-us,
.help-feedback {
  padding: 30rpx;
  color: #4B3425;
  cursor: pointer;
  min-height: 80rpx;
  display: flex;
  align-items: center;
}

.font-size-setting {
  padding: 30rpx;
  color: #4B3425;
  cursor: pointer;
  min-height: 80rpx;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.font-size-setting .function-label {
  margin-right: auto;
  text-align: left;
  font-size: 32rpx;
  font-weight: 600;
}

.divider-line {
  height: 2rpx;
  background-color: #e8e8e8;
  margin: 0 30rpx;
}
.personal-info,
.account-security,
.about-us,
.help-feedback {
  justify-content: space-between;
}

.function-label {
  font-size: 32rpx;
  color: #4B3425;
  font-weight: 600;
  text-align: left;
  margin-right: auto;
}
.notification-setting {
  justify-content: space-between;
}

.notification-setting text {
  font-size: 32rpx;
  color: #4B3425;
  font-weight: 600;
  text-align: left;
  margin-right: auto;
}

.current-size-display {
  display: flex;
  align-items: center;
  gap: 8rpx;
  flex-shrink: 0;
}

.current-size-text {
  font-size: 28rpx;
  color: #666;
}
.arrow-icon {
  width: 24rpx;
  height: 24rpx;
  opacity: 0.6;
}

.enter-icon {
  width: 32rpx;
  height: 32rpx;
  opacity: 0.7;
}

/* 弹窗样式 */
.popup-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: rgba(0, 0, 0, 0.5);
  z-index: 9999;
  display: flex;
  align-items: flex-end;
  justify-content: center;
}

.text-size-modal {
  background-color: white;
  border-radius: 32rpx 32rpx 0 0;
  padding: 40rpx 30rpx;
  min-height: 500rpx;
  width: 100%;
  max-width: 750rpx;
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 40rpx;
  padding-bottom: 20rpx;
  border-bottom: 2rpx solid #f1f1f1;
}

.modal-title {
  font-size: 36rpx;
  font-weight: bold;
  color: #4B3425;
}

.close-icon {
  width: 40rpx;
  height: 40rpx;
  opacity: 0.6;
}

.text-size-options {
  display: flex;
  justify-content: space-between;
  margin-bottom: 40rpx;
  gap: 16rpx;
}

.text-size-option {
  padding: 20rpx 16rpx;
  border-radius: 16rpx;
  cursor: pointer;
  background-color: #F8F4F4;
  border: 2rpx solid transparent;
  transition: all 0.2s ease;
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 100rpx;
}

.text-size-option.active {
  background-color: #9AB169;
  color: white;
  border-color: #9AB169;
}

.size-label {
  font-size: 24rpx;
  display: block;
  text-align: center;
  margin-top: 8rpx;
  color: #4B3425;
}

.text-size-option.active .size-label {
  color: white;
}

.text-size-option.active text {
  color: white !important;
}

.apply-button {
  background-color: #9AB169;
  color: white;
  padding: 24rpx 40rpx;
  border: none;
  border-radius: 16rpx;
  font-size: 32rpx;
  cursor: pointer;
  font-weight: 600;
  box-shadow: 0 2rpx 8rpx rgba(154,177,105,0.3);
  width: 100%;
}
</style>