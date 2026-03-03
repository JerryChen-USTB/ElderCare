<template>
  <view class="chat-container">
    <!-- 状态栏占位 -->
    <view class="status-bar" :style="{height: statusBarHeight + 'px'}"></view>
    
    <!-- 自定义导航栏 -->
    <view class="custom-header">
      <image 
        src="/static/icons/back.png" 
        class="nav-icon back-icon" 
        @click="goBack"
        mode="aspectFit"
      />
      <view class="header-title">智能助手小乐</view>
      <view class="header-right">
        <image 
          src="/static/icons/new_chat.png" 
          class="nav-icon" 
          @click="startNewChat"
          mode="aspectFit"
        />
      </view>
    </view>
    
    <!-- 聊天内容区域（仅添加高度自适应） -->

    <scroll-view 
      class="chat-area" 
      scroll-y 
      :scroll-top="chatScrollTop"
      :style="{height: scrollHeight + 'px'}"
      @scroll="onChatScroll"
    >
      <!-- 顶部间距占位 -->
      <view class="top-spacer"></view>
    <!-- 聊天内容区域 -->
      <view 
        v-for="(msg, index) in chatHistory" 
        :key="index" 
        :id="'msg-' + index"
        :class="['message', msg.sender, { 'tap-highlight': tapHighlightIndex === index }]"
      >
        <view class="message-time">{{ formatTime(msg.timestamp) }}</view>
        <view class="message-with-icon">
          <view 
            class="message-content"
            :class="{ 'tap-active': tapHighlightIndex === index }"
            @tap="handleDoubleTap(msg, index)"
          >
            <!-- 如果是AI正在思考，显示思考动画 -->
            <view v-if="msg.sender === 'ai' && (msg.content === '正在思考中...' || msg.content.includes('正在思考'))" class="thinking-animation">
              <view class="thinking-dots">
                <view class="thinking-dot thinking-dot-1"></view>
                <view class="thinking-dot thinking-dot-2"></view>
                <view class="thinking-dot thinking-dot-3"></view>
              </view>
              <text class="thinking-text"> AI 正在思考中</text>
            </view>
            <!-- 正常消息内容 -->
            <text v-else>{{ msg.content }}</text>
          </view>
          <image 
            v-if="msg.recommendations && msg.recommendations.length > 0"
            src="/static/recommend-icon.png" 
            class="recommend-icon"
            @click="showRecommendations(index)"
          ></image>
        </view>
      </view>
      <!-- 底部间距占位 -->
      <view class="bottom-spacer"></view>
    </scroll-view>

    <!-- 自动滚动状态指示器 -->
    <view 
      v-show="!autoScrollToBottom && !isAtBottom" 
      class="scroll-indicator" 
      @click="enableAutoScrollAndGoBottom"
    >
      <view class="scroll-icon">↓</view>
      <text class="scroll-text">回到底部</text>
    </view>

    <!-- TTS 悬浮播放器 -->
    <view v-if="ttsState !== 'idle'" class="tts-player-float">
      <!-- 正在合成中 -->
      <view v-if="ttsState === 'synthesizing'" class="tts-synthesizing">
        <view class="synthesis-loading">
          <view class="loading-dot loading-dot-1"></view>
          <view class="loading-dot loading-dot-2"></view>
          <view class="loading-dot loading-dot-3"></view>
        </view>
        <text class="tts-status-text">正在合成语音...</text>
      </view>

      <!-- 播放控制 -->
      <view v-else class="tts-controls">
        <view class="tts-buttons">
          <!-- 暂停按钮（播放中显示） -->
          <view 
            v-if="ttsState === 'playing'" 
            class="tts-btn tts-pause-btn"
            @click="pauseTts"
          >
            <image src="/static/elder/TTS-pause.png" class="tts-icon-img" mode="aspectFit" />
          </view>
          
          <!-- 继续播放按钮（暂停时显示） -->
          <view 
            v-else-if="ttsState === 'paused'" 
            class="tts-btn tts-continue-btn"
            @click="resumeTts"
          >
            <image src="/static/elder/TTS-continue.png" class="tts-icon-img" mode="aspectFit" />
          </view>
          
          <!-- 结束按钮（始终显示） -->
          <view class="tts-btn tts-stop-btn" @click="stopTts">
            <image src="/static/elder/TTS-stop.png" class="tts-icon-img" mode="aspectFit" />
          </view>
        </view>
      </view>
    </view>

    <!-- 输入区域 -->
    <view class="input-area">
      <!-- 正常状态：输入框和功能按钮 -->
      <view v-if="!isRecording" class="normal-input-mode">
        <!-- 上层：输入框区域 -->
        <view class="input-text-area">
          <textarea
            class="input-textarea"
            placeholder="输入消息或点击麦克风说话..."
            v-model="message"
            :auto-height="true"
            :maxlength="1000"
            :show-confirm-bar="false"
            :adjust-position="true"
            :cursor-spacing="60"
            :hold-keyboard="true"
            @confirm="sendMessage"
            @input="onInputChange"
            @linechange="onTextareaHeightChange"
            @focus="onInputFocus"
            @blur="onInputBlur"
            confirm-type="send"
          />
        </view>
        
        <!-- 下层：功能按钮区域 -->
        <view class="input-controls">
          <!-- 左侧兴趣图标 -->
          <image 
            src="/static/icons/interest.png" 
            class="control-icon interest-icon" 
            @click="showInterestPanel"
            mode="aspectFit"
          />
          
          <!-- 右侧按钮组 -->
          <view class="right-controls">
            <view 
              class="control-icon voice-btn" 
              @click="toggleRecording"
            >
              <image src="/static/mic.png" mode="aspectFit" class="mic-icon"></image>
            </view>
            <button class="send-btn" @click="sendMessage">发送</button>
          </view>
        </view>
      </view>

      <!-- 录音状态：大型停止录音按钮 -->
      <view v-else class="recording-input-mode">
        <view class="recording-container">
          <!-- 录音状态提示 -->
          <view class="recording-hint">
            <text class="hint-text">正在聆听您的语音...</text>
            <view class="voice-wave">
              <view class="wave-dot wave-dot-1"></view>
              <view class="wave-dot wave-dot-2"></view>
              <view class="wave-dot wave-dot-3"></view>
            </view>
          </view>
          
          <!-- 大型停止录音按钮 -->
          <view 
            :class="['stop-recording-btn', { 'btn-pressed': isStopBtnPressed }]"
            @touchstart="onStopBtnPress"
            @touchend="onStopBtnRelease"
            @touchcancel="onStopBtnRelease"
            @click="toggleRecording"
          >
            <view class="stop-icon">
              <view class="stop-rect"></view>
            </view>
            <text class="stop-text">停止录音</text>
          </view>
        </view>
      </view>
    </view>

    <!-- 兴趣选择面板（完全不变） -->
    <uni-popup ref="interestPopup" type="bottom">
      <view class="interest-panel">
        <view class="panel-header">
          <text>我的兴趣标签</text>
          <uni-icons type="close" size="20" @click="closeInterestPanel"></uni-icons>
        </view>
        <view class="interest-tags">
          <view 
            v-for="(tag, index) in interestTags" 
            :key="index"
            :class="['tag', { active: selectedTags.includes(tag) }]"
            @click="toggleTag(tag)"
          >
            {{ tag }}
          </view>
        </view>
        <button class="confirm-btn" @click="updateInterests">确认更新</button>
      </view>
    </uni-popup>

    <!-- 推荐话题面板（完全不变） -->
    <uni-popup ref="recommendPopup" type="bottom">
      <view class="recommend-panel">
        <view class="panel-header">
          <text>推荐话题</text>
          <uni-icons type="close" size="20" @click="closeRecommendPanel"></uni-icons>
        </view>
        <view class="recommend-list">
          <view 
            v-for="(topic, index) in currentRecommendations" 
            :key="index"
            class="recommend-item"
            @click="selectRecommendation(topic)"
          >
            {{ topic }}
          </view>
        </view>
      </view>
    </uni-popup>

    <!-- 新的语音识别组件（隐藏） -->
    <yue-asr-tx 
      v-if="showAsrComponent"
      ref="yueAsrRefs" 
      :options="asrOptions" 
      @countDown="onAsrCountDown" 
      @result="onAsrResult" 
      @onStop="onAsrStop"
      @onOpen="onAsrOpen" 
      @change="onAsrChange"
      style="display: none;"
    ></yue-asr-tx>
  </view>
</template>

<script>
import request from '@/utils/request.js'
import { getCurrentUserId } from '@/utils/auth.js'
import config from '@/utils/config.js'
// 引入ASR相关模块（暂时保留旧模块）
/* #ifdef APP-PLUS */
import Record from '@/utils/asr/record.js';
/* #endif */
import permission from '@/utils/asr/appPermission';
import TengxunAsr from '@/utils/asr/TengxunAsr.js';

// ASR全局变量
let recorderManager = null;
let asr = null;

