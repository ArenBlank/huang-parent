<template>
  <view class="orders-page">
    <view class="page-glow page-glow--left"></view>
    <view class="page-glow page-glow--right"></view>

    <view class="topbar">
      <button class="icon-button" aria-label="返回" @click="goBack">
        <image class="top-icon" :src="icons.back" mode="aspectFit" />
      </button>
      <text class="topbar-title">订单中心</text>
      <button class="icon-button" aria-label="更多" @click="openFilterSheet">
        <image class="top-icon" :src="icons.more" mode="aspectFit" />
      </button>
    </view>

    <view class="search-row">
      <view class="search-box">
        <image class="search-icon" :src="icons.search" mode="aspectFit" />
        <input
          v-model.trim="keyword"
          class="search-input"
          placeholder="搜索订单号 / 课程 / 教练 / 订单备注"
          confirm-type="search"
        />
      </view>
      <button class="filter-button" @click="openFilterSheet">
        <image :src="icons.filter" mode="aspectFit" />
        <text>筛选</text>
      </button>
    </view>

    <scroll-view class="status-scroll" scroll-x show-scrollbar="false">
      <view class="status-tabs">
        <button
          v-for="tab in filterTabs"
          :key="tab.key"
          class="status-tab"
          :class="{ active: filterKey === tab.key }"
          @click="selectFilter(tab.key)"
        >
          {{ tab.label }}
        </button>
      </view>
    </scroll-view>

    <view v-if="!filteredOrders.length && !loading" class="empty-card">
      <image class="empty-icon" :src="icons.receipt" mode="aspectFit" />
      <text class="empty-title">暂无匹配订单</text>
      <text class="empty-desc">完成课程报名或教练预约后，订单会同步到这里。</text>
      <button class="empty-button" @click="loadOrders">刷新订单</button>
    </view>

    <view v-else class="order-list">
      <button
        v-for="order in filteredOrders"
        :key="order.id"
        class="order-card"
        @click="openOrder(order)"
      >
        <view class="order-top">
          <view class="type-row">
            <view class="type-icon" :class="bizTone(order.bizType)">
              <image :src="bizIcon(order.bizType)" mode="aspectFit" />
            </view>
            <text class="type-label">{{ formatBizType(order.bizType) }}</text>
            <text class="status-pill" :class="statusMeta(order).tone">{{ statusMeta(order).text }}</text>
          </view>
          <view class="time-row">
            <text>{{ formatDateTime(order.createTime) }}</text>
            <image class="chevron" :src="icons.right" mode="aspectFit" />
          </view>
        </view>

        <view class="order-main">
          <view class="order-visual" :class="bizTone(order.bizType)">
            <image :src="bizIcon(order.bizType)" mode="aspectFit" />
          </view>
          <view class="order-copy">
            <text class="order-title">{{ orderTitle(order) }}</text>
            <text class="order-sub">{{ orderSubtitle(order) }}</text>
            <text class="order-location">{{ formatBizType(order.bizType) }} · {{ itemTypeText(order) }}</text>
          </view>
          <view class="amount-box">
            <text class="amount">¥{{ formatAmount(order.totalAmount) }}</text>
            <text>共{{ itemCount(order) }}件</text>
          </view>
        </view>

        <view class="order-divider"></view>

        <view class="order-meta">
          <view class="meta-left">
            <view class="meta-line">
              <text>订单号</text>
              <text class="order-no">{{ order.orderNo || '-' }}</text>
              <button class="copy-mini" @click.stop="copyOrderNo(order.orderNo)">
                <image :src="icons.copy" mode="aspectFit" />
              </button>
            </view>
            <view class="meta-line">
              <text>支付方式</text>
              <text>{{ formatPayChannel(detailOf(order)?.payChannel) }}</text>
            </view>
          </view>
          <view class="meta-right">
            <view class="meta-line">
              <text>支付状态</text>
              <text :class="statusMeta(order).tone">{{ formatPayStatus(order.payStatus) }}</text>
            </view>
            <view class="meta-line">
              <text>订单状态</text>
              <text :class="orderStatusTone(order.orderStatus)">{{ formatOrderStatus(order.orderStatus) }}</text>
            </view>
          </view>
        </view>

        <view v-if="isUnpaid(order)" class="order-actions">
          <button class="pay-button" @click.stop="openOrder(order)">查看详情</button>
        </view>
      </button>
    </view>

    <view class="custom-tabbar">
      <button v-for="tab in tabs" :key="tab.url" class="tab-item" :class="{ active: tab.active }" @click="goTab(tab)">
        <image class="tab-icon" :src="tab.icon" mode="aspectFit" />
        <text>{{ tab.text }}</text>
      </button>
      <view class="home-indicator"></view>
    </view>

    <view v-if="filterSheetVisible" class="sheet-mask" @click="filterSheetVisible = false">
      <view class="filter-sheet" @click.stop>
        <view class="sheet-handle"></view>
        <view class="sheet-head">
          <view>
            <text class="sheet-title">订单筛选</text>
            <text class="sheet-sub">按后端订单状态和支付状态过滤，不额外伪造分类。</text>
          </view>
          <button class="sheet-close" @click="filterSheetVisible = false">关闭</button>
        </view>
        <view class="sheet-tabs">
          <button
            v-for="tab in filterTabs"
            :key="tab.key"
            class="sheet-tab"
            :class="{ active: filterKey === tab.key }"
            @click="selectFilter(tab.key)"
          >
            <text>{{ tab.label }}</text>
            <text>{{ tab.count }}</text>
          </button>
        </view>
        <view class="sheet-actions">
          <button class="sheet-ghost" @click="keyword = ''">清空搜索</button>
          <button class="sheet-primary" :disabled="loading" @click="refreshFromSheet">
            {{ loading ? '刷新中...' : '刷新订单' }}
          </button>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup>
