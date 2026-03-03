<template>
  <view class="calendar-container">
    <!-- 头部月份导航 -->
    <view class="calendar-header">
      <view class="arrow-btn" @click="prevMonth">
        <uni-icons type="arrowleft" size="24" color="#333"></uni-icons>
      </view>
      <view class="month-title">{{ currentYear }}年{{ currentMonth + 1 }}月</view>
      <view class="arrow-btn" @click="nextMonth">
        <uni-icons type="arrowright" size="24" color="#333"></uni-icons>
      </view>
    </view>
    
    <!-- 星期标题 -->
    <view class="week-header">
      <view class="week-day" v-for="day in weekDays" :key="day">{{ day }}</view>
    </view>
    
    <!-- 日历内容 -->
    <view class="calendar-body">
      <view 
        class="calendar-day" 
        v-for="(day, index) in days" 
        :key="index"
        :class="{
          'current-month': day.isCurrentMonth,
          'today': day.isToday,
          'selected': day.isSelected,
          'has-event': day.hasEvent
        }"
        @click="selectDay(day)"
      >
        <view class="day-number">{{ day.day }}</view>
        <view class="event-mark" v-if="day.hasEvent"></view>
      </view>
    </view>
    
    <!-- 日程列表 -->
    <view class="event-list">
      <view class="section-title">
        <text>{{ selectedDate.getMonth() + 1 }}月{{ selectedDate.getDate() }}日日程</text>
        <text class="event-count" v-if="events.length > 0">({{ events.length }})</text>
      </view>
      <view v-if="events.length === 0" class="no-events">
        <image src="/static/images/no-data.png" mode="aspectFit" class="empty-image"></image>
        <text class="empty-text">暂无日程安排</text>
      </view>
      <scroll-view scroll-y class="event-scroll" v-else>
        <view 
          class="event-item" 
          v-for="(event, index) in events" 
          :key="index"
          :style="{borderLeftColor: getEventColor(event.type)}"
        >
          <view class="event-time">{{ event.startTime }} - {{ event.endTime }}</view>
          <view class="event-title">{{ event.title }}</view>
          <view class="event-footer">
            <view class="event-location">
              <uni-icons type="location" size="14" color="#999"></uni-icons>
              {{ event.location }}
            </view>
            <view class="event-type" :style="{backgroundColor: getEventColor(event.type)}">
              {{ getEventTypeName(event.type) }}
            </view>
            <!-- 日程项中的取消按钮 -->
            <button class="cancel-btn" @click="openCancelConfirm(event)">取消</button>
          </view>
        </view>
      </scroll-view>
    </view>
    
    <!-- 取消日程确认弹窗 -->
    <uni-popup ref="cancelDialog" type="dialog" :mask-click="false">
      <view class="custom-dialog">
        <view class="dialog-header">
          <text class="dialog-title">确认取消</text>
          <uni-icons type="closeempty" size="24" color="#999" @click="closeCancelDialog"></uni-icons>
        </view>
        <scroll-view class="dialog-content">
          <view class="form-group">
            <view class="form-label">您确定要取消 "{{ eventToCancel?.title || '该日程' }}" 吗？</view>
          </view>
        </scroll-view>
        <view class="dialog-footer">
          <view class="btn cancel-btn" @click="closeCancelDialog">取消</view>
          <view class="btn confirm-btn" @click="confirmCancelEvent">确定</view>
        </view>
      </view>
    </uni-popup>
  </view>
</template>

<script>
import config from '@/utils/config.js'

