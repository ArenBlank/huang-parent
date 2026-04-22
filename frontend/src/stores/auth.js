import { defineStore } from 'pinia'
import { adminClient } from '../api/client'

const STORAGE_KEY = 'fitness_admin_token'

const parseStoredSession = () => {
  const raw = localStorage.getItem(STORAGE_KEY)
  if (!raw) {
    return {
      accessToken: '',
      refreshToken: '',
      user: null,
      roles: []
    }
  }
  try {
    const parsed = JSON.parse(raw)
    return {
      accessToken: parsed?.accessToken || '',
      refreshToken: parsed?.refreshToken || '',
      user: parsed?.user || null,
      roles: parsed?.roles || []
    }
  } catch (_) {
    return {
      accessToken: raw,
      refreshToken: '',
      user: null,
      roles: []
    }
  }
}

const persistSession = (session) => {
  localStorage.setItem(STORAGE_KEY, JSON.stringify({
    accessToken: session.accessToken || '',
    refreshToken: session.refreshToken || '',
    user: session.user || null,
    roles: session.roles || []
  }))
}

export const useAuthStore = defineStore('auth', {
  state: () => ({
    ...parseStoredSession()
  }),
  actions: {
    setSession(payload = {}) {
      this.accessToken = payload.accessToken || ''
      this.refreshToken = payload.refreshToken || ''
      this.user = payload.user || (payload.userId ? {
        id: payload.userId,
        username: payload.username
      } : null)
      this.roles = payload.roleCodes || payload.roles || []
      persistSession(this)
    },
    async login(account, password, captchaVerification) {
      const { data } = await adminClient.post('/admin/auth/login', {
        account,
        password,
        captchaVerification
      })
      if (data.code !== 200) {
        throw new Error(data.message || '登录失败')
      }
      this.setSession({
        accessToken: data.data.accessToken,
        refreshToken: data.data.refreshToken,
        userId: data.data.userId,
        username: data.data.username,
        roleCodes: data.data.roleCodes || []
      })
    },
    logout() {
      this.accessToken = ''
      this.refreshToken = ''
      this.user = null
      this.roles = []
      localStorage.removeItem(STORAGE_KEY)
    }
  }
})
