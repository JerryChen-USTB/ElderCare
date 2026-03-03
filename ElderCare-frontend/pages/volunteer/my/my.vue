<template>
  <view class="volunteer-my">
    <!-- 个人信息卡片 -->
    <view class="user-info">
       <image :src="avatarUrl" class="avatar" @click="editAvatar" @error="handleAvatarError" />
      <view class="info">
        <text class="name">{{ userInfo.name }}</text>
        <text class="phone">志愿者编号: {{ userInfo.id }}</text>
      </view>
    </view>
    
    <!-- 志愿者信息和服务记录 -->
    <view class="card-group">
      <view class="personal-info" @click="navigateTo('profile')">
        <text class="function-label">志愿者信息</text>
        <image class="enter-icon" src="/static/icons/enter.png"></image>
      </view>
      <view class="divider-line"></view>
      <view class="account-security" @click="navigateTo('service')">
        <text class="function-label">服务记录</text>
        <image class="enter-icon" src="/static/icons/enter.png"></image>
      </view>
    </view>

    <!-- 关于我们和帮助反馈 -->
    <view class="card-group">
      <view class="about-us" @click="navigateTo('about')">
        <text class="function-label">关于我们</text>
        <image class="arrow-icon" src="/static/arrow_right.png"></image>
      </view>
      <view class="divider-line"></view>
      <view class="help-feedback" @click="navigateTo('feedback')">
        <text class="function-label">帮助与反馈</text>
        <image class="arrow-icon" src="/static/arrow_right.png"></image>
      </view>
    </view>
    
    <!-- 退出登录 -->
    <button class="logout-btn" @click="logout">退出登录</button>

    <!-- 底部导航 -->
    <custom-tabbar :current="1" :role="'volunteer'" />
  </view>
</template>

<script>
import request from '@/utils/request.js'
import CustomTabbar from '@/components/custom-tabbar.vue'

