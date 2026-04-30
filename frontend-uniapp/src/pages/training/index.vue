<template>
  <view class="training-page">
    <view class="page-bg page-bg--left"></view>
    <view class="page-bg page-bg--right"></view>

    <view class="topbar">
      <button class="icon-button" aria-label="返回" @click="goBackHome">
        <image class="top-icon" :src="icons.back" mode="aspectFit" />
      </button>
      <text class="topbar-title">打卡成长</text>
      <button class="icon-button" aria-label="选择日期" @click="openDateSheet">
        <image class="top-icon" :src="icons.calendar" mode="aspectFit" />
      </button>
    </view>

    <view class="hero">
      <view class="hero-cloud hero-cloud--one"></view>
      <view class="hero-cloud hero-cloud--two"></view>
      <view class="hero-hill hero-hill--back"></view>
      <view class="hero-hill hero-hill--front"></view>
      <view class="hero-trail"></view>
      <view class="coach-figure">
        <view class="coach-head"></view>
        <view class="coach-hair"></view>
        <view class="coach-body"></view>
        <view class="coach-arm coach-arm--left"></view>
        <view class="coach-arm coach-arm--right"></view>
        <view class="coach-board"></view>
        <view class="coach-leg coach-leg--left"></view>
        <view class="coach-leg coach-leg--right"></view>
      </view>
      <view class="hero-copy">
        <text class="hero-title">每一次坚持，</text>
        <text class="hero-title">都在让未来的你更强大</text>
        <text class="hero-sub">记录真实训练计划、动作、日期和感受，沉淀你的成长曲线。</text>
      </view>
    </view>

    <view class="weekly-card">
      <view class="card-head">
        <text class="section-title">本周统计</text>
        <button class="ghost-link" :disabled="statsLoading" @click="loadStats">
          <text>{{ statsLoading ? '同步中' : '刷新统计' }}</text>
          <image class="tiny-icon" :src="icons.right" mode="aspectFit" />
        </button>
      </view>
      <view class="metric-row">
        <view class="metric-card metric-card--purple">
          <view class="metric-icon">
            <image :src="icons.clipboardCheck" mode="aspectFit" />
          </view>
          <text class="metric-label">本周打卡</text>
          <view class="metric-value">
            <text>{{ checkinCount }}</text>
            <text>/{{ statDays }} 天</text>
          </view>
          <text class="metric-note">连续 {{ streakDays }} 天</text>
        </view>
        <view class="metric-card metric-card--rose">
          <view class="metric-icon">
            <image :src="icons.clock" mode="aspectFit" />
          </view>
          <text class="metric-label">训练时长</text>
          <view class="metric-value">
            <text>{{ totalDuration }}</text>
            <text> 分钟</text>
          </view>
          <text class="metric-note">均次 {{ avgDuration }} 分钟</text>
        </view>
        <view class="metric-card metric-card--gold">
          <view class="metric-icon">
            <image :src="icons.fire" mode="aspectFit" />
          </view>
          <text class="metric-label">消耗热量</text>
          <view class="metric-value">
            <text>{{ totalCalories }}</text>
            <text> kcal</text>
          </view>
          <text class="metric-note">均次 {{ avgCalories }} kcal</text>
        </view>
      </view>
    </view>

    <view class="check-section">
      <text class="section-title section-title--standalone">今日打卡</text>
      <view class="check-grid">
        <view class="form-card">
          <view class="field-row">
            <text class="field-label">训练计划</text>
            <button class="select-pill" :disabled="plansLoading" @click="openPlanSheet">
              <text>{{ currentPlanTitle }}</text>
              <image class="field-icon" :src="icons.right" mode="aspectFit" />
            </button>
          </view>
          <view class="field-row">
            <text class="field-label">动作选择</text>
            <button class="select-pill" :disabled="!planItems.length || planItemLoading" @click="openActionSheet">
              <text>{{ selectedItem?.actionName || '选择本次训练动作' }}</text>
              <image class="field-icon" :src="icons.right" mode="aspectFit" />
            </button>
          </view>
          <view class="field-row">
            <text class="field-label">训练日期</text>
            <button class="select-pill" @click="openDateSheet">
              <image class="field-icon field-icon--left" :src="icons.calendar" mode="aspectFit" />
              <text>{{ form.recordDate }}</text>
            </button>
          </view>
          <view class="field-row">
            <text class="field-label">训练时长</text>
            <view class="input-pill">
              <image class="field-icon field-icon--left" :src="icons.clock" mode="aspectFit" />
              <input v-model.number="form.durationMin" type="number" class="mini-input" placeholder="分钟" />
              <text>分钟</text>
            </view>
          </view>
          <view class="field-row">
            <text class="field-label">消耗热量</text>
            <view class="input-pill">
              <image class="field-icon field-icon--left" :src="icons.fire" mode="aspectFit" />
              <input v-model.number="form.calories" type="number" class="mini-input" placeholder="kcal" />
              <text>kcal</text>
            </view>
          </view>
          <view class="mood-row">
            <text class="field-label">训练感受</text>
            <view class="mood-list">
              <button
                v-for="mood in moods"
                :key="mood.value"
                class="mood-button"
                :class="{ active: selectedMood === mood.value }"
                @click="selectedMood = mood.value"
              >
                <image class="mood-icon" :src="mood.icon" mode="aspectFit" />
              </button>
            </view>
          </view>
          <view class="textarea-wrap">
            <textarea
              v-model.trim="form.feeling"
              class="feeling-textarea"
              maxlength="100"
              placeholder="记录一下你的训练感受吧..."
            />
            <text class="textarea-count">{{ form.feeling.length }}/100</text>
          </view>
          <button class="submit-button" :disabled="submitting || !canSubmit" @click="submit">
            {{ submitting ? '提交中...' : '完成打卡' }}
          </button>
        </view>

        <view class="preview-card">
          <view class="preview-head">
            <text class="section-title-sm">动作预览（{{ planItems.length }}个）</text>
            <button class="refresh-mini" :disabled="planItemLoading" @click="reloadCurrentPlan">
              <image :class="{ spinning: planItemLoading }" :src="icons.refresh" mode="aspectFit" />
            </button>
          </view>
          <view v-if="!planItems.length && !planItemLoading" class="empty-mini">
            <image class="empty-icon" :src="icons.dumbbell" mode="aspectFit" />
            <text>当前计划暂无动作节点</text>
          </view>
          <view v-else class="action-list">
            <view
              v-for="item in previewItems"
              :key="item.id"
              class="action-card"
              :class="{ active: Number(form.planItemId) === Number(item.id) }"
              @click="selectAction(item)"
            >
              <view class="action-avatar">
                <image :src="resolveActionIcon(item)" mode="aspectFit" />
              </view>
              <view class="action-copy">
                <text class="action-title">{{ item.actionName || '训练动作' }}</text>
                <text class="action-meta">
                  {{ item.sets || 0 }} 组 × {{ item.reps || 0 }} 次
                </text>
                <text class="action-meta">休息 {{ item.restSec || 0 }} 秒</text>
              </view>
              <button class="play-button" :disabled="!item.video?.playUrl" @click.stop="openVideo(item)">
                <image :src="icons.play" mode="aspectFit" />
              </button>
            </view>
          </view>
          <button v-if="planItems.length > 4" class="all-actions" @click="openActionSheet">
            查看全部动作
            <image class="tiny-icon" :src="icons.right" mode="aspectFit" />
          </button>
        </view>
      </view>
    </view>

    <view class="records-section">
      <view class="card-head">
        <text class="section-title">最近训练记录</text>
        <button class="ghost-link" :disabled="recordsLoading" @click="loadRecords">
          <text>{{ recordsLoading ? '刷新中' : '全部记录' }}</text>
          <image class="tiny-icon" :src="icons.right" mode="aspectFit" />
        </button>
      </view>
      <view v-if="!recentRecords.length && !recordsLoading" class="empty-record">
        <image class="empty-icon" :src="icons.clipboardCheck" mode="aspectFit" />
        <text>暂无打卡记录，完成今天第一次训练吧</text>
      </view>
      <view v-else class="record-timeline">
        <view v-for="record in recentRecords" :key="record.id" class="record-row">
          <view class="record-date">
            <text>{{ formatRecordMonthDay(record.recordDate) }}</text>
            <text>{{ formatRecordWeekday(record.recordDate) }}</text>
          </view>
          <view class="record-line">
            <view class="record-dot" :class="{ muted: !record.durationMin }">
              <image v-if="record.durationMin" :src="icons.check" mode="aspectFit" />
            </view>
          </view>
          <view class="record-card">
            <view class="record-icon">
              <image :src="recordIcon(record)" mode="aspectFit" />
            </view>
            <view class="record-main">
              <text class="record-title">{{ recordTitle(record) }}</text>
              <view class="record-chips">
                <text>{{ record.durationMin || 0 }} 分钟</text>
                <text>{{ record.calories || 0 }} kcal</text>
              </view>
            </view>
            <text class="record-status">打卡成功</text>
          </view>
        </view>
      </view>
    </view>

    <view v-if="planSheetVisible" class="sheet-mask" @click="closeSheets">
      <view class="bottom-sheet" @click.stop>
        <view class="sheet-handle"></view>
        <view class="sheet-head">
          <view>
            <text class="sheet-title">选择训练计划</text>
            <text class="sheet-sub">只展示你已激活且可打卡的训练计划</text>
          </view>
          <button class="sheet-close" @click="closeSheets">
            <image :src="icons.close" mode="aspectFit" />
          </button>
        </view>
        <view v-if="!plans.length" class="empty-sheet">
          <text>你还没有可打卡计划</text>
          <button class="sheet-primary" @click="goPlans">去训练计划中心</button>
        </view>
        <view v-else class="sheet-list">
          <button
            v-for="plan in plans"
            :key="plan.id"
            class="sheet-option"
            :class="{ active: Number(form.planId) === Number(plan.id) }"
            @click="selectPlan(plan)"
          >
            <text>{{ plan.title || `训练计划 ${plan.id}` }}</text>
            <text>开始于 {{ plan.startDate || '未设置' }} · 周期 {{ plan.durationWeeks || 0 }} 周</text>
          </button>
        </view>
      </view>
    </view>

    <view v-if="actionSheetVisible" class="sheet-mask" @click="closeSheets">
      <view class="bottom-sheet" @click.stop>
        <view class="sheet-handle"></view>
        <view class="sheet-head">
          <view>
            <text class="sheet-title">选择训练动作</text>
            <text class="sheet-sub">{{ currentPlanTitle }}</text>
          </view>
          <button class="sheet-close" @click="closeSheets">
            <image :src="icons.close" mode="aspectFit" />
          </button>
        </view>
        <view class="sheet-list action-sheet-list">
          <button
            v-for="item in planItems"
            :key="item.id"
            class="sheet-option action-option"
            :class="{ active: Number(form.planItemId) === Number(item.id) }"
            @click="selectAction(item)"
          >
            <view>
              <text>{{ item.actionName || '训练动作' }}</text>
              <text>DAY {{ item.dayIndex || '-' }} · {{ item.sets || 0 }} 组 × {{ item.reps || 0 }} 次 · 休息 {{ item.restSec || 0 }} 秒</text>
            </view>
            <image class="sheet-option-icon" :src="resolveActionIcon(item)" mode="aspectFit" />
          </button>
        </view>
      </view>
    </view>

    <view v-if="dateSheetVisible" class="sheet-mask" @click="closeSheets">
      <view class="bottom-sheet" @click.stop>
        <view class="sheet-handle"></view>
        <view class="sheet-head">
          <view>
            <text class="sheet-title">选择训练日期</text>
            <text class="sheet-sub">开始日期之前的记录后端会拒绝写入</text>
          </view>
          <button class="sheet-close" @click="closeSheets">
            <image :src="icons.close" mode="aspectFit" />
          </button>
        </view>
        <view class="date-list">
          <button
            v-for="day in dateOptions"
            :key="day.value"
            class="date-option"
            :class="{ active: form.recordDate === day.value }"
            @click="chooseDate(day.value)"
          >
            <text>{{ day.label }}</text>
            <text>{{ day.week }}</text>
            <text>{{ day.value.slice(5) }}</text>
          </button>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup>
