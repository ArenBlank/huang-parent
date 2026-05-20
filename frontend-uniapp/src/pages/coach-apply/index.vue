<template>
  <view class="coach-page">
    <view class="page-glow page-glow--left"></view>
    <view class="page-glow page-glow--right"></view>

    <view class="topbar">
      <button class="icon-button" aria-label="返回" hover-class="none" @click="goBack">
        <image class="top-icon" :src="icons.back" mode="aspectFit" />
      </button>
      <text class="topbar-title">教练申请</text>
      <button class="guide-button" hover-class="none" @click="showGuide">
        <image :src="icons.clipboard" mode="aspectFit" />
        <text>申请指南</text>
      </button>
    </view>

    <view class="hero">
      <view class="hero-cloud hero-cloud--one"></view>
      <view class="hero-cloud hero-cloud--two"></view>
      <view class="hero-hill hero-hill--back"></view>
      <view class="hero-hill hero-hill--front"></view>

      <view class="status-panel">
        <view class="status-head">
          <view class="status-icon" :class="statusTone">
            <image :src="statusIcon" mode="aspectFit" />
          </view>
          <view class="status-copy">
            <text class="status-title" :class="statusTone">{{ statusTitle }}</text>
            <text class="status-sub">{{ statusHint }}</text>
          </view>
        </view>

        <view class="step-line">
          <view
            v-for="(step, index) in steps"
            :key="step.label"
            class="step-item"
            :class="{ active: index <= activeStepIndex, current: index === activeStepIndex }"
          >
            <view class="step-dot">
              <image v-if="index <= activeStepIndex" :src="icons.checkWhite" mode="aspectFit" />
            </view>
            <text class="step-label">{{ step.label }}</text>
            <text class="step-note">{{ step.note }}</text>
          </view>
        </view>
      </view>

      <view class="metric-column">
        <view class="hero-metric">
          <view class="hero-metric-icon purple">
            <image :src="icons.wallet" mode="aspectFit" />
          </view>
          <view>
            <text class="hero-metric-label">课时价格（元/节）</text>
            <text class="hero-metric-value">¥{{ displayPrice }}</text>
            <text class="hero-metric-note">由 /app/coach 接口提交</text>
          </view>
        </view>
        <view class="hero-metric">
          <view class="hero-metric-icon blue">
            <image :src="icons.briefcase" mode="aspectFit" />
          </view>
          <view>
            <text class="hero-metric-label">从业年限</text>
            <text class="hero-metric-value">{{ form.years || 0 }} 年</text>
            <text class="hero-metric-note">认证审核关键字段</text>
          </view>
        </view>
      </view>

      <view class="coach-figure">
        <view class="coach-head"></view>
        <view class="coach-hair"></view>
        <view class="coach-body">
          <text>COACH</text>
        </view>
        <view class="coach-arm coach-arm--left"></view>
        <view class="coach-arm coach-arm--right"></view>
      </view>
    </view>

    <view class="form-card">
      <view class="section-head">
        <view>
          <text class="section-title">申请资料</text>
          <text class="section-sub">表单严格按后端 DTO 提交：简介、擅长领域、年限、价格。</text>
        </view>
        <button class="sample-button" hover-class="none" @click="fillSample">
          <image :src="icons.magic" mode="aspectFit" />
          <text>填充示例</text>
        </button>
      </view>

      <view class="field-block">
        <text class="field-label">个人简介</text>
        <view class="textarea-wrap">
          <textarea
            v-model.trim="form.bio"
            class="bio-textarea"
            maxlength="500"
            placeholder="介绍你的训练背景、服务风格和擅长方向"
          />
          <text class="textarea-count">{{ form.bio.length }}/500</text>
        </view>
      </view>

      <view class="field-block">
        <text class="field-label">擅长领域（可多选）</text>
        <view class="tag-cloud">
          <button
            v-for="tag in expertiseOptions"
            :key="tag"
            class="tag-chip"
            :class="{ active: expertiseTags.includes(tag) }"
            hover-class="none"
            @click="toggleExpertise(tag)"
          >
            <text>{{ tag }}</text>
            <image v-if="expertiseTags.includes(tag)" :src="icons.close" mode="aspectFit" />
          </button>
        </view>
        <view class="manual-field">
          <image :src="icons.plus" mode="aspectFit" />
          <input
            v-model.trim="manualExpertise"
            class="manual-input"
            placeholder="补充领域后点添加"
            confirm-type="done"
            @confirm="addManualExpertise"
          />
          <button hover-class="none" @click="addManualExpertise">添加</button>
        </view>
      </view>

      <view class="field-grid">
        <view class="field-block">
          <text class="field-label">从业年限</text>
          <view class="number-stepper">
            <button hover-class="none" @click="changeYears(-1)">
              <image :src="icons.minus" mode="aspectFit" />
            </button>
            <input v-model.number="form.years" type="number" class="number-input" />
            <text>年</text>
            <button hover-class="none" @click="changeYears(1)">
              <image :src="icons.plus" mode="aspectFit" />
            </button>
          </view>
        </view>

        <view class="field-block">
          <text class="field-label">课时价格</text>
          <view class="price-input">
            <text>¥</text>
            <input v-model.number="form.price" type="digit" placeholder="199" />
            <text>/节</text>
          </view>
          <text class="field-tip">建议参考市场价设置合理价格</text>
        </view>
      </view>
    </view>

    <view class="preview-card">
      <view class="section-head compact">
        <view>
          <text class="section-title">教练档案预览</text>
          <text class="section-sub">这里用你的真实用户资料和当前申请字段生成预览。</text>
        </view>
        <text class="status-pill" :class="statusTone">{{ statusTitle }}</text>
      </view>

      <view class="profile-preview">
        <view class="avatar-wrap">
          <image v-if="profileAvatar" class="avatar-img" :src="profileAvatar" mode="aspectFill" />
          <image v-else class="avatar-icon" :src="icons.userTie" mode="aspectFit" />
        </view>
        <view class="preview-main">
          <view class="preview-head">
            <text>{{ displayName }}</text>
            <text>¥{{ displayPrice }}/节</text>
          </view>
          <view class="preview-tags">
            <text v-for="tag in previewTags" :key="tag">{{ tag }}</text>
          </view>
          <view class="preview-line">
            <image :src="icons.certificate" mode="aspectFit" />
            <text>{{ form.years || 0 }}年 · 平台教练认证资料</text>
          </view>
          <view class="preview-line">
            <image :src="icons.star" mode="aspectFit" />
            <text>{{ form.bio || '填写简介后，这里会展示给审核端参考。' }}</text>
          </view>
        </view>
      </view>
    </view>

    <view class="record-section">
      <view class="section-head compact">
        <view>
          <text class="section-title">申请记录</text>
          <text class="section-sub">保留最近一次申请状态与更新时间。</text>
        </view>
        <button class="refresh-button" :disabled="loading" hover-class="none" @click="loadApply">
          <image :class="{ spinning: loading }" :src="icons.refresh" mode="aspectFit" />
        </button>
      </view>

      <view v-if="!applyInfo" class="record-card empty">
        <image :src="icons.file" mode="aspectFit" />
        <text>暂无申请记录</text>
        <text>提交后会显示审核状态和最近更新时间。</text>
      </view>
      <view v-else class="record-card">
        <view class="record-top">
          <view>
            <text class="record-title">本次申请</text>
            <text class="record-sub">申请编号 {{ applyInfo.profileId || '-' }}</text>
          </view>
          <text class="status-pill" :class="statusTone">{{ statusTitle }}</text>
        </view>
        <view class="record-lines">
          <view>
            <text>申请时间</text>
            <text>{{ formatDateTime(applyInfo.updateTime) }}</text>
          </view>
          <view>
            <text>擅长领域</text>
            <text>{{ applyInfo.expertise || '-' }}</text>
          </view>
          <view>
            <text>课时价格</text>
            <text>¥{{ formatAmount(applyInfo.price) }} / 节</text>
          </view>
          <view>
            <text>从业年限</text>
            <text>{{ applyInfo.years ?? '-' }} 年</text>
          </view>
        </view>
      </view>
    </view>

    <view class="secure-tip">
      <image :src="icons.shield" mode="aspectFit" />
      <text>我们将严格保护您的信息安全，仅用于教练审核</text>
    </view>

    <button class="submit-button" :disabled="submitting" hover-class="none" @click="submit">
      {{ submitting ? '提交中...' : submitLabel }}
    </button>
  </view>
