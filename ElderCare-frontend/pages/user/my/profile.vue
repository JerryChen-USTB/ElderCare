<template>
  <view class="profile-container">


    <!-- 头像和信息区域 -->
    <view class="profile-header">
      <image 
        class="profile-avatar" 
        :src="getAvatarUrl(userInfo.avatarUrl)" 
        mode="aspectFill"
        @click="chooseAvatar"
        @error="handleImageError"
        @load="handleImageLoad"
      ></image>
      <view class="profile-info">
        <text class="profile-name" @click="showNameEditModal">{{ userInfo.name || '用户' }}</text>
        <text class="profile-id">ID: {{ formatPhone(userInfo.phone) }}</text>
      </view>
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

    <!-- 详细信息卡片 -->
    <view class="profile-card">
      <view class="card-item">
        <view class="item-left">
          <image class="item-icon" src="/static/icons/health-record.png"></image>
          <text>健康档案</text>
        </view>
        <image 
          class="enter-icon" 
          src="/static/icons/enter.png" 
          mode="aspectFit"
          @click="navigateTo('health')"
        ></image>
      </view>
      
      <view class="card-item">
        <view class="item-left">
          <image class="item-icon" src="/static/icons/address.png"></image>
          <text>地址管理</text>
        </view>
        <image 
          class="enter-icon" 
          src="/static/icons/enter.png" 
          mode="aspectFit"
          @click="showAddressViewModal"
        ></image>
      </view>
      
      <view class="card-item">
        <view class="item-left">
          <image class="item-icon" src="/static/icons/family.png"></image>
          <text>家庭成员</text>
        </view>
        <image 
          class="enter-icon" 
          src="/static/icons/enter.png" 
          mode="aspectFit"
          @click="navigateTo('family')"
        ></image>
      </view>
    </view>

    <!-- 退出登录按钮 -->
    <button class="logout-btn" @click="handleLogout">退出登录</button>
    
    <!-- 修改姓名弹窗 -->
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
    
    <!-- 修改性别弹窗 -->
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
            {{ genderOptions[genderIndex] || '请选择性别' }}
            <text class="picker-arrow">▼</text>
          </view>
        </picker>
        <view class="dialog-buttons">
          <button class="cancel-btn" @click="cancelGenderEdit">取消</button>
          <button class="confirm-btn" @click="confirmGenderEdit">确认</button>
        </view>
      </view>
    </uni-popup>
    
    <!-- 修改出生日期弹窗 -->
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
            {{ newBirthday || '请选择出生日期' }}
            <text class="picker-arrow">▼</text>
          </view>
        </picker>
        <view class="dialog-buttons">
          <button class="cancel-btn" @click="cancelBirthdayEdit">取消</button>
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
    
    <!-- 地址编辑弹窗 -->
    <uni-popup ref="addressEditPopup" type="dialog">
      <view class="edit-dialog">
        <view class="dialog-title">编辑家庭地址</view>
        <textarea 
          v-model="newAddress" 
          placeholder="请输入家庭地址" 
          class="address-textarea"
          maxlength="200"
        />
        <view class="dialog-buttons">
          <button class="cancel-btn" @click="cancelAddressEdit">取消</button>
          <button class="confirm-btn" @click="confirmAddressEdit">保存</button>
        </view>
      </view>
    </uni-popup>
  </view>
</template>

<script>
import request from '@/utils/request.js'

