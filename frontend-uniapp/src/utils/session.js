export const STORAGE_KEY = 'fitness_app_token'

export const readStoredSession = () => {
  const raw = uni.getStorageSync(STORAGE_KEY)
  if (!raw) {
    return {
      accessToken: '',
      refreshToken: '',
      user: null
    }
  }
  try {
    const parsed = typeof raw === 'string' ? JSON.parse(raw) : raw
    return {
      accessToken: parsed?.accessToken || '',
      refreshToken: parsed?.refreshToken || '',
      user: parsed?.user || null
    }
  } catch (_) {
    return {
      accessToken: String(raw || ''),
      refreshToken: '',
      user: null
    }
  }
}

export const persistSession = (session) => {
  uni.setStorageSync(STORAGE_KEY, {
    accessToken: session.accessToken || '',
    refreshToken: session.refreshToken || '',
    user: session.user || null
  })
}

export const clearStoredSession = () => {
  uni.removeStorageSync(STORAGE_KEY)
}
