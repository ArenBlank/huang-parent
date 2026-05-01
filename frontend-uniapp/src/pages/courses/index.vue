<template>
  <view class="course-page">
    <view class="page-bg page-bg--left"></view>
    <view class="page-bg page-bg--right"></view>

    <view class="topbar">
      <button class="icon-button" aria-label="返回首页" @click="goBackHome">
        <image class="top-icon" :src="icons.back" mode="aspectFit" />
      </button>
      <text class="topbar-title">课程目录</text>
      <button class="icon-button" aria-label="刷新课程" :disabled="loading" @click="refreshWorkspace">
        <image class="top-icon" :class="{ spinning: loading || scheduleLoading || enrollmentsLoading }" :src="icons.refresh" mode="aspectFit" />
      </button>
    </view>

    <view class="hero-card">
      <view class="hero-cloud hero-cloud--one"></view>
      <view class="hero-cloud hero-cloud--two"></view>
      <view class="hero-hill hero-hill--back"></view>
      <view class="hero-hill hero-hill--front"></view>
      <view class="hero-trail"></view>
      <view class="hero-tree hero-tree--one">
        <view class="tree-crown"></view>
        <view class="tree-trunk"></view>
      </view>
      <view class="hero-tree hero-tree--two">
        <view class="tree-crown"></view>
        <view class="tree-trunk"></view>
      </view>
      <view class="hero-tree hero-tree--three">
        <view class="tree-crown"></view>
        <view class="tree-trunk"></view>
      </view>

      <view class="hero-copy">
        <text class="eyebrow">COURSE CATALOG</text>
        <text class="hero-title">选课、排期、报名，</text>
        <text class="hero-title">都收进一个工作台</text>
        <text class="hero-sub">左手找课，右手处理排期和订单后续，手机端不再来回跳。</text>
      </view>
    </view>

    <view class="metric-strip">
      <view class="metric-item" @click="switchView('discover')">
        <view class="metric-icon is-purple">
          <image :src="icons.book" mode="aspectFit" />
        </view>
        <text class="metric-label">课程</text>
        <text class="metric-value">{{ courses.length }}</text>
      </view>
      <view class="metric-item" @click="scrollToSchedules">
        <view class="metric-icon is-coral">
          <image :src="icons.calendar" mode="aspectFit" />
        </view>
        <text class="metric-label">可报排期</text>
        <text class="metric-value">{{ availableSchedules }}</text>
      </view>
      <view class="metric-item" @click="switchView('mine')">
        <view class="metric-icon is-gold">
          <image :src="icons.clipboard" mode="aspectFit" />
        </view>
        <text class="metric-label">我的报名</text>
        <text class="metric-value">{{ enrollments.length }}</text>
      </view>
      <view class="metric-item" @click="switchView('mine')">
        <view class="metric-icon is-mint">
          <image :src="icons.qrcode" mode="aspectFit" />
        </view>
        <text class="metric-label">待上课</text>
        <text class="metric-value">{{ mySchedules.length }}</text>
      </view>
    </view>

    <view class="tabs">
      <button
        class="tab-button"
        :class="{ active: viewMode === 'mine' }"
        @click="switchView('mine')"
      >
        我的课程 {{ mySchedules.length }}
      </button>
      <button
        class="tab-button"
        :class="{ active: viewMode === 'discover' }"
        @click="switchView('discover')"
      >
        寻找课程 {{ courses.length }}
      </button>
    </view>

    <template v-if="viewMode === 'discover'">
      <view class="workbench-card" id="course-workbench">
        <view class="workbench-head">
          <view>
            <text class="section-kicker">报名工作台</text>
            <text class="section-title">当前课程</text>
            <text class="section-sub">选中课程后，这里同步排期、报名和最近一次订单处理。</text>
          </view>
          <button class="ghost-button" @click="scrollToCatalog">换一门课</button>
        </view>

        <view v-if="selectedCourse" class="selected-course">
          <image class="selected-cover" :src="resolveCourseCover(selectedCourse.coverUrl)" mode="aspectFill" />
          <view class="selected-main">
            <text class="selected-title">{{ selectedCourse.title || `课程 ${selectedCourse.id}` }}</text>
            <text class="selected-summary">{{ selectedCourse.summary || '暂无课程简介，可继续查看下方排期。' }}</text>
            <view class="chip-row">
              <text class="chip">价格 {{ selectedCourse.price ?? '-' }}</text>
              <text class="chip">状态 {{ formatCourseStatus(selectedCourse.status) }}</text>
              <text v-if="selectedCourse.level" class="chip">难度 {{ getDifficultyLabel(selectedCourse.level) }}</text>
              <text v-if="getCourseTarget(selectedCourse)" class="chip">目标 {{ getTargetLabel(getCourseTarget(selectedCourse)) }}</text>
            </view>
          </view>
        </view>

        <view v-else class="empty-soft">
          <image class="empty-icon" :src="icons.info" mode="aspectFit" />
          <text>先在下方选择一门课程</text>
        </view>
      </view>

      <view class="schedule-section" id="schedule-section">
        <view class="section-head">
          <view>
            <text class="section-title">选择合适时间</text>
            <text class="section-sub">排期来自当前选中课程，只有可报名排期才能提交。</text>
          </view>
          <text class="count-pill">可报 {{ availableSchedules }}</text>
        </view>

        <view v-if="!selectedCourse" class="empty-card">
          <image class="empty-icon" :src="icons.calendar" mode="aspectFit" />
          <text class="empty-title">请选择课程</text>
          <text class="empty-desc">选中课程后会展示可报名排期。</text>
        </view>

        <view v-else-if="!schedules.length && !scheduleLoading" class="empty-card">
          <image class="empty-icon" :src="icons.calendar" mode="aspectFit" />
          <text class="empty-title">暂无可用排期</text>
          <text class="empty-desc">后台创建可报名排期后，这里会自动出现。</text>
        </view>

        <scroll-view v-else class="schedule-scroll" scroll-x show-scrollbar="false">
          <view class="schedule-row">
            <button
              v-for="schedule in schedules"
              :key="schedule.id"
              class="schedule-card"
              :class="{
                active: selectedSchedule?.id === schedule.id,
                disabled: !isScheduleAvailable(schedule)
              }"
              @click="selectSchedule(schedule)"
            >
              <view class="schedule-top">
                <text class="schedule-time">{{ formatScheduleTime(schedule) }}</text>
                <text class="status-pill" :class="scheduleStatus(schedule).tone">{{ scheduleStatus(schedule).text }}</text>
              </view>
              <view class="schedule-meta">
                <text>排期 {{ schedule.id }}</text>
                <text>教练 {{ schedule.coachId || '-' }}</text>
              </view>
              <view class="schedule-chips">
                <text class="mini-chip">余量 {{ remainingSlots(schedule) }}</text>
                <text class="mini-chip">开始 {{ formatTime(schedule.startTime) }}</text>
                <text class="mini-chip">结束 {{ formatTime(schedule.endTime) }}</text>
              </view>
            </button>
          </view>
        </scroll-view>
      </view>

      <view class="submit-card">
        <view>
          <text class="section-kicker">报名操作</text>
          <text class="section-title">确认并提交</text>
          <text class="section-sub">提交后会创建课程报名订单，未支付前可以取消。</text>
        </view>
        <view v-if="selectedCourse && selectedSchedule" class="confirm-panel">
          <text class="confirm-title">{{ selectedCourse.title }}</text>
          <text class="confirm-time">{{ formatScheduleTime(selectedSchedule) }}</text>
          <view class="chip-row">
            <text class="chip">排期ID {{ selectedSchedule.id }}</text>
            <text class="chip">余量 {{ remainingSlots(selectedSchedule) }}</text>
            <text class="chip">状态 {{ scheduleStatus(selectedSchedule).text }}</text>
          </view>
        </view>
        <button class="primary-button" :disabled="!canEnroll" @click="enrollSelected">
          <image class="button-icon" :src="icons.clipboardCheck" mode="aspectFit" />
          <text>{{ enrolling ? '提交中...' : '提交报名' }}</text>
        </button>
      </view>

      <view ref="catalogAnchor" class="catalog-section">
        <view class="section-head">
          <view>
            <text class="section-title">课程列表</text>
            <text class="section-sub">搜索课程名、简介、难度或训练目标，点选后上方工作台立即刷新。</text>
          </view>
          <button class="ghost-button" @click="loadCourses">刷新课程</button>
        </view>

        <view class="search-box">
          <image class="search-icon" :src="icons.search" mode="aspectFit" />
          <input v-model.trim="courseKeyword" class="search-input" placeholder="搜索课程名 / 简介 / 难度" />
          <button v-if="courseKeyword" class="clear-button" @click="courseKeyword = ''">清空</button>
        </view>

        <view v-if="!filteredCourses.length && !loading" class="empty-card">
          <image class="empty-icon" :src="icons.book" mode="aspectFit" />
          <text class="empty-title">没有匹配课程</text>
          <text class="empty-desc">换个关键词，或者刷新课程列表。</text>
        </view>

        <view v-else class="course-list">
          <button
            v-for="course in filteredCourses"
            :key="course.id"
            class="course-card"
            :class="{ active: selectedCourse?.id === course.id }"
            @click="selectCourse(course)"
          >
            <image class="course-cover" :src="resolveCourseCover(course.coverUrl)" mode="aspectFill" />
            <view class="course-main">
              <view class="course-top">
                <text class="course-title">{{ course.title || `课程 ${course.id}` }}</text>
                <text v-if="selectedCourse?.id === course.id" class="chosen-pill">已选中</text>
              </view>
              <text class="course-summary">{{ course.summary || '暂无课程简介' }}</text>
              <view class="chip-row">
                <text class="chip">价格 {{ course.price ?? '-' }}</text>
                <text class="chip">{{ formatCourseStatus(course.status) }}</text>
                <text v-if="course.level" class="chip">{{ getDifficultyLabel(course.level) }}</text>
                <text v-if="getCourseTarget(course)" class="chip">{{ getTargetLabel(getCourseTarget(course)) }}</text>
              </view>
            </view>
            <image class="card-chevron" :src="icons.right" mode="aspectFit" />
          </button>
        </view>
      </view>

      <view ref="recentAnchor" class="recent-card">
        <view class="section-head">
          <view>
            <text class="section-title">后续处理</text>
            <text class="section-sub">最近一次报名可以继续模拟支付、取消未支付或跳转订单。</text>
          </view>
          <button class="ghost-button" @click="loadEnrollments">刷新报名</button>
        </view>

        <view v-if="!lastEnrollment" class="empty-soft">
          <image class="empty-icon" :src="icons.receipt" mode="aspectFit" />
          <text>暂无报名记录</text>
        </view>

        <view v-else class="last-enrollment">
          <view class="last-top">
            <view>
              <text class="last-title">报名 {{ lastEnrollment.enrollmentId || lastEnrollment.id }}</text>
              <text class="last-sub">{{ lastEnrollment.courseTitle || selectedCourse?.title || '最近一次课程报名' }}</text>
            </view>
            <text class="status-pill" :class="enrollmentStatusTone(lastEnrollment.status)">
              {{ formatEnrollmentStatus(lastEnrollment.status) }}
            </text>
          </view>
          <view class="chip-row">
            <text class="chip">订单ID {{ lastEnrollment.orderId || '-' }}</text>
            <text class="chip">排期ID {{ lastEnrollment.scheduleId || '-' }}</text>
            <text class="chip">创建 {{ formatDateTime(lastEnrollment.createTime || lastEnrollment.enrollTime) }}</text>
          </view>
          <view class="action-row">
            <button v-if="canMockPayEnrollment" class="small-solid" :disabled="paying" @click="mockPayLast">模拟支付</button>
            <button v-if="canCancelEnrollment" class="small-danger" :disabled="canceling" @click="cancelLast">取消未支付</button>
            <button v-if="canRefundEnrollment && !refundVisible" class="small-danger outline" @click="openRefund">申请退款</button>
            <button class="small-ghost" :disabled="!lastEnrollment.orderId" @click="goToOrder(lastEnrollment)">查看订单</button>
          </view>
          <view v-if="refundVisible" class="refund-box">
            <textarea
              v-model.trim="refundReason"
              class="refund-textarea"
              maxlength="200"
              placeholder="请输入退款原因，最多 200 字"
            />
            <view class="action-row">
              <button class="small-ghost" @click="closeRefund">取消</button>
              <button class="small-danger" :disabled="refunding" @click="refundLast">提交退款</button>
            </view>
          </view>
        </view>
      </view>
    </template>

    <template v-else>
      <view class="my-overview">
        <view class="section-head">
          <view>
            <text class="section-title">我的待上课</text>
            <text class="section-sub">只展示已支付且待上课的课程，便于现场出示核销码。</text>
          </view>
          <button class="ghost-button" @click="loadMySchedules">刷新</button>
        </view>

        <view v-if="!mySchedules.length && !mySchedulesLoading" class="empty-card">
          <image class="empty-icon" :src="icons.qrcode" mode="aspectFit" />
          <text class="empty-title">还没有待上课课程</text>
          <text class="empty-desc">去寻找课程报名并完成支付后，会出现在这里。</text>
          <button class="empty-button" @click="switchView('discover')">去寻找课程</button>
        </view>

        <view v-else class="itinerary-list">
          <view v-for="item in pagedMySchedules" :key="item.enrollmentId || item.id" class="itinerary-card">
            <view class="itinerary-left">
              <image class="itinerary-cover" :src="resolveCourseCover(item.coverUrl)" mode="aspectFill" />
              <view class="itinerary-label">
                <image class="itinerary-label-icon" :src="icons.qrcode" mode="aspectFit" />
                <text>我的待上课</text>
              </view>
            </view>
            <view class="itinerary-main">
              <view class="itinerary-top">
                <view>
                  <text class="itinerary-title">{{ item.courseTitle || `课程 ${item.courseId}` }}</text>
                  <text class="itinerary-time">{{ formatScheduleTime(item) }}</text>
                </view>
                <text class="status-pill success">待上课</text>
              </view>
              <view class="chip-row">
                <text class="chip">报名ID {{ item.enrollmentId }}</text>
                <text class="chip">订单ID {{ item.orderId || '-' }}</text>
                <text class="chip">教练 {{ item.coachId || '-' }}</text>
              </view>
              <view class="action-row">
                <button class="small-solid" @click="openCheckIn(item)">出示核销码</button>
                <button class="small-ghost" :disabled="!item.orderId" @click="goToOrder(item)">查看订单</button>
              </view>
            </view>
          </view>
        </view>
        <view v-if="mySchedules.length > mySchedulePageSize" class="pagination-bar">
          <button class="page-button" :disabled="safeMySchedulePage <= 1" @click="prevMySchedulePage">上一页</button>
          <text class="page-count">{{ safeMySchedulePage }} / {{ myScheduleTotalPages }}</text>
          <button class="page-button" :disabled="safeMySchedulePage >= myScheduleTotalPages" @click="nextMySchedulePage">下一页</button>
        </view>
      </view>

      <view class="history-section">
        <view class="section-head">
          <view>
            <text class="section-title">报名历史</text>
            <text class="section-sub">保留全部报名记录，支付、取消、退款状态都在这里追踪。</text>
          </view>
          <button class="ghost-button" @click="loadEnrollments">刷新报名</button>
        </view>

        <view v-if="!enrollments.length && !enrollmentsLoading" class="empty-card">
          <image class="empty-icon" :src="icons.receipt" mode="aspectFit" />
          <text class="empty-title">暂无报名记录</text>
          <text class="empty-desc">完成一次课程报名后，这里会同步订单状态。</text>
        </view>

        <view v-else class="history-list">
          <view v-for="row in pagedEnrollments" :key="row.id" class="history-card">
            <view class="history-top">
              <view>
                <text class="history-title">报名 {{ row.id }}</text>
                <text class="history-course">{{ row.courseTitle || courseMap[row.courseId] || `课程 ${row.courseId}` }}</text>
              </view>
              <text class="status-pill" :class="enrollmentStatusTone(row.status)">{{ formatEnrollmentStatus(row.status) }}</text>
            </view>
            <view class="chip-row">
              <text class="chip">排期 {{ row.scheduleId || '-' }}</text>
              <text class="chip">订单 {{ row.orderId || '-' }}</text>
              <text class="chip">{{ formatDateTime(row.createTime || row.enrollTime) }}</text>
            </view>
            <view class="action-row">
              <button class="small-ghost" :disabled="!row.orderId" @click="goToOrder(row)">查看订单</button>
            </view>
          </view>
        </view>
        <view v-if="enrollments.length > enrollmentPageSize" class="pagination-bar">
          <button class="page-button" :disabled="safeEnrollmentPage <= 1" @click="prevEnrollmentPage">上一页</button>
          <text class="page-count">{{ safeEnrollmentPage }} / {{ enrollmentTotalPages }}</text>
          <button class="page-button" :disabled="safeEnrollmentPage >= enrollmentTotalPages" @click="nextEnrollmentPage">下一页</button>
        </view>
      </view>
    </template>

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

    <view v-if="checkInVisible" class="dialog-mask" @click="closeCheckIn">
      <view class="checkin-dialog" @click.stop>
        <view class="dialog-head">
          <view>
            <text class="dialog-title">出示核销码</text>
            <text class="dialog-desc">到店后向前台或教练出示下方 6 位码即可完成签到。</text>
          </view>
          <button class="dialog-close" @click="closeCheckIn">
            <image :src="icons.close" mode="aspectFit" />
          </button>
        </view>
        <text class="checkin-code">{{ activeCheckIn?.checkInCode || '------' }}</text>
        <view class="checkin-meta">
          <text>{{ activeCheckIn?.courseTitle || '课程' }}</text>
          <text>{{ formatScheduleTime(activeCheckIn) }}</text>
        </view>
        <view class="action-row">
          <button class="small-ghost" @click="closeCheckIn">我知道了</button>
          <button class="small-solid" @click="copyCheckInCode">复制核销码</button>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup>
