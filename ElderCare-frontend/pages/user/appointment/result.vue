<template>
  <view class="result-container">
    <!-- 固定头部区域 -->
    <view class="fixed-header">
      <!-- 状态栏占位 -->
      <view class="status-bar" :style="{height: statusBarHeight + 'px'}"></view>
      
      <!-- 自定义导航栏 -->
      <view class="custom-header">
        <image 
          src="/static/icons/back.png" 
          class="nav-icon back-icon" 
          @click="goBack"
          mode="aspectFit"
        />
        <view class="header-title">预约结果</view>
        <view class="header-right"></view>
      </view>
    </view>

    <!-- 内容占位，避免被固定头部遮挡 -->
    <view class="header-placeholder" :style="{height: (statusBarHeight + 44) + 'px'}"></view>

    <!-- 主体内容 -->
    <view class="content">
      <!-- 成功状态图标 -->
      <view class="success-section">
        <view class="success-icon">✓</view>
        <text class="success-title">预约提交成功</text>
        <text class="success-subtitle">您的预约申请已提交，请等待志愿者接单</text>
      </view>

      <!-- 预约信息卡片 -->
      <view class="info-card">
        <view class="card-title">
          <text>预约详情</text>
        </view>
        
        <!-- 预约类型 -->
        <view class="info-item">
          <view class="info-label">预约类型</view>
          <view class="info-value">{{getTypeName(appointmentData.appointmentType)}}</view>
        </view>

        <!-- 服务内容 -->
        <view class="info-item content-item">
        <view class="info-label">服务内容</view>
        <view class="info-value content-text">{{appointmentData.appointmentContent}}</view>
        </view>
        
        <!-- 预约时间 -->
        <view class="info-item">
          <view class="info-label">预约时间</view>
          <view class="info-value">{{formatDateTime(appointmentData.startTime)}}</view>
        </view>
        
        <!-- 结束时间（如果有） -->
        <view class="info-item" v-if="appointmentData.endTime">
          <view class="info-label">结束时间</view>
          <view class="info-value">{{formatDateTime(appointmentData.endTime)}}</view>
        </view>
        
        <!-- 服务地点（如果有） -->
        <view class="info-item" v-if="appointmentData.location">
          <view class="info-label">服务地点</view>
          <view class="info-value">{{appointmentData.location}}</view>
        </view>
        
        <!-- 预约ID -->
        <view class="info-item">
          <view class="info-label">预约编号</view>
          <view class="info-value appointment-id">#{{appointmentData.id}}</view>
        </view>
        
        <!-- 预约状态 -->
        <view class="info-item">
          <view class="info-label">预约状态</view>
          <view class="info-value status-pending">待接单</view>
        </view>
      </view>

      <!-- 温馨提示 -->
      <view class="tips-card">
        <view class="tips-title">
          <text>温馨提示</text>
        </view>
        <view class="tips-content">
          <text>• 志愿者将在24小时内响应您的预约申请</text>
          <text>• 如需修改或取消预约，请联系客服</text>
          <text>• 服务过程中如有疑问，可随时与志愿者沟通</text>
        </view>
      </view>

      <!-- 操作按钮 -->
      <view class="action-buttons">
        <button class="btn-secondary" @click="goBack">返回首页</button>
        <button class="btn-primary" @click="viewMyAppointments">我的预约</button>
      </view>
    </view>
  </view>
</template>

<script>
import { getCurrentUserId } from '@/utils/auth.js'

export default {
  data() {
    return {
      statusBarHeight: 0,
      appointmentData: {}, // 预约数据
      appointmentTypes: [
        { id: 'doctor', name: '医生问诊' },
        { id: 'nurse', name: '护理服务' },  
        { id: 'rehab', name: '康复指导' },
        { id: 'therapy', name: '心理治疗' },
        { id: 'other', name: '其他服务' }
      ]
    }
  },
  onLoad(options) {
    // 获取状态栏高度
    const systemInfo = uni.getSystemInfoSync();
    this.statusBarHeight = systemInfo.statusBarHeight || 0;
    
    // 获取传递的预约数据
    if (options.appointmentData) {
      try {
        this.appointmentData = JSON.parse(decodeURIComponent(options.appointmentData));
        console.log('接收到预约数据:', this.appointmentData);
      } catch (error) {
        console.error('解析预约数据失败:', error);
        uni.showToast({ title: '数据加载失败', icon: 'none' });
      }
    }
  },
  methods: {
    // 返回首页
    goBack() {
      uni.reLaunch({
        url: '/pages/user/index/index'
      });
    },
    
    // 查看我的预约，跳转到日程管理页面并筛选预约
    viewMyAppointments() {
      uni.navigateTo({
        url: '/pages/user/schedule/detail?type=appointment'
      });
    },
    
    // 获取预约类型名称
    getTypeName(typeId) {
      const type = this.appointmentTypes.find(t => t.id === typeId);
      return type ? type.name : typeId;
    },
    
    // 格式化日期时间
    formatDateTime(dateTimeStr) {
      if (!dateTimeStr) return '';
      
      try {
        const date = new Date(dateTimeStr);
        const year = date.getFullYear();
        const month = (date.getMonth() + 1).toString().padStart(2, '0');
        const day = date.getDate().toString().padStart(2, '0');
        const hours = date.getHours().toString().padStart(2, '0');
        const minutes = date.getMinutes().toString().padStart(2, '0');
        
        const weekdays = ['周日', '周一', '周二', '周三', '周四', '周五', '周六'];
        const weekday = weekdays[date.getDay()];
        
        return `${year}年${month}月${day}日 ${weekday} ${hours}:${minutes}`;
      } catch (error) {
        console.error('日期格式化失败:', error);
        return dateTimeStr;
      }
    }
  }
}
</script>