export default {
  data() {
    return {
      userInfo: {
        name: '',
        phone: '',
        avatarUrl: '',
        userId: null
      },
      basicInfo: {
        gender: '',
        birthday: '',
        address: ''
      },
      elderId: null, // 保存Elder的ID
      newName: '',
      uploading: false,
      notificationSent: false, // 避免重复发送通知
      // 编辑基本信息相关数据
      newBirthday: '',
      newAddress: '', // 新地址输入
      genderOptions: ['男', '女', '未设置'],
      genderIndex: 2 // 默认选择"未设置"
    }
  },
  onLoad() {
    this.loadUserInfo();
    
    // 监听头像和姓名更新事件（来自其他页面的通知）
    uni.$on('avatarUpdated', this.handleAvatarUpdated);
    uni.$on('nameUpdated', this.handleNameUpdated);
  },
  
  onShow() {
    // 页面显示时刷新用户信息（处理从其他页面返回的情况）
    this.refreshUserInfo();
  },
  
  onUnload() {
    // 移除事件监听
    uni.$off('avatarUpdated', this.handleAvatarUpdated);
    uni.$off('nameUpdated', this.handleNameUpdated);
  },
  methods: {
    // 加载用户信息
    async loadUserInfo() {
      try {
        // 从本地存储获取用户信息
        const storedUser = uni.getStorageSync('userInfo');
        if (storedUser) {
          this.userInfo.phone = storedUser.phone;
          this.userInfo.userId = storedUser.id;
          
          // 获取老人详细信息
          const elderRes = await request.elderApi.getElderInfo(storedUser.id);
          
          if (elderRes.success && elderRes.elder) {
            this.userInfo.name = elderRes.elder.name || storedUser.phone;
            this.userInfo.avatarUrl = elderRes.elder.avatarUrl || '';
            
            // 保存Elder ID
            this.elderId = elderRes.elder.id;
            
            // 加载基本信息
            this.basicInfo.gender = elderRes.elder.gender || '';
            this.basicInfo.birthday = elderRes.elder.birthday ? this.formatDateToString(elderRes.elder.birthday) : '';
            this.basicInfo.address = elderRes.elder.address || '';
            
            // 调试：打印接收到的性别数据
            console.log('从后端接收到的性别值:', elderRes.elder.gender);
            console.log('映射后的显示文本:', this.getGenderText(elderRes.elder.gender));
          } else {
            this.userInfo.name = storedUser.phone; // 默认使用手机号作为姓名
          }
        } else {
          // 如果没有存储的用户信息，跳转到登录页
          uni.showToast({
            title: '请先登录',
            icon: 'none'
          });
          setTimeout(() => {
            uni.reLaunch({
              url: '/pages/login/login'
            });
          }, 1500);
        }
      } catch (error) {
        console.error('获取用户信息失败:', error);
        uni.showToast({
          title: '获取用户信息失败',
          icon: 'none'
        });
      }
    },
    
    // 格式化手机号显示
    formatPhone(phone) {
      if (!phone) return '';
      return phone.replace(/(\d{3})\d{4}(\d{4})/, '$1****$2');
    },
    
    // 获取头像URL
    getAvatarUrl(avatarUrl) {
      if (avatarUrl && avatarUrl.trim() !== '') {
        // 如果是相对路径，添加后端基础URL
        if (avatarUrl.startsWith('/uploads/')) {
          // 如果已经带有时间戳，直接添加基础URL
          if (avatarUrl.includes('?t=')) {
            return request.BASE_URL + avatarUrl;
          }
          // 否则添加时间戳避免缓存
          const fullUrl = request.BASE_URL + avatarUrl + '?t=' + Date.now();
          return fullUrl;
        }
        return avatarUrl;
      }
      // 默认头像（Spring Boot静态资源自动映射）
      return request.BASE_URL + '/uploads/avatars/default-avatar.png';
    },
    
    // 处理图片加载成功
    handleImageLoad(event) {
      // 图片加载成功，无需额外日志
    },
    
    // 处理图片加载失败
    handleImageError(event) {
      console.error('❌ 头像图片加载失败', event);
      
      // 如果当前头像加载失败，尝试重新加载或显示默认头像
      if (this.userInfo.avatarUrl && this.userInfo.avatarUrl.includes('/uploads/')) {
        console.log('🔄 尝试重新加载头像...');
        
        // 等待1秒后重新尝试
        setTimeout(() => {
          this.$forceUpdate();
        }, 1000);
      }
    },
    
    // 更新本地存储的用户信息
    async updateLocalUserInfo() {
      try {
        const storedUser = uni.getStorageSync('userInfo');
        if (storedUser && this.userInfo.userId) {
          // 重新获取最新的Elder信息
          const elderRes = await request.elderApi.getElderInfo(this.userInfo.userId);
          if (elderRes.success && elderRes.elder) {
            // 更新本地存储，但保持原有的基本用户信息
            const updatedUser = {
              ...storedUser,
              elderInfo: elderRes.elder
            };
            uni.setStorageSync('userInfo', updatedUser);
            console.log('本地用户信息已更新');
          }
        }
      } catch (error) {
        console.error('更新本地用户信息失败:', error);
      }
    },
    
    // 通知其他页面更新数据
    notifyOtherPages() {
      // 使用uni-app的事件总线通知其他页面
      uni.$emit('avatarUpdated', {
        userId: this.userInfo.userId,
        avatarUrl: this.userInfo.avatarUrl,
        timestamp: Date.now(),
        sender: 'profile' // 标识发送者，避免自己处理自己的事件
      });
    },
    
    // 刷新用户信息（轻量级，不显示loading）
    async refreshUserInfo() {
      try {
        const storedUser = uni.getStorageSync('userInfo');
        if (storedUser && storedUser.id) {
          const elderRes = await request.elderApi.getElderInfo(storedUser.id);
          if (elderRes.success && elderRes.elder) {
            const oldAvatarUrl = this.userInfo.avatarUrl;
            const oldName = this.userInfo.name;
            
            this.userInfo.name = elderRes.elder.name || storedUser.phone;
            this.userInfo.avatarUrl = elderRes.elder.avatarUrl || '';
            
            // 保存Elder ID
            this.elderId = elderRes.elder.id;
            
            // 更新基本信息
            const oldGender = this.basicInfo.gender;
            const oldBirthday = this.basicInfo.birthday;
            const oldAddress = this.basicInfo.address;
            
            this.basicInfo.gender = elderRes.elder.gender || '';
            this.basicInfo.birthday = elderRes.elder.birthday ? this.formatDateToString(elderRes.elder.birthday) : '';
            this.basicInfo.address = elderRes.elder.address || '';
            
            // 调试：打印刷新后的性别数据
            console.log('📋 refreshUserInfo - 接收到的性别值:', elderRes.elder.gender);
            
            // 如果头像、姓名或基本信息有变化，强制更新页面
            if (oldAvatarUrl !== this.userInfo.avatarUrl || 
                oldName !== this.userInfo.name ||
                oldGender !== this.basicInfo.gender ||
                oldBirthday !== this.basicInfo.birthday ||
                oldAddress !== this.basicInfo.address) {
              console.log('Profile页面数据已刷新');
              this.$forceUpdate();
            }
          }
        }
      } catch (error) {
        console.error('刷新用户信息失败:', error);
      }
    },
    
    // 处理头像更新事件（仅处理来自其他页面的更新）
    handleAvatarUpdated(data) {
      // 忽略来自自己页面的事件，避免循环处理
      if (data.sender === 'profile') {
        return;
      }
      
      if (data.userId === this.userInfo.userId) {
        // 只有当前页面没有在上传状态时才处理外部更新
        if (!this.uploading) {
          this.userInfo.avatarUrl = data.avatarUrl;
          // 强制更新页面
          this.$forceUpdate();
        }
      }
    },
    
    // 处理姓名更新事件
    handleNameUpdated(data) {
      if (data.userId === this.userInfo.userId) {
        console.log('Profile页面收到姓名更新通知:', data);
        this.userInfo.name = data.name;
        // 强制更新页面
        this.$forceUpdate();
      }
    },
    
    // 显示修改姓名弹窗
    showNameEditModal() {
      this.newName = this.userInfo.name;
      this.$refs.nameEditPopup.open();
    },
    
    // 取消修改姓名
    cancelNameEdit() {
      this.newName = '';
      this.$refs.nameEditPopup.close();
    },
    
    // 确认修改姓名
    async confirmNameEdit() {
      if (!this.newName || this.newName.trim() === '') {
        uni.showToast({
          title: '姓名不能为空',
          icon: 'none'
        });
        return;
      }
      
      if (this.newName.trim() === this.userInfo.name) {
        this.$refs.nameEditPopup.close();
        return;
      }
      
      try {
        uni.showLoading({ title: '更新中...' });
        
        const res = await request.elderApi.updateElderName(this.userInfo.userId, this.newName.trim());
        
        uni.hideLoading();
        
        if (res.success) {
          this.userInfo.name = this.newName.trim();
          
          // 同步更新本地存储的用户信息
          this.updateLocalUserInfo();
          
          // 通知其他页面姓名已更新
          uni.$emit('nameUpdated', {
            userId: this.userInfo.userId,
            name: this.userInfo.name,
            timestamp: Date.now()
          });
          
          uni.showToast({
            title: '姓名更新成功',
            icon: 'success'
          });
          this.$refs.nameEditPopup.close();
        } else {
          uni.showToast({
            title: res.message || '更新失败',
            icon: 'none'
          });
        }
      } catch (error) {
        uni.hideLoading();
        console.error('更新姓名失败:', error);
        uni.showToast({
          title: '网络错误，请重试',
          icon: 'none'
        });
      }
    },
    
    // 选择头像
    chooseAvatar() {
      uni.showActionSheet({
        itemList: ['拍照', '从相册选择'],
        success: (res) => {
          if (res.tapIndex === 0) {
            this.takePhoto();
          } else if (res.tapIndex === 1) {
            this.chooseFromAlbum();
          }
        }
      });
    },
    
    // 拍照
    takePhoto() {
      uni.chooseImage({
        count: 1,
        sourceType: ['camera'],
        sizeType: ['compressed'],
        success: (res) => {
          this.uploadAvatar(res.tempFilePaths[0]);
        },
        fail: (error) => {
          console.error('拍照失败:', error);
        }
      });
    },
    
    // 从相册选择
    chooseFromAlbum() {
      uni.chooseImage({
        count: 1,
        sourceType: ['album'],
        sizeType: ['compressed'],
        success: (res) => {
          this.uploadAvatar(res.tempFilePaths[0]);
        },
        fail: (error) => {
          console.error('选择图片失败:', error);
        }
      });
    },
    
    // 预加载并更新头像
    async preloadAndUpdateAvatar(avatarUrl, retryCount = 0) {
      const maxRetries = 3;
      // 检查avatarUrl是否已包含时间戳，避免重复添加
      let timestampUrl;
      if (avatarUrl.includes('?t=')) {
        timestampUrl = avatarUrl;
      } else {
        timestampUrl = avatarUrl + '?t=' + Date.now();
      }
      const fullUrl = request.BASE_URL + timestampUrl;
      
      // 只在第一次尝试或重试时显示日志
      if (retryCount === 0) {
        console.log('🔄 正在验证头像文件...');
      } else {
        console.log(`🔄 重试验证头像 (${retryCount + 1}/${maxRetries + 1})`);
      }
      
      try {
        // 使用uni-app兼容的图片预加载方法
        await new Promise((resolve, reject) => {
          // 设置超时
          const timeout = setTimeout(() => {
            reject(new Error('图片加载超时'));
          }, 8000);
          
          // 使用uni-app的getImageInfo方法检查图片是否可用
          uni.getImageInfo({
            src: fullUrl,
            success: (res) => {
              clearTimeout(timeout);
              resolve();
            },
            fail: (error) => {
              clearTimeout(timeout);
              console.error('🚫 getImageInfo失败详情:', {
                avatarUrl: avatarUrl,
                fullUrl: fullUrl,
                error: error,
                retryCount: retryCount
              });
              reject(new Error('图片加载失败: ' + error.errMsg));
            }
          });
        });
        
        // 图片预加载成功，更新UI
        console.log('🎉 头像更新成功');
        
        this.userInfo.avatarUrl = timestampUrl;
        
        // 强制刷新页面显示
        this.$forceUpdate();
        
        // 触发页面间数据同步（只有当前页面是profile时才发送）
        if (this.$options.name === 'profile' || !this.notificationSent) {
          this.notifyOtherPages();
          this.notificationSent = true; // 标记已发送，避免重复
        }
        
        // 异步更新本地存储
        this.updateLocalUserInfo().catch(error => {
          console.error('更新本地存储失败:', error);
        });
        
        uni.showToast({
          title: '头像更新成功',
          icon: 'success'
        });
        
      } catch (error) {
        if (retryCount < maxRetries) {
          // 等待一段时间后重试
          const delay = (retryCount + 1) * 1000; // 1s, 2s, 3s
          setTimeout(() => {
            this.preloadAndUpdateAvatar(avatarUrl, retryCount + 1);
          }, delay);
        } else {
          // 重试次数用尽，显示错误
          console.error('❌ 头像更新失败，请重试');
          uni.showToast({
            title: '头像更新失败，请重试',
            icon: 'none'
          });
          
          // 恢复上传状态，允许用户重新尝试
          this.uploading = false;
        }
      }
    },
    
    // 上传头像
    async uploadAvatar(filePath) {
      if (this.uploading) return;
      
      this.uploading = true;
      uni.showLoading({ title: '上传中...' });
      
      try {
        const res = await new Promise((resolve, reject) => {
          uni.uploadFile({
            url: request.BASE_URL + '/api/elder/uploadAvatar',
            filePath: filePath,
            name: 'file',
            formData: {
              userId: this.userInfo.userId
            },
            success: (uploadRes) => {
              try {
                const data = JSON.parse(uploadRes.data);
                resolve(data);
              } catch (e) {
                reject(e);
              }
            },
            fail: reject
          });
        });
        
        uni.hideLoading();
        this.uploading = false;
        
        if (res.success) {
          // 重置通知标志
          this.notificationSent = false;
          
          // 等待文件系统同步，然后预加载新头像
          setTimeout(() => {
            this.preloadAndUpdateAvatar(res.avatarUrl);
          }, 500); // 增加500ms延迟，确保文件完全写入
        } else {
          uni.showToast({
            title: res.message || '上传失败',
            icon: 'none'
          });
        }
      } catch (error) {
        uni.hideLoading();
        this.uploading = false;
        console.error('上传头像失败:', error);
        uni.showToast({
          title: '上传失败，请重试',
          icon: 'none'
        });
      }
    },
    
    // 返回按钮点击事件
    goBack() {
      uni.navigateBack({
        delta: 1
      });
    },
    navigateToEdit() {
      uni.navigateTo({
        url: '/pages/user/my/profile-edit'
      })
    },
    navigateTo(page) {
      const routes = {
        'health': '/pages/user/health/record',
        'address': '/pages/user/address/list',
        'family': '/pages/user/family/list',
        'security': '/pages/user/settings/security',
        'notification': '/pages/user/settings/notification',
        'privacy': '/pages/user/settings/privacy'
      }
      uni.navigateTo({
        url: routes[page]
      })
    },
    handleLogout() {
      uni.showModal({
        title: '确认退出',
        content: '你确定要退出登录吗？',
        confirmText: '退出',
        cancelText: '返回',
        confirmColor: '#FF0000',
        cancelColor: '#000000',
        success: (res) => {
          if (res.confirm) {
            // 清除本地存储的用户信息
            uni.removeStorageSync('userInfo');
            uni.removeStorageSync('token');
            
            uni.showToast({
              title: '退出登录成功',
              icon: 'success'
            });
            
            // 跳转到登录页
            setTimeout(() => {
              uni.reLaunch({
                url: '/pages/login/login'
              });
            }, 1000);
          }
        }
      });
    },
    
    // === 基本信息相关方法 ===
    
    // 获取性别显示文本
    getGenderText(gender) {
      const genderMap = {
        'male': '男',
        'female': '女',
        'unknown': '未设置'
      };
      return genderMap[gender] || '未设置';
    },
    
    // 格式化生日显示
    formatBirthday(birthday) {
      if (!birthday) return '未设置';
      // 如果birthday是字符串格式的日期，直接返回
      if (typeof birthday === 'string') {
        return birthday;
      }
      // 如果是Date对象，格式化为YYYY-MM-DD
      return this.formatDateToString(birthday);
    },
    
    // 计算年龄
    calculateAge(birthday) {
      if (!birthday) return '未知';
      
      let birthDate;
      if (typeof birthday === 'string') {
        birthDate = new Date(birthday);
      } else {
        birthDate = new Date(birthday);
      }
      
      if (isNaN(birthDate.getTime())) {
        return '未知';
      }
      
      const today = new Date();
      let age = today.getFullYear() - birthDate.getFullYear();
      const monthDiff = today.getMonth() - birthDate.getMonth();
      
      if (monthDiff < 0 || (monthDiff === 0 && today.getDate() < birthDate.getDate())) {
        age--;
      }
      
      return age > 0 ? age : 0;
    },
    
    // 格式化日期为字符串（YYYY-MM-DD）
    formatDateToString(date) {
      if (!date) return '';
      const d = new Date(date);
      if (isNaN(d.getTime())) return '';
      
      const year = d.getFullYear();
      const month = String(d.getMonth() + 1).padStart(2, '0');
      const day = String(d.getDate()).padStart(2, '0');
      return `${year}-${month}-${day}`;
    },
    
    // 获取当前日期（作为日期选择器的最大值）
    getCurrentDate() {
      return this.formatDateToString(new Date());
    },
    
    // 显示性别编辑弹窗
    showGenderEditModal() {
      console.log('🎯 显示性别编辑弹窗:');
      console.log('  - 当前存储的性别值:', this.basicInfo.gender);
      console.log('  - 当前显示的性别文本:', this.getGenderText(this.basicInfo.gender));
      
      // 设置性别选择器的索引
      this.setGenderIndex(this.basicInfo.gender);
      
      console.log('  - 设置后的选择器索引:', this.genderIndex);
      console.log('  - 对应的选择器选项:', this.genderOptions[this.genderIndex]);
      
      this.$refs.genderEditPopup.open();
    },
    
    // 显示出生日期编辑弹窗
    showBirthdayEditModal() {
      this.newBirthday = this.basicInfo.birthday;
      this.$refs.birthdayEditPopup.open();
    },
    
    // 设置性别选择器索引
    setGenderIndex(gender) {
      const genderMap = {
        'male': 0,
        'female': 1,
        'unknown': 2
      };
      
      // 清理输入值，去除可能的空格和换行符
      const cleanGender = gender ? gender.toString().trim() : '';
      
      console.log('🔍 setGenderIndex 调试信息:');
      console.log('  - 原始性别值:', JSON.stringify(gender));
      console.log('  - 清理后性别值:', JSON.stringify(cleanGender));
      console.log('  - 性别值类型:', typeof cleanGender);
      console.log('  - genderMap对象:', genderMap);
      console.log('  - 直接访问genderMap[cleanGender]:', genderMap[cleanGender]);
      console.log('  - 映射结果索引:', genderMap[cleanGender] !== undefined ? genderMap[cleanGender] : 2);
      console.log('  - 选择器选项数组:', this.genderOptions);
      
      this.genderIndex = genderMap[cleanGender] !== undefined ? genderMap[cleanGender] : 2;
    },
    
    // 性别改变事件
    onGenderChange(e) {
      this.genderIndex = e.detail.value;
    },
    
    // 生日改变事件
    onBirthdayChange(e) {
      this.newBirthday = e.detail.value;
    },
    
    // 取消编辑性别
    cancelGenderEdit() {
      this.genderIndex = 2;
      this.$refs.genderEditPopup.close();
    },
    
    // 确认编辑性别
    async confirmGenderEdit() {
      try {
        uni.showLoading({ title: '更新中...' });
        
        const genderValues = ['male', 'female', 'unknown'];
        const newGender = genderValues[this.genderIndex];
        
        const updateData = {
          id: this.elderId,
          userId: this.userInfo.userId,
          gender: newGender,
          birthday: this.basicInfo.birthday,
          address: this.basicInfo.address || ''
        };
        
        const res = await request.elderApi.updateElderInfo(updateData);
        
        uni.hideLoading();
        
        if (res.success) {
          // 更新本地数据
          this.basicInfo.gender = newGender;
          
          // 同步更新本地存储的用户信息
          this.updateLocalUserInfo();
          
          uni.showToast({
            title: '性别更新成功',
            icon: 'success'
          });
          
          this.$refs.genderEditPopup.close();
        } else {
          uni.showToast({
            title: res.message || '更新失败',
            icon: 'none'
          });
        }
      } catch (error) {
        uni.hideLoading();
        console.error('更新性别失败:', error);
        uni.showToast({
          title: '网络错误，请重试',
          icon: 'none'
        });
      }
    },
    
    // 取消编辑出生日期
    cancelBirthdayEdit() {
      this.newBirthday = '';
      this.$refs.birthdayEditPopup.close();
    },
    
    // 确认编辑出生日期
    async confirmBirthdayEdit() {
      if (!this.newBirthday) {
        uni.showToast({
          title: '请选择出生日期',
          icon: 'none'
        });
        return;
      }
      
      try {
        uni.showLoading({ title: '更新中...' });
        
        const updateData = {
          id: this.elderId,
          userId: this.userInfo.userId,
          gender: this.basicInfo.gender,
          birthday: this.newBirthday,
          address: this.basicInfo.address || ''
        };
        
        const res = await request.elderApi.updateElderInfo(updateData);
        
        uni.hideLoading();
        
        if (res.success) {
          // 更新本地数据
          this.basicInfo.birthday = this.newBirthday;
          
          // 同步更新本地存储的用户信息
          this.updateLocalUserInfo();
          
          uni.showToast({
            title: '出生日期更新成功',
            icon: 'success'
          });
          
          this.$refs.birthdayEditPopup.close();
        } else {
          uni.showToast({
            title: res.message || '更新失败',
            icon: 'none'
          });
        }
      } catch (error) {
        uni.hideLoading();
        console.error('更新出生日期失败:', error);
        uni.showToast({
          title: '网络错误，请重试',
          icon: 'none'
        });
      }
    },
    
    // === 地址管理相关方法 ===
    
    // 显示地址查看弹窗
    showAddressViewModal() {
      this.$refs.addressViewPopup.open();
    },
    
    // 关闭地址查看弹窗
    closeAddressViewModal() {
      this.$refs.addressViewPopup.close();
    },
    
    // 显示地址编辑弹窗
    showAddressEditModal() {
      this.newAddress = this.basicInfo.address || '';
      this.$refs.addressViewPopup.close();
      this.$refs.addressEditPopup.open();
    },
    
    // 取消编辑地址
    cancelAddressEdit() {
      this.newAddress = '';
      this.$refs.addressEditPopup.close();
      this.$refs.addressViewPopup.open();
    },
    
    // 确认编辑地址
    async confirmAddressEdit() {
      try {
        uni.showLoading({ title: '保存中...' });
        
        const updateData = {
          id: this.elderId,
          userId: this.userInfo.userId,
          gender: this.basicInfo.gender,
          birthday: this.basicInfo.birthday,
          address: this.newAddress || ''
        };
        
        const res = await request.elderApi.updateElderInfo(updateData);
        
        uni.hideLoading();
        
        if (res.success) {
          // 更新本地数据
          this.basicInfo.address = this.newAddress;
          
          // 同步更新本地存储的用户信息
          this.updateLocalUserInfo();
          
          uni.showToast({
            title: '地址保存成功',
            icon: 'success'
          });
          
          this.$refs.addressEditPopup.close();
          this.$refs.addressViewPopup.open();
        } else {
          uni.showToast({
            title: res.message || '保存失败',
            icon: 'none'
          });
        }
      } catch (error) {
        uni.hideLoading();
        console.error('保存地址失败:', error);
        uni.showToast({
          title: '网络错误，请重试',
          icon: 'none'
        });
      }
    }
  }
}
</script>

