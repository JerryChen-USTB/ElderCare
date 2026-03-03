<template>
	<!-- 新增：大字警告 -->
	    <view class="function-not-open">功能未开放！</view>
</template>

<script>
</script>

<style>
	.function-not-open {
	  /* 固定定位：页面滚动时仍可见，垂直位置避开顶部筛选区 */
	  position: fixed;
	  top: 180rpx; 
	  left: 0;
	  width: 100%;
	  /* 视觉醒目：红色背景+白色粗体大字体 */
	  background-color: #ff3b30; /* 苹果红，警告色更易识别 */
	  color: #ffffff;
	  font-size: 52rpx; /* 超大字体，强制注意力 */
	  font-weight: 700;
	  text-align: center;
	  padding: 35rpx 0; /* 上下内边距，增加视觉面积 */
	  z-index: 100; /* 确保在预警列表上方，不被遮挡 */
	  box-shadow: 0 6rpx 16rpx rgba(255, 59, 48, 0.4); /* 红色阴影强化警告感 */
	  }
</style>