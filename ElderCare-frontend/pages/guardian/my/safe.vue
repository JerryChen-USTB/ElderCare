<template>
  <view class="safe-container" :class="isDarkMode ? 'dark' : ''">

    <!-- 用户信息卡片 -->
    <view class="user-card" :class="isDarkMode ? 'dark-card' : ''">
      <view class="user-info">
        <text class="username">{{ userInfo.nickname || '未设置昵称' }}</text> 
        <text class="user-id">ID: {{ userInfo.userId }}</text> 
      </view>
      <view class="security-level">
        <text class="level-text">安全等级</text>
        <view class="level-bar">
          <view 
            class="level-progress" 
            :style="{width: securityLevel + '%'}"
            :class="getSecurityLevelClass"
          ></view>
        </view>
        <text class="level-value">{{ securityLevel }}%</text>
      </view>
    </view>

    <!-- 安全提示 -->
    <view class="security-tip" :class="isDarkMode ? 'dark-tip' : ''">
      <image class="tip-icon" src="/static/icons/info.png"></image>
      <text class="tip-text">为了您的账号安全，请及时绑定手机和邮箱并设置强密码</text>
    </view>

    <!-- 功能列表 -->
    <view class="function-list" :class="isDarkMode ? 'dark-list' : ''">
      <!-- 修改密码 -->
      <view class="function-item" @click="showChangePasswordPopup">
        <view class="function-left">
          <image class="function-icon" src="/static/icons/lock.png"></image>
          <text class="function-label">修改密码</text>
        </view>
        <image class="arrow-icon" src="/static/arrow_right.png"></image>
      </view>
      
      <!-- 绑定手机 -->
      <view class="function-item" @click="showBindPhonePopup">
        <view class="function-left">
          <image class="function-icon" src="/static/icons/phone.png"></image>
          <text class="function-label">绑定手机</text>
          <text class="phone-number" v-if="userInfo.phone">
            {{ formatPhone(userInfo.phone) }} 
          </text>
          <text class="unbind" v-else>未绑定</text>
        </view>
        <image class="arrow-icon" src="/static/arrow_right.png"></image>
      </view>
      
      <!-- 绑定邮箱 -->
      <view class="function-item" @click="showBindEmailPopup">
        <view class="function-left">
          <image class="function-icon" src="/static/icons/email.png"></image>
          <text class="function-label">绑定邮箱</text>
          <text class="email" v-if="userInfo.email">{{ userInfo.email }}</text>
          <text class="unbind" v-else>未绑定</text>
        </view>
        <image class="arrow-icon" src="/static/arrow_right.png"></image>
      </view>
      
      <!-- 登录设备管理 -->
      <view class="function-item" @click="showDevicesPopup">
        <view class="function-left">
          <image class="function-icon" src="/static/icons/devices.png"></image>
          <text class="function-label">登录设备管理</text>
          <text class="device-count">{{ loginDevicesCount }}台设备</text>
        </view>
        <image class="arrow-icon" src="/static/arrow_right.png"></image>
      </view>
      
      <!-- 账号注销 -->
      <view class="function-item danger-item" @click="showCancelAccountDialog">
        <view class="function-left">
          <image class="function-icon" src="/static/icons/warning.png"></image>
          <text class="danger-label">账号注销</text>
        </view>
        <image class="arrow-icon" src="/static/arrow_right.png"></image>
      </view>
    </view>

    <!-- 修改密码弹窗 -->
    <uni-popup ref="changePasswordPopup" type="dialog">
      <view class="popup-content">
        <text class="popup-title">修改密码</text>
        <text class="popup-subtitle">当前绑定手机号: {{ formatPhone(userInfo.phone) }}</text>
        <view class="input-group">
          <input v-model="newPassword" placeholder="请输入新密码" />
        </view>
        <view class="input-group">
          <input v-model="verificationCode" placeholder="请输入验证码" />
          <button @click="getVerificationCode('changePassword')">获取验证码</button>
        </view>
        <button @click="handleChangePassword">确认修改</button>
        <button @click="closeChangePasswordPopup">取消</button>
      </view>
    </uni-popup>

    <!-- 绑定手机弹窗 -->
    <uni-popup ref="bindPhonePopup" type="dialog">
      <view class="popup-content">
        <text class="popup-title">绑定手机</text>
        <text class="popup-subtitle" v-if="userInfo.phone">当前绑定手机号: {{ formatPhone(userInfo.phone) }}</text>
        <view class="input-group">
          <input v-model="newPhoneNumber" :placeholder="userInfo.phone ? '请输入新手机号' : '请输入手机号'" />
        </view>
        <view v-if="userInfo.phone">
          <radio-group @change="onVerificationMethodChange">
            <label><radio value="code" :checked="verificationMethod === 'code'" /> 通过原手机号验证码验证</label>
            <label><radio value="password" :checked="verificationMethod === 'password'" /> 通过当前账号密码验证</label>
          </radio-group>
          <view class="input-group" v-if="verificationMethod === 'code'">
            <input v-model="bindPhoneVerificationCode" placeholder="请输入验证码" />
            <button @click="getVerificationCode('bindPhone')">获取验证码</button>
          </view>
          <view class="input-group" v-if="verificationMethod === 'password'">
            <input v-model="currentPassword" placeholder="请输入当前账号密码" />
          </view>
        </view>
        <button @click="handleBindPhone">确认绑定</button>
        <button @click="closeBindPhonePopup">取消</button>
      </view>
    </uni-popup>

    <!-- 绑定邮箱弹窗 -->
    <uni-popup ref="bindEmailPopup" type="dialog">
      <uni-popup-dialog 
        mode="input"
        title="绑定邮箱"
        :placeholder="userInfo.email ? '请输入新邮箱' : '请输入邮箱'"
        :value="userInfo.email || ''"
        :type="isDarkMode ? 'dark' : 'default'"
        @confirm="handleBindEmail"
        @close="closeBindEmailPopup"
        :before-close="true"
      ></uni-popup-dialog>
    </uni-popup>

    <!-- 登录设备管理弹窗 -->
    <uni-popup ref="devicesPopup" type="bottom">
      <view class="devices-popup-content" :class="isDarkMode ? 'dark-popup' : ''">
        <view class="popup-header">
          <text class="popup-title">登录设备管理</text>
          <text class="popup-close" @click="closeDevicesPopup">关闭</text>
        </view>
        <scroll-view class="devices-list" scroll-y>
          <view 
            class="device-item" 
            v-for="(device, index) in loginDevices" 
            :key="index"
            :class="isDarkMode ? 'dark-item' : ''"
          >
            <view class="device-info">
              <image class="device-icon" src="/static/icons/device.png"></image>
              <view class="device-details">
                <text class="device-name">{{ device.deviceName }}</text>
                <text class="device-location">{{ device.location }} · {{ device.lastLoginTime }}</text>
              </view>
            </view>
            <view class="device-actions">
              <text 
                class="device-action" 
                @click="logoutDevice(device.deviceId)"
                :class="isDarkMode ? 'dark-action' : ''"
              >退出登录</text>
              <text 
                class="device-action danger" 
                @click="removeDevice(device.deviceId)"
                :class="isDarkMode ? 'dark-action' : ''"
              >移除设备</text>
            </view>
          </view>
        </scroll-view>
      </view>
    </uni-popup>

    <!-- 注销账号确认弹窗 -->
    <uni-popup ref="cancelAccountPopup" type="dialog">
      <uni-popup-dialog 
        mode="base"
        title="确认注销账号"
        content="注销后所有数据将被删除且无法恢复，确定继续吗？"
        :type="isDarkMode ? 'dark' : 'default'"
        @confirm="handleCancelAccount"
        @close="closeCancelAccountDialog"
        :before-close="true"
      ></uni-popup-dialog>
    </uni-popup>

    <!-- 验证身份弹窗 -->
    <uni-popup ref="verifyIdentityPopup" type="dialog">
      <uni-popup-dialog 
        mode="input"
        title="验证身份"
        placeholder="请输入当前密码"
        :type="isDarkMode ? 'dark' : 'default'"
        @confirm="verifyIdentity"
        @close="closeVerifyIdentityPopup"
        :before-close="true"
      ></uni-popup-dialog>
    </uni-popup>
    
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
import request from '@/utils/request.js'
import EmergencyModal from '@/components/emergency-modal.vue'
import EmergencyModalMixin from '@/mixins/emergency-modal.js'

