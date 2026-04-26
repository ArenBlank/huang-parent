import fs from "node:fs/promises"

const bookingPath = "D:/DevelopmentLOOK/Idea/idea_project_workspace/huang-parent/frontend-app/src/pages/Booking.vue"
const ordersPath = "D:/DevelopmentLOOK/Idea/idea_project_workspace/huang-parent/frontend-app/src/pages/Orders.vue"

const normalize = (text) => text.replace(/\r\n/g, "\n")

let booking = normalize(await fs.readFile(bookingPath, "utf8"))
let orders = normalize(await fs.readFile(ordersPath, "utf8"))

const bookingTemplate = String.raw`<template>
  <div class="page-stack booking-workspace">
    <section class="booking-stats-bar">
      <div class="booking-stats-copy">
        <p class="quest-kicker">Coach Booking</p>
        <h1 class="section-title">棰勭害鏁欑粌锛屽舰鎴愮ǔ瀹氳缁冭妭寰?/h1>
        <p class="section-sub">绛涢€夈€侀€夋。銆佹敮浠樺拰璇勪环鏀惰繘涓€涓伐浣滃彴锛屼笉鍐嶄竴璺悜涓嬫粴銆?/p>
      </div>
      <div class="booking-stat-pills">
        <span class="tag">褰撳墠鍙害 {{ visibleSchedules.length }}</span>
        <span class="tag">鏈潵鍙绾?{{ effectiveAvailableSchedules }}</span>
        <span class="tag">鎴戠殑棰勭害 {{ myBookings.length }}</span>
      </div>
      <el-button type="primary" :loading="loading" @click="loadSchedules">鍒锋柊棰勭害宸ヤ綔鍙?/el-button>
    </section>

    <el-row :gutter="20" class="booking-dashboard">
      <el-col :xs="24" :md="8">
        <aside class="booking-sidebar">
          <section v-if="lastBooking" class="workspace-card">
            <div class="toolbar">
              <div>
                <h2 class="section-title-sm">褰撳墠寰呭姙</h2>
                <p class="section-sub">蹇€熷鐞嗕綘鎵嬪ご鐨勯绾﹀崟銆?/p>
              </div>
            </div>
            <div class="detail detail-card">
              <div class="detail-card__title">褰撳墠寰呭鐞嗛绾?/div>
              <div class="detail-card__headline">{{ formatScheduleTime(lastBooking) }}</div>
              <div class="detail-chip-row">
                <span class="tag">棰勭害ID {{ lastBooking.bookingId || lastBooking.id }}</span>
                <span class="tag">璁㈠崟ID {{ lastBooking.orderId || '-' }}</span>
                <span class="tag">棰勭害鐘舵€?{{ formatBookingStatus(lastBooking.bookingStatus) }}</span>
                <span class="tag">鏀粯鐘舵€?{{ formatPayStatus(lastBooking.payStatus) }}</span>
              </div>
              <div class="detail-list">
                <div>璁㈠崟鍙凤細{{ lastBooking.orderNo || '-' }}</div>
                <div>閲戦锛歿{ lastBooking.amount ?? '-' }}</div>
                <div>鏁欑粌锛歿{ bookingCoachDisplayName(lastBooking) }}</div>
              </div>
              <div class="action-row action-row--wrap">
                <el-button type="primary" size="small" :loading="paying" :disabled="!canMockPay(lastBooking)" @click="mockPay">妯℃嫙鏀粯</el-button>
                <el-button
                  type="danger"
                  size="small"
                  plain
                  :loading="isCancellingBooking(lastBooking)"
                  :disabled="!canCancelBooking(lastBooking)"
                  @click="cancelBooking(lastBooking)"
                >
                  鍙栨秷棰勭害
                </el-button>
                <el-button type="success" size="small" :loading="completing" :disabled="!canCompleteBooking(lastBooking)" @click="completeBooking">
                  纭瀹屾垚
                </el-button>
                <el-button size="small" :disabled="!lastBooking?.orderId" @click="goToOrder(lastBooking)">璁㈠崟璇︽儏</el-button>
              </div>
            </div>
          </section>

          <section ref="createBookingRef" class="workspace-card create-card">
            <div class="toolbar">
              <div>
                <h2 class="section-title-sm">鍒涘缓棰勭害</h2>
                <p class="section-sub">閫変腑鍙充晶妗ｆ湡鍚庯紝杩欓噷浼氬悓姝ユ洿鏂般€?/p>
              </div>
            </div>
            <el-empty v-if="!selected" :description="createBookingHint" />
            <div v-else class="detail detail-card selected-schedule-card">
              <div class="detail-card__title">宸查€夋椂闂存</div>
              <div class="detail-card__headline">{{ formatScheduleTime(selected) }}</div>
              <div class="detail-chip-row">
                <span class="tag">鏃堕棿娈礗D {{ selected.id }}</span>
                <span class="tag">鏁欑粌 {{ coachDisplayName(selected.coachId) }}</span>
                <span class="tag">浠锋牸 {{ selected.price }}</span>
                <span class="tag">浣欓噺 {{ remainingSlots(selected) }}</span>
                <span class="tag">鐘舵€?{{ scheduleStatus(selected).text }}</span>
              </div>
              <el-button type="success" class="create-submit" :disabled="!selected || selectedUnavailable" :loading="submitting" @click="createBooking">
                鎻愪氦棰勭害
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
                <h2 class="section-title-sm">鍙绾︽椂闂存绛涢€?/h2>
                <p class="section-sub">鎸夋暀缁冧笌鏃ユ湡绛涢€夊綋鍓嶈繕鑳介绾︾殑鏃堕棿娈点€?/p>
              </div>
            </div>
            <el-form label-position="top">
              <div class="booking-filter-row">
                <el-form-item label="鏁欑粌">
                  <div class="coach-picker-field">
                    <button type="button" class="coach-picker-trigger" @click="openCoachPicker">
                      <span v-if="selectedCoachLabel">{{ selectedCoachLabel }}</span>
                      <span v-else class="muted">浠庢暀缁冨垪琛ㄤ腑閫夋嫨</span>
                    </button>
                    <el-button
                      v-if="coachId !== null && coachId !== undefined && coachId !== ''"
                      text
                      type="primary"
                      @click="setCoach(null)"
                    >
                      娓呯┖
                    </el-button>
                  </div>
                </el-form-item>
                <el-form-item label="棰勭害鏈嶅姟鏃ユ湡">
                  <el-date-picker v-model="date" type="date" value-format="YYYY-MM-DD" placeholder="绛涢€夋煇涓€澶╃殑鍙绾︽椂闂存" />
                </el-form-item>
                <el-form-item label="蹇€熸搷浣?>
                  <div class="quick-wrap">
                    <el-button size="small" :type="coachId === null ? 'primary' : 'default'" @click="setCoach(null)">鍏ㄩ儴</el-button>
                    <el-button size="small" plain @click="openCoachPicker">閫夋嫨鏁欑粌</el-button>
                  </div>
                </el-form-item>
                <el-form-item label="鎿嶄綔" class="booking-filter-action">
                  <el-button type="primary" @click="loadSchedules">鏌ヨ</el-button>
                </el-form-item>
              </div>
            </el-form>
            <p class="muted">杩欓噷鏄剧ず鐨勬槸鐢ㄦ埛褰撳墠鐪熸杩樿兘棰勭害鐨勬椂闂存锛屾弧鍛樺拰杩囨湡妗ｆ湡涓嶄細鍐嶆贩杩涙潵銆?/p>
            <div class="detail-chip-row booking-summary-row">
              <span class="tag">鍙绾︽暀缁?{{ coachPickerOptions.length }}</span>
              <span class="tag">鏈潵鏃堕棿娈?{{ scheduleSummary.futureSchedules ?? 0 }}</span>
              <span class="tag">鍙绾︽椂闂存 {{ effectiveAvailableSchedules }}</span>
              <span class="tag">鏈€杩戜竴娆″紑鏀炬椂闂?{{ formatDate(scheduleSummary.lastScheduleDate) }}</span>
            </div>
            <p class="summary-hint">{{ scheduleSummaryHint }}</p>
          </section>

          <el-tabs v-model="activeBookingTab" type="border-card" class="booking-tabs">
            <el-tab-pane label="鍙绾︽。鏈? name="schedules">
              <div v-if="hasCoachFilter" class="coach-list-block">
                <div class="toolbar coach-list-toolbar">
                  <div>
                    <h3 class="section-title-sm">鏁欑粌姒傝</h3>
                    <p class="section-sub">浠呭湪鎸囧畾鏁欑粌鍚庢樉绀猴紝閬垮厤榛樿椤甸潰淇℃伅杩囪浇銆?/p>
                  </div>
                  <span class="tag">鏁欑粌 {{ coachCards.length }}</span>
                </div>
                <el-empty v-if="!loading && !coachCards.length" description="鏆傛棤鍙绾︽暀缁? />
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
                        <span class="coach-card__rating">{{ coach.availableCount ? '鍙害' : '婊″憳' }}</span>
                      </div>
                      <div class="coach-card__tags">
                        <el-tag v-for="tag in coach.tags" :key="tag" size="small" effect="plain">{{ tag }}</el-tag>
                      </div>
                      <div class="coach-card__highlight">鏈€杩戝彲绾?{{ coach.nextTime }}</div>
                      <div class="coach-card__meta">
                        <span>鍩虹浠?楼{{ coach.minPrice }}</span>
                        <span>褰撳墠浣欓噺 {{ coach.remaining }}</span>
                        <span>鍙害妗ｆ湡 {{ coach.availableCount }}</span>
                      </div>
                      <div class="coach-card__actions">
                        <el-button size="small" round plain @click="viewCoachReviews(coach)">鏌ョ湅璇勪环</el-button>
                        <el-button size="small" round type="primary" :disabled="!coach.targetSchedule" @click="bookCoach(coach)">棰勭害</el-button>
                      </div>
                    </div>
                  </article>
                </div>
              </div>

              <div class="schedule-grid-shell" v-loading="loading">
                <el-empty v-if="!loading && !visibleSchedules.length" description="鏆傛棤鍙绾︽椂闂存">
                  <div class="empty-actions">
                    <el-button size="small" @click="loadSchedules">閲嶈瘯</el-button>
                    <el-button size="small" @click="goTo('/courses')">鍘昏绋嬫姤鍚?/el-button>
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
                      <span>鏁欑粌 {{ bookingCoachDisplayName(row) }}</span>
                      <span>浣欓噺 {{ remainingSlots(row) }}</span>
                      <span>楼{{ row.price }}</span>
                    </div>
                  </button>
                </div>
              </div>
            </el-tab-pane>

            <el-tab-pane label="鎴戠殑棰勭害璁板綍" name="bookings">
              <div class="toolbar bookings-tab-toolbar">
                <div>
                  <h3 class="section-title-sm">鎴戠殑棰勭害</h3>
                  <p class="section-sub">宸插畬鎴愰绾﹀彲鐩存帴鍙戣捣璇勪环銆?/p>
                </div>
                <div class="action-row">
                  <el-button size="small" @click="fillReviewFromCompleted">鑷姩閫夋嫨宸插畬鎴愰绾?/el-button>
                  <el-button type="primary" size="small" :loading="myLoading" @click="loadMyBookings">鍒锋柊</el-button>
                </div>
              </div>
              <el-empty v-if="!myBookings.length && !myLoading" description="鏆傛棤棰勭害璁板綍" />
              <el-table v-else :data="myBookings" v-loading="myLoading" style="width: 100%">
                <el-table-column prop="id" label="棰勭害ID" width="90" />
                <el-table-column label="棰勭害鏃堕棿" min-width="220">
                  <template #default="{ row }">
                    <div class="booking-table__slot">
                      <strong>{{ formatScheduleTime(row) }}</strong>
                      <span class="muted">鏁欑粌 {{ bookingCoachDisplayName(row) }}</span>
                    </div>
                  </template>
                </el-table-column>
                <el-table-column label="棰勭害鐘舵€? width="120">
                  <template #default="{ row }">{{ formatBookingStatus(row.bookingStatus) }}</template>
                </el-table-column>
                <el-table-column label="鏀粯鐘舵€? width="120">
                  <template #default="{ row }">{{ formatPayStatus(row.payStatus) }}</template>
                </el-table-column>
                <el-table-column label="璁㈠崟" min-width="220">
                  <template #default="{ row }">
                    <div class="booking-table__order">
                      <strong class="order-no">{{ row.orderNo || ('璁㈠崟ID ' + (row.orderId || '-')) }}</strong>
                      <div class="booking-table__order-actions">
                        <span class="muted">閲戦 {{ formatAmount(row.amount) }}</span>
                        <el-button size="small" type="primary" plain :disabled="!row.orderId" @click="goToOrder(row)">鏌ョ湅璁㈠崟璇︽儏</el-button>
                        <el-button
                          size="small"
                          type="danger"
                          plain
                          :loading="isCancellingBooking(row)"
                          :disabled="!canCancelBooking(row)"
                          @click="cancelBooking(row)"
                        >
                          鍙栨秷棰勭害
                        </el-button>
                      </div>
                    </div>
                  </template>
                </el-table-column>
                <el-table-column label="鍒涘缓鏃堕棿" min-width="170">
                  <template #default="{ row }">{{ formatDateTime(row.createTime) }}</template>
                </el-table-column>
                <el-table-column label="璇勪环" width="120">
                  <template #default="{ row }">
                    <el-button size="small" type="primary" plain :disabled="row.bookingStatus !== 'COMPLETED'" @click="openReview(row)">
                      鍐欒瘎浠?                    </el-button>
                  </template>
                </el-table-column>
              </el-table>

              <section v-if="selectedReviewBooking" class="workspace-card review-panel">
                <div class="toolbar">
                  <div>
                    <h2 class="section-title-sm">鍐欒瘎浠?/h2>
                    <p class="section-sub">閫変腑宸插畬鎴愰绾﹀悗濉啓璇勪环銆?/p>
                  </div>
                </div>
                <div v-if="selectedReviewBooking" class="detail">
                  <div>棰勭害ID锛歿{ selectedReviewBooking.id }}</div>
                  <div class="muted">棰勭害鐘舵€侊細{{ formatBookingStatus(selectedReviewBooking.bookingStatus) }}</div>
                  <div class="muted">璁㈠崟ID锛歿{ selectedReviewBooking.orderId }}</div>
                  <div class="muted">棰勭害鏃堕棿锛歿{ formatScheduleTime(selectedReviewBooking) }}</div>
                </div>
                <el-form :model="reviewForm" label-position="top">
                  <el-form-item label="棰勭害ID">
                    <el-input v-model="reviewForm.bookingId" disabled />
                  </el-form-item>
                  <el-form-item label="璇勫垎">
                    <el-select v-model.number="reviewForm.score" style="width: 180px">
                      <el-option v-for="n in 5" :key="n" :label="n + ' 鏄?" :value="n" />
                    </el-select>
                  </el-form-item>
                  <el-form-item label="璇勪环鍐呭">
                    <el-input
                      v-model="reviewForm.content"
                      type="textarea"
                      :rows="4"
                      maxlength="500"
                      show-word-limit
                      placeholder="璇磋鏁欑粌瀹夋帓銆佹矡閫氬拰璁粌鎰熷彈"
                    />
                  </el-form-item>
                  <div class="action-row">
                    <el-button @click="fillReviewSample">濉厖绀轰緥</el-button>
                    <el-button type="primary" :loading="reviewing" @click="submitReview">鎻愪氦璇勪环</el-button>
                  </div>
                </el-form>
              </section>
              <el-alert v-else type="info" show-icon :closable="false" title="璇峰厛鍦ㄥ乏渚у垪琛ㄩ€夋嫨涓€鏉″凡瀹屾垚棰勭害锛屽啀鏉ュ啓璇勪环銆? style="margin-top: 14px" />
            </el-tab-pane>
          </el-tabs>
        </main>
      </el-col>
    </el-row>
  </div>

  <el-dialog v-model="coachPickerVisible" title="閫夋嫨鏁欑粌" width="760px">
    <div class="coach-picker-dialog">
      <div class="coach-picker-dialog__head">
        <div>
          <strong>褰撳墠鍙绾︽暀缁?{{ filteredCoachOptions.length }} 浣?/strong>
          <p>{{ coachPickerNotice }}</p>
        </div>
        <el-input v-model="coachPickerKeyword" clearable placeholder="鎸夊鍚嶃€佽处鍙锋垨鎿呴暱棰嗗煙绛涢€? style="width: 260px" />
      </div>
      <el-empty v-if="!filteredCoachOptions.length && !coachOptionsLoading" description="鏆傛棤鍙€夋暀缁?>
        <p class="empty-tip">{{ scheduleEmptyReason }}</p>
      </el-empty>
      <div v-else class="coach-picker-grid" v-loading="coachOptionsLoading">
        <button v-for="coach in filteredCoachOptions" :key="coach.coachId" type="button" class="coach-picker-card" @click="selectCoachOption(coach)">
          <div class="coach-picker-card__top">
            <strong>{{ coachDisplayName(coach.coachId) }}</strong>
            <span>鏁欑粌ID {{ coach.coachId }}</span>
          </div>
          <div class="coach-picker-card__meta">
            <span v-if="coach.expertise">{{ coach.expertise }}</span>
            <span v-if="coach.years !== null && coach.years !== undefined">鏁欓緞 {{ coach.years }} 骞?/span>
            <span v-if="coach.rating !== null && coach.rating !== undefined">璇勫垎 {{ coach.rating }}</span>
          </div>
          <div class="coach-picker-card__footer">
            <span>{{ coach.username || '-' }}</span>
            <span v-if="coach.price !== null && coach.price !== undefined">鍩虹浠?楼{{ coach.price }}</span>
          </div>
        </button>
      </div>
    </div>
  </el-dialog>
</template>`

