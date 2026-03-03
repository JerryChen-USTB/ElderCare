/**
 * 紧急求助弹窗全局混入
 * 用于在所有页面显示紧急求助通知
 * 采用单例模式，确保只在当前活跃页面显示弹窗
 */
export default {
  data() {
    return {
      showEmergencyModal: false,
      emergencyTitle: '',
      emergencyContent: '',
      emergencyData: null,
      isCurrentPage: false  // 标记当前页面是否活跃
    }
  },
  
  onLoad() {
    // 监听全局紧急求助弹窗事件
    uni.$on('showEmergencyModal', this.handleShowEmergencyModal)
    // 监听全局关闭弹窗事件
    uni.$on('closeAllEmergencyModals', this.handleCloseAllModals)
    console.log('✅ [当前页面] 已注册紧急弹窗事件监听')
  },
  
  onShow() {
    // 页面显示时标记为当前页面
    this.isCurrentPage = true
    console.log('👁️ [当前页面] 页面已显示，设置为活跃状态')
  },
  
  onHide() {
    // 页面隐藏时取消标记，并关闭弹窗
    this.isCurrentPage = false
    if (this.showEmergencyModal) {
      console.log('🙈 [当前页面] 页面已隐藏，关闭弹窗')
      this.showEmergencyModal = false
    }
  },
  
  onUnload() {
    // 移除全局事件监听
    uni.$off('showEmergencyModal', this.handleShowEmergencyModal)
    uni.$off('closeAllEmergencyModals', this.handleCloseAllModals)
    console.log('🔚 [当前页面] 已移除紧急弹窗事件监听')
  },
  
  methods: {
    // 处理显示紧急求助弹窗
    handleShowEmergencyModal(data) {
      console.log('📨 [当前页面] 收到紧急弹窗事件:', data)
      console.log('📨 [当前页面] isCurrentPage:', this.isCurrentPage)
      
      // 只在当前活跃页面显示弹窗
      if (this.isCurrentPage) {
        this.emergencyTitle = data.title
        this.emergencyContent = data.content
        this.emergencyData = data.data
        this.showEmergencyModal = true
        console.log('✅ [当前页面] 紧急弹窗已显示')
      } else {
        console.log('⏭️ [当前页面] 非活跃页面，跳过显示弹窗')
      }
    },
    
    // 处理全局关闭弹窗事件
    handleCloseAllModals() {
      if (this.showEmergencyModal) {
        console.log('🔒 [当前页面] 收到全局关闭事件，关闭弹窗')
        this.showEmergencyModal = false
        this.emergencyTitle = ''
        this.emergencyContent = ''
        this.emergencyData = null
      }
    },
    
    // 关闭紧急求助弹窗
    closeEmergencyModal() {
      console.log('👆 [当前页面] 用户点击知道了')
      this.showEmergencyModal = false
      this.emergencyTitle = ''
      this.emergencyContent = ''
      this.emergencyData = null
      
      // 触发全局关闭事件，通知所有页面关闭弹窗
      uni.$emit('closeAllEmergencyModals')
      console.log('📢 [当前页面] 已发送全局关闭弹窗事件')
    }
  }
}