import {
  faCalendarCheck,
  faChevronLeft,
  faChevronRight,
  faCirclePlay,
  faClipboardList,
  faCopy,
  faEllipsis,
  faFilter,
  faHouse,
  faListCheck,
  faMagnifyingGlass,
  faReceipt,
  faUser
} from '@fortawesome/free-solid-svg-icons'
import { computed, reactive, ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { getOrderDetail, listOrders } from '../../api/modules/order'
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
  copy: faIcon(faCopy, '#9b88bf'),
  course: faIcon(faClipboardList),
  filter: faIcon(faFilter),
  home: faIcon(faHouse),
  list: faIcon(faListCheck),
  more: faIcon(faEllipsis),
  play: faIcon(faCirclePlay),
  receipt: faIcon(faReceipt),
  right: faIcon(faChevronRight),
  search: faIcon(faMagnifyingGlass, '#6f6095'),
  user: faIcon(faUser)
}

const loading = ref(false)
const keyword = ref('')
const filterKey = ref('all')
const filterSheetVisible = ref(false)
const orders = ref([])
const detailCache = reactive({})

const tabs = computed(() => [
  { text: '首页', url: '/pages/home/index', icon: icons.home },
  { text: '计划', url: '/pages/plans/index', icon: icons.list },
  { text: '课程', url: '/pages/courses/index', icon: icons.play },
  { text: '我的', url: '/pages/mine/index', icon: icons.user, active: true }
])

const filterTabs = computed(() => [
  { key: 'all', label: '全部', count: orders.value.length },
  { key: 'unpaid', label: '待支付', count: orders.value.filter((row) => matchesFilter(row, 'unpaid')).length },
  { key: 'paid', label: '已支付', count: orders.value.filter((row) => matchesFilter(row, 'paid')).length },
  { key: 'refund', label: '已退款', count: orders.value.filter((row) => matchesFilter(row, 'refund')).length },
  { key: 'closed', label: '已取消', count: orders.value.filter((row) => matchesFilter(row, 'closed')).length }
])

const filteredOrders = computed(() => {
  const key = keyword.value.trim().toLowerCase()
  return orders.value.filter((row) => {
    if (!matchesFilter(row, filterKey.value)) return false
    if (!key) return true
    const detail = detailOf(row)
    const haystack = [
      row.orderNo,
      row.bizType,
      formatBizType(row.bizType),
      row.payStatus,
      formatPayStatus(row.payStatus),
      row.orderStatus,
      formatOrderStatus(row.orderStatus),
      row.totalAmount,
      ...(detail?.items || []).map((item) => item.itemName)
    ]
      .filter(Boolean)
      .join(' ')
      .toLowerCase()
    return haystack.includes(key)
  })
})

