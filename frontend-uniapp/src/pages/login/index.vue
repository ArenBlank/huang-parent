<template>
  <view class="login-page">
    <view class="hero">
      <view class="top-row">
        <view class="brand">
          <view class="brand-mark">
            <view class="tree-line tree-line--stem"></view>
            <view class="tree-line tree-line--left"></view>
            <view class="tree-line tree-line--right"></view>
            <view class="tree-line tree-line--root-left"></view>
            <view class="tree-line tree-line--root-right"></view>
          </view>
          <view>
            <text class="brand-name">森练计划</text>
            <text class="brand-sub">SENLIAN PLAN</text>
          </view>
        </view>

        <view class="browse-button" @click="previewApp">
          <text>随便看看</text>
          <image class="browse-arrow" :src="icons.chevronRight" mode="aspectFit" />
        </view>
      </view>

      <view class="hero-copy">
        <text class="hero-title">把训练旅程做成</text>
        <text class="hero-title">一片<text class="hero-title-hot">持续生长</text>的森林</text>
        <text class="hero-subtitle">每一次坚持，都会让森林更茂盛</text>
      </view>

      <view class="cloud cloud--right"></view>
      <view class="cloud cloud--middle"></view>
      <view class="sun-haze"></view>
      <view class="mountain mountain--left"></view>
      <view class="mountain mountain--mid"></view>
      <view class="hill hill--back"></view>
      <view class="hill hill--front"></view>
      <view class="path"></view>

      <view class="tree tree--large tree--purple">
        <view class="tree-crown"></view>
        <view class="tree-trunk"></view>
      </view>
      <view class="tree tree--large tree--coral">
        <view class="tree-crown"></view>
        <view class="tree-trunk"></view>
      </view>
      <view class="tree tree--small tree--violet">
        <view class="tree-crown"></view>
        <view class="tree-trunk"></view>
      </view>
      <view class="tree tree--small tree--pink">
        <view class="tree-crown"></view>
        <view class="tree-trunk"></view>
      </view>
    </view>

    <view class="metric-row">
      <view v-for="metric in metrics" :key="metric.label" class="metric-card">
        <view class="metric-icon" :class="metric.tone">
          <image class="metric-fa" :src="metric.icon" mode="aspectFit" />
        </view>
        <view class="metric-copy">
          <text class="metric-label">{{ metric.label }}</text>
          <text class="metric-value">{{ metric.value }}</text>
          <text class="metric-note">{{ metric.note }}</text>
        </view>
      </view>
    </view>

    <view class="auth-card">
      <view class="mode-switch">
        <view class="mode-tab" :class="{ active: mode === 'login' }" @click="switchMode('login')">登录</view>
        <view class="mode-tab" :class="{ active: mode === 'register' }" @click="switchMode('register')">注册</view>
      </view>

      <view class="form-stack">
        <view class="input-shell">
          <image class="field-icon" :src="icons.user" mode="aspectFit" />
          <input class="field-input" v-model.trim="form.account" :placeholder="mode === 'login' ? '请输入用户名 / 手机号' : '请设置用户名'" placeholder-class="field-placeholder" />
        </view>

        <view v-if="mode === 'register'" class="input-shell">
          <image class="field-icon" :src="icons.signature" mode="aspectFit" />
          <input class="field-input" v-model.trim="form.nickname" placeholder="请输入昵称" placeholder-class="field-placeholder" />
        </view>

        <view v-if="mode === 'register'" class="input-shell">
          <image class="field-icon" :src="icons.phone" mode="aspectFit" />
          <input class="field-input" v-model.trim="form.phone" placeholder="请输入手机号" placeholder-class="field-placeholder" />
        </view>

        <view v-if="mode === 'register'" class="input-shell">
          <image class="field-icon" :src="icons.envelope" mode="aspectFit" />
          <input class="field-input" v-model.trim="form.email" placeholder="请输入邮箱（可选）" placeholder-class="field-placeholder" />
        </view>

        <view class="input-shell">
          <image class="field-icon" :src="icons.lock" mode="aspectFit" />
          <input class="field-input" v-model="form.password" :password="!passwordVisible" placeholder="请输入密码" placeholder-class="field-placeholder" />
          <view class="password-eye" @click="passwordVisible = !passwordVisible">
            <image class="eye-icon" :src="passwordVisible ? icons.eye : icons.eyeSlash" mode="aspectFit" />
          </view>
        </view>

        <view v-if="mode === 'register'" class="input-shell">
          <image class="field-icon" :src="icons.lock" mode="aspectFit" />
          <input class="field-input" v-model="form.confirmPassword" password placeholder="请再次输入密码" placeholder-class="field-placeholder" />
        </view>
      </view>

      <view class="captcha-hint">
        <text>点击按钮后先完成拼图验证，验证通过才会提交{{ mode === 'login' ? '登录' : '注册' }}。</text>
      </view>

      <view class="submit-button" :class="{ loading }" @click="submit">
        <text>{{ loading ? '处理中...' : submitText }}</text>
      </view>

      <view class="agreement" @click="agreed = !agreed">
        <view class="radio" :class="{ checked: agreed }"></view>
        <text>我已阅读并同意</text>
        <text class="agreement-link">《用户协议》</text>
        <text>与</text>
        <text class="agreement-link">《隐私政策》</text>
      </view>
    </view>

    <SlCaptcha
      :visible="captchaVisible"
      @success="handleCaptchaSuccess"
      @close="captchaVisible = false"
    />
  </view>
