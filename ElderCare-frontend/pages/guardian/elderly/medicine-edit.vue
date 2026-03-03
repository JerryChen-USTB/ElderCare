<template>
  <view class="medicine-edit-container">
    <form @submit="submitMedication" report-submit>
      <view class="form-item">
        <text class="label required">药品名称：</text>
        <input v-model="medicineName" placeholder="请输入药品名称" />
        <text class="error-text" v-if="errors.medicineName">{{ errors.medicineName }}</text>
      </view>
      <view class="form-item">
        <text class="label required">剂量：</text>
        <input v-model="dosage" placeholder="请输入剂量（如 1片/次）" />
        <text class="error-text" v-if="errors.dosage">{{ errors.dosage }}</text>
      </view>
      <view class="form-item">
        <text class="label required">服用频率：</text>
        <input v-model="frequency" placeholder="请输入频率（如 每日3次）" />
        <text class="error-text" v-if="errors.frequency">{{ errors.frequency }}</text>
      </view>
      
      <!-- 开始日期：滚动选择器 -->
      <view class="form-item">
        <text class="label required">开始日期：</text>
        <picker 
          mode="date" 
          :value="startDate" 
          start="1970-01-01" 
          end="2100-12-31"
          @change="onStartDateChange"
          class="date-picker"
        >
          <view class="picker-view">
            {{ startDate || '请选择开始日期' }}
          </view>
        </picker>
        <text class="error-text" v-if="errors.startDate">{{ errors.startDate }}</text>
      </view>
      
      <!-- 结束日期：滚动选择器 -->
      <view class="form-item">
        <text class="label">结束日期：</text>
        <picker 
          mode="date" 
          :value="endDate" 
          end="2100-12-31"
          @change="onEndDateChange"
          class="date-picker"
        >
          <view class="picker-view" :class="{'gray-placeholder': !endDate}">
            {{ endDate || '请选择结束日期（可选）' }}
          </view>
        </picker>
        <text class="error-text" v-if="errors.endDate">{{ errors.endDate }}</text>
      </view>
	  <!-- 药物过期日期：滚动选择器（新增） -->
	  <view class="form-item">
	    <text class="label">药物过期日期：</text>
	    <picker 
	      mode="date" 
	      :value="expireDate" 
	      end="2100-12-31"
	      @change="onExpireDateChange"
	      class="date-picker"
	    >
	      <view class="picker-view" :class="{'gray-placeholder': !expireDate}">
	        {{ expireDate || '请选择药物过期日期（可选）' }}
	      </view>
	    </picker>
	    <text class="error-text" v-if="errors.expireDate">{{ errors.expireDate }}</text>
	  </view>
	  
      
      <view class="form-item">
        <text class="label">备注：</text>
        <textarea v-model="notes" placeholder="选填：用药备注" />
      </view>
      
      <button 
        type="submit" 
        class="submit-btn"
		@click="submitMedication"
        :loading="isSubmitting"
        :disabled="isSubmitting"
      >
        {{ isSubmitting ? '提交中...' : '提交新增' }}
      </button>
    </form>
    
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
import request from '@/utils/request.js';
import EmergencyModal from '@/components/emergency-modal.vue'
import EmergencyModalMixin from '@/mixins/emergency-modal.js'

