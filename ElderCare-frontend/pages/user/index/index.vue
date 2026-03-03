<template>
  <view class="user-index">
    <view class="header">
      <text class="main-greeting">{{ timeGreeting }}{{ userName }}</text>
      <text class="sub-greeting">{{ randomGreeting }}</text>
    </view>

    <view class="content">
      <swiper class="swiper" indicator-dots autoplay circular>
        <swiper-item>
          <image src="/static/elder/eldercare_pic.png" mode="aspectFill"></image>
        </swiper-item>
        <swiper-item>
          <image src="/static/elder/AI_chat_pic.png" mode="aspectFill"></image>
        </swiper-item>
        <swiper-item>
          <image src="/static/elder/remote_assistance_pic.png" mode="aspectFill"></image>
        </swiper-item>
      </swiper>

      <view class="function-grid">
        <view class="grid-item" @click="navigateTo('chat')">
          <image src="/static/elder/AI_chat.png" class="grid-icon" mode="aspectFit"></image>
          <text class="grid-text">智能聊天</text>
        </view>
        <view class="grid-item" @click="navigateTo('health')">
          <image src="/static/elder/health.png" class="grid-icon" mode="aspectFit"></image>
          <text class="grid-text">健康监测</text>
        </view>
        <view class="grid-item" @click="navigateTo('medication-reminder')">
          <image src="/static/elder/medication.png" class="grid-icon" mode="aspectFit"></image>
          <text class="grid-text">用药提醒</text>
        </view>
        <view class="grid-item" @click="navigateTo('schedule')">
          <image src="/static/elder/schedule.png" class="grid-icon" mode="aspectFit"></image>
          <text class="grid-text">我的日程</text>
        </view>
      </view>
	  

      <view class="appointment-card" @click="navigateTo('appointment')">
        <text>预约志愿者服务</text>
      </view>

      <view class="button-container">
        <view class="assist-button" @click="navigateTo('remote-assist')">
          <text class="button-assist-text">远程协助</text>
          <image src="/static/elder/remote_assistance.png" class="button-icon-space" mode="aspectFit"></image>
        </view>
        <view class="emergency-button-new" @click="navigateTo('emergency')">
          <text class="button-emergency-text">紧急求助</text>
          <image src="/static/elder/emergency.png" class="button-icon-space" mode="aspectFit"></image>
        </view>
      </view>
	  
	  <!-- 添加的空白行 -->
	  <!-- <view style="height: 30rpx;"></view> -->

      <view class="card">
        <text class="card-title">近期安排</text>
        <view v-if="upcomingSchedules.length === 0" class="empty-schedule">
          <text class="empty-text">暂无近期安排</text>
        </view>
        <view v-for="schedule in upcomingSchedules" :key="schedule.id" class="activity" @click="navigateToSchedule(schedule.id)">
          <view class="activity-content">
            <text class="activity-title">{{ truncateText(schedule.content, 15) }}</text>
            <text class="activity-time">{{ formatScheduleTime(schedule.time) }}</text>
          </view>
          <view class="activity-type" :class="getScheduleTypeClass(schedule.type)">
            <text class="type-text">{{ getScheduleTypeName(schedule.type) }}</text>
          </view>
        </view>
      </view>
    </view>

    <custom-tabbar :current="0" :role="'user'" />
  </view>
</template>

<script>
import CustomTabbar from '@/components/custom-tabbar.vue'
import request from '@/utils/request.js'

