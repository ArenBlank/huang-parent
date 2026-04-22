<template>
  <el-dialog
    :model-value="visible"
    width="390px"
    class="aj-captcha-dialog"
    :show-close="!checking"
    :close-on-click-modal="!checking"
    @open="loadCaptcha"
    @close="emit('close')"
  >
    <template #header>
      <div class="captcha-head">
        <p class="captcha-kicker">Admin Check</p>
        <strong>完成滑块拼图验证</strong>
      </div>
    </template>

    <div class="captcha-box" v-loading="loading">
      <div class="captcha-canvas" ref="canvasRef">
        <img
          v-if="captcha.originalImageBase64"
          class="captcha-image"
          :src="imageSrc(captcha.originalImageBase64)"
          alt="验证码背景"
          draggable="false"
        />
        <img
          v-if="captcha.jigsawImageBase64"
          class="captcha-jigsaw"
          :src="imageSrc(captcha.jigsawImageBase64)"
          :style="{ transform: `translateX(${dragX}px)` }"
          alt="滑块拼图"
          draggable="false"
        />
        <div v-if="!captcha.originalImageBase64 && !loading" class="captcha-empty">
          验证码加载失败，请刷新重试
        </div>
      </div>

      <div
        class="captcha-track"
        :class="{ 'is-dragging': dragging, 'is-success': success }"
      >
        <div class="captcha-progress" :style="{ width: `${dragX + sliderSize}px` }"></div>
        <div
          class="captcha-slider"
          :style="{ transform: `translateX(${dragX}px)` }"
          @pointerdown.prevent="startDrag"
        >
          <span>{{ success ? "✓" : "→" }}</span>
        </div>
        <span class="captcha-track-text">
          {{ success ? "验证通过" : "按住滑块拖动完成拼图" }}
        </span>
      </div>

      <div class="captcha-actions">
        <span class="captcha-tip">{{ errorText || "验证通过后将继续登录管理端" }}</span>
        <el-button size="small" link :disabled="checking" @click="loadCaptcha">换一张</el-button>
      </div>
    </div>
  </el-dialog>
</template>

<script setup>
import CryptoJS from "crypto-js"
import { computed, reactive, ref } from "vue"
import { ElMessage } from "element-plus"
import { adminClient } from "../api/client"

