<template>
  <view class="profile-container">
    <!-- 头像和信息区域 -->
    <view class="profile-header">
      <image 
        class="profile-avatar" 
        :src="getAvatarUrl(userInfo.avatar)" 
        mode="aspectFill"
        @click="chooseAvatar"
        @error="handleAvatarError"
      ></image>
      <view class="profile-info">
        <text class="profile-name" @click="showNameEditModal">{{ userInfo.name || '加载中...' }}</text>
        <text class="profile-id">ID: {{ formattedUserId }}</text>
      </view>
      <button class="edit-btn" @click="navigateToEdit">
        <image src="/static/icons/edit.png" mode="aspectFit"></image>
      </button>
    </view>
	
	<!-- 基本信息卡片 -->
	<view class="profile-card">
	  <!-- 性别 -->
	  <view class="card-item">
	    <view class="item-left">
	      <text class="item-label">性别</text>
	      <text class="item-value">{{ getGenderText(basicInfo.gender) }}</text>
	    </view>
	    <image 
	      class="enter-icon" 
	      src="/static/icons/enter.png" 
	      mode="aspectFit"
	      @click="showGenderEditModal"
	    ></image>
	  </view>
	  
	  <!-- 出生日期 -->
	  <view class="card-item">
	    <view class="item-left">
	      <text class="item-label">出生日期</text>
	      <text class="item-value">{{ formatBirthday(basicInfo.birthday) }}</text>
	    </view>
	    <image 
	      class="enter-icon" 
	      src="/static/icons/enter.png" 
	      mode="aspectFit"
	      @click="showBirthdayEditModal"
	    ></image>
	  </view>
	  
	  <!-- 年龄（只读） -->
	  <view class="card-item readonly">
	    <view class="item-left">
	      <text class="item-label">年龄</text>
	      <text class="item-value">{{ calculateAge(basicInfo.birthday) }}</text>
	    </view>
	  </view>
	</view>
	

    <!-- 卡片式信息区域 -->
    <view class="profile-card">
      <view class="card-item" @click="showUnavailableTip">
        <view class="item-left">
          <image class="item-icon" src="/static/icons/health-record.png"></image>
          <text>健康档案</text>
        </view>
        <image class="arrow-icon" src="/static/icons/arrow-right.png"></image>
      </view>
      
      <view class="card-item" @click="showAddressViewModal">
        <view class="item-left">
          <image class="item-icon" src="/static/icons/address.png"></image>
          <text>地址管理</text>
        </view>
        <image class="arrow-icon" src="/static/icons/arrow-right.png"></image>
      </view>
      
      <view class="card-item" @click="showUnavailableTip">
        <view class="item-left">
          <image class="item-icon" src="/static/icons/family.png"></image>
          <text>家庭成员</text>
        </view>
        <image class="arrow-icon" src="/static/icons/arrow-right.png"></image>
      </view>
    </view>

    <!-- 姓名编辑弹窗 -->
    <uni-popup ref="nameEditPopup" type="dialog">
      <view class="name-edit-dialog">
        <view class="dialog-title">修改姓名</view>
        <input 
          v-model="newName" 
          placeholder="请输入新姓名" 
          class="name-input"
          maxlength="20"
        />
        <view class="dialog-buttons">
          <button class="cancel-btn" @click="cancelNameEdit">取消</button>
          <button class="confirm-btn" @click="confirmNameEdit">确认</button>
        </view>
      </view>
    </uni-popup>
    
    <!-- 性别编辑弹窗 -->
    <uni-popup ref="genderEditPopup" type="dialog">
      <view class="edit-dialog">
        <view class="dialog-title">选择性别</view>
        <picker 
          @change="onGenderChange" 
          :value="genderIndex" 
          :range="genderOptions"
          class="form-picker"
        >
          <view class="picker-display">
            {{ genderOptions[genderIndex] }}
            <text class="picker-arrow">▼</text>
          </view>
        </picker>
        <view class="dialog-buttons">
          <button class="cancel-btn" @click="$refs.genderEditPopup.close()">取消</button>
          <button class="confirm-btn" @click="confirmGenderEdit">确认</button>
        </view>
      </view>
    </uni-popup>
    
    <!-- 生日编辑弹窗 -->
    <uni-popup ref="birthdayEditPopup" type="dialog">
      <view class="edit-dialog">
        <view class="dialog-title">选择出生日期</view>
        <picker 
          mode="date" 
          :value="newBirthday" 
          @change="onBirthdayChange"
          :end="getCurrentDate()"
          class="form-picker"
        >
          <view class="picker-display">
            {{ newBirthday || '请选择日期' }}
            <text class="picker-arrow">▼</text>
          </view>
        </picker>
        <view class="dialog-buttons">
          <button class="cancel-btn" @click="$refs.birthdayEditPopup.close()">取消</button>
          <button class="confirm-btn" @click="confirmBirthdayEdit">确认</button>
        </view>
      </view>
    </uni-popup>
    
    <!-- 地址查看弹窗 -->
    <uni-popup ref="addressViewPopup" type="dialog">
      <view class="address-view-dialog">
        <view class="dialog-title">家庭地址</view>
        <view class="address-display">
          <view class="address-content">
            <text class="address-text">{{ basicInfo.address || '未设置地址' }}</text>
          </view>
          <image 
            class="address-edit-icon"
            src="/static/icons/edit.png" 
            mode="aspectFit"
            @click="showAddressEditModal"
          ></image>
        </view>
        <view class="dialog-buttons">
          <button class="cancel-btn" @click="closeAddressViewModal">关闭</button>
        </view>
      </view>
    </uni-popup>
    
    <!-- 地址编辑弹窗（普通文本输入） -->
    <uni-popup ref="addressEditPopup" type="dialog">
      <view class="edit-dialog">
        <view class="dialog-title">编辑家庭地址</view>
        
        <!-- 普通文本输入框替代省市区选择器 -->
        <textarea 
          v-model="newAddress" 
          placeholder="请输入完整家庭地址" 
          class="address-textarea"
          maxlength="200"
        />
        
        <view class="dialog-buttons">
          <button class="cancel-btn" @click="cancelAddressEdit">取消</button>
          <button class="confirm-btn" @click="confirmAddressEdit">保存</button>
        </view>
      </view>
    </uni-popup>
    
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
// 导入请求工具
import request from '@/utils/request.js'
import config from '@/utils/config.js'
import EmergencyModal from '@/components/emergency-modal.vue'
import EmergencyModalMixin from '@/mixins/emergency-modal.js'

