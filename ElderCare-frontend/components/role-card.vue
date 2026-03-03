<template>
  <view class="role-card" :style="{ backgroundColor: color }" @click="$emit('click')">
    <image :src="iconPath" class="icon" />
    <text class="title">{{ title }}</text>
    <text class="desc">{{ desc }}</text>
  </view>
</template>

<script>
export default {
  props: {
    title: String,
    desc: String,
    icon: String,
    color: String
  },
  computed: {
    iconPath() {
      // 根据传入的icon名称返回对应的图标路径
      const iconMap = {
        user: '/static/icons/user.png',
        guardian: '/static/icons/guardian.png',
        volunteer: '/static/icons/volunteer.png'
      };
      return iconMap[this.icon] || '';
    }
  }
}
</script>

<style scoped>
.role-card {
  padding: 40rpx;
  border-radius: 30rpx;
  color: white;
  display: flex;
  flex-direction: column;
  align-items: center;
  box-shadow: 0 10rpx 20rpx rgba(0,0,0,0.1);
}

.icon {
  width: 120rpx;
  height: 120rpx;
  margin-bottom: 30rpx;
}

.title {
  font-size: 40rpx;
  font-weight: bold;
  margin-bottom: 15rpx;
}

.desc {
  font-size: 28rpx;
  opacity: 0.9;
}
</style>