export default {
  data() {
    return {
      message: '',
      memoryId: '', // 会话ID，用于会话隔离
      statusBarHeight: 0, // 状态栏高度
      scrollIntoView: '', // 用于控制滚动到指定消息  
      chatScrollTop: 0, // 聊天区域滚动位置（瞬间跳转，无动画）
      autoScrollToBottom: true, // 是否自动滚动到底部
      lastScrollTop: 0, // 上次滚动位置
      isAtBottom: true, // 当前是否在底部
      chatHistory: [
        {
          sender: 'ai',
          content: '您好！我是您的智能聊天助手小乐，可以陪您聊聊天、解答问题，也可以代您操作软件，您有什么想聊的吗？\n\n小贴士：双击消息可进行语音播放',
          timestamp: new Date().getTime(),
          recommendations: ['天气情况', '健康养生', '今日新闻']
        }
      ],
      interestTags: ['健康养生', '钓鱼', '戏曲', '书法', '园艺', '烹饪', '旅游', '历史'],
      selectedTags: ['健康养生', '钓鱼'],
      currentRecommendations: [],
      currentRecommendIndex: -1,
      isRecording: false,
      recordingTimer: null,
      scrollHeight: 500, // 新增高度变量
      // 新ASR插件相关数据
      voiceInputContent: '', // 实时语音识别内容
      isVoiceInputMode: false, // 是否正在语音输入模式
      voiceInputMessageIndex: -1, // 语音输入消息在聊天记录中的索引
      currentVoiceSessionId: '', // 当前语音会话ID（用于结果隔离）
      // 新插件配置（请填入你的腾讯云密钥）
      asrOptions: {
        receordingDuration: 60,  // 录音最长时间（秒）
        APPID: 'YOUR_TENCENT_APPID',     // 请替换为你的腾讯云APPID
        API_SECRET: 'YOUR_TENCENT_API_SECRET',  // 请替换为你的腾讯云API_SECRET
        API_KEY: 'YOUR_TENCENT_API_KEY'      // 请替换为你的腾讯云API_KEY
      },
      // 录音状态
      downtime: -1,    // 录音倒计时，-1表示连接中
      downed: false,   // 是否显示录音状态
      disabled: false, // 录音按钮是否禁用
      // 录音UI状态
      isStopBtnPressed: false, // 停止录音按钮是否被按下
      // 组件控制
      showAsrComponent: true,   // 控制ASR组件的显示/重新创建
      
      // TTS 语音合成相关
      ttsState: 'idle',  // TTS状态：idle(空闲), synthesizing(合成中), playing(播放中), paused(暂停)
      currentTtsText: '', // 当前正在播放的文本
      currentTtsAudioUrl: '', // 当前音频URL
      ttsAudioContext: null, // 音频上下文
      lastTapTime: 0, // 上次点击时间（用于双击检测）
      lastTapIndex: -1, // 上次点击的消息索引
      tapHighlightIndex: -1, // 点击高亮的消息索引
      ttsSynthesizeRetryCount: 0, // TTS合成重试次数
      ttsPlayRetryCount: 0, // TTS播放重试次数
      ttsCurrentMessage: null // 当前TTS消息内容（用于重试）
    }
  },
  created() {
    // 将request挂载到this上，方便在方法中使用
    this.$request = request;
  },
  mounted() {
    this.generateMemoryId(); // 初始化会话ID
    this.loadUserInterests();
    this.initializeChatHistory(); // 初始化聊天历史记录
    this.initializeUserKnowledge(); // 初始化用户知识库（新增）
    
    // 初始化ASR和录音功能（旧插件）
    this.initializeASR();
    this.initializeRecorder();
    
    // 申请录音权限（新插件）
    this.requestRecordPermission();
    
    // 获取状态栏高度
    const systemInfo = uni.getSystemInfoSync();
    this.statusBarHeight = systemInfo.statusBarHeight || 0;
    
    // 延迟计算高度，确保页面完全加载
    setTimeout(() => {
      this.calculateScrollHeight();
    }, 100);
    
    // 监听窗口大小变化
    uni.onWindowResize(() => {
      // 延迟执行，避免频繁计算
      setTimeout(() => {
        this.calculateScrollHeight();
      }, 100);
    });
  },
  beforeDestroy() {
    uni.offWindowResize();
    // 清理ASR和录音资源（旧插件）
    if (recorderManager) {
      recorderManager.destroyed();
    }
    if (asr) {
      asr.close();
    }
    // 清理新插件资源
    if (this.recordingTimer) {
      clearTimeout(this.recordingTimer);
    }
    // 清理TTS资源
    if (this.ttsAudioContext) {
      this.ttsAudioContext.stop();
      this.ttsAudioContext.destroy();
      this.ttsAudioContext = null;
    }
    // 清理TTS重试相关
    this.ttsSynthesizeRetryCount = 0;
    this.ttsPlayRetryCount = 0;
    this.ttsCurrentMessage = null;
  },
  methods: {
    // 初始化ASR语音识别
    initializeASR() {
      try {
        // 初始化腾讯云ASR，请填入你的实际密钥
        asr = new TengxunAsr({
          secretKey: 'YOUR_TENCENT_SECRET_KEY',   // 请替换为你的腾讯云Secret Key
          secretId: 'YOUR_TENCENT_SECRET_ID',     // 请替换为你的腾讯云Secret ID
          appId: 'YOUR_TENCENT_APP_ID',           // 请替换为你的腾讯云App ID
          params: this.asrParams
        });

        // 初始化ASR回调
        asr.init({
          onOpen: (res) => {
            console.log('🎤 ASR连接已打开', res);
          },
          onMessage: (res) => {
            console.log('🎤 ASR返回数据：', res);
            
            // 处理腾讯云ASR返回结果
            if (asr.type === 'tengxun') {
              const { voice_text_str = '' } = res.result || {};
              if (voice_text_str) {
                // 实时更新语音识别内容
                this.voiceInputContent = voice_text_str;
                this.updateVoiceInputDisplay();
              }
            }
          },
          onClose: (res) => {
            console.log('🎤 ASR连接已关闭', res);
          },
          onError: (error) => {
            console.error('🎤 ASR连接错误：', error);
            uni.showToast({
              title: '语音识别服务连接失败',
              icon: 'none'
            });
          }
        });
        
        console.log('✅ ASR初始化成功');
      } catch (error) {
        console.error('❌ ASR初始化失败：', error);
      }
    },

    // 初始化录音管理器（完全按照demo的方式）
    initializeRecorder() {
      try {
        /* #ifdef MP-WEIXIN */
        recorderManager = uni.getRecorderManager();
        /* #endif */
        /* #ifdef APP-PLUS */
        recorderManager = new Record();
        /* #endif */
        
        if (!recorderManager) {
          console.error('❌ 录音管理器初始化失败，当前平台可能不支持');
          return;
        }
        
        console.log('📱 录音管理器类型:', typeof recorderManager, recorderManager.constructor.name);
        
        // 确保传递的是函数，使用function声明而不是箭头函数
        function startCallback() {
          console.log('录音开始');
        }
        
        const self = this; // 保存this引用
        
        function stopCallback(res) {
          try {
            console.log('录音停止, res:', res);
            // 结束的数据
            const { btyArray, int8Array, arraybuffer, path } = res || {};
            // 赋值临时路径，保存为wav格式
            if (path) {
              self.speechPath = path;
              console.log('%c Line:157 🍔 语音临时路径为', path);
            }
            
            // ElderCare项目特有：完成语音输入，发送消息（不使用async/await）
            self.isRecording = false;
            self.finishVoiceInput().catch(error => {
              console.error('❌ 处理语音输入完成失败:', error);
            });
          } catch (error) {
            console.error('❌ 处理录音停止事件失败:', error);
            return Promise.reject(error);
          }
        }
        
        function frameCallback(res) {
          console.log('持续录音对象', res);
          // console.log('持续录音中,持续返回每帧的录音数据，以ArrayBuffer格式，基本常规解析语音的都是这个格式');
          // 分贝值
          const { frameBuffer, dBArray } = res || {};
          if (dBArray && dBArray.length) {
            console.log('分贝数组', dBArray);
            self.dBArrayText = dBArray.join('-');
          }
          //我的业务是通过websocket来发送解析语音的，具体看你的业务,发送录音数据的逻辑你根据你的业务去写
          if (asr && frameBuffer) {
            asr.send({
              data: frameBuffer,
              success: e => {},
              fail: err => {},
              complete: res => {}
            });
          }
        }
        
        // 调试：检查函数类型
        console.log('🔍 函数类型检查:');
        console.log('startCallback type:', typeof startCallback, Object.prototype.toString.call(startCallback));
        console.log('stopCallback type:', typeof stopCallback, Object.prototype.toString.call(stopCallback));
        console.log('frameCallback type:', typeof frameCallback, Object.prototype.toString.call(frameCallback));
        
        // 绑定事件监听器
        console.log('🔗 开始绑定onStart...');
        recorderManager.onStart(startCallback);
        console.log('✅ onStart绑定成功');
        
        console.log('🔗 开始绑定onStop...');
        recorderManager.onStop(stopCallback);
        console.log('✅ onStop绑定成功');
        
        console.log('🔗 开始绑定onFrameRecorded...');
        recorderManager.onFrameRecorded(frameCallback);
        console.log('✅ onFrameRecorded绑定成功');
        
        console.log('✅ 录音管理器初始化成功');
      } catch (error) {
        console.error('❌ 录音管理器初始化失败:', error);
        uni.showToast({
          title: '录音功能初始化失败',
          icon: 'none'
        });
      }
    },

    // 更新语音输入实时显示
    updateVoiceInputDisplay() {
      if (this.isVoiceInputMode && this.voiceInputMessageIndex >= 0) {
        // 更新聊天记录中的语音输入消息
        this.chatHistory[this.voiceInputMessageIndex].content = this.voiceInputContent + '...';
        
        // 自动滚动到底部
        if (this.autoScrollToBottom) {
          this.scrollToBottom();
        }
      }
    },

    // 完成语音输入并发送消息
    async finishVoiceInput() {
      if (!this.isVoiceInputMode || this.voiceInputMessageIndex < 0) {
        return;
      }

      // 获取最终的语音识别内容
      const finalContent = this.voiceInputContent.trim();
      
      if (!finalContent) {
        // 如果没有识别到内容，删除语音输入消息
        this.chatHistory.splice(this.voiceInputMessageIndex, 1);
        uni.showToast({
          title: '未识别到语音内容',
          icon: 'none'
        });
        // 重置语音输入状态
        this.resetVoiceInputState();
        return;
      }

      // 更新最终的消息内容（去掉'...'）
      this.chatHistory[this.voiceInputMessageIndex].content = finalContent;
      
      // 直接处理AI回复，不调用sendMessage()避免重复消息
      await this.sendVoiceMessageToAI(finalContent);

      // 重置语音输入状态
      this.resetVoiceInputState();
    },

    // 发送语音消息给AI（避免重复用户消息）
    async sendVoiceMessageToAI(messageContent) {
      try {
        // 启用自动滚动并跳到底部
        this.autoScrollToBottom = true;
        this.scrollToBottom();
        
        // 创建AI消息占位符，用于流式更新
        const aiMsgIndex = this.chatHistory.length;
        const aiMsg = {
          sender: 'ai',
          content: '正在思考中...',
          timestamp: new Date().getTime(),
          recommendations: []
        };
        this.chatHistory.push(aiMsg);
        
        // 使用WebSocket流式聊天
        console.log('🚀 开始使用WebSocket流式聊天');
        await this.$request.chatApi.sendWebSocketMessage(
          messageContent,
          // onChunk - 处理流式数据块
          (accumulatedContent) => {
            this.chatHistory[aiMsgIndex].content = accumulatedContent;
            
            if (this.autoScrollToBottom) {
              this.scrollToBottom();
            }
          },
          // onComplete - 流式完成
          (complete) => {
            this.chatHistory[aiMsgIndex].content = complete;
            this.chatHistory[aiMsgIndex].recommendations = this.generateRecommendations(complete);
            
            if (this.autoScrollToBottom) {
              this.scrollToBottom();
            }
          },
          // onError - WebSocket错误，保持思考状态
          async (error) => {
            console.log('🔄 语音WebSocket连接中断，保持思考状态...');
            // 不做任何处理，保持思考动画
          },
          this.memoryId // 传递memoryId
        );
        
      } catch (error) {
        console.log('🔄 语音消息处理异常，保持思考状态...', error);
        // 不做任何处理，保持思考动画
      }
    },

    // 重置语音输入状态
    resetVoiceInputState() {
      this.voiceInputContent = '';
      this.isVoiceInputMode = false;
      this.voiceInputMessageIndex = -1;
    },

    // 检查录音权限
    async checkRecordPermission() {
      try {
        /* #ifdef APP-PLUS */
        const appPermission = await permission.authForApp('麦克风权限');
        if (!appPermission) {
          uni.showToast({
            title: '需要麦克风权限才能使用语音功能',
            icon: 'none'
          });
          return false;
        }
        /* #endif */
        
        /* #ifdef MP-WEIXIN */
        const wechatPermission = await this.checkWeChatRecordAuth();
        if (!wechatPermission) {
          return false;
        }
        /* #endif */
        
        return true;
      } catch (error) {
        console.error('❌ 检查录音权限失败：', error);
        return false;
      }
    },

    // 检查微信小程序录音权限
    checkWeChatRecordAuth() {
      return new Promise((resolve) => {
        uni.getSetting({
          success: (res) => {
            if (!res.authSetting['scope.record']) {
              uni.authorize({
                scope: 'scope.record',
                success: () => resolve(true),
                fail: () => {
                  uni.showModal({
                    title: '权限申请',
                    content: '需要录音权限才能使用语音功能，请前往设置开启',
                    success: (modalRes) => {
                      if (modalRes.confirm) {
                        uni.openSetting();
                      }
                      resolve(false);
                    }
                  });
                }
              });
            } else {
              resolve(true);
            }
          },
          fail: () => resolve(false)
        });
      });
    },

    // 返回到首页
    goBack() {
      uni.navigateTo({
        url: '/pages/user/index/index'
      });
    },
    // 生成会话ID（用于单次会话）
    generateMemoryId() {
      const userId = getCurrentUserId();
      if (!userId) {
        uni.showToast({
          title: '请先登录',
          icon: 'none'
        });
        setTimeout(() => {
          uni.navigateTo({
            url: '/pages/login/login'
          });
        }, 1500);
        return;
      }
      this.memoryId = 'chat_' + userId + '_' + Date.now() + '_' + Math.random().toString(36).substr(2, 9);
      console.log('🔗 生成新的会话ID:', this.memoryId);
    },

    // 生成用户固定的会话ID（用于持久化记录）
    generateUserMemoryId() {
      const userId = getCurrentUserId();
      if (!userId) {
        return null;
      }
      return 'chat_' + userId + '_persistent';
    },

    // 初始化用户知识库
    async initializeUserKnowledge() {
      try {
        const userId = getCurrentUserId();
        if (!userId) {
          console.warn('⚠️ 用户未登录，跳过知识库初始化');
          return;
        }
        
        console.log('🔄 触发用户知识库初始化: user_' + userId);
        
        // 调用后端初始化接口
        const result = await this.$request.post('/api/chat/initialize', { userId });
        
        if (result.success) {
          console.log('✅ 用户知识库初始化成功:', result.message);
        } else {
          console.warn('⚠️ 用户知识库初始化失败:', result.message);
        }
      } catch (error) {
        console.error('❌ 初始化用户知识库异常:', error);
        // 不影响聊天功能，仅记录错误
      }
    },
    
    // 初始化聊天历史记录
    async initializeChatHistory() {
      try {
        const userId = getCurrentUserId();
        if (!userId) {
          console.warn('⚠️ 用户未登录，无法加载历史记录');
          this.initDefaultWelcome();
          return;
        }

        // 使用固定的用户会话ID来获取持久化记录
        const userMemoryId = this.generateUserMemoryId();
        console.log('🔗 用户固定会话ID:', userMemoryId);
        if (!userMemoryId) {
          this.initDefaultWelcome();
          return;
        }

        // 更新当前会话ID为用户固定的ID
        this.memoryId = userMemoryId;
        console.log('🔗 使用用户固定会话ID:', this.memoryId);

        console.log('📡 正在获取历史记录...');
        
        // 获取历史记录
        const result = await this.$request.chatApi.getChatHistory(this.memoryId);
        console.log('📡 后端返回结果:', result);
        
        if (result.success) {
          if (result.messages && result.messages.length > 0) {
            console.log('📚 服务器返回历史记录:', result.count, '条消息');
            console.log('📋 原始消息数据:', result.messages);
            
            // 将后端返回的消息转换为前端格式
            const convertedHistory = this.convertLangChainToFrontend(result.messages);
            
            if (convertedHistory.length > 0) {
              this.chatHistory = convertedHistory;
              console.log('✅ 历史记录加载成功，显示', convertedHistory.length, '条消息');
              // 加载历史记录后瞬间跳到底部
              setTimeout(() => {
                this.scrollToBottom();
              }, 100);
            } else {
              console.log('📝 转换后没有可显示的消息，使用默认欢迎消息');
              this.initDefaultWelcome();
            }
          } else {
            console.log('📝 服务器返回空的历史记录，使用默认欢迎消息');
            this.initDefaultWelcome();
          }
        } else {
          console.log('❌ 服务器返回失败:', result.message);
          this.initDefaultWelcome();
        }
        
      } catch (error) {
        console.error('❌ 加载历史记录失败:', error);
        this.initDefaultWelcome();
      }
    },

    // 初始化默认欢迎消息
    initDefaultWelcome() {
      this.chatHistory = [
        {
          sender: 'ai',
          content: '您好！我是您的智能聊天助手小乐，可以陪您聊聊天、解答问题，也可以代您操作软件，您有什么想聊的吗？\n\n小贴士：双击消息可进行语音播放~',
          timestamp: new Date().getTime(),
          recommendations: ['天气情况', '健康养生', '今日新闻']
        }
      ];
      // 初始化后也瞬间跳到底部
      setTimeout(() => {
        this.scrollToBottom();
      }, 100);
    },

    // 将后端返回的消息转换为前端格式
    convertLangChainToFrontend(messages) {
      const converted = [];
      
      try {
        console.log('🔄 开始转换历史消息格式，消息数量:', messages.length);
        
        messages.forEach((msg, index) => {
          // 处理消息内容
          const rawContent = msg.content || msg.text || '';
          const processedContent = this.processMessageContent(rawContent);
          console.log('📋 处理消息:', index, msg.type, processedContent?.substring(0, 50) + '...');
          
          // 跳过系统消息
          if (msg.type === 'SYSTEM') {
            return;
          }
          
          if (msg.type === 'USER') {
            converted.push({
              sender: 'user',
              content: processedContent,
              timestamp: new Date().getTime() - (messages.length - index) * 1000 // 简单的时间戳估算
            });
          } else if (msg.type === 'AI') {
            converted.push({
              sender: 'ai',
              content: processedContent,
              timestamp: new Date().getTime() - (messages.length - index - 1) * 1000,
              recommendations: this.generateRecommendations(processedContent)
            });
          }
        });
        
        console.log('✅ 消息转换完成，转换后数量:', converted.length);
        
      } catch (error) {
        console.error('❌ 转换历史记录格式失败:', error);
        return [];
      }
      
      return converted;
    },

    // 处理输入框内容变化
    onInputChange(e) {
      let value = e.detail.value;
      
      // // 限制行数为6行，每行大约30-40个字符（根据字体大小）
      // const lines = value.split('\n');
      // if (lines.length > 6) {
      //   // 如果超过6行，只保留最后6行
      //   const lastSixLines = lines.slice(-6);
      //   value = lastSixLines.join('\n');
      // }
      
      this.message = value;
    },

    // 输入框获得焦点时的处理
    onInputFocus() {
      this.autoScrollToBottom = true;
      setTimeout(() => {
        this.scrollToBottom();
      }, 100);
    },

    // 输入框失去焦点时的处理
    onInputBlur() {
      setTimeout(() => {
        this.calculateScrollHeight();
      }, 300);
    },

    // 瞬间跳转到聊天区域底部（无滚动动画）
    scrollToBottom() {
      this.$nextTick(() => {
        this.chatScrollTop = Date.now();
        setTimeout(() => {
          this.chatScrollTop = 999999999;
        }, 10);
      });
    },

    // 监听聊天区域滚动事件
    onChatScroll(e) {
      const { scrollTop, scrollHeight } = e.detail;
      
      // 获取scroll-view的可视高度
      const query = uni.createSelectorQuery().in(this);
      query.select('.chat-area').boundingClientRect((data) => {
        if (!data) return;
        
        const viewHeight = data.height;
        
        // 判断滚动方向
        const isScrollingUp = scrollTop < this.lastScrollTop;
        
        // 计算距离底部的距离
        const distanceFromBottom = scrollHeight - viewHeight - scrollTop + 10;
        
        // 判断是否在底部（容忍60px的误差）
        const isNearBottom = distanceFromBottom <= 60;
        

        
        // 如果用户向上滚动且不在底部，停止自动滚动
        if (isScrollingUp && !isNearBottom) {
          if (this.autoScrollToBottom) {
            this.autoScrollToBottom = false;
          }
        }
        
        // 如果用户滚动到底部附近，恢复自动滚动
        if (isNearBottom) {
          if (!this.autoScrollToBottom) {
            this.autoScrollToBottom = true;
          }
        }

        this.lastScrollTop = scrollTop;
        this.isAtBottom = isNearBottom;
      }).exec();
    },

    // 启用自动滚动并跳转到底部
    enableAutoScrollAndGoBottom() {
      this.autoScrollToBottom = true;
      this.scrollToBottom();
    },
    
    // 开始新的聊天会话
    startNewChat() {
      uni.showModal({
        title: '新建聊天',
        content: '确定要开始新的聊天会话吗？当前聊天记录将被清空。',
        success: async (res) => {
          if (res.confirm) {
            try {
              // 先清理当前用户的会话记录
              const userMemoryId = this.generateUserMemoryId();
              if (userMemoryId) {
                console.log('🗑️ 清理旧的会话记录:', userMemoryId);
                await this.$request.chatApi.clearChatMemory(userMemoryId);
                console.log('✅ 旧会话记录清理成功');
              }
              
              // 重新生成用户固定的会话ID
              this.memoryId = userMemoryId;
              
              // 新建聊天时重置自动滚动状态
              this.autoScrollToBottom = true;
              
              // 重置聊天历史（initDefaultWelcome会自动跳到底部）
              this.initDefaultWelcome();
              
              uni.showToast({
                title: '已开始新聊天',
                icon: 'success'
              });
              
            } catch (error) {
              console.error('❌ 清理会话记录失败:', error);
              // 即使清理失败，也要开始新聊天
              this.initDefaultWelcome();
              uni.showToast({
                title: '已开始新聊天',
                icon: 'success'
              });
            }
          }
        }
      });
    },
    
    // 新增方法：计算可滚动区域高度
    calculateScrollHeight() {
      // 延迟执行，确保DOM元素已经渲染完成
      this.$nextTick(() => {
        const query = uni.createSelectorQuery().in(this);
        query.select('.custom-header').boundingClientRect();
        query.select('.input-area').boundingClientRect();
        query.exec(([header, input]) => {
          // 添加错误检查，确保获取到了正确的元素信息
          if (header && input && header.height && input.height) {
            const systemInfo = uni.getSystemInfoSync();
            // 总高度 = 窗口高度 - 状态栏高度 - 自定义导航栏高度 - 输入区域高度
            this.scrollHeight = systemInfo.windowHeight - this.statusBarHeight - header.height - input.height;
            console.log('📐 高度计算:', {
              windowHeight: systemInfo.windowHeight,
              statusBarHeight: this.statusBarHeight,
              headerHeight: header.height,
              inputAreaHeight: input.height,
              scrollHeight: this.scrollHeight
            });
          } else {
            // 如果无法获取元素高度，使用默认值
            console.warn('⚠️ 无法获取元素高度，使用默认值');
            this.scrollHeight = 500;
          }
        });
      });
    },

    // 当输入框高度变化时重新计算聊天区域高度
    onTextareaHeightChange() {
      // 延迟执行，确保DOM更新完成
      setTimeout(() => {
        this.calculateScrollHeight();
      }, 100);
    },
    

    
    async sendMessage() {
      if (!this.message.trim()) {
        uni.showToast({ title: '消息不能为空', icon: 'none' });
        return;
      }

      const userMsg = {
        sender: 'user',
        content: this.message,
        timestamp: new Date().getTime()
      };
      this.chatHistory.push(userMsg);
      
      const tempMsg = this.message;
      this.message = '';
      
      // 用户发送消息时，强制启用自动滚动并跳到底部
      this.autoScrollToBottom = true;
      this.scrollToBottom();
      
      // 创建AI消息占位符，用于流式更新
      const aiMsgIndex = this.chatHistory.length;
      const aiMsg = {
        sender: 'ai',
        content: '正在思考中...',
        timestamp: new Date().getTime(),
        recommendations: []
      };
      this.chatHistory.push(aiMsg);
      
      try {
        // 使用WebSocket流式聊天（完美支持所有平台包括Android）
        console.log('🚀 开始使用WebSocket流式聊天');
        await this.$request.chatApi.sendWebSocketMessage(
          tempMsg,
          // onChunk - 处理流式数据块（已经是累积的完整内容）
          (accumulatedContent) => {
            // 直接使用累积的内容，不需要再次累积
            this.chatHistory[aiMsgIndex].content = accumulatedContent;

            
            // 只有在允许自动滚动时才滚动到底部
            if (this.autoScrollToBottom) {
              this.scrollToBottom();
            }
          },
          // onComplete - 流式完成
          (complete) => {
            this.chatHistory[aiMsgIndex].content = complete;
            this.chatHistory[aiMsgIndex].recommendations = this.generateRecommendations(complete);
            
            // 只有在允许自动滚动时才滚动到底部
            if (this.autoScrollToBottom) {
              this.scrollToBottom();
            }
          },
          // onError - WebSocket错误，保持思考状态
          async (error) => {
            console.log('🔄 WebSocket连接中断，保持思考状态...');
            // 不做任何处理，保持思考动画
          },
          this.memoryId // 传递memoryId
        );
        
      } catch (error) {
        console.log('🔄 聊天处理异常，保持思考状态...', error);
        // 不做任何处理，保持思考动画
      }
    },
    
    // 开始语音录音（真实ASR实现）
    async startRecording() {
      try {
        // 检查录音权限
        const hasPermission = await this.checkRecordPermission();
        if (!hasPermission) {
          return;
        }

        // 如果正在录音，则停止录音
        if (this.isRecording) {
          await this.stopRecording();
          return;
        }

        // 开始语音输入模式
        this.isVoiceInputMode = true;
        this.voiceInputContent = '';
        
        // 在聊天记录中添加语音输入占位消息
        const voiceInputMessage = {
          sender: 'user',
          content: '正在聆听中...',
          timestamp: new Date().getTime()
        };
        this.chatHistory.push(voiceInputMessage);
        this.voiceInputMessageIndex = this.chatHistory.length - 1;

        // 启用自动滚动并跳到底部
        this.autoScrollToBottom = true;
        this.scrollToBottom();

        // 连接ASR服务
        if (asr) {
          asr.changeParams(this.asrParams);
          await asr.open();
          
          // 延迟一点时间等待ASR初始化
          await new Promise(resolve => setTimeout(resolve, 200));
        }

        // 开始录音（按照demo的配置）
        const recordConfig = {
          frameSize: 2,                    // 帧大小(KB)
          duration: 30 * 60 * 1000,       // 最长录音时间(30分钟)
          sampleRate: 16000,              // 采样率(16kHz，兼容所有ASR服务)
          format: 'PCM',                  // 音频格式
          createPath: true                // 生成录音临时地址，保存为wav格式
        };

        recorderManager.start(recordConfig);
        this.isRecording = true;         // 立即设置录音状态
        
        uni.showToast({
          title: '请开始说话...',
          icon: 'none',
          duration: 2000
        });

        console.log('🎤 开始语音录音');
        
      } catch (error) {
        console.error('❌ 开始录音失败：', error);
        uni.showToast({
          title: '录音启动失败，请重试',
          icon: 'none'
        });
        this.resetVoiceInputState();
      }
    },
    
    // 停止语音录音
    async stopRecording() {
      try {
        if (!this.isRecording) {
          return;
        }

        console.log('🎤 停止语音录音');
        
        // 停止录音
        recorderManager.stop();
        
        // 关闭ASR连接
        if (asr) {
          asr.close();
        }

        uni.showToast({
          title: '语音识别完成',
          icon: 'success',
          duration: 1000
        });
        
      } catch (error) {
        console.error('❌ 停止录音失败：', error);
        uni.showToast({
          title: '录音停止失败',
          icon: 'none'
        });
        
        // 出错时重置状态
        this.resetVoiceInputState();
      }
    },

    // 切换录音状态（点击麦克风图标）
    async toggleRecording() {
      // 使用新插件进行语音识别
      if (this.isRecording) {
        // 如果正在录音，则停止录音
        this.stopNewAsr();
      } else {
        // 如果没有录音，则开始录音
        this.startNewAsr();
      }
    },

    // 停止录音按钮按下
    onStopBtnPress() {
      this.isStopBtnPressed = true;
    },

    // 停止录音按钮释放
    onStopBtnRelease() {
      this.isStopBtnPressed = false;
    },

    // 播放录音音频（按照demo的方式）
    palySpeech() {
      let innerAudioContext = uni.createInnerAudioContext();
      innerAudioContext.autoplay = true;
      // innerAudioContext.src = 'https://web-ext-storage.dcloud.net.cn/uni-app/ForElise.mp3';
      innerAudioContext.src = this.speechPath;
      innerAudioContext.onPlay(() => {
        console.log('开始播放');
      });
      innerAudioContext.onError(res => {
        console.log(res.errMsg);
        console.log(res.errCode);
      });
      innerAudioContext.onStop(res => {
        innerAudioContext.destroy();
        innerAudioContext = null;
      });
      // innerAudioContext.play()
    },

    // 清除内容（按照demo的方式）
    clearContent() {
      this.voiceInputContent = '';
      this.dBArrayText = '';
    },

    // === 新插件相关方法 ===
    
    // 申请录音权限（新插件）
    requestRecordPermission() {
      // #ifdef APP
      plus.android.requestPermissions(["android.permission.RECORD_AUDIO"], (e) => {
        console.log('录音权限申请成功');
      }, (e) => {
        console.log('录音权限申请失败', e);
        uni.showToast({
          title: '需要录音权限才能使用语音功能',
          icon: 'none'
        });
      });
      // #endif
    },

    // 重置新插件UI状态
    resumeNewAsrUI() {
      this.downed = false;
      this.downtime = -1;  // 设置为-1停止倒计时
      this.disabled = false;
      this.isRecording = false;
      // 注意：不要重新设置downtime，保持-1状态停止倒计时
    },

    // 生成唯一的Voice ID（增强用户隔离）
    generateUniqueVoiceId() {
      const userId = getCurrentUserId() || 'anonymous';
      const timestamp = Date.now(); // 毫秒级时间戳
      const random = Math.random().toString(36).substr(2, 9); // 随机字符串
      const voiceId = `${userId}_${timestamp}_${random}`;
      console.log('🔑 生成唯一Voice ID:', voiceId);
      return voiceId;
    },

    // 新插件开始录音
    startNewAsr() {
      if (this.disabled || this.isRecording) {
        return;
      }
      console.log("开始新插件录音");
      
      // 开始语音输入模式
      this.isVoiceInputMode = true;
      this.voiceInputContent = '';
      
      // 在聊天记录中添加语音输入占位消息
      const voiceInputMessage = {
        sender: 'user',
        content: '正在聆听中...',
        timestamp: new Date().getTime()
      };
      this.chatHistory.push(voiceInputMessage);
      this.voiceInputMessageIndex = this.chatHistory.length - 1;

      // 启用自动滚动并跳到底部
      this.autoScrollToBottom = true;
      this.scrollToBottom();

      this.downed = true;
      this.isRecording = true;
      this.downtime = this.asrOptions.receordingDuration; // 重置倒计时为录音总时长
      
      // 生成应用层的语音会话ID用于结果隔离
      this.currentVoiceSessionId = this.generateUniqueVoiceId();
      
      // ⚠️ 注意：由于插件限制，我们无法直接设置voice_id
      // 插件内部仍使用时间戳生成voice_id，存在并发冲突风险
      console.log('⚠️ 警告：当前插件使用时间戳生成voice_id，多用户同时录音可能冲突');
      console.log('🔒 应用层会话ID:', this.currentVoiceSessionId);
      
      this.$refs.yueAsrRefs.start();
      this.disabled = true;
      
      uni.showToast({
        title: '请开始说话...',
        icon: 'none',
        duration: 2000
      });
    },

    // 新插件停止录音
    stopNewAsr() {
      if (!this.isRecording) {
        return;
      }
      console.log("停止新插件录音");
      this.$refs.yueAsrRefs.end();
    },

    // === 新插件回调方法 ===
    
    // 倒计时回调
    onAsrCountDown(seconds) {
      console.log('ASR倒计时', seconds);
      // 只有在录音状态时才更新倒计时
      if (this.isRecording && this.downed) {
        this.downtime = seconds;
      } else {
        console.log('录音已停止，忽略倒计时更新');
      }
    },

    // 识别结果回调
    onAsrResult(result) {
      console.log('ASR识别结果', result);
      
      // 验证会话归属（防止多用户结果混乱）
      if (!this.isRecording || !this.currentVoiceSessionId) {
        console.log('🚫 忽略无效的语音识别结果：当前无活跃会话');
        return;
      }
      
      console.log('✅ 会话验证通过，处理识别结果:', this.currentVoiceSessionId);
      this.voiceInputContent = result;
      
      // 实时更新语音输入显示
      if (this.isVoiceInputMode && this.voiceInputMessageIndex >= 0) {
        this.chatHistory[this.voiceInputMessageIndex].content = result + '...';
        
        // 自动滚动到底部
        if (this.autoScrollToBottom) {
          this.scrollToBottom();
        }
      }
    },

    // 录音停止回调
    onAsrStop(result) {
      console.log('ASR录音停止', result);
      
      // 立即停止倒计时和录音状态
      this.isRecording = false;
      this.downed = false;
      this.downtime = -1;
      this.disabled = false;
      
      console.log('状态已重置：isRecording=', this.isRecording, 'downed=', this.downed, 'downtime=', this.downtime);
      console.log('🔒 清理会话ID:', this.currentVoiceSessionId);
      
      // 清理会话ID（防止后续误处理其他用户的结果）
      const sessionId = this.currentVoiceSessionId;
      this.currentVoiceSessionId = '';
      
      // 强制重新创建组件来清理所有内部状态（解决插件倒计时bug）
      this.showAsrComponent = false;
      this.$nextTick(() => {
        this.showAsrComponent = true;
        console.log('ASR组件已重新创建，内部定时器已清理');
      });
      
      // 完成语音输入
      this.finishNewAsrVoiceInput();
    },

    // 录音开始回调
    onAsrOpen(event) {
      console.log('ASR录音开始', event);
    },

    // 状态变化回调
    onAsrChange(event) {
      console.log('ASR状态变化', event);
    },

    // 完成新插件语音输入
    async finishNewAsrVoiceInput() {
      if (!this.isVoiceInputMode || this.voiceInputMessageIndex < 0) {
        return;
      }

      // 获取最终的语音识别内容
      const finalContent = this.voiceInputContent.trim();
      
      if (!finalContent) {
        // 如果没有识别到内容，删除语音输入消息
        this.chatHistory.splice(this.voiceInputMessageIndex, 1);
        uni.showToast({
          title: '未识别到语音内容',
          icon: 'none'
        });
        // 重置语音输入状态
        this.resetVoiceInputState();
        return;
      }

      // 更新最终的消息内容（去掉'...'）
      this.chatHistory[this.voiceInputMessageIndex].content = finalContent;
      
      // 直接处理AI回复
      await this.sendVoiceMessageToAI(finalContent);

      // 重置语音输入状态
      this.resetVoiceInputState();
    },
    
    loadUserInterests() {
      // 模拟加载用户兴趣
      setTimeout(() => {
        this.selectedTags = ['健康养生', '钓鱼'];
      }, 500);
    },
    
    showInterestPanel() {
      this.$refs.interestPopup.open();
    },
    
    closeInterestPanel() {
      this.$refs.interestPopup.close();
    },
    
    toggleTag(tag) {
      if (this.selectedTags.includes(tag)) {
        this.selectedTags = this.selectedTags.filter(t => t !== tag);
      } else {
        this.selectedTags.push(tag);
      }
    },
    
    updateInterests() {
      uni.showToast({ title: '兴趣更新成功', icon: 'success' });
      this.$refs.interestPopup.close();
    },
    
    showRecommendations(index) {
      this.currentRecommendIndex = index;
      this.currentRecommendations = this.chatHistory[index].recommendations || [];
      this.$refs.recommendPopup.open();
    },
    
    closeRecommendPanel() {
      this.$refs.recommendPopup.close();
    },
    
    selectRecommendation(topic) {
      this.message = topic;
      this.$refs.recommendPopup.close();
      this.sendMessage();
    },
    
    formatTime(timestamp) {
      const date = new Date(timestamp);
      return `${date.getHours()}:${date.getMinutes().toString().padStart(2, '0')}`;
    },

    // ========== TTS 语音合成功能 ==========
    
    // 处理双击事件（用于触发TTS）
    handleDoubleTap(msg, index) {
      const now = Date.now();
      const timeDiff = now - this.lastTapTime;
      
      console.log('🖱️ [双击检测] 点击消息:', {
        index,
        timeDiff: timeDiff + 'ms',
        lastTapIndex: this.lastTapIndex,
        sender: msg.sender,
        contentPreview: msg.content.substring(0, 30)
      });
      
      // 检测是否为双击（400ms内连续点击同一消息）
      if (timeDiff < 400 && this.lastTapIndex === index) {
        console.log('✅ [双击检测] 检测到双击，触发TTS');
        console.log('📝 [双击检测] 消息详情:', {
          sender: msg.sender,
          contentLength: msg.content.length,
          timestamp: msg.timestamp
        });
        
        // 触发TTS
        this.triggerTts(msg);
        
        // 重置双击检测
        this.lastTapTime = 0;
        this.lastTapIndex = -1;
        
        // 震动反馈
        uni.vibrateShort({
          success: () => {
            console.log('📳 [双击检测] 震动反馈成功');
          },
          fail: (e) => {
            console.log('⚠️ [双击检测] 震动反馈失败:', e);
          }
        });
      } else {
        console.log('📝 [双击检测] 单击记录，等待第二次点击');
        console.log('⏱️ [双击检测] 上次点击时间:', this.lastTapTime, '当前时间:', now);
        
        // 记录单击
        this.lastTapTime = now;
        this.lastTapIndex = index;
        
        // 添加点击高亮效果
        this.tapHighlightIndex = index;
        setTimeout(() => {
          this.tapHighlightIndex = -1;
        }, 400);
      }
    },

    // 触发TTS语音合成
    async triggerTts(msg, isRetry = false) {
      console.log('🎵 [TTS] 触发TTS合成', {
        isRetry,
        contentLength: msg.content.length,
        contentPreview: msg.content.substring(0, 50)
      });
      
      // 过滤掉思考中的消息
      if (msg.content.includes('正在思考') || msg.content === '正在聆听中...') {
        console.log('⚠️ [TTS] 过滤掉思考中的消息');
        uni.showToast({ title: '该消息无法播放', icon: 'none' });
        return;
      }

      // 保存当前消息（用于重试）
      if (!isRetry) {
        this.ttsCurrentMessage = msg;
        this.ttsSynthesizeRetryCount = 0;
        this.ttsPlayRetryCount = 0;
        console.log('💾 [TTS] 保存当前消息用于重试');
      }

      // 如果正在合成或播放，先停止
      if (this.ttsState !== 'idle' && !isRetry) {
        console.log('⏸️ [TTS] 停止当前TTS，状态:', this.ttsState);
        this.stopTts();
        await new Promise(resolve => setTimeout(resolve, 200));
      }

      // 开始合成
      this.ttsState = 'synthesizing';
      this.currentTtsText = this.truncateText(msg.content, 30);
      console.log('🔄 [TTS] 开始合成，状态设置为 synthesizing');

      try {
        const requestUrl = `${config.API_BASE_URL}/api/tts/synthesize`;
        console.log('📡 [TTS] 发送合成请求:', {
          url: requestUrl,
          textLength: msg.content.length,
          voice: 'Cherry',
          languageType: 'Chinese'
        });
        
        const response = await uni.request({
          url: requestUrl,
          method: 'POST',
          data: {
            text: msg.content,
            voice: 'Cherry',
            languageType: 'Chinese'
          },
          header: {
            'Content-Type': 'application/json'
          },
          timeout: 30000 // 30秒超时
        });

        console.log('📥 [TTS] 收到响应:', {
          statusCode: response.statusCode,
          dataKeys: Object.keys(response.data || {}),
          success: response.data?.success,
          audioUrl: response.data?.audioUrl,
          message: response.data?.message
        });

        // 检查HTTP状态码
        if (response.statusCode !== 200) {
          throw new Error(`HTTP ${response.statusCode}: 请求失败`);
        }

        // 检查响应数据
        if (!response.data) {
          throw new Error('响应数据为空');
        }

        if (response.data.success) {
          console.log('✅ [TTS] 合成成功');
          
          // 处理后端返回的URL（可能是相对路径或完整URL）
          let audioUrl = response.data.audioUrl;
          console.log('🔗 [TTS] 原始音频URL:', audioUrl);
          
          if (!audioUrl) {
            throw new Error('音频URL为空');
          }
          
          // 如果是相对路径（以 /api 开头），拼接完整URL
          if (audioUrl.startsWith('/api')) {
            audioUrl = config.API_BASE_URL + audioUrl;
            console.log('🔗 [TTS] 拼接后的完整URL:', audioUrl);
          }
          
          this.currentTtsAudioUrl = audioUrl;
          this.ttsPlayRetryCount = 0;
          
          console.log('▶️ [TTS] 准备播放音频');
          // 自动播放
          this.playTts();
        } else {
          const errorMsg = response.data.message || 'TTS合成失败';
          console.error('❌ [TTS] 合成失败:', errorMsg);
          throw new Error(errorMsg);
        }

      } catch (error) {
        console.error('❌ [TTS] 合成异常:', {
          error: error,
          message: error.message,
          errMsg: error.errMsg,
          retryCount: this.ttsSynthesizeRetryCount
        });
        
        // 尝试重试合成
        if (this.ttsSynthesizeRetryCount < 3) {
          this.ttsSynthesizeRetryCount++;
          console.log(`🔄 [TTS] 准备第 ${this.ttsSynthesizeRetryCount} 次重试`);
          setTimeout(() => {
            this.triggerTts(msg, true);
          }, 1000);
        } else {
          console.error('❌ [TTS] 重试次数已达上限，放弃');
          uni.showToast({ 
            title: '语音合成失败: ' + (error.message || '未知错误'),
            icon: 'none',
            duration: 3000
          });
          this.ttsState = 'idle';
          this.ttsSynthesizeRetryCount = 0;
          this.ttsPlayRetryCount = 0;
        }
      }
    },

    // 播放TTS音频
    playTts() {
      console.log('▶️ [TTS] playTts 开始执行');
      
      if (!this.currentTtsAudioUrl) {
        console.error('❌ [TTS] 没有音频URL，无法播放');
        this.ttsState = 'idle';
        return;
      }

      console.log('🔗 [TTS] 当前音频URL:', this.currentTtsAudioUrl);
      console.log('📊 [TTS] 当前状态:', this.ttsState);

      // 如果已有音频上下文且是暂停状态，继续播放
      if (this.ttsAudioContext && this.ttsState === 'paused') {
        console.log('▶️ [TTS] 从暂停状态继续播放');
        this.ttsAudioContext.play();
        this.ttsState = 'playing';
        return;
      }

      // 清理旧的音频上下文（如果存在）
      if (this.ttsAudioContext) {
        console.log('🗑️ [TTS] 清理旧的音频上下文');
        this.ttsAudioContext.destroy();
        this.ttsAudioContext = null;
      }

      // 创建新的音频上下文
      console.log('🎧 [TTS] 创建新的音频上下文');
      this.ttsAudioContext = uni.createInnerAudioContext();
      
      if (!this.ttsAudioContext) {
        console.error('❌ [TTS] 创建音频上下文失败');
        this.ttsState = 'idle';
        uni.showToast({ title: '音频播放器创建失败', icon: 'none' });
        return;
      }
      
      // 监听等待数据加载事件
      this.ttsAudioContext.onWaiting(() => {
        console.log('⏳ [TTS] onWaiting - 音频正在加载数据');
      });
      
      // 监听音频准备好事件
      this.ttsAudioContext.onCanplay(() => {
        console.log('✅ [TTS] onCanplay - 音频可以播放了');
        if (this.ttsState === 'synthesizing') {
          this.ttsState = 'playing';
          console.log('🔄 [TTS] 状态从 synthesizing 变为 playing');
        }
      });
      
      // 监听播放开始事件
      this.ttsAudioContext.onPlay(() => {
        console.log('▶️ [TTS] onPlay - 音频开始播放');
        if (this.ttsState !== 'playing') {
          this.ttsState = 'playing';
          console.log('🔄 [TTS] 状态变为 playing');
        }
        
        // 播放成功，重置重试计数
        if (this.ttsPlayRetryCount > 0 || this.ttsSynthesizeRetryCount > 0) {
          console.log('✅ [TTS] 播放成功，重置重试计数');
          this.ttsPlayRetryCount = 0;
          this.ttsSynthesizeRetryCount = 0;
          this.ttsCurrentMessage = null;
        }
      });
      
      // 播放暂停事件
      this.ttsAudioContext.onPause(() => {
        console.log('⏸️ [TTS] onPause - 音频已暂停');
      });
      
      // 播放跳转事件
      this.ttsAudioContext.onSeeking(() => {
        console.log('⏩ [TTS] onSeeking - 音频跳转中');
      });
      
      // 播放结束事件
      this.ttsAudioContext.onEnded(() => {
        console.log('⏹️ [TTS] onEnded - 音频播放结束');
        this.stopTts();
      });

      // 播放错误事件
      this.ttsAudioContext.onError((e) => {
        console.error('❌ [TTS] onError - 播放错误:', {
          errMsg: e?.errMsg,
          errCode: e?.errCode,
          src: this.ttsAudioContext?.src,
          currentTime: this.ttsAudioContext?.currentTime,
          duration: this.ttsAudioContext?.duration
        });
        uni.showToast({ 
          title: '播放失败: ' + (e?.errMsg || '未知错误'),
          icon: 'none',
          duration: 3000
        });
        this.stopTts();
      });
      
      // 设置音频源并播放
      console.log('🎵 [TTS] 设置音频源:', this.currentTtsAudioUrl);
      this.ttsAudioContext.src = this.currentTtsAudioUrl;
      
      console.log('▶️ [TTS] 调用 play() 方法');
      this.ttsAudioContext.play();
      
      // 不再使用定时检测，完全依赖事件监听
      // onPlay 事件触发时表示播放成功
      // onError 事件触发时表示播放失败
      // onCanplay 事件触发时表示音频已加载可以播放
      console.log('✅ [TTS] 已设置音频源并调用play，等待事件回调');
    },

    // 暂停播放
    pauseTts() {
      console.log('⏸️ [TTS] pauseTts 调用');
      if (this.ttsAudioContext && this.ttsState === 'playing') {
        this.ttsAudioContext.pause();
        this.ttsState = 'paused';
        console.log('✅ [TTS] 已暂停');
      } else {
        console.log('⚠️ [TTS] 无法暂停，当前状态:', this.ttsState);
      }
    },

    // 继续播放
    resumeTts() {
      console.log('▶️ [TTS] resumeTts 调用');
      if (this.ttsAudioContext && this.ttsState === 'paused') {
        this.ttsAudioContext.play();
        this.ttsState = 'playing';
        console.log('✅ [TTS] 已继续播放');
      } else {
        console.log('⚠️ [TTS] 无法继续，当前状态:', this.ttsState);
      }
    },

    // 停止播放并清理资源
    stopTts() {
      console.log('⏹️ [TTS] stopTts 调用，当前状态:', this.ttsState);
      
      if (this.ttsAudioContext) {
        console.log('🗑️ [TTS] 销毁音频上下文');
        try {
          this.ttsAudioContext.stop();
          this.ttsAudioContext.destroy();
        } catch (e) {
          console.warn('⚠️ [TTS] 销毁音频上下文时出现错误:', e);
        }
        this.ttsAudioContext = null;
      }
      
      this.ttsState = 'idle';
      this.currentTtsText = '';
      this.currentTtsAudioUrl = '';
      this.ttsSynthesizeRetryCount = 0;
      this.ttsPlayRetryCount = 0;
      this.ttsCurrentMessage = null;
      
      console.log('✅ [TTS] 已清理所有TTS资源');
    },

    // 截取文本用于显示
    truncateText(text, maxLength) {
      if (!text) return '';
      if (text.length <= maxLength) {
        return text;
      }
      return text.substring(0, maxLength) + '...';
    },
    
    // 处理消息内容：删除Additional Information及其后续内容，并清理末尾空格和换行
    processMessageContent(content) {
      if (!content || typeof content !== 'string') {
        return content;
      }
      
      let processedContent = content;
      
      // 1. 如果消息中含有"Additional Information:"字符串，取最后一个并删除它及其之后的内容
      const additionalInfoIndex = processedContent.lastIndexOf('Additional Information:');
      if (additionalInfoIndex !== -1) {
        processedContent = processedContent.substring(0, additionalInfoIndex);
      }
      
      // 2. 剔除末尾的换行和空格
      processedContent = processedContent.trimEnd();
      
      return processedContent;
    },

    // 根据AI回复内容生成推荐话题
    generateRecommendations(content) {
      const recommendations = [];
      
      // 基于内容关键词生成推荐
      if (content.includes('健康') || content.includes('养生')) {
        recommendations.push('健康小贴士', '养生方法');
      }
      if (content.includes('天气') || content.includes('气温')) {
        recommendations.push('明天天气', '出行建议');
      }
      if (content.includes('钓鱼') || content.includes('垂钓')) {
        recommendations.push('钓鱼技巧', '鱼类知识');
      }
      if (content.includes('戏曲') || content.includes('京剧')) {
        recommendations.push('戏曲欣赏', '名段推荐');
      }
      if (content.includes('新闻') || content.includes('资讯')) {
        recommendations.push('今日要闻', '社会热点');
      }
      
      // 基于用户兴趣标签生成推荐
      this.selectedTags.forEach(tag => {
        if (!recommendations.some(rec => rec.includes(tag))) {
          recommendations.push(`${tag}相关话题`);
        }
      });
      
      // 默认推荐
      if (recommendations.length === 0) {
        recommendations.push('更多话题', '生活趣事', '健康建议');
      }
      
      // 限制推荐数量
      return recommendations.slice(0, 3);
    }
  }
}
</script>

