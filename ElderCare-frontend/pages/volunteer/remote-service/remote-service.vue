<template>
  <view class="remote-service-page">
    <!-- 接单列表页面 -->
    <view v-if="currentPage === 'list'" class="list-page">
      
      <!-- 协助列表 -->
      <view class="assistance-list">
        <view v-if="waitingAssistances.length === 0 && !isLoadingList" class="empty-list">
          <image src="/static/volunteer/remote_service/none.png" class="empty-image" mode="aspectFit"></image>
          <text class="empty-text">暂无待接单的协助请求</text>
          <text class="empty-desc">请稍后刷新查看新的协助请求</text>
        </view>
        
        <view v-else>
          <view 
            v-for="(item, index) in waitingAssistances" 
            :key="item.id" 
            class="assistance-item">
            
            <view class="item-content">
              <!-- 左侧信息 -->
              <view class="item-left">
                <text class="assistance-title">{{ truncateText(item.appointmentContent, 18) }}</text>
                <view class="assistance-details">
                  <text class="detail-time">申请时间：{{ formatDateTime(item.applyTime) }}</text>
                  <text class="detail-elder">老年人：{{ item.elderName }}</text>
                </view>
              </view>
              
              <!-- 右侧按钮 -->
              <view class="item-right">
                <button @click="acceptAssistance(item)" class="btn btn-accept">
                  协助
                </button>
              </view>
            </view>
          </view>
        </view>
      </view>
    </view>

    <!-- 等待老年人呼叫页面 -->
    <view v-if="currentPage === 'waiting'" class="waiting-page">
      <view class="waiting-section">
        <image src="/static/volunteer/remote_service/waiting.png" class="waiting-image" mode="aspectFit"></image>
        <text class="waiting-desc">请耐心等待老年人进行呼叫</text>
      </view>
      
      <!-- 当前协助信息 -->
      <view v-if="currentAssistance" class="current-assistance">
        <text class="section-title">当前协助信息</text>
        <view class="assistance-info">
          <view class="info-row">
            <text class="info-label">协助内容：</text>
            <text class="info-value">{{ currentAssistance.appointmentContent }}</text>
          </view>
          <view class="info-row">
            <text class="info-label">申请时间：</text>
            <text class="info-value">{{ formatDateTime(currentAssistance.applyTime) }}</text>
          </view>
          <view class="info-row">
            <text class="info-label">老年人：</text>
            <text class="info-value">{{ currentAssistance.elderName }}</text>
          </view>
        </view>
      </view>
      
      <!-- 操作按钮 -->
      <view class="waiting-actions">
        <button @click="backToList" class="btn btn-cancel-large">
          退出协助
        </button>
      </view>
    </view>

    <!-- 协助完成页面 -->
    <view v-if="currentPage === 'completed'" class="result-page">
      <view class="result-section">
        <image src="/static/volunteer/remote_service/thanks.png" class="result-image" mode="aspectFit"></image>
        <text class="result-desc">远程协助顺利完成，感谢您的服务！</text>
        
        <view v-if="currentAssistance" class="result-info">
          <text class="result-content">协助内容：{{ currentAssistance.appointmentContent }}</text>
          <text class="result-elder">服务对象：{{ currentAssistance.elderName }}</text>
        </view>
      </view>
      
      <view class="result-actions">
        <button @click="backToList" class="btn btn-primary btn-large">
          返回列表
        </button>
      </view>
    </view>

    <!-- 协助取消页面 -->
    <view v-if="currentPage === 'cancelled'" class="result-page">
      <view class="result-section">
        <image src="/static/volunteer/remote_service/thanks.png" class="result-image" mode="aspectFit"></image>
        <text class="result-desc">老年人已取消服务，同样感谢您的服务！</text>
        
        <view v-if="currentAssistance" class="result-info">
          <text class="result-content">协助内容：{{ currentAssistance.appointmentContent }}</text>
          <text class="result-elder">服务对象：{{ currentAssistance.elderName }}</text>
        </view>
      </view>
      
      <view class="result-actions">
        <button @click="backToList" class="btn btn-primary btn-large">
          返回列表
        </button>
      </view>
    </view>

    <!-- 底部固定刷新按钮 - 仅在列表页面显示 -->
    <view v-if="currentPage === 'list'" class="fixed-refresh-btn">
      <button @click="loadWaitingAssistances" class="btn btn-refresh-fixed" :disabled="isLoadingList">
        {{ isLoadingList ? '加载中...' : '刷新列表' }}
      </button>
    </view>
  </view>
