$bookingPath = 'D:\DevelopmentLOOK\Idea\idea_project_workspace\huang-parent\frontend-app\src\pages\Booking.vue'
$ordersPath = 'D:\DevelopmentLOOK\Idea\idea_project_workspace\huang-parent\frontend-app\src\pages\Orders.vue'

function Normalize-Newlines([string]$text) { return $text -replace "`r`n", "`n" }
function Save-Utf8NoBom([string]$path, [string]$text) {
  $utf8NoBom = New-Object System.Text.UTF8Encoding($false)
  [System.IO.File]::WriteAllText($path, $text, $utf8NoBom)
}

$booking = Normalize-Newlines (Get-Content $bookingPath -Raw)
$orders = Normalize-Newlines (Get-Content $ordersPath -Raw)

$bookingTemplate = @'
<template>
  <div class="page-stack booking-workspace">
    <section class="booking-stats-bar">
      <div class="booking-stats-copy">
        <p class="quest-kicker">Coach Booking</p>
        <h1 class="section-title">预约教练，形成稳定训练节律</h1>
        <p class="section-sub">筛选、选档、支付和评价收进一个工作台，不再一路向下滚。</p>
      </div>
      <div class="booking-stat-pills">
        <span class="tag">当前可约 {{ visibleSchedules.length }}</span>
        <span class="tag">未来可预约 {{ effectiveAvailableSchedules }}</span>
        <span class="tag">我的预约 {{ myBookings.length }}</span>
      </div>
      <el-button type="primary" :loading="loading" @click="loadSchedules">刷新预约工作台</el-button>
    </section>

    <el-row :gutter="20" class="booking-dashboard">
      <el-col :xs="24" :md="8">
        <aside class="booking-sidebar">
          <section v-if="lastBooking" class="workspace-card">
            <div class="toolbar">
              <div>
                <h2 class="section-title-sm">当前待办</h2>
                <p class="section-sub">快速处理你手头的预约单。</p>
              </div>
            </div>
            <div class="detail detail-card">
              <div class="detail-card__title">当前待处理预约</div>
              <div class="detail-card__headline">{{ formatScheduleTime(lastBooking) }}</div>
              <div class="detail-chip-row">
                <span class="tag">预约ID {{ lastBooking.bookingId || lastBooking.id }}</span>
                <span class="tag">订单ID {{ lastBooking.orderId || '-' }}</span>
                <span class="tag">预约状态 {{ formatBookingStatus(lastBooking.bookingStatus) }}</span>
                <span class="tag">支付状态 {{ formatPayStatus(lastBooking.payStatus) }}</span>
              </div>
              <div class="detail-list">
                <div>订单号：{{ lastBooking.orderNo || '-' }}</div>
                <div>金额：{{ lastBooking.amount ?? '-' }}</div>
                <div>教练：{{ bookingCoachDisplayName(lastBooking) }}</div>
              </div>
              <div class="action-row action-row--wrap">
                <el-button type="primary" size="small" :loading="paying" :disabled="!canMockPay(lastBooking)" @click="mockPay">模拟支付</el-button>
                <el-button
                  type="danger"
                  size="small"
                  plain
                  :loading="isCancellingBooking(lastBooking)"
                  :disabled="!canCancelBooking(lastBooking)"
                  @click="cancelBooking(lastBooking)"
                >
                  取消预约
                </el-button>
                <el-button type="success" size="small" :loading="completing" :disabled="!canCompleteBooking(lastBooking)" @click="completeBooking">
                  确认完成
                </el-button>
                <el-button size="small" :disabled="!lastBooking?.orderId" @click="goToOrder(lastBooking)">订单详情</el-button>
              </div>
            </div>
          </section>

          <section ref="createBookingRef" class="workspace-card create-card">
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
                <span class="tag">教练 {{ coachDisplayName(selected.coachId) }}</span>
                <span class="tag">价格 {{ selected.price }}</span>
                <span class="tag">余量 {{ remainingSlots(selected) }}</span>
                <span class="tag">状态 {{ scheduleStatus(selected).text }}</span>
              </div>
              <el-button type="success" class="create-submit" :disabled="!selected || selectedUnavailable" :loading="submitting" @click="createBooking">
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
                <el-form-item label="教练">
                  <div class="coach-picker-field">
                    <button type="button" class="coach-picker-trigger" @click="openCoachPicker">
                      <span v-if="selectedCoachLabel">{{ selectedCoachLabel }}</span>
                      <span v-else class="muted">从教练列表中选择</span>
                    </button>
                    <el-button
                      v-if="coachId !== null && coachId !== undefined && coachId !== ''"
                      text
                      type="primary"
                      @click="setCoach(null)"
                    >
                      清空
                    </el-button>
                  </div>
                </el-form-item>
                <el-form-item label="预约服务日期">
                  <el-date-picker v-model="date" type="date" value-format="YYYY-MM-DD" placeholder="筛选某一天的可预约时间段" />
                </el-form-item>
                <el-form-item label="快速操作">
                  <div class="quick-wrap">
                    <el-button size="small" :type="coachId === null ? 'primary' : 'default'" @click="setCoach(null)">全部</el-button>
                    <el-button size="small" plain @click="openCoachPicker">选择教练</el-button>
                  </div>
                </el-form-item>
                <el-form-item label="操作" class="booking-filter-action">
                  <el-button type="primary" @click="loadSchedules">查询</el-button>
                </el-form-item>
              </div>
            </el-form>
            <p class="muted">这里显示的是用户当前真正还能预约的时间段，满员和过期档期不会再混进来。</p>
            <div class="detail-chip-row booking-summary-row">
              <span class="tag">可预约教练 {{ coachPickerOptions.length }}</span>
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
                        <span class="coach-card__rating">{{ coach.availableCount ? '可约' : '满员' }}</span>
                      </div>
                      <div class="coach-card__tags">
                        <el-tag v-for="tag in coach.tags" :key="tag" size="small" effect="plain">{{ tag }}</el-tag>
                      </div>
                      <div class="coach-card__highlight">最近可约 {{ coach.nextTime }}</div>
                      <div class="coach-card__meta">
                        <span>基础价 ¥{{ coach.minPrice }}</span>
                        <span>当前余量 {{ coach.remaining }}</span>
                        <span>可约档期 {{ coach.availableCount }}</span>
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
                <el-empty v-if="!loading && !visibleSchedules.length" description="暂无可预约时间段">
                  <div class="empty-actions">
                    <el-button size="small" @click="loadSchedules">重试</el-button>
                    <el-button size="small" @click="goTo('/courses')">去课程报名</el-button>
                  </div>
                  <p class="empty-tip">{{ scheduleEmptyReason }}</p>
                </el-empty>
                <div v-else class="schedule-grid">
                  <button
                    v-for="row in visibleSchedules"
                    :key="row.id"
                    type="button"
                    class="schedule-card"
                    :class="{ 'schedule-card--active': selected?.id === row.id, 'schedule-card--disabled': !isScheduleAvailable(row) }"
                    :data-schedule-id="row.id"
                    @click="selectSchedule(row)"
                  >
                    <div class="schedule-card__top">
                      <span class="schedule-card__date">{{ formatDate(row.scheduleDate) }}</span>
                      <el-tag :type="scheduleStatus(row).type" size="small">{{ scheduleStatus(row).text }}</el-tag>
                    </div>
                    <strong class="schedule-card__time">{{ formatTime(row.startTime) }} - {{ formatTime(row.endTime) }}</strong>
                    <div class="schedule-card__meta">
                      <span>教练 {{ bookingCoachDisplayName(row) }}</span>
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
                  <el-button type="primary" size="small" :loading="myLoading" @click="loadMyBookings">刷新</el-button>
                </div>
              </div>
              <el-empty v-if="!myBookings.length && !myLoading" description="暂无预约记录" />
              <el-table v-else :data="myBookings" v-loading="myLoading" style="width: 100%">
                <el-table-column prop="id" label="预约ID" width="90" />
                <el-table-column label="预约时间" min-width="220">
                  <template #default="{ row }">
                    <div class="booking-table__slot">
                      <strong>{{ formatScheduleTime(row) }}</strong>
                      <span class="muted">教练 {{ bookingCoachDisplayName(row) }}</span>
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
                      <strong class="order-no">{{ row.orderNo || ('订单ID ' + (row.orderId || '-')) }}</strong>
                      <div class="booking-table__order-actions">
                        <span class="muted">金额 {{ formatAmount(row.amount) }}</span>
                        <el-button size="small" type="primary" plain :disabled="!row.orderId" @click="goToOrder(row)">查看订单详情</el-button>
                        <el-button
                          size="small"
                          type="danger"
                          plain
                          :loading="isCancellingBooking(row)"
                          :disabled="!canCancelBooking(row)"
                          @click="cancelBooking(row)"
                        >
                          取消预约
                        </el-button>
                      </div>
                    </div>
                  </template>
                </el-table-column>
                <el-table-column label="创建时间" min-width="170">
                  <template #default="{ row }">{{ formatDateTime(row.createTime) }}</template>
                </el-table-column>
                <el-table-column label="评价" width="120">
                  <template #default="{ row }">
                    <el-button size="small" type="primary" plain :disabled="row.bookingStatus !== 'COMPLETED'" @click="openReview(row)">
                      写评价
                    </el-button>
                  </template>
                </el-table-column>
              </el-table>

              <section v-if="selectedReviewBooking" class="workspace-card review-panel">
                <div class="toolbar">
                  <div>
                    <h2 class="section-title-sm">写评价</h2>
                    <p class="section-sub">选中已完成预约后填写评价。</p>
                  </div>
                </div>
                <div v-if="selectedReviewBooking" class="detail">
                  <div>预约ID：{{ selectedReviewBooking.id }}</div>
                  <div class="muted">预约状态：{{ formatBookingStatus(selectedReviewBooking.bookingStatus) }}</div>
                  <div class="muted">订单ID：{{ selectedReviewBooking.orderId }}</div>
                  <div class="muted">预约时间：{{ formatScheduleTime(selectedReviewBooking) }}</div>
                </div>
                <el-form :model="reviewForm" label-position="top">
                  <el-form-item label="预约ID">
                    <el-input v-model="reviewForm.bookingId" disabled />
                  </el-form-item>
                  <el-form-item label="评分">
                    <el-select v-model.number="reviewForm.score" style="width: 180px">
                      <el-option v-for="n in 5" :key="n" :label="n + ' 星'" :value="n" />
                    </el-select>
                  </el-form-item>
                  <el-form-item label="评价内容">
                    <el-input
                      v-model="reviewForm.content"
                      type="textarea"
                      :rows="4"
                      maxlength="500"
                      show-word-limit
                      placeholder="说说教练安排、沟通和训练感受"
                    />
                  </el-form-item>
                  <div class="action-row">
                    <el-button @click="fillReviewSample">填充示例</el-button>
                    <el-button type="primary" :loading="reviewing" @click="submitReview">提交评价</el-button>
                  </div>
                </el-form>
              </section>
              <el-alert v-else type="info" show-icon :closable="false" title="请先在左侧列表选择一条已完成预约，再来写评价。" style="margin-top: 14px" />
            </el-tab-pane>
          </el-tabs>
        </main>
      </el-col>
    </el-row>
  </div>

  <el-dialog v-model="coachPickerVisible" title="选择教练" width="760px">
    <div class="coach-picker-dialog">
      <div class="coach-picker-dialog__head">
        <div>
          <strong>当前可预约教练 {{ filteredCoachOptions.length }} 位</strong>
          <p>{{ coachPickerNotice }}</p>
        </div>
        <el-input v-model="coachPickerKeyword" clearable placeholder="按姓名、账号或擅长领域筛选" style="width: 260px" />
      </div>
      <el-empty v-if="!filteredCoachOptions.length && !coachOptionsLoading" description="暂无可选教练">
        <p class="empty-tip">{{ scheduleEmptyReason }}</p>
      </el-empty>
      <div v-else class="coach-picker-grid" v-loading="coachOptionsLoading">
        <button v-for="coach in filteredCoachOptions" :key="coach.coachId" type="button" class="coach-picker-card" @click="selectCoachOption(coach)">
          <div class="coach-picker-card__top">
            <strong>{{ coachDisplayName(coach.coachId) }}</strong>
            <span>教练ID {{ coach.coachId }}</span>
          </div>
          <div class="coach-picker-card__meta">
            <span v-if="coach.expertise">{{ coach.expertise }}</span>
            <span v-if="coach.years !== null && coach.years !== undefined">教龄 {{ coach.years }} 年</span>
            <span v-if="coach.rating !== null && coach.rating !== undefined">评分 {{ coach.rating }}</span>
          </div>
          <div class="coach-picker-card__footer">
            <span>{{ coach.username || '-' }}</span>
            <span v-if="coach.price !== null && coach.price !== undefined">基础价 ¥{{ coach.price }}</span>
          </div>
        </button>
      </div>
    </div>
  </el-dialog>