export default {
  components: {
    CustomTabbar
  },
  data() {
    return {
	  locationTimer: null, // 定时上传位置的计时器（控制定时任务）
	  isUploading: false,   // 防止位置重复上传的锁（避免并发请求）
      userName: '亲爱的朋友',
      randomGreeting: '',
      upcomingSchedules: [] // 近期日程数据
    }
  },
  computed: {
    timeGreeting() {
      const hour = new Date().getHours()
      if (hour >= 5 && hour < 12) {
        return '上午好，'
      } else if (hour >= 12 && hour < 18) {
        return '下午好，'
      } else if (hour >= 18 && hour < 23) {
        return '晚上好，'
      } else {
        return '夜深了，'
      }
    }
  },
  mounted() {
    this.setRandomGreeting()
    this.getUserName()
    this.loadUpcomingSchedules()
    this.disablePageBounce()
  },
  onShow() {
    // 页面显示时重新获取用户名和日程（从其他页面返回时可能数据已更新）
    this.getUserName()
    this.loadUpcomingSchedules()
  },
   onLoad() {
      this.startLocationTimer(); // 页面加载时启动定时上传
    },
    
    // 2. 把onUnload移到根级别
    onUnload() {
      if (this.locationTimer) {
        clearInterval(this.locationTimer);
        this.locationTimer = null; // 页面销毁时清除定时器
      }
    },
  methods: {
	  startLocationTimer() {
	        // 第一步：页面加载后，立即上传一次位置（确保初始位置同步）
	        this.getAndUploadLocation();
	        // 第二步：设置定时器，每5分钟上传一次（可按需调整时间间隔）
	        this.locationTimer = setInterval(() => {
	          this.getAndUploadLocation();
	        }, 5 * 60 * 1000); // 时间间隔：5分钟 = 5*60*1000 毫秒
	      },
	  // 1. 核心：获取当前位置并上传到后端
	  getAndUploadLocation() {
	    // 防止重复请求（若上一次上传未完成，不触发新请求）
	    if (this.isUploading) return;
	    this.isUploading = true;
	  
	    // 1.1 使用uniapp的getLocation API获取设备经纬度（GPS坐标）
	    uni.getLocation({
	      type: 'wgs84', // 标准GPS坐标（与地图组件兼容，避免坐标系偏差）
	      success: (res) => {
	        // 提取经纬度
	        const { latitude, longitude } = res;
	        // 从本地存储获取当前用户信息（老年人自身的userId）
	        const userInfo = uni.getStorageSync('userInfo');
	        const elderUserId = userInfo?.id; 
	  
	        // 校验用户信息：若未获取到userId，提示异常并终止
	        if (!elderUserId) {
	          uni.showToast({ title: '用户信息异常，无法上传位置', icon: 'none' });
	          this.isUploading = false;
	          return;
	        }
	  
	        // 1.2 调用后端接口，将位置信息上传
	        const token = uni.getStorageSync('token'); // 登录令牌（用于接口鉴权）
	        const baseUrl = request.BASE_URL; // 使用config.js中的环境配置
	        uni.request({
	          url: `${baseUrl}/api/elderly/location/upload`, // 后端位置上传接口
	          method: 'POST',
	          header: {
	            'Authorization': `Bearer ${token}`, // 鉴权头（JWT令牌格式）
	            'Content-Type': 'application/x-www-form-urlencoded' // 表单格式（适配后端接收方式）
	          },
	          data: {
	            userId: elderUserId, // 老年人用户ID（标识位置所属用户）
	            latitude: latitude,  // 纬度
	            longitude: longitude // 经度
	          },
	          // 接口请求成功处理
	          success: (res) => {
	            // 按后端返回的code码区分结果
	            if (res.data.code === 200) {
	              console.log('位置上传成功：', latitude, longitude); // 成功日志
	            } else if (res.data.code === 400) {
	              // 业务错误（如参数无效）
	              uni.showToast({ title: res.data.msg || '位置上传失败', icon: 'none' });
	            } else if (res.data.code === 500) {
	              // 服务器错误
	              uni.showToast({ title: '服务器错误，位置上传失败', icon: 'none' });
	            }
	          },
	          // 接口请求失败处理（网络问题）
	          fail: (err) => {
	            console.error('位置上传请求失败：', err); // 错误日志
	            uni.showToast({ title: '网络错误，位置上传失败', icon: 'none' });
	          },
	          // 请求完成（无论成功/失败）：释放上传锁
	          complete: () => {
	            this.isUploading = false;
	          }
	        });
	      },
	      // 获取位置失败处理（如用户拒绝授权）
	      fail: (err) => {
	        console.error('获取位置失败：', err);
	        // 常见场景：用户拒绝位置权限，引导开启
	        if (err.errMsg.includes('auth deny')) {
	          uni.showModal({
	            title: '需要位置权限',
	            content: '为了让监护人查看您的位置，需开启位置权限',
	            confirmText: '去开启',
	            success: () => {
	              // 跳转至系统权限设置页（让用户手动开启位置权限）
	              uni.openSetting({
	                success: (res) => {
	                  // 若用户开启了位置权限，重新尝试获取位置
	                  if (res.authSetting['scope.userLocation']) {
	                    this.getAndUploadLocation();
	                  }
	                }
	              });
	            }
	          });
	        }
	        // 释放上传锁
	        this.isUploading = false;
	      }
	    });
	  },
    setRandomGreeting() {
      const greetings = [
        '现在心情怎么样？',
        '小乐准备好与您聊天了',
        '志愿者准备好为您服务了'
      ]
      const randomIndex = Math.floor(Math.random() * greetings.length)
      this.randomGreeting = greetings[randomIndex]
    },
    
    // 截取文本内容，超出指定长度用"..."代替
    truncateText(text, maxLength = 12) {
      if (!text) return '';
      if (text.length <= maxLength) {
        return text;
      }
      return text.substring(0, maxLength) + '...';
    },
    async getUserName() {
      try {
        // 从本地存储获取用户信息
        const storedUser = uni.getStorageSync('userInfo');
        if (storedUser && storedUser.id) {
          // 获取老人详细信息
          const elderRes = await request.elderApi.getElderInfo(storedUser.id);
          
          if (elderRes.success && elderRes.elder && elderRes.elder.name) {
            this.userName = elderRes.elder.name;
          } else {
            // 如果没有设置姓名，使用手机号的前三位+后四位作为默认显示
            this.userName = storedUser.phone ? this.formatPhoneForDisplay(storedUser.phone) : '亲爱的朋友';
          }
        } else {
          this.userName = '亲爱的朋友';
        }
      } catch (error) {
        console.error('获取用户名失败:', error);
        this.userName = '亲爱的朋友';
      }
    },
    formatPhoneForDisplay(phone) {
      if (!phone || phone.length < 7) return '用户';
      // 显示前三位+后四位，中间用***代替
      return phone.substring(0, 3) + '***' + phone.substring(phone.length - 4);
    },
    navigateTo(page) {
      // 根据不同页面设置正确的跳转路径
	  // 1. 添加：获取用户ID
	      const userInfo = uni.getStorageSync('userInfo');
	      const userId = userInfo?.id;
      let url = '';
      switch(page) {
        case 'chat':
          url = '/pages/user/chat/chat';
          break;
        case 'health':
          url = '/pages/user/health/health';
          break;
        case 'schedule':
          url = '/pages/user/schedule/detail'; // 我的日程跳转到detail页面
          break;
		case 'medication-reminder':
		    // 2. 修改：拼接userId参数
		    url = `/pages/user/medication/medication?userId=${userId}`; 
		    break;
        case 'appointment':
          url = '/pages/user/appointment/appointment';
          break;
        case 'emergency':
          url = '/pages/user/emergency/emergency';
          break;
        case 'remote-assist':
          url = '/pages/user/assistance/assistance';
          break;
        case 'tts-test':
          url = '/pages/test/tts-test';
          break;
        default:
          url = '/pages/user/index/index';
      }
      
      uni.navigateTo({
        url: url
      })
    },
    
    // 获取近期日程数据
    async loadUpcomingSchedules() {
      try {
        // 从本地存储获取用户信息
        const storedUser = uni.getStorageSync('userInfo');
        if (!storedUser || !storedUser.id) {
          console.log('用户未登录，无法获取日程数据');
          return;
        }
        
        const response = await request.request({
          url: `/api/schedule/upcoming?userId=${storedUser.id}&limit=5`,
          method: 'GET'
        });
        
        if (response.success && response.data) {
          this.upcomingSchedules = response.data;
        } else {
          console.log('获取近期日程失败:', response.message);
          this.upcomingSchedules = [];
        }
      } catch (error) {
        console.error('获取近期日程失败:', error);
        this.upcomingSchedules = [];
      }
    },
    
    // 格式化日程时间显示
    formatScheduleTime(timeStr) {
      if (!timeStr) return '';
      
      const scheduleTime = new Date(timeStr);
      const now = new Date();
      const today = new Date(now.getFullYear(), now.getMonth(), now.getDate());
      const tomorrow = new Date(today.getTime() + 24 * 60 * 60 * 1000);
      const scheduleDate = new Date(scheduleTime.getFullYear(), scheduleTime.getMonth(), scheduleTime.getDate());
      
      const timeString = scheduleTime.toTimeString().substring(0, 5); // HH:MM格式
      
      if (scheduleDate.getTime() === today.getTime()) {
        return `今天 ${timeString}`;
      } else if (scheduleDate.getTime() === tomorrow.getTime()) {
        return `明天 ${timeString}`;
      } else {
        const month = scheduleTime.getMonth() + 1;
        const day = scheduleTime.getDate();
        return `${month}月${day}日 ${timeString}`;
      }
    },
    
    // 获取日程类型名称
    getScheduleTypeName(type) {
      const typeNames = {
        'medicine': '服药',
        'doctor': '就医',
        'exercise': '运动',
        'meal': '饮食',
        'sleep': '睡觉',
        'appointment': '预约',
        'other': '其他'
      };
      return typeNames[type] || '其他';
    },
    
    // 获取日程类型样式类
    getScheduleTypeClass(type) {
      return `type-${type}`;
    },
    
    // 点击日程跳转到日程管理页面
    navigateToSchedule(scheduleId) {
      uni.navigateTo({
        url: `/pages/user/schedule/detail?id=${scheduleId}`
      });
    },
    
    // 禁用页面弹性滚动
    disablePageBounce() {
      // #ifdef APP-PLUS
      try {
        // 获取当前页面的webview
        const currentWebview = this.$scope.$getAppWebview();
        if (currentWebview) {
          // 设置页面样式，禁用弹性效果
          currentWebview.setStyle({
            bounce: 'none',          // 禁用页面弹性
            bounceBackground: '#F8F4F4', // 设置弹性背景色（以防万一）
            scrollIndicator: 'none'  // 隐藏滚动指示器
          });
        }
      } catch (error) {
        console.log('设置APP页面属性失败:', error);
      }
      // #endif
      
      // #ifdef H5
      try {
        // H5端设置
        const style = document.createElement('style');
        style.textContent = `
          * {
            overscroll-behavior: none !important;
            overscroll-behavior-y: none !important;
            -webkit-overflow-scrolling: auto !important;
          }
          body, html {
            overscroll-behavior: none !important;
            overscroll-behavior-y: none !important;
            -webkit-overflow-scrolling: auto !important;
          }
        `;
        document.head.appendChild(style);
      } catch (error) {
        console.log('设置H5页面属性失败:', error);
      }
      // #endif
      
      // #ifdef MP
      try {
        // 小程序端处理
        wx.pageScrollTo({
          scrollTop: 0,
          duration: 0
        });
      } catch (error) {
        console.log('设置小程序页面属性失败:', error);
      }
      // #endif
    }
  }
}
</script>

