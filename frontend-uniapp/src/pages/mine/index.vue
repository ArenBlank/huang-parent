<template>
  <view class="mine-page">
    <view class="page-glow page-glow--left"></view>
    <view class="page-glow page-glow--right"></view>

    <view class="hero">
      <view class="hero-cloud hero-cloud--one"></view>
      <view class="hero-cloud hero-cloud--two"></view>
      <view class="hero-hill hero-hill--back"></view>
      <view class="hero-hill hero-hill--front"></view>

      <view class="topbar">
        <text class="page-title">我的</text>
        <view class="top-actions">
          <button class="icon-button" hover-class="none" aria-label="账户安全" @click="goPage('/pages/security/index')">
            <image :src="icons.gear" mode="aspectFit" />
          </button>
          <button class="icon-button notify-button" hover-class="none" aria-label="通知" @click="toastLater">
            <image :src="icons.bell" mode="aspectFit" />
            <view class="notify-dot"></view>
          </button>
        </view>
      </view>

      <view class="profile-hero">
        <view class="avatar">
          <image v-if="avatarSrc" class="avatar-image" :src="avatarSrc" mode="aspectFill" />
          <view v-else class="avatar-fallback">
            <text>{{ avatarFallback }}</text>
          </view>
        </view>
        <view class="profile-copy">
          <view class="name-row">
            <text class="display-name">{{ displayName }}</text>
            <text v-if="vipLabel" class="vip-pill">
              <image :src="icons.shield" mode="aspectFit" />
              {{ vipLabel }}
            </text>
          </view>
          <view class="identity-row">
            <text>{{ profileCompleted ? '资料已完善' : '待完善资料' }}</text>
            <text>{{ accountStatusText }}</text>
          </view>
          <view class="completion-row">
            <text>资料完成度 {{ completionRate }}%</text>
            <view class="progress-track">
              <view class="progress-bar" :style="{ width: `${completionRate}%` }"></view>
            </view>
          </view>
        </view>
        <button class="profile-enter" hover-class="none" @click="goPage('/pages/profile-edit/index')">
          <image :src="icons.chevronRight" mode="aspectFit" />
        </button>
      </view>
    </view>

    <view class="content">
      <view class="overview-card">
        <view class="section-head">
          <text class="section-title">资料概览</text>
          <text class="verified-pill" :class="{ complete: profileCompleted }">
            <image :src="icons.shield" mode="aspectFit" />
            {{ profileCompleted ? '已完善' : '待完善' }}
          </text>
        </view>

        <view class="profile-grid">
          <view v-for="item in profileItems" :key="item.label" class="profile-item">
            <view class="item-icon">
              <image :src="item.icon" mode="aspectFit" />
            </view>
            <view class="item-copy">
              <text class="item-label">{{ item.label }}</text>
              <text class="item-value">{{ item.value }}</text>
            </view>
          </view>
        </view>

        <view class="complete-banner">
          <view class="complete-icon">
            <image :src="icons.clipboardCheck" mode="aspectFit" />
          </view>
          <text>{{ profileCompleted ? '资料已完善，可继续使用个性化训练服务' : '完善资料，获取个性化训练与服务' }}</text>
          <button hover-class="none" @click="goPage('/pages/profile-edit/index')">去完善</button>
        </view>
      </view>

      <view class="service-card">
        <text class="section-title">常用服务</text>
        <button
          v-for="item in serviceMenu"
          :key="item.url"
          class="service-row"
          hover-class="none"
          @click="goPage(item.url)"
        >
          <view class="service-icon">
            <image :src="item.icon" mode="aspectFit" />
          </view>
          <view class="service-copy">
            <text class="service-title">{{ item.title }}</text>
            <text class="service-desc">{{ item.desc }}</text>
          </view>
          <text v-if="item.status" class="service-status" :class="item.tone">{{ item.status }}</text>
          <image class="row-chevron" :src="icons.chevronRight" mode="aspectFit" />
        </button>
      </view>

      <button class="logout-button" hover-class="none" @click="logout">
        <image :src="icons.logout" mode="aspectFit" />
        <text>退出登录</text>
      </button>

      <view class="footer-copy">
        <text>隐私政策</text>
        <text>|</text>
        <text>用户协议</text>
      </view>
    </view>

    <view class="custom-tabbar">
      <button
        v-for="tab in tabs"
        :key="tab.text"
        class="tab-item"
        :class="{ active: tab.active }"
        hover-class="none"
        @click="goTab(tab)"
      >
        <image class="tab-icon" :src="tab.active ? tab.activeIcon : tab.icon" mode="aspectFit" />
        <text>{{ tab.text }}</text>
      </button>
      <view class="home-indicator"></view>
    </view>
  </view>