booking = booking.replace(/<template>[\s\S]*?<\/template>/, bookingTemplate)

const replaceAll = (content, pairs) => {
  let next = content
  for (const [oldText, newText] of pairs) {
    next = next.replace(oldText, newText)
  }
  return next
}

booking = replaceAll(booking, [
  [String.raw`const coachPickerNotice = computed(() => {
  if (coachOptions.value.length) {
    return "??????????????????????"
  }
  if (coachOptionsUnavailable.value) {
    return "???????????????????????????????????????"
  }
  return "??????????????????????"
})`, String.raw`const coachPickerNotice = computed(() => {
  if (coachOptions.value.length) {
    return "杩欓噷灞曠ず鐨勬槸褰撳墠瀛樺湪鍙绾︽椂闂存鐨勬暀缁冨垪琛ㄣ€?
  }
  if (coachOptionsUnavailable.value) {
    return "鏁欑粌鍚嶅唽鎺ュ彛鏆傛椂涓嶅彲鐢紝褰撳墠鍏堟牴鎹彲棰勭害妗ｆ湡鑷姩鏁寸悊鏁欑粌鍒楄〃锛屼笉褰卞搷缁х画棰勭害銆?
  }
  return "杩欓噷灞曠ず鐨勬槸褰撳墠瀛樺湪鍙绾︽椂闂存鐨勬暀缁冨垪琛ㄣ€?
})`],
  [String.raw`const scheduleEmptyReason = computed(() => {
  if (date.value && effectiveAvailableSchedules.value > 0) {
    return 
\`???? \${date.value} ???????????????? \${effectiveAvailableSchedules.value} ??????????\`
  }
  if (!scheduleSummary.value?.futureSchedules) {
    const lastDate = formatDate(scheduleSummary.value?.lastScheduleDate)
    return lastDate === "-"
      ? "???????????????????????????????"
      : 
\`?????????????????????????? \${lastDate}??????????????\`
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
\`???????????? \${nextDate} ????????????????????????\`
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

const isMissingEndpointMessage = (message) => String(message || "").includes("?????")`, String.raw`const scheduleEmptyReason = computed(() => {
  if (date.value && effectiveAvailableSchedules.value > 0) {
    return \`褰撳墠鏃ユ湡 \${date.value} 娌℃湁鍙绾︽椂闂存锛屾竻绌烘棩鏈熷悗浠嶆湁 \${effectiveAvailableSchedules.value} 涓湭鏉ュ彲棰勭害鏃堕棿娈点€俓`
  }
  if (!scheduleSummary.value?.futureSchedules) {
    const lastDate = formatDate(scheduleSummary.value?.lastScheduleDate)
    return lastDate === "-"
      ? "褰撳墠绯荤粺閲岃繕娌℃湁浠讳綍鏁欑粌鍙绾︽椂闂存锛岃鍏堝湪绠＄悊绔垱寤烘椂闂存銆?
      : \`褰撳墠绯荤粺閲屾病鏈夋湭鏉ュ彲棰勭害鏃堕棿娈碉紝鏈€杩戜竴娆″紑鏀炬椂闂村仠鍦?\${lastDate}銆傝鍏堝湪绠＄悊绔ˉ鍏呮柊鏃堕棿娈点€俓`
  }
  if (!effectiveAvailableSchedules.value) {
    return "鏈潵鏃堕棿娈佃櫧鐒跺瓨鍦紝浣嗗綋鍓嶉兘宸叉弧鍛橈紝鎴栬€呬綘宸茬粡棰勭害瀹岃嚜宸辫繕鑳介€夌殑鏃堕棿娈点€?
  }
  return "褰撳墠绛涢€夋潯浠朵笅娌℃湁鍙绾︽椂闂存锛屽彲浠ヨ皟鏁存暀缁冩垨鏃ユ湡閲嶆柊鏌ョ湅銆?
})

