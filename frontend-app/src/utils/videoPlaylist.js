const PLAYLIST_PREFIX = 'video-playlist:'

const safeText = (value, fallback = '') => {
  if (typeof value === 'string') {
    const trimmed = value.trim()
    return trimmed || fallback
  }
  return fallback
}

export const createVideoPlaylist = ({
  items = [],
  source = '训练计划',
  cover = '',
  fallbackTitle = '训练教学视频'
} = {}) =>
  items
    .filter((item) => item?.video?.playUrl)
    .map((item) => ({
      id: item?.id ?? null,
      dayIndex: item?.dayIndex ?? null,
      action: safeText(item?.actionName, '未命名动作'),
      title: safeText(item?.video?.title, safeText(item?.actionName, fallbackTitle)),
      url: item.video.playUrl,
      cover: safeText(cover),
      source: safeText(source, '训练计划')
    }))

export const saveVideoPlaylist = (playlist, keySeed = 'default') => {
  if (!Array.isArray(playlist) || !playlist.length) {
    return ''
  }

  const key = `${PLAYLIST_PREFIX}${keySeed}:${Date.now()}`
  try {
    sessionStorage.setItem(key, JSON.stringify(playlist))
    return key
  } catch (error) {
    return ''
  }
}

export const readVideoPlaylist = (playlistKey) => {
  if (!playlistKey) {
    return []
  }

  try {
    const raw = sessionStorage.getItem(playlistKey)
    if (!raw) {
      return []
    }
    const parsed = JSON.parse(raw)
    return Array.isArray(parsed) ? parsed : []
  } catch (error) {
    return []
  }
}