</template>
'@

$booking = [regex]::Replace($booking, '(?s)<template>.*?</template>', $bookingTemplate)

$booking = $booking.Replace(@'
const coachPickerNotice = computed(() => {
  if (coachOptions.value.length) {
    return "??????????????????????"
  }
  if (coachOptionsUnavailable.value) {
    return "???????????????????????????????????????"
  }
  return "??????????????????????"
})
'@, @'
const coachPickerNotice = computed(() => {
  if (coachOptions.value.length) {
    return "这里展示的是当前存在可预约时间段的教练列表。"
  }
  if (coachOptionsUnavailable.value) {
    return "教练名册接口暂时不可用，当前先根据可预约档期自动整理教练列表，不影响继续预约。"
  }
  return "这里展示的是当前存在可预约时间段的教练列表。"
})
'@)

$booking = $booking.Replace(@'
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
'@, @'
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
  if (lastBooking.value?.bookingStatus === "WAIT_PAY") {
    return "你有一笔待支付预约，请先处理左侧卡片或在列表中完成支付。"
  }
  if (!visibleSchedules.value.length) {
    return "当前没有可选的可预约时间段，先看上方说明或刷新列表。"
  }
  return "← 请从右侧选择一个时间段开始创建。"
})

const isMissingEndpointMessage = (message) => String(message || "").includes("接口不存在")
'@)

