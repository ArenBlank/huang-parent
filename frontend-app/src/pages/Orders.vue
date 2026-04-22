<template>
  <div class="page-stack orders-page">
    <section class="hero-panel">
      <div class="order-hero">
        <div>
          <p class="quest-kicker">Orders Center</p>
          <h1 class="hero-title">订单列表留在左侧，详情固定在右侧</h1>
          <p class="hero-subtitle">切换订单时不再把人带到长页面下方。现在可以边看订单列表，边在旁边查看详情、资金说明和退款原因。</p>
          <div class="hero-badges">
            <span class="badge-pill is-dark">订单 {{ orders.length }}</span>
            <span class="badge-pill is-dark">明细 {{ normalized.items.length }}</span>
            <span class="badge-pill is-dark">当前状态 {{ formatOrderStatus(normalized.orderStatus) }}</span>
          </div>
        </div>
        <div class="toolbar-actions">
          <el-button v-if="showBackToSource" @click="backToSource">{{ backToSourceLabel }}</el-button>
          <el-button plain @click="scrollToOrders">回到订单列表</el-button>
          <el-button type="primary" @click="loadOrders" :loading="ordersLoading">刷新订单</el-button>
        </div>
      </div>
    </section>

    <div class="orders-workspace">
      <section ref="ordersRef" class="card orders-browser">
        <div class="toolbar">
          <div>
            <h2 class="section-title">订单浏览区</h2>
            <p class="section-sub">订单列表保持在当前视野里，查看一单后还能立即切换下一单。</p>
          </div>
        </div>

        <el-empty v-if="!orders.length && !ordersLoading" description="暂无订单，可先完成报名或预约">
          <div class="empty-actions">
            <el-button size="small" @click="go('/courses')">去报名课程</el-button>
            <el-button size="small" @click="go('/booking')">去预约教练</el-button>
          </div>
        </el-empty>

        <template v-else>
          <div class="orders-controls">
            <el-input
              v-model="orderKeyword"
              class="order-search"
              clearable
              placeholder="搜索订单号、类型、状态或金额"
            />
            <div class="orders-stats">
              <span class="badge-pill is-dark">显示 {{ filteredOrders.length }}</span>
              <span class="badge-pill">总计 {{ orders.length }}</span>
            </div>
          </div>

          <div v-if="selectedOrder" class="orders-selected-banner">
            <div>
              <p class="selected-caption">当前选中订单</p>
              <strong>{{ selectedOrder.orderNo }}</strong>
              <p>
                订单ID {{ selectedOrder.id }}，
                {{ formatBizType(selectedOrder.bizType) }}，
                {{ formatOrderStatus(selectedOrder.orderStatus) }}，
                右侧详情已同步。
              </p>
            </div>
            <div class="orders-selected-chips">
              <span class="tag">订单ID {{ selectedOrder.id }}</span>
              <span class="tag">金额 {{ formatAmount(selectedOrder.totalAmount) }}</span>
              <span class="tag">支付 {{ formatPayStatus(selectedOrder.payStatus) }}</span>
              <span class="tag">时间 {{ formatDateTime(selectedOrder.createTime) }}</span>
            </div>
          </div>

          <el-empty v-if="!filteredOrders.length && !ordersLoading" description="没有匹配订单">
            <div class="empty-actions">
              <el-button size="small" @click="orderKeyword = ''">清空搜索</el-button>
            </div>
          </el-empty>

          <div v-else class="orders-scroller" v-loading="ordersLoading">
            <div class="orders-list">
              <button
                v-for="row in filteredOrders"
                :key="row.id"
                type="button"
                class="order-card eco-clickable"
                :class="{ 'order-card--active': Number(row.id) === Number(orderId) }"
                @click="handleOrderSelect(row)"
              >
                <div class="order-card__top">
                  <div>
                    <p class="order-card__eyebrow">{{ formatBizType(row.bizType) }} · 订单ID {{ row.id }}</p>
                    <strong>{{ row.orderNo }}</strong>
                  </div>
                  <span class="order-card__amount">{{ formatAmount(row.totalAmount) }}</span>
                </div>
                <div class="order-card__meta">
                  <span class="tag">支付 {{ formatPayStatus(row.payStatus) }}</span>
                  <span class="tag">订单 {{ formatOrderStatus(row.orderStatus) }}</span>
                </div>
                <div class="order-card__foot">
                  <span>{{ formatDateTime(row.createTime) }}</span>
                  <span class="order-card__hint">查看详情</span>
                </div>
              </button>
            </div>
          </div>
        </template>
      </section>

      <div class="detail-column">
        <section class="card detail-panel">
          <div class="detail-shell">
            <div class="detail-head">
              <div>
                <p class="section-eyebrow">订单详情</p>
                <h2 class="section-title-sm">当前订单工作台</h2>
                <p class="section-sub">详情固定显示在右侧，订单切换时不需要反复上下滚动页面。</p>
              </div>
              <el-button plain size="small" @click="scrollToOrders">换一张订单</el-button>
            </div>

            <el-empty v-if="orders.length && !detail && !loading" description="请选择一条订单查看详情" />

            <div v-else-if="detail" class="detail-stack" v-loading="loading">
              <article v-if="sourceContext" class="source-card">
                <div class="source-card__head">
                  <div>
                    <p class="section-eyebrow">来源定位</p>
                    <h3 class="section-title-sm">这就是你刚才点开的那一单</h3>
                  </div>
                </div>
                <div class="source-card__chips">
                  <span class="tag">来源 {{ sourceContext.sourceLabel }}</span>
                  <span class="tag">{{ sourceEntityLabel }} {{ sourceContext.enrollmentId || "-" }}</span>
                  <span class="tag">订单ID {{ sourceContext.orderId || "-" }}</span>
                </div>
                <p class="source-card__summary">
                  {{ sourceContext.courseTitle ? `${sourceSummaryLabel}：${sourceContext.courseTitle}` : "来源信息已同步到当前订单工作台。" }}
                </p>
              </article>

              <article class="order-no-card">
                <div>
                  <p class="metric-label">订单ID</p>
                  <p class="order-id">{{ normalized.orderId || "-" }}</p>
                  <p class="metric-label">订单号</p>
                  <p class="order-no">{{ normalized.orderNo }}</p>
                </div>
                <div class="order-no-actions">
                  <el-button size="small" @click="copyOrderNo">复制订单号</el-button>
                </div>
              </article>

              <div class="summary-grid">
                <article class="summary-item">
                  <p class="metric-label">订单ID</p>
                  <p class="summary-value">{{ normalized.orderId || "-" }}</p>
                </article>
                <article class="summary-item">
                  <p class="metric-label">{{ sourceEntityLabel }}</p>
                  <p class="summary-value">{{ sourceContext?.enrollmentId || normalized.bizId || "-" }}</p>
                </article>
                <article class="summary-item">
                  <p class="metric-label">支付状态</p>
                  <p class="summary-value">{{ formatPayStatus(normalized.payStatus) }}</p>
                </article>
                <article class="summary-item">
                  <p class="metric-label">订单状态</p>
                  <p class="summary-value">{{ formatOrderStatus(normalized.orderStatus) }}</p>
                </article>
                <article class="summary-item">
                  <p class="metric-label">金额</p>
                  <p class="summary-value">{{ formatAmount(normalized.totalAmount) }}</p>
                </article>
                <article class="summary-item">
                  <p class="metric-label">实付</p>
                  <p class="summary-value">{{ formatAmount(normalized.paidAmount) }}</p>
                </article>
                <article class="summary-item">
                  <p class="metric-label">退款</p>
                  <p class="summary-value">{{ formatAmount(normalized.refundAmount) }}</p>
                </article>
                <article class="summary-item">
                  <p class="metric-label">净支付</p>
                  <p class="summary-value">{{ formatAmount(normalized.netPaid) }}</p>
                </article>
                <article class="summary-item">
                  <p class="metric-label">支付渠道</p>
                  <p class="summary-value">{{ formatPayChannel(normalized.payChannel) }}</p>
                </article>
                <article class="summary-item">
                  <p class="metric-label">支付时间</p>
                  <p class="summary-value">{{ formatDateTime(normalized.payTime) }}</p>
                </article>
                <article class="summary-item">
                  <p class="metric-label">退款状态</p>
                  <p class="summary-value">{{ formatPayStatus(normalized.refundStatus) }}</p>
                </article>
                <article class="summary-item">
                  <p class="metric-label">退款时间</p>
                  <p class="summary-value">{{ formatDateTime(normalized.refundTime) }}</p>
                </article>
              </div>

              <article class="detail-block">
                <div class="detail-block__head">
                  <div>
                    <p class="section-eyebrow">订单明细</p>
                    <h3 class="section-title-sm">费用组成</h3>
                  </div>
                </div>
                <div class="data-shell">
                  <el-table :data="normalized.items" style="width: 100%" size="small">
                    <el-table-column prop="itemName" label="名称" />
                    <el-table-column prop="itemType" label="类型" width="120" />
                    <el-table-column prop="quantity" label="数量" width="90" />
                    <el-table-column prop="price" label="单价" width="120" />
                    <el-table-column prop="amount" label="金额" width="120" />
                  </el-table>
                </div>
              </article>

              <article class="finance-card">
                <p class="section-eyebrow">资金说明</p>
                <h3 class="section-title-sm finance-main">
                  {{ formatFinanceDisplay(normalized.financeSummary.displayText, normalized.refundStatus, normalized.payStatus, normalized.orderStatus) }}
                </h3>
                <p class="finance-sub">
                  {{ formatFinanceExplain(normalized.financeSummary.statusExplain, normalized.refundStatus, normalized.payStatus, normalized.orderStatus) }}
                </p>
                <div class="finance-grid">
                  <div class="finance-line"><span>状态文案</span><strong>{{ formatFinanceField(normalized.financeSummary.statusText) }}</strong></div>
                  <div class="finance-line"><span>状态提示</span><strong>{{ formatFinanceField(normalized.financeSummary.statusHint) }}</strong></div>
                  <div class="finance-line"><span>阶段</span><strong>{{ formatFinanceField(normalized.financeSummary.stage) }}</strong></div>
                  <div class="finance-line"><span>说明</span><strong>{{ formatFinanceField(normalized.financeSummary.statusExplain) }}</strong></div>
                </div>
              </article>

              <article class="reason-card">
                <p class="section-eyebrow">退款原因</p>
                <h3 class="section-title-sm">说明</h3>
                <p>{{ formatValue(normalized.refundReason) }}</p>
              </article>
            </div>

            <div v-else class="detail-loading-state" v-loading="loading"></div>
          </div>
        </section>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, nextTick, ref, watch } from "vue"
