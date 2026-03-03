<template>
  <view class="elderly-detail-container">
    <!-- 加载状态 -->
    <view v-if="isLoading" class="loading">
      <view class="loading-spinner"></view>
      <text>加载中...</text>
    </view>

    <!-- 正常内容 -->
    <view v-else>
      <!-- 老人信息卡片 -->
            <view class="profile-card">
              <view class="left">
                <image class="avatar" :src="getAvatarUrl(elder.avatarUrl)"></image>
                <view class="health-status" :class="getHealthStatusClass(elder.healthStatus)">
                  {{ elder.healthStatus || '状态正常' }}
                </view>
              </view>
              <view class="right">
                <!-- 姓名+年龄：处理年龄为空的情况 -->
                <text class="name">
                  {{ elder.name }}（{{ elder.age === '未知' ? '未知年龄' : elder.age + '岁' }}）
                </text>
                <!-- 性别+关系：处理性别为空的情况 -->
                <text class="gender">
                  {{ elder.gender }} | {{ elder.relationship }}
                </text>
				
          <view class="quick-actions">
            <view class="action-btn" @click="getLocation">
              <uni-icons type="location" size="20" color="#3cc51f"></uni-icons>
              <text>定位</text>
            </view>
          </view>
        </view>
      </view>

      <!-- 健康数据面板 -->
      <view class="health-panel">
        <view class="panel-title">
          <text>健康指标</text>
          <text class="more" @click="navigateTo('healthrecord')">历史记录 ></text>
        </view>

        <view class="health-grid">
          <view v-for="(item, index) in healthData" :key="index" class="health-item" @click="showDetail(item.type)">
            <text class="label">{{ item.label }}</text>
            <text class="value" :class="{ 'warning': item.isWarning }">
              {{ item.value }}
            </text>
            <text class="unit">{{ item.unit }}</text>
          </view>
        </view>
      </view>

      <!-- 用药提醒（核心修改部分） -->
      <view class="medicine-panel">
        <view class="panel-title">
          <text>用药提醒</text>
          <text class="more" @click="navigateTo('medicineremind')">全部 ></text>
        </view>
      
        <!-- 空状态提示 -->
        <view v-if="medicines.length === 0" class="empty-medication">
          <image src="/static/guardian/empty-med.png" class="empty-icon"></image>
          <text class="empty-text">暂无用药提醒记录</text>
        </view>
      
        <!-- 用药列表（从接口获取数据） -->
        <view 
          v-else 
          v-for="(med, index) in medicines" 
          :key="`med-${med.id}-${index}`"
          class="medicine-item" 
          @touchstart="touchStart(index)"
          @touchend="touchEnd" 
          :style="activeMedIndex === index ? { transform: 'scale(0.98)' } : {}"
          @click="openMedicineDetail(med)" 
        >
          <!-- 1. 日期（显示开始日期，更简洁） -->
          <text class="time">{{ formatDate(med.startDate) }}</text>
          
          <!-- 2. 药品名称（中间弹性空间，过长省略） -->
          <text class="name">{{ med.medicineName }}</text>
          
          <!-- 3. 剂量（右侧固定空间，过长省略） -->
          <text class="dosage">{{ med.dosage }}</text>
          
          <!-- 4. 用药状态图标 -->
          <uni-icons
            :type="med.isActive === '正在服用' ? 'checkbox-filled' : 'circle'"
            :color="med.isActive === '正在服用' ? '#3cc51f' : '#ccc'"
            size="24"
          ></uni-icons>
        </view>
	  </view>
	  
	  <!-- 新增：用药详情弹窗 -->
	  <view v-if="showMedicineModal" class="medicine-modal" @click="closeMedicineDetail">
	    <view class="modal-content" @click.stop>
	      <view class="modal-header">
	        <text class="modal-title">用药详情</text>
	        <uni-icons type="close" size="24" color="#999" @click="closeMedicineDetail"></uni-icons>
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
			<!-- 新增：保质期至展示项 -->
			<view class="detail-item">
			  <text class="detail-label">保质期至：</text>
			  <text class="detail-value">{{ currentMedicine.expireDate || '未设置' }}</text>
			</view>
	        <view class="detail-item">
	          <text class="detail-label">用药状态：</text>
	          <text class="detail-value" :class="{ 'active-status': currentMedicine.isActive === '正在服用' }">
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
	baseUrl: config.API_BASE_URL, // 添加基础URL配置
	showMapModal: false, // 控制地图弹窗显示/隐藏
	    elderLocation: {     // 存储老年人的位置信息
	      latitude: 0,
	      longitude: 0,
	      updateTime: ''
	    },
          // 老人信息（初始为空，等待API填充）
         elder: {
               id: '',        // 保持与首页一致
               userId: '',    // 新增，对应后端users.id
               name: '',
               age: '',
               gender: '',
               relationship: '', // 改为relationship保持与首页一致
               healthStatus: '', // 改为healthStatus保持与首页一致
               phone: '',
               avatarUrl: ''
             },
      healthData: [],
      medicines: [],
      activeMedIndex: -1,
      isLoading: false,
      errorMsg: '',
	  // 新增：用药详情弹窗相关变量
	  showMedicineModal: false, // 控制弹窗显示
	  currentMedicine: null  // 当前选中的药品详情
    }
  },
  // 详情页 onLoad 方法（接收 userId 并调用新接口）
  onLoad(options) {
    console.log('接收到的userId:', options.userId); // 调试：确认传参
    if (!options.userId) {
      this.errorMsg = '缺少用户ID，无法加载数据';
      this.isLoading = false;
      return;
    }
    this.elder.userId = options.userId;
    // 加载老人基础数据 → 加载健康数据 → 加载用药数据
    this.loadElderDataByUserId(options.userId).then(() => {
      this.loadHealthData(this.elder.userId);
      this.loadMedicationData(this.elder.userId); // 调用用药数据加载
    }).catch(err => {
      console.error('加载数据失败:', err);
    });
	// 新增：注册“用药新增成功”的事件监听
	    this.medAddListener = uni.$on('medicationAdded', () => {
	      console.log('elderly-detail 监听到用药新增，重新加载用药数据');
	      // 关键：重新调用用药数据加载方法，获取最新列表
	      this.loadMedicationData(this.elder.userId);
	    });
  },
  onUnload() {
      // 新增：页面销毁时移除监听（必须加，防止多次注册）
      if (this.medAddListener) {
        uni.$off('medicationAdded', this.medAddListener);
      }
    },
  methods: {
	  // 获取头像URL（添加BASE_URL和时间戳避免缓存）
	  getAvatarUrl(avatarUrl) {
	    if (avatarUrl && avatarUrl.trim() !== '') {
	      // 如果是相对路径，添加后端基础URL
	      // 支持 /uploads/ 和 /upload/ 两种格式
	      if (avatarUrl.startsWith('/uploads/') || avatarUrl.startsWith('/upload/')) {
	        // 如果已经带有时间戳，直接添加基础URL
	        if (avatarUrl.includes('?t=')) {
	          return this.baseUrl + avatarUrl;
	        }
	        // 否则添加时间戳避免缓存
	        const fullUrl = this.baseUrl + avatarUrl + '?t=' + Date.now();
	        return fullUrl;
	      }
	      return avatarUrl;
	    }
	    // 默认头像（Spring Boot静态资源自动映射）
	    return this.baseUrl + '/uploads/avatars/default-avatar.png';
	  },
	  
	  formatDate(dateStr) {
	      if (!dateStr) return '';
	      // 切割出"月-日"，如 2025-09-13 → 09-13
	      return dateStr.split('-').slice(1).join('-');
	    },
	  // 新增：显示用药详情
	  openMedicineDetail(medicine) {
	    this.currentMedicine = medicine;
	    this.showMedicineModal = true;
	  },
	  
	  // 新增：关闭用药详情弹窗
	  closeMedicineDetail() {
	    this.showMedicineModal = false;
	    this.currentMedicine = null;
	  },

	  formatElderData(elder) {
	    return {
	      id: elder.id,
	      userId: elder.userId,
	      name: elder.name || '未命名用户',
	      age: elder.age || '未知',
	      gender: elder.gender || '未知',
	      relationship: elder.relationship || '未知关系', // 保持与首页一致
	      healthStatus: elder.healthCondition || '状态正常',
	      phone: elder.phone || '未绑定电话',
	      avatarUrl: elder.avatarUrl || '/static/avatar-default.png'
	    };
	  },
	  
	  // 重新加载数据
	      reloadData() {
	        this.isLoading = true;
	        this.errorMsg = '';
	        this.loadElderData(this.elder.id);
	      },
		  
		  // 核心修改：从后端获取老人数据
		  // 修改为返回Promise
		loadElderDataByUserId(userId) {
		  return new Promise((resolve, reject) => {
		    this.isLoading = true;
		    const token = uni.getStorageSync('token');
		    
		    
		    uni.request({
		      url: `${config.API_BASE_URL}/api/elder/selfinfo/${userId}`,
		      method: 'GET',
		      header: { 'Authorization': `Bearer ${token}` },
		      success: (res) => {
		        if (res.statusCode === 200 && res.data) {
		          console.log('详情页API数据:', res.data);
		          this.elder = this.formatElderData(res.data);
		          console.log('格式化后数据:', this.elder);
		          resolve(); // 成功时调用resolve
		        } else {
		          this.errorMsg = `加载失败：${res.statusCode}`;
		          reject(new Error(this.errorMsg)); // 失败时调用reject
		        }
		      },
		      fail: (err) => {
		        this.errorMsg = `请求失败：${err.errMsg}`;
		        reject(new Error(this.errorMsg)); // 失败时调用reject
		      },
		      complete: () => {
		        this.isLoading = false;
		      }
		    });
		  });
		},
	  // 加载健康数据
	      loadHealthData(userId) {
	        const token = uni.getStorageSync('token');
	        
	        uni.request({
	          url: `${config.API_BASE_URL}/api/guardian/health/latestinfo/${userId}`,
	          method: 'GET',
	          header: { 'Authorization': `Bearer ${token}` },
	          success: (res) => {
	            if (res.statusCode === 200 && res.data) {
	              this.healthData = res.data.map(item => {
	                let label = '';
	                switch(item.healthType) {
	                  case 'heart_rate': label = '心率'; break;
	                  case 'blood_pressure': label = '血压'; break;
	                  case 'blood_sugar': label = '血糖'; break;
	                  case 'weight': label = '体重'; break;
	                  case 'temperature': label = '体温'; break;
	                  case 'steps': label = '步数'; break;
	                  default: label = '其他指标';
	                }
	    
	                let isWarning = false;
	                if (item.healthType === 'blood_pressure') {
	                  const [systolic, diastolic] = item.value.split('/').map(Number);
	                  isWarning = systolic > 140 || diastolic > 90;
	                } else if (item.healthType === 'heart_rate') {
	                  const rate = Number(item.value);
	                  isWarning = rate < 60 || rate > 100;
	                }
	    
	                return {
	                  label,
	                  value: item.value,
	                  unit: item.unit,
	                  type: item.healthType,
	                  isWarning
	                };
	              });
	            }
	          },
	          fail: (err) => {
	            console.error('获取健康数据失败:', err);
	            uni.showToast({ title: '健康数据加载失败', icon: 'none' });
	          }
	        });
	      }, 
	loadMedicationData(elderUserId) {
  const token = uni.getStorageSync('token');
  const userInfo = uni.getStorageSync('userInfo');
   
  const guardianUserId = userInfo?.id;

  if (!guardianUserId) {
    uni.showToast({ title: '请先登录', icon: 'none' });
    return;
  }

  uni.request({
    url: `${config.API_BASE_URL}/api/guardian/medication/list/${elderUserId}`,
    method: 'GET',
    header: { 
      'Authorization': `Bearer ${token}`,
      'Content-Type': 'application/json'
    },
    data: { guardianUserId }, 
    success: (res) => {
      console.log('用药记录接口响应:', res.data);
      if (res.data.code === 200 && res.data.data) {
        // 1. 先赋值原始数据
        this.medicines = res.data.data; 
        
        // 2. 新增排序：正在服用 → 其他状态（已停用/已过期）
        this.medicines.sort((medA, medB) => {
          // 定义优先级：“正在服用”排最前，其他排后
          const isActiveA = medA.isActive === '正在服用';
          const isActiveB = medB.isActive === '正在服用';
          
          // 逻辑：A是正在服用，B不是 → A排前（返回-1）；反之B排前（返回1）；相同则保持原顺序（返回0）
          if (isActiveA && !isActiveB) return -1;
          if (!isActiveA && isActiveB) return 1;
          return 0;
        });

      } else {
        uni.showToast({ 
          title: res.data.msg || '获取用药记录失败', 
          icon: 'none' 
        });
        this.medicines = [];
      }
    },
    fail: (err) => {
      console.error('获取用药记录失败:', err);
      uni.showToast({ title: '网络错误，获取用药记录失败', icon: 'none' });
      this.medicines = [];
    }
  });
},
    
        
    navigateBack() {
      uni.navigateBack();
    },
      navigateTo(page) {
        let url = '';
        if (page === 'healthrecord') {
          url = `/pages/guardian/elderly/healthrecord?userId=${this.elder.userId}`;
        } else if (page === 'medicineremind') {
          url = `/pages/guardian/elderly/medicineremind?userId=${this.elder.userId}`;
        }

      uni.navigateTo({
        url,
        fail: (err) => {
          console.error('页面跳转失败:', err);
          uni.showToast({
            title: '页面加载失败',
            icon: 'none'
          });
        }
      });
    },
    getLocation() {
       // 后端地址（确认正确）
      const elderUserId = this.elder.userId;   // 老年人ID（从详情页数据获取，正确）
      const userInfo = uni.getStorageSync('userInfo'); // 获取监护人的登录信息
      const guardianUserId = userInfo?.id;     // 监护人ID（对应后端需要的guardianUserId，需确认字段名）
    
      // 1. 基础校验：避免关键参数缺失
      if (!elderUserId) {
        uni.showToast({ title: '老年人ID异常', icon: 'none' });
        return;
      }
      if (!guardianUserId) {
        uni.showToast({ title: '监护人信息异常，请重新登录', icon: 'none' });
        return;
      }
    
      // 2. 调用后端接口（修改地址、补充请求头）
      uni.request({
        // 问题1修复：接口地址前缀从 guardian → elderly（匹配后端 @RequestMapping("/api/elderly/location")）
        url: `${config.API_BASE_URL}/api/elderly/location/get/${elderUserId}`, 
        method: 'GET',
        // 问题2修复：补充 guardianUserId 请求头（后端必须通过该参数做权限校验）
        header: { 
          'guardianUserId': guardianUserId    // 新增：监护人ID（关键！）
        },
        success: (res) => {
          // 3. 响应数据解析（问题3修复：从 res.data.data 取位置数据）
          if (res.data.code === 200) {
            const locationData = res.data.data; // 位置数据在 data 嵌套层中
            if (locationData && locationData.latitude && locationData.longitude) {
              // 显示位置信息（正确解析经纬度和更新时间）
              uni.showModal({
                title: '老年人位置',
                content: `纬度：${locationData.latitude}\n经度：${locationData.longitude}\n最后更新：${locationData.updateTime || '未知'}`,
                showCancel: false,
                confirmText: '知道了'
              });
            } else {
              // 后端返回200，但无位置数据（如老年人未上传过位置）
              uni.showToast({ title: '未获取到位置信息', icon: 'none' });
            }
          } else if (res.data.code === 403) {
            // 处理权限不足（如监护人未绑定该老年人）
            uni.showToast({ title: res.data.msg || '您没有权限查看该老年人位置', icon: 'none' });
          } else {
            // 其他业务错误（如参数无效）
            uni.showToast({ title: res.data.msg || '位置查询失败', icon: 'none' });
          }
        },
        fail: (err) => {
          // 处理网络错误（如后端服务未启动、地址错误）
          console.error('获取位置失败（网络错误）：', err);
          uni.showToast({ title: '网络错误，无法获取位置', icon: 'none' });
        }
      });
    },
    
      // 新增：关闭地图弹窗
      closeMapModal() {
        this.showMapModal = false;
      },
    triggerSOS() {
      uni.showModal({
        title: '紧急求助',
        content: '确定要发送SOS求助信号吗？',
        confirmText: '发送',
        confirmColor: '#ff4444',
        success: (res) => {
          if (res.confirm) {
            // 触发震动反馈
            uni.vibrateLong();

            // 预留接口位置，实际使用时替换
            /*
            uni.request({
              url: 'https://your-api.com/emergency/sos',
              method: 'POST',
              data: {
                id: this.elder.id
              },
              success: () => {
                uni.showToast({
                  title: 'SOS求助已发送',
                  icon: 'success'
                });
              },
              fail: (err) => {
                console.error('发送SOS失败:', err);
                uni.showToast({
                  title: '求助发送失败，请重试',
                  icon: 'none'
                });
              }
            });
            */

            // 模拟发送成功
            uni.showToast({
              title: 'SOS求助已发送',
              icon: 'success'
            });
          }
        }
      });
    },
    // 完善：切换用药确认状态（同步后端）
    toggleMedicine(medId) {
      const currentMed = this.medicines.find(med => med.id === medId);
      if (!currentMed) return;
    
      const newConfirmed = !currentMed.confirmed;
      const token = uni.getStorageSync('token');
      const userInfo = uni.getStorageSync('userInfo'); // 从登录用户信息中获取
      const guardianUserId = userInfo ? userInfo.userId : ''; // 监护人ID即用户ID
      const elderUserId = this.elder.userId;
      
    
      // 新增校验：防止 userInfo 为空
      if (!userInfo || !guardianUserId) {
        uni.showToast({ title: '监护人信息异常，请重新登录', icon: 'none' });
        return;
      }
    
      // 调用后端更新接口
      uni.request({
        url: `${config.API_BASE_URL}/api/guardian/medication/confirm/${medId}`, // 后端用药记录ID
        method: 'PUT', // 建议用PUT请求更新状态
        header: { 
          'Authorization': `Bearer ${token}`,
          'Content-Type': 'application/json'
        },
        data: {
          guardianUserId: guardianUserId,
          elderUserId: elderUserId,
          confirmed: newConfirmed
        },
        success: (res) => {
          if (res.statusCode === 200 && res.data.code === 200) {
            // 接口成功后更新前端状态
            this.medicines = this.medicines.map(med => 
              med.id === medId ? { ...med, confirmed: newConfirmed } : med
            );
            uni.showToast({ 
              title: newConfirmed ? '已标记为已服用' : '已取消标记', 
              icon: 'success' 
            });
          } else {
            uni.showToast({ title: res.data.msg || '更新用药状态失败', icon: 'none' });
          }
        },
        fail: (err) => {
          uni.showToast({ title: '网络错误，更新失败', icon: 'none' });
          console.error('更新用药状态失败:', err);
        }
      });
    },
    touchStart(index) {
      this.activeMedIndex = index;
    },
    touchEnd() {
      this.activeMedIndex = -1;
    },
    getHealthStatusClass(status) {
      return status.includes('偏高') || status.includes('异常') ? 'warning' : 'normal';
    },
    updateTextSize(size) {
      // 更新字体大小逻辑
    },
    showDetail(type) {
      // 显示健康指标详情
      console.log('查看详情:', type);
    },
	// 新增：用药时间格式化（如："08:00:00" → "08:00"）
	formatMedTime(timeStr) {
	  if (!timeStr) return '';
	  return timeStr.split(':').slice(0, 2).join(':');
	}
  }
}
</script>


