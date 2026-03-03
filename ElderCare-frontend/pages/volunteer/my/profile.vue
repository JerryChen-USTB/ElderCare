<template>
  <view class="volunteer-profile">
    <!-- 个人信息卡片 -->
    <view class="profile-card">
      <view class="avatar-container" @click="editAvatar">
        <image 
          class="avatar" 
          :src="getAvatarUrl(userInfo.avatarUrl)" 
          mode="aspectFill"
          @load="handleImageLoad"
          @error="handleImageError"
        ></image>
      </view>
      <view class="basic-info">
        <text class="name">{{ userInfo.name }}</text>
        <text class="id">志愿者编号: {{ userInfo.id }}</text>
      </view>
    </view>

    <!-- 详细信息 -->
    <view class="detail-info">
      <!-- 1. 基本信息 -->
      <view class="info-group">
        <text class="group-title">基本信息</text>
        <view class="info-item" @click="editField('name', '姓名', userInfo.name)">
          <text class="item-label">姓名</text>
          <text class="item-value">{{ userInfo.name || '未填写' }}</text>
          <image class="edit-icon" src="/static/icons/edit.png"></image>
        </view>
        <view class="info-item" @click="editField('gender', '性别', userInfo.gender, ['male', 'female', 'unknown'], 'select')">
          <text class="item-label">性别</text>
          <text class="item-value">{{ formatGender(userInfo.gender) }}</text>
          <image class="edit-icon" src="/static/icons/edit.png"></image>
        </view>
      </view>

      <!-- 2. 服务相关 -->
      <view class="info-group">
        <text class="group-title">服务偏好</text>
        <view class="info-item" @click="editField('serviceArea', '服务区域', userInfo.serviceArea, serviceAreaOptions, 'select')">
          <text class="item-label">服务区域</text>
          <text class="item-value">{{ userInfo.serviceArea || '未选择' }}</text>
          <image class="edit-icon" src="/static/icons/edit.png"></image>
        </view>
        <view class="info-item" @click="editField('availability', '可用时间', userInfo.availability, availabilityOptions, 'select')">
          <text class="item-label">可用时间</text>
          <text class="item-value">{{ userInfo.availability || '未填写' }}</text>
          <image class="edit-icon" src="/static/icons/edit.png"></image>
        </view>
        <view class="info-item" @click="editMultiSelect('skills', '擅长领域', userInfo.skills, skillOptions)">
          <text class="item-label">擅长领域</text>
          <text class="item-value">{{ userInfo.skills?.join('、') || '未选择' }}</text>
          <image class="edit-icon" src="/static/icons/edit.png"></image>
        </view>
        <view class="info-item" @click="editMultiSelect('trainingCertificates', '培训证书', userInfo.trainingCertificates, certificateOptions)">
          <text class="item-label">培训证书</text>
          <text class="item-value">{{ userInfo.trainingCertificates?.join('、') || '未选择' }}</text>
          <image class="edit-icon" src="/static/icons/edit.png"></image>
        </view>
      </view>

      <!-- 3. 志愿者经验与认证 -->
      <view class="info-group">
        <text class="group-title">志愿者经验与认证</text>
        <view class="info-item" @click="editField('experience', '志愿服务经验', userInfo.experience, [], 'textarea')">
          <text class="item-label">志愿服务经验</text>
          <text class="item-value">{{ userInfo.experience || '未填写' }}</text>
          <image class="edit-icon" src="/static/icons/edit.png"></image>
        </view>
        <view class="info-item" @click="editMultiSelect('verificationDocuments', '认证材料', userInfo.verificationDocuments, ['身份证照片', '证书照片', '其他材料'])">
          <text class="item-label">认证材料</text>
          <text class="item-value">{{ userInfo.verificationDocuments?.join('、') || '未上传' }}</text>
          <image class="edit-icon" src="/static/icons/edit.png"></image>
        </view>
      </view>
    </view>

    <!-- 底部导航 -->
    <custom-tabbar :current="1" :role="'volunteer'" />

    <!-- 编辑弹窗 -->
    <uni-popup ref="editPopup" type="dialog">
      <uni-popup-dialog
        mode="input"
        :title="editDialog.title"
        :value="editDialog.value"
        :placeholder="'请输入' + editDialog.title"
        @confirm="confirmEdit"
        @close="closeEdit"
      ></uni-popup-dialog>
    </uni-popup>

    <!-- 多选弹窗 -->
    <uni-popup ref="multiSelectPopup" type="bottom">
      <view class="multi-select-popup">
        <view class="popup-header">
          <text class="popup-title">{{ multiSelectDialog.title }}</text>
          <text class="popup-confirm" @click="confirmMultiSelect">确定</text>
        </view>
        <checkbox-group @change="multiSelectChange">
          <view class="checkbox-item" v-for="(item, index) in multiSelectDialog.options" :key="index">
            <checkbox :value="item" :checked="multiSelectDialog.selected.includes(item)" />
            <text>{{ item }}</text>
          </view>
        </checkbox-group>
      </view>
    </uni-popup>

    <!-- 日期选择弹窗 -->
    <uni-popup-dialog
      v-if="editDialog.type === 'date'"
      mode="custom"
      :title="editDialog.title"
    >
      <picker mode="date" :value="editDialog.value" @change="onDateChange">
        <view class="date-picker">
          {{ editDialog.value || '请选择日期' }}
        </view>
      </picker>
      <view class="dialog-buttons">
        <button @click="$refs.editPopup.close()">取消</button>
        <button @click="confirmEdit(editDialog.value)">确定</button>
      </view>
    </uni-popup-dialog>

    <!-- 加载遮罩 -->
    <view v-if="loading" class="loading-mask">
      <view class="loading-content">
        <text class="loading-text">加载中...</text>
      </view>
    </view>
  </view>