</template>

<script setup>
import { computed, reactive, ref } from 'vue'
import {
  faChartLine,
  faChevronRight,
  faEnvelope,
  faEye,
  faEyeSlash,
  faFireFlameCurved,
  faLock,
  faPhone,
  faShieldHalved,
  faSignature,
  faUser
} from '@fortawesome/free-solid-svg-icons'
import SlCaptcha from '../../components/SlCaptcha.vue'
import { useAppAuthStore } from '../../stores/auth'

const store = useAppAuthStore()
const mode = ref('login')
const loading = ref(false)
const agreed = ref(false)
const captchaVisible = ref(false)
const passwordVisible = ref(false)

const form = reactive({
  account: '',
  nickname: '',
  phone: '',
  email: '',
  password: '',
  confirmPassword: '',
  captchaVerification: ''
})

const faIcon = (definition, color = '#24104f') => {
  const [width, height, , , pathData] = definition.icon
  const paths = Array.isArray(pathData)
    ? pathData.map((path) => `<path fill="${color}" d="${path}"/>`).join('')
    : `<path fill="${color}" d="${pathData}"/>`
  const svg = `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 ${width} ${height}">${paths}</svg>`
  return `data:image/svg+xml;charset=utf-8,${encodeURIComponent(svg)}`
}

const icons = {
  chartLine: faIcon(faChartLine),
  shield: faIcon(faShieldHalved),
  fire: faIcon(faFireFlameCurved),
  user: faIcon(faUser, '#86779b'),
  signature: faIcon(faSignature, '#86779b'),
  phone: faIcon(faPhone, '#86779b'),
  envelope: faIcon(faEnvelope, '#86779b'),
  lock: faIcon(faLock, '#86779b'),
  eye: faIcon(faEye, '#86779b'),
  eyeSlash: faIcon(faEyeSlash, '#86779b'),
  chevronRight: faIcon(faChevronRight)
}

const metrics = [
  { label: '训练进度', value: '68%', note: '本周完成 4/6 次', icon: icons.chartLine, tone: 'metric-icon--violet' },
  { label: '认证徽章', value: '12', note: '已获得 12 枚', icon: icons.shield, tone: 'metric-icon--gold' },
  { label: '成长影响', value: '320', note: '影响了 128 人', icon: icons.fire, tone: 'metric-icon--coral' }
]

const submitText = computed(() => (mode.value === 'login' ? '验证并登录' : '验证并注册'))

const switchMode = (nextMode) => {
  mode.value = nextMode
  captchaVisible.value = false
  form.captchaVerification = ''
}

const previewApp = () => {
  uni.showToast({ title: '登录后可查看真实训练数据', icon: 'none' })
}

const validate = () => {
  if (!form.account || !form.password) {
    uni.showToast({ title: '请输入账号和密码', icon: 'none' })
    return false
  }
  if (!agreed.value) {
    uni.showToast({ title: '请先同意用户协议', icon: 'none' })
    return false
  }
  if (mode.value === 'register') {
    if (!form.nickname || !form.phone || !form.confirmPassword) {
      uni.showToast({ title: '请补全注册信息', icon: 'none' })
      return false
    }
    if (!/^[a-zA-Z0-9_]{3,20}$/.test(form.account)) {
      uni.showToast({ title: '用户名需为3-20位字母数字或下划线', icon: 'none' })
      return false
    }
    if (!/^1[3-9]\d{9}$/.test(form.phone)) {
      uni.showToast({ title: '请输入正确手机号', icon: 'none' })
      return false
    }
    if (form.password !== form.confirmPassword) {
      uni.showToast({ title: '两次密码不一致', icon: 'none' })
      return false
    }
  }
  return true
}