const scheduleSummaryHint = computed(() => {
  const nextDate = formatDate(scheduleSummary.value?.nextScheduleDate)
  if (nextDate !== "-") {
    return \`涓嬩竴鎵规湭鏉ュ彲棰勭害鏃堕棿娈典粠 \${nextDate} 寮€濮嬶紱濡傛灉鍒楄〃杩樻槸绌猴紝閫氬父鏄綘褰撳墠绛涢€夋潯浠惰繃涓ャ€俓`
  }
  return "杩欓噷灞曠ず鐨勬槸瀹炴椂鍙绾︽椂闂存憳瑕侊紝涓嶆槸鍐欐鐨勬紨绀烘暟瀛椼€?
})

const createBookingHint = computed(() => {
  if (lastBooking.value?.bookingStatus === "WAIT_PAY") {
    return "浣犳湁涓€绗斿緟鏀粯棰勭害锛岃鍏堝鐞嗗乏渚у崱鐗囨垨鍦ㄥ垪琛ㄤ腑瀹屾垚鏀粯銆?
  }
  if (!visibleSchedules.value.length) {
    return "褰撳墠娌℃湁鍙€夌殑鍙绾︽椂闂存锛屽厛鐪嬩笂鏂硅鏄庢垨鍒锋柊鍒楄〃銆?
  }
  return "鈫?璇蜂粠鍙充晶閫夋嫨涓€涓椂闂存寮€濮嬪垱寤恒€?
})

