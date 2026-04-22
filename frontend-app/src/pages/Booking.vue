<template>
  <div class="page-stack booking-workspace">
    <section class="booking-stats-bar">
      <div class="booking-stats-copy">
        <p class="quest-kicker">Coach Booking</p>
        <h1 class="section-title">预约教练，形成稳定训练节律</h1>
        <p class="section-sub">筛选、选档、支付和评价收进一个工作台，不再一路向下滚。</p>
      </div>
      <div class="booking-stat-pills">
        <span class="tag">当前列表 {{ schedules.length }}</span>
        <span class="tag">未来可预约 {{ effectiveAvailableSchedules }}</span>
        <span class="tag">我的预约 {{ myBookings.length }}</span>
      </div>
      <el-button type="primary" @click="loadSchedules" :loading="loading">刷新可预约时间</el-button>
    </section>

    <el-row :gutter="20" class="booking-dashboard">
      <el-col :xs="24" :md="8">
        <aside class="booking-sidebar">
          <section class="workspace-card">
            <div class="toolbar">
              <div>
                <h2 class="section-title-sm">最近预约</h2>
                <p class="section-sub">支付、完结和订单详情固定在这里。</p>
              </div>
            </div>
            <el-empty v-if="!lastBooking" description="暂无最近预约" />
            <div v-else class="detail detail-card">
              <div class="detail-card__title">最近一次预约</div>
              <div class="detail-card__headline">{{ formatScheduleTime(lastBooking) }}</div>
              <div class="detail-chip-row">
                <span class="tag">预约ID {{ lastBooking.bookingId || lastBooking.id }}</span>
                <span class="tag">订单ID {{ lastBooking.orderId || "-" }}</span>
                <span class="tag">预约状态 {{ formatBookingStatus(lastBooking.bookingStatus) }}</span>
                <span class="tag">支付状态 {{ formatPayStatus(lastBooking.payStatus) }}</span>
              </div>
              <div class="detail-list">
                <div>订单号：{{ lastBooking.orderNo || "-" }}</div>
                <div>金额：{{ lastBooking.amount ?? "-" }}</div>
                <div>教练：{{ lastBooking.coachId ? `#${lastBooking.coachId}` : "-" }}</div>
              </div>
              <div class="action-row action-row--wrap">
                <el-button type="primary" size="small" :loading="paying" @click="mockPay">模拟支付</el-button>
                <el-button type="success" size="small" :loading="completing" @click="completeBooking">确认完成</el-button>
                <el-button size="small" :disabled="!lastBooking?.orderId" @click="goToOrder(lastBooking)">订单详情</el-button>
              </div>
            </div>
          </section>

          <section class="workspace-card create-card" ref="createBookingRef">
            <div class="toolbar">
              <div>
                <h2 class="section-title-sm">创建预约</h2>
                <p class="section-sub">选中右侧档期后，这里会同步更新。</p>
              </div>
            </div>
            <el-empty v-if="!selected" :description="createBookingHint" />
            <div v-else class="detail detail-card selected-schedule-card">
              <div class="detail-card__title">已选时间段</div>
              <div class="detail-card__headline">{{ formatScheduleTime(selected) }}</div>
              <div class="detail-chip-row">
                <span class="tag">时间段ID {{ selected.id }}</span>
                <span class="tag">教练 #{{ selected.coachId }}</span>
                <span class="tag">价格 {{ selected.price }}</span>
                <span class="tag">余量 {{ remainingSlots(selected) }}</span>
                <span class="tag">状态 {{ scheduleStatus(selected).text }}</span>
              </div>
              <el-button
                type="success"
                class="create-submit"
                :disabled="!selected || selectedUnavailable"
                :loading="submitting"
                @click="createBooking"
              >
                提交预约
              </el-button>
            </div>
          </section>
        </aside>
      </el-col>

      <el-col :xs="24" :md="16">
        <main class="booking-main">
          <section class="workspace-card">
            <div class="toolbar">
              <div>
                <h2 class="section-title-sm">可预约时间段筛选</h2>
                <p class="section-sub">按教练与日期筛选当前还能预约的时间段。</p>
              </div>
            </div>
            <el-form label-position="top">
              <div class="booking-filter-row">
                <el-form-item label="教练ID">
                  <el-select
                    v-model="coachId"
                    filterable
                    clearable
                    allow-create
                    default-first-option
                    placeholder="留空查看全部"
                    style="width: 200px"
                  >
                    <el-option v-for="id in coachOptions" :key="id" :label="`教练 ${id}`" :value="id" />
                  </el-select>
                </el-form-item>
                <el-form-item label="日期">
                  <el-date-picker v-model="date" type="date" placeholder="可选" value-format="YYYY-MM-DD" />
                </el-form-item>
                <el-form-item label="快速操作">
                  <div class="quick-wrap">
                    <el-button size="small" :type="coachId === null ? 'primary' : 'default'" @click="setCoach(null)">全部</el-button>
                  </div>
                </el-form-item>
                <el-form-item label="操作" class="booking-filter-action">
                  <el-button type="primary" @click="loadSchedules">查询</el-button>
                </el-form-item>
              </div>
            </el-form>
            <p class="muted">当前接口仅支持教练ID筛选，教练姓名筛选将在后续版本扩展。</p>
            <div class="detail-chip-row booking-summary-row">
              <span class="tag">未来时间段 {{ scheduleSummary.futureSchedules ?? 0 }}</span>
              <span class="tag">可预约时间段 {{ effectiveAvailableSchedules }}</span>
              <span class="tag">最近一次开放时间 {{ formatDate(scheduleSummary.lastScheduleDate) }}</span>
            </div>
            <p class="summary-hint">{{ scheduleSummaryHint }}</p>
          </section>

          <el-tabs v-model="activeBookingTab" type="border-card" class="booking-tabs">
            <el-tab-pane label="可预约档期" name="schedules">
              <div v-if="hasCoachFilter" class="coach-list-block">
                <div class="toolbar coach-list-toolbar">
                  <div>
                    <h3 class="section-title-sm">教练概览</h3>
                    <p class="section-sub">仅在指定教练后显示，避免默认页面信息过载。</p>
                  </div>
                  <span class="tag">教练 {{ coachCards.length }}</span>
                </div>
                <el-empty v-if="!loading && !coachCards.length" description="暂无可预约教练" />
                <div v-else class="coach-list">
                  <article v-for="coach in coachCards" :key="coach.id" class="coach-card">
                    <el-image :src="coach.photo" fit="cover" class="coach-card__photo">
                      <template #error>
                        <div class="coach-card__fallback">{{ coach.initial }}</div>
                      </template>
                    </el-image>
                    <div class="coach-card__body">
                      <div class="coach-card__top">
                        <div>
                          <strong class="coach-card__name">{{ coach.name }}</strong>
                          <p class="coach-card__title">{{ coach.title }}</p>
                        </div>
                        <span class="coach-card__rating">{{ coach.availableCount ? "可约" : "满员" }}</span>
                      </div>
                      <div class="coach-card__tags">
                        <el-tag v-for="tag in coach.tags" :key="tag" size="small" effect="plain">{{ tag }}</el-tag>
                      </div>
                      <div class="coach-card__meta">
                        <span>最近 {{ coach.nextTime }}</span>
                        <span>¥{{ coach.minPrice }} 起</span>
                        <span>余量 {{ coach.remaining }}</span>
                      </div>
                      <div class="coach-card__actions">
                        <el-button size="small" round plain @click="viewCoachReviews(coach)">查看评价</el-button>
                        <el-button size="small" round type="primary" :disabled="!coach.targetSchedule" @click="bookCoach(coach)">预约</el-button>
                      </div>
                    </div>
                  </article>
                </div>
              </div>

              <div class="schedule-grid-shell" v-loading="loading">
                <el-alert
                  v-if="!loading && schedules.length && !availableSchedules"
                  type="warning"
                  show-icon
                  :closable="false"
                  title="当前筛选结果中没有可预约时间段"
                  style="margin-bottom: 12px"
                />
                <el-empty v-if="!loading && !schedules.length" description="暂无可预约时间段">
                  <div class="empty-actions">
                    <el-button size="small" @click="loadSchedules">重试</el-button>
                    <el-button size="small" @click="goTo('/courses')">去课程报名</el-button>
                  </div>
                  <p class="empty-tip">{{ scheduleEmptyReason }}</p>
                </el-empty>
                <div v-else class="schedule-grid">
                  <button
                    v-for="row in schedules"
                    :key="row.id"
                    type="button"
                    class="schedule-card"
                    :class="{
                      'schedule-card--active': selected?.id === row.id,
                      'schedule-card--disabled': !isScheduleAvailable(row)
                    }"
                    :data-schedule-id="row.id"
                    @click="selectSchedule(row)"
                  >
                    <div class="schedule-card__top">
                      <span class="schedule-card__date">{{ formatDate(row.scheduleDate) }}</span>
                      <el-tag :type="scheduleStatus(row).type" size="small">{{ scheduleStatus(row).text }}</el-tag>
                    </div>
                    <strong class="schedule-card__time">{{ formatTime(row.startTime) }} - {{ formatTime(row.endTime) }}</strong>
                    <div class="schedule-card__meta">
                      <span>教练 #{{ row.coachId }}</span>
                      <span>余量 {{ remainingSlots(row) }}</span>
                      <span>¥{{ row.price }}</span>
                    </div>
                  </button>
                </div>
              </div>
            </el-tab-pane>

            <el-tab-pane label="我的预约记录" name="bookings">
              <div class="toolbar bookings-tab-toolbar">
                <div>
                  <h3 class="section-title-sm">我的预约</h3>
                  <p class="section-sub">已完成预约可直接发起评价。</p>
                </div>
                <div class="action-row">
                  <el-button size="small" @click="fillReviewFromCompleted">自动选择已完成预约</el-button>
                  <el-button type="primary" size="small" @click="loadMyBookings" :loading="myLoading">刷新</el-button>
                </div>
              </div>
              <el-empty v-if="!myBookings.length && !myLoading" description="暂无预约记录" />
              <el-table v-else :data="myBookings" v-loading="myLoading" style="width: 100%">
                <el-table-column prop="id" label="预约ID" width="90" />
                <el-table-column label="预约时间" min-width="220">
                  <template #default="{ row }">
                    <div class="booking-table__slot">
                      <strong>{{ formatScheduleTime(row) }}</strong>
                      <span class="muted">教练 {{ row.coachId ? `#${row.coachId}` : "-" }}</span>
                    </div>
                  </template>
                </el-table-column>
                <el-table-column label="预约状态" width="120">
                  <template #default="{ row }">{{ formatBookingStatus(row.bookingStatus) }}</template>
                </el-table-column>
                <el-table-column label="支付状态" width="120">
                  <template #default="{ row }">{{ formatPayStatus(row.payStatus) }}</template>
                </el-table-column>
                <el-table-column label="订单" min-width="220">
                  <template #default="{ row }">
                    <div class="booking-table__order">
                      <strong class="order-no">{{ row.orderNo || `订单ID ${row.orderId || "-"}` }}</strong>
                      <div class="booking-table__order-actions">
                        <span class="muted">金额 {{ formatAmount(row.amount) }}</span>
                        <el-button size="small" type="primary" plain :disabled="!row.orderId" @click="goToOrder(row)">
                          查看订单详情
                        </el-button>
                      </div>
                    </div>
                  </template>
                </el-table-column>
                <el-table-column label="评价" width="120">
                  <template #default="{ row }">
                    <el-button size="small" type="primary" plain :disabled="row.bookingStatus !== 'COMPLETED'" @click="openReview(row)">
                      写评价
                    </el-button>
                  </template>
                </el-table-column>
                <el-table-column label="创建时间" min-width="170">
                  <template #default="{ row }">{{ formatDateTime(row.createTime) }}</template>
                </el-table-column>
              </el-table>

              <section v-if="reviewPanelVisible" class="review-panel" ref="reviewSectionRef">
                <div class="toolbar">
                  <div>
                    <h2 class="section-title-sm">预约评价</h2>
                    <p class="section-sub">评价仅支持已完成预约。</p>
                  </div>
                </div>
                <div v-if="selectedReviewBooking" class="detail">
                  <div>预约ID：{{ selectedReviewBooking.id }}</div>
                  <div class="muted">状态：{{ formatBookingStatus(selectedReviewBooking.bookingStatus) }}</div>
                  <div class="muted">订单ID：{{ selectedReviewBooking.orderId }}</div>
                  <div class="muted">预约时间：{{ formatScheduleTime(selectedReviewBooking) }}</div>
                </div>
                <el-form :model="reviewForm" label-position="top">
                  <el-form-item label="预约ID">
                    <el-input v-model="reviewForm.bookingId" disabled />
                  </el-form-item>
                  <el-form-item label="评分">
                    <el-select v-model.number="reviewForm.score" style="width: 180px">
                      <el-option v-for="n in 5" :key="n" :label="`${n} 分`" :value="n" />
                    </el-select>
                  </el-form-item>
                  <el-form-item label="评价内容">
                    <el-input
                      v-model="reviewForm.content"
                      type="textarea"
                      :rows="4"
                      maxlength="500"
                      show-word-limit
                      placeholder="填写训练体验、动作指导、服务感受"
                    />
                  </el-form-item>
                  <div class="action-row">
                    <el-button @click="fillReviewSample">填充示例</el-button>
                    <el-button type="primary" :loading="reviewing" @click="submitReview">提交评价</el-button>
                  </div>
                </el-form>
              </section>
              <el-alert
                v-else
                type="info"
                show-icon
                :closable="false"
                title="点击已完成预约的“写评价”后，会在这里打开评价面板"
                style="margin-top: 14px"
              />
            </el-tab-pane>
          </el-tabs>
        </main>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { computed, nextTick, reactive, ref } from "vue"
