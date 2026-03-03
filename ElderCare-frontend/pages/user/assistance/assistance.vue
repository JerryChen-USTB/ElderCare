<template>
  <view class="assistance-page">
    <!-- 顶部功能描述 -->
    <view class="header">
      <!-- 返回按钮 -->
      <view class="back-button" @click="goBack">
        <image class="back-icon" src="/static/back.png" mode="aspectFit"></image>
      </view>
      
      <view class="header-content">
        <text class="header-title">远程协助服务</text>
        <text class="header-description">一键申请专业志愿者为您提供远程协助服务</text>
      </view>
    </view>

    <!-- 中间状态插画 -->
    <view class="status-illustration">
      <image :src="getStatusImage()" class="status-image" mode="aspectFit"></image>
      <text class="status-text">{{ getStatusText() }}</text>
    </view>

    <!-- 底部服务状态卡片 -->
    <view class="service-card">
      <!-- 取消按钮（右上角） -->
      <view class="cancel-button-area">
        <!-- 等待志愿者应答：取消申请 -->
        <button 
          v-if="currentStatus === 'waiting_response'"
          @click="cancelApplication"
          class="btn-cancel-corner">
          取消申请
        </button>

        <!-- 已应答，待呼叫：取消服务 -->
        <button 
          v-if="currentStatus === 'waiting_call'"
          @click="cancelService" 
          class="btn-cancel-corner">
          取消服务
        </button>

        <!-- waiting_call_again状态：不显示取消服务按钮 -->
      </view>

      <view class="card-content">
        <!-- 服务状态标题 -->
        <view class="status-title">
          <text class="status-label">{{ getStatusLabel() }}</text>
        </view>

        <!-- 申请详细信息 -->
        <view class="service-details">
          <view class="detail-item" v-if="assistanceDetail && assistanceDetail.assistance && assistanceDetail.assistance.applyTime">
            <text class="detail-label">申请时间：</text>
            <text class="detail-value">{{ formatDateTime(assistanceDetail.assistance.applyTime) }}</text>
          </view>
          
          <view class="detail-item" v-if="assistanceDetail && assistanceDetail.waitingMinutes">
            <text class="detail-label">等待时间：</text>
            <text class="detail-value">{{ formatDuration(assistanceDetail.waitingMinutes) }}</text>
          </view>
          
          <view class="detail-item" v-if="assistanceDetail && assistanceDetail.responseTime">
            <text class="detail-label">应答时间：</text>
            <text class="detail-value">{{ assistanceDetail.responseTime }}</text>
          </view>
          
          <view class="detail-item">
            <text class="detail-label">地点：</text>
            <text class="detail-value">
              {{ currentAddress || (assistanceDetail && assistanceDetail.location) || locationStatus }}
            </text>
          </view>
          
          <view class="detail-item editable-item" @click="showEditContentModal" v-if="currentStatus !== 'waiting_apply'">
            <text class="detail-label">协助内容：</text>
            <text class="detail-value">{{ assistanceContent || '请填写协助内容' }}</text>
            <text class="arrow-icon">修改 ></text>
          </view>

          <!-- 在waiting_apply状态下显示可编辑的协助内容输入框 -->
          <view class="detail-item" v-if="currentStatus === 'waiting_apply'">
            <text class="detail-label">协助内容：</text>
            <textarea 
              v-model="assistanceContent" 
              placeholder="请详细描述您需要的协助内容"
              class="assistance-input"
              maxlength="100">
            </textarea>
          </view>
          
          <view class="detail-item" v-if="assistanceDetail && assistanceDetail.volunteerName">
            <text class="detail-label">协助志愿者：</text>
            <text class="detail-value">{{ assistanceDetail.volunteerName }}</text>
          </view>
          
          <view class="detail-item" v-if="assistanceDetail && assistanceDetail.volunteerNumber">
            <text class="detail-label">志愿者编号：</text>
            <text class="detail-value">{{ assistanceDetail.volunteerNumber }}</text>
          </view>
        </view>
      </view>

      <!-- 底部操作按钮 -->
      <view class="card-bottom-buttons">
        <!-- 待申请状态：提交申请 -->
        <button 
          v-if="currentStatus === 'waiting_apply'"
          @click="submitApplication"
          class="btn btn-primary btn-full">
          提交申请
        </button>

        <!-- 等待志愿者应答：无主要操作按钮 -->

        <!-- 已应答，待呼叫：进行呼叫 -->
        <button 
          v-if="currentStatus === 'waiting_call'"
          @click="startCall" 
          class="btn btn-success btn-full">
          进行呼叫
        </button>

        <!-- 正在呼叫：取消呼叫 -->
        <button 
          v-if="currentStatus === 'calling'"
          @click="hangupCall"
          class="btn btn-cancel btn-full">
          取消呼叫
        </button>

        <!-- 正在协助：结束通话 -->
        <button 
          v-if="currentStatus === 'in_progress'"
          @click="endCall"
          class="btn btn-warning btn-full">
          结束通话
        </button>

        <!-- waiting_call_again：进行呼叫 和 完成协助 -->
        <view v-if="currentStatus === 'waiting_call_again'" class="dual-buttons">
          <button 
            @click="startCall" 
            class="btn btn-success btn-half">
            进行呼叫
          </button>
          <button 
            @click="completeAssistance"
            class="btn btn-primary btn-half">
            完成协助
          </button>
        </view>

        <!-- 已结束/已取消：重新申请 和 查看对话 -->
        <view v-if="currentStatus === 'completed' || currentStatus === 'cancelled'" class="dual-buttons">
          <button 
            @click="resetApplication"
            class="btn btn-primary btn-half">
            重新申请
          </button>
          <button 
            v-if="currentStatus === 'completed' && transcriptFileName"
            @click="showConversationDialog"
            class="btn btn-success btn-half">
            查看对话
          </button>
        </view>
      </view>
    </view>

    <!-- 编辑协助内容弹窗 -->
    <view v-if="showEditModal" class="modal-overlay" @click="closeEditModal">
      <view class="modal-content" @click.stop>
        <view class="modal-header">
          <text class="modal-title">编辑协助内容</text>
          <view class="modal-close" @click="closeEditModal">×</view>
        </view>
        <view class="modal-body">
          <textarea 
            v-model="tempAssistanceContent" 
            placeholder="请详细描述您需要的协助内容"
            class="modal-textarea"
            maxlength="100">
          </textarea>
          <text class="char-count">{{ tempAssistanceContent.length }}/100</text>
        </view>
        <view class="modal-footer">
          <button @click="closeEditModal" class="btn btn-cancel">取消</button>
          <button @click="saveEditContent" class="btn btn-primary">保存</button>
        </view>
      </view>
    </view>

    <!-- 查看对话弹窗 -->
    <view v-if="showConversationModal" class="modal-overlay" @click="closeConversationDialog">
      <view class="conversation-modal" @click.stop>
        <view class="conversation-header">
          <text class="conversation-title">{{ conversationTitle }}</text>
          <view class="conversation-close" @click="closeConversationDialog">×</view>
        </view>
        <view class="conversation-body">
          <view v-if="isLoadingContent" class="loading-content">
            <text>正在加载内容...</text>
          </view>
          <view v-else class="conversation-content">
            <pre class="conversation-text">{{ conversationContent }}</pre>
          </view>
        </view>
        <view class="conversation-footer">
          <button @click="closeConversationDialog" class="btn btn-cancel">关闭</button>
          
          <!-- 查看对话状态 -->
          <button 
            v-if="conversationMode === 'transcript'"
            @click="showAISummary" 
            class="btn btn-primary"
            :disabled="isLoadingContent">
            AI智能总结
          </button>
          
          <!-- AI总结状态 -->
          <button 
            v-if="conversationMode === 'summary'"
            @click="addToKnowledgeBase" 
            class="btn btn-success"
            :disabled="isLoadingContent">
            加入知识库
          </button>
        </view>
      </view>
    </view>

  </view>