const isMissingEndpointMessage = (message) => String(message || "").includes("鎺ュ彛涓嶅瓨鍦?)`],
  [String.raw`const coachPersonas = [
  { title: "????", tags: ["??", "??", "1v1"] },
  { title: "????", tags: ["??", "??", "??"] },
  { title: "????", tags: ["??", "??", "??"] },
  { title: "????", tags: ["??", "???", "???"] }
]`, String.raw`const coachPersonas = [
  { title: "閲戠墝鏁欑粌", tags: ["澧炶倢", "濉戝舰", "1v1"] },
  { title: "浣撴€佽缁?, tags: ["鍑忚剛", "浣撴€?, "鎷変几"] },
  { title: "鍔涢噺璁粌", tags: ["鍔涢噺", "鏍稿績", "杩涢樁"] },
  { title: "搴峰璁粌", tags: ["搴峰", "鐏垫椿鎬?, "浣庡啿鍑?] }
]`],
  ['throw new Error(data.message || "???????????")', 'throw new Error(data.message || "鍔犺浇棰勭害鎽樿澶辫触")'],
  ['throw new Error(data.message || "????????")', 'throw new Error(data.message || "鍔犺浇鏁欑粌鍒楄〃澶辫触")'],
  ['ElMessage.error(err.message || "????????")', 'ElMessage.error(err.message || "鍔犺浇鏁欑粌鍒楄〃澶辫触")'],
  ['ElMessage.warning("?????????????")', 'ElMessage.warning("褰撳墠鏁欑粌鏆傛棤鍙绾︽椂闂存")'],
  ['ElMessage.info("?????????????????????")', 'ElMessage.info("宸插悓姝ュ埌褰撳墠寰呭姙锛岃浼樺厛澶勭悊宸︿晶棰勭害鍗曘€?)'],
  ['ElMessage.warning("???????????????????")', 'ElMessage.warning("杩欎釜鏃堕棿娈靛綋鍓嶄笉鍙绾︼紝璇锋崲涓€涓彲棰勭害妗ｆ湡銆?)'],
  ['ElMessage.warning("????????????")', 'ElMessage.warning("璇峰厛閫夋嫨涓€涓椂闂存")'],
  ['throw new Error(data.message || "????")', 'throw new Error(data.message || "鎻愪氦澶辫触")'],
  ['ElMessage.success("????")', 'ElMessage.success("棰勭害宸插垱寤?)'],
  ['ElMessage.error(err.message || "????")', 'ElMessage.error(err.message || "鎿嶄綔澶辫触")'],
  ['ElMessage.warning("???????")', 'ElMessage.warning("褰撳墠娌℃湁鍙鐞嗙殑棰勭害")'],
  ['throw new Error(data.message || "??????")', 'throw new Error(data.message || "鎿嶄綔澶辫触")'],
  ['ElMessage.success("???????")', 'ElMessage.success("妯℃嫙鏀粯瀹屾垚")'],
  ['ElMessage.error(err.message || "??????")', 'ElMessage.error(err.message || "鎿嶄綔澶辫触")'],
  ['ElMessage.success("?????")', 'ElMessage.success("棰勭害宸插畬鎴?)'],
  ['ElMessage.warning("??????????")', 'ElMessage.warning("褰撳墠棰勭害鏃犳硶鍙栨秷")'],
  ['"???????????????????????????????"', '"鍙栨秷鍚庝細閲婃斁褰撳墠棰勭害鍗犵敤鐨勫悕棰濓紝纭缁х画鍚楋紵"'],
  ['"??????"', '"鍙栨秷寰呮敮浠橀绾?'],
  ['confirmButtonText: "????"', 'confirmButtonText: "纭鍙栨秷"'],
  ['cancelButtonText: "???"', 'cancelButtonText: "鍏堜笉鍙栨秷"'],
  ['ElMessage.success("??????????????")', 'ElMessage.success("棰勭害宸插彇娑堬紝鍚嶉宸查噴鏀?)'],
  ['throw new Error(data.message || "????????")', 'throw new Error(data.message || "鍔犺浇鎴戠殑棰勭害澶辫触")'],
  ['ElMessage.error(err.message || "????????")', 'ElMessage.error(err.message || "鍔犺浇鎴戠殑棰勭害澶辫触")'],
  ['ElMessage.warning("???????????")', 'ElMessage.warning("褰撳墠娌℃湁鍙瘎浠风殑宸插畬鎴愰绾?)'],
  ['reviewForm.content = "?????????????????????"', 'reviewForm.content = "鏁欑粌鑺傚瀹夋帓寰楀緢绋筹紝璁茶В娓呮锛岃缁冨悗鍙嶉涔熷緢鍙婃椂銆?'],
  ['ElMessage.warning("??????????")', 'ElMessage.warning("鍙湁宸插畬鎴愰绾︽墠鑳借瘎浠?)'],
  ['ElMessage.warning("??????")', 'ElMessage.warning("璇峰厛閫夋嫨棰勭害")'],
  ['ElMessage.warning("???????")', 'ElMessage.warning("璇峰～鍐欒瘎浠峰唴瀹?)'],
  ['ElMessage.success("????")', 'ElMessage.success("璇勪环宸叉彁浜?)']
])

