<template>
  <div class="section-stack">
    <section class="card">
      <div class="page-heading">
        <div>
          <div class="eyebrow">Orders Desk</div>
          <h2>订单与退款总览</h2>
          <p>从订单列表快速进入单笔财务明细，第一眼看到状态，第二眼定位支付与退款问题。</p>
        </div>
        <div class="toolbar-actions">
          <span class="code-pill mono">ORDER / PAYMENT / REFUND</span>
          <el-button type="primary" :loading="loading" @click="loadOrders">刷新订单</el-button>
        </div>
      </div>

      <div class="metric-grid">
        <div class="metric-card">
          <div class="metric-label">订单总数</div>
          <div class="metric-value">{{ orders.length }}</div>
          <div class="metric-note">当前列表返回的订单数量</div>
        </div>
        <div class="metric-card">
          <div class="metric-label">已支付</div>
          <div class="metric-value">{{ paidCount }}</div>
          <div class="metric-note">支付状态为 PAID 的订单</div>
        </div>
        <div class="metric-card">
          <div class="metric-label">已关闭</div>
          <div class="metric-value">{{ closedCount }}</div>
          <div class="metric-note">已关闭但仍可复核财务摘要</div>
        </div>
        <div class="metric-card">
          <div class="metric-label">已退款</div>
          <div class="metric-value">{{ refundedCount }}</div>
          <div class="metric-note">建议联动支付回调和退款审核查看</div>
        </div>
      </div>
    </section>

    <section class="split-layout">
      <div class="section-stack">
        <div class="card">
          <div class="toolbar">
            <div>
              <h3 class="section-title-sm">订单列表</h3>
              <div class="section-copy">点击任意订单即可在右侧打开完整明细，不再把状态与金额混在一排里。</div>
            </div>
            <div class="chip-row">
              <span class="tag">最新订单 {{ orders[0]?.id || '-' }}</span>
            </div>
          </div>

          <div class="table-shell">
            <el-table :data="orders" style="width: 100%" v-loading="loading" @row-click="loadDetail">
              <el-table-column prop="id" label="ID" width="70" />
              <el-table-column prop="orderNo" label="订单号" min-width="220">
                <template #default="{ row }">
                  <div class="mono order-no">{{ row.orderNo }}</div>
                </template>
              </el-table-column>
              <el-table-column prop="totalAmount" label="金额" width="120" />
              <el-table-column label="支付状态" width="120">
                <template #default="{ row }">
                  <el-tag :type="payStatusTag(row.payStatus)">
                    {{ formatPayStatus(row.payStatus) }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column label="订单状态" width="120">
                <template #default="{ row }">
                  <el-tag :type="orderStatusTag(row.orderStatus)">
                    {{ formatOrderStatus(row.orderStatus) }}
                  </el-tag>
                </template>
              </el-table-column>
            </el-table>
          </div>
        </div>

        <div class="card" v-if="detail">
          <div class="section-title-sm">接口原始响应</div>
          <pre class="payload">{{ detailText }}</pre>
        </div>
      </div>

      <div class="section-stack">
        <div class="card">
          <div class="toolbar">
            <div>
              <h3 class="section-title-sm">订单详情</h3>
              <div class="section-copy">把支付、订单、金额和退款拆成清晰区块，避免超长订单号压住其他字段。</div>
            </div>
          </div>

          <div v-if="!detail" class="empty-block">
            <el-empty description="请选择订单查看详情" />
          </div>

          <template v-else>
            <div class="detail-panels">
              <div class="detail-panel detail-panel-wide">
                <div class="detail-panel-head">
                  <div class="info-label">订单号</div>
                  <el-button size="small" @click="copyOrderNo">复制订单号</el-button>
                </div>
                <div class="info-value mono wrap">{{ normalized.orderNo }}</div>
              </div>
              <div class="detail-panel">
                <div class="info-label">支付状态</div>
                <div class="info-value">
                  <el-tag :type="payStatusTag(normalized.payStatus)">
                    {{ formatPayStatus(normalized.payStatus) }}
                  </el-tag>
                </div>
              </div>
              <div class="detail-panel">
                <div class="info-label">订单状态</div>
                <div class="info-value">
                  <el-tag :type="orderStatusTag(normalized.orderStatus)">
                    {{ formatOrderStatus(normalized.orderStatus) }}
                  </el-tag>
                </div>
              </div>
              <div class="detail-panel">
                <div class="info-label">金额</div>
                <div class="info-value mono">{{ formatAmount(normalized.totalAmount) }}</div>
              </div>
              <div class="detail-panel">
                <div class="info-label">实付</div>
                <div class="info-value mono">{{ formatAmount(normalized.paidAmount) }}</div>
              </div>
              <div class="detail-panel">
                <div class="info-label">退款</div>
                <div class="info-value mono">{{ formatAmount(normalized.refundAmount) }}</div>
              </div>
              <div class="detail-panel">
                <div class="info-label">净支付</div>
                <div class="info-value mono">{{ formatAmount(normalized.netPaid) }}</div>
              </div>
              <div class="detail-panel">
                <div class="info-label">支付渠道</div>
                <div class="info-value">{{ formatPayChannel(normalized.payChannel) }}</div>
              </div>
              <div class="detail-panel">
                <div class="info-label">支付时间</div>
                <div class="info-value wrap">{{ formatValue(normalized.payTime) }}</div>
              </div>
              <div class="detail-panel">
                <div class="info-label">退款状态</div>
                <div class="info-value">{{ formatRefundStatus(normalized.refundStatus) }}</div>
              </div>
              <div class="detail-panel">
                <div class="info-label">退款时间</div>
                <div class="info-value wrap">{{ formatValue(normalized.refundTime) }}</div>
              </div>
            </div>

            <div class="detail-card">
              <div class="section-title-sm">订单明细</div>
              <div class="table-shell">
                <el-table :data="normalized.items" style="width: 100%" size="small">
                  <el-table-column prop="itemName" label="名称" min-width="180" />
                  <el-table-column prop="itemType" label="类型" width="120" />
                  <el-table-column prop="quantity" label="数量" width="80" />
                  <el-table-column prop="price" label="单价" width="120" />
                  <el-table-column prop="amount" label="金额" width="120" />
                </el-table>
              </div>
            </div>

            <div class="detail-card finance-block">
              <div class="section-title-sm">资金说明</div>
              <div class="finance-main">{{ formatFinanceText(normalized.financeSummary.displayText) }}</div>
              <div class="finance-grid">
                <div class="info-row">
                  <div class="info-label">状态文案</div>
                  <div class="info-value">{{ formatFinanceText(normalized.financeSummary.statusText) }}</div>
                </div>
                <div class="info-row">
                  <div class="info-label">状态提示</div>
                  <div class="info-value">{{ formatFinanceText(normalized.financeSummary.statusHint) }}</div>
                </div>
                <div class="info-row">
                  <div class="info-label">阶段</div>
                  <div class="info-value">{{ formatFinanceText(normalized.financeSummary.stage) }}</div>
                </div>
                <div class="info-row">
                  <div class="info-label">说明</div>
                  <div class="info-value wrap">{{ formatFinanceText(normalized.financeSummary.statusExplain) }}</div>
                </div>
              </div>
            </div>

            <div class="detail-card">
              <div class="section-title-sm">退款原因</div>
              <div class="refund-copy">{{ formatValue(normalized.refundReason) }}</div>
            </div>
          </template>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { adminClient } from '../api/client'

const orders = ref([])
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

const paidCount = computed(() => orders.value.filter((item) => item.payStatus === 'PAID').length)
const closedCount = computed(() => orders.value.filter((item) => item.orderStatus === 'CLOSED').length)
const refundedCount = computed(() => orders.value.filter((item) => item.refundStatus === 'REFUNDED' || item.orderStatus === 'REFUNDED').length)

const loadOrders = async () => {
  try {
    loading.value = true
    const { data } = await adminClient.get('/admin/ops/order/list')
    if (data.code !== 200) throw new Error(data.message || '加载失败')
    orders.value = data.data || []
    if (orders.value.length && !detail.value) {
      await loadDetail(orders.value[0])
    }
  } catch (err) {
    ElMessage.error(err.message || '加载失败')
  } finally {
    loading.value = false
  }
}

const loadDetail = async (row) => {
  try {
    const { data } = await adminClient.get('/admin/ops/order/detail', {
      params: { orderId: row.id }
    })
    if (data.code !== 200) throw new Error(data.message || '获取详情失败')
    detail.value = data.data
  } catch (err) {
    ElMessage.error(err.message || '获取详情失败')
  }
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

const formatRefundStatus = (status) => {
  const map = {
    NONE: '无退款',
    PENDING: '处理中',
    REFUNDED: '已退款',
    REJECTED: '已拒绝'
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

const payStatusTag = (status) => {
  const map = {
    PAID: 'success',
    UNPAID: 'warning',
    CLOSED: 'info',
    REFUNDED: 'danger'
  }
  return map[status] || 'info'
}

const orderStatusTag = (status) => {
  const map = {
    PAID: 'success',
    NEW: 'warning',
    UNPAID: 'warning',
    CLOSED: 'info',
    CANCELLED: 'danger',
    REFUNDED: 'danger'
  }
  return map[status] || 'info'
}

const formatPayChannel = (channel) => {
  const map = {
    wechat: '微信',
    alipay: '支付宝',
    mock: '模拟'
  }
  return map[channel] || channel || '-'
}

const formatFinanceText = (value) => {
  if (value === null || value === undefined || value === '') return '-'
  const normalizedValue = String(value)
  const map = {
    refunded: '已退款',
    order_refunded: '订单已退款',
    REFUND: '退款完成',
    'Refund completed': '退款已完成',
    refunded_order: '退款订单',
    paid: '已支付',
    unpaid: '未支付',
    CLOSED: '已关闭',
    closed: '已关闭'
  }
  return map[normalizedValue] || normalizedValue
}

const formatValue = (value) => {
  if (value === null || value === undefined || value === '') return '-'
  return value
}

const formatAmount = (value) => {
  if (value === null || value === undefined || value === '') return '-'
  return value
}

const copyOrderNo = async () => {
  try {
    if (!normalized.value.orderNo || normalized.value.orderNo === '-') {
      ElMessage.warning('当前没有可复制的订单号')
      return
    }
    await navigator.clipboard.writeText(normalized.value.orderNo)
    ElMessage.success('订单号已复制')
  } catch (err) {
    ElMessage.error('复制订单号失败')
  }
}

onMounted(loadOrders)
</script>

<style scoped>
.order-no {
  font-size: 12px;
  line-height: 1.55;
  word-break: break-all;
}

.detail-panels {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.detail-panel,
.detail-card {
  padding: 16px;
  border-radius: 20px;
  border: 3px solid var(--border);
  background: rgba(255, 255, 255, 0.96);
  box-shadow: 0 8px 0 rgba(52, 45, 105, 0.08);
}

.detail-panel-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 8px;
}

.detail-panel-wide {
  grid-column: span 2;
}

.detail-card {
  margin-top: 14px;
}

.wrap {
  word-break: break-word;
}

.finance-block {
  background: linear-gradient(135deg, rgba(30, 64, 175, 0.05), rgba(245, 158, 11, 0.07));
}

.finance-main {
  color: var(--text);
  font-family: 'Fredoka', 'Nunito', sans-serif;
  font-size: 22px;
  font-weight: 700;
  margin-bottom: 12px;
}

.finance-grid {
  display: grid;
  gap: 10px;
}

.refund-copy {
  color: var(--text);
  line-height: 1.7;
}

@media (max-width: 960px) {
  .detail-panels {
    grid-template-columns: 1fr;
  }

  .detail-panel-wide {
    grid-column: span 1;
  }
}
</style>
