const TAB_PATHS = new Set([
  '/pages/home/index',
  '/pages/plans/index',
  '/pages/courses/index',
  '/pages/mine/index'
])

export const normalizePageUrl = (url) => (url.startsWith('/') ? url : `/${url}`)

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
