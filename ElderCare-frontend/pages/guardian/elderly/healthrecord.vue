<template>
  <view class="health-record-container">
    <!-- 筛选栏（新增hover/点击微动画） -->
    <view class="filter-bar">
      <!-- 指标类型筛选 -->
      <picker 
        mode="selector" 
        :range="indicatorTypes" 
        range-key="label"
        :value="currentTypeIndex" 
        @change="onTypeChange"
        class="type-filter"
      >
        <view class="filter-item" @tap="filterTapAnim">
          <uni-icons type="filter" size="22" color="#3cc51f" class="filter-icon"></uni-icons>
          <text>指标类型：</text>
          <text class="selected-type">{{ indicatorTypes[currentTypeIndex].label }}</text>
          <uni-icons type="down" size="20" color="#999" class="filter-arrow"></uni-icons>
        </view>
      </picker>
      
      <!-- 异常信息筛选（新增图标过渡动画） -->
      <view 
        class="abnormal-filter" 
        @click="toggleAbnormalFilter"
        @tap="filterTapAnim"
      >
        <uni-icons 
          :type="showOnlyAbnormal ? 'checkbox-filled' : 'checkbox'" 
          size="22" 
          :color="showOnlyAbnormal ? '#ff4d4f' : '#999'"
          class="abnormal-icon"
        ></uni-icons>
        <text :class="{ 'active': showOnlyAbnormal }">仅显示异常</text>
      </view>
    </view>

    <!-- 健康记录列表（新增加载淡入动画、异常点呼吸动效） -->
    <view 
      class="record-list" 
      v-if="!isLoading"
      :key="`list-key-${currentTypeIndex}-${showOnlyAbnormal}`" 
    >
      <!-- 列表标题（优化渐变质感） -->
      <view class="list-header">
        <text class="header-time">时间</text>
        <text class="header-type">指标类型</text>
        <text class="header-value">数值</text>
      </view>

      <!-- 记录条目（新增加载动画、hover增强） -->
      <view 
        v-for="(item, index) in filteredHealthHistory" 
        :key="`record-${item.id || index}`" 
        class="record-item"
        :class="{ 'warning': item.isWarning }"
        :style="`animation-delay: ${index * 0.05}s`" 
      >
        <!-- 异常标记点（新增呼吸动效） -->
        <view class="warning-dot" v-if="item.isWarning"></view>
        
        <text class="time">{{ formatTime(item.recordTime) }}</text>
        <text class="type" :class="{ 'warning-text': item.isWarning }">{{ getLabelByType(item.healthType) }}</text>
        <text class="value" :class="{ 'warning-text': item.isWarning }">
          {{ item.value }} 
          <text class="unit">{{ item.unit }}</text>
        </text>
      </view>

      <!-- 空状态（新增图标浮动动画） -->
      <view class="empty" v-if="filteredHealthHistory.length === 0">
        <uni-icons type="empty" size="60" color="#ddd" class="empty-icon"></uni-icons>
        <text>{{ showOnlyAbnormal ? '暂无异常记录' : `暂无${indicatorTypes[currentTypeIndex].label !== '全部' ? indicatorTypes[currentTypeIndex].label : ''}记录` }}</text>
      </view>

      <!-- 加载更多按钮（新增hover图标旋转） -->
      <view 
        class="load-more" 
        v-if="hasMore && !showOnlyAbnormal" 
        @click="loadHistoryData"
        @tap="loadMoreTapAnim"
      >
        <text>加载更多</text>
        <uni-icons type="refresh" size="18" color="#3cc51f" class="load-icon"></uni-icons>
      </view>
    </view>

    <!-- 加载状态（优化Spinner渐变旋转） -->
    <view v-if="isLoading" class="loading">
      <view class="loading-spinner"></view>
      <text>加载中...</text>
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
import config from '@/utils/config.js'
import EmergencyModal from '@/components/emergency-modal.vue'
import EmergencyModalMixin from '@/mixins/emergency-modal.js'