export default {
  components: { EmergencyModal },
  mixins: [EmergencyModalMixin],
  data() {
    return {
      userInfo: {
        name: '',
        userId: null,
        phone: '',
        avatar: ''
      },
      basicInfo: {
        gender: 'unknown',
        birthday: '',
        address: ''
      },
      genderOptions: ['男', '女', '未设置'],
      genderIndex: 2,
      newName: '',
      newBirthday: '',
      newAddress: '',
    }
  },
  computed: {
    formattedUserId() {
      if (!this.userInfo.userId) return '加载中...'
      const idStr = this.userInfo.userId.toString()
      if (idStr.length <= 4) return idStr
      return idStr.substring(0, 3) + '****' + idStr.substring(idStr.length - 4)
    },
    getGenderText() {
      return (gender) => {
        const genderMap = { male: '男', female: '女', unknown: '未设置' }
        return genderMap[gender] || '未设置'
      }
    },
    formatBirthday() {
      return (birthday) => {
        if (!birthday) return '未设置';
        return birthday.split('T')[0];
      };
    },
    calculateAge() {
      return (birthday) => {
        if (!birthday) return '未知'
        const birthDate = new Date(birthday)
        const today = new Date()
        let age = today.getFullYear() - birthDate.getFullYear()
        if (today.getMonth() < birthDate.getMonth() || (today.getMonth() === birthDate.getMonth() && today.getDate() < birthDate.getDate())) {
          age--
        }
        return age > 0 ? age : 0
      }
    }
  },
  onShow() {
    this.loadGuardianInfo()
  },
  methods: {
	  // 新增：未开放功能的提示方法
	    showUnavailableTip() {
	      uni.showToast({
	        title: '功能暂未开放，敬请期待...',
	        icon: 'none', // 纯文本提示（不显示默认图标）
	        duration: 2000, // 提示显示时长（2秒）
	        mask: true // 显示遮罩，防止用户重复点击
	      });
	    },
	    
	    // 获取头像URL（添加BASE_URL和时间戳避免缓存）
	    getAvatarUrl(avatarUrl) {
	      if (avatarUrl && avatarUrl.trim() !== '') {
	        // 如果是相对路径，添加后端基础URL
	        // 支持 /uploads/ 和 /upload/ 两种格式
	        if (avatarUrl.startsWith('/uploads/') || avatarUrl.startsWith('/upload/')) {
	          // 如果已经带有时间戳，直接添加基础URL
	          if (avatarUrl.includes('?t=')) {
	            return config.API_BASE_URL + avatarUrl;
	          }
	          // 否则添加时间戳避免缓存
	          const fullUrl = config.API_BASE_URL + avatarUrl + '?t=' + Date.now();
	          return fullUrl;
	        }
	        return avatarUrl;
	      }
	      // 默认头像（Spring Boot静态资源自动映射）
	      return config.API_BASE_URL + '/uploads/avatars/default-avatar.png';
	    },
	    
	    // 头像加载失败处理
	    handleAvatarError() {
	      console.warn('头像加载失败，将显示默认头像');
	      // 清空avatarUrl，让getAvatarUrl返回默认头像
	      this.userInfo.avatar = '';
	      // 强制刷新视图
	      this.$forceUpdate();
	    },
	    
    async loadGuardianInfo() {
      try {
        const storedUser = uni.getStorageSync('userInfo');
        if (!storedUser || !storedUser.id) {
          uni.showToast({ title: '未获取到用户信息', icon: 'none' });
          return;
        }
        this.userId = storedUser.id;
        uni.showLoading({ title: '加载中...' });
        
        const guardian = await request.get(`/api/guardian/info/${this.userId}`);
        
        this.userInfo = {
          name: guardian?.name || '未知姓名',
          userId: guardian?.userId || '000000',
          phone: guardian?.phone || '未绑定',
          avatar: guardian?.avatarUrl || '' // 直接保存相对路径，由getAvatarUrl方法处理
        };
        
        this.basicInfo = {
          gender: guardian?.gender || 'unknown',
          birthday: guardian?.birthday || '',
          address: guardian?.address || ''
        };
        
        this.setGenderIndex(this.basicInfo.gender);
      } catch (error) {
        console.error('加载监护人信息失败:', error);
        uni.showToast({ title: '信息加载失败', icon: 'none' });
        this.userInfo = {
          name: '未获取到姓名',
          userId: '000000',
          phone: '未获取到手机号',
          avatar: '' 
        };
        this.basicInfo = {
          gender: 'unknown',
          birthday: '',
          address: ''
        };
        this.setGenderIndex('unknown');
      } finally {
        uni.hideLoading();
      }
    },

    goBack() {
      uni.navigateBack({ delta: 1 });
    },

    navigateToEdit() {
      uni.navigateTo({ url: '/pages/user/my/profile-edit' });
    },

    navigateTo(page) {
      const routes = {
        'health': '/pages/user/health/record',
        'address': '/pages/user/address/list',
        'family': '/pages/user/family/list',
        'security': '/pages/user/settings/security',
        'notification': '/pages/user/settings/notification',
        'privacy': '/pages/user/settings/privacy'
      };
      uni.navigateTo({ url: routes[page] });
    },

    handleLogout() {
      uni.showModal({
        title: '提示',
        content: '确定要退出登录吗？',
        success: (res) => {
          if (res.confirm) {
            uni.removeStorageSync('token');
            uni.reLaunch({ url: '/pages/login/login' });
          }
        }
      });
    },

    setGenderIndex(gender) {
      const genderMap = { male: 0, female: 1, unknown: 2 };
      this.genderIndex = genderMap[gender] ?? 2;
    },

    showGenderEditModal() {
      this.setGenderIndex(this.basicInfo.gender);
      this.$refs.genderEditPopup.open();
    },

    onGenderChange(e) {
      this.genderIndex = e.detail.value;
    },

    async confirmGenderEdit() {
      const genderValues = ['male', 'female', 'unknown'];
      const newGender = genderValues[this.genderIndex];
      
      try {
        const updateDTO = {
          userId: this.userId,
          gender: newGender
        };
        
        await request.post('/api/guardian/updateinfo', updateDTO);
        this.basicInfo.gender = newGender;
        this.$refs.genderEditPopup.close();
        uni.showToast({ title: '性别更新成功', icon: 'success' });
      } catch (error) {
        console.error('更新性别失败:', error);
        uni.showToast({ title: '更新失败，请重试', icon: 'none' });
      }
    },

    showBirthdayEditModal() {
      const formattedBirthday = this.basicInfo.birthday 
        ? this.basicInfo.birthday.split('T')[0]
        : '';
      this.newBirthday = formattedBirthday;
      this.$refs.birthdayEditPopup.open();
    },

    onBirthdayChange(e) {
      this.newBirthday = e.detail.value;
    },

    async confirmBirthdayEdit() {
      if (!this.newBirthday) {
        uni.showToast({ title: '请选择日期', icon: 'none' });
        return;
      }
      
      try {
        const updateDTO = {
          userId: this.userId,
          birthday: this.newBirthday
        };
        
        await request.post('/api/guardian/updateinfo', updateDTO);
        this.basicInfo.birthday = this.newBirthday;
        this.$refs.birthdayEditPopup.close();
        uni.showToast({ title: '生日更新成功', icon: 'success' });
      } catch (error) {
        console.error('更新生日失败:', error);
        uni.showToast({ title: '更新失败，请重试', icon: 'none' });
      }
    },

    getCurrentDate() {
      return new Date().toISOString().split('T')[0];
    },

    showNameEditModal() {
      this.newName = this.userInfo.name;
      this.$refs.nameEditPopup.open();
    },

    cancelNameEdit() {
      this.newName = '';
      this.$refs.nameEditPopup.close();
    },

    async confirmNameEdit() {
      if (!this.newName.trim()) {
        uni.showToast({ title: '姓名不能为空', icon: 'none' });
        return;
      }
      
      try {
        const updateDTO = {
          userId: this.userId,
          name: this.newName.trim()
        };
        
        const res = await request.post('/api/guardian/updateinfo', updateDTO);
        this.userInfo.name = res.data.name;
        this.$refs.nameEditPopup.close();
        uni.showToast({ title: '姓名更新成功', icon: 'success' });
      } catch (error) {
        console.error('更新姓名失败:', error);
        uni.showToast({ title: '更新失败，请重试', icon: 'none' });
      }
    },

    // 地址管理相关
    showAddressViewModal() {
      this.$refs.addressViewPopup.open();
    },

    closeAddressViewModal() {
      this.$refs.addressViewPopup.close();
    },

    showAddressEditModal() {
      this.newAddress = this.basicInfo.address || '';
      this.$refs.addressViewPopup.close();
      this.$refs.addressEditPopup.open();
    },

    cancelAddressEdit() {
      this.newAddress = '';
      this.$refs.addressEditPopup.close();
      this.$refs.addressViewPopup.open();
    },

    async confirmAddressEdit() {
      if (!this.newAddress.trim()) {
        return uni.showToast({ title: '请输入地址', icon: 'none' });
      }

      try {
        const updateDTO = {
          userId: this.userId,
          address: this.newAddress.trim()
        };
        await request.post('/api/guardian/updateinfo', updateDTO);
        
        this.basicInfo.address = this.newAddress.trim();
        this.$refs.addressEditPopup.close();
        this.$refs.addressViewPopup.open();
        uni.showToast({ title: '地址保存成功', icon: 'success' });
      } catch (error) {
        console.error('更新地址失败:', error);
        uni.showToast({ title: '保存失败，请重试', icon: 'none' });
      }
    },

    chooseAvatar() {
      uni.showActionSheet({
        itemList: ['拍照', '从相册选择'],
        success: (res) => {
          if (res.tapIndex === 0) {
            this.takePhoto();
          } else {
            this.chooseFromAlbum();
          }
        }
      });
    },

    

    chooseFromAlbum() {
      uni.chooseImage({
        count: 1,
        sourceType: ['album'],
        sizeType: ['compressed'],
	    crop: {
	      quality: 90,
	      width: 300,
	      height: 300
	    },
        success: (res) => {
          // 修复：调用上传方法（之前遗漏）
          this.uploadAvatar(res.tempFilePaths[0]);
        },
        fail: (err) => {
          console.error('选择图片失败:', err);
          uni.showToast({ title: '获取图片失败', icon: 'none' });
        }
      });
    },
    uploadAvatar(filePath) {
      uni.showLoading({ title: '上传中...', mask: true });
    
      uni.uploadFile({
        url: `${config.API_BASE_URL}/api/guardian/avatar/upload`,
        filePath: filePath,
        name: 'file',
        formData: { userId: this.userId },
        success: (uploadRes) => {
          try {
            const result = uploadRes.data ? JSON.parse(uploadRes.data) : {};
            
            if (result.code === 200 && result.data?.url) {
              // 保存相对路径，由getAvatarUrl方法统一处理（添加时间戳参数）
              this.userInfo.avatar = result.data.url + '?t=' + Date.now();
              uni.showToast({ title: '头像更新成功', icon: 'success' });
            } else {
              uni.showToast({ title: result.msg || '上传失败', icon: 'none' });
            }
          } catch (e) {
            console.error('解析响应失败:', e);
            uni.showToast({ title: '服务器响应错误', icon: 'none' });
          }
        },
        fail: (err) => {
          console.error('上传失败:', err);
          uni.showToast({ title: '网络错误', icon: 'none' });
        },
        complete: () => {
          uni.hideLoading();
        }
      });
    }
  }
}
</script>

