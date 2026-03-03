<template>
  <view class="service-record">

    

    <!-- 第一排筛选：服务类型（全部/普通预约/远程协助） -->
    <view class="filter-bar type-filter">
      <view 
        class="filter-item" 
        :class="{ active: typeFilter === 'all' }" 
        @click="handleTypeFilter('all')"
      >
        <text>全部</text>
      </view>
      <view 
        class="filter-item" 
        :class="{ active: typeFilter === 'appointment' }" 
        @click="handleTypeFilter('appointment')"
      >
        <text>普通预约</text>
      </view>
      <view 
        class="filter-item" 
        :class="{ active: typeFilter === 'assistance' }" 
        @click="handleTypeFilter('assistance')"
      >
        <text>远程协助</text>
      </view>
    </view>

    <!-- 第二排筛选：状态（全部/待确认/已完成/已取消） -->
    <view class="filter-bar status-filter">
      <view 
        class="filter-item" 
        :class="{ active: statusFilter === 'all' }" 
        @click="handleStatusFilter('all')"
      >
        <text>全部</text>
      </view>
      <view 
        class="filter-item" 
        :class="{ active: statusFilter === 'pending' }" 
        @click="handleStatusFilter('pending')"
        v-if="typeFilter !== 'assistance'" 
      >
        <text>待确认</text>
      </view>
      <view 
        class="filter-item" 
        :class="{ active: statusFilter === 'completed' }" 
        @click="handleStatusFilter('completed')"
      >
        <text>已完成</text>
      </view>
      <view 
        class="filter-item" 
        :class="{ active: statusFilter === 'cancelled' }" 
        @click="handleStatusFilter('cancelled')"
      >
        <text>已取消</text>
      </view>
    </view>

    <!-- 服务记录列表 -->
    <view class="record-list">
      <view 
        class="record-item" 
        v-for="(record, index) in filteredRecords" 
        :key="`${record.type}-${record.id}`"
      >
        <!-- 记录头部：标题+状态+服务类型标签 -->
        <view class="record-header">
          <text class="record-title">{{ record.title }}</text>
          <view class="header-right">
            <text class="record-type-tag">{{ record.typeText }}</text>
            <text class="record-status" :class="getStatusClass(record.status)">
              {{ getStatusText(record.status) }}
            </text>
          </view>
        </view>

        <!-- 记录详情：适配普通预约/远程协助差异化字段 -->
        <view class="record-info">
          <!-- 公共字段：服务对象（关联users表老人姓名）、服务时间、服务时长 -->
          <view class="info-row">
            <text class="info-label">服务对象:</text>
            <text class="info-value">{{ record.elderName || '未知老人' }}</text>
          </view>
          <view class="info-row">
            <text class="info-label">服务时间:</text>
            <text class="info-value">{{ record.serviceDate }}</text>
          </view>
          <view class="info-row">
            <text class="info-label">服务时长:</text>
            <text class="info-value">{{ record.duration || 0 }}小时</text>
          </view>

          <!-- 普通预约专属字段：预约类型、服务地点 -->
          <view class="info-row" v-if="record.type === 'appointment'">
            <text class="info-label">预约类型:</text>
            <text class="info-value">{{ getAppointmentTypeText(record.appointmentType) }}</text>
          </view>
          <view class="info-row" v-if="record.type === 'appointment' && record.location">
            <text class="info-label">服务地点:</text>
            <text class="info-value">{{ record.location }}</text>
          </view>

          <!-- 远程协助专属字段：申请时间 -->
          <view class="info-row" v-if="record.type === 'assistance' && record.applyTime">
            <text class="info-label">申请时间:</text>
            <text class="info-value">{{ record.applyTime }}</text>
          </view>
        </view>

        <!-- 记录底部：创建时间+取消按钮（仅普通预约待确认可取消） -->
        <view class="record-footer">
          <text class="record-date">创建时间: {{ record.createDate }}</text>
          <view class="record-actions">
            <button 
              class="action-btn cancel-btn" 
              @click="cancelRecord(record)"
              v-if="record.status === 'pending' && record.type === 'appointment'"
            >
              取消服务
            </button>
          </view>
        </view>
      </view>
    </view>

    <!-- 空数据提示 -->
    <view class="empty-tip" v-if="filteredRecords.length === 0 && !isLoading">
      <image src="/static/icons/empty-record.png" mode="aspectFit"></image>
      <text>暂无匹配的服务记录</text>
    </view>

    <!-- 加载中提示 -->
    <view class="loading-tip" v-if="isLoading">
      <text class="loading-text">正在加载记录...</text>
    </view>

    
  </view>
