import axios from 'axios'
import { useAuthStore } from '../stores/auth'

const rawAdminBase = import.meta.env.VITE_ADMIN_BASE_URL || ''
const rawAppBase = import.meta.env.VITE_APP_BASE_URL || ''
const adminBase = rawAdminBase.replace(/\/admin\/?$/, '')
const appBase = rawAppBase.replace(/\/app\/?$/, '')

const adminClient = axios.create({
  baseURL: adminBase,
  timeout: 12000
})

const appClient = axios.create({
  baseURL: appBase,
  timeout: 12000
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

const attachErrorHandler = (client, serviceLabel) => {
  client.interceptors.response.use(
    (response) => response,
    (error) => {
      if (error?.code === 'ECONNABORTED') {
        return Promise.reject(new Error('请求超时，请检查后端服务是否启动'))
      }
      if (!error?.response) {
        return Promise.reject(new Error(`无法连接后端，请确认 ${serviceLabel} 服务已启动`))
      }
      const status = error.response?.status
      if (status === 401) {
        return Promise.reject(new Error('登录失效，请重新登录'))
      }
      if (status === 403) {
        return Promise.reject(new Error('没有权限访问该资源'))
      }
      if (status === 404) {
        return Promise.reject(new Error('接口不存在，请检查前端代理或后端路由'))
      }
      if (status === 400) {
        const message = error.response?.data?.message
        return Promise.reject(new Error(message || '请求参数错误，请检查填写内容'))
      }
      if (status >= 500) {
        return Promise.reject(new Error('服务端异常，请查看后端日志'))
      }
      return Promise.reject(error)
    }
  )
}

attachErrorHandler(adminClient, '8080')
attachErrorHandler(appClient, '8081')

export { adminClient, appClient }
