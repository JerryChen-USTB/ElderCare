<template>
  <view class="login-container">
    <view class="header">
      <image src="/static/eldercare_logo.png" class="logo" />
      <text class="title">欢迎使用银杏智伴</text>
    </view>
    
    <view class="form-container">
      <input class="input" placeholder="请输入手机号" v-model="phone" type="number" maxlength="11" />
      <input class="input" placeholder="请输入密码" v-model="password" password />
      
      <button class="btn login-btn" @click="handleLogin">登录</button>
      <button class="btn register-btn" @click="goToRegister">注册</button>
      
      <view class="forgot-password" @click="showForgotPasswordModal">
        <text>忘记密码?</text>
      </view>
    </view>
    
    <view class="footer">
      <text>其他登录方式</text>
      <view class="third-party">
        <image src="/static/icons/wechat.png" class="icon" @click="thirdPartyLogin('wechat')" />
        <image src="/static/icons/qq.png" class="icon" @click="thirdPartyLogin('qq')" />
      </view>
    </view>

    <!-- 忘记密码弹窗 -->
    <view class="modal-mask" 
          v-show="forgotPasswordVisible" 
          :style="{ display: forgotPasswordVisible ? 'flex' : 'none' }"
          @touchmove.stop.prevent>
      <view class="modal-container" :class="{ 'show-modal': forgotPasswordVisible }">
        <view class="modal-header">
          <text class="modal-title">找回密码</text>
          <text class="close-btn" @click="closeForgotPasswordModal">×</text>
        </view>
        <view class="modal-content">
          <input class="modal-input" placeholder="请输入手机号" v-model="forgotPhone" type="number" maxlength="11" />
          <view class="verify-code-group">
            <input class="modal-input" placeholder="请输入验证码" v-model="verifyCode" />
            <button class="get-code-btn" :disabled="countdown > 0" @click="getVerifyCode">
              {{ countdown > 0 ? `${countdown}s` : '获取验证码' }}
            </button>
          </view>
          <input class="modal-input" placeholder="请输入新密码" v-model="newPassword" password />
          <input class="modal-input" placeholder="请再次输入新密码" v-model="confirmPassword" password />
          <view class="password-tips" v-if="newPassword && !isPasswordValid">
            <text class="error">密码需8-16位，包含数字和字母，符号只能使用._</text>
          </view>
        </view>
        <view class="modal-footer">
          <button class="modal-btn cancel-btn" @click="closeForgotPasswordModal">取消</button>
          <button class="modal-btn confirm-btn" @click="resetPassword" :disabled="!isResetFormValid">确认</button>
        </view>
      </view>
    </view>

    <!-- 第三方登录弹窗 -->
    <view class="auth-modal" v-show="authModalVisible">
      <view class="auth-container">
        <text class="auth-title">{{ authPlatformName }}登录</text>
        <view class="auth-content">
          <image :src="authPlatformIcon" class="auth-icon" />
          <text class="auth-message">请使用{{ authPlatformName }}扫描二维码登录</text>
          <view class="qrcode-container">
            <image :src="authQrcodeUrl" class="qrcode" mode="aspectFit" />
          </view>
          <text class="auth-tips">扫码后请在手机端确认登录</text>
        </view>
        <button class="auth-cancel-btn" @click="closeAuthModal">取消</button>
      </view>
    </view>
  </view>
</template>

<script>
import request from '@/utils/request.js'