import { useRoute, useRouter } from "vue-router"
import { ElMessage } from "element-plus"
import { appClient } from "../api/client"

const route = useRoute()
const router = useRouter()

const orderId = ref(null)
const orders = ref([])
const orderKeyword = ref("")
const ordersLoading = ref(false)
const detail = ref(null)
const loading = ref(false)
const ordersRef = ref(null)

const sourceFrom = computed(() => String(route.query.from || ""))
const showBackToSource = computed(() => sourceFrom.value === "courses" || sourceFrom.value === "booking")
const backToSourceLabel = computed(() => (sourceFrom.value === "booking" ? "返回我的预约" : "返回最近报名"))
const sourceEntityLabel = computed(() => (sourceFrom.value === "booking" ? "预约ID" : "报名ID"))
const sourceSummaryLabel = computed(() => (sourceFrom.value === "booking" ? "档期" : "课程"))

const filteredOrders = computed(() => {
  const keyword = orderKeyword.value.trim().toLowerCase()
  if (!keyword) return orders.value
  return orders.value.filter((row) => {
    const haystack = [
      row.orderNo,
      row.bizType,
      formatBizType(row.bizType),
      row.payStatus,
      formatPayStatus(row.payStatus),
      row.orderStatus,
      formatOrderStatus(row.orderStatus),
      row.totalAmount
    ]
      .filter(Boolean)
      .join(" ")
      .toLowerCase()
    return haystack.includes(keyword)
  })
})