</template>

<script setup>
import {
  faAward,
  faBriefcase,
  faCertificate,
  faChevronLeft,
  faCircleCheck,
  faClipboardList,
  faFileSignature,
  faMinus,
  faPenToSquare,
  faPlus,
  faRotateRight,
  faShieldHalved,
  faStar,
  faUserTie,
  faWallet,
  faWandMagicSparkles,
  faXmark
} from '@fortawesome/free-solid-svg-icons'
import { computed, reactive, ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { storeToRefs } from 'pinia'
import { applyCoach, getMyCoachApplication } from '../../api/modules/coach'
import { useAppAuthStore } from '../../stores/auth'
import { ensureLogin } from '../../utils/authGuard'
import { toPublicUrl } from '../../utils/mediaUrl'
import { goBack as backToPrevious } from '../../utils/navigation'

const faIcon = (definition, color = '#24104f') => {
  const [width, height, , , pathData] = definition.icon
  const paths = Array.isArray(pathData)
    ? pathData.map((path) => `<path fill="${color}" d="${path}"/>`).join('')
    : `<path fill="${color}" d="${pathData}"/>`
  const svg = `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 ${width} ${height}">${paths}</svg>`
  return `data:image/svg+xml;charset=utf-8,${encodeURIComponent(svg)}`
}

const icons = {
  award: faIcon(faAward, '#8b63ff'),
  back: faIcon(faChevronLeft),
  briefcase: faIcon(faBriefcase, '#4cb2ff'),
  certificate: faIcon(faCertificate, '#8b63ff'),
  check: faIcon(faCircleCheck, '#32c46f'),
  checkWhite: faIcon(faCircleCheck, '#ffffff'),
  clipboard: faIcon(faClipboardList),
  close: faIcon(faXmark, '#8b63ff'),
  edit: faIcon(faPenToSquare, '#ff806f'),
  file: faIcon(faFileSignature, '#8b63ff'),
  magic: faIcon(faWandMagicSparkles, '#ffffff'),
  minus: faIcon(faMinus, '#24104f'),
  plus: faIcon(faPlus, '#8b63ff'),
  refresh: faIcon(faRotateRight),
  shield: faIcon(faShieldHalved, '#8b63ff'),
  star: faIcon(faStar, '#ffb845'),
  userTie: faIcon(faUserTie, '#24104f'),
  wallet: faIcon(faWallet, '#8b63ff')
}

const authStore = useAppAuthStore()
const { user } = storeToRefs(authStore)

const appBase = (import.meta.env.VITE_APP_BASE_URL || '').replace(/\/app\/?$/, '').replace(/\/$/, '')
const expertiseOptions = ['增肌塑形', '减脂燃脂', '体态改善', '力量提升', '运动康复', '基础体能']
const submitting = ref(false)
const loading = ref(false)
const applyInfo = ref(null)
const manualExpertise = ref('')

const form = reactive({
  bio: '',
  expertise: '',
  years: 2,
  price: 199
})

const statusTitle = computed(() => {
  const status = Number(applyInfo.value?.certStatus)
  if (status === 1) return '已通过'
  if (status === 2) return '已驳回'
  if (status === 0) return '审核中'
  return applyInfo.value?.certStatusText || '待提交'
})

const statusTone = computed(() => {
  const status = Number(applyInfo.value?.certStatus)
  if (status === 1) return 'success'
  if (status === 2) return 'danger'
  if (status === 0) return 'warning'
  return 'plain'
})

const statusIcon = computed(() => {
  const status = Number(applyInfo.value?.certStatus)
  if (status === 1) return icons.check
  if (status === 2) return icons.edit
  if (status === 0) return icons.check
  return icons.award
})

const statusHint = computed(() => {
  const status = Number(applyInfo.value?.certStatus)
  if (status === 1) return '认证已完成，可作为平台教练参与后续预约业务。'
  if (status === 2) return '申请未通过，请修改资料后重新提交审核。'
  if (status === 0) return '我们将在 1-2 个工作日内完成审核。'
  return '提交资料后进入平台审核流程，结果会同步到这里。'
})

const activeStepIndex = computed(() => {
  const status = Number(applyInfo.value?.certStatus)
  if (status === 1) return 3
  if (status === 0 || status === 2) return 1
  return -1
})

const steps = computed(() => {
  const status = Number(applyInfo.value?.certStatus)
  return [
    { label: '提交申请', note: applyInfo.value?.updateTime ? formatDateTime(applyInfo.value.updateTime).slice(5, 10) : '待提交' },
    { label: '资料审核', note: status === 0 ? '进行中' : status === 2 ? '需修改' : status === 1 ? '已通过' : '待审核' },
    { label: '平台审核', note: status === 1 ? '已通过' : '待审核' },
    { label: '审核完成', note: status === 1 ? '已完成' : '待完成' }
  ]
})

const expertiseTags = computed(() =>
  String(form.expertise || '')
    .split(',')
    .map((item) => item.trim())
    .filter(Boolean)
)

const previewTags = computed(() => (expertiseTags.value.length ? expertiseTags.value.slice(0, 4) : ['待填写领域']))
const displayName = computed(() => user.value?.nickname || user.value?.username || '训练用户')
const displayPrice = computed(() => formatAmount(form.price))
const submitLabel = computed(() => (applyInfo.value ? '更新申请资料' : '提交申请'))

const profileAvatar = computed(() => {
  return toPublicUrl(user.value?.avatar || user.value?.avatarUrl)
})

const setExpertiseTags = (tags) => {
  form.expertise = [...new Set(tags.map((item) => String(item).trim()).filter(Boolean))].join(',')
}

const toggleExpertise = (tag) => {
  const current = expertiseTags.value
  if (current.includes(tag)) {
    setExpertiseTags(current.filter((item) => item !== tag))
    return
  }
  setExpertiseTags([...current, tag])
}

const addManualExpertise = () => {
  const value = manualExpertise.value.trim()
  if (!value) return
  setExpertiseTags([...expertiseTags.value, value])
  manualExpertise.value = ''
}

const changeYears = (step) => {
  const next = Number(form.years || 0) + step
  form.years = Math.min(60, Math.max(0, next))
}

const fillSample = () => {
  form.bio = '国家职业健身教练，擅长减脂增肌与动作矫正，注重训练计划和饮食建议协同。'
  form.expertise = '减脂燃脂,增肌塑形,力量提升'
  form.years = 3
  form.price = 199
}

const loadApply = async () => {
  try {
    loading.value = true
    const { data } = await getMyCoachApplication()
    applyInfo.value = data || null
    if (data) {
      form.bio = data.bio || form.bio
      form.expertise = data.expertise || form.expertise
      form.years = Number(data.years ?? form.years)
      form.price = Number(data.price ?? form.price)
    }
  } finally {
    loading.value = false
  }
}

const validateForm = () => {
  if (!form.bio.trim()) {
    uni.showToast({ title: '请填写个人简介', icon: 'none' })
    return false
  }
  if (form.bio.trim().length > 500) {
    uni.showToast({ title: '个人简介不能超过 500 字', icon: 'none' })
    return false
  }
  if (!form.expertise.trim()) {
    uni.showToast({ title: '请选择或填写擅长领域', icon: 'none' })
    return false
  }
  if (form.expertise.trim().length > 200) {
    uni.showToast({ title: '擅长领域不能超过 200 字', icon: 'none' })
    return false
  }
  if (Number(form.years) < 0 || Number(form.years) > 60) {
    uni.showToast({ title: '从业年限需在 0-60 年内', icon: 'none' })
    return false
  }
  if (Number(form.price) < 0) {
    uni.showToast({ title: '课时价格不能小于 0', icon: 'none' })
    return false
  }
  return true
}

const submit = async () => {
  if (!validateForm()) return
  try {
    submitting.value = true
    await applyCoach({
      bio: form.bio.trim(),
      expertise: form.expertise.trim(),
      years: Number(form.years || 0),
      price: Number(form.price || 0)
    })
    uni.showToast({ title: '申请已提交', icon: 'success' })
    await loadApply()
  } finally {
    submitting.value = false
  }
}

const showGuide = () => {
  uni.showModal({
    title: '申请指南',
    content: '当前后端仅接收个人简介、擅长领域、从业年限和课时价格。提交后可在本页查看审核状态。',
    showCancel: false,
    confirmText: '知道了'
  })
}

const formatAmount = (value) => {
  if (value === null || value === undefined || value === '') return '0'
  const amount = Number(value)
  if (!Number.isFinite(amount)) return String(value)
  return Number.isInteger(amount) ? String(amount) : amount.toFixed(2)
}

const formatDateTime = (value) => {
  if (!value) return '-'
  const raw = String(value)
  if (raw.includes('T')) {
    const [date, time = ''] = raw.split('T')
    return `${date} ${time.slice(0, 5)}`
  }
  return raw.length >= 16 ? raw.slice(0, 16) : raw
}

const goBack = () => {
  backToPrevious()
}

onShow(() => {
  if (ensureLogin()) {
    Promise.allSettled([authStore.fetchProfile(), loadApply()])
  }
})
</script>

<style scoped lang="scss">
.coach-page {
  position: relative;
  min-height: 100vh;
  padding: calc(var(--status-bar-height) + 28rpx) 28rpx 140rpx;
  overflow-x: hidden;
  background:
    radial-gradient(circle at 0 0, rgba(139, 99, 255, 0.18), transparent 34%),
    radial-gradient(circle at 100% 8%, rgba(255, 128, 111, 0.18), transparent 30%),
    linear-gradient(180deg, #fff7fb 0%, #fffaf4 52%, #fffdf9 100%);
  color: #24104f;
}

.page-glow {
  position: fixed;
  z-index: 0;
  width: 320rpx;
  height: 320rpx;
  border-radius: 999rpx;
  filter: blur(22rpx);
  opacity: 0.44;
  pointer-events: none;
}

.page-glow--left {
  left: -170rpx;
  top: 120rpx;
  background: #e8ddff;
}

.page-glow--right {
  right: -160rpx;
  top: 20rpx;
  background: #ffd8df;
}

.topbar,
.hero,
.form-card,
.preview-card,
.record-section,
.secure-tip,
.submit-button {
  position: relative;
  z-index: 1;
}

button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  margin: 0;
  padding: 0;
  border: 0;
  background: transparent;
  line-height: 1;
  box-sizing: border-box;
}

button::after {
  border: 0;
}

.topbar {
  display: grid;
  grid-template-columns: 64rpx 1fr 164rpx;
  align-items: center;
  min-height: 70rpx;
  margin-bottom: 28rpx;
}

.topbar-title {
  color: #24104f;
  font-size: 34rpx;
  font-weight: 950;
  text-align: center;
}

.icon-button {
  width: 64rpx;
  height: 64rpx;
}

.top-icon {
  width: 36rpx;
  height: 36rpx;
}

.guide-button {
  gap: 8rpx;
  height: 58rpx;
  border-radius: 999rpx;
  color: #24104f;
  font-size: 23rpx;
  font-weight: 950;
}

.guide-button image {
  width: 28rpx;
  height: 28rpx;
}

.hero {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 228rpx;
  gap: 18rpx;
  min-height: 340rpx;
  margin: 0 -28rpx;
  padding: 0 28rpx 28rpx;
  overflow: hidden;
}

.hero-cloud,
.hero-hill {
  position: absolute;
}

.hero-cloud {
  z-index: 0;
  border-radius: 999rpx;
  background: rgba(255, 255, 255, 0.78);
}

.hero-cloud--one {
  right: 184rpx;
  top: 62rpx;
  width: 98rpx;
  height: 28rpx;
}

.hero-cloud--two {
  right: 80rpx;
  top: 120rpx;
  width: 70rpx;
  height: 22rpx;
}

.hero-hill--back {
  right: -120rpx;
  bottom: 16rpx;
  width: 520rpx;
  height: 180rpx;
  border-radius: 100% 0 0 0;
  background: rgba(139, 99, 255, 0.25);
  transform: skewY(-8deg);
}

.hero-hill--front {
  right: -90rpx;
  bottom: -32rpx;
  width: 500rpx;
  height: 170rpx;
  border-radius: 100% 0 0 0;
  background: rgba(255, 185, 118, 0.46);
  transform: skewY(-11deg);
}

.status-panel,
.metric-column {
  position: relative;
  z-index: 1;
}

.status-panel {
  min-height: 286rpx;
  padding: 26rpx;
  border-radius: 28rpx;
  background: rgba(255, 255, 255, 0.94);
  box-shadow: 0 14rpx 34rpx rgba(52, 32, 95, 0.08);
}

.status-head {
  display: flex;
  gap: 18rpx;
}

.status-icon {
  display: grid;
  width: 86rpx;
  height: 86rpx;
  flex: 0 0 auto;
  place-items: center;
  border-radius: 50%;
}

.status-icon image {
  width: 48rpx;
  height: 48rpx;
}

.status-icon.success,
.status-icon.warning {
  background: #e6f9ed;
}

.status-icon.danger {
  background: #ffe5ec;
}

.status-icon.plain {
  background: #f0e7ff;
}

.status-copy {
  min-width: 0;
}

.status-title,
.status-sub {
  display: block;
}

.status-title {
  font-size: 48rpx;
  font-weight: 950;
  line-height: 1.06;
}

.status-title.success,
.success {
  color: #20a96c;
}

.status-title.warning,
.warning {
  color: #ff9f43;
}

.status-title.danger,
.danger {
  color: #ff6f61;
}

.status-title.plain,
.plain {
  color: #8b63ff;
}

.status-sub {
  margin-top: 12rpx;
  color: #6f6095;
  font-size: 24rpx;
  font-weight: 800;
  line-height: 1.45;
}

.step-line {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 10rpx;
  margin-top: 36rpx;
}

.step-item {
  position: relative;
  display: flex;
  align-items: center;
  flex-direction: column;
  gap: 7rpx;
  color: #8d7fa8;
  font-size: 20rpx;
  font-weight: 850;
  text-align: center;
}

.step-item::before {
  content: "";
  position: absolute;
  top: 17rpx;
  left: -50%;
  width: 100%;
  height: 4rpx;
  border-radius: 999rpx;
  background: #e7dfef;
}

.step-item:first-child::before {
  display: none;
}

.step-item.active::before {
  background: #9adfae;
}

.step-dot {
  position: relative;
  z-index: 1;
  display: grid;
  width: 34rpx;
  height: 34rpx;
  place-items: center;
  border: 6rpx solid #e7dfef;
  border-radius: 50%;
  background: #fff;
}

.step-item.active .step-dot {
  border-color: #9adfae;
  background: #4bc878;
}

.step-item.current .step-label {
  color: #24104f;
}

.step-dot image {
  width: 20rpx;
  height: 20rpx;
}

.step-label,
.step-note {
  display: block;
  line-height: 1.2;
}

.metric-column {
  display: flex;
  flex-direction: column;
  gap: 16rpx;
}

.hero-metric {
  min-height: 132rpx;
  padding: 18rpx;
  border-radius: 22rpx;
  background: rgba(255, 255, 255, 0.92);
  box-shadow: 0 12rpx 28rpx rgba(52, 32, 95, 0.08);
}

.hero-metric-icon {
  display: grid;
  width: 48rpx;
  height: 48rpx;
  place-items: center;
  border-radius: 50%;
}

.hero-metric-icon.purple {
  background: #f0e7ff;
}

.hero-metric-icon.blue {
  background: #e3f2ff;
}

.hero-metric-icon image {
  width: 28rpx;
  height: 28rpx;
}

.hero-metric-label,
.hero-metric-value,
.hero-metric-note {
  display: block;
}

.hero-metric-label {
  margin-top: 8rpx;
  color: #6f6095;
  font-size: 21rpx;
  font-weight: 900;
}

.hero-metric-value {
  margin-top: 7rpx;
  color: #24104f;
  font-size: 34rpx;
  font-weight: 950;
}

.hero-metric-note {
  margin-top: 6rpx;
  color: #8d7fa8;
  font-size: 18rpx;
  font-weight: 760;
}

.coach-figure {
  position: absolute;
  z-index: 2;
  right: 4rpx;
  bottom: -6rpx;
  width: 210rpx;
  height: 250rpx;
  pointer-events: none;
}

.coach-head {
  position: absolute;
  left: 74rpx;
  top: 22rpx;
  width: 58rpx;
  height: 64rpx;
  border: 4rpx solid #24104f;
  border-radius: 45% 45% 48% 48%;
  background: #ffd1b8;
}

.coach-hair {
  position: absolute;
  left: 60rpx;
  top: 12rpx;
  width: 92rpx;
  height: 48rpx;
  border: 4rpx solid #24104f;
  border-radius: 55% 45% 40% 35%;
  background: #1f1744;
}

.coach-body {
  position: absolute;
  left: 42rpx;
  top: 92rpx;
  display: grid;
  width: 124rpx;
  height: 96rpx;
  place-items: center;
  border: 4rpx solid #24104f;
  border-radius: 32rpx 32rpx 22rpx 22rpx;
  background: #333553;
}

.coach-body text {
  color: #ffffff;
  font-size: 22rpx;
  font-weight: 950;
  letter-spacing: 1rpx;
}

.coach-arm {
  position: absolute;
  top: 116rpx;
  width: 72rpx;
  height: 16rpx;
  border: 4rpx solid #24104f;
  border-radius: 999rpx;
  background: #ffd1b8;
}

.coach-arm--left {
  left: 12rpx;
  transform: rotate(24deg);
}

.coach-arm--right {
  right: 10rpx;
  transform: rotate(-24deg);
}

.form-card,
.preview-card,
.record-section {
  margin-top: 24rpx;
  padding: 28rpx;
  border: 2rpx solid rgba(52, 32, 95, 0.1);
  border-radius: 30rpx;
  background: rgba(255, 255, 255, 0.96);
  box-shadow: 0 12rpx 32rpx rgba(52, 32, 95, 0.08);
}

.section-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 18rpx;
}

