<template>
  <SlPage>
    <SlTopBar title="教练申请" eyebrow="COACH" show-back subtitle="申请资料、审核状态和教练档案预览的手机端入口。" />
    <view class="sl-grid-2 sl-section">
      <SlMetricCard label="申请状态" :value="applyInfo?.status || '待提交'" note="我的申请接口" />
      <SlMetricCard label="课时价格" :value="applyInfo?.price || form.price" note="元 / 课时" tone="warm" />
    </view>
    <view class="apply-card sl-card sl-section">
      <text class="sl-section-title">申请资料</text>
      <view class="sl-form form-space">
        <textarea class="sl-textarea" v-model="form.bio" placeholder="个人简介" />
        <input class="sl-input" v-model="form.skills" placeholder="擅长领域，逗号分隔" />
        <input class="sl-input" v-model="form.years" placeholder="从业年限" />
        <input class="sl-input" v-model="form.price" placeholder="课时价格（元）" />
      </view>
      <SlPrimaryButton text="提交申请" :loading="submitting" @click="submit" />
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
import { applyCoach, getMyCoachApplication } from '../../api/modules/coach'
import { ensureLogin } from '../../utils/authGuard'

const applyInfo = ref(null)
const submitting = ref(false)
const form = reactive({
  bio: '',
  skills: '',
  years: '2',
  price: '199'
})

const loadApply = async () => {
  const { data } = await getMyCoachApplication()
  applyInfo.value = data || null
  if (data) {
    form.bio = data.bio || form.bio
    form.skills = data.skills || data.skillTags || form.skills
    form.years = String(data.years || form.years)
    form.price = String(data.price || form.price)
  }
}

const submit = async () => {
  if (!form.bio || !form.skills) {
    uni.showToast({ title: '请填写简介和擅长领域', icon: 'none' })
    return
  }
  try {
    submitting.value = true
    await applyCoach({
      bio: form.bio,
      skills: form.skills,
      years: Number(form.years || 0),
      price: Number(form.price || 0)
    })
    uni.showToast({ title: '申请已提交', icon: 'success' })
    loadApply()
  } finally {
    submitting.value = false
  }
}

onShow(() => {
  if (ensureLogin()) {
    loadApply()
  }
})
</script>

<style scoped lang="scss">
.apply-card {
  padding: 28rpx;
}

.form-space {
  margin: 24rpx 0;
}
</style>
