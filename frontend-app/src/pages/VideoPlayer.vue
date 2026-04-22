<template>
  <div class="player-shell">
    <section class="player-hero">
      <div class="hero-copy">
        <p class="quest-kicker">Immersive Playback</p>
        <h1 class="hero-title">{{ videoTitle }}</h1>
        <p class="hero-subtitle">{{ sourceText }}</p>
      </div>
      <div class="hero-actions">
        <el-button @click="playPrevious">上一个动作</el-button>
        <el-button @click="playNext">下一个动作</el-button>
        <el-button @click="goBack">返回上一页</el-button>
        <el-button type="primary" @click="reloadPlayer" :disabled="!videoUrl">重新加载</el-button>
      </div>
    </section>

    <section class="player-stage card">
      <el-empty v-if="!videoUrl" description="缺少视频播放地址，请返回上一页重新进入。">
        <el-button type="primary" @click="goBack">返回</el-button>
      </el-empty>

      <div v-else class="player-layout">
        <div ref="playerContainer" class="player-container"></div>

        <aside class="player-side">
          <div class="info-card">
            <div class="info-label">视频标题</div>
            <div class="info-value">{{ videoTitle }}</div>
          </div>

          <div class="info-card">
            <div class="info-label">来源</div>
            <div class="info-value">{{ sourceName }}</div>
          </div>

          <div class="info-card">
            <div class="info-label">动作节点</div>
            <div class="info-value">{{ actionName }}</div>
          </div>

          <div class="info-card">
            <div class="info-label">动作位置</div>
            <div class="info-value">{{ actionPosition }}</div>
          </div>

          <div class="info-card">
            <div class="info-label">播放地址</div>
            <div class="mono-url">{{ videoUrl }}</div>
          </div>
        </aside>
      </div>
    </section>
  </div>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref, shallowRef, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import DPlayer from 'dplayer'
import { readVideoPlaylist } from '../utils/videoPlaylist'

const route = useRoute()
const router = useRouter()

const playerContainer = ref(null)
const player = shallowRef(null)

const queryValue = (key) => {
  const raw = route.query[key]
  if (Array.isArray(raw)) {
    return raw[0] || ''
  }
  return raw || ''
}

const playlistKey = computed(() => queryValue('playlistKey'))
const playlist = computed(() => {
  if (!playlistKey.value) {
    return []
  }
  return readVideoPlaylist(playlistKey.value)
})
const currentIndex = computed(() => {
  const raw = Number(queryValue('index'))
  if (Number.isFinite(raw) && raw >= 0) {
    return Math.floor(raw)
  }
  return 0
})
const currentVideo = computed(() => playlist.value[currentIndex.value] || null)
const returnTo = computed(() => queryValue('returnTo') || '/plans')
const actionId = computed(() => currentVideo.value?.id || queryValue('actionId'))

const videoUrl = computed(() => currentVideo.value?.url || queryValue('url'))
const videoTitle = computed(() => currentVideo.value?.title || queryValue('title') || '训练教学视频')
const coverUrl = computed(() => currentVideo.value?.cover || queryValue('cover'))
const sourceName = computed(() => currentVideo.value?.source || queryValue('source') || '训练计划')
const actionName = computed(() => {
  return currentVideo.value?.action || queryValue('action') || (actionId.value ? `动作 #${actionId.value}` : '未命名动作')
})
const actionPosition = computed(() => {
  if (!playlist.value.length) {
    return '当前为独立视频'
  }
  return `第 ${Math.min(currentIndex.value + 1, playlist.value.length)} / ${playlist.value.length} 个动作`
})
const sourceText = computed(() => {
  const parts = [`来源：${sourceName.value}`]
  if (actionName.value && actionName.value !== '未命名动作') {
    parts.push(`节点：${actionName.value}`)
  }
  if (playlist.value.length) {
    parts.push(actionPosition.value)
  }
  return parts.join(' · ')
})

const destroyPlayer = () => {
  if (player.value) {
    player.value.destroy()
    player.value = null
  }
}