import {
  faCalendarDays,
  faChevronLeft,
  faChevronRight,
  faCircleCheck,
  faCirclePlay,
  faCircleXmark,
  faClipboardCheck,
  faClock,
  faDumbbell,
  faFaceFrown,
  faFaceGrinStars,
  faFaceMeh,
  faFaceSmile,
  faFireFlameCurved,
  faPersonRunning,
  faRotateRight
} from '@fortawesome/free-solid-svg-icons'
import { computed, reactive, ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { getPlanDetail, listPlans } from '../../api/modules/plan'
import { checkIn, getWeeklyStat, listMyRecords } from '../../api/modules/record'
import { ensureLogin } from '../../utils/authGuard'
import { createVideoPlaylist, saveVideoPlaylist } from '../../utils/videoPlaylist'

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
  calendar: faIcon(faCalendarDays),
  check: faIcon(faCircleCheck, '#ffffff'),
  clipboardCheck: faIcon(faClipboardCheck, '#8b63ff'),
  clock: faIcon(faClock, '#f65f8f'),
  close: faIcon(faCircleXmark),
  dumbbell: faIcon(faDumbbell),
  faceFrown: faIcon(faFaceFrown, '#a092bc'),
  faceMeh: faIcon(faFaceMeh, '#a092bc'),
  faceSmile: faIcon(faFaceSmile, '#a092bc'),
  faceGreat: faIcon(faFaceGrinStars, '#ffad43'),
  fire: faIcon(faFireFlameCurved, '#ff9f28'),
  play: faIcon(faCirclePlay),
  refresh: faIcon(faRotateRight),
  right: faIcon(faChevronRight),
  run: faIcon(faPersonRunning)
}