<style scoped>
/* 样式保持不变 */
.profile-container {
  padding: 0;
  background-color: #f8f9fa;
  min-height: 100vh;
}

.profile-header {
  display: flex;
  align-items: center;
  padding: 40rpx;
  background: linear-gradient(to right, #65a23f, #458a00);
  color: white;
  position: relative;
  margin-bottom: 40rpx;
}

.profile-avatar {
  width: 140rpx;
  height: 140rpx;
  border-radius: 50%;
  border: 4rpx solid rgba(255, 255, 255, 0.5);
  margin-right: 30rpx;
  cursor: pointer;
  transition: opacity 0.3s;
}

.profile-avatar:active {
  opacity: 0.8;
}

.profile-info {
  flex: 1;
}

.profile-name {
  font-size: 40rpx;
  font-weight: bold;
  display: block;
  margin-bottom: 10rpx;
  cursor: pointer;
  transition: color 0.3s;
}

.profile-name:active {
  color: #65a23f;
}

.profile-id {
  font-size: 28rpx;
  opacity: 0.9;
}

.edit-btn {
  position: absolute;
  right: 30rpx;
  top: 30rpx;
  background: rgba(255, 255, 255, 0.2);
  width: 60rpx;
  height: 60rpx;
  border-radius: 50%;
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 0;
  border: none;
}

.edit-btn image {
  width: 30rpx;
  height: 30rpx;
}

.profile-card {
  background-color: #fff;
  border-radius: 20rpx;
  padding: 0 30rpx;
  margin: 0 30rpx 30rpx;
  box-shadow: 0 4rpx 12rpx rgba(0, 0, 0, 0.05);
}

.card-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 30rpx 0;
  border-bottom: 1rpx solid #f1f1f1;
}