const selectedOrder = computed(() => orders.value.find((row) => Number(row.id) === Number(orderId.value)) || null)

const normalized = computed(() => {
  const d = detail.value || {}
  const order = d.order || {}
  const base = Object.keys(order).length ? order : d
  return {
    orderId: base.id || d.id || orderId.value,
    orderNo: base.orderNo || "-",
    orderStatus: base.orderStatus ?? d.orderStatus,
    payStatus: base.payStatus ?? d.payStatus,
    payChannel: base.payChannel ?? d.payChannel,
    payTime: base.payTime ?? d.payTime,
    refundStatus: base.refundStatus ?? d.refundStatus,
    refundTime: base.refundTime ?? d.refundTime,
    refundReason: base.refundReason ?? d.refundReason,
    totalAmount: base.totalAmount ?? d.totalAmount,
    bizType: base.bizType ?? d.bizType,
    bizId: base.bizId ?? d.bizId,
    paidAmount: d.paidAmount ?? base.paidAmount ?? base.totalAmount ?? d.totalAmount,
    refundAmount: d.refundAmount ?? base.refundAmount,
    netPaid: d.netPaid ?? base.netPaid,
    items: d.items || base.items || [],
    financeSummary: d.financeSummary || base.financeSummary || {}
  }
})

const sourceContext = computed(() => {
  if (!showBackToSource.value) return null
  if (sourceFrom.value === "booking") {
    const bookingId = route.query.bookingId ? Number(route.query.bookingId) : normalized.value.bizId
    const scheduleDate = route.query.scheduleDate ? String(route.query.scheduleDate) : ""
    const startTime = route.query.startTime ? String(route.query.startTime).slice(0, 5) : ""
    const endTime = route.query.endTime ? String(route.query.endTime).slice(0, 5) : ""
    const timeText = scheduleDate ? `${scheduleDate} ${startTime || "--:--"}-${endTime || "--:--"}` : ""
    return {
      sourceLabel: "教练预约",
      courseTitle: timeText,
      enrollmentId: Number.isFinite(Number(bookingId)) && Number(bookingId) > 0 ? Number(bookingId) : null,
      orderId: normalized.value.orderId || (route.query.orderId ? Number(route.query.orderId) : null)
    }
  }

  const courseTitle = route.query.courseTitle ? String(route.query.courseTitle) : ""
  const enrollmentId = route.query.enrollmentId ? Number(route.query.enrollmentId) : normalized.value.bizId
  return {
    sourceLabel: "课程报名",
    courseTitle,
    enrollmentId: Number.isFinite(Number(enrollmentId)) && Number(enrollmentId) > 0 ? Number(enrollmentId) : null,
    orderId: normalized.value.orderId || (route.query.orderId ? Number(route.query.orderId) : null)
  }
})

