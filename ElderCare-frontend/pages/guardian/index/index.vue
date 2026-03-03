<template>
  <view class="user-index">
    <!-- 顶部欢迎信息 -->
    <view class="header">
      <text class="welcome-text">监护人{{ userName }}，欢迎您！</text>
      <text class="info-text">当前绑定用户：{{ elderList.length }}人</text>
    </view>

    <!-- 功能选项导航栏 -->
    <view class="function-nav">
      <view
        class="function-item"
        :class="{active: currentTab === 'binduser'}"
        @click="switchTab('binduser')"
      >
        绑定用户
      </view>
      <view
        class="function-item"
        :class="{active: currentTab === 'healthcheck'}"
        @click="switchTab('healthcheck')"
      >
        健康检测
      </view>
      <view
        class="function-item"
        :class="{active: currentTab === 'locationtracking'}"
        @click="switchTab('locationtracking')"
      >
        位置追踪
      </view>
    </view>

    <!-- 内容显示区域 -->
    <view class="content">
      <view v-show="currentTab === 'binduser'" class="binduser-content">
        <!-- 已绑定用户列表 -->
        <view class="card-list" v-if="elderList.length > 0">
          <view
            v-for="(elder, index) in elderList"
            :key="elder.userId"
            class="elder-card"
             @tap="handleCardTap(elder.id, index)"  
              @longpress="handleLongPress(elder.userId, index)" 
            >
            <!-- 卡片内容区域（左滑时位移） -->
            <view class="card-content">
              <view class="left">
                <image class="avatar" :src="getAvatarUrl(elder.avatar)"></image>
              </view>
              <view class="right">
                <view class="info-row">
                  <text class="name">{{ elder.name }}（{{ elder.age }}岁）</text>
                </view>
                <view class="info-row">
                  <text class="info-label">健康状态：</text>
                  <text class="status">{{ elder.healthStatus || '暂无数据' }}</text>
                </view>
                <view class="info-row">
                  <text class="info-label">关系：</text>
                  <text class="relation">{{ elder.relation }}</text>
                </view>
                <view class="info-row">
                  <text class="info-label">绑定时间：</text>
                  <text class="bind-time">{{ elder.bindTime }}</text>
                </view>
              </view>
            </view>
          </view>
          
        </view>
        
        <!-- 空状态提示 -->
        <view class="empty-state" v-if="elderList.length === 0">
          <text class="empty-text">暂无绑定用户</text>
          <text class="empty-desc">点击下方按钮添加需要监护的用户</text>
        </view>

        <!-- 添加绑定用户区域 -->
        <view class="add-bind-user-card" @click="toggleAddBindUser">
          <view class="left">
            <image class="add-icon" src="/static/plususer.png"></image>
          </view>
          <view class="right">
            <text class="add-text">添加绑定用户</text>
          </view>
        </view>

        <!-- 添加绑定用户弹窗 -->
        <view v-show="showAddBindUser" class="add-bind-user-popup">
          <view class="add-bind-user-modal">
            <view class="modal-header">
              <text class="modal-title">添加绑定用户</text>
              <view class="close-btn" @click="toggleAddBindUser">×</view>
            </view>
            
            <view class="modal-body">
              <view class="form-group">
                <text class="form-label">被监护人账号</text>
                <view class="form-input-container">
                  <input 
                    class="form-input" 
                    type="text" 
                    placeholder="请输入被监护人账号绑定电话" 
                    v-model="bindForm.targetPhone" 
                    @blur="validateField('targetPhone')"
                  />
                </view>
                <text class="error-message" v-if="errors.targetPhone">
                  {{ errors.targetPhone }}
                </text>
              </view>
              
              <view class="form-group">
                <text class="form-label">被监护人姓名</text>
                <view class="form-input-container">
                  <input 
                    class="form-input" 
                    type="text" 
                    placeholder="请输入真实姓名" 
                    v-model="bindForm.targetUserName" 
                    @blur="validateField('targetUserName')"
                  />
                </view>
                <text class="error-message" v-if="errors.targetUserName">
                  {{ errors.targetUserName }}
                </text>
              </view>
              
              <view class="form-group">
                  <text class="form-label">与被监护人关系</text>
                  <view class="form-input-container">
                    <picker 
                      class="form-picker" 
                      :value="relationIndex" 
                      :range="relationOptions"
                      @change="handleRelationChange"
                    >
                      <view class="picker-view">
                        {{ bindForm.relation || '请选择关系' }}
                        <uni-icons type="arrowdown" size="24"></uni-icons>
                      </view>
                    </picker>
                  </view>
                  <text class="error-message" v-if="errors.relation">
                    {{ errors.relation }}
                  </text>
                </view>
              
              <view class="form-group">
                <text class="form-label">验证码</text>
                <view class="form-input-container code-container">
                  <input 
                    class="form-input" 
                    type="text" 
                    placeholder="请输入验证码" 
                    v-model="bindForm.verifyCode" 
                    maxlength="6"
                    @blur="validateField('verifyCode')"
                  />
                  <button 
                    class="get-code-btn" 
                    :disabled="counting || isSubmitting ||!validateField('targetPhone', true)"
                    @click="getVerifyCode"
                  >
                    {{ counting? `${countDown}s后重新获取` : '获取验证码' }}
                  </button>
                </view>
                <text class="error-message" v-if="errors.verifyCode">
                  {{ errors.verifyCode }}
                </text>
				        <text class="hint-text">验证码将发送到被监护者的绑定手机</text>
              </view>
            </view>
            
            <view class="modal-footer">
              <button 
                class="form-btn cancel" 
                @click="toggleAddBindUser"
                :disabled="isSubmitting"
              >
                取消
              </button>
              <button 
                class="form-btn confirm" 
                @click="submitBind"
                :disabled="!isFormValid || isSubmitting"
              >
                {{ isSubmitting? '提交中...' : '确认绑定' }}
              </button>
            </view>
          </view>
        </view>
      </view>

      <!-- 健康检测区域 -->
      <view v-show="currentTab === 'healthcheck'" class="healthcheck-content">
		  <!-- 1. 选择绑定用户（切换查看不同老人的健康数据） -->
		    <view class="user-selector" v-if="elderList.length > 0">
		      <picker 
		        class="user-picker" 
		        :value="selectedElderIndex" 
		        :range="elderList.map(item => item.name)"
		        @change="onElderChangeForHealth"
		      >
		        <view class="picker-view">
		          当前查看：{{ elderList[selectedElderIndex]?.name || '请选择老人' }}
		        </view>
		      </picker>
		    </view>
        <!-- SOS预警和提醒事项 -->
        <view class="emergency-area">
          <view class="sos-box" @click="navigateTo('emergencyc')">
            <image class="sos-icon" src="/static/sos-icon.png"></image>
            <text class="sos-text">SOS预警（{{ sosCount }}条）</text>
          </view>
          
          <!-- 健康统计卡片 -->
          <view class="health-stats">
            <view class="stats-card" v-for="(stat, index) in healthStats" :key="index">
              <view class="card-icon">
                <image :src="stat.icon" mode="aspectFit"></image>
              </view>
              <view class="card-content">
                <text class="card-title">{{ stat.title }}</text>
                <text class="card-value">{{ stat.value }}</text>
              </view>
            </view>
          </view>
          
          <!-- 健康趋势图 -->
          <view class="health-trend">
            <text class="trend-title">健康指标趋势</text>
			
			<!-- 维度切换+时间范围选择 -->
			<view class="time-filter-container">
			  <!-- 原有1日/7日切换 -->
			  <view class="dimension-tabs">
			    <view class="tab-item" :class="{active: currentDimension === '1day'}" @click="switchDimension('1day')">1日</view>
			    <view class="tab-item" :class="{active: currentDimension === '7day'}" @click="switchDimension('7day')">7日</view>
			  </view>
			  <!-- 新增自定义时间选择 -->
			  <view class="custom-time-selector">
			    <!-- 仅显示开始时间，无点击功能 -->
			    <view class="time-btn">
			      开始: {{ formatDateText(startDate) }}
			    </view>
			    <!-- 仅显示结束时间，无点击功能 -->
			    <view class="time-btn">
			      结束: {{ formatDateText(endDate) }}
			    </view>
			  </view>
			</view>
			
            <!-- 图表容器：使用uCharts组件 -->
                <view v-if="isLoadingChart" class="loading-chart">
                  <uni-icons type="loading" size="24" color="#009800"></uni-icons>
                  <text>加载中...</text>
                </view>
                <view v-else-if="chartData.series.length > 0" class="chart-container">
                  <qiun-data-charts
                    type="line" 
                    :chartData="chartData"  
                    :opts="chartOpts" 
                    width="100%"
                    height="400rpx"
                    @touchCallBack="onChartTouch" 
                  />
                </view>
                <view v-else class="empty-chart">
                  暂无{{ currentDimension === '1day' ? '1日' : '7日' }}内的健康数据
                </view>
			
          </view>
        </view>
      </view>

      <!-- 位置追踪区域 -->
      <view v-show="currentTab === 'locationtracking'" class="locationtracking-content">
        <!-- 选择要查看的用户（有用户时显示） -->
        <view class="location-selector" v-if="elderList.length > 0">
          <picker 
            class="location-picker" 
            :value="selectedElderIndex" 
            :range="elderList.map(item => item.name)"
            @change="onElderChange"
          >
            <view class="picker-view">
              查看：{{ elderList[selectedElderIndex]?.name || '请选择用户' }}
            </view>
          </picker>
        </view>
      
        <!-- 有绑定用户时：显示地图 + 位置信息 + 历史轨迹按钮 -->
        <view v-if="elderList.length > 0">
          <!-- 地图组件 -->
          <map
            :longitude="longitude"
            :latitude="latitude"
            :markers="markers"
            :scale="16"
            :show-location="true"
            style="width: 100%; height: 400px;"
          ></map>
          
          <!-- 位置信息（经纬度在这里显示） -->
          <view class="location-info" v-if="locationInfo">
            <text class="coordinates" v-if="locationInfo.coordinates">{{ locationInfo.coordinates }}</text>
            <text class="update-time">更新于：{{ locationInfo.updateTime }}</text>
          </view>
        
          <!-- 历史轨迹按钮：移到经纬度信息下方 -->
          <view class="history-btn" @click="showHistoryTrack">
            <uni-icons type="time" size="24"></uni-icons>
            <text>查看历史轨迹</text>
          </view>
        </view>
        
        <!-- 无绑定用户时：显示空状态 -->
        <view v-else class="location-empty">
          <text>请先绑定用户以查看位置信息</text>
        </view>
        
      </view>
    </view>

    <custom-tabbar :current="0" :role="'guardian'" />
    
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
import CustomTabbar from '@/components/custom-tabbar.vue'
import request from '@/utils/request.js'
import config from '@/utils/config.js'
import qiunDataCharts from '@/uni_modules/qiun-data-charts/components/qiun-data-charts/qiun-data-charts.vue'
import EmergencyModal from '@/components/emergency-modal.vue'
import EmergencyModalMixin from '@/mixins/emergency-modal.js'