.card-item:last-child {
  border-bottom: none;
}

.item-left {
  display: flex;
  align-items: center;
}

.card-item .item-label {
  width: 160rpx;
  font-size: 28rpx;
  color: #333;
  flex-shrink: 0;
}

.card-item .item-value {
  font-size: 28rpx;
  color: #464646;
  margin-left: 20rpx;
}

.item-icon {
  width: 40rpx;
  height: 40rpx;
  margin-right: 20rpx;
}

.arrow-icon, .enter-icon {
  width: 30rpx;
  height: 30rpx;
  cursor: pointer;
  transition: opacity 0.3s;
  padding: 8rpx;
}

.arrow-icon:active, .enter-icon:active {
  opacity: 0.6;
}

.logout-btn {
  margin: 30rpx;
  height: 90rpx;
  line-height: 90rpx;
  background-color: #fff;
  color: #f56c6c;
  font-size: 34rpx;
  border-radius: 10rpx;
  border: none;
}

.card-item.readonly {
  cursor: default;
}

.name-edit-dialog, .edit-dialog, .address-view-dialog {
  background-color: #fff;
  border-radius: 20rpx;
  padding: 40rpx;
}

.dialog-title {
  font-size: 36rpx;
  font-weight: bold;
  text-align: center;
  margin-bottom: 30rpx;
  color: #333;
}

