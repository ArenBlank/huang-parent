<template>
  <div class="page-stack courses-page">
    <section class="hero-panel">
      <div class="course-hero">
        <div>
          <p class="quest-kicker">Course Catalog</p>
          <h1 class="hero-title">选课留在左侧，排期与报名固定在右侧</h1>
          <p class="hero-subtitle">
            课程列表不再把用户带离操作链。现在可以一边浏览课程，一边在旁边完成排期选择、报名提交和最近报名处理。
          </p>
          <div class="hero-badges">
            <span class="badge-pill is-dark">课程 {{ courses.length }}</span>
            <span class="badge-pill is-dark">当前可报排期 {{ availableSchedules }}</span>
            <span class="badge-pill is-dark">我的报名 {{ enrollments.length }}</span>
            <span class="badge-pill is-dark">待上课 {{ mySchedules.length }}</span>
          </div>
        </div>
        <div class="hero-actions">
          <el-button type="primary" @click="loadCourses" :loading="loading">刷新课程</el-button>
          <el-button plain @click="scrollToCatalog">回到选课区</el-button>
        </div>
      </div>
    </section>

    <div class="workspace-shell">
      <section ref="catalogRef" class="card catalog-panel">
        <div class="toolbar catalog-toolbar">
          <div>
            <h2 class="section-title">选课区</h2>
            <p class="section-sub">课程列表保持在当前视野里，切换课程时不需要把整页重新滚回顶部。</p>
          </div>
        </div>

        <el-empty v-if="!courses.length" :description="loading ? '正在加载课程...' : '暂无课程'">
          <div class="empty-actions">
            <el-button size="small" @click="loadCourses">重试</el-button>
            <el-button size="small" @click="goTo('/booking')">去教练预约</el-button>
          </div>
        </el-empty>

        <template v-else>
          <div class="catalog-controls">
            <el-input
              v-model="courseKeyword"
              class="course-search"
              clearable
              placeholder="搜索课程名、简介或难度"
            />
            <div class="catalog-stats">
              <span class="badge-pill is-dark">显示 {{ filteredCourses.length }}</span>
              <span class="badge-pill">总计 {{ courses.length }}</span>
            </div>
          </div>

          <div v-if="selectedCourse" class="catalog-selected-banner">
            <div>
              <p class="selected-caption">当前选中课程</p>
              <strong>{{ selectedCourse.title }}</strong>
              <p>{{ selectedCourse.summary || "右侧工作台已经同步当前课程的排期与报名操作。" }}</p>
            </div>
            <div class="selected-banner__chips">
              <span class="tag">排期 {{ schedules.length }}</span>
              <span class="tag">可报 {{ availableSchedules }}</span>
              <span class="tag">状态 {{ formatCourseStatus(selectedCourse.status) }}</span>
            </div>
          </div>

          <el-empty v-if="!filteredCourses.length" description="没有匹配课程">
            <div class="empty-actions">
              <el-button size="small" @click="courseKeyword = ''">清空搜索</el-button>
            </div>
          </el-empty>

          <div v-else class="catalog-scroller">
            <div class="course-grid">
              <button
                v-for="course in filteredCourses"
                :key="course.id"
                type="button"
                class="course-card eco-clickable"
                :class="{ 'course-card--active': selectedCourse?.id === course.id }"
                @click="selectCourse(course)"
              >
                <span v-if="selectedCourse?.id === course.id" class="course-card__flag">已选中</span>
                <div class="course-cover-wrap">
                  <el-image v-if="course.coverUrl" :src="course.coverUrl" fit="cover" class="course-cover" />
                  <div v-else class="course-cover fallback">暂无封面</div>
                </div>
                <div class="course-body">
                  <strong>{{ course.title }}</strong>
                  <p>{{ course.summary || "暂无课程简介" }}</p>
                  <div class="badge-row">
                    <span class="tag">价格 {{ course.price }}</span>
                    <span class="tag">状态 {{ formatCourseStatus(course.status) }}</span>
                    <span class="tag" v-if="course.level">难度 {{ course.level }}</span>
                  </div>
                  <span class="course-card__hint">选中后右侧立即更新排期与报名操作</span>
                </div>
              </button>
            </div>
          </div>
        </template>
      </section>

      <div class="workflow-column">
        <section class="card workflow-panel">
          <div class="workflow-stack">
            <div class="workflow-block workflow-block--highlight">
              <div class="workflow-heading">
                <div>
                  <p class="section-eyebrow">当前课程</p>
                  <h2 class="section-title-sm">报名工作台</h2>
                  <p class="section-sub">课程切换后，右侧内容即时刷新，你可以随时返回左侧继续挑课。</p>
                </div>
                <el-button plain size="small" @click="scrollToCatalog">换一门课</el-button>
              </div>

              <el-empty v-if="!selectedCourse" description="先在左侧选择课程" />

              <div v-else class="selected-course-card">
                <div class="selected-course-cover-wrap">
                  <el-image v-if="selectedCourse.coverUrl" :src="selectedCourse.coverUrl" fit="cover" class="course-cover" />
                  <div v-else class="course-cover fallback">暂无封面</div>
                </div>
                <div class="selected-course-copy">
                  <strong>{{ selectedCourse.title }}</strong>
                  <p>{{ selectedCourse.summary || "可在下方继续选择排期并提交报名。" }}</p>
                  <div class="selected-course-stats">
                    <span class="tag">价格 {{ selectedCourse.price }}</span>
                    <span class="tag">状态 {{ formatCourseStatus(selectedCourse.status) }}</span>
                    <span class="tag">排期 {{ schedules.length }}</span>
                    <span class="tag">可报 {{ availableSchedules }}</span>
                  </div>
                </div>
              </div>
            </div>

            <div class="workflow-block">
              <div>
                <p class="section-eyebrow">排期选择</p>
                <h2 class="section-title-sm">选择合适时间</h2>
                <p class="section-sub">排期放在独立列表里，始终和选课区并列显示，不需要上下长距离跳转。</p>
              </div>

              <el-empty v-if="!selectedCourse" description="请先选择课程" />

              <el-empty
                v-else-if="!schedules.length && !scheduleLoading"
                description="暂无排期，可在管理端创建后再来报名"
              >
                <div class="empty-actions">
                  <el-button size="small" @click="loadCourses">刷新课程</el-button>
                </div>
              </el-empty>

              <div v-else class="schedule-area">
                <el-alert
                  v-if="selectedCourse && !scheduleLoading && schedules.length && !availableSchedules"
                  type="warning"
                  show-icon
                  :closable="false"
                  title="该课程当前没有可报名排期"
                  style="margin-bottom: 12px"
                />

                <div ref="scheduleListRef" class="schedule-list" v-loading="scheduleLoading">
                  <button
                    v-for="schedule in schedules"
                    :key="schedule.id"
                    :data-schedule-id="schedule.id"
                    type="button"
                    class="schedule-item eco-clickable"
                    :class="{
                      'schedule-item--active': selectedSchedule?.id === schedule.id,
                      'schedule-item--disabled': !isScheduleAvailable(schedule)
                    }"
                    @click="selectSchedule(schedule)"
                  >
                    <div class="schedule-item__top">
                      <strong>{{ formatScheduleTime(schedule) }}</strong>
                      <el-tag :type="scheduleStatus(schedule).type" size="small">{{ scheduleStatus(schedule).text }}</el-tag>
                    </div>
                    <div class="schedule-item__meta">
                      <span>排期 {{ schedule.id }}</span>
                      <span>教练 {{ schedule.coachId }}</span>
                      <span>价格 {{ schedule.price }}</span>
                    </div>
                    <div class="schedule-item__stats">
                      <span class="tag">余量 {{ remainingSlots(schedule) }}</span>
                      <span class="tag">开始 {{ formatTime(schedule.startTime) }}</span>
                      <span class="tag">结束 {{ formatTime(schedule.endTime) }}</span>
                    </div>
                  </button>
                </div>
              </div>
            </div>

            <div class="workflow-block">
              <div class="workflow-action-head">
                <div>
                  <p class="section-eyebrow">报名操作</p>
                  <h2 class="section-title-sm">确认并提交</h2>
                  <p class="section-sub">只要右侧排期已选好，就可以立即提交，不需要再滚动到页面底部。</p>
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

              <div v-else class="detail-card">
                <div class="detail-card__main">
                  <strong>{{ selectedCourse?.title }}</strong>
                  <p class="muted">{{ formatScheduleTime(selectedSchedule) }}</p>
                </div>
                <div class="detail-chip-row">
                  <span class="tag">排期ID {{ selectedSchedule.id }}</span>
                  <span class="tag">价格 {{ selectedSchedule.price }}</span>
                  <span class="tag">余量 {{ remainingSlots(selectedSchedule) }}</span>
                  <span class="tag">状态 {{ scheduleStatus(selectedSchedule).text }}</span>
                </div>
              </div>
            </div>

            <section id="recent-enrollment" ref="recentEnrollmentRef" class="workflow-block">
              <div class="workflow-action-head">
                <div>
                  <p class="section-eyebrow">最近报名</p>
                  <h2 class="section-title-sm">后续处理</h2>
                  <p class="section-sub">支付模拟、取消未支付、退款和查看订单都固定在工作台里，报名后不用再找卡片。</p>
                </div>
                <el-button type="primary" plain size="small" @click="loadEnrollments" :loading="enrollmentsLoading">
                  刷新报名
                </el-button>
              </div>

              <el-empty v-if="!lastEnrollment" description="暂无最近报名记录">
                <div class="empty-actions">
                  <el-button size="small" @click="loadEnrollments">刷新报名</el-button>
                </div>
              </el-empty>

              <div v-else class="detail-card detail-card--stack">
                <div class="detail-card__main">
                  <strong>报名 {{ lastEnrollment.enrollmentId }}</strong>
                  <p class="muted">订单 {{ lastEnrollment.orderId || "-" }}</p>
                </div>
                <div class="detail-list">
                  <div>订单号：{{ lastEnrollment.orderNo || "-" }}</div>
                  <div>支付号：{{ lastEnrollment.payNo || "-" }}</div>
                  <div>金额：{{ lastEnrollment.amount || "-" }}</div>
                </div>
                <el-input v-model="refundReason" size="small" class="input-inline" placeholder="请输入退款原因" />
                <div class="action-row">
                  <el-button type="primary" size="small" :loading="paying" @click="mockPay">模拟支付</el-button>
                  <el-button type="warning" size="small" :loading="canceling" @click="cancelUnpaid">取消未支付</el-button>
                  <el-button type="danger" size="small" :loading="refunding" @click="refundPaid">发起退款</el-button>
                  <el-button size="small" :disabled="!lastEnrollment?.orderId" @click="goToOrder(lastEnrollment?.orderId)">
                    查看订单
                  </el-button>
                </div>
              </div>
            </section>
          </div>
        </section>
      </div>
    </div>

    <section class="card itinerary-panel">
      <div class="toolbar">
        <div>
          <h2 class="section-title">待上课 / 我的行程</h2>
          <p class="section-sub">支付完成后，这里会自动汇总你待上课的排期，并提供核销码出示入口。</p>
        </div>
        <div class="hero-actions">
          <el-button plain @click="loadMySchedules" :loading="mySchedulesLoading">刷新行程</el-button>
          <el-button type="primary" plain @click="loadEnrollments" :loading="enrollmentsLoading">刷新报名</el-button>
        </div>
      </div>

      <el-tabs v-model="itineraryTab" class="itinerary-tabs">
        <el-tab-pane :label="`待上课 / 我的行程 ${mySchedules.length}`" name="upcoming">
          <el-empty v-if="!mySchedules.length && !mySchedulesLoading" description="你还没有待上课行程">
            <div class="empty-actions">
              <el-button size="small" type="primary" @click="scrollToCatalog">去报名课程</el-button>
              <el-button size="small" @click="loadMySchedules">刷新行程</el-button>
            </div>
          </el-empty>

          <div v-else class="itinerary-grid" v-loading="mySchedulesLoading">
            <article
              v-for="schedule in mySchedules"
              :key="schedule.enrollmentId"
              class="itinerary-card"
            >
              <div class="itinerary-card__cover-wrap">
                <el-image
                  v-if="schedule.coverUrl"
                  :src="schedule.coverUrl"
                  fit="cover"
                  class="itinerary-card__cover"
                />
                <div v-else class="itinerary-card__cover fallback">待上课</div>
              </div>

              <div class="itinerary-card__body">
                <div class="itinerary-card__header">
                  <div>
                    <p class="section-eyebrow">待上课排期</p>
                    <strong>{{ schedule.courseTitle || `课程 ${schedule.courseId}` }}</strong>
                  </div>
                  <span class="tag">核销码 {{ schedule.checkInCode || "------" }}</span>
                </div>

                <div class="itinerary-card__meta">
                  <span>上课时间：{{ formatScheduleTime(schedule) }}</span>
                  <span>教练：{{ schedule.coachId ? `#${schedule.coachId}` : "待分配" }}</span>
                  <span>订单：{{ schedule.orderId || "-" }}</span>
                  <span>价格：{{ schedule.price ?? "-" }}</span>
                </div>

                <div class="itinerary-card__actions">
                  <el-button type="primary" @click="openCheckInDialog(schedule)">出示核销码 / 去打卡</el-button>
                  <el-button plain @click="goToOrder(schedule.orderId)" :disabled="!schedule.orderId">查看订单</el-button>
                </div>
              </div>
            </article>
          </div>
        </el-tab-pane>

        <el-tab-pane :label="`报名历史 ${enrollments.length}`" name="history">
          <el-empty v-if="!enrollments.length && !enrollmentsLoading" description="暂无报名记录" />
          <el-table v-else :data="enrollments" v-loading="enrollmentsLoading" style="width: 100%">
            <el-table-column prop="id" label="报名ID" width="90" />
            <el-table-column label="课程">
              <template #default="{ row }">{{ courseMap[row.courseId] || row.courseId }}</template>
            </el-table-column>
            <el-table-column label="订单" width="120">
              <template #default="{ row }">
                <el-button size="small" text :disabled="!row.orderId" @click="goToOrder(row.orderId)">查看</el-button>
              </template>
            </el-table-column>
            <el-table-column label="状态" width="120">
              <template #default="{ row }">{{ formatEnrollmentStatus(row.status) }}</template>
            </el-table-column>
            <el-table-column label="创建时间" min-width="180">
              <template #default="{ row }">{{ formatDateTime(row.createTime || row.enrollTime) }}</template>
            </el-table-column>
          </el-table>
        </el-tab-pane>
      </el-tabs>
    </section>

    <el-dialog
      v-model="checkInDialogVisible"
      width="520px"
      title="出示核销码"
      destroy-on-close
      @closed="closeCheckInDialog"
    >
      <div v-if="activeCheckInSchedule" class="checkin-dialog">
        <p class="checkin-dialog__tip">到店后向前台或教练出示下方 6 位核销码即可完成签到。</p>
        <div class="checkin-dialog__code">{{ activeCheckInSchedule.checkInCode || "------" }}</div>

        <div class="checkin-dialog__meta">
          <div class="checkin-dialog__meta-item">
            <span>课程名称</span>
            <strong>{{ activeCheckInSchedule.courseTitle || `课程 ${activeCheckInSchedule.courseId}` }}</strong>
          </div>
          <div class="checkin-dialog__meta-item">
            <span>上课时间</span>
            <strong>{{ formatScheduleTime(activeCheckInSchedule) }}</strong>
          </div>
          <div class="checkin-dialog__meta-item">
            <span>教练</span>
            <strong>{{ activeCheckInSchedule.coachId ? `#${activeCheckInSchedule.coachId}` : "待分配" }}</strong>
          </div>
        </div>
      </div>

      <template #footer>
        <el-button @click="closeCheckInDialog">我知道了</el-button>
        <el-button type="primary" @click="copyCheckInCode">复制核销码</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, nextTick, ref, watch } from "vue"
