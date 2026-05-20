<template>
  <view class="booking-page">
    <view class="topbar">
      <button class="icon-button" aria-label="返回" @click="goBackHome">
        <image class="top-icon" :src="icons.back" mode="aspectFit" />
      </button>
      <text class="topbar-title">预约大厅</text>
      <button class="icon-button" aria-label="筛选" @click="filterVisible = true">
        <image class="top-icon" :src="icons.filter" mode="aspectFit" />
      </button>
    </view>

    <view class="hero">
      <view class="hero-cloud hero-cloud--one"></view>
      <view class="hero-cloud hero-cloud--two"></view>
      <view class="hero-hill hero-hill--back"></view>
      <view class="hero-hill hero-hill--front"></view>
      <view class="hero-person">
        <view class="person-head"></view>
        <view class="person-hair"></view>
        <view class="person-body"></view>
        <view class="person-arm person-arm--left"></view>
        <view class="person-arm person-arm--right"></view>
        <view class="person-clipboard"></view>
      </view>
      <view class="hero-copy">
        <text class="hero-title">预约教练，</text>
        <text class="hero-title">形成稳定训练节律</text>
        <text class="hero-sub">筛选、选档、支付、完成和评价，都在手机端同权处理。</text>
      </view>
    </view>

    <scroll-view class="coach-chip-scroll" scroll-x show-scrollbar="false">
      <view class="coach-chip-row">
        <button class="coach-chip" :class="{ active: coachId === null }" @click="setCoach(null)">
          全部教练
        </button>
        <button
          v-for="coach in coachPickerOptions"
          :key="coach.coachId"
          class="coach-chip"
          :class="{ active: Number(coachId) === Number(coach.coachId) }"
          @click="setCoach(coach.coachId)"
        >
          {{ coachDisplayName(coach.coachId) }}
        </button>
      </view>
    </scroll-view>

    <view class="date-card">
      <button class="date-side" @click="clearDate">
        <image :src="icons.shield" mode="aspectFit" />
        <text>全部</text>
      </button>
      <scroll-view class="date-scroll" scroll-x show-scrollbar="false">
        <view class="date-row">
          <button
            v-for="item in dateOptions"
            :key="item.value"
            class="date-item"
            :class="{ active: date === item.value }"
            @click="setDate(item.value)"
          >
            <text class="date-week">{{ item.label }}</text>
            <text class="date-value">{{ item.short }}</text>
          </button>
        </view>
      </scroll-view>
      <picker mode="date" :value="date || todayValue" @change="onDatePick">
        <view class="date-side">
          <image :src="icons.calendar" mode="aspectFit" />
          <text>日历</text>
        </view>
      </picker>
    </view>

    <view class="summary-strip">
      <view class="summary-item">
        <text>{{ visibleSchedules.length }}</text>
        <text>当前可约</text>
      </view>
      <view class="summary-item">
        <text>{{ effectiveAvailableSchedules }}</text>
        <text>未来可约</text>
      </view>
      <view class="summary-item">
        <text>{{ myBookings.length }}</text>
        <text>我的预约</text>
      </view>
    </view>

    <view v-if="lastBooking" class="todo-card" :class="todoToneClass">
      <view class="todo-head">
        <view class="todo-title-wrap">
          <image class="todo-icon" :src="icons.clock" mode="aspectFit" />
          <text class="todo-title">当前待办</text>
        </view>
        <text class="countdown-pill">{{ formatBookingStatus(lastBooking.bookingStatus) }}</text>
      </view>
      <view class="todo-main">
        <view class="coach-avatar">
          <image v-if="resolveCoachPhoto(lastBooking)" :src="resolveCoachPhoto(lastBooking)" mode="aspectFit" />
          <text v-else>{{ coachInitial(lastBooking.coachId) }}</text>
        </view>
        <view class="todo-info">
          <view class="todo-name-row">
            <text class="coach-name">{{ bookingCoachDisplayName(lastBooking) }}</text>
            <text class="soft-chip">{{ formatPayStatus(lastBooking.payStatus) }}</text>
          </view>
          <view class="todo-line">
            <image :src="icons.location" mode="aspectFit" />
            <text>预约ID {{ lastBooking.bookingId || lastBooking.id }} · 订单 {{ lastBooking.orderId || '-' }}</text>
          </view>
          <view class="todo-line">
            <image :src="icons.calendar" mode="aspectFit" />
            <text>{{ formatScheduleTime(lastBooking) }}</text>
          </view>
        </view>
        <view class="price-col">
          <text class="price">¥{{ formatAmount(lastBooking.amount) }}</text>
          <text>{{ formatPayStatus(lastBooking.payStatus) }}</text>
        </view>
      </view>
      <view class="todo-actions">
        <button v-if="canMockPay(lastBooking)" class="pill-action primary" :disabled="paying" @click="mockPay(lastBooking)">模拟支付</button>
        <button v-if="canCancelBooking(lastBooking)" class="pill-action danger" :disabled="isCancellingBooking(lastBooking)" @click="cancelBooking(lastBooking)">取消预约</button>
        <button v-if="canCompleteBooking(lastBooking)" class="pill-action success" :disabled="completing" @click="completeSelectedBooking(lastBooking)">确认完成</button>
        <button class="pill-action ghost" :disabled="!lastBooking.orderId" @click="goToOrder(lastBooking)">订单详情</button>
      </view>
    </view>

    <view class="mode-tabs">
      <button :class="{ active: activeTab === 'schedules' }" @click="activeTab = 'schedules'">可预约档期</button>
      <button :class="{ active: activeTab === 'bookings' }" @click="activeTab = 'bookings'">我的预约记录</button>
    </view>

    <template v-if="activeTab === 'schedules'">
      <view class="section-head">
        <view>
          <text class="section-title">可预约时段</text>
          <text class="section-sub">{{ scheduleSummaryHint }}</text>
        </view>
        <button class="refresh-button" :disabled="loading" @click="loadSchedules">刷新</button>
      </view>

      <view v-if="!visibleSchedules.length && !loading" class="empty-card">
        <image class="empty-icon" :src="icons.calendar" mode="aspectFit" />
        <text class="empty-title">暂无可预约时间段</text>
        <text class="empty-desc">{{ scheduleEmptyReason }}</text>
      </view>

      <view v-else class="schedule-list">
        <view
          v-for="row in visibleSchedules"
          :key="row.id"
          class="schedule-card"
          :class="{ active: selected?.id === row.id }"
          @click="selectSchedule(row)"
        >
          <view class="coach-avatar small">
            <image v-if="resolveCoachPhoto(row)" :src="resolveCoachPhoto(row)" mode="aspectFit" />
            <text v-else>{{ coachInitial(row.coachId) }}</text>
          </view>
          <view class="schedule-info">
            <view class="coach-row">
              <text class="coach-name">{{ bookingCoachDisplayName(row) }}</text>
              <text class="status-dot">可约</text>
            </view>
            <view class="rating-row">
              <image :src="icons.star" mode="aspectFit" />
              <text>{{ coachRating(row) }}</text>
              <text>|</text>
              <text>剩余 {{ remainingSlots(row) }} 个名额</text>
            </view>
            <view class="tag-row">
              <text v-for="tag in coachTags(row)" :key="tag">{{ tag }}</text>
            </view>
          </view>
          <view class="time-price">
            <text class="time">{{ formatTime(row.startTime) }} - {{ formatTime(row.endTime) }}</text>
            <text class="slot-date">{{ formatDate(row.scheduleDate || row.startTime) }}</text>
            <text class="price">¥{{ formatAmount(row.price) }}</text>
            <button class="book-button" :disabled="submitting && selected?.id === row.id" @click.stop="confirmBooking(row)">
              {{ selected?.id === row.id ? '提交预约' : '预约' }}
            </button>
          </view>
        </view>
      </view>

      <view v-if="selected" class="selected-card">
        <view>
          <text class="selected-title">已选时间段</text>
          <text class="selected-sub">{{ bookingCoachDisplayName(selected) }} · {{ formatScheduleTime(selected) }}</text>
        </view>
        <button class="selected-submit" :disabled="selectedUnavailable || submitting" @click="createSelectedBooking">
          {{ submitting ? '提交中...' : '确认提交预约' }}
        </button>
      </view>

      <view class="package-banner" @click="goCourses">
        <view class="package-icon">
          <image :src="icons.calendarCheck" mode="aspectFit" />
        </view>
        <view>
          <text class="package-title">想长期稳定训练？</text>
          <text class="package-sub">课程目录也已接入报名与订单处理</text>
        </view>
        <view class="package-button">
          <text>查看课程</text>
          <image :src="icons.right" mode="aspectFit" />
        </view>
      </view>

      <view class="shortcut-card">
        <button class="shortcut-item" @click="activeTab = 'bookings'">
          <view class="shortcut-icon"><image :src="icons.clipboard" mode="aspectFit" /></view>
          <view>
            <text>我的预约</text>
            <text>查看全部预约记录</text>
          </view>
          <image class="shortcut-arrow" :src="icons.right" mode="aspectFit" />
        </button>
        <button class="shortcut-item" @click="openReviewFromCompleted">
          <view class="shortcut-icon"><image :src="icons.pen" mode="aspectFit" /></view>
          <view>
            <text>写评价</text>
            <text>分享你的训练体验</text>
          </view>
          <image class="shortcut-arrow" :src="icons.right" mode="aspectFit" />
        </button>
      </view>
    </template>

    <template v-else>
      <view class="section-head">
        <view>
          <text class="section-title">我的预约</text>
          <text class="section-sub">PC 端可处理的支付、取消、完成、评价，手机端同样保留。</text>
        </view>
        <button class="refresh-button" :disabled="myLoading" @click="loadMyBookings">刷新</button>
      </view>

      <view v-if="!myBookings.length && !myLoading" class="empty-card">
        <image class="empty-icon" :src="icons.clipboard" mode="aspectFit" />
        <text class="empty-title">暂无预约记录</text>
        <text class="empty-desc">完成一次预约后，这里会同步订单和评价入口。</text>
      </view>

      <view v-else class="booking-list">
        <view v-for="row in myBookings" :key="row.id" class="booking-card">
          <view class="booking-card-head">
            <view>
              <text class="booking-title">预约 {{ row.id }}</text>
              <text class="booking-sub">{{ bookingCoachDisplayName(row) }} · {{ formatScheduleTime(row) }}</text>
            </view>
            <text class="booking-status" :class="bookingTone(row.bookingStatus)">{{ formatBookingStatus(row.bookingStatus) }}</text>
          </view>
          <view class="booking-meta">
            <text>订单 {{ row.orderId || '-' }}</text>
            <text>金额 ¥{{ formatAmount(row.amount) }}</text>
            <text>支付 {{ formatPayStatus(row.payStatus) }}</text>
          </view>
          <view class="todo-actions compact">
            <button v-if="canMockPay(row)" class="pill-action primary" @click="mockPay(row)">模拟支付</button>
            <button v-if="canCancelBooking(row)" class="pill-action danger" :disabled="isCancellingBooking(row)" @click="cancelBooking(row)">取消预约</button>
            <button v-if="canCompleteBooking(row)" class="pill-action success" @click="completeSelectedBooking(row)">确认完成</button>
            <button v-if="row.bookingStatus === 'COMPLETED'" class="pill-action primary" @click="openReview(row)">写评价</button>
            <button class="pill-action ghost" :disabled="!row.orderId" @click="goToOrder(row)">订单详情</button>
          </view>
        </view>
      </view>

      <view v-if="reviewVisible" class="review-card">
        <view class="section-head compact-head">
          <view>
            <text class="section-title">写评价</text>
            <text class="section-sub">仅已完成预约可评价，且后端限制每单只能评价一次。</text>
          </view>
          <button class="refresh-button" @click="fillReviewSample">示例</button>
        </view>
        <view class="score-row">
          <button v-for="score in [1, 2, 3, 4, 5]" :key="score" :class="{ active: reviewForm.score === score }" @click="reviewForm.score = score">
            {{ score }} 星
          </button>
        </view>
        <textarea
          v-model.trim="reviewForm.content"
          class="review-textarea"
          maxlength="500"
          placeholder="说说教练安排、沟通和训练感受"
        />
        <button class="selected-submit full" :disabled="reviewing || !reviewForm.bookingId" @click="submitReview">
          {{ reviewing ? '提交中...' : '提交评价' }}
        </button>
      </view>
    </template>

    <view class="safe-note">
      <image :src="icons.shield" mode="aspectFit" />
      <text>教练通过平台认证 · 训练安全有保障</text>
    </view>

    <view v-if="filterVisible" class="sheet-mask" @click="filterVisible = false">
      <view class="filter-sheet" @click.stop>
        <view class="sheet-head">
          <view>
            <text class="sheet-title">筛选预约档期</text>
            <text class="sheet-sub">教练和日期会直接请求真实可预约接口。</text>
          </view>
          <button class="sheet-close" @click="filterVisible = false">
            <image :src="icons.close" mode="aspectFit" />
          </button>
        </view>
        <view class="sheet-section">
          <text class="sheet-label">教练</text>
          <view class="coach-grid">
            <button :class="{ active: coachId === null }" @click="setCoach(null)">全部</button>
            <button
              v-for="coach in coachPickerOptions"
              :key="coach.coachId"
              :class="{ active: Number(coachId) === Number(coach.coachId) }"
              @click="setCoach(coach.coachId)"
            >
              {{ coachDisplayName(coach.coachId) }}
            </button>
          </view>
        </view>
        <view class="sheet-section">
          <text class="sheet-label">日期</text>
          <picker mode="date" :value="date || todayValue" @change="onDatePick">
            <view class="date-picker-field">
              <image :src="icons.calendar" mode="aspectFit" />
              <text>{{ date || '选择某一天' }}</text>
            </view>
          </picker>
        </view>
        <view class="sheet-actions">
          <button class="pill-action ghost" @click="clearFilters">清空</button>
          <button class="pill-action primary" @click="applyFilters">查询</button>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup>
