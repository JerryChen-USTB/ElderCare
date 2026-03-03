<template>
  <view class="custom-tabbar">
    <view
      v-for="(item, index) in tabConfigs"
      :key="index"
      class="tab-item"
      :class="{ active: current === index }"
      @click="switchTab(index)"
    >
      <image
        :src="current === index ? item.activeIcon : item.icon"
        mode="aspectFit"
      ></image>
      <text :style="{ color: current === index ? selectedColor : color }">
        {{ item.text }}
      </text>
    </view>
  </view>
</template>

<script>
export default {
  props: {
    current: {
      type: Number,
      default: 0
    },
    role: {
      type: String,
      default: 'user'
    },
    color: {
      type: String,
      default: '#999'
    },
    selectedColor: {
      type: String,
      default: '#4B3425'
    }
  },
  computed: {
    tabConfigs() {
      const baseIconPath = '/static/icons/';
      const tabConfigs = {
        user: [
          {
            text: '首页',
            icon: baseIconPath + 'home.png',
            activeIcon: baseIconPath + 'home-active.png'
          },
          {
            text: '我的',
            icon: baseIconPath + 'my.png',
            activeIcon: baseIconPath + 'my-active.png'
          }
        ],
        guardian: [
          {
            text: '首页',
            icon: baseIconPath + 'home.png',
            activeIcon: baseIconPath + 'home-active.png'
          },
          {
            text: '我的',
            icon: baseIconPath + 'my.png',
            activeIcon: baseIconPath + 'my-active.png'
          }
        ],
        volunteer: [
          {
            text: '首页',
            icon: baseIconPath + 'home.png',
            activeIcon: baseIconPath + 'home-active.png'
          },
          {
            text: '我的',
            icon: baseIconPath + 'my.png',
            activeIcon: baseIconPath + 'my-active.png'
          }
        ]
      };

      return tabConfigs[this.role] || tabConfigs.user;
    }
  },
  methods: {
    switchTab(index) {
      if (this.current === index) return;

      // 根据索引切换页面
      const pagePaths = {
        user: ['/pages/user/index/index', '/pages/user/my/my'],
        guardian: ['/pages/guardian/index/index', '/pages/guardian/my/my'],
        volunteer: ['/pages/volunteer/index/index', '/pages/volunteer/my/my']
      };

      const paths = pagePaths[this.role] || pagePaths.user;

      uni.reLaunch({
        url: paths[index]
      });

      // 触发自定义事件通知父组件
      this.$emit('tabChange', index);
    }
  }
};
</script>

<style scoped>
.custom-tabbar {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  height: 100rpx;
  background-color: #ffffff;
  border-top: 1rpx solid #e8e8e8;
  display: flex;
  justify-content: space-around;
  align-items: center;
  z-index: 999;
}

.tab-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}

.tab-item image {
  width: 40rpx;
  height: 40rpx;
  margin-bottom: 5rpx;
}

.tab-item.active image {
  filter: brightness(0) saturate(100%) invert(18%) sepia(26%) saturate(1686%) hue-rotate(359deg) brightness(96%) contrast(91%);
}

.tab-item text {
  font-size: 24rpx;
}

.tab-item.active text {
  color: #4B3425;
  font-weight: 520;
}
</style>