</template>

<script>
import config from '@/utils/config.js'
import { getCurrentUserId as getAuthUserId, requireLogin } from '@/utils/auth.js'
import { genTestUserSig } from '@/debug/GenerateTestUserSig.js'

// 导入TUICallKit插件
const TUICallKit = uni.requireNativePlugin('TencentCloud-TUICallKit')
uni.$TUICallKit = TUICallKit

// 导入TUICallEngine插件
const TUICallEngine = uni.requireNativePlugin('TencentCloud-TUICallKit-TUICallEngine')

// 导入TUICallKit事件插件
const TUICallKitEvent = uni.requireNativePlugin('globalEvent')

export default {
  data() {
    return {
      // 当前服务状态
      currentStatus: 'waiting_apply', // waiting_apply, waiting_response, waiting_call, calling, in_progress, waiting_call_again, completed, cancelled
      
      // 协助内容数据
      assistanceContent: '',
      
      // 编辑弹窗相关
      showEditModal: false,
      tempAssistanceContent: '',
      
      // 协助详细信息
      assistanceDetail: null,
      
      // 用户ID（从登录信息动态获取）
      elderId: null,
      
      // API基础地址
      apiBaseUrl: config.API_BASE_URL,
      
      // 位置信息相关
      location: null, // 位置坐标信息
      currentAddress: '', // 当前地址描述
      locationStatus: '获取位置中...', // 位置获取状态
      isLocating: false, // 是否正在定位
      
      // TUICallKit 相关状态
      isTUICallKitLogin: false, // TUICallKit登录状态
      callKitUserID: '', // TUICallKit用户ID
      targetVolunteerID: '', // 目标志愿者ID
      
      // 事件处理函数引用
      onCallBeginHandler: null,
      onCallEndHandler: null,
      onCallCancelledHandler: null,
      
      // 记录呼叫前的状态
      statusBeforeCall: null,
      
      // 转录相关状态
      currentRoomId: null, // 当前通话房间ID
      transcriptionTaskId: null, // 转录任务ID
      transcriptFileName: null, // 当前协助的转录文件名
      
      // 对话查看弹窗相关状态
      showConversationModal: false, // 是否显示对话弹窗
      conversationMode: 'transcript', // 当前模式：'transcript' 或 'summary'
      conversationTitle: '远程协助对话记录', // 弹窗标题
      conversationContent: '', // 弹窗内容
      isLoadingContent: false, // 是否正在加载内容
      
      // 定时刷新定时器
      refreshTimer: null
    }
  },

  methods: {
    // 返回上一页
    goBack() {
      uni.navigateBack({
        delta: 1
      })
    },
    
    // 获取状态对应的插画图片
    getStatusImage() {
      const statusImages = {
        'waiting_apply': '/static/elder/assistance_apply.png',
        'waiting_response': '/static/elder/assistance_waiting.png', 
        'waiting_call': '/static/elder/assistance_ready.png',
        'calling': '/static/elder/assistance_calling.png',
        'in_progress': '/static/elder/assistance_progress.png',
        'waiting_call_again': '/static/elder/assistance_ready.png',
        'completed': '/static/elder/assistance_completed.png',
        'cancelled': '/static/elder/assistance_cancelled.png'
      }
      return statusImages[this.currentStatus] || '/static/elder/assistance_apply.png'
    },
    
    // 获取状态描述文本
    getStatusText() {
      const statusTexts = {
        'waiting_apply': '请填写协助内容并提交申请',
        'waiting_response': '申请已提交，等待志愿者应答',
        'waiting_call': '志愿者已应答，可以进行呼叫',
        'calling': '正在呼叫志愿者，请等待接通',
        'in_progress': '正在进行远程协助服务',
        'waiting_call_again': '可以再次呼叫或完成本次协助',
        'completed': '协助服务已完成',
        'cancelled': '协助服务已取消'
      }
      return statusTexts[this.currentStatus] || ''
    },
    
    // 获取状态标签
    getStatusLabel() {
      const statusLabels = {
        'waiting_apply': '待申请服务...',
        'waiting_response': '等待志愿者应答...',
        'waiting_call': '已应答，待呼叫...',
        'calling': '正在呼叫...',
        'in_progress': '正在协助...',
        'waiting_call_again': '可再次呼叫...',
        'completed': '已结束...',
        'cancelled': '已取消...'
      }
      return statusLabels[this.currentStatus] || ''
    },
    
    // 显示编辑协助内容弹窗
    showEditContentModal() {
      this.tempAssistanceContent = this.assistanceContent
      this.showEditModal = true
    },
    
    // 关闭编辑弹窗
    closeEditModal() {
      this.showEditModal = false
      this.tempAssistanceContent = ''
    },
    
    // 保存编辑内容
    async saveEditContent() {
      if (this.tempAssistanceContent.trim() === '') {
        uni.showToast({
          title: '协助内容不能为空',
          icon: 'none'
        })
        return
      }
      
      try {
        const response = await this.updateAssistanceContent(this.tempAssistanceContent.trim())
        if (response.success) {
          this.assistanceContent = this.tempAssistanceContent.trim()
          this.showEditModal = false

        }
      } catch (error) {
        // 修改失败静默处理
      }
    },
    
    // 提交申请
    async submitApplication() {
      console.log('🎯 用户点击提交申请按钮')
      
      if (!this.assistanceContent.trim()) {
        console.warn('⚠️ 协助内容为空')
        uni.showToast({
          title: '请填写协助内容',
          icon: 'none'
        })
        return
      }

      console.log('✅ 协助内容验证通过:', this.assistanceContent.trim())

      try {
        uni.showLoading({ title: '提交申请中...' })
        console.log('⏳ 显示加载提示')
        
        const response = await this.submitAssistanceRequest(this.assistanceContent.trim())
        console.log('📨 收到提交申请响应:', response)
        
        uni.hideLoading()
        console.log('✅ 隐藏加载提示')
        
        if (response && response.success) {
          console.log('🎉 申请提交成功')
          await this.loadAssistanceData()
        } else {
          console.warn('⚠️ 申请提交失败 - 服务器返回失败:', response)
          const errorMessage = response ? response.message || '申请提交失败' : '申请提交失败'
          console.log('📝 显示错误消息:', errorMessage)
          

        }
      } catch (error) {
        console.error('💥 申请提交异常:', error)
        console.error('🔍 异常详情:', JSON.stringify(error, null, 2))
        
        uni.hideLoading()
        
        // 显示详细的错误信息
        let errorMsg = '申请提交失败'
        if (error.errMsg) {
          if (error.errMsg.includes('connect')) {
            errorMsg = '无法连接到服务器，请检查网络'
          } else if (error.errMsg.includes('timeout')) {
            errorMsg = '请求超时，请检查网络或稍后重试'
          } else {
            errorMsg = error.errMsg
          }
        }
        
        const suggestions = this.getConnectionSuggestions(this.apiBaseUrl)
        

      }
    },

    // 开始呼叫
    async startCall() {
      if (!this.assistanceDetail || !this.assistanceDetail.assistance) {
        uni.showToast({
          title: '协助信息错误',
          icon: 'none'
        })
        return
      }

      if (!this.targetVolunteerID) {
        uni.showToast({
          title: '无志愿者信息，无法呼叫',
          icon: 'none'
        })
        return
      }

      // 记录呼叫前的状态
      this.statusBeforeCall = this.currentStatus

      // 确保TUICallKit已登录
      if (!this.isTUICallKitLogin) {
        const loginSuccess = await this.loginTUICallKit()
        if (!loginSuccess) {
          return
        }
      }

      try {
        // 先调用后端API更新状态为calling
        const response = await this.callStartCall(this.assistanceDetail.assistance.id)
        
        if (response.success) {
          this.currentStatus = 'calling'
          
          // 使用TUICallKit发起视频通话
          const callSuccess = await this.makeTUICallKitCall(this.targetVolunteerID)
          
          if (!callSuccess) {
            // 如果TUICallKit通话发起失败，回退到原状态
            const failResponse = await this.callCallFailed(this.assistanceDetail.assistance.id)
            if (failResponse.success) {
              this.currentStatus = this.statusBeforeCall

              await this.loadAssistanceData()
            }
          }
        }
      } catch (error) {
        // 发起呼叫失败静默处理
      }
    },
    
    // 取消申请（waiting_response状态）
    async cancelApplication() {
      uni.showModal({
        title: '确认取消',
        content: '确定要取消这次远程协助申请吗？',
        success: async (res) => {
          if (res.confirm) {
            try {
              const response = await this.callCancelAssistance(this.assistanceDetail.assistance.id)
              
              if (response.success) {

                await this.loadAssistanceData()
              } else {

              }
            } catch (error) {

            }
          }
        }
      })
    },
    
    // 取消服务
    async cancelService() {
      uni.showModal({
        title: '确认取消',
        content: '确定要取消这次远程协助服务吗？',
        success: async (res) => {
          if (res.confirm) {
            try {
              const response = await this.callCancelAssistance(this.assistanceDetail.assistance.id)
              
              if (response.success) {

                await this.loadAssistanceData()
              } else {

              }
            } catch (error) {

            }
          }
        }
      })
    },
    
    // 结束通话 - 只在 in_progress 状态下调用
    endCall() {
      uni.showModal({
        title: '确认结束通话',
        content: '确定要结束本次通话吗？',
        success: (res) => {
          if (res.confirm) {
            try {
              // 使用TUICallEngine结束通话
              TUICallEngine.hangup()
              console.log('已调用TUICallEngine.hangup()结束通话')
              
              // 通话结束后会触发onCallEnd事件，自动处理状态转换

            } catch (error) {
              console.error('结束通话失败:', error)

            }
          }
        }
      })
    },

    // 完成协助
    async completeAssistance() {
      uni.showModal({
        title: '确认完成',
        content: '确定要完成这次远程协助服务吗？',
        success: async (res) => {
          if (res.confirm) {
            try {
              const response = await this.callEndAssistance(this.assistanceDetail.assistance.id)
              
              if (response.success) {
                this.currentStatus = 'completed'
                
                // 触发AI总结对话内容
                if (this.transcriptFileName) {
                  this.startAISummary(this.transcriptFileName)
                }

                await this.loadAssistanceData()
              } else {

              }
            } catch (error) {

            }
          }
        }
      })
    },

    // 重新申请
    resetApplication() {
      // 重置所有相关状态，开始新的申请流程
      this.currentStatus = 'waiting_apply'
      this.assistanceContent = ''
      this.assistanceDetail = null
      this.targetVolunteerID = ''
      this.statusBeforeCall = null
      this.currentRoomId = null
      this.transcriptionTaskId = null
      this.transcriptFileName = null
      
      // 重置对话查看相关状态
      this.showConversationModal = false
      this.conversationMode = 'transcript'
      this.conversationContent = ''
      
      // 位置信息保持不变，不重新获取
      
      console.log('🔄 手动重新申请：已重置所有状态（位置信息保持不变）')
      

    },

    // ==================== 定时刷新功能 ====================
    
    // 启动定时刷新
    startRefreshTimer() {
      this.clearRefreshTimer() // 先清除之前的定时器
      
      if (this.currentStatus === 'waiting_response') {
        this.refreshTimer = setInterval(() => {
          // 只有在 waiting_response 状态时才继续刷新
          if (this.currentStatus === 'waiting_response') {
            console.log('定时刷新: waiting_response 状态')
            this.loadAssistanceData()
          } else {
            // 状态改变了，清除定时器
            this.clearRefreshTimer()
          }
        }, 3000) // 每3秒刷新一次
        console.log('已启动定时刷新(每3秒)')
      }
    },
    
    // 清除定时刷新
    clearRefreshTimer() {
      if (this.refreshTimer) {
        clearInterval(this.refreshTimer)
        this.refreshTimer = null
        console.log('已停止定时刷新')
      }
    },

    // ==================== API调用方法 ====================
    
    // 加载协助数据
    async loadAssistanceData() {
      console.log('🚀 开始加载协助数据，elderId:', this.elderId)
      
      if (!this.elderId) {
        console.error('❌ elderId为空，无法加载协助数据')
        uni.showToast({
          title: '用户信息异常',
          icon: 'none'
        })
        return;
      }
      
      console.log('🔗 当前API基础地址:', this.apiBaseUrl)
      console.log('⚙️ 环境配置详情:', config.configs)
      
      try {
        const requestConfig = {
          url: `${this.apiBaseUrl}/api/assistance/active/${this.elderId}`,
          method: 'GET',
          timeout: 10000 // 10秒超时
        }
        console.log('📡 请求配置:', requestConfig)
        
        const response = await uni.request(requestConfig)
        console.log('📥 响应数据:', response)

        if (response.statusCode === 200) {
          if (response.data.success && response.data.data) {
            const data = response.data.data
            console.log('✅ 成功获取数据:', data)
            
            if (data.assistance) {
              this.assistanceDetail = data
              this.currentStatus = data.assistance.status
              this.assistanceContent = data.assistance.appointmentContent || ''
              console.log('📝 更新状态:', this.currentStatus, '内容:', this.assistanceContent)
              
              // 为新的协助创建转录文件名（仅在第一次waiting_call状态时）
              if (this.currentStatus === 'waiting_call' && !this.transcriptFileName) {
                this.createTranscriptFileName()
              }
              
              // 启动或停止定时刷新
              if (this.currentStatus === 'waiting_response') {
                this.startRefreshTimer()
              } else {
                this.clearRefreshTimer()
              }
              
              // 设置目标志愿者ID
              if (data.assistance.volunteerId) {
                this.targetVolunteerID = data.assistance.volunteerId.toString()
                console.log('🎯 设置目标志愿者ID:', this.targetVolunteerID)
              }
              
              // 如果状态是waiting_call，自动登录TUICallKit
              if (this.currentStatus === 'waiting_call' && !this.isTUICallKitLogin) {
                console.log('🔄 检测到waiting_call状态，准备登录TUICallKit')
                this.loginTUICallKit()
              }
            } else {
              // 没有活跃项目，显示默认状态
              this.currentStatus = data.status || 'waiting_apply'
              this.assistanceContent = ''
              this.assistanceDetail = null
              this.targetVolunteerID = ''
              console.log('📝 设置默认状态:', this.currentStatus)
            }
          } else {
            console.warn('⚠️ API返回失败:', response.data)
          }
        } else {
          console.error('❌ HTTP状态码错误:', response.statusCode)
        }
      } catch (error) {
        console.error('❌ 加载协助数据失败:', error)
        console.error('错误详情:', JSON.stringify(error))
        
        // 显示详细错误信息
        const currentApiUrl = this.apiBaseUrl
        const suggestions = this.getConnectionSuggestions(currentApiUrl)
        

      }
    },

    // 提交协助申请
    async submitAssistanceRequest(content) {
      console.log('📤 开始提交协助申请')
      console.log('📋 申请数据:', { elderId: this.elderId, content: content })
      
      // 准备提交的数据，包含位置信息
      const submitData = {
        elderId: this.elderId,
        content: content
      }
      
      // 如果有位置信息，添加到提交数据中
      if (this.location && this.currentAddress) {
        submitData.longitude = this.location.longitude
        submitData.latitude = this.location.latitude
        submitData.location = this.currentAddress
        console.log('📍 包含位置信息:', {
          longitude: submitData.longitude,
          latitude: submitData.latitude,
          location: submitData.location
        })
      } else if (this.currentAddress) {
        // 只有地址描述，没有坐标
        submitData.location = this.currentAddress
        console.log('📍 只包含地址描述:', submitData.location)
      } else {
        console.warn('⚠️ 未获取到位置信息')
      }
      
      const requestConfig = {
        url: `${this.apiBaseUrl}/api/assistance/submit`,
        method: 'POST',
        data: submitData,
        header: {
          'Content-Type': 'application/json'
        },
        timeout: 15000 // 15秒超时
      }
      console.log('📡 提交申请请求配置:', requestConfig)
      
      try {
        const response = await uni.request(requestConfig)
        console.log('📥 提交申请响应:', response)
        console.log('📊 响应状态码:', response.statusCode)
        console.log('📄 响应数据:', response.data)
        
        if (response.statusCode !== 200) {
          console.error('❌ HTTP状态码异常:', response.statusCode)
        }
        
        return response.data
      } catch (error) {
        console.error('❌ 提交申请请求失败:', error)
        console.error('🔍 错误详情:', JSON.stringify(error, null, 2))
        throw error
      }
    },

    // 开始呼叫
    async callStartCall(assistanceId) {
      const response = await uni.request({
        url: `${this.apiBaseUrl}/api/assistance/call/start/${assistanceId}`,
        method: 'POST'
      })
      return response.data
    },

    // 呼叫失败
    async callCallFailed(assistanceId) {
      const response = await uni.request({
        url: `${this.apiBaseUrl}/api/assistance/call/failed/${assistanceId}`,
        method: 'POST'
      })
      return response.data
    },

    // 呼叫失败，返回指定状态
    async callCallFailedWithStatus(assistanceId, targetStatus) {
      const response = await uni.request({
        url: `${this.apiBaseUrl}/api/assistance/call/failed/${assistanceId}/${targetStatus}`,
        method: 'POST'
      })
      return response.data
    },

    // 开始协助
    async callStartAssistance(assistanceId) {
      const response = await uni.request({
        url: `${this.apiBaseUrl}/api/assistance/start/${assistanceId}`,
        method: 'POST'
      })
      return response.data
    },

    // 结束协助
    async callEndAssistance(assistanceId) {
      const response = await uni.request({
        url: `${this.apiBaseUrl}/api/assistance/end/${assistanceId}`,
        method: 'POST'
      })
      return response.data
    },

    // 取消协助
    async callCancelAssistance(assistanceId) {
      const response = await uni.request({
        url: `${this.apiBaseUrl}/api/assistance/cancel/${assistanceId}`,
        method: 'POST'
      })
      return response.data
    },

    // 更新状态为 waiting_call_again
    async callUpdateToWaitingCallAgain(assistanceId) {
      const response = await uni.request({
        url: `${this.apiBaseUrl}/api/assistance/waiting-call-again/${assistanceId}`,
        method: 'POST'
      })
      return response.data
    },

    // 更新协助内容
    async updateAssistanceContent(content) {
      const response = await uni.request({
        url: `${this.apiBaseUrl}/api/assistance/update-content`,
        method: 'POST',
        header: {
          'Content-Type': 'application/json'
        },
        data: {
          elderId: this.elderId,
          content: content
        }
      })
      return response.data
    },

    // 更新状态为 waiting_call_again
    async updateStatusToWaitingCallAgain() {
      try {
        console.log('更新状态为 waiting_call_again')
        
        // 调用后端API更新状态为 waiting_call_again
        const response = await this.callUpdateToWaitingCallAgain(this.assistanceDetail.assistance.id)
        
        if (response && response.success) {
          this.currentStatus = 'waiting_call_again'
          console.log('后端状态已更新为 waiting_call_again')
          await this.loadAssistanceData()
        } else {
          // 如果后端API不存在或失败，则在前端管理状态
          console.log('后端API调用失败，使用前端状态管理')
          this.currentStatus = 'waiting_call_again'
        }
      } catch (error) {
        console.error('后端API调用失败，使用前端状态管理:', error)
        this.currentStatus = 'waiting_call_again'
      }
    },

    // 通话取消后恢复状态
    async restoreStatusAfterCallCancel() {
      try {
        if (!this.assistanceDetail || !this.assistanceDetail.assistance) {
          console.error('无协助详情，无法恢复状态')
          return
        }

        console.log('恢复呼叫取消前的状态:', this.statusBeforeCall)
        
        if (!this.statusBeforeCall) {
          console.warn('未记录呼叫前状态，默认恢复到waiting_call')
          this.statusBeforeCall = 'waiting_call'
        }
        
        // 统一调用后端API，根据呼叫前状态恢复相应状态
        const targetStatus = this.statusBeforeCall === 'waiting_call_again' ? 'waiting_call_again' : 'waiting_call'
        
        const response = await this.callCallFailedWithStatus(this.assistanceDetail.assistance.id, targetStatus)
        
        if (response && response.success) {
          this.currentStatus = targetStatus
          console.log('状态已通过后端API恢复为:', targetStatus)
          await this.loadAssistanceData()
        } else {
          console.error('后端API调用失败，使用前端状态恢复:', response)
          this.currentStatus = targetStatus
        }
      } catch (error) {
        console.error('恢复状态失败:', error)
        // 发生错误时，至少恢复到默认状态
        const fallbackStatus = this.statusBeforeCall || 'waiting_call'
        this.currentStatus = fallbackStatus
        console.log('错误恢复，设置状态为:', fallbackStatus)
      }
    },

    // 挂断呼叫 - calling状态下使用TUICallEngine.hangup
    hangupCall() {
      uni.showModal({
        title: '确认取消呼叫',
        content: '确定要取消本次呼叫吗？',
        success: (res) => {
          if (res.confirm) {
            try {
              console.log('用户主动取消呼叫，调用TUICallEngine.hangup()')
              
              // 使用TUICallEngine挂断通话
              TUICallEngine.hangup()
              

            } catch (error) {
              console.error('取消呼叫失败:', error)
              

            }
          }
        }
      })
    },

    // ==================== TUICallKit 相关方法 ====================
    
    // 设置TUICallKit事件监听器
    setupTUICallKitListeners() {
      console.log('设置TUICallKit事件监听器')
      
      // 通话开始事件
      this.onCallBeginHandler = (res) => {
        console.log('onCallBegin触发:', res)
        console.log('通话开始事件详细信息:', JSON.stringify(res, null, 2))
        
        // 保存房间ID，用于转录
        if (res && res.roomID) {
          this.currentRoomId = res.roomID.toString()
          console.log('保存房间ID用于转录:', this.currentRoomId)
          
          // 启动转录任务
          this.startTranscription(this.currentRoomId)
        }
        
        if (this.assistanceDetail && this.assistanceDetail.assistance) {
          this.callStartAssistance(this.assistanceDetail.assistance.id).then(response => {
            if (response && response.success) {
              this.currentStatus = 'in_progress'
              this.loadAssistanceData()
            }
          })
        }
      }
      
      // 通话结束事件 - 改为回到 waiting_call_again 状态
      this.onCallEndHandler = (res) => {
        console.log('onCallEnd触发:', res)
        
        // 停止转录任务
        if (this.currentRoomId) {
          this.stopTranscription(this.currentRoomId)
        }
        
        if (this.assistanceDetail && this.assistanceDetail.assistance) {
          // 调用后端API，将状态更新为 waiting_call_again
          this.updateStatusToWaitingCallAgain()
        }
      }

      // 通话取消事件 - 根据呼叫前状态决定回到哪个状态
      this.onCallCancelledHandler = (res) => {
        console.log('onCallCancelled触发:', res)
        
        // 停止转录任务
        if (this.currentRoomId) {
          this.stopTranscription(this.currentRoomId)
        }
        
        if (this.assistanceDetail && this.assistanceDetail.assistance) {
          // 统一调用状态恢复方法，该方法会调用后端API
          this.restoreStatusAfterCallCancel()
        }
      }
      
      // 添加事件监听器
      TUICallKitEvent.addEventListener('onCallBegin', this.onCallBeginHandler)
      TUICallKitEvent.addEventListener('onCallEnd', this.onCallEndHandler)
      TUICallKitEvent.addEventListener('onCallCancelled', this.onCallCancelledHandler)
    },

    // 清理TUICallKit事件监听器
    cleanupTUICallKitListeners() {
      console.log('清理TUICallKit事件监听器')
      
      if (this.onCallBeginHandler) {
        TUICallKitEvent.removeEventListener('onCallBegin', this.onCallBeginHandler)
        this.onCallBeginHandler = null
      }
      
      if (this.onCallEndHandler) {
        TUICallKitEvent.removeEventListener('onCallEnd', this.onCallEndHandler)
        this.onCallEndHandler = null
      }

      if (this.onCallCancelledHandler) {
        TUICallKitEvent.removeEventListener('onCallCancelled', this.onCallCancelledHandler)
        this.onCallCancelledHandler = null
      }
    },
    
    // TUICallKit 登录
    async loginTUICallKit() {
      if (this.isTUICallKitLogin) {
        console.log('🔌 TUICallKit 已登录')
        return true
      }

      if (!this.elderId) {
        console.error('❌ 无法获取用户ID，无法登录TUICallKit')
        return false
      }

      try {
        console.log('🚀 开始登录 TUICallKit，userID:', this.elderId)
        
        // 生成用户签名（从后端获取配置）
        const { userSig, sdkAppID: SDKAppID } = await genTestUserSig(this.elderId.toString())
        const loginParams = { 
          SDKAppID, 
          userID: this.elderId.toString(), 
          userSig 
        }
        
        console.log('📡 TUICallKit 登录参数:', loginParams)

        return new Promise((resolve) => {
          uni.$TUICallKit.login(loginParams, (res) => {
            console.log('📥 TUICallKit 登录响应:', res)
            
            if (res.code === 0) {
              this.isTUICallKitLogin = true
              this.callKitUserID = this.elderId.toString()
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
    
    // 使用TUICallKit进行视频通话
    async makeTUICallKitCall(targetUserID) {
      if (!this.isTUICallKitLogin) {
        console.error('❌ TUICallKit 未登录')

        return false
      }

      try {
        console.log('📞 开始拨打视频通话，目标用户ID:', targetUserID)
        
        const callParams = {
          userIDList: [targetUserID.toString()],
          callMediaType: 2// 1 -- 语音通话，2 -- 视频通话
		  // callParams: { roomID: 234, strRoomID: '2323423', timeout:30 },
        }
        
        console.log('📡 TUICallKit 通话参数:', callParams)

        return new Promise((resolve) => {
          uni.$TUICallKit.calls(callParams, (res) => {
            console.log('📥 TUICallKit 通话响应:', res)
            
            if (res.code === 0) {
              console.log('✅ 成功发起视频通话')

              resolve(true)
            } else {
              console.error('❌ 发起通话失败:', res.msg)

              resolve(false)
            }
          })
        })
      } catch (error) {
        console.error('💥 TUICallKit 通话异常:', error)

        return false
      }
    },

    // ==================== 转录相关方法 ====================
    
    /**
     * 启动转录任务
     * @param roomId 房间ID
     */
    async startTranscription(roomId) {
      if (!roomId) {
        console.warn('房间ID为空，无法启动转录')
        return
      }
      
      try {
        console.log('🎙️ 开始启动转录任务，房间ID:', roomId)
        
        // 使用前端生成转录机器人的UserSig（从后端获取配置）
        const { userSig: robotUserSig } = await genTestUserSig('robot')
        console.log('生成转录机器人UserSig:', robotUserSig)
        
        // 确保有转录文件名
        if (!this.transcriptFileName) {
          this.createTranscriptFileName()
        }
        
        const response = await uni.request({
          url: `${this.apiBaseUrl}/api/trtc/transcription/start/${roomId}`,
          method: 'POST',
          header: {
            'Content-Type': 'application/json'
          },
          data: {
            robotUserSig: robotUserSig,
            transcriptFileName: this.transcriptFileName,
            elderId: this.elderId
          },
          timeout: 10000
        })
        
        console.log('转录启动响应:', response)
        
        if (response.statusCode === 200 && response.data.success) {
          this.transcriptionTaskId = response.data.taskId
          console.log('✅ 转录任务启动成功, TaskId:', this.transcriptionTaskId)
          
          // uni.showToast({
          //   title: '语音转文字已启动',
          //   icon: 'success',
          //   duration: 2000
          // })
        } else {
          console.warn('⚠️ 转录任务启动失败:', response.data.message)
        }
        
      } catch (error) {
        console.error('❌ 启动转录任务异常:', error)
      }
    },
    
    /**
     * 停止转录任务
     * @param roomId 房间ID
     */
    async stopTranscription(roomId) {
      if (!roomId) {
        console.warn('房间ID为空，无法停止转录')
        return
      }
      
      try {
        console.log('🛑 开始停止转录任务，房间ID:', roomId)
        
        const response = await uni.request({
          url: `${this.apiBaseUrl}/api/trtc/transcription/stop/${roomId}`,
          method: 'POST',
          timeout: 10000
        })
        
        console.log('转录停止响应:', response)
        
        if (response.statusCode === 200 && response.data.success) {
          console.log('✅ 转录任务停止成功')
          this.currentRoomId = null
          this.transcriptionTaskId = null
          
          // uni.showToast({
          //   title: '语音转文字已停止',
          //   icon: 'success',
          //   duration: 2000
          // })
        } else {
          console.warn('⚠️ 转录任务停止失败:', response.data.message)
        }
        
      } catch (error) {
        console.error('❌ 停止转录任务异常:', error)
      }
    },
    
    /**
     * 启动AI总结对话内容
     * @param transcriptFileName 转录文件名
     */
    async startAISummary(transcriptFileName) {
      if (!transcriptFileName || !this.elderId) {
        console.warn('无法启动AI总结：转录文件名或用户ID为空')
        return
      }
      
      try {
        console.log('🤖 开始AI总结对话内容，文件名:', transcriptFileName)
        
        // uni.showToast({
        //   title: 'AI正在总结对话内容...',
        //   icon: 'loading',
        //   duration: 3000
        // })
        
        const response = await uni.request({
          url: `${this.apiBaseUrl}/api/summary/assistance`,
          method: 'POST',
          header: {
            'Content-Type': 'application/json'
          },
          data: {
            userId: this.elderId.toString(),
            transcriptFileName: transcriptFileName
          },
          timeout: 30000 // 30秒超时，AI处理可能需要较长时间
        })
        
        console.log('AI总结响应:', response)
        
        if (response.statusCode === 200 && response.data.success) {
          console.log('✅ AI总结完成')
          
          // uni.showToast({
          //   title: 'AI对话总结已生成',
          //   icon: 'success',
          //   duration: 3000
          // })
        } else {
          console.warn('⚠️ AI总结失败:', response.data.message)
          
          uni.showToast({
            title: 'AI总结失败，未检测到人声',
            icon: 'none',
            duration: 3000
          })
        }
        
      } catch (error) {
        console.error('❌ AI总结异常:', error)
        
        uni.showToast({
          title: 'AI总结服务暂时不可用',
          icon: 'none',
          duration: 3000
        })
      }
    },
    
    /**
     * 显示对话查看弹窗
     */
    async showConversationDialog() {
      if (!this.transcriptFileName || !this.elderId) {
        uni.showToast({
          title: '无法获取对话记录',
          icon: 'none',
          duration: 2000
        })
        return
      }
      
      this.showConversationModal = true
      this.conversationMode = 'transcript'
      this.conversationTitle = '远程协助对话记录'
      this.isLoadingContent = true
      
      try {
        const response = await uni.request({
          url: `${this.apiBaseUrl}/api/files/transcript/${this.elderId}/${this.transcriptFileName}`,
          method: 'GET',
          timeout: 10000
        })
        
        if (response.statusCode === 200 && response.data.success) {
          this.conversationContent = response.data.content
        } else {
          this.conversationContent = '无法加载对话内容：未检测到人声'
        }
        
      } catch (error) {
        console.error('获取对话内容失败:', error)
        this.conversationContent = '加载对话内容时发生错误，请稍后重试'
      } finally {
        this.isLoadingContent = false
      }
    },
    
    /**
     * 显示AI总结
     */
    async showAISummary() {
      if (!this.transcriptFileName || !this.elderId) {
        return
      }
      
      this.conversationMode = 'summary'
      this.conversationTitle = 'AI智能总结'
      this.isLoadingContent = true
      
      try {
        // 先尝试获取已有的总结文件
        const response = await uni.request({
          url: `${this.apiBaseUrl}/api/files/summary/${this.elderId}/${this.transcriptFileName}`,
          method: 'GET',
          timeout: 10000
        })
        
        if (response.statusCode === 200 && response.data.success) {
          // 总结文件存在，直接显示
          this.conversationContent = response.data.content
        } else if (response.data.needGenerate) {
          // 需要生成总结，调用AI总结接口
          this.conversationContent = '正在生成AI总结，请稍候...'
          await this.generateAISummary()
        } else {
          this.conversationContent = '无法加载总结内容：' + (response.data.message || '未知错误')
        }
        
      } catch (error) {
        console.error('获取总结内容失败:', error)
        this.conversationContent = '加载总结内容时发生错误，请稍后重试'
      } finally {
        this.isLoadingContent = false
      }
    },
    
    /**
     * 生成AI总结
     */
    async generateAISummary() {
      try {
        const response = await uni.request({
          url: `${this.apiBaseUrl}/api/summary/assistance`,
          method: 'POST',
          header: {
            'Content-Type': 'application/json'
          },
          data: {
            userId: this.elderId.toString(),
            transcriptFileName: this.transcriptFileName
          },
          timeout: 30000
        })
        
        if (response.statusCode === 200 && response.data.success) {
          this.conversationContent = response.data.summary
        } else {
          this.conversationContent = 'AI总结生成失败：' + (response.data.message || '未知错误')
        }
        
      } catch (error) {
        console.error('生成AI总结失败:', error)
        this.conversationContent = 'AI总结服务暂时不可用，请稍后重试'
      }
    },
    
    /**
     * 将协助记录加入知识库
     */
    async addToKnowledgeBase() {
      if (!this.transcriptFileName || !this.elderId) {
        return
      }
      
      this.isLoadingContent = true
      
      try {
        const response = await uni.request({
          url: `${this.apiBaseUrl}/api/files/copy-to-rag/${this.elderId}/${this.transcriptFileName}`,
          method: 'POST',
          timeout: 10000
        })
        
        if (response.statusCode === 200 && response.data.success) {
          uni.showToast({
            title: '已加入个人知识库',
            icon: 'success',
            duration: 3000
          })
          
          // 成功后关闭弹窗
          setTimeout(() => {
            this.closeConversationDialog()
          }, 1500)
          
        } else {
          uni.showToast({
            title: response.data.message || '加入知识库失败',
            icon: 'none',
            duration: 3000
          })
        }
        
      } catch (error) {
        console.error('加入知识库失败:', error)
        uni.showToast({
          title: '加入知识库失败，请稍后重试',
          icon: 'none',
          duration: 3000
        })
      } finally {
        this.isLoadingContent = false
      }
    },
    
    /**
     * 关闭对话查看弹窗
     */
    closeConversationDialog() {
      this.showConversationModal = false
      this.conversationMode = 'transcript'
      this.conversationTitle = '远程协助对话记录'
      this.conversationContent = ''
      this.isLoadingContent = false
    },

    // ==================== 位置获取相关方法 ====================
    
    /**
     * 获取实时位置
     */
    async getRealTimeLocation() {
      // 防止重复定位
      if (this.isLocating) {
        console.log('⚠️ 正在定位中，跳过重复请求')
        return
      }
      
      this.isLocating = true
      this.locationStatus = '正在定位...'
      
      console.log('📍 开始获取远程协助位置...')
      
      uni.getLocation({
        type: 'gcj02', // 国测局坐标
        altitude: true, // 获取高度
        isHighAccuracy: true, // 高精度定位
        success: async (res) => {
          console.log('✅ 定位成功！', res)
          
          this.location = {
            longitude: res.longitude,
            latitude: res.latitude,
            accuracy: res.accuracy,
            altitude: res.altitude
          }
          
          this.locationStatus = '解析地址中...'
          
          // 调用逆地理编码获取地址
          await this.callReverseGeocode(res.longitude, res.latitude)
        },
        fail: (err) => {
          console.error('❌ 定位失败：', err)
          this.locationStatus = '定位失败，请检查定位权限'
          this.currentAddress = ''
          this.location = null
          
          uni.showToast({
            title: '定位失败',
            icon: 'none',
            duration: 2000
          })
        },
        complete: () => {
          this.isLocating = false
        }
      })
    },
    
    /**
     * 调用高德逆地理编码API
     */
    async callReverseGeocode(longitude, latitude) {
      try {
        console.log('🗺️ 调用逆地理编码API:', longitude, latitude)
        
        const response = await uni.request({
          url: `${config.API_BASE_URL}/api/geocoding/regeo`,
          method: 'GET',
          data: {
            longitude: longitude,
            latitude: latitude,
            radius: 500,
            extensions: 'base'
          }
        })
        
        console.log('📥 逆地理编码返回:', response.data)
        
        if (response.data.success) {
          this.currentAddress = response.data.formattedAddress
          console.log('✅ 地址解析成功:', this.currentAddress)
        } else {
          this.currentAddress = `经度:${longitude.toFixed(6)}, 纬度:${latitude.toFixed(6)}`
          console.error('❌ 地址解析失败:', response.data.message)
        }
        
      } catch (error) {
        console.error('❌ 逆地理编码API调用失败:', error)
        this.currentAddress = `经度:${longitude.toFixed(6)}, 纬度:${latitude.toFixed(6)}`
      }
    },
    
    // ==================== 工具方法 ====================
    
    /**
     * 为当前协助创建转录文件名
     * 格式：record_{elderId}_{timestamp}.txt
     */
    createTranscriptFileName() {
      if (!this.elderId) {
        console.warn('无法创建转录文件名：用户ID为空')
        return
      }
      
      const now = new Date()
      const timestamp = now.getFullYear() + 
        String(now.getMonth() + 1).padStart(2, '0') + 
        String(now.getDate()).padStart(2, '0') + '_' + 
        String(now.getHours()).padStart(2, '0') + 
        String(now.getMinutes()).padStart(2, '0') + 
        String(now.getSeconds()).padStart(2, '0')
      
      this.transcriptFileName = `record_${this.elderId}_${timestamp}.txt`
      console.log('📝 为当前协助创建转录文件名:', this.transcriptFileName)
    },
    
    // 获取当前用户ID
    getCurrentUserId() {
      const userId = getAuthUserId();
      if (!userId) {
        // 如果获取不到用户ID，说明用户未登录，需要跳转到登录页
        requireLogin();
        return null;
      }
      return userId;
    },
    
    // 获取连接建议
    getConnectionSuggestions(apiUrl) {
      if (apiUrl.includes('localhost') || apiUrl.includes('127.0.0.1')) {
        return `建议检查：
1. 后端服务器是否在8080端口启动
2. 如果在APP/小程序中测试，请使用IP地址
3. 尝试在浏览器中访问：${apiUrl}`
      } else {
        const ip = apiUrl.match(/(\d+\.\d+\.\d+\.\d+)/)?.[1]
        return `建议检查：
1. 后端服务器是否在${ip}:8080启动
2. 设备是否与服务器在同一网络
3. 防火墙是否开放8080端口
4. 在浏览器中测试：${apiUrl}`
      }
    },
    
    // 检查网络状态
    checkNetworkStatus() {
      try {
        uni.getNetworkType({
          success: (res) => {
            console.log('🌐 网络类型:', res.networkType)
            if (res.networkType === 'none') {
              console.error('❌ 无网络连接')
              uni.showModal({
                title: '网络错误',
                content: '当前无网络连接，请检查网络设置后重试',
                showCancel: false
              })
            } else {
              console.log('✅ 网络连接正常:', res.networkType)
            }
          },
          fail: (error) => {
            console.error('❌ 获取网络状态失败:', error)
          }
        })
      } catch (error) {
        console.error('❌ 网络状态检查异常:', error)
      }
    },
    
    // 格式化日期时间
    formatDateTime(dateString) {
      if (!dateString) return ''
      const date = new Date(dateString)
      return `${date.getFullYear()}-${(date.getMonth()+1).toString().padStart(2,'0')}-${date.getDate().toString().padStart(2,'0')} ${date.getHours().toString().padStart(2,'0')}:${date.getMinutes().toString().padStart(2,'0')}`
    },
    
    // 格式化持续时间
    formatDuration(minutes) {
      if (!minutes || minutes <= 0) return ''
      const hours = Math.floor(minutes / 60)
      const mins = minutes % 60
      return hours > 0 ? `${hours}小时${mins}分钟` : `${mins}分钟`
    }
  },
  
  // 页面加载时的逻辑
  onLoad() {
    console.log('📱 远程协助页面加载 - onLoad')
    
    // 检查用户是否已登录，获取用户ID
    if (!requireLogin(false)) {
      return; // 如果未登录，requireLogin会处理跳转
    }
    
    // 获取当前用户ID
    this.elderId = this.getCurrentUserId()
    if (!this.elderId) {
      console.error('❌ 无法获取用户ID')
      return;
    }
    
    console.log('👤 当前用户ID:', this.elderId)
    console.log('🌐 API基础地址:', this.apiBaseUrl)
    console.log('🏃 当前运行环境:', config.ENV)
    
    // 设置TUICallKit事件监听器
    this.setupTUICallKitListeners()
    
    // 检查网络状态
    this.checkNetworkStatus()
    
    // 获取位置信息
    this.getRealTimeLocation()
    
    // 显示当前API地址

    
    this.loadAssistanceData()
  },

  // 页面显示时重新加载数据
  async onShow() {
    console.log('👁️ 远程协助页面显示 - onShow')
    
    // 确保有用户ID
    if (!this.elderId) {
      this.elderId = this.getCurrentUserId()
    }
    
    if (this.elderId) {
      await this.loadAssistanceData()
      
      // 如果状态是completed或cancelled，自动重置为waiting_apply
      // 这样用户重新进入页面时就可以直接开始新的申请
      if (this.currentStatus === 'completed' || this.currentStatus === 'cancelled') {
        console.log('🔄 重新进入页面，检测到已完成/已取消状态，自动重置为等待申请状态')
        this.currentStatus = 'waiting_apply'
        this.assistanceContent = ''
        this.assistanceDetail = null
        this.targetVolunteerID = ''
        this.statusBeforeCall = null
        this.transcriptFileName = null
        
        // 重置对话查看相关状态
        this.showConversationModal = false
        this.conversationMode = 'transcript'
        this.conversationContent = ''
        
        // 位置信息保持不变，不重新获取
        

      }
    }
  },

  // 页面卸载时的逻辑
  onUnload() {
    console.log('📱 远程协助页面卸载 - onUnload')
    
    // 清理TUICallKit事件监听器
    this.cleanupTUICallKitListeners()
    
    // 清理定时刷新
    this.clearRefreshTimer()
  }
}
</script>

<style scoped>
.assistance-page {
  height: 100vh;
  background-color: #f8f9fa;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

/* 顶部功能描述 */
.header {
  background: linear-gradient(135deg, #4CAF50, #81C784);
  padding: 90rpx 30rpx 40rpx;
  color: white;
  position: relative;
}

.back-button {
  position: absolute;
  left: 20rpx;
  top: 82rpx;
  width: 60rpx;
  height: 60rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  z-index: 10;
}

.back-icon {
  width: 36rpx;
  height: 36rpx;
  
}

.header-content {
  text-align: center;
}

.header-title {
  font-size: 44rpx;
  font-weight: bold;
  display: block;
  margin-bottom: 20rpx;
}

.header-description {
  font-size: 28rpx;
  opacity: 0.9;
  display: block;
}

/* 中间状态插画 */
.status-illustration {
  text-align: center;
  /* padding: 60rpx 30rpx; */
  background-color: #ffffff00;
  margin: 10rpx;
  border-radius: 20rpx;
  padding-top: 50rpx;
}

.status-image {
  /* width: 200rpx;
  height: 200rpx; */
  margin-bottom: 10rpx;
}

.status-text {
  font-size: 32rpx;
  color: #666666a4;
  display: block;
}

/* 服务状态卡片 */
.service-card {
  background-color: #fff;
  border-radius: 50rpx 50rpx 0 0;
  padding: 0;
  box-shadow: 0 -4rpx 12rpx rgba(0,0,0,0.1);
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  z-index: 999;
  min-height: auto;
  max-height: 90vh;
  overflow: visible;
  display: flex;
  flex-direction: column;
}

/* 取消按钮区域（右上角） */
.cancel-button-area {
  position: absolute;
  top: 20rpx;
  right: 30rpx;
  z-index: 1000;
}

/* 卡片内容区域 */
.card-content {
  flex: 1;
  padding: 40rpx;
  overflow-y: visible;
  min-height: auto;
}

.status-title {
  margin-bottom: 30rpx;
}

.status-label {
  font-size: 36rpx;
  font-weight: bold;
  color: #4CAF50;
  display: block;
}

/* 服务详情 */
.service-details {
  margin-bottom: 20rpx;
}

.detail-item {
  display: flex;
  margin-bottom: 15rpx;
  align-items: flex-start;
}

.detail-label {
  font-size: 28rpx;
  color: #666;
  min-width: 180rpx;
}

.detail-value {
  font-size: 28rpx;
  color: #333;
  flex: 1;
  word-break: break-all;
}



/* 底部按钮区域 */
.card-bottom-buttons {
  padding: 20rpx;
  background-color: #fff;
}

.btn {
  padding: 20rpx 40rpx;
  border-radius: 24rpx;
  font-size: 30rpx;
  font-weight: bold;
  border: none;
  min-width: 160rpx;
  text-align: center;
}

.btn-primary {
  background-color: #4CAF50;
  color: white;
}

.btn-primary:disabled {
  background-color: #cccccc;
  color: #999;
}

.btn-success {
  background-color: #2196F3;
  color: white;
}

.btn-warning {
  background-color: #FF9800;
  color: white;
}



.btn-disabled {
  background-color: #f5f5f5;
  color: #999;
  cursor: not-allowed;
}

/* 全宽按钮 */
.btn-full {
  width: 100%;
  display: block;
}

/* 双按钮容器 */
.dual-buttons {
  display: flex;
  gap: 20rpx;
}

/* 半宽按钮 */
.btn-half {
  flex: 1;
  display: block;
}

/* 右上角取消按钮 */
.btn-cancel-corner {
  background-color: #fff;
  color: #c5c5c5;
  border: 2rpx solid #bfbfbf;
  padding: 4rpx 8rpx;     /* 按钮内边距，参数：上右下左 */
  border-radius: 12rpx;
  font-size: 24rpx;
  font-weight: normal;
  min-width: auto;    /* 最小宽度，自动适应内容 */
  /* 上距离 */
  margin-top: 10rpx;
  margin-right: 10rpx;
}

.btn-cancel-corner.btn-disabled {
  background-color: #f5f5f5;
  color: #999;
  border: 2rpx solid #e1e1e1;
  box-shadow: none;
}

/* 可编辑项样式 */
.editable-item {
  position: relative;
  cursor: pointer;
}

.editable-item:hover {
  background-color: rgba(76, 175, 80, 0.05);
}

.arrow-icon {
  color: #a8a8a8;
  font-size: 25rpx;
  margin-left: 10rpx;
}

/* 弹窗样式 */
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: rgba(0, 0, 0, 0.5);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 999;
}

.modal-content {
  background-color: #fff;
  border-radius: 20rpx;
  width: 80%;
  max-width: 600rpx;
  max-height: 80vh;
  overflow: hidden;
  box-shadow: 0 8rpx 24rpx rgba(0, 0, 0, 0.15);
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 40rpx;
  border-bottom: 2rpx solid #f1f1f1;
}

.modal-title {
  font-size: 36rpx;
  font-weight: bold;
  color: #333;
}

.modal-close {
  font-size: 48rpx;
  color: #999;
  cursor: pointer;
  width: 60rpx;
  height: 60rpx;
  display: flex;
  justify-content: center;
  align-items: center;
}

.modal-body {
  padding: 20rpx;
}

.modal-textarea {
  width: 100%;
  min-height: 200rpx;
  padding: 10rpx;  /* 内边距，参数：上右下左 */
  border: 2rpx solid #e1e1e1;
  border-radius: 12rpx;
  font-size: 28rpx;
  background-color: #fafafa;
  resize: none;
  display: block;
  margin: 0 auto;
  box-sizing: border-box;
}

.char-count {
  display: block;
  text-align: right;
  font-size: 24rpx;
  color: #999;
  margin-top: 10rpx;
}

.modal-footer {
  display: flex;
  justify-content: flex-end;
  gap: 20rpx;
  padding: 40rpx;
  border-top: 2rpx solid #f1f1f1;
  background-color: #fafafa;
}

/* 协助内容输入框样式 */
.assistance-input {
  width: 100%;
  min-height: 120rpx;
  padding: 15rpx;
  border: 2rpx solid #e1e1e1;
  border-radius: 12rpx;
  font-size: 28rpx;
  background-color: #fafafa;
  margin-top: 10rpx;
  box-sizing: border-box;
}

/* 取消呼叫按钮样式 */
.btn-cancel {
  background-color: #FF9800;
  color: white;
}

/* 对话查看弹窗样式 */
.conversation-modal {
  background-color: #fff;
  border-radius: 20rpx;
  width: 90%;
  max-width: 800rpx;
  max-height: 80vh;
  overflow: hidden;
  box-shadow: 0 8rpx 24rpx rgba(0, 0, 0, 0.15);
  display: flex;
  flex-direction: column;
}

.conversation-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 30rpx 40rpx;
  border-bottom: 2rpx solid #f1f1f1;
  background-color: #fafafa;
}

.conversation-title {
  font-size: 36rpx;
  font-weight: bold;
  color: #333;
}

.conversation-close {
  font-size: 48rpx;
  color: #999;
  cursor: pointer;
  width: 60rpx;
  height: 60rpx;
  display: flex;
  justify-content: center;
  align-items: center;
}

.conversation-body {
  flex: 1;
  overflow-y: auto;
  min-height: 300rpx;
  max-height: 600rpx;
}

.loading-content {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 200rpx;
  color: #666;
  font-size: 28rpx;
}

.conversation-content {
  padding: 30rpx;
}

.conversation-text {
  font-size: 28rpx;
  line-height: 1.6;
  color: #333;
  white-space: pre-wrap;
  word-break: break-word;
  font-family: system-ui, -apple-system, sans-serif;
}

.conversation-footer {
  display: flex;
  justify-content: space-between;
  padding: 30rpx 40rpx;
  border-top: 2rpx solid #f1f1f1;
  background-color: #fafafa;
}

.conversation-footer .btn {
  flex: 1;
  margin: 0 10rpx;
  padding: 24rpx;
  font-size: 28rpx;
}

.conversation-footer .btn:first-child {
  margin-left: 0;
}

.conversation-footer .btn:last-child {
  margin-right: 0;
}
</style>