.name-input {
  width: 100%;
  height: 80rpx;
  border: 2rpx solid #e0e0e0;
  border-radius: 10rpx;
  padding: 0 20rpx;
  font-size: 32rpx;
  margin-bottom: 40rpx;
  box-sizing: border-box;
}

.name-input:focus {
  border-color: #65a23f;
}

.form-picker {
  margin-bottom: 40rpx;
}

.picker-display {
  display: flex;
  justify-content: space-between;
  align-items: center;
  height: 80rpx;
  border: 2rpx solid #e0e0e0;
  border-radius: 10rpx;
  padding: 0 20rpx;
  font-size: 30rpx;
  color: #333;
  box-sizing: border-box;
}

.picker-display:active {
  border-color: #65a23f;
}

.picker-arrow {
  font-size: 24rpx;
  color: #999;
}

.address-textarea {
  width: 100%;
  min-height: 120rpx;
  border: 2rpx solid #e0e0e0;
  border-radius: 10rpx;
  padding: 20rpx;
  font-size: 30rpx;
  resize: none;
  box-sizing: border-box;
  margin-bottom: 40rpx;
}

.address-textarea:focus {
  border-color: #65a23f;
}

.dialog-buttons {
  display: flex;
  justify-content: space-between;
  padding-bottom: 10rpx;
}

