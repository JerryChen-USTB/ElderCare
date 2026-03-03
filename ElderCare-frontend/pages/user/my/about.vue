<template>
  <view class="about-container">
    <!-- 内容区域 -->
    <view class="content">
      <!-- 应用信息卡片 -->
      <view class="info-card">
        <image class="app-logo" src="/static/eldercare_logo.png" mode="aspectFit"></image>
        <text class="app-name">{{ appInfo.name }}</text>
        <text class="app-version">版本号: {{ appInfo.version }}</text>
        
        <!-- 
          API: 获取应用信息
          接口地址: /api/app/info
          请求方法: GET
          返回: {name: "应用名称", version: "1.0.0", description: "应用描述"}
        -->
      </view>
      
      <!-- 功能介绍 -->
      <view class="section">
        <text class="section-title">功能介绍</text>
        <view class="feature-list">
          <view class="feature-item" v-for="(feature, index) in features" :key="index">
            <uni-icons :type="feature.icon" size="20" color="#3cc51f"></uni-icons>
            <text class="feature-text">{{ feature.text }}</text>
          </view>
        </view>
      </view>
      
      <!-- 联系我们 -->
      <view class="section">
        <text class="section-title">联系我们</text>
        <view class="contact-list">
          <view class="contact-item" @click="makePhoneCall">
            <uni-icons type="phone" size="20" color="#3cc51f"></uni-icons>
            <text class="contact-text">客服电话: {{ contactInfo.phone }}</text>
          </view>
          <view class="contact-item" @click="sendEmail">
            <uni-icons type="email" size="20" color="#3cc51f"></uni-icons>
            <text class="contact-text">客服邮箱: {{ contactInfo.email }}</text>
          </view>
          <view class="contact-item" @click="openWebsite">
            <uni-icons type="link" size="20" color="#3cc51f"></uni-icons>
            <text class="contact-text">官方网站: {{ contactInfo.website }}</text>
          </view>
        </view>
        
        <!-- 
          API: 获取联系信息
          接口地址: /api/contact/info
          请求方法: GET
          返回: {phone: "客服电话", email: "客服邮箱", website: "官网地址"}
        -->
      </view>
      
      <!-- 用户协议和隐私政策 -->
      <view class="agreement-section">
        <text class="agreement-text" @click="showAgreement">《用户协议》</text>
        <text class="agreement-text" @click="showPrivacyPolicy">《隐私政策》</text>
        
        <!-- 
          API: 获取协议内容
          接口地址: /api/agreement/content
          请求方法: GET
          参数: type (agreement/privacy)
          返回: {title: "标题", content: "内容"}
        -->
      </view>
    </view>
  </view>
</template>

<script>
export default {
  data() {
    return {
      appInfo: {
        name: "银杏智伴",
        version: "1.0.3",
        description: "专为老年人设计的健康监测与生活辅助应用"
      },
      features: [
        { icon: "heart", text: "实时健康监测" },
        { icon: "chat", text: "智能语音交互" },
        { icon: "chat", text: "志愿者远程协助" },
        { icon: "help", text: "紧急求助功能" }, 
        { icon: "calendar", text: "智能提醒服务" },
        { icon: "location", text: "实时定位监护" }
      ],
      contactInfo: {
        phone: "18910488457",
        email: "U202342508@xs.ustb.edu.cn",
        website: "www.eldercare.com"
      }
    };
  },
  methods: {
    goBack() {
      uni.navigateBack();
    },
    makePhoneCall() {
      uni.makePhoneCall({
        phoneNumber: this.contactInfo.phone
      });
    },
    sendEmail() {
      // 预留发送邮件接口
      /*
      API: 发送邮件接口
      接口地址: /api/contact/email
      请求方法: POST
      参数: {email: "收件邮箱", subject: "邮件主题", content: "邮件内容"}
      返回: 发送状态
      */
      uni.showToast({
        title: '即将打开邮件应用',
        icon: 'none'
      });
    },
    openWebsite() {
      // 预留打开网站接口
      /*
      API: 获取官网链接
      接口地址: /api/website/url
      请求方法: GET
      返回: {url: "官网链接"}
      */
      uni.navigateTo({
        url: '/pages/webview/webview?url=' + encodeURIComponent(this.contactInfo.website)
      });
    },
    showAgreement() {
      // 预留获取用户协议接口
      uni.navigateTo({
        url: '/pages/agreement/agreement?type=agreement'
      });
    },
    showPrivacyPolicy() {
      // 预留获取隐私政策接口
      uni.navigateTo({
        url: '/pages/agreement/agreement?type=privacy'
      });
    },
    fetchAppInfo() {
      // 预留获取应用信息接口
      /*
      API: 获取应用信息
      接口地址: /api/app/info
      请求方法: GET
      返回: {name: "应用名称", version: "版本号", description: "描述"}
      */
    },
    fetchContactInfo() {
      // 预留获取联系信息接口
      /*
      API: 获取联系信息
      接口地址: /api/contact/info
      请求方法: GET
      返回: {phone: "电话", email: "邮箱", website: "官网"}
      */
    }
  },
  onLoad() {
    this.fetchAppInfo();
    this.fetchContactInfo();
  }
};
</script>

<style>
.about-container {
  display: flex;
  flex-direction: column;
  height: 100vh;
  background-color: #F8F4F4;
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

.info-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 40rpx 20rpx;
  background-color: #ffffff;
  border-radius: 32rpx;
  margin-bottom: 30rpx;
  box-shadow: 0 4rpx 12rpx rgba(0, 0, 0, 0.05);
}

.app-logo {
  width: 120rpx;
  height: 120rpx;
  margin-bottom: 20rpx;
  border-radius: 40rpx;
}

.app-name {
  font-size: 36rpx;
  font-weight: bold;
  color: #333;
  margin-bottom: 10rpx;
}

.app-version {
  font-size: 28rpx;
  color: #999;
}

.section {
  background-color: #ffffff;
  padding: 30rpx;
  border-radius: 32rpx;
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

.feature-list {
  display: flex;
  flex-direction: column;
}

.feature-item {
  display: flex;
  align-items: center;
  padding: 20rpx 0;
  border-bottom: 1rpx solid #f5f5f5;
}

.feature-item:last-child {
  border-bottom: none;
}

.feature-text {
  margin-left: 20rpx;
  font-size: 28rpx;
  color: #666;
}

.contact-list {
  display: flex;
  flex-direction: column;
}

.contact-item {
  display: flex;
  align-items: center;
  padding: 20rpx 0;
  border-bottom: 1rpx solid #f5f5f5;
}

.contact-item:last-child {
  border-bottom: none;
}

.contact-text {
  margin-left: 20rpx;
  font-size: 28rpx;
  color: #666;
}

.agreement-section {
  display: flex;
  justify-content: center;
  margin-top: 40rpx;
}

.agreement-text {
  font-size: 26rpx;
  color: #3cc51f;
  margin: 0 20rpx;
  text-decoration: underline;
}
</style>