<style scoped>
/* 原有样式基础上仅添加自适应相关修改 */
.chat-container {
  height: 100vh;
  display: flex;
  flex-direction: column;
  background-color: #f8f8f8;
  box-sizing: border-box;
}

/* 状态栏占位 */
.status-bar {
  width: 100%;
  background-color: #fff;
  flex-shrink: 0;
}

/* 自定义导航栏 */
.custom-header {
  height: 88rpx;
  min-height: 88rpx;
  display: flex;
  align-items: center;
  position: relative;
  padding: 0 30rpx;   /* 导航栏内边距 */
  background-color: #fff;
  border-bottom: 1rpx solid #eee;
  flex-shrink: 0;
  box-sizing: border-box;
}

.header-title {
  position: absolute;
  left: 50%;
  transform: translateX(-50%);
  font-size: 32rpx;
  font-weight: bold;
  color: #333;
  text-align: center;
  z-index: 1;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 20rpx;
  margin-left: auto;
  z-index: 2;
}

.nav-icon {
  width: 50rpx;
  height: 50rpx;
  transition: opacity 0.3s ease;
}

.nav-icon:active {
  opacity: 0.5;   /* 点击时透明度降低 */
  transform: scale(0.90);   /* 点击时缩小 */
}

.back-icon {
  width: 32rpx;
  height: 32rpx;
  z-index: 2;
  position: relative;
}