const WEEK_LABELS = ['周日', '周一', '周二', '周三', '周四', '周五', '周六']
const STORAGE_PLAN_ID = 'senlian_training_plan_id'
const STORAGE_ACTION_ID = 'senlian_training_action_id'

const formatDateValue = (date) => {
  const local = new Date(date.getTime() - date.getTimezoneOffset() * 60000)
  return local.toISOString().slice(0, 10)
}

const today = () => formatDateValue(new Date())

const addDays = (date, offset) => {
  const next = new Date(date)
  next.setDate(next.getDate() + offset)
  return next
}

const toDateObject = (value) => {
  if (!/^\d{4}-\d{2}-\d{2}$/.test(value || '')) return new Date()
  const [year, month, day] = value.split('-').map(Number)
  return new Date(year, month - 1, day)
}

const submitting = ref(false)
const statsLoading = ref(false)
const plansLoading = ref(false)
const planItemLoading = ref(false)
const recordsLoading = ref(false)
const planSheetVisible = ref(false)
const actionSheetVisible = ref(false)
const dateSheetVisible = ref(false)
const selectedMood = ref('状态很好')
const stats = ref({})
const plans = ref([])
const selectedPlan = ref(null)
const planItems = ref([])
const records = ref([])
const form = reactive({
  planId: '',
  planItemId: '',
  recordDate: today(),
  durationMin: 30,
  calories: 200,
  feeling: ''
})

const moods = [
  { value: '状态一般', icon: icons.faceFrown },
  { value: '有点累', icon: icons.faceMeh },
  { value: '完成不错', icon: icons.faceSmile },
  { value: '状态很好', icon: icons.faceGreat }
]

const normalizeList = (payload) => {
  const data = payload?.data ?? payload
  if (Array.isArray(data)) return data
  if (Array.isArray(data?.list)) return data.list
  if (Array.isArray(data?.records)) return data.records
  return []
}

const isAvailablePlan = (plan) => Boolean(plan?.subscribed) && Number(plan?.status) === 1

const planTitleMap = computed(() => {
  const map = {}
  plans.value.forEach((plan) => {
    map[plan.id] = plan.title
  })
  return map
})

const actionTitleMap = computed(() => {
  const map = {}
  planItems.value.forEach((item) => {
    map[item.id] = item.actionName
  })
  return map
})

const selectedItem = computed(() => {
  if (!form.planItemId) return null
  return planItems.value.find((item) => Number(item.id) === Number(form.planItemId)) || null
})

const currentPlanTitle = computed(() => selectedPlan.value?.title || '选择训练计划')
const previewItems = computed(() => planItems.value.slice(0, 4))
const recentRecords = computed(() => records.value.slice(0, 4))
const statDays = computed(() => Number(stats.value?.days || 7))
const checkinCount = computed(() => Number(stats.value?.checkinCount ?? stats.value?.totalCount ?? 0))
const totalDuration = computed(() => Number(stats.value?.totalDurationMin ?? stats.value?.totalDuration ?? 0))
const totalCalories = computed(() => Number(stats.value?.totalCalories ?? 0))
const avgDuration = computed(() => (checkinCount.value ? Math.round(totalDuration.value / checkinCount.value) : 0))
const avgCalories = computed(() => (checkinCount.value ? Math.round(totalCalories.value / checkinCount.value) : 0))
const canSubmit = computed(() => Boolean(form.planId && form.planItemId && form.recordDate))

