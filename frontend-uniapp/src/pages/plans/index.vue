<template>
  <view class="plan-page">
    <view class="page-bg page-bg--left"></view>
    <view class="page-bg page-bg--right"></view>

    <view class="topbar">
      <button class="icon-button" aria-label="返回" @click="goBackHome">
        <image class="top-icon" :src="icons.back" mode="aspectFit" />
      </button>
      <text class="topbar-title">训练计划中心</text>
      <button class="icon-button" aria-label="刷新计划" :disabled="loading" @click="refreshPlans">
        <image class="top-icon" :class="{ spinning: loading }" :src="icons.refresh" mode="aspectFit" />
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
        <text class="eyebrow">PLAN HUB</text>
        <text class="hero-title">先选训练主线，</text>
        <text class="hero-title">再稳定推进每一天</text>
        <text class="hero-sub">我的计划只放已激活训练，发现大厅用于挑选和激活。</text>
      </view>

      <view class="hero-badges">
        <text>我的计划 {{ myPlans.length }}</text>
        <text>发现大厅 {{ libraryPlans.length }}</text>
        <text>当前动作 {{ selected?.items?.length || 0 }}</text>
      </view>
    </view>

    <view class="progress-card">
      <view class="progress-head">
        <view>
          <text class="progress-title">订阅进度</text>
          <text class="progress-note">{{ progressNote }}</text>
        </view>
        <text class="progress-percent">{{ itemProgress }}%</text>
      </view>
      <view class="progress-track">
        <view class="progress-fill" :style="{ width: `${itemProgress}%` }"></view>
      </view>
    </view>

    <view class="action-card">
      <button class="action-button action-button--ai" :disabled="aiSubmitting" @click="openAiDialog">
        <image class="action-icon" :src="icons.ai" mode="aspectFit" />
        <view class="action-copy">
          <text>{{ aiSubmitting ? 'AI 定制中...' : 'AI 一键定制' }}</text>
          <text>生成后进入你的个人训练区</text>
        </view>
      </button>
      <button class="action-button" :disabled="loading" @click="refreshPlans">
        <image class="action-icon" :src="icons.refresh" mode="aspectFit" />
        <view class="action-copy">
          <text>刷新计划</text>
          <text>同步我的计划和发现大厅</text>
        </view>
      </button>
    </view>

    <view class="tabs">
      <button
        class="tab-button"
        :class="{ active: activeTab === 'mine' }"
        @click="switchTab('mine')"
      >
        我的计划 {{ myPlans.length }}
      </button>
      <button
        class="tab-button"
        :class="{ active: activeTab === 'library' }"
        @click="switchTab('library')"
      >
        发现大厅 {{ libraryPlans.length }}
      </button>
    </view>

    <view class="section-head">
      <view>
        <text class="section-title">计划列表</text>
        <text class="section-sub">{{ activeTab === 'mine' ? '只展示你已经激活的训练主线。' : '公共计划和未激活的 AI 专属计划在这里。' }}</text>
      </view>
    </view>

    <view v-if="!visiblePlans.length && !loading" class="empty-card">
      <image class="empty-icon" :src="icons.info" mode="aspectFit" />
      <text class="empty-title">{{ activeTab === 'mine' ? '你还没有激活训练计划' : '当前没有新的可激活计划' }}</text>
      <button v-if="activeTab === 'mine'" class="empty-button" @click="switchTab('library')">去发现大厅挑选</button>
      <button v-else class="empty-button" @click="refreshPlans">刷新计划</button>
    </view>

    <view v-else class="plan-list">
      <view
        v-for="plan in pagedPlans"
        :key="plan.id"
        class="plan-card"
        :class="{ active: selected?.plan?.id === plan.id }"
        @click="selectPlan(plan)"
      >
        <view class="plan-icon-wrap" :class="plan.planType === 'AI_PRIVATE' ? 'is-ai' : 'is-public'">
          <image class="plan-icon" :src="plan.planType === 'AI_PRIVATE' ? icons.aiDark : icons.plan" mode="aspectFit" />
        </view>
        <view class="plan-main">
          <view class="plan-top">
            <text class="plan-title">{{ plan.title || `训练计划 ${plan.id}` }}</text>
            <text class="status-pill" :class="plan.subscribed ? 'status-active' : 'status-library'">
              {{ plan.subscribed ? '已激活' : plan.planType === 'AI_PRIVATE' ? 'AI 专属' : formatDifficulty(plan.level) }}
            </text>
          </view>
          <text class="plan-goal">{{ plan.goal || '按周期推进训练目标' }}</text>
          <view class="meta-row">
            <view class="meta-item">
              <image class="meta-icon" :src="icons.calendar" mode="aspectFit" />
              <text>{{ plan.subscribed ? `开始于 ${formatDateText(plan.startDate)}` : `周期 ${plan.durationWeeks || 0} 周` }}</text>
            </view>
            <view class="meta-item">
              <image class="meta-icon" :src="icons.layer" mode="aspectFit" />
              <text>{{ plan.planType === 'AI_PRIVATE' ? '你的私有计划' : '公共计划' }}</text>
            </view>
          </view>
        </view>
        <image class="card-chevron" :src="icons.right" mode="aspectFit" />
      </view>
    </view>
    <view v-if="visiblePlans.length > planPageSize" class="pagination-bar">
      <button class="page-button" :disabled="safePlanPage <= 1" @click="prevPlanPage">上一页</button>
      <text class="page-count">{{ safePlanPage }} / {{ planTotalPages }}</text>
      <button class="page-button" :disabled="safePlanPage >= planTotalPages" @click="nextPlanPage">下一页</button>
    </view>

    <view v-if="selected" class="detail-card">
      <view class="detail-head">
        <view>
          <text class="section-title">计划详情</text>
          <text class="section-sub">激活后进入我的计划，动作节点可直接查看绑定视频。</text>
        </view>
      </view>

      <view class="summary-card">
        <view class="summary-top">
          <view>
            <text class="summary-title">{{ selected.plan?.title || '训练计划' }}</text>
            <text class="summary-goal">{{ selected.plan?.goal || '暂无目标描述' }}</text>
          </view>
          <text class="summary-tag">{{ selected.planType === 'AI_PRIVATE' ? 'AI 专属' : selected.subscription ? '已激活' : formatDifficulty(selected.plan?.level) }}</text>
        </view>
        <view class="chip-row">
          <text class="chip">计划ID {{ selected.plan?.id }}</text>
          <text class="chip">周期 {{ selected.plan?.durationWeeks || 0 }} 周</text>
          <text class="chip">动作 {{ selected.items?.length || 0 }} 个</text>
        </view>
      </view>

      <view class="subscribe-card">
        <view class="date-row">
          <text class="field-label">开始日期</text>
          <view class="date-picker" @click="openDateSheet">
            <image class="date-icon" :src="icons.calendar" mode="aspectFit" />
            <text>{{ startDate || '请选择开始日期' }}</text>
          </view>
        </view>
        <view class="subscribe-tip" :class="{ active: selected.subscription }">
          <image class="tip-icon" :src="selected.subscription ? icons.check : icons.info" mode="aspectFit" />
          <text>{{ selected.subscription ? `当前订阅开始于 ${selected.subscription.startDate}` : activationHint }}</text>
        </view>
        <view class="detail-actions" :class="{ 'single-action': !selected.subscription }">
          <button
            v-if="selected.subscription"
            class="outline-danger"
            :disabled="unsubmitting"
            @click="cancelActivation"
          >
            取消激活
          </button>
          <button
            class="solid-success"
            :disabled="submitting"
            @click="subscribeSelected"
          >
            {{ selected.subscription ? '更新订阅日期' : '激活计划' }}
          </button>
        </view>
      </view>

      <view class="nodes-head">
        <text class="section-title-sm">动作节点</text>
        <text class="section-sub">按天展开动作，可查看时长、组数和绑定视频。</text>
      </view>

      <view v-if="!selected.items?.length" class="empty-node">
        <image class="empty-icon" :src="icons.list" mode="aspectFit" />
        <text>该计划还没有动作节点</text>
      </view>

      <view v-else class="node-list">
        <view v-for="item in pagedItems" :key="item.id" class="node-card">
          <view class="node-rail">
            <view class="node-dot"></view>
          </view>
          <view class="node-main">
            <view class="node-top">
              <view class="node-title-wrap">
                <text class="day-chip">DAY {{ item.dayIndex || '-' }}</text>
                <text class="node-title">{{ item.actionName || '训练动作' }}</text>
              </view>
              <text class="duration-chip">{{ item.durationMin || 0 }} 分钟</text>
            </view>
            <text class="node-meta">组数 {{ item.sets || 0 }} · 次数 {{ item.reps || 0 }} · 休息 {{ item.restSec || 0 }} 秒</text>
            <view class="node-action-row">
              <button class="video-button" :disabled="!item.video?.playUrl" @click.stop="openVideo(item)">
                <image class="video-icon" :src="item.video?.playUrl ? icons.play : icons.video" mode="aspectFit" />
                <text>{{ item.video?.playUrl ? '查看视频' : '暂无视频' }}</text>
              </button>
              <text class="video-title">{{ item.video?.title || '当前节点未绑定视频资源' }}</text>
            </view>
          </view>
        </view>
      </view>
      <view v-if="detailItems.length > itemPageSize" class="pagination-bar pagination-bar--nodes">
        <button class="page-button" :disabled="safeItemPage <= 1" @click="prevItemPage">上一页</button>
        <text class="page-count">{{ safeItemPage }} / {{ itemTotalPages }}</text>
        <button class="page-button" :disabled="safeItemPage >= itemTotalPages" @click="nextItemPage">下一页</button>
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

    <view v-if="dateSheetVisible" class="date-sheet-mask" @click="closeDateSheet">
      <view class="date-sheet" @click.stop>
        <view class="date-sheet-handle"></view>
        <view class="date-sheet-head">
          <view>
            <text class="date-sheet-title">选择开始日期</text>
            <text class="date-sheet-sub">选好日期后，再点击{{ selected?.subscription ? '更新订阅日期' : '激活计划' }}提交</text>
          </view>
          <button class="sheet-close" @click="closeDateSheet">
            <image class="close-icon" :src="icons.close" mode="aspectFit" />
          </button>
        </view>

        <view class="date-quick-row">
          <button class="quick-date" @click="chooseQuickDate(0)">今天</button>
          <button class="quick-date" @click="chooseQuickDate(1)">明天</button>
          <button class="quick-date" @click="chooseQuickDate(7)">一周后</button>
        </view>

        <view class="calendar-card">
          <view class="calendar-nav">
            <button class="month-button" @click="shiftDateMonth(-1)">
              <image class="month-icon" :src="icons.back" mode="aspectFit" />
            </button>
            <text class="calendar-title">{{ calendarTitle }}</text>
            <button class="month-button" @click="shiftDateMonth(1)">
              <image class="month-icon" :src="icons.right" mode="aspectFit" />
            </button>
          </view>
          <view class="week-grid">
            <text v-for="label in weekLabels" :key="label" class="week-label">{{ label }}</text>
          </view>
          <view class="days-grid">
            <button
              v-for="day in calendarDays"
              :key="day.value"
              class="calendar-day"
              :class="{ muted: day.muted, today: day.today, selected: day.selected }"
              @click="chooseCalendarDate(day.value)"
            >
              <text>{{ day.day }}</text>
            </button>
          </view>
        </view>
      </view>
    </view>

    <view v-if="aiDialogVisible" class="dialog-mask" @click="closeAiDialog">
      <view class="ai-dialog" @click.stop>
        <view class="dialog-head">
          <view>
            <text class="dialog-title">让 AI 为你定制专属计划</text>
            <text class="dialog-desc">描述目标、器械条件和训练频率，生成后会进入你的个人训练区。</text>
          </view>
          <button class="dialog-close" @click="closeAiDialog">
            <image class="close-icon" :src="icons.close" mode="aspectFit" />
          </button>
        </view>
        <textarea
          v-model="aiPrompt"
          class="ai-textarea"
          maxlength="300"
          :disabled="aiSubmitting"
          placeholder="例如：我是新手，想在家用哑铃减脂，每周练 3 天。"
        />
        <text class="textarea-count">{{ aiPrompt.length }}/300</text>
        <view v-if="aiSubmitting" class="ai-running">
          <view class="ai-dot"></view>
          <text>AI 教练正在生成计划，通常需要 10~20 秒...</text>
        </view>
        <view class="dialog-actions">
          <button class="dialog-secondary" @click="closeAiDialog">{{ aiSubmitting ? '先关闭' : '取消' }}</button>
          <button class="dialog-primary" :disabled="aiSubmitting" @click="submitAiPlan">
            {{ aiSubmitting ? '提交中' : '提交' }}
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
  faCircleInfo,
  faCircleXmark,
  faClipboardList,
  faHome,
  faLayerGroup,
  faListCheck,
  faPlay,
  faPlayCircle,
  faRotateRight,
  faUser,
  faVideo,
  faWandMagicSparkles
} from '@fortawesome/free-solid-svg-icons'
import { computed, ref } from 'vue'
import { onHide, onShow } from '@dcloudio/uni-app'
import {
  fetchPlanOverview,
  generateAiPlan,
  getPlanDetail,
  subscribePlan,
  unsubscribePlan
} from '../../api/modules/plan'
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
  ai: faIcon(faWandMagicSparkles, '#ffffff'),
  aiDark: faIcon(faWandMagicSparkles),
  back: faIcon(faChevronLeft),
  calendar: faIcon(faCalendarDays, '#7b6f98'),
  check: faIcon(faCircleCheck, '#20a96c'),
  close: faIcon(faCircleXmark),
  info: faIcon(faCircleInfo, '#8b63ff'),
  layer: faIcon(faLayerGroup, '#7b6f98'),
  list: faIcon(faListCheck),
  plan: faIcon(faClipboardList),
  play: faIcon(faPlay, '#ffffff'),
  refresh: faIcon(faRotateRight),
  right: faIcon(faChevronRight),
  video: faIcon(faVideo, '#8b63ff'),
  home: faIcon(faHome),
  planActive: faIcon(faClipboardList, '#8b63ff'),
  course: faIcon(faPlayCircle),
  courseActive: faIcon(faPlayCircle, '#8b63ff'),
  mine: faIcon(faUser),
  mineActive: faIcon(faUser, '#8b63ff')
}

