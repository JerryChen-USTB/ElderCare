<template>
  <view class="emergencycenter-container">
    <!-- 顶部标题和筛选 -->
    <view class="header">
      <view class="filter">
        <view 
          class="filter-item" 
          :class="{active: currentFilter === 'all'}" 
          @click="currentFilter = 'all'"
        >
          全部
        </view>
        <view 
          class="filter-item" 
          :class="{active: currentFilter === 'unhandled'}" 
          @click="currentFilter = 'unhandled'"
        >
          未处理
        </view>
        <view 
          class="filter-item" 
          :class="{active: currentFilter === 'handled'}" 
          @click="currentFilter = 'handled'"
        >
          已处理
        </view>
      </view>
    </view>
	
    <!-- 新增：大字警告 -->
        <view class="function-not-open">功能未开放！</view>
		
    <!-- 预警列表 -->
    <view class="alert-list">
      <view 
        v-for="(alert, index) in filteredAlerts" 
        :key="index" 
        class="alert-item"
      >
        <view class="alert-header">
          <text class="alert-type">{{ alert.type }}</text>
          <text class="alert-time">{{ alert.time }}</text>
          <text 
            class="alert-status" 
            :class="{pending: alert.status === 'pending', resolved: alert.status === 'resolved'}"
          >
            {{ alert.status === 'pending' ? '未处理' : '已处理' }}
          </text>
        </view>
        <view class="alert-content">
          <text>{{ alert.content }}</text>
        </view>
        <view class="alert-footer" v-if="alert.status === 'pending'">
          <button class="handle-btn" @click="handleAlert(alert.id, index)">处理预警</button>
        </view>
      </view>
    </view>
    
    <!-- 处理预警弹窗 -->
    <view v-show="showHandleModal" class="modal-mask">
      <view class="modal-container">
        <view class="modal-header">
          <text>处理预警</text>
        </view>
        <view class="modal-content">
          <textarea 
            v-model="handleRemark" 
            placeholder="请输入处理说明" 
            class="remark-input"
          ></textarea>
        </view>
        <view class="modal-footer">
          <button class="cancel-btn" @click="closeHandleModal">取消</button>
          <button class="confirm-btn" @click="confirmHandle">确认处理</button>
        </view>
      </view>
    </view>
    
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
import EmergencyModal from '@/components/emergency-modal.vue'
import EmergencyModalMixin from '@/mixins/emergency-modal.js'

export default {
  components: { EmergencyModal },
  mixins: [EmergencyModalMixin],
  data() {
    return {
      // 预警数据
      alerts: [
        {
          id: 1,
          type: '心率异常',
          time: '2025-06-23 09:15:30',
          content: '用户张三心率持续高于120次/分钟，已持续15分钟',
          status: 'pending',
          user: '张三',
          remark: ''
        },
        {
          id: 2,
          type: '体温异常',
          time: '2025-06-23 08:30:45',
          content: '用户李四体温38.5℃，疑似发烧',
          status: 'resolved',
          user: '李四',
          remark: '已联系家属并安排就医'
        },
        {
          id: 3,
          type: '跌倒预警',
          time: '2025-06-23 07:45:10',
          content: '用户张三活动轨迹异常，疑似跌倒',
          status: 'pending',
          user: '张三',
          remark: ''
        }
      ],
      // 当前筛选条件
      currentFilter: 'all',
      // 是否显示处理弹窗
      showHandleModal: false,
      // 当前处理的预警ID
      currentAlertId: null,
      // 当前处理的预警索引
      currentAlertIndex: null,
      // 处理备注
      handleRemark: ''
    }
  },
  computed: {
    // 筛选后的预警列表
    filteredAlerts() {
      if (this.currentFilter === 'all') {
        return this.alerts;
      } else if (this.currentFilter === 'unhandled') {
        return this.alerts.filter(alert => alert.status === 'pending');
      } else {
        return this.alerts.filter(alert => alert.status === 'resolved');
      }
    }
  },
  methods: {
    // 处理预警
    handleAlert(id, index) {
      this.currentAlertId = id;
      this.currentAlertIndex = index;
      this.showHandleModal = true;
    },
    
    // 关闭处理弹窗
    closeHandleModal() {
      this.showHandleModal = false;
      this.handleRemark = '';
    },
    
    // 确认处理
    confirmHandle() {
      if (!this.handleRemark.trim()) {
        uni.showToast({
          title: '请输入处理说明',
          icon: 'none'
        });
        return;
      }
      
      // 更新预警状态
      const alert = this.alerts[this.currentAlertIndex];
      alert.status = 'resolved';
      alert.remark = this.handleRemark;
      
      // 关闭弹窗
      this.showHandleModal = false;
      this.handleRemark = '';
      
      // 显示成功提示
      uni.showToast({
        title: '处理成功',
        icon: 'success'
      });
      
      // 预留后端接口调用位置
      /*
      uni.request({
        url: 'https://your-backend-api-url.com/handleAlert',
        method: 'POST',
        data: {
          alertId: this.currentAlertId,
          status: 'resolved',
          remark: this.handleRemark
        },
        success: (res) => {
          if (res.data.code === 200) {
            // 更新成功
            uni.showToast({
              title: '处理成功',
              icon: 'success'
            });
          } else {
            // 更新失败，回滚状态
            alert.status = 'pending';
            alert.remark = '';
            uni.showToast({
              title: '处理失败，请重试',
              icon: 'none'
            });
          }
        },
        fail: (err) => {
          // 处理失败，回滚状态
          alert.status = 'pending';
          alert.remark = '';
          uni.showToast({
            title: '网络错误，请重试',
            icon: 'none'
          });
        }
      });
      */
    }
  }
}
</script>