const submit = async () => {
  if (loading.value) return
  if (!validate()) return
  captchaVisible.value = true
}

const handleCaptchaSuccess = async (captchaVerification) => {
  captchaVisible.value = false
  form.captchaVerification = captchaVerification
  try {
    loading.value = true
    if (mode.value === 'login') {
      await store.login(form.account, form.password, form.captchaVerification)
    } else {
      await store.register({
        username: form.account,
        nickname: form.nickname,
        phone: form.phone,
        email: form.email || undefined,
        password: form.password,
        confirmPassword: form.confirmPassword,
        captchaVerification: form.captchaVerification
      })
    }
    uni.switchTab({ url: '/pages/home/index' })
  } catch (err) {
    uni.showToast({
      title: err?.message || `${mode.value === 'login' ? '登录' : '注册'}失败`,
      icon: 'none',
      duration: 2200
    })
  } finally {
    form.captchaVerification = ''
    loading.value = false
  }
}
</script>

<style scoped lang="scss">
.login-page {
  min-height: 100vh;
  padding-bottom: 28rpx;
  overflow: hidden;
  background:
    radial-gradient(circle at 0% 10%, rgba(141, 111, 255, 0.28), transparent 32%),
    radial-gradient(circle at 100% 4%, rgba(255, 119, 105, 0.24), transparent 28%),
    linear-gradient(180deg, #f8e8ff 0%, #fff1d8 45%, #fffaf4 100%);
  color: #24104f;
}

.hero {
  position: relative;
  min-height: 720rpx;
  padding: calc(var(--status-bar-height) + 42rpx) 42rpx 0;
  overflow: hidden;
}

.top-row,
.brand,
.browse-button,
.metric-row,
.metric-card,
.input-shell,
.agreement {
  display: flex;
  align-items: center;
}

.top-row {
  position: relative;
  z-index: 4;
  justify-content: space-between;
}

.brand {
  gap: 18rpx;
}

.brand-mark {
  position: relative;
  width: 76rpx;
  height: 76rpx;
  border-radius: 50%;
  background: #24104f;
  box-shadow: 0 8rpx 20rpx rgba(36, 16, 79, 0.2);
}

.tree-line {
  position: absolute;
  left: 50%;
  top: 18rpx;
  width: 6rpx;
  height: 44rpx;
  border-radius: 999rpx;
  background: #ffffff;
  transform-origin: bottom center;
}

.tree-line--stem {
  transform: translateX(-50%);
}

.tree-line--left {
  height: 24rpx;
  transform: translateX(-50%) rotate(-48deg);
}

.tree-line--right {
  height: 24rpx;
  transform: translateX(-50%) rotate(48deg);
}

.tree-line--root-left {
  top: 36rpx;
  height: 20rpx;
  transform: translateX(-50%) rotate(-118deg);
}

.tree-line--root-right {
  top: 36rpx;
  height: 20rpx;
  transform: translateX(-50%) rotate(118deg);
}

.brand-name {
  display: block;
  color: #24104f;
  font-size: 34rpx;
  font-weight: 900;
  line-height: 1.05;
}

.brand-sub {
  display: block;
  margin-top: 8rpx;
  color: #24104f;
  font-size: 17rpx;
  font-weight: 900;
  letter-spacing: 7rpx;
}

.browse-button {
  justify-content: center;
  gap: 8rpx;
  min-width: 156rpx;
  height: 62rpx;
  padding: 0 22rpx;
  border: 3rpx solid rgba(52, 32, 95, 0.38);
  border-radius: 22rpx;
  background: rgba(255, 240, 244, 0.46);
  color: #24104f;
  font-size: 28rpx;
  font-weight: 900;
  box-shadow: none;
}

.browse-arrow {
  width: 22rpx;
  height: 22rpx;
}

.hero-copy {
  position: relative;
  z-index: 4;
  margin-top: 110rpx;
}

.hero-title {
  display: block;
  color: #24104f;
  font-size: 58rpx;
  font-weight: 900;
  line-height: 1.33;
  text-shadow: 0 4rpx 0 rgba(36, 16, 79, 0.05);
}

.hero-title-hot {
  color: #ff806f;
}

.hero-subtitle {
  display: block;
  margin-top: 28rpx;
  color: #69548f;
  font-size: 28rpx;
  font-weight: 800;
}

.sun-haze {
  position: absolute;
  right: 80rpx;
  top: 250rpx;
  width: 280rpx;
  height: 280rpx;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(255, 210, 130, 0.45), transparent 68%);
}

