<template>
  <view class="register-container">
    <view class="header">
      <image src="/static/eldercare_logo.jpg" class="logo" />
      <text class="title">欢迎注册银杏智伴</text>
    </view>
    
    <view class="form-container">
      <input class="input" placeholder="请输入手机号" v-model="phone" type="number" maxlength="11" />
      
      <!-- 验证码输入框 -->
      <view class="verify-code-group">
        <input class="input code-input" placeholder="请输入验证码" v-model="verifyCode" maxlength="6" type="number" />
        <button 
          class="get-code-btn" 
          :disabled="countdown > 0 || !isPhoneValid" 
          @click="getVerifyCode"
        >
          {{ countdown > 0 ? `${countdown}s` : '获取验证码' }}
        </button>
      </view>
      
      <input class="input" placeholder="请输入密码" v-model="password" password />
      <view class="password-tips" v-if="password && !isPasswordValid">
        <text class="error">密码需8-16位，包含数字和字母，符号只能使用._</text>
      </view>
      <input class="input" placeholder="请再次输入密码" v-model="confirmPassword" password />
      
      <view class="agreement">
        <checkbox-group @change="onAgreementChange">
          <label class="checkbox">
            <checkbox :checked="agreed" />
            <text>我已阅读并同意<a href="/pages/agreement/privacy">《隐私政策》</a>和<a href="/pages/agreement/terms">《用户协议》</a></text>
          </label>
        </checkbox-group>
      </view>
      
      <button class="btn register-btn" @click="handleRegister" :disabled="!isFormValid">注册</button>
      <button class="btn login-btn" @click="goToLogin">已有账号？返回登录</button>
    </view>
  </view>
</template>

<script>
import request from '@/utils/request.js'

export default {
  data() {
    return {
      phone: '',
      verifyCode: '',
      password: '',
      confirmPassword: '',
      agreed: false,
      countdown: 0,
      timer: null
    }
  },
  computed: {
    isPhoneValid() {
      return /^1[3-9]\d{9}$/.test(this.phone)
    },
    isPasswordValid() {
      // 密码正则：8-16位，必须包含数字和字母，可选符号._
      const pattern = /^(?=.*[A-Za-z])(?=.*\d)[A-Za-z\d._]{8,16}$/
      return pattern.test(this.password)
    },
    isFormValid() {
      return this.phone && this.verifyCode && this.password && this.confirmPassword && 
             this.password === this.confirmPassword && 
             this.isPasswordValid && this.agreed && this.isPhoneValid
    }
  },
  methods: {
    onAgreementChange(e) {
      this.agreed = e.detail.value.length > 0
    },
    
    // 获取验证码
    async getVerifyCode() {
      if (!this.isPhoneValid) {
        uni.showToast({
          title: '请输入正确的手机号',
          icon: 'none'
        })
        return
      }
      
      try {
        // 开始倒计时
        this.countdown = 60
        this.timer = setInterval(() => {
          this.countdown--
          if (this.countdown <= 0) {
            clearInterval(this.timer)
            this.timer = null
          }
        }, 1000)
        
        // 调用后端接口
        uni.showLoading({ title: '发送中...' })
        const res = await request.post('/api/user/register/send-code', { phone: this.phone })
        uni.hideLoading()
        
        if (res.code === 200 && res.data) {
          // 后端返回验证码，自动填充
          this.verifyCode = res.data
          
          // 顶部自动关闭提示
          uni.showToast({
            title: `【银杏智伴】您的验证码是：${res.data}，请尽快验证！`,
            icon: 'none',
            duration: 3000,
            position: 'top'
          })
        } else {
          // 发送失败，清除倒计时
          clearInterval(this.timer)
          this.countdown = 0
          uni.showToast({
            title: res.msg || '发送失败',
            icon: 'none'
          })
        }
      } catch (error) {
        clearInterval(this.timer)
        this.countdown = 0
        uni.hideLoading()
        console.error('获取验证码失败:', error)
        uni.showToast({
          title: '获取验证码失败',
          icon: 'none'
        })
      }
    },
    
    async handleRegister() {
      if (!this.isFormValid) {
        if (!this.agreed) {
          uni.showToast({
            title: '请同意隐私政策和用户协议',
            icon: 'none'
          })
          return
        }
        return
      }
      
      try {
        uni.showLoading({ title: '注册中...' })
        
        const userInfo = {
          phone: this.phone,
          password: this.password,
          verifyCode: this.verifyCode
        }
        
        const result = await request.userApi.register(userInfo)
        
        if (result.success) {
          // 注册成功后，自动登录
          console.log('✅ 注册成功，开始自动登录...')
          uni.showLoading({ title: '登录中...' })
          
          try {
            const loginResult = await request.userApi.login(this.phone, this.password)
            
            if (loginResult.success) {
              // 保存登录信息
              uni.setStorageSync('token', loginResult.token)
              uni.setStorageSync('userInfo', loginResult.user)
              
              uni.hideLoading()
              uni.showToast({ 
                title: '注册成功！', 
                icon: 'success',
                duration: 1500
              })
              
              // 延迟跳转到角色选择页面
              setTimeout(() => {
                uni.reLaunch({ url: '/pages/role/role' })
              }, 1500)
            } else {
              // 登录失败，跳转到登录页面
              uni.hideLoading()
              uni.showToast({
                title: '注册成功，请登录',
                icon: 'success'
              })
              setTimeout(() => {
                uni.navigateTo({ url: '/pages/login/login' })
              }, 1500)
            }
          } catch (loginError) {
            // 自动登录失败，跳转到登录页面
            console.error('自动登录失败:', loginError)
            uni.hideLoading()
            uni.showToast({
              title: '注册成功，请登录',
              icon: 'success'
            })
            setTimeout(() => {
              uni.navigateTo({ url: '/pages/login/login' })
            }, 1500)
          }
        } else {
          uni.hideLoading()
          uni.showToast({
            title: result.message,
            icon: 'none'
          })
        }
        
      } catch (error) {
        uni.hideLoading()
        console.error('注册失败:', error)
        uni.showToast({
          title: '注册失败，请检查网络连接',
          icon: 'none'
        })
      }
    },
    goToLogin() {
      uni.navigateTo({ url: '/pages/login/login' })
    }
  }
}
</script>

