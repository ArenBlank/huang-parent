<template>
  <SlPage>
    <SlTopBar title="订单详情" eyebrow="ORDER DETAIL" show-back subtitle="订单号、资金状态和明细会在这里集中展示。" />
    <view class="detail-card sl-card">
      <text class="sl-list-card__title">{{ detail?.orderNo || `订单 ${orderId || '-'}` }}</text>
      <text class="amount">{{ detail?.totalAmount ?? '-' }}</text>
      <view class="sl-chip-row">
        <text class="sl-chip">支付 {{ detail?.payStatus || '-' }}</text>
        <text class="sl-chip">订单 {{ detail?.orderStatus || '-' }}</text>
      </view>
    </view>
    <SlEmpty v-if="!detail" class="sl-section" title="等待订单详情" description="从订单列表进入后会读取 /app/order/detail。" action-text="重新加载" @action="loadDetail" />
  </SlPage>
</template>

<script setup>
import { ref } from 'vue'
import { onLoad, onShow } from '@dcloudio/uni-app'
import SlEmpty from '../../components/SlEmpty.vue'
import SlPage from '../../components/SlPage.vue'
import SlTopBar from '../../components/SlTopBar.vue'
import { getOrderDetail } from '../../api/modules/order'
import { ensureLogin } from '../../utils/authGuard'

const orderId = ref('')
const detail = ref(null)

const loadDetail = async () => {
  if (!orderId.value) return
  const { data } = await getOrderDetail({ orderId: orderId.value })
  detail.value = data || null
}

onLoad((query) => {
  orderId.value = query?.id || ''
})

onShow(() => {
  if (ensureLogin()) {
    loadDetail()
  }
})
</script>

<style scoped lang="scss">
.detail-card {
  margin-top: 24rpx;
  padding: 28rpx;
}

.amount {
  display: block;
  margin-top: 20rpx;
  color: #8b63ff;
  font-size: 58rpx;
  font-weight: 900;
}
</style>
