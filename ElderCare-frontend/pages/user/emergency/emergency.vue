<template>
  <view class="emergency-container">
    <!-- 主内容区 -->
    <view class="content">
      <!-- 完美居中的求助按钮 -->
      <view class="btn-wrapper">
        <view 
          class="emergency-btn" 
          :class="{ 'pressed': isBtnPressed, 'requesting': isRequesting }"
          @touchstart="handleTouchStart"
          @touchend="handleTouchEnd"
          @touchcancel="handleTouchEnd"
          @click="handleEmergency"
        >
          <!-- 旋转光圈 -->
          <view class="rotating-ring"></view>
          
          <!-- 内层光晕 -->
          <view class="inner-glow"></view>
          
          <!-- 按钮主体 -->
          <view class="btn-inner">
            <image v-if="!isRequesting" src="/static/emergency-icon.png" class="btn-icon"></image>
            <!-- 请求中显示加载动画 -->
            <view v-else class="loading-spinner"></view>
            <text class="btn-text">{{ isRequesting ? '发送中...' : '一键救助' }}</text>
          </view>
        </view>
      </view>

      <!-- 实时定位信息 -->
      <view class="location-card">
        <view class="location-header">
          <view class="location-title-wrapper">
            <uni-icons type="location-filled" size="18" color="#e54d42"></uni-icons>
            <text>当前位置</text>
          </view>
          <view class="refresh-wrapper" @click="refreshLocation">
            <text class="refresh-text">刷新位置</text>
            <view :class="['refresh-btn', { 'refreshing': isLocating }]">
              <uni-icons type="refresh" size="18" color="#e54d42"></uni-icons>
            </view>
          </view>
        </view>
        <view class="location-content">
          <text v-if="currentAddress" class="address-text">{{ currentAddress }}</text>
          <text v-else class="loading-text">{{ locationStatus }}</text>
          <view class="location-coords" v-if="location">
            <text>经度: {{ location.longitude.toFixed(6) }}</text>
            <text>纬度: {{ location.latitude.toFixed(6) }}</text>
            <text v-if="location.accuracy">精度: {{ location.accuracy }}米</text>
          </view>
        </view>
      </view>

      <!-- 紧急联系人 -->
      <view class="section-title">
        <text>紧急联系人</text>
      </view>
      <view class="contact-list">
        <!-- 120急救电话（固定） -->
        <view class="contact-item emergency-120">
          <view class="contact-info">
            <text class="contact-name">急救中心</text>
            <text class="contact-phone">120</text>
          </view>
          <button class="call-btn emergency-call-btn" @click="callContact('120')">
            呼叫
          </button>
        </view>
        
        <!-- 用户添加的紧急联系人 -->
        <view 
          class="contact-item" 
          v-for="contact in emergencyContacts" 
          :key="contact.relationId"
          @longpress="confirmDeleteContact(contact)"
        >
          <view class="contact-info">
            <text class="contact-name">{{ contact.remarks || contact.name }}</text>
            <text class="contact-phone">{{ contact.phone }}</text>
          </view>
          <button class="call-btn" @click="callContact(contact.phone)">
            呼叫
          </button>
        </view>
        
        <!-- 添加联系人按钮 -->
        <view class="add-contact-btn" @click="showAddContactDialog">
          <uni-icons type="plusempty" size="20" color="#666"></uni-icons>
          <text>添加紧急联系人</text>
        </view>
      </view>
    </view>

    <!-- 添加紧急联系人弹窗 -->
    <uni-popup ref="addContactPopup" type="dialog">
      <view class="dialog-container">
        <view class="dialog-header">
          <text class="dialog-title">{{ dialogStep === 1 ? '输入电话号码' : '确认联系人' }}</text>
          <uni-icons type="closeempty" size="24" @click="closeAddContactDialog"></uni-icons>
        </view>
        
        <!-- 步骤1：输入电话号码 -->
        <view v-if="dialogStep === 1" class="dialog-content">
          <view class="form-group">
            <text class="form-label">监护人电话</text>
            <input 
              class="form-input" 
              type="number" 
              v-model="newContactPhone"
              placeholder="请输入监护人的手机号码"
              maxlength="11"
            />
          </view>
        </view>
        
        <!-- 步骤2：确认监护人信息并输入备注 -->
        <view v-else-if="dialogStep === 2" class="dialog-content">
          <view class="confirm-info">
            <view class="info-row">
              <text class="info-label">姓名：</text>
              <text class="info-value">{{ verifiedGuardian.name }}</text>
            </view>
            <view class="info-row">
              <text class="info-label">电话：</text>
              <text class="info-value">{{ verifiedGuardian.phone }}</text>
            </view>
          </view>
          
          <view class="form-group">
            <text class="form-label">备注名称</text>
            <input 
              class="form-input" 
              type="text" 
              v-model="newContactRemarks"
              placeholder="例如：儿子、女儿、老伴等"
              maxlength="20"
            />
          </view>
        </view>
        
        <view class="dialog-footer">
          <button class="dialog-btn cancel-btn" @click="closeAddContactDialog">
            取消
          </button>
          <button 
            class="dialog-btn confirm-btn" 
            @click="dialogStep === 1 ? verifyGuardian() : confirmAddContact()"
          >
            {{ dialogStep === 1 ? '下一步' : '完成' }}
          </button>
        </view>
      </view>
    </uni-popup>
  </view>

    <!-- 后端接口实现示例（实际应放在API文件中） -->
    <!-- 
    // 获取实时位置接口
    async function getRealTimeLocation(token) {
      try {
        const res = await uni.request({
          url: '/api/location/realtime',
          method: 'GET',
          header: { 'Authorization': `Bearer ${token}` }
        });
        return {
          success: true,
          data: {
            longitude: res.data.longitude,
            latitude: res.data.latitude,
            address: res.data.address
          }
        };
      } catch (error) {
        return { success: false, error };
      }
    }

    // 触发紧急求助接口
    async function triggerEmergency(token, location) {
      try {
        const res = await uni.request({
          url: '/api/emergency',
          method: 'POST',
          header: { 'Authorization': `Bearer ${token}` },
          data: {
            longitude: location.longitude,
            latitude: location.latitude,
            timestamp: new Date().getTime()
          }
        });
        return { success: true, orderId: res.data.helporderid };
      } catch (error) {
        return { success: false, error };
      }
    }
    -->
 <!-- </view> -->