export default {
  components: { EmergencyModal },
  mixins: [EmergencyModalMixin],
  data() {
    return {
      userId: '',
      healthHistory: [],
      isLoading: false,
      currentPage: 1,
      pageSize: 10,
      healthType: '',
      indicatorTypes: [
        { label: '全部', value: '' },
        { label: '心率', value: 'heart_rate' },
        { label: '血压', value: 'blood_pressure' },
        { label: '血糖', value: 'blood_sugar' },
        { label: '体重', value: 'weight' },
        { label: '体温', value: 'temperature' },
        { label: '步数', value: 'steps' }
      ],
      currentTypeIndex: 0,
      hasMore: true,
      showOnlyAbnormal: false,
      normalRanges: {
        heart_rate: { min: 60, max: 100 },
        blood_pressure: { systolic: { max: 140 }, diastolic: { max: 90 } },
        blood_sugar: { min: 3.9, max: 6.1 },
        temperature: { min: 36.1, max: 37.2 }
      }
    }
  },
  computed: {
    filteredHealthHistory() {
      if (!this.showOnlyAbnormal) {
        return this.healthHistory;
      }
      return this.healthHistory.filter(item => item.isWarning);
    }
  },
  onLoad(options) {
    this.userId = options.userId;
    this.loadHistoryData();
  },
  methods: {
    navigateBack() {
      uni.navigateBack();
    },

    toggleAbnormalFilter() {
      this.showOnlyAbnormal = !this.showOnlyAbnormal;
    },

    onTypeChange(e) {
      const index = e.detail.value;
      this.currentTypeIndex = index;
      this.healthType = this.indicatorTypes[index].value;
      this.currentPage = 1;
      this.healthHistory = [];
      this.hasMore = true;
      this.loadHistoryData();
    },

    loadHistoryData() {
      if (this.isLoading || !this.hasMore) return;
      
      this.isLoading = true;
      const token = uni.getStorageSync('token');
      
      uni.request({
        url: `${config.API_BASE_URL}/api/guardian/health/history/${this.userId}`,
        method: 'GET',
        header: { 'Authorization': `Bearer ${token}` },
        data: {
          healthType: this.healthType,
          page: this.currentPage,
          size: this.pageSize
        },
        success: (res) => {
          if (res.statusCode === 200) {
            const responseData = res.data;
            let records = [];
            let total = 0;
    
            if (Array.isArray(responseData)) {
              records = responseData;
              total = responseData.length;
            } else if (responseData?.records) {
              records = responseData.records;
              total = responseData.total || 0;
            } else if (responseData?.data) {
              records = responseData.data;
              total = records.length;
            }
    
            records = Array.isArray(records) ? records : [];
            
            const processedRecords = records.map(item => ({
              ...item,
              isWarning: this.checkIsWarning(item)
            }));
    
            this.healthHistory = [...this.healthHistory, ...processedRecords];
            this.hasMore = this.healthHistory.length < total;
            this.currentPage++;
          } else {
            uni.showToast({ title: `加载失败: ${res.statusCode}`, icon: 'none' });
          }
        },
        fail: (err) => {
          console.error('请求失败:', err);
          uni.showToast({ title: '网络错误', icon: 'none' });
        },
        complete: () => {
          this.isLoading = false;
        }
      });
    },

    checkIsWarning(item) {
      let isWarning = false;
      
      if (item.healthType === 'blood_pressure') {
        const [systolic, diastolic] = item.value.split('/').map(Number);
        isWarning = systolic > 140 || diastolic > 90;
      } else if (item.healthType === 'heart_rate') {
        const rate = Number(item.value);
        isWarning = rate < 60 || rate > 100;
      } else if (item.healthType === 'blood_sugar') {
        const level = Number(item.value);
        isWarning = level < 3.9 || level > 6.1;
      } else if (item.healthType === 'temperature') {
        const temp = Number(item.value);
        isWarning = temp < 36.1 || temp > 37.2;
      }
      
      return isWarning;
    },

    getLabelByType(type) {
      const typeMap = {
        'heart_rate': '心率',
        'blood_pressure': '血压',
        'blood_sugar': '血糖',
        'weight': '体重',
        'temperature': '体温',
        'steps': '步数'
      };
      return typeMap[type] || '未知指标';
    },

    formatTime(timeStr) {
      if (!timeStr) return '';
      return timeStr.replace('T', ' ').slice(0, 16);
    },

    // 新增：筛选项点击微动画（防止重复点击视觉反馈）
    filterTapAnim(e) {
      e.currentTarget.style.transform = 'scale(0.96)';
      setTimeout(() => {
        e.currentTarget.style.transform = 'scale(1)';
      }, 150);
    },

    // 新增：加载更多点击动画
    loadMoreTapAnim(e) {
      e.currentTarget.style.transform = 'scale(0.95)';
      setTimeout(() => {
        e.currentTarget.style.transform = 'scale(1)';
      }, 150);
    }
  }
}
</script>

