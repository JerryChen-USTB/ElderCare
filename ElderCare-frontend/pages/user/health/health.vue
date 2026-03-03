<template>
	<view class="health-container">
		<!-- 健康数据展示区 -->
		<view class="content">
			<!-- 实时健康数据 -->
			<view class="health-data">
				<view class="data-item" v-for="item in healthData" :key="item.type">
					<text class="data-value" :class="{'warning': item.isWarning}">{{item.value}}</text>
					<text class="data-label">{{item.label}}</text>
					<text class="data-time" v-if="item.time">{{item.time}}</text>
				</view>
			</view>
			
			<!-- 健康趋势图表 -->
			<view class="chart-container">
				<text class="section-title">健康趋势</text>
				<!-- 预留图表位置 -->
				<view class="chart-placeholder">
					<text>健康数据图表将显示在这里</text>
					<!-- 
						API: 获取健康趋势数据
						接口地址: /api/health/trend
						请求方法: GET
						参数: userid, datatype, daterange
						返回: 健康趋势数据
					-->
				</view>
			</view>
			
			<!-- 报警记录 -->
			<view class="alert-container" v-if="alerts.length > 0">
				<text class="section-title">报警记录</text>
				<view class="alert-list">
					<view class="alert-item" v-for="alert in alerts" :key="alert.id">
						<uni-icons type="warning" size="20" color="#ff0000"></uni-icons>
						<text class="alert-type">{{alert.type}}</text>
						<text class="alert-time">{{alert.time}}</text>
						<text class="alert-status" :class="{'resolved': alert.status === 'resolved'}">
							{{alert.status === 'resolved' ? '已处理' : '未处理'}}
						</text>
					</view>
				</view>
				<!-- 
					API: 获取报警记录
					接口地址: /api/alertlog
					请求方法: GET
					参数: userid
					返回: 报警记录列表
				-->
			</view>
		</view>
		
		<!-- 一键求助按钮 -->
		<view class="emergency-btn" @click="handleEmergency">
			<uni-icons type="sound" size="32" color="#ffffff"></uni-icons>
			<text>一键求助</text>
		</view>
		
		<!-- 功能未开放弹窗 -->
		<view v-if="showUnavailableModal" class="unavailable-modal-overlay">
			<view class="unavailable-modal-container">
				<!-- 插画 -->
				<image class="unavailable-image" src="/static/elder/unavailable.png" mode="aspectFit"></image>
				
				<!-- 提示文字 -->
				<text class="unavailable-title">本功能暂未开放</text>
				<text class="unavailable-subtitle">敬请期待！</text>
				
				<!-- 返回首页按钮 -->
				<view class="back-home-btn" @click="backToHome">
					返回首页
				</view>
			</view>
		</view>
	</view>
</template>

<script>
	export default {
		data() {
			return {
				showUnavailableModal: false, // 控制未开放弹窗显示
				healthData: [
					{ type: 'heartrate', value: '72', label: '心率(bpm)', time: '10:30', isWarning: false },
					{ type: 'temperature', value: '36.5', label: '体温(°C)', time: '10:30', isWarning: false },
					{ type: 'bloodpressure', value: '120/80', label: '血压(mmHg)', time: '10:30', isWarning: false }
				],
				alerts: [
					{ id: 1, type: '血压过高', time: '2023-05-20 09:15', status: 'resolved' },
					{ id: 2, type: '心率异常', time: '2023-05-19 14:30', status: 'pending' }
				]
			}
		},
		methods: {
			goBack() {
				uni.navigateBack();
			},
			backToHome() {
				// 返回首页
				uni.reLaunch({
					url: '/pages/user/index/index'
				});
			},
			handleEmergency() {
				// 预留紧急求助接口
				/*
				API: 紧急求助接口
				接口地址: /api/emergency
				请求方法: POST
				参数: userid, location, emergencytype
				返回: 求助订单号
				*/
				uni.showToast({
					title: '求助请求已发送',
					icon: 'none'
				});
				
				// 模拟获取位置
				this.getLocation();
			},
			getLocation() {
				// 预留获取位置接口
				/*
				API: 实时定位接口
				接口地址: /api/location/realtime
				请求方法: GET
				参数: userid, deviceid
				返回: 经纬度和地址
				*/
				console.log("获取用户位置...");
			},
			fetchHealthData() {
				// 预留获取健康数据接口
				/*
				API: 健康数据上传接口
				接口地址: /api/healthdata
				请求方法: POST
				参数: userid, datatype, value, timestamp
				返回: 成功状态
				*/
				console.log("获取健康数据...");
			}
		},
		onLoad() {
			// 页面加载时显示功能未开放弹窗
			this.showUnavailableModal = true;
		},
		onShow() {
			// 页面显示时也显示弹窗（防止从其他页面返回时不显示）
			this.showUnavailableModal = true;
		}
	}
</script>