</template>

<script>
import config from '@/utils/config.js'

export default {
  data() {
    return {
      location: null,
      currentAddress: '',
      locationStatus: '获取位置中...',
      emergencyContacts: [], // 紧急联系人列表
      isRequesting: false,
      isLocating: false, // 防止重复定位
      isBtnPressed: false, // 按钮按下状态
      
      // 添加联系人相关
      dialogStep: 1, // 1=输入电话，2=确认并输入备注
      newContactPhone: '',
      newContactRemarks: '',
      verifiedGuardian: {} // 验证通过的监护人信息
    }
  },
  methods: {
    goBack() {
      uni.navigateBack();
    },
    
    // 处理触摸开始
    handleTouchStart() {
      this.isBtnPressed = true;
      // 短震动反馈
      uni.vibrateShort({
        success: () => {
          console.log('✅ 按钮触摸震动反馈');
        }
      });
    },
    
    // 处理触摸结束
    handleTouchEnd() {
      this.isBtnPressed = false;
    },
    
    // 获取实时位置
    async getRealTimeLocation() {
      // 防止重复定位
      if (this.isLocating) {
        console.log('⚠️ 正在定位中，跳过重复请求');
        return;
      }
      
      this.isLocating = true;
      this.locationStatus = '正在定位...';
      
      console.log('📍 开始获取紧急求助位置...');
      
      uni.getLocation({
        type: 'gcj02', // 国测局坐标
        altitude: true, // 获取高度
        isHighAccuracy: true, // 高精度定位
        success: async (res) => {
          console.log('✅ 定位成功！', res);
          
          this.location = {
            longitude: res.longitude,
            latitude: res.latitude,
            accuracy: res.accuracy,
            altitude: res.altitude
          };
          
          this.locationStatus = '解析地址中...';
          
          // 调用逆地理编码获取地址
          await this.callReverseGeocode(res.longitude, res.latitude);
        },
        fail: (err) => {
          console.error('❌ 定位失败：', err);
          this.locationStatus = '定位失败，请检查定位权限';
          this.currentAddress = '';
          this.location = null;
          
          uni.showToast({
            title: '定位失败',
            icon: 'none'
          });
        },
        complete: () => {
          this.isLocating = false;
        }
      });
    },
    
    // 调用高德逆地理编码API
    async callReverseGeocode(longitude, latitude) {
      try {
        console.log('🗺️ 调用逆地理编码API:', longitude, latitude);
        
        const response = await uni.request({
          url: `${config.API_BASE_URL}/api/geocoding/regeo`,
          method: 'GET',
          data: {
            longitude: longitude,
            latitude: latitude,
            radius: 500,
            extensions: 'base'
          }
        });
        
        console.log('📥 逆地理编码返回:', response.data);
        
        if (response.data.success) {
          this.currentAddress = response.data.formattedAddress;
          console.log('✅ 地址解析成功:', this.currentAddress);
        } else {
          this.currentAddress = `经度:${longitude.toFixed(6)}, 纬度:${latitude.toFixed(6)}`;
          console.error('❌ 地址解析失败:', response.data.message);
        }
        
      } catch (error) {
        console.error('❌ 逆地理编码API调用失败:', error);
        this.currentAddress = `经度:${longitude.toFixed(6)}, 纬度:${latitude.toFixed(6)}`;
      }
    },
    
    // 刷新位置
    refreshLocation() {
      if (this.isLocating) {
        uni.showToast({
          title: '正在定位中...',
          icon: 'none'
        });
        return;
      }
      
      this.getRealTimeLocation();
    },
    
    // 处理紧急求助
    async handleEmergency() {
      if (this.isRequesting) return;
      
      if (!this.location) {
        uni.showToast({ title: '请等待定位完成', icon: 'none' });
        return;
      }
      
      uni.showModal({
        title: '紧急求助确认',
        content: `将向紧急联系人发送您的位置：
${this.currentAddress || '位置获取中'}`,
        confirmText: '立即求助',
        confirmColor: '#e54d42',
        success: async (res) => {
          if (res.confirm) {
            this.isRequesting = true;
            
            try {
              // 1. 获取用户信息
              const userInfo = uni.getStorageSync('userInfo');
              const elderId = userInfo?.id;
              
              if (!elderId) {
                uni.showToast({ title: '请先登录', icon: 'none' });
                this.isRequesting = false;
                uni.hideLoading();
                return;
              }
              
              // 2. 获取老年人姓名
              const elderResponse = await uni.request({
                url: `${config.API_BASE_URL}/api/elder/selfinfo/${elderId}`,
                method: 'GET'
              });
              
              const elderName = elderResponse.data?.name || '未知用户';
              console.log('✅ 老年人姓名:', elderName);
              
              // 3. 获取紧急联系人的推送信息
              const contactsResponse = await uni.request({
                url: `${config.API_BASE_URL}/api/emergency-contact/push-info`,
                method: 'GET',
                data: { elderId: elderId }
              });
              
              if (!contactsResponse.data.success) {
                uni.showToast({ 
                  title: contactsResponse.data.message || '获取联系人失败', 
                  icon: 'none' 
                });
                this.isRequesting = false;
                uni.hideLoading();
                return;
              }
              
              const pushInfoList = contactsResponse.data.pushInfoList || [];
              console.log('✅ 获取到紧急联系人:', pushInfoList.length, '个');
              
              if (pushInfoList.length === 0) {
                uni.showModal({
                  title: '提示',
                  content: '您还没有添加紧急联系人，无法发送求助消息',
                  showCancel: false
                });
                this.isRequesting = false;
                uni.hideLoading();
                return;
              }
              
              // 4. 调用云函数向每个紧急联系人推送消息
              const pushContent = `老年人【${elderName}】向您发出紧急求助，地点位于${this.currentAddress || '未知地点'}`;
              
              // 构造 payload 数据（注意：需要转为字符串）
              const payloadData = {
                type: 'emergency',
                elderId: elderId,
                elderName: elderName,
                location: this.currentAddress,
                longitude: this.location.longitude,
                latitude: this.location.latitude,
                timestamp: new Date().getTime()
              };
              
              let successCount = 0;
              let failCount = 0;
              
              for (const contact of pushInfoList) {
                try {
                  console.log(`📤 向 ${contact.name} 推送消息，pushClientId: ${contact.pushClientId}`);
                  
                  const pushResult = await uniCloud.callFunction({
                    name: 'push',
                    data: {
                      push_clientid: contact.pushClientId,
                      title: '紧急求助通知',
                      content: pushContent,
                      payload: JSON.stringify(payloadData) // 转为字符串
                    }
                  });
                  
                  console.log(`📡 云函数返回结果 ${contact.name}:`, pushResult);
                  
                  if (pushResult.result && pushResult.result.errCode === 0) {
                    console.log(`✅ 成功推送给 ${contact.name}`);
                    successCount++;
                  } else {
                    console.error(`❌ 推送失败 ${contact.name}:`, {
                      result: pushResult.result,
                      errCode: pushResult.result?.errCode,
                      errMsg: pushResult.result?.errMsg
                    });
                    failCount++;
                  }
                } catch (pushError) {
                  console.error(`❌ 推送异常 ${contact.name}:`, {
                    error: pushError,
                    message: pushError.message,
                    code: pushError.code
                  });
                  failCount++;
                }
              }
              
              // 5. 显示推送结果
              uni.hideLoading();
              
              if (successCount > 0) {
                uni.showToast({ 
                  title: `已向${successCount}个联系人发送求助`, 
                  icon: 'success',
                  duration: 2000
                });
                
                // 6. 推送成功后，自动拨打120急救电话
                setTimeout(() => {
                  this.callEmergencyNumber();
                }, 2000); // 等待提示显示完成后拨打
              } else {
                uni.showToast({ 
                  title: '推送失败，请检查网络', 
                  icon: 'none' 
                });
              }
              
              if (failCount > 0) {
                console.warn(`⚠️ 有${failCount}个联系人推送失败`);
              }
              
            } catch (error) {
              console.error('❌ 求助失败:', error);
              uni.hideLoading();
              uni.showToast({ title: '求助发送失败', icon: 'none' });
            } finally {
              this.isRequesting = false;
            }
          }
        }
      });
    },
    
    // 拨打120急救电话
    callEmergencyNumber() {
      uni.showModal({
        title: '拨打120急救电话',
        content: '是否立即拨打120急救电话？',
        confirmText: '立即拨打',
        confirmColor: '#e54d42',
        cancelText: '暂不拨打',
        success: (res) => {
          if (res.confirm) {
            console.log('📞 开始拨打120急救电话');
            uni.makePhoneCall({
              phoneNumber: '120',
              success: () => {
                console.log('✅ 120急救电话拨打成功');
              },
              fail: (err) => {
                console.error('❌ 拨打120失败:', err);
                uni.showToast({
                  title: '拨打失败，请手动拨打120',
                  icon: 'none',
                  duration: 2000
                });
              }
            });
          } else {
            console.log('👤 用户取消拨打120');
          }
        }
      });
    },
    
    // 呼叫联系人
    callContact(phone) {
      console.log('📞 准备拨打电话:', phone);
      
      uni.makePhoneCall({ 
        phoneNumber: phone,
        success: () => {
          console.log('✅ 电话拨打成功:', phone);
        },
        fail: (err) => {
          console.error('❌ 拨打电话失败:', err);
          uni.showToast({
            title: '拨打失败，请检查权限',
            icon: 'none',
            duration: 2000
          });
        }
      });
    },

    // 获取紧急联系人列表
    async getEmergencyContacts() {
      try {
        const userInfo = uni.getStorageSync('userInfo');
        const elderId = userInfo?.id;
        
        if (!elderId) {
          console.error('未获取到用户ID');
          return;
        }
        
        const response = await uni.request({
          url: `${config.API_BASE_URL}/api/emergency-contact/list`,
          method: 'GET',
          data: { elderId: elderId }
        });
        
        if (response.data.success) {
          this.emergencyContacts = response.data.contacts || [];
          console.log('✅ 获取到', this.emergencyContacts.length, '个紧急联系人');
        } else {
          console.error('获取紧急联系人失败:', response.data.message);
        }
        
      } catch (error) {
        console.error('获取紧急联系人异常:', error);
      }
    },

    // 显示添加联系人弹窗
    showAddContactDialog() {
      this.dialogStep = 1;
      this.newContactPhone = '';
      this.newContactRemarks = '';
      this.verifiedGuardian = {};
      this.$refs.addContactPopup.open();
    },

    // 关闭添加联系人弹窗
    closeAddContactDialog() {
      this.$refs.addContactPopup.close();
      this.dialogStep = 1;
      this.newContactPhone = '';
      this.newContactRemarks = '';
      this.verifiedGuardian = {};
    },

    // 验证监护人电话
    async verifyGuardian() {
      if (!this.newContactPhone || this.newContactPhone.length !== 11) {
        uni.showToast({
          title: '请输入正确的手机号',
          icon: 'none'
        });
        return;
      }
      
      const userInfo = uni.getStorageSync('userInfo');
      const elderId = userInfo?.id;
      
      if (!elderId) {
        uni.showToast({ title: '请先登录', icon: 'none' });
        return;
      }
      
      uni.showLoading({ title: '验证中...' });
      
      try {
        const response = await uni.request({
          url: `${config.API_BASE_URL}/api/emergency-contact/verify`,
          method: 'GET',
          data: {
            phone: this.newContactPhone,
            elderId: elderId
          }
        });
        
        uni.hideLoading();
        
        if (response.data.success) {
          // 验证成功，进入第二步
          this.verifiedGuardian = response.data.guardianInfo;
          this.dialogStep = 2;
        } else {
          uni.showModal({
            title: '验证失败',
            content: response.data.message,
            showCancel: false
          });
        }
        
      } catch (error) {
        uni.hideLoading();
        console.error('验证监护人失败:', error);
        uni.showToast({ title: '验证失败，请重试', icon: 'none' });
      }
    },

    // 确认添加联系人
    async confirmAddContact() {
      if (!this.newContactRemarks || this.newContactRemarks.trim() === '') {
        uni.showToast({
          title: '请输入备注名称',
          icon: 'none'
        });
        return;
      }
      
      const userInfo = uni.getStorageSync('userInfo');
      const elderId = userInfo?.id;
      
      if (!elderId) {
        uni.showToast({ title: '请先登录', icon: 'none' });
        return;
      }
      
      uni.showLoading({ title: '添加中...' });
      
      try {
        const response = await uni.request({
          url: `${config.API_BASE_URL}/api/emergency-contact/add`,
          method: 'POST',
          data: {
            elderId: elderId,
            guardianId: this.verifiedGuardian.guardianId,
            remarks: this.newContactRemarks.trim()
          },
          header: {
            'Content-Type': 'application/json'
          }
        });
        
        uni.hideLoading();
        
        if (response.data.success) {
          uni.showToast({
            title: '添加成功',
            icon: 'success'
          });
          
          // 关闭弹窗并刷新列表
          this.closeAddContactDialog();
          this.getEmergencyContacts();
        } else {
          uni.showModal({
            title: '添加失败',
            content: response.data.message,
            showCancel: false
          });
        }
        
      } catch (error) {
        uni.hideLoading();
        console.error('添加联系人失败:', error);
        uni.showToast({ title: '添加失败，请重试', icon: 'none' });
      }
    },

    // 确认删除联系人
    confirmDeleteContact(contact) {
      uni.showModal({
        title: '删除联系人',
        content: `确定要删除紧急联系人"${contact.remarks || contact.name}"吗？`,
        confirmColor: '#e54d42',
        success: (res) => {
          if (res.confirm) {
            this.deleteContact(contact);
          }
        }
      });
    },

    // 删除联系人
    async deleteContact(contact) {
      const userInfo = uni.getStorageSync('userInfo');
      const elderId = userInfo?.id;
      
      if (!elderId) {
        uni.showToast({ title: '请先登录', icon: 'none' });
        return;
      }
      
      uni.showLoading({ title: '删除中...' });
      
      try {
        const response = await uni.request({
          url: `${config.API_BASE_URL}/api/emergency-contact/delete?relationId=${contact.relationId}&elderId=${elderId}`,
          method: 'DELETE'
        });
        
        uni.hideLoading();
        
        if (response.data.success) {
          uni.showToast({
            title: '已删除',
            icon: 'success'
          });
          
          // 刷新列表
          this.getEmergencyContacts();
        } else {
          uni.showToast({
            title: response.data.message,
            icon: 'none'
          });
        }
        
      } catch (error) {
        uni.hideLoading();
        console.error('删除联系人失败:', error);
        uni.showToast({ title: '删除失败，请重试', icon: 'none' });
      }
    }
  },
  
  mounted() {
    // 进入页面时自动获取位置
    this.getRealTimeLocation();
    
    // 获取紧急联系人列表
    this.getEmergencyContacts();
  }
}
</script>

