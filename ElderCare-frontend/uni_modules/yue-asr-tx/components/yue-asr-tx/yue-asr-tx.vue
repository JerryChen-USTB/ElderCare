<template>
	<view>
		<view id="record" class="record" :listeningRecordingBegins="recordFlag"
			:change:listeningRecordingBegins="record.listeningRecordingBeginsHandler" :options="options"
			:change:options="record.optionsHandler">
		</view>
	</view>
</template>

<script>
	export default {
		data() {
			return {
				msg: '',
				recordFlag: null,
				scriptPath: ''
			};
		},
		props: {
			options: {
				type: Object,
				default: () => {
					return {
						receordingDuration: 60,
						APPID: '',
						API_SECRET: '',
						API_KEY: ''
					}
				}
			}
		},
		methods: {
			start() {
				console.log(this.recordFlag);
				if (this.recordFlag == 'START') return
				this.recordFlag = 'START';
			},
			end() {
				if (this.recordFlag == 'END') return
				this.recordFlag = 'END';
			},
			resultMsg(e) {
				this.$emit('result', e)
				this.msg = e;
			},
			endCallback(e) {
				//结束
				this.recordFlag = 'END';
				this.$emit('onStop', e)
			},
			startCallback(e) {
				this.$emit('onOpen', e)
			},
			seconds(e) {
				this.$emit('countDown', e)
			},
			change(e) {
				this.$emit('change', e)
			},
		}
	};
</script>

