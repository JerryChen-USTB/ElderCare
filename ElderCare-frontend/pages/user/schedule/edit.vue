<template>
  <view class="edit-schedule-container">
    <!-- uv-calendars 日历组件 -->
    <view class="calendar-wrapper">
      <uv-calendars 
        insert 
        :color="'#007AFF'"
        :startDate="minDate"
        :endDate="maxDate"
        :date="scheduleDate"
        @change="change" 
      />
    </view>

    <!-- 日程表单 -->
    <view class="form-container">
      <!-- 选择日期信息作为表单第一行 -->
      <view class="form-item">
        <text class="label">选择日期</text>
        <view class="selected-date-display">
          <text class="selected-date-text">{{ formatDate(scheduleDate) }} {{ getWeekday(scheduleDate) }}</text>
        </view>
      </view>
      <!-- 日程类型选择 -->
      <view class="form-item">
        <text class="label">日程类型</text>
        <picker 
          @change="bindTypeChange" 
          :value="typeIndex" 
          :range="scheduleTypes"
          range-key="name"
        >
          <view class="picker-item">
            <text>{{scheduleTypes[typeIndex].name}}</text>
            <uni-icons type="arrowright" size="16" color="#999"></uni-icons>
          </view>
        </picker>
      </view>

      <!-- 日程内容输入 -->
      <view class="form-item">
        <text class="label">日程内容</text>
        <input 
          v-model="scheduleContent" 
          placeholder="请输入日程内容" 
          class="input"
        />
      </view>

      <!-- 日程时间选择 -->
      <view class="form-item">
        <text class="label">日程时间</text>
        <view class="picker-item" @click="openTimePicker">
          <text>{{scheduleTime}}</text>
          <uni-icons type="arrowright" size="16" color="#999"></uni-icons>
        </view>
      </view>
      
      <!-- 时间选择器组件 -->
      <uv-datetime-picker 
        ref="timePicker"
        v-model="scheduleTimeValue"
        mode="time"
        title="选择日程时间"
        :itemHeight="88"
        :visibleItemCount="5"
        @confirm="confirmTime"
      ></uv-datetime-picker>

      <!-- 日程地点选择 -->
      <view class="form-item">
        <text class="label">日程地点</text>
        <picker 
          @change="bindLocationChange" 
          :value="locationIndex" 
          :range="commonLocations"
        >
          <view class="picker-item">
            <text>{{ locationIndex === commonLocations.length - 1 ? (customLocation || '自定义地点') : commonLocations[locationIndex] }}</text>
            <uni-icons type="arrowright" size="16" color="#999"></uni-icons>
          </view>
        </picker>
      </view>

      <!-- 自定义地点输入 -->
      <view class="form-item" v-if="locationIndex === commonLocations.length - 1">
        <text class="label">自定义地点</text>
        <input 
          v-model="customLocation" 
          placeholder="请输入地点名称" 
          class="input"
        />
      </view>
    </view>

    <!-- 提交按钮 -->
    <view class="submit-btn" @click="submitSchedule">
      <text>保存修改</text>
    </view>
  </view>
</template>

<script>
import request from '@/utils/request.js';
import { getCurrentUserId as getAuthUserId, requireLogin } from '@/utils/auth.js';