<style scoped>
.emergency-container {
  height: 100vh;
  display: flex;
  flex-direction: column;
  background-color: #f8f8f8;
}

.header {
  height: 88rpx;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 30rpx;
  background-color: #fff;
  border-bottom: 1rpx solid #eee;
}

.header-title {
  position: absolute;
  left: 50%;
  transform: translateX(-50%);
  font-size: 32rpx;
  font-weight: bold;
  color: #333;
}

.content {
  flex: 1;
  padding: 20rpx 30rpx;
  overflow-y: auto;
}

/* 完美居中按钮样式 */
.btn-wrapper {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 400rpx;
  position: relative;
  padding: 40rpx 0;
}

/* 外层光晕容器 */
.emergency-btn {
  width: 320rpx;
  height: 320rpx;
  position: relative;
  display: flex;
  justify-content: center;
  align-items: center;
}

/* 外层光晕1（最外层，慢速脉冲） */
.emergency-btn::before {
  content: '';
  position: absolute;
  width: 100%;
  height: 100%;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(229, 77, 66, 0.3) 0%, rgba(229, 77, 66, 0.1) 50%, transparent 70%);
  animation: pulse-outer 2s ease-in-out infinite;
  z-index: 1;
}

/* 中层光晕2（中速脉冲） */
.emergency-btn::after {
  content: '';
  position: absolute;
  width: 90%;
  height: 90%;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(255, 77, 79, 0.4) 0%, rgba(255, 77, 79, 0.2) 60%, transparent 80%);
  animation: pulse-middle 1.5s ease-in-out infinite;
  animation-delay: 0.3s;
  z-index: 2;
}

