<template>
  <view class="appointment-container">
    <!-- 主体内容 -->
    <view class="content">
      <!-- 预约类型选择 -->
      <view class="form-card">
        <view class="form-title">
          <text>预约类型</text>
          <text class="required">*</text>
        </view>
        <picker 
          @change="bindTypeChange" 
          :value="typeIndex" 
          :range="appointmentTypes"
          range-key="name"
        >
          <view class="picker-item">
            <text>{{appointmentTypes[typeIndex].name}}</text>
            <uni-icons type="arrowright" size="16" color="#999"></uni-icons>
          </view>
        </picker>
      </view>

      <!-- 服务内容 -->
      <view class="form-card">
        <view class="form-title">
          <text>服务内容</text>
          <text class="required">*</text>
        </view>
        <textarea 
          v-model="serviceContent" 
          placeholder="请详细描述需要的服务内容..." 
          class="content-input"
          maxlength="500"
        ></textarea>
        <view class="char-count">{{serviceContent.length}}/500</view>
      </view>

      <!-- 预约时间选择 -->
      <view class="form-card">
        <view class="form-title">
          <text>预约时间</text>
          <text class="required">*</text>
        </view>
        
        <!-- 预约日期 -->
        <view class="time-item">
          <view class="time-label">预约日期</view>
          <picker 
            mode="date" 
            :value="appointmentDate" 
            :start="minDate"
            end="2025-12-31" 
            @change="bindDateChange"
            fields="day"
          >
            <view class="picker-item">
              <text>{{formatDate(appointmentDate)}}</text>
              <uni-icons type="arrowright" size="16" color="#999"></uni-icons>
            </view>
          </picker>
        </view>

        <!-- 开始时间 -->
        <view class="time-item">
          <view class="time-label">开始时间</view>
          <view class="picker-item-elder" @click="openStartTimePicker">
            <text class="picker-text-elder">{{startTime || '请选择开始时间'}}</text>
            <uni-icons type="arrowright" size="18" color="#999"></uni-icons>
          </view>
        </view>

        <!-- 结束时间 -->
        <view class="time-item">
          <view class="time-label">
            <text>结束时间</text>
            <text class="optional">(选填)</text>
          </view>
          <view class="picker-item-elder" @click="openEndTimePicker">
            <text class="picker-text-elder">{{endTime || '请选择结束时间'}}</text>
            <uni-icons type="arrowright" size="18" color="#999"></uni-icons>
          </view>
        </view>
        
        <!-- 时间选择器组件 -->
        <uv-datetime-picker 
          ref="startTimePicker"
          v-model="startTimeValue"
          mode="time"
          title="选择开始时间"
          :itemHeight="88"
          :visibleItemCount="5"
          @confirm="confirmStartTime"
          @cancel="cancelStartTime"
        ></uv-datetime-picker>
        
        <uv-datetime-picker 
          ref="endTimePicker"
          v-model="endTimeValue"
          mode="time"
          title="选择结束时间"
          :itemHeight="88"
          :visibleItemCount="5"
          @confirm="confirmEndTime"
          @cancel="cancelEndTime"
        ></uv-datetime-picker>
      </view>

      <!-- 服务地点 -->
      <view class="form-card">
        <view class="form-title">
          <text>服务地点</text>
          <text class="optional">(选填)</text>
        </view>
        <textarea 
          v-model="location" 
          placeholder="请输入服务地点，如具体的医院名称、地址等..." 
          class="location-input"
          maxlength="255"
        ></textarea>
        <view class="char-count">{{location.length}}/255</view>
      </view>

      <!-- 备注信息 -->
      <view class="form-card">
        <view class="form-title">
          <text>备注信息</text>
          <text class="optional">(选填)</text>
        </view>
        <textarea 
          v-model="remarks" 
          placeholder="请输入特殊需求或备注信息..." 
          class="remarks-input"
          maxlength="200"
        ></textarea>
        <view class="char-count">{{remarks.length}}/200</view>
      </view>

      <!-- 提交按钮 -->
      <button class="submit-btn" @click="submitAppointment">提交预约</button>
    </view>
  </view>