import { useRoute, useRouter } from "vue-router"
import { ElMessage } from "element-plus"
import { appClient } from "../api/client"
import { fetchMyCourseSchedules } from "../api/course"

const router = useRouter()
const route = useRoute()

const courses = ref([])
const loading = ref(false)
const courseKeyword = ref("")
const selectedCourse = ref(null)
const catalogRef = ref(null)

const schedules = ref([])
const scheduleLoading = ref(false)
const selectedSchedule = ref(null)
const scheduleListRef = ref(null)

const enrollments = ref([])
const enrollmentsLoading = ref(false)
const enrolling = ref(false)
const mySchedules = ref([])
const mySchedulesLoading = ref(false)
const itineraryTab = ref("upcoming")
const checkInDialogVisible = ref(false)
const activeCheckInSchedule = ref(null)

const lastEnrollment = ref(null)
const recentEnrollmentRef = ref(null)
const paying = ref(false)
const canceling = ref(false)
const refunding = ref(false)
const refundReason = ref("临时有事，申请退款")

const STORAGE_ENROLLMENT = "fp_last_enrollment"
const STORAGE_COURSE_ID = "fp_last_course_id"

const filteredCourses = computed(() => {
  const keyword = courseKeyword.value.trim().toLowerCase()
  if (!keyword) return courses.value
  return courses.value.filter((course) => {
    const haystack = [course.title, course.summary, course.level, course.id].filter(Boolean).join(" ").toLowerCase()
    return haystack.includes(keyword)
  })
})

