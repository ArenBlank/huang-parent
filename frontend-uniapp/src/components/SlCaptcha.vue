<template>
  <view v-if="visible" class="captcha-mask" @touchmove.stop.prevent>
    <view class="captcha-panel">
      <view class="captcha-head">
        <view>
          <text class="captcha-kicker">HUMAN CHECK</text>
          <text class="captcha-title">完成滑块拼图验证</text>
        </view>
        <view class="captcha-close" @click="close">×</view>
      </view>

      <view class="captcha-canvas">
        <image
          v-if="captcha.originalImageBase64"
          class="captcha-image"
          :src="imageSrc(captcha.originalImageBase64)"
          mode="scaleToFill"
        />
        <image
          v-if="captcha.jigsawImageBase64"
          class="captcha-jigsaw"
          :src="imageSrc(captcha.jigsawImageBase64)"
          :style="{ transform: `translateX(${dragX}px)` }"
          mode="scaleToFill"
        />
        <view v-if="loading" class="captcha-state">正在生成拼图...</view>
        <view v-else-if="!captcha.originalImageBase64" class="captcha-state">拼图加载失败，请刷新重试</view>
      </view>

      <view
        class="captcha-track"
        :class="{ dragging, success }"
        @touchstart="startDrag"
        @touchmove.stop.prevent="onDrag"
        @touchend="endDrag"
        @pointerdown.stop.prevent="startDrag"
        @pointermove.stop.prevent="onDrag"
        @pointerup.stop.prevent="endDrag"
        @mousedown.stop.prevent="startDrag"
        @mousemove.stop.prevent="onDrag"
        @mouseup.stop.prevent="endDrag"
        @mouseleave="cancelMouseDrag"
      >
        <view class="captcha-progress" :style="{ width: progressWidth }"></view>
        <view
          class="captcha-slider"
          :style="{ transform: `translateX(${dragX}px)` }"
          @touchstart.stop="startDrag"
          @touchmove.stop.prevent="onDrag"
          @touchend.stop="endDrag"
          @pointerdown.stop.prevent="startDrag"
          @pointermove.stop.prevent="onDrag"
          @pointerup.stop.prevent="endDrag"
          @mousedown.stop.prevent="startDrag"
          @mousemove.stop.prevent="onDrag"
          @mouseup.stop.prevent="endDrag"
        >
          <text>{{ success ? '✓' : '›' }}</text>
        </view>
        <slider
          class="captcha-native-slider"
          :value="sliderPercent"
          :disabled="loading || checking || !captcha.token || success"
          min="0"
          max="100"
          step="1"
          block-size="44"
          block-color="rgba(255,255,255,0)"
          activeColor="rgba(255,255,255,0)"
          backgroundColor="rgba(255,255,255,0)"
          @changing="onNativeSliderChanging"
          @change="onNativeSliderChange"
        />
        <text class="captcha-track-text">
          {{ success ? '验证通过' : checking ? '正在校验...' : '按住滑块拖动完成拼图' }}
        </text>
      </view>

      <view class="captcha-actions">
        <text class="captcha-tip">{{ errorText || '验证通过后将继续登录或注册' }}</text>
        <view class="captcha-refresh" @click="loadCaptcha">换一张</view>
      </view>
    </view>
  </view>
</template>

<script setup>
import CryptoJS from 'crypto-js'
import { computed, getCurrentInstance, nextTick, onUnmounted, reactive, ref, watch } from 'vue'
import { checkCaptcha, getCaptcha } from '../api/modules/auth'