/* 旋转光圈 */
.rotating-ring {
  position: absolute;
  width: 310rpx;
  height: 310rpx;
  border-radius: 50%;
  border: 3rpx solid transparent;
  border-top-color: rgba(255, 255, 255, 0.6);
  border-right-color: rgba(255, 255, 255, 0.4);
  border-bottom-color: rgba(255, 255, 255, 0.2);
  animation: rotate-ring 3s linear infinite;
  z-index: 2;
}

@keyframes rotate-ring {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}

/* 内层光晕 */
.inner-glow {
  position: absolute;
  width: 305rpx;
  height: 305rpx;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(255, 255, 255, 0.3) 0%, transparent 70%);
  animation: glow-pulse 2s ease-in-out infinite;
  z-index: 2;
}

@keyframes glow-pulse {
  0%, 100% {
    opacity: 0.5;
    transform: scale(0.98);
  }
  50% {
    opacity: 0.8;
    transform: scale(1.02);
  }
}

/* 内层按钮主体 */
.btn-inner {
  width: 300rpx;
  height: 300rpx;
  background: linear-gradient(135deg, #ff4d4f 0%, #ff6b6b 50%, #e54d42 100%);
  border-radius: 50%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  box-shadow: 
    0 10rpx 40rpx rgba(229, 77, 66, 0.5),
    0 20rpx 60rpx rgba(229, 77, 66, 0.3),
    0 30rpx 80rpx rgba(229, 77, 66, 0.2),
    inset 0 -5rpx 15rpx rgba(0, 0, 0, 0.2),
    inset 0 5rpx 15rpx rgba(255, 255, 255, 0.3);
  position: relative;
  z-index: 3;
  transition: all 0.3s cubic-bezier(0.34, 1.56, 0.64, 1);
  animation: float 3s ease-in-out infinite;
}

/* 点击反馈效果 */
.emergency-btn.pressed .btn-inner {
  transform: scale(0.92);
  box-shadow: 
    0 5rpx 20rpx rgba(229, 77, 66, 0.6),
    0 10rpx 30rpx rgba(229, 77, 66, 0.4),
    inset 0 -3rpx 10rpx rgba(0, 0, 0, 0.3),
    inset 0 3rpx 10rpx rgba(255, 255, 255, 0.2);
  animation: none; /* 按下时暂停浮动动画 */
}

/* 增强按下时的光晕效果 */
.emergency-btn.pressed::before {
  animation: pulse-pressed 0.3s ease-out;
}

.emergency-btn.pressed::after {
  animation: pulse-pressed 0.3s ease-out;
  animation-delay: 0.1s;
}

/* 按下时旋转光圈加速 */
.emergency-btn.pressed .rotating-ring {
  animation: rotate-ring 0.8s linear infinite;
  border-width: 5rpx;
  opacity: 1;
}

/* 按下时内层光晕增强 */
.emergency-btn.pressed .inner-glow {
  opacity: 1;
  transform: scale(1.05);
}

/* 按下时的脉冲动画 */
@keyframes pulse-pressed {
  0% {
    transform: scale(1);
    opacity: 0.6;
  }
  50% {
    transform: scale(1.3);
    opacity: 0.8;
  }
  100% {
    transform: scale(1.15);
    opacity: 0.3;
  }
}

/* 图标和文字容器 - 使用绝对定位居中 */
.btn-icon {
  width: 70rpx;
  height: 70rpx;
  position: absolute;
  top: 60rpx;
  left: 50%;
  transform: translateX(-50%);
  filter: drop-shadow(0 4rpx 8rpx rgba(0, 0, 0, 0.3));
}

.btn-text {
  color: white;
  font-size: 38rpx;
  font-weight: bold;
  text-align: center;
  letter-spacing: 4rpx;
  text-shadow: 0 4rpx 8rpx rgba(0, 0, 0, 0.3);
  line-height: 1;
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  white-space: nowrap;
  /* 严格居中，不添加任何margin偏移 */
}

/* 外层脉冲动画（慢速） */
@keyframes pulse-outer {
  0%, 100% {
    transform: scale(1);
    opacity: 0.6;
  }
  50% {
    transform: scale(1.15);
    opacity: 0.3;
  }
}

/* 中层脉冲动画（中速） */
@keyframes pulse-middle {
  0%, 100% {
    transform: scale(1);
    opacity: 0.8;
  }
  50% {
    transform: scale(1.1);
    opacity: 0.4;
  }
}

/* 按钮主体浮动动画 */
@keyframes float {
  0%, 100% {
    transform: translateY(0) scale(1);
  }
  50% {
    transform: translateY(-10rpx) scale(1.02);
  }
}

/* 请求中状态 */
.emergency-btn.requesting .btn-inner {
  animation: requesting-pulse 1s ease-in-out infinite;
  pointer-events: none; /* 禁止重复点击 */
}

.emergency-btn.requesting .rotating-ring {
  animation: rotate-ring 1s linear infinite;
  border-top-color: rgba(255, 255, 255, 0.9);
  border-width: 4rpx;
}

@keyframes requesting-pulse {
  0%, 100% {
    transform: scale(1);
    opacity: 0.9;
  }
  50% {
    transform: scale(1.05);
    opacity: 1;
  }
}

@keyframes spin {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}

/* 定位卡片样式 */
.location-card {
  background-color: #fff;
  border-radius: 32rpx;
  padding: 24rpx;
  margin: 30rpx 0;
  box-shadow: 0 2rpx 12rpx rgba(0, 0, 0, 0.05);
}

.location-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20rpx;
  font-size: 28rpx;
  color: #333;
}

