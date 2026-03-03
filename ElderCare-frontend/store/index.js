import Vue from 'vue'
import Vuex from 'vuex'

Vue.use(Vuex)

console.log('正在初始化 Vuex store...')

const store = new Vuex.Store({
  state: {
    isLoggedIn: false,
    userRole: null,
    userInfo: null
  },
  mutations: {
    login(state, payload) {
      state.isLoggedIn = true
      state.userRole = payload.role
      state.userInfo = payload.userInfo
      console.log('登录成功，角色:', payload.role)
    },
    logout(state) {
      state.isLoggedIn = false
      state.userRole = null
      state.userInfo = null
      console.log('已退出登录')
    },
    setRole(state, role) {
      console.log('设置角色为:', role)
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

console.log('Vuex store 初始化完成')

export default store