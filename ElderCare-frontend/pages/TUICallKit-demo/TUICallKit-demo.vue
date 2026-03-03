<template>
	<view class="container">
		<input type="text" v-model="inputID" :placeholder=" isLogin ? 'please enter a caller userID' : 'please enter your login userID' " />
		<text v-show="isLogin"> your userID: {{ userID }} </text>
		<button v-show="!isLogin" @click="handleLogin"> Login </button>
		<button v-show="isLogin" @click="handleCall"> start call </button>
	</view>
</template>
<script>
	const TUICallKit = uni.requireNativePlugin('TencentCloud-TUICallKit'); //【1】import TUICallKit plugin
	uni.$TUICallKit = TUICallKit;
	import { genTestUserSig } from '../../debug/GenerateTestUserSig.js'
	export default {
		data() {
			return {
				inputID: '',
				isLogin: false,
				userID: '',
			}
		},
		methods: {
			async handleLogin() {
				this.userID = this.inputID;
				// 从后端获取配置生成用户签名
				const { userSig, sdkAppID: SDKAppID } = await genTestUserSig(this.userID);
				const loginParams = { SDKAppID, userID: this.userID, userSig };   // apply SDKAppID、userSig
                //【2】Login
				uni.$TUICallKit.login( loginParams, res => {
						if (res.code === 0) {
							this.isLogin = true;
							this.inputID = '';
							console.log('[TUICallKit] login success.');
						} else { 
                            console.error('[TUICallKit] login failed, failed message = ', res.msg, params);
						}
					}
				);	
			},
			handleCall() {
				try {
					const callParams = {
						userIDList: [this.inputID],
						callMediaType: 2   // 1 -- audio call，2 -- video call
					};
					console.log('📡 TUICallKit 通话参数:', callParams)
                    //【3】start 1v1 video call
					uni.$TUICallKit.calls( callParams, res => {
							console.log('[TUICallKit] call params: ', JSON.stringify(res));
						}
					);
					this.inputID = '';
				} catch (error) {
					console.log('[TUICallKit] call error: ', error);
				}
			}
		}
	}
</script>
<style>
.container {
	margin: 30px;
}
.container input {
	height: 50px;
	border: 1px solid;
}
.container button {
	margin-top: 30px;
}
</style>