.location-title-wrapper {
  display: flex;
  align-items: center;
}

.location-title-wrapper text {
  margin-left: 10rpx;
}

.refresh-wrapper {
  display: flex;
  align-items: center;
  gap: 10rpx;
  padding: 8rpx 12rpx;
  background: #f5f5f5;
  border-radius: 30rpx;
  transition: all 0.2s ease;
}

.refresh-wrapper:active {
  background: #e0e0e0;
}

.refresh-text {
  font-size: 24rpx;
  color: #666;
  white-space: nowrap;
}

.refresh-btn {
  width: 32rpx;
  height: 32rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.3s ease;
}

/* 刷新中的旋转动画 */
.refresh-btn.refreshing {
  animation: rotate-refresh 1s linear infinite;
}

@keyframes rotate-refresh {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}

.location-content {
  font-size: 28rpx;
}

.address-text {
  color: #333;
  font-size: 30rpx;
  line-height: 1.6;
  font-weight: 500;
}

.location-coords {
  margin-top: 15rpx;
  font-size: 24rpx;
  color: #999;
  display: flex;
  flex-direction: column;
  gap: 8rpx;
}

.location-coords text {
  line-height: 1.4;
}

.loading-text {
  color: #999;
  font-size: 28rpx;
}

/* 联系人列表样式 */
.section-title {
  font-size: 32rpx;
  font-weight: bold;
  margin: 30rpx 0 20rpx;
  color: #333;
}

