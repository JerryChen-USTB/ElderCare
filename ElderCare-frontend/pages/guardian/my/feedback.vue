<template>
  <view class="feedback-container">
    <!-- 内容区域 -->
    <view class="content">
      <!-- 常见问题 -->
      <view class="section">
        <text class="section-title">常见问题</text>
        <uni-collapse>
          <uni-collapse-item v-for="(item, index) in faqs" :key="index" :title="item.question">
            <text class="faq-answer">{{ item.answer }}</text>
          </uni-collapse-item>
        </uni-collapse>
      </view>
      
      <!-- 反馈表单 -->
      <view class="section">
        <text class="section-title">意见反馈</text>
        
        <!-- 反馈类型 -->
        <view class="form-item">
          <text class="form-label">反馈类型</text>
          <picker @change="bindTypeChange" :value="feedback.typeIndex" :range="feedbackTypes">
            <view class="picker">
              <text v-if="feedback.typeIndex >= 0 && feedback.typeIndex < feedbackTypes.length">
                {{ feedbackTypes[feedback.typeIndex] }}
              </text>
              <text v-else class="placeholder">请选择反馈类型</text>
            </view>
          </picker>
        </view>
        
        <!-- 反馈内容 -->
        <view class="form-item">
          <text class="form-label">反馈内容</text>
          <textarea 
            class="feedback-content" 
            v-model="feedback.content" 
            placeholder="请详细描述您的问题或建议" 
            maxlength="500"
            @input="validateContent"
          ></textarea>
          <text class="word-count {{ feedback.content.length > 450 ? 'warning' : '' }}">
            {{ feedback.content.length }}/500
          </text>
          <text class="error-message" v-if="errors.content">{{ errors.content }}</text>
        </view>
        
        <!-- 上传截图 -->
        <view class="form-item">
          <text class="form-label">上传截图</text>
          <view class="upload-container">
            <uni-file-picker 
              limit="3" 
              fileMediatype="image" 
              @select="selectImage"
              @delete="deleteImage"
              :disabled="isUploading"
            ></uni-file-picker>
            
            <!-- 图片预览 -->
            <view class="image-preview" v-if="feedback.images && feedback.images.length > 0">
              <view class="image-item" v-for="(image, index) in feedback.images" :key="index">
                <image :src="image" mode="aspectFill"></image>
                <view class="delete-btn" @click.stop="deleteImage({index})">
                  <uni-icons type="closeempty" size="24" color="#fff"></uni-icons>
                </view>
              </view>
            </view>
            
            <!-- 上传中状态 -->
            <view class="uploading-status" v-if="isUploading">
              <uni-icons type="loading" size="30" color="#3cc51f"></uni-icons>
              <text>图片上传中...</text>
            </view>
          </view>
        </view>
        
        <!-- 联系方式 -->
        <view class="form-item">
          <text class="form-label">联系方式</text>
          <input 
            class="contact-input" 
            v-model="feedback.contact" 
            placeholder="手机/邮箱(选填)" 
            type="text"
            @blur="validateContact"
          />
          <text class="error-message" v-if="errors.contact">{{ errors.contact }}</text>
        </view>
        
        <!-- 提交按钮 -->
        <button 
          class="submit-btn" 
          @click="submitFeedback"
          :disabled="isSubmitting || !isFormValid"
          :class="{ 'disabled': !isFormValid || isSubmitting }"
        >
          {{ isSubmitting ? '提交中...' : '提交反馈' }}
        </button>
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
import EmergencyModal from '@/components/emergency-modal.vue'
import EmergencyModalMixin from '@/mixins/emergency-modal.js'