const initPlayer = async () => {
  destroyPlayer()

  if (!videoUrl.value || !playerContainer.value) {
    return
  }

  await nextTick()

  player.value = new DPlayer({
    container: playerContainer.value,
    autoplay: false,
    theme: '#6d67ff',
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
}

const reloadPlayer = async () => {
  if (!videoUrl.value) {
    ElMessage.warning('当前缺少视频播放地址')
    return
  }
  await initPlayer()
  ElMessage.success('播放器已重新加载')
}

const navigateToIndex = (targetIndex) => {
  router.replace({
    path: '/video-player',
    query: {
      ...route.query,
      playlistKey: playlistKey.value,
      index: String(targetIndex)
    }
  })
}

const playPrevious = () => {
  if (!playlist.value.length || currentIndex.value <= 0) {
    ElMessage.info('当前已经是第一个动作了')
    return
  }
  navigateToIndex(currentIndex.value - 1)
}

const playNext = () => {
  if (!playlist.value.length || currentIndex.value >= playlist.value.length - 1) {
    ElMessage.info('当前已经是最后一个动作了')
    return
  }
  navigateToIndex(currentIndex.value + 1)
}

const goBack = () => {
  if (window.history.length > 1) {
    router.back()
    return
  }
  router.push(returnTo.value || '/plans')
}

watch(
  () => route.fullPath,
  async () => {
    await initPlayer()
  }
)

onMounted(async () => {
  await initPlayer()
})

onBeforeUnmount(() => {
  destroyPlayer()
})
</script>

<style scoped>
.player-shell {
  min-height: 100vh;
  padding: 24px;
  display: flex;
  flex-direction: column;
  gap: 18px;
  background:
    radial-gradient(circle at top left, rgba(109, 103, 255, 0.18), transparent 30%),
    radial-gradient(circle at bottom right, rgba(255, 208, 122, 0.2), transparent 28%),
    #f6f7fb;
}

.player-hero,
.player-stage {
  width: min(1320px, 100%);
  margin: 0 auto;
}

.player-hero {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: flex-end;
  padding: 24px 28px;
  border-radius: 32px;
  border: 3px solid var(--eco-border);
  background: linear-gradient(135deg, #6d67ff 0%, #8ca5ff 46%, #ffd07a 100%);
  box-shadow: 0 16px 0 rgba(52, 45, 105, 0.12), 0 24px 34px rgba(91, 83, 255, 0.12);
  color: #fff;
}

.hero-copy {
  max-width: 760px;
}

.hero-actions {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  justify-content: flex-end;
}

.player-layout {
  display: grid;
  grid-template-columns: minmax(0, 1.2fr) minmax(260px, 0.45fr);
  gap: 18px;
  align-items: stretch;
}

.player-container {
  height: 600px;
  border-radius: 24px;
  overflow: hidden;
  background: #000;
}

.player-container :deep(.dplayer) {
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  background: #000;
}

.player-container :deep(.dplayer-video-wrap) {
  flex: 1 1 auto;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  background: #000;
}

.player-container :deep(video) {
  width: 100%;
  height: 100%;
  object-fit: contain;
  outline: none;
  background: #000;
}

.player-side {
  display: flex;
  flex-direction: column;
  gap: 12px;
  height: 600px;
  overflow-y: auto;
}

.info-card {
  border-radius: 18px;
  border: 2px solid var(--eco-border);
  background: #fff;
  padding: 14px 16px;
}

.info-label {
  color: var(--eco-text-soft);
  font-size: 12px;
  font-weight: 800;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.info-value {
  margin-top: 8px;
  font-size: 16px;
  font-weight: 700;
  line-height: 1.6;
}

.mono-url {
  margin-top: 8px;
  word-break: break-all;
  color: var(--eco-text-soft);
  font-size: 12px;
  line-height: 1.7;
}

@media (max-width: 980px) {
  .player-hero {
    flex-direction: column;
    align-items: flex-start;
  }

  .hero-actions {
    justify-content: flex-start;
  }

  .player-layout {
    grid-template-columns: 1fr;
  }

  .player-container {
    height: 420px;
  }

  .player-side {
    height: auto;
    overflow-y: visible;
  }
}
</style>