<style scoped>
/* 样式保持不变 */
.elderly-detail-container {
  padding: 20rpx;
  min-height: 100vh;
  background-color: #f8f8f8;
  position: relative;
  padding-bottom: 120rpx; /* 给底部导航留空间 */
}

/* 返回导航 */
.nav-back {
  display: flex;
  align-items: center;
  padding: 20rpx;
  font-size: 28rpx;
  color: #333;
}

/* 个人信息卡片 */
.profile-card {
  display: flex;
  background: #fff;
  border-radius: 16rpx;
  padding: 30rpx;
  margin-bottom: 20rpx;
  box-shadow: 0 4rpx 12rpx rgba(0, 0, 0, 0.08);
}
.profile-card .left {
  margin-right: 30rpx;
  text-align: center;
}
.profile-card .avatar {
  width: 150rpx;
  height: 150rpx;
  border-radius: 50%;
  border: 2rpx solid #eee;
}
.health-status {
  font-size: 24rpx;
  padding: 6rpx 12rpx;
  border-radius: 20rpx;
  margin-top: 10rpx;
}
.health-status.normal {
  background: #e7f7e4;
  color: #3cc51f;
}
.health-status.warning {
  background: #ffebeb;
  color: #ff5500;
}
.profile-card .name {
  font-size: 36rpx;
  font-weight: bold;
  color: #333;
  display: block;
  margin-bottom: 8rpx;
}
.profile-card .gender {
  font-size: 28rpx;
  color: #666;
  display: block;
  margin-bottom: 20rpx;
}
.quick-actions {
  display: flex;
  justify-content: space-between;
  margin-top: 20rpx;
}
.action-btn {
  text-align: center;
  padding: 10rpx 15rpx;
  background: #f5f5f5;
  border-radius: 10rpx;
  flex: 1;
  margin: 0 5rpx;
}
.action-btn text {
  display: block;
  font-size: 24rpx;
  margin-top: 5rpx;
}

