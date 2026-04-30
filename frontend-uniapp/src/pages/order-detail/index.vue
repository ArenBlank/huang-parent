<template>
  <view class="detail-page">
    <view class="page-glow page-glow--left"></view>
    <view class="page-glow page-glow--right"></view>

    <view class="topbar">
      <button class="icon-button" aria-label="返回" @click="goBack">
        <image class="top-icon" :src="icons.back" mode="aspectFit" />
      </button>
      <text class="topbar-title">订单详情</text>
      <button class="icon-button" aria-label="联系客服" @click="contactService">
        <image class="top-icon" :src="icons.headset" mode="aspectFit" />
      </button>
    </view>

    <view v-if="loading && !detail" class="loading-card">
      <image class="loading-icon spinning" :src="icons.refresh" mode="aspectFit" />
      <text>正在读取订单详情...</text>
    </view>

    <template v-else-if="detail">
      <view class="summary-card">
        <view class="summary-top">
          <view class="order-no-row">
            <view class="summary-icon">
              <image :src="bizIcon(normalized.bizType)" mode="aspectFit" />
            </view>
            <text>订单号：</text>
            <text class="summary-order-no">{{ normalized.orderNo }}</text>
            <button class="copy-mini" @click="copyOrderNo">
              <image :src="icons.copy" mode="aspectFit" />
            </button>
          </view>
          <text class="status-pill" :class="statusMeta.tone">{{ statusMeta.text }}</text>
        </view>

        <view class="summary-main">
          <view class="summary-visual" :class="bizTone(normalized.bizType)">
            <image :src="bizIcon(normalized.bizType)" mode="aspectFit" />
          </view>
          <view class="summary-copy">
            <text class="summary-title">{{ primaryItemName }}</text>
            <text class="summary-sub">{{ primaryItemSubtitle }}</text>
            <text class="summary-location">{{ formatBizType(normalized.bizType) }} · {{ primaryItemType }}</text>
          </view>
          <view class="summary-amount">
            <text>¥{{ formatAmount(normalized.totalAmount) }}</text>
            <text>共{{ itemQuantity }}件</text>
          </view>
        </view>
      </view>

      <view class="status-grid-card">
        <view
          v-for="cell in statusCells"
          :key="cell.label"
          class="status-cell"
          :class="{ wide: cell.wide }"
        >
          <view class="cell-icon" :class="cell.tone">
            <image :src="cell.icon" mode="aspectFit" />
          </view>
          <view>
            <text class="cell-label">{{ cell.label }}</text>
            <text class="cell-value" :class="cell.valueTone">{{ cell.value }}</text>
          </view>
        </view>
      </view>

      <view class="fee-card">
        <text class="section-title">费用明细</text>
        <view v-if="!normalized.items.length" class="empty-soft">
          <image :src="icons.receipt" mode="aspectFit" />
          <text>暂无订单明细</text>
        </view>
        <view v-else class="fee-list">
          <view v-for="item in normalized.items" :key="item.id || item.itemId || item.itemName" class="fee-item">
            <view class="fee-icon">
              <image :src="bizIcon(normalized.bizType)" mode="aspectFit" />
            </view>
            <view class="fee-main">
              <text class="fee-title">{{ item.itemName || primaryItemName }}</text>
              <text class="fee-sub">{{ item.itemType || '订单项目' }} · 数量 {{ item.quantity || 1 }}</text>
            </view>
            <text class="fee-amount">¥{{ formatAmount(item.amount ?? item.price) }}</text>
          </view>
        </view>
        <view class="fee-lines">
          <view>
            <text>商品金额</text>
            <text>¥{{ formatAmount(normalized.totalAmount) }}</text>
          </view>
          <view>
            <text>优惠券抵扣</text>
            <text class="danger">- ¥0.00</text>
          </view>
          <view class="total-line">
            <text>合计</text>
            <text>¥{{ formatAmount(normalized.totalAmount) }}</text>
          </view>
        </view>
      </view>

      <view class="finance-card">
        <view class="section-head">
          <view class="section-icon">
            <image :src="icons.shield" mode="aspectFit" />
          </view>
          <text class="section-title">资金说明</text>
        </view>
        <view class="finance-note">
          <view>
            <text>{{ financeDisplay }}</text>
            <text>{{ financeExplain }}</text>
          </view>
          <image :src="icons.headset" mode="aspectFit" />
        </view>
      </view>

      <view class="refund-card">
        <view class="refund-head">
          <view class="section-head">
            <view class="section-icon orange">
              <image :src="icons.refund" mode="aspectFit" />
            </view>
            <text class="section-title">退款信息</text>
          </view>
          <text class="status-pill" :class="refundTone">{{ refundStatusText }}</text>
        </view>
        <view class="refund-lines">
          <view>
            <text>退款状态</text>
            <text>{{ refundStatusText }}</text>
          </view>
          <view>
            <text>退款金额</text>
            <text>¥{{ formatAmount(normalized.refundAmount) }}</text>
          </view>
          <view>
            <text>退款原因</text>
            <text>{{ normalized.refundReason || '-' }}</text>
          </view>
          <view>
            <text>退款时间</text>
            <text>{{ formatDateTime(normalized.refundTime) }}</text>
          </view>
        </view>
      </view>
    </template>

    <view v-else class="loading-card">
      <image class="loading-icon" :src="icons.receipt" mode="aspectFit" />
      <text>未找到订单详情</text>
      <button class="retry-button" @click="loadDetail">重新加载</button>
    </view>

    <view class="bottom-actions">
      <button class="service-button" @click="contactService">
        <image :src="icons.headset" mode="aspectFit" />
        <text>联系客服</text>
      </button>
      <button class="primary-button" @click="repeatOrder">
        {{ repeatLabel }}
      </button>
    </view>
  </view>
