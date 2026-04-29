<template>
  <SlPage>
    <SlTopBar title="编辑资料" eyebrow="PROFILE EDIT" show-back subtitle="基础资料表单已经接入 /app/profile/info。" />
    <view class="profile-form sl-card">
      <view class="sl-form">
        <input class="sl-input" v-model="form.nickname" placeholder="昵称" />
        <input class="sl-input" v-model="form.email" placeholder="邮箱" />
        <input class="sl-input" v-model="form.phone" placeholder="手机号" />
        <input class="sl-input" v-model="form.occupation" placeholder="职业" />
        <input class="sl-input" v-model="form.height" placeholder="身高（cm）" />
        <input class="sl-input" v-model="form.weight" placeholder="体重（kg）" />
        <input class="sl-input" v-model="form.address" placeholder="地址" />
        <textarea class="sl-textarea" v-model="form.bio" placeholder="简介" />
      </view>
      <SlPrimaryButton class="submit-button" text="保存资料" :loading="saving" @click="save" />
    </view>
  </SlPage>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import SlPage from '../../components/SlPage.vue'
import SlPrimaryButton from '../../components/SlPrimaryButton.vue'
import SlTopBar from '../../components/SlTopBar.vue'
import { getProfile, updateProfile } from '../../api/modules/profile'
import { ensureLogin } from '../../utils/authGuard'
import { useAppAuthStore } from '../../stores/auth'

const store = useAppAuthStore()
const saving = ref(false)
const form = reactive({
  nickname: '',
  email: '',
  phone: '',
  occupation: '',
  height: '',
  weight: '',
  address: '',
  bio: ''
})

const fillForm = (profile = {}) => {
  Object.keys(form).forEach((key) => {
    form[key] = profile[key] == null ? '' : String(profile[key])
  })
}

const loadProfile = async () => {
  const { data } = await getProfile()
  fillForm(data || {})
}

const save = async () => {
  try {
    saving.value = true
    await updateProfile({
      ...form,
      height: form.height ? Number(form.height) : undefined,
      weight: form.weight ? Number(form.weight) : undefined
    })
    await store.fetchProfile()
    uni.showToast({ title: '资料已保存', icon: 'success' })
  } finally {
    saving.value = false
  }
}

onShow(() => {
  if (ensureLogin()) {
    loadProfile()
  }
})
</script>

<style scoped lang="scss">
.profile-form {
  display: flex;
  flex-direction: column;
  gap: 28rpx;
  margin-top: 24rpx;
  padding: 28rpx;
}
</style>
