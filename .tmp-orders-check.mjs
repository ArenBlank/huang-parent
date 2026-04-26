
import { computed, nextTick, ref, watch } from "vue"
import { useRoute, useRouter } from "vue-router"
import { ElMessage, ElMessageBox } from "element-plus"
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
const cancelingBooking = ref(false)

const sourceFrom = computed(() => String(route.query.from || ""))
const showBackToSource = computed(() => sourceFrom.value === "courses" || sourceFrom.value === "booking")
const backToSourceLabel = computed(() => (sourceFrom.value === "booking" ? "??????" : "??????"))
const sourceEntityLabel = computed(() => (sourceFrom.value === "booking" ? "??ID" : "??ID"))
const sourceSummaryLabel = computed(() => (sourceFrom.value === "booking" ? "??" : "??"))
const bookingReturnQuery = computed(() => {
  const query = {}
  if (route.query.bookingTab) query.bookingTab = String(route.query.bookingTab)
  if (route.query.coachId) query.coachId = String(route.query.coachId)
  if (route.query.date) query.date = String(route.query.date)
  return query
})

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
    const timeText = scheduleDate ? scheduleDate + " " + (startTime || "--:--") + "-" + (endTime || "--:--") : ""
    return {
      sourceLabel: "????",
      courseTitle: timeText,
      enrollmentId: Number.isFinite(Number(bookingId)) && Number(bookingId) > 0 ? Number(bookingId) : null,
      orderId: normalized.value.orderId || (route.query.orderId ? Number(route.query.orderId) : null)
    }
  }

  const courseTitle = route.query.courseTitle ? String(route.query.courseTitle) : ""
  const enrollmentId = route.query.enrollmentId ? Number(route.query.enrollmentId) : normalized.value.bizId
  return {
    sourceLabel: "????",
    courseTitle,
    enrollmentId: Number.isFinite(Number(enrollmentId)) && Number(enrollmentId) > 0 ? Number(enrollmentId) : null,
    orderId: normalized.value.orderId || (route.query.orderId ? Number(route.query.orderId) : null)
  }
})

const currentBookingId = computed(() => {
  if (normalized.value.bizType !== "coach_booking") return null
  const raw = sourceFrom.value === "booking" ? sourceContext.value?.enrollmentId : normalized.value.bizId
  const parsed = Number(raw)
  return Number.isFinite(parsed) && parsed > 0 ? parsed : null
})

const canCancelCurrentBooking = computed(() => {
  if (!currentBookingId.value) return false
  if (normalized.value.bizType !== "coach_booking") return false
  if (normalized.value.payStatus !== "UNPAID") return false
  return !["CLOSED", "REFUNDED"].includes(normalized.value.orderStatus)
})

const formatPayStatus = (status) => {
  const map = { UNPAID: "???", PAID: "???", CLOSED: "???", REFUNDED: "???" }
  return map[status] || status || "-"
}

const formatOrderStatus = (status) => {
  const map = {
    NEW: "??",
    UNPAID: "???",
    PAID: "???",
    CLOSED: "???",
    CANCELLED: "???",
    REFUNDED: "???"
  }
  return map[status] || status || "-"
}

const formatPayChannel = (channel) => {
  const map = { wechat: "??", alipay: "???", mock: "??" }
  return map[channel] || channel || "-"
}

const formatBizType = (value) => {
  const map = { coach_booking: "????", course: "????", course_enrollment: "????" }
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
    refunded: "???",
    order_refunded: "?????",
    REFUND: "????",
    Refunded: "???",
    "Refund completed": "?????",
    refunded_order: "????",
    paid: "???",
    Paid: "???",
    "Paid (awaiting service)": "?????????",
    "Payment received": "???????",
    unpaid: "???",
    Unpaid: "???",
    CLOSED: "???",
    closed: "???",
    Closed: "???",
    "Order closed": "?????",
    order_unknown: "??????",
    Unknown: "????",
    "Unknown status": "??????????",
    NONE: "???",
    PENDING: "???",
    REJECTED: "???"
  }
  return map[raw] || raw
}

const formatFinanceDisplay = (value, refundStatus, payStatus, orderStatus) => {
  const raw = String(value || "").trim()
  const map = {
    Refunded: "???",
    "Paid (awaiting service)": "???",
    Closed: "???",
    Unpaid: "???",
    Unknown: "????"
  }
  if (map[raw]) return map[raw]
  if (refundStatus === "REFUNDED") return "???"
  if (payStatus === "PAID") return "???"
  if (orderStatus === "CLOSED") return "???"
  if (payStatus === "UNPAID") return "???"
  return formatValue(value)
}

const formatFinanceExplain = (value, refundStatus, payStatus, orderStatus) => {
  const raw = String(value || "").trim()
  const map = {
    "Refund completed": "?????",
    "Payment received": "???????",
    "Order closed": "?????",
    "Awaiting payment": "????",
    "Unknown status": "??????????"
  }
  if (map[raw]) return map[raw]
  if (refundStatus === "REFUNDED") return "?????"
  if (payStatus === "PAID") return "???????"
  if (orderStatus === "CLOSED") return "?????"
  if (payStatus === "UNPAID") return "????"
  return formatValue(value)
}

const formatDateTime = (value) => {
  if (value === null || value === undefined || value === "") return "-"
  const raw = String(value)
  if (raw.includes("T")) {
    const [date, time] = raw.split("T")
    return date + " " + time.slice(0, 8)
  }
  if (raw.length >= 16 && raw.includes("-")) return raw.slice(0, 16)
  return raw
}

const formatAmount = (value) => {
  if (value === null || value === undefined || value === "") return "-"
  const amount = Number(value)
  return Number.isFinite(amount) ? amount.toFixed(2) : String(value)
}

const copyOrderNo = async () => {
  if (!normalized.value.orderNo || normalized.value.orderNo === "-") {
    ElMessage.warning("???????????")
    return
  }
  try {
    await navigator.clipboard.writeText(String(normalized.value.orderNo))
    ElMessage.success("??????")
  } catch {
    ElMessage.error("??????????")
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
    if (data.code !== 200) throw new Error(data.message || "??????")
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
    ElMessage.error(err.message || "??????")
  } finally {
    ordersLoading.value = false
  }
}

const loadDetail = async () => {
  if (!orderId.value) {
    ElMessage.warning("?????")
    return
  }
  try {
    loading.value = true
    const { data } = await appClient.get("/app/order/detail", { params: { orderId: orderId.value } })
    if (data.code !== 200) throw new Error(data.message || "??????")
    detail.value = data.data
  } catch (err) {
    ElMessage.error(err.message || "??????")
  } finally {
    loading.value = false
  }
}

const cancelCurrentBooking = async () => {
  if (!canCancelCurrentBooking.value) {
    ElMessage.warning("???????????")
    return
  }
  try {
    await ElMessageBox.confirm(
      "??????????????????????????????",
      "??????",
      {
        confirmButtonText: "????",
        cancelButtonText: "????",
        type: "warning"
      }
    )
  } catch {
    return
  }

  try {
    cancelingBooking.value = true
    const { data } = await appClient.post("/app/booking/cancel-unpaid", null, {
      params: { bookingId: currentBookingId.value }
    })
    if (data.code !== 200) throw new Error(data.message || "??????")
    ElMessage.success("???????????")
    await loadOrders()
  } catch (err) {
    ElMessage.error(err.message || "??????")
  } finally {
    cancelingBooking.value = false
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
    router.push({ path: "/booking", query: bookingReturnQuery.value })
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