.contact-list {
  background-color: #fff;
  border-radius: 32rpx;
  overflow: hidden;
  box-shadow: 0 2rpx 12rpx rgba(0, 0, 0, 0.05);
}

.contact-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 28rpx;
  border-bottom: 1rpx solid #f0f0f0;
  transition: background-color 0.2s ease;
}

.contact-item:active {
  background-color: #f8f8f8;
}

/* 120急救电话特殊样式 */
.emergency-120 {
  background: linear-gradient(90deg, #fff5f5 0%, #ffffff 100%);
  border-left: 4rpx solid #e54d42;
}

.contact-info {
  flex: 1;
}

.contact-name {
  font-size: 32rpx;
  display: block;
  margin-bottom: 8rpx;
}

.contact-phone {
  font-size: 26rpx;
  color: #999;
}

.call-btn {
  background-color: #e54d42;
  color: white;
  font-size: 26rpx;
  height: 56rpx;
  line-height: 56rpx;
  padding: 0 30rpx;
  border-radius: 28rpx;
}

/* 添加联系人按钮 */
.add-contact-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 28rpx;
  color: #666;
  font-size: 28rpx;
  gap: 10rpx;
  background: #ffffff;
  border-top: 2rpx dashed #e0e0e0;
}

.add-contact-btn:active {
  background: #f5f5f5;
}

