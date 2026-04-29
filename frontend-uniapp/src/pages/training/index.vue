<template>
  <SlPage>
    <SlTopBar title="打卡成长" eyebrow="CHECK-IN" show-back subtitle="先保留今日打卡表单，后续按 UI 图补动作预览。" />
    <view class="sl-grid-2 sl-section">
      <SlMetricCard label="本周打卡" :value="stats.checkinCount" note="累计次数" />
      <SlMetricCard label="训练时长" :value="stats.duration" note="分钟" tone="warm" />
    </view>
    <view class="check-card sl-card sl-section">
      <text class="sl-section-title">今日打卡</text>
      <view class="sl-form form-space">
        <input class="sl-input" v-model="form.planId" placeholder="训练计划 ID" />
        <input class="sl-input" v-model="form.planItemId" placeholder="动作 ID" />
        <input class="sl-input" v-model="form.durationMin" placeholder="训练时长（分钟）" />
        <input class="sl-input" v-model="form.calories" placeholder="热量消耗" />
        <textarea class="sl-textarea" v-model="form.feeling" placeholder="训练感受" />
      </view>
      <SlPrimaryButton text="提交打卡" :loading="submitting" @click="submit" />
    </view>
  </SlPage>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import SlMetricCard from '../../components/SlMetricCard.vue'
import SlPage from '../../components/SlPage.vue'
import SlPrimaryButton from '../../components/SlPrimaryButton.vue'
import SlTopBar from '../../components/SlTopBar.vue'
import { checkIn, getWeeklyStat } from '../../api/modules/record'
import { ensureLogin } from '../../utils/authGuard'

const submitting = ref(false)
const stats = reactive({ checkinCount: 0, duration: 0 })
const form = reactive({
  planId: '',
  planItemId: '',
  durationMin: '',
  calories: '',
  feeling: ''
})

const loadStats = async () => {
  const { data } = await getWeeklyStat()
  stats.checkinCount = data?.checkinCount ?? data?.totalCount ?? 0
  stats.duration = data?.totalDurationMin ?? data?.totalDuration ?? 0
}

const submit = async () => {
  if (!form.planId || !form.planItemId) {
    uni.showToast({ title: '请填写计划和动作 ID', icon: 'none' })
    return
  }
  try {
    submitting.value = true
    await checkIn({
      planId: Number(form.planId),
      planItemId: Number(form.planItemId),
      durationMin: Number(form.durationMin || 0),
      calories: Number(form.calories || 0),
      feeling: form.feeling
    })
    uni.showToast({ title: '打卡成功', icon: 'success' })
    loadStats()
  } finally {
    submitting.value = false
  }
}

onShow(() => {
  if (ensureLogin()) {
    loadStats()
  }
})
</script>

<style scoped lang="scss">
.check-card {
  padding: 28rpx;
}

.form-space {
  margin: 24rpx 0;
}
</style>