</template>

<script>
import request from '@/utils/request.js'
import CustomTabbar from '@/components/custom-tabbar.vue' 
import uniPopup from '@dcloudio/uni-ui/lib/uni-popup/uni-popup.vue'
import uniPopupDialog from '@dcloudio/uni-ui/lib/uni-popup-dialog/uni-popup-dialog.vue'

export default {
  components: { 
    CustomTabbar,
    uniPopup,
    uniPopupDialog
  },
  data() {
    return {
      loading: false,
      uploading: false,
      notificationSent: false,
      userInfo: {
        id: null,
        userId: null,
        name: '',
        gender: 'unknown',
        birthday: '',
        avatarUrl: '/static/avatar-default.png',
        skills: [],
        availability: '',
        experience: '',
        trainingCertificates: [],
        serviceArea: '',
        verificationStatus: 'pending',
        verificationDocuments: [],
        serviceHours: 0,
        phone: '',
        email: '',
        address: '',
        idNumber: ''
      },
      verificationStatusOptions: ['pending', 'approved', 'rejected'],
      availabilityOptions: ['工作日白天', '工作日晚上', '周末全天', '节假日'],
      serviceAreaOptions: ['西城区', '东城区', '朝阳区', '海淀区', '丰台区'],
      skillOptions: ['医疗护理', '心理咨询', '语言翻译', '电脑操作', '手工制作', '音乐艺术'],
      certificateOptions: ['急救证书', '社工证书', '外语等级证书', '其他'],
      editDialog: {
        field: '',
        title: '',
        value: '',
        type: 'text',
        options: []
      },
      multiSelectDialog: {
        field: '',
        title: '',
        selected: [],
        options: []
      },
      tempBirthday: ''
    }
  },
  onLoad() {
    uni.$on('volunteerAvatarUpdated', this.handleAvatarUpdated);
  },
  onShow() {
    this.loadVolunteerInfo();
  },
  onUnload() {
    uni.$off('volunteerAvatarUpdated', this.handleAvatarUpdated);
    if (this.timer) clearInterval(this.timer);
    if (this.authPollingTimer) clearInterval(this.authPollingTimer);
  },
  computed: {
    isPasswordValid() {
      const pattern = /^(?=.*[A-Za-z])(?=.*\d)[A-Za-z\d._]{8,16}$/;
      return pattern.test(this.newPassword);
    },
    isResetFormValid() {
      return this.forgotPhone && this.verifyCode && this.newPassword && this.confirmPassword && 
             this.newPassword === this.confirmPassword && this.isPasswordValid;
    }
  },
  methods: {
    formatVerificationStatus(status) {
      const statusMap = {
        pending: '待审核',
        approved: '已通过',
        rejected: '已拒绝'
      };
      return statusMap[status] || '未知状态';
    },
    formatGender(gender) {
      const genderMap = {
        male: '男',
        female: '女',
        unknown: '未知'
      };
      return genderMap[gender] || '未知';
    },
    editBirthday() {
      this.tempBirthday = this.userInfo.birthday;
      this.$refs.datePopup.open();
    },
    onDateChange(e) {
      this.tempBirthday = e.detail.value;
    },
    confirmDate() {
      if (this.tempBirthday) {
        this.userInfo.birthday = this.tempBirthday;
        this.$refs.datePopup.close();
        this.saveToServer();
      } else {
        uni.showToast({ title: '请选择有效日期', icon: 'none' });
      }
    },

    // 加载志愿者信息
    async loadVolunteerInfo() {
      let loadingShown = false;
      try {
        const loginUser = uni.getStorageSync('userInfo');
        if (!loginUser || !loginUser.id) {
          uni.showToast({ title: '请先登录', icon: 'none' });
          setTimeout(() => uni.redirectTo({ url: '/pages/login/login' }), 1500);
          return;
        }
        const userId = parseInt(loginUser.id, 10);
        this.userInfo.userId = userId;

        uni.showLoading({ title: '加载中...' });
        loadingShown = true;

        const res = await request.get('/api/volunteer/infoByUserId', { userId });

        if (res.success && res.volunteerInfo) {
          const backendData = res.volunteerInfo;
          this.userInfo = {
            ...this.userInfo,
            ...backendData,
            skills: this.parseBlobToArray(backendData.skills),
            trainingCertificates: this.parseBlobToArray(backendData.trainingCertificates),
            verificationDocuments: this.parseBlobToArray(backendData.verificationDocuments),
            birthday: backendData.birthday || '',
            avatarUrl: backendData.avatarUrl || '/static/avatar-default.png',
            serviceHours: backendData.serviceHours || 0,
            phone: backendData.phone || '',
            email: backendData.email || '',
            address: backendData.address || '',
            idNumber: backendData.idNumber || ''
          };
          uni.setStorageSync('volunteerProfile', this.userInfo);
        } else {
          const cachedInfo = uni.getStorageSync('volunteerProfile');
          if (cachedInfo) {
            this.userInfo = { ...this.userInfo, ...cachedInfo };
          }
          uni.showToast({ title: res.message || '获取志愿者信息失败', icon: 'none' });
        }
      } catch (error) {
        console.error('加载志愿者信息失败:', error);
        uni.showToast({ title: '网络错误，无法加载信息', icon: 'none' });
        const cachedInfo = uni.getStorageSync('volunteerProfile');
        if (cachedInfo) this.userInfo = { ...this.userInfo, ...cachedInfo };
      } finally {
        if (loadingShown) {
          uni.hideLoading();
        }
        this.loading = false;
      }
    },

    parseBlobToArray(blobData) {
      if (!blobData || typeof blobData !== 'string') return [];
      return blobData.split(',').map(item => item.trim()).filter(item => item);
    },

    confirmEdit(value) {
      if (this.editDialog.type === 'date') {
        if (value) {
          this.userInfo[this.editDialog.field] = value;
          this.$refs.editPopup.close();
          this.saveToServer();
        } else {
          uni.showToast({ title: '请选择有效日期', icon: 'none' });
        }
        return;
      }

      if (value && value.trim()) {
        this.userInfo[this.editDialog.field] = value.trim();
        this.$refs.editPopup.close();
        this.saveToServer();
      } else {
        uni.showToast({ title: '请输入有效内容', icon: 'none' });
      }
    },

    getAvatarUrl(avatarUrl) {
      if (!avatarUrl || avatarUrl.trim() === '') {
        console.log('使用默认头像（头像URL为空）');
        return '/static/avatar-default.png';
      }

      if (avatarUrl.startsWith('http://') || avatarUrl.startsWith('https://')) {
        return avatarUrl.includes('?t=') 
          ? avatarUrl 
          : `${avatarUrl}?t=${Date.now()}`;
      }

      if (!request.BASE_URL) {
        console.error('❌ 未配置 request.BASE_URL，请检查/utils/request.js');
        return '/static/avatar-default.png';
      }

      const baseUrl = request.BASE_URL.endsWith('/') 
        ? request.BASE_URL.slice(0, -1)
        : request.BASE_URL;
      
      const normalizedAvatarUrl = avatarUrl.startsWith('/') 
        ? avatarUrl 
        : `/${avatarUrl}`;
      
      const fullUrl = `${baseUrl}${normalizedAvatarUrl}`;
      
      return fullUrl.includes('?t=') 
        ? fullUrl 
        : `${fullUrl}?t=${Date.now()}`;
    },

    handleImageLoad() {
      console.log('✅ 志愿者头像加载成功');
    },

    handleImageError(event) {
      console.error('❌ 志愿者头像加载失败', {
        error: event,
        currentAvatarUrl: this.getAvatarUrl(this.userInfo.avatarUrl),
        userId: this.userInfo.userId
      });
      
      if (this.userInfo.avatarUrl && this.userInfo.avatarUrl.includes('/uploads/')) {
        console.log('🔄 尝试重新加载志愿者头像...');
        setTimeout(() => {
          this.$forceUpdate();
        }, 1000);
      }
    },

    editAvatar() {
      if (this.uploading) {
        uni.showToast({ title: '正在上传头像，请稍候', icon: 'none' });
        return;
      }
      uni.showActionSheet({
        itemList: ['拍照', '从相册选择'],
        success: (res) => {
          if (res.tapIndex === 0) {
            this.chooseImage('camera');
          } else if (res.tapIndex === 1) {
            this.chooseImage('album');
          }
        },
        fail: (error) => {
          console.error('❌ 打开头像选择弹窗失败', error);
          uni.showToast({ title: '操作取消', icon: 'none' });
        }
      });
    },

    chooseImage(sourceType) {
      uni.chooseImage({
        count: 1,
        sizeType: ['compressed'],
        sourceType: [sourceType],
        success: (res) => {
          this.uploadAvatar(res.tempFilePaths[0]);
        },
        fail: (error) => {
          console.error(`❌ 选择${sourceType === 'camera' ? '拍照' : '相册图片'}失败`, error);
          uni.showToast({
            title: `获取图片失败，请检查${sourceType === 'camera' ? '相机' : '相册'}权限`,
            icon: 'none'
          });
        }
      });
    },

    async uploadAvatar(filePath) {
      if (this.uploading || !filePath) return;
      this.uploading = true;
      let loadingShown = false;
    
      try {
        const volunteerInfo = this.userInfo;
        if (!volunteerInfo || !volunteerInfo.id || !volunteerInfo.userId) {
          throw new Error('志愿者信息不完整，请重新进入页面');
        }
    
        uni.showLoading({ title: '头像上传中...', mask: true });
        loadingShown = true;
    
        const uploadRes = await new Promise((resolve, reject) => {
          uni.uploadFile({
            url: `${request.BASE_URL}/api/volunteer/uploadAvatar`,
            filePath: filePath,
            name: 'file',
            formData: { 
              id: volunteerInfo.id,
              userId: volunteerInfo.userId
            },
            header: {
              'X-App-Version': '1.0.0'
            },
            success: resolve,
            fail: (err) => {
              if (err.errMsg.includes('timeout')) {
                reject(new Error('上传超时，请检查网络稳定性'));
              } else if (err.errMsg.includes('network')) {
                reject(new Error('网络异常，请检查WiFi或数据连接'));
              } else if (err.errMsg.includes('auth deny')) {
                reject(new Error('请授予相册/相机权限后重试'));
              } else {
                reject(new Error(`上传请求失败: ${err.errMsg}`));
              }
            }
          });
        });
    
        // 修复：更宽松的响应处理
        let result;
        try {
          result = uploadRes.data ? JSON.parse(uploadRes.data) : {};
        } catch (e) {
          console.warn('响应解析异常，但继续处理:', e);
          // 即使解析失败，如果状态码是200也尝试处理
          if (uploadRes.statusCode === 200) {
            result = { success: true };
          } else {
            throw new Error('服务器响应异常');
          }
        }
    
        // 修复：多种成功情况的判断
        const isSuccess = (
          (result.success === true) ||
          (result.code === 200) ||
          (uploadRes.statusCode === 200 && result.message && result.message.includes('成功'))
        );
    
        if (isSuccess) {
          // 修复：优先使用返回的avatarUrl，如果没有则使用现有逻辑
          let newAvatarUrl = result.avatarUrl;
          
          if (!newAvatarUrl) {
            // 如果没有返回avatarUrl，构建默认URL
            newAvatarUrl = `/uploads/volunteer/avatar/${volunteerInfo.id}_${Date.now()}.jpg`;
            console.log('使用默认头像URL:', newAvatarUrl);
          }
    
          // 修复：立即更新本地状态
          this.userInfo.avatarUrl = newAvatarUrl;
          
          // 修复：强制视图更新
          this.$forceUpdate();
          
          // 修复：保存到本地存储
          uni.setStorageSync('volunteerProfile', this.userInfo);
          
          // 修复：更新用户信息中的头像
          const loginUser = uni.getStorageSync('userInfo');
          if (loginUser) {
            loginUser.avatarUrl = newAvatarUrl;
            uni.setStorageSync('userInfo', loginUser);
          }
    
          // 修复：显示成功提示
          uni.showToast({ 
            title: '头像上传成功', 
            icon: 'success',
            duration: 2000 
          });
    
          // 修复：通知其他页面
          this.notifyOtherPages();
    
          // 修复：可选 - 重新加载用户信息确保数据一致
          setTimeout(() => {
            this.loadVolunteerInfo();
          }, 500);
    
        } else {
          // 修复：更友好的错误提示
          const errorMsg = result.message || '头像上传失败，请稍后重试';
          throw new Error(errorMsg);
        }
    
      } catch (error) {
        console.error('头像上传失败:', error);
        uni.showToast({ 
          title: error.message, 
          icon: 'none', 
          duration: 3000 
        });
        
        if (error.message.includes('登录已过期')) {
          setTimeout(() => {
            uni.redirectTo({ url: '/pages/login/login' });
          }, 1500);
        }
      } finally {
        this.uploading = false;
        if (loadingShown) {
          uni.hideLoading();
        }
      }
    },
    // 二次确认登录状态
    async checkLoginStatus() {
      let loadingShown = false;
      try {
        uni.showLoading({ title: '检查登录状态...', mask: false });
        loadingShown = true;

        const res = await request.get('/api/volunteer/infoByUserId', {
          userId: this.userInfo.userId
        });
        return res.code === 401;
      } catch (error) {
        return false;
      } finally {
        if (loadingShown) {
          uni.hideLoading();
        }
      }
    },

    async preloadAndUpdateAvatar(avatarUrl, retryCount = 0) {
      const maxRetries = 3;
      const fullUrl = this.getAvatarUrl(avatarUrl);
      let loadingShown = false;

      try {
        if (retryCount === 0) {
          uni.showLoading({ title: '加载头像...', mask: false });
          loadingShown = true;
        }

        await new Promise((resolve, reject) => {
          const timeout = setTimeout(() => {
            reject(new Error('图片加载超时'));
          }, 8000);

          uni.getImageInfo({
            src: fullUrl,
            success: () => {
              clearTimeout(timeout);
              resolve();
            },
            fail: (error) => {
              clearTimeout(timeout);
              reject(new Error(`图片加载失败: ${error.errMsg}`));
            }
          });
        });

        this.userInfo.avatarUrl = fullUrl;
        this.$forceUpdate();
        await this.saveToServer();
        this.notifyOtherPages();
        uni.showToast({ title: '头像更新成功', icon: 'success' });
      } catch (error) {
        if (retryCount < maxRetries) {
          const delay = (retryCount + 1) * 1000;
          console.log(`🔄 重试验证头像 (${retryCount + 1}/${maxRetries})`, error.message);
          setTimeout(() => {
            this.preloadAndUpdateAvatar(avatarUrl, retryCount + 1);
          }, delay);
        } else {
          console.error('❌ 头像更新失败，重试用尽', error);
          uni.showToast({ title: '头像更新失败，请重试', icon: 'none' });
        }
      } finally {
        if (loadingShown && retryCount === 0) {
          uni.hideLoading();
        }
      }
    },

    notifyOtherPages() {
      if (this.notificationSent) return;
      uni.$emit('volunteerAvatarUpdated', {
        userId: this.userInfo.userId,
        avatarUrl: this.userInfo.avatarUrl,
        timestamp: Date.now(),
        sender: 'volunteerProfile'
      });
      this.notificationSent = true;
    },

    handleAvatarUpdated(data) {
      if (data.sender === 'volunteerProfile') return;
      if (data.userId === this.userInfo.userId && !this.uploading) {
        this.userInfo.avatarUrl = data.avatarUrl;
        this.$forceUpdate();
      }
    },

    editField(field, title, value, options = [], type = 'text') {
      this.editDialog = { 
        field, 
        title, 
        value: value || '', 
        type,
        options 
      };

      if (type === 'date') {
        this.$refs.editPopup.open('center');
        return;
      }

      if (type === 'select' && options.length > 0) {
        const labelMap = {
          gender: {
            'male': '男',
            'female': '女',
            'unknown': '未知'
          },
          verificationStatus: {
            'pending': '待审核',
            'approved': '已通过',
            'rejected': '已拒绝'
          }
        };

        const currentMap = labelMap[field] || {};
        const itemList = options.map(opt => currentMap[opt] || opt);
        
        uni.showActionSheet({
          title: `选择${title}`,
          itemList: itemList,
          success: (res) => {
            const selectedValue = options[res.tapIndex];
            this.userInfo[field] = selectedValue;
            this.saveToServer();
          }
        });
        return;
      }

      if (type === 'textarea') {
        this.$refs.editPopup.open('center');
        return;
      }

      this.$refs.editPopup.open('center');
    },

    editMultiSelect(field, title, selected, options) {
      this.multiSelectDialog = {
        field,
        title,
        selected: [...(selected || [])],
        options
      }
      this.$refs.multiSelectPopup.open('bottom');
    },

    closeEdit() {
      this.$refs.editPopup.close();
    },

    multiSelectChange(e) {
      this.multiSelectDialog.selected = e.detail.value;
    },

    confirmMultiSelect() {
      if (this.multiSelectDialog.selected.length > 0) {
        this.userInfo[this.multiSelectDialog.field] = [...this.multiSelectDialog.selected];
        this.$refs.multiSelectPopup.close();
        this.saveToServer();
      } else {
        uni.showToast({ title: '请至少选择一项', icon: 'none' });
      }
    },

    validateSubmitData() {
      const data = this.userInfo;
      if (!data.id || !data.userId) {
        uni.showToast({ title: '用户信息不完整，无法保存', icon: 'none' });
        return false;
      }
      if (data.idNumber && !/^\d{17}[\dXx]$/.test(data.idNumber)) {
        uni.showToast({ title: '身份证号格式不正确', icon: 'none' });
        return false;
      }
      if (data.phone && !/^\d{11}$/.test(data.phone)) {
        uni.showToast({ title: '手机号格式不正确', icon: 'none' });
        return false;
      }
      if (data.email && !/^[\w-]+(\.[\w-]+)*@([\w-]+\.)+[a-zA-Z]{2,7}$/.test(data.email)) {
        uni.showToast({ title: '邮箱格式不正确', icon: 'none' });
        return false;
      }
      if (data.birthday) {
        const birthday = new Date(data.birthday);
        const today = new Date();
        if (birthday > today) {
          uni.showToast({ title: '出生日期不能是未来日期', icon: 'none' });
          return false;
        }
      }
      return true;
    },

    async saveToServer() {
      if (!this.validateSubmitData()) return;
      let loadingShown = false;
      try {
        this.loading = true;
        uni.showLoading({ title: '保存中...', mask: true });
        loadingShown = true;

        let avatarUrl = this.userInfo.avatarUrl;
        if (avatarUrl.includes(request.BASE_URL)) {
          avatarUrl = avatarUrl.replace(request.BASE_URL, '');
        }

        const submitData = {
          id: this.userInfo.id,                  
          userId: this.userInfo.userId,          
          name: this.userInfo.name,              
          gender: this.userInfo.gender,          
          birthday: this.userInfo.birthday,      
          avatarUrl: avatarUrl,
          skills: this.userInfo.skills?.join(','),
          availability: this.userInfo.availability,
          experience: this.userInfo.experience,   
          trainingCertificates: this.userInfo.trainingCertificates?.join(','),
          serviceArea: this.userInfo.serviceArea, 
          verificationStatus: this.userInfo.verificationStatus,
          verificationDocuments: this.userInfo.verificationDocuments?.join(','),
          serviceHours: this.userInfo.serviceHours,
          phone: this.userInfo.phone,
          email: this.userInfo.email,
          address: this.userInfo.address,
          idNumber: this.userInfo.idNumber
        };

        const res = await request.post('/api/volunteer/update', submitData);
        if (res.success) {
          uni.showToast({ title: '修改成功', icon: 'success' });
          uni.setStorageSync('volunteerProfile', this.userInfo);
          this.loadVolunteerInfo();
        } else {
          uni.showToast({ title: res.message || '修改失败', icon: 'none' });
          const cachedInfo = uni.getStorageSync('volunteerProfile');
          if (cachedInfo) this.userInfo = { ...this.userInfo, ...cachedInfo };
        }
      } catch (error) {
        console.error('提交修改失败:', error);
        uni.showToast({ title: '网络错误，修改未保存', icon: 'none' });
        const cachedInfo = uni.getStorageSync('volunteerProfile');
        if (cachedInfo) this.userInfo = { ...this.userInfo, ...cachedInfo };
      } finally {
        this.loading = false;
        if (loadingShown) {
          uni.hideLoading();
        }
      }
    }
  }
}
</script>