const ordersTemplate = String.raw`<template>
  <div class="page-stack orders-page">
    <section class="hero-panel">
      <div class="order-hero">
        <div>
          <p class="quest-kicker">Orders Center</p>
          <h1 class="hero-title">璁㈠崟鍒楄〃鐣欏湪宸︿晶锛岃鎯呭浐瀹氬湪鍙充晶</h1>
          <p class="hero-subtitle">鍒囨崲璁㈠崟鏃朵笉鍐嶆妸浜哄甫鍒伴暱椤甸潰涓嬫柟銆傜幇鍦ㄥ彲浠ヨ竟鐪嬭鍗曞垪琛紝杈瑰湪鏃佽竟鏌ョ湅璇︽儏銆佽祫閲戣鏄庡拰閫€娆惧師鍥犮€?/p>
          <div class="hero-badges">
            <span class="badge-pill is-dark">璁㈠崟 {{ orders.length }}</span>
            <span class="badge-pill is-dark">鏄庣粏 {{ normalized.items.length }}</span>
            <span class="badge-pill is-dark">褰撳墠鐘舵€?{{ formatOrderStatus(normalized.orderStatus) }}</span>
          </div>
        </div>
        <div class="toolbar-actions">
          <el-button v-if="showBackToSource" @click="backToSource">{{ backToSourceLabel }}</el-button>
          <el-button plain @click="scrollToOrders">鍥炲埌璁㈠崟鍒楄〃</el-button>
          <el-button type="primary" :loading="ordersLoading" @click="loadOrders">鍒锋柊璁㈠崟</el-button>
        </div>
      </div>
    </section>

    <div class="orders-workspace">
      <section ref="ordersRef" class="card orders-browser">
        <div class="toolbar">
          <div>
            <h2 class="section-title">璁㈠崟娴忚鍖?/h2>
            <p class="section-sub">璁㈠崟鍒楄〃淇濇寔鍦ㄥ綋鍓嶈閲庨噷锛屾煡鐪嬩竴鍗曞悗杩樿兘绔嬪嵆鍒囨崲涓嬩竴鍗曘€?/p>
          </div>
        </div>

        <el-empty v-if="!orders.length && !ordersLoading" description="鏆傛棤璁㈠崟锛屽彲鍏堝畬鎴愭姤鍚嶆垨棰勭害">
          <div class="empty-actions">
            <el-button size="small" @click="go('/courses')">鍘绘姤鍚嶈绋?/el-button>
            <el-button size="small" @click="go('/booking')">鍘婚绾︽暀缁?/el-button>
          </div>
        </el-empty>

        <template v-else>
          <div class="orders-controls">
            <el-input v-model="orderKeyword" class="order-search" clearable placeholder="鎼滅储璁㈠崟鍙枫€佺被鍨嬨€佺姸鎬佹垨閲戦" />
            <div class="orders-stats">
              <span class="badge-pill is-dark">鏄剧ず {{ filteredOrders.length }}</span>
              <span class="badge-pill">鎬昏 {{ orders.length }}</span>
            </div>
          </div>

          <div v-if="selectedOrder" class="orders-selected-banner">
            <div>
              <p class="selected-caption">褰撳墠閫変腑璁㈠崟</p>
              <strong>{{ selectedOrder.orderNo }}</strong>
              <p>
                璁㈠崟ID {{ selectedOrder.id }}锛寋{ formatBizType(selectedOrder.bizType) }}锛寋{ formatOrderStatus(selectedOrder.orderStatus) }}锛屽彸渚ц鎯呭凡鍚屾銆?              </p>
            </div>
            <div class="orders-selected-chips">
              <span class="tag">璁㈠崟ID {{ selectedOrder.id }}</span>
              <span class="tag">閲戦 {{ formatAmount(selectedOrder.totalAmount) }}</span>
              <span class="tag">鏀粯 {{ formatPayStatus(selectedOrder.payStatus) }}</span>
              <span class="tag">鏃堕棿 {{ formatDateTime(selectedOrder.createTime) }}</span>
            </div>
          </div>

          <el-empty v-if="!filteredOrders.length && !ordersLoading" description="娌℃湁鍖归厤璁㈠崟">
            <div class="empty-actions">
              <el-button size="small" @click="orderKeyword = ''">娓呯┖鎼滅储</el-button>
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
                    <p class="order-card__eyebrow">{{ formatBizType(row.bizType) }} 路 璁㈠崟ID {{ row.id }}</p>
                    <strong>{{ row.orderNo }}</strong>
                  </div>
                  <span class="order-card__amount">{{ formatAmount(row.totalAmount) }}</span>
                </div>
                <div class="order-card__meta">
                  <span class="tag">鏀粯 {{ formatPayStatus(row.payStatus) }}</span>
                  <span class="tag">璁㈠崟 {{ formatOrderStatus(row.orderStatus) }}</span>
                </div>
                <div class="order-card__foot">
                  <span>{{ formatDateTime(row.createTime) }}</span>
                  <span class="order-card__hint">鏌ョ湅璇︽儏</span>
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
                <p class="section-eyebrow">璁㈠崟璇︽儏</p>
                <h2 class="section-title-sm">褰撳墠璁㈠崟宸ヤ綔鍙?/h2>
                <p class="section-sub">璇︽儏鍥哄畾鏄剧ず鍦ㄥ彸渚э紝璁㈠崟鍒囨崲鏃朵笉闇€瑕佸弽澶嶄笂涓嬫粴鍔ㄩ〉闈€?/p>
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
                  鍙栨秷棰勭害
                </el-button>
                <el-button plain size="small" @click="scrollToOrders">鎹竴寮犺鍗?/el-button>
              </div>
            </div>

            <el-empty v-if="orders.length && !detail && !loading" description="璇烽€夋嫨涓€鏉¤鍗曟煡鐪嬭鎯? />

            <div v-else-if="detail" class="detail-stack" v-loading="loading">
              <article v-if="sourceContext" class="source-card">
                <div class="source-card__head">
                  <div>
                    <p class="section-eyebrow">鏉ユ簮瀹氫綅</p>
                    <h3 class="section-title-sm">杩欏氨鏄綘鍒氭墠鐐瑰紑鐨勯偅涓€鍗?/h3>
                  </div>
                </div>
                <div class="source-card__chips">
                  <span class="tag">鏉ユ簮 {{ sourceContext.sourceLabel }}</span>
                  <span class="tag">{{ sourceEntityLabel }} {{ sourceContext.enrollmentId || '-' }}</span>
                  <span class="tag">璁㈠崟ID {{ sourceContext.orderId || '-' }}</span>
                </div>
                <p class="source-card__summary">
                  {{ sourceContext.courseTitle ? sourceSummaryLabel + '锛? + sourceContext.courseTitle : '鏉ユ簮淇℃伅宸插悓姝ュ埌褰撳墠璁㈠崟宸ヤ綔鍙般€? }}
                </p>
              </article>

              <article class="order-no-card">
                <div>
                  <p class="metric-label">璁㈠崟ID</p>
                  <p class="order-id">{{ normalized.orderId || '-' }}</p>
                  <p class="metric-label">璁㈠崟鍙?/p>
                  <p class="order-no">{{ normalized.orderNo }}</p>
                </div>
                <div class="order-no-actions">
                  <el-button size="small" @click="copyOrderNo">澶嶅埗璁㈠崟鍙?/el-button>
                </div>
              </article>

              <div class="summary-grid">
                <article class="summary-item"><p class="metric-label">璁㈠崟ID</p><p class="summary-value">{{ normalized.orderId || '-' }}</p></article>
                <article class="summary-item"><p class="metric-label">{{ sourceEntityLabel }}</p><p class="summary-value">{{ sourceContext?.enrollmentId || normalized.bizId || '-' }}</p></article>
                <article class="summary-item"><p class="metric-label">鏀粯鐘舵€?/p><p class="summary-value">{{ formatPayStatus(normalized.payStatus) }}</p></article>
                <article class="summary-item"><p class="metric-label">璁㈠崟鐘舵€?/p><p class="summary-value">{{ formatOrderStatus(normalized.orderStatus) }}</p></article>
                <article class="summary-item"><p class="metric-label">閲戦</p><p class="summary-value">{{ formatAmount(normalized.totalAmount) }}</p></article>
                <article class="summary-item"><p class="metric-label">瀹炰粯</p><p class="summary-value">{{ formatAmount(normalized.paidAmount) }}</p></article>
                <article class="summary-item"><p class="metric-label">閫€娆?/p><p class="summary-value">{{ formatAmount(normalized.refundAmount) }}</p></article>
                <article class="summary-item"><p class="metric-label">鍑€鏀粯</p><p class="summary-value">{{ formatAmount(normalized.netPaid) }}</p></article>
                <article class="summary-item"><p class="metric-label">鏀粯娓犻亾</p><p class="summary-value">{{ formatPayChannel(normalized.payChannel) }}</p></article>
                <article class="summary-item"><p class="metric-label">鏀粯鏃堕棿</p><p class="summary-value">{{ formatDateTime(normalized.payTime) }}</p></article>
                <article class="summary-item"><p class="metric-label">閫€娆剧姸鎬?/p><p class="summary-value">{{ formatPayStatus(normalized.refundStatus) }}</p></article>
                <article class="summary-item"><p class="metric-label">閫€娆炬椂闂?/p><p class="summary-value">{{ formatDateTime(normalized.refundTime) }}</p></article>
              </div>

              <article class="detail-block">
                <div class="detail-block__head">
                  <div>
                    <p class="section-eyebrow">璁㈠崟鏄庣粏</p>
                    <h3 class="section-title-sm">璐圭敤缁勬垚</h3>
                  </div>
                </div>
                <div class="data-shell">
                  <el-table :data="normalized.items" style="width: 100%" size="small">
                    <el-table-column prop="itemName" label="鍚嶇О" />
                    <el-table-column prop="itemType" label="绫诲瀷" width="120" />
                    <el-table-column prop="quantity" label="鏁伴噺" width="90" />
                    <el-table-column prop="price" label="鍗曚环" width="120" />
                    <el-table-column prop="amount" label="閲戦" width="120" />
                  </el-table>
                </div>
              </article>

              <article class="finance-card">
                <p class="section-eyebrow">璧勯噾璇存槑</p>
                <h3 class="section-title-sm finance-main">
                  {{ formatFinanceDisplay(normalized.financeSummary.displayText, normalized.refundStatus, normalized.payStatus, normalized.orderStatus) }}
                </h3>
                <p class="finance-sub">
                  {{ formatFinanceExplain(normalized.financeSummary.statusExplain, normalized.refundStatus, normalized.payStatus, normalized.orderStatus) }}
                </p>
                <div class="finance-grid">
                  <div class="finance-line"><span>鐘舵€佹枃妗?/span><strong>{{ formatFinanceField(normalized.financeSummary.statusText) }}</strong></div>
                  <div class="finance-line"><span>鐘舵€佹彁绀?/span><strong>{{ formatFinanceField(normalized.financeSummary.statusHint) }}</strong></div>
                  <div class="finance-line"><span>闃舵</span><strong>{{ formatFinanceField(normalized.financeSummary.stage) }}</strong></div>
                  <div class="finance-line"><span>璇存槑</span><strong>{{ formatFinanceField(normalized.financeSummary.statusExplain) }}</strong></div>
                </div>
              </article>

              <article class="reason-card">
                <p class="section-eyebrow">閫€娆惧師鍥?/p>
                <h3 class="section-title-sm">璇存槑</h3>
                <p>{{ formatValue(normalized.refundReason) }}</p>
              </article>
            </div>

            <div v-else class="detail-loading-state" v-loading="loading"></div>
          </div>
        </section>
      </div>
    </div>
  </div>
</template>`