import {
  faCalendarCheck,
  faCalendarDays,
  faChevronLeft,
  faChevronRight,
  faCircleXmark,
  faClipboardList,
  faClock,
  faFilter,
  faLocationDot,
  faPenToSquare,
  faShieldHeart,
  faStar
} from '@fortawesome/free-solid-svg-icons'
import { computed, reactive, ref, watch } from 'vue'
import { onHide, onShow } from '@dcloudio/uni-app'
import {
  cancelUnpaidBooking,
  completeBooking,
  createBooking,
  getBookingScheduleSummary,
  listBookingCoachOptions,
  listBookingSchedules,
  listMyBookings,
  mockPayBooking,
  reviewBooking
} from '../../api/modules/booking'
import { ensureLogin } from '../../utils/authGuard'
import { hideTabBarSafely, showTabBarSafely } from '../../utils/navigation'

const STORAGE_BOOKING = 'fp_last_booking'
const STORAGE_COACH_ID = 'fp_last_coach_id'

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
  calendar: faIcon(faCalendarDays, '#7b6f98'),
  calendarCheck: faIcon(faCalendarCheck),
  clipboard: faIcon(faClipboardList),
  clock: faIcon(faClock, '#ff6f61'),
  close: faIcon(faCircleXmark),
  filter: faIcon(faFilter),
  location: faIcon(faLocationDot, '#ff6f61'),
  pen: faIcon(faPenToSquare),
  right: faIcon(faChevronRight),
  shield: faIcon(faShieldHeart, '#8b63ff'),
  star: faIcon(faStar, '#ffb23f')
}