.cloud {
  position: absolute;
  z-index: 2;
  height: 24rpx;
  border-radius: 999rpx;
  background: rgba(255, 255, 255, 0.8);
}

.cloud::before,
.cloud::after {
  content: "";
  position: absolute;
  bottom: 0;
  border-radius: 50%;
  background: inherit;
}

.cloud--right {
  right: -20rpx;
  top: 260rpx;
  width: 130rpx;
}

.cloud--right::before {
  left: 22rpx;
  width: 50rpx;
  height: 50rpx;
}

.cloud--right::after {
  right: 18rpx;
  width: 78rpx;
  height: 78rpx;
}

.cloud--middle {
  right: 250rpx;
  top: 420rpx;
  width: 76rpx;
  height: 16rpx;
}

.cloud--middle::before {
  left: 12rpx;
  width: 28rpx;
  height: 28rpx;
}

.cloud--middle::after {
  right: 12rpx;
  width: 38rpx;
  height: 38rpx;
}

.mountain {
  position: absolute;
  z-index: 1;
  bottom: 76rpx;
  width: 330rpx;
  height: 170rpx;
  background: rgba(151, 117, 218, 0.22);
  clip-path: polygon(0% 100%, 44% 18%, 100% 100%);
}

.mountain--left {
  left: -40rpx;
}

.mountain--mid {
  left: 260rpx;
  bottom: 72rpx;
  width: 250rpx;
  height: 128rpx;
  background: rgba(151, 117, 218, 0.16);
}

.hill {
  position: absolute;
  left: -80rpx;
  right: -80rpx;
  bottom: 0;
  height: 220rpx;
  border-radius: 52% 52% 0 0;
}