.chat-area {
  flex: 1;
  padding: 0rpx 20rpx 0rpx 20rpx;   /* 聊天区域内边距离，四个参数分别为上右下左 */
  background-color: #fff;
  overflow-y: auto;
  box-sizing: border-box;
  /* 隐藏滚动条 */
  scrollbar-width: none;
  -ms-overflow-style: none;
}

/* 顶部间距占位 */
.top-spacer {
  height: 10rpx; /* 可以根据需要调整底部间距 */
}
/* 底部间距占位 */
.bottom-spacer {
  height: 5rpx; /* 可以根据需要调整底部间距 */
}

.message {
  margin-bottom: 30rpx;
  display: flex;
  flex-direction: column;
  align-items: flex-start;
}

.message.user {
  align-items: flex-end;
}

.message.ai {
  align-items: flex-start;
}

.message-content {  
  padding: 20rpx;     /* 内边距 */
  border-radius: 24rpx;
  position: relative;
  padding-right: 16rpx;
}

.message.user .message-content {
  background-color: #95ec69dd;
  color: #333;
  font-size: large;
  transition: all 0.2s ease;
}

.message.ai .message-content {
  background-color: #F5F5F5;
  color: #333;
  font-size: large;
  transition: all 0.2s ease;
}

/* 点击高亮效果 */
.message.user .message-content.tap-active {
  background-color: #7dd148;
  transform: scale(1.02);
  box-shadow: 0 4rpx 12rpx rgba(149, 236, 105, 0.5);
}