import { useRouter } from "vue-router"
import { ElMessage } from "element-plus"
import { appClient } from "../api/client"

const router = useRouter()
const schedules = ref([])
const loading = ref(false)
const submitting = ref(false)
const selected = ref(null)
const lastBooking = ref(null)
const paying = ref(false)
const completing = ref(false)
const createBookingRef = ref(null)
const reviewSectionRef = ref(null)
const activeBookingTab = ref("schedules")

const myBookings = ref([])
const myLoading = ref(false)
const selectedReviewBooking = ref(null)
const reviewPanelVisible = ref(false)
const reviewing = ref(false)
const reviewForm = reactive({
  bookingId: "",
  score: 5,
  content: ""
})

const coachId = ref(null)
const date = ref("")
const scheduleSummary = ref({
  coachId: null,
  totalSchedules: 0,
  futureSchedules: 0,
  availableSchedules: 0,
  lastScheduleDate: null,
  nextScheduleDate: null
})
const STORAGE_BOOKING = "fp_last_booking"
const STORAGE_COACH_ID = "fp_last_coach_id"

const selectedUnavailable = computed(() => {
  if (!selected.value) return false
  return !isScheduleAvailable(selected.value)
})

const availableSchedules = computed(() => schedules.value.filter((row) => isScheduleAvailable(row)).length)
const effectiveAvailableSchedules = computed(() => {
  const count = Number(scheduleSummary.value?.availableSchedules)
  return Number.isFinite(count) ? count : availableSchedules.value
})
const hasCoachFilter = computed(() => coachId.value !== null && coachId.value !== undefined && String(coachId.value).trim() !== "")
const coachCards = computed(() => {
  const map = new Map()
  schedules.value.forEach((row) => {
    const id = Number(row?.coachId)
    if (!Number.isFinite(id)) return
    const persona = coachPersona(id)
    if (!map.has(id)) {
      map.set(id, {
        id,
        name: row.coachName || `教练 #${id}`,
        title: row.coachTitle || persona.title,
        tags: normalizeCoachTags(row.specialties || row.tags, persona.tags),
        photo: resolveCoachPhoto(row),
        initial: String(row.coachName || id).slice(0, 1),
        schedules: [],
        availableCount: 0,
        remaining: 0,
        minPrice: null,
        nextSchedule: null,
        targetSchedule: null
      })
    }
    const card = map.get(id)
    card.schedules.push(row)
    card.remaining += remainingSlots(row)
    const price = Number(row.price)
    if (Number.isFinite(price)) card.minPrice = card.minPrice === null ? price : Math.min(card.minPrice, price)
    if (!card.nextSchedule || scheduleTimeValue(row) < scheduleTimeValue(card.nextSchedule)) card.nextSchedule = row
    if (isScheduleAvailable(row)) {
      card.availableCount += 1
      if (!card.targetSchedule || scheduleTimeValue(row) < scheduleTimeValue(card.targetSchedule)) card.targetSchedule = row
    }
  })
  return Array.from(map.values())
    .map((card) => {
      const schedule = card.targetSchedule || card.nextSchedule
      return {
        ...card,
        nextTime: formatScheduleTime(schedule),
        minPrice: card.minPrice ?? "-"
      }
    })
    .sort((a, b) => b.availableCount - a.availableCount || scheduleTimeValue(a.targetSchedule) - scheduleTimeValue(b.targetSchedule))
})