<style scoped>
/* 强制移除button边框的全局样式 */
button {
  border: none !important;
  outline: none !important;
}

button::after {
  border: none !important;
}

.profile-container {
  padding: 20rpx;
  background-color: #F8F4F4;
  min-height: 100vh;
  box-sizing: border-box;
}

/* 导航栏样式 */
.nav-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 30rpx;
  background-color: #fff;
  border-bottom: 1rpx solid #f1f1f1;
}

.nav-left {
  display: flex;
  align-items: center;
  font-size: 30rpx;
  color: #333;
}

.back-icon {
  width: 36rpx;
  height: 36rpx;
  margin-right: 10rpx;
}

.nav-title {
  font-size: 34rpx;
  font-weight: bold;
  color: #333;
}

.nav-right {
  width: 60rpx;
}

/* 头像和信息区域样式 - 与my.vue保持一致 */
.profile-header {
  display: flex;
  align-items: center;
  padding: 60rpx 40rpx;
  background-color: #9AB169;
  color: white;
  border-radius: 32rpx;
  margin: 0rpx 0rpx 20rpx;
  box-shadow: 0 4rpx 12rpx rgba(0,0,0,0.05);
}

.profile-avatar {
  width: 140rpx;
  height: 140rpx;
  border-radius: 50%;
  border: 4rpx solid rgba(255, 255, 255, 0.5);
  margin-right: 30rpx;
}