export default {
  components: { CustomTabbar, qiunDataCharts, EmergencyModal },
  mixins: [EmergencyModalMixin],
  data() {
    return {
	  healthStats: null, 
	  userInfo: null, // 新增：存储用户信息
      isLoading: false,
	  userName: '', // 新增：存储用户昵称
      guardianUserId: '',
	  baseUrl: config.API_BASE_URL, 
      relationIndex: 0,
      elderList: [],
      bindForm: {
        targetPhone: '',
        targetUserName: '',
        relation: '',
        verifyCode: '',
        guardianId: ''
      },
      errors: {
        targetPhone: '',
        targetUserName: '',
        relation: '',
        verifyCode: ''
      },
      relationOptions: ['父亲', '母亲', '配偶', '子女', '祖父/母', '其他'],
      counting: false,
      countDown: 60,
      isSubmitting: false,
      currentTab: 'binduser',
      showAddBindUser: false,
      longitude: 0, // 初始化为0，待接口返回后更新
      latitude: 0,  // 初始化为0，待接口返回后更新
      markers: [
        {
          id: 0,
          longitude: 0,
          latitude: 0,
          title: '待获取位置',
          iconPath: '/static/location-marker.png',
          width: 30,
          height: 30
        }
      ],
      locationInfo: {
        updateTime: '暂无更新'
      },
	  locationInterval: null,
	  isLoadingLocation: false, // 新增：位置获取加载状态
      sosCount: 0,
      reminds: [
        { type: 'medicine', text: '3条用药提醒', icon: 'calendar' },
        { type: 'checkup', text: '2条体检提醒', icon: 'notification' }
      ],
	  // 健康趋势图相关变量
	      selectedElderIndex: 0, // 当前选中的老人索引
	      currentDimension: '1day', // 当前时间维度（1day/7day）
	      chartData: { // uCharts所需数据格式
	        categories: [], // X轴：时间点（如“08:00”“10-01”）
	        series: [] // 折线数据：[{name: '心率', data: [72,75,...]}, ...]
	      },
	      chartOpts: { // 图表样式配置
	        color: ['#009800', '#ff4444', '#2196F3'], // 折线颜色
	        grid: { top: 30, right: 20, bottom: 40, left: 50 }, // 内边距
	        xAxis: {
	          label: { fontSize: 12 }, // X轴文字大小
	          rotate: 30 // 时间文字旋转30度，避免重叠
	        },
	        yAxis: {
	          label: { fontSize: 12 },
	          unit: '' // 动态设置单位（如“次/分”）
	        },
	        series: {
	          type: 'line',
	          smooth: true, // 曲线平滑
	          symbol: 'circle', // 数据点样式
	          symbolSize: 6
	        },
	        legend: { show: true, position: 'top' } // 显示图例
	      },
	      isLoadingChart: false ,// 图表加载状态
		  // 新增：时间范围控制变量
		      startDate: new Date().toISOString(), // 开始时间（ISO格式）
		      endDate: '', // 结束时间（ISO格式）
		  
	    };
  },
  
  onLoad() {
    const userInfo = uni.getStorageSync('userInfo'); 
    if (!userInfo || !userInfo.id) {
      uni.showToast({ title: '请先登录', icon: 'none' });
      return;
    }
	this.userInfo = userInfo; // 关键：赋值给this.userInfo
    this.guardianUserId = userInfo.id;
    this.bindForm.guardianId = this.guardianUserId; 
	this.getUserInfo(); 
    this.loadBoundUsers();
  },
  
  onUnload() {
    if (this.locationInterval) {
      clearInterval(this.locationInterval);
    }
  },
  // 新增：生命周期函数，页面显示时触发（放在这里）
    onShow() {
      this.stopLocationTracking();
      if (this.currentTab === 'healthcheck' && this.elderList.length > 0) {
        this.selectedElderIndex = 0; // 默认选中第一个老人
        const now = new Date();
        this.endDate = now.toISOString(); // 结束时间：当前时间
        // 关键修改：1日维度用“近24小时”，与switchDimension逻辑一致
        this.startDate = this.currentDimension === '1day' 
          ? new Date(now.getTime() - 24 * 60 * 60 * 1000).toISOString() 
          : new Date(now.getTime() - 7 * 24 * 60 * 60 * 1000).toISOString();
        
        // 确保有选中的老人ID后，再请求数据
        if (this.currentElderUserId) {
          this.fetchHealthTrendData();
        }
      }
    },
  
  computed: {
    isFormValid() {
      return Object.values(this.errors).every(error =>!error) && 
             this.bindForm.targetPhone &&
             this.bindForm.targetUserName &&
             this.bindForm.relation &&
             this.bindForm.verifyCode;
    },
	currentElderUserId() {
	    return this.elderList[this.selectedElderIndex]?.userId || null;
	  }
  },
  
  methods: {
	  /** 格式化时间显示（用于UI展示） */
	  formatDateText(dateStr) {
	    const date = new Date(dateStr);
	    return `${date.getFullYear()}-${(date.getMonth()+1).toString().padStart(2, '0')}-${date.getDate().toString().padStart(2, '0')} ${date.getHours().toString().padStart(2, '0')}:${date.getMinutes().toString().padStart(2, '0')}`;
	  },
	  // 1. 切换选中的老人（健康数据专用）
	    onElderChangeForHealth(e) {
	      this.selectedElderIndex = e.detail.value;
	      this.fetchHealthTrendData(); // 切换后立即加载数据
	    },
	    
	    // 2. 切换时间维度（1日/7日）
	    switchDimension(dimension) {
	      this.currentDimension = dimension;
	      const now = new Date();
	      this.endDate = now.toISOString(); // 结束时间始终为当前时间
	    
	      // 根据维度设置开始时间：1日=当前时间-24小时，7日=当前时间-7天
	      if (dimension === '1day') {
	        // 关键修改：当前时间往前推24小时（而非当天0点）
	        this.startDate = new Date(now.getTime() - 24 * 60 * 60 * 1000).toISOString();
	      } else {
	        // 7日逻辑保持：当前时间往前推7天
	        this.startDate = new Date(now.getTime() - 7 * 24 * 60 * 60 * 1000).toISOString();
	      }
	    
	      this.fetchHealthTrendData(); // 重新加载筛选后的数据
	    },
	    
	    // 3. 请求后端健康趋势数据（已移除token，改用userInfo）
	    async fetchHealthTrendData() {
	      // 1. 基础校验
	      if (!this.currentElderUserId) {
	        this.chartData = { categories: [], series: [] };
	        return;
	      }
	      this.isLoadingChart = true;
	      const token = uni.getStorageSync('token'); // 新增：获取登录token
	    
	      try {
	        // 2. 调用后端历史数据接口（仅传后端支持的参数）
	        const res = await uni.request({
	          url: `${this.baseUrl}/api/guardian/health/history/${this.currentElderUserId}`,
	          method: 'GET',
	          header: {
	            'Content-Type': 'application/x-www-form-urlencoded',
	            'Authorization': `Bearer ${token}` // 新增：接口认证（避免401）
	          },
	          data: {
	            healthType: '', // 空=查询所有健康类型（心率/血压/步数）
	            page: 1, // 分页：第1页
	            size: 100 // 单次获取100条（覆盖多数场景）
	          }
	        });
	    
	        // 3. 处理响应（后端直接返回List<HealthInfo>，无code字段）
	        if (res.statusCode === 200) {
	          const allHistoryData = res.data; // 后端返回的全量历史数据
	          // 新增：前端时间范围筛选
	          const filteredData = this.filterDataByTime(allHistoryData);
	          // 格式化筛选后的数据并渲染图表
	          this.formatChartData(filteredData);
	        } else {
	          this.chartData = { categories: [], series: [] };
	          uni.showToast({ 
	            title: `获取数据失败（状态码：${res.statusCode}）`, 
	            icon: 'none' 
	          });
	        }
	      } catch (err) {
	        this.chartData = { categories: [], series: [] };
	        uni.showToast({ title: '网络错误，请重试', icon: 'none' });
	        console.error('健康数据请求失败：', err);
	      } finally {
	        this.isLoadingChart = false;
	      }
	    },
	    filterDataByTime(originalData) {
	      if (!originalData || originalData.length === 0) {
	        console.log('[筛选日志] 原始健康数据为空');
	        return [];
	      }
	    
	      // 1. 统一转换为时间戳（毫秒），避免时区偏差
	      const start = new Date(this.startDate).getTime();
	      const end = new Date(this.endDate).getTime();
	      console.log(`[筛选日志] 时间范围：${new Date(start).toLocaleString()} ~ ${new Date(end).toLocaleString()}`);
	    
	      // 2. 筛选逻辑：兼容recordTime为「ISO字符串」或「毫秒时间戳」
	      const filteredData = originalData.filter(item => {
	        let recordTimeStamp;
	        // 处理不同格式的recordTime
	        if (typeof item.recordTime === 'string') {
	          recordTimeStamp = new Date(item.recordTime).getTime(); // 字符串转时间戳
	        } else if (typeof item.recordTime === 'number') {
	          recordTimeStamp = item.recordTime; // 直接使用时间戳
	        } else {
	          console.warn('[筛选日志] 无效的recordTime类型', typeof item.recordTime, item.recordTime);
	          return false;
	        }
	    
	        // 3. 校验时间有效性并筛选（包含边界值）
	        if (isNaN(recordTimeStamp)) {
	          console.warn('[筛选日志] 无效的recordTime值', item.recordTime);
	          return false;
	        }
	        const isInRange = recordTimeStamp >= start && recordTimeStamp <= end;
	        if (!isInRange) {
	          console.log(`[筛选日志] 数据${item.recordTime}（${new Date(recordTimeStamp).toLocaleString()}）超出范围`);
	        }
	        return isInRange;
	      });
	    
	      console.log(`[筛选日志] 筛选前：${originalData.length}条，筛选后：${filteredData.length}条`);
	      return filteredData;
	    },
	    // 4. 格式化数据为uCharts所需格式（保持不变）
	    formatChartData(healthRecords) {
	      // 按健康类型分组（心率、血压、步数）
	      const grouped = {
	        heart_rate: [],
	        blood_pressure: [],
	        steps: []
	      };
	      
	      // 遍历原始数据，填充分组
	      healthRecords.forEach(record => {
	        const time = this.formatTimeForChart(record.recordTime); // 格式化时间
	        const type = record.healthType;
	        const value = record.value;
	        
	        switch (type) {
	          case 'heart_rate':
	            grouped.heart_rate.push({ time, value: Number(value) });
	            break;
	          case 'blood_pressure':
	            // 血压格式：“130/85” → 拆分为收缩压和舒张压
	            const [sys, dia] = value.split('/').map(Number);
	            if (!isNaN(sys) && !isNaN(dia)) {
	              grouped.blood_pressure.push({ time, sys, dia });
	            }
	            break;
	          case 'steps':
	            grouped.steps.push({ time, value: Number(value) });
	            break;
	        }
	      });
	      
	      // 提取所有时间点（去重并排序）
	      const allTimes = [...new Set(
	        [...grouped.heart_rate.map(item => item.time),
	         ...grouped.blood_pressure.map(item => item.time),
	         ...grouped.steps.map(item => item.time)]
	      )].sort((a, b) => new Date(`2025-01-01 ${a}`) - new Date(`2025-01-01 ${b}`));
	      
	      // 构建图表series数据
	      const series = [];
	      // 心率
	      if (grouped.heart_rate.length > 0) {
	        series.push({
	          name: '心率',
	          data: allTimes.map(time => {
	            const item = grouped.heart_rate.find(i => i.time === time);
	            return item?.value || 0;
	          }),
	          unit: '次/分'
	        });
	      }
	      // 血压（收缩压+舒张压）
	      if (grouped.blood_pressure.length > 0) {
	        series.push({
	          name: '收缩压',
	          data: allTimes.map(time => {
	            const item = grouped.blood_pressure.find(i => i.time === time);
	            return item?.sys || 0;
	          }),
	          unit: 'mmHg'
	        });
	        series.push({
	          name: '舒张压',
	          data: allTimes.map(time => {
	            const item = grouped.blood_pressure.find(i => i.time === time);
	            return item?.dia || 0;
	          }),
	          unit: 'mmHg'
	        });
	      }
	      // 步数
	      if (grouped.steps.length > 0) {
	        series.push({
	          name: '步数',
	          data: allTimes.map(time => {
	            const item = grouped.steps.find(i => i.time === time);
	            return item?.value || 0;
	          }),
	          unit: '步'
	        });
	      }
	      
	      // 更新图表数据
	      this.chartData = { categories: allTimes, series };
	      // 更新Y轴单位（取第一个series的单位）
	      if (series.length > 0) {
	        this.chartOpts.yAxis.unit = series[0].unit;
	      }
	    },
	    
	    // 辅助方法：格式化时间为图表X轴显示格式（保持不变）
	    formatTimeForChart(timeStr) {
	      const date = new Date(timeStr);
	      if (this.currentDimension === '1day') {
	        // 1日：显示“小时:分钟”（如“08:30”）
	        return `${date.getHours().toString().padStart(2, '0')}:${date.getMinutes().toString().padStart(2, '0')}`;
	      } else {
	        // 7日：显示“月-日”（如“10-01”）
	        return `${(date.getMonth() + 1).toString().padStart(2, '0')}-${date.getDate().toString().padStart(2, '0')}`;
	      }
	    },
	    
	    // 可选：图表点击事件（显示数据点详情）（保持不变）
	    onChartTouch(e) {
	      const { componentType, dataIndex, seriesIndex } = e.detail;
	      if (componentType === 'series') {
	        const time = this.chartData.categories[dataIndex];
	        const series = this.chartData.series[seriesIndex];
	        const value = series.data[dataIndex];
	        uni.showToast({
	          title: `${series.name}：${value}${series.unit}（${time}）`,
	          icon: 'none',
	          duration: 2000
	        });
	      }
	    },
	  async getUserInfo() {
	      try {
	        const res = await request.get(`/api/guardian/info/${this.guardianUserId}`);
	        this.userName = res.name || '用户'; // 无名字时默认显示“用户”
	      } catch (error) {
	        console.error('获取用户信息失败：', error);
	        this.userName = '用户'; // 错误时兜底显示“用户”
	      }
	    },
	  
	  // 在 methods 中添加 handleCardTap 方法
	  // 修改 handleCardTap 方法
	 handleCardTap(elderId, index) {
	   // 直接跳转详情页（无需额外判断，因为已无删除按钮和左滑状态）
	   this.navigateToElderDetail(elderId);
	 },
	  
	  async loadBoundUsers() {
	    this.isLoading = true;
	    const token = uni.getStorageSync('token');
	    
	    uni.request({
	      url: `${this.baseUrl}/api/guardian/related-elders/${this.guardianUserId}`, 
	      method: 'GET',
	      header: {
	        'Authorization': `Bearer ${token}`,
	        'Content-Type': 'application/x-www-form-urlencoded'
	      },
	      success: (res) => {
	        console.log('后端返回数据:', res.data); // 确认是数组格式
	        // 2. 直接处理数组（无需判断 res.data.code）
	        if (res.statusCode === 200) { 
	          // 后端返回的就是数组，直接映射
	          this.elderList = res.data.map(elder => this.formatElderData(elder));
	          if (this.elderList.length === 0) {
	            uni.showToast({ title: '暂无绑定用户', icon: 'none' });
	          } else {
	            console.log(`成功加载 ${this.elderList.length} 个绑定用户`);
	          }
	        } else {
	          this.elderList = [];
	          uni.showToast({ title: `加载失败（${res.statusCode}）`, icon: 'none' });
	        }
	      },
	      fail: (err) => {
	        this.elderList = [];
	        uni.showToast({ 
	          title: `网络错误：${err.errMsg}`, 
	          icon: 'none' 
	        });
	        console.error('请求失败:', err);
	      },
	      complete: () => {
	        this.isLoading = false;
	      }
	    });
	  },
	  formatElderData(elder) {
		  console.log('后端elder对象的createdAt:', elder.createdAt, '类型:', typeof elder.createdAt); // 新增日志
	    return {
	      id: elder.id,
		  userId: elder.userId,
	      name: elder.name || '未命名用户',
	      age: elder.age || '未知',
	      gender: elder.gender || '未知',
	      healthStatus: elder.healthCondition || '状态正常',
	      relation: elder.relationship || '未知关系',
	      bindTime: this.formatDate(elder.createdAt) || '未知时间',
	      avatar: elder.avatarUrl || '/static/avatar-default.png'
	    };
	  },
	    
	    formatGender(gender) {
	      const map = {
	        male: '男',
	        female: '女',
	        unknown: '未知'
	      };
	      return map[gender] || '未知';
	    },
	    
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
	    
    handleRelationChange(e) {
      this.relationIndex = e.detail.value;
      this.bindForm.relation = this.relationOptions[this.relationIndex];
    },
    
    switchTab(tab) {
      this.currentTab = tab;
      if (tab === 'locationtracking' && this.elderList.length > 0) {
        this.startLocationTracking();
      } else {
        this.stopLocationTracking();
      }
    
      // 关键修改：统一1日维度的startDate为“近24小时”
      if (tab === 'healthcheck' && this.elderList.length > 0) {
	    this.selectedElderIndex = 0; // 强制选中第一个老人（关键补充）
        const now = new Date();
        this.endDate = now.toISOString(); // 结束时间：当前时间
        // 1日维度：当前时间往前推24小时（与switchDimension逻辑一致）
        // 7日维度：当前时间往前推7天
        this.startDate = this.currentDimension === '1day' 
          ? new Date(now.getTime() - 24 * 60 * 60 * 1000).toISOString() 
          : new Date(now.getTime() - 7 * 24 * 60 * 60 * 1000).toISOString();
        
        // 确保有选中的老人ID后，再请求数据
        if (this.currentElderUserId) {
          this.fetchHealthTrendData();
        }
      }
    },
	
    
    formatDate(timeStr) {
      if (!timeStr) return '';
      // 处理ISO格式或时间戳（避免解析失败）
        const date = new Date(timeStr);
        // 检查日期是否有效（避免Invalid Date）
        if (isNaN(date.getTime())) return '';
        // 格式化为“年-月-日”
        return `${date.getFullYear()}-${(date.getMonth() + 1).toString().padStart(2, '0')}-${date.getDate().toString().padStart(2, '0')}`;
      },
    
    validateField(field, silent = false) {
      const value = this.bindForm[field];
      let error = '';
      
      switch(field) {
        case 'targetPhone':
          if (!value) {
            error = '请输入被监护人手机号';
          } else if (!/^1[3-9]\d{9}$/.test(value))  {
            error = '请输入正确的手机号格式';
          }
          break;
          
        case 'targetUserName':
          if (!value) {
            error = '请输入被监护人姓名';
          } else if (!/^[\u4e00-\u9fa5]{2,8}$/.test(value)) {
            error = '请输入2-8位中文字符';
          }
          break;
          
        case 'relation':
          if (!value) {
            error = '请选择与被监护人的关系';
          }
          break;
          
        case 'verifyCode':
          if (!value) {
            error = '请输入验证码';
          } else if (value.length!== 6 || isNaN(Number(value))) {
            error = '请输入6位数字验证码';
          }
          break;
      }
      
      this.errors[field] = error;
      return!error;
    },
    
    getVerifyCode() {
      if (!this.validateField('targetPhone')) {
        return;
      }
      
      this.counting = true;
      const timer = setInterval(() => {
        this.countDown--;
        if (this.countDown <= 0) {
          clearInterval(timer);
          this.counting = false;
          this.countDown = 60;
        }
      }, 1000);
      
      const token = uni.getStorageSync('token');
      
      uni.request({
        url: `${this.baseUrl}/api/guardian/relation/send-code`, // 统一使用 this.baseUrl
        method: 'POST',
        header: {
          'Authorization': `Bearer ${token}`,
          'Content-Type': 'application/x-www-form-urlencoded'
        },
        data: {
          targetPhone: this.bindForm.targetPhone
        },
        success: (res) => {
          // 添加调试日志：打印完整响应
          console.log('📱 [获取验证码] 完整响应:', res);
          console.log('📱 [获取验证码] statusCode:', res.statusCode);
          console.log('📱 [获取验证码] res.data:', res.data);
          console.log('📱 [获取验证码] res.data.code:', res.data?.code);
          console.log('📱 [获取验证码] res.data.data:', res.data?.data);
          
          if (res.statusCode === 200) {
            // 兼容多种响应格式
            let verifyCode = null;
            let isSuccess = false;
            
            // 格式1: { code: 200, data: "验证码" }
            if (res.data?.code === 200 && res.data?.data) {
              verifyCode = res.data.data;
              isSuccess = true;
            } 
            // 格式2: { code: 200, msg: "xxx", verifyCode: "验证码" }
            else if (res.data?.code === 200 && res.data?.verifyCode) {
              verifyCode = res.data.verifyCode;
              isSuccess = true;
            }
            // 格式3: 直接返回字符串验证码（没有 code 字段）
            else if (typeof res.data === 'string' && res.data.length === 6) {
              verifyCode = res.data;
              isSuccess = true;
            }
            // 格式4: { code: 200 } 但没有验证码字段（仅发送成功）
            else if (res.data?.code === 200) {
              isSuccess = true;
            }
            
            if (verifyCode) {
              console.log('✅ [获取验证码] 成功获取验证码:', verifyCode);
              // 自动填充到表单
              this.bindForm.verifyCode = verifyCode;
              
              // 顶部提示，显示验证码内容
              uni.showToast({
                title: `【银杏智伴】老年人已收到验证码，并确认后发送给您：${verifyCode}，请尽快验证！`,
                icon: 'none',
                duration: 3000,
                position: 'top'
              });
            } else if (isSuccess) {
              console.log('⚠️ [获取验证码] 发送成功但未找到验证码字段');
              // 如果后端没有返回验证码，显示普通提示
              uni.showToast({
                title: res.data?.msg || '验证码已发送',
                icon: 'none'
              });
            } else {
              console.error('❌ [获取验证码] 业务状态码异常:', res.data?.code, res.data?.msg);
              clearInterval(timer);
              this.counting = false;
              this.countDown = 60;
              uni.showToast({
                title: res.data?.msg || '发送失败',
                icon: 'none'
              });
            }
          } else {
            console.error('❌ [获取验证码] HTTP状态码异常:', res.statusCode);
            clearInterval(timer);
            this.counting = false;
            this.countDown = 60;
            uni.showToast({ title: '网络错误', icon: 'none' });
          }
        },
        fail: (err) => {
          clearInterval(timer);
          this.counting = false;
          this.countDown = 60;
          console.error('发送验证码失败:', err);
          uni.showToast({ title: '发送失败，请重试', icon: 'none' });
        }
      });
    },
    
    // 提交绑定（对接后端接口）
    submitBind() {
      Object.keys(this.bindForm).forEach(field => {
        this.validateField(field);
      });
    
      if (!this.isFormValid) {
        return;
      }
    
      this.isSubmitting = true;
    
      const token = uni.getStorageSync('token');
	  
	  console.log('提交绑定参数:', this.bindForm); // 添加日志
      
      uni.request({
        url: `${this.baseUrl}/api/guardian/relation/addbind`, 
        method: 'POST',
        header: {
          'Authorization': `Bearer ${token}`,
          'Content-Type': 'application/x-www-form-urlencoded'
        },
        data: {
          targetPhone: this.bindForm.targetPhone,
          targetName: this.bindForm.targetUserName,
          relation: this.bindForm.relation,
          verifyCode: this.bindForm.verifyCode,
          guardianId: this.bindForm.guardianId  // 补充监护人ID
        },
        success: (res) => {
              console.log('绑定响应:', res); // 添加详细响应日志
              if (res.statusCode === 200) {
                if (res.data.code === 200) {
                  uni.showToast({
                    title: '绑定成功',
                    icon: 'success',
                    duration: 2000
                  });
                  this.toggleAddBindUser();
                  this.loadBoundUsers();
                } else {
                  uni.showToast({
                    title: res.data.msg || '绑定失败',
                    icon: 'none'
                  });
                }
              } else {
                // 更详细的错误信息
                const errorMsg = res.data && res.data.message 
                  ? res.data.message 
                  : `服务器错误(${res.statusCode})`;
                uni.showToast({ 
                  title: errorMsg, 
                  icon: 'none',
                  duration: 3000
                });
              }
            },
            fail: (err) => {
              console.error('绑定请求失败:', err);
              uni.showToast({ 
                title: '请求失败: ' + err.errMsg, 
                icon: 'none',
                duration: 3000
              });
            },
            complete: () => {
              this.isSubmitting = false;
            }
          });
        },
		handleLongPress(elderId, index) {
		    // 触发手机震动（仅在支持震动的设备生效，提升体验）
		      uni.vibrateShort({
		        success: () => {},
		        fail: () => {}
		      });
		      // 调用解绑方法
		      this.handleUnbind(elderId, index);
		  },
    
    handleUnbind(elderUserId, index) {
      const elderName = this.elderList[index].name;
      uni.showModal({
        title: '确认解除绑定',
        content: `您确定要解除与${elderName}的监护关系吗？`,
        success: (res) => {
          if (res.confirm) {
            // 显示加载状态
            uni.showLoading({ title: '正在解除绑定...' });
            
            const token = uni.getStorageSync('token');
            uni.request({
              url: `${this.baseUrl}/api/guardian/relation/unbind`,
              method: 'POST',
              header: {
                'Authorization': `Bearer ${token}`,
                'Content-Type': 'application/x-www-form-urlencoded'
              },
              data: {
                guardianId: parseInt(this.guardianUserId), // 确保是数字类型
                elderlyId: parseInt(elderUserId) // 对应后端的elderlyId参数
              },
              success: (res) => {
                if (res.statusCode === 200) {
                  if (res.data.code === 200) {
                    uni.showToast({
                      title: '解除绑定成功',
                      icon: 'success'
                    });
                    // 重新加载用户列表，确保数据同步
                    this.loadBoundUsers();
                  } else {
                    uni.showToast({
                      title: res.data.msg || '解除绑定失败',
                      icon: 'none'
                    });
                  }
                } else {
                  uni.showToast({
                    title: `服务器错误: ${res.statusCode}`,
                    icon: 'none'
                  });
                }
              },
              fail: (err) => {
                console.error('解除绑定请求失败:', err);
                uni.showToast({
                  title: `网络错误: ${err.errMsg}`,
                  icon: 'none'
                });
              },
              complete: () => {
                // 无论成功失败都关闭加载
                uni.hideLoading();
              }
            });
          }
        }
      });
    },
    
    toggleAddBindUser() {
      this.showAddBindUser =!this.showAddBindUser;
      
      if (!this.showAddBindUser) {
        this.bindForm = {
          targetPhone: '',
          targetUserName: '',
          relation: '',
          verifyCode: '',
          guardianId: this.bindForm.guardianId
        };
        
        this.errors = {
          targetPhone: '',
          targetUserName: '',
          relation: '',
          verifyCode: ''
        };
        
        this.counting = false;
        this.countDown = 60;
        this.isSubmitting = false;
      }
    },    
    navigateToElderDetail(id) {
      const currentElder = this.elderList.find(elder => elder.id === id);
      if (!currentElder) return;
	  console.log('跳转携带数据:', { 
	      id, 
	      userId: currentElder.userId,
	      relationship: currentElder.relation
	    });
      uni.navigateTo({
        url: `/pages/guardian/elderlyc?userId=${currentElder.userId}`
      });
    },
    
    navigateTo(page) {
      uni.navigateTo({
        url: `/pages/guardian/${page}`
      });
    },
    
    handleRemind(type) {
      const pageMap = {
        medicine: 'medicineremind',
        checkup: 'physicalexamination'
      };
      this.navigateTo(pageMap[type] || type);
    },
    
    startLocationTracking() {
        if (this.locationInterval) {
          clearInterval(this.locationInterval); // 清除原有定时器
        }
        // 立即获取一次位置，再启动定时器（5秒刷新一次，可调整）
        this.fetchRealLocation();
        this.locationInterval = setInterval(() => {
          this.fetchRealLocation();
        }, 5000);
      },
    
    stopLocationTracking() {
      if (this.locationInterval) {
        clearInterval(this.locationInterval);
        this.locationInterval = null;
      }
    },
    
    fetchRealLocation() {
        // 1. 参数校验：确保监护人ID和选中的被监护人ID存在
        const userInfo = uni.getStorageSync('userInfo');
        const guardianUserId = userInfo?.id; // 监护人ID（从登录信息获取）
        const currentElder = this.elderList[this.selectedElderIndex]; // 当前选中的被监护人
        const currentElderUserId = currentElder?.userId; // 被监护人userId（关键参数）
    
        // 校验监护人信息
        if (!guardianUserId) {
          uni.showToast({ title: '监护人信息异常，请重新登录', icon: 'none' });
          this.stopLocationTracking(); // 停止定时器
          return;
        }
        // 校验被监护人信息
        if (!currentElderUserId) {
          this.locationInfo = { address: '请选择被监护人'};
          return;
        }
    
        // 2. 开始请求：显示加载状态
        this.isLoadingLocation = true;
    
        // 3. 调用后端位置接口（参考参考代码的接口地址和参数）
        uni.request({
          url: `${this.baseUrl}/api/elderly/location/get/${currentElderUserId}`, // 被监护人userId作为路径参数
          method: 'GET',
          header: {
            'guardianUserId': guardianUserId,   // 参考逻辑：传递监护人ID做权限校验
            'Content-Type': 'application/x-www-form-urlencoded'
          },
          success: (res) => {
            // 4. 接口响应处理（仅处理经纬度）
                  if (res.data?.code === 200) {
                    const locationData = res.data.data;
                    // 仅验证经纬度是否存在
                    if (typeof locationData?.latitude === 'number' && typeof locationData?.longitude === 'number') {
                      // 更新地图坐标
                      this.longitude = locationData.longitude;
                      this.latitude = locationData.latitude;
                      // 更新地图标记点
                      this.markers = [
                        {
                          id: 0,
                          longitude: this.longitude,
                          latitude: this.latitude,
                          title: `${currentElder.name}的位置`,
                          iconPath: '/static/location-marker.png',
                          width: 30,
                          height: 30
                        }
                      ];
                      // 更新位置信息（仅显示经纬度和时间）
                      this.locationInfo = {
                        // 显示经纬度信息
                        coordinates: `纬度: ${this.latitude.toFixed(6)}, 经度: ${this.longitude.toFixed(6)}`,
                        // 使用当前时间或后端返回的时间戳
                        updateTime: locationData.updateTime 
                          ? new Date(locationData.updateTime).toLocaleString()
                          : new Date().toLocaleString()
                      };
              } else {
          // 经纬度数据不完整
          this.locationInfo = { updateTime: '未获取到有效位置数据' };
        }
      } else if (res.data?.code === 403) {
        // 权限不足
        uni.showToast({ title: res.data.msg || '您无权限查看该用户位置', icon: 'none' });
        this.locationInfo = { updateTime: '权限不足' };
      } else {
        // 其他业务错误
        uni.showToast({ title: res.data.msg || '位置查询失败', icon: 'none' });
        this.locationInfo = { updateTime: '查询失败' };
      }
    },
          fail: (err) => {
                // 5. 网络错误处理
                console.error('获取位置失败（网络错误）：', err);
                uni.showToast({ title: '网络错误，无法获取位置', icon: 'none' });
                this.locationInfo = { updateTime: '网络错误' };
              },
              complete: () => {
                // 6. 结束请求：隐藏加载状态
                this.isLoadingLocation = false;
          }
        });
      },
    
    onElderChange(e) {
        this.selectedElderIndex = e.detail.value;
        // 切换被监护人后，停止原有定时器，重新启动位置追踪（立即更新位置）
        this.stopLocationTracking();
        this.startLocationTracking();
      },
    
    showHistoryTrack() {
      if (this.elderList.length === 0) {
        return;
      }
      
      uni.navigateTo({
        url: `/pages/guardian/locationhistory?elderId=${this.elderList[this.selectedElderIndex].id}`
      });
    }
  }
};
</script>