orders = orders.replace(/<template>[\s\S]*?<\/template>/, ordersTemplate)

orders = replaceAll(orders, [
  ['const backToSourceLabel = computed(() => (sourceFrom.value === "booking" ? "??????" : "??????"))', 'const backToSourceLabel = computed(() => (sourceFrom.value === "booking" ? "杩斿洖鎴戠殑棰勭害" : "杩斿洖鏈€杩戞姤鍚?))'],
  ['const sourceEntityLabel = computed(() => (sourceFrom.value === "booking" ? "??ID" : "??ID"))', 'const sourceEntityLabel = computed(() => (sourceFrom.value === "booking" ? "棰勭害ID" : "鎶ュ悕ID"))'],
  ['const sourceSummaryLabel = computed(() => (sourceFrom.value === "booking" ? "??" : "??"))', 'const sourceSummaryLabel = computed(() => (sourceFrom.value === "booking" ? "妗ｆ湡" : "璇剧▼"))'],
  ['sourceLabel: "????"', 'sourceLabel: "鏁欑粌棰勭害"'],
  ['sourceLabel: "????"', 'sourceLabel: "璇剧▼鎶ュ悕"'],
  ['const map = { UNPAID: "???", PAID: "???", CLOSED: "???", REFUNDED: "???" }', 'const map = { UNPAID: "鏈敮浠?, PAID: "宸叉敮浠?, CLOSED: "宸插叧闂?, REFUNDED: "宸查€€娆? }'],
  ['NEW: "??"', 'NEW: "鏂板缓"'],
  ['UNPAID: "???"', 'UNPAID: "寰呮敮浠?'],
  ['PAID: "???"', 'PAID: "宸叉敮浠?'],
  ['CLOSED: "???"', 'CLOSED: "宸插叧闂?'],
  ['CANCELLED: "???"', 'CANCELLED: "宸插彇娑?'],
  ['REFUNDED: "???"', 'REFUNDED: "宸查€€娆?'],
  ['const map = { wechat: "??", alipay: "???", mock: "??" }', 'const map = { wechat: "寰俊", alipay: "鏀粯瀹?, mock: "妯℃嫙" }'],
  ['const map = { coach_booking: "????", course: "????", course_enrollment: "????" }', 'const map = { coach_booking: "鏁欑粌棰勭害", course: "璇剧▼鎶ュ悕", course_enrollment: "璇剧▼鎶ュ悕" }'],
  ['ElMessage.warning("???????????")', 'ElMessage.warning("褰撳墠娌℃湁鍙鍒剁殑璁㈠崟鍙?)'],
  ['ElMessage.success("??????")', 'ElMessage.success("璁㈠崟鍙峰凡澶嶅埗")'],
  ['ElMessage.error("??????????")', 'ElMessage.error("澶嶅埗澶辫触锛岃鎵嬪姩澶嶅埗")'],
  ['throw new Error(data.message || "??????")', 'throw new Error(data.message || "鍔犺浇璁㈠崟澶辫触")'],
  ['ElMessage.error(err.message || "??????")', 'ElMessage.error(err.message || "鍔犺浇璁㈠崟澶辫触")'],
  ['ElMessage.warning("?????")', 'ElMessage.warning("璇烽€夋嫨璁㈠崟")'],
  ['ElMessage.warning("???????????")', 'ElMessage.warning("褰撳墠璁㈠崟涓嶆敮鎸佸彇娑堥绾?)'],
  ['"??????????????????????????????"', '"鍙栨秷鍚庝細閲婃斁杩欑瑪寰呮敮浠橀绾﹀崰鐢ㄧ殑鍚嶉锛岀‘璁ょ户缁悧锛?'],
  ['"??????"', '"鍙栨秷寰呮敮浠橀绾?'],
  ['confirmButtonText: "????"', 'confirmButtonText: "纭鍙栨秷"'],
  ['cancelButtonText: "????"', 'cancelButtonText: "鍏堜笉鍙栨秷"'],
  ['ElMessage.success("???????????")', 'ElMessage.success("棰勭害宸插彇娑堬紝璁㈠崟宸插叧闂?)']
])

await fs.writeFile(bookingPath, booking, "utf8")
await fs.writeFile(ordersPath, orders, "utf8")
console.log("done")
