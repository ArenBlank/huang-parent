import { clearStoredSession, persistSession, readStoredSession } from './session'

const rawBaseUrl = import.meta.env.VITE_APP_BASE_URL || ''
const appBaseUrl = rawBaseUrl.replace(/\/app\/?$/, '').replace(/\/$/, '')

let refreshPromise = null
let lastToast = { message: '', at: 0 }

const SYSTEM_BUSY = '系统繁忙，请稍后再试'
const LOGIN_EXPIRED = '登录已过期，请重新登录'
const ACCOUNT_DISABLED = '您的账号已被禁用'

const normalizePath = (base, url) => {
  if (!url || /^https?:\/\//.test(url)) {
    return url
  }
  if (!base) {
    return url
  }
  if (base.endsWith('/app') && url.startsWith('/app')) {
    return `${base}${url.slice('/app'.length)}`
  }
  return `${base}${url.startsWith('/') ? url : `/${url}`}`
}

const showToastOnce = (message, icon = 'none') => {
  const safeMessage = message || SYSTEM_BUSY
  const now = Date.now()
  if (lastToast.message === safeMessage && now - lastToast.at < 1500) {
    return
  }
  lastToast = { message: safeMessage, at: now }
  uni.showToast({
    title: safeMessage,
    icon,
    duration: 2200
  })
}

const requestOnce = ({ url, method = 'GET', data, header = {}, skipAuth = false, timeout }) => {
  const session = readStoredSession()
  const headers = {
    'content-type': 'application/json',
    ...header
  }
  if (!skipAuth && session.accessToken) {
    headers['access-token'] = session.accessToken
  }

  return new Promise((resolve, reject) => {
    uni.request({
      url: normalizePath(appBaseUrl, url),
      method,
      data,
      header: headers,
      timeout,
      success: (response) => {
        const statusCode = Number(response.statusCode)
        if (statusCode >= 200 && statusCode < 300) {
          resolve(response.data)
          return
        }
        reject(new Error(response.data?.message || `请求失败：${statusCode}`))
      },
      fail: () => {
        reject(new Error('无法连接后端，请确认 App 服务已启动'))
      }
    })
  })
}

const goLogin = () => {
  const pages = getCurrentPages()
  const current = pages[pages.length - 1]?.route || ''
  if (current !== 'pages/login/index') {
    uni.redirectTo({ url: '/pages/login/index' })
  }
}

const refreshAccessToken = async () => {
  const session = readStoredSession()
  if (!session.refreshToken) {
    throw new Error('missing refresh token')
  }
  const payload = await requestOnce({
    url: '/app/auth/refresh-token',
    method: 'POST',
    data: { refreshToken: session.refreshToken },
    skipAuth: true
  })
  if (payload?.code !== 200 || !payload?.data?.accessToken) {
    throw new Error(payload?.message || '刷新登录态失败')
  }
  persistSession({
    accessToken: payload.data.accessToken,
    refreshToken: payload.data.refreshToken || session.refreshToken,
    user: session.user
  })
  return payload.data.accessToken
}

export const request = async (options) => {
  const payload = await requestOnce(options)
  if (!payload || typeof payload.code !== 'number' || payload.code === 200) {
    return payload
  }

  const session = readStoredSession()
  const message = payload.message || SYSTEM_BUSY
  const isRefreshRequest = String(options.url || '').includes('/auth/refresh-token')
  const isLoginRequest = String(options.url || '').includes('/auth/login')

  if (payload.code === 501) {
    clearStoredSession()
    showToastOnce(ACCOUNT_DISABLED)
    goLogin()
    throw new Error(ACCOUNT_DISABLED)
  }

  if (payload.code === 602) {
    if (!options._retry && !isRefreshRequest && !isLoginRequest && session.refreshToken) {
      try {
        refreshPromise = refreshPromise || refreshAccessToken().finally(() => {
          refreshPromise = null
        })
        await refreshPromise
        return request({
          ...options,
          _retry: true
        })
      } catch (_) {
        // fall through to logout
      }
    }

    clearStoredSession()
    showToastOnce(LOGIN_EXPIRED)
    goLogin()
    throw new Error(LOGIN_EXPIRED)
  }

  showToastOnce(message)
  throw new Error(message)
}

export const get = (url, data, options = {}) => request({ url, data, method: 'GET', ...options })

export const post = (url, data, options = {}) => request({ url, data, method: 'POST', ...options })

export const put = (url, data, options = {}) => request({ url, data, method: 'PUT', ...options })