<style scoped>
/* 页面容器：优化背景柔和度 */
.health-record-container {
  padding: 0;
  min-height: 100vh;
  background-color: #f5f7fa;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
}

/* 筛选栏：新增hover过渡、微动画 */
.filter-bar {
  display: flex;
  align-items: center;
  background: #fff;
  padding: 20rpx 30rpx;
  margin: 15rpx;
  border-radius: 16rpx;
  box-shadow: 0 2rpx 10rpx rgba(0, 0, 0, 0.03);
  transition: box-shadow 0.3s ease;
}
.filter-bar:hover {
  box-shadow: 0 4rpx 16rpx rgba(0, 0, 0, 0.05);
}

.type-filter {
  flex: 1;
  margin-right: 20rpx;
}

.filter-item {
  font-size: 28rpx;
  color: #666;
  display: flex;
  align-items: center;
  padding: 10rpx 15rpx;
  background-color: #f8f8f8;
  border-radius: 8rpx;
  transition: all 0.2s ease; /* 背景色/变换过渡 */
  cursor: pointer;
}
.filter-item:hover {
  background-color: #f0f7f2;
}

.filter-icon {
  margin-right: 10rpx;
}

.selected-type {
  color: #333;
  font-weight: 500;
  margin: 0 10rpx;
  flex: 1;
}

.filter-arrow {
  margin-left: 10rpx;
  transition: transform 0.3s ease;
}
.filter-item:active .filter-arrow {
  transform: rotate(180deg);
}

/* 异常筛选：图标颜色平滑过渡 */
.abnormal-filter {
  display: flex;
  align-items: center;
  padding: 10rpx 15rpx;
  border-radius: 8rpx;
  background-color: #f8f8f8;
  font-size: 28rpx;
  color: #999;
  transition: all 0.2s ease;
  cursor: pointer;
}
.abnormal-filter:hover {
  background-color: #fff0f0;
}
.abnormal-filter.active {
  color: #ff4d4f;
  background-color: #fff0f0;
}
.abnormal-icon {
  transition: color 0.3s ease; /* 图标颜色过渡 */
}
.abnormal-filter text {
  margin-left: 8rpx;
}

/* 记录列表：容器过渡（筛选切换时淡入淡出） */
.record-list {
  padding: 0 15rpx;
  opacity: 1;
  transition: opacity 0.3s ease;
}
.record-list[key^="list-key"] {
  opacity: 0;
  animation: listFadeIn 0.3s ease forwards;
}
@keyframes listFadeIn {
  to { opacity: 1; }
}