<style scoped>
.emergencycenter-container {
  padding: 20rpx;
  /* 绿色系渐变背景，营造健康氛围 */
  background: linear-gradient(to bottom, #e6f7ef, #f5faf8);
  min-height: 100vh;
}

.header {
  margin-bottom: 30rpx;
}

.filter {
  display: flex;
  justify-content: space-around;
  background-color: #fff;
  border-radius: 20rpx;
  padding: 15rpx 0;
  box-shadow: 0 4rpx 12rpx rgba(0, 0, 0, 0.05);
}

.filter-item {
  flex: 1;
  text-align: center;
  font-size: 28rpx;
  color: #666;
  cursor: pointer;
  padding: 10rpx 0;
  position: relative;
  transition: all 0.3s ease;
}

.filter-item.active {
  color: #3cc51f;
  font-weight: bold;
}

/* 选中项底部绿色指示条（强化视觉反馈） */
.filter-item.active::after {
  content: '';
  position: absolute;
  bottom: 0;
  left: 50%;
  transform: translateX(-50%);
  width: 40rpx;
  height: 6rpx;
  background-color: #3cc51f;
  border-radius: 3rpx;
}

.alert-list {
  margin-top: 20rpx;
  display: flex;
  flex-direction: column;
  gap: 24rpx; /* 卡片间距更透气 */
}

.alert-item {
  background-color: #fff;
  border-radius: 16rpx;
  padding: 30rpx;
  box-shadow: 0 6rpx 16rpx rgba(0, 0, 0, 0.06);
  transition: all 0.3s ease; /* 悬停动画过渡 */
  position: relative;
  overflow: hidden;
  /* 左侧绿色竖条（不同预警类型区分颜色） */
  border-left: 8rpx solid #3cc51f;
}

/* 不同预警类型的左侧竖条颜色（绿色系深浅区分） */
.alert-item:has(.alert-type:contains('心率异常')) {
  border-left-color: #4caf50;
}
.alert-item:has(.alert-type:contains('体温异常')) {
  border-left-color: #8bc34a;
}
.alert-item:has(.alert-type:contains('跌倒预警')) {
  border-left-color: #388e3c;
}

/* 卡片悬停效果（上浮+阴影加深） */
.alert-item:hover {
  transform: translateY(-4rpx);
  box-shadow: 0 10rpx 24rpx rgba(0, 0, 0, 0.1);
}

.alert-header {
  display: flex;
  justify-content: space-between;
  margin-bottom: 18rpx;
  align-items: center;
}

.alert-type {
  font-size: 32rpx;
  font-weight: bold;
  color: #333;
}

.alert-time {
  font-size: 24rpx;
  color: #999;
}

.alert-status {
  font-size: 24rpx;
  padding: 6rpx 16rpx;
  border-radius: 12rpx;
  min-width: 120rpx;
  text-align: center;
}

.alert-status.pending {
  background-color: #ffebee;
  color: #ef5350;
}

.alert-status.resolved {
  background-color: #e8f5e9;
  color: #43a047;
}

.alert-content {
  font-size: 28rpx;
  color: #666;
  line-height: 1.6; /* 提升长文本可读性 */
  margin-bottom: 20rpx;
}

.alert-footer {
  text-align: right;
}

.handle-btn {
  background: linear-gradient(to right, #4caf50, #388e3c); /* 绿色渐变按钮 */
  color: white;
  border: none;
  border-radius: 12rpx;
  padding: 12rpx 24rpx;
  font-size: 28rpx;
  cursor: pointer;
  transition: transform 0.2s ease; /* 点击缩放反馈 */
}

.handle-btn:hover {
  transform: scale(1.05);
}

.handle-btn:active {
  transform: scale(0.95);
}

/* 模态框样式优化 */
.modal-mask {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background-color: rgba(0, 0, 0, 0.5);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 999;
}

.modal-container {
  background-color: #fff;
  border-radius: 24rpx;
  width: 85%;
  max-width: 640rpx;
  box-shadow: 0 12rpx 32rpx rgba(0, 0, 0, 0.15);
  overflow: hidden;
  animation: modalFadeIn 0.3s ease; /* 淡入动画 */
}

@keyframes modalFadeIn {
  from {
    opacity: 0;
    transform: scale(0.9);
  }
  to {
    opacity: 1;
    transform: scale(1);
  }
}

.modal-header {
  padding: 30rpx;
  text-align: center;
  font-size: 36rpx;
  font-weight: bold;
  color: #fff;
  background-color: #3cc51f; /* 模态框头部绿色背景 */
}

.modal-content {
  padding: 30rpx;
}

.remark-input {
  width: 100%;
  height: 220rpx;
  border: 2rpx solid #e0e0e0;
  border-radius: 12rpx;
  padding: 16rpx;
  font-size: 28rpx;
  line-height: 1.6;
  resize: none;
  transition: border-color 0.3s ease; /* 聚焦边框变色 */
}

.remark-input:focus {
  border-color: #3cc51f;
  outline: none;
}

.modal-footer {
  display: flex;
  border-top: 1rpx solid #f0f0f0;
}

.cancel-btn, .confirm-btn {
  flex: 1;
  text-align: center;
  padding: 24rpx;
  font-size: 32rpx;
  cursor: pointer;
  transition: all 0.3s ease;
}

.cancel-btn {
  color: #666;
  border-right: 1rpx solid #f0f0f0;
  background-color: #f5f5f5;
}

.cancel-btn:hover {
  background-color: #eaeaea;
}

.confirm-btn {
  color: #fff;
  background: linear-gradient(to right, #4caf50, #388e3c);
}

.confirm-btn:hover {
  background: linear-gradient(to right, #55c259, #43a047);
}

.confirm-btn:active {
  transform: scale(0.98);
}
.function-not-open {
  /* 固定定位：页面滚动时仍可见，垂直位置避开顶部筛选区 */
  position: fixed;
  top: 180rpx; 
  left: 0;
  width: 100%;
  /* 视觉醒目：红色背景+白色粗体大字体 */
  background-color: #ff3b30; /* 苹果红，警告色更易识别 */
  color: #ffffff;
  font-size: 52rpx; /* 超大字体，强制注意力 */
  font-weight: 700;
  text-align: center;
  padding: 35rpx 0; /* 上下内边距，增加视觉面积 */
  z-index: 100; /* 确保在预警列表上方，不被遮挡 */
  box-shadow: 0 6rpx 16rpx rgba(255, 59, 48, 0.4); /* 红色阴影强化警告感 */
  }
</style>