.message.ai .message-content.tap-active {
  background-color: #e8e8e8;
  transform: scale(1.02);
  box-shadow: 0 4rpx 12rpx rgba(0, 0, 0, 0.15);
}

.message-time {
  font-size: 20rpx;
  color: #999;
  margin-bottom: 4rpx;
  margin-left: 12rpx;
  margin-right: 12rpx;
}

.message.user .message-time {
  text-align: right;
  color: #999;
}

.message.ai .message-time {
  text-align: left;
  color: #666;
}

.message-with-icon {
  display: flex;
  align-items: flex-start;
  max-width: 92%;
}

.recommend-icon {
  width: 40rpx;
  height: 40rpx;
  margin-left: 10rpx;
  margin-top: 10rpx;
  flex-shrink: 0;
}

/* 输入区域主容器 */
.input-area {
  background-color: #ffffff;
  flex-shrink: 0;
  box-sizing: border-box;
  padding: 20rpx 20rpx 20rpx 20rpx;  /* 输入框区域左右边距20rpx，底部边距20rpx */
  border-top: 2rpx solid #e7e7e7;
  border-radius: 32rpx 32rpx 0rpx 0rpx;  /* 输入框区域圆角，四个参数方向： 左上，右上，右下，左下 */
}

/* 上层：输入框区域 */
.input-text-area {
  margin-bottom: 20rpx;
}

