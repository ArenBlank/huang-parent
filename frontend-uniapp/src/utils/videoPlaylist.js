const PLAYLIST_PREFIX = 'senlian_video_playlist_'

export const createVideoPlaylist = ({ items = [], source = '', cover = '', fallbackTitle = '训练教学视频' } = {}) => {
  return (items || [])
    .filter((item) => item?.video?.playUrl)
    .map((item) => ({
      id: item.id,
      url: item.video.playUrl,
      title: item.video.title || item.actionName || fallbackTitle,
      cover,
      source,
      action: item.actionName || '',
      dayIndex: item.dayIndex,
      durationMin: item.durationMin,
      sets: item.sets,
      reps: item.reps,
      restSec: item.restSec
    }))
}

export const saveVideoPlaylist = (playlist, scope = 'default') => {
  const key = `${PLAYLIST_PREFIX}${scope}_${Date.now()}`
  uni.setStorageSync(key, playlist || [])
  return key
}

export const readVideoPlaylist = (key) => {
  if (!key) return []
  const raw = uni.getStorageSync(key)
  return Array.isArray(raw) ? raw : []
}