.cancel-btn, .confirm-btn {
  width: 240rpx;
  height: 70rpx;
  line-height: 70rpx;
  text-align: center;
  border-radius: 10rpx;
  font-size: 30rpx;
  border: none;
}

.confirm-btn {
  margin-left: 20rpx;
}

.cancel-btn {
  background-color: #f5f5f5;
  color: #666;
}

.confirm-btn {
  background-color: #65a23f;
  color: #fff;
}

.address-view-dialog {
  width: 600rpx;
}

.address-display {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 30rpx;
  padding: 30rpx;
  background-color: #f8f9fa;
  border-radius: 15rpx;
  min-height: 100rpx;
}

.address-content {
  flex: 1;
  margin-right: 20rpx;
}

.address-text {
  font-size: 30rpx;
  color: #333;
  line-height: 1.5;
  word-break: break-all;
}

.address-edit-icon {
  width: 36rpx;
  height: 36rpx;
  cursor: pointer;
  transition: all 0.3s ease;
  padding: 12rpx;
  border-radius: 50%;
  background-color: rgba(101, 162, 63, 0);
  border: 2rpx solid transparent;
}

.address-edit-icon:active {
  opacity: 0.8;
  transform: scale(0.95);
  background-color: rgba(152, 152, 152, 0.3);
}

@media screen and (max-width: 750rpx) {
  .edit-dialog {
    width: 90vw;
    max-width: 500rpx;
  }
  
  .address-view-dialog {
    width: 90vw;
    max-width: 600rpx;
  }
}
</style>