const courseMap = computed(() => {
  const map = {}
  courses.value.forEach((course) => {
    map[course.id] = course.title
  })
  return map
})

const selectedUnavailable = computed(() => {
  if (!selectedSchedule.value) return false
  return !isScheduleAvailable(selectedSchedule.value)
})

const availableSchedules = computed(() => schedules.value.filter((row) => isScheduleAvailable(row)).length)

const parseCourseId = (value) => {
  if (value === null || value === undefined || value === "") return null
  const parsed = Number(value)
  return Number.isFinite(parsed) ? parsed : null
}

const loadCourses = async () => {
  try {
    loading.value = true
    const { data } = await appClient.get("/app/course/list")
    if (data.code !== 200) throw new Error(data.message || "加载课程失败")
    courses.value = data.data || []
    if (courses.value.length) {
      const cached = parseCourseId(localStorage.getItem(STORAGE_COURSE_ID))
      const preferred = (cached !== null && courses.value.find((item) => Number(item.id) === cached)) || courses.value[0]
      if (!selectedCourse.value || Number(selectedCourse.value.id) !== Number(preferred.id)) {
        await selectCourse(preferred)
      }
    } else {
      selectedCourse.value = null
      schedules.value = []
      selectedSchedule.value = null
    }
  } catch (err) {
    ElMessage.error(err.message || "加载课程失败")
  } finally {
    loading.value = false
  }
}

