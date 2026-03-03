<template>
  <view class="medicine-remind-container">

    <!-- 加载状态 -->
    <view v-if="isLoading" class="loading-overlay">
      <view class="loading-spinner"></view>
    </view>

    <!-- 错误状态 -->
    <view v-if="errorMsg && !isLoading" class="error-container">
      <image src="/static/error-icon.png" class="error-icon"></image>
      <text class="error-text">{{ errorMsg }}</text>
      <button class="retry-btn" @click="loadMedicationData">重试</button>
    </view>

    <!-- 筛选栏 -->
    <view v-if="!isLoading && !errorMsg" class="filter-bar">
      <view 
        class="filter-item" 
        :class="{ active: filterStatus === 'all' }" 
        @click="changeFilter('all')"
      >
        <text>全部</text>
      </view>
      <view 
        class="filter-item" 
        :class="{ active: filterStatus === 'active' }" 
        @click="changeFilter('active')"
      >
        <text>正在服用</text>
      </view>
      <view 
        class="filter-item" 
        :class="{ active: filterStatus === 'expired' }" 
        @click="changeFilter('expired')"
      >
        <text>已过期</text>
      </view>
    </view>

    <!-- 用药列表 -->
    <scroll-view 
      v-if="!isLoading && !errorMsg" 
      class="medicine-list" 
      scroll-y="true"
    >
      <!-- 空状态 -->
      <view v-if="filteredMedicines.length === 0" class="empty-state">
        <image src="/static/guardian/empty-med.png" class="empty-icon"></image>
        <text class="empty-text">暂无用药提醒</text>
        <text class="empty-subtext">（无添加入口，已移除相关功能）</text>
      </view>

      <!-- 用药项 -->
      <view 
        v-for="(med, index) in filteredMedicines" 
        :key="`med-${med.id}-${index}`"
        class="medicine-card"
        @touchstart="touchStart(index)"
        @touchend="touchEnd"
        :style="activeIndex === index ? { transform: 'scale(0.98)' } : {}"
        @click="showMedicineDetail(med)"
      >
        <view class="medicine-header">
          <text class="medicine-name">{{ med.medicineName }}</text>
          <view class="status-badge" :class="getStatusClass(med.isActive, med.isExpired)">
            {{ med.isExpired === '已过期' ? '药物已过期' : med.isActive }}
          </view>
        </view>

        <view class="medicine-details">
          <view class="detail-item">
            <uni-icons type="time" size="16" color="#666"></uni-icons>
            <text class="detail-text">剂量: {{ med.dosage }}</text>
          </view>
          <view class="detail-item">
            <uni-icons type="calendar" size="16" color="#666"></uni-icons>
            <text class="detail-text">频率: {{ med.frequency }}次/天</text>
          </view>
          <view class="detail-item">
            <uni-icons type="info" size="16" color="#666"></uni-icons>
            <text class="detail-text">周期: {{ med.startDate }} 至 {{ med.endDate }}</text>
          </view>
        </view>
      </view>
    </scroll-view>

    <!-- 用药详情弹窗 -->
    <view v-if="showDetailModal" class="modal-overlay" @click="closeDetailModal">
      <view class="modal-content" @click.stop>
        <view class="modal-header">
          <text class="modal-title">用药详情</text>
          <uni-icons type="close" size="24" @click="closeDetailModal"></uni-icons>
        </view>
        <view class="modal-body" v-if="currentMedicine">
          <view class="detail-item">
            <text class="detail-label">药品名称：</text>
            <text class="detail-value">{{ currentMedicine.medicineName }}</text>
          </view>
          <view class="detail-item">
            <text class="detail-label">剂量：</text>
            <text class="detail-value">{{ currentMedicine.dosage }}</text>
          </view>
          <view class="detail-item">
            <text class="detail-label">服用频率：</text>
            <text class="detail-value">{{ currentMedicine.frequency }}次/天</text>
          </view>
          <view class="detail-item">
            <text class="detail-label">开始日期：</text>
            <text class="detail-value">{{ currentMedicine.startDate }}</text>
          </view>
          <view class="detail-item">
            <text class="detail-label">结束日期：</text>
            <text class="detail-value">{{ currentMedicine.endDate }}</text>
          </view>
          <!-- 新增：保质期至 -->
          <view class="detail-item">
            <text class="detail-label">保质期至：</text>
            <text class="detail-value">{{ currentMedicine.expireDate || '未设置' }}</text>
          </view>
          <!-- 新增：药物过期状态（颜色区分） -->
          <view class="detail-item">
            <text class="detail-label">是否过期：</text>
            <text class="detail-value" :style="currentMedicine.isExpired === '已过期' ? 'color: #ff4444;' : (currentMedicine.isExpired === '未过期' ? 'color: #3cc51f;' : 'color: #999;')">
              {{ currentMedicine.isExpired }}
            </text>
          </view>
          <view class="detail-item">
            <text class="detail-label">用药状态：</text>
            <text class="detail-value" :class="currentMedicine.isActive === '正在服用' ? 'active-status' : 'expired-status'">
              {{ currentMedicine.isActive }}
            </text>
          </view>
          <view class="detail-item notes-item" v-if="currentMedicine.notes">
            <text class="detail-label">备注：</text>
            <text class="detail-value notes-content">{{ currentMedicine.notes }}</text>
          </view>
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
      userId: '',                // 用户ID
      medicines: [],             // 原始用药列表
      filterStatus: 'all',       // 筛选状态：all/active/expired
      isLoading: false,          // 加载状态
      errorMsg: '',              // 错误信息
      activeIndex: -1,           // 列表项点击动效
      showDetailModal: false,    // 详情弹窗显示
      currentMedicine: null      // 当前选中的用药记录
    }
  },

  computed: {
    filteredMedicines() {
      const currentDate = this.getCurrentDate(); // 当前日期
      if (this.filterStatus === 'all') {
        return this.medicines;
      } else if (this.filterStatus === 'active') {
        // “正在服用”：排除药物过期 + 用药状态为“正在服用”
        return this.medicines.filter(med => 
          med.isActive === '正在服用' && 
          this.compareDate(med.expireDate, currentDate) >= 0
        );
      } else if (this.filterStatus === 'expired') {
        // “已过期”：包含两种情况：1.药物过期；2.用药状态已过期
        return this.medicines.filter(med => {
          const isDrugExpired = this.compareDate(med.expireDate, currentDate) < 0;
          return med.isActive === '已过期' || isDrugExpired;
        });
      }
      return this.medicines;
    }
  },

  onLoad() {
    this.initUserInfo(); // 初始化用户信息
  },

  methods: {
    // 从本地存储初始化用户信息（提取userId）
    initUserInfo() {
      const userInfo = uni.getStorageSync('userInfo');
      if (userInfo && userInfo.id) {
        this.userId = userInfo.id; // 从userInfo中取id作为userId
        this.loadMedicationData(); // 加载用药数据
      } else {
        this.errorMsg = '未获取到用户信息，请先登录';
        this.isLoading = false;
      }
    },

    // 获取当前日期（格式：yyyy-MM-dd）
    getCurrentDate() {
      const now = new Date();
      const year = now.getFullYear();
      const month = String(now.getMonth() + 1).padStart(2, '0');
      const day = String(now.getDate()).padStart(2, '0');
      return `${year}-${month}-${day}`;
    },
  
    // 比较两个日期（yyyy-MM-dd格式）
    compareDate(date1, date2) {
      if (!date1 || !date2) return 1; // 空日期视为“未过期”
      const d1 = new Date(date1);
      const d2 = new Date(date2);
      if (d1 < d2) return -1;
      if (d1 > d2) return 1;
      return 0;
    },
  
    // 加载用药记录
    loadMedicationData() {
      this.isLoading = true;
      const token = uni.getStorageSync('token'); // 从缓存取登录token
      const baseUrl = config.API_BASE_URL;

      // 接口请求
      uni.request({
        url: `${baseUrl}/api/elder/medications/by-user-id`,
        method: 'GET',
        header: {
          'Authorization': `Bearer ${token}`,
          'Content-Type': 'application/json'
        },
        data: { userId: this.userId },
        success: (res) => {
          if (res.data.code === 200 && res.data.data) {
            // 格式化“未设置”的字段
            this.medicines = res.data.data.map(med => ({
              ...med,
              expireDate: med.expireDate || med.expireDate === '' ? med.expireDate : '未设置',
              endDate: med.endDate || med.endDate === '' ? med.endDate : '未设置',
              isExpired: med.expireDate ? med.isExpired : '未过期' 
            }));
        
            // 优化排序
            this.medicines.sort((medA, medB) => {
              const getPriority = (med) => {
                // 1. 最高优先级：正在服用 + （保质期未设置 或 明确未过期）
                if (med.isActive === '正在服用' && (med.isExpired === '未过期' || med.expireDate === '未设置')) {
                  return 1;
                }
                // 2. 次高优先级：已停用 + （保质期未设置 或 明确未过期）
                else if (med.isActive === '已停用' && (med.isExpired === '未过期' || med.expireDate === '未设置')) {
                  return 2;
                }
                // 3. 最低优先级：已过期（无论是否设置保质期）
                else {
                  return 3;
                }
              };
              return getPriority(medA) - getPriority(medB);
            });
        
            this.errorMsg = '';
          } else {
            this.errorMsg = res.data.msg || '获取用药记录失败';
            this.medicines = [];
          }
        },
        fail: (err) => {
          this.errorMsg = `网络错误：${err.errMsg}`;
          this.medicines = [];
        },
        complete: () => {
          this.isLoading = false;
        }
      });
    },

    // 筛选切换
    changeFilter(status) {
      this.filterStatus = status;
    },

    // 返回上一页
    navigateBack() {
      uni.navigateBack();
    },

    // 显示用药详情
    showMedicineDetail(medicine) {
      this.currentMedicine = medicine;
      this.showDetailModal = true;
    },

    // 关闭详情弹窗
    closeDetailModal() {
      this.showDetailModal = false;
      this.currentMedicine = null;
    },


    // 获取状态标签样式
    getStatusClass(activeStatus, expireStatus) {
      // 药物已过期：强制显示过期样式
      if (expireStatus === '已过期') {
        return 'expired';
      }
      // 正常用药状态
      switch (activeStatus) {
        case '正在服用':
          return 'active';
        case '已过期':
        case '已停用':
          return 'expired';
        default:
          return 'default';
      }
    },

    // 列表项点击动效
    touchStart(index) {
      this.activeIndex = index;
    },
    touchEnd() {
      this.activeIndex = -1;
    }
  }
}
</script>