/* 列表表头：优化渐变质感 */
.list-header {
  display: flex;
  padding: 20rpx 30rpx;
  background: linear-gradient(90deg, #f0f7f2, #e6f7ef); /* 柔和渐变 */
  border-radius: 12rpx 12rpx 0 0;
  margin-top: 15rpx;
  box-shadow: 0 2rpx 4rpx rgba(60, 197, 31, 0.05);
}

.header-time, .header-type, .header-value {
  font-size: 24rpx;
  color: #555;
  font-weight: 500;
}

.header-time {
  flex: 1;
}
.header-type {
  flex: 1;
  text-align: center;
}
.header-value {
  flex: 1;
  text-align: right;
}

/* 记录条目：新增加载动画、hover增强 */
.record-item {
  padding: 24rpx 30rpx;
  border-bottom: 1rpx solid #f5f5f5;
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 26rpx;
  background-color: #fff;
  position: relative;
  padding-left: 40rpx;
  transition: all 0.25s ease; /* 背景/阴影过渡 */
  /* 条目加载动画：从下往上淡入 */
  opacity: 0;
  transform: translateY(10rpx);
  animation: recordFadeUp 0.3s ease forwards;
}
@keyframes recordFadeUp {
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.record-item:last-child {
  border-bottom: none;
  border-radius: 0 0 12rpx 12rpx;
}
.record-item:first-child {
  border-radius: 12rpx 12rpx 0 0;
  margin-top: 2rpx;
}

/* 条目hover增强：轻微缩放+阴影 */
.record-item:hover {
  background-color: #fafafa;
  transform: translateY(-2rpx);
  box-shadow: 0 4rpx 8rpx rgba(0, 0, 0, 0.03);
}

/* 异常条目：优化背景色，更柔和 */
.warning {
  background-color: #fff8f8;
  border-left: 4rpx solid #ff4d4f; /* 左侧红色边框，强化异常标识 */
}

/* 异常标记点：呼吸动效（提醒注意） */
.warning-dot {
  position: absolute;
  left: 15rpx;
  top: 50%;
  transform: translateY(-50%);
  width: 16rpx;
  height: 16rpx;
  background-color: #ff4d4f;
  border-radius: 50%;
  animation: pulse 2s infinite alternate; /* 呼吸动画 */
}
@keyframes pulse {
  from { opacity: 0.7; transform: translateY(-50%) scale(0.9); }
  to { opacity: 1; transform: translateY(-50%) scale(1.1); }
}

.warning-text {
  color: #ff4d4f !important;
  font-weight: 500;
}

.time {
  color: #888;
  flex: 1;
  transition: color 0.2s ease;
}
.record-item:hover .time {
  color: #666;
}

.type {
  color: #333;
  flex: 1;
  text-align: center;
}

.value {
  color: #333;
  flex: 1;
  text-align: right;
  font-weight: 500;
  transition: color 0.2s ease;
}
.record-item:hover .value {
  color: #2d9716; /* hover时数值变主色，突出重点 */
}

.unit {
  color: #999;
  font-size: 24rpx;
  margin-left: 5rpx;
}

/* 空状态：图标浮动动画 */
.empty {
  text-align: center;
  padding: 120rpx 0;
  color: #999;
  font-size: 28rpx;
  background-color: #fff;
  border-radius: 12rpx;
  margin-top: 15rpx;
  display: flex;
  flex-direction: column;
  align-items: center;
  box-shadow: 0 2rpx 8rpx rgba(0, 0, 0, 0.02);
}
.empty-icon {
  margin-bottom: 20rpx;
  animation: float 3s ease-in-out infinite; /* 上下浮动 */
}
@keyframes float {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-10rpx); }
}

/* 加载更多：hover图标旋转 */
.load-more {
  text-align: center;
  padding: 16rpx 40rpx;
  font-size: 26rpx;
  color: #3cc51f;
  cursor: pointer;
  margin: 30rpx auto;
  background-color: #f0f7f2;
  border-radius: 60rpx;
  display: inline-flex;
  align-items: center;
  box-shadow: 0 2rpx 8rpx rgba(60, 197, 31, 0.15);
  transition: all 0.25s ease;
}
.load-more:hover {
  background-color: #e6f7ef;
  box-shadow: 0 4rpx 12rpx rgba(60, 197, 31, 0.2);
}
/* hover时图标缓慢旋转 */
.load-more:hover .load-icon {
  animation: spinSlow 2s linear infinite;
}
@keyframes spinSlow {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}
.load-more:active {
  transform: scale(0.96);
  box-shadow: 0 2rpx 4rpx rgba(60, 197, 31, 0.15);
}

.load-icon {
  margin-left: 10rpx;
  transition: transform 0.3s ease;
}

/* 加载状态：优化Spinner渐变旋转 */
.loading {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 60vh;
}
.loading-spinner {
  width: 60rpx;
  height: 60rpx;
  border: 5rpx solid #f0f7f2;
  border-top: 5rpx solid #3cc51f;
  /* 渐变Spinner：旋转时颜色过渡 */
  border-image: conic-gradient(#3cc51f, #81c784, #a5d6a7) 1;
  border-radius: 50%;
  animation: spin 1.2s linear infinite;
}
@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}
.loading text {
  margin-top: 20rpx;
  color: #666;
  font-size: 28rpx;
  letter-spacing: 2rpx;
}
</style>