<style scoped>
.volunteer-profile {
  padding-bottom: 120rpx;
  min-height: 100vh;
  background-color: #F8F4F4;
  padding: 20rpx;
}

.profile-card {
  background: #9AB169;
  padding: 60rpx 40rpx;
  display: flex;
  align-items: center;
  color: white;
  border-radius: 32rpx;
  margin-top: 20rpx;
  margin-bottom: 30rpx;
  box-shadow: 0 4rpx 12rpx rgba(0,0,0,0.05);
}

.avatar-container {
  width: 140rpx;
  height: 140rpx;
  border-radius: 50%;
  background-color: rgba(255,255,255,0.2);
  padding: 5rpx;
  margin-right: 30rpx;
  position: relative;
}

.avatar {
  width: 100%;
  height: 100%;
  border-radius: 50%;
  border: 4rpx solid rgba(255,255,255,0.3);
}

.basic-info {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.name {
  font-size: 40rpx;
  font-weight: bold;
  margin-bottom: 10rpx;
  color: white;
}

.id {
  font-size: 30rpx;
  opacity: 0.9;
  margin-bottom: 5rpx;
  color: white;
}

.hours {
  font-size: 30rpx;
  opacity: 0.9;
  color: white;
}

.detail-info {
  background-color: transparent;
}

/* 分组标题（在卡片外部） */
.section-title {
  font-size: 32rpx;
  font-weight: bold;
  color: #4B3425;
  margin-bottom: 15rpx;
  margin-top: 10rpx;
  display: block;
  text-align: center;
  /* 底部间距 */
  margin-bottom: 40rpx;
}

.info-group {
  background-color: #fff;
  border-radius: 32rpx;
  margin-bottom: 30rpx;
  overflow: hidden;
  box-shadow: 0 4rpx 12rpx rgba(0,0,0,0.05);
}

/* 移除原来的 group-title */
.group-title {
  display: none;
}

/* 添加分隔线 */
.divider-line {
  height: 2rpx;
  background-color: #e8e8e8;
  margin: 0 30rpx;
}

.info-item {
  display: flex;
  padding: 30rpx;
  align-items: center;
  position: relative;
  transition: background-color 0.2s ease;
}

.info-item:active {
  background-color: #f8f9fa;
}

/* 底部留白 */
.bottom-spacer {
  height: 100rpx;
}

.item-label {
  width: 200rpx;
  font-size: 32rpx;
  color: #4B3425;
  font-weight: 600;
}

.item-value {
  flex: 1;
  font-size: 30rpx;
  color: #666;
  text-align: right;
  padding-right: 45rpx;
}

.edit-icon {
  width: 32rpx;
  height: 32rpx;
  position: absolute;
  right: 30rpx;
  opacity: 0.7;
}

/* 多选弹窗样式 */
.multi-select-popup {
  background-color: #fff;
  padding: 40rpx 30rpx;
  border-radius: 32rpx 32rpx 0 0;
  max-height: 70vh;
  overflow-y: auto;
}

.popup-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-bottom: 20rpx;
  border-bottom: 2rpx solid #e8e8e8;
  margin-bottom: 30rpx;
}

.popup-title {
  font-size: 36rpx;
  font-weight: bold;
  color: #4B3425;
}

.popup-confirm {
  font-size: 32rpx;
  color: #9AB169;
  font-weight: 600;
}

.checkbox-item {
  display: flex;
  align-items: center;
  padding: 25rpx 0;
  border-bottom: 1rpx solid #f5f5f5;
}

.checkbox-item text {
  font-size: 30rpx;
  color: #4B3425;
}

checkbox {
  margin-right: 20rpx;
  transform: scale(1.1);
}
</style>