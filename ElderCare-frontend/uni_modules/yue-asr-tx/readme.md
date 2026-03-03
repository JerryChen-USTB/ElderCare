# yue-asr-xf

## 功能介绍

腾讯云的流式语音识别，支持**android**和**ios** **H5**的流式输出，解决app上录音不支持分片的功能

## 基础配置

请将模块下static目录的资源放到项目的static目录下面

```javascript
//内部语音对象创建
this.recorder = new RecorderManager(path + 'static/dist')
```

请放置路径正确

## 1.部署方案

这里很重要。

### 1.直接打包到app中

- 只支持<b>android</b>和**H5**	

### 2.**使用网页部署方式**

支持**android** **ios** **H5**，具体版本支持情况请看**支持问题** 

<a href="https://test.xiaofangfang.asia/yuyintx/">测试地址</a>

测试demo

```vue
<template>
	<view>
        <!--
      当前url对应代码参考使用demo对应代码，
-->
		<web-view src="https://test.xiaofangfang.asia/yuyintx/" @message="handleMessage"></web-view>
	</view>
</template>

<script>
	export default {
		methods: {
			handleMessage(evt) {
				let c = evt.detail.data;
				let {
					func,
					data
				} = c[0];
				console.log('回调函数：' + func);
				console.log('数据：' + JSON.stringify(data));
			}
		}
	}
</script>
```

#### 1.消息交互

使用H5部署的方案，本质语音识别模块已经脱离了当前项目的环境，对于结果直接使用@message返回，

交互需要使用 [uni.webview.js]，对于原来已经集成到项目，请将重新copy dist目录




## 2.权限问题 android

当前项目打包在app上面，需要配置系统权限

```
<uses-permission android:name="android.permission.RECORD_AUDIO" />
<uses-permission android:name="android.permission.MODIFY_AUDIO_SETTINGS" />
```

对于android应用，基于安全权限，请在调用时动态申请危险权限

```javascript
// #ifdef APP
plus.android.requestPermissions(["android.permission.RECORD_AUDIO"], (e) => {}, (e) => {})
// #endif
```

## 3.支持问题

1.在uniapp 在app上不支持**录音分片**（截止到2025-01-01）为了解决这个问题，底层使用了**renderjs**

使用了web相关的技术。

2.内置录音采用 MediaDevices接口，不兼容ie浏览器、夸克浏览器和uc浏览器，其他浏览器兼容性具体文档可参考 https://developer.mozilla.org/zh-CN/docs/Web/API/MediaDevices  

3.浏览器测试时，请改为移动端模式。


## 4.使用demo

```vue
<template>
	<view class="content">
		<view placeholder="转文字" class="inputarea">
			{{msg}}
		</view>
		<view class="down-ui" v-if="downed" :style="{backgroundColor:downtime==-1?'#e43d33':'#1acf3b'}">
			<!-- 效果显示 -->
			<view v-if="downtime==-1">
				建立连接中
			</view>
			<view v-else>
				语音倒计时
				<text style="color: red;">{{downtime}} </text>
			</view>
		</view>

		<button class="btn-bottom" :disabled="disabled" @touchstart.stop="start" @touchend.stop="end">按下说话</button>
		
		<yue-asr-tx ref="yueAsrRefs" :options="optionstx" @countDown="countDown" @result="resultMsg" @onStop="onStop"
			@onOpen="onOpen" @change="change"></yue-asr-tx>
	</view>
</template>

<script>
	export default {
		data() {

			let second = 60;
			return {
				title: 'Hello',
				msg: '转文字',
				optionsxf: {
					receordingDuration: second,
					APPID: '',
					API_SECRET: '',
					API_KEY: ''
				},
				downtime: -1, //默认-1
				downed: false,
				disabled: false,
                second,
			};
		},
		onLoad() {
			// #ifdef APP
			plus.android.requestPermissions(["android.permission.RECORD_AUDIO"], (e) => {}, (e) => {})
			// #endif
		},
		methods: {

			resumeUi() {
				this.downed = false;
				this.downtime = -1;
				this.disabled = false;
                this.downtime=this.second;
			},

			start() {
				if (this.disabled) {
					return;
				}
				console.log("开始")
				this.downed = true;
				this.$refs.yueAsrRefs.start();
				this.disabled = true;
				//建立连接
			},
			end() {
				console.log("结束")
				this.$refs.yueAsrRefs.end();
			},
			countDown(e) {
				console.log('countDown', e);
				this.downtime = e;
			},
			onStop(e) {
				console.log('onStop', e);
				this.resumeUi();
			},
			onOpen(e) {
				console.log('onOpen', e);
			},
			change(e) {
				console.log('change', e);
			},
			resultMsg(e) {
				this.msg = e
				console.log('resultMsg', e);
			}
		}
	};
</script>

<style>
	.btn-bottom {
		width: 100vw;
		position: absolute;
		bottom: 0px;
	}
	.inputarea {
		text-align: left;
		color: red;
		height: 200rpx;
		border: 1px solid #ccc;
		margin: 10rpx;
		border-radius: 10rpx;
		padding: 5rpx;
		overflow-y: scroll;
	}

	.down-ui {
		height: 100px;
		width: 100%;
		position: absolute;
		bottom: 50px;
		text-align: center;
		display: flex;
		justify-content: center;
		align-items: center;
	}
</style>
```