.section-head.compact {
  align-items: center;
}

.section-title,
.section-sub {
  display: block;
}

.section-title {
  color: #24104f;
  font-size: 32rpx;
  font-weight: 950;
}

.section-sub {
  margin-top: 8rpx;
  color: #7b6f98;
  font-size: 23rpx;
  line-height: 1.45;
}

.sample-button,
.refresh-button {
  flex: 0 0 auto;
}

.sample-button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 10rpx;
  min-width: 146rpx;
  height: 56rpx;
  padding: 0 18rpx;
  border-radius: 999rpx;
  background: linear-gradient(110deg, #8b63ff 0%, #d987d5 52%, #ff806f 100%);
  color: #ffffff;
  font-size: 22rpx;
  font-weight: 950;
  line-height: 1;
  box-sizing: border-box;
}

.sample-button image {
  width: 26rpx;
  height: 26rpx;
}

.refresh-button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 58rpx;
  height: 58rpx;
  border: 2rpx solid rgba(52, 32, 95, 0.2);
  border-radius: 50%;
  background: #ffffff;
  box-shadow: 0 6rpx 0 rgba(52, 32, 95, 0.06);
}

.refresh-button image {
  width: 28rpx;
  height: 28rpx;
}

.field-block {
  margin-top: 26rpx;
}