const streakDays = computed(() => {
  const recordSet = new Set((records.value || []).map((item) => String(item.recordDate || '').slice(0, 10)).filter(Boolean))
  let streak = 0
  for (let offset = 0; offset < 30; offset += 1) {
    const value = formatDateValue(addDays(new Date(), -offset))
    if (!recordSet.has(value)) break
    streak += 1
  }
  return streak
})

const dateOptions = computed(() => {
  const base = new Date()
  return Array.from({ length: 14 }, (_, index) => {
    const date = addDays(base, index)
    const value = formatDateValue(date)
    const label = index === 0 ? '今天' : index === 1 ? '明天' : `${date.getMonth() + 1}/${date.getDate()}`
    return {
      value,
      label,
      week: WEEK_LABELS[date.getDay()]
    }
  })
})

const loadStats = async () => {
  try {
    statsLoading.value = true
    const payload = await getWeeklyStat()
    stats.value = payload?.data || payload || {}
  } finally {
    statsLoading.value = false
  }
}

const loadRecords = async () => {
  try {
    recordsLoading.value = true
    const payload = await listMyRecords({ limit: 20 })
    records.value = normalizeList(payload)
  } finally {
    recordsLoading.value = false
  }
}

const loadPlans = async () => {
  try {
    plansLoading.value = true
    const payload = await listPlans()
    plans.value = normalizeList(payload).filter(isAvailablePlan)
    if (!plans.value.length) {
      selectedPlan.value = null
      planItems.value = []
      form.planId = ''
      form.planItemId = ''
      return
    }

    const cachedPlanId = Number(uni.getStorageSync(STORAGE_PLAN_ID) || 0)
    const currentId = Number(form.planId || cachedPlanId || 0)
    const target = plans.value.find((plan) => Number(plan.id) === currentId) || plans.value[0]
    await selectPlan(target, { silent: true })
  } finally {
    plansLoading.value = false
  }
}

const loadPlanDetail = async (planId, preferredActionId = form.planItemId) => {
  if (!planId) {
    planItems.value = []
    form.planItemId = ''
    return
  }
  try {
    planItemLoading.value = true
    const payload = await getPlanDetail(planId)
    const items = normalizeList(payload?.data?.items || payload?.items || [])
    planItems.value = items.sort((a, b) => Number(a.dayIndex || 0) - Number(b.dayIndex || 0) || Number(a.sort || 0) - Number(b.sort || 0))
    const cachedActionId = Number(uni.getStorageSync(STORAGE_ACTION_ID) || 0)
    const actionId = Number(preferredActionId || cachedActionId || 0)
    const nextAction = planItems.value.find((item) => Number(item.id) === actionId) || planItems.value[0]
    if (nextAction) {
      selectAction(nextAction, { silent: true })
    } else {
      form.planItemId = ''
    }
  } finally {
    planItemLoading.value = false
  }
}

const selectPlan = async (plan, options = {}) => {
  if (!plan?.id) return
  selectedPlan.value = plan
  form.planId = plan.id
  uni.setStorageSync(STORAGE_PLAN_ID, String(plan.id))
  if (plan.startDate && form.recordDate < String(plan.startDate)) {
    form.recordDate = String(plan.startDate)
  }
  closeSheets()
  await loadPlanDetail(plan.id)
  if (!options.silent) {
    uni.showToast({ title: '已切换计划', icon: 'none' })
  }
}

const selectAction = (item, options = {}) => {
  if (!item?.id) return
  form.planItemId = item.id
  uni.setStorageSync(STORAGE_ACTION_ID, String(item.id))
  if (!form.durationMin || Number(form.durationMin) <= 0) {
    form.durationMin = Number(item.durationMin || 30)
  }
  if (!form.calories || Number(form.calories) <= 0) {
    form.calories = Math.max(80, Number(item.durationMin || 30) * 8)
  }
  closeSheets()
  if (!options.silent) {
    uni.showToast({ title: '已选择动作', icon: 'none' })
  }
}

const reloadCurrentPlan = async () => {
  if (!form.planId) return
  await loadPlanDetail(form.planId)
}

const submit = async () => {
  if (!plans.value.length) {
    uni.showToast({ title: '请先激活训练计划', icon: 'none' })
    return
  }
  if (!canSubmit.value) {
    uni.showToast({ title: '请完整选择计划、动作和日期', icon: 'none' })
    return
  }
  try {
    submitting.value = true
    const note = form.feeling.trim()
    await checkIn({
      planId: Number(form.planId),
      planItemId: Number(form.planItemId),
      recordDate: form.recordDate,
      durationMin: Number(form.durationMin || 0),
      calories: Number(form.calories || 0),
      feeling: [selectedMood.value, note].filter(Boolean).join('｜')
    })
    uni.showToast({ title: '打卡成功', icon: 'success' })
    form.feeling = ''
    await Promise.all([loadStats(), loadRecords()])
  } finally {
    submitting.value = false
  }
}