<style scoped>
/* 页面容器 */
.medicine-remind-container {
  padding: 20rpx;
  min-height: 100vh;
  background-color: #f8f8f8;
}

/* 筛选栏 */
.filter-bar {
  display: flex;
  background: #fff;
  border-radius: 16rpx;
  margin-bottom: 20rpx;
  padding: 20rpx;
}
.filter-item {
  flex: 1;
  text-align: center;
  padding: 16rpx;
  font-size: 28rpx;
  color: #666;
  border-radius: 8rpx;
  margin: 0 10rpx;
}
.filter-item.active {
  background: #3cc51f;
  color: #fff;
}

/* 用药列表 */
.medicine-list {
  height: calc(100vh - 300rpx);
}

/* 用药卡片 */
.medicine-card {
  background: #fff;
  border-radius: 16rpx;
  padding: 30rpx;
  margin-bottom: 20rpx;
  box-shadow: 0 4rpx 12rpx rgba(0, 0, 0, 0.06);
  transition: transform 0.2s;
}

.medicine-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20rpx;
  white-space: nowrap;
}

.medicine-name {
  font-size: 32rpx;
  font-weight: bold;
  color: #333;
  flex: 1;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

/* 状态标签 */
.status-badge {
  padding: 8rpx 16rpx;
  border-radius: 20rpx;
  font-size: 24rpx;
}
.status-badge.active {
  background: #e7f7e4;
  color: #3cc51f;
}
.status-badge.expired {
  background: #ffebeb;
  color: #ff5500;
}
.status-badge.default {
  background: #f5f5f5;
  color: #999;
}

/* 用药详情项 */
.medicine-details {
  margin-bottom: 20rpx;
}
.detail-item {
  display: flex;
  align-items: center;
  margin-bottom: 12rpx;
}
.detail-text {
  font-size: 26rpx;
  color: #666;
  margin-left: 10rpx;
}

/* 操作按钮 */
.medicine-actions {
  display: flex;
  justify-content: flex-end;
  gap: 20rpx;
}
.action-btn {
  display: flex;
  align-items: center;
  padding: 10rpx 20rpx;
  background: #f5f5f5;
  border-radius: 8rpx;
  font-size: 24rpx;
}

/* 空状态 */
.empty-state {
  text-align: center;
  padding: 100rpx 0;
  color: #999;
}
.empty-icon {
  width: 120rpx;
  height: 120rpx;
  margin-bottom: 20rpx;
  opacity: 0.6;
}
.empty-text {
  display: block;
  font-size: 32rpx;
  margin-bottom: 10rpx;
}
.empty-subtext {
  font-size: 26rpx;
  color: #ccc;
}

/* 错误状态 */
.error-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 100rpx 20rpx;
}
.error-icon {
  width: 120rpx;
  height: 120rpx;
  margin-bottom: 30rpx;
  opacity: 0.6;
}
.error-text {
  font-size: 28rpx;
  color: #ff5500;
  margin-bottom: 30rpx;
  text-align: center;
}
.retry-btn {
  background-color: #3cc51f;
  color: #fff;
  padding: 16rpx 60rpx;
  border-radius: 30rpx;
  font-size: 28rpx;
}

