<template>
  <SlPage>
    <SlTopBar title="账户安全" eyebrow="SECURITY" show-back subtitle="修改密码和后续安全能力会集中在这里。" />

    <view class="account-card sl-card">
      <view class="avatar-wrap">
        <image v-if="avatarSrc" class="avatar-img" :src="avatarSrc" mode="aspectFill" />
        <text v-else class="avatar-text">{{ avatarFallback }}</text>
      </view>
      <view class="account-main">
        <text class="account-name">{{ displayName }}</text>
        <text class="account-meta">{{ accountMeta }}</text>
        <view class="security-pill">
          <image :src="icons.shield" mode="aspectFit" />
          <text>{{ profile?.profileCompleted ? '资料已完善' : '建议完善资料' }}</text>
        </view>
      </view>
    </view>

    <view class="security-card sl-card">
      <text class="sl-section-title">更新密码</text>
      <text class="sl-section-sub">请输入旧密码并设置新密码，保存后下次登录会使用新密码。</text>
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
import { faShieldHalved } from '@fortawesome/free-solid-svg-icons'
import { computed, reactive, ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import SlPage from '../../components/SlPage.vue'
import SlPrimaryButton from '../../components/SlPrimaryButton.vue'
import SlTopBar from '../../components/SlTopBar.vue'
import { updatePassword } from '../../api/modules/profile'
import { ensureLogin } from '../../utils/authGuard'
import { toPublicUrl } from '../../utils/mediaUrl'
import { useAppAuthStore } from '../../stores/auth'

const faIcon = (definition, color = '#8b63ff') => {
  const [width, height, , , pathData] = definition.icon
  const paths = Array.isArray(pathData)
    ? pathData.map((path) => `<path fill="${color}" d="${path}"/>`).join('')
    : `<path fill="${color}" d="${pathData}"/>`
  const svg = `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 ${width} ${height}">${paths}</svg>`
  return `data:image/svg+xml;charset=utf-8,${encodeURIComponent(svg)}`
}

const icons = {
  shield: faIcon(faShieldHalved)
}

const store = useAppAuthStore()
const submitting = ref(false)
const profile = computed(() => store.user || null)
const appBase = (import.meta.env.VITE_APP_BASE_URL || '').replace(/\/app\/?$/, '').replace(/\/$/, '')

const form = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

const displayName = computed(() => profile.value?.nickname || profile.value?.username || '训练用户')
const accountMeta = computed(() => {
  const phone = profile.value?.phone ? `手机号 ${profile.value.phone}` : ''
  const email = profile.value?.email ? `邮箱 ${profile.value.email}` : ''
  return phone || email || '登录资料未完善'
})
const avatarSrc = computed(() => toPublicUrl(profile.value?.avatar || profile.value?.avatarUrl))
const avatarFallback = computed(() => String(displayName.value || 'U').slice(0, 1).toUpperCase())

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
    form.oldPassword = ''
    form.newPassword = ''
    form.confirmPassword = ''
    uni.showToast({ title: '密码已更新', icon: 'success' })
  } finally {
    submitting.value = false
  }
}

onShow(() => {
  if (ensureLogin()) {
    store.fetchProfile()
  }
})
</script>

<style scoped lang="scss">
.account-card {
  display: grid;
  grid-template-columns: 116rpx minmax(0, 1fr);
  gap: 22rpx;
  align-items: center;
  margin-top: 24rpx;
  padding: 24rpx;
  border-color: rgba(52, 32, 95, 0.22);
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.96), rgba(244, 236, 255, 0.94));
}

.avatar-wrap {
  display: grid;
  width: 108rpx;
  height: 108rpx;
  overflow: hidden;
  place-items: center;
  border: 4rpx solid #24104f;
  border-radius: 50%;
  background: linear-gradient(135deg, #fff0c8, #eee6ff);
  box-shadow: 0 10rpx 22rpx rgba(52, 32, 95, 0.12);
}

.avatar-img {
  width: 100%;
  height: 100%;
}

.avatar-text {
  color: #24104f;
  font-size: 42rpx;
  font-weight: 950;
}

.account-main {
  min-width: 0;
}

.account-name,
.account-meta {
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.account-name {
  color: #24104f;
  font-size: 34rpx;
  font-weight: 950;
}

.account-meta {
  margin-top: 8rpx;
  color: #7b6f98;
  font-size: 23rpx;
  font-weight: 800;
}

.security-pill {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8rpx;
  min-height: 44rpx;
  margin-top: 14rpx;
  padding: 0 16rpx;
  border-radius: 999rpx;
  background: #f0e7ff;
  color: #8b63ff;
  font-size: 22rpx;
  font-weight: 900;
  line-height: 1;
}

.security-pill image {
  width: 22rpx;
  height: 22rpx;
}

.security-card {
  margin-top: 24rpx;
  padding: 28rpx;
}

.form-space {
  margin: 24rpx 0;
}
</style>