export default {
  components: { EmergencyModal },
  mixins: [EmergencyModalMixin],
  data() {
    return {
      userInfo: {
        userId: '123456',
        nickname: '用户昵称',
        phone: '13800138000',
        email: ''
      },
      isDarkMode: false,
      loginDevices: [
        {
          deviceId: '1',
          deviceName: 'iPhone 12',
          location: '上海',
          lastLoginTime: '今天 10:30'
        },
        {
          deviceId: '2',
          deviceName: '华为 P40',
          location: '北京',
          lastLoginTime: '昨天 18:45'
        }
      ],
      securityLevel: 60, // 安全等级百分比
      currentAction: '', // 当前操作类型
      newPassword: '',
      verificationCode: '',
      newPhoneNumber: '',
      verificationMethod: 'code',
      bindPhoneVerificationCode: '',
      currentPassword: ''
    }
  },
  computed: {
    // 计算安全等级样式类
    getSecurityLevelClass() {
      if (this.securityLevel < 40) return 'level-low';
      if (this.securityLevel < 70) return 'level-medium';
      return 'level-high';
    },
    // 登录设备数量
    loginDevicesCount() {
      return this.loginDevices.length;
    }
  },
  onShow() {
    this.loadUserInfo();
    this.loadDarkModeSetting();
  },
  methods: {
    // 格式化手机号显示
    formatPhone(phone) {
      return phone ? phone.replace(/(\d{3})\d{4}(\d{4})/, '$1****$2') : ''
    },
    
    // 处理验证方式选择变化
    onVerificationMethodChange(e) {
      this.verificationMethod = e.detail.value;
    },
    
    // 加载用户信息
    async loadUserInfo() {
      try {
        // 从本地存储获取用户信息
        const storedUser = uni.getStorageSync('userInfo');
        if (storedUser) {
          this.userInfo.userId = storedUser.id || '未知';
          this.userInfo.phone = storedUser.phone || '';
          
          // 获取监护人详细信息
          const guardianRes = await request.get(`/api/guardian/info/${storedUser.id}`);
          
          if (guardianRes && guardianRes.name) {
            this.userInfo.nickname = guardianRes.name || storedUser.phone || '用户';
          } else {
            this.userInfo.nickname = storedUser.phone || '用户';
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
        
        this.calculateSecurityLevel();
      } catch (error) {
        console.error('获取用户信息失败:', error);
        uni.showToast({
          title: '获取用户信息失败',
          icon: 'none'
        });
      }
    },
	
    
    // 计算安全等级
    calculateSecurityLevel() {
      let level = 0;
      
      // 有密码 +20
      level += 20;
      
      // 绑定手机 +30
      if (this.userInfo.phone) level += 30;
      
      // 绑定邮箱 +30
      if (this.userInfo.email) level += 30;
      
      // 登录设备数量少 +20
      if (this.loginDevicesCount <= 1) level += 20;
      
      this.securityLevel = level;
    },
    
    // 显示修改密码弹窗
    showChangePasswordPopup() {
      uni.showToast({
        title: '此功能暂未开放',
        icon: 'none'
      });
    },

    // 关闭修改密码弹窗
    closeChangePasswordPopup() {
      this.$refs.changePasswordPopup.close();
    },

    // 处理修改密码
    handleChangePassword() {
      uni.showToast({
        title: '此功能暂未开放',
        icon: 'none'
      });
    },
    
    // 显示绑定手机弹窗
    showBindPhonePopup() {
      uni.showToast({
        title: '此功能暂未开放',
        icon: 'none'
      });
    },

    // 关闭绑定手机弹窗
    closeBindPhonePopup() {
      this.$refs.bindPhonePopup.close();
    },

    // 处理绑定手机
    handleBindPhone() {
      uni.showToast({
        title: '此功能暂未开放',
        icon: 'none'
      });
    },

    // 显示绑定邮箱弹窗
    showBindEmailPopup() {
      uni.showToast({
        title: '此功能暂未开放',
        icon: 'none'
      });
    },

    // 关闭绑定邮箱弹窗
    closeBindEmailPopup() {
      this.$refs.bindEmailPopup.close();
    },

    // 处理绑定邮箱
    handleBindEmail(value) {
      uni.showToast({
        title: '此功能暂未开放',
        icon: 'none'
      });
    },

    // 显示登录设备管理弹窗
    showDevicesPopup() {
      uni.showToast({
        title: '此功能暂未开放',
        icon: 'none'
      });
    },

    // 关闭登录设备管理弹窗
    closeDevicesPopup() {
      this.$refs.devicesPopup.close();
    },

    // 退出设备登录
    logoutDevice(deviceId) {
      uni.showToast({
        title: '此功能暂未开放',
        icon: 'none'
      });
    },
    
    // 移除设备
    removeDevice(deviceId) {
      uni.showToast({
        title: '此功能暂未开放',
        icon: 'none'
      });
    },
    
    // 显示注销账号弹窗
    showCancelAccountDialog() {
      uni.showToast({
        title: '此功能暂未开放',
        icon: 'none'
      });
    },
    
    // 关闭注销账号弹窗
    closeCancelAccountDialog() {
      this.$refs.cancelAccountPopup.close();
    },
    
    // 处理账号注销
    handleCancelAccount() {
      uni.showToast({
        title: '此功能暂未开放',
        icon: 'none'
      });
    },
    
    // 验证身份
    verifyIdentity(value) {
      uni.showToast({
        title: '此功能暂未开放',
        icon: 'none'
      });
    },
    
    // 关闭验证身份弹窗
    closeVerifyIdentityPopup() {
      this.$refs.verifyIdentityPopup.close();
    },
    
    // 执行账号注销
    performCancelAccount() {
      uni.showToast({
        title: '此功能暂未开放',
        icon: 'none'
      });
    },
    
    // 加载深色模式设置
    loadDarkModeSetting() {
      const darkMode = uni.getStorageSync('darkMode');
      if (darkMode !== undefined) {
        this.isDarkMode = darkMode;
      }
    },

    // 获取验证码
    getVerificationCode(action) {
      uni.showToast({
        title: '此功能暂未开放',
        icon: 'none'
      });
    }
  }
}
</script>

<style scoped>
.safe-container {
  padding: 0;
  background-color: #f8f9fa;
  min-height: 100vh;
  transition: background-color 0.3s ease;
}

.safe-container.dark {
  background-color: #1e1e1e;
}

/* 用户卡片样式 */
.user-card {
  display: flex;
  align-items: center;
  padding: 30rpx;
  background-color: #fff;
  margin: 20rpx;
  border-radius: 16rpx;
  box-shadow: 0 4rpx 16rpx rgba(0,0,0,0.05);
  transition: background-color 0.3s ease;
}

.dark-card {
  background-color: #2c2c2c;
}

.user-info {
  display: flex;
  flex-direction: column;
  flex: 1;
}

.username {
  font-size: 36rpx;
  font-weight: bold;
  margin-bottom: 10rpx;
  color: #333;
}

.dark .username {
  color: #fff;
}

.user-id {
  font-size: 26rpx;
  color: #999;
}

.dark .user-id {
  color: #b0b0b0;
}

.security-level {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
}

.level-text {
  font-size: 24rpx;
  color: #999;
  margin-bottom: 10rpx;
}

.level-bar {
  width: 200rpx;
  height: 12rpx;
  background-color: #e0e0e0;
  border-radius: 6rpx;
  overflow: hidden;
  margin-bottom: 10rpx;
}

.level-progress {
  height: 100%;
  transition: width 0.5s ease;
}

.level-low {
  background-color: #ff4d4f;
}

.level-medium {
  background-color: #faad14;
}

.level-high {
  background-color: #52c41a;
}

.level-value {
  font-size: 24rpx;
  color: #999;
}

/* 安全提示样式 */
.security-tip {
  display: flex;
  align-items: center;
  padding: 20rpx 30rpx;
  background-color: #fff7e6;
  border-radius: 12rpx;
  margin: 20rpx;
  transition: background-color 0.3s ease;
}

.dark-tip {
  background-color: #3a3328;
}

.tip-icon {
  width: 32rpx;
  height: 32rpx;
  margin-right: 15rpx;
}

.tip-text {
  font-size: 26rpx;
  color: #faad14;
}

.dark .tip-text {
  color: #ffd591;
}

/* 功能列表样式 */
.function-list {
  background-color: #fff;
  margin: 20rpx;
  border-radius: 16rpx;
  box-shadow: 0 4rpx 16rpx rgba(0,0,0,0.05);
  overflow: hidden;
  transition: background-color 0.3s ease;
}

.dark-list {
  background-color: #2c2c2c;
}

.function-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 30rpx;
  border-bottom: 1rpx solid #f1f1f1;
  transition: background-color 0.2s ease;
}

.dark .function-item {
  border-bottom-color: #3a3a3a;
}

.function-item:active {
  background-color: #f5f5f5;
}

.dark .function-item:active {
  background-color: #3a3a3a;
}

.function-left {
  display: flex;
  align-items: center;
  flex: 1;
}

.function-icon {
  width: 40rpx;
  height: 40rpx;
  margin-right: 20rpx;
}

.function-label {
  font-size: 30rpx;
  color: #333;
  margin-right: 20rpx;
}

.dark .function-label {
  color: #fff;
}

.danger-label {
  font-size: 30rpx;
  color: #ff4d4f;
}

.phone-number, .email, .device-count {
  font-size: 26rpx;
  color: #999;
  flex: 1;
  text-align: right;
  padding-right: 20rpx;
}

.dark .phone-number, .dark .email, .dark .device-count {
  color: #b0b0b0;
}

.unbind {
  font-size: 26rpx;
  color: #ff4d4f;
  flex: 1;
  text-align: right;
  padding-right: 20rpx;
}

.arrow-icon {
  width: 30rpx;
  height: 30rpx;
}

.danger-item {
  border-bottom: none;
}

/* 设备管理弹窗样式 */
.devices-popup-content {
  background-color: #fff;
  border-radius: 24rpx 24rpx 0 0;
  padding: 30rpx;
  max-height: 70vh;
  transition: background-color 0.3s ease;
}

.dark-popup {
  background-color: #2c2c2c;
}

.popup-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 30rpx;
}