const props = defineProps({
  visible: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(["success", "close"])

const canvasRef = ref(null)
const loading = ref(false)
const checking = ref(false)
const dragging = ref(false)
const success = ref(false)
const dragX = ref(0)
const startX = ref(0)
const startDragX = ref(0)
const errorText = ref("")
const sliderSize = 44
const baseImageWidth = 310
const basePointY = 5.0

const captcha = reactive({
  token: "",
  secretKey: "",
  originalImageBase64: "",
  jigsawImageBase64: ""
})

const trackWidth = computed(() => {
  const width = canvasRef.value?.clientWidth || baseImageWidth
  return Math.max(width - sliderSize, 0)
})

const normalizedDragX = computed(() => {
  const renderedWidth = canvasRef.value?.clientWidth || baseImageWidth
  if (!renderedWidth) return dragX.value
  return Number(((dragX.value * baseImageWidth) / renderedWidth).toFixed(2))
})

const normalizeCaptchaResponse = (raw) => {
  const payload = raw?.code === 200 && raw?.data ? raw.data : raw
  const repData = payload?.repData || payload?.data || (
    payload?.token || payload?.captchaVerification || payload?.originalImageBase64 ? payload : null
  )
  return {
    ok: payload?.repCode === "0000" || payload?.success === true || raw?.code === 200,
    message: payload?.repMsg || payload?.message || raw?.message,
    data: repData
  }
}

const imageSrc = (source) => {
  if (!source) return ""
  const value = String(source).trim()
  if (value.startsWith("data:image") || value.startsWith("http") || value.startsWith("/")) {
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

const buildCaptchaVerification = (point) => {
  return aesEncrypt(`${captcha.token}---${JSON.stringify(point)}`)
}

const resetDrag = () => {
  dragX.value = 0
  startX.value = 0
  startDragX.value = 0
  dragging.value = false
  success.value = false
}

const loadCaptcha = async () => {
  if (!props.visible) return
  loading.value = true
  errorText.value = ""
  resetDrag()
  try {
    const { data } = await adminClient.post("/admin/auth/captcha/get", {
      captchaType: "blockPuzzle",
      clientUid: `admin-${Date.now()}-${Math.random().toString(16).slice(2)}`
    })
    const normalized = normalizeCaptchaResponse(data)
    if (!normalized.ok || !normalized.data) {
      throw new Error(normalized.message || "验证码加载失败")
    }
    Object.assign(captcha, {
      token: normalized.data.token || "",
      secretKey: normalized.data.secretKey || "",
      originalImageBase64: normalized.data.originalImageBase64 || "",
      jigsawImageBase64: normalized.data.jigsawImageBase64 || ""
    })
  } catch (err) {
    errorText.value = err?.message || "验证码加载失败，请稍后再试"
  } finally {
    loading.value = false
  }
}

const startDrag = (event) => {
  if (loading.value || checking.value || !captcha.token) return
  dragging.value = true
  startX.value = event.clientX
  startDragX.value = dragX.value
  window.addEventListener("pointermove", onDrag)
  window.addEventListener("pointerup", endDrag, { once: true })
}

const onDrag = (event) => {
  if (!dragging.value) return
  const nextX = startDragX.value + event.clientX - startX.value
  dragX.value = Math.min(Math.max(nextX, 0), trackWidth.value)
}

const endDrag = async () => {
  window.removeEventListener("pointermove", onDrag)
  if (!dragging.value) return
  dragging.value = false
  if (dragX.value < 8) return
  await checkCaptcha()
}

const checkCaptcha = async () => {
  checking.value = true
  errorText.value = ""
  try {
    const point = {
      x: normalizedDragX.value,
      y: basePointY
    }
    const pointJson = encryptPoint(point)
    const { data } = await adminClient.post("/admin/auth/captcha/check", {
      captchaType: "blockPuzzle",
      token: captcha.token,
      pointJson
    })
    const normalized = normalizeCaptchaResponse(data)
    if (!normalized.ok || normalized.data?.result === false) {
      throw new Error(normalized.message || "拼图位置不正确")
    }
    success.value = true
    emit("success", buildCaptchaVerification(point))
  } catch (err) {
    errorText.value = err?.message || "验证失败，请重试"
    ElMessage.warning(errorText.value)
    await loadCaptcha()
  } finally {
    checking.value = false
  }
}
</script>

<style scoped>
.captcha-head {
  display: grid;
  gap: 4px;
}

.captcha-kicker {
  margin: 0;
  color: #2563eb;
  font-size: 11px;
  font-weight: 900;
  letter-spacing: 0.16em;
  text-transform: uppercase;
}

.captcha-box {
  display: grid;
  gap: 14px;
  justify-items: center;
}

.captcha-canvas {
  position: relative;
  width: 310px;
  max-width: 100%;
  height: 155px;
  box-sizing: border-box;
  overflow: hidden;
  border: 2px solid #163d74;
  border-radius: 16px;
  background: #eef4ff;
}

.captcha-image,
.captcha-jigsaw {
  position: absolute;
  top: 0;
  left: 0;
  user-select: none;
  pointer-events: none;
  max-width: none !important;
  min-width: 0 !important;
  min-height: 0 !important;
  object-fit: fill !important;
}

.captcha-image {
  width: 310px !important;
  height: 155px !important;
}

.captcha-jigsaw {
  z-index: 2;
  width: 47px !important;
  height: 155px !important;
  transform-origin: left center;
  will-change: transform;
}

.captcha-empty {
  height: 100%;
  display: grid;
  place-items: center;
  color: #64748b;
  font-weight: 800;
}

.captcha-track {
  position: relative;
  width: 310px;
  max-width: 100%;
  height: 48px;
  line-height: 48px;
  box-sizing: border-box;
  display: flex;
  align-items: center;
  border: 2px solid #163d74;
  border-radius: 999px;
  background: #ffffff;
  overflow: hidden;
  touch-action: none;
  box-shadow: 0 6px 0 rgba(15, 23, 42, 0.08);
}

.captcha-track.is-dragging {
  cursor: default;
}

.captcha-progress {
  position: absolute;
  inset: 0 auto 0 0;
  z-index: 0;
  border-radius: 999px;
  background: linear-gradient(90deg, rgba(37, 99, 235, 0.18), rgba(96, 165, 250, 0.36));
}

.captcha-slider {
  position: absolute;
  top: 2px;
  left: 2px;
  z-index: 2;
  width: 44px;
  height: 44px;
  line-height: 44px;
  box-sizing: border-box;
  display: flex;
  align-items: center;
  justify-content: center;
  line-height: 1;
  flex: 0 0 44px;
  border: 2px solid #163d74;
  border-radius: 999px;
  background: #2563eb;
  color: #ffffff;
  font-weight: 900;
  cursor: grab;
  touch-action: none;
  box-shadow: 0 4px 0 rgba(15, 23, 42, 0.14);
}

.captcha-track.is-dragging .captcha-slider {
  cursor: grabbing;
}

.captcha-slider span {
  display: block;
  line-height: 1;
  font-size: 20px;
  transform: translateY(-1px);
}

.captcha-track.is-success .captcha-slider {
  background: #10b981;
  color: #0f172a;
}

.captcha-track-text {
  position: absolute;
  inset: 0;
  z-index: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0 18px 0 60px;
  line-height: 48px;
  text-align: center;
  color: #64748b;
  font-size: 13px;
  font-weight: 800;
  pointer-events: none;
}

.captcha-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 310px;
  max-width: 100%;
  gap: 12px;
}

.captcha-tip {
  color: #64748b;
  font-size: 12px;
}

:global(.aj-captcha-dialog) {
  border-radius: 18px !important;
  overflow: hidden;
}

:global(.aj-captcha-dialog .el-dialog__body) {
  padding-top: 8px;
}
</style>
