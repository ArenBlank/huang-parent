<template>
  <view class="home-page">
    <view class="hero">
      <view class="hero-cloud hero-cloud--left"></view>
      <view class="hero-cloud hero-cloud--mid"></view>
      <view class="hero-sun"></view>
      <view class="hill hill--back"></view>
      <view class="hill hill--front"></view>
      <view class="trail"></view>

      <view class="forest-tree forest-tree--one">
        <view class="forest-crown"></view>
        <view class="forest-trunk"></view>
      </view>
      <view class="forest-tree forest-tree--two">
        <view class="forest-crown"></view>
        <view class="forest-trunk"></view>
      </view>
      <view class="forest-tree forest-tree--three">
        <view class="forest-crown"></view>
        <view class="forest-trunk"></view>
      </view>

      <view class="top-actions">
        <view class="icon-button notify-button" @click="toastLater">
          <image class="action-icon" :src="icons.bell" mode="aspectFit" />
          <view class="notify-dot"></view>
        </view>
        <view class="icon-button" @click="loadHome">
          <image class="action-icon" :src="icons.expand" mode="aspectFit" />
        </view>
      </view>

      <view class="profile">
        <view class="avatar">
          <image v-if="avatarSrc" class="avatar-image" :src="avatarSrc" mode="aspectFill" />
          <view v-else class="avatar-fallback">
            <text>{{ avatarFallback }}</text>
          </view>
        </view>
        <view class="profile-copy">
          <view class="greeting-row">
            <text class="greeting">Hi, {{ displayName }}</text>
            <text class="wave">👏</text>
          </view>
          <view class="tags">
            <text v-if="roleLabel" class="tag">{{ roleLabel }}</text>
            <text class="tag">{{ ageLabel }}</text>
          </view>
        </view>
      </view>
    </view>

    <view class="content">
      <view class="stats-panel">
        <view
          v-for="stat in stats"
          :key="stat.label"
          class="stat-item"
          :class="stat.panelClass"
          @click="goSmart(stat.url)"
        >
          <view class="stat-pill" :class="stat.tone">
            <image class="stat-icon" :src="stat.icon" mode="aspectFit" />
            <text>{{ stat.shortLabel }}</text>
          </view>
          <text class="stat-label">{{ stat.label }}</text>
          <text class="stat-value">{{ stat.value }}</text>
          <image class="chevron-icon" :src="icons.chevronRight" mode="aspectFit" />
        </view>
      </view>

      <view class="quick-panel">
        <view
          v-for="entry in entries"
          :key="entry.title"
          class="quick-item"
          @click="goSmart(entry.url)"
        >
          <view class="quick-icon" :class="entry.tone">
            <image class="quick-fa" :src="entry.icon" mode="aspectFit" />
          </view>
          <text class="quick-title">{{ entry.title }}</text>
          <view class="quick-meta">
            <text>{{ entry.desc }}</text>
            <image class="mini-chevron" :src="icons.chevronRight" mode="aspectFit" />
          </view>
        </view>
      </view>

      <view class="promo-section">
        <view class="promo-head">
          <text>活动推荐</text>
          <text>来自 /app/banner/list</text>
        </view>
        <swiper
          v-if="hasBanners"
          class="promo-swiper"
          circular
          autoplay
          :interval="4200"
          :duration="420"
          @change="onBannerChange"
        >
          <swiper-item v-for="banner in bannerCards" :key="banner.key">
            <view class="promo-card" @click="openBanner(banner)">
              <view class="promo-copy">
                <text class="promo-kicker">运营 Banner</text>
                <text class="promo-title">{{ banner.title }}</text>
                <text class="promo-sub">{{ banner.subtitle }}</text>
                <view class="promo-button" :class="{ disabled: !banner.targetUrl }">
                  <text>{{ banner.linkText }}</text>
                  <image class="promo-arrow" :src="icons.chevronRight" mode="aspectFit" />
                </view>
              </view>
              <image
                v-if="banner.image && !failedBannerImages[banner.key]"
                class="promo-image"
                :src="banner.image"
                mode="aspectFill"
                @error="markBannerImageFailed(banner.key)"
              />
              <view v-else class="promo-image-fallback">
                <image :src="icons.bullhorn" mode="aspectFit" />
                <text>活动</text>
              </view>
              <view class="promo-mountain promo-mountain--one"></view>
              <view class="promo-mountain promo-mountain--two"></view>
            </view>
          </swiper-item>
        </swiper>
        <view v-else class="promo-empty" @click="loadHome">
          <view class="promo-empty-icon">
            <image :src="icons.bullhorn" mode="aspectFit" />
          </view>
          <view class="promo-empty-copy">
            <text>暂无活动推荐</text>
            <text>后台启用 Banner 后这里会自动展示，不再使用假轮播。</text>
          </view>
        </view>
        <view class="dots" v-if="bannerCards.length > 1">
          <view
            v-for="(_, index) in bannerCards"
            :key="index"
            class="dot"
            :class="{ active: bannerIndex === index }"
          ></view>
        </view>
      </view>

      <view class="notice-head">
        <text>平台公告</text>
        <view class="notice-more" @click="toastLater">
          <text>全部</text>
          <image class="mini-chevron" :src="icons.chevronRight" mode="aspectFit" />
        </view>
      </view>

      <view class="notice-card">
        <view v-for="notice in noticeList" :key="notice.key" class="notice-row">
          <view class="notice-icon">
            <image :src="icons.bullhorn" mode="aspectFit" />
          </view>
          <text class="notice-title">{{ notice.title }}</text>
          <text class="notice-date">{{ notice.date }}</text>
        </view>
      </view>
    </view>

    <view class="custom-tabbar">
      <view
        v-for="tab in tabs"
        :key="tab.text"
        class="tab-item"
        :class="{ active: tab.active }"
        @click="goTab(tab)"
      >
        <image class="tab-icon" :src="tab.active ? tab.activeIcon : tab.icon" mode="aspectFit" />
        <text>{{ tab.text }}</text>
      </view>
      <view class="home-indicator"></view>
    </view>
  </view>