</template>

<script setup>
import {
  faCalendarCheck,
  faChevronLeft,
  faChevronRight,
  faClipboardCheck,
  faClipboardList,
  faClock,
  faCopy,
  faCreditCard,
  faHeadset,
  faMoneyBillTransfer,
  faReceipt,
  faRotateRight,
  faShieldHalved,
  faWallet
} from '@fortawesome/free-solid-svg-icons'
import { computed, ref } from 'vue'
import { onLoad, onShow } from '@dcloudio/uni-app'
import { getOrderDetail } from '../../api/modules/order'
import { ensureLogin } from '../../utils/authGuard'
import { goBack as backToPrevious } from '../../utils/navigation'

const faIcon = (definition, color = '#24104f') => {
  const [width, height, , , pathData] = definition.icon
  const paths = Array.isArray(pathData)
    ? pathData.map((path) => `<path fill="${color}" d="${path}"/>`).join('')
    : `<path fill="${color}" d="${pathData}"/>`
  const svg = `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 ${width} ${height}">${paths}</svg>`
  return `data:image/svg+xml;charset=utf-8,${encodeURIComponent(svg)}`
}

const icons = {
  back: faIcon(faChevronLeft),
  booking: faIcon(faCalendarCheck),
  card: faIcon(faCreditCard, '#8b63ff'),
  check: faIcon(faClipboardCheck, '#8b63ff'),
  clock: faIcon(faClock, '#8b63ff'),
  copy: faIcon(faCopy, '#9b88bf'),
  course: faIcon(faClipboardList),
  headset: faIcon(faHeadset, '#8b63ff'),
  receipt: faIcon(faReceipt),
  refresh: faIcon(faRotateRight),
  refund: faIcon(faMoneyBillTransfer, '#ff9f43'),
  right: faIcon(faChevronRight),
  shield: faIcon(faShieldHalved, '#8b63ff'),
  wallet: faIcon(faWallet, '#8b63ff')
}

const orderId = ref('')
const detail = ref(null)
const loading = ref(false)

const normalized = computed(() => {
  const d = detail.value || {}
  const order = d.order || d
  const payment = d.payment || {}
  const refund = d.refund || {}
  return {
    orderId: order.id || d.id || orderId.value,
    orderNo: order.orderNo || d.orderNo || '-',
    orderStatus: order.orderStatus ?? d.orderStatus,
    payStatus: d.payStatus ?? order.payStatus ?? payment.payStatus,
    payChannel: d.payChannel ?? payment.payChannel,
    payTime: d.payTime ?? payment.payTime,
    refundStatus: d.refundStatus ?? refund.refundStatus,
    refundTime: d.refundTime ?? refund.refundTime,
    refundReason: d.refundReason ?? refund.reason,
    totalAmount: order.totalAmount ?? d.totalAmount,
    bizType: order.bizType ?? d.bizType,
    bizId: order.bizId ?? d.bizId,
    paidAmount: d.paidAmount ?? payment.payAmount ?? 0,
    refundAmount: d.refundAmount ?? refund.refundAmount ?? 0,
    netPaid: d.netPaid ?? 0,
    items: d.items || order.items || [],
    financeSummary: d.financeSummary || {}
  }
})