.field-label {
  display: block;
  margin-bottom: 14rpx;
  color: #24104f;
  font-size: 25rpx;
  font-weight: 900;
}

.textarea-wrap {
  position: relative;
}

.bio-textarea {
  width: 100%;
  min-height: 164rpx;
  padding: 22rpx 22rpx 48rpx;
  border: 2rpx solid rgba(52, 32, 95, 0.12);
  border-radius: 24rpx;
  background: #fffdf9;
  color: #24104f;
  font-size: 25rpx;
  line-height: 1.55;
  box-sizing: border-box;
}

.textarea-count {
  position: absolute;
  right: 22rpx;
  bottom: 16rpx;
  color: #9b8ab8;
  font-size: 22rpx;
}

.tag-cloud {
  display: flex;
  flex-wrap: wrap;
  gap: 14rpx;
}

.tag-chip {
  gap: 8rpx;
  height: 54rpx;
  padding: 0 22rpx;
  border-radius: 999rpx;
  background: #f4efff;
  color: #8b63ff;
  font-size: 23rpx;
  font-weight: 900;
}

.tag-chip.active {
  border: 2rpx solid rgba(139, 99, 255, 0.55);
  background: #efe7ff;
}

.tag-chip image {
  width: 22rpx;
  height: 22rpx;
}

