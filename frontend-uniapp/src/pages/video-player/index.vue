<template>
  <view class="player-page">
    <view class="topbar">
      <button class="icon-button" @click="goBack">
        <image class="top-icon" :src="icons.back" mode="aspectFit" />
      </button>
      <view class="title-block">
        <text class="page-title">{{ videoTitle }}</text>
        <text class="page-sub">{{ sourceText }}</text>
      </view>
      <button class="icon-button" @click="reloadPlayer">
        <image class="top-icon" :src="icons.refresh" mode="aspectFit" />
      </button>
    </view>

    <view class="stage-card">
      <view v-if="!videoUrl" class="empty-state">
        <image class="empty-icon" :src="icons.video" mode="aspectFit" />
        <text class="empty-title">缺少视频播放地址</text>
        <button class="primary-pill" @click="goBack">返回计划</button>
      </view>

      <view v-else class="player-shell">
        <!-- #ifdef H5 -->
        <view v-show="!h5Fallback" id="senlian-dplayer" class="dplayer-host"></view>
        <video
          v-if="h5Fallback"
          class="native-video"
          :src="videoUrl"
          :poster="coverUrl"
          controls
          autoplay
          object-fit="contain"
        />
        <!-- #endif -->

        <!-- #ifndef H5 -->
        <video
          class="native-video"
          :src="videoUrl"
          :poster="coverUrl"
          controls
          autoplay
          object-fit="contain"
        />
        <!-- #endif -->
      </view>
    </view>

    <view class="controls-card">
      <button class="control-button" :disabled="currentIndex <= 0" @click="playPrevious">
        <image class="control-icon" :src="icons.prev" mode="aspectFit" />
        <text>上一动作</text>
      </button>
      <button class="control-button" :disabled="!playlist.length || currentIndex >= playlist.length - 1" @click="playNext">
        <text>下一动作</text>
        <image class="control-icon" :src="icons.next" mode="aspectFit" />
      </button>
    </view>

    <view class="info-card">
      <view class="info-row">
        <text class="info-label">动作节点</text>
        <text class="info-value">{{ actionName }}</text>
      </view>
      <view class="info-row">
        <text class="info-label">播放位置</text>
        <text class="info-value">{{ actionPosition }}</text>
      </view>
      <view class="info-row">
        <text class="info-label">来源</text>
        <text class="info-value">{{ sourceName }}</text>
      </view>
    </view>
  </view>
</template>

<script setup>
import {
  faChevronLeft,
  faChevronRight,
  faPlay,
  faRotateRight,
  faVideo
} from '@fortawesome/free-solid-svg-icons'
import { computed, nextTick, ref, shallowRef } from 'vue'
import { onLoad, onReady, onUnload } from '@dcloudio/uni-app'
import { readVideoPlaylist } from '../../utils/videoPlaylist'

const faIcon = (definition, color = '#24104f') => {
  const [width, height, , , pathData] = definition.icon
  const paths = Array.isArray(pathData)
    ? pathData.map((path) => `<path fill="${color}" d="${path}"/>`).join('')
    : `<path fill="${color}" d="${pathData}"/>`
  const svg = `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 ${width} ${height}">${paths}</svg>`
  return `data:image/svg+xml;charset=utf-8,${encodeURIComponent(svg)}`
}

const icons = {
  back: faIcon(faChevronLeft),
  next: faIcon(faChevronRight),
  play: faIcon(faPlay, '#8b63ff'),
  prev: faIcon(faChevronLeft),
  refresh: faIcon(faRotateRight),
  video: faIcon(faVideo, '#8b63ff')
}

const query = ref({})
const playlist = ref([])
const currentIndex = ref(0)
const player = shallowRef(null)
const h5Fallback = ref(false)

const queryValue = (key) => {
  const value = query.value[key]
  return Array.isArray(value) ? value[0] || '' : value || ''
}

const currentVideo = computed(() => playlist.value[currentIndex.value] || null)
const videoUrl = computed(() => currentVideo.value?.url || decodeURIComponent(queryValue('url') || ''))
const videoTitle = computed(() => currentVideo.value?.title || decodeURIComponent(queryValue('title') || '') || '训练教学视频')
const coverUrl = computed(() => currentVideo.value?.cover || decodeURIComponent(queryValue('cover') || ''))
const sourceName = computed(() => currentVideo.value?.source || decodeURIComponent(queryValue('source') || '') || '训练计划')
const actionName = computed(() => currentVideo.value?.action || decodeURIComponent(queryValue('action') || '') || '未命名动作')
const actionPosition = computed(() => {
  if (!playlist.value.length) return '当前为独立视频'
  return `第 ${Math.min(currentIndex.value + 1, playlist.value.length)} / ${playlist.value.length} 个动作`
})
const sourceText = computed(() => `${sourceName.value} · ${actionPosition.value}`)

const destroyPlayer = () => {
  if (player.value) {
    player.value.destroy()
    player.value = null
  }
}

const initH5Player = async () => {
  // #ifdef H5
  destroyPlayer()
  h5Fallback.value = false
  if (!videoUrl.value) return
  await nextTick()
  const container = document.getElementById('senlian-dplayer')
  if (!container) return
  try {
    const module = await import('dplayer')
    const DPlayer = module.default || module
    player.value = new DPlayer({
      container,
      autoplay: false,
      theme: '#8b63ff',
      loop: false,
      screenshot: true,
      hotkey: true,
      airplay: true,
      preload: 'auto',
      volume: 0.7,
      video: {
        url: videoUrl.value,
        pic: coverUrl.value || undefined
      }
    })
  } catch (_) {
    h5Fallback.value = true
  }
  // #endif
}