<style scoped>
.coordinates {
    font-size: 32rpx;
    margin-bottom: 12rpx;
    font-weight: 500;
    word-break: break-all;
}
/* 基础样式 */
.user-index {
  padding-bottom: 120rpx; /* 现有值 */
    min-height: 100vh;
    background-color: #f8f9fa;
}

/* 1. 顶部欢迎区优化（渐变+层次） */
.header {
  padding: 80rpx 30rpx 60rpx;
  background: linear-gradient(135deg, #009800 0%, #2dbf6b 100%);
  color: white;
  border-radius: 0 0 40rpx 40rpx;
  box-shadow: 0 8rpx 24rpx rgba(0, 152, 0, 0.15);
  animation: headerFadeIn 0.5s ease;
  position: relative;
  overflow: hidden;
  /* 确保子元素垂直排列 */
  display: flex;
  flex-direction: column;
  gap: 15rpx; /* 增加两行文本的间距 */
}


.header::after {
  content: '';
  position: absolute;
  top: -50rpx;
  right: -50rpx;
  width: 200rpx;
  height: 200rpx;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.15);
}

.welcome-text {
  font-size: 42rpx;
  font-weight: 600;
  margin-bottom: 15rpx;
  text-shadow: 0 2rpx 4rpx rgba(0, 0, 0, 0.1);
  position: relative;
  z-index: 1;
}