/* 健康面板 */
.health-panel, .medicine-panel {
  background: #fff;
  border-radius: 16rpx;
  padding: 25rpx;
  margin-bottom: 20rpx;
  box-shadow: 0 4rpx 12rpx rgba(0, 0, 0, 0.06);
}
.panel-title {
  display: flex;
  justify-content: space-between;
  margin-bottom: 20rpx;
  font-size: 30rpx;
  font-weight: 500;
}
.panel-title .more {
  font-size: 26rpx;
  color: #3cc51f;
}
.health-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 20rpx;
}
.health-item {
  background: #f9f9f9;
  border-radius: 10rpx;
  padding: 20rpx;
  text-align: center;
}
.health-item .label {
  font-size: 26rpx;
  color: #666;
  display: block;
}
.health-item .value {
  font-size: 36rpx;
  font-weight: bold;
  color: #333;
  display: block;
  margin: 5rpx 0;
}
.health-item .warning {
  color: #ff5500;
}
.health-item .unit {
  font-size: 24rpx;
  color: #999;
}

/* 用药提醒 */
/* 用药项整体：Flex 横向排列，强制不换行 */
.medicine-item {
  display: flex;
  align-items: center;
  justify-content: space-between; /* 元素均匀分布 */
  padding: 20rpx; /* 增加内边距，更宽松 */
  border-bottom: 1rpx solid #eee;
  transition: all 0.2s;
  white-space: nowrap; /* 强制内部元素不换行 */
}

