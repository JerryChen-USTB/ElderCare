# 🎙️ 语音合成功能快速测试指南

## 一、测试前准备检查

### ✅ 1. 环境变量检查
确认已设置阿里云 API Key 环境变量：

**检查方式（Windows）：**
```cmd
echo %DASHSCOPE_API_KEY%
```

**检查方式（Linux/Mac）：**
```bash
echo $DASHSCOPE_API_KEY
```

如果显示为空，请先配置：
```cmd
# Windows
setx DASHSCOPE_API_KEY "你的API-Key"

# Linux/Mac
export DASHSCOPE_API_KEY="你的API-Key"
```

⚠️ **注意：** 设置后需要重启 IDE 和终端！

### ✅ 2. Maven 依赖检查
确认 `pom.xml` 中已添加 DashScope SDK 依赖（已完成）

### ✅ 3. 启动服务
- **后端：** 启动 Spring Boot 应用（端口 8080）
- **前端：** 启动 uni-app 开发服务器

## 二、测试方式

### 方式一：使用测试页面（推荐）

#### 1. 访问测试页面
在浏览器中打开：
```
http://localhost:8080/#/pages/test/tts-test
```

或在 uni-app 中任意页面添加导航按钮：
```vue
<button @click="goToTtsTest">测试 TTS</button>

<script>
methods: {
  goToTtsTest() {
    uni.navigateTo({ url: '/pages/test/tts-test' })
  }
}
</script>
```

#### 2. 测试步骤
1. 输入文本（或点击快捷示例）
2. 选择音色（推荐中文用 Cherry，英文用 Dylan）
3. 选择语言类型
4. 点击"🎵 开始合成"
5. 等待合成完成，自动播放音频

#### 3. 功能验证
- ✅ 文本输入正常
- ✅ 音色切换有效
- ✅ 语言识别准确
- ✅ 音频播放流畅
- ✅ URL 可复制

### 方式二：直接调用 API

#### 使用 Postman/Apifox 测试

**接口信息：**
- **URL:** `http://localhost:8080/api/tts/synthesize`
- **方法:** POST
- **Content-Type:** application/json

**请求体示例：**
```json
{
  "text": "你好，我是智能语音助手小乐，很高兴为您服务！",
  "voice": "Cherry",
  "languageType": "Chinese"
}
```

**成功响应示例：**
```json
{
  "success": true,
  "audioUrl": "https://dashscope-result-bj.oss-cn-beijing.aliyuncs.com/...",
  "message": "语音合成成功"
}
```

复制 `audioUrl` 在浏览器中打开即可播放！

### 方式三：使用 curl 命令

```bash
curl -X POST http://localhost:8080/api/tts/synthesize \
  -H "Content-Type: application/json" \
  -d '{
    "text": "今天天气真不错",
    "voice": "Cherry",
    "languageType": "Chinese"
  }'
```

## 三、测试场景

### 🇨🇳 中文测试
```json
{
  "text": "欢迎使用智慧养老助手系统，我们致力于为老年人提供贴心的服务。",
  "voice": "Cherry",
  "languageType": "Chinese"
}
```

### 🇺🇸 英文测试
```json
{
  "text": "Hello! Today is a wonderful day to build something people love!",
  "voice": "Dylan",
  "languageType": "English"
}
```

### 🎭 不同音色对比
测试所有音色，感受差异：
- **Cherry** - 甜美温柔，适合女性场景
- **Qwen** - 沉稳平和，适合男性场景
- **Dylan** - 成熟专业，适合商务场景
- **Jada** - 亲切自然，适合客服场景
- **Sunny** - 活泼年轻，适合活力场景

## 四、常见问题排查

### ❌ 错误 1：API Key 未配置
**现象：**
```json
{
  "success": false,
  "message": "API Key 未配置，请检查环境变量 DASHSCOPE_API_KEY"
}
```

**解决：**
1. 检查环境变量是否设置
2. 重启 IDE 和后端服务
3. 在代码中打印验证：`System.getenv("DASHSCOPE_API_KEY")`

### ❌ 错误 2：文本为空
**现象：**
```json
{
  "success": false,
  "message": "文本内容不能为空"
}
```

**解决：** 确保 `text` 字段不为空

### ❌ 错误 3：后端连接失败
**现象：** 前端显示"网络错误"

**解决：**
1. 检查后端是否启动（8080 端口）
2. 检查 `config.js` 中的 API 地址配置
3. 查看浏览器控制台网络请求

### ❌ 错误 4：音频无法播放
**可能原因：**
- 音频 URL 已过期
- 浏览器不支持该音频格式
- 网络问题

**解决：**
1. 重新生成音频
2. 尝试在新标签页打开音频 URL
3. 检查浏览器控制台错误

## 五、性能测试

### 测试指标
- ⏱️ **响应时间：** 通常 1-3 秒
- 🎵 **音频质量：** 48kHz 采样率，清晰自然
- 📦 **文件大小：** 约 100-500KB（取决于文本长度）

### 压力测试建议
```javascript
// 批量测试示例
const texts = [
  "测试文本1",
  "测试文本2",
  "测试文本3"
]

for (let text of texts) {
  await synthesize(text)
  await sleep(1000) // 避免请求过快
}
```

## 六、后续集成计划

### 📋 下一步工作
1. ✅ 完成 TTS 基础接入（已完成）
2. 🔲 在智能聊天模块添加语音播放按钮
3. 🔲 实现音频缓存机制
4. 🔲 添加语速、音调等高级参数控制
5. 🔲 支持离线音频下载

### 🎯 集成建议
参考 `TTS_INTEGRATION_GUIDE.md` 中的详细集成方案。

---

## 📞 技术支持

如遇到问题：
1. 查看后端控制台日志
2. 查看浏览器控制台错误
3. 参考 `TTS_INTEGRATION_GUIDE.md` 详细文档
4. 提交 Issue 或联系开发团队

**测试愉快！🎉**
