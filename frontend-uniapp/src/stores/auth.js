import { defineStore } from 'pinia'
import { login, register, getProfile } from '../api/modules/auth'
import { clearStoredSession, persistSession, readStoredSession } from '../utils/session'

const createRegisteredUser = (data) => ({
  id: data?.userId,
  username: data?.username,
  nickname: data?.nickname,
  phone: data?.phone
})

export const useAppAuthStore = defineStore('appAuth', {
  state: () => ({
    ...readStoredSession()
  }),
  getters: {
    displayName: (state) => state.user?.nickname || state.user?.username || '训练用户'
  },
  actions: {
    setSession(payload = {}) {
      this.accessToken = payload.accessToken || ''
      this.refreshToken = payload.refreshToken || ''
      this.user = payload.userInfo || payload.user || null
      persistSession(this)
    },
    async login(account, password, captchaVerification) {
      const { data } = await login({
        account,
        password,
        loginType: 'password',
        captchaVerification,
        deviceType: 'web'
      })
      this.setSession({
        accessToken: data.accessToken,
        refreshToken: data.refreshToken,
        userInfo: data.userInfo || null
      })
    },
    async register(payload) {
      const { data } = await register(payload)
      this.setSession({
        accessToken: data.accessToken,
        refreshToken: data.refreshToken,
        userInfo: data.userInfo || createRegisteredUser(data)
      })
    },
    async fetchProfile() {
      if (!this.accessToken) return null
      const { data } = await getProfile()
      this.user = data || null
      persistSession(this)
      return this.user
    },
    logout() {
      this.accessToken = ''
      this.refreshToken = ''
      this.user = null
      clearStoredSession()
    }
  }
})