const openVideo = (item) => {
  const playUrl = item?.video?.playUrl
  if (!playUrl) {
    uni.showToast({ title: '当前动作暂无视频', icon: 'none' })
    return
  }
  const playlist = createVideoPlaylist({
    items: planItems.value,
    source: currentPlanTitle.value,
    cover: '',
    fallbackTitle: '训练教学视频'
  })
  const playlistIndex = playlist.findIndex((entry) => Number(entry.id) === Number(item.id))
  const playlistKey = saveVideoPlaylist(playlist, `training-${form.planId || 'checkin'}`)

  uni.navigateTo({
    url:
      `/pages/video-player/index?playlistKey=${encodeURIComponent(playlistKey)}`
      + `&index=${Math.max(playlistIndex, 0)}`
      + `&url=${encodeURIComponent(playUrl)}`
      + `&title=${encodeURIComponent(item?.video?.title || item?.actionName || '训练教学视频')}`
      + `&source=${encodeURIComponent(currentPlanTitle.value)}`
      + `&action=${encodeURIComponent(item?.actionName || '')}`
  })
}

const resolveActionIcon = (item) => {
  const name = String(item?.actionName || '')
  if (/跑|跳|波比|有氧|燃脂/.test(name)) return icons.run
  if (/火|燃|热/.test(name)) return icons.fire
  return icons.dumbbell
}

const recordIcon = (record) => {
  const title = recordTitle(record)
  if (/跑|跳|燃脂|有氧/.test(title)) return icons.run
  return icons.dumbbell
}

const recordTitle = (record) => actionTitleMap.value[record.planItemId] || planTitleMap.value[record.planId] || `训练记录 ${record.id || ''}`

const formatRecordMonthDay = (value) => {
  const raw = String(value || '')
  return raw.length >= 10 ? raw.slice(5, 10) : '--'
}

const formatRecordWeekday = (value) => {
  const date = toDateObject(value)
  return WEEK_LABELS[date.getDay()]
}

const chooseDate = (value) => {
  form.recordDate = value
  closeSheets()
}

const openPlanSheet = () => {
  planSheetVisible.value = true
}

const openActionSheet = () => {
  if (!planItems.value.length) {
    uni.showToast({ title: '当前计划暂无动作', icon: 'none' })
    return
  }
  actionSheetVisible.value = true
}

const openDateSheet = () => {
  dateSheetVisible.value = true
}

const closeSheets = () => {
  planSheetVisible.value = false
  actionSheetVisible.value = false
  dateSheetVisible.value = false
}

const goPlans = () => {
  closeSheets()
  uni.switchTab({ url: '/pages/plans/index' })
}

const goBackHome = () => {
  const pages = getCurrentPages()
  if (pages.length > 1) {
    uni.navigateBack()
    return
  }
  uni.switchTab({ url: '/pages/home/index' })
}

onShow(() => {
  if (ensureLogin()) {
    loadPlans()
    loadStats()
    loadRecords()
  }
})
</script>