const normalizeList = (payload) => {
  const data = payload?.data ?? payload
  if (Array.isArray(data)) return data
  if (Array.isArray(data?.list)) return data.list
  if (Array.isArray(data?.records)) return data.records
  return []
}

const detailOf = (order) => detailCache[order?.id] || null

const normalizeDetail = (payload) => {
  const d = payload?.data ?? payload ?? {}
  const order = d.order || d
  return {
    ...d,
    order,
    items: d.items || order.items || [],
    payChannel: d.payChannel || d.payment?.payChannel,
    payTime: d.payTime || d.payment?.payTime,
    refundStatus: d.refundStatus || d.refund?.refundStatus,
    refundReason: d.refundReason || d.refund?.reason
  }
}

const loadOrderDetails = async (rows) => {
  await Promise.allSettled(
    rows.map(async (row) => {
      if (!row?.id || detailCache[row.id]) return
      const { data } = await getOrderDetail({ orderId: row.id })
      detailCache[row.id] = normalizeDetail(data)
    })
  )
}

const loadOrders = async () => {
  try {
    loading.value = true
    const { data } = await listOrders({ limit: 50 })
    orders.value = normalizeList(data)
    await loadOrderDetails(orders.value)
  } finally {
    loading.value = false
  }
}

const matchesFilter = (row, key) => {
  if (key === 'all') return true
  const pay = String(row?.payStatus || '').toUpperCase()
  const order = String(row?.orderStatus || '').toUpperCase()
  if (key === 'unpaid') return pay === 'UNPAID' && !['CLOSED', 'CANCELLED'].includes(order)
  if (key === 'paid') return pay === 'PAID' && order !== 'REFUNDED'
  if (key === 'refund') return pay === 'REFUNDED' || order === 'REFUNDED'
  if (key === 'closed') return ['CLOSED', 'CANCELLED'].includes(order) || pay === 'CLOSED'
  return true
}

const selectFilter = (key) => {
  filterKey.value = key
  filterSheetVisible.value = false
}

const openFilterSheet = () => {
  filterSheetVisible.value = true
}

const refreshFromSheet = async () => {
  await loadOrders()
  filterSheetVisible.value = false
}

const openOrder = (order) => {
  if (!order?.id) return
  uni.navigateTo({ url: `/pages/order-detail/index?id=${encodeURIComponent(order.id)}` })
}

const goBack = () => {
  backToPrevious()
}

const goTab = (tab) => {
  if (tab.active) return
  uni.switchTab({ url: tab.url })
}

const copyOrderNo = (value) => {
  if (!value) {
    uni.showToast({ title: '暂无订单号', icon: 'none' })
    return
  }
  uni.setClipboardData({
    data: String(value),
    success: () => uni.showToast({ title: '订单号已复制', icon: 'success' })
  })
}

const bizIcon = (value) => {
  const raw = String(value || '').toLowerCase()
  if (raw.includes('booking') || raw.includes('coach')) return icons.booking
  return icons.course
}

const bizTone = (value) => {
  const raw = String(value || '').toLowerCase()
  if (raw.includes('booking') || raw.includes('coach')) return 'is-orange'
  return 'is-purple'
}

const orderTitle = (order) => {
  const items = detailOf(order)?.items || []
  return items[0]?.itemName || `${formatBizType(order.bizType)} ${order.id || ''}`
}

const orderSubtitle = (order) => {
  const items = detailOf(order)?.items || []
  const item = items[0]
  if (item?.itemType) return `项目类型 ${item.itemType}`
  return formatDateTime(order.createTime)
}

const itemTypeText = (order) => {
  const item = (detailOf(order)?.items || [])[0]
  return item?.itemType || '订单项目'
}

const itemCount = (order) => {
  const items = detailOf(order)?.items || []
  return items.reduce((sum, item) => sum + Number(item.quantity || 1), 0) || 1
}

const isUnpaid = (order) => String(order?.payStatus || '').toUpperCase() === 'UNPAID'

