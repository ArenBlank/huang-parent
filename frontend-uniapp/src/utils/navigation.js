const TAB_PATHS = new Set([
  '/pages/home/index',
  '/pages/plans/index',
  '/pages/courses/index',
  '/pages/mine/index'
])

export const normalizePageUrl = (url = '') => {
  const path = String(url || '').split('?')[0]
  if (!path) return ''
  return path.startsWith('/') ? path : `/${path}`
}

const isTabBarPage = (url) => TAB_PATHS.has(normalizePageUrl(url))

const currentPageUrl = () => {
  const pages = getCurrentPages()
  const current = pages[pages.length - 1]
  return normalizePageUrl(current?.route || '')
}

const isIgnorableTabBarError = (error) => String(error?.errMsg || '').includes('not TabBar page')

const callTabBarApi = (method) => {
  const result = uni[method]({
    fail: (error) => {
      if (!isIgnorableTabBarError(error)) {
        console.warn(`[navigation] ${method} failed`, error)
      }
    }
  })
  if (result && typeof result.catch === 'function') {
    result.catch((error) => {
      if (!isIgnorableTabBarError(error)) {
        console.warn(`[navigation] ${method} failed`, error)
      }
    })
  }
  return result
}

export const hideTabBarSafely = () => {
  if (!isTabBarPage(currentPageUrl())) {
    return
  }
  return callTabBarApi('hideTabBar')
}

export const showTabBarSafely = () => {
  if (!isTabBarPage(currentPageUrl())) {
    return
  }
  return callTabBarApi('showTabBar')
}

export const goPage = (url) => {
  const target = normalizePageUrl(url)
  if (TAB_PATHS.has(target)) {
    uni.switchTab({ url: target })
    return
  }
  uni.navigateTo({ url: target })
}

export const replacePage = (url) => {
  uni.redirectTo({ url: normalizePageUrl(url) })
}

export const goBack = () => {
  const pages = getCurrentPages()
  if (pages.length > 1) {
    uni.navigateBack()
    return
  }
  uni.switchTab({ url: '/pages/home/index' })
}
