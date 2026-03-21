<template>
  <div class="card">
    <div class="toolbar">
      <div>
        <h2 class="section-title">我的订单</h2>
        <p class="section-sub">查看最近订单并打开详情</p>
      </div>
      <el-button type="primary" @click="loadOrders" :loading="ordersLoading">刷新订单</el-button>
    </div>

    <el-empty v-if="!orders.length && !ordersLoading" description="暂无订单，可先完成报名或预约">
      <div class="empty-actions">
        <el-button size="small" @click="go('/courses')">去报名课程</el-button>
        <el-button size="small" @click="go('/booking')">去预约教练</el-button>
      </div>
    </el-empty>

    <el-table
      v-else
      :data="orders"
      v-loading="ordersLoading"
      style="width: 100%; margin-bottom: 16px"
      @row-click="handleOrderSelect"
      highlight-current-row
    >
      <el-table-column prop="orderNo" label="订单号" min-width="220" />
      <el-table-column label="类型" width="120">
        <template #default="{ row }">
          {{ formatBizType(row.bizType) }}
        </template>
      </el-table-column>
      <el-table-column label="金额" width="120">
        <template #default="{ row }">
          {{ formatAmount(row.totalAmount) }}
        </template>
      </el-table-column>
      <el-table-column label="支付状态" width="120">
        <template #default="{ row }">
          {{ formatPayStatus(row.payStatus) }}
        </template>
      </el-table-column>
      <el-table-column label="订单状态" width="120">
        <template #default="{ row }">
          {{ formatOrderStatus(row.orderStatus) }}
        </template>
      </el-table-column>
      <el-table-column label="下单时间" min-width="160">
        <template #default="{ row }">
          {{ formatDateTime(row.createTime) }}
        </template>
      </el-table-column>
      <el-table-column label="操作" width="100">
        <template #default="{ row }">
          <el-button text size="small" @click.stop="openOrder(row.id)">查看</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-empty v-if="orders.length && !detail" description="请选择一条订单查看详情" />

    <div v-else-if="detail">
      <div class="summary-grid">
        <div>
          <div class="label">订单号</div>
          <div class="value">{{ normalized.orderNo }}</div>
        </div>
        <div>
          <div class="label">支付状态</div>
          <div class="value">{{ formatPayStatus(normalized.payStatus) }}</div>
        </div>
        <div>
          <div class="label">订单状态</div>
          <div class="value">{{ formatOrderStatus(normalized.orderStatus) }}</div>
        </div>
        <div>
          <div class="label">金额</div>
          <div class="value">{{ formatAmount(normalized.totalAmount) }}</div>
        </div>
        <div>
          <div class="label">实付</div>
          <div class="value">{{ formatAmount(normalized.paidAmount) }}</div>
        </div>
        <div>
          <div class="label">退款</div>
          <div class="value">{{ formatAmount(normalized.refundAmount) }}</div>
        </div>
        <div>
          <div class="label">净支付</div>
          <div class="value">{{ formatAmount(normalized.netPaid) }}</div>
        </div>
        <div>
          <div class="label">支付渠道</div>
          <div class="value">{{ formatPayChannel(normalized.payChannel) }}</div>
        </div>
        <div>
          <div class="label">支付时间</div>
          <div class="value">{{ formatDateTime(normalized.payTime) }}</div>
        </div>
        <div>
          <div class="label">退款状态</div>
          <div class="value">{{ formatPayStatus(normalized.refundStatus) }}</div>
        </div>
        <div>
          <div class="label">退款时间</div>
          <div class="value">{{ formatDateTime(normalized.refundTime) }}</div>
        </div>
      </div>

      <el-divider />

      <div class="section-title-sm">订单明细</div>
      <el-table :data="normalized.items" style="width: 100%" size="small">
        <el-table-column prop="itemName" label="名称" />
        <el-table-column prop="itemType" label="类型" width="120" />
        <el-table-column prop="quantity" label="数量" width="80" />
        <el-table-column prop="price" label="单价" width="120" />
        <el-table-column prop="amount" label="金额" width="120" />
      </el-table>

      <el-divider />

      <div class="section-title-sm">资金说明</div>
      <div class="finance-card">
        <div class="finance-main">{{ formatFinanceDisplay(normalized.financeSummary.displayText, normalized.refundStatus, normalized.payStatus, normalized.orderStatus) }}</div>
        <div class="finance-sub">{{ formatFinanceExplain(normalized.financeSummary.statusExplain, normalized.refundStatus, normalized.payStatus, normalized.orderStatus) }}</div>
      </div>

      <el-divider />

      <div class="section-title-sm">退款原因</div>
      <div class="muted">{{ formatValue(normalized.refundReason) }}</div>
    </div>
  </div>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { appClient } from '../api/client'

const route = useRoute()
const router = useRouter()
const orderId = ref(null)
const orders = ref([])
const ordersLoading = ref(false)
const detail = ref(null)
const loading = ref(false)

const normalized = computed(() => {
  const d = detail.value || {}
  const order = d.order || {}
  const base = Object.keys(order).length ? order : d
  return {
    orderNo: base.orderNo || '-',
    orderStatus: base.orderStatus ?? d.orderStatus,
    payStatus: base.payStatus ?? d.payStatus,
    payChannel: base.payChannel ?? d.payChannel,
    payTime: base.payTime ?? d.payTime,
    refundStatus: base.refundStatus ?? d.refundStatus,
    refundTime: base.refundTime ?? d.refundTime,
    refundReason: base.refundReason ?? d.refundReason,
    totalAmount: base.totalAmount ?? d.totalAmount,
    paidAmount: d.paidAmount ?? base.paidAmount ?? base.totalAmount ?? d.totalAmount,
    refundAmount: d.refundAmount ?? base.refundAmount,
    netPaid: d.netPaid ?? base.netPaid,
    items: d.items || base.items || [],
    financeSummary: d.financeSummary || base.financeSummary || {}
  }
})

