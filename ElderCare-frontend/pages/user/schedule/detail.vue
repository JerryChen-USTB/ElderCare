<template>
  <!-- 页面结构 -->
  <view class="schedule-container">
    <!-- 固定的日程类型筛选栏 -->
    <view class="filter-tabs-fixed">
      <view class="filter-tabs-container">
        <!-- 第一行筛选标签 -->
        <view class="filter-tabs filter-tabs-row">
          <view 
            class="filter-tab" 
            :class="{ active: currentType === 'all' }"
            @click="handleFilterChange('all')"
          >
            全部
          </view>
          <view 
            class="filter-tab" 
            :class="{ active: currentType === 'medicine' }"
            @click="handleFilterChange('medicine')"
          >
            服药
          </view>
          <view 
            class="filter-tab" 
            :class="{ active: currentType === 'doctor' }"
            @click="handleFilterChange('doctor')"
          >
            就医
          </view>
          <view 
            class="filter-tab" 
            :class="{ active: currentType === 'exercise' }"
            @click="handleFilterChange('exercise')"
          >
            运动
          </view>
        </view>
        
        <!-- 第二行筛选标签 -->
        <view class="filter-tabs filter-tabs-row">
          <view 
            class="filter-tab" 
            :class="{ active: currentType === 'meal' }"
            @click="handleFilterChange('meal')"
          >
            饮食
          </view>
          <view 
            class="filter-tab" 
            :class="{ active: currentType === 'sleep' }"
            @click="handleFilterChange('sleep')"
          >
            睡觉
          </view>
          <view 
            class="filter-tab" 
            :class="{ active: currentType === 'appointment' }"
            @click="handleFilterChange('appointment')"
          >
            预约
          </view>
          <view 
            class="filter-tab" 
            :class="{ active: currentType === 'other' }"
            @click="handleFilterChange('other')"
          >
            其他
          </view>
        </view>
      </view>
    </view>

    <!-- 日程列表容器 -->
    <view class="schedule-content">
      <!-- 日程列表 -->
    <scroll-view 
      class="schedule-list"
      scroll-y
      enable-back-to-top
      :enable-flex="true"
      :scroll-with-animation="true"
      :enhanced="true"
      :bounces="false"
      :show-scrollbar="false"
      :fast-deceleration="false"
      :enable-passive="false"
      :refresher-enabled="false"
      :scroll-into-view="scrollToView"
    >
      <!-- 按天分组的日程卡片 -->
      <view 
        v-for="(group, groupIndex) in groupedScheduleList" 
        :key="group.date"
        :id="getDateGroupId(group.date)"
        class="date-group"
      >
        <!-- 日期卡片头部 -->
        <view class="date-header" :class="{ 'date-header-past': isDatePast(group.date) }">
          <text class="date-text" :class="{ 'text-past': isDatePast(group.date) }">{{ getFormattedDate(group.date) }}</text>
          <text class="weekday-text" :class="{ 'text-past': isDatePast(group.date) }">{{ getWeekday(group.date) }}</text>
        </view>
        
        <!-- 当天的事件列表 -->
        <view class="date-card">
          <view 
            v-for="(item, itemIndex) in group.items" 
            :key="item.id"
            class="event-row"
            :class="{ 
              'item-medicine': item.type === 'medicine', 
              'item-doctor': item.type === 'doctor', 
              'item-exercise': item.type === 'exercise',
              'item-meal': item.type === 'meal',
              'item-sleep': item.type === 'sleep',
              'item-appointment': item.type === 'appointment',
              'item-other': item.type === 'other',
              'event-past': isSchedulePast(item)
            }"
            @click="viewSchedule(item.id)"
            @touchstart="handleTouchStart($event, getGlobalIndex(groupIndex, itemIndex))"
            @touchmove="handleTouchMove"
            @touchend="handleTouchEnd"
          >
            <!-- 左侧时间 -->
            <view class="event-time">
              <text class="time-text" :class="{ 'text-past': isSchedulePast(item) }">{{ item.time }}</text>
            </view>
            
            <!-- 右侧事件信息 -->
            <view class="event-info">
              <view class="event-content">
                <text class="content-text" :class="{ 'text-past': isSchedulePast(item) }">{{ truncateContent(item.content, 10) }}</text>
              </view>
              <view class="event-location">
                <text class="location-text" :class="{ 'text-past': isSchedulePast(item) }">{{ item.location }}</text>
              </view>
            </view>
            
            <!-- 完成状态图标 -->
            <view class="status-icon-container">
              <image 
                :src="getStatusIcon(item)" 
                mode="aspectFit" 
                class="status-icon"
                :class="{ 'non-clickable': item.type === 'appointment' }"
                @click.stop="item.type !== 'appointment' ? toggleStatus(getGlobalIndex(groupIndex, itemIndex)) : null"
              ></image>
            </view>
          </view>
        </view>
      </view>
      
      <!-- 空状态 -->
      <view v-if="groupedScheduleList.length === 0" class="empty-state">
        <image src="/static/empty.png" mode="aspectFit" class="empty-icon"></image>
        <text class="empty-text">暂无日程安排</text>
      </view>
      
      <!-- 底部占位空间，为悬浮按钮预留位置 -->
      <view class="bottom-spacer"></view>
    </scroll-view>
    </view>

    <!-- 添加按钮 -->
    <view class="add-btn" @click="handleAdd">
      <text>+ 新建日程</text>
    </view>

    <!-- 确认删除弹窗 -->
    <view v-if="showDeleteModal" class="modal-mask">
      <view class="modal-container">
        <text class="modal-content">确定删除该日程吗？</text>
        <view class="modal-buttons">
          <view class="modal-button cancel" @click="showDeleteModal = false">取消</view>
          <view class="modal-button confirm" @click="confirmDelete">确认</view>
        </view>
      </view>
    </view>

    <!-- 重复日程删除选择弹窗 -->
    <view v-if="showRepeatDeleteModal" class="modal-mask">
      <view class="modal-container repeat-delete-modal">
        <text class="modal-title">删除重复日程</text>
        <text class="modal-content">
          该日程为"{{ getRepeatTypeName(repeatDeleteSchedule && repeatDeleteSchedule.repeatType) }}"类型，请选择删除方式：
        </text>
        <view class="modal-buttons vertical">
          <view class="modal-button primary" @click="deleteCurrentOnly">仅删除当前日程</view>
          <view class="modal-button danger" @click="deleteAllRepeats">删除所有同类日程</view>
          <view class="modal-button cancel" @click="cancelRepeatDelete">取消</view>
        </view>
      </view>
    </view>

    <!-- 确认取消预约弹窗 -->
    <view v-if="showCancelAppointmentModal" class="modal-mask">
      <view class="modal-container">
        <text class="modal-title">取消预约</text>
        <text class="modal-content">确定要取消这个志愿者服务预约吗？</text>
        <view class="modal-buttons">
          <view class="modal-button cancel" @click="showCancelAppointmentModal = false">取消</view>
          <view class="modal-button danger" @click="confirmCancelAppointment">确认取消</view>
        </view>
      </view>
    </view>

    <!-- 日程详情弹窗 -->
    <view v-if="showDetailModal" class="detail-mask" :class="{ 'mask-show': showDetailModal }" @click="closeDetail">
      <view class="detail-card" :class="{ 'card-show': showDetailModal }" @click.stop>
        <view class="card-header">
          <text class="card-title">日程详情</text>
        </view>
        
        <view class="card-content">
          <view class="info-row">
            <text class="info-label">类型</text>
            <text class="info-value" :class="{ 'text-past': selectedSchedule && isSchedulePast(selectedSchedule) }">{{ getTypeName(selectedSchedule.type) }}</text>
          </view>
          <view class="info-row">
            <text class="info-label">时间</text>
            <text class="info-value" :class="{ 'text-past': selectedSchedule && isSchedulePast(selectedSchedule) }">{{ selectedSchedule.time }}</text>
          </view>
          <view class="info-row">
            <text class="info-label">内容</text>
            <text class="info-value" :class="{ 'text-past': selectedSchedule && isSchedulePast(selectedSchedule) }">{{ selectedSchedule.content }}</text>
          </view>
          <view class="info-row">
            <text class="info-label">地点</text>
            <text class="info-value" :class="{ 'text-past': selectedSchedule && isSchedulePast(selectedSchedule) }">{{ selectedSchedule.location }}</text>
          </view>
          <!-- 预约类型显示志愿者ID -->
          <view v-if="selectedSchedule && selectedSchedule.type === 'appointment'" class="info-row">
            <text class="info-label">志愿者</text>
            <text class="info-value" :class="{ 'text-past': selectedSchedule && isSchedulePast(selectedSchedule) }">
              {{ selectedSchedule.volunteerId ? `志愿者ID: ${selectedSchedule.volunteerId}` : '待分配志愿者' }}
            </text>
          </view>
          <view class="info-row">
            <text class="info-label">状态</text>
            <text class="info-value" :class="{ 'text-past': selectedSchedule && isSchedulePast(selectedSchedule) }">
              {{ getStatusName(selectedSchedule.type === 'appointment' ? selectedSchedule.appointmentStatus : selectedSchedule.status, selectedSchedule.type) }}
              <text v-if="selectedSchedule && isSchedulePast(selectedSchedule)" class="past-indicator"> (已过期)</text>
            </text>
          </view>
          <view v-if="selectedSchedule && selectedSchedule.type !== 'appointment'" class="info-row">
            <text class="info-label">重复</text>
            <text class="info-value" :class="{ 'text-past': selectedSchedule && isSchedulePast(selectedSchedule) }">{{ getRepeatTypeName(selectedSchedule.repeatType) }}</text>
          </view>
        </view>
        
        <view class="card-footer">
          <!-- 预约类型的按钮 -->
          <view v-if="selectedSchedule && selectedSchedule.type === 'appointment'" class="button-row single-button">
            <view class="action-btn cancel-appointment-btn" @click="handleCancelAppointment">
              <text>取消预约</text>
            </view>
          </view>
          <!-- 普通日程的按钮 - 分两层显示 -->
          <template v-else>
            <!-- 第一层：编辑和删除按钮 -->
            <view class="button-row">
              <view class="action-btn edit-btn" @click="handleDetailEdit">
                <text>编辑日程</text>
              </view>
              <view class="action-btn delete-btn" @click="handleDetailDelete">
                <text>删除日程</text>
              </view>
            </view>
            <!-- 第二层：完成状态按钮 -->
            <view class="button-row single-button">
              <view class="action-btn complete-btn" @click="handleDetailToggleStatus">
                <text>{{ selectedSchedule.status === 'completed' ? '标记为未完成' : '标记为已完成' }}</text>
              </view>
            </view>
          </template>
        </view>
      </view>
    </view>
  </view>