const formatPayStatus = (status) => {
  const map = { UNPAID: "未支付", PAID: "已支付", CLOSED: "已关闭", REFUNDED: "已退款" }
  return map[status] || status || "-"
}

const formatOrderStatus = (status) => {
  const map = {
    NEW: "新建",
    UNPAID: "待支付",
    PAID: "已支付",
    CLOSED: "已关闭",
    CANCELLED: "已取消",
    REFUNDED: "已退款"
  }
  return map[status] || status || "-"
}

const formatPayChannel = (channel) => {
  const map = { wechat: "微信", alipay: "支付宝", mock: "模拟" }
  return map[channel] || channel || "-"
}

const formatBizType = (value) => {
  const map = { coach_booking: "教练预约", course: "课程报名", course_enrollment: "课程报名" }
  return map[value] || value || "-"
}

const formatValue = (value) => {
  if (value === null || value === undefined || value === "") return "-"
  return value
}

const formatFinanceField = (value) => {
  if (value === null || value === undefined || value === "") return "-"
  const raw = String(value).trim()
  const map = {
    refunded: "已退款",
    order_refunded: "订单已退款",
    REFUND: "退款完成",
    Refunded: "已退款",
    "Refund completed": "退款已完成",
    refunded_order: "退款订单",
    paid: "已支付",
    Paid: "已支付",
    "Paid (awaiting service)": "已支付，待服务完成",
    "Payment received": "订单已完成支付",
    unpaid: "未支付",
    Unpaid: "待支付",
    CLOSED: "已关闭",
    closed: "已关闭",
    Closed: "已关闭",
    "Order closed": "订单已关闭",
    order_unknown: "订单状态未知",
    Unknown: "状态未知",
    "Unknown status": "当前状态暂时无法识别",
    NONE: "无退款",
    PENDING: "处理中",
    REJECTED: "已拒绝"
  }
  return map[raw] || raw
}