.input-textarea {
  width: 100%;
  min-height: 72rpx;
  max-height: 288rpx; /* 6行的高度，假设每行48rpx */
  font-size: 28rpx;
  padding: 20rpx;
  background-color: #f5f5f5;
  border-radius: 20rpx;
  border: none;
  box-sizing: border-box;
  line-height: 1.5;
  resize: none;
  overflow-y: auto; /* 超出时显示滚动条 */
  word-wrap: break-word; /* 长单词自动换行 */
  word-break: break-all; /* 强制换行 */
}

/* 下层：控制按钮区域 */
.input-controls {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 80rpx;
}

/* 左侧兴趣图标 */
.interest-icon {
  width: 60rpx;
  height: 60rpx;
  opacity: 0.7;
  transition: opacity 0.3s ease, transform 0.3s ease;
}

.interest-icon:active {
  opacity: 0.5;
  transform: scale(0.9);
}

/* 右侧按钮组 */
.right-controls {
  display: flex;
  align-items: center;
  gap: 20rpx;
}

/* 控制图标通用样式 */
.control-icon {
  width: 60rpx;
  height: 60rpx;
  display: flex;
  justify-content: center;
  align-items: center;
  opacity: 0.8;
  transition: opacity 0.3s ease, transform 0.3s ease;
}