const scheduleEmptyReason = computed(() => {
  if (date.value && effectiveAvailableSchedules.value > 0) {
    return `当前日期 ${date.value} 没有可预约时间段，清空日期后仍有 ${effectiveAvailableSchedules.value} 个未来可预约时间段。`
  }
  if (!scheduleSummary.value?.futureSchedules) {
    const lastDate = formatDate(scheduleSummary.value?.lastScheduleDate)
    return lastDate === "-"
      ? "当前系统里还没有任何教练可预约时间段，请先在管理端创建时间段。"
      : `当前系统里没有未来可预约时间段，最近一次开放时间停在 ${lastDate}。请先在管理端补充新时间段。`
  }
  if (!effectiveAvailableSchedules.value) {
    return "未来时间段虽然存在，但当前都已满员，或者你已经预约完自己还能选的时间段。"
  }
  return "当前筛选条件下没有可预约时间段，可以调整教练或日期重新查看。"
})

const scheduleSummaryHint = computed(() => {
  const nextDate = formatDate(scheduleSummary.value?.nextScheduleDate)
  if (nextDate !== "-") {
    return `下一批未来可预约时间段从 ${nextDate} 开始；如果列表还是空，通常是你当前筛选条件过严。`
  }
  return "这里展示的是实时可预约时间摘要，不是写死的演示数字。"
})