</template>

<script setup>
import {
  faBell,
  faBriefcase,
  faCalendarCheck,
  faCalendarDays,
  faChevronRight,
  faClipboardCheck,
  faClipboardList,
  faEnvelope,
  faFileLines,
  faGear,
  faHome,
  faIdBadge,
  faLocationDot,
  faMobileScreenButton,
  faPlayCircle,
  faRightFromBracket,
  faRulerVertical,
  faShieldHalved,
  faUser,
  faUserPen,
  faWeightScale
} from '@fortawesome/free-solid-svg-icons'
import { computed, ref } from 'vue'
import { onHide, onShow } from '@dcloudio/uni-app'
import { getMyCoachApplication } from '../../api/modules/coach'
import { getProfile } from '../../api/modules/profile'
import { ensureLogin } from '../../utils/authGuard'
import { toPublicUrl } from '../../utils/mediaUrl'
import { hideTabBarSafely, showTabBarSafely } from '../../utils/navigation'
import { useAppAuthStore } from '../../stores/auth'

const store = useAppAuthStore()
const profile = ref(null)
const coachApply = ref(null)

const faIcon = (definition, color = '#24104f') => {
  const [width, height, , , pathData] = definition.icon
  const paths = Array.isArray(pathData)
    ? pathData.map((path) => `<path fill="${color}" d="${path}"/>`).join('')
    : `<path fill="${color}" d="${pathData}"/>`
  const svg = `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 ${width} ${height}">${paths}</svg>`
  return `data:image/svg+xml;charset=utf-8,${encodeURIComponent(svg)}`
}

const icons = {
  bell: faIcon(faBell),
  briefcase: faIcon(faBriefcase, '#8b63ff'),
  calendar: faIcon(faCalendarCheck, '#8b63ff'),
  calendarDays: faIcon(faCalendarDays, '#8b63ff'),
  chevronRight: faIcon(faChevronRight),
  clipboard: faIcon(faClipboardList),
  clipboardCheck: faIcon(faClipboardCheck, '#8b63ff'),
  email: faIcon(faEnvelope, '#8b63ff'),
  file: faIcon(faFileLines, '#8b63ff'),
  gear: faIcon(faGear),
  home: faIcon(faHome),
  homeActive: faIcon(faHome, '#8b63ff'),
  location: faIcon(faLocationDot, '#8b63ff'),
  logout: faIcon(faRightFromBracket, '#ff6f61'),
  mobile: faIcon(faMobileScreenButton, '#8b63ff'),
  play: faIcon(faPlayCircle),
  playActive: faIcon(faPlayCircle, '#8b63ff'),
  ruler: faIcon(faRulerVertical, '#8b63ff'),
  shield: faIcon(faShieldHalved, '#8b63ff'),
  user: faIcon(faUser),
  userActive: faIcon(faUser, '#8b63ff'),
  userPen: faIcon(faUserPen, '#8b63ff'),
  weight: faIcon(faWeightScale, '#8b63ff'),
  coach: faIcon(faIdBadge, '#8b63ff')
}

const appBase = (import.meta.env.VITE_APP_BASE_URL || '').replace(/\/app\/?$/, '').replace(/\/$/, '')

const displayName = computed(() => currentProfile.value?.nickname || currentProfile.value?.username || '训练用户')
const currentProfile = computed(() => profile.value || store.user || {})
const profileCompleted = computed(() => Boolean(currentProfile.value?.profileCompleted))
const completionRate = computed(() => {
  const value = Number(currentProfile.value?.completionRate)
  if (Number.isFinite(value)) return Math.min(100, Math.max(0, Math.round(value)))
  return profileCompleted.value ? 100 : 0
})
const accountStatusText = computed(() => (Number(currentProfile.value?.status) === 0 ? '账号禁用' : '账号正常'))
const vipLabel = computed(() => {
  const level = Number(currentProfile.value?.vipLevel || 0)
  return level > 0 ? `LV${level}` : ''
})
const avatarSrc = computed(() => toPublicUrl(currentProfile.value?.avatar || currentProfile.value?.avatarUrl))
const avatarFallback = computed(() => {
  const name = displayName.value || ''
  return name ? name.slice(0, 1).toUpperCase() : 'U'
})

