<template>
  <div class="card">
    <div class="toolbar">
      <div>
        <h2 class="section-title">订单查询</h2>
        <p class="section-sub">输入订单ID查看详情</p>
      </div>
    </div>
    <el-form :inline="true" label-position="top">
      <el-form-item label="订单ID">
        <el-input v-model.number="orderId" placeholder="例如 4（可从报名/预约详情点击带入）" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="loadDetail" :loading="loading">查询</el-button>
      </el-form-item>
      <el-form-item>
        <el-button @click="loadLastOrder">最近订单</el-button>
      </el-form-item>
    </el-form>
    <el-empty v-if="!detail" description="暂无订单详情，可先完成报名或预约">
      <div class="empty-actions">
        <el-button size="small" @click="go('/courses')">去报名课程</el-button>
        <el-button size="small" @click="go('/booking')">去预约教练</el-button>
      </div>
    </el-empty>
    <div v-else>
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
        <div class="finance-main">{{ formatValue(normalized.financeSummary.displayText) }}</div>
        <div class="finance-sub">
          {{ formatValue(normalized.financeSummary.statusText) }}
          {{ normalized.financeSummary.statusHint ? `｜${normalized.financeSummary.statusHint}` : '' }}
        </div>
        <div class="finance-sub">{{ formatValue(normalized.financeSummary.statusExplain) }}</div>
        <div class="finance-sub">阶段：{{ formatValue(normalized.financeSummary.stage) }}</div>
      </div>

      <el-divider />

      <div class="section-title-sm">退款原因</div>
      <div class="muted">{{ formatValue(normalized.refundReason) }}</div>

      <pre class="payload">{{ detailText }}</pre>
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
const orderId = ref(4)
const detail = ref(null)
const loading = ref(false)
const detailText = computed(() => (detail.value ? JSON.stringify(detail.value, null, 2) : ''))
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

const formatValue = (value) => {
  if (value === null || value === undefined || value === '') return '-'
  return value
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

const loadDetail = async () => {
  if (!orderId.value) {
    ElMessage.warning('请输入订单ID')
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

const loadLastOrder = () => {
  const cached = localStorage.getItem('fp_last_order_id')
  if (!cached) {
    ElMessage.warning('暂无最近订单')
    return
  }
  orderId.value = Number(cached)
  loadDetail()
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

loadFromRoute()
</script>

<style scoped>
.payload {
  color: #365062;
  font-size: 12px;
  white-space: pre-wrap;
}

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