export default {
  data() {
    return {
      weekDays: ['日', '一', '二', '三', '四', '五', '六'],
      currentDate: new Date(),
      selectedDate: new Date(),
      // 日程数据模型（适配后端接口字段）
      newEvent: {
        title: '',
        date: '',
        startTime: '',
        endTime: '',
        location: '',
        typeIndex: 0, // 类型选择器索引
        type: 1, // 后端需要的数字类型（1=doctor，2=nurse...）
        typeName: '医疗', // 前端显示的中文名称
        description: ''
      },
      // 与后端convertType方法匹配的类型映射表
      eventTypes: [
        { id: 1, name: '医疗', code: 'doctor' },
        { id: 2, name: '医护', code: 'nurse' },
        { id: 3, name: '复建', code: 'rehab' },
        { id: 4, name: '诊疗', code: 'therapy' },
        { id: 5, name: '其他', code: 'other' }
      ],
      events: [], // 存储后端返回的日程列表（type为数字）
      days: [], // 日历天数数据
      formErrors: {}, // 表单验证错误
      eventToCancel: null // 待取消的日程
    }
  },
  computed: {
    currentYear() {
      return this.currentDate.getFullYear()
    },
    currentMonth() {
      return this.currentDate.getMonth()
    }
  },
  created() {
    // 初始化时检查登录状态（仅依赖userInfo）
    this.checkLoginStatus()
    this.generateCalendar()
    this.fetchEvents()
  },
  methods: {
    // 检查登录状态：仅通过本地存储的userInfo判断
    checkLoginStatus() {
      const userInfo = uni.getStorageSync('userInfo')
      if (!userInfo || !userInfo.id) {
        uni.showToast({ title: '请先登录', icon: 'none' })
        setTimeout(() => uni.navigateTo({ url: '/pages/volunteer/login' }), 1500)
      }
    },

    // 生成日历数据
    generateCalendar() {
      this.days = []
      const year = this.currentYear
      const month = this.currentMonth
      
      // 当月第一天/最后一天
      const firstDay = new Date(year, month, 1)
      const lastDay = new Date(year, month + 1, 0)
      // 上个月最后一天/下个月需补充的天数
      const prevLastDay = new Date(year, month, 0)
      const nextDays = 7 - lastDay.getDay() - 1
      
      // 1. 填充上个月剩余天数
      for (let i = firstDay.getDay(); i > 0; i--) {
        const day = prevLastDay.getDate() - i + 1
        this.days.push({
          day,
          isCurrentMonth: false,
          isToday: false,
          isSelected: false,
          hasEvent: false,
          date: new Date(year, month - 1, day)
        })
      }
      
      // 2. 填充当月天数（核心）
      for (let i = 1; i <= lastDay.getDate(); i++) {
        const date = new Date(year, month, i)
        this.days.push({
          day: i,
          isCurrentMonth: true,
          isToday: this.isSameDay(date, new Date()), // 判断是否为今天
          isSelected: this.isSameDay(date, this.selectedDate), // 判断是否为选中日期
          hasEvent: this.checkHasEvent(date), // 判断是否有日程
          date: date
        })
      }
      
      // 3. 填充下个月补充天数
      for (let i = 1; i <= nextDays; i++) {
        this.days.push({
          day: i,
          isCurrentMonth: false,
          isToday: false,
          isSelected: false,
          hasEvent: false,
          date: new Date(year, month + 1, i)
        })
      }
    },

    // 检查某天是否有日程（对比日期）
    checkHasEvent(date) {
      return this.events.some(event => this.isSameDay(new Date(event.date), date))
    },

    // 判断两个日期是否为同一天
    isSameDay(date1, date2) {
      return date1.getFullYear() === date2.getFullYear() &&
             date1.getMonth() === date2.getMonth() &&
             date1.getDate() === date2.getDate()
    },

    // 选择日期：更新选中状态+重新拉取日程
    selectDay(day) {
      this.selectedDate = day.date
      this.generateCalendar() // 刷新日历选中样式
      this.fetchEvents() // 拉取选中日期的日程
    },

    // 切换到上个月
    prevMonth() {
      this.currentDate = new Date(this.currentYear, this.currentMonth - 1, 1)
      this.generateCalendar()
    },

    // 切换到下个月
    nextMonth() {
      this.currentDate = new Date(this.currentYear, this.currentMonth + 1, 1)
      this.generateCalendar()
    },

    // 从后端获取日程列表（仅传userInfo中的userId，无token）
    fetchEvents() {
      // 1. 获取userInfo中的userId（核心鉴权字段）
      const userInfo = uni.getStorageSync('userInfo')
      const userId = userInfo?.id
      if (!userId) {
        uni.showToast({ title: '请先登录', icon: 'none' })
        setTimeout(() => uni.navigateTo({ url: '/pages/volunteer/login' }), 1500)
        return
      }

      // 2. 格式化请求日期（后端要求的yyyy-MM-dd格式）
      const date = new Date(this.selectedDate)
      const targetDate = `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`

      // 3. 调用后端接口（无token，仅传userId和日期）
      uni.request({
        url: `${config.API_BASE_URL}/api/volunteer/appointment/list`,
        method: 'GET',
        data: { 
          userId: userId, // 仅用userInfo中的userId鉴权
          targetDate: targetDate 
        },
        header: {
          'Content-Type': 'application/json' // 移除Authorization头（无token）
        },
        success: (res) => {
          if (res.statusCode === 200 && res.data.success) {
            // 接收后端返回的日程列表（type为数字类型）
            this.events = res.data.events || []
            this.generateCalendar() // 刷新日历的日程标记
          } else {
            // 处理后端返回的错误（如用户不存在、无权限等）
            uni.showToast({ title: res.data?.message || '获取日程失败', icon: 'none' })
            this.events = []
          }
        },
        fail: (err) => {
          console.error('获取日程失败：', err)
          uni.showToast({ title: '网络错误，请重试', icon: 'none' })
          this.events = []
        }
      })
    },

    // 根据后端返回的数字类型，显示对应的中文名称
    getEventTypeName(type) {
      const matchType = this.eventTypes.find(item => item.id === type)
      return matchType ? matchType.name : '未知类型'
    },

    // 根据日程类型设置颜色（视觉区分）
    getEventColor(type) {
      const colorMap = {
        1: '#67C23A', // 医生-绿色
        2: '#409EFF', // 护士-蓝色
        3: '#E6A23C', // 康复师-橙色
        4: '#F56C6C', // 治疗师-红色
        5: '#909399'  // 其他-灰色
      }
      return colorMap[type] || '#909399'
    },

    // 打开取消日程的确认弹窗
    openCancelConfirm(event) {
      this.eventToCancel = event // 暂存要取消的日程
      this.$refs.cancelDialog.open()
    },

    // 关闭取消日程弹窗
    closeCancelDialog() {
      this.eventToCancel = null // 清空暂存的日程
      this.$refs.cancelDialog.close()
    },

    // 确认取消日程（调用后端接口，仅传userId）
    confirmCancelEvent() {
      if (!this.eventToCancel) return;
    
      const userInfo = uni.getStorageSync('userInfo');
      const userId = userInfo?.id;
      if (!userId) {
        uni.showToast({ title: '请先登录', icon: 'none' });
        this.closeCancelDialog();
        return;
      }
    
      uni.request({
        // 注意：这里的 URL 要带上 userId 查询参数
        url: `${config.API_BASE_URL}/api/volunteer/appointment/cancel/${this.eventToCancel.id}?userId=${userId}`,
        method: 'POST', // 与后端 @PostMapping 匹配
        header: {
          'Content-Type': 'application/json'
        },
        success: (res) => {
          if (res.data.success) {
            uni.showToast({ title: '取消成功', icon: 'success' });
            this.fetchEvents(); // 刷新日程列表
          } else {
            uni.showToast({ title: res.data.message || '取消失败', icon: 'none' });
          }
        },
        fail: (err) => {
          console.error('取消失败：', err);
          uni.showToast({ title: '网络错误，请重试', icon: 'none' });
        },
        complete: () => {
          this.closeCancelDialog();
        }
      });
    }
  }
}
</script>