.manual-field {
  display: grid;
  grid-template-columns: 34rpx minmax(0, 1fr) 90rpx;
  align-items: center;
  gap: 12rpx;
  min-height: 66rpx;
  margin-top: 16rpx;
  padding: 0 18rpx;
  border: 2rpx solid rgba(52, 32, 95, 0.1);
  border-radius: 999rpx;
  background: #fffdf9;
}

.manual-field image {
  width: 28rpx;
  height: 28rpx;
}

.manual-input {
  height: 66rpx;
  color: #24104f;
  font-size: 24rpx;
}

.manual-field button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  height: 46rpx;
  border-radius: 999rpx;
  background: #f4efff;
  color: #8b63ff;
  font-size: 22rpx;
  font-weight: 900;
  line-height: 1;
  box-sizing: border-box;
}

.field-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20rpx;
}

.number-stepper,
.price-input {
  display: flex;
  align-items: center;
  min-height: 66rpx;
  border: 2rpx solid rgba(52, 32, 95, 0.1);
  border-radius: 22rpx;
  background: #fffdf9;
}

.number-stepper button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 66rpx;
  height: 66rpx;
}

.number-stepper image {
  width: 26rpx;
  height: 26rpx;
}

.number-input {
  flex: 1;
  height: 66rpx;
  color: #24104f;
  font-size: 26rpx;
  font-weight: 950;
  text-align: center;
}