export default {
  data() {
    return {
      scheduleId: null, // 要编辑的日程ID
      scheduleTypes: [
        { name: '服药', value: 'medicine' },
        { name: '就医', value: 'doctor' },
        { name: '运动', value: 'exercise' },
        { name: '饮食', value: 'meal' },
        { name: '睡觉', value: 'sleep' },
        { name: '其他', value: 'other' }
      ],
      typeIndex: 0,
      scheduleTime: '08:00',
      scheduleTimeValue: '08:00', // 绑定到选择器的值
      scheduleContent: '',
      scheduleDate: '', // 选择的日期
      minDate: '', // 最小可选日期
      maxDate: '', // 最大可选日期
      commonLocations: [
        '家中',
        '医院',
        '社区医院',
        '药店',
        '公园',
        '健身房',
        '广场',
        '康复中心',
        '自定义地点'
      ],
      locationIndex: 0,
      customLocation: '',
      originalSchedule: null // 存储原始日程数据，用于对比
    };
  },
  onLoad(options) {
    uni.setNavigationBarTitle({ title: '编辑日程' });
    
    // 获取要编辑的日程ID
    if (options.id) {
      this.scheduleId = parseInt(options.id);
      this.loadScheduleDetail();
    } else {
      // 没有传入ID，返回上一页
      uni.showToast({
        title: '参数错误',
        icon: 'none'
      });
      setTimeout(() => {
        uni.navigateBack();
      }, 1000);
    }
  },
  created() {
    // 检查用户是否已登录
    if (!requireLogin(false)) {
      return; // 如果未登录，requireLogin会处理跳转
    }
    this.initializeDates();
  },
  methods: {
    // 加载日程详情数据
    async loadScheduleDetail() {
      try {
        const response = await uni.request({
          url: `${request.BASE_URL}/api/schedule/${this.scheduleId}`,
          method: 'GET',
          header: {
            'Content-Type': 'application/json'
          }
        });
        
        if (response.data.success && response.data.data) {
          const schedule = response.data.data;
          this.originalSchedule = schedule;
          
          // 填充表单数据
          this.populateFormData(schedule);
        } else {
          uni.showToast({
            title: '获取日程信息失败',
            icon: 'none'
          });
          setTimeout(() => {
            uni.navigateBack();
          }, 1000);
        }
      } catch (error) {
        console.error('获取日程详情失败:', error);
        uni.showToast({
          title: '网络异常',
          icon: 'none'
        });
        setTimeout(() => {
          uni.navigateBack();
        }, 1000);
      }
    },
    
    // 填充表单数据
    populateFormData(schedule) {
      // 设置日程类型
      const typeIndex = this.scheduleTypes.findIndex(type => type.value === schedule.type);
      this.typeIndex = typeIndex >= 0 ? typeIndex : 0;
      
      // 设置日程内容
      this.scheduleContent = schedule.content || '';
      
      // 处理时间字段
      if (schedule.time) {
        const scheduleTime = new Date(schedule.time);
        
        // 设置日期
        this.scheduleDate = this.formatDateToString(scheduleTime);
        
        // 设置时间
        this.scheduleTime = this.formatTimeToString(scheduleTime);
        this.scheduleTimeValue = this.scheduleTime; // 同步到选择器值
      }
      
      // 设置地点
      const locationIndex = this.commonLocations.indexOf(schedule.location);
      if (locationIndex >= 0 && locationIndex < this.commonLocations.length - 1) {
        // 是预设地点
        this.locationIndex = locationIndex;
        this.customLocation = '';
      } else {
        // 是自定义地点
        this.locationIndex = this.commonLocations.length - 1;
        this.customLocation = schedule.location || '';
      }
    },
    
    // 将Date对象格式化为YYYY-MM-DD字符串
    formatDateToString(date) {
      const year = date.getFullYear();
      const month = String(date.getMonth() + 1).padStart(2, '0');
      const day = String(date.getDate()).padStart(2, '0');
      return `${year}-${month}-${day}`;
    },
    
    // 将Date对象格式化为HH:MM字符串
    formatTimeToString(date) {
      const hours = String(date.getHours()).padStart(2, '0');
      const minutes = String(date.getMinutes()).padStart(2, '0');
      return `${hours}:${minutes}`;
    },
    
    // 处理日历日期变化
    change(e) {
      // uv-calendars 返回的事件对象包含选中的日期信息
      if (e && e.fulldate) {
        this.scheduleDate = e.fulldate;
      } else if (e && e.date) {
        this.scheduleDate = e.date;
      }
    },

    // 初始化日期相关数据
    initializeDates() {
      const today = new Date();
      const maxDate = new Date();
      maxDate.setFullYear(today.getFullYear() + 1); // 最大可选日期为一年后
      
      // 使用本地时间而不是UTC时间，避免时区问题
      const formatDate = (date) => {
        const year = date.getFullYear();
        const month = String(date.getMonth() + 1).padStart(2, '0');
        const day = String(date.getDate()).padStart(2, '0');
        return `${year}-${month}-${day}`;
      };
      
      // 如果scheduleDate还没有设置（从loadScheduleDetail设置），则使用今天
      if (!this.scheduleDate) {
        this.scheduleDate = formatDate(today);
      }
      this.minDate = formatDate(today);
      this.maxDate = formatDate(maxDate);
    },
    
    bindTypeChange(e) {
      this.typeIndex = e.detail.value;
    },
    
    // 打开时间选择器
    openTimePicker() {
      this.scheduleTimeValue = this.scheduleTime;
      this.$refs.timePicker.open();
      console.log('📅 打开时间选择器');
    },
    
    // 确认选择时间
    confirmTime(e) {
      this.scheduleTime = e.value;
      console.log('✅ 确认时间:', this.scheduleTime);
      
      // 震动反馈
      uni.vibrateShort();
    },
    
    bindDateChange(e) {
      this.scheduleDate = e.detail.value;
    },
    
    bindLocationChange(e) {
      this.locationIndex = e.detail.value;
      // 如果不是自定义地点，清空自定义地点输入
      if (this.locationIndex !== this.commonLocations.length - 1) {
        this.customLocation = '';
      }
    },
    
    // 格式化日期显示
    formatDate(dateStr) {
      const date = new Date(dateStr);
      const month = String(date.getMonth() + 1).padStart(2, '0');
      const day = String(date.getDate()).padStart(2, '0');
      return `${month}月${day}日`;
    },
    
    // 获取星期
    getWeekday(dateStr) {
      const date = new Date(dateStr);
      const weekdays = ['星期日', '星期一', '星期二', '星期三', '星期四', '星期五', '星期六'];
      return weekdays[date.getDay()];
    },
    
    // 获取最终的地点值
    getFinalLocation() {
      if (this.locationIndex === this.commonLocations.length - 1) {
        return this.customLocation || '待确定';
      }
      return this.commonLocations[this.locationIndex];
    },
    
    // 表单验证
    validateForm() {
      if (!this.scheduleContent.trim()) {
        uni.showToast({
          title: '请输入日程内容',
          icon: 'none'
        });
        return false;
      }
      
      if (this.locationIndex === this.commonLocations.length - 1 && !this.customLocation.trim()) {
        uni.showToast({
          title: '请输入自定义地点',
          icon: 'none'
        });
        return false;
      }
      
      return true;
    },
    
    async submitSchedule() {
      if (!this.validateForm()) {
        return;
      }
      
      try {
        const userId = this.getCurrentUserId(); // 获取当前用户ID
        if (!userId) {
          // 用户未登录，不能提交
          return;
        }
        
        const updateData = {
          date: this.scheduleDate,
          time: this.scheduleTime,
          content: this.scheduleContent,
          location: this.getFinalLocation(),
          type: this.scheduleTypes[this.typeIndex].value
        };
        
        // 调用后端API更新日程
        const response = await uni.request({
          url: `${request.BASE_URL}/api/schedule/${this.scheduleId}`,
          method: 'PUT',
          header: {
            'Content-Type': 'application/json'
          },
          data: updateData
        });
        
        if (response.statusCode === 200) {
          if (response.data && response.data.success) {
            uni.showToast({
              title: '日程更新成功',
              icon: 'success'
            });
            
            // 返回详情页，详情页的onShow方法会自动重新加载数据
            uni.navigateBack({
              delta: 1
            });
          } else {
            uni.showToast({
              title: '更新失败：' + (response.data ? response.data.message : '未知错误'),
              icon: 'none'
            });
          }
        } else {
          uni.showToast({
            title: `请求失败 (${response.statusCode})`,
            icon: 'none'
          });
        }
      } catch (error) {
        console.error('更新日程失败:', error);
        uni.showToast({
          title: '网络异常，请重试',
          icon: 'none'
        });
      }
    },
    
    // 获取当前用户ID
    getCurrentUserId() {
      const userId = getAuthUserId();
      if (!userId) {
        // 如果获取不到用户ID，说明用户未登录，需要跳转到登录页
        requireLogin();
        return null;
      }
      return userId;
    }
  }
};
</script>