export default {
  data() {
    return {
      phone: '',
      password: '',
      forgotPasswordVisible: false,
      forgotPhone: '',
      verifyCode: '',
      newPassword: '',
      confirmPassword: '',
      countdown: 0,
      timer: null,
      loading: false,
      isModalRendered: false,
      authModalVisible: false,
      authType: '',
      authPlatformName: '',
      authPlatformIcon: '',
      authQrcodeUrl: '',
      authPollingTimer: null
    }
  },
  computed: {
    isPasswordValid() {
      const pattern = /^(?=.*[A-Za-z])(?=.*\d)[A-Za-z\d._]{8,16}$/
      return pattern.test(this.newPassword)
    },
    isResetFormValid() {
      return this.forgotPhone && this.verifyCode && this.newPassword && this.confirmPassword && 
             this.newPassword === this.confirmPassword && this.isPasswordValid
    }
  },
  methods: {
    async handleLogin() {
      if (!this.phone) {
        uni.showToast({
          title: '请输入手机号',
          icon: 'none'
        })
        return
      }
      
      if (!this.password) {
        uni.showToast({
          title: '请输入密码',
          icon: 'none'
        })
        return
      }
      
      if (!/^1[3-9]\d{9}$/.test(this.phone)) {
        uni.showToast({
          title: '请输入正确的手机号',
          icon: 'none'
        })
        return
      }
      
      try {
        this.loading = true
        uni.showLoading({ title: '登录中...' })
        
        const result = await request.userApi.login(this.phone, this.password);
        
        this.loading = false
        uni.hideLoading()
        
        if (result.success) {
          // 保存用户信息到本地存储
          uni.setStorageSync('userInfo', result.user)
		  
		  // ========== 新增日志 ==========
		    console.log('登录成功，用户信息：', result.user);
		    console.log('用户ID：', result.user.id);
		    console.log('用户角色：', result.user.role);
		    // ==============================
          
          // 获取并保存推送设备ID
          this.savePushClientId(result.user.id)
          
          uni.showToast({ title: result.message, icon: 'success' })
          
          setTimeout(() => {
            // 检查用户是否已选择角色
            if (result.user.role) {
              // 已选择角色，直接跳转到对应页面
              const rolePageMap = {
                elder: '/pages/user/index/index',
                guardian: '/pages/guardian/index/index', 
                volunteer: '/pages/volunteer/index/index'
              }
              
              const targetPage = rolePageMap[result.user.role]
              if (targetPage) {
                uni.reLaunch({ url: targetPage })
              } else {
                uni.navigateTo({ url: '/pages/role/role' })
              }
            } else {
              // 未选择角色，跳转到角色选择页面
              uni.navigateTo({ url: '/pages/role/role' })
            }
          }, 1500)
        } else {
          uni.showToast({
            title: result.message,
            icon: 'none'
          })
        }
        
      } catch (error) {
        this.loading = false
        uni.hideLoading()
        console.error('登录失败:', error)
        uni.showToast({
          title: '登录失败，请检查网络连接',
          icon: 'none'
        })
      }
    },
    goToRegister() {
      uni.navigateTo({ url: '/pages/register/register' })
    },
    async showForgotPasswordModal() {
      try {
        console.log('准备显示弹窗，当前状态:', this.forgotPasswordVisible)
        
        // 重置表单状态
        this.resetForgotForm()
        
        // 强制更新DOM
        this.isModalRendered = false
        await this.$nextTick()
        this.isModalRendered = true
        
        // 显示弹窗
        this.forgotPasswordVisible = true
        await this.$nextTick()
        
        console.log('弹窗已显示，当前状态:', this.forgotPasswordVisible)
      } catch (error) {
        console.error('显示弹窗出错:', error)
        uni.showToast({ title: '操作失败，请重试', icon: 'none' })
      }
    },
    closeForgotPasswordModal() {
      this.forgotPasswordVisible = false
      this.resetForgotForm()
    },
    resetForgotForm() {
      this.forgotPhone = ''
      this.verifyCode = ''
      this.newPassword = ''
      this.confirmPassword = ''
      this.countdown = 0
      if (this.timer) clearInterval(this.timer)
      this.timer = null
    },
    async getVerifyCode() {
      if (!this.forgotPhone) {
        uni.showToast({
          title: '请输入手机号',
          icon: 'none'
        })
        return
      }
      
      if (!/^1[3-9]\d{9}$/.test(this.forgotPhone)) {
        uni.showToast({
          title: '请输入正确的手机号',
          icon: 'none'
        })
        return
      }
      
      this.loading = true
      uni.showLoading({ title: '发送中...' })
      
      try {
          // 调用后端“发送忘记密码验证码”接口
          const result = await request.userApi.sendForgotPwdCode({ phone: this.forgotPhone });
          
          this.loading = false;
          uni.hideLoading();
      
          if (result.success) {
            // 如果后端返回了验证码（开发/测试模式），自动填充
            if (result.data) {
              this.verifyCode = result.data;
              console.log('✅ 验证码已自动填充：', result.data);
            } else {
              uni.showToast({ title: result.message, icon: 'success' });
            }
            
            // 启动倒计时（60秒后可重新获取）
            this.countdown = 60;
            this.timer = setInterval(() => {
              this.countdown--;
              if (this.countdown <= 0) {
                clearInterval(this.timer);
                this.timer = null;
              }
            }, 1000);
          } else {
            uni.showToast({ title: result.message, icon: 'none' });
          }
        } catch (error) {
          this.loading = false;
          uni.hideLoading();
          console.error('发送验证码失败:', error);
          uni.showToast({ title: '发送验证码失败，请重试', icon: 'none' });
        }
    },
    async resetPassword() {
      // 1. 表单有效性校验（复用原有逻辑）
      if (!this.isResetFormValid) {
        if (this.newPassword && !this.isPasswordValid) {
          uni.showToast({ title: '密码格式不符合要求', icon: 'none' });
        } else if (this.newPassword !== this.confirmPassword) {
          uni.showToast({ title: '两次输入的密码不一致', icon: 'none' });
        } else {
          uni.showToast({ title: '请填写完整信息', icon: 'none' });
        }
        return;
      }
    
      this.loading = true;
      uni.showLoading({ title: '重置中...' });
    
      try {
        // 调用后端“重置密码”接口，传递手机号、验证码、新密码
        const params = {
          phone: this.forgotPhone,
          verifyCode: this.verifyCode,
          newPassword: this.newPassword
        };
        const result = await request.userApi.resetPassword(params);
        
        this.loading = false;
        uni.hideLoading();
    
        if (result.success) {
          this.closeForgotPasswordModal(); // 关闭弹窗
          uni.showToast({ 
            title: result.message, 
            icon: 'success',
            duration: 3000
          });
        } else {
          uni.showToast({ title: result.message, icon: 'none' });
        }
      } catch (error) {
        this.loading = false;
        uni.hideLoading();
        console.error('重置密码失败:', error);
        uni.showToast({ title: '重置密码失败，请重试', icon: 'none' });
      }
    },
    thirdPartyLogin(type) {
      // 显示暂不支持的提示
      const platformName = type === 'wechat' ? '微信' : type === 'qq' ? 'QQ' : type
      
      uni.showModal({
        title: '提示',
        content: `暂不支持${platformName}登录，请使用手机号登录`,
        showCancel: false,
        confirmText: '知道了',
        confirmColor: '#4a9ff5'
      })
    },
    async getAuthQrcode() {
      try {
        this.loading = true
        uni.showLoading({ title: `获取${this.authPlatformName}登录二维码...` })
        
        // 调用后端接口获取二维码
        // 接口地址: POST /api/auth/${this.authType}/qrcode
        // 参数: { appId: 'your-app-id', redirectUri: 'your-redirect-uri' }
        const response = {
          data: {
            qrcodeUrl: `/static/qrcode/${this.authType}.png`, // 实际项目中应从后端获取
            uuid: '1234567890' // 实际项目中应从后端获取
          }
        }
        
        this.authQrcodeUrl = response.data.qrcodeUrl
        this.authModalVisible = true
        
        // 开始轮询检查扫码状态
        this.startAuthPolling(response.data.uuid)
      } catch (error) {
        console.error(`${this.authPlatformName}登录出错:`, error)
        uni.showToast({ 
          title: `获取${this.authPlatformName}登录二维码失败`, 
          icon: 'none' 
        })
      } finally {
        this.loading = false
        uni.hideLoading()
      }
    },
    startAuthPolling(uuid) {
      // 每3秒轮询一次扫码状态
      this.authPollingTimer = setInterval(async () => {
        try {
          // 调用后端接口检查扫码状态
          // 接口地址: GET /api/auth/${this.authType}/status
          // 参数: { uuid: uuid }
          
          // 模拟不同状态
          const statusList = ['pending', 'scanned', 'confirmed']
          const randomStatus = statusList[Math.floor(Math.random() * statusList.length)]
          
          const response = {
            data: {
              status: randomStatus,
              token: 'fake-auth-token-' + Date.now()
            }
          }
          
          const { status, token } = response.data
          
          switch(status) {
            case 'pending': // 等待扫码
              break
            case 'scanned': // 已扫码，等待确认
              uni.showToast({ title: '已扫码，请在手机端确认', icon: 'none' })
              break
            case 'confirmed': // 已确认，登录成功
              clearInterval(this.authPollingTimer)
              this.authPollingTimer = null
              this.saveAuthToken(token)
              this.authModalVisible = false
              uni.showToast({ title: `${this.authPlatformName}登录成功`, icon: 'success' })
              uni.navigateTo({ url: '/pages/role/role' })
              break
            case 'expired': // 二维码过期
              clearInterval(this.authPollingTimer)
              this.authPollingTimer = null
              uni.showToast({ title: '二维码已过期，请重新获取', icon: 'none' })
              this.getAuthQrcode() // 重新获取二维码
              break
          }
        } catch (error) {
          console.error('轮询扫码状态出错:', error)
          // 忽略错误，继续轮询
        }
      }, 3000)
    },
    getAppId(type) {
      // 根据不同平台返回对应的AppID
      // 实际项目中应从配置文件或环境变量获取
      return type === 'wechat' ? 'wx1234567890' : 'qq1234567890'
    },
    getRedirectUri(type) {
      // 根据不同平台返回对应的回调URL
      // 实际项目中应从配置文件或环境变量获取
      return type === 'wechat' 
        ? 'https://your-domain.com/api/auth/wechat/callback' 
        : 'https://your-domain.com/api/auth/qq/callback'
    },
    saveAuthToken(token) {
      // 保存认证令牌到本地存储
      uni.setStorageSync('auth_token', token)
      // 可以添加其他状态管理逻辑，如Vuex/Pinia
    },
    closeAuthModal() {
      console.log('点击取消')
      console.log('当前轮询状态:', this.authPollingTimer)
      if (this.authPollingTimer) {
        clearInterval(this.authPollingTimer)
        this.authPollingTimer = null
      }
      this.authModalVisible = false
    },

    // 获取并保存推送设备ID
    async savePushClientId(userId) {
      try {
        console.log('📱 开始获取推送设备ID...')
        
        uni.getPushClientId({
          success: async ({ cid }) => {
            console.log('✅ 获取到 push_clientid =', cid)
            
            // 保存到后端
            try {
              const response = await request.request({
                url: '/api/user/updatePushClientId',
                method: 'POST',
                data: {
                  userId: userId,
                  pushClientId: cid
                }
              })
              
              if (response.success) {
                console.log('✅ 推送设备ID已保存到数据库')
              } else {
                console.error('❌ 保存推送设备ID失败:', response.message)
              }
            } catch (error) {
              console.error('❌ 保存推送设备ID异常:', error)
            }
          },
          fail: (err) => {
            console.error('❌ 获取 push_clientid 失败：', err)
          }
        })
      } catch (error) {
        console.error('❌ savePushClientId 异常:', error)
      }
    }
  },
  watch: {
    forgotPasswordVisible(val) {
      console.log('watch监听: 弹窗状态变化为', val)
    }
  },
  onLoad() {
    console.log('登录页面加载完成')
  },
  onUnload() {
    if (this.timer) clearInterval(this.timer)
    if (this.authPollingTimer) clearInterval(this.authPollingTimer)
  }
}
</script>