</template>

<script setup>
import {
  faBell,
  faBookOpen,
  faBriefcase,
  faCalendarCheck,
  faChevronRight,
  faClipboardCheck,
  faClipboardList,
  faDumbbell,
  faExpand,
  faFireFlameCurved,
  faHome,
  faIdBadge,
  faPlayCircle,
  faUser,
  faVolumeHigh
} from '@fortawesome/free-solid-svg-icons'
import { computed, reactive, ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { listBanners, listNotices } from '../../api/modules/content'
import { listMyCourseSchedules, listMyEnrollments } from '../../api/modules/course'
import { listOrders } from '../../api/modules/order'
import { fetchPlanOverview } from '../../api/modules/plan'
import { getWeeklyStat } from '../../api/modules/record'
import { useAppAuthStore } from '../../stores/auth'
import { ensureLogin } from '../../utils/authGuard'

const store = useAppAuthStore()

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
  expand: faIcon(faExpand),
  clipboard: faIcon(faClipboardList),
  clipboardCheck: faIcon(faClipboardCheck),
  book: faIcon(faBookOpen),
  calendar: faIcon(faCalendarCheck),
  fire: faIcon(faFireFlameCurved),
  briefcase: faIcon(faBriefcase),
  coach: faIcon(faIdBadge),
  chevronRight: faIcon(faChevronRight),
  bullhorn: faIcon(faVolumeHigh, '#8b63ff'),
  home: faIcon(faHome),
  homeActive: faIcon(faHome, '#8b63ff'),
  plan: faIcon(faClipboardList),
  course: faIcon(faPlayCircle),
  mine: faIcon(faUser)
}

const metrics = reactive({
  planCount: 0,
  courseCount: 0,
  orderCount: 0,
  checkinCount: 0
})

const notices = reactive([])
const banners = reactive([])
const failedBannerImages = reactive({})
const bannerIndex = ref(0)

const appBase = (import.meta.env.VITE_APP_BASE_URL || '').replace(/\/app\/?$/, '').replace(/\/$/, '')