.control-icon:active {
  opacity: 0.5;
  transform: scale(0.9);
}

/* 语音按钮 */
.voice-btn {
  background-color: transparent;
  padding: 0;
  border-radius: 50%;
  transition: all 0.3s ease;
}

/* 录音输入模式样式 */
.recording-input-mode {
  padding: 40rpx 30rpx;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 200rpx;
  background: linear-gradient(135deg, #fff 0%, #f8f9fa 100%);
}

.recording-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  width: 100%;
}

/* 录音状态提示 */
.recording-hint {
  display: flex;
  flex-direction: column;
  align-items: center;
  margin-bottom: 40rpx;
}

.hint-text {
  font-size: 28rpx;
  color: #666;
  margin-bottom: 20rpx;
}

/* 声音波形动画 */
.voice-wave {
  display: flex;
  align-items: center;
  gap: 8rpx;
}

.wave-dot {
  width: 12rpx;
  height: 12rpx;
  background-color: #ff4d4f;
  border-radius: 50%;
  animation: wave-pulse 1.4s ease-in-out infinite both;
}

.wave-dot-1 {
  animation-delay: 0s;
}

.wave-dot-2 {
  animation-delay: 0.2s;
}

.wave-dot-3 {
  animation-delay: 0.4s;
}

@keyframes wave-pulse {
  0%, 80%, 100% {
    transform: scale(0.8);
    opacity: 0.6;
  }
  40% {
    transform: scale(1.2);
    opacity: 1;
  }
}

