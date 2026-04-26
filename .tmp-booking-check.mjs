
import { computed, nextTick, reactive, ref, watch } from "vue"
import { useRoute, useRouter } from "vue-router"
import { ElMessage, ElMessageBox } from "element-plus"
import { appClient } from "../api/client"

const route = useRoute()
const router = useRouter()
const fallbackImage = import.meta.env.VITE_FALLBACK_IMAGE || "/test.png"
const schedules = ref([])
const loading = ref(false)
const submitting = ref(false)
const selected = ref(null)
const lastBooking = ref(null)
const paying = ref(false)
const completing = ref(false)
const cancelingBookingId = ref(null)
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
const coachOptions = ref([])
const coachOptionsLoading = ref(false)
const coachOptionsUnavailable = ref(false)
const coachPickerVisible = ref(false)
const coachPickerKeyword = ref("")
const scheduleSummary = ref({
  coachId: null,
  totalSchedules: 0,
  futureSchedules: 0,
  availableSchedules: 0,
  lastScheduleDate: null,
  nextScheduleDate: null
})
const coachNameCache = reactive({})
const STORAGE_BOOKING = "fp_last_booking"
const STORAGE_COACH_ID = "fp_last_coach_id"
const BOOKING_KEEP_ALIVE_MS = 60 * 60 * 1000
const BOOKING_TABS = new Set(["schedules", "bookings"])

const selectedUnavailable = computed(() => {
  if (!selected.value) return false
  return !isScheduleAvailable(selected.value)
})

const visibleSchedules = computed(() => schedules.value.filter((row) => isScheduleAvailable(row)))
const availableSchedules = computed(() => schedules.value.filter((row) => isScheduleAvailable(row)).length)
const effectiveAvailableSchedules = computed(() => {
  const count = Number(scheduleSummary.value?.availableSchedules)
  return Number.isFinite(count) ? count : availableSchedules.value
})
const hasCoachFilter = computed(() => coachId.value !== null && coachId.value !== undefined && String(coachId.value).trim() !== "")
const derivedCoachOptions = computed(() => {
  const map = new Map()
  visibleSchedules.value.forEach((row) => {
    const id = Number(row?.coachId)
    if (!Number.isFinite(id) || map.has(id)) return
    map.set(id, {
      coachId: id,
      displayName: row?.coachName || row?.coachDisplayName || `鏁欑粌妗ｆ ${id}`,
      username: row?.coachUsername || "",
      nickname: row?.coachNickname || row?.coachName || "",
      avatar: row?.coachAvatar || row?.avatar || row?.photoUrl || row?.imageUrl || "",
      expertise: row?.coachTitle || row?.specialties || row?.tags || coachPersona(id).title,
      years: row?.coachYears ?? null,
      price: row?.price ?? null,
      rating: row?.rating ?? null
    })
  })
  return Array.from(map.values()).sort((a, b) => String(a.displayName || "").localeCompare(String(b.displayName || ""), "zh-CN"))
})
const coachPickerOptions = computed(() => (coachOptions.value.length ? coachOptions.value : derivedCoachOptions.value))
const coachOptionMap = computed(() => {
  const map = new Map()
  coachPickerOptions.value.forEach((item) => map.set(Number(item.coachId), item))
  return map
})
const selectedCoachOption = computed(() => coachOptionMap.value.get(Number(coachId.value)) || null)
const selectedCoachLabel = computed(() => {
  if (selectedCoachOption.value) return coachDisplayName(selectedCoachOption.value.coachId)
  const parsedCoachId = parseCoachId(coachId.value)
  return parsedCoachId === null ? "" : coachDisplayName(parsedCoachId)
})
const filteredCoachOptions = computed(() => {
  const keyword = coachPickerKeyword.value.trim().toLowerCase()
  if (!keyword) return coachPickerOptions.value
  return coachPickerOptions.value.filter((item) => {
    return [
      item.displayName,
      item.username,
      item.nickname,
      item.expertise
    ].some((field) => String(field || "").toLowerCase().includes(keyword))
  })
})
const coachPickerNotice = computed(() => {
  if (coachOptions.value.length) {
    return "??????????????????????"
  }
  if (coachOptionsUnavailable.value) {
    return "???????????????????????????????????????"
  }
  return "??????????????????????"
})

