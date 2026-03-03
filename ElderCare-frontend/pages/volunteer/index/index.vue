<template>
  <view class="volunteer-index">
    <!-- 顶部欢迎信息 -->
    <view class="header">
      <text class="main-greeting">{{ timeGreeting }}亲爱的志愿者</text>
      <text class="sub-greeting">感谢您的爱心付出</text>
    </view>

    <!-- 主要内容区 -->
    <view class="content">
      <!-- 功能宫格 - 前两行 -->
      <view class="function-grid">
        <view class="grid-item" @click="navigateTo('tasklist')">
          <image src="/static/volunteer/task.png" class="grid-icon" mode="aspectFit"></image>
          <text class="grid-text">任务列表</text>
        </view>
        <view class="grid-item" @click="navigateTo('volunteerrecord')">
          <image src="/static/volunteer/service_record.png" class="grid-icon" mode="aspectFit"></image>
          <text class="grid-text">服务记录</text>
        </view>
        <view class="grid-item" @click="navigateTo('calender')">
          <image src="/static/volunteer/calender.png" class="grid-icon" mode="aspectFit"></image>
          <text class="grid-text">日程管理</text>
        </view>
        <view class="grid-item" @click="showNotOpenTip">
          <image src="/static/volunteer/communication.png" class="grid-icon" mode="aspectFit"></image>
          <text class="grid-text">交流中心</text>
        </view>
      </view>

      <!-- 远程协助按钮 - 独占一行 -->
      <view class="remote-assist-card" @click="navigateTo('remote-service')">
        <text class="remote-assist-text">远程协助</text>
        <image src="/static/volunteer/remote_assistance.png" class="remote-assist-icon" mode="aspectFit"></image>
      </view>

      <!-- 推荐任务 -->
      <view class="recommended-tasks">
      <text class="section-title">推荐任务</text>
      
      <!-- 加载中状态 -->
      <view class="loading" v-if="loading">
        <text class="loading-text">加载推荐任务中...</text>
      </view>
      
      <!-- 无任务状态 -->
      <view class="no-task" v-else-if="displayTasks.length === 0">
        <image src="/static/images/no-task.png" mode="aspectFit" class="no-task-img" />
        <text class="no-task-text">暂无未接单任务</text>
      </view>
      
      <!-- 任务列表 -->
      <view class="task-list" v-else>
        <view class="task-item" v-for="(task, index) in displayTasks" :key="task.id || index">
          <view class="task-header">
            <text class="task-title">{{ task.title || '未命名任务' }}</text>
            <text class="task-status">{{ task.status || '未知状态' }}</text>
          </view>
          
          <view class="task-info">
            <text class="task-date">{{ task.date || '未设置时间' }}</text>
            <text class="task-location">{{ task.location || '未设置地点' }}</text>
          </view>
          
          <view class="task-reward-container">
            <text class="task-reward">报酬: {{ task.reward || '无' }}</text>
          </view>
          
          <!-- 右下角的接受任务按钮 -->
          <button 
            class="accept-btn" 
            @click="acceptTask(task.id, task)"
            :disabled="!task.id"
          >
            接受任务
          </button>
        </view>
        
        <!-- 查看全部按钮 -->
        <view class="view-all-btn" v-if="allPendingTasks.length > 2" @click="navigateTo('all-tasks')">
          <text>查看全部未接单任务 →</text>
        </view>
      </view>
    </view>

    </view>

    <!-- 底部导航 -->
    <custom-tabbar :current="0" :role="'volunteer'" />
  </view>
</template>

<script>
import CustomTabbar from '@/components/custom-tabbar.vue'
import config from '@/utils/config.js'