const selectCourse = async (row) => {
  selectedCourse.value = row
  if (row?.id !== undefined && row?.id !== null) localStorage.setItem(STORAGE_COURSE_ID, String(row.id))
  selectedSchedule.value = null
  schedules.value = []
  try {
    scheduleLoading.value = true
    const { data } = await appClient.get(`/app/course/${row.id}/schedule/list`)
    if (data.code !== 200) throw new Error(data.message || "加载排期失败")
    schedules.value = data.data || []
    if (schedules.value.length) {
      selectedSchedule.value = schedules.value.find((item) => isScheduleAvailable(item)) || schedules.value[0]
      await scrollToSelectedSchedule()
    }
  } catch (err) {
    ElMessage.error(err.message || "加载排期失败")
  } finally {
    scheduleLoading.value = false
  }
}

const selectSchedule = (row) => {
  if (!isScheduleAvailable(row)) {
    ElMessage.warning("该排期不可报名，请选择其他时间")
    return
  }
  selectedSchedule.value = row
  scrollToSelectedSchedule()
}

const enroll = async () => {
  if (!selectedSchedule.value?.id) {
    ElMessage.warning("请选择排期")
    return
  }
  try {
    enrolling.value = true
    const { data } = await appClient.post("/app/course/enroll", { scheduleId: selectedSchedule.value.id })
    if (data.code !== 200) throw new Error(data.message || "报名失败")
    ElMessage.success("报名成功")
    lastEnrollment.value = data.data
    if (data.data?.enrollmentId) {
      localStorage.setItem(STORAGE_ENROLLMENT, JSON.stringify(data.data))
      if (data.data.orderId) localStorage.setItem("fp_last_order_id", String(data.data.orderId))
    }
    await loadEnrollments()
  } catch (err) {
    ElMessage.error(err.message || "报名失败")
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
    ElMessage.warning("暂无报名可支付")
    return
  }
  try {
    paying.value = true
    const { data } = await appClient.post("/app/course/pay-success", null, {
      params: { enrollmentId: lastEnrollment.value.enrollmentId }
    })
    if (data.code !== 200) throw new Error(data.message || "模拟支付失败")
    ElMessage.success("支付状态已更新")
    await Promise.all([loadEnrollments(), loadMySchedules()])
  } catch (err) {
    ElMessage.error(err.message || "模拟支付失败")
  } finally {
    paying.value = false
  }
}

