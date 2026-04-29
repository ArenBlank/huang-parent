<template>
  <SlPage>
    <SlTopBar title="订单中心" eyebrow="ORDERS" show-back subtitle="移动端先以订单卡片流承接列表和详情跳转。" />
    <view class="search-card sl-card">
      <input class="sl-input" v-model.trim="keyword" placeholder="搜索订单号 / 状态 / 金额" />
      <SlPrimaryButton text="刷新订单" :loading="loading" @click="loadOrders" />
    </view>
    <view class="sl-section sl-list">
      <SlEmpty v-if="!filteredOrders.length && !loading" title="暂无订单" description="完成报名或预约后会在这里出现。" />
      <view v-for="order in filteredOrders" :key="order.id" class="sl-list-card sl-card" @click="openOrder(order)">
        <text class="sl-list-card__title">{{ order.orderNo || `订单 ${order.id}` }}</text>
        <text class="sl-list-card__meta">金额 {{ order.totalAmount ?? '-' }} · 支付 {{ order.payStatus || '-' }}</text>
        <view class="sl-chip-row">
          <text class="sl-chip">{{ order.bizType || '业务订单' }}</text>
          <text class="sl-chip">{{ order.orderStatus || '状态未知' }}</text>
        </view>
      </view>
    </view>
  </SlPage>
</template>

<script setup>
import { computed, ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import SlEmpty from '../../components/SlEmpty.vue'
import SlPage from '../../components/SlPage.vue'
import SlPrimaryButton from '../../components/SlPrimaryButton.vue'
import SlTopBar from '../../components/SlTopBar.vue'
import { listOrders } from '../../api/modules/order'
import { ensureLogin } from '../../utils/authGuard'
import { goPage } from '../../utils/navigation'

const loading = ref(false)
const keyword = ref('')
const orders = ref([])
const filteredOrders = computed(() => {
  const key = keyword.value.toLowerCase()
  if (!key) return orders.value
  return orders.value.filter((item) => JSON.stringify(item).toLowerCase().includes(key))
})

const loadOrders = async () => {
  try {
    loading.value = true
    const { data } = await listOrders({ limit: 20 })
    orders.value = Array.isArray(data) ? data : []
  } finally {
    loading.value = false
  }
}

const openOrder = (order) => {
  goPage(`/pages/order-detail/index?id=${order.id}`)
}

onShow(() => {
  if (ensureLogin()) {
    loadOrders()
  }
})
</script>

<style scoped lang="scss">
.search-card {
  display: flex;
  flex-direction: column;
  gap: 22rpx;
  margin-top: 24rpx;
  padding: 24rpx;
}
</style>