</template>

<script>
import { getCurrentUserId } from '@/utils/auth.js'
import request from '@/utils/request.js'

export default {
  data() {
    return {
      appointmentTypes: [
        { id: 'doctor', name: '医生问诊' },
        { id: 'nurse', name: '护理服务' },  
        { id: 'rehab', name: '康复指导' },
        { id: 'therapy', name: '心理治疗' },
        { id: 'other', name: '其他服务' }
      ],
      typeIndex: 0,
      serviceContent: '', // 服务内容
      appointmentDate: this.getDefaultDate(),
      minDate: this.getDefaultDate(), // 最小日期为今天
      startTime: '', // 开始时间（显示用，格式：HH:mm）
      endTime: '', // 结束时间（显示用，格式：HH:mm）
      startTimeValue: '09:00', // 开始时间值（绑定到选择器，默认9点）
      endTimeValue: '10:00', // 结束时间值（绑定到选择器，默认10点）
      location: '', // 服务地点
      remarks: ''
    }
  },
  methods: {
    goBack() {
      uni.navigateBack();
    },
    
    // 打开开始时间选择器
    openStartTimePicker() {
      // 如果已有时间，使用当前时间
      if (this.startTime) {
        this.startTimeValue = this.startTime;
      }
      this.$refs.startTimePicker.open();
      console.log('📅 打开开始时间选择器');
    },
    
    // 打开结束时间选择器
    openEndTimePicker() {
      // 如果已有时间，使用当前时间
      if (this.endTime) {
        this.endTimeValue = this.endTime;
      } else if (this.startTime) {
        // 如果有开始时间，默认为开始时间+1小时
        const [hour, minute] = this.startTime.split(':').map(Number);
        const endHour = hour + 1 >= 24 ? 23 : hour + 1;
        this.endTimeValue = `${String(endHour).padStart(2, '0')}:${String(minute).padStart(2, '0')}`;
      }
      this.$refs.endTimePicker.open();
      console.log('📅 打开结束时间选择器');
    },
    
    // 确认选择开始时间
    confirmStartTime(e) {
      this.startTime = e.value;
      console.log('✅ 确认开始时间:', this.startTime);
      this.validateTime();
      
      // 震动反馈
      uni.vibrateShort();
    },
    
    // 取消选择开始时间
    cancelStartTime() {
      console.log('❌ 取消选择开始时间');
    },
    
    // 确认选择结束时间
    confirmEndTime(e) {
      this.endTime = e.value;
      console.log('✅ 确认结束时间:', this.endTime);
      this.validateTime();
      
      // 震动反馈
      uni.vibrateShort();
    },
    
    // 取消选择结束时间
    cancelEndTime() {
      console.log('❌ 取消选择结束时间');
    },
    
    // 获取默认日期（今天）
    getDefaultDate() {
      const date = new Date();
      const year = date.getFullYear();
      const month = (date.getMonth() + 1).toString().padStart(2, '0');
      const day = date.getDate().toString().padStart(2, '0');
      return `${year}-${month}-${day}`;
    },
    
    // 格式化日期显示
    formatDate(dateStr) {
      if (!dateStr) return '请选择日期';
      const date = new Date(dateStr);
      const weekdays = ['周日', '周一', '周二', '周三', '周四', '周五', '周六'];
      const month = (date.getMonth() + 1).toString().padStart(2, '0');
      const day = date.getDate().toString().padStart(2, '0');
      return `${date.getFullYear()}年${month}月${day}日 ${weekdays[date.getDay()]}`;
    },
    
    // 选择预约类型
    bindTypeChange(e) {
      this.typeIndex = e.detail.value;
    },
    
    // 选择日期
    bindDateChange(e) {
      this.appointmentDate = e.detail.value;
      // 如果选择的是今天，需要验证时间
      this.validateTime();
    },

    // 选择开始时间
    bindStartTimeChange(e) {
      this.startTime = e.detail.value;
      this.validateTime();
    },

    // 选择结束时间
    bindEndTimeChange(e) {
      this.endTime = e.detail.value;
      this.validateTime();
    },

    // 验证时间逻辑
    validateTime() {
      // 验证开始时间
      if (this.startTime) {
        const startDateTime = new Date(`${this.appointmentDate} ${this.startTime}`);
        const now = new Date();
        
        // 如果是今天，检查时间不能早于当前时间
        if (this.appointmentDate === this.getDefaultDate() && startDateTime < now) {
          uni.showToast({ title: '预约时间不能早于当前时间', icon: 'none' });
          this.startTime = '';
          return;
        }
      }
      
      // 如果填写了结束时间，需要验证
      if (this.startTime && this.endTime) {
        const startDateTime = new Date(`${this.appointmentDate} ${this.startTime}`);
        const endDateTime = new Date(`${this.appointmentDate} ${this.endTime}`);
        
        // 检查结束时间必须晚于开始时间
        if (endDateTime <= startDateTime) {
          uni.showToast({ title: '结束时间必须晚于开始时间', icon: 'none' });
          this.endTime = '';
          return;
        }
        
        // 检查服务时间不能超过8小时
        const timeDiff = (endDateTime - startDateTime) / (1000 * 60 * 60);
        if (timeDiff > 8) {
          uni.showToast({ title: '单次服务时长不能超过8小时', icon: 'none' });
          this.endTime = '';
          return;
        }
      }
    },
    
    // 表单验证
    validateForm() {
      if (!this.serviceContent.trim()) {
        uni.showToast({ title: '请填写服务内容', icon: 'none' });
        return false;
      }
      
      if (!this.appointmentDate) {
        uni.showToast({ title: '请选择预约日期', icon: 'none' });
        return false;
      }
      
      if (!this.startTime) {
        uni.showToast({ title: '请选择开始时间', icon: 'none' });
        return false;
      }
      
      return true;
    },
    
    // 提交预约
    async submitAppointment() {
      // 验证表单
      if (!this.validateForm()) {
        return;
      }
      
      // 获取用户ID
      const userId = getCurrentUserId();
      if (!userId) {
        uni.showToast({ title: '请先登录', icon: 'none' });
        setTimeout(() => {
          uni.navigateTo({ url: '/pages/login/login' });
        }, 1500);
        return;
      }
      
      // 显示加载状态
      uni.showLoading({ title: '提交中...' });
      
      try {
        // 构造预约数据，将备注信息合并到服务内容中
        let fullContent = this.serviceContent.trim();
        if (this.remarks.trim()) {
          fullContent += '\n\n备注：' + this.remarks.trim();
        }
        
        const appointmentData = {
          elderId: userId,
          appointmentType: this.appointmentTypes[this.typeIndex].id,
          appointmentContent: fullContent,
          startTime: `${this.appointmentDate} ${this.startTime}:00`,
          endTime: this.endTime ? `${this.appointmentDate} ${this.endTime}:00` : null,
          location: this.location.trim() || null, // 地点信息（可选）
          status: 'pending'
        };
        
        console.log('提交预约数据:', appointmentData);
        
        // 调用后端API创建预约
        const result = await request.appointmentApi.createAppointment(appointmentData);
        console.log('提交预约结果:', result);
        
        uni.hideLoading();
        
        // 后端返回格式：{success: true/false, message: "消息", data: 数据}
        if (result.success) {
          // 跳转到预约结果页面，传递预约数据
          const appointmentDataStr = encodeURIComponent(JSON.stringify(result.data));
          uni.navigateTo({
            url: `/pages/user/appointment/result?appointmentData=${appointmentDataStr}`
          });
        } else {
          throw new Error(result.message || '预约创建失败');
        }
        
      } catch (error) {
        console.error('预约失败:', error);
        uni.hideLoading();
        uni.showToast({ 
          title: error.message || '预约提交失败，请重试', 
          icon: 'none' 
        });
      }
    }
  }
}
</script>