.number-stepper > text {
  margin-right: 6rpx;
  color: #6f6095;
  font-size: 22rpx;
  font-weight: 850;
}

.price-input {
  gap: 10rpx;
  padding: 0 18rpx;
}

.price-input text {
  color: #24104f;
  font-size: 25rpx;
  font-weight: 950;
}

.price-input input {
  flex: 1;
  min-width: 0;
  height: 66rpx;
  color: #24104f;
  font-size: 26rpx;
  font-weight: 950;
}

.field-tip {
  display: block;
  margin-top: 10rpx;
  color: #8d7fa8;
  font-size: 22rpx;
}

.preview-card {
  background: linear-gradient(135deg, rgba(246, 240, 255, 0.98), rgba(255, 248, 250, 0.98));
}

.profile-preview {
  display: grid;
  grid-template-columns: 118rpx minmax(0, 1fr);
  gap: 20rpx;
  align-items: center;
  margin-top: 26rpx;
}

.avatar-wrap {
  display: grid;
  width: 108rpx;
  height: 108rpx;
  place-items: center;
  overflow: hidden;
  border: 4rpx solid #ffffff;
  border-radius: 50%;
  background: #f0e7ff;
  box-shadow: 0 10rpx 24rpx rgba(52, 32, 95, 0.12);
}