<style scoped>
/* 保持原有样式不变 */
.register-container {
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
  margin-bottom: 20rpx;
  font-size: 32rpx;
  box-shadow: 0 4rpx 12rpx rgba(0,0,0,0.05);
}

/* 验证码输入组 */
.verify-code-group {
  display: flex;
  gap: 20rpx;
  margin-bottom: 20rpx;
  align-items: center;
}

.code-input {
  flex: 1;
  margin-bottom: 0;
}

.get-code-btn {
  width: 200rpx;
  height: 100rpx;
  background: linear-gradient(to right, #4a9ff5, #5eb8ff);
  color: white;
  border-radius: 16rpx;
  font-size: 28rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  border: none;
  white-space: nowrap;
  padding: 0 20rpx;
  position: relative;
  overflow: hidden;
}

/* 移除默认边框，防止圆角缺失 */
.get-code-btn::after {
  border: none;
}

.get-code-btn[disabled] {
  background: #eeeeee;
  color: #999;
}

.get-code-btn[disabled]::after {
  border: none;
}

.password-tips {
  padding-left: 40rpx;
  margin-bottom: 20rpx;
  height: 40rpx;
}

.error {
  color: #ff4500;
  font-size: 24rpx;
}

.agreement {
  display: flex;
  align-items: center;
  padding: 0 10rpx;
  margin-top: 40rpx; /* 增加与上方的距离 */
  margin-bottom: 10rpx;
  font-size: 24rpx;
  color: #666;
}

.agreement checkbox {
  transform: scale(0.8);
  margin-right: 10rpx;
}

.agreement a {
  color: #4a9ff5;
}

.btn {
  height: 100rpx;
  border-radius: 50rpx;
  font-size: 36rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 30rpx;
  position: relative;
  box-sizing: border-box;
}

/* 移除微信小程序默认的伪元素边框（关键：解决圆角缺失） */
.btn::after {
  border: none !important;
}

.register-btn {
  background-color: #fff;
  color: #4a9ff5;
  border: 2rpx solid #e0e0e0;
}

.register-btn::after {
  border: none !important;
}

.register-btn:disabled {
  background-color: #f5f5f5;
  color: #999;
  border-color: #d0d0d0;
}

.register-btn:disabled::after {
  border: none !important;
}

.login-btn {
  background-color: #fff;
  color: #4a9ff5;
  border: 2rpx solid #4a9ff5;
}

.login-btn::after {
  border: none !important;
}
</style>