const profileItems = computed(() => [
  { label: '手机号', value: maskPhone(currentProfile.value?.phone), icon: icons.mobile },
  { label: '邮箱', value: maskEmail(currentProfile.value?.email), icon: icons.email },
  { label: '职业', value: currentProfile.value?.occupation || '未填写', icon: icons.briefcase },
  { label: '年龄', value: formatAge(currentProfile.value?.age), icon: icons.calendarDays },
  { label: '身高', value: formatHeight(currentProfile.value?.height), icon: icons.ruler },
  { label: '体重', value: formatWeight(currentProfile.value?.weight), icon: icons.weight },
  { label: '地址', value: currentProfile.value?.address || '未填写', icon: icons.location },
  { label: '简介', value: currentProfile.value?.bio || '未填写', icon: icons.file }
])

const coachStatus = computed(() => {
  const status = Number(coachApply.value?.certStatus)
  if (status === 1) return { text: '已通过', tone: 'success' }
  if (status === 2) return { text: '已驳回', tone: 'danger' }
  if (status === 0) return { text: '审核中', tone: 'warning' }
  return { text: '', tone: '' }
})

const serviceMenu = computed(() => [
  {
    title: '编辑资料',
    desc: '完善个人信息，管理资料',
    icon: icons.userPen,
    url: '/pages/profile-edit/index'
  },
  {
    title: '我的订单',
    desc: '查看课程、预约、商品订单',
    icon: icons.clipboard,
    url: '/pages/orders/index'
  },
  {
    title: '我的预约',
    desc: '查看教练预约与上课安排',
    icon: icons.calendar,
    url: '/pages/booking/index'
  },
  {
    title: '教练申请',
    desc: '申请成为教练，查看审核进度',
    icon: icons.coach,
    url: '/pages/coach-apply/index',
    status: coachStatus.value.text,
    tone: coachStatus.value.tone
  },
  {
    title: '账户安全',
    desc: '修改密码、账号安全设置',
    icon: icons.shield,
    url: '/pages/security/index'
  }
])

const tabs = [
  { text: '首页', icon: icons.home, activeIcon: icons.homeActive, url: '/pages/home/index' },
  { text: '计划', icon: icons.clipboard, activeIcon: faIcon(faClipboardList, '#8b63ff'), url: '/pages/plans/index' },
  { text: '课程', icon: icons.play, activeIcon: icons.playActive, url: '/pages/courses/index' },
  { text: '我的', icon: icons.user, activeIcon: icons.userActive, active: true, url: '/pages/mine/index' }
]

const maskPhone = (value) => {
  const raw = String(value || '').trim()
  if (!raw) return '未填写'
  if (raw.length < 7) return raw
  return `${raw.slice(0, 3)} **** ${raw.slice(-4)}`
}

const maskEmail = (value) => {
  const raw = String(value || '').trim()
  if (!raw) return '未填写'
  const [name, domain] = raw.split('@')
  if (!domain) return raw
  return `${name.slice(0, 3)}***@${domain}`
}

const formatAge = (value) => {
  const age = Number(value)
  return Number.isFinite(age) && age > 0 ? `${age} 岁` : '未填写'
}

const formatHeight = (value) => {
  const height = Number(value)
  return Number.isFinite(height) && height > 0 ? `${height} cm` : '未填写'
}

const formatWeight = (value) => {
  const weight = Number(value)
  return Number.isFinite(weight) && weight > 0 ? `${weight} kg` : '未填写'
}

const loadMine = async () => {
  const [profileResult, coachResult] = await Promise.allSettled([getProfile(), getMyCoachApplication()])
  if (profileResult.status === 'fulfilled') {
    profile.value = profileResult.value?.data || null
    if (profile.value) {
      store.setSession({
        accessToken: store.accessToken,
        refreshToken: store.refreshToken,
        user: profile.value
      })
    }
  }
  if (coachResult.status === 'fulfilled') {
    coachApply.value = coachResult.value?.data || null
  }
}

const goPage = (url) => {
  uni.navigateTo({ url })
}

const goTab = (tab) => {
  if (tab.active) return
  showTabBarSafely()
  uni.switchTab({ url: tab.url })
}

const toastLater = () => {
  uni.showToast({ title: '通知功能完善中', icon: 'none' })
}