<style scoped>
.appointment-container {
  height: 100vh;
  display: flex;
  flex-direction: column;
  background: linear-gradient(to bottom, #f8f9fa 0%, #e9ecef 100%);
}

.content {
  flex: 1;
  padding: 30rpx;
  overflow-y: auto;
}

.form-card {
  background-color: #fff;
  border-radius: 20rpx;
  padding: 30rpx;
  margin-bottom: 20rpx;     /* 表单卡片之间的间距 */
  box-shadow: 0 4rpx 20rpx rgba(0, 0, 0, 0.08);
  border: 1rpx solid #f0f0f0;
  position: relative;
}



.form-title {
  display: flex;
  align-items: center;
  margin-bottom: 25rpx;
  font-size: 32rpx;
  font-weight: 600;
  color: #333;
}

.form-title text:first-child {
  margin-right: 8rpx;
}

.required {
  color: #ff4757;
  font-size: 28rpx;
  font-weight: normal;
}

.optional {
  color: #999;
  font-size: 24rpx;
  font-weight: normal;
  margin-left: 8rpx;
}

.picker-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 25rpx 20rpx;
  border-radius: 12rpx;
  background-color: #f8f9fa;
  border: 2rpx solid #e9ecef;
  transition: all 0.3s ease;
}

.picker-item:active {
  background-color: #e9ecef;
  border-color: #6ABF45;
  transform: scale(0.98);
}

