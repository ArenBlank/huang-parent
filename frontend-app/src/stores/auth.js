import { defineStore } from 'pinia'
import { appClient } from '../api/client'

const STORAGE_KEY = 'fitness_app_token'

const parseStoredSession = () => {
  const raw = localStorage.getItem(STORAGE_KEY)
  if (!raw) {
    return {
      accessToken: '',
      refreshToken: '',
      user: null
    }
  }
  try {
    const parsed = JSON.parse(raw)
    return {
      accessToken: parsed?.accessToken || '',
      refreshToken: parsed?.refreshToken || '',
      user: parsed?.user || null
    }
  } catch (_) {
    return {
      accessToken: raw,
      refreshToken: '',
      user: null
    }
  }
}

const persistSession = (session) => {
  localStorage.setItem(STORAGE_KEY, JSON.stringify({
    accessToken: session.accessToken || '',
    refreshToken: session.refreshToken || '',
    user: session.user || null
  }))
}

export const useAppAuthStore = defineStore('appAuth', {
  state: () => ({
    ...parseStoredSession()
  }),
  actions: {
    setSession(payload = {}) {
      this.accessToken = payload.accessToken || ''
      this.refreshToken = payload.refreshToken || ''
      this.user = payload.userInfo || payload.user || null
      persistSession(this)
    },
    async login(account, password) {
      const { data } = await appClient.post('/app/auth/login', {
        account,
        password,
        loginType: 'password'
      })
      if (data.code !== 200) {
        throw new Error(data.message || '登录失败')
      }
      this.setSession({
        accessToken: data.data.accessToken,
        refreshToken: data.data.refreshToken,
        userInfo: data.data.userInfo || null
      })
    },
    async fetchProfile() {
      if (!this.accessToken) return
      const { data } = await appClient.get('/app/profile/info')
      if (data.code !== 200) {
        throw new Error(data.message || '加载用户信息失败')
      }
      this.user = data.data || null
      persistSession(this)
    },
    logout() {
      this.accessToken = ''
      this.refreshToken = ''
      this.user = null
      localStorage.removeItem(STORAGE_KEY)
    }
  }
})
