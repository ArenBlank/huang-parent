import axios from 'axios'
import { useAuthStore } from '../stores/auth'

const adminClient = axios.create({
  baseURL: import.meta.env.VITE_ADMIN_BASE_URL || '/admin'
})

const appClient = axios.create({
  baseURL: import.meta.env.VITE_APP_BASE_URL || '/app'
})

adminClient.interceptors.request.use((config) => {
  const store = useAuthStore()
  if (store.accessToken) {
    config.headers = config.headers || {}
    config.headers.Authorization = `Bearer ${store.accessToken}`
  }
  return config
})

export { adminClient, appClient }
