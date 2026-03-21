<template>
  <div class="card">
    <div class="toolbar">
      <div>
        <h2 class="section-title">课程列表</h2>
        <p class="section-sub">选择课程查看排期并报名</p>
      </div>
      <el-button type="primary" @click="loadCourses" :loading="loading">刷新</el-button>
    </div>

    <el-table
      v-if="courses.length"
      :data="courses"
      v-loading="loading"
      style="width: 100%"
      @row-click="selectCourse"
      :row-class-name="courseRowClass"
      highlight-current-row
      ref="courseTableRef"
    >
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="title" label="课程名称" />
      <el-table-column prop="price" label="价格" width="120" />
      <el-table-column label="状态" width="120">
        <template #default="{ row }">
          {{ formatCourseStatus(row.status) }}
        </template>
      </el-table-column>
    </el-table>
    <el-empty v-else :description="loading ? '正在加载课程...' : '暂无课程，可在管理端创建并上架课程'">
      <div class="empty-actions">
        <el-button size="small" @click="loadCourses">刷新</el-button>
        <el-button size="small" @click="goTo('/booking')">去教练预约</el-button>
      </div>
    </el-empty>
  </div>

  <div class="card" style="margin-top: 16px;">
    <div class="toolbar">
      <div>
        <h2 class="section-title">课程排期</h2>
        <p class="section-sub">点击上表课程加载排期</p>
      </div>
    </div>
    <el-empty v-if="!selectedCourse" description="请选择课程" />
    <el-alert
      v-if="selectedCourse && !scheduleLoading && schedules.length && !availableSchedules"
      type="warning"
      show-icon
      style="margin-bottom: 12px"
      title="该课程暂无可报名的排期"
    />
    <el-empty
      v-if="selectedCourse && !scheduleLoading && !schedules.length"
      description="暂无排期，可在管理端为该课程创建排期"
    >
      <div class="empty-actions">
        <el-button size="small" @click="loadCourses">刷新课程</el-button>
        <el-button size="small" @click="goTo('/booking')">去教练预约</el-button>
      </div>
    </el-empty>
    <el-table
      v-else
      :data="schedules"
      v-loading="scheduleLoading"
      style="width: 100%"
      @row-click="selectSchedule"
      :row-class-name="scheduleRowClass"
      highlight-current-row
      ref="scheduleTableRef"
    >
      <el-table-column prop="id" label="排期ID" width="80" />
      <el-table-column label="日期">
        <template #default="{ row }">
          {{ formatDate(row.scheduleDate || row.startTime) }}
        </template>
      </el-table-column>
      <el-table-column label="开始" width="120">
        <template #default="{ row }">
          {{ formatTime(row.startTime) }}
        </template>
      </el-table-column>
      <el-table-column label="结束" width="120">
        <template #default="{ row }">
          {{ formatTime(row.endTime) }}
        </template>
      </el-table-column>
      <el-table-column prop="coachId" label="教练ID" width="90" />
      <el-table-column prop="price" label="价格" width="120" />
      <el-table-column prop="capacity" label="容量" width="90" />
      <el-table-column prop="bookedCount" label="已约" width="90" />
      <el-table-column label="余量" width="90">
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

  <div class="card" style="margin-top: 16px;">
    <div class="toolbar">
      <div>
        <h2 class="section-title">报名</h2>
        <p class="section-sub">从排期列表选择后提交</p>
      </div>
      <el-button
        type="success"
        :disabled="!selectedSchedule || selectedUnavailable"
        :loading="enrolling"
        @click="enroll"
      >
        提交报名
      </el-button>
    </div>
    <el-empty v-if="!selectedSchedule" description="请选择排期" />
    <div v-else class="detail">
      <div>排期ID：{{ selectedSchedule.id }}</div>
      <div class="muted">{{ formatScheduleTime(selectedSchedule) }}</div>
    </div>
  </div>

  <div class="card" style="margin-top: 16px;">
    <div class="toolbar">
      <div>
        <h2 class="section-title">最近报名</h2>
        <p class="section-sub">便捷模拟支付、取消或退款</p>
      </div>
    </div>
    <el-empty v-if="!lastEnrollment" description="暂无最近报名">
      <div class="empty-actions">
        <el-button size="small" @click="loadEnrollments">刷新报名</el-button>
      </div>
    </el-empty>
    <div v-else class="detail">
      <div>报名ID：{{ lastEnrollment.enrollmentId }}</div>
      <div>订单ID：{{ lastEnrollment.orderId }}</div>
      <div class="muted">订单号：{{ lastEnrollment.orderNo }}</div>
      <div class="muted">支付号：{{ lastEnrollment.payNo }}</div>
      <div class="muted">金额：{{ lastEnrollment.amount }}</div>
      <el-input v-model="refundReason" size="small" class="input-inline" placeholder="退款原因" />
      <div class="action-row">
        <el-button type="primary" size="small" :loading="paying" @click="mockPay">模拟支付</el-button>
        <el-button type="warning" size="small" :loading="canceling" @click="cancelUnpaid">取消未支付</el-button>
        <el-button type="danger" size="small" :loading="refunding" @click="refundPaid">发起退款</el-button>
        <el-button
          size="small"
          :disabled="!lastEnrollment?.orderId"
          @click="goToOrder(lastEnrollment?.orderId)"
        >
          查看订单
        </el-button>
      </div>
    </div>
  </div>

  <div class="card" style="margin-top: 16px;">
    <div class="toolbar">
      <div>
        <h2 class="section-title">我的报名</h2>
        <p class="section-sub">查看已报名课程</p>
      </div>
      <el-button type="primary" @click="loadEnrollments" :loading="enrollmentsLoading">刷新</el-button>
    </div>
    <el-table v-if="enrollments.length" :data="enrollments" v-loading="enrollmentsLoading" style="width: 100%">
      <el-table-column prop="id" label="报名ID" width="80" />
      <el-table-column label="课程" min-width="140">
        <template #default="{ row }">
          {{ courseMap[row.courseId] || row.courseId }}
        </template>
      </el-table-column>
      <el-table-column label="订单" width="120">
        <template #default="{ row }">
          <el-button size="small" text :disabled="!row.orderId" @click="goToOrder(row.orderId)">查看</el-button>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="120">
        <template #default="{ row }">
          {{ formatEnrollmentStatus(row.status) }}
        </template>
      </el-table-column>
      <el-table-column label="创建时间" min-width="160">
        <template #default="{ row }">
          {{ formatDateTime(row.createTime || row.enrollTime) }}
        </template>
      </el-table-column>
    </el-table>
    <el-empty v-else :description="enrollmentsLoading ? '正在加载报名...' : '暂无报名记录，可先报名课程'">
      <div class="empty-actions">
        <el-button size="small" @click="loadEnrollments">刷新报名</el-button>
      </div>
    </el-empty>
  </div>