/* 加载状态 */
.loading-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(255, 255, 255, 0.8);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 2000;
}
.loading-spinner {
  width: 60rpx;
  height: 60rpx;
  border: 4rpx solid #f3f3f3;
  border-top: 4rpx solid #3cc51f;
  border-radius: 50%;
  animation: spin 1s linear infinite;
}

/* 详情弹窗 */
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 1000;
}
.modal-content {
  background: #fff;
  border-radius: 16rpx;
  width: 90%;
  max-width: 600rpx;
  max-height: 80vh;
  overflow-y: auto;
}
.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 30rpx;
  border-bottom: 1rpx solid #eee;
}
.modal-title {
  font-size: 32rpx;
  font-weight: bold;
  color: #333;
}
.modal-body {
  padding: 30rpx;
}
.modal-body .detail-item {
  display: flex;
  align-items: flex-start;
  padding: 10rpx 0;
  margin-bottom: 15rpx;
}

.modal-body .detail-label {
  width: 140rpx;
  text-align: right;
  margin-right: 20rpx;
  color: #666;
  font-size: 28rpx;
}

.modal-body .detail-value {
  flex: 1;
  color: #333;
  font-size: 28rpx;
  line-height: 1.5;
  word-break: break-all;
}
.active-status {
  color: #3cc51f;
  font-weight: 500;
}
.expired-status {
  color: #ff5500;
  font-weight: 500;
}
.notes-item {
  flex-direction: column;
}
.notes-content {
  margin-top: 10rpx;
  padding: 20rpx;
  background-color: #f9f9f9;
  border-radius: 8rpx;
  line-height: 1.6;
}

/* 加载动画 */
@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}
</style>