const coachCards = computed(() => {
  const map = new Map()
  visibleSchedules.value.forEach((row) => {
    const id = Number(row?.coachId)
    if (!Number.isFinite(id)) return
    const meta = coachOptionMap.value.get(id)
    const persona = coachPersona(id)
    if (!map.has(id)) {
      map.set(id, {
        id,
        name: coachDisplayName(id),
        title: meta?.expertise || row.coachTitle || persona.title,
        tags: normalizeCoachTags(meta?.expertise || row.specialties || row.tags, persona.tags),
        photo: resolveCoachPhoto(row),
        initial: String(meta?.displayName || row.coachName || id).slice(0, 1),
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
    card.availableCount += 1
    if (!card.targetSchedule || scheduleTimeValue(row) < scheduleTimeValue(card.targetSchedule)) card.targetSchedule = row
  })
  return Array.from(map.values())
    .map((card) => {
      const schedule = card.targetSchedule
      return {
        ...card,
        nextTime: formatScheduleTime(schedule),
        minPrice: card.minPrice ?? "-"
      }
    })
    .filter((card) => !!card.targetSchedule)
    .sort((a, b) => b.availableCount - a.availableCount || scheduleTimeValue(a.targetSchedule) - scheduleTimeValue(b.targetSchedule))
})

const scheduleEmptyReason = computed(() => {
  if (date.value && effectiveAvailableSchedules.value > 0) {
    return 
`???? ${date.value} ???????????????? ${effectiveAvailableSchedules.value} ??????????`
  }
  if (!scheduleSummary.value?.futureSchedules) {
    const lastDate = formatDate(scheduleSummary.value?.lastScheduleDate)
    return lastDate === "-"
      ? "???????????????????????????????"
      : 
`?????????????????????????? ${lastDate}??????????????`
  }
  if (!effectiveAvailableSchedules.value) {
    return "????????????????????????????????????"
  }
  return "??????????????????????????????"
})

const scheduleSummaryHint = computed(() => {
  const nextDate = formatDate(scheduleSummary.value?.nextScheduleDate)
  if (nextDate !== "-") {
    return 
`???????????? ${nextDate} ????????????????????????`
  }
  return "??????????????????????????"
})

const createBookingHint = computed(() => {
  if (lastBooking.value?.bookingStatus === "WAIT_PAY") {
    return "????????????????????????????"
  }
  if (!visibleSchedules.value.length) {
    return "??????????????????????????"
  }
  return "? ????????????????"
})

const isMissingEndpointMessage = (message) => String(message || "").includes("?????")

const parseCoachId = (value) => {
  if (value === null || value === undefined || value === "") return null
  const parsed = Number(value)
  return Number.isFinite(parsed) ? parsed : null
}

const coachPersonas = [
  { title: "????", tags: ["??", "??", "1v1"] },
  { title: "????", tags: ["??", "??", "??"] },
  { title: "????", tags: ["??", "??", "??"] },
  { title: "????", tags: ["??", "???", "???"] }
]

const coachPersona = (id) => coachPersonas[Math.abs(Math.trunc(Number(id) || 0)) % coachPersonas.length]

const normalizeCoachTags = (value, fallback) => {
  if (Array.isArray(value)) return value.filter(Boolean).slice(0, 3)
  if (typeof value === "string" && value.trim()) return value.split(/[、，,\s]+/).filter(Boolean).slice(0, 3)
  return fallback
}

const normalizeCoachName = (value) => {
  const text = String(value || "").trim()
  if (!text || text === "-" || text === "教练") return ""
  if (/^[?？]+$/u.test(text)) return ""
  if (/^教练档案\s*\d+$/u.test(text)) return ""
  return text
}

const rememberCoachName = (coachIdValue, ...candidates) => {
  const id = Number(coachIdValue)
  if (!Number.isFinite(id)) return ""
  const name = candidates.map((item) => normalizeCoachName(item)).find(Boolean) || ""
  if (!name) return ""
  coachNameCache[id] = name
  return name
}

const rememberCoachNamesFromOptions = (items = []) => {
  items.forEach((item) => {
    rememberCoachName(item?.coachId, item?.displayName, item?.nickname, item?.username)
  })
}

const rememberCoachNamesFromRows = (rows = []) => {
  rows.forEach((row) => {
    rememberCoachName(row?.coachId, row?.coachName, row?.coachDisplayName, row?.coachNickname, row?.coachUsername)
  })
}

const resolveCoachPhoto = (row) => {
  const meta = coachOptionMap.value.get(Number(row?.coachId))
  return meta?.avatar || row?.coachAvatar || row?.avatar || row?.photoUrl || row?.imageUrl || fallbackImage
}

const coachDisplayName = (coachIdValue) => {
  const id = Number(coachIdValue)
  if (!Number.isFinite(id)) return "-"
  if (coachNameCache[id]) return coachNameCache[id]
  const coach = coachOptionMap.value.get(id)
  const cached = rememberCoachName(id, coach?.displayName, coach?.nickname, coach?.username)
  if (cached) return cached
  return `鏁欑粌妗ｆ ${id}`
}

const bookingCoachDisplayName = (row) => {
  if (!row) return "-"
  const inline = rememberCoachName(row.coachId, row.coachName, row.coachDisplayName, row.coachNickname, row.coachUsername)
  return inline || coachDisplayName(row.coachId)
}

const cachedCoachDisplayName = (coachIdValue) => {
  const id = Number(coachIdValue)
  if (!Number.isFinite(id)) return ""
  if (coachNameCache[id]) return coachNameCache[id]
  const coach = coachOptionMap.value.get(id)
  return rememberCoachName(id, coach?.displayName, coach?.nickname, coach?.username)
}

const hydrateBookingCoachName = (item) => {
  const normalized = toRecentBooking(item)
  if (!normalized) return null
  const inline = rememberCoachName(
    normalized.coachId,
    normalized.coachName,
    normalized.coachDisplayName,
    normalized.coachNickname,
    normalized.coachUsername
  )
  if (inline) return normalized
  const currentBookingId = Number(lastBooking.value?.bookingId || lastBooking.value?.id)
  const targetBookingId = Number(normalized.bookingId || normalized.id)
  const currentCoachName = currentBookingId === targetBookingId
    ? rememberCoachName(
        normalized.coachId,
        lastBooking.value?.coachName,
        lastBooking.value?.coachDisplayName,
        lastBooking.value?.coachNickname,
        lastBooking.value?.coachUsername
      )
    : ""
  const cached = currentCoachName || cachedCoachDisplayName(normalized.coachId)
  return cached ? { ...normalized, coachName: cached } : normalized
}

const ensureSelectedCoachName = async () => {
  if (!selected.value) return ""
  let name = rememberCoachName(
    selected.value.coachId,
    selected.value.coachName,
    selected.value.coachDisplayName,
    selectedCoachOption.value?.displayName,
    selectedCoachOption.value?.nickname,
    selectedCoachOption.value?.username
  )
  if (name) return name
  if (!coachOptions.value.length && !coachOptionsLoading.value) {
    await loadCoachOptions()
    name = rememberCoachName(
      selected.value.coachId,
      selected.value.coachName,
      selected.value.coachDisplayName,
      selectedCoachOption.value?.displayName,
      selectedCoachOption.value?.nickname,
      selectedCoachOption.value?.username
    )
  }
  return name || cachedCoachDisplayName(selected.value.coachId)
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

const clearLastBooking = () => {
  lastBooking.value = null
  localStorage.removeItem(STORAGE_BOOKING)
}

const persistLastBooking = () => {
  if (!lastBooking.value) {
    localStorage.removeItem(STORAGE_BOOKING)
    return
  }
  localStorage.setItem(STORAGE_BOOKING, JSON.stringify(lastBooking.value))
}

const isUrgentBooking = (item) => ["WAIT_PAY", "PAID"].includes(item?.bookingStatus)

const canMockPay = (item) => item?.bookingStatus === "WAIT_PAY" && item?.payStatus === "UNPAID"
const canCancelBooking = (item) => item?.bookingStatus === "WAIT_PAY" && item?.payStatus === "UNPAID"
const canCompleteBooking = (item) => item?.bookingStatus === "PAID" && item?.payStatus === "PAID"
const isCancellingBooking = (item) => Number(cancelingBookingId.value) === Number(item?.bookingId || item?.id)

const isRecentPassiveBooking = (item) => {
  if (!["COMPLETED", "CANCELLED"].includes(item?.bookingStatus)) return false
  const createdAt = new Date(item?.createTime || 0).getTime()
  if (!Number.isFinite(createdAt) || createdAt <= 0) return false
  return Date.now() - createdAt <= BOOKING_KEEP_ALIVE_MS
}

const syncLastBooking = (rows) => {
  const normalizedRows = (rows || []).map((item) => hydrateBookingCoachName(item)).filter(Boolean)
  if (!normalizedRows.length) {
    clearLastBooking()
    return
  }

  const urgentBooking = normalizedRows.find((item) => isUrgentBooking(item))
  if (urgentBooking) {
    lastBooking.value = urgentBooking
    persistLastBooking()
    return
  }

  const passiveBooking = normalizedRows.find((item) => ["COMPLETED", "CANCELLED"].includes(item?.bookingStatus))
  if (passiveBooking && isRecentPassiveBooking(passiveBooking)) {
    lastBooking.value = passiveBooking
    persistLastBooking()
    return
  }

  clearLastBooking()
}

const loadScheduleSummary = async () => {
  try {
    const params = {}
    const parsedCoachId = parseCoachId(coachId.value)
    if (parsedCoachId !== null) {
      params.coachId = parsedCoachId
    }
    const { data } = await appClient.get("/app/booking/schedule/summary", { params })
    if (data.code !== 200) throw new Error(data.message || "???????????")
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

const loadCoachOptions = async () => {
  try {
    coachOptionsLoading.value = true
    const { data } = await appClient.get("/app/booking/coach-options")
    if (data.code !== 200) throw new Error(data.message || "????????")
    coachOptions.value = data.data || []
    rememberCoachNamesFromOptions(coachOptions.value)
    coachOptionsUnavailable.value = false
  } catch (err) {
    coachOptions.value = []
    coachOptionsUnavailable.value = true
    if (!isMissingEndpointMessage(err.message)) {
      ElMessage.error(err.message || "????????")
    }
  } finally {
    coachOptionsLoading.value = false
  }
}

const refreshBookingWorkspace = async ({ keepSelection = true } = {}) => {
  await Promise.all([
    loadCoachOptions(),
    loadSchedules({ keepSelection }),
    loadMyBookings()
  ])
}

const loadSchedules = async ({ keepSelection = true } = {}) => {
  try {
    loading.value = true
    const currentSelectedId = keepSelection ? Number(selected.value?.id) : null
    const params = {}
    const parsedCoachId = parseCoachId(coachId.value)
    if (coachId.value !== null && coachId.value !== undefined && coachId.value !== "") {
      params.coachId = parsedCoachId
    }
    if (date.value) params.date = date.value
    const { data } = await appClient.get("/app/booking/schedule/list", { params })
    if (data.code !== 200) throw new Error(data.message || "鍔犺浇鍙绾︽椂闂存澶辫触")
    schedules.value = data.data || []
    rememberCoachNamesFromRows(schedules.value)
    const nextAvailable = schedules.value.find((item) => isScheduleAvailable(item)) || null
    if (!keepSelection) {
      selected.value = null
    } else if (currentSelectedId && !schedules.value.some((item) => Number(item.id) === currentSelectedId && isScheduleAvailable(item))) {
      selected.value = null
    }
    if (!selected.value && lastBooking.value?.bookingStatus !== "WAIT_PAY") {
      selected.value = nextAvailable
    }
    if (parsedCoachId !== null) localStorage.setItem(STORAGE_COACH_ID, String(parsedCoachId))
    await loadScheduleSummary()
  } catch (err) {
    ElMessage.error(err.message || "鍔犺浇鍙绾︽椂闂存澶辫触")
  } finally {
    loading.value = false
  }
}

const setCoach = (value) => {
  coachId.value = value
  if (value === null || value === undefined || value === "") localStorage.removeItem(STORAGE_COACH_ID)
  else localStorage.setItem(STORAGE_COACH_ID, String(value))
}

const openCoachPicker = async () => {
  coachPickerVisible.value = true
  if (!coachOptions.value.length && !coachOptionsLoading.value) {
    await loadCoachOptions()
  }
}

const selectCoachOption = async (coach) => {
  setCoach(coach?.coachId ?? null)
  coachPickerVisible.value = false
  await loadSchedules()
}

const bookCoach = async (coach) => {
  if (!coach?.targetSchedule) {
    ElMessage.warning("?????????????")
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
    ElMessage.info("?????????????????????")
  }
  await nextTick()
  reviewSectionRef.value?.scrollIntoView?.({ behavior: "smooth", block: "start" })
}

const selectSchedule = (row) => {
  if (!isScheduleAvailable(row)) {
    ElMessage.warning("???????????????????")
    return
  }
  selected.value = row
  scrollToSelectedSchedule()
}

const createBooking = async () => {
  if (!selected.value?.id) {
    ElMessage.warning("????????????")
    return
  }
  try {
    submitting.value = true
    const { data } = await appClient.post("/app/booking/create", { scheduleId: selected.value.id })
    if (data.code !== 200) throw new Error(data.message || "????")
    ElMessage.success("????")
    const selectedCoachName = rememberCoachName(
      selected.value.coachId,
      selected.value.coachName,
      selected.value.coachDisplayName,
      selectedCoachOption.value?.displayName,
      selectedCoachOption.value?.nickname,
      selectedCoachOption.value?.username
    )
    lastBooking.value = toRecentBooking({
      ...data.data,
      coachId: selected.value.coachId,
      coachName: selectedCoachName || coachDisplayName(selected.value.coachId),
      scheduleDate: selected.value.scheduleDate,
      startTime: selected.value.startTime,
      endTime: selected.value.endTime,
      bookingStatus: "WAIT_PAY",
      payStatus: "UNPAID"
    })
    if (data.data?.bookingId) {
      persistLastBooking()
      localStorage.setItem("fp_last_order_id", String(data.data.orderId || ""))
    }
    selected.value = null
    await refreshBookingWorkspace({ keepSelection: false })
  } catch (err) {
    ElMessage.error(err.message || "????")
  } finally {
    submitting.value = false
  }
}

const loadLastBooking = () => {
  try {
    const cached = localStorage.getItem(STORAGE_BOOKING)
    if (!cached) {
      lastBooking.value = null
      return
    }
    const normalized = toRecentBooking(JSON.parse(cached))
    rememberCoachName(normalized?.coachId, normalized?.coachName, normalized?.coachDisplayName, normalized?.coachNickname, normalized?.coachUsername)
    if (isUrgentBooking(normalized) || isRecentPassiveBooking(normalized)) {
      lastBooking.value = normalized
      return
    }
    clearLastBooking()
  } catch {
    clearLastBooking()
  }
}

const mockPay = async () => {
  if (!lastBooking.value?.bookingId) {
    ElMessage.warning("???????")
    return
  }
  try {
    paying.value = true
    const { data } = await appClient.post("/app/booking/pay-success", null, {
      params: { bookingId: lastBooking.value.bookingId }
    })
    if (data.code !== 200) throw new Error(data.message || "??????")
    ElMessage.success("???????")
    await loadMyBookings()
  } catch (err) {
    ElMessage.error(err.message || "??????")
  } finally {
    paying.value = false
  }
}

const completeBooking = async () => {
  if (!lastBooking.value?.bookingId) {
    ElMessage.warning("???????")
    return
  }
  try {
    completing.value = true
    const { data } = await appClient.post("/app/booking/complete", null, {
      params: { bookingId: lastBooking.value.bookingId }
    })
    if (data.code !== 200) throw new Error(data.message || "????")
    ElMessage.success("?????")
    await loadMyBookings()
  } catch (err) {
    ElMessage.error(err.message || "????")
  } finally {
    completing.value = false
  }
}

const cancelBooking = async (row = lastBooking.value) => {
  const bookingId = Number(row?.bookingId || row?.id)
  if (!Number.isFinite(bookingId) || bookingId <= 0) {
    ElMessage.warning("??????????")
    return
  }
  if (!canCancelBooking(row)) {
    ElMessage.warning("??????????")
    return
  }
  try {
    await ElMessageBox.confirm(
      "???????????????????????????????",
      "??????",
      {
        confirmButtonText: "????",
        cancelButtonText: "???",
        type: "warning"
      }
    )
  } catch {
    return
  }

  try {
    cancelingBookingId.value = bookingId
    const isCurrentLastBooking = Number(lastBooking.value?.bookingId || lastBooking.value?.id) === bookingId
    if (isCurrentLastBooking) {
      clearLastBooking()
    }
    if (Number(selectedReviewBooking.value?.id) === bookingId) {
      selectedReviewBooking.value = null
      reviewPanelVisible.value = false
      reviewForm.bookingId = ""
      reviewForm.content = ""
    }
    const { data } = await appClient.post("/app/booking/cancel-unpaid", null, {
      params: { bookingId }
    })
    if (data.code !== 200) throw new Error(data.message || "??????")
    ElMessage.success("??????????????")
    selected.value = null
    await refreshBookingWorkspace({ keepSelection: false })
  } catch (err) {
    ElMessage.error(err.message || "??????")
    await loadMyBookings()
  } finally {
    cancelingBookingId.value = null
  }
}

const loadMyBookings = async () => {
  try {
    myLoading.value = true
    const { data } = await appClient.get("/app/booking/my/list")
    if (data.code !== 200) throw new Error(data.message || "????????")
    myBookings.value = (data.data || []).map((item) => toRecentBooking(item))
    rememberCoachNamesFromRows(myBookings.value)
    syncLastBooking(myBookings.value)
    if (!selectedReviewBooking.value) fillReviewFromCompleted(false)
  } catch (err) {
    ElMessage.error(err.message || "????????")
  } finally {
    myLoading.value = false
  }
}

const fillReviewFromCompleted = (showMessage = true) => {
  const target = myBookings.value.find((row) => row.bookingStatus === "COMPLETED")
  if (!target) {
    if (showMessage) ElMessage.warning("???????????")
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
  reviewForm.content = "?????????????????????"
}

const openReview = (row) => {
  if (row.bookingStatus !== "COMPLETED") {
    ElMessage.warning("??????????")
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
    ElMessage.warning("??????")
    return
  }
  if (!reviewForm.content.trim()) {
    ElMessage.warning("???????")
    return
  }
  try {
    reviewing.value = true
    const { data } = await appClient.post("/app/booking/review", {
      bookingId: Number(reviewForm.bookingId),
      score: reviewForm.score,
      content: reviewForm.content.trim()
    })
    if (data.code !== 200) throw new Error(data.message || "????")
    ElMessage.success("????")
    reviewForm.content = ""
    await loadMyBookings()
  } catch (err) {
    ElMessage.error(err.message || "????")
  } finally {
    reviewing.value = false
  }
}

const goToOrder = (payload) => {
  const orderId = typeof payload === "object" && payload !== null ? payload.orderId : payload
  if (!orderId) return
  const query = {
    orderId: String(orderId),
    from: "booking",
    bookingTab: activeBookingTab.value
  }
  if (coachId.value !== null && coachId.value !== undefined && String(coachId.value).trim() !== "") query.coachId = String(coachId.value)
  if (date.value) query.date = String(date.value)
  if (payload && typeof payload === "object") {
    if (payload.id || payload.bookingId) query.bookingId = String(payload.id || payload.bookingId)
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
  const map = { WAIT_PAY: "???", PAID: "???", COMPLETED: "???", CANCELLED: "???" }
  return map[status] || status || "-"
}

const formatPayStatus = (status) => {
  const map = { UNPAID: "???", PAID: "???", CLOSED: "???", REFUNDED: "???" }
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
  if (row.status !== null && row.status !== undefined && Number(row.status) !== 1) return { text: "??", type: "info" }
  const start = resolveScheduleStart(row)
  if (start && start.getTime() < Date.now() - 60 * 1000) return { text: "???", type: "warning" }
  if (remainingSlots(row) <= 0) return { text: "??", type: "danger" }
  return { text: "???", type: "success" }
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

const restoreBookingWorkspace = () => {
  loadCoachId()
  const routeCoachId = parseCoachId(route.query.coachId)
  if (routeCoachId !== null) coachId.value = routeCoachId
  const routeDate = typeof route.query.date === "string" ? route.query.date.trim() : ""
  date.value = routeDate
  const routeTab = typeof route.query.bookingTab === "string" ? route.query.bookingTab : ""
  if (BOOKING_TABS.has(routeTab)) {
    activeBookingTab.value = routeTab
  }
}

const buildBookingRouteQuery = () => {
  const query = {}
  if (BOOKING_TABS.has(activeBookingTab.value)) query.bookingTab = activeBookingTab.value
  const parsedCoachId = parseCoachId(coachId.value)
  if (parsedCoachId !== null) query.coachId = String(parsedCoachId)
  if (date.value) query.date = String(date.value)
  return query
}

const syncBookingRouteQuery = async () => {
  if (route.path !== "/booking") return
  const nextQuery = buildBookingRouteQuery()
  const currentBookingTab = typeof route.query.bookingTab === "string" ? route.query.bookingTab : ""
  const currentCoachId = typeof route.query.coachId === "string" ? route.query.coachId : ""
  const currentDate = typeof route.query.date === "string" ? route.query.date : ""
  const nextBookingTab = typeof nextQuery.bookingTab === "string" ? nextQuery.bookingTab : ""
  const nextCoachId = typeof nextQuery.coachId === "string" ? nextQuery.coachId : ""
  const nextDate = typeof nextQuery.date === "string" ? nextQuery.date : ""
  if (currentBookingTab === nextBookingTab && currentCoachId === nextCoachId && currentDate === nextDate) return
  await router.replace({ path: "/booking", query: nextQuery })
}

watch([activeBookingTab, coachId, date], () => {
  syncBookingRouteQuery().catch(() => {})
})

restoreBookingWorkspace()
loadCoachOptions()
loadSchedules()
loadMyBookings()
loadLastBooking()