const formatPayStatus = (status) => {
  const map = {
    UNPAID: '未支付',
    PAID: '已支付',
    CLOSED: '已关闭',
    REFUNDED: '已退款'
  }
  return map[status] || status || '-'
}

const formatOrderStatus = (status) => {
  const map = {
    NEW: '新建',
    UNPAID: '待支付',
    PAID: '已支付',
    CLOSED: '已关闭',
    CANCELLED: '已取消',
    REFUNDED: '已退款'
  }
  return map[status] || status || '-'
}

const formatPayChannel = (channel) => {
  const map = {
    wechat: '微信',
    alipay: '支付宝',
    mock: '模拟'
  }
  return map[channel] || channel || '-'
}

const formatBizType = (value) => {
  const map = {
    coach_booking: '教练预约',
    course: '课程报名',
    course_enrollment: '课程报名'
  }
  return map[value] || value || '-'
}

const formatValue = (value) => {
  if (value === null || value === undefined || value === '') return '-'
  return value
}

const formatFinanceDisplay = (value, refundStatus, payStatus, orderStatus) => {
  const raw = String(value || '').trim()
  const map = {
    Refunded: '已退款',
    'Paid (awaiting service)': '已支付',
    Closed: '已关闭',
    Unpaid: '待支付',
    Unknown: '状态未知'
  }
  if (map[raw]) return map[raw]
  if (refundStatus === 'REFUNDED') return '已退款'
  if (payStatus === 'PAID') return '已支付'
  if (orderStatus === 'CLOSED') return '已关闭'
  if (payStatus === 'UNPAID') return '待支付'
  return formatValue(value)
}

const formatFinanceExplain = (value, refundStatus, payStatus, orderStatus) => {
  const raw = String(value || '').trim()
  const map = {
    'Refund completed': '退款已完成',
    'Payment received': '订单已完成支付',
    'Order closed': '订单已关闭',
    'Awaiting payment': '等待支付',
    'Unknown status': '当前状态暂时无法识别'
  }
  if (map[raw]) return map[raw]
  if (refundStatus === 'REFUNDED') return '退款已完成'
  if (payStatus === 'PAID') return '订单已完成支付'
  if (orderStatus === 'CLOSED') return '订单已关闭'
  if (payStatus === 'UNPAID') return '等待支付'
  return formatValue(value)
}

const formatDateTime = (value) => {
  if (value === null || value === undefined || value === '') return '-'
  const raw = String(value)
  if (raw.includes('T')) {
    const [date, time] = raw.split('T')
    return `${date} ${time.slice(0, 8)}`
  }
  if (raw.length >= 16 && raw.includes('-')) return raw.slice(0, 16)
  return raw
}

const formatAmount = (value) => {
  if (value === null || value === undefined || value === '') return '-'
  return value
}

const openOrder = async (id) => {
  if (!id) return
  orderId.value = Number(id)
  localStorage.setItem('fp_last_order_id', String(id))
  await loadDetail()
}

const handleOrderSelect = (row) => {
  if (!row?.id) return
  openOrder(row.id)
}

const loadOrders = async () => {
  try {
    ordersLoading.value = true
    const { data } = await appClient.get('/app/order/my/list', { params: { limit: 20 } })
    if (data.code !== 200) throw new Error(data.message || '加载失败')
    orders.value = data.data || []
    if (!orders.value.length) {
      detail.value = null
      return
    }
    const routeId = Number(route.query.orderId)
    const cachedId = Number(localStorage.getItem('fp_last_order_id'))
    const preferredId = [orderId.value, routeId, cachedId, orders.value[0].id].find((item) => Number.isFinite(Number(item)) && Number(item) > 0)
    if (preferredId) {
      await openOrder(preferredId)
    }
  } catch (err) {
    ElMessage.error(err.message || '加载失败')
  } finally {
    ordersLoading.value = false
  }
}

const loadDetail = async () => {
  if (!orderId.value) {
    ElMessage.warning('请选择订单')
    return
  }
  try {
    loading.value = true
    const { data } = await appClient.get('/app/order/detail', { params: { orderId: orderId.value } })
    if (data.code !== 200) throw new Error(data.message || '查询失败')
    detail.value = data.data
  } catch (err) {
    ElMessage.error(err.message || '查询失败')
  } finally {
    loading.value = false
  }
}

const loadFromRoute = () => {
  const raw = route.query.orderId
  if (!raw) return
  const parsed = Number(raw)
  if (!Number.isFinite(parsed) || parsed <= 0) return
  orderId.value = parsed
  loadDetail()
}

const go = (path) => {
  router.push(path)
}

watch(
  () => route.query.orderId,
  () => loadFromRoute()
)

loadOrders()
loadFromRoute()
</script>

<style scoped>
.summary-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(160px, 1fr));
  gap: 12px;
  margin-bottom: 12px;
}

.section-title-sm {
  font-size: 14px;
  font-weight: 600;
  margin-bottom: 8px;
}

.finance-card {
  padding: 12px;
  border-radius: 12px;
  border: 1px dashed var(--border);
  background: #ffffffcc;
}

.finance-main {
  font-weight: 600;
  margin-bottom: 6px;
}

.finance-sub {
  font-size: 12px;
  color: var(--muted);
  margin-top: 4px;
}

.muted {
  font-size: 12px;
  color: var(--muted);
}

.label {
  font-size: 12px;
  color: var(--muted);
}

.value {
  font-weight: 600;
  margin-top: 4px;
}
</style>