const formatFinanceDisplay = (value, refundStatus, payStatus, orderStatus) => {
  const raw = String(value || "").trim()
  const map = {
    Refunded: "已退款",
    "Paid (awaiting service)": "已支付",
    Closed: "已关闭",
    Unpaid: "待支付",
    Unknown: "状态未知"
  }
  if (map[raw]) return map[raw]
  if (refundStatus === "REFUNDED") return "已退款"
  if (payStatus === "PAID") return "已支付"
  if (orderStatus === "CLOSED") return "已关闭"
  if (payStatus === "UNPAID") return "待支付"
  return formatValue(value)
}

const formatFinanceExplain = (value, refundStatus, payStatus, orderStatus) => {
  const raw = String(value || "").trim()
  const map = {
    "Refund completed": "退款已完成",
    "Payment received": "订单已完成支付",
    "Order closed": "订单已关闭",
    "Awaiting payment": "等待支付",
    "Unknown status": "当前状态暂时无法识别"
  }
  if (map[raw]) return map[raw]
  if (refundStatus === "REFUNDED") return "退款已完成"
  if (payStatus === "PAID") return "订单已完成支付"
  if (orderStatus === "CLOSED") return "订单已关闭"
  if (payStatus === "UNPAID") return "等待支付"
  return formatValue(value)
}

const formatDateTime = (value) => {
  if (value === null || value === undefined || value === "") return "-"
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
  return value
}

const copyOrderNo = async () => {
  if (!normalized.value.orderNo || normalized.value.orderNo === "-") {
    ElMessage.warning("当前没有可复制的订单号")
    return
  }
  try {
    await navigator.clipboard.writeText(String(normalized.value.orderNo))
    ElMessage.success("订单号已复制")
  } catch (err) {
    ElMessage.error("复制失败，请手动复制")
  }
}

const openOrder = async (id) => {
  if (!id) return
  orderId.value = Number(id)
  localStorage.setItem("fp_last_order_id", String(id))
  const nextQuery = { ...route.query, orderId: String(id) }
  if (Number(route.query.orderId) !== Number(id)) {
    delete nextQuery.enrollmentId
    delete nextQuery.courseId
    delete nextQuery.courseTitle
    delete nextQuery.from
    delete nextQuery.returnTo
  }
  router.replace({ query: nextQuery })
  await loadDetail()
}

const handleOrderSelect = (row) => {
  if (!row?.id) return
  openOrder(row.id)
}

const orderExists = (id) => {
  const normalizedId = Number(id)
  return Number.isFinite(normalizedId) && normalizedId > 0 && orders.value.some((order) => Number(order.id) === normalizedId)
}