<style scoped>
.calendar-container {
  padding: 20rpx;
  background-color: #f7f7f7;
  height: 100vh;
  display: flex;
  flex-direction: column;
}

.calendar-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20rpx 0;
  background-color: #fff;
  border-radius: 16rpx;
  margin-bottom: 20rpx;
  box-shadow: 0 2rpx 12rpx rgba(0, 0, 0, 0.05);
}

.month-title {
  flex: 1;
  text-align: center;
  font-size: 36rpx;
  font-weight: bold;
  color: #333;
}

.arrow-btn {
  padding: 20rpx 40rpx;
}

.week-header {
  display: flex;
  margin-bottom: 10rpx;
  background-color: #fff;
  border-radius: 16rpx;
  padding: 10rpx 0;
  box-shadow: 0 2rpx 12rpx rgba(0, 0, 0, 0.05);
}

.week-day {
  flex: 1;
  text-align: center;
  font-size: 28rpx;
  color: #666;
  padding: 10rpx 0;
}

.calendar-body {
  display: flex;
  flex-wrap: wrap;
  background-color: #fff;
  border-radius: 16rpx;
  padding: 10rpx 0;
  margin-bottom: 20rpx;
  box-shadow: 0 2rpx 12rpx rgba(0, 0, 0, 0.05);
}

.calendar-day {
  width: calc(100% / 7);
  height: 100rpx;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  position: relative;
}

.calendar-day.current-month {
  background-color: #fff;
}

.calendar-day:not(.current-month) {
  background-color: #fff;
  color: #ccc;
}

.day-number {
  font-size: 32rpx;
  font-weight: 500;
}

.today .day-number {
  color: #409EFF;
  font-weight: bold;
}

.selected {
  background-color: #ecf5ff !important;
  border-radius: 50%;
}

.selected .day-number {
  color: #409EFF;
  font-weight: bold;
}

.event-mark {
  width: 12rpx;
  height: 12rpx;
  border-radius: 50%;
  background-color: #F56C6C;
  margin-top: 5rpx;
}

.event-list {
  flex: 1;
  background-color: #fff;
  border-radius: 16rpx;
  padding: 20rpx;
  box-shadow: 0 2rpx 12rpx rgba(0, 0, 0, 0.05);
}

.section-title {
  font-size: 32rpx;
  font-weight: bold;
  padding: 10rpx 0 20rpx;
  color: #333;
  display: flex;
  align-items: center;
}

.event-count {
  font-size: 24rpx;
  color: #999;
  margin-left: 10rpx;
}

.no-events {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60rpx 0;
}

.empty-image {
  width: 200rpx;
  height: 200rpx;
  margin-bottom: 20rpx;
}