<script lang="renderjs" module="record">
	import asrauthentication from './asrauthentication.js';
	let APPID = "";
	let API_SECRET = "";
	let API_KEY = "";
	let receordingDuration = 60
	let ws = null;
	let resultText = "";
	let resultTextTemp = "";
	let timer = null;
	let tapeStatus = {
		CONNECTING: 'CONNECTING',
		OPEN: 'OPEN',
		CLOSING: 'CLOSING',
		CLOSED: 'CLOSED'
	}
	export default {
		data() {
			return {
				recorder: null,
				recorderPath: '',
			}
		},

		mounted() {
			//加载脚本
			this.loadScript();
		},

		methods: {
			listeningRecordingBeginsHandler(flag) {
				console.log("======监听", flag)
				if (flag == null) return
				if (flag == 'START') {
					this.connectWebSocket()
				} else if (flag == 'END') {
					this.recorder.stop();
					if (ws != null) {
						ws.close();
					}
				}
			},
			loadScript() {
				var recordScript = document.getElementById("recordScript");
				if (recordScript) {
					console.log('有标签了');
				} else {
					var script = document.createElement('script');
					script.id = 'recordScript'
					script.src = `static/dist/index.umd.js`;
					document.body.appendChild(script);

					let path = null;
					// #ifdef APP
					path = 'file://' + plus.io.convertLocalFileSystemURL('/');
					// #endif	
					// #ifdef H5
					path = "./";
					// #endif

					script.onload = () => {
						this.recorder = new RecorderManager(path + 'static/dist')
						console.log("音频录制", this.recorder)
						this.initListen()
					}
				}

				//消息透传
				var uniewbview = document.getElementById("uniewbview");
				if (uniewbview) {
					console.log('有标签了');
				} else {
					var script = document.createElement('script');
					script.id = 'uniewbview'
					script.src = `static/dist/uni.webview.js`;
					document.body.appendChild(script);
				}

			},
			optionsHandler(options) {
				APPID = options.APPID
				API_SECRET = options.API_SECRET
				API_KEY = options.API_KEY
				receordingDuration = options.receordingDuration
			},
			initListen() {
				this.recorder.onStart = () => {
					//判断是否已经关闭了，处理权限的异步处理
					if (ws.readyState != WebSocket.OPEN) {
						//代表已经关闭
						return;
					}
					this.changeStatus(tapeStatus.OPEN);
				}
				this.recorder.onFrameRecorded = ({
					isLastFrame,
					frameBuffer
				}) => {
					if (ws.readyState === ws.OPEN) {
						ws.send(frameBuffer);
						if (isLastFrame) {
							ws.send(JSON.stringify({
								type: 'end'
							}));
							this.changeStatus("CLOSING");
						}
					}
				};
				this.recorder.onStop = () => {
					clearInterval(timer);
				};
			},
			closeAll() {
				console.log("关闭连接");
				this.recorder.stop();
				this.changeStatus(tapeStatus.CLOSED)
			},

			connectWebSocket() {
				const websocketUrl = "wss://" + this.getWebSocketUrl();
				console.log("连接url" + websocketUrl)
				if ("WebSocket" in window) {
					ws = new WebSocket(websocketUrl);
				} else if ("MozWebSocket" in window) {
					ws = new MozWebSocket(websocketUrl);
				} else {
					console.log("不支持WebSocket");
					return;
				}
				// console.log("连接url"+websocketUrl)
				this.changeStatus(tapeStatus.CONNECTING);
				ws.onopen = () => {
					console.log("打开连接");
					this.recorder.start({
						sampleRate: 16000,
						frameSize: 1280,
					})
				}
				ws.onmessage = (e) => {
					console.log('socket message', e.data);
					this.renderResult(e.data);
				};

				ws.onerror = (e) => {
					console.error(e);
					this.closeAll();
				};
				ws.onclose = (e) => {
					console.log("关闭连接", e);
					this.recorder.stop();
					this.closeAll();
				};
			},
			getWebSocketUrl() {
				const timestamp = parseInt(new Date().getTime() / 1000) - 1;
				const params = {
					secretid: API_SECRET,
					timestamp: timestamp,
					expired: timestamp + 60 * 60,
					nonce: timestamp,
					engine_model_type: '16k_zh',
					voice_id: timestamp.toString(),
					voice_format: 1 //pcm 
				};
				const url =
					'asr.cloud.tencent.com/asr/v2/' +
					APPID +
					'?' +
					Object.keys(params)
					.sort(function(a, b) {
						return a.localeCompare(b);
					})
					.map(key => {
						return encodeURIComponent(key) + '=' + encodeURIComponent(params[key]);
					})
					.join('&');
				const signature = asrauthentication.signCallback(url, API_KEY);
				return url + '&signature=' + encodeURIComponent(signature);
				return url;
			},
			toBase64(buffer) {
				let binary = "";
				const bytes = new Uint8Array(buffer);
				const len = bytes.byteLength;
				for (let i = 0; i < len; i++) {
					binary += String.fromCharCode(bytes[i]);
				}
				return window.btoa(binary);
			},
			renderResult(resultData) {
				let jsonData = JSON.parse(resultData);
				if (jsonData.code == 0) {
					//成功
					let data = jsonData.result;
					resultText = data.voice_text_str;
					this.$ownerInstance.callMethod('resultMsg', resultTextTemp || resultText || '')
					webUni.postMessage({
						data: {
							func: 'result',
							data: resultTextTemp || resultText || ''
						}
					});
				}

				if (jsonData.final === 1) {
					//解析结束
					ws.close();
				}
				if (jsonData.code !== 0) {
					//发生错误
					ws.close();
					console.error(jsonData);
				}
			},
			changeStatus(status) {
				let statusText = ''

				if (status === "CONNECTING") {
					statusText = '建立连接中'
					resultText = "";
					resultTextTemp = "";
				} else if (status === "OPEN") {
					statusText = '开始录音'
					this.$ownerInstance.callMethod('startCallback', {
						status,
						msg: statusText
					})
					webUni.postMessage({
						data: {
							func: 'onOpen',
							data: {
								status,
								msg: statusText
							}
						}
					});
					this.countdown();
				} else if (status === "CLOSING") {
					statusText = '关闭连接中'
				} else if (status === "CLOSED") {
					statusText = "录音已关闭";
					this.$ownerInstance.callMethod('endCallback', {
						status,
						msg: statusText
					})
					webUni.postMessage({
						data: {
							func: 'onStop',
							data: {
								status,
								msg: statusText
							}
						}
					});
				}
				this.$ownerInstance.callMethod('change', {
					status,
					msg: statusText
				})
				webUni.postMessage({
					data: {
						func: 'change',
						data: {
							status,
							msg: statusText
						}
					}
				});
			},
			countdown() {
				let seconds = receordingDuration
				let that = this;
				if (that.timer != null) {
					clearInterval(that.timer);
				}
				that.calltime(seconds, timer);
				that.timer = setInterval(() => {
					seconds = seconds - 1;
					that.calltime(seconds, timer);
				}, 1000);
			},

			calltime(seconds, timer) {
				if (seconds <= 0) {
					clearInterval(timer);
					this.recorder.stop();
					//关闭socket
					if (ws != null) {
						ws.close();
					}
				} else {
					this.$ownerInstance.callMethod('seconds', seconds)
					webUni.postMessage({
						data: {
							func: 'countDown',
							data: seconds
						}
					});
				}
			}

		}

	}
</script>
<style></style>