.medicine-item:last-child {
  border-bottom: none;
}

/* 日期：固定宽度+溢出省略 */
.medicine-item .time {
  font-size: 26rpx;
  color: #666;
  width: 120rpx; /* 足够容纳“09-30” */
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis; /* 过长显示“...” */
  margin-right: 15rpx;
}

/* 药品名称：弹性空间+溢出省略 */
.medicine-item .name {
  font-size: 28rpx;
  color: #333;
  flex: 1; /* 占据中间剩余空间 */
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis; /* 过长显示“...” */
  margin: 0 15rpx;
}

/* 剂量：固定宽度+溢出省略 */
.medicine-item .dosage {
  font-size: 26rpx;
  color: #999;
  width: 100rpx; /* 足够容纳“1片/次” */
  text-align: right;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis; /* 过长显示“...” */
  margin-right: 15rpx;
}

/* 底部导航 */
.bottom-tabs {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  display: flex;
  background: #fff;
  box-shadow: 0 -2rpx 10rpx rgba(0, 0, 0, 0.05);
  padding: 15rpx 0;
  z-index: 100;
}
.tab-item {
  flex: 1;
  text-align: center;
  font-size: 24rpx;
  color: #666;
}
.tab-item.active {
  color: #3cc51f;
}
.tab-item text {
  display: block;
  margin-top: 5rpx;
}