export default {
  components: { EmergencyModal },
  mixins: [EmergencyModalMixin],
  data() {
    return {
      userId: '',          // 老人ID（从路由参数获取）
      medicineName: '',    // 药品名称
      dosage: '',          // 剂量
      frequency: '',       // 服用频率
      startDate: '',       // 开始日期（yyyy-MM-dd）
      endDate: '',         // 结束日期（yyyy-MM-dd）
	  expireDate: '',      // 新增：药物过期日期（yyyy-MM-dd）
      notes: '',           // 备注
      errors: {},          // 表单验证错误信息
      isSubmitting: false, // 提交状态，防止重复提交
	  currentDate: this.getCurrentDate() // 新增：当前日期（用于过期日期选择限制）
    }
  },
  onLoad(options) { 
    console.log('用药记录编辑页加载，接收参数：', options);
    // 从路由参数提取老人ID（兼容不同参数名）
    this.userId = options.userId || options.elderId || options.id;
    if (this.userId) {
      console.log('获取到老人ID：', this.userId);
    } else {
      uni.showToast({ title: '参数错误：未获取到老人ID', icon: 'none' });
      console.error('未从路由参数中获取到用户ID');
      setTimeout(() => {
        uni.navigateBack();
      }, 1500);
    }
	this.startDate = this.getCurrentDate();
  },
  methods: {
	  // 格式化当前日期为 yyyy-MM-dd
	  getCurrentDate() {
	    const now = new Date();
	    const year = now.getFullYear();
	    const month = String(now.getMonth() + 1).padStart(2, '0'); // 月份从0开始，补0
	    const day = String(now.getDate()).padStart(2, '0');
	    return `${year}-${month}-${day}`;
	  },
	  onExpireDateChange(e) {
	    this.expireDate = e.detail.value;
	    const currentDate = this.getCurrentDate();
	    // 仅弹窗提示，不修改用户选择的日期
	      if (this.expireDate && this.expireDate < currentDate) {
	        uni.showToast({ 
	          title: '药物过期日期不能早于当前日期', 
	          icon: 'none',
	          duration: 2000 
	        });
	      }
	      console.log('药物过期日期已选择：', this.expireDate);
	  },
    onStartDateChange(e) {
      this.startDate = e.detail.value;
      console.log('开始日期已选择：', this.startDate);
      // 自动处理结束日期有效性
      if (this.endDate && this.endDate < this.startDate) {
        this.endDate = '';
        uni.showToast({ title: '结束日期已重置（需晚于开始日期）', icon: 'none', duration: 1500 });
      }
    },
    
    onEndDateChange(e) {
      this.endDate = e.detail.value;
      const currentDate = this.getCurrentDate();
      // 弹窗提示“不能早于当前日期”，不自动修改日期
        if (this.endDate && this.endDate < currentDate) {
          uni.showToast({ 
            title: '结束日期不能早于当前日期', // 明确禁止逻辑，删除“标为已完成”文案
            icon: 'none',
            duration: 2000 
          });
        }
        console.log('结束日期已选择：', this.endDate, '当前日期：', currentDate);
    },
    
    // 表单验证
    validateForm() {
      const errors = {};
	  const currentDate = this.getCurrentDate(); // 新增：定义当前日期变量
      
      // 药品名称验证
      if (!this.medicineName.trim()) {
        errors.medicineName = '药品名称不能为空';
      }
      
      // 剂量验证
      if (!this.dosage.trim()) {
        errors.dosage = '剂量不能为空';
      }
      
      // 频率验证
      if (!this.frequency.trim()) {
        errors.frequency = '服用频率不能为空';
      }
      
      // 开始日期验证
      if (!this.startDate) {
        errors.startDate = '开始日期不能为空';
      } else if (!this.isValidDate(this.startDate)) {
        errors.startDate = '日期格式错误（正确格式：yyyy-MM-dd）';
      }
      
      // 结束日期验证（微调文案，与弹窗一致）
        if (this.endDate && !this.isValidDate(this.endDate)) {
          errors.endDate = '日期格式错误（正确格式：yyyy-MM-dd）';
        } else if (this.startDate && this.endDate && this.endDate < this.startDate) {
          errors.endDate = '结束日期不能早于开始日期';
        } else if (this.endDate && this.endDate < currentDate) { // 新增：结束日期早于当前的校验
          errors.endDate = '结束日期不能早于当前日期'; // 与弹窗文案一致
        }
      
        // 药物过期日期验证（文案不变，确保提交拦截）
        if (this.expireDate) {
          if (!this.isValidDate(this.expireDate)) {
            errors.expireDate = '日期格式错误（正确格式：yyyy-MM-dd）';
          } else if (this.expireDate < currentDate) {
            errors.expireDate = '药物过期日期不能早于当前日期';
          }
        }

      
      this.errors = errors;
      return Object.keys(errors).length === 0;
    },
    
    // 日期格式验证
    isValidDate(dateString) {
      const reg = /^\d{4}-\d{2}-\d{2}$/;
      if (!reg.test(dateString)) return false;
      
      const date = new Date(dateString);
      return date.toISOString().slice(0, 10) === dateString;
    },
    
    async submitMedication(e) {
      try {
        e.preventDefault();
        console.log('===== 开始提交表单 =====', e);  // 确认事件触发
        
        // 表单验证（添加日志）
        const isValid = this.validateForm();
        console.log('表单验证结果：', isValid, '错误信息：', this.errors);
        
        if (!isValid) {
          uni.showToast({ 
            title: '请完善表单信息', 
            icon: 'none',
            duration: 2000
          });
          return;
        }
        
        // 获取用户信息（添加日志）
        const userInfo = uni.getStorageSync('userInfo');
        const token = uni.getStorageSync('auth_token') || uni.getStorageSync('token');
        console.log('用户信息：', userInfo, 'Token：', token ? '存在' : '不存在');
        
        if (!userInfo || !userInfo.id) {
          console.error('未获取到用户信息');
          uni.showToast({ title: '请先登录', icon: 'none' });
          return;
        }
        
        if (!this.userId) {
          console.error('老人ID不存在');
          uni.showToast({ title: '参数错误', icon: 'none' });
          return;
        }
        
        // 构建请求数据
        const requestData = {
          medicineName: this.medicineName.trim(),
          dosage: this.dosage.trim(),
          frequency: this.frequency.trim(),
          startDate: this.startDate,
          endDate: this.endDate || null,
          expireDate: this.expireDate || null,
          notes: this.notes.trim() || null
        };
        console.log('请求数据：', requestData);
        
        // 直接拼接 URL 参数（兼容 uni-app 所有端）
        const requestUrl = `/api/guardian/medication/add/${this.userId}?guardianUserId=${userInfo.id}`;
        console.log('请求URL：', requestUrl);
        
        this.isSubmitting = true;
        
        // 发送请求
        const response = await request.post(
          requestUrl,
          requestData,
          { headers: { 'Authorization': `Bearer ${token}` } }
        );
        console.log('接口响应：', response);
        
        if (response && (response.code === 200 || response.success)) {
              uni.showToast({ title: '添加成功', icon: 'success' });
              // 关键修改：触发事件，传递当前老人的 userId（列表页需此参数匹配）
              uni.$emit('medicationAdded', this.userId); 
              setTimeout(() => uni.navigateBack(), 1500); // 返回列表页
            }
      } catch (error) {
        // 捕获所有异常并输出
        console.error('提交异常：', error);
        uni.showToast({ 
          title: error.errMsg || '提交失败，请重试', 
          icon: 'none' 
        });
      } finally {
        this.isSubmitting = false;
        console.log('===== 提交流程结束 =====');
      }
    }
	}
}
</script>