.profile-info {
  flex: 1;
}

.profile-name {
  font-size: 40rpx;
  font-weight: bold;
  display: block;
  margin-bottom: 10rpx;
}

.profile-id {
  font-size: 28rpx;
  opacity: 0.9;
}

.profile-card {
  background-color: #ffffff;
  border-radius: 32rpx;
  padding: 0 30rpx;
  margin: 0 0 20rpx;
  box-shadow: 0 4rpx 12rpx rgba(0,0,0,0.05);
  overflow: hidden;
}

.card-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 30rpx;
  border-bottom: 2rpx solid #e8e8e8;
  color: #4B3425;
  cursor: pointer;
  min-height: 80rpx;
}

.card-item:last-child {
  border-bottom: none;
}

.item-left {
  display: flex;
  align-items: center;
  flex: 1;
}

/* 基本信息卡片中的标签和值样式 */
.card-item .item-label {
  width: 160rpx;
  font-size: 32rpx;
  color: #4B3425;
  flex-shrink: 0;
  font-weight: 600;
}

.card-item .item-value {
  font-size: 28rpx;
  color: #4B3425;
  margin-left: 20rpx;
}

.item-icon {
  width: 40rpx;
  height: 40rpx;
  margin-right: 20rpx;
}

.enter-icon {
  width: 32rpx;
  height: 32rpx;
  cursor: pointer;
  transition: opacity 0.3s;
  padding: 8rpx;
}