const cancelUnpaid = async () => {
  if (!lastEnrollment.value?.enrollmentId) {
    ElMessage.warning("暂无报名可取消")
    return
  }
  try {
    canceling.value = true
    const { data } = await appClient.post("/app/course/cancel-unpaid", null, {
      params: { enrollmentId: lastEnrollment.value.enrollmentId }
    })
    if (data.code !== 200) throw new Error(data.message || "取消失败")
    ElMessage.success("已取消未支付报名")
    await Promise.all([loadEnrollments(), loadMySchedules()])
  } catch (err) {
    ElMessage.error(err.message || "取消失败")
  } finally {
    canceling.value = false
  }
}

const refundPaid = async () => {
  if (!lastEnrollment.value?.enrollmentId) {
    ElMessage.warning("暂无报名可退款")
    return
  }
  if (!refundReason.value.trim()) {
    ElMessage.warning("请输入退款原因")
    return
  }
  try {
    refunding.value = true
    const { data } = await appClient.post("/app/course/refund", {
      enrollmentId: lastEnrollment.value.enrollmentId,
      reason: refundReason.value.trim()
    })
    if (data.code !== 200) throw new Error(data.message || "退款失败")
    ElMessage.success("退款已提交")
    await Promise.all([loadEnrollments(), loadMySchedules()])
  } catch (err) {
    ElMessage.error(err.message || "退款失败")
  } finally {
    refunding.value = false
  }
}