const todayValue = new Date().toISOString().slice(0, 10)
const activeTab = ref('schedules')
const filterVisible = ref(false)
const loading = ref(false)
const myLoading = ref(false)
const coachOptionsLoading = ref(false)
const submitting = ref(false)
const paying = ref(false)
const completing = ref(false)
const cancelingBookingId = ref(null)
const reviewing = ref(false)
const reviewVisible = ref(false)

const schedules = ref([])
const myBookings = ref([])
const coachOptions = ref([])
const selected = ref(null)
const lastBooking = ref(null)
const coachId = ref(null)
const date = ref('')
const scheduleSummary = ref({})
const coachNameCache = reactive({})
const reviewForm = reactive({
  bookingId: '',
  score: 5,
  content: ''
})

const dateOptions = computed(() => {
  const labels = ['今天', '明天', '周一', '周二', '周三', '周四', '周五', '周六', '周日']
  const base = new Date()
  return Array.from({ length: 7 }).map((_, index) => {
    const d = new Date(base)
    d.setDate(base.getDate() + index)
    const value = d.toISOString().slice(0, 10)
    const week = index < 2 ? labels[index] : labels[d.getDay() + 2]
    return {
      value,
      label: week,
      short: value.slice(5)
    }
  })
})

const visibleSchedules = computed(() => schedules.value.filter((row) => isScheduleAvailable(row)))
const availableSchedules = computed(() => visibleSchedules.value.length)
const effectiveAvailableSchedules = computed(() => {
  const count = Number(scheduleSummary.value?.availableSchedules)
  return Number.isFinite(count) ? count : availableSchedules.value
})
const selectedUnavailable = computed(() => !selected.value || !isScheduleAvailable(selected.value))
const coachPickerOptions = computed(() => (coachOptions.value.length ? coachOptions.value : derivedCoachOptions.value))
const coachOptionMap = computed(() => {
  const map = new Map()
  coachPickerOptions.value.forEach((coach) => map.set(Number(coach.coachId), coach))
  return map
})
const derivedCoachOptions = computed(() => {
  const map = new Map()
  schedules.value.forEach((row) => {
    const id = Number(row.coachId)
    if (!Number.isFinite(id) || map.has(id)) return
    map.set(id, {
      coachId: id,
      displayName: row.coachName || row.coachDisplayName || `教练 ${id}`,
      nickname: row.coachNickname || '',
      username: row.coachUsername || '',
      avatar: row.coachAvatar || row.avatar || '',
      expertise: row.specialties || row.tags || coachPersona(id).tags.join(' ')
    })
  })
  return Array.from(map.values())
})
const scheduleSummaryHint = computed(() => {
  const nextDate = formatDate(scheduleSummary.value?.nextScheduleDate)
  if (nextDate !== '-') return `下一批未来可预约时间从 ${nextDate} 开始。`
  return '这里显示实时可预约摘要，不是静态演示数字。'
})
const scheduleEmptyReason = computed(() => {
  if (date.value && effectiveAvailableSchedules.value > 0) {
    return `当前日期 ${date.value} 没有可预约时间段，清空日期后仍有 ${effectiveAvailableSchedules.value} 个未来可预约时间段。`
  }
  if (!scheduleSummary.value?.futureSchedules) {
    const lastDate = formatDate(scheduleSummary.value?.lastScheduleDate)
    return lastDate === '-'
      ? '当前系统里还没有任何教练可预约时间段，请先在管理端创建。'
      : `当前没有未来可预约时间段，最近一次开放时间是 ${lastDate}。`
  }
  return '当前筛选条件下没有可预约时间段，可以换教练或日期。'
})
const todoToneClass = computed(() => ({
  'is-pay': lastBooking.value?.bookingStatus === 'WAIT_PAY',
  'is-finish': lastBooking.value?.bookingStatus === 'PAID',
  'is-done': lastBooking.value?.bookingStatus === 'COMPLETED'
}))

const coachPersonas = [
  { tags: ['力量提升', '增肌塑形'] },
  { tags: ['减脂燃脂', '体态改善'] },
  { tags: ['核心训练', '综合训练'] },
  { tags: ['普拉提', '柔韧提升'] }
]

const coachPersona = (id) => coachPersonas[Math.abs(Math.trunc(Number(id) || 0)) % coachPersonas.length]

const normalizeList = (payload) => {
  const data = payload?.data ?? payload
  if (Array.isArray(data)) return data
  if (Array.isArray(data?.list)) return data.list
  if (Array.isArray(data?.records)) return data.records
  return []
}

