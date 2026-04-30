import { get, put } from '../../utils/request'
import { readStoredSession } from '../../utils/session'

export const getProfile = () => get('/app/profile/info')

export const updateProfile = (payload) => put('/app/profile/info', payload)

export const updatePassword = (payload) => put('/app/profile/password', payload)

const rawBaseUrl = import.meta.env.VITE_APP_BASE_URL || ''
const appBaseUrl = rawBaseUrl.replace(/\/app\/?$/, '').replace(/\/$/, '')

const normalizePath = (base, url) => {
  if (!url || /^https?:\/\//.test(url)) return url
  if (!base) return url
  if (base.endsWith('/app') && url.startsWith('/app')) {
    return `${base}${url.slice('/app'.length)}`
  }
  return `${base}${url.startsWith('/') ? url : `/${url}`}`
}

const parseUploadResponse = (response) => {
  if (typeof response === 'string') {
    try {
      return JSON.parse(response)
    } catch (_) {
      return { code: 500, message: response || '头像上传失败' }
    }
  }
  return response || {}
}

export const uploadAvatar = (filePath) =>
  new Promise((resolve, reject) => {
    const session = readStoredSession()
    uni.uploadFile({
      url: normalizePath(appBaseUrl, '/app/profile/avatar/upload'),
      filePath,
      name: 'file',
      header: session.accessToken ? { 'access-token': session.accessToken } : {},
      success: (response) => {
        const statusCode = Number(response.statusCode)
        const payload = parseUploadResponse(response.data)
        if (statusCode >= 200 && statusCode < 300 && (!payload.code || payload.code === 200)) {
          resolve(payload)
          return
        }
        reject(new Error(payload.message || `头像上传失败：${statusCode}`))
      },
      fail: () => {
        reject(new Error('头像上传失败，请确认后端服务和文件存储已启动'))
      }
    })
  })
