<template>
  <div class="card">
    <div class="toolbar">
      <div>
        <h2>预约运维</h2>
        <p>查看教练预约、完成预约、清理超时未支付</p>
      </div>
      <div class="toolbar-actions">
        <el-select v-model="query.status" placeholder="状态" clearable style="width: 150px">
          <el-option label="待支付" value="WAIT_PAY" />
          <el-option label="已支付" value="PAID" />
          <el-option label="已完成" value="COMPLETED" />
          <el-option label="已取消" value="CANCELLED" />
        </el-select>
        <el-button type="primary" @click="loadBookings" :loading="loading">刷新</el-button>
      </div>
    </div>

    <div class="summary-grid">
      <div class="summary-card">
        <div class="summary-label">预约总数</div>
        <div class="summary-value">{{ bookings.length }}</div>
        <div class="summary-sub">当前筛选结果</div>
      </div>
      <div class="summary-card">
        <div class="summary-label">已支付</div>
        <div class="summary-value">{{ paidCount }}</div>
        <div class="summary-sub">可手动完结</div>
      </div>
      <div class="summary-card">
        <div class="summary-label">已完成</div>
        <div class="summary-value">{{ completedCount }}</div>
        <div class="summary-sub">已履约结束</div>
      </div>
      <div class="summary-card">
        <div class="summary-label">待支付</div>
        <div class="summary-value">{{ waitingCount }}</div>
        <div class="summary-sub">可关闭超时单</div>
      </div>
    </div>

    <div class="ops-bar">
      <el-input-number
        v-model="closeTimeoutMinutes"
        :min="1"
        :max="720"
        controls-position="right"
        style="width: 160px"
      />
      <el-button type="warning" @click="closeTimeout" :loading="closing">关闭超时预约</el-button>
      <span class="hint">关闭 WAIT_PAY 且超时的预约，并同步释放档期余量。</span>
    </div>

    <el-table
      :data="bookings"
      style="width: 100%"
      v-loading="loading"
      @row-click="openDetail"
      highlight-current-row
    >
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="userId" label="用户" width="90" />
      <el-table-column prop="coachId" label="教练" width="90" />
      <el-table-column prop="scheduleId" label="档期" width="90" />
      <el-table-column prop="orderId" label="订单" width="90" />
      <el-table-column label="预约状态" width="120">
        <template #default="{ row }">
          <el-tag :type="bookingStatusTag(row.bookingStatus)">
            {{ formatBookingStatus(row.bookingStatus) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="支付状态" width="120">
        <template #default="{ row }">
          <el-tag :type="payStatusTag(row.payStatus)">
            {{ formatPayStatus(row.payStatus) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="到场时间" width="170">
        <template #default="{ row }">
          {{ formatDateTime(row.checkinTime) }}
        </template>
      </el-table-column>
      <el-table-column label="完成时间" width="170">
        <template #default="{ row }">
          {{ formatDateTime(row.finishTime) }}
        </template>
      </el-table-column>
      <el-table-column label="创建时间" width="170">
        <template #default="{ row }">
          {{ formatDateTime(row.createTime) }}
        </template>
      </el-table-column>
      <el-table-column label="操作" width="140">
        <template #default="{ row }">
          <el-button
            size="small"
            type="success"
            :disabled="!canComplete(row)"
            @click.stop="completeBooking(row)"
          >
            完成预约
          </el-button>
        </template>
      </el-table-column>
    </el-table>
  </div>

  <el-drawer v-model="detailVisible" title="预约详情" size="36%">
    <div v-if="selectedBooking" class="detail-panel">
      <div class="detail-grid">
        <div>
          <div class="label">预约ID</div>
          <div class="value">{{ selectedBooking.id }}</div>
        </div>
        <div>
          <div class="label">用户ID</div>
          <div class="value">{{ selectedBooking.userId }}</div>
        </div>
        <div>
          <div class="label">教练ID</div>
          <div class="value">{{ selectedBooking.coachId }}</div>
        </div>
        <div>
          <div class="label">档期ID</div>
          <div class="value">{{ selectedBooking.scheduleId }}</div>
        </div>
        <div>
          <div class="label">订单ID</div>
          <div class="value">{{ selectedBooking.orderId }}</div>
        </div>
        <div>
          <div class="label">预约状态</div>
          <div class="value">{{ formatBookingStatus(selectedBooking.bookingStatus) }}</div>
        </div>
        <div>
          <div class="label">支付状态</div>
          <div class="value">{{ formatPayStatus(selectedBooking.payStatus) }}</div>
        </div>
        <div>
          <div class="label">到场时间</div>
          <div class="value">{{ formatDateTime(selectedBooking.checkinTime) }}</div>
        </div>
        <div>
          <div class="label">完成时间</div>
          <div class="value">{{ formatDateTime(selectedBooking.finishTime) }}</div>
        </div>
      </div>

      <el-divider />

      <div class="detail-actions">
        <el-button
          type="success"
          :disabled="!canComplete(selectedBooking)"
          :loading="completing"
          @click="completeSelected"
        >
          完成预约
        </el-button>
        <el-button @click="detailVisible = false">关闭</el-button>
      </div>

      <pre class="payload">{{ detailText }}</pre>
    </div>
  </el-drawer>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { adminClient } from '../api/client'

const bookings = ref([])
const loading = ref(false)
const closing = ref(false)
const completing = ref(false)
const detailVisible = ref(false)
const selectedBooking = ref(null)

const query = reactive({
  status: ''
})

const closeTimeoutMinutes = ref(30)

const paidCount = computed(() => bookings.value.filter((item) => item.payStatus === 'PAID').length)
const completedCount = computed(() => bookings.value.filter((item) => item.bookingStatus === 'COMPLETED').length)
const waitingCount = computed(() => bookings.value.filter((item) => item.bookingStatus === 'WAIT_PAY').length)
const detailText = computed(() => (selectedBooking.value ? JSON.stringify(selectedBooking.value, null, 2) : ''))

const loadBookings = async () => {
  try {
    loading.value = true
    const { data } = await adminClient.get('/admin/ops/booking/list', {
      params: {
        status: query.status || undefined
      }
    })
    if (data.code !== 200) throw new Error(data.message || '加载失败')
    bookings.value = data.data || []
    if (selectedBooking.value) {
      const updated = bookings.value.find((item) => item.id === selectedBooking.value.id)
      if (updated) {
        selectedBooking.value = updated
      }
    }
  } catch (err) {
    ElMessage.error(err.message || '加载失败')
  } finally {
    loading.value = false
  }
}

const closeTimeout = async () => {
  try {
    closing.value = true
    const { data } = await adminClient.post('/admin/ops/booking/close-timeout', null, {
      params: { timeoutMinutes: closeTimeoutMinutes.value }
    })
    if (data.code !== 200) throw new Error(data.message || '关闭失败')
    ElMessage.success(`已关闭 ${data.data || 0} 条超时预约`)
    await loadBookings()
  } catch (err) {
    ElMessage.error(err.message || '关闭失败')
  } finally {
    closing.value = false
  }
}

const canComplete = (row) => {
  if (!row) return false
  return row.bookingStatus !== 'COMPLETED' && row.payStatus === 'PAID'
}

const completeBooking = async (row) => {
  if (!row) return
  try {
    completing.value = true
    const { data } = await adminClient.post('/admin/ops/booking/complete', null, {
      params: { bookingId: row.id }
    })
    if (data.code !== 200) throw new Error(data.message || '完成失败')
    ElMessage.success('预约已完成')
    await loadBookings()
  } catch (err) {
    ElMessage.error(err.message || '完成失败')
  } finally {
    completing.value = false
  }
}

const completeSelected = async () => {
  if (!selectedBooking.value) return
  await completeBooking(selectedBooking.value)
}

const openDetail = (row) => {
  selectedBooking.value = row
  detailVisible.value = true
}

const formatBookingStatus = (status) => {
  const map = {
    WAIT_PAY: '待支付',
    PAID: '已支付',
    COMPLETED: '已完成',
    CANCELLED: '已取消'
  }
  return map[status] || status || '-'
}

const bookingStatusTag = (status) => {
  const map = {
    WAIT_PAY: 'warning',
    PAID: 'success',
    COMPLETED: 'info',
    CANCELLED: 'info'
  }
  return map[status] || 'info'
}

const formatPayStatus = (status) => {
  const map = {
    UNPAID: '未支付',
    PAID: '已支付',
    CLOSED: '已关闭',
    REFUNDED: '已退款'
  }
  return map[status] || status || '-'
}

const payStatusTag = (status) => {
  const map = {
    UNPAID: 'warning',
    PAID: 'success',
    CLOSED: 'info',
    REFUNDED: 'danger'
  }
  return map[status] || 'info'
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

onMounted(loadBookings)
</script>

<style scoped>
.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}

.toolbar h2 {
  margin: 0 0 6px;
}

.toolbar p {
  margin: 0;
  color: #8aa0af;
  font-size: 13px;
}

.toolbar-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.summary-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  gap: 12px;
  margin-bottom: 16px;
}

.summary-card {
  padding: 12px;
  border-radius: 14px;
  border: 1px solid var(--border);
  background: #ffffffcc;
}

.summary-label {
  font-size: 12px;
  color: #8aa0af;
}

.summary-value {
  font-size: 22px;
  font-weight: 700;
  margin: 6px 0;
}

.summary-sub,
.hint {
  font-size: 12px;
  color: #6b7280;
}

.ops-bar {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
  margin-bottom: 16px;
}

.detail-panel {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.detail-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.label {
  font-size: 12px;
  color: var(--muted);
}

.value {
  margin-top: 4px;
  font-weight: 600;
}

.detail-actions {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.payload {
  margin: 0;
  padding: 12px;
  border-radius: 12px;
  background: #f6f9fb;
  color: #365062;
  font-size: 12px;
  white-space: pre-wrap;
}
</style>