const displayName = computed(() => store.user?.nickname || store.user?.username || '训练用户')
const roleLabel = computed(() => {
  const raw = String(store.user?.occupation || store.user?.role || store.user?.userType || '').trim()
  if (raw === 'coach') return '教练'
  if (raw === 'member' || raw === 'student') return '训练会员'
  return raw
})
const ageLabel = computed(() => {
  const age = Number(store.user?.age)
  if (Number.isFinite(age) && age > 0) return `${age}岁`
  return '年龄未填'
})
const avatarSrc = computed(() => {
  const raw = String(store.user?.avatar || store.user?.avatarUrl || '').trim()
  if (!raw) return ''
  if (/^https?:\/\//.test(raw)) return raw
  return appBase ? `${appBase}${raw.startsWith('/') ? raw : `/${raw}`}` : raw
})
const avatarFallback = computed(() => {
  const name = displayName.value || ''
  return name ? name.slice(0, 1).toUpperCase() : 'U'
})

const stats = computed(() => [
  {
    label: '计划数',
    shortLabel: '计划数',
    value: metrics.planCount,
    icon: icons.clipboard,
    tone: 'tone-violet',
    panelClass: 'stat-item--active',
    url: '/pages/plans/index'
  },
  {
    label: '课程数',
    shortLabel: '课程数',
    value: metrics.courseCount,
    icon: icons.book,
    tone: 'tone-coral',
    url: '/pages/courses/index'
  },
  {
    label: '订单数',
    shortLabel: '订单数',
    value: metrics.orderCount,
    icon: icons.clipboard,
    tone: 'tone-gold',
    url: '/pages/orders/index'
  },
  {
    label: '打卡次数',
    shortLabel: '打卡',
    value: metrics.checkinCount,
    icon: icons.fire,
    tone: 'tone-violet',
    url: '/pages/training/index'
  }
])

const entries = [
  { title: '训练计划', desc: '查看我的计划', icon: icons.clipboard, tone: 'tone-violet', url: '/pages/plans/index' },
  { title: '课程目录', desc: '探索更多课程', icon: icons.book, tone: 'tone-coral', url: '/pages/courses/index' },
  { title: '预约大厅', desc: '预约线下课程', icon: icons.calendar, tone: 'tone-gold', url: '/pages/booking/index' },
  { title: '打卡成长', desc: '记录每日进步', icon: icons.calendar, tone: 'tone-coral', url: '/pages/training/index' },
  { title: '订单中心', desc: '查看我的订单', icon: icons.briefcase, tone: 'tone-violet', url: '/pages/orders/index' },
  { title: '教练申请', desc: '成为认证教练', icon: icons.coach, tone: 'tone-gold', url: '/pages/coach-apply/index' }
]

const tabs = [
  { text: '首页', icon: icons.home, activeIcon: icons.homeActive, active: true, url: '/pages/home/index' },
  { text: '计划', icon: icons.plan, activeIcon: faIcon(faClipboardList, '#8b63ff'), url: '/pages/plans/index' },
  { text: '课程', icon: icons.course, activeIcon: faIcon(faPlayCircle, '#8b63ff'), url: '/pages/courses/index' },
  { text: '我的', icon: icons.mine, activeIcon: faIcon(faUser, '#8b63ff'), url: '/pages/mine/index' }
]

const fallbackNotices = [
  { key: 'mock-1', title: '五一放假期间课程安排调整通知', date: '04-24' },
  { key: 'mock-2', title: '新功能上线：训练计划模板库', date: '04-22' },
  { key: 'mock-3', title: '关于打卡活动奖励发放说明', date: '04-20' }
]

const noticeList = computed(() => (notices.length ? notices : fallbackNotices))
const hasBanners = computed(() => banners.length > 0)
const bannerCards = computed(() => {
  return banners.slice(0, 4).map((item, index) => ({
    key: `banner-${item.id || index}`,
    title: item.title || `活动 ${item.id || index + 1}`,
    subtitle: item.linkUrl ? '点击进入后台配置的活动入口' : '当前活动仅展示，未配置跳转链接',
    image: resolveAssetUrl(item.imageUrl),
    linkUrl: item.linkUrl || '',
    targetUrl: resolveBannerTarget(item.linkUrl),
    linkText: resolveBannerTarget(item.linkUrl) ? '查看详情' : '仅展示'
  }))
})

const normalizeList = (payload) => {
  const data = payload?.data
  if (Array.isArray(data)) return data
  if (Array.isArray(data?.list)) return data.list
  if (Array.isArray(data?.records)) return data.records
  return []
}

const countPurchasedCourses = (enrollments = [], schedules = []) => {
  const paidRows = enrollments.filter((row) => Number(row?.status) === 2)
  const ids = new Set(
    paidRows
      .map((row) => row.courseId || row.courseTitle || row.id)
      .filter((value) => value !== undefined && value !== null && value !== '')
  )
  if (ids.size) return ids.size
  return new Set(
    schedules
      .map((row) => row.courseId || row.courseTitle || row.enrollmentId)
      .filter((value) => value !== undefined && value !== null && value !== '')
  ).size
}

const goSmart = (url) => {
  if (!url) return
  const tabUrls = ['/pages/home/index', '/pages/plans/index', '/pages/courses/index', '/pages/mine/index']
  if (tabUrls.includes(url)) {
    uni.showTabBar()
    uni.switchTab({ url })
    return
  }
  uni.navigateTo({ url })
}

const goTab = (tab) => {
  if (tab.active) return
  uni.showTabBar()
  uni.switchTab({ url: tab.url })
}

const toastLater = () => {
  uni.showToast({ title: '功能完善中', icon: 'none' })
}

const resolveAssetUrl = (value) => {
  const raw = String(value || '').trim()
  if (!raw) return ''
  if (/^https?:\/\//.test(raw)) return raw
  return appBase ? `${appBase}${raw.startsWith('/') ? raw : `/${raw}`}` : raw
}

const onBannerChange = ({ detail }) => {
  bannerIndex.value = Number(detail?.current || 0)
}

const markBannerImageFailed = (key) => {
  failedBannerImages[key] = true
}

const resolveBannerTarget = (value) => {
  const raw = String(value || '').trim()
  if (!raw) return ''
  const planMatch = raw.match(/^\/app\/plan\/(\d+)$/)
  if (planMatch) return `/pages/plan-detail/index?id=${planMatch[1]}`
  if (/^\/app\/course/.test(raw)) return '/pages/courses/index'
  if (/^\/app\/booking/.test(raw)) return '/pages/booking/index'
  if (/^\/app\/order/.test(raw)) return '/pages/orders/index'
  if (/^\/app\/record|^\/app\/training/.test(raw)) return '/pages/training/index'
  if (raw.startsWith('/pages/')) return raw
  return raw
}

const openBanner = (banner) => {
  const url = String(banner?.targetUrl || '').trim()
  if (!url) {
    uni.showToast({ title: '该活动暂未配置跳转', icon: 'none' })
    return
  }
  if (url.startsWith('/pages/')) {
    goSmart(url)
    return
  }
  if (/^https?:\/\//.test(url)) {
    // #ifdef H5
    window.location.href = url
    // #endif
    // #ifndef H5
    uni.setClipboardData({
      data: url,
      success: () => uni.showToast({ title: '链接已复制', icon: 'success' })
    })
    // #endif
    return
  }
  uni.showToast({ title: '暂不支持该跳转地址', icon: 'none' })
}

const loadHome = async () => {
  try {
    await store.fetchProfile()
    const [planOverview, enrollmentsPayload, schedulesPayload, weekly, orders, bannerPayload, noticePayload] = await Promise.all([
      fetchPlanOverview(),
      listMyEnrollments(),
      listMyCourseSchedules(),
      getWeeklyStat(),
      listOrders({ limit: 50 }),
      listBanners(),
      listNotices({ limit: 5 })
    ])

    const myPlans = Array.isArray(planOverview?.data?.myPlans) ? planOverview.data.myPlans : []
    const enrollmentList = normalizeList(enrollmentsPayload)
    const scheduleList = normalizeList(schedulesPayload)
    const orderList = normalizeList(orders)
    const noticeRows = normalizeList(noticePayload)

    metrics.planCount = myPlans.length
    metrics.courseCount = countPurchasedCourses(enrollmentList, scheduleList)
    metrics.orderCount = orderList.length
    metrics.checkinCount = weekly?.data?.checkinCount ?? weekly?.data?.totalCount ?? 0

    const nextNotices = noticeRows.slice(0, 3).map((item, index) => ({
      key: `notice-${item.id || index}`,
      title: item.title || item.name || '系统公告',
      date: String(item.publishTime || item.createTime || item.createdAt || fallbackNotices[index]?.date || '')
        .slice(5, 10)
        .replace('-', '-')
    }))
    notices.splice(0, notices.length, ...nextNotices)

    const bannerRows = normalizeList(bannerPayload)
    banners.splice(0, banners.length, ...bannerRows)
    Object.keys(failedBannerImages).forEach((key) => {
      delete failedBannerImages[key]
    })
    bannerIndex.value = 0
  } catch (_) {
    // request layer has shown the toast; mock values keep the UI usable.
  }
}

onShow(() => {
  uni.hideTabBar()
  if (ensureLogin()) {
    loadHome()
  }
})
</script>

<style scoped lang="scss">
.home-page {
  min-height: 100vh;
  padding-bottom: 178rpx;
  overflow-x: hidden;
  background:
    radial-gradient(circle at 0% 5%, rgba(139, 99, 255, 0.14), transparent 30%),
    linear-gradient(180deg, #fff6f8 0%, #fffaf4 46%, #fffdf8 100%);
  color: #24104f;
}

.hero {
  position: relative;
  min-height: 338rpx;
  padding: calc(var(--status-bar-height) + 52rpx) 30rpx 0;
  overflow: hidden;
  background:
    radial-gradient(circle at 8% 0%, rgba(154, 130, 255, 0.42), transparent 32%),
    radial-gradient(circle at 88% 10%, rgba(255, 128, 111, 0.28), transparent 28%),
    linear-gradient(120deg, #d9c9ff 0%, #ffe0e7 48%, #fff3c9 100%);
}

.hero-cloud {
  position: absolute;
  z-index: 1;
  height: 18rpx;
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

.hero-cloud--left {
  left: 32rpx;
  top: 72rpx;
  width: 72rpx;
  filter: blur(1rpx);
}

.hero-cloud--left::before {
  left: 5rpx;
  width: 38rpx;
  height: 38rpx;
}

.hero-cloud--left::after {
  left: 28rpx;
  width: 46rpx;
  height: 46rpx;
}

.hero-cloud--mid {
  right: 225rpx;
  top: 154rpx;
  width: 86rpx;
}

.hero-cloud--mid::before {
  left: 15rpx;
  width: 36rpx;
  height: 36rpx;
}

.hero-cloud--mid::after {
  right: 11rpx;
  width: 48rpx;
  height: 48rpx;
}

.hero-sun {
  position: absolute;
  right: 42rpx;
  top: 118rpx;
  width: 130rpx;
  height: 130rpx;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(255, 171, 121, 0.6), transparent 68%);
}

.hill {
  position: absolute;
  left: -90rpx;
  right: -100rpx;
  border-radius: 52% 52% 0 0;
}

.hill--back {
  z-index: 1;
  bottom: 42rpx;
  height: 132rpx;
  background: linear-gradient(125deg, #8d72f3 0%, #c3b0ff 50%, #8066db 100%);
  transform: rotate(5deg);
}

.hill--front {
  z-index: 2;
  right: -250rpx;
  bottom: -30rpx;
  height: 138rpx;
  background: linear-gradient(130deg, #fff0c8 0%, #ffbe82 34%, #7b61d8 78%);
  transform: rotate(-8deg);
}

.trail {
  position: absolute;
  z-index: 3;
  right: 112rpx;
  bottom: 58rpx;
  width: 285rpx;
  height: 54rpx;
  border-radius: 50%;
  background: linear-gradient(90deg, rgba(255, 236, 175, 0.95), rgba(255, 149, 118, 0.68));
  transform: rotate(-13deg);
}

.forest-tree {
  position: absolute;
  z-index: 4;
}

.forest-crown {
  border: 4rpx solid #5b3aa6;
  border-radius: 48% 52% 45% 55%;
}

.forest-trunk {
  width: 7rpx;
  margin: -5rpx auto 0;
  border-radius: 999rpx;
  background: #5b3aa6;
}

.forest-tree--one {
  right: 40rpx;
  bottom: 58rpx;
}

.forest-tree--one .forest-crown {
  width: 60rpx;
  height: 82rpx;
  background: #ff8e82;
}

.forest-tree--one .forest-trunk {
  height: 54rpx;
}

.forest-tree--two {
  right: 118rpx;
  bottom: 45rpx;
}

.forest-tree--two .forest-crown {
  width: 52rpx;
  height: 66rpx;
  background: #c88bde;
}

.forest-tree--two .forest-trunk {
  height: 42rpx;
}

.forest-tree--three {
  right: 225rpx;
  bottom: 56rpx;
}

.forest-tree--three .forest-crown {
  width: 34rpx;
  height: 44rpx;
  background: #ff9a84;
}

.forest-tree--three .forest-trunk {
  height: 28rpx;
}

.top-actions {
  position: absolute;
  top: calc(var(--status-bar-height) + 34rpx);
  right: 28rpx;
  z-index: 8;
  display: flex;
  align-items: center;
  gap: 28rpx;
}

.icon-button {
  position: relative;
  display: grid;
  width: 58rpx;
  height: 58rpx;
  place-items: center;
}

.action-icon {
  width: 46rpx;
  height: 46rpx;
}

.notify-dot {
  position: absolute;
  right: 3rpx;
  top: 4rpx;
  width: 16rpx;
  height: 16rpx;
  border-radius: 50%;
  background: #ff806f;
}

.profile {
  position: relative;
  z-index: 7;
  display: flex;
  align-items: center;
  gap: 26rpx;
  margin-top: 22rpx;
}

.avatar {
  display: grid;
  width: 120rpx;
  height: 120rpx;
  overflow: hidden;
  place-items: center;
  border: 5rpx solid #24104f;
  border-radius: 50%;
  background: linear-gradient(135deg, #ffd7d0, #eee6ff);
  box-shadow: 0 8rpx 20rpx rgba(36, 16, 79, 0.12);
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
  font-size: 44rpx;
  font-weight: 900;
}

.avatar-face {
  position: relative;
  width: 86rpx;
  height: 96rpx;
}

.avatar-head {
  position: absolute;
  left: 17rpx;
  top: 20rpx;
  width: 54rpx;
  height: 58rpx;
  border: 3rpx solid #24104f;
  border-radius: 42% 42% 48% 48%;
  background: #ffc3ae;
}

.avatar-hair {
  position: absolute;
  left: 14rpx;
  top: 12rpx;
  z-index: 2;
  width: 62rpx;
  height: 34rpx;
  border: 3rpx solid #24104f;
  border-radius: 50% 50% 42% 38%;
  background: #6a3a35;
}

.avatar-ear {
  position: absolute;
  top: 46rpx;
  width: 11rpx;
  height: 16rpx;
  border: 3rpx solid #24104f;
  border-radius: 50%;
  background: #ffc3ae;
}

.avatar-ear--left {
  left: 9rpx;
}

.avatar-ear--right {
  right: 9rpx;
}

.avatar-eye {
  position: absolute;
  top: 28rpx;
  width: 5rpx;
  height: 5rpx;
  border-radius: 50%;
  background: #24104f;
}

.avatar-eye--left {
  left: 16rpx;
}

.avatar-eye--right {
  right: 16rpx;
}

.avatar-smile {
  position: absolute;
  left: 20rpx;
  bottom: 15rpx;
  width: 16rpx;
  height: 8rpx;
  border-bottom: 3rpx solid #24104f;
  border-radius: 0 0 999rpx 999rpx;
}

.avatar-body {
  position: absolute;
  left: 19rpx;
  bottom: 0;
  width: 52rpx;
  height: 28rpx;
  border: 3rpx solid #24104f;
  border-radius: 28rpx 28rpx 10rpx 10rpx;
  background: #6b58ba;
}

.profile-copy {
  min-width: 0;
  padding-right: 150rpx;
}

.greeting-row {
  display: flex;
  align-items: center;
  gap: 12rpx;
}

.greeting {
  max-width: 420rpx;
  overflow: hidden;
  color: #24104f;
  font-size: 39rpx;
  font-weight: 900;
  line-height: 1.15;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.wave {
  font-size: 32rpx;
}

.tags {
  display: flex;
  gap: 14rpx;
  margin-top: 16rpx;
}

.tag {
  min-width: 78rpx;
  padding: 8rpx 18rpx;
  border: 3rpx solid rgba(52, 32, 95, 0.18);
  border-radius: 18rpx;
  background: rgba(255, 255, 255, 0.28);
  color: #24104f;
  font-size: 25rpx;
  font-weight: 900;
  text-align: center;
  box-shadow: 0 6rpx 12rpx rgba(52, 32, 95, 0.08);
}

.content {
  position: relative;
  z-index: 10;
  margin-top: -42rpx;
  padding: 0 28rpx;
}

.stats-panel,
.quick-panel,
.notice-card,
.custom-tabbar {
  border: 3rpx solid rgba(120, 86, 170, 0.34);
  background: rgba(255, 255, 255, 0.94);
  box-shadow: 0 8rpx 0 rgba(52, 32, 95, 0.05), 0 18rpx 38rpx rgba(52, 32, 95, 0.1);
}

.stats-panel {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  min-height: 182rpx;
  overflow: hidden;
  border-radius: 30rpx;
}

.stat-item {
  position: relative;
  display: flex;
  align-items: center;
  flex-direction: column;
  justify-content: center;
  min-width: 0;
  padding: 20rpx 8rpx 16rpx;
}

.stat-item::after {
  content: "";
  position: absolute;
  top: 44rpx;
  right: 0;
  bottom: 42rpx;
  width: 2rpx;
  background: linear-gradient(180deg, transparent, rgba(88, 56, 130, 0.28), transparent);
}

.stat-item:last-child::after {
  display: none;
}

.stat-item--active {
  background: linear-gradient(112deg, rgba(139, 99, 255, 0.12), rgba(255, 255, 255, 0));
}

.stat-pill {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 9rpx;
  min-width: 116rpx;
  height: 55rpx;
  padding: 0 12rpx;
  border-radius: 999rpx;
}

.stat-icon {
  width: 31rpx;
  height: 31rpx;
}

.stat-pill text {
  color: #24104f;
  font-size: 21rpx;
  font-weight: 800;
}

.tone-violet {
  background: #eee6ff;
}

.tone-coral {
  background: #ffe3dc;
}

.tone-gold {
  background: #fff0c8;
}

.stat-label {
  margin-top: 16rpx;
  color: #24104f;
  font-size: 25rpx;
  font-weight: 800;
}

.stat-value {
  margin-top: 6rpx;
  color: #24104f;
  font-size: 38rpx;
  font-weight: 900;
  line-height: 1;
}

.chevron-icon {
  width: 20rpx;
  height: 20rpx;
  margin-top: 8rpx;
}

.quick-panel {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  margin-top: 28rpx;
  overflow: hidden;
  border-radius: 30rpx;
}

.quick-item {
  position: relative;
  min-height: 200rpx;
  padding: 28rpx 14rpx 22rpx;
  text-align: center;
}

.quick-item::after {
  content: "";
  position: absolute;
  top: 34rpx;
  right: 0;
  bottom: 34rpx;
  width: 2rpx;
  background: linear-gradient(180deg, transparent, rgba(88, 56, 130, 0.22), transparent);
}

.quick-item:nth-child(3n)::after {
  display: none;
}

.quick-item:nth-child(-n + 3)::before {
  content: "";
  position: absolute;
  left: 28rpx;
  right: 28rpx;
  bottom: 0;
  height: 2rpx;
  background: linear-gradient(90deg, transparent, rgba(88, 56, 130, 0.18), transparent);
}

.quick-icon {
  display: grid;
  width: 88rpx;
  height: 88rpx;
  margin: 0 auto;
  place-items: center;
  border-radius: 50%;
}

.quick-fa {
  width: 48rpx;
  height: 48rpx;
}

.quick-title {
  display: block;
  margin-top: 17rpx;
  color: #24104f;
  font-size: 29rpx;
  font-weight: 900;
}

.quick-meta {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12rpx;
  margin-top: 8rpx;
  color: #65527d;
  font-size: 22rpx;
  font-weight: 700;
}

.mini-chevron {
  width: 18rpx;
  height: 18rpx;
}

.promo-section {
  margin-top: 28rpx;
}

.promo-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin: 0 4rpx 14rpx;
}

.promo-head text:first-child {
  color: #24104f;
  font-size: 31rpx;
  font-weight: 900;
}

.promo-head text:last-child {
  color: #8d7fa8;
  font-size: 21rpx;
  font-weight: 800;
}

.promo-swiper {
  height: 214rpx;
  overflow: hidden;
  border-radius: 30rpx;
}

.promo-card {
  position: relative;
  height: 214rpx;
  overflow: hidden;
  border: 7rpx solid #ffffff;
  border-radius: 30rpx;
  background:
    radial-gradient(circle at 84% 78%, rgba(151, 117, 245, 0.28), transparent 22%),
    linear-gradient(110deg, #9e76ff 0%, #d88bde 48%, #ffad8d 100%);
  box-shadow: 0 8rpx 0 rgba(52, 32, 95, 0.09), 0 20rpx 32rpx rgba(52, 32, 95, 0.12);
}

.promo-copy {
  position: relative;
  z-index: 4;
  padding: 33rpx 220rpx 0 80rpx;
}

.promo-kicker {
  display: inline-flex;
  align-items: center;
  align-self: flex-start;
  min-height: 34rpx;
  padding: 0 14rpx;
  border-radius: 999rpx;
  background: rgba(255, 255, 255, 0.58);
  color: #5f4a85;
  font-size: 19rpx;
  font-weight: 900;
}

.promo-title {
  display: block;
  margin-top: 8rpx;
  overflow: hidden;
  color: #24104f;
  font-size: 35rpx;
  font-weight: 900;
  line-height: 1.15;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.promo-fire {
  font-size: 32rpx;
}

.promo-sub {
  display: block;
  margin-top: 16rpx;
  overflow: hidden;
  color: #24104f;
  font-size: 29rpx;
  font-weight: 800;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.promo-sub text {
  color: #ff806f;
  font-size: 36rpx;
  font-weight: 900;
}

.promo-button {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 9rpx;
  width: 152rpx;
  height: 48rpx;
  margin-top: 16rpx;
  border: 3rpx solid #24104f;
  border-radius: 999rpx;
  background: #fff9d8;
  color: #24104f;
  font-size: 23rpx;
  font-weight: 900;
}

.promo-button.disabled {
  opacity: 0.58;
}

.promo-arrow {
  width: 18rpx;
  height: 18rpx;
}

.promo-image {
  position: absolute;
  top: 0;
  right: 0;
  z-index: 3;
  width: 238rpx;
  height: 100%;
  opacity: 0.9;
}

.promo-image-fallback {
  position: absolute;
  top: 34rpx;
  right: 52rpx;
  z-index: 3;
  display: grid;
  width: 128rpx;
  height: 128rpx;
  place-items: center;
  border: 5rpx solid rgba(36, 16, 79, 0.16);
  border-radius: 34rpx;
  background: rgba(255, 255, 255, 0.34);
  transform: rotate(5deg);
}

.promo-image-fallback image {
  width: 50rpx;
  height: 50rpx;
}

.promo-image-fallback text {
  color: #24104f;
  font-size: 24rpx;
  font-weight: 900;
}

.promo-mountain {
  position: absolute;
  z-index: 1;
  right: 12rpx;
  bottom: -8rpx;
  border-radius: 50% 50% 0 0;
  background: rgba(125, 92, 222, 0.28);
}

.promo-mountain--one {
  width: 265rpx;
  height: 90rpx;
}

.promo-mountain--two {
  right: 170rpx;
  width: 150rpx;
  height: 58rpx;
  background: rgba(255, 225, 216, 0.32);
}

.runner {
  position: absolute;
  right: 84rpx;
  bottom: 34rpx;
  z-index: 3;
  width: 150rpx;
  height: 152rpx;
}

.runner-head {
  position: absolute;
  left: 55rpx;
  top: 8rpx;
  width: 37rpx;
  height: 40rpx;
  border: 3rpx solid #24104f;
  border-radius: 50%;
  background: #ffc3ae;
}

.runner-hair {
  position: absolute;
  left: 70rpx;
  top: 0;
  width: 58rpx;
  height: 42rpx;
  border-radius: 50%;
  border-top: 5rpx solid #24104f;
  transform: rotate(16deg);
}

.runner-body {
  position: absolute;
  left: 55rpx;
  top: 48rpx;
  width: 54rpx;
  height: 56rpx;
  border: 3rpx solid #24104f;
  border-radius: 48% 48% 16rpx 16rpx;
  background: #7b60d8;
  transform: rotate(-10deg);
}

.runner-arm,
.runner-leg {
  position: absolute;
  border-radius: 999rpx;
  background: #372063;
}

.runner-arm {
  width: 8rpx;
  height: 62rpx;
  top: 46rpx;
}

.runner-arm--back {
  left: 99rpx;
  transform: rotate(42deg);
}

.runner-arm--front {
  left: 45rpx;
  transform: rotate(-44deg);
}

.runner-leg {
  height: 10rpx;
}

.runner-leg--front {
  left: 16rpx;
  bottom: 20rpx;
  width: 95rpx;
  transform: rotate(-4deg);
}

.runner-leg--back {
  right: 0;
  bottom: 28rpx;
  width: 86rpx;
  transform: rotate(25deg);
}

.runner-foot {
  position: absolute;
  width: 34rpx;
  height: 10rpx;
  border-radius: 999rpx;
  background: #ffffff;
  border: 3rpx solid #24104f;
}

.runner-foot--front {
  left: 6rpx;
  bottom: 12rpx;
}

.runner-foot--back {
  right: -13rpx;
  bottom: 20rpx;
  transform: rotate(14deg);
}

.dots {
  position: relative;
  z-index: 12;
  display: flex;
  justify-content: center;
  gap: 10rpx;
  margin-top: -30rpx;
  pointer-events: none;
}

.dot {
  width: 16rpx;
  height: 16rpx;
  border-radius: 999rpx;
  background: rgba(77, 49, 127, 0.36);
}

.dot.active {
  width: 35rpx;
  background: #5e3cad;
}

.promo-empty {
  display: grid;
  grid-template-columns: 76rpx minmax(0, 1fr);
  gap: 18rpx;
  align-items: center;
  min-height: 154rpx;
  padding: 24rpx;
  border: 3rpx solid rgba(120, 86, 170, 0.22);
  border-radius: 30rpx;
  background: rgba(255, 255, 255, 0.88);
  box-shadow: 0 8rpx 0 rgba(52, 32, 95, 0.05), 0 18rpx 38rpx rgba(52, 32, 95, 0.08);
}

.promo-empty-icon {
  display: grid;
  width: 68rpx;
  height: 68rpx;
  place-items: center;
  border-radius: 22rpx;
  background: #eee6ff;
}

.promo-empty-icon image {
  width: 38rpx;
  height: 38rpx;
}

.promo-empty-copy text {
  display: block;
}

.promo-empty-copy text:first-child {
  color: #24104f;
  font-size: 28rpx;
  font-weight: 900;
}

.promo-empty-copy text:last-child {
  margin-top: 8rpx;
  color: #7b6f98;
  font-size: 22rpx;
  font-weight: 800;
  line-height: 1.45;
}

.notice-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin: 34rpx 2rpx 18rpx;
}

.notice-head > text {
  color: #24104f;
  font-size: 34rpx;
  font-weight: 900;
}

.notice-more {
  display: flex;
  align-items: center;
  gap: 8rpx;
  color: #24104f;
  font-size: 27rpx;
  font-weight: 800;
}

.notice-card {
  overflow: hidden;
  border-color: rgba(120, 86, 170, 0.18);
  border-radius: 24rpx;
  box-shadow: none;
}

.notice-row {
  display: flex;
  align-items: center;
  gap: 17rpx;
  min-height: 76rpx;
  padding: 0 25rpx;
}

.notice-row + .notice-row {
  border-top: 2rpx solid #efe7ef;
}

.notice-icon {
  display: grid;
  flex: 0 0 auto;
  width: 44rpx;
  height: 44rpx;
  place-items: center;
  border-radius: 12rpx;
  background: #eee6ff;
}

.notice-icon image {
  width: 25rpx;
  height: 25rpx;
}

.notice-title {
  flex: 1;
  min-width: 0;
  overflow: hidden;
  color: #24104f;
  font-size: 25rpx;
  font-weight: 700;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.notice-date {
  color: #7b6f98;
  font-size: 23rpx;
  font-weight: 700;
}

.custom-tabbar {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  z-index: 50;
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  min-height: 137rpx;
  padding: 16rpx 18rpx calc(env(safe-area-inset-bottom) + 23rpx);
  border-width: 5rpx 5rpx 5rpx;
  border-radius: 32rpx 32rpx 0 0;
  background: rgba(255, 255, 255, 0.96);
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
  .greeting {
    max-width: 350rpx;
    font-size: 34rpx;
  }

  .stats-panel {
    min-height: 174rpx;
  }

  .stat-pill {
    min-width: 100rpx;
  }

  .stat-pill text {
    font-size: 19rpx;
  }

  .quick-title {
    font-size: 26rpx;
  }
}
</style>