const reloadPlayer = () => {
  initH5Player()
}

const navigateToIndex = async (nextIndex) => {
  if (nextIndex < 0 || nextIndex >= playlist.value.length) return
  currentIndex.value = nextIndex
  await initH5Player()
}

const playPrevious = () => {
  if (currentIndex.value <= 0) {
    uni.showToast({ title: '当前已经是第一个动作', icon: 'none' })
    return
  }
  navigateToIndex(currentIndex.value - 1)
}

const playNext = () => {
  if (!playlist.value.length || currentIndex.value >= playlist.value.length - 1) {
    uni.showToast({ title: '当前已经是最后一个动作', icon: 'none' })
    return
  }
  navigateToIndex(currentIndex.value + 1)
}

const goBack = () => {
  const pages = getCurrentPages()
  if (pages.length > 1) {
    uni.navigateBack()
    return
  }
  uni.switchTab({ url: '/pages/plans/index' })
}

onLoad((options = {}) => {
  query.value = options
  const nextPlaylist = readVideoPlaylist(options.playlistKey)
  playlist.value = nextPlaylist
  const rawIndex = Number(options.index)
  currentIndex.value = Number.isFinite(rawIndex) && rawIndex >= 0 ? Math.floor(rawIndex) : 0
})

onReady(() => {
  initH5Player()
})

onUnload(() => {
  destroyPlayer()
})
</script>

<style scoped lang="scss">
.player-page {
  min-height: 100vh;
  padding: calc(var(--status-bar-height) + 28rpx) 28rpx 44rpx;
  background:
    radial-gradient(circle at 0 0, rgba(139, 99, 255, 0.18), transparent 30%),
    radial-gradient(circle at 100% 0, rgba(255, 128, 111, 0.16), transparent 28%),
    #fffaf4;
  color: #24104f;
}

button {
  margin: 0;
  padding: 0;
  border: 0;
  background: transparent;
  line-height: 1;
}

button::after {
  border: 0;
}

.topbar {
  display: grid;
  grid-template-columns: 62rpx minmax(0, 1fr) 62rpx;
  gap: 14rpx;
  align-items: center;
}

.icon-button {
  display: grid;
  width: 62rpx;
  height: 62rpx;
  place-items: center;
}

.top-icon {
  width: 36rpx;
  height: 36rpx;
}

.title-block {
  min-width: 0;
  text-align: center;
}

.page-title,
.page-sub {
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.page-title {
  color: #24104f;
  font-size: 30rpx;
  font-weight: 900;
}

.page-sub {
  margin-top: 6rpx;
  color: #7b6f98;
  font-size: 21rpx;
  font-weight: 700;
}

.stage-card,
.controls-card,
.info-card {
  border: 2rpx solid rgba(120, 86, 170, 0.16);
  border-radius: 28rpx;
  background: rgba(255, 255, 255, 0.94);
  box-shadow: 0 10rpx 28rpx rgba(52, 32, 95, 0.08);
}

.stage-card {
  margin-top: 28rpx;
  overflow: hidden;
}

.player-shell,
.dplayer-host,
.native-video {
  width: 100%;
  height: 520rpx;
  background: #000;
}

.dplayer-host :deep(.dplayer) {
  width: 100%;
  height: 100%;
  background: #000;
}

.dplayer-host :deep(video) {
  width: 100%;
  height: 100%;
  object-fit: contain;
  background: #000;
}

.empty-state {
  display: flex;
  align-items: center;
  flex-direction: column;
  justify-content: center;
  min-height: 420rpx;
  padding: 40rpx;
}

.empty-icon {
  width: 70rpx;
  height: 70rpx;
}

.empty-title {
  margin-top: 18rpx;
  color: #24104f;
  font-size: 28rpx;
  font-weight: 900;
}

.primary-pill {
  min-width: 180rpx;
  height: 62rpx;
  margin-top: 24rpx;
  border-radius: 999rpx;
  background: #8b63ff;
  color: #fff;
  font-size: 24rpx;
  font-weight: 900;
}

.controls-card {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16rpx;
  margin-top: 22rpx;
  padding: 16rpx;
}

.control-button {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10rpx;
  min-height: 70rpx;
  border-radius: 999rpx;
  background: #f3edff;
  color: #24104f;
  font-size: 24rpx;
  font-weight: 900;
}

.control-button[disabled] {
  opacity: 0.48;
}

.control-icon {
  width: 22rpx;
  height: 22rpx;
}

.info-card {
  margin-top: 22rpx;
  padding: 24rpx;
}

.info-row + .info-row {
  margin-top: 18rpx;
  padding-top: 18rpx;
  border-top: 2rpx solid rgba(120, 86, 170, 0.12);
}

.info-label,
.info-value {
  display: block;
}

.info-label {
  color: #7b6f98;
  font-size: 21rpx;
  font-weight: 800;
}

.info-value {
  margin-top: 7rpx;
  color: #24104f;
  font-size: 25rpx;
  font-weight: 900;
}
</style>