/* 弹窗样式 */
.dialog-container {
  width: 600rpx;
  background: #fff;
  border-radius: 20rpx;
  overflow: hidden;
}

.dialog-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 30rpx;
  border-bottom: 1rpx solid #f0f0f0;
}

.dialog-title {
  font-size: 32rpx;
  font-weight: bold;
  color: #333;
}

.dialog-content {
  padding: 30rpx;
  max-height: 60vh;
  overflow-y: auto;
}

.form-group {
  margin-bottom: 30rpx;
}

.form-label {
  display: block;
  font-size: 28rpx;
  color: #666;
  margin-bottom: 15rpx;
}

.form-input {
  width: 100%;
  height: 80rpx;
  padding: 0 20rpx;
  font-size: 28rpx;
  border: 1rpx solid #e0e0e0;
  border-radius: 10rpx;
  box-sizing: border-box;
}

.form-input:focus {
  border-color: #9AB169;
}

.confirm-info {
  background: #f8f9fa;
  border-radius: 10rpx;
  padding: 20rpx;
  margin-bottom: 30rpx;
}

.info-row {
  display: flex;
  padding: 10rpx 0;
  font-size: 28rpx;
}

.info-label {
  color: #666;
  width: 120rpx;
}

.info-value {
  flex: 1;
  color: #333;
  font-weight: 500;
}

.dialog-footer {
  display: flex;
  border-top: 1rpx solid #f0f0f0;
}

.dialog-btn {
  flex: 1;
  height: 90rpx;
  line-height: 90rpx;
  text-align: center;
  font-size: 28rpx;
  border: none;
  background: none;
}

.cancel-btn {
  color: #666;
  border-right: 1rpx solid #f0f0f0;
}

.confirm-btn {
  color: #e54d42;
  font-weight: bold;
}
</style>