const primaryItem = computed(() => normalized.value.items[0] || {})
const primaryItemName = computed(() => primaryItem.value.itemName || `${formatBizType(normalized.value.bizType)} ${normalized.value.bizId || ''}`)
const primaryItemType = computed(() => primaryItem.value.itemType || '订单项目')
const primaryItemSubtitle = computed(() => {
  const payTime = formatDateTime(normalized.value.payTime)
  if (payTime !== '-') return payTime
  return `业务ID ${normalized.value.bizId || '-'}`
})
const itemQuantity = computed(() => normalized.value.items.reduce((sum, item) => sum + Number(item.quantity || 1), 0) || 1)

const statusMeta = computed(() => {
  const pay = String(normalized.value.payStatus || '').toUpperCase()
  const status = String(normalized.value.orderStatus || '').toUpperCase()
  if (pay === 'UNPAID') return { text: '待支付', tone: 'warning' }
  if (pay === 'PAID') return { text: status === 'PAID' ? '已完成' : '已支付', tone: 'success' }
  if (pay === 'REFUNDED' || status === 'REFUNDED') return { text: '已退款', tone: 'info' }
  if (status === 'CLOSED' || status === 'CANCELLED') return { text: '已取消', tone: 'muted' }
  return { text: formatOrderStatus(status), tone: 'plain' }
})

const refundStatusText = computed(() => formatRefundStatus(normalized.value.refundStatus))
const refundTone = computed(() => {
  const status = String(normalized.value.refundStatus || '').toUpperCase()
  if (status === 'REFUNDED') return 'info'
  if (status === 'PENDING') return 'warning'
  if (status === 'REJECTED') return 'danger'
  return 'success'
})

const financeDisplay = computed(() =>
  formatFinanceDisplay(normalized.value.financeSummary.displayText, normalized.value.refundStatus, normalized.value.payStatus, normalized.value.orderStatus)
)
const financeExplain = computed(() =>
  formatFinanceExplain(normalized.value.financeSummary.statusExplain, normalized.value.refundStatus, normalized.value.payStatus, normalized.value.orderStatus)
)
const repeatLabel = computed(() => (isBooking(normalized.value.bizType) ? '再次预约' : '再次报名'))

const statusCells = computed(() => [
  {
    label: '支付状态',
    value: formatPayStatus(normalized.value.payStatus),
    valueTone: payTone(normalized.value.payStatus),
    icon: icons.check,
    tone: 'purple'
  },
  {
    label: '订单状态',
    value: formatOrderStatus(normalized.value.orderStatus),
    valueTone: orderTone(normalized.value.orderStatus),
    icon: icons.course,
    tone: 'purple',
    arrow: true
  },
  {
    label: '支付金额',
    value: `¥${formatAmount(normalized.value.paidAmount)}`,
    icon: icons.wallet,
    tone: 'purple'
  },
  {
    label: '退款金额',
    value: `¥${formatAmount(normalized.value.refundAmount)}`,
    icon: icons.refund,
    tone: 'orange'
  },
  {
    label: '净支付',
    value: `¥${formatAmount(normalized.value.netPaid)}`,
    icon: icons.wallet,
    tone: 'purple'
  },
  {
    label: '支付渠道',
    value: formatPayChannel(normalized.value.payChannel),
    icon: icons.card,
    tone: 'purple'
  },
  {
    label: '支付时间',
    value: formatDateTime(normalized.value.payTime),
    icon: icons.clock,
    tone: 'purple',
    wide: true
  }
])

const normalizeDetail = (payload) => {
  const d = payload?.data ?? payload ?? {}
  return d
}

const loadDetail = async () => {
  if (!orderId.value) return
  try {
    loading.value = true
    const { data } = await getOrderDetail({ orderId: orderId.value })
    detail.value = normalizeDetail(data)
  } finally {
    loading.value = false
  }
}

