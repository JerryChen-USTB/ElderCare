<template>
  <view class="role-container">
    <view class="header">
      <text class="title">请选择您的身份</text>
      <text class="subtitle">不同身份将提供不同的功能服务</text>
    </view>
    
    <view class="role-cards">
      <view class="role-card" @click="selectRole('elder')" style="background-color: #4a9ff5;">
        <image src="/static/icons/user.png" class="role-icon" />
        <text class="role-title">老年人用户</text>
        <text class="role-desc">需要帮助的老年人</text>
      </view>
      
      <view class="role-card" @click="selectRole('guardian')" style="background-color: #ff7e5f;">
        <image src="/static/icons/guardian.png" class="role-icon" />
        <text class="role-title">监护人</text>
        <text class="role-desc">家人或看护人员</text>
      </view>
      
      <view class="role-card" @click="selectRole('volunteer')" style="background-color: #6ac259;">
        <image src="/static/icons/volunteer.png" class="role-icon" />
        <text class="role-title">志愿者</text>
        <text class="role-desc">提供志愿服务</text>
      </view>
    </view>
  </view>
</template>

<script>
import request from '@/utils/request.js'

export default {
  data() {
    return {
      userInfo: null
    }
  },
  onLoad() {
    // 获取本地存储的用户信息
    this.userInfo = uni.getStorageSync('userInfo')
    if (!this.userInfo) {
      uni.showToast({
        title: '请先登录',
        icon: 'none'
      })
      setTimeout(() => {
        uni.navigateTo({ url: '/pages/login/login' })
      }, 1500)
    }
  },
  methods: {
    async selectRole(role) {
      console.log('选择的角色:', role)
      
      if (!this.userInfo) {
        uni.showToast({
          title: '用户信息不存在，请重新登录',
          icon: 'none'
        })
        return
      }
      
      try {
        uni.showLoading({ title: '保存中...' })
        
        // 调用API更新用户角色
        const result = await request.userApi.updateRole(this.userInfo.phone, role)
        
        uni.hideLoading()
        
        if (result.success) {
          // 更新本地存储的用户信息
          uni.setStorageSync('userInfo', result.user)
          this.userInfo = result.user
          
          uni.showToast({ 
            title: '角色设置成功', 
            icon: 'success' 
          })
      
      // 根据角色跳转到对应首页
      const roleMap = {
            elder: '/pages/user/index/index',
        guardian: '/pages/guardian/index/index',
        volunteer: '/pages/volunteer/index/index'
      }
      
      const targetPath = roleMap[role]
      console.log('跳转到:', targetPath)
      
          setTimeout(() => {
      // 使用 reLaunch 确保完全跳转
      uni.reLaunch({
        url: targetPath,
        success: () => {
          console.log('跳转成功')
        },
        fail: (err) => {
          console.error('跳转失败:', err)
          uni.showToast({
            title: '跳转失败，请检查页面是否存在',
            icon: 'none'
          })
        }
      })
          }, 1500)
          
        } else {
          uni.showToast({
            title: result.message,
            icon: 'none'
          })
        }
        
      } catch (error) {
        uni.hideLoading()
        console.error('角色设置失败:', error)
        uni.showToast({
          title: '角色设置失败，请检查网络连接',
          icon: 'none'
        })
      }
    }
  }
}
</script>

<style scoped>
.role-container {
  padding: 40rpx;
  min-height: 100vh;
  background: linear-gradient(to bottom, #f0f7ff, #ffffff);
}

.header {
  text-align: center;
  padding: 80rpx 0 100rpx;
}

.title {
  font-size: 48rpx;
  font-weight: bold;
  color: #333;
  display: block;
  margin-bottom: 20rpx;
}

.subtitle {
  font-size: 32rpx;
  color: #888;
}

.role-cards {
  display: flex;
  flex-direction: column;
  gap: 40rpx;
}

.role-card {
  padding: 50rpx 30rpx;
  border-radius: 30rpx;
  color: white;
  display: flex;
  flex-direction: column;
  align-items: center;
  box-shadow: 0 10rpx 20rpx rgba(0,0,0,0.1);
}

.role-icon {
  width: 120rpx;
  height: 120rpx;
  margin-bottom: 30rpx;
}

.role-title {
  font-size: 40rpx;
  font-weight: bold;
  margin-bottom: 15rpx;
}

.role-desc {
  font-size: 28rpx;
  opacity: 0.9;
}
</style>