const loadEnrollments = async () => {
  try {
    enrollmentsLoading.value = true
    const { data } = await appClient.get("/app/course/my/enrollments")
    if (data.code !== 200) throw new Error(data.message || "加载报名失败")
    enrollments.value = (data.data || []).map((item) => ({ ...item, createTime: item.createTime || item.enrollTime || "" }))
  } catch (err) {
    ElMessage.error(err.message || "加载报名失败")
  } finally {
    enrollmentsLoading.value = false
  }
}

const loadMySchedules = async () => {
  try {
    mySchedulesLoading.value = true
    const { data } = await fetchMyCourseSchedules()
    if (data.code !== 200) throw new Error(data.message || "加载我的行程失败")
    mySchedules.value = data.data || []
  } catch (err) {
    ElMessage.error(err.message || "加载我的行程失败")
  } finally {
    mySchedulesLoading.value = false
  }
}

const openCheckInDialog = (row) => {
  activeCheckInSchedule.value = row
  checkInDialogVisible.value = true
}

const closeCheckInDialog = () => {
  checkInDialogVisible.value = false
  activeCheckInSchedule.value = null
}

const copyCheckInCode = async () => {
  if (!activeCheckInSchedule.value?.checkInCode) {
    ElMessage.warning("当前没有可用核销码")
    return
  }
  try {
    await navigator.clipboard.writeText(activeCheckInSchedule.value.checkInCode)
    ElMessage.success("核销码已复制")
  } catch (err) {
    ElMessage.error("复制失败，请手动记录核销码")
  }
}

const goToOrder = (orderId) => {
  if (!orderId) return
  router.push({ path: "/orders", query: { orderId, from: "courses", returnTo: "recent-enrollment" } })
}

const goTo = (path) => {
  router.push(path)
}

const scrollToCatalog = async () => {
  await nextTick()
  catalogRef.value?.scrollIntoView?.({ behavior: "smooth", block: "start" })
}

const scrollToRecentEnrollment = async () => {
  await nextTick()
  recentEnrollmentRef.value?.scrollIntoView?.({ behavior: "smooth", block: "start" })
}

watch(
  () => route.hash,
  async (hash) => {
    if (hash === "#recent-enrollment") await scrollToRecentEnrollment()
  },
  { immediate: true }
)

const remainingSlots = (row) => {
  const capacity = Number(row?.capacity ?? 0)
  const booked = Number(row?.bookedCount ?? 0)
  const left = capacity - booked
  return Number.isFinite(left) ? Math.max(left, 0) : 0
}

const resolveScheduleStart = (row) => {
  if (!row) return null
  const rawStart = row.startTime
  if (rawStart && String(rawStart).includes("T")) {
    const ts = new Date(rawStart)
    return Number.isNaN(ts.getTime()) ? null : ts
  }
  if (!row.scheduleDate) return null
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
  return { text: "可报名", type: "success" }
}

const scrollToSelectedSchedule = async () => {
  await nextTick()
  if (!scheduleListRef.value || !selectedSchedule.value) return
  const currentItem = scheduleListRef.value.querySelector(`[data-schedule-id="${selectedSchedule.value.id}"]`)
  currentItem?.scrollIntoView?.({ block: "nearest", behavior: "smooth" })
}

const formatCourseStatus = (status) => {
  if (status === 1) return "上架"
  if (status === 0) return "下架"
  return status ?? "-"
}

