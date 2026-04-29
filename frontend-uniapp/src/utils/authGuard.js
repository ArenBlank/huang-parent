import { useAppAuthStore } from '../stores/auth'
import { readStoredSession } from './session'

export const LOGIN_PAGE = '/pages/login/index'

export const ensureLogin = () => {
  const store = useAppAuthStore()
  const session = readStoredSession()
  if (store.accessToken && session.accessToken) {
    return true
  }
  store.logout()
  uni.redirectTo({ url: LOGIN_PAGE })
  return false
}

export const redirectLoggedInUser = () => {
  const store = useAppAuthStore()
  const session = readStoredSession()
  if (store.accessToken && session.accessToken) {
    uni.switchTab({ url: '/pages/home/index' })
    return true
  }
  if (store.accessToken && !session.accessToken) {
    store.logout()
  }
  return false
}