const today = () => {
  const date = new Date()
  const local = new Date(date.getTime() - date.getTimezoneOffset() * 60000)
  return local.toISOString().slice(0, 10)
}

const toDateObject = (value) => {
  const fallback = today()
  const source = /^\d{4}-\d{2}-\d{2}$/.test(value || '') ? value : fallback
  const [year, month, day] = source.split('-').map(Number)
  return new Date(year, month - 1, day)
}

const formatDateValue = (date) => {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

const getMonthStart = (value) => {
  const date = toDateObject(value)
  return new Date(date.getFullYear(), date.getMonth(), 1)
}

const addMonths = (date, amount) => new Date(date.getFullYear(), date.getMonth() + amount, 1)

const loading = ref(false)
const submitting = ref(false)
const unsubmitting = ref(false)
const aiDialogVisible = ref(false)
const aiPrompt = ref('')
const aiSubmitting = ref(false)
const activeTab = ref('mine')
const syncingTab = ref(false)
const startDate = ref(today())
const planPageSize = 3
const itemPageSize = 2
const planPage = ref(1)
const itemPage = ref(1)
const dateSheetVisible = ref(false)
const dateDraft = ref(startDate.value)
const dateCursor = ref(getMonthStart(startDate.value))
const weekLabels = ['一', '二', '三', '四', '五', '六', '日']
const overview = ref({
  myPlans: [],
  libraryPlans: [],
  currentPlanId: null
})
const selected = ref(null)

const myPlans = computed(() => overview.value.myPlans || [])
const libraryPlans = computed(() => overview.value.libraryPlans || [])
const visiblePlans = computed(() => (activeTab.value === 'mine' ? myPlans.value : libraryPlans.value))
const planTotalPages = computed(() => Math.max(1, Math.ceil(visiblePlans.value.length / planPageSize)))
const safePlanPage = computed(() => Math.min(Math.max(planPage.value, 1), planTotalPages.value))
const pagedPlans = computed(() => {
  const start = (safePlanPage.value - 1) * planPageSize
  return visiblePlans.value.slice(start, start + planPageSize)
})
const detailItems = computed(() => selected.value?.items || [])
const itemTotalPages = computed(() => Math.max(1, Math.ceil(detailItems.value.length / itemPageSize)))
const safeItemPage = computed(() => Math.min(Math.max(itemPage.value, 1), itemTotalPages.value))
const pagedItems = computed(() => {
  const start = (safeItemPage.value - 1) * itemPageSize
  return detailItems.value.slice(start, start + itemPageSize)
})
const todayValue = computed(() => today())

const tabs = [
  { text: '首页', icon: icons.home, activeIcon: faIcon(faHome, '#8b63ff'), url: '/pages/home/index' },
  { text: '计划', icon: icons.plan, activeIcon: icons.planActive, active: true, url: '/pages/plans/index' },
  { text: '课程', icon: icons.course, activeIcon: icons.courseActive, url: '/pages/courses/index' },
  { text: '我的', icon: icons.mine, activeIcon: icons.mineActive, url: '/pages/mine/index' }
]

const calendarTitle = computed(() => {
  const date = dateCursor.value
  return `${date.getFullYear()}年${String(date.getMonth() + 1).padStart(2, '0')}月`
})

const calendarDays = computed(() => {
  const cursor = dateCursor.value
  const year = cursor.getFullYear()
  const month = cursor.getMonth()
  const firstDay = new Date(year, month, 1)
  const mondayOffset = (firstDay.getDay() + 6) % 7
  const gridStart = new Date(year, month, 1 - mondayOffset)

  return Array.from({ length: 42 }, (_, index) => {
    const date = new Date(gridStart)
    date.setDate(gridStart.getDate() + index)
    const value = formatDateValue(date)

    return {
      day: date.getDate(),
      value,
      muted: date.getMonth() !== month,
      selected: value === dateDraft.value,
      today: value === todayValue.value
    }
  })
})

const itemProgress = computed(() => {
  const total = selected.value?.items?.length || 0
  if (!total) return selected.value?.subscription ? 35 : 10
  return selected.value?.subscription ? Math.min(38 + total * 8, 100) : Math.min(18 + total * 6, 92)
})

const progressNote = computed(() => {
  if (selected.value?.subscription) return `已订阅，开始日期 ${selected.value.subscription.startDate}`
  if (activeTab.value === 'mine') return '你还没有激活计划，去发现大厅挑一个开始吧'
  return '当前计划尚未激活，设置开始日期后即可加入我的计划'
})

const activationHint = computed(() => (
  selected.value?.planType === 'AI_PRIVATE'
    ? '这是你的 AI 专属计划，设置日期后即可重新激活'
    : '这是公共计划，设置日期后即可加入我的计划'
))

const formatDifficulty = (level) => {
  switch (String(level || '').toLowerCase()) {
    case 'advanced':
      return '高阶'
    case 'intermediate':
      return '进阶'
    case 'beginner':
      return '入门'
    default:
      return '标准难度'
  }
}

const formatDateText = (value) => value || '未设置'

const normalizeOverview = (payload) => ({
  myPlans: payload?.myPlans || [],
  libraryPlans: payload?.libraryPlans || [],
  currentPlanId: payload?.currentPlanId || null
})

const findRowById = (planId) => {
  if (!planId) return null
  return myPlans.value.find((item) => item.id === planId) || libraryPlans.value.find((item) => item.id === planId) || null
}

const loadOverview = async (options = {}) => {
  const { preferredId = null, preferredTab = null } = options
  try {
    loading.value = true
    const response = await fetchPlanOverview()
    overview.value = normalizeOverview(response?.data)

    let nextTab = preferredTab || (myPlans.value.length ? 'mine' : 'library')
    if (nextTab === 'mine' && !myPlans.value.length) {
      nextTab = libraryPlans.value.length ? 'library' : 'mine'
    }
    if (nextTab === 'library' && !libraryPlans.value.length) {
      nextTab = myPlans.value.length ? 'mine' : 'library'
    }

    syncingTab.value = true
    activeTab.value = nextTab
    planPage.value = 1
    itemPage.value = 1
    const targetId = preferredId || overview.value.currentPlanId
    await syncSelectedPlan(targetId, nextTab)
  } catch (_) {
    selected.value = null
  } finally {
    syncingTab.value = false
    loading.value = false
  }
}

const syncSelectedPlan = async (preferredId = null, tab = activeTab.value) => {
  const rows = tab === 'mine' ? myPlans.value : libraryPlans.value
  if (!rows.length) {
    selected.value = null
    startDate.value = today()
    return
  }
  const currentId = selected.value?.plan?.id
  const target = rows.find((item) => item.id === preferredId) || rows.find((item) => item.id === currentId) || rows[0]
  await selectPlan(target)
}

const selectPlan = async (row) => {
  if (!row?.id) return
  try {
    const response = await getPlanDetail(row.id)
    selected.value = response?.data || null
    startDate.value = selected.value?.subscription?.startDate || today()
    itemPage.value = 1
  } catch (_) {
    selected.value = null
  }
}

const switchTab = async (tab) => {
  if (activeTab.value === tab) return
  activeTab.value = tab
  planPage.value = 1
  itemPage.value = 1
  if (!syncingTab.value) {
    await syncSelectedPlan(null, tab)
  }
}

const prevPlanPage = () => {
  planPage.value = Math.max(1, safePlanPage.value - 1)
}

const nextPlanPage = () => {
  planPage.value = Math.min(planTotalPages.value, safePlanPage.value + 1)
}

const prevItemPage = () => {
  itemPage.value = Math.max(1, safeItemPage.value - 1)
}

const nextItemPage = () => {
  itemPage.value = Math.min(itemTotalPages.value, safeItemPage.value + 1)
}

const refreshPlans = () => {
  loadOverview({
    preferredTab: activeTab.value,
    preferredId: selected.value?.plan?.id || null
  })
}

const openDateSheet = () => {
  dateDraft.value = startDate.value || today()
  dateCursor.value = getMonthStart(dateDraft.value)
  dateSheetVisible.value = true
}

const closeDateSheet = () => {
  dateSheetVisible.value = false
}

const shiftDateMonth = (amount) => {
  dateCursor.value = addMonths(dateCursor.value, amount)
}

const chooseCalendarDate = (value) => {
  dateDraft.value = value
  startDate.value = value
  dateCursor.value = getMonthStart(value)
  closeDateSheet()
}

const chooseQuickDate = (offset) => {
  const date = new Date()
  date.setDate(date.getDate() + offset)
  chooseCalendarDate(formatDateValue(date))
}

const subscribeSelected = async () => {
  if (!selected.value?.plan?.id) {
    uni.showToast({ title: '请先选择计划', icon: 'none' })
    return
  }
  if (!startDate.value) {
    uni.showToast({ title: '请选择开始日期', icon: 'none' })
    return
  }
  try {
    submitting.value = true
    await subscribePlan({
      planId: selected.value.plan.id,
      startDate: startDate.value
    })
    uni.showToast({ title: selected.value.subscription ? '订阅日期已更新' : '计划已激活', icon: 'success' })
    await loadOverview({
      preferredTab: 'mine',
      preferredId: selected.value.plan.id
    })
  } finally {
    submitting.value = false
  }
}

const cancelActivation = () => {
  if (!selected.value?.plan?.id) return
  uni.showModal({
    title: '确认取消激活？',
    content: '取消后，该计划会回到发现大厅，之后仍可再次激活。',
    confirmText: '确认取消',
    cancelText: '再想想',
    success: async (result) => {
      if (!result.confirm) return
      try {
        unsubmitting.value = true
        const planId = selected.value.plan.id
        await unsubscribePlan({ planId })
        uni.showToast({ title: '已取消激活', icon: 'success' })
        await loadOverview({
          preferredTab: 'library',
          preferredId: planId
        })
      } finally {
        unsubmitting.value = false
      }
    }
  })
}

const openAiDialog = () => {
  aiDialogVisible.value = true
}

const closeAiDialog = () => {
  aiDialogVisible.value = false
}

const submitAiPlan = async () => {
  const prompt = aiPrompt.value.trim()
  if (!prompt) {
    uni.showToast({ title: '请先描述训练需求', icon: 'none' })
    return
  }
  if (aiSubmitting.value) return

  try {
    aiSubmitting.value = true
    const response = await generateAiPlan({ userPrompt: prompt })
    const planId = response?.data?.planId || null
    aiPrompt.value = ''
    aiDialogVisible.value = false
    uni.showToast({ title: '专属计划已生成', icon: 'success' })
    await loadOverview({
      preferredTab: 'mine',
      preferredId: planId
    })
  } finally {
    aiSubmitting.value = false
  }
}

const openVideo = (item) => {
  const playUrl = item?.video?.playUrl
  if (!playUrl) {
    uni.showToast({ title: '当前节点暂无视频', icon: 'none' })
    return
  }

  const playlist = createVideoPlaylist({
    items: selected.value?.items || [],
    source: selected.value?.plan?.title || '训练计划',
    cover: selected.value?.plan?.coverUrl || '',
    fallbackTitle: '训练教学视频'
  })
  const playlistIndex = playlist.findIndex((entry) => entry.id === item.id)
  const playlistKey = saveVideoPlaylist(playlist, `plan-${selected.value?.plan?.id || 'detail'}`)

  uni.navigateTo({
    url:
      `/pages/video-player/index?playlistKey=${encodeURIComponent(playlistKey)}`
      + `&index=${Math.max(playlistIndex, 0)}`
      + `&url=${encodeURIComponent(playUrl)}`
      + `&title=${encodeURIComponent(item?.video?.title || item?.actionName || '训练教学视频')}`
      + `&cover=${encodeURIComponent(selected.value?.plan?.coverUrl || '')}`
      + `&source=${encodeURIComponent(selected.value?.plan?.title || '训练计划')}`
      + `&action=${encodeURIComponent(item?.actionName || '')}`
  })
}

const goBackHome = () => {
  const pages = getCurrentPages()
  if (pages.length > 1) {
    uni.navigateBack()
    return
  }
  uni.showTabBar()
  uni.switchTab({ url: '/pages/home/index' })
}

const goTab = (tab) => {
  if (tab.active) return
  uni.showTabBar()
  uni.switchTab({ url: tab.url })
}

onShow(() => {
  uni.hideTabBar()
  if (ensureLogin()) {
    loadOverview()
  }
})

onHide(() => {
  uni.showTabBar()
})
</script>

<style scoped lang="scss">
.plan-page {
  position: relative;
  min-height: 100vh;
  padding: calc(var(--status-bar-height) + 28rpx) 28rpx 206rpx;
  overflow-x: hidden;
  background:
    radial-gradient(circle at 0% 2%, rgba(139, 99, 255, 0.16), transparent 28%),
    radial-gradient(circle at 100% 0%, rgba(255, 128, 111, 0.2), transparent 24%),
    linear-gradient(180deg, #fff7f8 0%, #fffaf4 44%, #fffdf9 100%);
  color: #24104f;
}

.page-bg {
  position: absolute;
  width: 250rpx;
  height: 250rpx;
  border-radius: 50%;
  filter: blur(16rpx);
  opacity: 0.4;
  pointer-events: none;
}

.page-bg--left {
  left: -150rpx;
  top: 96rpx;
  background: #d8c6ff;
}

.page-bg--right {
  right: -150rpx;
  top: 18rpx;
  background: #ffd1ca;
}

.topbar,
.hero-card,
.progress-card,
.action-card,
.tabs,
.section-head,
.empty-card,
.plan-list,
.pagination-bar,
.detail-card,
.dialog-mask,
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

.topbar-title {
  color: #24104f;
  font-size: 34rpx;
  font-weight: 900;
  text-align: center;
}

.hero-card {
  min-height: 302rpx;
  margin-top: 30rpx;
  padding: 32rpx 26rpx;
  overflow: hidden;
  border-radius: 30rpx;
  background:
    radial-gradient(circle at 18% 10%, rgba(199, 174, 255, 0.55), transparent 34%),
    radial-gradient(circle at 88% 8%, rgba(255, 139, 126, 0.28), transparent 34%),
    linear-gradient(112deg, #eadcff 0%, #ffe2df 51%, #fff0c8 100%);
  box-shadow: 0 16rpx 34rpx rgba(52, 32, 95, 0.08);
}

.hero-copy {
  position: relative;
  z-index: 6;
  max-width: 500rpx;
}

.eyebrow {
  display: inline-flex;
  margin-bottom: 18rpx;
  padding: 8rpx 18rpx;
  border: 3rpx solid rgba(52, 32, 95, 0.22);
  border-radius: 999rpx;
  background: rgba(255, 255, 255, 0.72);
  color: #24104f;
  font-size: 18rpx;
  font-weight: 900;
  letter-spacing: 3rpx;
}

.hero-title {
  display: block;
  color: #24104f;
  font-size: 42rpx;
  font-weight: 900;
  line-height: 1.18;
}

.hero-sub {
  display: block;
  margin-top: 15rpx;
  color: #5f4a85;
  font-size: 23rpx;
  font-weight: 700;
  line-height: 1.55;
}

.hero-badges {
  position: absolute;
  left: 26rpx;
  right: 26rpx;
  bottom: 22rpx;
  z-index: 7;
  display: flex;
  flex-wrap: wrap;
  gap: 12rpx;
}

.hero-badges text {
  min-height: 42rpx;
  padding: 0 16rpx;
  border: 3rpx solid #34205f;
  border-radius: 999rpx;
  background: rgba(255, 255, 255, 0.82);
  color: #24104f;
  font-size: 21rpx;
  font-weight: 900;
  line-height: 36rpx;
}

.hero-cloud,
.hero-cloud::before,
.hero-cloud::after {
  position: absolute;
  border-radius: 999rpx;
  background: rgba(255, 255, 255, 0.72);
}

.hero-cloud {
  z-index: 2;
  height: 17rpx;
}

.hero-cloud::before,
.hero-cloud::after {
  content: "";
  bottom: 0;
}

.hero-cloud--one {
  right: 260rpx;
  top: 126rpx;
  width: 72rpx;
}

.hero-cloud--one::before {
  left: 13rpx;
  width: 33rpx;
  height: 33rpx;
}

.hero-cloud--one::after {
  right: 9rpx;
  width: 42rpx;
  height: 42rpx;
}

.hero-cloud--two {
  right: 110rpx;
  top: 78rpx;
  width: 58rpx;
}

.hero-cloud--two::before {
  left: 8rpx;
  width: 28rpx;
  height: 28rpx;
}

.hero-cloud--two::after {
  right: 6rpx;
  width: 34rpx;
  height: 34rpx;
}

.hero-hill {
  position: absolute;
  left: -80rpx;
  right: -90rpx;
  border-radius: 52% 52% 0 0;
}

.hero-hill--back {
  z-index: 1;
  bottom: 48rpx;
  height: 104rpx;
  background: linear-gradient(120deg, #bfa4ff 0%, #e5d9ff 48%, #8067d9 100%);
  transform: rotate(4deg);
}

.hero-hill--front {
  z-index: 3;
  right: -240rpx;
  bottom: 12rpx;
  height: 118rpx;
  background: linear-gradient(130deg, #fff0c8 0%, #ffbe82 35%, #8062d7 80%);
  transform: rotate(-8deg);
}

.hero-trail {
  position: absolute;
  z-index: 4;
  right: 104rpx;
  bottom: 78rpx;
  width: 286rpx;
  height: 48rpx;
  border-radius: 50%;
  background: linear-gradient(90deg, rgba(255, 245, 190, 0.95), rgba(255, 149, 118, 0.68));
  transform: rotate(-13deg);
}

.hero-tree {
  position: absolute;
  z-index: 5;
}

.tree-crown {
  border: 4rpx solid #5b3aa6;
  border-radius: 48% 52% 45% 55%;
}

.tree-trunk {
  width: 7rpx;
  margin: -4rpx auto 0;
  border-radius: 999rpx;
  background: #5b3aa6;
}

.hero-tree--one {
  right: 25rpx;
  bottom: 61rpx;
}

.hero-tree--one .tree-crown {
  width: 64rpx;
  height: 86rpx;
  background: #ff8e82;
}

.hero-tree--one .tree-trunk {
  height: 58rpx;
}

.hero-tree--two {
  right: 112rpx;
  bottom: 70rpx;
}

.hero-tree--two .tree-crown {
  width: 52rpx;
  height: 66rpx;
  background: #c58be4;
}

.hero-tree--two .tree-trunk {
  height: 44rpx;
}

.hero-tree--three {
  right: 210rpx;
  bottom: 82rpx;
}

.hero-tree--three .tree-crown {
  width: 36rpx;
  height: 46rpx;
  background: #ff9a84;
}

.hero-tree--three .tree-trunk {
  height: 28rpx;
}

.progress-card,
.action-card,
.empty-card,
.plan-card,
.detail-card {
  border: 2rpx solid rgba(120, 86, 170, 0.16);
  background: rgba(255, 255, 255, 0.94);
  box-shadow: 0 9rpx 0 rgba(52, 32, 95, 0.035), 0 18rpx 34rpx rgba(52, 32, 95, 0.06);
}

.progress-card {
  margin-top: 22rpx;
  padding: 22rpx;
  border-radius: 26rpx;
}

.progress-head {
  display: flex;
  justify-content: space-between;
  gap: 20rpx;
}

.progress-title,
.progress-note {
  display: block;
}

.progress-title {
  color: #24104f;
  font-size: 27rpx;
  font-weight: 900;
}

.progress-note {
  margin-top: 7rpx;
  color: #7b6f98;
  font-size: 22rpx;
  font-weight: 700;
}

.progress-percent {
  color: #6b4eea;
  font-size: 26rpx;
  font-weight: 900;
}

.progress-track {
  height: 12rpx;
  margin-top: 18rpx;
  overflow: hidden;
  border-radius: 999rpx;
  background: #eee7f7;
}

.progress-fill {
  height: 100%;
  border-radius: inherit;
  background: linear-gradient(90deg, #8b63ff, #6b4eea);
}

.action-card {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 14rpx;
  margin-top: 20rpx;
  padding: 14rpx;
  border-radius: 26rpx;
}

.action-button {
  display: grid;
  grid-template-columns: 52rpx minmax(0, 1fr);
  gap: 12rpx;
  align-items: center;
  min-height: 88rpx;
  padding: 14rpx;
  border-radius: 22rpx;
  background: #fffaf4;
  text-align: left;
}

.action-button--ai {
  background: linear-gradient(135deg, #8b63ff 0%, #ff806f 100%);
  color: #fff;
}

.action-icon {
  width: 40rpx;
  height: 40rpx;
}

.action-copy text {
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.action-copy text:first-child {
  font-size: 24rpx;
  font-weight: 900;
}

.action-copy text:last-child {
  margin-top: 6rpx;
  color: inherit;
  font-size: 18rpx;
  opacity: 0.72;
}

.tabs {
  display: grid;
  grid-template-columns: 1fr 1fr;
  margin-top: 28rpx;
  align-items: end;
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

.section-head {
  margin-top: 28rpx;
}

.section-title,
.section-sub,
.section-title-sm {
  display: block;
}

.section-title {
  color: #24104f;
  font-size: 34rpx;
  font-weight: 900;
}

.section-title-sm {
  color: #24104f;
  font-size: 28rpx;
  font-weight: 900;
}

.section-sub {
  margin-top: 8rpx;
  color: #7b6f98;
  font-size: 23rpx;
  line-height: 1.5;
}

.empty-card {
  display: flex;
  align-items: center;
  flex-direction: column;
  margin-top: 22rpx;
  padding: 42rpx 26rpx;
  border-radius: 28rpx;
}

.empty-icon {
  width: 56rpx;
  height: 56rpx;
}

.empty-title {
  margin-top: 14rpx;
  color: #24104f;
  font-size: 27rpx;
  font-weight: 900;
}

.empty-button {
  min-width: 190rpx;
  height: 56rpx;
  margin-top: 20rpx;
  border-radius: 999rpx;
  background: #8b63ff;
  color: #fff;
  font-size: 24rpx;
  font-weight: 900;
}

.plan-list {
  display: flex;
  flex-direction: column;
  gap: 18rpx;
  margin-top: 22rpx;
}

.pagination-bar {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 18rpx;
  margin-top: 18rpx;
}

.pagination-bar--nodes {
  margin-top: 20rpx;
}

.page-button {
  min-width: 132rpx;
  min-height: 54rpx;
  border: 2rpx solid rgba(52, 32, 95, 0.24);
  border-radius: 999rpx;
  background: #ffffff;
  color: #24104f;
  font-size: 22rpx;
  font-weight: 900;
  box-shadow: 0 6rpx 0 rgba(52, 32, 95, 0.06);
}

.page-button[disabled] {
  opacity: 0.42;
  box-shadow: none;
}

.page-count {
  min-width: 92rpx;
  color: #6b4eea;
  font-size: 23rpx;
  font-weight: 900;
  text-align: center;
}

.plan-card {
  display: grid;
  grid-template-columns: 82rpx minmax(0, 1fr) 28rpx;
  gap: 18rpx;
  align-items: center;
  min-height: 166rpx;
  padding: 24rpx 22rpx;
  border-radius: 28rpx;
}

.plan-card.active {
  border-color: rgba(139, 99, 255, 0.76);
  background: linear-gradient(180deg, #fbf8ff 0%, #ffffff 100%);
}

.plan-icon-wrap {
  display: grid;
  width: 78rpx;
  height: 78rpx;
  place-items: center;
  border-radius: 50%;
}

.plan-icon-wrap.is-ai {
  background: linear-gradient(135deg, #8b63ff, #d7c8ff);
}

.plan-icon-wrap.is-public {
  background: #eee6ff;
}

.plan-icon {
  width: 42rpx;
  height: 42rpx;
}

.plan-main {
  min-width: 0;
}

.plan-top {
  display: flex;
  align-items: flex-start;
  gap: 12rpx;
}

.plan-title {
  flex: 1;
  overflow: hidden;
  color: #24104f;
  font-size: 28rpx;
  font-weight: 900;
  line-height: 1.25;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.status-pill,
.summary-tag {
  flex: 0 0 auto;
  padding: 8rpx 14rpx;
  border-radius: 999rpx;
  font-size: 20rpx;
  font-weight: 900;
}

.status-active {
  background: #dcf7e7;
  color: #20a96c;
}

.status-library {
  background: #eee6ff;
  color: #7b57f2;
}

.plan-goal {
  display: block;
  margin-top: 10rpx;
  overflow: hidden;
  color: #6d5a87;
  font-size: 22rpx;
  font-weight: 700;
  line-height: 1.45;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.meta-row {
  display: flex;
  flex-wrap: wrap;
  gap: 14rpx;
  margin-top: 12rpx;
}

.meta-item {
  display: flex;
  align-items: center;
  gap: 7rpx;
  color: #7b6f98;
  font-size: 21rpx;
  font-weight: 800;
}

.meta-icon {
  width: 22rpx;
  height: 22rpx;
}

.card-chevron {
  width: 24rpx;
  height: 24rpx;
}

.detail-card {
  margin-top: 26rpx;
  padding: 24rpx;
  border-radius: 30rpx;
}

.summary-card,
.subscribe-card,
.node-card {
  border: 2rpx solid rgba(120, 86, 170, 0.16);
  border-radius: 24rpx;
  background: #fffdf8;
}

.summary-card {
  margin-top: 20rpx;
  padding: 22rpx;
}

.summary-top {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16rpx;
}

.summary-title,
.summary-goal {
  display: block;
}

.summary-title {
  color: #24104f;
  font-size: 30rpx;
  font-weight: 900;
  line-height: 1.25;
}

.summary-goal {
  margin-top: 10rpx;
  color: #6d5a87;
  font-size: 23rpx;
  line-height: 1.5;
}

.summary-tag {
  background: #eee6ff;
  color: #7b57f2;
}

.chip-row {
  display: flex;
  flex-wrap: wrap;
  gap: 12rpx;
  margin-top: 18rpx;
}

.chip {
  padding: 8rpx 16rpx;
  border: 2rpx solid rgba(52, 32, 95, 0.22);
  border-radius: 999rpx;
  color: #24104f;
  font-size: 21rpx;
  font-weight: 900;
}

.subscribe-card {
  margin-top: 18rpx;
  padding: 22rpx;
}

.field-label {
  display: block;
  color: #24104f;
  font-size: 24rpx;
  font-weight: 900;
}

.date-picker {
  display: flex;
  align-items: center;
  gap: 10rpx;
  min-height: 62rpx;
  margin-top: 12rpx;
  padding: 0 18rpx;
  border: 3rpx solid #34205f;
  border-radius: 18rpx;
  background: #fff;
  color: #24104f;
  font-size: 24rpx;
  font-weight: 800;
  cursor: pointer;
}

.date-picker:active {
  transform: translateY(1rpx);
  background: #fbf8ff;
}

.date-icon,
.tip-icon {
  width: 24rpx;
  height: 24rpx;
}

.subscribe-tip {
  display: flex;
  align-items: flex-start;
  gap: 10rpx;
  margin-top: 18rpx;
  padding: 18rpx;
  border-radius: 18rpx;
  background: #f4f0ff;
  color: #6d5a87;
  font-size: 23rpx;
  font-weight: 800;
  line-height: 1.45;
}

.subscribe-tip.active {
  background: #edf8ee;
  color: #20a96c;
}

.detail-actions {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 14rpx;
  margin-top: 18rpx;
}

.detail-actions.single-action {
  grid-template-columns: 1fr;
}

.outline-danger,
.solid-success {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 66rpx;
  border-radius: 999rpx;
  font-size: 25rpx;
  font-weight: 900;
  line-height: 1;
  text-align: center;
}

.outline-danger {
  border: 3rpx solid #e95e7f;
  background: #fff0f5;
  color: #c84264;
}

.solid-success {
  background: #8fe3cb;
  color: #24104f;
}

.nodes-head {
  margin-top: 24rpx;
}

.empty-node {
  display: flex;
  align-items: center;
  gap: 14rpx;
  margin-top: 16rpx;
  padding: 28rpx;
  border-radius: 22rpx;
  background: #f7f1ff;
  color: #7b6f98;
  font-size: 24rpx;
  font-weight: 800;
}

.node-list {
  display: flex;
  flex-direction: column;
  gap: 16rpx;
  margin-top: 18rpx;
}

.node-card {
  display: grid;
  grid-template-columns: 28rpx minmax(0, 1fr);
  gap: 14rpx;
  padding: 20rpx;
}

.node-rail {
  display: flex;
  justify-content: center;
  padding-top: 10rpx;
}

.node-dot {
  width: 16rpx;
  height: 16rpx;
  border: 5rpx solid #d8c6ff;
  border-radius: 50%;
  background: #8b63ff;
}

.node-top {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12rpx;
}

.node-title-wrap {
  display: flex;
  align-items: center;
  gap: 10rpx;
  min-width: 0;
}

.day-chip,
.duration-chip {
  flex: 0 0 auto;
  padding: 7rpx 13rpx;
  border-radius: 999rpx;
  font-size: 20rpx;
  font-weight: 900;
}

.day-chip {
  background: #eee6ff;
  color: #7b57f2;
}

.duration-chip {
  border: 2rpx solid #34205f;
  color: #24104f;
}

.node-title {
  overflow: hidden;
  color: #24104f;
  font-size: 25rpx;
  font-weight: 900;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.node-meta {
  display: block;
  margin-top: 12rpx;
  color: #7b6f98;
  font-size: 22rpx;
  font-weight: 700;
}

.node-action-row {
  display: flex;
  align-items: center;
  gap: 14rpx;
  margin-top: 16rpx;
}

.video-button {
  display: flex;
  align-items: center;
  gap: 8rpx;
  min-width: 138rpx;
  height: 54rpx;
  padding: 0 18rpx;
  border-radius: 999rpx;
  background: #6b5cff;
  color: #fff;
  font-size: 22rpx;
  font-weight: 900;
}

.video-button[disabled] {
  background: #eee6ff;
  color: #7b6f98;
}

.video-icon {
  width: 20rpx;
  height: 20rpx;
}

.video-title {
  flex: 1;
  min-width: 0;
  overflow: hidden;
  color: #7b6f98;
  font-size: 22rpx;
  text-overflow: ellipsis;
  white-space: nowrap;
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
  padding: 16rpx 24rpx calc(env(safe-area-inset-bottom) + 24rpx);
  border: 3rpx solid rgba(120, 86, 170, 0.34);
  border-bottom: 0;
  border-radius: 42rpx 42rpx 0 0;
  background: rgba(255, 255, 255, 0.94);
  box-shadow: 0 8rpx 0 rgba(52, 32, 95, 0.05), 0 18rpx 38rpx rgba(52, 32, 95, 0.1);
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

.date-sheet-mask {
  position: fixed;
  inset: 0;
  z-index: 120;
  display: flex;
  align-items: flex-end;
  padding: 26rpx;
  background: rgba(36, 16, 79, 0.42);
}

.date-sheet {
  width: 100%;
  padding: 16rpx 22rpx calc(26rpx + env(safe-area-inset-bottom));
  border-radius: 34rpx 34rpx 26rpx 26rpx;
  background:
    radial-gradient(circle at 12% 0%, rgba(139, 99, 255, 0.12), transparent 34%),
    radial-gradient(circle at 96% 8%, rgba(255, 128, 111, 0.14), transparent 34%),
    #fffdf8;
  box-shadow: 0 -22rpx 60rpx rgba(36, 16, 79, 0.2);
}

.date-sheet-handle {
  width: 76rpx;
  height: 8rpx;
  margin: 0 auto 18rpx;
  border-radius: 999rpx;
  background: #d9ccef;
}

.date-sheet-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 20rpx;
}

.date-sheet-title,
.date-sheet-sub {
  display: block;
}

.date-sheet-title {
  color: #24104f;
  font-size: 31rpx;
  font-weight: 900;
}

.date-sheet-sub {
  margin-top: 8rpx;
  color: #7b6f98;
  font-size: 22rpx;
  font-weight: 700;
  line-height: 1.45;
}

.sheet-close {
  display: grid;
  width: 58rpx;
  height: 58rpx;
  flex: 0 0 auto;
  place-items: center;
  border-radius: 50%;
  background: #f4f0ff;
}

.date-quick-row {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12rpx;
  margin-top: 22rpx;
}

.quick-date {
  min-height: 58rpx;
  border: 2rpx solid rgba(139, 99, 255, 0.22);
  border-radius: 999rpx;
  background: #f7f1ff;
  color: #6b4eea;
  font-size: 23rpx;
  font-weight: 900;
}

.calendar-card {
  margin-top: 18rpx;
  padding: 18rpx;
  border: 2rpx solid rgba(120, 86, 170, 0.16);
  border-radius: 24rpx;
  background: rgba(255, 255, 255, 0.78);
}

.calendar-nav {
  display: grid;
  grid-template-columns: 60rpx 1fr 60rpx;
  align-items: center;
  margin-bottom: 14rpx;
}

.month-button {
  display: grid;
  width: 60rpx;
  height: 60rpx;
  place-items: center;
  border-radius: 50%;
  background: #fff6f5;
}

.month-icon {
  width: 24rpx;
  height: 24rpx;
}

.calendar-title {
  color: #24104f;
  font-size: 28rpx;
  font-weight: 900;
  text-align: center;
}

.week-grid,
.days-grid {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
}

.week-label {
  display: block;
  min-height: 42rpx;
  color: #7b6f98;
  font-size: 20rpx;
  font-weight: 900;
  line-height: 42rpx;
  text-align: center;
}

.days-grid {
  gap: 8rpx;
}

.calendar-day {
  display: grid;
  min-width: 0;
  height: 64rpx;
  place-items: center;
  border-radius: 18rpx;
  color: #24104f;
  font-size: 24rpx;
  font-weight: 900;
}

.calendar-day.muted {
  color: #b2a8c5;
  font-weight: 800;
}

.calendar-day.today {
  background: #fff0d4;
  color: #c36b18;
}

.calendar-day.selected {
  background: linear-gradient(135deg, #8b63ff, #ff806f);
  color: #fff;
  box-shadow: 0 8rpx 16rpx rgba(139, 99, 255, 0.24);
}

.dialog-mask {
  position: fixed;
  inset: 0;
  z-index: 90;
  display: flex;
  align-items: flex-end;
  padding: 26rpx;
  background: rgba(36, 16, 79, 0.38);
}

.ai-dialog {
  width: 100%;
  padding: 28rpx;
  border-radius: 30rpx;
  background: #fffdf8;
  box-shadow: 0 -20rpx 60rpx rgba(36, 16, 79, 0.18);
}

.dialog-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 18rpx;
}

.dialog-title,
.dialog-desc,
.textarea-count {
  display: block;
}

.dialog-title {
  color: #24104f;
  font-size: 30rpx;
  font-weight: 900;
}

.dialog-desc {
  margin-top: 8rpx;
  color: #7b6f98;
  font-size: 23rpx;
  line-height: 1.5;
}

.dialog-close {
  display: grid;
  width: 56rpx;
  height: 56rpx;
  place-items: center;
}

.close-icon {
  width: 34rpx;
  height: 34rpx;
}

.ai-textarea {
  width: 100%;
  min-height: 190rpx;
  margin-top: 22rpx;
  padding: 20rpx;
  border: 3rpx solid rgba(52, 32, 95, 0.22);
  border-radius: 22rpx;
  background: #ffffff;
  color: #24104f;
  font-size: 25rpx;
  line-height: 1.55;
}

.textarea-count {
  margin-top: 8rpx;
  color: #7b6f98;
  font-size: 21rpx;
  text-align: right;
}

.ai-running {
  display: flex;
  align-items: center;
  gap: 12rpx;
  margin-top: 14rpx;
  color: #7b57f2;
  font-size: 23rpx;
  font-weight: 800;
}

.ai-dot {
  width: 16rpx;
  height: 16rpx;
  border-radius: 50%;
  background: #8b63ff;
  animation: pulse 1s ease-in-out infinite;
}

.dialog-actions {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 14rpx;
  margin-top: 24rpx;
}

.dialog-secondary,
.dialog-primary {
  min-height: 68rpx;
  border-radius: 999rpx;
  font-size: 25rpx;
  font-weight: 900;
}

.dialog-secondary {
  background: #f2edf8;
  color: #6d5a87;
}

.dialog-primary {
  background: linear-gradient(135deg, #8b63ff, #ff806f);
  color: #fff;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

@keyframes pulse {
  0%,
  100% {
    transform: scale(0.86);
    opacity: 0.6;
  }

  50% {
    transform: scale(1.08);
    opacity: 1;
  }
}

@media (max-width: 360px) {
  .hero-title {
    font-size: 38rpx;
  }

  .action-card {
    grid-template-columns: 1fr;
  }

  .plan-title {
    font-size: 26rpx;
  }

  .detail-actions {
    grid-template-columns: 1fr;
  }
}
</style>