const parseCoachId = (value) => {
  if (value === null || value === undefined || value === '') return null
  const parsed = Number(value)
  return Number.isFinite(parsed) ? parsed : null
}

const rememberCoachName = (id, ...candidates) => {
  const parsed = Number(id)
  if (!Number.isFinite(parsed)) return ''
  const name = candidates.map((item) => String(item || '').trim()).find(Boolean)
  if (name) coachNameCache[parsed] = name
  return name || ''
}

const rememberCoachNames = (rows = []) => {
  rows.forEach((row) => rememberCoachName(row.coachId, row.coachName, row.coachDisplayName, row.displayName, row.nickname, row.username))
}

const coachDisplayName = (id) => {
  const parsed = Number(id)
  if (!Number.isFinite(parsed)) return '-'
  if (coachNameCache[parsed]) return coachNameCache[parsed]
  const coach = coachOptionMap.value.get(parsed)
  return rememberCoachName(parsed, coach?.displayName, coach?.nickname, coach?.username) || `教练 ${parsed}`
}

const bookingCoachDisplayName = (row) => {
  if (!row) return '-'
  return rememberCoachName(row.coachId, row.coachName, row.coachDisplayName, row.coachNickname, row.coachUsername) || coachDisplayName(row.coachId)
}

const coachInitial = (id) => coachDisplayName(id).slice(0, 1)

const resolveCoachPhoto = (row) => {
  const coach = coachOptionMap.value.get(Number(row?.coachId))
  return coach?.avatar || row?.coachAvatar || row?.avatar || row?.photoUrl || row?.imageUrl || ''
}

const coachRating = (row) => {
  const coach = coachOptionMap.value.get(Number(row?.coachId))
  const rating = row?.rating ?? coach?.rating
  return rating ?? '4.9'
}

const coachTags = (row) => {
  const coach = coachOptionMap.value.get(Number(row?.coachId))
  const raw = row?.specialties || row?.tags || coach?.expertise
  if (Array.isArray(raw)) return raw.filter(Boolean).slice(0, 2)
  if (typeof raw === 'string' && raw.trim()) return raw.split(/[、，,\s]+/).filter(Boolean).slice(0, 2)
  return coachPersona(row?.coachId).tags
}

const loadCoachOptions = async () => {
  try {
    coachOptionsLoading.value = true
    const payload = await listBookingCoachOptions()
    coachOptions.value = normalizeList(payload)
    rememberCoachNames(coachOptions.value)
  } finally {
    coachOptionsLoading.value = false
  }
}

const loadScheduleSummary = async () => {
  try {
    const params = {}
    const parsedCoachId = parseCoachId(coachId.value)
    if (parsedCoachId !== null) params.coachId = parsedCoachId
    const payload = await getBookingScheduleSummary(params)
    scheduleSummary.value = payload?.data || payload || {}
  } catch (_) {
    scheduleSummary.value = {
      futureSchedules: schedules.value.length,
      availableSchedules: availableSchedules.value,
      lastScheduleDate: schedules.value.at(-1)?.scheduleDate || null,
      nextScheduleDate: schedules.value[0]?.scheduleDate || null
    }
  }
}

const loadSchedules = async () => {
  try {
    loading.value = true
    const params = {}
    const parsedCoachId = parseCoachId(coachId.value)
    if (parsedCoachId !== null) params.coachId = parsedCoachId
    if (date.value) params.date = date.value
    const payload = await listBookingSchedules(params)
    schedules.value = normalizeList(payload)
    rememberCoachNames(schedules.value)
    if (!selected.value || !schedules.value.some((item) => Number(item.id) === Number(selected.value.id))) {
      selected.value = visibleSchedules.value[0] || null
    }
    await loadScheduleSummary()
  } finally {
    loading.value = false
  }
}

const loadMyBookings = async () => {
  try {
    myLoading.value = true
    const payload = await listMyBookings()
    myBookings.value = normalizeList(payload).map((item) => toRecentBooking(item))
    rememberCoachNames(myBookings.value)
    syncLastBooking(myBookings.value)
  } finally {
    myLoading.value = false
  }
}

const refreshWorkspace = async () => {
  await Promise.all([loadCoachOptions(), loadSchedules(), loadMyBookings()])
}

const toRecentBooking = (item) => ({
  ...item,
  bookingId: item?.bookingId ?? item?.id
})

const syncLastBooking = (rows = []) => {
  const urgent = rows.find((item) => ['WAIT_PAY', 'PAID'].includes(item.bookingStatus))
  lastBooking.value = urgent || rows[0] || null
  if (lastBooking.value) uni.setStorageSync(STORAGE_BOOKING, JSON.stringify(lastBooking.value))
  else uni.removeStorageSync(STORAGE_BOOKING)
}

const loadLastBooking = () => {
  try {
    const cached = uni.getStorageSync(STORAGE_BOOKING)
    if (cached) lastBooking.value = toRecentBooking(typeof cached === 'string' ? JSON.parse(cached) : cached)
  } catch (_) {
    lastBooking.value = null
  }
}

const setCoach = (value) => {
  coachId.value = parseCoachId(value)
  if (coachId.value === null) uni.removeStorageSync(STORAGE_COACH_ID)
  else uni.setStorageSync(STORAGE_COACH_ID, String(coachId.value))
}

const setDate = (value) => {
  date.value = value
}

const clearDate = () => {
  date.value = ''
}

const clearFilters = () => {
  setCoach(null)
  date.value = ''
}

const applyFilters = async () => {
  filterVisible.value = false
  await loadSchedules()
}

const onDatePick = (event) => {
  date.value = event.detail.value
}

const selectSchedule = (row) => {
  if (!isScheduleAvailable(row)) {
    uni.showToast({ title: '该时间段不可预约', icon: 'none' })
    return
  }
  selected.value = row
}

const confirmBooking = async (row) => {
  selectSchedule(row)
  await createSelectedBooking()
}

const createSelectedBooking = async () => {
  if (!selected.value?.id || selectedUnavailable.value) {
    uni.showToast({ title: '请选择可预约时段', icon: 'none' })
    return
  }
  try {
    submitting.value = true
    const payload = await createBooking({ scheduleId: selected.value.id })
    lastBooking.value = toRecentBooking({
      ...(payload?.data || payload),
      coachId: selected.value.coachId,
      coachName: bookingCoachDisplayName(selected.value),
      scheduleDate: selected.value.scheduleDate,
      startTime: selected.value.startTime,
      endTime: selected.value.endTime,
      bookingStatus: 'WAIT_PAY',
      payStatus: 'UNPAID'
    })
    uni.setStorageSync(STORAGE_BOOKING, JSON.stringify(lastBooking.value))
    uni.showToast({ title: '预约已创建', icon: 'success' })
    selected.value = null
    await Promise.all([loadSchedules(), loadMyBookings()])
  } finally {
    submitting.value = false
  }
}

