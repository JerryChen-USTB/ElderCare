<template>
  <view class="medicine-remind-container">

    <!-- 加载状态（目标风格） -->
    <view v-if="isLoading" class="loading-overlay">
      <view class="loading-spinner"></view>
    </view>

    <!-- 错误状态（保留功能正确的错误提示逻辑） -->
    <view v-if="errorMsg && !isLoading" class="error-container">
      <image src="/static/error-icon.png" class="error-icon"></image>
      <text class="error-text">{{ errorMsg }}</text>
      <button class="retry-btn" @click="loadMedicationData">重试</button>
    </view>

    <!-- 筛选栏（目标风格 + 功能正确的筛选逻辑：all/active/expired） -->
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

    <!-- 用药列表（目标风格：scroll-view + 卡片式 + 功能正确的筛选数据） -->
    <scroll-view 
      v-if="!isLoading && !errorMsg" 
      class="medicine-list" 
      scroll-y="true"
    >
      <!-- 空状态（更新提示文本：匹配右下角添加按钮） -->
      <view v-if="filteredMedicines.length === 0" class="empty-state">
        <image src="/static/guardian/empty-med.png" class="empty-icon"></image>
        <text class="empty-text">暂无用药提醒</text>
        <text class="empty-subtext">点击右下角按钮添加用药提醒</text>
      </view>

      <!-- 用药项（目标风格卡片 + 功能正确的列表渲染） -->
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

        <!-- 操作按钮（保留功能正确的编辑/删除入口） -->
        <view class="medicine-actions">
          <view class="action-btn" @click.stop="navigateToEdit(med.id)">
            <uni-icons type="edit" size="18" color="#3cc51f"></uni-icons>
            <text>编辑</text>
          </view>
          <view class="action-btn" @click.stop="deleteMedicine(med.id)">
            <uni-icons type="trash" size="18" color="#ff4444"></uni-icons>
            <text>删除</text>
          </view>
        </view>
      </view>
    </scroll-view>

    <!-- 用药详情弹窗（功能正确的弹窗逻辑 + 目标风格样式） -->
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

    <!-- 右下角圆形悬浮添加按钮（核心新增） -->
    <view class="floating-add-btn" @click="addNewMedicine">
      <uni-icons type="plus" size="24" color="#fff"></uni-icons>
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
	import config from '@/utils/config.js';
	import EmergencyModal from '@/components/emergency-modal.vue'
	import EmergencyModalMixin from '@/mixins/emergency-modal.js'
	