<style>
/* 全局禁用弹性滚动 */
page {
  overscroll-behavior: none !important;
  overscroll-behavior-y: none !important;
  -webkit-overflow-scrolling: auto !important;
}

/* 针对H5端的body元素 */
/* #ifdef H5 */
body, html {
  overscroll-behavior: none !important;
  overscroll-behavior-y: none !important;
  -webkit-overflow-scrolling: auto !important;
}
/* #endif */
</style>

<style scoped>
/* 样式保持不变 */
.user-index {
  padding-bottom: 120rpx;
  /* min-height: 100vh; */
  background-color: #F8F4F4;
  /* 禁用弹性滚动 */
  overscroll-behavior: none;
  overscroll-behavior-y: none;
  /* 阻止iOS橡皮筋效果 */
  -webkit-overflow-scrolling: auto;
}

.header {
  padding: 100rpx 30rpx 140rpx;
  background: linear-gradient(to bottom, #D5E3C6 0%, #D5E3C6 50%, #F8F4F4 100%);
  color: #4B3425;
  border-radius: 0 0 30rpx 30rpx;
  position: relative;
  z-index: 1;
}

.main-greeting {
  font-size: 52rpx;
  font-weight: bold;
  display: block;
  margin-bottom: 18rpx;
  color: #4B3425;
}

.sub-greeting {
  font-size: 32rpx;
  display: block;
  color: #4B3425;
  opacity: 0.8;
  font-weight: 600;
}

.info-text {
  font-size: 32rpx;
  opacity: 0.9;
}

.content {
  padding: 30rpx;
  padding-bottom: 100rpx; /* 增加底部内边距，让白色卡片区域延伸到底部 */
  background-color: #F8F4F4; /* 与页面背景色一致 */
}

.card {
  background-color: #fff;
  border-radius: 32rpx;
  padding: 30rpx;
  margin-bottom: 30rpx;
  box-shadow: 0 4rpx 12rpx rgba(0,0,0,0.05);
}

/* 最后一个卡片不需要下边距 */
.card:last-child {
  margin-bottom: 0;
}

.card-title {
  font-size: 36rpx;
  font-weight: bold;
  color: #4B3425;
  margin-bottom: 30rpx;
  display: block;
}

.services {
  display: flex;
  justify-content: space-between;
}

.service-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  width: 23%;
}