</template>

<script>
import CustomTabbar from '@/components/custom-tabbar.vue'
import { getCurrentUserId as getAuthUserId, requireLogin } from '@/utils/auth.js'
import { genTestUserSig } from '@/debug/GenerateTestUserSig.js'
import config from '@/utils/config.js'

// 导入TUICallKit插件
const TUICallKit = uni.requireNativePlugin('TencentCloud-TUICallKit')
uni.$TUICallKit = TUICallKit

export default {
  components: { CustomTabbar },
  
  data() {
    return {
      volunteerId: null,
      isTUICallKitLogin: false,
      serviceStatus: 'uninitialized', // uninitialized, initializing, ready, error
      
      // 页面状态管理
      currentPage: 'list', // list, waiting, completed, cancelled
      
      // 接单列表相关
      waitingAssistances: [], // 待接单的协助列表
      isLoadingList: false, // 是否正在加载列表
      
      // 当前协助信息
      currentAssistance: null, // 当前接单的协助信息
      
      // 轮询相关
      statusTimer: null, // 状态轮询定时器
      
      // API基础地址
      apiBaseUrl: config.API_BASE_URL
    }
  },

  methods: {
    // 获取当前状态图片
    getStatusImage() {
      const statusImages = {
        'uninitialized': '/static/volunteer/service_init.png',
        'initializing': '/static/volunteer/service_loading.png', 
        'ready': '/static/volunteer/service_ready.png',
        'error': '/static/volunteer/service_error.png'
      }
      return statusImages[this.serviceStatus] || '/static/volunteer/service_init.png'
    },

    // 获取状态文本
    getStatusText() {
      const statusTexts = {
        'uninitialized': '远程协助服务未初始化',
        'initializing': '正在初始化服务...',
        'ready': '远程协助服务已就绪',
        'error': '服务初始化失败'
      }
      return statusTexts[this.serviceStatus] || ''
    },

    // 获取状态描述
    getStatusDescription() {
      const statusDescriptions = {
        'uninitialized': '点击下方按钮开始初始化远程协助服务',
        'initializing': '请稍等，正在配置通话功能...',
        'ready': '您现在可以接收和处理老人用户的远程协助请求',
        'error': '初始化过程中出现问题，请重试或联系技术支持'
      }
      return statusDescriptions[this.serviceStatus] || ''
    },

    // 获取当前用户ID
    getCurrentUserId() {
      const userId = getAuthUserId()
      if (!userId) {
        requireLogin()
        return null
      }
      return userId
    },

    // 初始化远程协助服务
    async initializeService() {
      if (!this.volunteerId) {
        uni.showToast({
          title: '用户信息异常',
          icon: 'none'
        })
        return
      }

      this.serviceStatus = 'initializing'

      try {
        console.log('🚀 开始初始化志愿者远程协助服务，用户ID:', this.volunteerId)
        
        uni.showLoading({ title: '初始化服务中...' })

        const success = await this.loginTUICallKit()

        uni.hideLoading()

        if (success) {
          this.serviceStatus = 'ready'
          this.isTUICallKitLogin = true
          
          uni.showToast({
            title: '远程协助服务初始化成功！',
            icon: 'success',
            duration: 2000
          })

          console.log('✅ 志愿者远程协助服务初始化完成')
        } else {
          this.serviceStatus = 'error'
          
          uni.showModal({
            title: '初始化失败',
            content: '远程协助服务初始化失败，请检查网络连接后重试',
            showCancel: false
          })
        }
      } catch (error) {
        console.error('❌ 志愿者服务初始化异常:', error)
        
        uni.hideLoading()
        this.serviceStatus = 'error'
        
        uni.showModal({
          title: '初始化异常',
          content: '服务初始化过程中出现异常，请重试或联系技术支持',
          showCancel: false
        })
      }
    },

    // TUICallKit 登录
    async loginTUICallKit() {
      try {
        console.log('🔐 志愿者登录 TUICallKit，userID:', this.volunteerId)
        
        // 生成用户签名（从后端获取配置）
        const { userSig, sdkAppID: SDKAppID } = await genTestUserSig(this.volunteerId.toString())
        const loginParams = { 
          SDKAppID, 
          userID: this.volunteerId.toString(), 
          userSig 
        }
        
        console.log('📡 TUICallKit 登录参数:', loginParams)

        return new Promise((resolve) => {
          // 设置超时处理
          const timeout = setTimeout(() => {
            console.warn('⏰ TUICallKit 登录超时')
            resolve(false)
          }, 15000) // 15秒超时

          uni.$TUICallKit.login(loginParams, (res) => {
            clearTimeout(timeout)
            console.log('📥 TUICallKit 登录响应:', res)
            
            if (res.code === 0) {
              console.log('✅ TUICallKit 登录成功')
              resolve(true)
            } else {
              console.error('❌ TUICallKit 登录失败:', res.msg)
              resolve(false)
            }
          })
        })
      } catch (error) {
        console.error('💥 TUICallKit 登录异常:', error)
        return false
      }
    },

    // 测试连接状态
    testConnection() {
      if (this.isTUICallKitLogin) {
        uni.showToast({
          title: '连接状态正常 ✅',
          icon: 'success'
        })
      } else {
        uni.showToast({
          title: '连接异常，请重新初始化',
          icon: 'none'
        })
      }
    },

    // ==================== 接单相关方法 ====================
    
    /**
     * 检查志愿者是否有等待中的协助项目
     */
    async checkVolunteerWaitingStatus() {
      console.log('🔍 检查志愿者等待中的协助项目')
      
      try {
        const response = await uni.request({
          url: `${this.apiBaseUrl}/api/volunteer/assistance/volunteer/${this.volunteerId}/waiting`,
          method: 'GET',
          timeout: 10000
        })
        
        console.log('📥 等待状态检查响应:', response)
        
        if (response.statusCode === 200 && response.data.success) {
          if (response.data.hasWaiting && response.data.data) {
            // 志愿者有等待中的项目，直接进入等待页面
            console.log('✅ 志愿者有等待中的项目，进入等待页面')
            
            this.currentAssistance = {
              id: response.data.data.id,
              appointmentContent: response.data.data.appointmentContent,
              applyTime: response.data.data.applyTime,
              elderId: response.data.data.elderId,
              elderName: response.data.data.elderName,
              status: response.data.data.status
            }
            
            // 切换到等待页面
            this.currentPage = 'waiting'
            
            // 开始状态轮询
            this.startStatusPolling()
            
            // 初始化TUICallKit登录
            await this.loginTUICallKit()
            
          } else {
            // 没有等待中的项目，正常显示列表
            console.log('ℹ️ 志愿者没有等待中的项目，显示接单列表')
            this.loadWaitingAssistances()
          }
        } else {
          console.warn('⚠️ 检查等待状态失败:', response.data.message)
          // 检查失败时也显示列表
          this.loadWaitingAssistances()
        }
        
      } catch (error) {
        console.error('❌ 检查等待状态异常:', error)
        // 异常时也显示列表
        this.loadWaitingAssistances()
      }
    },
    
    /**
     * 加载待接单的协助列表
     */
    async loadWaitingAssistances() {
      if (this.isLoadingList) return
      
      this.isLoadingList = true
      
      try {
        console.log('🔄 加载待接单协助列表')
        
        const response = await uni.request({
          url: `${this.apiBaseUrl}/api/volunteer/assistance/waiting`,
          method: 'GET',
          timeout: 10000
        })
        
        console.log('📥 接单列表响应:', response)
        
        if (response.statusCode === 200 && response.data.success) {
          this.waitingAssistances = response.data.data || []
          console.log('✅ 成功加载协助列表，共{}项', this.waitingAssistances.length)
        } else {
          console.warn('⚠️ 加载协助列表失败:', response.data.message)
          uni.showToast({
            title: response.data.message || '加载列表失败',
            icon: 'none'
          })
        }
        
      } catch (error) {
        console.error('❌ 加载协助列表异常:', error)
        uni.showToast({
          title: '网络异常，请稍后重试',
          icon: 'none'
        })
      } finally {
        this.isLoadingList = false
      }
    },
    
    /**
     * 志愿者接单
     */
    async acceptAssistance(assistanceItem) {
      if (!assistanceItem || !assistanceItem.id) {
        uni.showToast({
          title: '协助信息错误',
          icon: 'none'
        })
        return
      }
      
      try {
        uni.showLoading({ title: '正在接单...' })
        console.log('📞 志愿者开始接单，协助ID:', assistanceItem.id)
        
        // 1. 调用接单API
        const response = await uni.request({
          url: `${this.apiBaseUrl}/api/volunteer/assistance/accept`,
          method: 'POST',
          header: {
            'Content-Type': 'application/json'
          },
          data: {
            assistanceId: assistanceItem.id,
            volunteerId: this.volunteerId
          },
          timeout: 15000
        })
        
        console.log('📥 接单响应:', response)
        
        if (response.statusCode === 200 && response.data.success) {
          console.log('✅ 接单成功')
          
          // 2. 保存当前协助信息
          this.currentAssistance = response.data.assistanceInfo
          
          // 3. 初始化TUICallKit服务
          const loginSuccess = await this.loginTUICallKit()
          
          if (loginSuccess) {
            // 4. 切换到等待页面
            this.currentPage = 'waiting'
            
            // 5. 开始状态轮询
            this.startStatusPolling()
            
            uni.hideLoading()
            
            uni.showToast({
              title: '接单成功！',
              icon: 'success'
            })
            
          } else {
            uni.hideLoading()
            uni.showToast({
              title: '服务初始化失败，请重试',
              icon: 'none'
            })
          }
          
        } else {
          uni.hideLoading()
          uni.showToast({
            title: response.data.message || '接单失败',
            icon: 'none'
          })
          
          // 重新加载列表，因为可能被其他志愿者接单了
          await this.loadWaitingAssistances()
        }
        
      } catch (error) {
        console.error('❌ 接单异常:', error)
        uni.hideLoading()
        
        uni.showToast({
          title: '接单失败，请稍后重试',
          icon: 'none'
        })
      }
    },
    
    /**
     * 开始状态轮询
     */
    startStatusPolling() {
      if (!this.currentAssistance) return
      
      console.log('⏱️ 开始状态轮询')
      
      // 清除之前的定时器
      this.stopStatusPolling()
      
      // 每3秒轮询一次
      this.statusTimer = setInterval(async () => {
        await this.checkAssistanceStatus()
      }, 3000)
    },
    
    /**
     * 停止状态轮询
     */
    stopStatusPolling() {
      if (this.statusTimer) {
        clearInterval(this.statusTimer)
        this.statusTimer = null
        console.log('⏹️ 停止状态轮询')
      }
    },
    
    /**
     * 检查协助状态
     */
    async checkAssistanceStatus() {
      if (!this.currentAssistance) return
      
      try {
        const response = await uni.request({
          url: `${this.apiBaseUrl}/api/volunteer/assistance/${this.currentAssistance.id}`,
          method: 'GET',
          timeout: 5000
        })
        
        if (response.statusCode === 200 && response.data.success) {
          const assistanceInfo = response.data.data
          const currentStatus = assistanceInfo.status
          
          console.log('🔍 当前协助状态:', currentStatus)
          
          // 更新当前协助信息
          this.currentAssistance = assistanceInfo
          
          // 根据状态切换页面
          if (currentStatus === 'completed') {
            this.stopStatusPolling()
            this.currentPage = 'completed'
          } else if (currentStatus === 'cancelled') {
            this.stopStatusPolling()
            this.currentPage = 'cancelled'
          }
        }
        
      } catch (error) {
        console.warn('⚠️ 状态轮询失败:', error)
      }
    },
    
    /**
     * 返回列表
     */
    backToList() {
      // 停止轮询
      this.stopStatusPolling()
      
      // 重置状态
      this.currentPage = 'list'
      this.currentAssistance = null
      this.serviceStatus = 'uninitialized'
      this.isTUICallKitLogin = false
      
      // 重新加载列表
      this.loadWaitingAssistances()
      
      console.log('🔙 返回协助列表')
    },
    
    /**
     * 格式化时间显示
     */
    formatDateTime(dateString) {
      if (!dateString) return ''
      const date = new Date(dateString)
      const now = new Date()
      
      // 格式化时间部分 HH:mm
      const hours = String(date.getHours()).padStart(2, '0')
      const minutes = String(date.getMinutes()).padStart(2, '0')
      const timeStr = `${hours}:${minutes}`
      
      // 判断日期关系
      const dateOnly = new Date(date.getFullYear(), date.getMonth(), date.getDate())
      const todayOnly = new Date(now.getFullYear(), now.getMonth(), now.getDate())
      const yesterdayOnly = new Date(now.getFullYear(), now.getMonth(), now.getDate() - 1)
      
      if (dateOnly.getTime() === todayOnly.getTime()) {
        return `今天 ${timeStr}`
      } else if (dateOnly.getTime() === yesterdayOnly.getTime()) {
        return `昨天 ${timeStr}`
      } else {
        // 其他日期显示完整的年月日时间
        const year = date.getFullYear()
        const month = String(date.getMonth() + 1).padStart(2, '0')
        const day = String(date.getDate()).padStart(2, '0')
        return `${year}-${month}-${day} ${timeStr}`
      }
    },
    
    /**
     * 截断文字并添加省略号
     */
    truncateText(text, maxLength = 20) {
      if (!text) return ''
      return text.length > maxLength ? text.substring(0, maxLength) + '...' : text
    }
  },

  // 页面生命周期
  onLoad() {
    console.log('📱 志愿者远程服务页面加载')
    
    // 检查用户登录状态
    if (!requireLogin(false)) {
      return
    }
    
    // 获取当前用户ID
    this.volunteerId = this.getCurrentUserId()
    if (!this.volunteerId) {
      console.error('❌ 无法获取志愿者ID')
      return
    }
    
    console.log('👤 志愿者ID:', this.volunteerId)
    
    // 检查志愿者是否有等待中的协助项目
    this.checkVolunteerWaitingStatus()
  },

  onShow() {
    console.log('👁️ 志愿者远程服务页面显示')
    
    // 隐藏底部导航栏
    uni.hideTabBar()
    
    // 如果在列表页面，重新加载数据
    if (this.currentPage === 'list') {
      this.loadWaitingAssistances()
    }
  },

  onHide() {
    console.log('👋 志愿者远程服务页面隐藏')
    // 显示底部导航栏
    uni.showTabBar()
  },

  onUnload() {
    console.log('📱 志愿者远程服务页面卸载')
    
    // 清理定时器
    this.stopStatusPolling()
  }
}
</script>