export default {
  components: { EmergencyModal },
  mixins: [EmergencyModalMixin],
  data() {
    return {
      // 基础状态（保留功能正确的字段）
      userId: '',                // 接收老人ID
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
        // "正在服用"：排除药物过期 + 用药状态为"正在服用"
        return this.medicines.filter(med => 
          med.isActive === '正在服用' && 
          this.compareDate(med.expireDate, currentDate) >= 0
        );
      } else if (this.filterStatus === 'expired') {
        // "已过期"：包含两种情况：1.药物过期；2.用药状态已过期
        return this.medicines.filter(med => {
          const isDrugExpired = this.compareDate(med.expireDate, currentDate) < 0;
          return med.isActive === '已过期' || isDrugExpired;
        });
      }
      return this.medicines;
    }
  },
  onShow() {
    // 页面每次显示时，重新注册事件监听（先移除旧监听，避免重复注册）
    if (this.medAddListener) {
      uni.$off('medicationAdded', this.medAddListener);
    }
    
    // 注册“新增成功”事件监听，增加 userId 校验
    this.medAddListener = uni.$on('medicationAdded', (addedUserId) => {
      console.log('监听到新增用药记录，验证 userId 是否匹配');
      // 关键：只当新增记录的 userId 与当前页面 userId 一致时，才重新加载
      if (addedUserId === this.userId) {
        this.loadMedicationData(); // 重新请求接口，实时更新列表
      }
    });
  },

  onLoad(options) {
    // 保留原有的参数接收和初始加载逻辑，删除事件监听代码
    if (options.userId) {
      this.userId = options.userId;
      this.loadMedicationData(); // 初始加载数据
    } else {
      this.errorMsg = '参数错误，无法加载数据';
    }
  },
  onUnload() {
      // 3. 新增：页面销毁时移除监听（必须加，防止多次注册）
      if (this.medAddListener) {
        uni.$off('medicationAdded', this.medAddListener);
      }
    },

  methods: {
	  // 1. 获取当前日期（格式：yyyy-MM-dd，与后端一致）
	    getCurrentDate() {
	      const now = new Date();
	      const year = now.getFullYear();
	      const month = String(now.getMonth() + 1).padStart(2, '0');
	      const day = String(now.getDate()).padStart(2, '0');
	      return `${year}-${month}-${day}`;
	    },
	  
	    // 2. 比较两个日期（yyyy-MM-dd格式）：date1 < date2 返回-1，相等返回0，date1>date2返回1
	    compareDate(date1, date2) {
	      if (!date1 || !date2) return 1; // 空日期视为"未过期"
	      const d1 = new Date(date1);
	      const d2 = new Date(date2);
	      if (d1 < d2) return -1;
	      if (d1 > d2) return 1;
	      return 0;
	    },
	  
    // 核心功能：加载用药记录（保留功能正确的回调式请求，避免res[1]错误）
    loadMedicationData() {
      this.isLoading = true;
      const token = uni.getStorageSync('token');
      const userInfo = uni.getStorageSync('userInfo');
      
      const guardianUserId = userInfo?.id;

      // 登录状态校验
      if (!guardianUserId) {
        this.errorMsg = '请先登录';
        this.isLoading = false;
        return;
      }

      // 接口请求（功能正确的回调逻辑）
      uni.request({
        url: `${config.API_BASE_URL}/api/guardian/medication/list/${this.userId}`,
        method: 'GET',
        header: {
          'Authorization': `Bearer ${token}`,
          'Content-Type': 'application/json'
        },
        data: { guardianUserId },
        success: (res) => {
          if (res.data.code === 200 && res.data.data) {
            // ?? 第一步：格式化"未设置"的字段
            this.medicines = res.data.data.map(med => ({
              ...med,
              // 保质期未设置：空/null/undefined → 显示"未设置"
              expireDate: med.expireDate || med.expireDate === '' ? med.expireDate : '未设置',
              // 结束日期未设置：空/null/undefined → 显示"未设置"
              endDate: med.endDate || med.endDate === '' ? med.endDate : '未设置',
              // 确保 isExpired 字段：未设置保质期时 → 视为"未过期"（避免误判）
              isExpired: med.expireDate ? med.isExpired : '未过期' 
            }));
        
            // ?? 第二步：优化排序（兼容"未设置"场景）
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
              return getPriority(medA) - getPriority(medB); // 数值越小排越前
            });
        
            this.errorMsg = '';
          }
         else {
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

    // 筛选切换（功能正确的逻辑 + 目标风格触发）
    changeFilter(status) {
      this.filterStatus = status;
    },

    // 返回上一页（功能正确的逻辑）
    navigateBack() {
      uni.navigateBack();
    },

    // 显示用药详情（功能正确的弹窗逻辑）
    showMedicineDetail(medicine) {
      this.currentMedicine = medicine;
      this.showDetailModal = true;
    },

    // 关闭详情弹窗（功能正确的逻辑）
    closeDetailModal() {
      this.showDetailModal = false;
      this.currentMedicine = null;
    },

    // 跳转到编辑页面（功能正确的跳转逻辑）
    navigateToEdit(medId) {
      this.closeDetailModal();
      uni.navigateTo({
        url: `/pages/guardian/elderly/medicine-edit?userId=${this.userId}&medId=${medId}`
      });
    },

    // 添加新用药记录（功能正确的跳转逻辑）
    addNewMedicine() {
      uni.navigateTo({
        url: `/pages/guardian/elderly/medicine-edit?userId=${this.userId}`
      });
    },

    // 删除用药记录（功能正确的确认+接口逻辑）
    deleteMedicine(medId) {
      uni.showModal({
        title: '确认删除',
        content: '确定要删除这条用药记录吗？',
        confirmColor: '#ff4444',
        success: (res) => {
          if (res.confirm) {
            this._doDelete(medId);
          }
        }
      });
    },

    // 执行删除操作（功能正确的接口调用）
    _doDelete(medId) {
      const token = uni.getStorageSync('token');
	  // 在这里添加：获取userInfo
	  const userInfo = uni.getStorageSync('userInfo'); 
      
	  const guardianUserId = userInfo?.id; // 从登录态获取监护人ID

      // 校验监护人ID是否存在
        if (!guardianUserId) {
          uni.showToast({ title: '请先登录', icon: 'none' });
          return;
        }
      
        uni.request({
          // 拼接查询参数：?guardianUserId=xxx
          url: `${config.API_BASE_URL}/api/guardian/medication/delete/${medId}?guardianUserId=${guardianUserId}`,
          method: 'DELETE',
          header: {
            'Authorization': `Bearer ${token}`,
            'Content-Type': 'application/json'
          },
        success: (res) => {
          if (res.data.code === 200) {
            uni.showToast({ title: '删除成功', icon: 'success' });
            this.loadMedicationData(); // 重新加载列表
            this.closeDetailModal();
          } else {
            uni.showToast({ title: res.data.msg || '删除失败', icon: 'none' });
          }
        },
        fail: () => {
          uni.showToast({ title: '网络错误，删除失败', icon: 'none' });
        }
      });
    },
	

    // 获取状态标签样式（适配目标风格的状态颜色）
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
    // 列表项点击动效（功能正确的逻辑）
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
/* 页面容器（目标风格） */
.medicine-remind-container {
  padding: 20rpx;
  min-height: 100vh;
  background-color: #f8f8f8;
  /* 为底部悬浮按钮预留空间 */
  padding-bottom: 120rpx;
}

/* 顶部导航（目标风格） */
.nav-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20rpx;
  background: #fff;
  border-radius: 16rpx;
  margin-bottom: 20rpx;
  box-shadow: 0 2rpx 12rpx rgba(0, 0, 0, 0.04);
}
.nav-title {
  font-size: 32rpx;
  font-weight: bold;
  color: #333;
}

/* 筛选栏（目标风格） */
.filter-bar {
  display: flex;
  background: #fff;
  border-radius: 16rpx;
  margin-bottom: 20rpx;
  padding: 20rpx;
  box-shadow: 0 2rpx 12rpx rgba(0, 0, 0, 0.04);
}
.filter-item {
  flex: 1;
  text-align: center;
  padding: 16rpx;
  font-size: 28rpx;
  color: #666;
  border-radius: 8rpx;
  margin: 0 10rpx;
  transition: all 0.3s ease;
}
.filter-item.active {
  background: linear-gradient(135deg, #3cc51f, #2da815);
  color: #fff;
  box-shadow: 0 4rpx 12rpx rgba(60, 197, 31, 0.3);
}

/* 用药列表（目标风格：scroll-view） */
.medicine-list {
  height: calc(100vh - 300rpx);
}

/* 用药卡片（目标风格） */
.medicine-card {
  background: #fff;
  border-radius: 16rpx;
  padding: 30rpx;
  margin-bottom: 20rpx;
  box-shadow: 0 4rpx 12rpx rgba(0, 0, 0, 0.06);
  transition: all 0.3s ease;
  border-left: 6rpx solid #3cc51f;
}
.medicine-card:active {
  transform: scale(0.98);
  box-shadow: 0 2rpx 8rpx rgba(0, 0, 0, 0.1);
}
/* 用药卡片头部（日期、名称、状态横向排列） */
.medicine-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20rpx;
  white-space: nowrap; /* 强制内部元素不换行 */
}

/* 日期样式：固定宽度 + 颜色区分 */
.medicine-date {
  font-size: 26rpx;
  color: #666;
  margin-right: 20rpx;
  min-width: 120rpx; /* 保证日期区域不被挤压 */
}

/* 药品名称：溢出省略（防止过长撑换行） */
.medicine-name {
  font-size: 32rpx;
  font-weight: bold;
  color: #333;
  flex: 1; /* 占用中间剩余空间 */
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis; /* 文本过长时显示"..." */
}

/* 状态标签（目标风格 + 功能正确的状态色） */
.status-badge {
  padding: 8rpx 16rpx;
  border-radius: 20rpx;
  font-size: 24rpx;
  font-weight: 500;
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

/* 用药详情项（目标风格） */
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

/* 操作按钮（目标风格） */
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
  transition: all 0.2s ease;
}
.action-btn:active {
  background: #e5e5e5;
  transform: scale(0.95);
}

/* 空状态（目标风格） */
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
  color: #666;
}
.empty-subtext {
  font-size: 26rpx;
  color: #ccc;
}

/* 错误状态（功能正确的样式 + 目标风格适配） */
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
  background: linear-gradient(135deg, #3cc51f, #2da815);
  color: #fff;
  padding: 16rpx 60rpx;
  border-radius: 30rpx;
  font-size: 28rpx;
  box-shadow: 0 4rpx 12rpx rgba(60, 197, 31, 0.3);
}

/* 加载状态（目标风格） */
.loading-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(255, 255, 255, 0.9);
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

/* 详情弹窗（目标风格 + 功能正确的布局） */
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
  box-shadow: 0 10rpx 30rpx rgba(0, 0, 0, 0.15);
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
  display: flex;         /* 横向排列标签和值 */
  align-items: flex-start; /* 文字顶部对齐（避免多行时错位） */
  padding: 10rpx 0;      /* 上下增加内边距，减少拥挤感 */
  margin-bottom: 15rpx;  /* 减少项与项之间的间距，避免太松散 */
}

.modal-body .detail-label {
  width: 140rpx;         /* 标签固定宽度 */
  text-align: right;     /* 标签右对齐 */
  margin-right: 20rpx;   /* 标签与值之间留间距 */
  color: #666;
  font-size: 28rpx;
}

.modal-body .detail-value {
  flex: 1;               /* 值占剩余空间 */
  color: #333;
  font-size: 28rpx;
  line-height: 1.5;      /* 行高更舒展，方便阅读 */
  word-break: break-all; /* 长文本自动换行，避免溢出 */
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
.modal-actions {
  display: flex;
  justify-content: flex-end;
  gap: 20rpx;
  margin-top: 40rpx;
  padding-top: 20rpx;
  border-top: 1rpx solid #eee;
}
.btn-cancel {
  background: #f5f5f5;
  color: #666;
  padding: 12rpx 30rpx;
  border-radius: 8rpx;
  font-size: 26rpx;
}
.btn-edit {
  background: #3cc51f;
  color: #fff;
  padding: 12rpx 30rpx;
  border-radius: 8rpx;
  font-size: 26rpx;
}

/* 右下角圆形悬浮添加按钮（核心新增样式） */
.floating-add-btn {
  position: fixed;
  right: 40rpx;
  bottom: 40rpx;
  width: 100rpx;
  height: 100rpx;
  background: linear-gradient(135deg, #3cc51f, #2da815);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 6rpx 20rpx rgba(60, 197, 31, 0.4);
  z-index: 100;
  /* 点击反馈 */
  transition: all 0.3s ease;
}
.floating-add-btn:active {
  transform: scale(0.9);
  box-shadow: 0 4rpx 12rpx rgba(60, 197, 31, 0.3);
}

/* 加载动画 */
@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}
</style>