export default {
  components: { EmergencyModal },
  mixins: [EmergencyModalMixin],
  data() {
    return {
      faqs: [
        {
          question: "如何添加健康监测设备?",
          answer: "在'我的设备'页面点击右上角+号，选择设备类型并按照提示完成配对。"
        },
        {
          question: "紧急求助功能如何使用?",
          answer: "长按首页的红色求助按钮3秒，或说出'救命'等关键词即可触发求助。"
        },
        {
          question: "如何修改个人资料?",
          answer: "进入'我的'页面，点击头像旁边的编辑按钮即可修改个人信息。"
        }
      ],
      feedbackTypes: ["功能建议", "使用问题", "投诉", "其他"],
      feedback: {
        typeIndex: 0,
        content: "",
        images: [],
        contact: ""
      },
      errors: {
        content: "",
        contact: ""
      },
      isSubmitting: false,
      isUploading: false,
      uploadedImages: [] // 存储已上传的图片URL
    };
  },
  computed: {
    // 计算属性：判断表单是否有效
    isFormValid() {
      return this.feedback.content.length > 0 && !this.errors.content && !this.errors.contact;
    }
  },
  methods: {
    goBack() {
      uni.navigateBack();
    },
    
    // 选择反馈类型
    bindTypeChange(e) {
      this.feedback.typeIndex = e.detail.value;
    },
    
    // 选择图片
    async selectImage(e) {
      console.log("选择文件:", e);
      
      // 如果有临时文件，开始上传
      if (e.tempFilePaths && e.tempFilePaths.length > 0) {
        this.isUploading = true;
        
        try {
          // 模拟图片上传过程
          await this.uploadImages(e.tempFilePaths);
          
          // 更新图片列表
          this.feedback.images = [...this.feedback.images, ...e.tempFilePaths];
          
          uni.showToast({
            title: "图片上传成功",
            icon: "success",
            duration: 1500
          });
        } catch (error) {
          console.error("图片上传失败:", error);
          uni.showToast({
            title: "图片上传失败，请重试",
            icon: "none",
            duration: 2000
          });
        } finally {
          this.isUploading = false;
        }
      }
    },
    
    // 删除图片
    deleteImage(e) {
      console.log("删除文件:", e);
      this.feedback.images = this.feedback.images.filter(
        (item, index) => index !== e.index
      );
    },
    
    // 验证反馈内容
    validateContent() {
      if (!this.feedback.content.trim()) {
        this.errors.content = "请填写反馈内容";
      } else if (this.feedback.content.length > 500) {
        this.errors.content = "反馈内容不能超过500字";
      } else {
        this.errors.content = "";
      }
    },
    
    // 验证联系方式
    validateContact() {
      if (this.feedback.contact.trim()) {
        // 如果填写了联系方式，验证格式
        const phoneReg = /^1[3-9]\d{9}$/;
        const emailReg = /^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$/;
        
        if (!phoneReg.test(this.feedback.contact) && !emailReg.test(this.feedback.contact)) {
          this.errors.contact = "请输入正确的手机号或邮箱";
        } else {
          this.errors.contact = "";
        }
      } else {
        // 未填写则清空错误
        this.errors.contact = "";
      }
    },
    
    // 上传图片（模拟）
    uploadImages(filePaths) {
      return new Promise((resolve, reject) => {
        // 实际项目中应调用后端图片上传接口
        // 这里使用setTimeout模拟上传过程
        setTimeout(() => {
          // 模拟上传成功，生成临时URL
          const uploadedUrls = filePaths.map(path => {
            // 实际项目中应返回真实的图片URL
            return path;
          });
          
          // 保存已上传的图片URL
          this.uploadedImages = [...this.uploadedImages, ...uploadedUrls];
          resolve(uploadedUrls);
        }, 1000);
      });
    },
    
    // 提交反馈
    async submitFeedback() {
      // 表单验证
      this.validateContent();
      this.validateContact();
      
      if (!this.isFormValid) {
        return;
      }
      
      // 如果有图片但还未上传，先上传图片
      if (this.feedback.images.length > 0 && this.uploadedImages.length !== this.feedback.images.length) {
        try {
          this.isUploading = true;
          await this.uploadImages(this.feedback.images);
        } catch (error) {
          this.isUploading = false;
          uni.showToast({
            title: "图片上传失败，请重试",
            icon: "none"
          });
          return;
        }
        this.isUploading = false;
      }
      
      // 开始提交反馈
      this.isSubmitting = true;
      
      try {
        // 调用提交反馈API
        const result = await this.submitFeedbackToAPI();
        
        if (result.success) {
          uni.showToast({
            title: "反馈提交成功！",
            icon: "success",
            duration: 2500
          });
          
          // 延迟返回上一页
          setTimeout(() => {
            this.goBack();
          }, 2500);
        } else {
          throw new Error(result.message || "提交失败");
        }
      } catch (error) {
        console.error("提交反馈失败:", error);
        uni.showToast({
          title: error.message || "提交失败，请重试",
          icon: "none",
          duration: 2500
        });
      } finally {
        this.isSubmitting = false;
      }
    },
    
    // 提交反馈到API（模拟）
    submitFeedbackToAPI() {
      return new Promise(resolve => {
        // 实际项目中应调用后端接口
        setTimeout(() => {
          resolve({
            success: true,
            message: "提交成功"
          });
        }, 1500);
      });
    },
    
    // 获取常见问题（模拟）
    fetchFAQs() {
      // 实际项目中应调用后端接口
      /*
      uni.request({
        url: '/api/faq/list',
        method: 'GET',
        success: (res) => {
          if (res.data && res.data.length > 0) {
            this.faqs = res.data;
          }
        },
        fail: (err) => {
          console.error('获取常见问题失败:', err);
        }
      });
      */
    }
  },
  onLoad() {
    this.fetchFAQs();
  }
};
</script>