const createBookingHint = computed(() => {
  if (!schedules.value.length) {
    return "当前没有可选的可预约时间段，先看上方说明或刷新列表。"
  }
  return "请先从上方时间段列表中选择一个可预约时间段。"
})

const parseCoachId = (value) => {
  if (value === null || value === undefined || value === "") return null
  const parsed = Number(value)
  return Number.isFinite(parsed) ? parsed : null
}

const coachPersonas = [
  { title: "金牌教练", tags: ["增肌", "塑形", "1v1"] },
  { title: "体态训练", tags: ["减脂", "体态", "拉伸"] },
  { title: "力量训练", tags: ["力量", "核心", "进阶"] },
  { title: "康复训练", tags: ["康复", "灵活性", "低冲击"] }
]

const coachPersona = (id) => coachPersonas[Math.abs(Math.trunc(Number(id) || 0)) % coachPersonas.length]

const normalizeCoachTags = (value, fallback) => {
  if (Array.isArray(value)) return value.filter(Boolean).slice(0, 3)
  if (typeof value === "string" && value.trim()) return value.split(/[、,，\s]+/).filter(Boolean).slice(0, 3)
  return fallback
}

const resolveCoachPhoto = (row) => {
  return row?.coachAvatar || row?.avatar || row?.photoUrl || row?.imageUrl || "/test.png"
}

const scheduleTimeValue = (row) => {
  const start = resolveScheduleStart(row)
  return start?.getTime?.() ?? Number.MAX_SAFE_INTEGER
}

const toRecentBooking = (item) => {
  if (!item) return null
  return {
    ...item,
    bookingId: item.bookingId ?? item.id
  }
}

const syncLastBooking = (rows) => {
  const normalizedRows = (rows || []).map((item) => toRecentBooking(item))
  const currentId = Number(lastBooking.value?.bookingId ?? lastBooking.value?.id ?? 0)
  const matched = currentId ? normalizedRows.find((item) => Number(item.bookingId) === currentId) : null
  if (matched) {
    lastBooking.value = {
      ...lastBooking.value,
      ...matched
    }
  } else if (normalizedRows.length) {
    lastBooking.value = {
      ...normalizedRows[0],
      orderNo: lastBooking.value?.orderId === normalizedRows[0].orderId ? lastBooking.value?.orderNo : normalizedRows[0].orderNo,
      amount: lastBooking.value?.orderId === normalizedRows[0].orderId ? lastBooking.value?.amount : normalizedRows[0].amount
    }
  }
  if (lastBooking.value) {
    localStorage.setItem(STORAGE_BOOKING, JSON.stringify(lastBooking.value))
  }
}