const props = defineProps({
  visible: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['success', 'close'])

const instance = getCurrentInstance()
const sliderSize = 44
const baseImageWidth = 310
const basePointY = 5

const loading = ref(false)
const checking = ref(false)
const dragging = ref(false)
const success = ref(false)
const dragX = ref(0)
const sliderPercent = ref(0)
const startX = ref(0)
const startDragX = ref(0)
const canvasWidth = ref(baseImageWidth)
const errorText = ref('')
let nativeTrackEl = null

const captcha = reactive({
  token: '',
  secretKey: '',
  originalImageBase64: '',
  jigsawImageBase64: ''
})

const trackWidth = computed(() => Math.max(canvasWidth.value - sliderSize, 0))
const progressWidth = computed(() => `${Math.min(dragX.value + sliderSize, canvasWidth.value)}px`)
const normalizedDragX = computed(() => {
  if (!canvasWidth.value) return dragX.value
  return Number(((dragX.value * baseImageWidth) / canvasWidth.value).toFixed(2))
})

const normalizeCaptchaResponse = (raw) => {
  const payload = raw?.code === 200 && raw?.data ? raw.data : raw
  const repData = payload?.repData || payload?.data || (
    payload?.token || payload?.captchaVerification || payload?.originalImageBase64 ? payload : null
  )
  return {
    ok: payload?.repCode === '0000' || payload?.success === true || raw?.code === 200,
    message: payload?.repMsg || payload?.message || raw?.message,
    data: repData
  }
}

const imageSrc = (source) => {
  if (!source) return ''
  const value = String(source).trim()
  if (value.startsWith('data:image') || value.startsWith('http') || value.startsWith('/')) {
    return value
  }
  return `data:image/png;base64,${value}`
}

const aesEncrypt = (plainText) => {
  const key = CryptoJS.enc.Utf8.parse(captcha.secretKey)
  const source = CryptoJS.enc.Utf8.parse(plainText)
  return CryptoJS.AES.encrypt(source, key, {
    mode: CryptoJS.mode.ECB,
    padding: CryptoJS.pad.Pkcs7
  }).toString()
}

const encryptPoint = (point) => aesEncrypt(JSON.stringify(point))
const buildCaptchaVerification = (point) => aesEncrypt(`${captcha.token}---${JSON.stringify(point)}`)

const updateCanvasWidth = () => new Promise((resolve) => {
  nextTick(() => {
    uni.createSelectorQuery()
      .in(instance?.proxy)
      .select('.captcha-canvas')
      .boundingClientRect((rect) => {
        if (rect?.width) canvasWidth.value = rect.width
        resolve()
      })
      .exec()
  })
})

const resetDrag = () => {
  dragX.value = 0
  sliderPercent.value = 0
  startX.value = 0
  startDragX.value = 0
  dragging.value = false
  success.value = false
}

const resetCaptcha = () => {
  resetDrag()
  Object.assign(captcha, {
    token: '',
    secretKey: '',
    originalImageBase64: '',
    jigsawImageBase64: ''
  })
}

const showError = (message) => {
  uni.showToast({
    title: message,
    icon: 'none',
    duration: 2200
  })
}

const loadCaptcha = async () => {
  if (!props.visible || loading.value || checking.value) return
  loading.value = true
  errorText.value = ''
  resetDrag()
  try {
    await updateCanvasWidth()
    const raw = await getCaptcha({
      captchaType: 'blockPuzzle',
      clientUid: `uniapp-${Date.now()}-${Math.random().toString(16).slice(2)}`
    })
    const normalized = normalizeCaptchaResponse(raw)
    if (!normalized.ok || !normalized.data) {
      throw new Error(normalized.message || '验证码加载失败')
    }
    Object.assign(captcha, {
      token: normalized.data.token || '',
      secretKey: normalized.data.secretKey || '',
      originalImageBase64: normalized.data.originalImageBase64 || '',
      jigsawImageBase64: normalized.data.jigsawImageBase64 || ''
    })
    await updateCanvasWidth()
  } catch (err) {
    resetCaptcha()
    errorText.value = err?.message || '验证码加载失败，请稍后重试'
    showError(errorText.value)
  } finally {
    loading.value = false
  }
}

const getClientX = (event) => {
  const touch = event?.touches?.[0] || event?.changedTouches?.[0]
  return touch?.clientX ?? event?.clientX ?? event?.detail?.x ?? 0
}

const handleDocumentMove = (event) => {
  if (dragging.value) event?.preventDefault?.()
  onDrag(event)
}

const handleDocumentEnd = (event) => {
  if (dragging.value) event?.preventDefault?.()
  endDrag()
}

const detachDocumentListeners = () => {
  if (typeof document === 'undefined') return
  document.removeEventListener('pointermove', handleDocumentMove)
  document.removeEventListener('pointerup', handleDocumentEnd)
  document.removeEventListener('mousemove', handleDocumentMove)
  document.removeEventListener('mouseup', handleDocumentEnd)
  document.removeEventListener('touchmove', handleDocumentMove)
  document.removeEventListener('touchend', handleDocumentEnd)
}

const attachDocumentListeners = () => {
  if (typeof document === 'undefined') return
  detachDocumentListeners()
  document.addEventListener('pointermove', handleDocumentMove)
  document.addEventListener('pointerup', handleDocumentEnd)
  document.addEventListener('mousemove', handleDocumentMove)
  document.addEventListener('mouseup', handleDocumentEnd)
  document.addEventListener('touchmove', handleDocumentMove, { passive: false })
  document.addEventListener('touchend', handleDocumentEnd)
}

const handleNativeStart = (event) => {
  event?.preventDefault?.()
  startDrag(event)
}

const handleGlobalNativeStart = (event) => {
  if (!event?.target?.closest?.('.captcha-track')) return
  handleNativeStart(event)
}

const detachNativeDomListeners = () => {
  if (!nativeTrackEl) return
  nativeTrackEl.removeEventListener('pointerdown', handleNativeStart)
  nativeTrackEl.removeEventListener('mousedown', handleNativeStart)
  nativeTrackEl.removeEventListener('touchstart', handleNativeStart)
  if (typeof document !== 'undefined') {
    document.removeEventListener('pointerdown', handleGlobalNativeStart, true)
    document.removeEventListener('mousedown', handleGlobalNativeStart, true)
    document.removeEventListener('touchstart', handleGlobalNativeStart, true)
  }
  nativeTrackEl = null
}

const attachNativeDomListeners = () => {
  if (typeof document === 'undefined') return
  nextTick(() => {
    const panel = document.querySelector('.captcha-panel')
    const nextTrack = panel?.querySelector('.captcha-track')
    if (!nextTrack || nextTrack === nativeTrackEl) return
    detachNativeDomListeners()
    nativeTrackEl = nextTrack
    nativeTrackEl.addEventListener('pointerdown', handleNativeStart)
    nativeTrackEl.addEventListener('mousedown', handleNativeStart)
    nativeTrackEl.addEventListener('touchstart', handleNativeStart, { passive: false })
    document.addEventListener('pointerdown', handleGlobalNativeStart, true)
    document.addEventListener('mousedown', handleGlobalNativeStart, true)
    document.addEventListener('touchstart', handleGlobalNativeStart, { capture: true, passive: false })
  })
}

const startDrag = (event) => {
  if (dragging.value || loading.value || checking.value || !captcha.token || success.value) return
  updateCanvasWidth()
  dragging.value = true
  startX.value = getClientX(event)
  startDragX.value = dragX.value
  attachDocumentListeners()
}

const onDrag = (event) => {
  if (!dragging.value) return
  const nextX = startDragX.value + getClientX(event) - startX.value
  dragX.value = Math.min(Math.max(nextX, 0), trackWidth.value)
  sliderPercent.value = trackWidth.value ? Math.round((dragX.value / trackWidth.value) * 100) : 0
}

const syncDragFromPercent = (value) => {
  updateCanvasWidth()
  const safeValue = Math.min(Math.max(Number(value) || 0, 0), 100)
  sliderPercent.value = safeValue
  dragX.value = Math.min(Math.max((trackWidth.value * safeValue) / 100, 0), trackWidth.value)
}

const onNativeSliderChanging = (event) => {
  if (loading.value || checking.value || !captcha.token || success.value) return
  dragging.value = true
  syncDragFromPercent(event?.detail?.value)
}

const onNativeSliderChange = async (event) => {
  if (loading.value || checking.value || !captcha.token || success.value) return
  syncDragFromPercent(event?.detail?.value)
  dragging.value = false
  detachDocumentListeners()
  if (dragX.value < 10) {
    resetDrag()
    return
  }
  await checkPuzzle()
}

const checkPuzzle = async () => {
  checking.value = true
  errorText.value = ''
  try {
    const point = {
      x: normalizedDragX.value,
      y: basePointY
    }
    const raw = await checkCaptcha({
      captchaType: 'blockPuzzle',
      token: captcha.token,
      pointJson: encryptPoint(point)
    })
    const normalized = normalizeCaptchaResponse(raw)
    if (!normalized.ok || normalized.data?.result === false) {
      throw new Error(normalized.message || '拼图位置不正确')
    }
    success.value = true
    uni.showToast({ title: '验证通过', icon: 'success', duration: 900 })
    setTimeout(() => {
      emit('success', buildCaptchaVerification(point))
    }, 180)
  } catch (err) {
    errorText.value = err?.message || '验证失败，请重新拖动'
    showError(errorText.value)
    await loadCaptcha()
  } finally {
    checking.value = false
  }
}

const endDrag = async () => {
  detachDocumentListeners()
  if (!dragging.value) return
  dragging.value = false
  if (dragX.value < 10) {
    resetDrag()
    return
  }
  await checkPuzzle()
}

const cancelMouseDrag = () => {
  if (typeof document !== 'undefined') return
  dragging.value = false
}

const close = () => {
  if (checking.value) return
  emit('close')
}

watch(
  () => props.visible,
  (visible) => {
    if (visible) {
      attachNativeDomListeners()
      loadCaptcha()
    } else {
      detachDocumentListeners()
      detachNativeDomListeners()
      resetCaptcha()
      errorText.value = ''
    }
  }
)

onUnmounted(() => {
  detachDocumentListeners()
  detachNativeDomListeners()
})
</script>

<style scoped lang="scss">
.captcha-mask {
  position: fixed;
  inset: 0;
  z-index: 999;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40rpx;
  background: rgba(36, 16, 79, 0.34);
  backdrop-filter: blur(10px);
}

.captcha-panel {
  width: min(650rpx, 100%);
  padding: 28rpx;
  border: 5rpx solid #34205f;
  border-radius: 34rpx;
  background:
    radial-gradient(circle at 0% 0%, rgba(139, 99, 255, 0.12), transparent 36%),
    linear-gradient(180deg, #fffdfa 0%, #fff6ed 100%);
  box-shadow: 0 20rpx 0 rgba(52, 32, 95, 0.12), 0 34rpx 70rpx rgba(36, 16, 79, 0.22);
}

.captcha-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 20rpx;
  margin-bottom: 22rpx;
}

.captcha-kicker,
.captcha-title {
  display: block;
}

.captcha-kicker {
  color: #8b63ff;
  font-size: 18rpx;
  font-weight: 900;
  letter-spacing: 4rpx;
}

.captcha-title {
  margin-top: 8rpx;
  color: #24104f;
  font-size: 34rpx;
  font-weight: 900;
}

.captcha-close {
  display: grid;
  width: 54rpx;
  height: 54rpx;
  place-items: center;
  border: 3rpx solid rgba(52, 32, 95, 0.25);
  border-radius: 50%;
  color: #24104f;
  font-size: 36rpx;
  font-weight: 700;
  line-height: 1;
}

.captcha-canvas {
  position: relative;
  width: 100%;
  aspect-ratio: 2 / 1;
  overflow: hidden;
  border: 3rpx solid #34205f;
  border-radius: 24rpx;
  background: #f3edff;
}

.captcha-image,
.captcha-jigsaw {
  position: absolute;
  top: 0;
  left: 0;
  user-select: none;
  pointer-events: none;
}

.captcha-image {
  width: 100%;
  height: 100%;
}

.captcha-jigsaw {
  z-index: 2;
  width: 15.2%;
  height: 100%;
  will-change: transform;
}

.captcha-state {
  position: absolute;
  inset: 0;
  display: grid;
  place-items: center;
  color: #69548f;
  font-size: 24rpx;
  font-weight: 800;
}

.captcha-track {
  position: relative;
  display: flex;
  align-items: center;
  width: 100%;
  height: 92rpx;
  margin-top: 24rpx;
  overflow: hidden;
  border: 3rpx solid #eadcf6;
  border-radius: 24rpx;
  background: linear-gradient(180deg, #fbf8ff 0%, #f4eeff 100%);
  color: #6c5c88;
  font-size: 26rpx;
  font-weight: 900;
  touch-action: none;
}

.captcha-progress {
  position: absolute;
  inset: 0 auto 0 0;
  z-index: 0;
  border-radius: inherit;
  background: linear-gradient(90deg, rgba(139, 99, 255, 0.28), rgba(255, 128, 111, 0.26));
}

.captcha-slider {
  position: absolute;
  top: 7rpx;
  left: 7rpx;
  z-index: 2;
  display: grid;
  width: 76rpx;
  height: 76rpx;
  place-items: center;
  border: 4rpx solid #34205f;
  border-radius: 20rpx;
  background: linear-gradient(135deg, #8b63ff, #a978ff);
  box-shadow: 0 7rpx 0 rgba(52, 32, 95, 0.14);
  color: #ffffff;
  font-size: 42rpx;
  font-weight: 900;
  line-height: 1;
}

.captcha-native-slider {
  position: absolute;
  inset: 0;
  z-index: 5;
  width: 100%;
  height: 100%;
  margin: 0;
  opacity: 0;
  touch-action: none;
}

.captcha-track.success .captcha-slider {
  background: linear-gradient(135deg, #68d8b4, #8b63ff);
}

.captcha-track-text {
  position: absolute;
  inset: 0;
  z-index: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding-left: 96rpx;
  pointer-events: none;
}

.captcha-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18rpx;
  margin-top: 18rpx;
}

.captcha-tip {
  flex: 1;
  min-width: 0;
  color: #75648f;
  font-size: 22rpx;
  line-height: 1.5;
}

.captcha-refresh {
  display: grid;
  min-width: 118rpx;
  height: 52rpx;
  place-items: center;
  border: 3rpx solid #34205f;
  border-radius: 999rpx;
  color: #24104f;
  font-size: 22rpx;
  font-weight: 900;
  background: rgba(255, 255, 255, 0.72);
}
</style>