.popup-title {
  font-size: 32rpx;
  font-weight: bold;
  color: #333;
}

.dark-popup .popup-title {
  color: #fff;
}

.popup-close {
  font-size: 28rpx;
  color: #999;
  padding: 10rpx;
}

.dark-popup .popup-close {
  color: #b0b0b0;
}

.devices-list {
  max-height: 60vh;
}

.device-item {
  padding: 20rpx 0;
  border-bottom: 1rpx solid #f1f1f1;
}

.dark-item {
  border-bottom-color: #3a3a3a;
}

.device-info {
  display: flex;
  align-items: center;
  margin-bottom: 20rpx;
}

.device-icon {
  width: 48rpx;
  height: 48rpx;
  margin-right: 20rpx;
}

.device-details {
  display: flex;
  flex-direction: column;
  flex: 1;
}

.device-name {
  font-size: 28rpx;
  color: #333;
  margin-bottom: 8rpx;
}

.dark-item .device-name {
  color: #fff;
}

.device-location {
  font-size: 24rpx;
  color: #999;
}

.dark-item .device-location {
  color: #b0b0b0;
}

.device-actions {
  display: flex;
  justify-content: flex-end;
}

.device-action {
  font-size: 26rpx;
  color: #1890ff;
  padding: 10rpx 20rpx;
  margin-left: 20rpx;
  border-radius: 8rpx;
}

.dark-action {
  color: #69c0ff;
}

.device-action.danger {
  color: #ff4d4f;
}

.dark-action.danger {
  color: #ff7875;
}

/* 深色模式过渡动画 */
.dark-transition {
  transition: all 0.3s ease !important;
}

/* 弹窗内容样式 */
.popup-content {
  padding: 30rpx;
}

.popup-title {
  font-size: 32rpx;
  font-weight: bold;
  margin-bottom: 20rpx;
}

.popup-subtitle {
  font-size: 26rpx;
  color: #999;
  margin-bottom: 20rpx;
}

.input-group {
  display: flex;
  align-items: center;
  margin-bottom: 20rpx;
}

.input-group input {
  flex: 1;
  padding: 10rpx;
  border: 1rpx solid #e0e0e0;
  border-radius: 8rpx;
}

.input-group button {
  margin-left: 20rpx;
  padding: 10rpx 20rpx;
  background-color: #1890ff;
  color: #fff;
  border: none;
  border-radius: 8rpx;
}

.popup-content button {
  width: 100%;
  padding: 10rpx;
  background-color: #1890ff;
  color: #fff;
  border: none;
  border-radius: 8rpx;
  margin-bottom: 10rpx;
}
</style>