.enter-icon:active {
  opacity: 0.6;
}

.settings-section {
  background-color: #fff;
  border-radius: 20rpx;
  padding: 0 30rpx;
  margin: 0 30rpx 30rpx;
  box-shadow: 0 4rpx 12rpx rgba(0, 0, 0, 0.05);
}

.section-title {
  font-size: 32rpx;
  font-weight: bold;
  color: #333;
  padding: 30rpx 0;
  border-bottom: 1rpx solid #f1f1f1;
}

.settings-list {
  padding: 0;
}

.setting-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 30rpx 0;
  border-bottom: 1rpx solid #f1f1f1;
}

.setting-item:last-child {
  border-bottom: none;
}

.setting-left {
  display: flex;
  align-items: center;
}

.setting-icon {
  width: 40rpx;
  height: 40rpx;
  margin-right: 20rpx;
}

.logout-btn {
  margin: 50rpx 0 0;
  height: 100rpx;
  line-height: 100rpx;
  background-color: #FFF0EC;
  color: #FF824D;
  font-size: 32rpx;
  border-radius: 32rpx;
  border: none !important;
  outline: none !important;
  -webkit-appearance: none;
  appearance: none;
  box-shadow: 0 4rpx 12rpx rgba(0,0,0,0.05);
  font-weight: 600;
}