const loadOrders = async () => {
  try {
    ordersLoading.value = true
    const { data } = await appClient.get("/app/order/my/list", { params: { limit: 20 } })
    if (data.code !== 200) throw new Error(data.message || "加载订单失败")
    orders.value = data.data || []
    if (!orders.value.length) {
      detail.value = null
      orderId.value = null
      return
    }
    const routeId = Number(route.query.orderId)
    const cachedRaw = localStorage.getItem("fp_last_order_id")
    const cachedId = Number(cachedRaw)
    const isValidCache = orderExists(cachedId)
    if (cachedRaw && !isValidCache) {
      localStorage.removeItem("fp_last_order_id")
    }
    const preferredId = [orderId.value, routeId, isValidCache ? cachedId : null, orders.value.length ? orders.value[0].id : null].find(
      (item) => orderExists(item)
    )
    if (preferredId) await openOrder(preferredId)
  } catch (err) {
    ElMessage.error(err.message || "加载订单失败")
  } finally {
    ordersLoading.value = false
  }
}

const loadDetail = async () => {
  if (!orderId.value) {
    ElMessage.warning("请选择订单")
    return
  }
  try {
    loading.value = true
    const { data } = await appClient.get("/app/order/detail", { params: { orderId: orderId.value } })
    if (data.code !== 200) throw new Error(data.message || "查询详情失败")
    detail.value = data.data
  } catch (err) {
    ElMessage.error(err.message || "查询详情失败")
  } finally {
    loading.value = false
  }
}

const loadFromRoute = () => {
  const raw = route.query.orderId
  if (!raw) return
  const parsed = Number(raw)
  if (!Number.isFinite(parsed) || parsed <= 0) return
  if (!orders.value.length) return
  if (!orderExists(parsed)) {
    const fallbackId = orders.value[0]?.id
    if (fallbackId) openOrder(fallbackId)
    return
  }
  orderId.value = parsed
  loadDetail()
}

const go = (path) => {
  router.push(path)
}

const backToSource = () => {
  if (sourceFrom.value === "booking") {
    router.push({ path: "/booking" })
    return
  }
  router.push({ path: "/courses", hash: "#recent-enrollment" })
}

const scrollToOrders = async () => {
  await nextTick()
  ordersRef.value?.scrollIntoView?.({ behavior: "smooth", block: "start" })
}

watch(
  () => route.query.orderId,
  () => loadFromRoute()
)

loadOrders()
</script>

<style scoped>
.orders-page {
  align-items: stretch;
}

.order-hero {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: flex-end;
  flex-wrap: wrap;
}

.orders-workspace {
  display: grid;
  grid-template-columns: minmax(0, 1.05fr) minmax(400px, 0.95fr);
  gap: 16px;
  align-items: start;
}

.orders-controls {
  display: flex;
  flex-wrap: wrap;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 16px;
}

.order-search {
  flex: 1 1 280px;
  max-width: 420px;
}