</template>

<script>
import config from '@/utils/config.js'

export default {
  data() {
    return {
      // 筛选条件：typeFilter（all/appointment/assistance）、statusFilter（all/pending/completed/cancelled）
      typeFilter: 'all',
      statusFilter: 'all',
      // 原始数据：普通预约列表、远程协助列表
      appointmentList: [],
      assistanceList: [],
      // 加载状态
      isLoading: false,
      // 当前用户ID（从本地存储获取，关联users表id）
      userId: uni.getStorageSync('userInfo')?.id || ''
    }
  },
  computed: {
    // 双维度筛选后的最终展示数据
    filteredRecords() {
      // 1. 合并普通预约和远程协助数据（添加类型标识）
      const mergedData = [
        ...this.appointmentList.map(item => ({ ...item, type: 'appointment', typeText: '普通预约' })),
        ...this.assistanceList.map(item => ({ ...item, type: 'assistance', typeText: '远程协助' }))
      ]

      // 2. 按服务类型筛选
      let typeFiltered = mergedData
      if (this.typeFilter === 'appointment') {
        typeFiltered = mergedData.filter(item => item.type === 'appointment')
      } else if (this.typeFilter === 'assistance') {
        typeFiltered = mergedData.filter(item => item.type === 'assistance')
      }

      // 3. 按状态筛选（适配两张表的状态映射）
      let statusFiltered = typeFiltered
      if (this.statusFilter !== 'all') {
        statusFiltered = typeFiltered.filter(item => {
          // 普通预约状态映射：confirmed→待确认，completed→已完成，canceled→已取消
          if (item.type === 'appointment') {
            const statusMap = {
              'pending': 'pending', // 若存在pending状态也归为待确认
              'confirmed': 'pending',
              'completed': 'completed',
              'canceled': 'cancelled',
              'no_show': 'cancelled', // 未到场归为已取消
              'time_out': 'cancelled' // 超时归为已取消
            }
            return statusMap[item.status] === this.statusFilter
          }
          // 远程协助状态映射：completed→已完成，cancelled→已取消（无待确认）
          else if (item.type === 'assistance') {
            const statusMap = {
              'completed': 'completed',
              'cancelled': 'cancelled'
            }
            return statusMap[item.status] === this.statusFilter
          }
          return false
        })
      }

      // 4. 按创建时间倒序（最新在前）
      return statusFiltered.sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt))
    }
  },
  onLoad() {
    // 页面加载时获取服务记录（需先登录，有userId）
    if (this.userId) {
      this.getServiceRecords()
    } else {
      uni.showToast({ title: '请先登录', icon: 'none', duration: 1500 })
      setTimeout(() => {
        uni.navigateTo({ url: '/pages/login/login' })
      }, 1500)
    }
  },
  methods: {
    // 返回上一页
    navigateBack() {
      uni.navigateBack({ delta: 1 })
    },

    // 切换服务类型筛选
    handleTypeFilter(type) {
      this.typeFilter = type
      // 若切换到远程协助，自动隐藏待确认状态，重置为全部状态
      if (type === 'assistance' && this.statusFilter === 'pending') {
        this.statusFilter = 'all'
      }
    },

    // 切换状态筛选
    handleStatusFilter(status) {
      this.statusFilter = status
    },

    // 核心：获取服务记录（普通预约+远程协助，通过userId关联）
    getServiceRecords() {
      this.isLoading = true
      // 并行请求两张表的数据，优化加载速度
      Promise.all([
        // 1. 获取普通预约记录（appointments表）
        // 1. 获取普通预约记录（appointments表）- 修正后
        new Promise((resolve, reject) => {
          uni.request({
            url: `${config.API_BASE_URL}/api/volunteer/appointment/records`, // 修正路径：appointment + /records
            method: 'GET',
            data: { userId: this.userId }, // 修正参数名：volunteerId → userId
            header: { 'Content-Type': 'application/json' },
            success: (res) => {
              if (res.statusCode === 200 && res.data.success) {
                // 格式化数据的逻辑不变（确保后端返回的字段与前端映射一致即可）
                this.appointmentList = res.data.data.map(item => ({
                  id: item.id,
                  elderId: item.elderId,
                  elderName: item.elderName, // 需确保后端返回该字段（若未关联用户表，需在Service中补充关联查询）
                  appointmentType: item.appointmentType,
                  appointmentContent: item.appointmentContent,
                  title: item.appointmentContent || '未填写预约内容',
                  status: item.status,
                  startTime: item.startTime,
                  endTime: item.endTime,
                  serviceDate: this.formatDateTimeRange(item.startTime, item.endTime),
                  duration: this.calculateDuration(item.startTime, item.endTime),
                  location: item.location,
                  createdAt: item.createdAt,
                  createDate: this.formatDate(item.createdAt)
                }))
                resolve()
              } else {
                uni.showToast({ title: '普通预约记录获取失败', icon: 'none' })
                reject()
              }
            },
            fail: () => {
              uni.showToast({ title: '网络错误，普通预约记录加载失败', icon: 'none' })
              reject()
            }
          })
        }),

        // 2. 获取远程协助记录（assistance表）
       // 2. 获取远程协助记录（assistance表）- 修正后
       new Promise((resolve, reject) => {
         uni.request({
           url: `${config.API_BASE_URL}/api/volunteer/assistance/records`, // 补充 /records
           method: 'GET',
           data: { userId: this.userId }, // 参数名从 volunteerId 改为 userId
           header: { 'Content-Type': 'application/json' },
           success: (res) => {
             if (res.statusCode === 200 && res.data.success) {
               // 格式化数据的逻辑不变...
               this.assistanceList = res.data.data.map(item => ({
                 id: item.id,
                 elderId: item.elderId,
                 elderName: item.elderName,
                 appointmentContent: item.appointmentContent,
                 title: item.appointmentContent || '未填写协助内容',
                 status: item.status,
                 startTime: item.startTime,
                 endTime: item.endTime,
                 applyTime: this.formatDateTime(item.applyTime),
                 serviceDate: this.formatDateTimeRange(item.startTime, item.endTime),
                 duration: this.calculateDuration(item.startTime, item.endTime),
                 createdAt: item.createdAt,
                 createDate: this.formatDate(item.createdAt)
               }))
               resolve()
             } else {
               uni.showToast({ title: '远程协助记录获取失败', icon: 'none' })
               reject()
             }
           },
           fail: () => {
             uni.showToast({ title: '网络错误，远程协助记录加载失败', icon: 'none' })
             reject()
           }
         })
       })
      ]).finally(() => {
        // 无论成功失败，关闭加载状态
        this.isLoading = false
      })
    },

    // 格式化日期：yyyy-MM-dd
    formatDate(dateStr) {
      if (!dateStr) return ''
      const date = new Date(dateStr)
      return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`
    },

    // 格式化日期时间：yyyy-MM-dd HH:mm
    formatDateTime(dateStr) {
      if (!dateStr) return ''
      const date = new Date(dateStr)
      return `${this.formatDate(dateStr)} ${String(date.getHours()).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}`
    },

    // 格式化时间范围：yyyy-MM-dd HH:mm - yyyy-MM-dd HH:mm
    formatDateTimeRange(startStr, endStr) {
      if (!startStr) return '未设置时间'
      return `${this.formatDateTime(startStr)}${endStr ? ' - ' + this.formatDateTime(endStr) : ' - 未设置结束时间'}`
    },

    // 计算服务时长（小时，保留1位小数）
    calculateDuration(startStr, endStr) {
      if (!startStr || !endStr) return 0
      const start = new Date(startStr).getTime()
      const end = new Date(endStr).getTime()
      const durationMs = end - start
      return (durationMs / (1000 * 60 * 60)).toFixed(1) * 1 // 转为数字，避免字符串
    },

    // 普通预约类型转中文（doctor→医生服务，nurse→护士服务等）
    getAppointmentTypeText(type) {
      const typeMap = {
        'doctor': '医生服务',
        'nurse': '护士服务',
        'rehab': '康复师服务',
        'therapy': '治疗师服务',
        'other': '其他服务'
      }
      return typeMap[type] || '其他服务'
    },

    // 状态转中文（pending→待确认，completed→已完成，cancelled→已取消）
    getStatusText(status) {
      const statusMap = {
        'pending': '待确认',
        'confirmed': '待确认',
        'completed': '已完成',
        'canceled': '已取消',
        'cancelled': '已取消',
        'no_show': '已取消',
        'time_out': '已取消'
      }
      return statusMap[status] || '未知状态'
    },

    // 状态样式（不同状态对应不同颜色标签）
    getStatusClass(status) {
      const classMap = {
        'pending': 'status-pending',
        'confirmed': 'status-pending',
        'completed': 'status-completed',
        'canceled': 'status-cancelled',
        'cancelled': 'status-cancelled',
        'no_show': 'status-cancelled',
        'time_out': 'status-cancelled'
      }
      return classMap[status] || ''
    },

    // 取消服务记录（仅普通预约待确认可取消）
	    cancelRecord(record) {
	      uni.showModal({
	        title: '确认取消',
	        content: `确定要取消【${record.title}】这项服务吗？取消后不可恢复`,
	        confirmColor: '#ff4444',
	        success: (res) => {
	          if (res.confirm) {
	            this.isLoading = true
	            // 调用后端取消接口（区分普通预约/远程协助，实际远程协助无待确认，此处仅处理普通预约）
	            uni.request({
	              url: `${config.API_BASE_URL}/api/volunteer/appointments/${record.id}/cancel`,
	              method: 'POST',
	              data: { volunteerId: this.userId },
	              header: { 'Content-Type': 'application/json' },
	              success: (res) => {
	                if (res.statusCode === 200 && res.data.success) {
	                  uni.showToast({ title: '取消成功', icon: 'success' })
	                  // 重新获取数据，刷新列表
	                  this.getServiceRecords()
	                } else {
	                  uni.showToast({ title: res.data.msg || '取消失败', icon: 'none' })
	                }
	              },
	              fail: () => {
	                uni.showToast({ title: '网络错误，取消失败', icon: 'none' })
	              },
	              complete: () => {
	                this.isLoading = false
	              }
	            })
	          }
	        }
	      })
	    }
	  }
	}
	</script>
	
	<style scoped>
	/* 页面基础样式 */
	.service-record {
	  padding-bottom: 0; /* 为底部导航留空 */
	  min-height: 100vh;
	  background-color: #f8f9fa;
	}
	
	
	
	
	/* 筛选栏通用样式 */
	.filter-bar {
	  display: flex;
	  background-color: #fff;
	  margin: 20rpx 30rpx;
	  border-radius: 10rpx;
	  box-shadow: 0 2rpx 8rpx rgba(0, 0, 0, 0.05);
	  overflow: hidden;
	}
	/* 服务类型筛选栏（第一排） */
	.type-filter {
	  margin-bottom: 0;
	  border-bottom-left-radius: 0;
	  border-bottom-right-radius: 0;
	}
	/* 状态筛选栏（第二排） */
	.status-filter {
	  margin-top: 0;
	  border-top-left-radius: 0;
	  border-top-right-radius: 0;
	}
	/* 筛选项样式 */
	.filter-item {
	  flex: 1;
	  text-align: center;
	  padding: 20rpx 0;
	}
	.filter-item text {
	  font-size: 28rpx;
	  color: #666;
	}
	/* 筛选项激活状态 */
	.filter-item.active {
	  background-color: #f0f9f0;
	}
	.filter-item.active text {
	  color: #6ac259;
	  font-weight: bold;
	}
	
	/* 记录列表容器 */
	.record-list {
	  padding: 0 30rpx;
	}
	/* 单条记录卡片 */
	.record-item {
	  background-color: #fff;
	  border-radius: 10rpx;
	  margin-bottom: 20rpx;
	  padding: 25rpx;
	  box-shadow: 0 2rpx 10rpx rgba(0, 0, 0, 0.05);
	}
	/* 记录头部（标题+状态+类型标签） */
	.record-header {
	  display: flex;
	  justify-content: space-between;
	  align-items: flex-start;
	  margin-bottom: 20rpx;
	}
	.record-title {
	  font-size: 32rpx;
	  font-weight: bold;
	  color: #333;
	  max-width: 60%;
	  white-space: nowrap;
	  overflow: hidden;
	  text-overflow: ellipsis;
	}
	.header-right {
	  display: flex;
	  align-items: center;
	  gap: 10rpx;
	}
	/* 服务类型标签（普通预约/远程协助） */
	.record-type-tag {
	  font-size: 22rpx;
	  color: #6ac259;
	  background-color: #f0f9f0;
	  padding: 5rpx 12rpx;
	  border-radius: 5rpx;
	}
	/* 状态标签 */
	.record-status {
	  font-size: 24rpx;
	  padding: 6rpx 15rpx;
	  border-radius: 5rpx;
	}
	.status-pending {
	  color: #ff7e5f;
	  background-color: rgba(255, 126, 95, 0.1);
	}
	.status-completed {
	  color: #3cc51f;
	  background-color: rgba(60, 197, 31, 0.1);
	}
	.status-cancelled {
	  color: #999;
	  background-color: rgba(153, 153, 153, 0.1);
	}
	
	/* 记录详情区域 */
	.record-info {
	  margin-bottom: 20rpx;
	}
	.info-row {
	  display: flex;
	  margin-bottom: 15rpx;
	  align-items: center;
	}
	.info-label {
	  width: 180rpx;
	  font-size: 26rpx;
	  color: #666;
	}
	.info-value {
	  flex: 1;
	  font-size: 26rpx;
	  color: #333;
	  word-break: break-all;
	}
	
	/* 记录底部（创建时间+操作按钮） */
	.record-footer {
	  display: flex;
	  justify-content: space-between;
	  align-items: center;
	  padding-top: 15rpx;
	  border-top: 1rpx solid #f5f5f5;
	}
	.record-date {
	  font-size: 24rpx;
	  color: #999;
	}
	/* 取消按钮 */
	.cancel-btn {
	  background-color: #ff4444;
	  color: #fff;
	  border-radius: 5rpx;
	  padding: 10rpx 25rpx;
	  font-size: 24rpx;
	}
	/* 清除默认按钮样式 */
	.cancel-btn::after {
	  border: none;
	}
	
	/* 空数据提示 */
	.empty-tip {
	  display: flex;
	  flex-direction: column;
	  align-items: center;
	  padding-top: 200rpx;
	}
	.empty-tip image {
	  width: 200rpx;
	  height: 200rpx;
	  margin-bottom: 30rpx;
	  opacity: 0.6;
	}
	.empty-tip text {
	  font-size: 30rpx;
	  color: #999;
	}
	
	/* 加载中提示 */
	.loading-tip {
	  display: flex;
	  justify-content: center;
	  align-items: center;
	  padding-top: 200rpx;
	}
	.loading-text {
	  font-size: 28rpx;
	  color: #666;
	}
	</style>