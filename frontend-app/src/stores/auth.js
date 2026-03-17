import { defineStore } from 'pinia'
import { appClient } from '../api/client'

const STORAGE_KEY = 'fitness_app_token'

export const useAppAuthStore = defineStore('appAuth', {
  state: () => ({
    accessToken: localStorage.getItem(STORAGE_KEY) || '',
    user: null
  }),
  actions: {
    async login(account, password) {
      const { data } = await appClient.post('/app/auth/login', {
        account,
        password,
        loginType: 'password'
      })
      if (data.code !== 200) {
        throw new Error(data.message || '登录失败')
      }
      this.accessToken = data.data.accessToken
      this.user = data.data.userInfo || null
      localStorage.setItem(STORAGE_KEY, this.accessToken)
    },
    async fetchProfile() {
      if (!this.accessToken) return
      const { data } = await appClient.get('/app/profile/info')
      if (data.code !== 200) {
        throw new Error(data.message || '加载用户信息失败')
      }
      this.user = data.data || null
    },
    logout() {
      this.accessToken = ''
      this.user = null
      localStorage.removeItem(STORAGE_KEY)
    }
  }
})