.orders-stats,
.orders-selected-chips {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.orders-selected-banner {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  padding: 18px;
  border-radius: 22px;
  border: 2px solid rgba(91, 83, 255, 0.18);
  background: linear-gradient(135deg, rgba(109, 103, 255, 0.09), rgba(255, 208, 122, 0.12));
  margin-bottom: 16px;
}

.selected-caption,
.section-eyebrow,
.order-card__eyebrow {
  margin: 0 0 8px;
  color: var(--eco-text-soft);
  font-size: 11px;
  font-weight: 800;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.orders-selected-banner strong {
  display: block;
  font-size: 20px;
  line-height: 1.2;
}

.orders-selected-banner p {
  margin: 8px 0 0;
  color: var(--eco-text-soft);
  font-size: 13px;
  line-height: 1.6;
}

.orders-scroller {
  max-height: min(74vh, 980px);
  overflow: auto;
  padding-right: 6px;
}

.orders-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.order-card {
  width: 100%;
  border: 2px solid rgba(52, 45, 105, 0.12);
  border-radius: 22px;
  background: linear-gradient(180deg, #ffffff 0%, #fffaf1 100%);
  padding: 16px;
  text-align: left;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.order-card--active {
  border-color: var(--eco-primary);
  background: linear-gradient(180deg, #f6f1ff 0%, #fffaf0 100%);
  box-shadow: 0 10px 0 rgba(42, 35, 86, 0.08), 0 18px 26px rgba(91, 83, 255, 0.12);
}

.order-card__top {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: flex-start;
}

.order-card__top strong {
  display: block;
  font-size: 18px;
  line-height: 1.35;
  word-break: break-all;
}

.order-card__amount {
  flex-shrink: 0;
  color: var(--eco-primary-strong);
  font-family: "Fredoka", "Nunito", sans-serif;
  font-size: 28px;
  line-height: 1;
  font-weight: 700;
}

.order-card__meta {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.order-card__foot {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  color: var(--eco-text-soft);
  font-size: 13px;
}

.order-card__hint {
  color: var(--eco-primary-strong);
  font-weight: 700;
}

.detail-column {
  position: sticky;
  top: 16px;
  align-self: start;
}

.detail-panel {
  max-height: calc(100vh - 32px);
  overflow: auto;
}

.detail-shell {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.detail-head {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 12px;
}

.detail-stack {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.detail-block,
.finance-card,
.reason-card,
.order-no-card,
.source-card {
  border: 2px solid rgba(52, 45, 105, 0.12);
  border-radius: 20px;
  background: #ffffff;
  padding: 16px;
}

.source-card {
  background: linear-gradient(180deg, #f8f3ff 0%, #fffaf3 100%);
}

.source-card__chips {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 12px;
}

.source-card__summary {
  margin: 12px 0 0;
  color: var(--eco-text-soft);
  line-height: 1.6;
}

.order-no-card {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: center;
}

.order-id {
  margin: 6px 0 12px;
  color: var(--eco-primary-strong);
  font-size: 26px;
  line-height: 1.1;
  font-weight: 800;
}

.order-no {
  margin: 6px 0 0;
  font-size: 20px;
  line-height: 1.25;
  font-weight: 800;
  word-break: break-all;
  overflow-wrap: anywhere;
}

.summary-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  gap: 10px;
}

.summary-item {
  border: 2px solid rgba(52, 45, 105, 0.12);
  border-radius: 16px;
  background: #fffdf8;
  padding: 14px;
}

.summary-value {
  margin: 8px 0 0;
  font-size: 24px;
  line-height: 1.15;
  font-weight: 800;
  word-break: break-word;
}

.detail-block__head {
  margin-bottom: 10px;
}

.finance-main {
  margin-top: 0;
}

.finance-sub {
  margin: 8px 0 0;
  color: var(--eco-text-soft);
  line-height: 1.6;
}

.finance-grid {
  margin-top: 12px;
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px;
}

.finance-line {
  border: 2px solid rgba(52, 45, 105, 0.12);
  border-radius: 14px;
  background: #fffdf8;
  padding: 10px 12px;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.finance-line span {
  color: var(--eco-text-soft);
  font-size: 12px;
}

.finance-line strong {
  color: var(--eco-text);
  font-size: 14px;
  line-height: 1.45;
  word-break: break-all;
}

.reason-card p {
  margin: 8px 0 0;
  color: var(--eco-text-soft);
  line-height: 1.6;
}

.detail-loading-state {
  min-height: 220px;
}

@media (max-width: 1180px) {
  .orders-workspace {
    grid-template-columns: minmax(0, 1fr) 420px;
  }
}

@media (max-width: 960px) {
  .orders-workspace {
    grid-template-columns: 1fr;
  }

  .detail-column {
    position: static;
  }

  .detail-panel {
    max-height: none;
    overflow: visible;
  }

  .orders-scroller {
    max-height: none;
    padding-right: 0;
  }
}

@media (max-width: 640px) {
  .orders-controls,
  .orders-selected-banner,
  .detail-head,
  .order-hero,
  .order-card__top,
  .order-card__foot,
  .order-no-card {
    flex-direction: column;
  }

  .order-card__amount {
    font-size: 24px;
  }

  .finance-grid {
    grid-template-columns: 1fr;
  }
}
</style>