.info-text {
  font-size: 32rpx;
  opacity: 0.95;
  position: relative;
  z-index: 1;
}

@keyframes headerFadeIn {
  from { opacity: 0; transform: translateY(-20rpx); }
  to { opacity: 1; transform: translateY(0); }
}

/* 2. 功能导航栏优化（选中状态突出） */
.function-nav {
  display: flex;
  justify-content: space-around;
  margin: 30rpx 20rpx 0;
  background-color: #fff;
  border-radius: 30rpx;
  padding: 8rpx;
  box-shadow: 0 2rpx 8rpx rgba(0, 0, 0, 0.02);
}

.function-item {
  padding: 20rpx 40rpx;
  font-size: 30rpx;
  color: #777;
  border-radius: 22rpx;
  transition: all 0.3s ease;
  white-space: nowrap;
  position: relative;
}

.function-item:hover {
  color: #009800;
  background-color: #f0faf0;
}

.function-item.active {
  color: #009800;
  font-weight: 500;
  background-color: #f0faf0;
  box-shadow: 0 4rpx 12rpx rgba(0, 152, 0, 0.1);
}

.function-item.active::after {
  content: '';
  position: absolute;
  bottom: 8rpx;
  left: 50%;
  transform: translateX(-50%);
  width: 16rpx;
  height: 8rpx;
  background-color: #009800;
  border-radius: 4rpx;
}

