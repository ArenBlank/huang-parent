import axios from 'axios'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '../stores/auth'

const rawAdminBase = import.meta.env.VITE_ADMIN_BASE_URL || ''
const rawAppBase = import.meta.env.VITE_APP_BASE_URL || ''
const adminBase = rawAdminBase.replace(/\/admin\/?$/, '')
const appBase = rawAppBase.replace(/\/app\/?$/, '')

const adminClient = axios.create({
  baseURL: adminBase,
  timeout: 12000
})

const adminRefreshClient = axios.create({
  baseURL: adminBase,
  timeout: 12000
})

const appClient = axios.create({
  baseURL: appBase,
  timeout: 12000
})

let adminRefreshPromise = null
let lastToast = { message: '', at: 0 }

const HANDLED_ERROR = 'HANDLED_BUSINESS_ERROR'

const MSG_SYSTEM_BUSY = '\u7cfb\u7edf\u7e41\u5fd9\uff0c\u8bf7\u7a0d\u540e\u518d\u8bd5'
const MSG_ACCOUNT_DISABLED = '\u60a8\u7684\u8d26\u53f7\u5df2\u88ab\u7981\u7528'
const MSG_LOGIN_EXPIRED = '\u767b\u5f55\u5df2\u8fc7\u671f\uff0c\u8bf7\u91cd\u65b0\u767b\u5f55'

const normalizePath = (base, url, prefix) => {
  if (!url) return url
  const safeBase = (base || '').replace(/\/$/, '')
  if (safeBase.endsWith(prefix) && url.startsWith(prefix)) {
    return url.slice(prefix.length) || '/'
  }
  return url
}

const showMessageOnce = (message, type = 'warning') => {
  const safeMessage = message || MSG_SYSTEM_BUSY
  const now = Date.now()
  if (lastToast.message === safeMessage && now - lastToast.at < 1500) {
    return
  }
  lastToast = { message: safeMessage, at: now }
  ElMessage({
    type,
    message: safeMessage,
    grouping: true
  })
}

const createHandledError = (code, message, response) => {
  const error = new Error(message || '\u8bf7\u6c42\u5931\u8d25')
  error.name = HANDLED_ERROR
  error.code = code
  error.response = response
  error.__handled = true
  error.isGlobalHandled = true
  return error
}

const suspendRequest = () => new Promise(() => {})

const redirectToLogin = () => {
  if (window.location.pathname !== '/login') {
    window.location.replace('/login')
  }
}

const isRefreshRequest = (config) => String(config?.url || '').includes('/auth/refresh-token')
const isLoginRequest = (config) => String(config?.url || '').includes('/auth/login')

const refreshAdminAccessToken = async () => {
  const store = useAuthStore()
  if (!store.refreshToken) {
    throw new Error('missing refresh token')
  }
  const { data } = await adminRefreshClient.post('/admin/auth/refresh-token', {
    refreshToken: store.refreshToken
  })
  if (data?.code !== 200 || !data?.data?.accessToken) {
    throw new Error(data?.message || '\u5237\u65b0\u767b\u5f55\u6001\u5931\u8d25')
  }
  store.setSession({
    accessToken: data.data.accessToken,
    refreshToken: data.data.refreshToken || store.refreshToken,
    user: store.user,
    roles: store.roles
  })
  return store.accessToken
}

const handleAdminBusinessCode = async (response) => {
  const payload = response?.data
  if (!payload || typeof payload.code !== 'number' || payload.code === 200) {
    return response
  }

  const message = payload.message || MSG_SYSTEM_BUSY
  const store = useAuthStore()
  const originalConfig = response.config || {}

  if (payload.code === 201 || payload.code === 203) {
    showMessageOnce(message, 'warning')
    throw createHandledError(payload.code, message, response)
  }

  if (payload.code === 501) {
    store.logout()
    showMessageOnce(MSG_ACCOUNT_DISABLED, 'error')
    redirectToLogin()
    return suspendRequest()
  }

  if (payload.code === 602) {
    if (!originalConfig._retry && !isRefreshRequest(originalConfig) && !isLoginRequest(originalConfig) && store.refreshToken) {
      originalConfig._retry = true
      try {
        adminRefreshPromise = adminRefreshPromise || refreshAdminAccessToken().finally(() => {
          adminRefreshPromise = null
        })
        const nextAccessToken = await adminRefreshPromise
        originalConfig.headers = originalConfig.headers || {}
        originalConfig.headers.Authorization = `Bearer ${nextAccessToken}`
        return adminClient.request(originalConfig)
      } catch (e) {
        console.error('Admin token refresh failed:', e)
        // fall through to logout
      }
    }

    store.logout()
    showMessageOnce(MSG_LOGIN_EXPIRED, 'warning')
    redirectToLogin()
    return suspendRequest()
  }

  throw new Error(message)
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

const attachErrorHandler = (client, serviceLabel, businessHandler) => {
  client.interceptors.response.use(
    async (response) => (businessHandler ? businessHandler(response) : response),
    (error) => {
      if (error?.__handled || error?.isGlobalHandled) {
        return Promise.reject(error)
      }
      if (error?.code === 'ECONNABORTED') {
        return Promise.reject(new Error('\u8bf7\u6c42\u8d85\u65f6\uff0c\u8bf7\u68c0\u67e5\u540e\u7aef\u670d\u52a1\u662f\u5426\u542f\u52a8'))
      }
      if (!error?.response) {
        return Promise.reject(new Error(`\u65e0\u6cd5\u8fde\u63a5\u540e\u7aef\uff0c\u8bf7\u786e\u8ba4 ${serviceLabel} \u670d\u52a1\u5df2\u542f\u52a8`))
      }
      const status = error.response?.status
      if (status === 401) {
        return Promise.reject(new Error('\u767b\u5f55\u5931\u6548\uff0c\u8bf7\u91cd\u65b0\u767b\u5f55'))
      }
      if (status === 403) {
        return Promise.reject(new Error('\u6ca1\u6709\u6743\u9650\u8bbf\u95ee\u8be5\u8d44\u6e90'))
      }
      if (status === 404) {
        return Promise.reject(new Error('\u63a5\u53e3\u4e0d\u5b58\u5728\uff0c\u8bf7\u68c0\u67e5\u524d\u7aef\u4ee3\u7406\u6216\u540e\u7aef\u8def\u7531'))
      }
      if (status === 400) {
        const message = error.response?.data?.message
        return Promise.reject(new Error(message || '\u8bf7\u6c42\u53c2\u6570\u9519\u8bef\uff0c\u8bf7\u68c0\u67e5\u586b\u5199\u5185\u5bb9'))
      }
      if (status >= 500) {
        return Promise.reject(new Error('\u670d\u52a1\u7aef\u5f02\u5e38\uff0c\u8bf7\u67e5\u770b\u540e\u7aef\u65e5\u5fd7'))
      }
      return Promise.reject(error)
    }
  )
}

attachErrorHandler(adminClient, 'Admin', handleAdminBusinessCode)
attachErrorHandler(appClient, 'App', null)

const isHandledBusinessError = (error) =>
  Boolean(error?.__handled || error?.isGlobalHandled || error?.name === HANDLED_ERROR)

export { adminClient, appClient, isHandledBusinessError }