export default {
  components: { CustomTabbar },
  data() {
    return {
      allPendingTasks: [],  // 存储所有未接单任务
      displayTasks: [],     // 显示前2条任务
      loading: false,       // 加载状态
      userId: ''            // 从缓存提取的用户ID
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
  onShow() {
    // 刷新用户ID，确保登录状态最新
    const userInfo = uni.getStorageSync('userInfo');
    this.userId = userInfo?.id || '';
    console.log('当前用户ID:', this.userId);
    
    if (this.userId) {
       this.getPendingAppointments(); // 重新获取最新的未接单任务列表
     } else {
       // 未登录：清空任务列表，提示登录
       this.allPendingTasks = [];
       this.displayTasks = [];
       uni.showToast({ title: '请先登录', icon: 'none' });
       setTimeout(() => uni.navigateTo({ url: '/pages/volunteer/login' }), 1500);
     }
  },
  onLoad() {
    // 首次加载时获取用户ID并加载任务
    const userInfo = uni.getStorageSync('userInfo');
    this.userId = userInfo?.id || '';
    this.getPendingAppointments();
  },
  methods: {
	  showNotOpenTip() {
	      uni.showToast({
	        title: '交流中心功能暂未开放，敬请期待～', // 提示文本
	        icon: 'none', // 不显示图标（仅文本提示，更清晰）
	        duration: 2000, // 提示显示时间（2秒后自动消失）
	        position: 'middle' // 提示框在屏幕中间显示（默认底部，中间更显眼）
	      });
	    },
    // 页面跳转
    navigateTo(page) {
      if (!page) {
        uni.showToast({ title: '页面路径不能为空', icon: 'none' });
        return;
      }

      const pageMap = {
        tasklist: '/pages/volunteer/index/task',
        volunteerrecord: '/pages/volunteer/index/service_record',
        calender: '/pages/volunteer/index/calender',
        communication: '/pages/volunteer/communication/communication',
        'remote-service': '/pages/volunteer/remote-service/remote-service',
        'all-tasks': '/pages/volunteer/index/task'
      };

      const pagePath = pageMap[page];
      if (!pagePath) {
        uni.showToast({ title: '页面不存在', icon: 'none' });
        return;
      }

      uni.navigateTo({ url: pagePath });
    },

    // 获取未接单任务
    getPendingAppointments() {
      this.loading = true;

      // 未登录拦截
      if (!this.userId) {
        this.loading = false;
        uni.showToast({ title: '请先登录', icon: 'none' });
        setTimeout(() => uni.navigateTo({ url: '/pages/volunteer/login' }), 1500);
        return;
      }

      uni.request({
        url: `${config.API_BASE_URL}/api/volunteer/appointment/pending-list`,
        method: 'GET',
        data: { userId: this.userId },
        header: { 'Content-Type': 'application/json' },
        success: (res) => {
          console.log('后端返回数据:', res.data);
          
          if (res.statusCode === 200 && res.data.success) {
            this.allPendingTasks = res.data.recommendedTasks || [];
            this.displayTasks = this.allPendingTasks.slice(0, 2);
            console.log('已加载任务数:', this.allPendingTasks.length);
          } else {
            uni.showToast({ title: res.data?.message || '获取任务失败', icon: 'none' });
          }
        },
        fail: (err) => {
          console.error('接口请求失败:', err);
        },
        complete: () => {
          this.loading = false;
        }
      });
    },

    // 接受任务
    acceptTask(appointmentId, task) {
      console.log('接受任务:', { appointmentId, task });

      if (!this.userId) {
        uni.showToast({ title: '请先登录', icon: 'none' });
        setTimeout(() => uni.navigateTo({ url: '/pages/volunteer/login' }), 1500);
        return;
      }

      if (!appointmentId || appointmentId <= 0) {
        uni.showToast({ title: '任务ID无效', icon: 'none' });
        return;
      }

      uni.showModal({
        title: '确认接受任务',
        content: `您确定要接受【${task.title || '未命名任务'}】吗？`,
        confirmText: '确认接受',
        cancelText: '取消',
        success: (res) => {
          if (res.confirm) {
            this.doAcceptTask(appointmentId);
          }
        }
      });
    },

     doAcceptTask(appointmentId) {
       // 1. 先校验userId是否有效（关键！避免传空值）
       if (!this.userId || this.userId <= 0) {
         uni.hideLoading();
         uni.showToast({ title: '请先登录', icon: 'none' });
         setTimeout(() => uni.navigateTo({ url: '/pages/volunteer/login' }), 1500);
         return;
       }
     
       // 2. 显示加载状态
       uni.showLoading({ title: '处理中...', mask: true });
     
       // 3. 关键：把userId拼在URL末尾作为查询参数（?userId=xxx）
       const requestUrl = `${config.API_BASE_URL}/api/volunteer/appointment/accept/${appointmentId}?userId=${this.userId}`;
       console.log('最终请求URL:', requestUrl); // 必须看到类似 "...?userId=4" 的格式
     
       // 4. 发送请求（无需在data中传userId）
       uni.request({
         url: requestUrl,
         method: 'POST',
         header: {
           'Content-Type': 'application/json' // 保持JSON头，不影响查询参数传递
         },
         data: {}, // 空请求体，userId已在URL中
         success: (res) => {
           if (res.statusCode === 200) {
             if (res.data.success) {
               uni.showToast({ title: '接单成功！', icon: 'success' });
               this.getPendingAppointments();
             } else {
               uni.showToast({ title: res.data.message || '接单失败', icon: 'none' });
             }
           } else {
             uni.showToast({ title: `请求异常(${res.statusCode})`, icon: 'none' });
           }
         },
         fail: (err) => {
           console.error('接单失败:', err);
           uni.showToast({ title: '网络错误，请重试', icon: 'none' });
         },
         complete: () => {
           uni.hideLoading();
         }
       });
     }
	}
}
</script>

<style scoped>
.volunteer-index {
  padding-bottom: 120rpx;
  background-color: #F8F4F4;
}

/* 顶部样式 */
.header {
  padding: 80rpx 30rpx 50rpx;   /* 参数分别表示：上边距、左边距、下边距 */
  background: #9AB169;
  color: white;
  border-radius: 0 0 50rpx 50rpx;
  position: relative;
  z-index: 1;
}

.main-greeting {
  font-size: 52rpx;
  font-weight: bold;
  display: block;
  margin-bottom: 18rpx;
  color: white;
}

.sub-greeting {
  font-size: 32rpx;
  display: block;
  color: white;
  opacity: 0.9;
  font-weight: 600;
}

.content {
  padding: 30rpx;
}

/* 功能宫格 - 2x2布局 */
.function-grid {
  display: flex;
  justify-content: space-between;
  flex-wrap: wrap;
  margin-bottom: 5rpx;
}

.grid-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  width: 48%;
  background-color: #fff;
  border-radius: 32rpx;
  padding: 40rpx 0;
  margin-bottom: 15rpx;
  box-shadow: 0 4rpx 12rpx rgba(0,0,0,0.05);
  transition: transform 0.2s ease;
}

.grid-item:active {
  transform: scale(0.95);
}

.grid-icon {
  width: 80rpx;
  height: 80rpx;
  margin-bottom: 15rpx;
}

.grid-text {
  font-size: 28rpx;
  color: #4B3425;
  font-weight: 500;
}

/* 远程协助卡片 - 独占一行 */
.remote-assist-card {
  height: 200rpx;
  background-color: #fff;
  border-radius: 32rpx;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 30rpx;
  margin-bottom: 20rpx;
  box-shadow: 0 4rpx 12rpx rgba(0,0,0,0.05);
  transition: transform 0.2s ease;
}

.remote-assist-card:active {
  transform: scale(0.98);
}

.remote-assist-text {
  margin-left: 20rpx;
  font-size: 36rpx;
  color: #4B3425;
  font-weight: 600;
}

.remote-assist-icon {
  margin-right: 30rpx;
  width: 90rpx;
  height: 90rpx;
}

/* 推荐任务区域 */
.recommended-tasks {
  background-color: #fff;
  padding: 30rpx;
  border-radius: 32rpx;
  box-shadow: 0 4rpx 12rpx rgba(0,0,0,0.05);
}

.section-title {
  font-size: 36rpx;
  font-weight: bold;
  margin-bottom: 30rpx;
  color: #4B3425;
  display: block;
}

/* 加载状态 */
.loading {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 60rpx 0;
}
.loading-text {
  font-size: 24rpx;
  color: #999;
}

/* 无任务状态 */
.no-task {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 60rpx 0;
}
.no-task-img {
  width: 200rpx;
  height: 200rpx;
  margin-bottom: 20rpx;
  object-fit: contain;
}
.no-task-text {
  font-size: 26rpx;
  color: #999;
}

/* 任务列表 */
.task-list {
  display: flex;
  flex-direction: column;
  gap: 20rpx;
}

.task-item {
  border: 1rpx solid #f0f0f0;
  border-radius: 24rpx;
  padding: 30rpx;
  background-color: #fafafa;
  position: relative;
  padding-bottom: 90rpx;
  transition: all 0.2s ease;
}

.task-item:active {
  background-color: #f5f5f5;
  transform: scale(0.98);
}
.task-header {
  display: flex;
  justify-content: space-between;
  margin-bottom: 15rpx;
  align-items: center;
}
.task-title {
  font-size: 30rpx;
  font-weight: bold;
  color: #4B3425;
}
.task-status {
  font-size: 24rpx;
  color: #3cc51f;
  padding: 5rpx 15rpx;
  border-radius: 20rpx;
  background-color: rgba(60, 197, 31, 0.1);
}
.task-info {
  display: flex;
  flex-wrap: wrap;
  margin-bottom: 15rpx;
  gap: 15rpx;
}
.task-date, .task-location {
  font-size: 26rpx;
  color: #666;
  display: flex;
  align-items: center;
}

/* 报酬信息容器 */
.task-reward-container {
  margin-bottom: 10rpx;
}
.task-reward {
  font-size: 26rpx;
  color: #ff7e5f;
  font-weight: 500;
}

/* 接受任务按钮 - 固定在右下角 */
.accept-btn {
  background: linear-gradient(135deg, #9AB169, #B5C88A);
  color: white;
  border-radius: 50rpx;
  padding: 18rpx 40rpx;
  font-size: 28rpx;
  font-weight: 600;
  border: none;
  outline: none;
  position: absolute;
  right: 25rpx;
  bottom: 25rpx;
  box-shadow: 0 4rpx 12rpx rgba(154, 177, 105, 0.3);
  transition: all 0.2s ease;
}

.accept-btn:disabled {
  background: #cccccc;
  color: #ffffff;
  box-shadow: none;
}

.accept-btn:active {
  transform: scale(0.95);
  box-shadow: 0 2rpx 8rpx rgba(154, 177, 105, 0.4);
}

/* 查看全部按钮 */
.view-all-btn {
  text-align: center;
  margin-top: 30rpx;
  color: #9AB169;
  font-size: 28rpx;
  padding: 20rpx 0;
  border-top: 1rpx solid #f0f0f0;
  font-weight: 500;
}

.view-all-btn:active {
  opacity: 0.7;
}
</style>