<style scoped>
.result-container {
  min-height: 100vh;
  background: linear-gradient(to bottom, #f8f9fa 0%, #e9ecef 100%);
}

/* 固定头部区域 */
.fixed-header {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  z-index: 1000;
  background-color: #fff;
}

/* 状态栏占位 */
.status-bar {
  width: 100%;
  background-color: #fff;
}

/* 自定义导航栏 */
.custom-header {
  height: 88rpx;
  display: flex;
  align-items: center;
  padding: 0 30rpx;
  background-color: #fff;
  border-bottom: 1rpx solid #eee;
}

/* 内容占位，避免被固定头部遮挡 */
.header-placeholder {
  width: 100%;
  flex-shrink: 0;
}

.header-title {
  position: absolute;
  left: 50%;
  transform: translateX(-50%);
  font-size: 32rpx;
  font-weight: bold;
  color: #333;
  text-align: center;
}

.header-right {
  width: 50rpx;
  margin-left: auto;
}

.nav-icon {
  width: 32rpx;
  height: 32rpx;
  transition: opacity 0.3s ease;
}

.nav-icon:active {
  opacity: 0.5;
  transform: scale(0.9);
}

/* 主体内容 */
.content {
  padding: 40rpx 30rpx;
}

/* 成功状态区域 */
.success-section {
  text-align: center;
  margin-bottom: 50rpx;
}

.success-icon {
  width: 120rpx;
  height: 120rpx;
  background: linear-gradient(135deg, #6ABF45, #5aa83a);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 30rpx;
  color: white;
  font-size: 60rpx;
  font-weight: bold;
  box-shadow: 0 8rpx 20rpx rgba(106, 191, 69, 0.3);
}

.success-title {
  display: block;
  font-size: 36rpx;
  font-weight: 600;
  color: #333;
  margin-bottom: 15rpx;
}

.success-subtitle {
  display: block;
  font-size: 28rpx;
  color: #666;
  line-height: 1.5;
}

/* 信息卡片 */
.info-card {
  background-color: #fff;
  border-radius: 20rpx;
  padding: 30rpx;
  margin-bottom: 30rpx;
  box-shadow: 0 4rpx 20rpx rgba(0, 0, 0, 0.08);
}

.card-title {
  font-size: 32rpx;
  font-weight: 600;
  color: #333;
  margin-bottom: 30rpx;
  padding-bottom: 20rpx;
  border-bottom: 2rpx solid #f0f0f0;
}

.info-item {
  display: flex;
  margin-bottom: 25rpx;
  align-items: flex-start;
}

.info-item:last-child {
  margin-bottom: 0;
}

.content-item {
  align-items: flex-start;
}

.info-label {
  width: 160rpx;
  font-size: 28rpx;
  color: #666;
  flex-shrink: 0;
}

.info-value {
  flex: 1;
  font-size: 28rpx;
  color: #333;
  font-weight: 500;
}

.content-text {
  line-height: 1.6;
  word-break: break-all;
}

.appointment-id {
  color: #6ABF45;
  font-weight: 600;
}

.status-pending {
  color: #ff9500;
  background-color: #fff7e6;
  padding: 8rpx 16rpx;
  border-radius: 16rpx;
  font-size: 24rpx;
}

/* 温馨提示卡片 */
.tips-card {
  background-color: #fff;
  border-radius: 20rpx;
  padding: 30rpx;
  margin-bottom: 40rpx;
  box-shadow: 0 4rpx 20rpx rgba(0, 0, 0, 0.08);
  border-left: 6rpx solid #6ABF45;
}

.tips-title {
  font-size: 30rpx;
  font-weight: 600;
  color: #333;
  margin-bottom: 20rpx;
}

.tips-content {
  display: flex;
  flex-direction: column;
  gap: 15rpx;
}

.tips-content text {
  font-size: 26rpx;
  color: #666;
  line-height: 1.5;
}

/* 操作按钮 */
.action-buttons {
  display: flex;
  gap: 20rpx;
  margin-top: 20rpx;
}

.btn-secondary {
  flex: 1;
  height: 90rpx;
  line-height: 90rpx;
  background-color: #f5f5f5;
  color: #333;
  border: 2rpx solid #ddd;
  border-radius: 45rpx;
  font-size: 30rpx;
  font-weight: 500;
}

.btn-primary {
  flex: 1;
  height: 90rpx;
  line-height: 90rpx;
  background: linear-gradient(135deg, #6ABF45, #5aa83a);
  color: white;
  border: none;
  border-radius: 45rpx;
  font-size: 30rpx;
  font-weight: 600;
  box-shadow: 0 6rpx 16rpx rgba(106, 191, 69, 0.3);
}

.btn-secondary:active {
  background-color: #e9e9e9;
}

.btn-primary:active {
  transform: translateY(2rpx);
  box-shadow: 0 4rpx 12rpx rgba(106, 191, 69, 0.4);
}

/* 响应式调整 */
@media (max-width: 750rpx) {
  .content {
    padding: 30rpx 20rpx;
  }
  
  .info-card, .tips-card {
    padding: 25rpx;
    margin-bottom: 25rpx;
  }
  
  .info-label {
    width: 140rpx;
    font-size: 26rpx;
  }
  
  .info-value {
    font-size: 26rpx;
  }
}
</style>