import {
  faBookOpen,
  faCalendarDays,
  faChevronLeft,
  faChevronRight,
  faCircleInfo,
  faCircleXmark,
  faClipboardCheck,
  faClipboardList,
  faHome,
  faMagnifyingGlass,
  faPlayCircle,
  faQrcode,
  faReceipt,
  faRotateRight,
  faUser
} from '@fortawesome/free-solid-svg-icons'
import { computed, nextTick, ref } from 'vue'
import { onHide, onShow } from '@dcloudio/uni-app'
import {
  cancelUnpaidCourse,
  enrollCourse,
  listCourseSchedules,
  listCourses,
  listMyCourseSchedules,
  listMyEnrollments,
  mockPayCourse,
  refundCourse
} from '../../api/modules/course'
import { ensureLogin } from '../../utils/authGuard'
import { hideTabBarSafely, showTabBarSafely } from '../../utils/navigation'

const STORAGE_COURSE_ID = 'fp_last_course_id'
const STORAGE_COURSE_VIEW = 'fp_course_workspace_view'
const STORAGE_ENROLLMENT = 'fp_last_enrollment'

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
  book: faIcon(faBookOpen),
  calendar: faIcon(faCalendarDays, '#7b6f98'),
  clipboard: faIcon(faClipboardList),
  clipboardCheck: faIcon(faClipboardCheck, '#ffffff'),
  close: faIcon(faCircleXmark),
  info: faIcon(faCircleInfo, '#8b63ff'),
  qrcode: faIcon(faQrcode),
  receipt: faIcon(faReceipt),
  refresh: faIcon(faRotateRight),
  right: faIcon(faChevronRight),
  search: faIcon(faMagnifyingGlass, '#8b63ff'),
  home: faIcon(faHome),
  plan: faIcon(faClipboardList),
  course: faIcon(faPlayCircle),
  courseActive: faIcon(faPlayCircle, '#8b63ff'),
  mine: faIcon(faUser)
}

