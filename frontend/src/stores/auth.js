import { defineStore } from 'pinia'
import { adminClient } from '../api/client'

const STORAGE_KEY = 'fitness_admin_token'

export const useAuthStore = defineStore('auth', {
  state: () => ({
    accessToken: localStorage.getItem(STORAGE_KEY) || '',
    user: null,
    roles: []
  }),
  actions: {
    async login(account, password) {
      const { data } = await adminClient.post('/admin/auth/login', {
        account,
        password
      })
      if (data.code !== 200) {
        throw new Error(data.message || 'Login failed')
      }
      this.accessToken = data.data.accessToken
      this.user = {
        id: data.data.userId,
        username: data.data.username
      }
      this.roles = data.data.roleCodes || []
      localStorage.setItem(STORAGE_KEY, this.accessToken)
    },
    logout() {
      this.accessToken = ''
      this.user = null
      this.roles = []
      localStorage.removeItem(STORAGE_KEY)
    }
  }
})