const mockPay = async (row = lastBooking.value) => {
  const bookingId = Number(row?.bookingId || row?.id)
  if (!bookingId) return
  try {
    paying.value = true
    await mockPayBooking(bookingId)
    uni.showToast({ title: '支付状态已更新', icon: 'success' })
    await loadMyBookings()
  } finally {
    paying.value = false
  }
}

const completeSelectedBooking = async (row = lastBooking.value) => {
  const bookingId = Number(row?.bookingId || row?.id)
  if (!bookingId) return
  try {
    completing.value = true
    await completeBooking(bookingId)
    uni.showToast({ title: '预约已完成', icon: 'success' })
    await loadMyBookings()
  } finally {
    completing.value = false
  }
}

const cancelBooking = async (row = lastBooking.value) => {
  const bookingId = Number(row?.bookingId || row?.id)
  if (!bookingId || !canCancelBooking(row)) return
  uni.showModal({
    title: '取消预约',
    content: '取消后会释放当前预约占用的名额，确认继续吗？',
    confirmText: '确认取消',
    success: async (res) => {
      if (!res.confirm) return
      try {
        cancelingBookingId.value = bookingId
        await cancelUnpaidBooking(bookingId)
        uni.showToast({ title: '预约已取消', icon: 'success' })
        if (Number(lastBooking.value?.bookingId || lastBooking.value?.id) === bookingId) {
          lastBooking.value = null
          uni.removeStorageSync(STORAGE_BOOKING)
        }
        await Promise.all([loadSchedules(), loadMyBookings()])
      } finally {
        cancelingBookingId.value = null
      }
    }
  })
}

const openReviewFromCompleted = () => {
  const target = myBookings.value.find((row) => row.bookingStatus === 'COMPLETED')
  if (!target) {
    uni.showToast({ title: '暂无可评价预约', icon: 'none' })
    activeTab.value = 'bookings'
    return
  }
  openReview(target)
}

const openReview = (row) => {
  if (row.bookingStatus !== 'COMPLETED') {
    uni.showToast({ title: '只有已完成预约才能评价', icon: 'none' })
    return
  }
  activeTab.value = 'bookings'
  reviewVisible.value = true
  reviewForm.bookingId = row.id
  reviewForm.score = 5
  reviewForm.content = ''
}

const fillReviewSample = () => {
  reviewForm.score = 5
  reviewForm.content = '教练节奏安排得很稳，讲解清楚，训练后反馈也很及时。'
}

const submitReview = async () => {
  if (!reviewForm.bookingId || !reviewForm.content.trim()) {
    uni.showToast({ title: '请选择预约并填写评价', icon: 'none' })
    return
  }
  try {
    reviewing.value = true
    await reviewBooking({
      bookingId: Number(reviewForm.bookingId),
      score: reviewForm.score,
      content: reviewForm.content.trim()
    })
    uni.showToast({ title: '评价已提交', icon: 'success' })
    reviewVisible.value = false
    reviewForm.content = ''
    await loadMyBookings()
  } finally {
    reviewing.value = false
  }
}

const canMockPay = (item) => item?.bookingStatus === 'WAIT_PAY' && item?.payStatus === 'UNPAID'
const canCancelBooking = (item) => item?.bookingStatus === 'WAIT_PAY' && item?.payStatus === 'UNPAID'
const canCompleteBooking = (item) => item?.bookingStatus === 'PAID' && item?.payStatus === 'PAID'
const isCancellingBooking = (item) => Number(cancelingBookingId.value) === Number(item?.bookingId || item?.id)

const remainingSlots = (row) => {
  const capacity = Number(row?.capacity ?? 0)
  const booked = Number(row?.bookedCount ?? 0)
  const left = capacity - booked
  return Number.isFinite(left) ? Math.max(left, 0) : 0
}

const resolveScheduleStart = (row) => {
  if (!row) return null
  if (row.startTime && String(row.startTime).includes('T')) {
    const ts = new Date(row.startTime)
    return Number.isNaN(ts.getTime()) ? null : ts
  }
  if (!row.scheduleDate) return null
  const ts = new Date(`${row.scheduleDate}T${row.startTime || '00:00:00'}`)
  return Number.isNaN(ts.getTime()) ? null : ts
}

const isScheduleAvailable = (row) => {
  if (!row) return false
  if (row.status !== null && row.status !== undefined && Number(row.status) !== 1) return false
  if (remainingSlots(row) <= 0) return false
  const start = resolveScheduleStart(row)
  if (start && start.getTime() < Date.now() - 60 * 1000) return false
  return true
}

const formatBookingStatus = (status) => {
  const map = { WAIT_PAY: '待支付', PAID: '已支付', COMPLETED: '已完成', CANCELLED: '已取消' }
  return map[status] || status || '-'
}

const formatPayStatus = (status) => {
  const map = { UNPAID: '未支付', PAID: '已支付', CLOSED: '已关闭', REFUNDED: '已退款' }
  return map[status] || status || '-'
}

const bookingTone = (status) => {
  const map = { WAIT_PAY: 'warning', PAID: 'success', COMPLETED: 'plain', CANCELLED: 'muted' }
  return map[status] || 'plain'
}

const formatDate = (value) => {
  if (!value) return '-'
  const raw = String(value)
  if (raw.includes('T')) return raw.split('T')[0]
  return raw.slice(0, 10)
}

const formatTime = (value) => {
  if (!value) return '-'
  const raw = String(value)
  if (raw.includes('T')) return (raw.split('T')[1] || '').slice(0, 5)
  if (raw.includes(':')) return raw.slice(0, 5)
  return raw
}

const formatScheduleTime = (row) => {
  if (!row) return '-'
  return `${formatDate(row.scheduleDate || row.startTime)} ${formatTime(row.startTime)}-${formatTime(row.endTime)}`
}

const formatAmount = (value) => {
  if (value === null || value === undefined || value === '') return '-'
  const amount = Number(value)
  return Number.isFinite(amount) ? amount.toFixed(amount % 1 ? 2 : 0) : String(value)
}

const goToOrder = (payload) => {
  const orderId = typeof payload === 'object' && payload !== null ? payload.orderId : payload
  if (!orderId) return
  uni.navigateTo({ url: `/pages/order-detail/index?id=${encodeURIComponent(orderId)}&from=booking` })
}

const goCourses = () => {
  uni.switchTab({ url: '/pages/courses/index' })
}

const goBackHome = () => {
  const pages = getCurrentPages()
  if (pages.length > 1) {
    uni.navigateBack()
    return
  }
  uni.switchTab({ url: '/pages/home/index' })
}

watch([coachId, date], () => {
  loadSchedules()
})