$booking = $booking.Replace(@'
const coachPersonas = [
  { title: "????", tags: ["??", "??", "1v1"] },
  { title: "????", tags: ["??", "??", "??"] },
  { title: "????", tags: ["??", "??", "??"] },
  { title: "????", tags: ["??", "???", "???"] }
]
'@, @'
const coachPersonas = [
  { title: "金牌教练", tags: ["增肌", "塑形", "1v1"] },
  { title: "体态训练", tags: ["减脂", "体态", "拉伸"] },
  { title: "力量训练", tags: ["力量", "核心", "进阶"] },
  { title: "康复训练", tags: ["康复", "灵活性", "低冲击"] }
]
'@)

$bookingReplacements = @(
  @('throw new Error(data.message || "???????????")','throw new Error(data.message || "加载预约摘要失败")'),
  @('throw new Error(data.message || "????????")','throw new Error(data.message || "加载教练列表失败")'),
  @('ElMessage.error(err.message || "????????")','ElMessage.error(err.message || "加载教练列表失败")'),
  @('ElMessage.warning("?????????????")','ElMessage.warning("当前教练暂无可预约时间段")'),
  @('ElMessage.info("?????????????????????")','ElMessage.info("已同步到当前待办，请优先处理左侧预约单。")'),
  @('ElMessage.warning("???????????????????")','ElMessage.warning("这个时间段当前不可预约，请换一个可预约档期。")'),
  @('ElMessage.warning("????????????")','ElMessage.warning("请先选择一个时间段")'),
  @('throw new Error(data.message || "????")','throw new Error(data.message || "提交失败")'),
  @('ElMessage.success("????")','ElMessage.success("预约已创建")'),
  @('ElMessage.error(err.message || "????")','ElMessage.error(err.message || "操作失败")'),
  @('ElMessage.warning("???????")','ElMessage.warning("当前没有可处理的预约")'),
  @('throw new Error(data.message || "??????")','throw new Error(data.message || "操作失败")'),
  @('ElMessage.success("???????")','ElMessage.success("模拟支付完成")'),
  @('ElMessage.error(err.message || "??????")','ElMessage.error(err.message || "操作失败")'),
  @('ElMessage.success("?????")','ElMessage.success("预约已完成")'),
  @('ElMessage.warning("??????????")','ElMessage.warning("当前预约无法取消")'),
  @('"???????????????????????????????"','"取消后会释放当前预约占用的名额，确认继续吗？"'),
  @('"??????"','"取消待支付预约"'),
  @('confirmButtonText: "????"','confirmButtonText: "确认取消"'),
  @('cancelButtonText: "???"','cancelButtonText: "先不取消"'),
  @('ElMessage.success("??????????????")','ElMessage.success("预约已取消，名额已释放")'),
  @('throw new Error(data.message || "????????")','throw new Error(data.message || "加载我的预约失败")'),
  @('ElMessage.error(err.message || "????????")','ElMessage.error(err.message || "加载我的预约失败")'),
  @('ElMessage.warning("???????????")','ElMessage.warning("当前没有可评价的已完成预约")'),
  @('reviewForm.content = "?????????????????????"','reviewForm.content = "教练节奏安排得很稳，讲解清楚，训练后反馈也很及时。"'),
  @('ElMessage.warning("??????????")','ElMessage.warning("只有已完成预约才能评价")'),
  @('ElMessage.warning("??????")','ElMessage.warning("请先选择预约")'),
  @('ElMessage.warning("???????")','ElMessage.warning("请填写评价内容")'),
  @('ElMessage.success("????")','ElMessage.success("评价已提交")')
)
foreach ($pair in $bookingReplacements) { $booking = $booking.Replace($pair[0], $pair[1]) }

$ordersTemplate = @'
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
          <el-button type="primary" :loading="ordersLoading" @click="loadOrders">刷新订单</el-button>
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
            <el-input v-model="orderKeyword" class="order-search" clearable placeholder="搜索订单号、类型、状态或金额" />
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
                订单ID {{ selectedOrder.id }}，{{ formatBizType(selectedOrder.bizType) }}，{{ formatOrderStatus(selectedOrder.orderStatus) }}，右侧详情已同步。
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
              <div class="detail-head__actions">
                <el-button
                  v-if="canCancelCurrentBooking"
                  plain
                  type="danger"
                  size="small"
                  :loading="cancelingBooking"
                  @click="cancelCurrentBooking"
                >
                  取消预约
                </el-button>
                <el-button plain size="small" @click="scrollToOrders">换一张订单</el-button>
              </div>
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
                  <span class="tag">{{ sourceEntityLabel }} {{ sourceContext.enrollmentId || '-' }}</span>
                  <span class="tag">订单ID {{ sourceContext.orderId || '-' }}</span>
                </div>
                <p class="source-card__summary">
                  {{ sourceContext.courseTitle ? sourceSummaryLabel + '：' + sourceContext.courseTitle : '来源信息已同步到当前订单工作台。' }}
                </p>
              </article>

              <article class="order-no-card">
                <div>
                  <p class="metric-label">订单ID</p>
                  <p class="order-id">{{ normalized.orderId || '-' }}</p>
                  <p class="metric-label">订单号</p>
                  <p class="order-no">{{ normalized.orderNo }}</p>
                </div>
                <div class="order-no-actions">
                  <el-button size="small" @click="copyOrderNo">复制订单号</el-button>
                </div>
              </article>

              <div class="summary-grid">
                <article class="summary-item"><p class="metric-label">订单ID</p><p class="summary-value">{{ normalized.orderId || '-' }}</p></article>
                <article class="summary-item"><p class="metric-label">{{ sourceEntityLabel }}</p><p class="summary-value">{{ sourceContext?.enrollmentId || normalized.bizId || '-' }}</p></article>
                <article class="summary-item"><p class="metric-label">支付状态</p><p class="summary-value">{{ formatPayStatus(normalized.payStatus) }}</p></article>
                <article class="summary-item"><p class="metric-label">订单状态</p><p class="summary-value">{{ formatOrderStatus(normalized.orderStatus) }}</p></article>
                <article class="summary-item"><p class="metric-label">金额</p><p class="summary-value">{{ formatAmount(normalized.totalAmount) }}</p></article>
                <article class="summary-item"><p class="metric-label">实付</p><p class="summary-value">{{ formatAmount(normalized.paidAmount) }}</p></article>
                <article class="summary-item"><p class="metric-label">退款</p><p class="summary-value">{{ formatAmount(normalized.refundAmount) }}</p></article>
                <article class="summary-item"><p class="metric-label">净支付</p><p class="summary-value">{{ formatAmount(normalized.netPaid) }}</p></article>
                <article class="summary-item"><p class="metric-label">支付渠道</p><p class="summary-value">{{ formatPayChannel(normalized.payChannel) }}</p></article>
                <article class="summary-item"><p class="metric-label">支付时间</p><p class="summary-value">{{ formatDateTime(normalized.payTime) }}</p></article>
                <article class="summary-item"><p class="metric-label">退款状态</p><p class="summary-value">{{ formatPayStatus(normalized.refundStatus) }}</p></article>
                <article class="summary-item"><p class="metric-label">退款时间</p><p class="summary-value">{{ formatDateTime(normalized.refundTime) }}</p></article>
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
'@

$orders = [regex]::Replace($orders, '(?s)<template>.*?</template>', $ordersTemplate)

$orders = $orders.Replace(@'
const backToSourceLabel = computed(() => (sourceFrom.value === "booking" ? "??????" : "??????"))
const sourceEntityLabel = computed(() => (sourceFrom.value === "booking" ? "??ID" : "??ID"))
const sourceSummaryLabel = computed(() => (sourceFrom.value === "booking" ? "??" : "??"))
'@, @'
const backToSourceLabel = computed(() => (sourceFrom.value === "booking" ? "返回我的预约" : "返回最近报名"))
const sourceEntityLabel = computed(() => (sourceFrom.value === "booking" ? "预约ID" : "报名ID"))
const sourceSummaryLabel = computed(() => (sourceFrom.value === "booking" ? "档期" : "课程"))
'@)

$orders = $orders.Replace(@'
    return {
      sourceLabel: "????",
      courseTitle: timeText,
      enrollmentId: Number.isFinite(Number(bookingId)) && Number(bookingId) > 0 ? Number(bookingId) : null,
      orderId: normalized.value.orderId || (route.query.orderId ? Number(route.query.orderId) : null)
    }
'@, @'
    return {
      sourceLabel: "教练预约",
      courseTitle: timeText,
      enrollmentId: Number.isFinite(Number(bookingId)) && Number(bookingId) > 0 ? Number(bookingId) : null,
      orderId: normalized.value.orderId || (route.query.orderId ? Number(route.query.orderId) : null)
    }
'@)
$orders = $orders.Replace(@'
  return {
    sourceLabel: "????",
    courseTitle,
    enrollmentId: Number.isFinite(Number(enrollmentId)) && Number(enrollmentId) > 0 ? Number(enrollmentId) : null,
    orderId: normalized.value.orderId || (route.query.orderId ? Number(route.query.orderId) : null)
  }
'@, @'
  return {
    sourceLabel: "课程报名",
    courseTitle,
    enrollmentId: Number.isFinite(Number(enrollmentId)) && Number(enrollmentId) > 0 ? Number(enrollmentId) : null,
    orderId: normalized.value.orderId || (route.query.orderId ? Number(route.query.orderId) : null)
  }
'@)

$orders = $orders.Replace(@'
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
'@, @'
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
'@)

$orders = $orders.Replace(@'
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
'@, @'
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
'@)

$ordersReplacements = @(
  @('ElMessage.warning("???????????")','ElMessage.warning("当前没有可复制的订单号")'),
  @('ElMessage.success("??????")','ElMessage.success("订单号已复制")'),
  @('ElMessage.error("??????????")','ElMessage.error("复制失败，请手动复制")'),
  @('throw new Error(data.message || "??????")','throw new Error(data.message || "加载订单失败")'),
  @('ElMessage.error(err.message || "??????")','ElMessage.error(err.message || "加载订单失败")'),
  @('ElMessage.warning("?????")','ElMessage.warning("请选择订单")'),
  @('ElMessage.warning("???????????")','ElMessage.warning("当前订单不支持取消预约")'),
  @('"??????????????????????????????"','"取消后会释放这笔待支付预约占用的名额，确认继续吗？"'),
  @('"??????"','"取消待支付预约"'),
  @('confirmButtonText: "????"','confirmButtonText: "确认取消"'),
  @('cancelButtonText: "????"','cancelButtonText: "先不取消"'),
  @('ElMessage.success("???????????")','ElMessage.success("预约已取消，订单已关闭")')
)
foreach ($pair in $ordersReplacements) { $orders = $orders.Replace($pair[0], $pair[1]) }

Save-Utf8NoBom $bookingPath $booking
Save-Utf8NoBom $ordersPath $orders
Write-Host 'done'
