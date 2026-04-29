<template>
  <SlPage>
    <SlTopBar title="计划详情" eyebrow="PLAN DETAIL" show-back subtitle="查看计划节点，后续会按设计图做动作时间线。" />
    <view class="detail-card sl-card">
      <text class="sl-list-card__title">{{ title }}</text>
      <text class="sl-list-card__meta">{{ summary }}</text>
      <view class="sl-chip-row">
        <text class="sl-chip">计划ID {{ planId || '-' }}</text>
        <text class="sl-chip">动作 {{ items.length }}</text>
      </view>
    </view>
    <view class="sl-section sl-list">
      <SlEmpty v-if="!items.length" title="暂无动作节点" description="有计划明细时这里会展示 Day 时间线。" />
      <view v-for="item in items" :key="item.id" class="sl-list-card sl-card">
        <text class="sl-list-card__title">{{ item.title || item.actionName || `动作 ${item.id}` }}</text>
        <text class="sl-list-card__meta">{{ item.description || `第 ${item.dayNo || '-'} 天 · ${item.durationMin || item.duration || '-'} 分钟` }}</text>
      </view>
    </view>
    <SlPrimaryButton text="订阅 / 激活计划" :loading="submitting" @click="subscribe" />
  </SlPage>
</template>

<script setup>
import { computed, ref } from 'vue'
import { onLoad, onShow } from '@dcloudio/uni-app'
import SlEmpty from '../../components/SlEmpty.vue'
import SlPage from '../../components/SlPage.vue'
import SlPrimaryButton from '../../components/SlPrimaryButton.vue'
import SlTopBar from '../../components/SlTopBar.vue'
import { getPlanDetail, subscribePlan } from '../../api/modules/plan'
import { ensureLogin } from '../../utils/authGuard'

const planId = ref('')
const detail = ref(null)
const submitting = ref(false)
const title = computed(() => detail.value?.plan?.title || detail.value?.title || '训练计划')
const summary = computed(() => detail.value?.plan?.description || detail.value?.description || '计划详情骨架已准备。')
const items = computed(() => detail.value?.items || detail.value?.planItems || [])

const loadDetail = async () => {
  if (!planId.value) return
  const { data } = await getPlanDetail(planId.value)
  detail.value = data
}

const subscribe = async () => {
  if (!planId.value) return
  try {
    submitting.value = true
    await subscribePlan({ planId: Number(planId.value) })
    uni.showToast({ title: '计划已提交', icon: 'success' })
  } finally {
    submitting.value = false
  }
}

onLoad((query) => {
  planId.value = query?.id || ''
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
</style>