onShow(() => {
  hideTabBarSafely()
  const cachedCoachId = parseCoachId(uni.getStorageSync(STORAGE_COACH_ID))
  if (cachedCoachId !== null) coachId.value = cachedCoachId
  if (ensureLogin()) {
    loadLastBooking()
    refreshWorkspace()
  }
})

onHide(() => {
  showTabBarSafely()
})
</script>

<style scoped lang="scss">
.booking-page {
  min-height: 100vh;
  padding: calc(var(--status-bar-height) + 28rpx) 28rpx 206rpx;
  overflow-x: hidden;
  background:
    radial-gradient(circle at 0% 0%, rgba(139, 99, 255, 0.2), transparent 30%),
    radial-gradient(circle at 100% 4%, rgba(255, 128, 111, 0.18), transparent 28%),
    linear-gradient(180deg, #fff6f8 0%, #fffaf4 48%, #fffdf9 100%);
  color: #24104f;
}

.topbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  position: relative;
  z-index: 2;
  margin-bottom: 22rpx;
}

.topbar-title {
  font-size: 34rpx;
  font-weight: 950;
}

.icon-button {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 70rpx;
  height: 70rpx;
}

.top-icon {
  width: 36rpx;
  height: 36rpx;
}

.hero {
  position: relative;
  min-height: 300rpx;
  margin: 0 -28rpx;
  padding: 42rpx 34rpx 84rpx;
  overflow: hidden;
  background: linear-gradient(122deg, #ebe2ff 0%, #ffdce0 54%, #fff0c8 100%);
}

.hero-copy {
  position: relative;
  z-index: 2;
  display: flex;
  flex-direction: column;
}

.hero-title {
  font-size: 48rpx;
  font-weight: 950;
  line-height: 1.2;
}

.hero-sub {
  width: 488rpx;
  margin-top: 20rpx;
  color: #5f4c89;
  font-size: 25rpx;
  font-weight: 800;
  line-height: 1.45;
}

.hero-cloud,
.hero-hill,
.hero-person {
  position: absolute;
}

.hero-cloud {
  border-radius: 999rpx;
  background: rgba(255, 255, 255, 0.7);
}

.hero-cloud--one {
  top: 76rpx;
  right: 56rpx;
  width: 112rpx;
  height: 36rpx;
}

.hero-cloud--two {
  right: 256rpx;
  bottom: 96rpx;
  width: 78rpx;
  height: 26rpx;
}

.hero-hill--back {
  right: -110rpx;
  bottom: -44rpx;
  width: 470rpx;
  height: 170rpx;
  border-radius: 100% 0 0 0;
  background: rgba(139, 99, 255, 0.28);
  transform: skewY(-11deg);
}

.hero-hill--front {
  right: -80rpx;
  bottom: -70rpx;
  width: 440rpx;
  height: 170rpx;
  border-radius: 100% 0 0 0;
  background: rgba(255, 185, 118, 0.52);
  transform: skewY(-14deg);
}

.hero-person {
  right: 60rpx;
  bottom: 40rpx;
  width: 174rpx;
  height: 230rpx;
  z-index: 1;
}

.person-head,
.person-body,
.person-hair,
.person-arm,
.person-clipboard {
  position: absolute;
  border: 5rpx solid #24104f;
}

.person-head {
  top: 20rpx;
  left: 56rpx;
  width: 64rpx;
  height: 64rpx;
  border-radius: 50%;
  background: #ffc7a8;
}

.person-hair {
  top: 8rpx;
  left: 84rpx;
  width: 70rpx;
  height: 78rpx;
  border-radius: 60% 60% 70% 35%;
  background: #4d2f88;
}

.person-body {
  top: 86rpx;
  left: 44rpx;
  width: 88rpx;
  height: 96rpx;
  border-radius: 48rpx 48rpx 18rpx 18rpx;
  background: #8b63ff;
}

.person-arm {
  top: 104rpx;
  width: 70rpx;
  height: 18rpx;
  border-radius: 999rpx;
  background: #ffc7a8;
}

.person-arm--left {
  left: 0;
  transform: rotate(25deg);
}

.person-arm--right {
  right: 0;
  transform: rotate(-20deg);
}

.person-clipboard {
  right: 0;
  top: 96rpx;
  width: 46rpx;
  height: 64rpx;
  border-radius: 10rpx;
  background: #fffaf4;
  transform: rotate(10deg);
}

.coach-chip-scroll {
  position: relative;
  z-index: 3;
  width: 100%;
  margin-top: -38rpx;
  white-space: nowrap;
}

.coach-chip-row {
  display: inline-flex;
  gap: 16rpx;
  padding: 0 2rpx 8rpx;
}

.coach-chip {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 140rpx;
  min-height: 66rpx;
  padding: 0 28rpx;
  border-radius: 999rpx;
  background: rgba(255, 255, 255, 0.72);
  color: #24104f;
  font-size: 25rpx;
  font-weight: 900;
  box-shadow: 0 8rpx 20rpx rgba(52, 32, 95, 0.06);
}

.coach-chip.active {
  background: linear-gradient(120deg, #8b63ff, #ff806f);
  color: #fff;
}

.date-card,
.summary-strip,
.todo-card,
.selected-card,
.package-banner,
.shortcut-card,
.booking-card,
.review-card,
.empty-card {
  border: 2rpx solid rgba(52, 32, 95, 0.13);
  background: rgba(255, 255, 255, 0.94);
  box-shadow: 0 10rpx 26rpx rgba(52, 32, 95, 0.08);
}

.date-card {
  display: grid;
  grid-template-columns: 92rpx minmax(0, 1fr) 92rpx;
  align-items: stretch;
  min-height: 126rpx;
  margin-top: 22rpx;
  border-radius: 28rpx;
  overflow: hidden;
}

.date-side {
  display: flex;
  align-items: center;
  justify-content: center;
  flex-direction: column;
  gap: 8rpx;
  color: #24104f;
  font-size: 22rpx;
  font-weight: 900;
}

.date-side image {
  width: 30rpx;
  height: 30rpx;
}

.date-scroll {
  white-space: nowrap;
}

.date-row {
  display: inline-flex;
  gap: 12rpx;
  padding: 16rpx 0;
}

.date-item {
  display: flex;
  align-items: center;
  justify-content: center;
  flex-direction: column;
  width: 100rpx;
  height: 94rpx;
  border-radius: 18rpx;
  color: #6f6095;
  font-weight: 850;
}

.date-item.active {
  border: 4rpx solid #8b63ff;
  color: #8b63ff;
  background: #fff;
}

.date-week {
  font-size: 24rpx;
}

.date-value {
  margin-top: 6rpx;
  font-size: 22rpx;
}

.summary-strip {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 2rpx;
  margin-top: 22rpx;
  border-radius: 26rpx;
  overflow: hidden;
}

.summary-item {
  display: flex;
  align-items: center;
  flex-direction: column;
  gap: 6rpx;
  padding: 20rpx 8rpx;
}

.summary-item text:first-child {
  font-size: 38rpx;
  font-weight: 950;
}

.summary-item text:last-child {
  color: #7b6f98;
  font-size: 22rpx;
  font-weight: 800;
}

.todo-card {
  margin-top: 26rpx;
  padding: 26rpx;
  border-radius: 30rpx;
  background: linear-gradient(112deg, #ffe3e6 0%, #fffaf4 100%);
}

.todo-card.is-finish {
  background: linear-gradient(112deg, #e3fff2 0%, #fffaf4 100%);
}

.todo-card.is-done {
  background: linear-gradient(112deg, #eee5ff 0%, #fffaf4 100%);
}

.todo-head,
.todo-title-wrap,
.todo-main,
.todo-line,
.coach-row,
.rating-row,
.todo-actions,
.section-head,
.package-banner,
.shortcut-item,
.booking-card-head,
.booking-meta,
.sheet-head,
.sheet-actions {
  display: flex;
  align-items: center;
}

.todo-head,
.section-head,
.booking-card-head,
.sheet-head {
  justify-content: space-between;
  gap: 18rpx;
}

.todo-title-wrap {
  gap: 10rpx;
}

.todo-icon {
  width: 30rpx;
  height: 30rpx;
}

.todo-title {
  font-size: 28rpx;
  font-weight: 950;
}

.countdown-pill,
.soft-chip,
.status-dot,
.booking-status {
  min-height: 42rpx;
  padding: 0 18rpx;
  border-radius: 999rpx;
  font-size: 22rpx;
  font-weight: 900;
}

.countdown-pill {
  background: #ffffff;
  color: #ff6f61;
}

.soft-chip,
.status-dot {
  background: #eee5ff;
  color: #8b63ff;
}

.todo-main {
  gap: 18rpx;
  margin-top: 24rpx;
}

.coach-avatar {
  display: flex;
  align-items: center;
  justify-content: center;
  flex: 0 0 96rpx;
  width: 96rpx;
  height: 96rpx;
  overflow: hidden;
  border-radius: 24rpx;
  background: linear-gradient(135deg, #eee5ff, #fff0c8);
  color: #24104f;
  font-size: 34rpx;
  font-weight: 950;
}

.coach-avatar.small {
  flex-basis: 86rpx;
  width: 86rpx;
  height: 86rpx;
  border-radius: 20rpx;
}

.coach-avatar image {
  width: 100%;
  height: 100%;
}

.todo-info,
.schedule-info {
  flex: 1;
  min-width: 0;
}

.todo-name-row {
  display: flex;
  align-items: center;
  gap: 14rpx;
}

.coach-name {
  color: #24104f;
  font-size: 30rpx;
  font-weight: 950;
}

.todo-line {
  gap: 10rpx;
  margin-top: 12rpx;
  color: #6f6095;
  font-size: 23rpx;
  font-weight: 800;
}

.todo-line image {
  width: 26rpx;
  height: 26rpx;
}

.price-col {
  display: flex;
  align-items: flex-end;
  flex-direction: column;
  min-width: 112rpx;
  color: #ff6f61;
  font-size: 22rpx;
  font-weight: 900;
}

.price {
  color: #ff6f61;
  font-size: 34rpx;
  font-weight: 950;
}

.todo-actions {
  flex-wrap: wrap;
  gap: 14rpx;
  margin-top: 24rpx;
}

.todo-actions.compact {
  margin-top: 18rpx;
}

.pill-action {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 142rpx;
  min-height: 58rpx;
  padding: 0 22rpx;
  border-radius: 999rpx;
  color: #24104f;
  font-size: 23rpx;
  font-weight: 950;
}

.pill-action.primary,
.book-button,
.selected-submit {
  background: linear-gradient(120deg, #8b63ff, #ff806f);
  color: #fff;
}

.pill-action.danger {
  background: #ffe6ee;
  color: #d94d77;
}

.pill-action.success {
  background: #8adfc9;
  color: #24104f;
}

.pill-action.ghost {
  border: 2rpx solid rgba(52, 32, 95, 0.24);
  background: #fff;
}

.mode-tabs {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 14rpx;
  margin: 34rpx 0 24rpx;
}

.mode-tabs button {
  min-height: 68rpx;
  border-radius: 999rpx;
  color: #9b8cab;
  font-size: 28rpx;
  font-weight: 900;
}

.mode-tabs button.active {
  background: #eee5ff;
  color: #24104f;
  transform: scale(1.03);
}

.section-title {
  display: block;
  color: #24104f;
  font-size: 34rpx;
  font-weight: 950;
}

.section-sub {
  display: block;
  margin-top: 8rpx;
  color: #7b6f98;
  font-size: 24rpx;
  line-height: 1.45;
}

.refresh-button {
  flex: 0 0 auto;
  min-width: 112rpx;
  min-height: 56rpx;
  border: 2rpx solid rgba(52, 32, 95, 0.22);
  border-radius: 999rpx;
  background: #fff;
  color: #24104f;
  font-size: 22rpx;
  font-weight: 900;
}

.schedule-list,
.booking-list {
  display: flex;
  flex-direction: column;
  gap: 18rpx;
  margin-top: 22rpx;
}

.schedule-card {
  display: grid;
  grid-template-columns: 86rpx minmax(0, 1fr) 190rpx;
  gap: 18rpx;
  align-items: center;
  padding: 22rpx;
  border: 2rpx solid rgba(52, 32, 95, 0.12);
  border-radius: 28rpx;
  background: #fff;
  box-shadow: 0 8rpx 22rpx rgba(52, 32, 95, 0.07);
}

.schedule-card.active {
  border-color: #8b63ff;
  background: linear-gradient(180deg, #f7f2ff 0%, #fff 100%);
}

.coach-row {
  justify-content: space-between;
  gap: 12rpx;
}

.rating-row {
  gap: 10rpx;
  margin-top: 8rpx;
  color: #7b6f98;
  font-size: 23rpx;
  font-weight: 800;
}

.rating-row image {
  width: 24rpx;
  height: 24rpx;
}

.tag-row {
  display: flex;
  flex-wrap: wrap;
  gap: 10rpx;
  margin-top: 8rpx;
}

.tag-row text {
  padding: 6rpx 14rpx;
  border-radius: 999rpx;
  background: #eee5ff;
  color: #8b63ff;
  font-size: 21rpx;
  font-weight: 850;
}

.time-price {
  display: flex;
  align-items: flex-end;
  flex-direction: column;
}

.time {
  font-size: 28rpx;
  font-weight: 950;
}

.slot-date {
  margin-top: 6rpx;
  color: #7b6f98;
  font-size: 22rpx;
  font-weight: 800;
}

.book-button {
  min-width: 118rpx;
  min-height: 54rpx;
  margin-top: 12rpx;
  border-radius: 999rpx;
  font-size: 22rpx;
  font-weight: 950;
}

.selected-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18rpx;
  margin-top: 22rpx;
  padding: 22rpx;
  border-radius: 28rpx;
}

.selected-title {
  display: block;
  font-size: 28rpx;
  font-weight: 950;
}

.selected-sub {
  display: block;
  margin-top: 8rpx;
  color: #7b6f98;
  font-size: 23rpx;
}

.selected-submit {
  flex: 0 0 auto;
  min-width: 190rpx;
  min-height: 68rpx;
  border-radius: 999rpx;
  font-size: 24rpx;
  font-weight: 950;
}

.selected-submit.full {
  width: 100%;
  margin-top: 18rpx;
}

.package-banner {
  gap: 20rpx;
  margin-top: 28rpx;
  padding: 26rpx;
  border-radius: 28rpx;
  background: linear-gradient(110deg, #eee5ff 0%, #c7a8ff 100%);
}

.package-icon,
.shortcut-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  flex: 0 0 82rpx;
  width: 82rpx;
  height: 82rpx;
  border-radius: 24rpx;
  background: rgba(255, 255, 255, 0.78);
}

.package-icon image,
.shortcut-icon image {
  width: 44rpx;
  height: 44rpx;
}

.package-title {
  display: block;
  font-size: 30rpx;
  font-weight: 950;
}

.package-sub {
  display: block;
  margin-top: 6rpx;
  color: #6f6095;
  font-size: 23rpx;
  font-weight: 800;
}

.package-button {
  display: flex;
  align-items: center;
  gap: 8rpx;
  margin-left: auto;
  padding: 16rpx 20rpx;
  border-radius: 999rpx;
  background: #fff;
  font-size: 23rpx;
  font-weight: 950;
}

.package-button image,
.shortcut-arrow {
  width: 22rpx;
  height: 22rpx;
}

.shortcut-card {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 2rpx;
  margin-top: 24rpx;
  border-radius: 28rpx;
  overflow: hidden;
}

.shortcut-item {
  gap: 16rpx;
  min-width: 0;
  padding: 24rpx;
  background: #fff;
  text-align: left;
}

.shortcut-item > view:nth-child(2) {
  flex: 1;
  min-width: 0;
}

.shortcut-item text:first-child {
  display: block;
  font-size: 27rpx;
  font-weight: 950;
}

.shortcut-item text:last-child {
  display: block;
  margin-top: 6rpx;
  color: #7b6f98;
  font-size: 21rpx;
}

.booking-card,
.review-card,
.empty-card {
  padding: 24rpx;
  border-radius: 28rpx;
}

.booking-title {
  display: block;
  font-size: 29rpx;
  font-weight: 950;
}

.booking-sub {
  display: block;
  margin-top: 8rpx;
  color: #7b6f98;
  font-size: 23rpx;
}

.booking-status.warning {
  background: #fff0cf;
  color: #c07604;
}

.booking-status.success {
  background: #dcf8e7;
  color: #19a65f;
}

.booking-status.plain {
  background: #eee5ff;
  color: #8b63ff;
}

.booking-status.muted {
  background: #f1edf5;
  color: #7b6f98;
}

.booking-meta {
  flex-wrap: wrap;
  gap: 12rpx;
  margin-top: 16rpx;
}

.booking-meta text {
  padding: 8rpx 14rpx;
  border-radius: 999rpx;
  background: #fffaf4;
  color: #6f6095;
  font-size: 22rpx;
  font-weight: 850;
}

.score-row {
  display: flex;
  gap: 12rpx;
  margin-top: 22rpx;
}

.score-row button {
  min-width: 92rpx;
  min-height: 54rpx;
  border-radius: 999rpx;
  background: #f1edf5;
  color: #7b6f98;
  font-weight: 900;
}

.score-row button.active {
  background: #8b63ff;
  color: #fff;
}

.review-textarea {
  width: 100%;
  min-height: 180rpx;
  margin-top: 18rpx;
  padding: 22rpx;
  border: 2rpx solid rgba(52, 32, 95, 0.16);
  border-radius: 24rpx;
  background: #fff;
  color: #24104f;
  font-size: 25rpx;
}

.empty-card {
  display: flex;
  align-items: center;
  flex-direction: column;
  gap: 12rpx;
  margin-top: 22rpx;
  text-align: center;
}

.empty-icon {
  width: 62rpx;
  height: 62rpx;
}

.empty-title {
  font-size: 29rpx;
  font-weight: 950;
}

.empty-desc {
  color: #7b6f98;
  font-size: 24rpx;
  line-height: 1.45;
}

.safe-note {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10rpx;
  margin: 42rpx 0 12rpx;
  color: #8c7cac;
  font-size: 22rpx;
  font-weight: 850;
}

.safe-note image {
  width: 26rpx;
  height: 26rpx;
}

.sheet-mask {
  position: fixed;
  inset: 0;
  z-index: 20;
  display: flex;
  align-items: flex-end;
  background: rgba(36, 16, 79, 0.34);
}

.filter-sheet {
  width: 100%;
  padding: 30rpx;
  border-radius: 36rpx 36rpx 0 0;
  background: #fffaf4;
}

.sheet-title {
  display: block;
  font-size: 32rpx;
  font-weight: 950;
}

.sheet-sub {
  display: block;
  margin-top: 8rpx;
  color: #7b6f98;
  font-size: 23rpx;
}

.sheet-close {
  width: 58rpx;
  height: 58rpx;
}

.sheet-close image {
  width: 36rpx;
  height: 36rpx;
}

.sheet-section {
  margin-top: 26rpx;
}

.sheet-label {
  display: block;
  margin-bottom: 14rpx;
  font-size: 25rpx;
  font-weight: 950;
}

.coach-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 12rpx;
}

.coach-grid button {
  min-height: 56rpx;
  padding: 0 22rpx;
  border-radius: 999rpx;
  background: #f1edf5;
  color: #7b6f98;
  font-size: 23rpx;
  font-weight: 900;
}

.coach-grid button.active {
  background: #8b63ff;
  color: #fff;
}

.date-picker-field {
  display: flex;
  align-items: center;
  gap: 14rpx;
  min-height: 82rpx;
  padding: 0 22rpx;
  border: 2rpx solid rgba(52, 32, 95, 0.16);
  border-radius: 22rpx;
  background: #fff;
  color: #24104f;
  font-size: 25rpx;
  font-weight: 850;
}

.date-picker-field image {
  width: 30rpx;
  height: 30rpx;
}

.sheet-actions {
  justify-content: flex-end;
  gap: 14rpx;
  margin-top: 30rpx;
}
</style>