<style scoped lang="scss">
.training-page {
  position: relative;
  min-height: 100vh;
  padding: calc(var(--status-bar-height) + 28rpx) 28rpx 56rpx;
  overflow-x: hidden;
  background:
    radial-gradient(circle at 0% 3%, rgba(139, 99, 255, 0.18), transparent 30%),
    radial-gradient(circle at 100% 4%, rgba(255, 128, 111, 0.2), transparent 28%),
    linear-gradient(180deg, #fff6f8 0%, #fffaf4 46%, #fffdf9 100%);
  color: #24104f;
}

.page-bg {
  position: fixed;
  width: 280rpx;
  height: 280rpx;
  border-radius: 50%;
  filter: blur(20rpx);
  opacity: 0.36;
  pointer-events: none;
}

.page-bg--left {
  left: -150rpx;
  top: 130rpx;
  background: #d8c6ff;
}

.page-bg--right {
  right: -150rpx;
  top: 40rpx;
  background: #ffcfc8;
}

.topbar,
.hero,
.weekly-card,
.check-section,
.records-section {
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

.topbar-title {
  color: #24104f;
  font-size: 34rpx;
  font-weight: 900;
  text-align: center;
}

.hero {
  min-height: 300rpx;
  margin-top: 22rpx;
  overflow: hidden;
  border-radius: 0 0 42rpx 42rpx;
}

.hero-copy {
  position: relative;
  z-index: 8;
  padding-top: 52rpx;
  max-width: 520rpx;
}

.hero-title {
  display: block;
  color: #24104f;
  font-size: 43rpx;
  font-weight: 950;
  line-height: 1.22;
  letter-spacing: -1rpx;
}

.hero-sub {
  display: block;
  margin-top: 18rpx;
  color: #655086;
  font-size: 23rpx;
  font-weight: 750;
  line-height: 1.48;
}

.hero-cloud {
  position: absolute;
  z-index: 3;
  height: 17rpx;
  border-radius: 999rpx;
  background: rgba(255, 255, 255, 0.76);
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
  right: 110rpx;
  top: 114rpx;
  width: 76rpx;
}

.hero-cloud--one::before {
  left: 9rpx;
  width: 34rpx;
  height: 34rpx;
}

.hero-cloud--one::after {
  right: 7rpx;
  width: 44rpx;
  height: 44rpx;
}

.hero-cloud--two {
  right: 286rpx;
  top: 174rpx;
  width: 56rpx;
}

.hero-cloud--two::before {
  left: 8rpx;
  width: 28rpx;
  height: 28rpx;
}

.hero-cloud--two::after {
  right: 5rpx;
  width: 35rpx;
  height: 35rpx;
}

.hero-hill {
  position: absolute;
  left: -95rpx;
  right: -95rpx;
  border-radius: 52% 52% 0 0;
}

.hero-hill--back {
  z-index: 1;
  bottom: 40rpx;
  height: 118rpx;
  background: linear-gradient(120deg, #a78bff 0%, #e7dcff 48%, #8a6fe6 100%);
  transform: rotate(5deg);
}

.hero-hill--front {
  z-index: 2;
  right: -240rpx;
  bottom: 0;
  height: 132rpx;
  background: linear-gradient(130deg, #fff0c8 0%, #ffc27e 36%, #8062d7 80%);
  transform: rotate(-9deg);
}

.hero-trail {
  position: absolute;
  z-index: 4;
  right: 112rpx;
  bottom: 76rpx;
  width: 296rpx;
  height: 54rpx;
  border-radius: 50%;
  background: linear-gradient(90deg, rgba(255, 245, 190, 0.94), rgba(255, 149, 118, 0.64));
  transform: rotate(-13deg);
}

.coach-figure {
  position: absolute;
  z-index: 7;
  right: 20rpx;
  bottom: 52rpx;
  width: 210rpx;
  height: 230rpx;
}

.coach-head {
  position: absolute;
  left: 78rpx;
  top: 28rpx;
  width: 54rpx;
  height: 58rpx;
  border: 4rpx solid #34205f;
  border-radius: 45% 45% 48% 48%;
  background: #ffd1b8;
}

.coach-hair {
  position: absolute;
  left: 102rpx;
  top: 16rpx;
  width: 96rpx;
  height: 72rpx;
  border: 4rpx solid #34205f;
  border-left: 0;
  border-radius: 20% 80% 72% 26%;
  background: #3b246d;
}

.coach-body {
  position: absolute;
  left: 62rpx;
  top: 88rpx;
  width: 86rpx;
  height: 68rpx;
  border: 4rpx solid #34205f;
  border-radius: 24rpx 24rpx 16rpx 16rpx;
  background: #a78bff;
}

.coach-arm {
  position: absolute;
  top: 102rpx;
  width: 48rpx;
  height: 12rpx;
  border: 4rpx solid #34205f;
  border-radius: 999rpx;
  background: #ffd1b8;
}

.coach-arm--left {
  left: 28rpx;
  transform: rotate(22deg);
}

.coach-arm--right {
  left: 134rpx;
  transform: rotate(-18deg);
}

.coach-board {
  position: absolute;
  left: 20rpx;
  top: 70rpx;
  width: 52rpx;
  height: 68rpx;
  border: 4rpx solid #34205f;
  border-radius: 10rpx;
  background: rgba(255, 255, 255, 0.86);
  transform: rotate(-8deg);
}

.coach-board::after {
  content: "";
  position: absolute;
  left: 15rpx;
  top: 22rpx;
  width: 20rpx;
  height: 20rpx;
  border-right: 5rpx solid #ff806f;
  border-bottom: 5rpx solid #ff806f;
  transform: rotate(38deg);
}

.coach-leg {
  position: absolute;
  top: 152rpx;
  width: 52rpx;
  height: 15rpx;
  border: 4rpx solid #34205f;
  border-radius: 999rpx;
  background: #3b246d;
}

.coach-leg--left {
  left: 60rpx;
  transform: rotate(78deg);
}

.coach-leg--right {
  left: 112rpx;
  transform: rotate(100deg);
}

.weekly-card,
.form-card,
.preview-card,
.records-section {
  border: 2rpx solid rgba(120, 86, 170, 0.16);
  background: rgba(255, 255, 255, 0.95);
  box-shadow: 0 12rpx 0 rgba(52, 32, 95, 0.035), 0 20rpx 40rpx rgba(52, 32, 95, 0.08);
}

.weekly-card {
  margin-top: -8rpx;
  padding: 26rpx;
  border-radius: 30rpx;
}

.card-head,
.preview-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18rpx;
}

.section-title,
.section-title-sm {
  color: #24104f;
  font-weight: 950;
  line-height: 1.2;
}

.section-title {
  font-size: 34rpx;
}

.section-title-sm {
  font-size: 29rpx;
}

.section-title--standalone {
  display: block;
  margin-bottom: 18rpx;
}

.ghost-link {
  display: inline-flex;
  align-items: center;
  gap: 8rpx;
  color: #7b57f2;
  font-size: 23rpx;
  font-weight: 900;
}

.tiny-icon {
  width: 20rpx;
  height: 20rpx;
}

.metric-row {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 18rpx;
  margin-top: 24rpx;
}

.metric-card {
  min-height: 190rpx;
  padding: 22rpx 18rpx;
  border-radius: 22rpx;
}

.metric-card--purple {
  background: linear-gradient(135deg, #f0e9ff 0%, #f5efff 100%);
}

.metric-card--rose {
  background: linear-gradient(135deg, #fff0f5 0%, #fdebef 100%);
}

.metric-card--gold {
  background: linear-gradient(135deg, #fff5d9 0%, #fffaf0 100%);
}

.metric-icon {
  display: grid;
  width: 54rpx;
  height: 54rpx;
  place-items: center;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.68);
}

.metric-icon image {
  width: 30rpx;
  height: 30rpx;
}

.metric-label,
.metric-note {
  display: block;
}

.metric-label {
  margin-top: 12rpx;
  color: #423068;
  font-size: 22rpx;
  font-weight: 900;
}

.metric-value {
  display: flex;
  align-items: baseline;
  gap: 5rpx;
  margin-top: 22rpx;
  color: #24104f;
  white-space: nowrap;
}

.metric-value text:first-child {
  font-size: 43rpx;
  font-weight: 950;
}

.metric-value text:last-child {
  font-size: 21rpx;
  font-weight: 800;
}

.metric-note {
  margin-top: 18rpx;
  color: #5f4a85;
  font-size: 22rpx;
  font-weight: 800;
}

.check-section {
  margin-top: 34rpx;
}

.check-grid {
  display: grid;
  grid-template-columns: 1.05fr 0.95fr;
  gap: 24rpx;
}

.form-card,
.preview-card {
  padding: 24rpx;
  border-radius: 28rpx;
}

.field-row {
  display: grid;
  grid-template-columns: 142rpx minmax(0, 1fr);
  gap: 14rpx;
  align-items: center;
  min-height: 70rpx;
  margin-bottom: 18rpx;
}

.field-label {
  color: #24104f;
  font-size: 24rpx;
  font-weight: 900;
}

.select-pill,
.input-pill {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10rpx;
  min-height: 64rpx;
  min-width: 0;
  padding: 0 18rpx;
  border: 2rpx solid rgba(120, 86, 170, 0.16);
  border-radius: 999rpx;
  background: #fff;
  color: #24104f;
  font-size: 23rpx;
  font-weight: 850;
}

.select-pill text {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.field-icon {
  width: 22rpx;
  height: 22rpx;
  flex: 0 0 auto;
}

.field-icon--left {
  opacity: 0.72;
}

.mini-input {
  flex: 1;
  min-width: 0;
  height: 64rpx;
  color: #24104f;
  font-size: 24rpx;
  font-weight: 900;
}

.input-pill text {
  color: #7b6f98;
  font-size: 22rpx;
  font-weight: 800;
}

.mood-row {
  display: grid;
  grid-template-columns: 142rpx minmax(0, 1fr);
  gap: 14rpx;
  align-items: center;
  margin-top: 6rpx;
}

.mood-list {
  display: flex;
  justify-content: space-between;
  gap: 8rpx;
}

.mood-button {
  display: grid;
  width: 52rpx;
  height: 52rpx;
  place-items: center;
  border-radius: 50%;
  background: transparent;
}

.mood-button.active {
  background: #fff0c8;
  box-shadow: 0 8rpx 18rpx rgba(255, 173, 67, 0.22);
}

.mood-icon {
  width: 36rpx;
  height: 36rpx;
}

.textarea-wrap {
  position: relative;
  margin-top: 20rpx;
}

.feeling-textarea {
  width: 100%;
  min-height: 148rpx;
  padding: 22rpx 22rpx 44rpx;
  border-radius: 22rpx;
  background: linear-gradient(135deg, #f8f3ff 0%, #fff7fb 100%);
  color: #24104f;
  font-size: 24rpx;
  line-height: 1.5;
}

.textarea-count {
  position: absolute;
  right: 22rpx;
  bottom: 16rpx;
  color: #8a7aa9;
  font-size: 22rpx;
}

.submit-button {
  display: grid;
  min-height: 72rpx;
  margin-top: 22rpx;
  place-items: center;
  border-radius: 22rpx;
  background: linear-gradient(110deg, #8b63ff 0%, #d987d5 52%, #ff806f 100%);
  color: #fff;
  font-size: 29rpx;
  font-weight: 950;
  box-shadow: 0 8rpx 0 rgba(52, 32, 95, 0.12);
}

.submit-button[disabled] {
  opacity: 0.54;
  box-shadow: none;
}

.refresh-mini {
  display: grid;
  width: 48rpx;
  height: 48rpx;
  place-items: center;
}

.refresh-mini image {
  width: 28rpx;
  height: 28rpx;
}

.action-list {
  display: flex;
  flex-direction: column;
  gap: 16rpx;
  margin-top: 22rpx;
}

.action-card {
  display: grid;
  grid-template-columns: 76rpx minmax(0, 1fr) 54rpx;
  gap: 14rpx;
  align-items: center;
  padding: 12rpx 0;
  border-bottom: 2rpx solid rgba(52, 32, 95, 0.08);
}

.action-card.active .action-title {
  color: #7b57f2;
}

.action-avatar {
  display: grid;
  width: 70rpx;
  height: 70rpx;
  place-items: center;
  border-radius: 50%;
  background: #f0e7ff;
}

.action-avatar image {
  width: 36rpx;
  height: 36rpx;
}

.action-copy {
  min-width: 0;
}

.action-title,
.action-meta {
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.action-title {
  color: #24104f;
  font-size: 24rpx;
  font-weight: 950;
}

.action-meta {
  margin-top: 6rpx;
  color: #7b6f98;
  font-size: 21rpx;
  font-weight: 750;
}

.play-button {
  display: grid;
  width: 54rpx;
  height: 54rpx;
  place-items: center;
  border: 3rpx solid #24104f;
  border-radius: 50%;
  background: #fff;
}

.play-button[disabled] {
  opacity: 0.32;
}

.play-button image {
  width: 27rpx;
  height: 27rpx;
}

.all-actions {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10rpx;
  min-height: 62rpx;
  margin-top: 18rpx;
  border-radius: 18rpx;
  background: #f4efff;
  color: #8b63ff;
  font-size: 24rpx;
  font-weight: 950;
}

.empty-mini,
.empty-record,
.empty-sheet {
  display: flex;
  align-items: center;
  flex-direction: column;
  justify-content: center;
  gap: 16rpx;
  min-height: 180rpx;
  color: #7b6f98;
  font-size: 24rpx;
  font-weight: 800;
  text-align: center;
}

.empty-icon {
  width: 56rpx;
  height: 56rpx;
}

.records-section {
  margin-top: 32rpx;
  padding: 26rpx;
  border-radius: 30rpx;
}

.record-timeline {
  display: flex;
  flex-direction: column;
  gap: 18rpx;
  margin-top: 24rpx;
}

.record-row {
  display: grid;
  grid-template-columns: 80rpx 36rpx minmax(0, 1fr);
  gap: 14rpx;
  align-items: stretch;
}

.record-date {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: flex-start;
  padding-top: 10rpx;
  color: #24104f;
  font-size: 22rpx;
  font-weight: 900;
}

.record-date text:last-child {
  margin-top: 8rpx;
  color: #5f4a85;
}

.record-line {
  position: relative;
  display: flex;
  justify-content: center;
}

.record-line::after {
  content: "";
  position: absolute;
  top: 54rpx;
  bottom: -18rpx;
  width: 4rpx;
  border-radius: 999rpx;
  background: #dfd1f5;
}

.record-row:last-child .record-line::after {
  display: none;
}

.record-dot {
  position: relative;
  z-index: 1;
  display: grid;
  width: 44rpx;
  height: 44rpx;
  place-items: center;
  border-radius: 50%;
  background: #45c977;
  box-shadow: 0 0 0 6rpx rgba(69, 201, 119, 0.14);
}

.record-dot.muted {
  border: 4rpx solid #b7a3d5;
  background: #fff;
  box-shadow: none;
}

.record-dot image {
  width: 22rpx;
  height: 22rpx;
}

.record-card {
  display: grid;
  grid-template-columns: 72rpx minmax(0, 1fr) 120rpx;
  gap: 14rpx;
  align-items: center;
  min-height: 104rpx;
  padding: 18rpx;
  border-radius: 24rpx;
  background: #fff;
  box-shadow: 0 8rpx 26rpx rgba(52, 32, 95, 0.06);
}

.record-icon {
  display: grid;
  width: 64rpx;
  height: 64rpx;
  place-items: center;
  border-radius: 50%;
  background: #f0e7ff;
}

.record-icon image {
  width: 34rpx;
  height: 34rpx;
}

.record-main {
  min-width: 0;
}

.record-title {
  display: block;
  overflow: hidden;
  color: #24104f;
  font-size: 25rpx;
  font-weight: 950;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.record-chips {
  display: flex;
  flex-wrap: wrap;
  gap: 8rpx;
  margin-top: 10rpx;
}

.record-chips text {
  padding: 4rpx 14rpx;
  border-radius: 999rpx;
  background: #eee6ff;
  color: #8b63ff;
  font-size: 20rpx;
  font-weight: 900;
}

.record-status {
  color: #20a96c;
  font-size: 24rpx;
  font-weight: 950;
  text-align: right;
}

.sheet-mask {
  position: fixed;
  inset: 0;
  z-index: 80;
  display: flex;
  align-items: flex-end;
  padding: 26rpx;
  background: rgba(36, 16, 79, 0.38);
}

.bottom-sheet {
  width: 100%;
  max-height: 76vh;
  padding: 16rpx 22rpx calc(26rpx + env(safe-area-inset-bottom));
  overflow: auto;
  border-radius: 34rpx 34rpx 26rpx 26rpx;
  background: #fffdf8;
  box-shadow: 0 -22rpx 60rpx rgba(36, 16, 79, 0.2);
}

.sheet-handle {
  width: 76rpx;
  height: 8rpx;
  margin: 0 auto 18rpx;
  border-radius: 999rpx;
  background: #d9ccef;
}

.sheet-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 18rpx;
}

.sheet-title,
.sheet-sub {
  display: block;
}

.sheet-title {
  color: #24104f;
  font-size: 31rpx;
  font-weight: 950;
}

.sheet-sub {
  margin-top: 8rpx;
  color: #7b6f98;
  font-size: 22rpx;
  font-weight: 750;
}

.sheet-close {
  display: grid;
  width: 58rpx;
  height: 58rpx;
  place-items: center;
  border-radius: 50%;
  background: #f4f0ff;
}

.sheet-close image {
  width: 34rpx;
  height: 34rpx;
}

.sheet-list,
.date-list {
  display: flex;
  flex-direction: column;
  gap: 14rpx;
  margin-top: 24rpx;
}

.sheet-option {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14rpx;
  min-height: 92rpx;
  padding: 18rpx;
  border: 2rpx solid rgba(52, 32, 95, 0.12);
  border-radius: 22rpx;
  background: #fff;
  text-align: left;
}

.sheet-option.active {
  border-color: #8b63ff;
  background: #f6f1ff;
}

.sheet-option text {
  display: block;
}

.sheet-option text:first-child {
  color: #24104f;
  font-size: 25rpx;
  font-weight: 950;
}

.sheet-option text:last-child {
  margin-top: 8rpx;
  color: #7b6f98;
  font-size: 21rpx;
  font-weight: 750;
}

.sheet-option-icon {
  width: 38rpx;
  height: 38rpx;
}

.sheet-primary {
  min-width: 220rpx;
  min-height: 62rpx;
  border-radius: 999rpx;
  background: #8b63ff;
  color: #fff;
  font-size: 24rpx;
  font-weight: 900;
}

.date-list {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
}

.date-option {
  display: flex;
  align-items: center;
  flex-direction: column;
  justify-content: center;
  gap: 8rpx;
  min-height: 120rpx;
  border: 2rpx solid rgba(52, 32, 95, 0.12);
  border-radius: 22rpx;
  background: #fff;
  color: #7b6f98;
  font-size: 21rpx;
  font-weight: 850;
}

.date-option text:first-child {
  color: #24104f;
  font-size: 24rpx;
  font-weight: 950;
}

.date-option.active {
  border-color: #8b63ff;
  background: linear-gradient(135deg, #8b63ff, #ff806f);
  color: #fff;
}

.date-option.active text {
  color: #fff;
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
  .metric-row,
  .check-grid {
    grid-template-columns: 1fr;
  }

  .hero-title {
    font-size: 38rpx;
  }

  .coach-figure {
    opacity: 0.46;
  }

  .record-card {
    grid-template-columns: 58rpx minmax(0, 1fr);
  }

  .record-status {
    grid-column: 2;
    text-align: left;
  }
}
</style>