</template>

<script>
import request from '@/utils/request.js';
import { getCurrentUserId as getAuthUserId, requireLogin } from '@/utils/auth.js';

export default {
  data() {
    return {
      scheduleList: [],
      currentType: 'all',
      showDeleteModal: false,
      deleteIndex: -1,
      refreshStatus: false,
      selectedSchedule: null,
      showDetailModal: false,  // 控制详情弹窗显示/隐藏
      scrollToView: '',  // 控制滚动到指定位置
      showRepeatDeleteModal: false, // 控制重复日程删除选择弹窗
      repeatDeleteSchedule: null, // 要删除的重复日程
      showCancelAppointmentModal: false, // 控制取消预约确认弹窗
      appointmentToCancel: null, // 要取消的预约日程
      // 长按检测相关
      touchStartTime: 0,
      touchStartX: 0,
      touchStartY: 0,
      touchCurrentIndex: -1,
      longPressTimer: null,
      hasMove: false,
      longPressThreshold: 800 // 长按触发时间阈值(毫秒)
    };
  },
  computed: {
    filteredScheduleList() {
      let filtered = this.currentType === 'all' 
        ? this.scheduleList 
        : this.scheduleList.filter(item => item.type === this.currentType);
      return filtered;
    },
    
    // 按日期分组的日程
    groupedScheduleList() {
      const groups = {};
      
      this.filteredScheduleList.forEach(item => {
        if (!groups[item.date]) {
          groups[item.date] = [];
        }
        groups[item.date].push(item);
      });
      
      // 按日期排序，每天内按时间排序
      const sortedGroups = Object.keys(groups)
        .sort()
        .map(date => ({
          date,
          items: groups[date].sort((a, b) => a.time.localeCompare(b.time))
        }));
      
      return sortedGroups;
    }
  },
  watch: {
    // 监听筛选类型变化，自动滚动到今天
    currentType: {
      handler(newType, oldType) {
        if (oldType !== undefined) { // 排除初始化时的调用
          this.$nextTick(() => {
            setTimeout(() => {
              this.scrollToToday();
            }, 300); // 等待DOM更新完成后再滚动
          });
        }
      }
    }
  },
  methods: {
    // 获取类型图标
    getTypeIcon(type) {
      const icons = {
        'medicine': '/static/medicine.png',
        'doctor': '/static/doctor.png',
        'exercise': '/static/exercise.png',
        'meal': '/static/meal.png',
        'sleep': '/static/sleep.png',
        'other': '/static/other.png'
      };
      return icons[type] || '/static/other.png';
    },
    
    // 获取类型名称
    getTypeName(type) {
      const names = {
        'medicine': '服药',
        'doctor': '就医',
        'exercise': '运动',
        'meal': '饮食',
        'sleep': '睡觉',
        'appointment': '预约',
        'other': '其他'
      };
      return names[type] || type;
    },
    
    // 获取状态名称
    getStatusName(status, type) {
      if (type === 'appointment') {
        // 预约状态名称
        const appointmentNames = {
          'pending': '待接单',
          'confirmed': '已确认',
          'completed': '已完成',
          'canceled': '已取消',
          'time_out': '已超时',
          'no_show': '爽约'
        };
        return appointmentNames[status] || status || '待接单';
      } else {
        // 普通日程状态名称
        const names = {
          'pending': '未完成',
          'completed': '已完成',
          'canceled': '已取消',
          'overdue': '已过期'
        };
        return names[status] || status || '未完成';
      }
    },
    
    // 获取重复类型名称
    getRepeatTypeName(repeatType) {
      const names = {
        'none': '一次性日程',
        'daily': '每天重复',
        'weekly': '每周重复',
        'monthly': '每月重复'
      };
      return names[repeatType] || repeatType || '一次性日程';
    },
    
    // 截取文本内容，超出指定长度用"..."代替
    truncateContent(content, maxLength = 10) {
      if (!content) return '';
      if (content.length <= maxLength) {
        return content;
      }
      return content.substring(0, maxLength) + '...';
    },
    
    // 获取格式化日期 (如 01/15)
    getFormattedDate(dateStr) {
      const date = new Date(dateStr);
      const month = String(date.getMonth() + 1).padStart(2, '0');
      const day = String(date.getDate()).padStart(2, '0');
      return `${month}/${day}`;
    },
    
    // 获取星期
    getWeekday(dateStr) {
      const date = new Date(dateStr);
      const weekdays = ['星期日', '星期一', '星期二', '星期三', '星期四', '星期五', '星期六'];
      return weekdays[date.getDay()];
    },
    
    // 获取在原数组中的全局索引（用于删除操作）
    getGlobalIndex(groupIndex, itemIndex) {
      let globalIndex = 0;
      for (let i = 0; i < groupIndex; i++) {
        globalIndex += this.groupedScheduleList[i].items.length;
      }
      globalIndex += itemIndex;
      return this.scheduleList.findIndex(item => 
        item.id === this.groupedScheduleList[groupIndex].items[itemIndex].id
      );
    },
    
    // 生成日期组的ID
    getDateGroupId(dateStr) {
      return `date-group-${dateStr}`;
    },
    
    // 滚动到今天的位置
    scrollToToday() {
      const today = this.getToday();
      const todayId = this.getDateGroupId(today);
      
      // 检查今天是否在当前筛选的列表中
      const todayExists = this.groupedScheduleList.some(group => group.date === today);
      
      if (todayExists) {
        this.scrollToView = todayId;
        // 重置滚动目标，以便下次能再次滚动到同一位置
        setTimeout(() => {
          this.scrollToView = '';
        }, 1000);
      } else {
        // 如果今天没有对应类型的日程，滚动到最近的日期
        this.scrollToNearestDate();
      }
    },
    
    // 滚动到最近的日期
    scrollToNearestDate() {
      if (this.groupedScheduleList.length === 0) return;
      
      const today = this.getToday();
      const todayTime = new Date(today).getTime();
      
      // 找到距离今天最近的日期
      let nearestGroup = this.groupedScheduleList[0];
      let minDiff = Math.abs(new Date(nearestGroup.date).getTime() - todayTime);
      
      this.groupedScheduleList.forEach(group => {
        const diff = Math.abs(new Date(group.date).getTime() - todayTime);
        if (diff < minDiff) {
          minDiff = diff;
          nearestGroup = group;
        }
      });
      
      const nearestId = this.getDateGroupId(nearestGroup.date);
      this.scrollToView = nearestId;
      
      // 重置滚动目标
      setTimeout(() => {
        this.scrollToView = '';
      }, 1000);
    },
    
    // 判断日程是否已过期
    isSchedulePast(item) {
      const currentDateTime = new Date(); // 获取系统当前时间
      const scheduleDateTime = new Date(`${item.date}T${item.time}:00`);
      return scheduleDateTime < currentDateTime;
    },
    
    // 判断整个日期是否已过期
    isDatePast(dateStr) {
      const currentDate = new Date();
      const scheduleDate = new Date(dateStr);
      // 只比较日期，不考虑时间
      currentDate.setHours(0, 0, 0, 0);
      scheduleDate.setHours(0, 0, 0, 0);
      return scheduleDate < currentDate;
    },
    
    // 获取状态图标路径
    getStatusIcon(item) {
      // 如果是预约类型，使用预约状态图标
      if (item.type === 'appointment') {
        return this.getAppointmentStatusIcon(item);
      }
      
      // 普通日程的状态图标
      if (item.status === 'completed') {
        return this.isSchedulePast(item) 
          ? '/static/icons/done-past.png' 
          : '/static/icons/done-unpast.png';
      } else {
        return '/static/icons/undone.png';
      }
    },
    
    // 获取预约状态图标
    getAppointmentStatusIcon(item) {
      // 根据预约状态返回对应图标（从实际的预约状态获取，而不是日程状态）
      const appointmentStatus = item.appointmentStatus || item.status;
      switch (appointmentStatus) {
        case 'pending':
          return '/static/elder/apointment_status_pendding.png';
        case 'confirmed':
          return '/static/elder/apointment_status_confirmed.png';
        case 'completed':
          return '/static/elder/apointment_status_completed.png';
        case 'canceled':
          return '/static/elder/apointment_status_canceled.png';
        case 'no_show':
          return '/static/elder/apointment_status_no_show.png';
        case 'time_out':
          return '/static/elder/apointment_status_timeout.png';
        default:
          return '/static/elder/apointment_status_pendding.png';
      }
    },
    
    // 获取今天的日期字符串
    getToday() {
      const today = new Date();

      // 使用本地时间而不是UTC时间，避免时区问题
      const year = today.getFullYear();
      const month = String(today.getMonth() + 1).padStart(2, '0');
      const day = String(today.getDate()).padStart(2, '0');
      const localDateStr = `${year}-${month}-${day}`;

      return localDateStr;
    },
    
    // 生成指定偏移天数的日期字符串（用于示例数据）
    getDateOffset(offset = 0) {
      const date = new Date();
      date.setDate(date.getDate() + offset);
      const year = date.getFullYear();
      const month = String(date.getMonth() + 1).padStart(2, '0');
      const day = String(date.getDate()).padStart(2, '0');
      return `${year}-${month}-${day}`;
    },
    
    // 从后端获取日程数据
    async loadScheduleData() {
      try {
        const userId = this.getCurrentUserId(); // 获取当前用户ID
        if (!userId) {
          // 用户未登录，使用示例数据
          this.initSampleData();
          return;
        }
        
        let scheduleData = [];
        console.log('📊 开始加载日程数据，筛选类型:', this.currentType);
        
        // 如果筛选类型是预约或全部，需要获取预约数据
        if (this.currentType === 'appointment' || this.currentType === 'all') {
          await this.loadAppointmentData(userId, scheduleData);
        }
        
        // 如果筛选类型不是纯预约，需要获取普通日程数据
        if (this.currentType !== 'appointment') {
          console.log('🔍 正在获取普通日程数据...');
          let url = `${request.BASE_URL}/api/schedule/list?userId=${userId}`;
          if (this.currentType !== 'all') {
            url += `&type=${this.currentType}`;
          }
          
          const response = await uni.request({
            url: url,
            method: 'GET',
            header: {
              'Content-Type': 'application/json'
            }
          });
          
          if (response.data.success) {
            // 转换后端数据格式为前端所需格式
            const normalSchedules = response.data.data
              .filter(item => item.type !== 'appointment') // 排除预约类型（预约数据从appointments表获取）
              .map(item => {
                const scheduleTime = new Date(item.time);
                return {
                  id: item.id,
                  date: this.formatDateToString(scheduleTime),
                  time: this.formatTimeToString(scheduleTime),
                  content: item.content,
                  location: item.location || '未指定',
                  type: item.type,
                  status: item.status || 'pending',
                  repeatType: item.repeatType || 'none'
                };
              });
            
            console.log('✅ 获取到', normalSchedules.length, '条普通日程数据');
            // 使用push方法添加到原数组中，而不是重新赋值
            scheduleData.push(...normalSchedules);
          }
        }
        
        this.scheduleList = scheduleData;
        
        console.log('✅ 日程列表设置完成，最终数据总数:', this.scheduleList.length);
        
        // 只有在完全没有数据并且没有正在筛选预约类型时才使用示例数据
        if (this.scheduleList.length === 0 && this.currentType !== 'appointment') {
          console.log('⚠️ 没有日程数据，使用示例数据');
          // 如果没有数据，使用示例数据作为备选
          this.initSampleData();
        } else if (this.scheduleList.length === 0 && this.currentType === 'appointment') {
          console.log('⚠️ 没有预约数据，这是正常情况');
          // 预约数据为空是正常情况，不需要示例数据
        }
      } catch (error) {
        console.error('加载日程数据失败:', error);
        uni.showToast({
          title: '网络异常',
          icon: 'none'
        });
        // 网络异常时使用示例数据
        this.initSampleData();
      }
    },
    
    // 从appointments表获取预约数据
    async loadAppointmentData(userId, scheduleData) {
      try {
        console.log('🔍 正在获取预约数据...');
        const response = await request.appointmentApi.getAppointmentsByElderId(userId);
        
        if (response && response.success && response.data) {
          console.log('✅ 获取到', response.data.length, '条预约数据');
          const appointmentSchedules = response.data.map(appointment => {
            const startTime = new Date(appointment.startTime);
            return {
              id: `appointment_${appointment.id}`, // 为预约数据添加前缀以区分
              appointmentId: appointment.id,  // 保存原始预约ID
              date: this.formatDateToString(startTime),
              time: this.formatTimeToString(startTime),
              content: appointment.appointmentContent || '志愿者服务预约',
              location: appointment.location || '待志愿者确认', // 使用预约的实际地点
              type: 'appointment',
              status: null, // 日程状态为null
              appointmentStatus: appointment.status, // 预约的实际状态
              volunteerId: appointment.volunteerId, // 志愿者ID
              repeatType: 'none' // 预约不重复
            };
          });
          
          // 使用push方法添加到原数组中，而不是重新赋值
          scheduleData.push(...appointmentSchedules);
          console.log('🔗 预约数据已添加，当前总数:', scheduleData.length);
        } else {
          console.log('⚠️ 没有预约数据');
        }
      } catch (error) {
        console.error('❌ 获取预约数据失败:', error);
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
    
    // 备用的示例数据初始化方法（网络异常时的降级方案）
    initSampleData() {
      this.scheduleList = [
        // 示例数据
        { id: 1, date: this.getDateOffset(-1), time: "08:00", content: "服用降压药", location: "家中", type: "medicine", status: "completed", repeatType: "daily" },
        { id: 2, date: this.getDateOffset(0), time: "10:30", content: "复查血压", location: "市人民医院", type: "doctor", status: "pending", repeatType: "none" },
        { id: 3, date: this.getDateOffset(0), time: "15:00", content: "午后健身操", location: "健身房", type: "exercise", status: "pending", repeatType: "weekly" },
        { id: 4, date: this.getDateOffset(1), time: "09:00", content: "眼科复诊", location: "眼科医院", type: "doctor", status: "pending", repeatType: "none" },
        // 添加预约示例数据
        { 
          id: "appointment_5", 
          appointmentId: 5,
          date: this.getDateOffset(0), 
          time: "14:00", 
          content: "护理服务预约", 
          location: "待志愿者确认", 
          type: "appointment", 
          status: null, 
          appointmentStatus: "pending", 
          volunteerId: null, 
          repeatType: "none" 
        },
        { 
          id: "appointment_6", 
          appointmentId: 6,
          date: this.getDateOffset(1), 
          time: "16:30", 
          content: "心理治疗预约", 
          location: "北京协和医院", 
          type: "appointment", 
          status: null, 
          appointmentStatus: "confirmed", 
          volunteerId: 101, 
          repeatType: "none" 
        }
      ];
      console.log('📝 已加载示例数据，包含预约数据');
    },
    
    // 处理筛选项变化
    handleFilterChange(type) {
      if (this.currentType !== type) {
        this.currentType = type;
        // 重新从后端获取对应类型的数据
        this.loadScheduleData();
        // watcher会自动处理滚动到今天的逻辑
      }
    },
    
    // 跳转到添加页面
    handleAdd() {
      uni.navigateTo({
        url: '/pages/user/schedule/new'
      });
    },

    // 查看日程详情
    viewSchedule(id) {
      const schedule = this.scheduleList.find(item => item.id === id);
      if (schedule) {
        this.selectedSchedule = schedule;
        this.showDetailModal = true;
      }
    },
    
    // 关闭详情弹窗
    closeDetail() {
      this.showDetailModal = false;
      this.selectedSchedule = null;
    },
    
    // 详情弹窗中的删除操作
    handleDetailDelete() {
      if (this.selectedSchedule) {
        // 先保存选中的日程引用，避免closeDetail()后丢失
        const currentSchedule = this.selectedSchedule;
        
        // 关闭详情弹窗
        this.closeDetail();
        
        // 检查是否为重复日程
        if (currentSchedule.repeatType && currentSchedule.repeatType !== 'none') {
          // 重复日程，显示删除选择弹窗
          this.repeatDeleteSchedule = currentSchedule;
          this.showRepeatDeleteModal = true;
        } else {
          // 一次性日程，直接删除
          const index = this.findScheduleIndex(currentSchedule.id);
          this.deleteIndex = index;
          this.showDeleteModal = true;
        }
      }
    },
    
    // 详情弹窗中的状态切换操作
    handleDetailToggleStatus() {
      if (this.selectedSchedule) {
        // 先保存选中的日程引用，避免closeDetail()后丢失
        const currentSchedule = this.selectedSchedule;
        const index = this.findScheduleIndex(currentSchedule.id);
        
        // 关闭详情弹窗
        this.closeDetail();
        
        // 切换状态
        this.toggleStatus(index);
      }
    },
    
    // 详情弹窗中的编辑操作
    handleDetailEdit() {
      if (this.selectedSchedule) {
        const scheduleId = this.selectedSchedule.id;
        
        // 关闭详情弹窗
        this.closeDetail();
        
        // 跳转到编辑页面
        uni.navigateTo({
          url: `/pages/user/schedule/edit?id=${scheduleId}`
        });
      }
    },
    
    // 查找日程索引
    findScheduleIndex(id) {
      return this.scheduleList.findIndex(item => item.id === id);
    },

    // 下拉刷新
    onRefresh() {
      this.refreshStatus = true;
      // 重新从后端加载数据
      this.loadScheduleData().finally(() => {
        this.refreshStatus = false;
        uni.stopPullDownRefresh();
      });
    },

    // 触摸开始事件
    handleTouchStart(event, index) {
      this.touchStartTime = Date.now();
      this.touchStartX = event.touches[0].clientX;
      this.touchStartY = event.touches[0].clientY;
      this.touchCurrentIndex = index;
      this.hasMove = false;
      
      // 设置长按定时器
      this.longPressTimer = setTimeout(() => {
        if (!this.hasMove && this.touchCurrentIndex === index) {
          this.handleLongPress(index);
        }
      }, this.longPressThreshold);
    },
    
    // 触摸移动事件
    handleTouchMove(event) {
      if (!this.touchStartTime) return;
      
      const currentX = event.touches[0].clientX;
      const currentY = event.touches[0].clientY;
      const deltaX = Math.abs(currentX - this.touchStartX);
      const deltaY = Math.abs(currentY - this.touchStartY);
      
      // 如果移动距离超过阈值，认为是滑动操作
      if (deltaX > 10 || deltaY > 10) {
        this.hasMove = true;
        this.clearLongPressTimer();
      }
    },
    
    // 触摸结束事件
    handleTouchEnd(event) {
      this.clearLongPressTimer();
      
      // 重置状态
      this.touchStartTime = 0;
      this.touchCurrentIndex = -1;
      this.hasMove = false;
    },
    
    // 清除长按定时器
    clearLongPressTimer() {
      if (this.longPressTimer) {
        clearTimeout(this.longPressTimer);
        this.longPressTimer = null;
      }
    },
    
    // 长按删除/取消
    handleLongPress(index) {
      const schedule = this.scheduleList[index];
      
      // 添加触觉反馈（如果支持）
      if (uni.vibrateShort) {
        uni.vibrateShort();
      }
      
      // 如果是预约类型，显示取消预约确认弹窗
      if (schedule.type === 'appointment') {
        this.appointmentToCancel = schedule;
        this.showCancelAppointmentModal = true;
        return;
      }
      
      // 普通日程的删除逻辑
      // 检查是否为重复日程
      if (schedule.repeatType && schedule.repeatType !== 'none') {
        // 重复日程，显示删除选择弹窗
        this.repeatDeleteSchedule = schedule;
        this.showRepeatDeleteModal = true;
      } else {
        // 一次性日程，直接删除
        this.deleteIndex = index;
        this.showDeleteModal = true;
      }
    },
    
    // 确认删除
    async confirmDelete() {
      if (this.deleteIndex >= 0) {
        try {
          const scheduleItem = this.scheduleList[this.deleteIndex];
          const response = await uni.request({
            url: `${request.BASE_URL}/api/schedule/${scheduleItem.id}`,
            method: 'DELETE',
            header: {
              'Content-Type': 'application/json'
            }
          });
          
          if (response.data.success) {
            this.scheduleList.splice(this.deleteIndex, 1);
            uni.showToast({
              title: '删除成功',
              icon: 'success'
            });
          } else {
            uni.showToast({
              title: '删除失败',
              icon: 'none'
            });
          }
        } catch (error) {

          uni.showToast({
            title: '网络异常',
            icon: 'none'
          });
        }
      }
      this.showDeleteModal = false;
      this.deleteIndex = -1;
    },
    
    // 标记日程完成状态
    async toggleStatus(index) {
      const item = this.scheduleList[index];
      if (item) {
        try {
          const newStatus = item.status === 'completed' ? 'pending' : 'completed';
          const response = await uni.request({
            url: `${request.BASE_URL}/api/schedule/status/${item.id}`,
            method: 'PUT',
            header: {
              'Content-Type': 'application/json'
            },
            data: {
              status: newStatus
            }
          });
          
          if (response.data.success) {
            item.status = item.status === 'completed' ? 'pending' : 'completed';
            uni.showToast({
              title: item.status === 'completed' ? '已标记完成' : '已取消完成',
              icon: 'success',
              duration: 1000
            });
          } else {
            uni.showToast({
              title: '状态更新失败',
              icon: 'none'
            });
          }
        } catch (error) {

          uni.showToast({
            title: '网络异常',
            icon: 'none'
          });
        }
      }
    },
    
    // 取消重复删除选择
    cancelRepeatDelete() {
      this.showRepeatDeleteModal = false;
      this.repeatDeleteSchedule = null;
    },
    
    // 仅删除当前日程
    async deleteCurrentOnly() {
      if (!this.repeatDeleteSchedule) return;
      
      try {
        const response = await uni.request({
          url: `${request.BASE_URL}/api/schedule/current/${this.repeatDeleteSchedule.id}`,
          method: 'DELETE',
          header: {
            'Content-Type': 'application/json'
          }
        });
        
        if (response.data.success) {
          // 从列表中移除该项
          const index = this.findScheduleIndex(this.repeatDeleteSchedule.id);
          if (index >= 0) {
            this.scheduleList.splice(index, 1);
          }
          
          uni.showToast({
            title: '当前日程已删除',
            icon: 'success'
          });
        } else {
          uni.showToast({
            title: '删除失败',
            icon: 'none'
          });
        }
      } catch (error) {
        uni.showToast({
          title: '网络异常',
          icon: 'none'
        });
      }
      
      this.cancelRepeatDelete();
    },
    
    // 删除所有重复日程
    async deleteAllRepeats() {
      if (!this.repeatDeleteSchedule) return;
      
      try {
        const response = await uni.request({
          url: `${request.BASE_URL}/api/schedule/all/${this.repeatDeleteSchedule.id}`,
          method: 'DELETE',
          header: {
            'Content-Type': 'application/json'
          }
        });
        
        if (response.data.success) {
          // 重新加载数据
          this.loadScheduleData();
          
          uni.showToast({
            title: '所有相关日程已删除',
            icon: 'success'
          });
        } else {
          uni.showToast({
            title: '删除失败',
            icon: 'none'
          });
        }
      } catch (error) {
        uni.showToast({
          title: '网络异常',
          icon: 'none'
        });
      }
      
      this.cancelRepeatDelete();
    },
    
    // 显示取消预约确认弹窗（从详情弹窗调用）
    handleCancelAppointment() {
      if (!this.selectedSchedule || this.selectedSchedule.type !== 'appointment') return;
      
      // 设置要取消的预约
      this.appointmentToCancel = this.selectedSchedule;
      
      // 立即关闭详情弹窗
      if (this.showDetailModal) {
        this.closeDetail();
      }
      
      // 显示确认弹窗
      this.showCancelAppointmentModal = true;
    },
    
    // 确认取消预约
    async confirmCancelAppointment() {
      if (!this.appointmentToCancel || this.appointmentToCancel.type !== 'appointment') return;
      
      try {
        // 调用后端API取消预约
        const response = await request.appointmentApi.cancelAppointment(this.appointmentToCancel.appointmentId);
        
        if (response && response.success) {
          // 更新本地数据
          this.appointmentToCancel.appointmentStatus = 'canceled';
          
          // 如果当前选中的日程是被取消的预约，也要更新
          if (this.selectedSchedule && this.selectedSchedule.id === this.appointmentToCancel.id) {
            this.selectedSchedule.appointmentStatus = 'canceled';
          }
          
          // 关闭确认弹窗
          this.showCancelAppointmentModal = false;
          this.appointmentToCancel = null;
          
          // 重新加载数据以确保状态同步
          this.loadScheduleData();
          
          uni.showToast({
            title: '预约已取消',
            icon: 'success'
          });
        } else {
          uni.showToast({
            title: response.message || '取消预约失败',
            icon: 'none'
          });
        }
      } catch (error) {
        console.error('取消预约失败:', error);
        uni.showToast({
          title: '网络异常',
          icon: 'none'
        });
      }
      
      // 清理状态
      this.showCancelAppointmentModal = false;
      this.appointmentToCancel = null;
    }
  },
  // 页面生命周期
  created() {
    // 检查用户是否已登录
    if (!requireLogin(false)) {
      return; // 如果未登录，requireLogin会处理跳转
    }
    // 不在created中加载数据，等待onLoad处理完参数后再加载
  },
  mounted() {
    // DOM渲染完成后，自动滚动到今天
    this.$nextTick(() => {
      setTimeout(() => {
        this.scrollToToday();
      }, 700); // 等待页面完全加载并避免卡顿后再滚动
    });
  },
  onLoad(options) {
    uni.setNavigationBarTitle({ title: '日程管理' });
    
    // 如果传递了type参数，设置筛选类型
    if (options.type) {
      this.currentType = options.type;
      console.log('📋 从参数设置筛选类型:', options.type);
    }
    
    // 参数处理完毕后，加载数据
    this.loadScheduleData();
    
    // 如果传递了id参数，显示对应的日程详情
    if (options.id) {
      const id = parseInt(options.id);
      // 需要等数据加载完成后再查找日程
      this.$nextTick(() => {
        setTimeout(() => {
          const schedule = this.scheduleList.find(item => item.id === id);
          if (schedule) {
            this.selectedSchedule = schedule;
            this.showDetailModal = true;
          }
        }, 500); // 等待数据加载
      });
    }
  },
  onShow() {
    // 页面显示时重新加载数据（从其他页面返回时，比如新增页面）
    this.loadScheduleData();
    // 然后尝试滚动到今天
    this.$nextTick(() => {
      setTimeout(() => {
        this.scrollToToday();
      }, 500);
    });
  },
  // 监听返回按钮
  onBackPress() {
    // 如果取消预约确认弹窗显示，则关闭弹窗
    if (this.showCancelAppointmentModal) {
      this.showCancelAppointmentModal = false;
      this.appointmentToCancel = null;
      return true; // 阻止默认返回行为
    }
    
    // 如果详情弹窗显示，则关闭弹窗
    if (this.showDetailModal) {
      this.closeDetail();
      return true; // 阻止默认返回行为
    }
    
    // 如果重复删除弹窗显示，则关闭弹窗
    if (this.showRepeatDeleteModal) {
      this.cancelRepeatDelete();
      return true; // 阻止默认返回行为
    }
    
    // 如果删除确认弹窗显示，则关闭弹窗
    if (this.showDeleteModal) {
      this.showDeleteModal = false;
      return true; // 阻止默认返回行为
    }
    
    return false; // 执行默认返回行为
  },
  // 下拉刷新配置
  onPullDownRefresh() {
    this.onRefresh();
  },
  // 页面滚动配置
  onPageScroll(e) {
    // 可添加滚动动画逻辑
  },
  // 页面销毁时清理定时器
  beforeDestroy() {
    this.clearLongPressTimer();
  },
  destroyed() {
    this.clearLongPressTimer();
  }
};
</script>

<style scoped>
/* 容器样式 */
.schedule-container {
  background-color: #f5f5f5;
  height: 100vh; /* 固定高度，防止过度滚动 */
  position: relative;
  overflow: hidden; /* 防止容器本身产生滚动条 */
  /* 完全控制滚动边界 */
  overscroll-behavior: none;
  touch-action: pan-y; /* 只允许垂直滚动 */
}

/* 固定筛选栏容器 */
.filter-tabs-fixed {
  position: fixed;
  left: 0;
  right: 0;
  z-index: 100; /* 确保筛选栏在最顶层 */
  background-color: #f5f5f500;
  padding: 4rpx;   /* 筛选栏内边距 */
  /* 防止被推动 */
  transform: translateZ(0);
  -webkit-transform: translateZ(0);
}

/* #ifdef H5 */
/* Web端筛选栏位置 - 需要避开导航栏 */
.filter-tabs-fixed {
  top: 44px; /* Web端导航栏高度，避开"日程管理"标题 */
  padding-top: 10rpx;
  padding-left: 10rpx;
  padding-right: 10rpx;
}
/* #endif */

/* #ifdef APP-PLUS */
/* Android/iOS App端筛选栏位置 */
.filter-tabs-fixed {
  top: 0;
  padding-top: calc(14rpx + env(safe-area-inset-top)); /* 状态栏适配 */
  padding-left: 14rpx;
  padding-right: 14rpx;
}
/* #endif */

/* #ifdef MP */
/* 小程序端筛选栏位置 */
.filter-tabs-fixed {
  top: 0;
  padding-top: calc(10rpx + env(safe-area-inset-top));
  padding-left: 10rpx;
  padding-right: 10rpx;
}
/* #endif */

/* 筛选标签容器 */
.filter-tabs-container {
  background: #fff;
  border-radius: 56rpx;
  box-shadow: 0 16rpx 48rpx rgba(0,0,0,0.08);
  padding: 20rpx 0;
}

/* 筛选标签行 */
.filter-tabs-row {
  display: flex;           /* 弹性布局，让标签水平排列 */
}

.filter-tabs-row:first-child {
  margin-bottom: 10rpx;    /* 第一行与第二行之间的间距 */
}

/* 筛选标签 - 保持原有样式 */
.filter-tabs {
  display: flex;           /* 弹性布局，让标签水平排列 */
}

.filter-tab {
  flex: 1;
  text-align: center;
  font-size: 30rpx;
  padding: 15rpx 0;
  color: #666;
  display: flex;
  justify-content: center;
  align-items: center;
}

.filter-tab.active {
  color: #007AFF;
  font-weight: bold;
  position: relative;   
}

.filter-tab.active::after {
  content: '';
  position: absolute;
  bottom: 5rpx;
  left: 50%;
  transform: translateX(-50%);
  width: 40rpx;
  height: 6rpx;
  background-color: #007AFF;
  border-radius: 3rpx;
}

/* 日程内容容器 */
.schedule-content {
  padding: 0 20rpx;
}

/* #ifdef H5 */
/* Web端内容区域 - 更新筛选栏高度以适应两行布局 */
.schedule-content {
  margin-top: calc(170rpx + 8rpx); /* 双行筛选栏高度 + Web导航栏高度 */
  min-height: calc(100vh - 170rpx - 44px);
}
/* #endif */

/* #ifdef APP-PLUS */
/* Android/iOS App端内容区域 - 更新筛选栏高度以适应两行布局 */
.schedule-content {
  margin-top: calc(170rpx + env(safe-area-inset-top)); /* 双行筛选栏高度 + 状态栏高度 */
  min-height: calc(100vh - 170rpx - env(safe-area-inset-top));
}
/* #endif */

/* #ifdef MP */
/* 小程序端内容区域 - 更新筛选栏高度以适应两行布局 */
.schedule-content {
  margin-top: calc(170rpx + env(safe-area-inset-top));
  min-height: calc(100vh - 170rpx - env(safe-area-inset-top));
}
/* #endif */

/* 列表样式 */
.schedule-list {
  box-sizing: border-box;
  overflow-y: auto;
  overflow-x: hidden;
  -webkit-overflow-scrolling: touch; /* iOS平滑滚动 */
  /* Android优化 - 严格控制滚动边界 */
  overscroll-behavior: contain; /* 防止过度滚动传播到父元素 */
  overscroll-behavior-y: contain; /* Y轴严格控制 */
  scroll-behavior: smooth; /* 平滑滚动 */
  /* 强制硬件加速 */
  transform: translateZ(0);
  -webkit-transform: translateZ(0);
  will-change: scroll-position;
  position: relative;
}

/* #ifdef H5 */
/* Web端列表高度 */
.schedule-list {
  height: calc(100vh - 120rpx - 44px);
  max-height: calc(100vh - 120rpx - 44px);
}
/* #endif */

/* #ifdef APP-PLUS */
/* Android/iOS App端列表高度 */
.schedule-list {
  height: calc(100vh - 120rpx - env(safe-area-inset-top));
  max-height: calc(100vh - 120rpx - env(safe-area-inset-top));
}
/* #endif */

/* #ifdef MP */
/* 小程序端列表高度 */
.schedule-list {
  height: calc(100vh - 120rpx - env(safe-area-inset-top));
  max-height: calc(100vh - 120rpx - env(safe-area-inset-top));
}
/* #endif */

.date-group:first-child {
  margin-top: 18rpx !important; /* 第一个日期组增加顶部间距 */
}

/* 为悬浮按钮留出空间 - 最后一个日期组增加底部间距 */
.date-group:last-child {
  margin-bottom: 140rpx !important; /* 为悬浮按钮留出空间 */
}

/* 日期分组样式 */
.date-group {
  margin-bottom: 30rpx;
}

/* 日期头部样式 */
.date-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20rpx 30rpx;
  margin-bottom: 0;
}

.date-text {
  font-size: 40rpx;
  font-weight: bold;
  color: #292929;
}

.weekday-text {
  font-size: 32rpx;
  color: #6c6c6c;
}

/* 过期日期头部样式 */
.date-header-past {
  opacity: 0.7;
}

.date-header-past .date-text {
  color: #bfbfbf !important;
}

.date-header-past .weekday-text {
  color: #bfbfbf !important;
}



/* 日期卡片样式 */
.date-card {
  background: #fff;
  border-radius: 30rpx;
  box-shadow: 0 6rpx 20rpx rgba(0,0,0,0.08);
  overflow: hidden;
}



/* 事件行样式 */
.event-row {
  display: flex;
  align-items: center;
  padding: 25rpx 30rpx;
  border-bottom: 1rpx solid #f0f0f0;
  position: relative;
}

.event-row:last-child {
  border-bottom: none;
}

.event-row::before {
  content: '';
  position: absolute;
  left: 0;
  top: 0;
  bottom: 0;
  width: 6rpx;
}

.item-medicine::before { background-color: #FF6B6B; }
.item-doctor::before { background-color: #4ECDC4; }
.item-exercise::before { background-color: #FFD166; }
.item-meal::before { background-color: #FF9F43; }
.item-sleep::before { background-color: #A55EEA; }
.item-appointment::before { background-color: #007AFF; }
.item-other::before { background-color: #778CA3; }

/* 事件时间样式 */
.event-time {
  width: 140rpx;
  margin-right: 30rpx;
}

.time-text {
  font-size: 40rpx;
  font-weight: bold;
  color: #333;
}

/* 事件信息样式 */
.event-info {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.event-content {
  margin-bottom: 8rpx;
}

.content-text {
  font-size: 36rpx;
  color: #333;
  line-height: 1.4;
}

.event-location {
  margin-top: 5rpx;
}

.location-text {
  font-size: 28rpx;
  color: #888;
  line-height: 1.3;
}

/* 过期日程样式 */
.text-past {
  color: #bfbfbf !important;
}

.event-past {
  opacity: 0.8;
}

.event-past .time-text {
  color: #bfbfbf;
}

.past-indicator {
  font-size: 28rpx;
  color: #ff6b6b;
  font-weight: normal;
}

/* 状态图标容器 */
.status-icon-container {
  display: flex;
  align-items: center;
  justify-content: center;
  margin-left: 20rpx;
  padding: 10rpx;
  border-radius: 50%;
  transition: background-color 0.2s ease;
}

.status-icon-container:hover {
  background-color: rgba(0, 122, 255, 0.1);
}

.status-icon {
  width: 44rpx;
  height: 44rpx;
  cursor: pointer;
  transition: all 0.2s ease;
  border-radius: 50%;
}

.status-icon:active {
  transform: scale(0.9);
}

.status-icon:hover {
  transform: scale(1.1);
}

/* 不可点击的状态图标 */
.status-icon.non-clickable {
  cursor: not-allowed;
  opacity: 0.8;
}

.status-icon.non-clickable:hover {
  transform: none;
}

.status-icon.non-clickable:active {
  transform: none;
}

.status-icon-container:has(.status-icon.non-clickable):hover {
  background-color: transparent;
}

/* 空状态 */
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 100rpx 0;
}

.empty-icon {
  width: 200rpx;
  height: 200rpx;
  margin-bottom: 30rpx;
  opacity: 0.5;
}

.empty-text {
  font-size: 32rpx;
  color: #999;
}

/* 底部占位空间 */
.bottom-spacer {
  height: 140rpx;
  width: 100%;
  flex-shrink: 0; /* 防止被压缩 */
}

/* 添加按钮 */
.add-btn {
  position: fixed;
  bottom: 80rpx;
  right: 40rpx;
  background: #007AFF;
  color: white;
  padding: 28rpx 55rpx;
  border-radius: 60rpx;
  font-size: 36rpx;
  box-shadow: 0 8rpx 24rpx rgba(0,122,255,0.5), 0 4rpx 16rpx rgba(0,0,0,0.1);    /* 阴影：x轴偏移量 模糊半径 颜色 内阴影x轴偏移量 内阴影模糊半径 内阴影颜色 */
  font-weight: bold;
  z-index: 1001; /* 确保在最顶层 */
  transition: all 0.3s ease; /* 添加过渡动画 */
  cursor: pointer;
}

.add-btn:hover {
  transform: translateY(-4rpx);
  box-shadow: 0 8rpx 24rpx rgba(0,122,255,0.6), 0 8rpx 20rpx rgba(0,0,0,0.15);
}

.add-btn:active {
  transform: scale(0.95) translateY(-2rpx);
  box-shadow: 0 12rpx 36rpx rgba(0,122,255,0.4), 0 4rpx 12rpx rgba(0,0,0,0.1);
}

/* 删除弹窗 */
.modal-mask {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: rgba(0,0,0,0.5);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 1010; /* 确保弹窗在按钮之上 */
}

.modal-container {
  background-color: #fff;
  width: 80%;
  max-width: 600rpx;
  border-radius: 20rpx;
  padding: 40rpx;
}

.modal-title {
  font-size: 38rpx;
  font-weight: bold;
  color: #333;
  text-align: center;
  margin-bottom: 20rpx;
  /* 居中显示 */
  display: flex;
  justify-content: center;
  align-items: center;
}

.modal-content {
  font-size: 34rpx;
  color: #666;
  text-align: center;
  margin-bottom: 40rpx;
}

.modal-buttons {
  display: flex;
  justify-content: space-around;
  margin-top: 30rpx;
}

.modal-buttons.vertical {
  flex-direction: column;
  gap: 20rpx;
}

.repeat-delete-modal {
  max-width: 700rpx;
}

.modal-button {
  width: 220rpx;
  height: 80rpx;
  line-height: 80rpx;
  text-align: center;
  border-radius: 40rpx;
  font-size: 34rpx;
}

.cancel {
  border: 2rpx solid #ddd;
  color: #666;
}

.confirm {
  background-color: #007AFF;
  color: white;
}

.primary {
  background-color: #007AFF;
  color: white;
}

.danger {
  background-color: #ff4d4f;
  color: white;
}

.modal-buttons.vertical .modal-button {
  width: 100%;
  margin: 0;
}

/* 详情弹窗 */
.detail-mask {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: rgba(0,0,0,0);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 1020; /* 确保详情弹窗在最顶层 */
  transition: background-color 0.3s ease;
}

.detail-mask.mask-show {
  background-color: rgba(0,0,0,0.5);
}

.detail-card {
  background-color: #fff;
  width: 90%;
  max-width: 600rpx;
  border-radius: 24rpx;
  box-shadow: 0 20rpx 60rpx rgba(0,0,0,0.15);
  overflow: hidden;
  transform: scale(0.7);
  opacity: 0;
  transition: all 0.3s cubic-bezier(0.25, 0.46, 0.45, 0.94);
}

.detail-card.card-show {
  transform: scale(1);
  opacity: 1;
}

.card-header {
  padding: 40rpx 40rpx 20rpx 40rpx;
  text-align: center;
  border-bottom: 1rpx solid #f0f0f0;
}

.card-title {
  font-size: 36rpx;
  font-weight: bold;
  color: #333;
}

.card-content {
  padding: 30rpx 40rpx;
}

.info-row {
  display: flex;
  align-items: flex-start;
  margin-bottom: 24rpx;
  min-height: 50rpx;
}

.info-row:last-child {
  margin-bottom: 0;
}

.info-label {
  font-size: 32rpx;
  color: #666;
  width: 140rpx;
  flex-shrink: 0;
  margin-right: 20rpx;
  line-height: 1.5;
}

.info-value {
  font-size: 32rpx;
  color: #333;
  flex: 1;
  line-height: 1.5;
  word-wrap: break-word;
}

.card-footer {
  padding: 30rpx 40rpx;
  display: flex;
  flex-direction: column;
  gap: 20rpx;
  border-top: 1rpx solid #f0f0f0;
}

.button-row {
  display: flex;
  justify-content: space-between;
  gap: 20rpx;
}

.button-row.single-button {
  justify-content: center;
}

.action-btn {
  flex: 1;
  text-align: center;
  padding: 24rpx 20rpx;
  border-radius: 16rpx;
  font-size: 32rpx;
  font-weight: bold;
  transition: all 0.2s ease;
  position: relative;
  overflow: hidden;
}

.action-btn::before {
  content: '';
  position: absolute;
  top: 50%;
  left: 50%;
  width: 0;
  height: 0;
  border-radius: 50%;
  background: rgba(255,255,255,0.3);
  transition: width 0.3s ease, height 0.3s ease;
  transform: translate(-50%, -50%);
}

.action-btn:active::before {
  width: 200rpx;
  height: 200rpx;
}

.delete-btn {
  background-color: #ff4d4f;
  color: white;
}

.delete-btn:active {
  background-color: #ff2626;
  transform: scale(0.95);
}

.complete-btn {
  background-color: #007AFF;
  color: white;
}

.complete-btn:active {
  background-color: #0056cc;
  transform: scale(0.95);
}

.cancel-appointment-btn {
  background-color: #FF9500;
  color: white;
}

.cancel-appointment-btn:active {
  background-color: #e08400;
  transform: scale(0.95);
}

.edit-btn {
  background-color: #28a745;
  color: white;
}

.edit-btn:active {
  background-color: #1e7e34;
  transform: scale(0.95);
}

/* 响应式设计 - 小屏幕适配 */
@media screen and (max-width: 750rpx) {
  .detail-card {
    width: 95%;
    max-width: none;
  }
  
  .card-footer {
    gap: 16rpx;
  }
  
  .button-row {
    gap: 16rpx;
  }
  
  .action-btn {
    padding: 28rpx 20rpx;
    font-size: 34rpx;
  }
  
  .info-label {
    width: 120rpx;
    font-size: 30rpx;
  }
  
  .info-value {
    font-size: 30rpx;
  }
}
</style>