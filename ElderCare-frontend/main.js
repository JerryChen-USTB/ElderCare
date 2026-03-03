import App from './App'

// 引入IP地址切换工具（开发环境便利工具）
// #ifdef H5
import './utils/ip-helper.js'
// #endif

// #ifndef VUE3
import Vue from 'vue'
import './uni.promisify.adaptor'
import uniTransition from '@dcloudio/uni-ui/lib/uni-transition/uni-transition.vue'

Vue.config.productionTip = false

// 全局注册 uni-transition 组件
Vue.component('uni-transition', uniTransition)

// 使用 Vuex
Vue.use(require('vuex').default)

App.mpType = 'app'

const app = new Vue({
  ...App
})

app.$mount()
// #endif

// #ifdef VUE3
import { createSSRApp } from 'vue'
import { createStore } from 'vuex'
import uniTransition from '@dcloudio/uni-ui/lib/uni-transition/uni-transition.vue'

// 创建 Vuex store
const vuexStore = createStore({
  state() {
    return {
      isLoggedIn: false,
      userRole: null,
      userInfo: null
    }
  },
  mutations: {
    login(state, payload) {
      state.isLoggedIn = true
      state.userRole = payload.role
      state.userInfo = payload.userInfo
    },
    logout(state) {
      state.isLoggedIn = false
      state.userRole = null
      state.userInfo = null
    },
    setRole(state, role) {
      state.userRole = role
    }
  },
  actions: {
    login({ commit }, userData) {
      return new Promise((resolve) => {
        setTimeout(() => {
          commit('login', {
            role: 'user',
            userInfo: {
              name: userData.name || '用户',
              phone: userData.phone
            }
          })
          resolve()
        }, 1000)
      })
    }
  }
})

console.log('Vuex store 已创建')

export function createApp() {
  const app = createSSRApp(App)
  
  // 全局注册 uni-transition 组件
  app.component('uni-transition', uniTransition)
  
  // 使用 Vuex store
  app.use(vuexStore)
  
  // 提供 store 以便在组件中注入
  app.provide('store', vuexStore)
  
  console.log('Vue3 应用已初始化')
  
  return {
    app,
    store: vuexStore
  }
}
// #endif