</template>

<script setup>
import { computed, nextTick, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { appClient } from '../api/client'

const router = useRouter()
const courses = ref([])
const loading = ref(false)
const selectedCourse = ref(null)
const courseTableRef = ref(null)
const scheduleTableRef = ref(null)

const schedules = ref([])
const scheduleLoading = ref(false)
const selectedSchedule = ref(null)

const enrollments = ref([])
const enrollmentsLoading = ref(false)
const enrolling = ref(false)

const lastEnrollment = ref(null)
const paying = ref(false)
const canceling = ref(false)
const refunding = ref(false)
const refundReason = ref('临时有事，申请退款')
const STORAGE_ENROLLMENT = 'fp_last_enrollment'
const STORAGE_COURSE_ID = 'fp_last_course_id'

const courseMap = computed(() => {
  const map = {}
  courses.value.forEach((course) => {
    map[course.id] = course.title
  })
  return map
})

const selectedFull = computed(() => {
  if (!selectedSchedule.value) return false
  return remainingSlots(selectedSchedule.value) <= 0
})

const selectedUnavailable = computed(() => {
  if (!selectedSchedule.value) return false
  return !isScheduleAvailable(selectedSchedule.value)
})

const availableSchedules = computed(() => {
  if (!schedules.value.length) return 0
  return schedules.value.filter((row) => isScheduleAvailable(row)).length
})

const parseCourseId = (value) => {
  if (value === null || value === undefined || value === '') return null
  const parsed = Number(value)
  return Number.isFinite(parsed) ? parsed : null
}

const loadCourses = async () => {
  try {
    loading.value = true
    const { data } = await appClient.get('/app/course/list')
    if (data.code !== 200) throw new Error(data.message || '加载失败')
    courses.value = data.data || []
    if (courses.value.length) {
      const cached = parseCourseId(localStorage.getItem(STORAGE_COURSE_ID))
      const preferred =
        (cached !== null && courses.value.find((item) => Number(item.id) === cached)) || courses.value[0]
      if (!selectedCourse.value || Number(selectedCourse.value.id) !== Number(preferred.id)) {
        await selectCourse(preferred)
      }
    } else {
      selectedCourse.value = null
      schedules.value = []
      selectedSchedule.value = null
    }
  } catch (err) {
    ElMessage.error(err.message || '加载失败')
  } finally {
    loading.value = false
  }
}

const selectCourse = async (row) => {
  selectedCourse.value = row
  if (row?.id !== undefined && row?.id !== null) {
    localStorage.setItem(STORAGE_COURSE_ID, String(row.id))
  }
  await scrollToSelectedCourse()
  selectedSchedule.value = null
  schedules.value = []
  try {
    scheduleLoading.value = true
    const { data } = await appClient.get(`/app/course/${row.id}/schedule/list`)
    if (data.code !== 200) throw new Error(data.message || '加载排期失败')
    schedules.value = data.data || []
    if (schedules.value.length) {
      selectedSchedule.value = schedules.value.find((item) => isScheduleAvailable(item)) || schedules.value[0]
      await scrollToSelectedSchedule()
    }
  } catch (err) {
    ElMessage.error(err.message || '加载排期失败')
  } finally {
    scheduleLoading.value = false
  }
}

const scrollToSelectedCourse = async () => {
  await nextTick()
  if (!courseTableRef.value || !selectedCourse.value) return
  try {
    courseTableRef.value.setCurrentRow?.(selectedCourse.value)
  } catch (err) {
    // ignore
  }
  await nextTick()
  const tableEl = courseTableRef.value?.$el
  if (!tableEl) return
  const currentRow = tableEl.querySelector('.el-table__body .current-row')
  if (currentRow?.scrollIntoView) {
    currentRow.scrollIntoView({ block: 'center', behavior: 'smooth' })
  }
}

const courseRowClass = ({ row }) => {
  const classes = []
  if (selectedCourse.value?.id === row.id) classes.push('row-selected')
  return classes.join(' ')
}

const selectSchedule = (row) => {
  if (!isScheduleAvailable(row)) {
    ElMessage.warning('该排期不可报名，请选择其他时间')
    return
  }
  selectedSchedule.value = row
  scrollToSelectedSchedule()
}

const enroll = async () => {
  if (!selectedSchedule.value?.id) {
    ElMessage.warning('请选择排期')
    return
  }
  try {
    enrolling.value = true
    const { data } = await appClient.post('/app/course/enroll', { scheduleId: selectedSchedule.value.id })
    if (data.code !== 200) throw new Error(data.message || '报名失败')
    ElMessage.success('报名成功')
    lastEnrollment.value = data.data
    if (data.data?.enrollmentId) {
      localStorage.setItem(STORAGE_ENROLLMENT, JSON.stringify(data.data))
      if (data.data.orderId) {
        localStorage.setItem('fp_last_order_id', String(data.data.orderId))
      }
    }
    await loadEnrollments()
  } catch (err) {
    ElMessage.error(err.message || '报名失败')
  } finally {
    enrolling.value = false
  }
}

const loadLastEnrollment = () => {
  try {
    const cached = localStorage.getItem(STORAGE_ENROLLMENT)
    if (cached) lastEnrollment.value = JSON.parse(cached)
  } catch (err) {
    lastEnrollment.value = null
  }
}

const mockPay = async () => {
  if (!lastEnrollment.value?.enrollmentId) {
    ElMessage.warning('暂无报名可支付')
    return
  }
  try {
    paying.value = true
    const { data } = await appClient.post('/app/course/pay-success', null, {
      params: { enrollmentId: lastEnrollment.value.enrollmentId }
    })
    if (data.code !== 200) throw new Error(data.message || '支付失败')
    ElMessage.success('支付状态已更新')
    await loadEnrollments()
  } catch (err) {
    ElMessage.error(err.message || '支付失败')
  } finally {
    paying.value = false
  }
}

const cancelUnpaid = async () => {
  if (!lastEnrollment.value?.enrollmentId) {
    ElMessage.warning('暂无报名可取消')
    return
  }
  try {
    canceling.value = true
    const { data } = await appClient.post('/app/course/cancel-unpaid', null, {
      params: { enrollmentId: lastEnrollment.value.enrollmentId }
    })
    if (data.code !== 200) throw new Error(data.message || '取消失败')
    ElMessage.success('已取消未支付报名')
    await loadEnrollments()
  } catch (err) {
    ElMessage.error(err.message || '取消失败')
  } finally {
    canceling.value = false
  }
}

const refundPaid = async () => {
  if (!lastEnrollment.value?.enrollmentId) {
    ElMessage.warning('暂无报名可退款')
    return
  }
  if (!refundReason.value.trim()) {
    ElMessage.warning('请输入退款原因')
    return
  }
  try {
    refunding.value = true
    const { data } = await appClient.post('/app/course/refund', {
      enrollmentId: lastEnrollment.value.enrollmentId,
      reason: refundReason.value.trim()
    })
    if (data.code !== 200) throw new Error(data.message || '退款失败')
    ElMessage.success('退款已提交')
    await loadEnrollments()
  } catch (err) {
    ElMessage.error(err.message || '退款失败')
  } finally {
    refunding.value = false
  }
}

const goToOrder = (orderId) => {
  if (!orderId) return
  router.push({ path: '/orders', query: { orderId } })
}

const goTo = (path) => {
  if (!path) return
  router.push(path)
}

const loadEnrollments = async () => {
  try {
    enrollmentsLoading.value = true
    const { data } = await appClient.get('/app/course/my/enrollments')
    if (data.code !== 200) throw new Error(data.message || '加载失败')
    enrollments.value = (data.data || []).map((item) => ({
      ...item,
      createTime: item.createTime || item.enrollTime || ''
    }))
  } catch (err) {
    ElMessage.error(err.message || '加载失败')
  } finally {
    enrollmentsLoading.value = false
  }
}

const remainingSlots = (row) => {
  if (!row) return 0
  const capacity = Number(row.capacity ?? 0)
  const booked = Number(row.bookedCount ?? 0)
  const left = capacity - booked
  return Number.isFinite(left) ? Math.max(left, 0) : 0
}

const isScheduleAvailable = (row) => {
  if (!row) return false
  if (row.status !== null && row.status !== undefined && Number(row.status) !== 1) return false
  if (remainingSlots(row) <= 0) return false
  const start = resolveScheduleStart(row)
  if (start && start.getTime() < Date.now() - 60 * 1000) return false
  return true
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

const scheduleStatus = (row) => {
  if (!row) return { text: '-', type: 'info' }
  if (row.status !== null && row.status !== undefined && Number(row.status) !== 1) {
    return { text: '停用', type: 'info' }
  }
  const start = resolveScheduleStart(row)
  if (start && start.getTime() < Date.now() - 60 * 1000) {
    return { text: '已过期', type: 'warning' }
  }
  if (remainingSlots(row) <= 0) {
    return { text: '已满', type: 'danger' }
  }
  return { text: '可报名', type: 'success' }
}

const scheduleRowClass = ({ row }) => {
  const classes = []
  if (selectedSchedule.value?.id === row.id) classes.push('row-selected')
  if (!isScheduleAvailable(row)) classes.push('row-disabled')
  return classes.join(' ')
}

const scrollToSelectedSchedule = async () => {
  await nextTick()
  if (!scheduleTableRef.value || !selectedSchedule.value) return
  try {
    scheduleTableRef.value.setCurrentRow?.(selectedSchedule.value)
  } catch (err) {
    // ignore
  }
  await nextTick()
  const tableEl = scheduleTableRef.value?.$el
  if (!tableEl) return
  const currentRow = tableEl.querySelector('.el-table__body .current-row')
  if (currentRow?.scrollIntoView) {
    currentRow.scrollIntoView({ block: 'center', behavior: 'smooth' })
  }
}

const formatCourseStatus = (status) => {
  if (status === 1) return '上架'
  if (status === 0) return '下架'
  return status ?? '-'
}

const formatEnrollmentStatus = (status) => {
  const map = {
    0: '已取消',
    1: '未支付',
    2: '已支付',
    3: '已退款'
  }
  return map[status] || status || '-'
}

const formatDate = (value) => {
  if (!value) return '-'
  const raw = String(value)
  if (raw.includes('T')) {
    return raw.split('T')[0]
  }
  return raw.slice(0, 10)
}

const formatTime = (value) => {
  if (!value) return '-'
  const raw = String(value)
  if (raw.includes('T')) {
    const time = raw.split('T')[1] || ''
    return time.slice(0, 5)
  }
  if (raw.includes(':')) {
    return raw.slice(0, 5)
  }
  return raw
}

const formatDateTime = (value) => {
  if (!value) return '-'
  const raw = String(value)
  if (raw.includes('T')) {
    const [date, time] = raw.split('T')
    return `${date} ${time.slice(0, 8)}`
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

loadCourses()
loadEnrollments()
loadLastEnrollment()
</script>

<style scoped>
.detail {
  padding: 12px 0;
}

.muted {
  font-size: 12px;
  color: var(--muted);
  margin-top: 4px;
}

.action-row {
  display: flex;
  gap: 8px;
  margin-top: 10px;
  flex-wrap: wrap;
}

.input-inline {
  margin-top: 8px;
  max-width: 360px;
}

.full {
  color: #ef4444;
  font-weight: 600;
}

:deep(.row-selected) td {
  background: var(--row-selected-bg) !important;
}

:deep(.row-disabled) td {
  color: #9ca3af;
}
</style>