.avatar-img {
  width: 100%;
  height: 100%;
}

.avatar-icon {
  width: 50rpx;
  height: 50rpx;
}

.preview-main {
  min-width: 0;
}

.preview-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12rpx;
}

.preview-head text:first-child {
  overflow: hidden;
  color: #24104f;
  font-size: 31rpx;
  font-weight: 950;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.preview-head text:last-child {
  flex: 0 0 auto;
  color: #ff6f61;
  font-size: 26rpx;
  font-weight: 950;
}

.preview-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 12rpx;
  margin-top: 14rpx;
}

.preview-tags text {
  padding: 4rpx 12rpx;
  border-radius: 999rpx;
  background: #ffffff;
  color: #7b57f2;
  font-size: 21rpx;
  font-weight: 850;
}

.preview-line {
  display: flex;
  align-items: flex-start;
  gap: 8rpx;
  margin-top: 12rpx;
  color: #5f4a85;
  font-size: 23rpx;
  font-weight: 800;
  line-height: 1.42;
}

.preview-line image {
  width: 24rpx;
  height: 24rpx;
  margin-top: 3rpx;
  flex: 0 0 auto;
}

.preview-line text {
  flex: 1;
}

.status-pill {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 44rpx;
  padding: 0 22rpx;
  border-radius: 999rpx;
  background: #f0e7ff;
  font-size: 23rpx;
  font-weight: 950;
  white-space: nowrap;
}