.empty-text {
  font-size: 28rpx;
  color: #999;
}

.event-scroll {
  max-height: 500rpx;
}

.event-item {
  padding: 20rpx;
  margin-bottom: 20rpx;
  background-color: #f9f9f9;
  border-radius: 8rpx;
  border-left-width: 8rpx;
  border-left-style: solid;
  transition: all 0.3s ease;
  position: relative; /* 为取消按钮定位 */
}

.event-item:hover {
  transform: translateX(5rpx);
}

.event-time {
  font-size: 24rpx;
  color: #666;
  margin-bottom: 10rpx;
}

.event-title {
  font-size: 30rpx;
  font-weight: bold;
  margin-bottom: 10rpx;
  color: #333;
}

.event-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 10rpx;
}

.event-location {
  font-size: 24rpx;
  color: #999;
  display: flex;
  align-items: center;
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.event-type {
  font-size: 20rpx;
  color: #fff;
  padding: 4rpx 12rpx;
  border-radius: 20rpx;
  margin: 0 10rpx;
}

.add-button {
  position: fixed;
  right: 40rpx;
  bottom: 80rpx;
  width: 100rpx;
  height: 100rpx;
  background-color: #409EFF;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 4rpx 12rpx rgba(64, 158, 255, 0.3);
  z-index: 1000;
  transition: all 0.3s ease;
}

.add-button:hover {
  transform: scale(1.1);
}

/* 弹窗样式 */
.custom-dialog {
  width: 86vw;
  max-width: 700rpx;
  background: #FFFFFF;
  border-radius: 24rpx;
  overflow: hidden;
  box-shadow: 0 10rpx 40rpx rgba(0, 0, 0, 0.15);
}

.dialog-header {
  padding: 32rpx;
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: linear-gradient(to right, #f5f7fa, #e8ebf0);
  border-bottom: 1rpx solid #eee;
}

.dialog-title {
  font-size: 36rpx;
  font-weight: 600;
  color: #333;
}

.close-icon {
  padding: 8rpx;
  border-radius: 50%;
  transition: background-color 0.3s;
}

.close-icon:active {
  background-color: rgba(0, 0, 0, 0.05);
}

.dialog-content {
  max-height: 60vh;
  padding: 0 32rpx;
}

.form-group {
  margin-bottom: 32rpx;
}

.form-label {
  font-size: 28rpx;
  color: #606266;
  margin-bottom: 16rpx;
  display: block;
  font-weight: 500;
}

.form-label.required::after {
  content: "*";
  color: #F56C6C;
  margin-left: 4rpx;
}

.form-control input,
.form-control textarea,
.time-picker,
.type-picker {
  width: 100%;
  padding: 20rpx 24rpx;
  background: #f8f9fa;
  border-radius: 12rpx;
  font-size: 28rpx;
  border: 1rpx solid #e4e7ed;
  transition: all 0.3s;
  box-sizing: border-box;
}

.form-control input:focus,
.form-control textarea:focus,
.time-picker:active,
.type-picker:active {
  border-color: #409EFF;
  background: #fff;
  box-shadow: 0 0 0 2rpx rgba(64, 158, 255, 0.2);
}

.form-control textarea {
  height: 160rpx;
  line-height: 1.6;
}

.time-picker-group {
  display: flex;
  align-items: center;
  gap: 16rpx;
}

.time-picker,
.type-picker {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.time-separator {
  color: #909399;
  font-size: 28rpx;
}

.error-message {
  color: #F56C6C;
  font-size: 24rpx;
  margin-top: 8rpx;
  display: block;
}

.form-control input.error {
  border-color: #F56C6C;
}

.dialog-footer {
  display: flex;
  border-top: 1rpx solid #f0f2f5;
}

.btn {
  flex: 1;
  text-align: center;
  padding: 24rpx;
  font-size: 32rpx;
  font-weight: 500;
  transition: all 0.3s;
}

.cancel-btn {
  color: #606266;
  border-right: 1rpx solid #f0f2f5;
}

.cancel-btn:active {
  background: rgba(0, 0, 0, 0.02);
}

.confirm-btn {
  color: #FFFFFF;
  background: linear-gradient(to right, #409EFF, #64B5FF);
}

.confirm-btn:active {
  background: linear-gradient(to right, #3a8be6, #5ca5f0);
}

.placeholder {
  color: #c0c4cc;
}

/* 日程项中的取消按钮 */
.event-item .cancel-btn {
  position: absolute;
  right: 20rpx;
  top: 20rpx;
  padding: 6rpx 14rpx;
  font-size: 22rpx;
  border-radius: 6rpx;
  background-color: #F56C6C;
  color: white;
  border: none;
  z-index: 1;
}

.event-item .cancel-btn:active {
  background-color: #e45a5a;
}
</style>