const defaultCoverUrl = '/static/course-fallback.svg'

const difficultyMap = {
  BEGINNER: '初级',
  INTERMEDIATE: '中级',
  ADVANCED: '高级'
}

const targetMap = {
  MUSCLE_GAIN: '增肌',
  WEIGHT_LOSS: '减脂',
  FAT_LOSS: '减脂',
  SHAPING: '塑形',
  BODY_SHAPING: '塑形',
  REHABILITATION: '康复',
  STRENGTH: '力量提升',
  ENDURANCE: '耐力提升',
  FLEXIBILITY: '柔韧性',
  CARDIO: '心肺训练',
  GENERAL_FITNESS: '综合体能'
}

const viewMode = ref(uni.getStorageSync(STORAGE_COURSE_VIEW) || 'mine')
const loading = ref(false)
const scheduleLoading = ref(false)
const enrollmentsLoading = ref(false)
const mySchedulesLoading = ref(false)
const enrolling = ref(false)
const paying = ref(false)
const canceling = ref(false)
const refunding = ref(false)
const refundVisible = ref(false)
const refundReason = ref('')
const courseKeyword = ref('')
const mySchedulePageSize = 2
const enrollmentPageSize = 3
const mySchedulePage = ref(1)
const enrollmentPage = ref(1)
const courses = ref([])
const schedules = ref([])
const enrollments = ref([])
const mySchedules = ref([])
const selectedCourse = ref(null)
const selectedSchedule = ref(null)
const lastEnrollment = ref(null)
const checkInVisible = ref(false)
const activeCheckIn = ref(null)
const catalogAnchor = ref(null)
const recentAnchor = ref(null)