.service-icon {
  width: 80rpx;
  height: 80rpx;
  margin-bottom: 15rpx;
}

.service-item text {
  font-size: 26rpx;
  color: #4B3425;
  text-align: center;
}

.activity {
  padding: 20rpx 0;
  border-bottom: 1rpx solid #f1f1f1;
  display: flex;
  justify-content: space-between;
  align-items: center;
  cursor: pointer;
  transition: background-color 0.2s ease;
}

.activity:last-child {
  border-bottom: none;
}

.activity:hover {
  background-color: #f8f9fa;
}

.activity:active {
  background-color: #e9ecef;
}

.activity-content {
  flex: 1;
}

.activity-title {
  font-size: 32rpx;
  color: #4B3425;
  display: block;
  margin-bottom: 10rpx;
  font-weight: 600;
}

.activity-time {
  font-size: 26rpx;
  color: #634531;
  font-weight: 400;
}

.activity-type {
  padding: 8rpx 16rpx;
  border-radius: 12rpx;
  margin-left: 16rpx;
}

.type-text {
  font-size: 24rpx;
  font-weight: 500;
  color: white;
}

/* 日程类型颜色 */
.type-medicine {
  background-color: #FF6B6B;
}

.type-doctor {
  background-color: #4ECDC4;
}

.type-exercise {
  background-color: #FFD166;
}

