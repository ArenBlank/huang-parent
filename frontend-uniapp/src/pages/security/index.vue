<template>
  <SlPage>
    <SlTopBar title="账户安全" eyebrow="SECURITY" show-back subtitle="修改密码和后续安全能力会集中在这里。" />
    <view class="security-card sl-card">
      <text class="sl-section-title">更新密码</text>
      <view class="sl-form form-space">
        <input class="sl-input" v-model="form.oldPassword" password placeholder="旧密码" />
        <input class="sl-input" v-model="form.newPassword" password placeholder="新密码" />
        <input class="sl-input" v-model="form.confirmPassword" password placeholder="确认新密码" />
      </view>
      <SlPrimaryButton text="更新密码" :loading="submitting" @click="submit" />
    </view>
  </SlPage>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import SlPage from '../../components/SlPage.vue'
import SlPrimaryButton from '../../components/SlPrimaryButton.vue'
import SlTopBar from '../../components/SlTopBar.vue'
import { updatePassword } from '../../api/modules/profile'
import { ensureLogin } from '../../utils/authGuard'

const submitting = ref(false)
const form = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

const submit = async () => {
  if (!form.oldPassword || !form.newPassword || !form.confirmPassword) {
    uni.showToast({ title: '请填写完整密码信息', icon: 'none' })
    return
  }
  if (form.newPassword !== form.confirmPassword) {
    uni.showToast({ title: '两次新密码不一致', icon: 'none' })
    return
  }
  try {
    submitting.value = true
    await updatePassword(form)
    uni.showToast({ title: '密码已更新', icon: 'success' })
  } finally {
    submitting.value = false
  }
}

onShow(() => {
  ensureLogin()
})
</script>

<style scoped lang="scss">
.security-card {
  margin-top: 24rpx;
  padding: 28rpx;
}

.form-space {
  margin: 24rpx 0;
}
</style>
