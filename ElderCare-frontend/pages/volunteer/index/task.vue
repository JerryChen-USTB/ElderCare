<template>
  <view class="task-list-page">
    <!-- 推荐任务区域 -->
    <view class="recommended-tasks">
      <text class="section-title">任务列表</text>
      
      <!-- 加载中状态 -->
      <view class="loading" v-if="loading">
        <text class="loading-text">加载任务中...</text>
      </view>
      
      <!-- 无任务状态 -->
      <view class="no-task" v-else-if="displayTasks.length === 0">
        <image src="/static/images/no-task.png" mode="aspectFit" class="no-task-img" />
        <text class="no-task-text">暂无任务</text>
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
        
        <!-- 查看全部按钮（如果需要） -->
        <view class="view-all-btn" v-if="allPendingTasks.length > displayTasks.length" @click="viewAllTasks">
          <text>查看全部任务 →</text>
        </view>
      </view>
    </view>
  </view>
</template>

<script>
import config from '@/utils/config.js'

export default {
  data() {
    return {
      allPendingTasks: [],  // 所有任务
      displayTasks: [],     // 显示的任务（可根据需求调整数量）
      loading: false,       // 加载状态
      userId: ''            // 用户ID（从缓存获取）
    }
  },
  onShow() {
    // 获取用户ID
    const userInfo = uni.getStorageSync('userInfo');
    this.userId = userInfo?.id || '';
    if (this.userId) {
      this.getTasks();
    } else {
      uni.showToast({ title: '请先登录', icon: 'none' });
      setTimeout(() => uni.navigateTo({ url: '/pages/volunteer/login' }), 1500);
    }
  },
  methods: {
    // 获取任务
    getTasks() {
      this.loading = true;

      uni.request({
        url: `${config.API_BASE_URL}/api/volunteer/appointment/pending-list`,
        method: 'GET',
        data: { userId: this.userId },
        header: { 'Content-Type': 'application/json' },
        success: (res) => {
          console.log('后端返回数据:', res.data);
          
          if (res.statusCode === 200 && res.data.success) {
            this.allPendingTasks = res.data.recommendedTasks || [];
            this.displayTasks = this.allPendingTasks.slice(0, 5); // 可自定义显示数量
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

    // 执行接单请求
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
              this.getTasks(); 
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
     },

    // 查看全部任务（可跳转到全量任务页面）
    viewAllTasks() {
      uni.navigateTo({ url: '/pages/volunteer/tasklist/all-tasks' });
    }
  }
}
</script>

<style scoped>
.task-list-page {
  padding: 20rpx 30rpx;
  min-height: 100vh;
  background-color: #f8f9fa;
}

/* 推荐任务区域 */
.recommended-tasks {
  background-color: white;
  margin: 20rpx 0;
  padding: 30rpx;
  border-radius: 20rpx;
  box-shadow: 0 2rpx 10rpx rgba(0, 0, 0, 0.05);
}
.section-title {
  font-size: 32rpx;
  font-weight: bold;
  margin-bottom: 20rpx;
  color: #333;
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
  border: 1rpx solid #eee;
  border-radius: 15rpx;
  padding: 25rpx;
  background-color: #fff;
  position: relative;
  padding-bottom: 90rpx;
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
  color: #333;
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
  background-color: #4a9ff5;
  color: white;
  border-radius: 20rpx;
  padding: 15rpx 30rpx;
  font-size: 26rpx;
  border: none;
  outline: none;
  position: absolute;
  right: 25rpx;
  bottom: 25rpx;
}
.accept-btn:disabled {
  background-color: #cccccc;
  color: #ffffff;
  cursor: not-allowed;
}
.accept-btn:active {
  background-color: #3a8fe5;
  transform: scale(0.98);
}

/* 查看全部按钮 */
.view-all-btn {
  text-align: center;
  margin-top: 20rpx;
  color: #007aff;
  font-size: 24rpx;
  padding: 15rpx 0;
  border-top: 1rpx solid #f0f0f0;
}
</style>