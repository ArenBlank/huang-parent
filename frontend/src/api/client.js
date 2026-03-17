import axios from 'axios'
import { useAuthStore } from '../stores/auth'

const rawAdminBase = import.meta.env.VITE_ADMIN_BASE_URL || ''
const rawAppBase = import.meta.env.VITE_APP_BASE_URL || ''
const adminBase = rawAdminBase.replace(/\/admin\/?$/, '')
const appBase = rawAppBase.replace(/\/app\/?$/, '')

const adminClient = axios.create({
  baseURL: adminBase
})

const appClient = axios.create({
  baseURL: appBase
})

const normalizePath = (base, url, prefix) => {
  if (!url) return url
  const safeBase = (base || '').replace(/\/$/, '')
  if (safeBase.endsWith(prefix) && url.startsWith(prefix)) {
    return url.slice(prefix.length) || '/'
  }
  return url
}

adminClient.interceptors.request.use((config) => {
  if (config.url) {
    config.url = normalizePath(config.baseURL, config.url, '/admin')
  }
  const store = useAuthStore()
  if (store.accessToken) {
    config.headers = config.headers || {}
    config.headers.Authorization = `Bearer ${store.accessToken}`
  }
  return config
})

appClient.interceptors.request.use((config) => {
  if (config.url) {
    config.url = normalizePath(config.baseURL, config.url, '/app')
  }
  return config
})

export { adminClient, appClient }