const statusMeta = (order) => {
  const pay = String(order?.payStatus || '').toUpperCase()
  const status = String(order?.orderStatus || '').toUpperCase()
  if (pay === 'UNPAID') return { text: '待支付', tone: 'warning' }
  if (pay === 'PAID') return { text: status === 'PAID' ? '已完成' : '已支付', tone: 'success' }
  if (pay === 'REFUNDED' || status === 'REFUNDED') return { text: '已退款', tone: 'info' }
  if (status === 'CLOSED' || status === 'CANCELLED') return { text: '已取消', tone: 'muted' }
  return { text: formatOrderStatus(order?.orderStatus), tone: 'plain' }
}

const orderStatusTone = (status) => {
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

const formatPayChannel = (channel) => {
  const map = { wechat: '微信支付', alipay: '支付宝支付', mock: '模拟支付' }
  return map[String(channel || '').toLowerCase()] || channel || '未支付'
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
    return `${date} ${String(time || '').slice(0, 5)}`
  }
  if (raw.length >= 16 && raw.includes('-')) return raw.slice(0, 16)
  return raw
}

onShow(() => {
  if (ensureLogin()) {
    loadOrders()
  }
})
</script>

<style scoped lang="scss">
.orders-page {
  position: relative;
  min-height: 100vh;
  padding: calc(var(--status-bar-height) + 28rpx) 28rpx 206rpx;
  overflow-x: hidden;
  background:
    radial-gradient(circle at 0 0, rgba(139, 99, 255, 0.18), transparent 34%),
    radial-gradient(circle at 100% 6%, rgba(255, 128, 111, 0.18), transparent 30%),
    linear-gradient(180deg, #fff7fb 0%, #fffaf4 48%, #fffdf9 100%);
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
  top: 150rpx;
  background: #e8ddff;
}

.page-glow--right {
  right: -160rpx;
  top: 20rpx;
  background: #ffd8df;
}

.topbar,
.search-row,
.status-scroll,
.order-list,
.empty-card,
.custom-tabbar {
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

.search-row {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 130rpx;
  gap: 18rpx;
  align-items: center;
}

.search-box,
.filter-button {
  min-height: 76rpx;
  border-radius: 999rpx;
  background: rgba(255, 255, 255, 0.92);
  box-shadow: 0 10rpx 28rpx rgba(52, 32, 95, 0.08);
}

.search-box {
  display: flex;
  align-items: center;
  gap: 16rpx;
  padding: 0 24rpx;
}

.search-icon,
.filter-button image {
  width: 34rpx;
  height: 34rpx;
}

.search-input {
  flex: 1;
  height: 76rpx;
  color: #24104f;
  font-size: 26rpx;
  font-weight: 750;
}

.filter-button {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10rpx;
  color: #24104f;
  font-size: 25rpx;
  font-weight: 900;
}

.status-scroll {
  width: 100%;
  margin: 26rpx 0;
  overflow: hidden;
  white-space: nowrap;
  scrollbar-width: none;
}

.status-scroll::-webkit-scrollbar,
.status-tabs::-webkit-scrollbar {
  display: none;
  width: 0;
  height: 0;
}

.status-scroll :deep(.uni-scroll-view),
.status-scroll :deep(.uni-scroll-view-content) {
  scrollbar-width: none;
}

.status-scroll :deep(.uni-scroll-view::-webkit-scrollbar),
.status-scroll :deep(.uni-scroll-view-content::-webkit-scrollbar) {
  display: none;
  width: 0;
  height: 0;
}

.status-tabs {
  display: inline-flex;
  gap: 18rpx;
  padding-bottom: 0;
}

.status-tab {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 126rpx;
  height: 64rpx;
  padding: 0 28rpx;
  border-radius: 999rpx;
  background: rgba(255, 255, 255, 0.78);
  color: #24104f;
  font-size: 25rpx;
  font-weight: 900;
  line-height: 1;
  box-shadow: 0 8rpx 20rpx rgba(52, 32, 95, 0.04);
  box-sizing: border-box;
  vertical-align: top;
}

.status-tab.active {
  background: linear-gradient(135deg, #8b63ff 0%, #9f6cff 100%);
  color: #ffffff;
  box-shadow: 0 12rpx 26rpx rgba(139, 99, 255, 0.26);
}

.order-list {
  display: flex;
  flex-direction: column;
  gap: 22rpx;
}

.order-card {
  width: 100%;
  padding: 24rpx;
  border: 2rpx solid rgba(52, 32, 95, 0.1);
  border-radius: 28rpx;
  background: rgba(255, 255, 255, 0.96);
  box-shadow: 0 12rpx 32rpx rgba(52, 32, 95, 0.08);
  text-align: left;
}

.order-top,
.type-row,
.time-row,
.order-main,
.meta-line,
.order-actions {
  display: flex;
  align-items: center;
}

.order-top {
  justify-content: space-between;
  gap: 16rpx;
}

.type-row {
  min-width: 0;
  gap: 12rpx;
}

.type-icon {
  display: grid;
  width: 42rpx;
  height: 42rpx;
  place-items: center;
  border-radius: 14rpx;
}

.type-icon image {
  width: 24rpx;
  height: 24rpx;
}

.type-label {
  color: #7b6f98;
  font-size: 25rpx;
  font-weight: 900;
}

.status-pill {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 42rpx;
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

.status-pill.muted {
  background: #f1edf5;
}

.status-pill.plain {
  background: #f0e7ff;
}

.time-row {
  flex-shrink: 0;
  gap: 10rpx;
  color: #7b6f98;
  font-size: 23rpx;
  font-weight: 750;
}

.chevron {
  width: 20rpx;
  height: 20rpx;
}

.order-main {
  gap: 20rpx;
  margin-top: 22rpx;
}

.order-visual {
  display: grid;
  width: 128rpx;
  height: 108rpx;
  flex: 0 0 auto;
  place-items: center;
  border-radius: 18rpx;
}

.order-visual image {
  width: 56rpx;
  height: 56rpx;
}

.is-purple {
  background: linear-gradient(135deg, #efe7ff 0%, #fff7fb 100%);
}

.is-orange {
  background: linear-gradient(135deg, #ffe2d8 0%, #fff0c8 100%);
}

.order-copy {
  flex: 1;
  min-width: 0;
}

.order-title,
.order-sub,
.order-location {
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.order-title {
  color: #24104f;
  font-size: 30rpx;
  font-weight: 950;
}

.order-sub {
  margin-top: 10rpx;
  color: #6f6095;
  font-size: 24rpx;
  font-weight: 750;
}

.order-location {
  margin-top: 8rpx;
  color: #6f6095;
  font-size: 23rpx;
  font-weight: 700;
}

.amount-box {
  flex: 0 0 156rpx;
  text-align: right;
}

.amount {
  display: block;
  color: #ff6f61;
  font-size: 32rpx;
  font-weight: 950;
}

.amount-box text:last-child {
  display: block;
  margin-top: 18rpx;
  color: #6f6095;
  font-size: 22rpx;
  font-weight: 800;
}

.order-divider {
  height: 2rpx;
  margin: 24rpx 0 18rpx;
  background: rgba(52, 32, 95, 0.08);
}

.order-meta {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 200rpx;
  gap: 16rpx;
}

.meta-left,
.meta-right {
  display: flex;
  flex-direction: column;
  gap: 12rpx;
}

.meta-line {
  gap: 12rpx;
  color: #24104f;
  font-size: 24rpx;
  font-weight: 850;
}

.meta-line text:first-child {
  flex: 0 0 auto;
  color: #24104f;
}

.meta-line text:nth-child(2) {
  min-width: 0;
  overflow: hidden;
  color: #8a7aa9;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.order-no {
  max-width: 250rpx;
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

.order-actions {
  justify-content: flex-end;
  margin-top: 18rpx;
}

.pay-button {
  min-width: 146rpx;
  min-height: 58rpx;
  border-radius: 999rpx;
  background: linear-gradient(110deg, #8b63ff, #ff806f);
  color: #fff;
  font-size: 24rpx;
  font-weight: 950;
}

.empty-card {
  display: flex;
  align-items: center;
  flex-direction: column;
  justify-content: center;
  gap: 14rpx;
  min-height: 420rpx;
  margin-top: 44rpx;
  padding: 44rpx;
  border-radius: 32rpx;
  background: rgba(255, 255, 255, 0.88);
  box-shadow: 0 12rpx 32rpx rgba(52, 32, 95, 0.08);
  text-align: center;
}

.empty-icon {
  width: 70rpx;
  height: 70rpx;
}

.empty-title {
  color: #24104f;
  font-size: 32rpx;
  font-weight: 950;
}

.empty-desc {
  color: #7b6f98;
  font-size: 24rpx;
  line-height: 1.5;
}

.empty-button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 190rpx;
  min-height: 62rpx;
  margin-top: 10rpx;
  border-radius: 999rpx;
  background: #8b63ff;
  color: #fff;
  font-size: 24rpx;
  font-weight: 900;
  line-height: 1;
  box-sizing: border-box;
}

.custom-tabbar {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  z-index: 8;
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  padding: 18rpx 22rpx calc(env(safe-area-inset-bottom) + 28rpx);
  border: 3rpx solid rgba(139, 99, 255, 0.5);
  border-bottom: 0;
  border-radius: 42rpx 42rpx 0 0;
  background: rgba(255, 255, 255, 0.96);
  box-shadow: 0 -10rpx 26rpx rgba(52, 32, 95, 0.09);
}

.tab-item {
  display: flex;
  align-items: center;
  flex-direction: column;
  gap: 8rpx;
  color: #24104f;
  font-size: 22rpx;
  font-weight: 850;
}

.tab-item.active {
  color: #8b63ff;
}

.tab-icon {
  width: 44rpx;
  height: 44rpx;
}

.home-indicator {
  position: absolute;
  left: 50%;
  bottom: calc(env(safe-area-inset-bottom) + 8rpx);
  width: 160rpx;
  height: 8rpx;
  border-radius: 999rpx;
  background: #24104f;
  transform: translateX(-50%);
}

.sheet-mask {
  position: fixed;
  inset: 0;
  z-index: 30;
  display: flex;
  align-items: flex-end;
  padding: 28rpx;
  background: rgba(36, 16, 79, 0.36);
}

.filter-sheet {
  width: 100%;
  padding: 16rpx 24rpx calc(28rpx + env(safe-area-inset-bottom));
  border-radius: 34rpx;
  background: #fffdf8;
  box-shadow: 0 -18rpx 48rpx rgba(36, 16, 79, 0.18);
}

.sheet-handle {
  width: 78rpx;
  height: 8rpx;
  margin: 0 auto 18rpx;
  border-radius: 999rpx;
  background: #d9ccef;
}

.sheet-head {
  display: flex;
  justify-content: space-between;
  gap: 20rpx;
}

.sheet-title,
.sheet-sub {
  display: block;
}

.sheet-title {
  color: #24104f;
  font-size: 32rpx;
  font-weight: 950;
}

.sheet-sub {
  margin-top: 8rpx;
  color: #7b6f98;
  font-size: 23rpx;
  line-height: 1.45;
}

.sheet-close {
  flex: 0 0 auto;
  color: #8b63ff;
  font-size: 24rpx;
  font-weight: 900;
}

.sheet-tabs {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16rpx;
  margin-top: 26rpx;
}

.sheet-tab {
  display: flex;
  align-items: center;
  justify-content: space-between;
  min-height: 76rpx;
  padding: 0 22rpx;
  border: 2rpx solid rgba(52, 32, 95, 0.14);
  border-radius: 22rpx;
  background: #fff;
  color: #24104f;
  font-size: 24rpx;
  font-weight: 900;
}

.sheet-tab.active {
  border-color: #8b63ff;
  background: #f4efff;
  color: #8b63ff;
}

.sheet-actions {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16rpx;
  margin-top: 24rpx;
}

.sheet-ghost,
.sheet-primary {
  min-height: 72rpx;
  border-radius: 999rpx;
  font-size: 25rpx;
  font-weight: 950;
}

.sheet-ghost {
  background: #f4efff;
  color: #24104f;
}

.sheet-primary {
  background: linear-gradient(110deg, #8b63ff, #ff806f);
  color: #fff;
}
</style>