const tabs = [
  { text: '首页', icon: icons.home, activeIcon: faIcon(faHome, '#8b63ff'), url: '/pages/home/index' },
  { text: '计划', icon: icons.plan, activeIcon: faIcon(faClipboardList, '#8b63ff'), url: '/pages/plans/index' },
  { text: '课程', icon: icons.course, activeIcon: icons.courseActive, url: '/pages/courses/index', active: true },
  { text: '我的', icon: icons.mine, activeIcon: faIcon(faUser, '#8b63ff'), url: '/pages/mine/index' }
]

const filteredCourses = computed(() => {
  const keyword = courseKeyword.value.trim().toLowerCase()
  if (!keyword) return courses.value
  return courses.value.filter((course) => {
    const target = getCourseTarget(course)
    const haystack = [
      course.title,
      course.summary,
      course.level,
      getDifficultyLabel(course.level),
      target,
      target ? getTargetLabel(target) : '',
      course.id
    ].filter(Boolean).join(' ').toLowerCase()
    return haystack.includes(keyword)
  })
})

const courseMap = computed(() => {
  const map = {}
  courses.value.forEach((course) => {
    map[course.id] = course.title
  })
  mySchedules.value.forEach((schedule) => {
    if (schedule.courseId && schedule.courseTitle) map[schedule.courseId] = schedule.courseTitle
  })
  return map
})

const availableSchedules = computed(() => schedules.value.filter((row) => isScheduleAvailable(row)).length)
const myScheduleTotalPages = computed(() => Math.max(1, Math.ceil(mySchedules.value.length / mySchedulePageSize)))
const safeMySchedulePage = computed(() => Math.min(Math.max(mySchedulePage.value, 1), myScheduleTotalPages.value))
const pagedMySchedules = computed(() => {
  const start = (safeMySchedulePage.value - 1) * mySchedulePageSize
  return mySchedules.value.slice(start, start + mySchedulePageSize)
})
const enrollmentTotalPages = computed(() => Math.max(1, Math.ceil(enrollments.value.length / enrollmentPageSize)))
const safeEnrollmentPage = computed(() => Math.min(Math.max(enrollmentPage.value, 1), enrollmentTotalPages.value))
const pagedEnrollments = computed(() => {
  const start = (safeEnrollmentPage.value - 1) * enrollmentPageSize
  return enrollments.value.slice(start, start + enrollmentPageSize)
})
const currentEnrollmentStatus = computed(() => Number(lastEnrollment.value?.status ?? -1))
const canMockPayEnrollment = computed(() => currentEnrollmentStatus.value === 1)
const canCancelEnrollment = computed(() => currentEnrollmentStatus.value === 1)
const canRefundEnrollment = computed(() => currentEnrollmentStatus.value === 2)
const canEnroll = computed(() => !!selectedSchedule.value?.id && isScheduleAvailable(selectedSchedule.value) && !enrolling.value)

const normalizeEnumKey = (value) => String(value || '').trim().replace(/-/g, '_').toUpperCase()

const normalizeList = (payload) => {
  const data = payload?.data ?? payload
  if (Array.isArray(data)) return data
  if (Array.isArray(data?.list)) return data.list
  if (Array.isArray(data?.records)) return data.records
  return []
}

const getDifficultyLabel = (value) => {
  const key = normalizeEnumKey(value)
  return difficultyMap[key] || value || '-'
}

const getCourseTarget = (course) =>
  course?.target || course?.goal || course?.trainingTarget || course?.courseTarget || ''

const getTargetLabel = (value) => {
  const key = normalizeEnumKey(value)
  return targetMap[key] || value || '-'
}

const resolveCourseCover = (value) => {
  const url = String(value || '').trim()
  if (!url || url.includes('127.0.0.1:9000') || url.includes('localhost:9000')) return defaultCoverUrl
  return url
}

const parseCourseId = (value) => {
  if (value === null || value === undefined || value === '') return null
  const parsed = Number(value)
  return Number.isFinite(parsed) ? parsed : null
}

const selectPreferredCourse = async () => {
  if (!courses.value.length) {
    selectedCourse.value = null
    schedules.value = []
    selectedSchedule.value = null
    return
  }
  const cached = parseCourseId(uni.getStorageSync(STORAGE_COURSE_ID))
  const preferred = (cached !== null && courses.value.find((item) => Number(item.id) === cached)) || courses.value[0]
  await selectCourse(preferred, false)
}

const loadCourses = async () => {
  try {
    loading.value = true
    const payload = await listCourses()
    courses.value = normalizeList(payload)
    if (!selectedCourse.value || !courses.value.some((item) => Number(item.id) === Number(selectedCourse.value.id))) {
      await selectPreferredCourse()
    }
  } finally {
    loading.value = false
  }
}

const selectCourse = async (course, showTip = true) => {
  if (!course?.id) return
  selectedCourse.value = course
  selectedSchedule.value = null
  schedules.value = []
  uni.setStorageSync(STORAGE_COURSE_ID, String(course.id))
  if (showTip) {
    uni.showToast({ title: '已切换课程', icon: 'none' })
  }
  try {
    scheduleLoading.value = true
    const payload = await listCourseSchedules(course.id)
    schedules.value = normalizeList(payload)
    selectedSchedule.value = schedules.value.find((item) => isScheduleAvailable(item)) || schedules.value[0] || null
  } finally {
    scheduleLoading.value = false
  }
}

const loadEnrollments = async () => {
  try {
    enrollmentsLoading.value = true
    const payload = await listMyEnrollments()
    enrollments.value = normalizeList(payload).map((item) => ({
      ...item,
      createTime: item.createTime || item.enrollTime || ''
    }))
    if (enrollmentPage.value > enrollmentTotalPages.value) enrollmentPage.value = enrollmentTotalPages.value
    syncLastEnrollment(enrollments.value)
  } finally {
    enrollmentsLoading.value = false
  }
}

const loadMySchedules = async () => {
  try {
    mySchedulesLoading.value = true
    const payload = await listMyCourseSchedules()
    mySchedules.value = normalizeList(payload)
    if (mySchedulePage.value > myScheduleTotalPages.value) mySchedulePage.value = myScheduleTotalPages.value
  } finally {
    mySchedulesLoading.value = false
  }
}

const refreshWorkspace = async () => {
  await Promise.all([loadCourses(), loadEnrollments(), loadMySchedules()])
}

const toRecentEnrollment = (item) => {
  if (!item) return null
  return {
    ...item,
    enrollmentId: item.enrollmentId ?? item.id,
    createTime: item.createTime || item.enrollTime || '',
    courseTitle: item.courseTitle || courseMap.value[item.courseId] || ''
  }
}

const syncLastEnrollment = (rows) => {
  const normalizedRows = (rows || []).map((item) => toRecentEnrollment(item))
  const currentId = Number(lastEnrollment.value?.enrollmentId ?? lastEnrollment.value?.id ?? 0)
  const matched = currentId ? normalizedRows.find((item) => Number(item.enrollmentId) === currentId) : null
  lastEnrollment.value = matched || normalizedRows[0] || lastEnrollment.value || null
  if (lastEnrollment.value) {
    uni.setStorageSync(STORAGE_ENROLLMENT, JSON.stringify(lastEnrollment.value))
  } else {
    uni.removeStorageSync(STORAGE_ENROLLMENT)
  }
  if (currentEnrollmentStatus.value !== 2) {
    closeRefund()
  }
}