.picker-item text {
  font-size: 30rpx;
  color: #333;
}

/* 时间选择按钮样式 */
.picker-item-elder {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 30rpx 25rpx;
  border-radius: 12rpx;
  background-color: #f8f9fa;
  border: 2rpx solid #e9ecef;
  transition: all 0.3s ease;
}

.picker-item-elder:active {
  background-color: #e9ecef;
  border-color: #ddd;
  transform: scale(0.98);
}

.picker-text-elder {
  font-size: 30rpx;
  color: #333;
}

.content-input, .location-input, .remarks-input {
  width: 100%;
  padding: 25rpx;
  background-color: #f8f9fa;
  border-radius: 12rpx;
  border: 2rpx solid #e9ecef;
  font-size: 30rpx;
  color: #333;
  line-height: 1.5;
  box-sizing: border-box;
  transition: border-color 0.3s ease;
}

.content-input {
  height: 200rpx;
  min-height: 120rpx;
}

.location-input {
  height: 120rpx;
  min-height: 80rpx;
}

.remarks-input {
  height: 150rpx;
  min-height: 100rpx;
}

.content-input:focus, .location-input:focus, .remarks-input:focus {
  border-color: #6ABF45;
  outline: none;
}

.char-count {
  text-align: right;
  font-size: 24rpx;
  color: #999;
  margin-top: 10rpx;
}

.submit-btn {
  background: linear-gradient(135deg, #6ABF45, #5aa83a);
  color: white;
  height: 100rpx;
  line-height: 100rpx;
  border-radius: 50rpx;
  font-size: 34rpx;
  font-weight: 600;
  margin: 40rpx 0 20rpx;
  box-shadow: 0 8rpx 20rpx rgba(106, 191, 69, 0.3);
  transition: all 0.3s ease;
  border: none;
}

.submit-btn:active {
  transform: translateY(2rpx);
  box-shadow: 0 4rpx 12rpx rgba(106, 191, 69, 0.4);
}

.submit-btn:disabled {
  background: #ccc;
  box-shadow: none;
  color: #999;
}

/* 表单动画效果 */
.form-card {
  animation: fadeInUp 0.6s ease-out;
}

@keyframes fadeInUp {
  from {
    opacity: 0;
    transform: translateY(30rpx);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

/* 时间项样式 */
.time-item {
  margin-bottom: 25rpx;
}

.time-item:last-child {
  margin-bottom: 0;
}

.time-label {
  display: flex;
  align-items: center;
  margin-bottom: 15rpx;
  font-size: 28rpx;
  color: #666;
  font-weight: 500;
}

.time-label text:first-child {
  margin-right: 8rpx;
}

/* 响应式调整 */
@media (max-width: 750rpx) {
  .content {
    padding: 20rpx;
  }
  
  .form-card {
    padding: 25rpx;
    margin-bottom: 25rpx;
  }
  
  .form-title {
    font-size: 30rpx;
  }
  
  .time-label {
    font-size: 26rpx;
  }
}
</style>