.hill--back {
  z-index: 1;
  bottom: 44rpx;
  background: linear-gradient(130deg, #8f76f4 0%, #c7b6ff 45%, #7d65e8 100%);
  transform: rotate(2deg);
}

.hill--front {
  z-index: 2;
  right: -260rpx;
  bottom: -12rpx;
  height: 190rpx;
  background: linear-gradient(135deg, #fff0c8 0%, #ffbd83 36%, #7459d3 76%);
  transform: rotate(-8deg);
}

.path {
  position: absolute;
  z-index: 2;
  right: 100rpx;
  bottom: 48rpx;
  width: 330rpx;
  height: 82rpx;
  border-radius: 50%;
  background: linear-gradient(90deg, rgba(255, 232, 167, 0.95), rgba(255, 147, 116, 0.68));
  transform: rotate(-15deg);
}

.tree {
  position: absolute;
  z-index: 3;
}

.tree-crown {
  border: 5rpx solid #4f3196;
  border-radius: 48% 52% 45% 55%;
}

.tree-trunk {
  width: 8rpx;
  margin: -8rpx auto 0;
  border-radius: 999rpx;
  background: #4f3196;
}

.tree--large .tree-crown {
  width: 76rpx;
  height: 112rpx;
}

.tree--large .tree-trunk {
  height: 92rpx;
}

.tree--small .tree-crown {
  width: 44rpx;
  height: 68rpx;
}

.tree--small .tree-trunk {
  height: 48rpx;
}

.tree--purple {
  right: 92rpx;
  bottom: 96rpx;
}

.tree--purple .tree-crown {
  background: linear-gradient(180deg, #a384ff, #7658db);
}

.tree--coral {
  right: 18rpx;
  bottom: 142rpx;
}

.tree--coral .tree-crown {
  background: linear-gradient(180deg, #ff9d8a, #ff776f);
}

.tree--violet {
  right: 250rpx;
  bottom: 88rpx;
}

.tree--violet .tree-crown {
  background: linear-gradient(180deg, #b19aff, #8068f2);
}

.tree--pink {
  right: 350rpx;
  bottom: 56rpx;
}

.tree--pink .tree-crown {
  background: linear-gradient(180deg, #ff9b9c, #ff736c);
}

.metric-row {
  position: relative;
  z-index: 5;
  justify-content: space-between;
  gap: 16rpx;
  margin: -82rpx 32rpx 0;
}

.metric-card {
  flex: 1;
  min-width: 0;
  min-height: 156rpx;
  justify-content: center;
  gap: 8rpx;
  padding: 16rpx 10rpx;
  border: 2rpx solid rgba(52, 32, 95, 0.14);
  border-radius: 30rpx;
  background: rgba(255, 255, 255, 0.94);
  box-shadow: 0 14rpx 0 rgba(52, 32, 95, 0.06), 0 22rpx 32rpx rgba(52, 32, 95, 0.08);
}

.metric-icon {
  display: grid;
  flex: 0 0 auto;
  width: 52rpx;
  height: 52rpx;
  place-items: center;
  border-radius: 50%;
}

.metric-fa {
  display: block;
  width: 34rpx;
  height: 34rpx;
}

.metric-icon--violet {
  background: #eee6ff;
}

.metric-icon--gold {
  background: #fff0c8;
}

.metric-icon--coral {
  background: #ffe1cc;
}

.metric-copy {
  min-width: 0;
}

.metric-label,
.metric-note {
  display: block;
  color: #4b3a72;
  font-size: 18rpx;
  font-weight: 800;
  white-space: nowrap;
}

.metric-value {
  display: block;
  margin: 6rpx 0 4rpx;
  color: #ff806f;
  font-size: 32rpx;
  font-weight: 900;
  line-height: 1;
}

.auth-card {
  margin: 34rpx 28rpx 0;
  padding: 38rpx 48rpx 34rpx;
  border: 5rpx solid #34205f;
  border-radius: 44rpx;
  background: rgba(255, 255, 255, 0.93);
  box-shadow: 0 16rpx 0 rgba(52, 32, 95, 0.08), 0 24rpx 38rpx rgba(139, 99, 255, 0.12);
}

.mode-switch {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 24rpx;
  margin-bottom: 38rpx;
}

.mode-tab {
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  min-height: 70rpx;
  border: 0;
  background: transparent;
  box-shadow: none;
  color: #988aa9;
  font-size: 34rpx;
  font-weight: 900;
}

.mode-tab.active {
  color: #24104f;
}

.mode-tab.active::after {
  content: "";
  position: absolute;
  left: 50%;
  bottom: 0;
  width: 92rpx;
  height: 8rpx;
  border-radius: 999rpx;
  background: #8b63ff;
  transform: translateX(-50%);
}

.form-stack {
  display: flex;
  flex-direction: column;
  gap: 22rpx;
}

.input-shell {
  min-height: 92rpx;
  padding: 0 22rpx;
  border: 2rpx solid #eedde0;
  border-radius: 24rpx;
  background: rgba(255, 255, 255, 0.86);
}

.field-icon {
  display: block;
  flex: 0 0 58rpx;
  width: 34rpx;
  height: 34rpx;
  margin-right: 10rpx;
}

.field-input {
  flex: 1;
  min-width: 0;
  height: 88rpx;
  color: #24104f;
  font-size: 27rpx;
}

.field-placeholder {
  color: #847798;
}

.password-eye {
  display: grid;
  width: 54rpx;
  height: 54rpx;
  place-items: center;
  border: 0;
  background: transparent;
  box-shadow: none;
}

.eye-icon {
  display: block;
  width: 34rpx;
  height: 34rpx;
}

.captcha-hint {
  margin-top: 22rpx;
  color: #75648f;
  font-size: 23rpx;
  font-weight: 700;
  line-height: 1.6;
}

.submit-button {
  display: grid;
  width: 100%;
  min-height: 100rpx;
  margin-top: 26rpx;
  place-items: center;
  border: 5rpx solid #24104f;
  border-radius: 28rpx;
  background: linear-gradient(105deg, #8b63ff 0%, #bb82ff 48%, #ff8e76 100%);
  box-shadow: 0 13rpx 0 rgba(52, 32, 95, 0.16);
  color: #fff;
  font-size: 34rpx;
  font-weight: 900;
}

.submit-button.loading {
  opacity: 0.72;
}

.agreement {
  justify-content: center;
  flex-wrap: wrap;
  gap: 8rpx;
  margin-top: 40rpx;
  border: 0;
  background: transparent;
  box-shadow: none;
  color: #716381;
  font-size: 22rpx;
}

.radio {
  width: 25rpx;
  height: 25rpx;
  border: 3rpx solid #d2c2dd;
  border-radius: 50%;
  background: #fff;
}

.radio.checked {
  border-color: #8b63ff;
  background: radial-gradient(circle, #8b63ff 0 42%, #fff 44%);
}

.agreement-link {
  color: #7d55ff;
  font-weight: 900;
}

@media (max-width: 360px) {
  .metric-card {
    flex-direction: column;
    min-height: 190rpx;
  }

  .metric-label,
  .metric-note {
    text-align: center;
  }

  .auth-card {
    padding-left: 34rpx;
    padding-right: 34rpx;
  }
}
</style>