const loadLastEnrollment = () => {
  try {
    const cached = uni.getStorageSync(STORAGE_ENROLLMENT)
    if (cached) lastEnrollment.value = toRecentEnrollment(JSON.parse(cached))
  } catch (_) {
    lastEnrollment.value = null
  }
}

const selectSchedule = (schedule) => {
  if (!isScheduleAvailable(schedule)) {
    uni.showToast({ title: '该排期不可报名', icon: 'none' })
    return
  }
  selectedSchedule.value = schedule
}

const enrollSelected = async () => {
  if (!canEnroll.value) {
    uni.showToast({ title: '请选择可报名排期', icon: 'none' })
    return
  }
  try {
    enrolling.value = true
    const payload = await enrollCourse({ scheduleId: selectedSchedule.value.id })
    lastEnrollment.value = toRecentEnrollment(payload?.data || payload)
    if (lastEnrollment.value) {
      uni.setStorageSync(STORAGE_ENROLLMENT, JSON.stringify(lastEnrollment.value))
    }
    uni.showToast({ title: '报名成功', icon: 'success' })
    await loadEnrollments()
    await loadCourses()
    scrollToRecent()
  } finally {
    enrolling.value = false
  }
}

const mockPayLast = async () => {
  if (!lastEnrollment.value?.enrollmentId) return
  try {
    paying.value = true
    await mockPayCourse(lastEnrollment.value.enrollmentId)
    uni.showToast({ title: '支付状态已更新', icon: 'success' })
    await Promise.all([loadEnrollments(), loadMySchedules()])
    if (mySchedules.value.length) switchView('mine')
  } finally {
    paying.value = false
  }
}

const cancelLast = async () => {
  if (!lastEnrollment.value?.enrollmentId) return
  try {
    canceling.value = true
    await cancelUnpaidCourse(lastEnrollment.value.enrollmentId)
    uni.showToast({ title: '已取消报名', icon: 'success' })
    await Promise.all([loadEnrollments(), loadCourses()])
  } finally {
    canceling.value = false
  }
}

const openRefund = () => {
  refundVisible.value = true
}

const closeRefund = () => {
  refundVisible.value = false
  refundReason.value = ''
}

const refundLast = async () => {
  if (!lastEnrollment.value?.enrollmentId) return
  if (!refundReason.value.trim()) {
    uni.showToast({ title: '请输入退款原因', icon: 'none' })
    return
  }
  try {
    refunding.value = true
    await refundCourse({
      enrollmentId: lastEnrollment.value.enrollmentId,
      reason: refundReason.value.trim()
    })
    uni.showToast({ title: '退款已提交', icon: 'success' })
    closeRefund()
    await Promise.all([loadEnrollments(), loadMySchedules()])
  } finally {
    refunding.value = false
  }
}

const switchView = (mode) => {
  viewMode.value = mode
  mySchedulePage.value = 1
  enrollmentPage.value = 1
  uni.setStorageSync(STORAGE_COURSE_VIEW, mode)
}

const prevMySchedulePage = () => {
  mySchedulePage.value = Math.max(1, safeMySchedulePage.value - 1)
}

const nextMySchedulePage = () => {
  mySchedulePage.value = Math.min(myScheduleTotalPages.value, safeMySchedulePage.value + 1)
}

const prevEnrollmentPage = () => {
  enrollmentPage.value = Math.max(1, safeEnrollmentPage.value - 1)
}

const nextEnrollmentPage = () => {
  enrollmentPage.value = Math.min(enrollmentTotalPages.value, safeEnrollmentPage.value + 1)
}

const scrollToCatalog = async () => {
  await nextTick()
  uni.pageScrollTo({ selector: '.catalog-section', duration: 260 })
}

const scrollToSchedules = async () => {
  switchView('discover')
  await nextTick()
  uni.pageScrollTo({ selector: '.schedule-section', duration: 260 })
}

const scrollToRecent = async () => {
  await nextTick()
  uni.pageScrollTo({ selector: '.recent-card', duration: 260 })
}

const openCheckIn = (item) => {
  activeCheckIn.value = item
  checkInVisible.value = true
}

const closeCheckIn = () => {
  checkInVisible.value = false
  activeCheckIn.value = null
}

const copyCheckInCode = () => {
  const code = activeCheckIn.value?.checkInCode
  if (!code) {
    uni.showToast({ title: '暂无核销码', icon: 'none' })
    return
  }
  uni.setClipboardData({
    data: code,
    success: () => uni.showToast({ title: '已复制核销码', icon: 'success' })
  })
}

const goToOrder = (payload) => {
  const orderId = typeof payload === 'object' && payload !== null ? payload.orderId : payload
  if (!orderId) return
  const params = [`id=${encodeURIComponent(orderId)}`]
  if (payload?.enrollmentId) params.push(`enrollmentId=${encodeURIComponent(payload.enrollmentId)}`)
  if (payload?.courseId) params.push(`courseId=${encodeURIComponent(payload.courseId)}`)
  uni.navigateTo({ url: `/pages/order-detail/index?${params.join('&')}` })
}

const goBackHome = () => {
  const pages = getCurrentPages()
  if (pages.length > 1) {
    uni.navigateBack()
    return
  }
  uni.switchTab({ url: '/pages/home/index' })
}

const goTab = (tab) => {
  if (tab.active) return
  uni.switchTab({ url: tab.url })
}

const remainingSlots = (row) => {
  const capacity = Number(row?.capacity ?? 0)
  const booked = Number(row?.bookedCount ?? 0)
  const left = capacity - booked
  return Number.isFinite(left) ? Math.max(left, 0) : 0
}