<style>
.feedback-container {
  display: flex;
  flex-direction: column;
  height: 100vh;
  background-color: #f8f8f8;
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 30rpx;
  background-color: #ffffff;
  border-bottom: 1rpx solid #f5f5f5;
}

.header-title {
  font-size: 36rpx;
  font-weight: bold;
  color: #333;
}

.content {
  flex: 1;
  padding: 20rpx;
  overflow-y: auto;
}

.section {
  background-color: #ffffff;
  padding: 30rpx;
  border-radius: 16rpx;
  margin-bottom: 30rpx;
  box-shadow: 0 4rpx 12rpx rgba(0, 0, 0, 0.05);
}

.section-title {
  display: block;
  font-size: 32rpx;
  font-weight: bold;
  color: #333;
  margin-bottom: 20rpx;
}

.faq-answer {
  display: block;
  font-size: 28rpx;
  color: #666;
  padding: 20rpx;
  line-height: 1.6;
}

.form-item {
  margin-bottom: 30rpx;
}

.form-label {
  display: block;
  font-size: 28rpx;
  color: #333;
  margin-bottom: 15rpx;
  font-weight: 500;
}

.picker {
  width: 100%;
  padding: 20rpx;
  border: 1rpx solid #e5e5e5;
  border-radius: 8rpx;
  font-size: 28rpx;
  color: #333;
  background-color: #fff;
}

.placeholder {
  color: #999;
}

.feedback-content {
  width: 100%;
  height: 200rpx;
  padding: 20rpx;
  border: 1rpx solid #e5e5e5;
  border-radius: 8rpx;
  font-size: 28rpx;
  color: #333;
  margin-bottom: 10rpx;
  line-height: 1.5;
}

.word-count {
  display: block;
  text-align: right;
  font-size: 24rpx;
  color: #999;
}

.word-count.warning {
  color: #ff4d4f;
}

.error-message {
  display: block;
  font-size: 24rpx;
  color: #ff4d4f;
  margin-top: 8rpx;
}

.contact-input {
  width: 100%;
  padding: 20rpx;
  border: 1rpx solid #e5e5e5;
  border-radius: 8rpx;
  font-size: 28rpx;
  color: #333;
}

.upload-container {
  position: relative;
}

.image-preview {
  display: flex;
  flex-wrap: wrap;
  margin-top: 20rpx;
}

.image-item {
  position: relative;
  width: 180rpx;
  height: 180rpx;
  margin-right: 20rpx;
  margin-bottom: 20rpx;
  border-radius: 8rpx;
  overflow: hidden;
}

.image-item image {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.delete-btn {
  position: absolute;
  top: 0;
  right: 0;
  width: 40rpx;
  height: 40rpx;
  background-color: rgba(0, 0, 0, 0.6);
  display: flex;
  justify-content: center;
  align-items: center;
}

.uploading-status {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: rgba(255, 255, 255, 0.8);
  display: flex;
  justify-content: center;
  align-items: center;
  color: #3cc51f;
  font-size: 28rpx;
  border-radius: 8rpx;
}

.submit-btn {
  width: 100%;
  height: 88rpx;
  line-height: 88rpx;
  background-color: #3cc51f;
  color: #ffffff;
  font-size: 32rpx;
  border-radius: 44rpx;
  margin-top: 40rpx;
  text-align: center;
  font-weight: 500;
  box-shadow: 0 4rpx 12rpx rgba(60, 197, 31, 0.2);
}

.submit-btn:active {
  opacity: 0.8;
}

.submit-btn.disabled {
  background-color: #e5e5e5;
  color: #999;
  box-shadow: none;
}
</style>