<style scoped>
.login-container {
  padding: 40rpx;
  background-color: #f8f9fa;
  height: 100vh;
  max-height: 100vh;
  position: relative;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  box-sizing: border-box;
}

.header {
  display: flex;
  flex-direction: column;
  align-items: center;
  margin-top: 100rpx;
  margin-bottom: 80rpx;
}

.logo {
  width: 150rpx;
  height: 150rpx;
  border-radius: 30rpx;
  background-color: #4a9ff5;
  margin-bottom: 30rpx;
}

.title {
  font-size: 40rpx;
  font-weight: bold;
  color: #333;
}

.form-container {
  margin-bottom: 60rpx;
}

.input {
  height: 100rpx;
  background-color: #fff;
  border-radius: 50rpx;
  padding: 0 40rpx;
  margin-bottom: 40rpx;
  font-size: 32rpx;
  box-shadow: 0 4rpx 12rpx rgba(0,0,0,0.05);
}

.btn {
  height: 100rpx;
  border-radius: 50rpx;
  font-size: 36rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 30rpx;
  transition: opacity 0.2s;
}

.btn:active {
  opacity: 0.8;
}

.login-btn {
  background-color: #4a9ff5;
  color: white;
}

.register-btn {
  background-color: #fff;
  color: #4a9ff5;
  border: 2rpx solid #4a9ff5;
}