const logout = () => {
  uni.showModal({
    title: '退出登录',
    content: '确认退出当前账号吗？',
    confirmText: '退出',
    confirmColor: '#ff6f61',
    success: ({ confirm }) => {
      if (!confirm) return
      store.logout()
      uni.redirectTo({ url: '/pages/login/index' })
    }
  })
}

onShow(() => {
  hideTabBarSafely()
  if (ensureLogin()) {
    loadMine()
  }
})

onHide(() => {
  showTabBarSafely()
})
</script>

<style scoped lang="scss">
.mine-page {
  position: relative;
  min-height: 100vh;
  padding-bottom: 206rpx;
  overflow-x: hidden;
  background:
    radial-gradient(circle at 0 0, rgba(139, 99, 255, 0.18), transparent 34%),
    radial-gradient(circle at 100% 6%, rgba(255, 128, 111, 0.18), transparent 30%),
    linear-gradient(180deg, #fff7fb 0%, #fffaf4 48%, #fffdf9 100%);
  color: #24104f;
}

.page-glow {
  position: fixed;
  z-index: 0;
  width: 320rpx;
  height: 320rpx;
  border-radius: 999rpx;
  filter: blur(22rpx);
  opacity: 0.45;
  pointer-events: none;
}

.page-glow--left {
  left: -170rpx;
  top: 150rpx;
  background: #e8ddff;
}

.page-glow--right {
  right: -160rpx;
  top: 20rpx;
  background: #ffd8df;
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

.hero {
  position: relative;
  z-index: 1;
  min-height: 360rpx;
  padding: calc(var(--status-bar-height) + 42rpx) 34rpx 86rpx;
  overflow: hidden;
  background:
    radial-gradient(circle at 12% 4%, rgba(156, 132, 255, 0.32), transparent 32%),
    radial-gradient(circle at 84% 12%, rgba(255, 128, 111, 0.22), transparent 30%),
    linear-gradient(120deg, #eee5ff 0%, #ffe1e9 50%, #fff3c9 100%);
}

.hero-cloud,
.hero-hill {
  position: absolute;
}

.hero-cloud {
  z-index: 1;
  border-radius: 999rpx;
  background: rgba(255, 255, 255, 0.72);
}

.hero-cloud::before,
.hero-cloud::after {
  content: "";
  position: absolute;
  bottom: 0;
  border-radius: 50%;
  background: inherit;
}

.hero-cloud--one {
  left: 28rpx;
  top: 70rpx;
  width: 78rpx;
  height: 20rpx;
}

.hero-cloud--one::before {
  left: 8rpx;
  width: 38rpx;
  height: 38rpx;
}

.hero-cloud--one::after {
  right: 2rpx;
  width: 46rpx;
  height: 46rpx;
}

.hero-cloud--two {
  right: 208rpx;
  top: 168rpx;
  width: 82rpx;
  height: 20rpx;
}

.hero-cloud--two::before {
  left: 10rpx;
  width: 34rpx;
  height: 34rpx;
}

.hero-cloud--two::after {
  right: 8rpx;
  width: 44rpx;
  height: 44rpx;
}

.hero-hill--back {
  left: -80rpx;
  right: -120rpx;
  bottom: -22rpx;
  height: 136rpx;
  border-radius: 52% 52% 0 0;
  background: rgba(139, 99, 255, 0.24);
  transform: rotate(4deg);
}

.hero-hill--front {
  right: -120rpx;
  bottom: -50rpx;
  width: 520rpx;
  height: 158rpx;
  border-radius: 100% 0 0 0;
  background: rgba(255, 185, 118, 0.4);
  transform: skewY(-11deg);
}

.topbar,
.profile-hero {
  position: relative;
  z-index: 2;
}

.topbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.page-title {
  color: #24104f;
  font-size: 42rpx;
  font-weight: 950;
}

.top-actions {
  display: flex;
  align-items: center;
  gap: 28rpx;
}

.icon-button {
  position: relative;
  width: 58rpx;
  height: 58rpx;
}

.icon-button image {
  width: 42rpx;
  height: 42rpx;
}

.notify-dot {
  position: absolute;
  right: 5rpx;
  top: 5rpx;
  width: 16rpx;
  height: 16rpx;
  border-radius: 50%;
  background: #ff806f;
}

.profile-hero {
  display: grid;
  grid-template-columns: 150rpx minmax(0, 1fr) 54rpx;
  gap: 24rpx;
  align-items: center;
  margin-top: 48rpx;
}

.avatar {
  display: grid;
  width: 136rpx;
  height: 136rpx;
  overflow: hidden;
  place-items: center;
  border: 6rpx solid #ffffff;
  border-radius: 50%;
  background: linear-gradient(135deg, #ffd7d0, #eee6ff);
  box-shadow: 0 12rpx 28rpx rgba(36, 16, 79, 0.12);
}

.avatar-image {
  width: 100%;
  height: 100%;
}

.avatar-fallback {
  display: grid;
  width: 100%;
  height: 100%;
  place-items: center;
  background: radial-gradient(circle at 35% 25%, #fff5d8, #d8c6ff 68%);
}

.avatar-fallback text {
  color: #24104f;
  font-size: 50rpx;
  font-weight: 950;
}

.profile-copy {
  min-width: 0;
}

.name-row {
  display: flex;
  align-items: center;
  gap: 14rpx;
}

.display-name {
  max-width: 320rpx;
  overflow: hidden;
  color: #24104f;
  font-size: 42rpx;
  font-weight: 950;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.vip-pill {
  display: inline-flex;
  align-items: center;
  gap: 6rpx;
  min-height: 36rpx;
  padding: 0 14rpx;
  border-radius: 999rpx;
  background: rgba(139, 99, 255, 0.12);
  color: #8b63ff;
  font-size: 20rpx;
  font-weight: 950;
}

.vip-pill image {
  width: 22rpx;
  height: 22rpx;
}

.identity-row {
  display: flex;
  flex-wrap: wrap;
  gap: 14rpx;
  margin-top: 14rpx;
}

.identity-row text {
  display: inline-flex;
  align-items: center;
  min-height: 42rpx;
  padding: 0 18rpx;
  border-radius: 999rpx;
  background: rgba(255, 255, 255, 0.48);
  color: #24104f;
  font-size: 23rpx;
  font-weight: 900;
}

.completion-row {
  margin-top: 16rpx;
}

.completion-row > text {
  color: #5f4a85;
  font-size: 24rpx;
  font-weight: 900;
}

.progress-track {
  height: 9rpx;
  margin-top: 11rpx;
  overflow: hidden;
  border-radius: 999rpx;
  background: rgba(255, 255, 255, 0.56);
}

.progress-bar {
  height: 100%;
  border-radius: inherit;
  background: linear-gradient(90deg, #8b63ff, #b86df4);
}

.profile-enter {
  width: 50rpx;
  height: 50rpx;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.42);
}

.profile-enter image {
  width: 22rpx;
  height: 22rpx;
}

.content {
  position: relative;
  z-index: 2;
  margin-top: -50rpx;
  padding: 0 28rpx;
}

.overview-card,
.service-card,
.logout-button {
  border: 2rpx solid rgba(52, 32, 95, 0.09);
  border-radius: 30rpx;
  background: rgba(255, 255, 255, 0.96);
  box-shadow: 0 12rpx 32rpx rgba(52, 32, 95, 0.08);
}

.overview-card,
.service-card {
  padding: 28rpx;
}

.section-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18rpx;
}

.section-title {
  color: #24104f;
  font-size: 31rpx;
  font-weight: 950;
}

.verified-pill {
  display: inline-flex;
  align-items: center;
  gap: 8rpx;
  height: 44rpx;
  padding: 0 18rpx;
  border-radius: 999rpx;
  background: #f0e7ff;
  color: #8b63ff;
  font-size: 22rpx;
  font-weight: 950;
}

.verified-pill.complete {
  background: #e5f9ed;
  color: #20a96c;
}

.verified-pill image {
  width: 24rpx;
  height: 24rpx;
}

.profile-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  column-gap: 30rpx;
  margin-top: 24rpx;
}

.profile-item {
  display: grid;
  grid-template-columns: 74rpx minmax(0, 1fr);
  gap: 18rpx;
  align-items: center;
  min-height: 118rpx;
  border-bottom: 2rpx solid rgba(52, 32, 95, 0.08);
}

.item-icon,
.service-icon,
.complete-icon {
  display: grid;
  place-items: center;
  border-radius: 22rpx;
  background: #f0e7ff;
}

.item-icon {
  width: 58rpx;
  height: 58rpx;
}

.item-icon image,
.service-icon image,
.complete-icon image {
  width: 32rpx;
  height: 32rpx;
}

.item-copy {
  min-width: 0;
}

.item-label,
.item-value {
  display: block;
}

.item-label {
  color: #7b6f98;
  font-size: 23rpx;
  font-weight: 850;
}

.item-value {
  margin-top: 8rpx;
  overflow: hidden;
  color: #24104f;
  font-size: 24rpx;
  font-weight: 900;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.complete-banner {
  display: grid;
  grid-template-columns: 58rpx minmax(0, 1fr) 130rpx;
  gap: 16rpx;
  align-items: center;
  margin-top: 26rpx;
  padding: 16rpx;
  border-radius: 22rpx;
  background: linear-gradient(90deg, rgba(139, 99, 255, 0.13), rgba(139, 99, 255, 0.05));
}

.complete-icon {
  width: 58rpx;
  height: 58rpx;
}

.complete-banner text {
  color: #6b4eea;
  font-size: 24rpx;
  font-weight: 950;
  line-height: 1.35;
}

.complete-banner button {
  height: 54rpx;
  border-radius: 999rpx;
  background: linear-gradient(135deg, #8b63ff 0%, #9d6bff 100%);
  color: #ffffff;
  font-size: 23rpx;
  font-weight: 950;
}

.service-card {
  margin-top: 24rpx;
}

.service-row {
  display: grid;
  grid-template-columns: 72rpx minmax(0, 1fr) auto 24rpx;
  gap: 18rpx;
  align-items: center;
  width: 100%;
  min-height: 112rpx;
  border-bottom: 2rpx solid rgba(52, 32, 95, 0.08);
  text-align: left;
}

.service-row:last-child {
  border-bottom: 0;
}

.service-icon {
  width: 58rpx;
  height: 58rpx;
}

.service-copy {
  min-width: 0;
}

.service-title,
.service-desc {
  display: block;
}

.service-title {
  color: #24104f;
  font-size: 28rpx;
  font-weight: 950;
}

.service-desc {
  margin-top: 10rpx;
  overflow: hidden;
  color: #7b6f98;
  font-size: 23rpx;
  font-weight: 800;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.service-status {
  font-size: 23rpx;
  font-weight: 950;
  white-space: nowrap;
}

.service-status.success {
  color: #20a96c;
}

.service-status.warning {
  color: #ff806f;
}

.service-status.danger {
  color: #d94d77;
}

.row-chevron {
  width: 22rpx;
  height: 22rpx;
}

.logout-button {
  gap: 12rpx;
  width: 100%;
  height: 86rpx;
  margin-top: 26rpx;
  color: #ff6f61;
  font-size: 28rpx;
  font-weight: 950;
}

.logout-button image {
  width: 34rpx;
  height: 34rpx;
}

.footer-copy {
  display: flex;
  justify-content: center;
  gap: 14rpx;
  margin: 24rpx 0 4rpx;
  color: #8d7fa8;
  font-size: 23rpx;
  font-weight: 800;
}

.custom-tabbar {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  z-index: 9999;
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  min-height: 137rpx;
  padding: 16rpx 18rpx calc(env(safe-area-inset-bottom) + 23rpx);
  border: 5rpx solid rgba(120, 86, 170, 0.34);
  border-radius: 32rpx 32rpx 0 0;
  background: rgba(255, 255, 255, 0.96);
  box-shadow: 0 -10rpx 26rpx rgba(52, 32, 95, 0.09);
}

.tab-item {
  display: flex;
  align-items: center;
  flex-direction: column;
  justify-content: center;
  gap: 8rpx;
  color: #24104f;
  font-size: 23rpx;
  font-weight: 800;
}

.tab-item.active {
  color: #8b63ff;
}

.tab-icon {
  width: 45rpx;
  height: 45rpx;
}

.home-indicator {
  position: absolute;
  left: 50%;
  bottom: calc(env(safe-area-inset-bottom) + 9rpx);
  width: 220rpx;
  height: 9rpx;
  border-radius: 999rpx;
  background: #24104f;
  transform: translateX(-50%);
}

@media (max-width: 360px) {
  .profile-hero {
    grid-template-columns: 124rpx minmax(0, 1fr) 46rpx;
  }

  .avatar {
    width: 112rpx;
    height: 112rpx;
  }

  .display-name {
    font-size: 36rpx;
  }

  .profile-grid {
    grid-template-columns: 1fr;
  }
}
</style>