const copyOrderNo = () => {
  if (!normalized.value.orderNo || normalized.value.orderNo === '-') {
    uni.showToast({ title: '暂无订单号', icon: 'none' })
    return
  }
  uni.setClipboardData({
    data: String(normalized.value.orderNo),
    success: () => uni.showToast({ title: '订单号已复制', icon: 'success' })
  })
}

const goBack = () => {
  backToPrevious()
}

const contactService = () => {
  uni.showToast({ title: '请联系在线客服处理', icon: 'none' })
}

const repeatOrder = () => {
  if (isBooking(normalized.value.bizType)) {
    uni.navigateTo({ url: '/pages/booking/index' })
    return
  }
  uni.switchTab({ url: '/pages/courses/index' })
}

const isBooking = (value) => {
  const raw = String(value || '').toLowerCase()
  return raw.includes('booking') || raw.includes('coach')
}

const bizIcon = (value) => (isBooking(value) ? icons.booking : icons.course)
const bizTone = (value) => (isBooking(value) ? 'is-orange' : 'is-purple')

const payTone = (status) => {
  const raw = String(status || '').toUpperCase()
  if (raw === 'PAID') return 'success'
  if (raw === 'UNPAID') return 'warning'
  if (raw === 'REFUNDED') return 'info'
  return 'plain'
}

const orderTone = (status) => {
  const raw = String(status || '').toUpperCase()
  if (raw === 'PAID') return 'success'
  if (raw === 'UNPAID' || raw === 'NEW') return 'warning'
  if (raw === 'REFUNDED') return 'info'
  if (raw === 'CLOSED' || raw === 'CANCELLED') return 'muted'
  return 'plain'
}

const formatBizType = (value) => {
  const map = { coach_booking: '预约订单', course: '课程订单', course_enrollment: '课程订单' }
  return map[String(value || '').toLowerCase()] || '业务订单'
}

const formatPayStatus = (status) => {
  const map = { UNPAID: '待支付', PAID: '已支付', CLOSED: '已关闭', REFUNDED: '已退款' }
  return map[String(status || '').toUpperCase()] || status || '-'
}

const formatOrderStatus = (status) => {
  const map = {
    NEW: '新建',
    UNPAID: '待支付',
    PAID: '已完成',
    CLOSED: '已关闭',
    CANCELLED: '已取消',
    REFUNDED: '已退款'
  }
  return map[String(status || '').toUpperCase()] || status || '-'
}

const formatRefundStatus = (status) => {
  const map = { PENDING: '处理中', REFUNDED: '已退款', REJECTED: '已驳回' }
  return map[String(status || '').toUpperCase()] || '无退款'
}

const formatPayChannel = (channel) => {
  const map = { wechat: '微信支付', alipay: '支付宝支付', mock: '模拟支付' }
  return map[String(channel || '').toLowerCase()] || channel || '未支付'
}

const formatFinanceDisplay = (value, refundStatus, payStatus, orderStatus) => {
  const raw = String(value || '').trim()
  const map = {
    Refunded: '本订单已退款，款项已按原路径退回。',
    'Paid (awaiting service)': '本订单已完成支付，款项已结算至平台。',
    Closed: '本订单已关闭，未产生实际支付。',
    Unpaid: '本订单正在等待支付，请在业务页面继续处理。',
    Unknown: '订单资金状态暂时无法识别。'
  }
  if (map[raw]) return map[raw]
  if (String(refundStatus || '').toUpperCase() === 'REFUNDED') return map.Refunded
  if (String(payStatus || '').toUpperCase() === 'PAID') return map['Paid (awaiting service)']
  if (String(orderStatus || '').toUpperCase() === 'CLOSED') return map.Closed
  return '本订单资金状态已同步。'
}

const formatFinanceExplain = (value, refundStatus, payStatus, orderStatus) => {
  const raw = String(value || '').trim()
  const map = {
    'Refund completed': '退款已完成，如未到账请联系在线客服。',
    'Payment received': '如有疑问，请联系在线客服。',
    'Order closed': '订单已关闭，无需继续支付。',
    'Awaiting payment': '请回到对应业务页面完成支付或取消。',
    'Unknown status': '请稍后刷新或联系在线客服。'
  }
  if (map[raw]) return map[raw]
  if (String(refundStatus || '').toUpperCase() === 'REFUNDED') return map['Refund completed']
  if (String(payStatus || '').toUpperCase() === 'PAID') return map['Payment received']
  if (String(orderStatus || '').toUpperCase() === 'CLOSED') return map['Order closed']
  return '如有疑问，请联系在线客服。'
}