/* AI思考动画 */
.thinking-animation {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 15rpx;
  padding-top: 8rpx; /* 给顶部留出空间，防止圆点跳跃时碰到边界 */
}

.thinking-dots {
  display: flex;
  align-items: center;
  gap: 10rpx;
  margin-top: 4rpx; /* 圆点容器额外的上边距 */
}

.thinking-dot {
  width: 12rpx;
  height: 12rpx;
  background-color: #4C3DFF;
  border-radius: 50%;
  animation: thinking-bounce 1.2s ease-in-out infinite both;
}

.thinking-dot-1 {
  animation-delay: 0s;
}

.thinking-dot-2 {
  animation-delay: 0.15s;
}

.thinking-dot-3 {
  animation-delay: 0.3s;
}

/* AI思考文字样式 */
.thinking-text {
  font-size: 26rpx; /* 字体大小 */
  color: #595959;      /* 字体颜色 */
  opacity: 0.8;     /* 透明度 */
  animation: thinking-text-fade 2s ease-in-out infinite alternate; /* 渐变动画 */
}

/* 思考圆点跳跃动画 */
@keyframes thinking-bounce {
  0%, 60%, 100% {
    transform: translateY(0);
    opacity: 0.7;
  }
  30% {
    transform: translateY(-12rpx);
    opacity: 1;
  }
}

/* 思考文字渐变动画 */
/* AI思考文字渐变动画 */
@keyframes thinking-text-fade {
  0% {
    opacity: 0.2; /* 动画起始时透明度较低 */
  }
  100% {
    opacity: 0.9; /* 动画结束时透明度较高 */
  }
}

/* 大型停止录音按钮 */
.stop-recording-btn {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  width: 200rpx;
  height: 200rpx;
  background: linear-gradient(135deg, #ff4d4f 0%, #ff7875 100%);
  border-radius: 80rpx;
  box-shadow: 0 8rpx 25rpx rgba(255, 77, 79, 0.3);
  position: relative;
  transition: all 0.2s ease;
  cursor: pointer;
}

/* 停止按钮光晕效果 */
.stop-recording-btn::before {
  content: '';
  position: absolute;
  top: -10rpx;
  left: -10rpx;
  right: -10rpx;
  bottom: -10rpx;
  background: rgba(255, 77, 79, 0.2);
  border-radius: 50rpx;
  z-index: -1;
  animation: stop-btn-glow 2s ease-in-out infinite alternate;
}

@keyframes stop-btn-glow {
  0% {
    box-shadow: 0 0 20rpx rgba(255, 77, 79, 0.3);
  }
  100% {
    box-shadow: 0 0 40rpx rgba(255, 77, 79, 0.5);
  }
}

/* 按钮按下状态 */
.stop-recording-btn.btn-pressed {
  transform: scale(0.95);
  box-shadow: 0 4rpx 15rpx rgba(255, 77, 79, 0.4);
}

/* 停止图标 */
.stop-icon {
  margin-bottom: 20rpx;
}

.stop-rect {
  width: 40rpx;
  height: 40rpx;
  background-color: white;
  border-radius: 8rpx;
  box-shadow: 0 2rpx 8rpx rgba(0, 0, 0, 0.15);
}

/* 停止文字 */
.stop-text {
  color: white;
  font-size: 24rpx;
  font-weight: 500;
  text-shadow: 0 1rpx 3rpx rgba(0, 0, 0, 0.2);
}

/* 发送按钮 */
.send-btn {
  background-color: #6ABF45;
  color: white;
  height: 60rpx;
  line-height: 60rpx;
  padding: 0 30rpx;
  border-radius: 30rpx;
  font-size: 26rpx;
  border: none;
  box-sizing: border-box;
}

.send-btn:active {
  background-color: #5aa83a;
}

/* 自动滚动状态指示器 */
.scroll-indicator {
  position: fixed;
  left: 50%;
  transform: translateX(-50%);
  bottom: 280rpx; /* 在输入框上方 */
  z-index: 1000;
  background-color: rgba(0, 0, 0, 0.353);
  color: white;
  border-radius: 40rpx;
  padding: 15rpx 20rpx;
  display: flex;
  align-items: center;
  gap: 10rpx;
  box-shadow: 0 4rpx 12rpx rgba(0, 0, 0, 0.3);
  backdrop-filter: blur(10rpx);
  transition: all 0.3s ease;
}

.scroll-indicator:active {
  transform: scale(0.95);
  background-color: rgba(0, 0, 0, 0.8);
}

.scroll-icon {
  font-size: 28rpx;
  font-weight: bold;
  line-height: 1;
}

.scroll-text {
  font-size: 24rpx;
  white-space: nowrap;
}

/* 以下为原有弹窗样式（完全不变） */
.interest-panel, .recommend-panel {
  background-color: #fff;
  padding: 30rpx;
  border-radius: 30rpx 30rpx 0 0;
}

.panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 30rpx;
  font-size: 32rpx;
  font-weight: bold;
  color: #333;
}

.interest-tags {
  display: flex;
  flex-wrap: wrap;
  margin-bottom: 40rpx;
}

.tag {
  padding: 15rpx 30rpx;
  margin: 0 20rpx 20rpx 0;
  background-color: #f0f0f0;
  border-radius: 40rpx;
  font-size: 26rpx;
  color: #666;
}

.tag.active {
  background-color: #9AB169;
  color: white;
}

.confirm-btn {
  background-color: #9AB169;
  color: white;
  height: 80rpx;
  line-height: 80rpx;
  border-radius: 40rpx;
  font-size: 28rpx;
}

.recommend-list {
  margin-bottom: 40rpx;
}

.recommend-item {
  padding: 25rpx;
  border-bottom: 1rpx solid #f0f0f0;
  font-size: 28rpx;
  color: #333;
}

.mic-icon {
  width: 36rpx;
  height: 36rpx;
  background-color: transparent;
}

/* ========== TTS 悬浮播放器样式 ========== */
.tts-player-float {
  position: fixed;
  left: 50%;
  transform: translateX(-50%);
  bottom: 300rpx; /* 在输入框上方，位置上移 */
  z-index: 999;
  background: #a18ffda7; /* 主题深绿色，纯色背景 */
  border-radius: 80rpx;
  padding: 20rpx 30rpx;
  box-shadow: 0 8rpx 25rpx rgba(#a18ffd);
  backdrop-filter: blur(10rpx);
  animation: tts-float-in 0.3s ease-out;
}

@keyframes tts-float-in {
  from {
    bottom: 250rpx;
    opacity: 0;
  }
  to {
    bottom: 300rpx;
    opacity: 1;
  }
}

/* 合成中状态 */
.tts-synthesizing {
  display: flex;
  align-items: center;
  gap: 20rpx;
  color: #fff;
}

.synthesis-loading {
  display: flex;
  align-items: center;
  gap: 8rpx;
}

.loading-dot {
  width: 10rpx;
  height: 10rpx;
  background-color: #fff;
  border-radius: 50%;
  animation: tts-loading-bounce 1.4s ease-in-out infinite both;
}

.loading-dot-1 {
  animation-delay: 0s;
}

.loading-dot-2 {
  animation-delay: 0.16s;
}

.loading-dot-3 {
  animation-delay: 0.32s;
}

@keyframes tts-loading-bounce {
  0%, 80%, 100% {
    transform: scale(0.8);
    opacity: 0.5;
  }
  40% {
    transform: scale(1.2);
    opacity: 1;
  }
}

.tts-status-text {
  font-size: 26rpx;
  color: #fff;
  white-space: nowrap;
}

/* 播放控制 */
.tts-controls {
  display: flex;
  align-items: center;
  justify-content: center;
}

.tts-buttons {
  display: flex;
  align-items: center;
  gap: 25rpx;
}

.tts-btn {
  width: 70rpx;
  height: 70rpx;
  background: rgba(255, 255, 255, 0.2);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s ease;
}

.tts-btn:active {
  transform: scale(0.92);
  background: rgba(255, 255, 255, 0.35);
}

.tts-icon-img {
  width: 36rpx;
  height: 36rpx;
}

.tts-pause-btn, .tts-continue-btn {
  background: rgba(255, 255, 255, 0.25);
}

.tts-stop-btn {
  background: rgba(255, 77, 79, 0.3);
}

.tts-stop-btn:active {
  background: rgba(255, 77, 79, 0.5);
}
</style>