const resolveScheduleStart = (row) => {
  if (!row) return null
  const rawStart = row.startTime
  if (rawStart && String(rawStart).includes('T')) {
    const ts = new Date(rawStart)
    return Number.isNaN(ts.getTime()) ? null : ts
  }
  if (!row.scheduleDate) return null
  const time = row.startTime || '00:00:00'
  const ts = new Date(`${row.scheduleDate}T${time}`)
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

const scheduleStatus = (row) => {
  if (!row) return { text: '-', tone: 'muted' }
  if (row.status !== null && row.status !== undefined && Number(row.status) !== 1) return { text: '停用', tone: 'muted' }
  const start = resolveScheduleStart(row)
  if (start && start.getTime() < Date.now() - 60 * 1000) return { text: '已过期', tone: 'warning' }
  if (remainingSlots(row) <= 0) return { text: '已满', tone: 'danger' }
  return { text: '可报名', tone: 'success' }
}

const formatCourseStatus = (status) => {
  if (Number(status) === 1) return '上架'
  if (Number(status) === 0) return '下架'
  return status ?? '-'
}

const formatEnrollmentStatus = (status) => {
  const map = { 0: '已取消', 1: '未支付', 2: '已支付', 3: '已退款' }
  return map[Number(status)] || status || '-'
}

const enrollmentStatusTone = (status) => {
  const map = { 0: 'muted', 1: 'warning', 2: 'success', 3: 'danger' }
  return map[Number(status)] || 'plain'
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

const formatDateTime = (value) => {
  if (!value) return '-'
  const raw = String(value)
  if (raw.includes('T')) {
    const [date, time] = raw.split('T')
    return `${date} ${time.slice(0, 5)}`
  }
  if (raw.length >= 16 && raw.includes('-')) return raw.slice(0, 16)
  return raw
}

const formatScheduleTime = (row) => {
  if (!row) return '-'
  const date = formatDate(row.scheduleDate || row.startTime)
  const start = formatTime(row.startTime)
  const end = formatTime(row.endTime)
  return `${date} ${start}-${end}`
}

onShow(() => {
  hideTabBarSafely()
  if (ensureLogin()) {
    loadLastEnrollment()
    refreshWorkspace()
  }
})

onHide(() => {
  showTabBarSafely()
})
</script>

<style scoped lang="scss">
.course-page {
  position: relative;
  min-height: 100vh;
  padding: calc(var(--status-bar-height) + 28rpx) 28rpx 206rpx;
  overflow-x: hidden;
  background:
    radial-gradient(circle at 6% 0%, rgba(139, 99, 255, 0.18), transparent 34%),
    radial-gradient(circle at 100% 8%, rgba(255, 128, 111, 0.18), transparent 28%),
    linear-gradient(180deg, #fff6f8 0%, #fffaf4 44%, #fffdf8 100%);
  color: #24104f;
}

.page-bg {
  position: fixed;
  z-index: 0;
  width: 360rpx;
  height: 360rpx;
  border-radius: 999rpx;
  filter: blur(20rpx);
  pointer-events: none;
}

.page-bg--left {
  left: -160rpx;
  top: 120rpx;
  background: rgba(139, 99, 255, 0.14);
}

.page-bg--right {
  right: -180rpx;
  top: 560rpx;
  background: rgba(255, 128, 111, 0.13);
}

.topbar,
.hero-card,
.metric-strip,
.tabs,
.workbench-card,
.schedule-section,
.submit-card,
.catalog-section,
.recent-card,
.my-overview,
.history-section,
.pagination-bar,
.custom-tabbar {
  position: relative;
  z-index: 1;
}

button {
  margin: 0;
  padding: 0;
  border: 0;
  background: transparent;
  line-height: 1.2;
}

button::after {
  border: 0;
}

.topbar {
  display: grid;
  grid-template-columns: 62rpx 1fr 62rpx;
  align-items: center;
  min-height: 62rpx;
  margin-bottom: 28rpx;
}

.topbar-title {
  color: #24104f;
  font-size: 34rpx;
  font-weight: 900;
  letter-spacing: 1rpx;
  text-align: center;
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

.spinning {
  animation: spin 0.9s linear infinite;
}

.hero-card {
  min-height: 268rpx;
  padding: 32rpx 30rpx;
  overflow: hidden;
  border-radius: 34rpx;
  background: linear-gradient(112deg, #e6dcff 0%, #ffdce0 52%, #fff0c8 100%);
  box-shadow: 0 16rpx 34rpx rgba(139, 99, 255, 0.14);
}

.hero-copy {
  position: relative;
  z-index: 2;
  display: flex;
  flex-direction: column;
}

.eyebrow,
.section-kicker {
  align-self: flex-start;
  margin-bottom: 12rpx;
  padding: 8rpx 16rpx;
  border: 2rpx solid rgba(36, 16, 79, 0.24);
  border-radius: 999rpx;
  background: rgba(255, 255, 255, 0.58);
  color: #24104f;
  font-size: 18rpx;
  font-weight: 900;
  letter-spacing: 2rpx;
}

.hero-title {
  font-size: 48rpx;
  font-weight: 950;
  line-height: 1.12;
  letter-spacing: -1rpx;
}

.hero-sub {
  margin-top: 18rpx;
  width: 460rpx;
  color: #6f6095;
  font-size: 24rpx;
  font-weight: 700;
  line-height: 1.45;
}

.hero-cloud,
.hero-hill,
.hero-trail,
.hero-tree {
  position: absolute;
}

.hero-cloud {
  border-radius: 999rpx;
  background: rgba(255, 255, 255, 0.75);
}

.hero-cloud--one {
  right: 68rpx;
  top: 66rpx;
  width: 96rpx;
  height: 30rpx;
}

.hero-cloud--two {
  right: 238rpx;
  top: 146rpx;
  width: 66rpx;
  height: 22rpx;
}

.hero-hill--back {
  right: -120rpx;
  bottom: -34rpx;
  width: 560rpx;
  height: 174rpx;
  border-radius: 100% 0 0 0;
  background: rgba(139, 99, 255, 0.34);
  transform: skewY(-8deg);
}

.hero-hill--front {
  right: -82rpx;
  bottom: -58rpx;
  width: 520rpx;
  height: 176rpx;
  border-radius: 100% 0 0 0;
  background: rgba(255, 185, 118, 0.6);
  transform: skewY(-11deg);
}

.hero-trail {
  right: 48rpx;
  bottom: 14rpx;
  width: 362rpx;
  height: 58rpx;
  border-radius: 999rpx;
  background: rgba(255, 245, 206, 0.64);
  transform: rotate(-10deg);
}

.hero-tree {
  bottom: 24rpx;
}

.hero-tree--one {
  right: 56rpx;
}

.hero-tree--two {
  right: 150rpx;
  transform: scale(0.74);
}

.hero-tree--three {
  right: 246rpx;
  transform: scale(0.52);
}

.tree-crown {
  width: 58rpx;
  height: 76rpx;
  border: 4rpx solid #34205f;
  border-radius: 50%;
  background: #ff806f;
}

.tree-trunk {
  width: 8rpx;
  height: 52rpx;
  margin: -8rpx auto 0;
  border-radius: 999rpx;
  background: #34205f;
}

.metric-strip {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  margin-top: -34rpx;
  padding: 24rpx 6rpx 22rpx;
  border-radius: 32rpx;
  background: rgba(255, 255, 255, 0.96);
  box-shadow: 0 12rpx 0 rgba(52, 32, 95, 0.08), 0 22rpx 40rpx rgba(139, 99, 255, 0.12);
}

.metric-item {
  display: flex;
  align-items: center;
  flex-direction: column;
  gap: 8rpx;
  padding: 4rpx;
  border-right: 2rpx solid rgba(52, 32, 95, 0.1);
}

.metric-item:last-child {
  border-right: none;
}

.metric-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 76rpx;
  height: 56rpx;
  border-radius: 999rpx;
}

.metric-icon image {
  width: 30rpx;
  height: 30rpx;
}

.is-purple {
  background: #eee5ff;
}

.is-coral {
  background: #ffe3dc;
}

.is-gold {
  background: #fff0c8;
}

.is-mint {
  background: #dff8eb;
}

.metric-label {
  color: #24104f;
  font-size: 22rpx;
  font-weight: 800;
}

.metric-value {
  color: #24104f;
  font-size: 36rpx;
  font-weight: 950;
}

.tabs {
  display: grid;
  grid-template-columns: 1fr 1fr;
  align-items: end;
  margin: 36rpx 0 24rpx;
}

.tab-button {
  min-height: 66rpx;
  color: rgba(36, 16, 79, 0.42);
  font-size: 29rpx;
  font-weight: 800;
  transition: color 0.18s ease, font-size 0.18s ease, transform 0.18s ease;
}

.tab-button.active {
  color: #6b4eea;
  font-size: 34rpx;
  font-weight: 900;
  transform: translateY(-2rpx);
}

.workbench-card,
.schedule-section,
.submit-card,
.catalog-section,
.recent-card,
.my-overview,
.history-section {
  margin-top: 22rpx;
  padding: 26rpx;
  border: 2rpx solid rgba(52, 32, 95, 0.15);
  border-radius: 30rpx;
  background: rgba(255, 255, 255, 0.94);
  box-shadow: 0 10rpx 28rpx rgba(52, 32, 95, 0.08);
}

.workbench-card {
  background: linear-gradient(180deg, rgba(245, 240, 255, 0.96) 0%, rgba(255, 250, 244, 0.96) 100%);
}

.workbench-head,
.section-head,
.schedule-top,
.course-top,
.last-top,
.itinerary-top,
.history-top,
.dialog-head {
  display: flex;
  justify-content: space-between;
  gap: 18rpx;
  align-items: flex-start;
}

.section-title {
  display: block;
  color: #24104f;
  font-size: 34rpx;
  font-weight: 950;
  line-height: 1.2;
}

.section-sub {
  display: block;
  margin-top: 8rpx;
  color: #786a99;
  font-size: 24rpx;
  line-height: 1.5;
}

.ghost-button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  min-width: 142rpx;
  min-height: 58rpx;
  padding: 0 20rpx;
  border: 2rpx solid rgba(52, 32, 95, 0.32);
  border-radius: 999rpx;
  background: #ffffff;
  color: #24104f;
  font-size: 23rpx;
  font-weight: 900;
  line-height: 1;
  box-shadow: 0 6rpx 0 rgba(52, 32, 95, 0.08);
  box-sizing: border-box;
}

.selected-course {
  display: grid;
  grid-template-columns: 154rpx minmax(0, 1fr);
  gap: 20rpx;
  margin-top: 22rpx;
}

.selected-cover,
.course-cover,
.itinerary-cover {
  width: 100%;
  border-radius: 24rpx;
  background: #f3efff;
}

.selected-cover {
  height: 154rpx;
}

.selected-title,
.course-title,
.itinerary-title,
.history-title,
.last-title,
.confirm-title {
  display: block;
  color: #24104f;
  font-size: 29rpx;
  font-weight: 950;
  line-height: 1.25;
}

.selected-summary,
.course-summary,
.last-sub,
.history-course,
.confirm-time,
.itinerary-time {
  display: block;
  margin-top: 8rpx;
  color: #776796;
  font-size: 23rpx;
  line-height: 1.45;
}

.chip-row {
  display: flex;
  flex-wrap: wrap;
  gap: 10rpx;
  margin-top: 16rpx;
}

.chip,
.mini-chip,
.count-pill,
.status-pill,
.chosen-pill {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 44rpx;
  padding: 0 16rpx;
  border-radius: 999rpx;
  font-size: 21rpx;
  font-weight: 850;
}

.chip {
  border: 2rpx solid rgba(52, 32, 95, 0.18);
  background: #fffaf4;
  color: #24104f;
}

.mini-chip {
  background: rgba(139, 99, 255, 0.1);
  color: #24104f;
}

.count-pill,
.chosen-pill {
  background: #f0e7ff;
  color: #8b63ff;
}

.status-pill {
  background: #f1edf5;
  color: #786a99;
}

.status-pill.success,
.success {
  background: #dcf8e7;
  color: #19a65f;
}

.status-pill.warning,
.warning {
  background: #fff0cf;
  color: #c07604;
}

.status-pill.danger,
.danger {
  background: #ffe6ed;
  color: #d94d77;
}

.plain {
  background: #f0e7ff;
  color: #8b63ff;
}

.muted {
  background: #f1edf5;
  color: #786a99;
}

.schedule-scroll {
  width: 100%;
  margin-top: 20rpx;
  white-space: nowrap;
}

.schedule-row {
  display: inline-flex;
  gap: 18rpx;
  padding-bottom: 8rpx;
}

.schedule-card {
  width: 560rpx;
  padding: 24rpx;
  border: 3rpx solid rgba(52, 32, 95, 0.14);
  border-radius: 28rpx;
  background: linear-gradient(180deg, #ffffff 0%, #fffaf4 100%);
  text-align: left;
  white-space: normal;
}

.schedule-card.active {
  border-color: #8b63ff;
  box-shadow: 0 10rpx 0 rgba(139, 99, 255, 0.16);
}

.schedule-card.disabled {
  opacity: 0.62;
}

.schedule-time {
  color: #24104f;
  font-size: 26rpx;
  font-weight: 950;
  line-height: 1.35;
}

.schedule-meta,
.schedule-chips {
  display: flex;
  flex-wrap: wrap;
  gap: 12rpx;
  margin-top: 16rpx;
}

.schedule-meta text {
  color: #7b6f98;
  font-size: 23rpx;
  font-weight: 700;
}

.submit-card {
  display: flex;
  flex-direction: column;
  gap: 22rpx;
}

.confirm-panel,
.last-enrollment,
.refund-box {
  padding: 22rpx;
  border: 2rpx solid rgba(52, 32, 95, 0.12);
  border-radius: 24rpx;
  background: #fffdf9;
}

.primary-button {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12rpx;
  min-height: 88rpx;
  border: 4rpx solid #24104f;
  border-radius: 28rpx;
  background: linear-gradient(110deg, #8b63ff 0%, #d88bde 52%, #ff806f 100%);
  color: #ffffff;
  font-size: 30rpx;
  font-weight: 950;
  line-height: 1;
  box-shadow: 0 10rpx 0 rgba(52, 32, 95, 0.12);
  box-sizing: border-box;
}

.primary-button[disabled],
.small-solid[disabled],
.small-danger[disabled],
.small-ghost[disabled] {
  opacity: 0.52;
}

.button-icon {
  width: 30rpx;
  height: 30rpx;
}

.search-box {
  display: flex;
  align-items: center;
  gap: 14rpx;
  min-height: 82rpx;
  margin-top: 22rpx;
  padding: 0 22rpx;
  border: 2rpx solid rgba(52, 32, 95, 0.15);
  border-radius: 999rpx;
  background: #ffffff;
}

.search-icon {
  width: 28rpx;
  height: 28rpx;
}

.search-input {
  flex: 1;
  height: 82rpx;
  color: #24104f;
  font-size: 26rpx;
}

.clear-button {
  color: #8b63ff;
  font-size: 23rpx;
  font-weight: 900;
}

.course-list,
.itinerary-list,
.history-list {
  display: flex;
  flex-direction: column;
  gap: 18rpx;
  margin-top: 22rpx;
}

.course-card {
  position: relative;
  display: grid;
  grid-template-columns: 172rpx minmax(0, 1fr) 28rpx;
  gap: 18rpx;
  align-items: center;
  padding: 18rpx;
  border: 2rpx solid rgba(52, 32, 95, 0.12);
  border-radius: 28rpx;
  background: rgba(255, 255, 255, 0.96);
  text-align: left;
  box-shadow: 0 8rpx 20rpx rgba(52, 32, 95, 0.06);
}

.course-card.active {
  border-color: #8b63ff;
  background: linear-gradient(180deg, #f7f2ff 0%, #fffaf4 100%);
}

.course-cover {
  height: 140rpx;
}

.card-chevron {
  width: 22rpx;
  height: 22rpx;
}

.action-row {
  display: flex;
  flex-wrap: wrap;
  gap: 14rpx;
  margin-top: 18rpx;
}

.small-solid,
.small-danger,
.small-ghost {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 148rpx;
  min-height: 58rpx;
  padding: 0 22rpx;
  border-radius: 999rpx;
  font-size: 23rpx;
  font-weight: 950;
  line-height: 1;
  box-sizing: border-box;
}

.small-solid {
  background: #8b63ff;
  color: #ffffff;
  box-shadow: 0 6rpx 0 rgba(52, 32, 95, 0.12);
}

.small-danger {
  background: #ffe8ef;
  color: #d94d77;
}

.small-danger.outline {
  border: 2rpx solid #d94d77;
  background: #ffffff;
}

.small-ghost {
  border: 2rpx solid rgba(52, 32, 95, 0.24);
  background: #ffffff;
  color: #24104f;
}

.refund-textarea {
  width: 100%;
  min-height: 142rpx;
  padding: 20rpx;
  border: 2rpx solid rgba(52, 32, 95, 0.16);
  border-radius: 22rpx;
  background: #ffffff;
  color: #24104f;
  font-size: 25rpx;
  line-height: 1.45;
}

.itinerary-card {
  display: grid;
  grid-template-columns: 176rpx minmax(0, 1fr);
  gap: 18rpx;
  align-items: stretch;
  padding: 18rpx;
  border: 2rpx solid rgba(52, 32, 95, 0.13);
  border-radius: 30rpx;
  background: #ffffff;
}

.itinerary-left {
  display: flex;
  flex-direction: column;
  gap: 12rpx;
}

.itinerary-cover {
  width: 176rpx;
  height: 138rpx;
  border-radius: 22rpx;
  background: #f3efff;
}

.itinerary-label {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8rpx;
  min-height: 48rpx;
  border-radius: 999rpx;
  background: #f4efff;
  color: #24104f;
  font-size: 20rpx;
  font-weight: 900;
}

.itinerary-label-icon {
  width: 22rpx;
  height: 22rpx;
}

.itinerary-main {
  min-width: 0;
  padding: 2rpx 0;
}

.itinerary-main .action-row {
  gap: 10rpx;
}

.itinerary-main .small-solid,
.itinerary-main .small-ghost {
  min-width: 132rpx;
  min-height: 54rpx;
  padding: 0 18rpx;
  font-size: 22rpx;
}

.history-card {
  padding: 22rpx;
  border: 2rpx solid rgba(52, 32, 95, 0.12);
  border-radius: 26rpx;
  background: #ffffff;
}

.pagination-bar {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 18rpx;
  margin-top: 18rpx;
}

.page-button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 132rpx;
  height: 54rpx;
  padding: 0 24rpx;
  border: 2rpx solid rgba(52, 32, 95, 0.24);
  border-radius: 999rpx;
  background: #ffffff;
  color: #24104f;
  font-size: 22rpx;
  font-weight: 900;
  line-height: 1;
  box-shadow: 0 6rpx 0 rgba(52, 32, 95, 0.06);
  box-sizing: border-box;
}

.page-button[disabled] {
  opacity: 0.42;
  box-shadow: none;
}

.page-count {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 92rpx;
  height: 54rpx;
  color: #6b4eea;
  font-size: 23rpx;
  font-weight: 900;
  line-height: 1;
  text-align: center;
}

.empty-card,
.empty-soft {
  display: flex;
  align-items: center;
  justify-content: center;
  flex-direction: column;
  gap: 12rpx;
  min-height: 240rpx;
  margin-top: 22rpx;
  padding: 28rpx;
  border-radius: 28rpx;
  background: rgba(246, 240, 255, 0.72);
}

.empty-soft {
  min-height: 168rpx;
}

.empty-icon {
  width: 58rpx;
  height: 58rpx;
}

.empty-title {
  color: #24104f;
  font-size: 28rpx;
  font-weight: 950;
}

.empty-desc,
.empty-soft text {
  color: #7b6f98;
  font-size: 24rpx;
  line-height: 1.45;
  text-align: center;
}

.empty-button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 180rpx;
  min-height: 62rpx;
  margin-top: 8rpx;
  border-radius: 999rpx;
  background: #8b63ff;
  color: #ffffff;
  font-size: 24rpx;
  font-weight: 900;
  line-height: 1;
  box-sizing: border-box;
}

.custom-tabbar {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  z-index: 10;
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  padding: 18rpx 22rpx calc(env(safe-area-inset-bottom) + 26rpx);
  border: 3rpx solid rgba(139, 99, 255, 0.52);
  border-bottom: 0;
  border-radius: 42rpx 42rpx 0 0;
  background: rgba(255, 255, 255, 0.95);
  box-shadow: 0 -10rpx 26rpx rgba(52, 32, 95, 0.09);
}

.tab-item {
  display: flex;
  align-items: center;
  flex-direction: column;
  gap: 8rpx;
  color: #24104f;
  font-size: 22rpx;
  font-weight: 850;
}

.tab-item.active {
  color: #8b63ff;
}

.tab-icon {
  width: 44rpx;
  height: 44rpx;
}

.home-indicator {
  position: absolute;
  left: 50%;
  bottom: calc(env(safe-area-inset-bottom) + 8rpx);
  width: 160rpx;
  height: 8rpx;
  border-radius: 999rpx;
  background: #24104f;
  transform: translateX(-50%);
}

.dialog-mask {
  position: fixed;
  inset: 0;
  z-index: 30;
  display: flex;
  align-items: flex-end;
  padding: 28rpx;
  background: rgba(36, 16, 79, 0.34);
}

.checkin-dialog {
  width: 100%;
  padding: 28rpx;
  border-radius: 34rpx;
  background: #fffaf4;
  box-shadow: 0 -18rpx 48rpx rgba(36, 16, 79, 0.18);
}

.dialog-title {
  display: block;
  color: #24104f;
  font-size: 34rpx;
  font-weight: 950;
}

.dialog-desc {
  display: block;
  margin-top: 8rpx;
  color: #7b6f98;
  font-size: 24rpx;
  line-height: 1.5;
}

.dialog-close {
  width: 58rpx;
  height: 58rpx;
}

.dialog-close image {
  width: 36rpx;
  height: 36rpx;
}

.checkin-code {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 168rpx;
  margin-top: 26rpx;
  border-radius: 28rpx;
  background: linear-gradient(135deg, #eee5ff 0%, #fff0c8 100%);
  color: #24104f;
  font-size: 72rpx;
  font-weight: 950;
  letter-spacing: 10rpx;
}

.checkin-meta {
  display: flex;
  flex-direction: column;
  gap: 8rpx;
  margin-top: 18rpx;
  color: #786a99;
  font-size: 24rpx;
  font-weight: 800;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}
</style>
