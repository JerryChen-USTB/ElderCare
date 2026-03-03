<script>
export default {
  onLaunch() {
    console.log('App Launch')
	
	// 延迟注册推送监听，确保应用完全启动
	setTimeout(() => {
	  console.log('🔔 开始监听推送消息...')
	  
	  // 监听在线推送（前台会走这里）
	  uni.onPushMessage(res => {
	    console.log('📨 收到推送消息 [原始数据]：', JSON.stringify(res))
	    
	    // 处理推送消息
	    this.handlePushMessage(res)
	  })
	  
	  console.log('✅ 推送消息监听器已注册')
	}, 500)
  },
  onShow() {
    console.log('App Show')
  },
  onHide() {
    console.log('App Hide')
  },
  methods: {
	/**
	 * 处理推送消息
	 */
	handlePushMessage(message) {
	  try {
		console.log('📨 [步骤1] 开始处理推送消息')
		console.log('📨 [步骤2] 消息内容:', message)
		
		// uni-push 2.0 的消息格式可能是 { type: 'receive', data: {...} }
		let msgData = message
		if (message.type === 'receive' && message.data) {
		  msgData = message.data
		  console.log('📨 [步骤3] 提取 data 字段:', msgData)
		}
		
		// 获取消息内容（兼容多种字段名）
		const title = msgData.title || msgData.notification?.title || '新消息'
		const content = msgData.content || msgData.notification?.body || ''
		const payload = msgData.payload || msgData.data?.payload || ''
		
		console.log('📨 [步骤4] 解析后 - title:', title)
		console.log('📨 [步骤5] 解析后 - content:', content)
		console.log('📨 [步骤6] 解析后 - payload:', payload)
		
		// 解析 payload
		let pushData = {}
		if (payload) {
		  try {
			pushData = typeof payload === 'string' ? JSON.parse(payload) : payload
			console.log('📨 [步骤7] payload 解析成功:', pushData)
		  } catch (e) {
			console.error('❌ 解析 payload 失败:', e)
			console.error('❌ payload 原始值:', payload)
		  }
		}
		
		// 判断消息类型
		console.log('📨 [步骤8] 判断消息类型, pushData.type =', pushData.type)
		
		if (pushData.type === 'emergency') {
		  console.log('🚨 [步骤9] 识别为紧急求助消息，开始处理')
		  // 紧急求助消息
		  this.handleEmergencyPush(title, content, pushData)
		} else {
		  console.log('📬 [步骤9] 识别为普通消息，显示提示')
		  // 其他类型的推送消息
		  this.showPushNotification(title, content)
		}
		
	  } catch (error) {
		console.error('❌ 处理推送消息失败:', error)
		console.error('❌ 错误堆栈:', error.stack)
	  }
	},
	
	/**
	 * 处理紧急求助推送
	 */
	handleEmergencyPush(title, content, data) {
	  console.log('🚨 [紧急求助-步骤1] 收到紧急求助通知')
	  console.log('🚨 [紧急求助-步骤2] title:', title)
	  console.log('🚨 [紧急求助-步骤3] content:', content)
	  console.log('🚨 [紧急求助-步骤4] data:', data)
	  
	  // 1. 三声长震动提醒
	  console.log('🚨 [紧急求助-步骤5] 准备触发三声长震动')
	  this.tripleVibrate()
	  
	  // 2. 触发全局事件，让页面显示弹窗
	  console.log('🚨 [紧急求助-步骤7] 准备触发全局弹窗事件')
	  uni.$emit('showEmergencyModal', {
		title: title || '紧急求助通知',
		content: content || '有人向您发出紧急求助',
		data: data
	  })
	  
	  console.log('✅ [紧急求助-完成] 紧急求助处理完成')
	},
	
	/**
	 * 三声长震动
	 */
	tripleVibrate() {
	  let count = 0
	  const vibrate = () => {
		uni.vibrateLong({
		  success: () => {
			count++
			console.log(`✅ 第${count}声震动成功`)
			
			// 如果还没到3次，继续震动
			if (count < 3) {
			  setTimeout(vibrate, 600) // 间隔600ms后继续
			}
		  },
		  fail: (err) => {
			console.error(`❌ 第${count + 1}声震动失败:`, err)
		  }
		})
	  }
	  vibrate()
	},
	
	/**
	 * 显示普通推送通知
	 */
	showPushNotification(title, content) {
	  uni.showToast({
		title: title || '新消息',
		icon: 'none',
		duration: 2000
	  })
	  
	  // 可选：短震动
	  uni.vibrateShort()
	}
  }
}
</script>

<style>
/* 全局样式 */
page {
  background-color: #f8f9fa;
  font-size: 28rpx;
  color: #333;
  font-family: -apple-system, BlinkMacSystemFont, 'Helvetica Neue', sans-serif;
}
</style>