.type-meal {
  background-color: #FF9F43;
}

.type-sleep {
  background-color: #A55EEA;
}

.type-appointment {
  background-color: #007AFF;
}

.type-other {
  background-color: #778CA3;
}

.empty-schedule {
  padding: 40rpx 0;
  text-align: center;
}

.empty-text {
  font-size: 28rpx;
  color: #999;
}

.swiper {
  height: 350rpx;
  width: 95%;
  margin: -140rpx auto 30rpx auto;
  position: relative;
  z-index: 2;
  background-color: transparent;
  border-radius: 30rpx;
  overflow: hidden;
}

.swiper-item {
  background-color: transparent;
  border-radius: 30rpx;
  overflow: hidden;
}

.swiper image {
  width: 100%;
  height: 100%;
  border-radius: 30rpx;
  box-shadow: 0 8rpx 24rpx rgba(0,0,0,0.15);
}

/* 控制轮播图指示点大小 */
.swiper /deep/ .uni-swiper-dot {
  width: 12rpx !important;
  height: 12rpx !important;
  margin: 0 6rpx !important;
  border-radius: 50% !important;
  background-color: rgba(255, 255, 255, 0.4) !important;
}

.swiper /deep/ .uni-swiper-dot-active {
  background-color: rgba(255, 255, 255, 0.8) !important;
}

.function-grid {
  display: flex;
  justify-content: space-between;
  margin-bottom: 40rpx;
}

.grid-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  width: 23%;
}

.grid-icon {
  width: 80rpx;
  height: 80rpx;
  margin-bottom: 6rpx;
}

.grid-text {
  font-size: 28rpx;
  color: #4B3425;
}

.appointment-card {
  height: 120rpx;
  background-color: #fff;
  border-radius: 32rpx;
  display: flex;
  justify-content: center;
  align-items: center;
  margin-bottom: 30rpx;
  font-size: 32rpx;
  color: #4B3425;
  box-shadow: 0 2rpx 8rpx rgba(0,0,0,0.1);
}

/* 按钮容器 */
.button-container {
  display: flex;
  justify-content: space-between;
  margin-bottom: 30rpx;
  gap: 20rpx;
}

/* 远程协助按钮 */
.assist-button {
  flex: 1;
  height: 150rpx;
  background-color: #fff;
  border-radius: 32rpx;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 30rpx;
  /* color: rgb(57, 57, 57); */
  box-shadow: 0 2rpx 8rpx rgba(0,0,0,0.1);
}

/* 紧急求助按钮 */
.emergency-button-new {
  flex: 1;
  height: 150rpx;
  background-color: #ffcac958;
  border-radius: 32rpx;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 30rpx;
  color: #d32f2f;
  box-shadow: 0 2rpx 8rpx rgba(0,0,0,0.1);
}

/* 按钮文字 */
.button-assist-text {
  font-size: 32rpx;
  color: #4B3425;
  /* font-weight: bold; */
}

.button-emergency-text {
  font-size: 32rpx;
  font-weight: bold;
  color: #ee5353;
}

/* 图标占位空间 */
.button-icon-space {
  width: 60rpx;
  height: 60rpx;
  /* 右侧留出空间 */
  margin-right: 8rpx;
}
</style>