const formatEnrollmentStatus = (status) => {
  const map = { 0: "已取消", 1: "未支付", 2: "已支付", 3: "已退款" }
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

const formatScheduleTime = (row) => {
  if (!row) return "-"
  const date = formatDate(row.scheduleDate || row.startTime)
  const start = formatTime(row.startTime)
  const end = formatTime(row.endTime)
  return `${date} ${start}-${end}`
}

loadCourses()
loadEnrollments()
loadMySchedules()
loadLastEnrollment()
</script>

<style scoped>
.courses-page {
  align-items: stretch;
}

.course-hero {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: flex-end;
  flex-wrap: wrap;
}

.hero-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.workspace-shell {
  display: grid;
  grid-template-columns: minmax(0, 1.18fr) minmax(360px, 0.82fr);
  gap: 16px;
  align-items: start;
}

.catalog-panel,
.workflow-panel {
  min-height: 0;
}

.catalog-controls {
  display: flex;
  flex-wrap: wrap;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 16px;
}

.course-search {
  flex: 1 1 280px;
  max-width: 420px;
}

.catalog-stats {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.catalog-selected-banner {
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
.section-eyebrow {
  margin: 0 0 8px;
  color: var(--eco-text-soft);
  font-size: 11px;
  font-weight: 800;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.catalog-selected-banner strong {
  display: block;
  font-size: 20px;
  line-height: 1.2;
}

.catalog-selected-banner p {
  margin: 8px 0 0;
  color: var(--eco-text-soft);
  font-size: 13px;
  line-height: 1.6;
}

.selected-banner__chips {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-content: flex-start;
  justify-content: flex-end;
}

.catalog-scroller {
  max-height: min(74vh, 960px);
  overflow: auto;
  padding-right: 6px;
}

.course-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
  gap: 12px;
}

.course-card {
  position: relative;
  border: 2px solid rgba(52, 45, 105, 0.14);
  border-radius: 22px;
  background: linear-gradient(180deg, #ffffff 0%, #fffaf1 100%);
  text-align: left;
  padding: 12px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.course-card__flag {
  position: absolute;
  top: 12px;
  right: 12px;
  z-index: 1;
  display: inline-flex;
  align-items: center;
  min-height: 30px;
  padding: 0 10px;
  border-radius: 999px;
  background: var(--eco-primary);
  color: #ffffff;
  font-size: 11px;
  font-weight: 800;
  box-shadow: 0 6px 0 rgba(52, 45, 105, 0.08);
}

.course-card--active {
  border-color: var(--eco-primary);
  background: linear-gradient(180deg, #f6f1ff 0%, #fffaf0 100%);
  box-shadow: 0 10px 0 rgba(42, 35, 86, 0.08), 0 18px 26px rgba(91, 83, 255, 0.12);
}

.course-cover-wrap,
.selected-course-cover-wrap {
  border-radius: 16px;
  overflow: hidden;
  border: 2px solid rgba(52, 45, 105, 0.12);
  background: #ffffff;
}

.course-cover-wrap {
  height: 138px;
}

.selected-course-cover-wrap {
  height: 132px;
}

.course-cover {
  width: 100%;
  height: 100%;
}

.fallback {
  display: grid;
  place-items: center;
  font-size: 12px;
  color: var(--eco-text-soft);
  background: #f4efff;
}

.course-body {
  display: grid;
  gap: 8px;
}

.course-body strong {
  display: block;
  font-size: 17px;
  line-height: 1.25;
}

.course-body p {
  margin: 0;
  color: var(--eco-text-soft);
  font-size: 13px;
  line-height: 1.65;
}

.course-card__hint {
  color: var(--eco-primary-strong);
  font-size: 12px;
  font-weight: 700;
}

.workflow-column {
  position: sticky;
  top: 16px;
  align-self: start;
}

.workflow-panel {
  max-height: calc(100vh - 32px);
  overflow: auto;
}

.workflow-stack {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.workflow-block {
  border-radius: 24px;
  border: 2px solid rgba(52, 45, 105, 0.12);
  background: linear-gradient(180deg, #ffffff 0%, #fffaf3 100%);
  padding: 18px;
}

.workflow-block--highlight {
  background: linear-gradient(180deg, #f3efff 0%, #fff9ee 100%);
}

.workflow-heading,
.workflow-action-head {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 12px;
  margin-bottom: 12px;
}

.selected-course-card {
  display: grid;
  grid-template-columns: 120px minmax(0, 1fr);
  gap: 16px;
  align-items: start;
}

.selected-course-copy strong {
  display: block;
  font-size: 22px;
  line-height: 1.15;
}

.selected-course-copy p {
  margin: 8px 0 0;
  color: var(--eco-text-soft);
  font-size: 13px;
  line-height: 1.7;
}

.selected-course-stats,
.detail-chip-row {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 12px;
}

.schedule-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
  max-height: 360px;
  overflow: auto;
  padding-right: 4px;
}

.schedule-item {
  width: 100%;
  border: 2px solid rgba(52, 45, 105, 0.12);
  border-radius: 20px;
  background: #ffffff;
  padding: 14px;
  text-align: left;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.schedule-item__top {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 12px;
}

.schedule-item__top strong {
  font-size: 15px;
  line-height: 1.45;
}

.schedule-item__meta {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 8px;
  color: var(--eco-text-soft);
  font-size: 12px;
}

.schedule-item__stats {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.schedule-item--active {
  border-color: var(--eco-primary);
  background: linear-gradient(180deg, #f5f1ff 0%, #fffaf1 100%);
}

.schedule-item--disabled {
  opacity: 0.76;
}

.detail-card {
  border-radius: 20px;
  border: 2px solid rgba(52, 45, 105, 0.12);
  background: #ffffff;
  padding: 16px;
}

.detail-card--stack {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.detail-card__main strong {
  display: block;
  font-size: 18px;
  line-height: 1.2;
}

.detail-list {
  display: grid;
  gap: 6px;
  color: var(--eco-text-soft);
  font-size: 13px;
}

.full {
  color: #ca5744;
  font-weight: 700;
}

.input-inline {
  margin-top: 6px;
  width: 100%;
}

.itinerary-panel {
  display: grid;
  gap: 12px;
}

.itinerary-tabs :deep(.el-tabs__header) {
  margin-bottom: 18px;
}

.itinerary-grid {
  display: grid;
  gap: 14px;
}

.itinerary-card {
  display: grid;
  grid-template-columns: 148px minmax(0, 1fr);
  gap: 16px;
  padding: 16px;
  border-radius: 22px;
  border: 2px solid rgba(52, 45, 105, 0.12);
  background: linear-gradient(180deg, #ffffff 0%, #fffaf3 100%);
}

.itinerary-card__cover-wrap {
  height: 132px;
  border-radius: 16px;
  overflow: hidden;
  border: 2px solid rgba(52, 45, 105, 0.12);
  background: #ffffff;
}

.itinerary-card__cover {
  width: 100%;
  height: 100%;
}

.itinerary-card__body {
  display: grid;
  gap: 12px;
  align-content: start;
}

.itinerary-card__header {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: flex-start;
}

.itinerary-card__header strong {
  display: block;
  font-size: 20px;
  line-height: 1.2;
}

.itinerary-card__meta {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px 12px;
  color: var(--eco-text-soft);
  font-size: 13px;
}

.itinerary-card__actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.checkin-dialog {
  display: grid;
  gap: 18px;
}

.checkin-dialog__tip {
  margin: 0;
  color: var(--eco-text-soft);
  font-size: 14px;
  line-height: 1.7;
}

.checkin-dialog__code {
  display: grid;
  place-items: center;
  min-height: 132px;
  padding: 18px;
  border-radius: 22px;
  border: 2px solid rgba(91, 83, 255, 0.18);
  background: linear-gradient(135deg, rgba(109, 103, 255, 0.1), rgba(255, 208, 122, 0.14));
  color: var(--eco-primary-strong);
  font-size: clamp(40px, 8vw, 64px);
  font-weight: 900;
  letter-spacing: 0.18em;
  text-indent: 0.18em;
}

.checkin-dialog__meta {
  display: grid;
  gap: 10px;
}

.checkin-dialog__meta-item {
  padding: 14px 16px;
  border-radius: 18px;
  border: 2px solid rgba(52, 45, 105, 0.12);
  background: #ffffff;
}

.checkin-dialog__meta-item span {
  display: block;
  margin-bottom: 8px;
  color: var(--eco-text-soft);
  font-size: 12px;
}

.checkin-dialog__meta-item strong {
  display: block;
  font-size: 16px;
  line-height: 1.4;
}

@media (max-width: 1180px) {
  .workspace-shell {
    grid-template-columns: minmax(0, 1fr) 380px;
  }
}

@media (max-width: 960px) {
  .workspace-shell {
    grid-template-columns: 1fr;
  }

  .workflow-column {
    position: static;
  }

  .workflow-panel {
    max-height: none;
    overflow: visible;
  }

  .catalog-scroller {
    max-height: none;
    padding-right: 0;
  }

  .selected-course-card {
    grid-template-columns: 1fr;
  }

  .itinerary-card {
    grid-template-columns: 1fr;
  }

  .itinerary-card__cover-wrap {
    height: 180px;
  }
}

@media (max-width: 640px) {
  .course-grid {
    grid-template-columns: 1fr;
  }

  .catalog-selected-banner,
  .workflow-heading,
  .workflow-action-head,
  .course-hero,
  .catalog-controls {
    flex-direction: column;
  }

  .selected-banner__chips {
    justify-content: flex-start;
  }

  .schedule-item__meta,
  .itinerary-card__meta {
    grid-template-columns: 1fr;
  }

  .itinerary-card__header {
    flex-direction: column;
  }
}
</style>