/* 3. 内容区域统一样式 */
.content {
  margin-top: 20rpx;
  padding: 30rpx;
}

/* 绑定用户区域 */
.binduser-content {
  padding: 20rpx;
  background-color: #fff;
  border-radius: 16rpx;
  box-shadow: 0 4rpx 16rpx rgba(0, 0, 0, 0.05);
  display: flex;
  flex-direction: column;
  gap: 20rpx;
}

/* 已绑定用户列表 */
.card-list {
  margin-top: 20rpx;
}

/* 用户卡片容器（渐变背景+交互） */
.elder-card {
  display: flex;
  padding: 30rpx;
  background: linear-gradient(180deg, #f0faf0 0%, #e6f5e6 100%);
  border-radius: 20rpx;
  margin-bottom: 24rpx;
  box-shadow: 0 6rpx 16rpx rgba(0, 152, 0, 0.1);
  transition: all 0.3s ease;
  position: relative;
  overflow: hidden;
  align-items: center;
}

.elder-card:hover {
  transform: translateY(-4rpx);
  box-shadow: 0 8rpx 24rpx rgba(0, 152, 0, 0.15);
}

.elder-card:active {
  background-color: #f5f5f5;
}

/* 卡片内容区域（左滑动画） */
.card-content {
  flex: 1;
  display: flex;
  transition: transform 0.3s ease;
}

/* 头像区域（渐变光环+呼吸动效） */
.left {
  width: 120rpx;
  height: 120rpx;
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
}

.avatar {
  width: 100%;
  height: 100%;
  border-radius: 50%;
  object-fit: cover;
  border: 4rpx solid #fff;
  box-shadow: 0 4rpx 8rpx rgba(0, 152, 0, 0.2);
  position: relative;
  z-index: 1;
}

.left::before {
  content: '';
  position: absolute;
  width: 130rpx;
  height: 130rpx;
  border-radius: 50%;
  background: linear-gradient(45deg, #009800, #52c41a);
  opacity: 0.15;
  animation: pulse 3s infinite ease-in-out;
}

@keyframes pulse {
  0%, 100% { transform: scale(1); opacity: 0.15; }
  50% { transform: scale(1.05); opacity: 0.2; }
}

/* 信息区域 */
.right {
  margin-left: 24rpx;
  flex: 1;
}

.info-row {
  display: flex;
  align-items: center;
  margin-bottom: 12rpx;
}

.name {
  font-size: 36rpx;
  color: #333;
  font-weight: 600;
  margin-bottom: 6rpx;
}

.info-label {
  font-size: 28rpx;
  color: #999;
  width: 150rpx;
}

.status, .relation, .bind-time {
  font-size: 28rpx;
  color: #666;
}

.status:contains('正常') { color: #009800; }
.status:contains('异常') { color: #ff5252; }
.status:contains('暂无数据') { color: #999; }

/* 解除绑定按钮（渐变+交互） */


/* 空状态（淡入动效） */
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 350rpx;
  padding: 40rpx 20rpx;
  text-align: center;
  animation: fadeIn 0.5s ease;
}

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(20rpx); }
  to { opacity: 1; transform: translateY(0); }
}

.empty-text {
  font-size: 36rpx;
  color: #666;
  margin-bottom: 16rpx;
  line-height: 1.4;
  font-weight: 600;
}

.empty-desc {
  font-size: 28rpx;
  color: #999;
  line-height: 1.4;
}

/* 添加绑定用户卡片（渐变+hover） */
.add-bind-user-card {
  display: flex;
  padding: 35rpx;
  background: #fff;
  border-radius: 20rpx;
  box-shadow: 0 4rpx 16rpx rgba(0, 0, 0, 0.05);
  cursor: pointer;
  align-items: center;
  border: 2rpx dashed #c6e9c6;
  margin-top: auto;
  transition: all 0.3s ease;
}

.add-bind-user-card:hover {
  background-color: #f0faf0;
  border-color: #9cd69c;
  transform: translateY(-2rpx);
}

.add-bind-user-card .left {
  width: 120rpx;
  height: 120rpx;
  display: flex;
  align-items: center;
  justify-content: center;
}

.add-icon {
  width: 100%;
  height: 100%;
  border-radius: 50%;
  background: linear-gradient(135deg, #e6f5e6, #d6f0d6);
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.3s ease;
}

.add-bind-user-card:hover .add-icon {
  background: linear-gradient(135deg, #d6f0d6, #c6e9c6);
  transform: scale(1.05);
}

.add-icon image {
  width: 60rpx;
  height: 60rpx;
  opacity: 0.8;
}

.add-text {
  font-size: 34rpx;
  color: #009800;
  margin-left: 24rpx;
  font-weight: 500;
}

/* 添加绑定用户弹窗（模糊+渐变） */
.add-bind-user-popup {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background-color: rgba(0, 0, 0, 0.5);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 100;
  backdrop-filter: blur(8rpx);
}

.add-bind-user-modal {
  background-color: #fff;
  border-radius: 30rpx;
  box-shadow: 0 12rpx 40rpx rgba(0, 0, 0, 0.15);
  width: 88%;
  max-width: 700rpx;
  position: relative;
  overflow: hidden;
  animation: modalPopIn 0.3s ease;
}

@keyframes modalPopIn {
  from { opacity: 0; transform: scale(0.95) translateY(20rpx); }
  to { opacity: 1; transform: scale(1) translateY(0); }
}

.modal-header {
  padding: 40rpx 40rpx 25rpx;
  border-bottom: 1rpx solid #f0f0f0;
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: linear-gradient(to right, #009800, #2dbf6b);
}

.modal-title {
  font-size: 36rpx;
  font-weight: 600;
  color: #fff;
}

.close-btn {
  width: 48rpx;
  height: 48rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 44rpx;
  color: #fff;
  border-radius: 50%;
  background-color: rgba(255, 255, 255, 0.25);
  transition: all 0.3s ease;
}

.close-btn:hover {
  background-color: rgba(255, 255, 255, 0.35);
  transform: scale(1.1);
}

.modal-body {
  padding: 40rpx;
}

.form-group {
  margin-bottom: 30rpx;
}

.form-label {
  font-size: 30rpx;
  color: #666;
  display: block;
  margin-bottom: 15rpx;
}

.form-input-container {
  position: relative;
}

/* 输入框（聚焦发光） */
.form-input {
  width: 100%;
  height: 100rpx;
  border: 2rpx solid #eee;
  border-radius: 20rpx;
  padding: 0 30rpx;
  font-size: 32rpx;
  background-color: #f9f9f9;
  box-sizing: border-box;
  transition: all 0.3s ease;
}

.form-input:focus {
  border-color: #009800;
  box-shadow: 0 0 0 8rpx rgba(0, 152, 0, 0.1);
  background-color: #fff;
}

.hint-text {
  color: #999;
  font-size: 24rpx;
  margin-top: 8rpx;
  display: block;
}

/* 选择器 */
.picker-view {
  width: 100%;
  height: 100rpx;
  border: 2rpx solid #eee;
  border-radius: 20rpx;
  padding: 0 30rpx;
  font-size: 32rpx;
  background-color: #f9f9f9;
  box-sizing: border-box;
  display: flex;
  align-items: center;
  justify-content: space-between;
  color: #666;
  transition: all 0.3s ease;
}

.picker-view:focus-within {
  border-color: #009800;
  box-shadow: 0 0 0 8rpx rgba(0, 152, 0, 0.1);
  background-color: #fff;
}

/* 验证码容器 */
.code-container {
  display: flex;
  align-items: center;
  gap: 20rpx;
}

.code-container .form-input {
  flex: 1;
  margin-right: 0;
}

/* 按钮（渐变+交互） */
.get-code-btn {
  background: linear-gradient(to right, #009800, #2dbf6b);
  color: white;
  font-size: 28rpx;
  padding: 0 30rpx;
  height: 100rpx;
  border-radius: 20rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  white-space: nowrap;
  flex-shrink: 0;
  transition: all 0.3s ease;
}

.get-code-btn[disabled] {
  background: linear-gradient(to right, #d9eed9, #c6e9c6);
  color: #fff;
}

.get-code-btn:not([disabled]):hover {
  background: linear-gradient(to right, #008600, #29ab60);
  transform: scale(1.02);
}

.error-message {
  color: #ff4444;
  font-size: 24rpx;
  margin-top: 12rpx;
  padding-left: 4rpx;
}

.modal-footer {
  padding: 0 40rpx 40rpx;
  display: flex;
  justify-content: space-between;
  gap: 20rpx;
}

.form-btn {
  flex: 1;
  height: 100rpx;
  border-radius: 20rpx;
  font-size: 32rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 600;
  transition: all 0.3s ease;
}

.cancel {
  background-color: #f5f5f5;
  color: #666;
}

.cancel:hover {
  background-color: #eeeeee;
  transform: scale(1.02);
}

.confirm {
  background: linear-gradient(to right, #009800, #2dbf6b);
  color: white;
}

.confirm:hover {
  background: linear-gradient(to right, #008600, #29ab60);
  transform: scale(1.02);
}

.confirm[disabled] {
  background: linear-gradient(to right, #d9eed9, #c6e9c6);
  color: #fff;
}

/* 健康检测区域 */
.healthcheck-content {
  padding: 20rpx;
  background-color: #fff;
  border-radius: 16rpx;
  box-shadow: 0 4rpx 16rpx rgba(0, 0, 0, 0.05);
}

/* 健康检测区域 - SOS预警容器，新增上方间距 */
.emergency-area {
  margin-top: 24rpx; /* 关键：拉开与“当前查看”选择器的距离 */
  margin-bottom: 20rpx; /* 保持原有下方间距，可选 */
}

/* SOS盒 */
.sos-box {
  background: linear-gradient(135deg, #ff4444, #ff6b6b);
  padding: 32rpx;
  border-radius: 16rpx;
  text-align: center;
  color: white;
  margin-bottom: 30rpx;
  box-shadow: 0 4rpx 16rpx rgba(255, 68, 68, 0.15);
  transition: all 0.3s ease;
  position: relative;
  overflow: hidden;
}

.sos-box::before {
  content: '';
  position: absolute;
  top: -50%;
  left: -50%;
  width: 200%;
  height: 200%;
  background: rgba(255, 255, 255, 0.1);
  transform: rotate(30deg);
  animation: sosShine 3s infinite linear;
}

@keyframes sosShine {
  0% { transform: rotate(30deg) translateX(-100%); }
  100% { transform: rotate(30deg) translateX(100%); }
}

.sos-box:hover {
  transform: translateY(-4rpx);
  box-shadow: 0 8rpx 24rpx rgba(255, 68, 68, 0.25);
}

.sos-icon {
  width: 80rpx;
  height: 80rpx;
  margin-bottom: 16rpx;
}

.sos-text {
  font-size: 32rpx;
  font-weight: 500;
  text-shadow: 0 2rpx 4rpx rgba(0, 0, 0, 0.1);
}

/* 健康统计卡片（悬浮上浮） */
.health-stats {
  display: flex;
  justify-content: space-between;
  margin-bottom: 30rpx;
  gap: 20rpx;
}

.stats-card {
  flex: 1;
  background: linear-gradient(to bottom, #f0faf0, #e6f5e6);
  border-radius: 20rpx;
  padding: 28rpx;
  box-shadow: 0 4rpx 12rpx rgba(0, 152, 0, 0.1);
  transition: all 0.3s ease;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.stats-card:hover {
  transform: translateY(-6rpx);
  box-shadow: 0 8rpx 16rpx rgba(0, 152, 0, 0.15);
}

.stats-card:last-child {
  margin-right: 0;
}

/* 统计卡片图标 */
.card-icon {
  width: 70rpx;
  height: 70rpx;
  border-radius: 50%;
  background-color: #009800;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 16rpx;
  box-shadow: 0 4rpx 8rpx rgba(0, 152, 0, 0.25);
}

.card-icon image {
  width: 40rpx;
  height: 40rpx;
  opacity: 0.9;
}

.card-title {
  font-size: 26rpx;
  color: #666;
  margin-bottom: 8rpx;
  font-weight: 500;
}

.card-value {
  font-size: 32rpx;
  color: #333;
  font-weight: 500;
}

/* 健康趋势图 */
.health-trend {
  background-color: #f9f9f9;
  border-radius: 16rpx;
  padding: 24rpx;
}

.trend-title {
  font-size: 32rpx;
  color: #333;
  font-weight: 500;
  margin-bottom: 20rpx;
}

.trend-chart {
  height: 300rpx;
  border-radius: 12rpx;
  overflow: hidden;
}

.trend-chart image {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

/* 位置追踪区域 */
.locationtracking-content {
  padding: 20rpx;
  background-color: #fff;
  border-radius: 16rpx;
  box-shadow: 0 4rpx 16rpx rgba(0, 0, 0, 0.05);
  position: relative;
}

.location-selector {
  margin-bottom: 20rpx;
}

/* 位置选择器 */
.location-picker {
  width: 100%;
  background-color: #f9f9f9;
  border: 1rpx solid #eee; /* 新增：浅灰色边框，强化组件边界 */
  border-radius: 12rpx;
  padding: 24rpx;
  font-size: 30rpx;
  color: #333;
}

/* 地图组件 */
/* 位置追踪区域 - 地图组件，新增上方间距 */
map {
  border-radius: 20rpx !important;
  box-shadow: 0 4rpx 16rpx rgba(0, 152, 0, 0.08);
  overflow: hidden;
  z-index: 1; /* 确保地图层级低于按钮 */
}

/* 位置信息（主色渐变） */
.location-info {
  padding: 28rpx;
  background: linear-gradient(to right, #009800, #2dbf6b);
  color: white;
  border-radius: 20rpx;
  margin-top: 24rpx;
  box-shadow: 0 4rpx 12rpx rgba(0, 152, 0, 0.2);
}

.address {
  font-size: 32rpx;
  margin-bottom: 12rpx;
  font-weight: 500;
}

.update-time {
  font-size: 26rpx;
  color: rgba(255, 255, 255, 0.8);
}

/* 历史轨迹按钮（渐变+交互） */
/* 历史轨迹按钮（修复后，位于页面最下方） */
/* 历史轨迹按钮：放在经纬度下方，居中显示 */
.history-btn {
  /* 去掉fixed定位，改为相对定位，跟随父容器布局 */
  position: relative;
  /* 与上方经纬度信息保持间距 */
  margin-top: 24rpx;
  /* 水平居中 */
  margin-left: auto;
  margin-right: auto;
  /* 按钮样式优化 */
  background: linear-gradient(to right, #009800, #2dbf6b);
  color: white;
  padding: 18rpx 40rpx;
  border-radius: 40rpx;
  display: flex;
  align-items: center;
  justify-content: center; /* 内容居中 */
  box-shadow: 0 4rpx 12rpx rgba(0, 152, 0, 0.2);
  transition: all 0.3s ease;
  /* 限制按钮最大宽度，避免过宽 */
  max-width: 300rpx;
}

.history-btn:hover {
  transform: scale(1.05);
  background: linear-gradient(to right, #008600, #29ab60);
}

.history-btn text {
  margin-left: 12rpx;
  font-size: 28rpx;
}
/* 位置空状态 */
.location-empty {
  padding: 100rpx 0;
  text-align: center;
}

.location-empty image {
  width: 240rpx;
  height: 240rpx;
  opacity: 0.6;
  margin-bottom: 30rpx;
}

.location-empty text {
  font-size: 32rpx;
  color: #999;
}
.user-selector {
  margin: 20rpx 0;
}
.user-picker {
  width: 100%;
  padding: 24rpx 30rpx;
  background-color: #f9f9f9;
  border: 1rpx solid #eee; /* 新增：浅灰色边框，强化组件边界 */
  border-radius: 16rpx;
  font-size: 30rpx;
  color: #333;
}

/* 维度切换按钮样式 */
.dimension-tabs {
  display: flex;
  margin: 20rpx 0;
  gap: 20rpx;
}
.tab-item {
  flex: 1;
  text-align: center;
  padding: 16rpx 0;
  background-color: #f0f0f0;
  border-radius: 12rpx;
  font-size: 28rpx;
  color: #666;
  transition: all 0.3s;
}
.tab-item.active {
  background-color: #009800;
  color: white;
  font-weight: 500;
}

/* 图表容器样式 */
.chart-container {
  width: 100%;
  height: 400rpx;
  background-color: #fff;
  border-radius: 16rpx;
  padding: 10rpx;
  box-sizing: border-box;
}

/* 加载中和空数据样式 */
.loading-chart {
  width: 100%;
  height: 400rpx;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 16rpx;
  color: #666;
}
.empty-chart {
  width: 100%;
  height: 400rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #999;
  font-size: 28rpx;
}
/* 时间筛选容器 */
.time-filter-container {
  margin-bottom: 20rpx;
}

/* 自定义时间选择器（开始/结束时间按钮） */
.custom-time-selector {
  display: flex;
  gap: 20rpx;
  margin-top: 16rpx;
}

.time-btn {
  flex: 1;
  text-align: center;
  padding: 16rpx 0;
  background-color: #f0f0f0;
  border-radius: 12rpx;
  font-size: 26rpx;
  color: #666;
  transition: all 0.3s;
}

.time-btn:hover {
  background-color: #e6e6e6;
}
/* 日期选择器模态框：全屏覆盖 + 半透明背景 */
.datetime-picker-modal {
  position: fixed;        /* 固定定位，全屏覆盖 */
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background-color: rgba(0, 0, 0, 0.5); /* 半透明背景，突出选择器 */
  display: flex;          /* 居中显示选择器 */
  justify-content: center;
  align-items: center;
  z-index: 999;           /* 确保层级最高，覆盖所有内容 */
}

</style>