<style scoped>
/* 样式保持不变 */
.medicine-edit-container {
  padding: 20rpx;
  background-color: #f9f9f9;
  min-height: 100vh;
}
.form-item {
  display: flex;
  flex-direction: column;
  margin-bottom: 30rpx;
  padding: 0 10rpx;
}
.label {
  font-size: 28rpx;
  color: #333;
  margin-bottom: 10rpx;
  display: flex;
  align-items: center;
}
.required::before {
  content: '*';
  color: #ff4d4f;
  margin-right: 5rpx;
}
input, textarea, .picker-view {
  border: 1rpx solid #ddd;
  border-radius: 8rpx;
  padding: 15rpx;
  font-size: 28rpx;
  background-color: #fff;
}
input:focus, textarea:focus {
  border-color: #3cc51f;
  outline: none;
}
textarea {
  min-height: 120rpx;
  resize: vertical;
}
.error-text {
  color: #ff4d4f;
  font-size: 24rpx;
  margin-top: 8rpx;
  min-height: 24rpx;
}
.submit-btn {
  width: 100%;
  background: #3cc51f;
  color: #fff;
  border-radius: 8rpx;
  padding: 20rpx;
  font-size: 30rpx;
  margin-top: 40rpx;
  border: none;
}
.submit-btn:active {
  background: #36b31a;
}
.submit-btn:disabled {
  background: #99d88c;
  opacity: 0.8;
}
.date-picker {
  border: 1rpx solid #ddd;
  border-radius: 8rpx;
}
.picker-view {
  border: none;
  padding: 15rpx;
  width: 100%;
}
.gray-placeholder {
  color: #999;
}
</style>