<style scoped>
.remote-service-page {
  min-height: 100vh;
  background-color: #f8f9fa;
  padding-bottom: 120rpx;
}

/* 页面标题 */
.page-title {
  padding: 30rpx;
  text-align: center;
  background-color: #fff;
  border-bottom: 2rpx solid #f1f1f1;
  margin-bottom: 20rpx;
}

.title-text {
  font-size: 36rpx;
  font-weight: bold;
  color: #333;
}

/* 通用按钮样式 */
.btn {
  border-radius: 50rpx;
  font-size: 30rpx;
  font-weight: bold;
  border: none;
  text-align: center;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.3s ease;
}

.btn-large {
  height: 100rpx;
  width: 100%;
  font-size: 32rpx;
}

.btn-primary {
  background-color: #6ac259;
  color: white;
}

.btn-primary:active {
  background-color: #5a9c4a;
}

/* ==================== 接单列表页面 ==================== */
.list-page {
  padding: 30rpx;
  padding-bottom: 150rpx; /* 为底部固定按钮留出空间 */
}

/* 刷新按钮区域 */
.refresh-section {
  margin-bottom: 30rpx;
  text-align: center;
}

.btn-refresh {
  background-color: #f8f9fa;
  color: #6ac259;
  border: 2rpx solid #6ac259;
  height: 70rpx;
  width: 200rpx;
}