.status-pill.success {
  background: #dcf8e7;
  color: #19a65f;
}

.status-pill.warning {
  background: #fff0cf;
  color: #c07604;
}

.status-pill.danger {
  background: #ffe6ed;
  color: #d94d77;
}

.status-pill.plain {
  background: #f0e7ff;
  color: #8b63ff;
}

.record-card {
  margin-top: 20rpx;
  padding: 24rpx;
  border: 2rpx solid rgba(52, 32, 95, 0.1);
  border-radius: 24rpx;
  background: #fffdf9;
}

.record-card.empty {
  display: flex;
  align-items: center;
  flex-direction: column;
  gap: 10rpx;
  color: #7b6f98;
  font-size: 24rpx;
  font-weight: 850;
  text-align: center;
}

.record-card.empty image {
  width: 54rpx;
  height: 54rpx;
}

.record-top {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 18rpx;
}

.record-title,
.record-sub {
  display: block;
}

.record-title {
  color: #24104f;
  font-size: 29rpx;
  font-weight: 950;
}

.record-sub {
  margin-top: 8rpx;
  color: #7b6f98;
  font-size: 22rpx;
}

.record-lines {
  display: flex;
  flex-direction: column;
  gap: 14rpx;
  margin-top: 20rpx;
}

.record-lines view {
  display: flex;
  justify-content: space-between;
  gap: 20rpx;
  color: #7b6f98;
  font-size: 24rpx;
  font-weight: 800;
}

.record-lines text:last-child {
  color: #24104f;
  text-align: right;
}

.secure-tip {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10rpx;
  margin-top: 26rpx;
  color: #8d7fa8;
  font-size: 23rpx;
  font-weight: 800;
}

.secure-tip image {
  width: 26rpx;
  height: 26rpx;
}

.submit-button {
  position: fixed;
  display: flex;
  align-items: center;
  justify-content: center;
  left: 28rpx;
  right: 28rpx;
  bottom: calc(24rpx + env(safe-area-inset-bottom));
  z-index: 10;
  height: 82rpx;
  border-radius: 999rpx;
  background: linear-gradient(110deg, #8b63ff 0%, #d987d5 52%, #ff806f 100%);
  color: #ffffff;
  font-size: 30rpx;
  font-weight: 950;
  line-height: 1;
  box-shadow: 0 10rpx 0 rgba(52, 32, 95, 0.1);
  box-sizing: border-box;
}

.submit-button[disabled],
.refresh-button[disabled] {
  opacity: 0.56;
}

.spinning {
  animation: spin 0.9s linear infinite;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

@media (max-width: 360px) {
  .hero,
  .field-grid {
    grid-template-columns: 1fr;
  }

  .coach-figure {
    opacity: 0.34;
  }

  .metric-column {
    grid-column: 1;
  }

  .topbar {
    grid-template-columns: 56rpx 1fr 140rpx;
  }
}
</style>