/* 修改姓名弹窗样式 */
.name-edit-dialog {
  width: 600rpx;
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
  border-color: #9AB169;
}

.dialog-buttons {
  display: flex;
  justify-content: space-between;
}

.cancel-btn,
.confirm-btn {
  width: 240rpx;
  height: 70rpx;
  line-height: 70rpx;
  text-align: center;
  border-radius: 10rpx;
  font-size: 30rpx;
  border: none;
}

.cancel-btn {
  background-color: #f5f5f5;
  color: #666;
}

.confirm-btn {
  background-color: #9AB169;
  color: #fff;
}

/* 头像点击效果 */
.profile-avatar {
  cursor: pointer;
  transition: opacity 0.3s;
}

.profile-avatar:active {
  opacity: 0.8;
}

/* 姓名点击效果 */
.profile-name {
  cursor: pointer;
  transition: color 0.3s;
}

.profile-name:active {
  color: #9AB169;
}

/* 基本信息项样式 */
.item-label {
  color: #999;
}

.item-value {
  color: #333;
}

/* 只读卡片项样式 */
.card-item.readonly {
  cursor: default;
}

.card-item.readonly:active {
  background-color: #fff;
}

.readonly-text {
  color: #999;
}

/* 编辑弹窗样式 */
.edit-dialog {
  width: 500rpx;
  background-color: #fff;
  border-radius: 20rpx;
  padding: 40rpx;
}

.form-group {
  margin-bottom: 30rpx;
}

.form-label {
  display: block;
  font-size: 30rpx;
  color: #333;
  margin-bottom: 15rpx;
  font-weight: 500;
}

.form-picker {
  margin-bottom: 40rpx;
  width: 100%;
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
  margin-bottom: 30rpx; /* 与下方留出一些空隙 */
}

.picker-display:active {
  border-color: #9AB169;
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
  border-color: #9AB169;
}

/* 地址管理弹窗样式 */
.address-view-dialog {
  width: 600rpx;
  background-color: #fff;
  border-radius: 20rpx;
  padding: 40rpx;
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

.address-edit-icon:hover {
  background-color: rgba(140, 140, 140, 0.2);
  border-color: rgba(101, 162, 63, 0.3);
  transform: scale(1.05);
}

.address-edit-icon:active {
  opacity: 0.8;
  transform: scale(0.95);
  background-color: rgba(152, 152, 152, 0.3);
}

/* 响应式布局优化 */
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