.btn-refresh:disabled {
  background-color: #f5f5f5;
  color: #ccc;
  border-color: #e1e1e1;
}

/* 协助列表 */
.assistance-list {
  display: flex;
  flex-direction: column;
  gap: 20rpx;
}

/* 空列表状态 */
.empty-list {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  text-align: center;
  padding: 200rpx 30rpx;
  min-height: 60vh;
}

.empty-image {
  width: 240rpx;
  height: 240rpx;
  margin-bottom: 40rpx;
  opacity: 0.7;
}

.empty-text {
  font-size: 36rpx;
  color: #666;
  display: block;
  margin-bottom: 20rpx;
  font-weight: 500;
}

.empty-desc {
  font-size: 28rpx;
  color: #999;
  display: block;
  line-height: 1.5;
}

/* 协助项目卡片 */
.assistance-item {
  background-color: #fff;
  border-radius: 20rpx;
  padding: 30rpx;
  box-shadow: 0 4rpx 12rpx rgba(0,0,0,0.08);
  border: 1rpx solid #f0f0f0;
}

.item-content {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.item-left {
  flex: 1;
  padding-right: 20rpx;
}

.assistance-title {
  font-size: 32rpx;
  font-weight: bold;
  color: #333;
  line-height: 1.4;
  display: block;
  margin-bottom: 15rpx;
}

.assistance-details {
  display: flex;
  flex-direction: column;
  gap: 8rpx;
}

.detail-time,
.detail-elder {
  font-size: 26rpx;
  color: #666;
  display: block;
}

.item-right {
  flex-shrink: 0;
}

.btn-accept {
  background-color: #2196F3;
  color: white;
  height: 70rpx;
  width: 140rpx;
  font-size: 28rpx;
}

.btn-accept:active {
  background-color: #1976D2;
}

/* ==================== 等待呼叫页面 ==================== */
.waiting-page {
  padding: 50rpx 30rpx;
}

.waiting-section {
  text-align: center;
  background-color: #fff;
  border-radius: 20rpx;
  padding: 80rpx 30rpx 50rpx;
  margin-bottom: 30rpx;
  box-shadow: 0 4rpx 12rpx rgba(0,0,0,0.05);
}

.waiting-image {
  width: 200rpx;
  height: 200rpx;
  margin-bottom: 30rpx;
}

.waiting-title {
  font-size: 36rpx;
  font-weight: bold;
  color: #333;
  display: block;
  margin-bottom: 15rpx;
}

.waiting-desc {
  font-size: 28rpx;
  color: #666;
  display: block;
}

/* 当前协助信息 */
.current-assistance {
  background-color: #fff;
  border-radius: 20rpx;
  padding: 30rpx;
  margin-bottom: 40rpx;
  box-shadow: 0 4rpx 12rpx rgba(0,0,0,0.05);
}

.section-title {
  font-size: 30rpx;
  font-weight: bold;
  color: #333;
  display: block;
  margin-bottom: 20rpx;
  padding-bottom: 15rpx;
  border-bottom: 2rpx solid #f1f1f1;
}

.assistance-info {
  display: flex;
  flex-direction: column;
  gap: 15rpx;
}

.info-row {
  display: flex;
  align-items: flex-start;
}

.info-label {
  font-size: 28rpx;
  color: #666;
  min-width: 160rpx;
  flex-shrink: 0;
}

.info-value {
  font-size: 28rpx;
  color: #333;
  flex: 1;
  line-height: 1.4;
}

.waiting-actions {
  text-align: center;
}

.btn-cancel-large {
  background-color: #FF9800;
  color: white;
  height: 80rpx;
  width: 200rpx;
}

/* ==================== 结果页面 ==================== */
.result-page {
  padding: 80rpx 30rpx;
}

.result-section {
  text-align: center;
  background-color: #fff;
  border-radius: 20rpx;
  padding: 80rpx 30rpx 50rpx;
  margin-bottom: 40rpx;
  box-shadow: 0 4rpx 12rpx rgba(0,0,0,0.05);
}

.result-image {
  width: 200rpx;
  height: 200rpx;
  margin-bottom: 30rpx;
}

.result-title {
  font-size: 36rpx;
  font-weight: bold;
  color: #333;
  display: block;
  margin-bottom: 15rpx;
}

.result-desc {
  font-size: 28rpx;
  color: #666;
  line-height: 1.5;
  display: block;
  margin-bottom: 30rpx;
}

.result-info {
  text-align: left;
  background-color: #f8f9fa;
  border-radius: 15rpx;
  padding: 25rpx;
  display: flex;
  flex-direction: column;
  gap: 12rpx;
}

.result-content,
.result-elder {
  font-size: 26rpx;
  color: #666;
  line-height: 1.4;
  display: block;
}

.result-actions {
  text-align: center;
}

/* 底部固定刷新按钮 */
.fixed-refresh-btn {
  position: fixed;
  bottom: 40rpx;
  left: 30rpx;
  right: 30rpx;
  z-index: 1000;
}

.btn-refresh-fixed {
  width: 100%;
  height: 80rpx;
  background: linear-gradient(135deg, #007AFF, #4A90E2);
  color: white;
  border: none;
  border-radius: 40rpx;
  font-size: 32rpx;
  font-weight: bold;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 8rpx 24rpx rgba(0, 122, 255, 0.3);
  transition: all 0.3s ease;
}

.btn-refresh-fixed:active {
  transform: scale(0.98);
  box-shadow: 0 4rpx 12rpx rgba(0, 122, 255, 0.2);
}

.btn-refresh-fixed:disabled {
  background: #ccc;
  box-shadow: none;
  transform: none;
}
</style>
