# ASR语音识别配置说明

## 📋 概述

此目录包含了集成腾讯云、阿里云、科大讯飞ASR语音识别功能所需的核心文件。目前ElderCare项目中已配置使用腾讯云ASR服务。

## 🔧 配置步骤

### 1. 申请腾讯云ASR服务

1. 前往 [腾讯云控制台](https://console.cloud.tencent.com/)
2. 开通"语音识别"服务
3. 获取以下密钥信息：
   - `secretId`：访问密钥ID
   - `secretKey`：访问密钥Key  
   - `appId`：应用ID

### 2. 配置密钥

在 `pages/user/chat/chat.vue` 文件中找到以下位置并替换密钥：

```javascript
// 初始化腾讯云ASR，请填入你的实际密钥
asr = new TengxunAsr({
  secretKey: 'your-tencent-secret-key',   // 替换为你的腾讯云Secret Key
  secretId: 'your-tencent-secret-id',     // 替换为你的腾讯云Secret ID
  appId: 'your-tencent-app-id',           // 替换为你的腾讯云App ID
  params: this.asrParams
});
```

### 3. 安装依赖

确保项目已安装crypto-js依赖：

```bash
npm install crypto-js@^4.2.0
```

## 📁 文件说明

- `TengxunAsr.js`：腾讯云ASR接口封装
- `AliyunAsr.js`：阿里云ASR接口封装（可选）
- `XunfeiAsr.js`：科大讯飞ASR接口封装（可选）
- `record.js`：录音管理器，处理音频采集
- `tool.js`：工具函数，包含音频格式转换等
- `appPermission.js`：权限管理，处理麦克风权限申请

## 🎯 功能特性

- ✅ 实时语音识别
- ✅ 边录边识别，无需等待录音结束
- ✅ 自动权限管理
- ✅ 跨平台支持（Android、iOS、小程序）
- ✅ 识别结果实时显示
- ✅ 完整的错误处理

## 🚀 使用方法

1. 点击聊天界面底部的麦克风图标开始录音
2. 语音识别结果会实时显示在聊天区域
3. 再次点击麦克风图标停止录音
4. 识别完成的内容会自动作为消息发送给大模型

## 🔍 注意事项

- 需要在真机上测试（模拟器不支持录音功能）
- 首次使用会申请麦克风权限
- 建议在安静环境下进行语音输入
- 网络连接需要稳定以保证ASR服务正常工作

## 🐛 常见问题

### Q1: 录音权限被拒绝
**A:** 检查设备权限设置，确保应用获得麦克风权限

### Q2: 语音识别不准确
**A:** 确保网络连接稳定，尝试在安静环境下说话

### Q3: ASR服务连接失败
**A:** 检查密钥配置是否正确，确认腾讯云服务是否开通

### Q4: 录音无响应
**A:** 确保在真机上测试，检查原生插件是否正确安装

## 📞 支持

如有问题，请检查：
1. 密钥配置是否正确
2. 网络连接是否正常
3. 设备权限是否授予
4. 是否在真机环境测试