<style scoped>
.edit-schedule-container {
  padding: 20rpx;
  background-color: #f5f5f5;
  min-height: 100vh;
  padding-bottom: 200rpx; /* 为底部按钮留出空间 */
}

/* 选中日期显示区域 - 与picker-item保持一致 */
.selected-date-display {
  flex: 1;
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 34rpx;
  color: #666;
  padding: 10rpx 0;
}

.selected-date-text {
  font-size: 34rpx;
  color: #666;
}

/* 日历容器样式 - 保持容器尺寸不变 */
.calendar-wrapper {
  width: 100%;
  max-width: 100%;
  margin: 0 auto;
  box-sizing: border-box;
  overflow: hidden;
  border-radius: 16rpx;
  box-shadow: 0 4rpx 16rpx rgba(0,0,0,0.1);
  background: #fff;
  display: flex;
  justify-content: center;
  align-items: center;
  margin-bottom: 20rpx;
}

/* 缩小日历组件本身，而不是容器 */
.calendar-wrapper /deep/ .uv-calendar {
  transform: scale(0.92);
  transform-origin: center;
}

/* 表单容器样式 */
.form-container {
  background: #fff;
  padding: 20rpx;
  border-radius: 16rpx;
}

.form-item {
  display: flex;
  align-items: center;
  padding: 25rpx 0;
  border-bottom: 1rpx solid #f5f5f5;
  min-height: 80rpx;
}

.form-item:last-child {
  border-bottom: none;
}

.label {
  font-size: 34rpx;
  color: #333;
  width: 200rpx;
  font-weight: 500;
}

.picker-item {
  flex: 1;
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 34rpx;
  color: #666;
  padding: 10rpx 0;
}

.input {
  flex: 1;
  font-size: 34rpx;
  color: #666;
  padding: 10rpx 0;
}

.input::placeholder {
  color: #ccc;
}

/* 提交按钮样式 */
.submit-btn {
  position: fixed;
  bottom: 60rpx;
  left: 50%;
  transform: translateX(-50%);
  background: #FF9500;
  color: white;
  padding: 28rpx 60rpx;
  border-radius: 60rpx;
  font-size: 36rpx;
  box-shadow: 0 8rpx 24rpx rgba(255,149,0,0.4);
  font-weight: bold;
  text-align: center;
  z-index: 100;
  transition: all 0.3s ease;
}

.submit-btn:active {
  transform: translateX(-50%) scale(0.95);
  box-shadow: 0 8rpx 24rpx rgba(255,149,0,0.3);
}

/* 响应式设计 */
@media screen and (max-width: 750rpx) {
  .label {
    width: 180rpx;
    font-size: 32rpx;
  }
  
  .picker-item, .input, .selected-date-display, .selected-date-text {
    font-size: 32rpx;
  }
}
</style>