const formatAmount = (value) => {
  if (value === null || value === undefined || value === '') return '0.00'
  const amount = Number(value)
  return Number.isFinite(amount) ? amount.toFixed(2) : String(value)
}

const formatDateTime = (value) => {
  if (!value) return '-'
  const raw = String(value)
  if (raw.includes('T')) {
    const [date, time] = raw.split('T')
    return `${date} ${String(time || '').slice(0, 8)}`
  }
  if (raw.length >= 16 && raw.includes('-')) return raw.slice(0, 16)
  return raw
}

onLoad((query) => {
  orderId.value = query?.id || query?.orderId || ''
})

onShow(() => {
  if (ensureLogin()) {
    loadDetail()
  }
})
</script>

<style scoped lang="scss">
.detail-page {
  position: relative;
  min-height: 100vh;
  padding: calc(var(--status-bar-height) + 28rpx) 28rpx 138rpx;
  overflow-x: hidden;
  background:
    radial-gradient(circle at 0 0, rgba(139, 99, 255, 0.18), transparent 34%),
    radial-gradient(circle at 100% 6%, rgba(255, 128, 111, 0.18), transparent 30%),
    linear-gradient(180deg, #fff7fb 0%, #fffaf4 52%, #fffdf9 100%);
  color: #24104f;
}

.page-glow {
  position: fixed;
  z-index: 0;
  width: 320rpx;
  height: 320rpx;
  border-radius: 999rpx;
  filter: blur(22rpx);
  opacity: 0.45;
  pointer-events: none;
}

.page-glow--left {
  left: -170rpx;
  top: 140rpx;
  background: #e8ddff;
}

.page-glow--right {
  right: -160rpx;
  top: 20rpx;
  background: #ffd8df;
}

.topbar,
.summary-card,
.status-grid-card,
.fee-card,
.finance-card,
.refund-card,
.loading-card,
.bottom-actions {
  position: relative;
  z-index: 1;
}

button {
  margin: 0;
  padding: 0;
  border: 0;
  background: transparent;
  line-height: 1.2;
}

button::after {
  border: 0;
}

.topbar {
  display: grid;
  grid-template-columns: 64rpx 1fr 64rpx;
  align-items: center;
  min-height: 70rpx;
  margin-bottom: 34rpx;
}

.topbar-title {
  color: #24104f;
  font-size: 34rpx;
  font-weight: 950;
  text-align: center;
}

.icon-button {
  display: grid;
  width: 64rpx;
  height: 64rpx;
  place-items: center;
}

.top-icon {
  width: 36rpx;
  height: 36rpx;
}

.summary-card,
.status-grid-card,
.fee-card,
.finance-card,
.refund-card,
.loading-card {
  border: 2rpx solid rgba(52, 32, 95, 0.1);
  border-radius: 28rpx;
  background: rgba(255, 255, 255, 0.96);
  box-shadow: 0 12rpx 32rpx rgba(52, 32, 95, 0.08);
}

.summary-card {
  padding: 24rpx;
  background: linear-gradient(110deg, rgba(246, 239, 255, 0.98), rgba(255, 234, 239, 0.98));
}

.summary-top,
.order-no-row,
.summary-main,
.section-head,
.refund-head,
.bottom-actions,
.fee-item,
.fee-lines view,
.refund-lines view {
  display: flex;
  align-items: center;
}

.summary-top {
  justify-content: space-between;
  gap: 16rpx;
}

.order-no-row {
  min-width: 0;
  gap: 12rpx;
  color: #6f6095;
  font-size: 24rpx;
  font-weight: 900;
}

.summary-icon {
  display: grid;
  width: 42rpx;
  height: 42rpx;
  place-items: center;
  border-radius: 14rpx;
  background: #eee5ff;
}

.summary-icon image {
  width: 24rpx;
  height: 24rpx;
}

.summary-order-no {
  max-width: 330rpx;
  overflow: hidden;
  color: #5f4a85;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.copy-mini {
  display: grid;
  width: 34rpx;
  height: 34rpx;
  flex: 0 0 auto;
  place-items: center;
}

.copy-mini image {
  width: 24rpx;
  height: 24rpx;
}

.status-pill {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 44rpx;
  padding: 0 22rpx;
  border-radius: 999rpx;
  font-size: 24rpx;
  font-weight: 950;
  white-space: nowrap;
}

.success {
  color: #19a65f;
}

.warning {
  color: #ff7a18;
}

.info {
  color: #238bd6;
}

.danger {
  color: #ff6f61;
}

.muted {
  color: #93869d;
}

.plain {
  color: #8b63ff;
}

.status-pill.success {
  background: #dcf8e7;
}

.status-pill.warning {
  background: #fff0d8;
}

.status-pill.info {
  background: #dff2ff;
}

.status-pill.danger {
  background: #ffe2e8;
}

.status-pill.muted {
  background: #f1edf5;
}

.status-pill.plain {
  background: #f0e7ff;
}

.summary-main {
  gap: 20rpx;
  margin-top: 24rpx;
}

.summary-visual {
  display: grid;
  width: 132rpx;
  height: 112rpx;
  flex: 0 0 auto;
  place-items: center;
  border-radius: 18rpx;
}

.summary-visual image {
  width: 58rpx;
  height: 58rpx;
}

.is-purple {
  background: linear-gradient(135deg, #efe7ff 0%, #fff7fb 100%);
}

.is-orange {
  background: linear-gradient(135deg, #ffe2d8 0%, #fff0c8 100%);
}

.summary-copy {
  flex: 1;
  min-width: 0;
}

.summary-title,
.summary-sub,
.summary-location {
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.summary-title {
  color: #24104f;
  font-size: 31rpx;
  font-weight: 950;
}

.summary-sub,
.summary-location {
  margin-top: 9rpx;
  color: #6f6095;
  font-size: 24rpx;
  font-weight: 760;
}

.summary-amount {
  flex: 0 0 150rpx;
  text-align: right;
}

.summary-amount text:first-child {
  display: block;
  color: #ff6f61;
  font-size: 36rpx;
  font-weight: 950;
}

.summary-amount text:last-child {
  display: block;
  margin-top: 20rpx;
  color: #6f6095;
  font-size: 22rpx;
  font-weight: 850;
}

.status-grid-card {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 18rpx;
  margin-top: 22rpx;
  padding: 24rpx;
}

.status-cell {
  display: grid;
  grid-template-columns: 74rpx minmax(0, 1fr);
  gap: 16rpx;
  align-items: center;
  min-height: 112rpx;
  padding: 18rpx;
  border-radius: 20rpx;
  background: linear-gradient(135deg, #fbf8ff 0%, #fffdf8 100%);
}

.status-cell.wide {
  grid-column: 1 / -1;
}

.cell-icon {
  display: grid;
  width: 66rpx;
  height: 66rpx;
  place-items: center;
  border-radius: 18rpx;
  background: #eee5ff;
}

.cell-icon.orange {
  background: #ffe7d5;
}

.cell-icon image {
  width: 34rpx;
  height: 34rpx;
}

.cell-label,
.cell-value {
  display: block;
}

.cell-label {
  color: #7b6f98;
  font-size: 23rpx;
  font-weight: 800;
}

.cell-value {
  margin-top: 10rpx;
  overflow: hidden;
  color: #24104f;
  font-size: 27rpx;
  font-weight: 950;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.fee-card,
.finance-card,
.refund-card,
.loading-card {
  margin-top: 22rpx;
  padding: 26rpx;
}

.section-title {
  color: #24104f;
  font-size: 30rpx;
  font-weight: 950;
}

.fee-list {
  display: flex;
  flex-direction: column;
  gap: 18rpx;
  margin-top: 24rpx;
}

.fee-item {
  gap: 18rpx;
}

.fee-icon {
  display: grid;
  width: 78rpx;
  height: 78rpx;
  place-items: center;
  border-radius: 18rpx;
  background: #f0e7ff;
}

.fee-icon image {
  width: 38rpx;
  height: 38rpx;
}

.fee-main {
  flex: 1;
  min-width: 0;
}

.fee-title,
.fee-sub {
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.fee-title {
  color: #24104f;
  font-size: 29rpx;
  font-weight: 950;
}

.fee-sub {
  margin-top: 8rpx;
  color: #7b6f98;
  font-size: 23rpx;
  font-weight: 760;
}

.fee-amount {
  color: #24104f;
  font-size: 31rpx;
  font-weight: 950;
}

.fee-lines {
  display: flex;
  flex-direction: column;
  gap: 18rpx;
  margin-top: 28rpx;
  padding-top: 24rpx;
  border-top: 2rpx solid rgba(52, 32, 95, 0.08);
}

.fee-lines view,
.refund-lines view {
  justify-content: space-between;
  gap: 20rpx;
  color: #7b6f98;
  font-size: 25rpx;
  font-weight: 800;
}

.fee-lines view text:last-child,
.refund-lines view text:last-child {
  color: #24104f;
  font-weight: 900;
  text-align: right;
}

.total-line {
  padding-top: 16rpx;
  border-top: 2rpx solid rgba(52, 32, 95, 0.08);
}

.total-line text:last-child {
  color: #ff6f61 !important;
  font-size: 34rpx;
}

.section-head {
  gap: 12rpx;
}

.section-icon {
  display: grid;
  width: 40rpx;
  height: 40rpx;
  place-items: center;
  border-radius: 12rpx;
  background: #eee5ff;
}

.section-icon.orange {
  background: #ffe7d5;
}

.section-icon image {
  width: 24rpx;
  height: 24rpx;
}

.finance-note {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 96rpx;
  gap: 18rpx;
  align-items: center;
  margin-top: 22rpx;
  padding: 24rpx;
  border-radius: 20rpx;
  background: linear-gradient(135deg, #f4efff 0%, #fff8fb 100%);
}

.finance-note text {
  display: block;
  color: #6f6095;
  font-size: 25rpx;
  font-weight: 800;
  line-height: 1.55;
}

.finance-note text + text {
  margin-top: 8rpx;
}

.finance-note image {
  width: 78rpx;
  height: 78rpx;
}

.refund-head {
  justify-content: space-between;
}

.refund-lines {
  display: flex;
  flex-direction: column;
  gap: 18rpx;
  margin-top: 24rpx;
}

.empty-soft {
  display: flex;
  align-items: center;
  flex-direction: column;
  justify-content: center;
  gap: 12rpx;
  min-height: 160rpx;
  margin-top: 20rpx;
  border-radius: 20rpx;
  background: #f6f1ff;
  color: #7b6f98;
  font-size: 24rpx;
  font-weight: 800;
}

.empty-soft image {
  width: 54rpx;
  height: 54rpx;
}

.loading-card {
  display: flex;
  align-items: center;
  flex-direction: column;
  justify-content: center;
  gap: 18rpx;
  min-height: 420rpx;
  color: #7b6f98;
  font-size: 25rpx;
  font-weight: 850;
}

.loading-icon {
  width: 70rpx;
  height: 70rpx;
}

.retry-button {
  min-width: 170rpx;
  min-height: 62rpx;
  border-radius: 999rpx;
  background: #8b63ff;
  color: #fff;
  font-size: 24rpx;
  font-weight: 900;
}

.bottom-actions {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  z-index: 10;
  display: grid;
  grid-template-columns: 0.86fr 1.5fr;
  gap: 18rpx;
  padding: 24rpx 28rpx calc(24rpx + env(safe-area-inset-bottom));
  border-radius: 34rpx 34rpx 0 0;
  background: rgba(255, 255, 255, 0.96);
  box-shadow: 0 -12rpx 30rpx rgba(52, 32, 95, 0.08);
}

.service-button,
.primary-button {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12rpx;
  min-height: 76rpx;
  border-radius: 999rpx;
  font-size: 28rpx;
  font-weight: 950;
}

.service-button {
  background: #f4efff;
  color: #24104f;
}

.service-button image {
  width: 34rpx;
  height: 34rpx;
}

.primary-button {
  background: linear-gradient(110deg, #8b63ff 0%, #d987d5 52%, #ff806f 100%);
  color: #fff;
}

.spinning {
  animation: spin 0.9s linear infinite;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}
</style>
