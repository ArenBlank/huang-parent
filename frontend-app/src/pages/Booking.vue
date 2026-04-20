<template>
  <div class="page-stack">
    <section class="hero-panel">
      <div class="booking-hero">
        <div>
          <p class="quest-kicker">Coach Booking</p>
          <h1 class="hero-title">预约教练，形成稳定训练节律</h1>
          <p class="hero-subtitle">先挑选可预约时间段，再提交预约，并在同页完成支付模拟、完结和评价。</p>
          <div class="hero-badges">
            <span class="badge-pill is-dark">当前列表 {{ schedules.length }}</span>
            <span class="badge-pill is-dark">未来可预约 {{ effectiveAvailableSchedules }}</span>
            <span class="badge-pill is-dark">我的预约 {{ myBookings.length }}</span>
          </div>
        </div>
        <el-button type="primary" @click="loadSchedules" :loading="loading">刷新可预约时间</el-button>
      </div>
    </section>

    <section class="card">
      <div class="toolbar">
        <div>
          <h2 class="section-title">可预约时间段筛选</h2>
          <p class="section-sub">按教练与日期筛选当前还能预约的时间段。</p>
        </div>
      </div>
      <el-form :inline="true" label-position="top">
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
        <el-form-item label="快捷教练">
          <div class="quick-wrap">
            <el-button size="small" :type="coachId === null ? 'primary' : 'default'" @click="setCoach(null)">全部</el-button>
            <el-button
              v-for="id in coachQuickOptions"
              :key="id"
              size="small"
              :type="Number(coachId) === Number(id) ? 'primary' : 'default'"
              @click="setCoach(id)"
            >
              {{ id }}
            </el-button>
          </div>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="loadSchedules">查询</el-button>
        </el-form-item>
      </el-form>
      <p class="muted">当前接口仅支持教练ID筛选，教练姓名筛选将在后续版本扩展。</p>
      <div class="detail-chip-row booking-summary-row">
        <span class="tag">未来时间段 {{ scheduleSummary.futureSchedules ?? 0 }}</span>
        <span class="tag">可预约时间段 {{ effectiveAvailableSchedules }}</span>
        <span class="tag">最近一次开放时间 {{ formatDate(scheduleSummary.lastScheduleDate) }}</span>
      </div>
      <p class="summary-hint">{{ scheduleSummaryHint }}</p>
    </section>

    <section class="card">
      <div class="toolbar">
        <div>
          <h2 class="section-title">可预约时间段列表</h2>
          <p class="section-sub">点击任一可预约时间段，就会自动带入下方“创建预约”。</p>
        </div>
      </div>

      <el-empty v-if="!loading && !schedules.length" description="暂无可预约时间段">
        <div class="empty-actions">
          <el-button size="small" @click="loadSchedules">重试</el-button>
          <el-button size="small" @click="goTo('/courses')">去课程报名</el-button>
        </div>
        <p class="empty-tip">{{ scheduleEmptyReason }}</p>
      </el-empty>

      <div v-else>
        <el-alert
          v-if="!loading && schedules.length && !availableSchedules"
          type="warning"
          show-icon
          :closable="false"
          title="当前筛选结果中没有可预约时间段"
          style="margin-bottom: 12px"
        />
        <el-table
          :data="schedules"
          v-loading="loading"
          style="width: 100%"
          @row-click="selectSchedule"
          :row-class-name="scheduleRowClass"
          highlight-current-row
          ref="scheduleTableRef"
        >
          <el-table-column prop="id" label="ID" width="80" />
          <el-table-column label="日期">
            <template #default="{ row }">{{ formatDate(row.scheduleDate) }}</template>
          </el-table-column>
          <el-table-column label="时间" min-width="170">
            <template #default="{ row }">{{ formatTime(row.startTime) }} - {{ formatTime(row.endTime) }}</template>
          </el-table-column>
          <el-table-column prop="coachId" label="教练ID" width="90" />
          <el-table-column prop="price" label="价格" width="100" />
          <el-table-column label="余量" width="100">
            <template #default="{ row }">
              <span :class="{ full: remainingSlots(row) <= 0 }">{{ remainingSlots(row) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="120">
            <template #default="{ row }">
              <el-tag :type="scheduleStatus(row).type" size="small">{{ scheduleStatus(row).text }}</el-tag>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </section>

    <section class="card">
      <div class="toolbar">
        <div>
          <h2 class="section-title">创建预约</h2>
          <p class="section-sub">选中一个可预约时间段后即可提交预约。</p>
        </div>
        <el-button type="success" :disabled="!selected || selectedUnavailable" :loading="submitting" @click="createBooking">
          提交预约
        </el-button>
      </div>
      <el-empty v-if="!selected" :description="createBookingHint" />
      <div v-else class="detail detail-card">
        <div class="detail-card__title">已选时间段</div>
        <div class="detail-card__headline">{{ formatScheduleTime(selected) }}</div>
        <div class="detail-chip-row">
          <span class="tag">时间段ID {{ selected.id }}</span>
          <span class="tag">教练 #{{ selected.coachId }}</span>
          <span class="tag">价格 {{ selected.price }}</span>
          <span class="tag">余量 {{ remainingSlots(selected) }}</span>
          <span class="tag">状态 {{ scheduleStatus(selected).text }}</span>
        </div>
      </div>
    </section>

    <section class="card">
      <div class="toolbar">
        <div>
          <h2 class="section-title">最近预约</h2>
          <p class="section-sub">支持支付模拟和确认完成。</p>
        </div>
      </div>
      <el-empty v-if="!lastBooking" description="暂无最近预约" />
      <div v-else class="detail detail-card">
        <div class="detail-card__title">最近一次预约</div>
        <div class="detail-chip-row">
          <span class="tag">预约ID {{ lastBooking.bookingId || lastBooking.id }}</span>
          <span class="tag">订单ID {{ lastBooking.orderId || "-" }}</span>
          <span class="tag">预约状态 {{ formatBookingStatus(lastBooking.bookingStatus) }}</span>
          <span class="tag">支付状态 {{ formatPayStatus(lastBooking.payStatus) }}</span>
        </div>
        <div class="detail-list">
          <div>订单号：{{ lastBooking.orderNo || "-" }}</div>
          <div>金额：{{ lastBooking.amount ?? "-" }}</div>
          <div>预约时间：{{ formatScheduleTime(lastBooking) }}</div>
          <div>教练：{{ lastBooking.coachId ? `#${lastBooking.coachId}` : "-" }}</div>
        </div>
        <div class="action-row">
          <el-button type="primary" size="small" :loading="paying" @click="mockPay">模拟支付</el-button>
          <el-button type="success" size="small" :loading="completing" @click="completeBooking">确认完成</el-button>
          <el-button size="small" :disabled="!lastBooking?.orderId" @click="goToOrder(lastBooking)">查看订单详情</el-button>
        </div>
      </div>
    </section>

    <section class="card">
      <div class="toolbar">
        <div>
          <h2 class="section-title">我的预约</h2>
          <p class="section-sub">已完成预约可直接发起评价。</p>
        </div>
        <el-button type="primary" @click="loadMyBookings" :loading="myLoading">刷新</el-button>
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
    </section>

    <section class="card">
      <div class="toolbar">
        <div>
          <h2 class="section-title">预约评价</h2>
          <p class="section-sub">评价仅支持已完成预约。</p>
        </div>
        <el-button size="small" @click="fillReviewFromCompleted">自动选择已完成预约</el-button>
      </div>
      <el-alert
        v-if="!selectedReviewBooking"
        type="info"
        show-icon
        :closable="false"
        title="先在我的预约中选择已完成记录"
        style="margin-bottom: 12px"
      />
      <div v-else class="detail">
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
const scheduleTableRef = ref(null)

const myBookings = ref([])
const myLoading = ref(false)
const selectedReviewBooking = ref(null)
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
      await scrollToSelectedSchedule()
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

const coachOptions = computed(() => {
  const set = new Set()
  schedules.value.forEach((item) => {
    if (item?.coachId !== null && item?.coachId !== undefined) set.add(item.coachId)
  })
  const parsed = parseCoachId(coachId.value)
  if (parsed !== null) set.add(parsed)
  return Array.from(set).sort((a, b) => Number(a) - Number(b))
})

const coachQuickOptions = computed(() => coachOptions.value.slice(0, 5))

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

const scheduleRowClass = ({ row }) => {
  const classes = []
  if (selected.value?.id === row.id) classes.push("row-selected")
  if (!isScheduleAvailable(row)) classes.push("row-disabled")
  return classes.join(" ")
}

const scrollToSelectedSchedule = async () => {
  await nextTick()
  if (!scheduleTableRef.value || !selected.value) return
  try {
    scheduleTableRef.value.setCurrentRow?.(selected.value)
  } catch (err) {
    // ignore
  }
  await nextTick()
  const tableEl = scheduleTableRef.value?.$el
  if (!tableEl) return
  const currentRow = tableEl.querySelector(".el-table__body .current-row")
  if (currentRow?.scrollIntoView) currentRow.scrollIntoView({ block: "center", behavior: "smooth" })
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
.booking-hero {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  gap: 12px;
  flex-wrap: wrap;
}

.quick-wrap {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.empty-tip {
  margin: 12px 0 0;
  color: var(--eco-text-soft);
  font-size: 13px;
  line-height: 1.6;
}

.full {
  color: #ca5744;
  font-weight: 700;
}

.detail-card {
  border: 2px solid rgba(52, 45, 105, 0.12);
  border-radius: 20px;
  background: #ffffff;
  padding: 16px;
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
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
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

:deep(.row-selected) td {
  background: #ecfff8 !important;
}

:deep(.row-disabled) td {
  color: #6f6894;
}
</style>