<style>
	.health-container {
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
	
	.health-data {
		display: flex;
		justify-content: space-around;
		padding: 30rpx 0;
		background-color: #ffffff;
		border-radius: 16rpx;
		margin-bottom: 30rpx;
		box-shadow: 0 4rpx 12rpx rgba(0, 0, 0, 0.05);
	}
	
	.data-item {
		display: flex;
		flex-direction: column;
		align-items: center;
	}
	
	.data-value {
		font-size: 48rpx;
		font-weight: bold;
		color: #3cc51f;
		margin-bottom: 10rpx;
	}
	
	.data-value.warning {
		color: #ff0000;
	}
	
	.data-label {
		font-size: 28rpx;
		color: #999;
	}
	
	.data-time {
		font-size: 24rpx;
		color: #ccc;
		margin-top: 10rpx;
	}
	
	.section-title {
		display: block;
		font-size: 32rpx;
		font-weight: bold;
		color: #333;
		margin: 30rpx 0 20rpx;
	}
	
	.chart-container {
		background-color: #ffffff;
		padding: 30rpx;
		border-radius: 16rpx;
		margin-bottom: 30rpx;
		box-shadow: 0 4rpx 12rpx rgba(0, 0, 0, 0.05);
	}
	
	.chart-placeholder {
		height: 300rpx;
		display: flex;
		justify-content: center;
		align-items: center;
		background-color: #f9f9f9;
		border-radius: 8rpx;
		color: #999;
	}
	
	.alert-container {
		background-color: #ffffff;
		padding: 30rpx;
		border-radius: 16rpx;
		box-shadow: 0 4rpx 12rpx rgba(0, 0, 0, 0.05);
	}
	
	.alert-list {
		margin-top: 20rpx;
	}
	
	.alert-item {
		display: flex;
		align-items: center;
		padding: 20rpx 0;
		border-bottom: 1rpx solid #f5f5f5;
	}
	
	.alert-type {
		flex: 1;
		margin-left: 20rpx;
		color: #333;
	}
	
	.alert-time {
		font-size: 24rpx;
		color: #999;
		margin-right: 20rpx;
	}
	
	.alert-status {
		font-size: 24rpx;
		color: #ff0000;
	}
	
	.alert-status.resolved {
		color: #3cc51f;
	}
	
	.emergency-btn {
		position: fixed;
		bottom: 60rpx;
		right: 60rpx;
		display: flex;
		flex-direction: column;
		align-items: center;
		justify-content: center;
		width: 120rpx;
		height: 120rpx;
		background-color: #ff0000;
		border-radius: 50%;
		box-shadow: 0 8rpx 24rpx rgba(255, 0, 0, 0.3);
		color: #ffffff;
		font-size: 24rpx;
	}
	
	/* 功能未开放弹窗样式 */
	.unavailable-modal-overlay {
		position: fixed;
		top: 0;
		left: 0;
		right: 0;
		bottom: 0;
		background-color: rgba(0, 0, 0, 0.7);
		display: flex;
		justify-content: center;
		align-items: center;
		z-index: 9999;
		animation: fadeIn 0.3s ease;
	}
	
	@keyframes fadeIn {
		from {
			opacity: 0;
		}
		to {
			opacity: 1;
		}
	}
	
	.unavailable-modal-container {
		width: 600rpx;
		background: linear-gradient(135deg, #ffffff 0%, #f8f9fa 100%);
		border-radius: 40rpx;
		padding: 80rpx 60rpx 60rpx;
		display: flex;
		flex-direction: column;
		align-items: center;
		box-shadow: 0 20rpx 60rpx rgba(0, 0, 0, 0.3);
		animation: slideUp 0.4s cubic-bezier(0.34, 1.56, 0.64, 1);
	}
	
	@keyframes slideUp {
		from {
			transform: translateY(100rpx);
			opacity: 0;
		}
		to {
			transform: translateY(0);
			opacity: 1;
		}
	}
	
	.unavailable-image {
		width: 400rpx;
		height: 300rpx;
		margin-bottom: 50rpx;
	}
	
	.unavailable-title {
		font-size: 44rpx;
		font-weight: bold;
		color: #333;
		margin-bottom: 20rpx;
		text-align: center;
	}
	
	.unavailable-subtitle {
		font-size: 32rpx;
		color: #666;
		margin-bottom: 60rpx;
		text-align: center;
	}
	
	.back-home-btn {
		width: 100%;
		height: 100rpx;
		background: linear-gradient(135deg, #3cc51f, #2aa515);
		border-radius: 50rpx;
		display: flex;
		justify-content: center;
		align-items: center;
		color: #ffffff;
		font-size: 36rpx;
		font-weight: bold;
		box-shadow: 0 10rpx 30rpx rgba(60, 197, 31, 0.4);
		transition: all 0.3s ease;
	}
	
	.back-home-btn:active {
		transform: scale(0.95);
		box-shadow: 0 5rpx 15rpx rgba(60, 197, 31, 0.3);
	}
</style>