/* 内容区 */
.content-area {
  min-height: 300rpx;
}

/* 新增加载和错误样式 */
.loading {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100vh;
  background-color: #f8f8f8;
}
.loading-spinner {
  width: 60rpx;
  height: 60rpx;
  border: 4rpx solid #f3f3f3;
  border-top: 4rpx solid #3cc51f;
  border-radius: 50%;
  animation: spin 1s linear infinite;
  margin-bottom: 20rpx;
}
.error-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100vh;
  background-color: #f8f8f8;
  padding: 0 40rpx;
}
.error-icon {
  width: 120rpx;
  height: 120rpx;
  margin-bottom: 20rpx;
}
.error-text {
  font-size: 28rpx;
  color: #ff5500;
  text-align: center;
  margin-bottom: 40rpx;
}
.retry-btn {
  background-color: #3cc51f;
  color: white;
  padding: 16rpx 60rpx;
  border-radius: 30rpx;
  font-size: 28rpx;
}
@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}
/* 用药提醒空状态 */
.empty-medication {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 80rpx 0;
  color: #999;
}
.empty-icon {
  width: 120rpx;
  height: 120rpx;
  margin-bottom: 20rpx;
  opacity: 0.6;
}
.empty-text {
  font-size: 28rpx;
}
/* 新增：用药详情弹窗样式 */
/* 新增：用药详情弹窗样式 */
.medicine-modal {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: rgba(0, 0, 0, 0.5);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 1000;
}

.modal-content {
  background-color: #fff;
  border-radius: 16rpx;
  width: 80%;
  max-width: 600rpx;
  padding: 30rpx;
  box-shadow: 0 4rpx 20rpx rgba(0, 0, 0, 0.15);
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 30rpx;
  border-bottom: 1rpx solid #eee;
  padding-bottom: 20rpx;
}

.modal-title {
  font-size: 32rpx;
  font-weight: bold;
  color: #333;
}

.modal-body {
  max-height: 60vh;
  overflow-y: auto;
}

.detail-item {
  display: flex;
  margin-bottom: 20rpx;
  align-items: flex-start;
}

.detail-label {
  font-size: 28rpx;
  color: #666;
  width: 140rpx;
  flex-shrink: 0;
}

.detail-value {
  font-size: 28rpx;
  color: #333;
  flex: 1;
}

.active-status {
  color: #3cc51f;
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

</style>