.forgot-password {
  display: flex;
  justify-content: flex-end;
  padding-right: 20rpx;
  color: #999;
  font-size: 28rpx;
}

.forgot-password:active {
  opacity: 0.7;
}

.footer {
  position: absolute;
  bottom: 60rpx;
  left: 0;
  right: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.footer text {
  color: #999;
  font-size: 28rpx;
  margin-bottom: 30rpx;
}

.third-party {
  display: flex;
  gap: 80rpx;
}

.icon {
  width: 80rpx;
  height: 80rpx;
  border-radius: 50%;
  transition: opacity 0.2s;
}

.icon:active {
  opacity: 0.7;
}

.modal-mask {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background-color: rgba(0, 0, 0, 0.5);
  display: none;
  justify-content: center;
  align-items: center;
  z-index: 999;
  transition: opacity 0.3s;
}

.modal-mask.show {
  display: flex;
}

.modal-container {
  width: 600rpx;
  background-color: white;
  border-radius: 20rpx;
  overflow: hidden;
  z-index: 1000;
  transform: translateY(50px);
  opacity: 0;
  transition: transform 0.3s, opacity 0.3s;
}

.modal-container.show-modal {
  transform: translateY(0);
  opacity: 1;
}

.modal-header {
  position: relative;
  padding: 30rpx 40rpx;
  border-bottom: 1rpx solid #eee;
}

.modal-title {
  font-size: 36rpx;
  font-weight: bold;
  color: #333;
  text-align: center;
}

.close-btn {
  position: absolute;
  right: 40rpx;
  top: 30rpx;
  font-size: 40rpx;
  color: #999;
  transition: color 0.2s;
}

.close-btn:active {
  color: #333;
}

.modal-content {
  padding: 40rpx;
}

.modal-input {
  height: 90rpx;
  background-color: #f5f5f5;
  border-radius: 10rpx;
  padding: 0 30rpx;
  margin-bottom: 30rpx;
  font-size: 32rpx;
  width: 100%;
  box-sizing: border-box;
}

.verify-code-group {
  display: flex;
  gap: 20rpx;
  margin-bottom: 30rpx;
}

.verify-code-group .modal-input {
  flex: 1;
  margin-bottom: 0;
}

.get-code-btn {
  width: 200rpx;
  height: 90rpx;
  background-color: #4a9ff5;
  color: white;
  border-radius: 10rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 28rpx;
  white-space: nowrap;
  transition: opacity 0.2s;
}

.get-code-btn:active {
  opacity: 0.8;
}

.get-code-btn:disabled {
  background-color: #cccccc;
}

.password-tips {
  padding-left: 30rpx;
  margin-top: -10rpx;
  margin-bottom: 20rpx;
  height: 40rpx;
}

.error {
  color: #ff4500;
  font-size: 24rpx;
}

.modal-footer {
  display: flex;
  border-top: 1rpx solid #eee;
}

.modal-btn {
  flex: 1;
  height: 100rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 34rpx;
  transition: opacity 0.2s;
}

.modal-btn:active {
  opacity: 0.8;
}

.cancel-btn {
  color: #666;
}

.confirm-btn {
  color: #4a9ff5;
  font-weight: bold;
  border-left: 1rpx solid #eee;
}

.confirm-btn:disabled {
  color: #999;
  font-weight: normal;
}

.auth-modal {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background-color: rgba(0, 0, 0, 0.7);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 1000;
}

.auth-container {
  width: 70%;
  max-width: 600rpx;
  background-color: white;
  border-radius: 20rpx;
  overflow: hidden;
  padding: 40rpx;
  text-align: center;
}

.auth-title {
  font-size: 36rpx;
  font-weight: bold;
  color: #333;
  margin-bottom: 30rpx;
}

.auth-content {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.auth-icon {
  width: 80rpx;
  height: 80rpx;
  margin-bottom: 20rpx;
}

.auth-message {
  font-size: 28rpx;
  color: #666;
  margin-bottom: 30rpx;
}

.qrcode-container {
  width: 400rpx;
  height: 400rpx;
  background-color: #f5f5f5;
  border-radius: 10rpx;
  display: flex;
  justify-content: center;
  align-items: center;
  margin-bottom: 20rpx;
}

.qrcode {
  width: 360rpx;
  height: 360rpx;
}

.auth-tips {
  font-size: 24rpx;
  color: #999;
  margin-bottom: 40rpx;
}

.auth-cancel-btn {
  width: 100%;
  height: 80rpx;
  background-color: #f5f5f5;
  color: #666;
  border-radius: 10rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 32rpx;
}
</style>