export default {
  components: { CustomTabbar },
  data() {
    return {
      loading: false,
      userInfo: {
        id: null,
        userId: null,
        name: '',
        avatarUrl: '',
        serviceHours: 0
      },
      defaultAvatar: '/static/avatar-default.png'
    }
  },
  computed: {
    // 计算头像URL，统一处理
    avatarUrl() {
      if (!this.userInfo.avatarUrl) {
        return this.defaultAvatar;
      }
      
      // 如果已经是完整URL，直接返回
      if (this.userInfo.avatarUrl.startsWith('http://') || this.userInfo.avatarUrl.startsWith('https://')) {
        return this.userInfo.avatarUrl + '?t=' + Date.now();
      }
      
      // 相对路径，拼接完整URL
      const baseUrl = request.BASE_URL.endsWith('/') 
        ? request.BASE_URL.slice(0, -1)
        : request.BASE_URL;
      
      const normalizedUrl = this.userInfo.avatarUrl.startsWith('/') 
        ? this.userInfo.avatarUrl 
        : `/${this.userInfo.avatarUrl}`;
      
      return `${baseUrl}${normalizedUrl}?t=${Date.now()}`;
    }
  },
  onShow() {
    this.loadVolunteerInfo()
  },
  onLoad() {
    uni.$on('volunteerAvatarUpdated', this.handleAvatarUpdated);
  },
  onUnload() {
    uni.$off('volunteerAvatarUpdated', this.handleAvatarUpdated);
  },
  methods: {
    // 简化的加载志愿者信息方法
    async loadVolunteerInfo() {
      this.loading = true;
      try {
        const loginUser = uni.getStorageSync('userInfo');
        if (!loginUser || !loginUser.id) {
          uni.showToast({ title: '请先登录', icon: 'none' });
          setTimeout(() => uni.redirectTo({ url: '/pages/login/login' }), 1500);
          return;
        }
        
        const userId = parseInt(loginUser.id, 10);
        this.userInfo.userId = userId;

        const res = await request.get('/api/volunteer/infoByUserId', { userId });

        if (res.success && res.volunteerInfo) {
          const backendData = res.volunteerInfo;
          this.userInfo = {
            ...this.userInfo,
            ...backendData,
            avatarUrl: backendData.avatarUrl || ''
          };
          uni.setStorageSync('volunteerMyInfo', this.userInfo);
        } else {
          // 从缓存加载
          const cachedInfo = uni.getStorageSync('volunteerMyInfo');
          if (cachedInfo) this.userInfo = { ...this.userInfo, ...cachedInfo };
        }
      } catch (error) {
        console.error('加载志愿者信息失败:', error);
        // 从缓存加载
        const cachedInfo = uni.getStorageSync('volunteerMyInfo');
        if (cachedInfo) this.userInfo = { ...this.userInfo, ...cachedInfo };
      } finally {
        this.loading = false;
      }
    },

    // 简化的头像编辑方法
    editAvatar() {
      uni.showActionSheet({
        itemList: ['拍照', '从相册选择'],
        success: (res) => {
          if (res.tapIndex === 0) {
            this.chooseImage('camera');
          } else if (res.tapIndex === 1) {
            this.chooseImage('album');
          }
        }
      });
    },

    // 选择图片
    chooseImage(sourceType) {
      uni.chooseImage({
        count: 1,
        sizeType: ['compressed'],
        sourceType: [sourceType],
        success: (res) => {
          this.uploadAvatar(res.tempFilePaths[0]);
        },
        fail: (error) => {
          console.error('选择图片失败:', error);
          uni.showToast({
            title: '获取图片失败，请重试',
            icon: 'none'
          });
        }
      });
    },

    // 简化的头像上传方法
    async uploadAvatar(filePath) {
      if (!filePath) return;
      
      this.loading = true;
      let loadingShown = false;

      try {
        uni.showLoading({ title: '上传中...', mask: true });
        loadingShown = true;

        const uploadRes = await new Promise((resolve, reject) => {
          uni.uploadFile({
            url: `${request.BASE_URL}/api/volunteer/uploadAvatar`,
            filePath: filePath,
            name: 'file',
            formData: { 
              userId: this.userInfo.userId.toString(),
              id: this.userInfo.id.toString()
            },
            header: {
              'X-App-Version': '1.0.0'
            },
            success: resolve,
            fail: (err) => {
              if (err.errMsg.includes('timeout')) {
                reject(new Error('上传超时'));
              } else if (err.errMsg.includes('network')) {
                reject(new Error('网络异常'));
              } else {
                reject(new Error('上传失败'));
              }
            }
          });
        });

        // 解析响应
        let result;
        try {
          result = uploadRes.data ? JSON.parse(uploadRes.data) : {};
        } catch (e) {
          console.warn('响应解析异常:', e);
          if (uploadRes.statusCode === 200) {
            result = { success: true };
          } else {
            throw new Error('服务器响应异常');
          }
        }

        // 判断成功
        const isSuccess = (
          (result.success === true) ||
          (result.code === 200) ||
          (uploadRes.statusCode === 200)
        );

        if (isSuccess) {
          // 使用返回的avatarUrl或构建默认URL
          const newAvatarUrl = result.avatarUrl || `/uploads/volunteer/avatar/${this.userInfo.id}_${Date.now()}.jpg`;
          
          // 立即更新本地数据
          this.userInfo.avatarUrl = newAvatarUrl;
          
          // 保存到本地存储
          uni.setStorageSync('volunteerMyInfo', this.userInfo);
          
          // 通知其他页面
          this.notifyOtherPages();
          
          uni.showToast({ 
            title: '头像上传成功', 
            icon: 'success',
            duration: 2000 
          });

          // 可选：重新加载信息确保数据一致
          setTimeout(() => {
            this.loadVolunteerInfo();
          }, 500);

        } else {
          throw new Error(result.message || '头像上传失败');
        }

      } catch (error) {
        console.error('头像上传失败:', error);
        uni.showToast({ 
          title: error.message, 
          icon: 'none', 
          duration: 3000 
        });
      } finally {
        this.loading = false;
        if (loadingShown) {
          uni.hideLoading();
        }
      }
    },

    // 通知其他页面
    notifyOtherPages() {
      uni.$emit('volunteerAvatarUpdated', {
        userId: this.userInfo.userId,
        avatarUrl: this.userInfo.avatarUrl,
        timestamp: Date.now(),
        sender: 'volunteerMy'
      });
    },

    // 处理头像更新事件
    handleAvatarUpdated(data) {
      if (data.sender === 'volunteerMy') return;
      if (data.userId === this.userInfo.userId) {
        this.userInfo.avatarUrl = data.avatarUrl;
        // 强制刷新视图
        this.$forceUpdate();
      }
    },

    // 头像加载失败处理
    handleAvatarError() {
      // 静默替换为默认头像，不打扰用户
      if (this.userInfo.avatarUrl !== this.defaultAvatar) {
        this.userInfo.avatarUrl = this.defaultAvatar;
        console.warn('头像加载失败，已替换为默认头像');
      }
    },

    navigateTo(page) {
      const pathMap = {
        profile: '/pages/volunteer/my/profile',
        service: '/pages/volunteer/my/service',
        about: '/pages/volunteer/my/about',
        feedback: '/pages/volunteer/my/feedback'
      };
      if (pathMap[page]) uni.navigateTo({ url: pathMap[page] });
    },

    logout() {
      uni.showModal({
        title: '确认退出',
        content: '你确定要退出当前账号吗？',
        success: (res) => {
          if (res.confirm) {
            uni.removeStorageSync('userInfo');
            uni.removeStorageSync('volunteerMyInfo');
            uni.reLaunch({ url: '/pages/login/login' });
          }
        }
      });
    }
  }
}
</script>