const loadScheduleSummary = async () => {
  try {
    const params = {}
    const parsedCoachId = parseCoachId(coachId.value)
    if (parsedCoachId !== null) {
      params.coachId = parsedCoachId
    }
    const { data } = await appClient.get("/app/booking/schedule/summary", { params })
    if (data.code !== 200) throw new Error(data.message || "加载可预约时间摘要失败")
    scheduleSummary.value = {
      ...scheduleSummary.value,
      ...(data.data || {})
    }
  } catch (err) {
    scheduleSummary.value = {
      coachId: parseCoachId(coachId.value),
      totalSchedules: schedules.value.length,
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
    if (coachId.value !== null && coachId.value !== undefined && coachId.value !== "") {
      if (parsedCoachId === null) {
        ElMessage.warning("教练ID需为数字")
        loading.value = false
        return
      }
      params.coachId = parsedCoachId
    }
    if (date.value) params.date = date.value
    const { data } = await appClient.get("/app/booking/schedule/list", { params })
    if (data.code !== 200) throw new Error(data.message || "加载可预约时间段失败")
    schedules.value = data.data || []
    if (schedules.value.length) {
      selected.value = schedules.value.find((item) => isScheduleAvailable(item)) || schedules.value[0]
    } else {
      selected.value = null
    }
    if (parsedCoachId !== null) localStorage.setItem(STORAGE_COACH_ID, String(parsedCoachId))
    await loadScheduleSummary()
  } catch (err) {
    ElMessage.error(err.message || "加载可预约时间段失败")
  } finally {
    loading.value = false
  }
}

const setCoach = (value) => {
  coachId.value = value
  if (value === null || value === undefined || value === "") localStorage.removeItem(STORAGE_COACH_ID)
  else localStorage.setItem(STORAGE_COACH_ID, String(value))
}

const bookCoach = async (coach) => {
  if (!coach?.targetSchedule) {
    ElMessage.warning("该教练当前没有可预约时间段")
    return
  }
  setCoach(coach.id)
  selected.value = coach.targetSchedule
  await nextTick()
  createBookingRef.value?.scrollIntoView?.({ behavior: "smooth", block: "start" })
}

const viewCoachReviews = async (coach) => {
  const target = myBookings.value.find((row) => Number(row.coachId) === Number(coach?.id) && row.bookingStatus === "COMPLETED")
  if (target) {
    selectedReviewBooking.value = target
    reviewForm.bookingId = target.id
    if (!reviewForm.score) reviewForm.score = 5
    reviewPanelVisible.value = true
    activeBookingTab.value = "bookings"
  } else {
    ElMessage.info("暂无该教练的已完成预约评价，可先预约体验")
  }
  await nextTick()
  reviewSectionRef.value?.scrollIntoView?.({ behavior: "smooth", block: "start" })
}

const coachOptions = computed(() => {
  const set = new Set()
  schedules.value.forEach((item) => {
    if (item?.coachId !== null && item?.coachId !== undefined) set.add(item.coachId)
  })
  const parsed = parseCoachId(coachId.value)
  if (parsed !== null) set.add(parsed)
  return Array.from(set).sort((a, b) => Number(a) - Number(b))
})

const selectSchedule = (row) => {
  if (!isScheduleAvailable(row)) {
    ElMessage.warning("这个时间段当前不可预约，请选择其他时间")
    return
  }
  selected.value = row
  scrollToSelectedSchedule()
}

const createBooking = async () => {
  if (!selected.value?.id) {
    ElMessage.warning("请先选择一个可预约时间段")
    return
  }
  try {
    submitting.value = true
    const { data } = await appClient.post("/app/booking/create", { scheduleId: selected.value.id })
    if (data.code !== 200) throw new Error(data.message || "预约失败")
    ElMessage.success("预约成功")
    lastBooking.value = toRecentBooking({
      ...data.data,
      coachId: selected.value.coachId,
      scheduleDate: selected.value.scheduleDate,
      startTime: selected.value.startTime,
      endTime: selected.value.endTime,
      bookingStatus: "WAIT_PAY",
      payStatus: "UNPAID"
    })
    if (data.data?.bookingId) {
      localStorage.setItem(STORAGE_BOOKING, JSON.stringify(lastBooking.value))
      localStorage.setItem("fp_last_order_id", String(data.data.orderId || ""))
    }
    await loadMyBookings()
  } catch (err) {
    ElMessage.error(err.message || "预约失败")
  } finally {
    submitting.value = false
  }
}

const loadLastBooking = () => {
  try {
    const cached = localStorage.getItem(STORAGE_BOOKING)
    if (cached) lastBooking.value = toRecentBooking(JSON.parse(cached))
  } catch {
    lastBooking.value = null
  }
}

const mockPay = async () => {
  if (!lastBooking.value?.bookingId) {
    ElMessage.warning("暂无预约可支付")
    return
  }
  try {
    paying.value = true
    const { data } = await appClient.post("/app/booking/pay-success", null, {
      params: { bookingId: lastBooking.value.bookingId }
    })
    if (data.code !== 200) throw new Error(data.message || "模拟支付失败")
    ElMessage.success("支付状态已更新")
    await loadMyBookings()
  } catch (err) {
    ElMessage.error(err.message || "模拟支付失败")
  } finally {
    paying.value = false
  }
}

const completeBooking = async () => {
  if (!lastBooking.value?.bookingId) {
    ElMessage.warning("暂无预约可完结")
    return
  }
  try {
    completing.value = true
    const { data } = await appClient.post("/app/booking/complete", null, {
      params: { bookingId: lastBooking.value.bookingId }
    })
    if (data.code !== 200) throw new Error(data.message || "操作失败")
    ElMessage.success("预约已完成")
    await loadMyBookings()
  } catch (err) {
    ElMessage.error(err.message || "操作失败")
  } finally {
    completing.value = false
  }
}

const loadMyBookings = async () => {
  try {
    myLoading.value = true
    const { data } = await appClient.get("/app/booking/my/list")
    if (data.code !== 200) throw new Error(data.message || "加载我的预约失败")
    myBookings.value = (data.data || []).map((item) => toRecentBooking(item))
    syncLastBooking(myBookings.value)
    if (!selectedReviewBooking.value) fillReviewFromCompleted(false)
  } catch (err) {
    ElMessage.error(err.message || "加载我的预约失败")
  } finally {
    myLoading.value = false
  }
}

const fillReviewFromCompleted = (showMessage = true) => {
  const target = myBookings.value.find((row) => row.bookingStatus === "COMPLETED")
  if (!target) {
    if (showMessage) ElMessage.warning("暂无可评价的已完成预约")
    return false
  }
  selectedReviewBooking.value = target
  reviewForm.bookingId = target.id
  if (!reviewForm.score) reviewForm.score = 5
  if (showMessage) reviewPanelVisible.value = true
  return true
}

const fillReviewSample = () => {
  if (!reviewForm.bookingId) {
    const ok = fillReviewFromCompleted()
    if (!ok) return
  }
  reviewForm.score = 5
  reviewForm.content = "训练安排合理，动作指导清晰，整体体验很好。"
}

const openReview = (row) => {
  if (row.bookingStatus !== "COMPLETED") {
    ElMessage.warning("只有已完成预约可评价")
    return
  }
  selectedReviewBooking.value = row
  reviewForm.bookingId = row.id
  reviewForm.score = 5
  reviewForm.content = ""
  reviewPanelVisible.value = true
  activeBookingTab.value = "bookings"
  nextTick(() => {
    reviewSectionRef.value?.scrollIntoView?.({ behavior: "smooth", block: "start" })
  })
}

const submitReview = async () => {
  if (!reviewForm.bookingId) {
    ElMessage.warning("请先选择预约")
    return
  }
  if (!reviewForm.content.trim()) {
    ElMessage.warning("请输入评价内容")
    return
  }
  try {
    reviewing.value = true
    const { data } = await appClient.post("/app/booking/review", {
      bookingId: Number(reviewForm.bookingId),
      score: reviewForm.score,
      content: reviewForm.content.trim()
    })
    if (data.code !== 200) throw new Error(data.message || "评价失败")
    ElMessage.success("评价成功")
    reviewForm.content = ""
    await loadMyBookings()
  } catch (err) {
    ElMessage.error(err.message || "评价失败")
  } finally {
    reviewing.value = false
  }
}

const goToOrder = (payload) => {
  const orderId = typeof payload === "object" && payload !== null ? payload.orderId : payload
  if (!orderId) return
  const query = {
    orderId: String(orderId),
    from: "booking"
  }
  if (payload && typeof payload === "object") {
    if (payload.id || payload.bookingId) query.bookingId = String(payload.id || payload.bookingId)
    if (payload.coachId) query.coachId = String(payload.coachId)
    if (payload.scheduleDate) query.scheduleDate = String(payload.scheduleDate)
    if (payload.startTime) query.startTime = String(payload.startTime)
    if (payload.endTime) query.endTime = String(payload.endTime)
  }
  router.push({ path: "/orders", query })
}

const goTo = (path) => {
  router.push(path)
}

const formatBookingStatus = (status) => {
  const map = { WAIT_PAY: "待支付", PAID: "已支付", COMPLETED: "已完成", CANCELLED: "已取消" }
  return map[status] || status || "-"
}

const formatPayStatus = (status) => {
  const map = { UNPAID: "未支付", PAID: "已支付", CLOSED: "已关闭", REFUNDED: "已退款" }
  return map[status] || status || "-"
}

const formatDate = (value) => {
  if (!value) return "-"
  const raw = String(value)
  if (raw.includes("T")) return raw.split("T")[0]
  return raw.slice(0, 10)
}

const formatTime = (value) => {
  if (!value) return "-"
  const raw = String(value)
  if (raw.includes("T")) return (raw.split("T")[1] || "").slice(0, 5)
  if (raw.includes(":")) return raw.slice(0, 5)
  return raw
}

const formatDateTime = (value) => {
  if (!value) return "-"
  const raw = String(value)
  if (raw.includes("T")) {
    const [date, time] = raw.split("T")
    return `${date} ${time.slice(0, 8)}`
  }
  if (raw.length >= 16 && raw.includes("-")) return raw.slice(0, 16)
  return raw
}

const formatAmount = (value) => {
  if (value === null || value === undefined || value === "") return "-"
  const amount = Number(value)
  return Number.isFinite(amount) ? amount.toFixed(2) : String(value)
}

const formatScheduleTime = (row) => {
  if (!row) return "-"
  const dateValue = row.scheduleDate || row.startTime
  return `${formatDate(dateValue)} ${formatTime(row.startTime)}-${formatTime(row.endTime)}`
}

const remainingSlots = (row) => {
  if (!row) return 0
  const capacity = Number(row.capacity ?? 0)
  const booked = Number(row.bookedCount ?? 0)
  const left = capacity - booked
  return Number.isFinite(left) ? Math.max(left, 0) : 0
}

const resolveScheduleStart = (row) => {
  if (!row?.scheduleDate) return null
  const time = row.startTime || "00:00:00"
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
  if (!row) return { text: "-", type: "info" }
  if (row.status !== null && row.status !== undefined && Number(row.status) !== 1) return { text: "停用", type: "info" }
  const start = resolveScheduleStart(row)
  if (start && start.getTime() < Date.now() - 60 * 1000) return { text: "已过期", type: "warning" }
  if (remainingSlots(row) <= 0) return { text: "已满", type: "danger" }
  return { text: "可预约", type: "success" }
}

const scrollToSelectedSchedule = async () => {
  await nextTick()
  if (!selected.value?.id) return
  const currentCard = document.querySelector(`[data-schedule-id="${selected.value.id}"]`)
  currentCard?.scrollIntoView?.({ block: "center", behavior: "smooth" })
}

const loadCoachId = () => {
  try {
    const cached = localStorage.getItem(STORAGE_COACH_ID)
    const parsed = parseCoachId(cached)
    if (parsed !== null) coachId.value = parsed
  } catch (err) {
    coachId.value = null
  }
}

loadCoachId()
loadSchedules()
loadMyBookings()
loadLastBooking()
</script>

<style scoped>
.booking-workspace {
  gap: 20px;
  --booking-border-strong: #171126;
  --booking-border-soft: rgba(23, 17, 38, 0.34);
  --booking-surface: #fffdf7;
  --booking-surface-purple: #f1edff;
  --booking-surface-green: #ebfff6;
  --booking-shadow-strong: 0 8px 0 rgba(23, 17, 38, 0.1), 0 18px 36px rgba(23, 17, 38, 0.1);
}

.booking-stats-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  flex-wrap: wrap;
  padding: 18px 20px;
  border: 2px solid var(--booking-border-strong);
  border-radius: 18px;
  background:
    radial-gradient(circle at 8% 0%, rgba(109, 103, 255, 0.28), transparent 34%),
    linear-gradient(135deg, #f2efff 0%, #fff8df 100%);
  box-shadow: var(--booking-shadow-strong);
}

.booking-stats-copy {
  display: grid;
  gap: 4px;
  min-width: 260px;
}

.booking-stats-copy .section-title,
.booking-stats-copy .section-sub {
  margin: 0;
}

.booking-stat-pills {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  margin-left: auto;
}

.booking-dashboard {
  align-items: flex-start;
}

.booking-sidebar {
  position: sticky;
  top: 20px;
  display: grid;
  gap: 16px;
}

.booking-main {
  display: grid;
  gap: 16px;
}

.workspace-card,
.booking-tabs {
  border: 2px solid var(--booking-border-strong);
  border-radius: 16px;
  background: var(--booking-surface);
  box-shadow: var(--booking-shadow-strong);
}

.workspace-card {
  padding: 18px;
}

.create-card {
  background: linear-gradient(180deg, #f2fff9 0%, #fffdf7 100%);
}

.create-submit {
  width: 100%;
  margin-top: 14px;
}

.booking-tabs {
  overflow: hidden;
}

.booking-tabs :deep(.el-tabs__header) {
  border-bottom: 2px solid var(--booking-border-strong);
  background: linear-gradient(90deg, #fff1c9, #ece8ff);
}

.booking-tabs :deep(.el-tabs__item) {
  color: #4d456d;
  font-weight: 800;
}

.booking-tabs :deep(.el-tabs__item.is-active) {
  background: #ffffff;
  color: var(--eco-primary);
}

.booking-tabs :deep(.el-tabs__content) {
  padding: 16px;
  background: #f8f5ff;
}

.booking-workspace .section-sub,
.booking-workspace .muted,
.summary-hint,
.detail-list,
.coach-card__meta,
.schedule-card__date,
.schedule-card__meta {
  color: #4d456d;
  font-weight: 650;
}

.booking-filter-row {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.booking-filter-row :deep(.el-form-item) {
  margin: 0;
}

.booking-filter-action :deep(.el-form-item__label) {
  color: transparent;
}

.quick-wrap,
.action-row--wrap {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  min-height: 40px;
}

.coach-list-block {
  margin: 0 0 14px;
  padding: 14px;
  border: 2px solid var(--booking-border-strong);
  border-radius: 16px;
  background: var(--booking-surface-purple);
  box-shadow: 0 6px 0 rgba(23, 17, 38, 0.07);
}

.coach-list-toolbar {
  margin-bottom: 10px;
}

.coach-list {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
  gap: 12px;
}

.coach-card {
  display: flex;
  flex-direction: row;
  align-items: flex-start;
  gap: 12px;
  padding: 16px;
  border: 2px solid var(--booking-border-strong);
  border-radius: 16px;
  background: #ffffff;
  box-shadow: 0 6px 0 rgba(23, 17, 38, 0.08);
  transition: transform 0.16s ease, box-shadow 0.16s ease, background-color 0.16s ease;
}

.coach-card:hover {
  transform: translateY(-1px);
  background: #fffaf0;
  box-shadow: 0 9px 0 rgba(23, 17, 38, 0.1), 0 16px 30px rgba(52, 45, 105, 0.12);
}

.coach-card__photo {
  width: 88px;
  height: 88px;
  flex: 0 0 88px;
  border-radius: 12px;
  overflow: hidden;
  border: 2px solid var(--booking-border-strong);
  background: #f2efff;
}

.coach-card__photo :deep(img) {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.coach-card__fallback {
  width: 100%;
  height: 100%;
  display: grid;
  place-items: center;
  background: linear-gradient(135deg, rgba(109, 103, 255, 0.18), rgba(255, 188, 109, 0.22));
  color: var(--eco-primary);
  font-size: 26px;
  font-weight: 900;
}

.coach-card__body {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.coach-card__top {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 10px;
}

.coach-card__name {
  display: block;
  color: var(--eco-text);
  font-size: 18px;
  line-height: 1.2;
}

.coach-card__title {
  margin: 3px 0 0;
  color: var(--eco-text-soft);
  font-size: 12px;
  font-weight: 700;
}

.coach-card__rating {
  flex: 0 0 auto;
  border-radius: 999px;
  border: 2px solid var(--booking-border-strong);
  background: #ece8ff;
  padding: 4px 9px;
  color: var(--eco-primary);
  font-size: 12px;
  font-weight: 800;
}

.coach-card__tags,
.detail-chip-row {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 6px;
}

.coach-card__tags :deep(.el-tag) {
  border-color: rgba(23, 17, 38, 0.42);
  background: #f7f3ff;
  color: var(--eco-primary);
  font-weight: 700;
}

.coach-card__meta {
  display: flex;
  flex-wrap: wrap;
  gap: 6px 12px;
  color: var(--eco-text-soft);
  font-size: 12px;
  line-height: 1.4;
}

.coach-card__actions {
  margin-top: auto;
  display: flex;
  justify-content: flex-end;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.schedule-grid-shell {
  min-height: 240px;
}

.schedule-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(190px, 1fr));
  gap: 12px;
}

.schedule-card {
  appearance: none;
  width: 100%;
  min-height: 128px;
  padding: 14px;
  border: 2px solid var(--booking-border-strong);
  border-radius: 16px;
  background: linear-gradient(180deg, #fffaf0 0%, #ffffff 100%);
  color: var(--eco-text);
  text-align: left;
  cursor: pointer;
  box-shadow: 0 6px 0 rgba(23, 17, 38, 0.08);
  transition: transform 0.16s ease, box-shadow 0.16s ease, border-color 0.16s ease, background 0.16s ease;
}

.schedule-card:hover {
  transform: translateY(-2px);
  border-color: #171126;
  background: linear-gradient(180deg, #fff4d7 0%, #ffffff 100%);
  box-shadow: 0 9px 0 rgba(23, 17, 38, 0.11), 0 16px 30px rgba(52, 45, 105, 0.12);
}

.schedule-card--active {
  border-color: var(--booking-border-strong);
  background: linear-gradient(180deg, var(--booking-surface-green) 0%, #ffffff 100%);
  box-shadow: 0 9px 0 rgba(57, 185, 136, 0.22), 0 16px 30px rgba(23, 17, 38, 0.1);
}

.schedule-card--disabled {
  opacity: 0.82;
  background: linear-gradient(180deg, #fff7e9 0%, #fffdf7 100%);
  cursor: not-allowed;
}

.schedule-card__top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.schedule-card__date {
  color: var(--eco-text-soft);
  font-size: 12px;
  font-weight: 800;
}

.schedule-card__time {
  display: block;
  margin-top: 14px;
  font-size: 20px;
  line-height: 1.2;
}

.schedule-card__meta {
  display: flex;
  flex-wrap: wrap;
  gap: 6px 10px;
  margin-top: 14px;
  color: var(--eco-text-soft);
  font-size: 12px;
  font-weight: 700;
}

.empty-tip {
  margin: 12px 0 0;
  color: var(--eco-text-soft);
  font-size: 13px;
  line-height: 1.6;
}

.detail-card {
  border: 2px solid var(--booking-border-strong);
  border-radius: 16px;
  background: linear-gradient(180deg, #ffffff 0%, #f1edff 100%);
  padding: 14px;
  box-shadow: 0 6px 0 rgba(23, 17, 38, 0.07);
}

.selected-schedule-card {
  background: linear-gradient(180deg, var(--booking-surface-green) 0%, #ffffff 100%);
}

.detail-card__title {
  color: var(--eco-text-soft);
  font-size: 12px;
  font-weight: 700;
  margin-bottom: 8px;
}

.detail-card__headline {
  font-size: 20px;
  line-height: 1.3;
  font-weight: 800;
}

.detail-chip-row {
  margin-top: 12px;
}

.detail-list {
  display: grid;
  gap: 6px;
  margin-top: 12px;
  color: var(--eco-text-soft);
  font-size: 13px;
}

.booking-summary-row {
  margin-top: 12px;
}

.summary-hint {
  margin: 10px 0 0;
  color: var(--eco-text-soft);
  font-size: 13px;
  line-height: 1.6;
}

.bookings-tab-toolbar {
  margin-bottom: 12px;
}

.review-panel {
  margin-top: 16px;
  padding: 16px;
  border: 2px solid var(--booking-border-strong);
  border-radius: 16px;
  background: linear-gradient(180deg, #ffffff, #ece8ff);
  box-shadow: 0 6px 0 rgba(23, 17, 38, 0.08);
}

.booking-tabs :deep(.el-table) {
  border: 2px solid var(--booking-border-strong);
  border-radius: 14px;
  overflow: hidden;
}

.booking-tabs :deep(.el-table th.el-table__cell) {
  background: #fff1c9;
  color: var(--eco-text);
}

.booking-tabs :deep(.el-alert) {
  border: 2px solid var(--booking-border-soft);
  border-radius: 14px;
}

.booking-table__slot,
.booking-table__order {
  display: inline-flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 4px;
}

.booking-table__order-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.order-no {
  max-width: 100%;
  word-break: break-all;
}

@media (max-width: 992px) {
  .booking-dashboard :deep(.el-col-md-8),
  .booking-dashboard :deep(.el-col-md-16) {
    flex: 0 0 100%;
    max-width: 100%;
  }

  .booking-sidebar {
    position: static;
    margin-bottom: 16px;
  }

  .booking-stats-bar {
    align-items: flex-start;
  }

  .booking-stat-pills {
    width: 100%;
    margin-left: 0;
  }
}

@media (max-width: 640px) {
  .workspace-card,
  .booking-tabs :deep(.el-tabs__content) {
    padding: 14px;
  }

  .coach-list,
  .schedule-grid {
    grid-template-columns: 1fr;
  }

  .coach-card {
    padding: 14px;
  }

  .coach-card__photo {
    width: 72px;
    height: 72px;
    flex-basis: 72px;
  }
}
</style>