<style scoped>
.volunteer-my {
  padding-bottom: 120rpx;
  background-color: #F8F4F4;
  padding: 20rpx;
  min-height: 100vh;
}

/* 强制移除button边框 */
button {
  border: none !important;
  outline: none !important;
}

button::after {
  border: none !important;
}

/* 个人信息卡片 */
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
}

/* 卡片组 */
.card-group {
  background-color: #ffffff;
  border-radius: 32rpx;
  box-shadow: 0 4rpx 12rpx rgba(0,0,0,0.05);
  margin-bottom: 20rpx;
  overflow: hidden;
}

.personal-info,
.account-security,
.about-us,
.help-feedback,
.service-hours {
  padding: 30rpx;
  color: #4B3425;
  cursor: pointer;
  min-height: 80rpx;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.function-label {
  font-size: 32rpx;
  color: #4B3425;
  font-weight: 600;
}

.divider-line {
  height: 2rpx;
  background-color: #e8e8e8;
  margin: 0 30rpx;
}

.arrow-icon,
.enter-icon {
  width: 32rpx;
  height: 32rpx;
  opacity: 0.7;
}

/* 服务时长显示 */
.hours-text {
  font-size: 36rpx;
  color: #9AB169;
  font-weight: bold;
}

/* 退出登录按钮 */
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
  box-shadow: 0 4rpx 12rpx rgba(0,0,0,0.05);
  font-weight: 600;
}
.avatar-popup-content {
  background: #fff;
  border-radius: 16rpx;
  padding: 40rpx;
  width: 60%;
}
.popup-title {
  text-align: center;
  font-size: 32rpx;
  margin-bottom: 40rpx;
  color: #333;
}
.popup-buttons {
  display: flex;
  flex-direction: column;
  gap: 20rpx;
}
.popup-btn {
  padding: 20rpx;
  border-radius: 12rpx;
  background: #f5f5f5;
  color: #333;
  font-size: 28rpx;
}
.cancel-btn {
  background: #eee;
  color: #666;
}
</style>