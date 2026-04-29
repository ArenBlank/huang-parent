<template>
  <SlPage>
    <SlTopBar title="我的" eyebrow="PROFILE" subtitle="资料、订单、预约和账户安全集中在这里。" />

    <view class="profile-card sl-card">
      <view class="avatar">{{ avatarFallback }}</view>
      <view class="profile-card__copy">
        <text class="profile-card__name">{{ store.displayName }}</text>
        <text class="profile-card__meta">{{ store.user?.phone || '未填写手机号' }}</text>
      </view>
    </view>

    <view class="sl-section sl-list">
      <button v-for="item in menu" :key="item.url" class="menu-card sl-card" @click="goPage(item.url)">
        <text class="menu-card__title">{{ item.title }}</text>
        <text class="menu-card__desc">{{ item.desc }}</text>
      </button>
    </view>

    <button class="logout-button" @click="logout">退出登录</button>
  </SlPage>
</template>

<script setup>
import { computed } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import SlPage from '../../components/SlPage.vue'
import SlTopBar from '../../components/SlTopBar.vue'
import { ensureLogin } from '../../utils/authGuard'
import { goPage } from '../../utils/navigation'
import { useAppAuthStore } from '../../stores/auth'

const store = useAppAuthStore()
const avatarFallback = computed(() => store.displayName.slice(0, 1).toUpperCase())

const menu = [
  { title: '编辑资料', desc: '昵称、联系方式、身体参数', url: '/pages/profile-edit/index' },
  { title: '订单中心', desc: '支付、退款和订单详情', url: '/pages/orders/index' },
  { title: '我的预约', desc: '教练档期和评价', url: '/pages/booking/index' },
  { title: '教练申请', desc: '认证状态和申请资料', url: '/pages/coach-apply/index' },
  { title: '账户安全', desc: '修改密码', url: '/pages/security/index' }
]

const logout = () => {
  store.logout()
  uni.redirectTo({ url: '/pages/login/index' })
}

onShow(() => {
  if (ensureLogin()) {
    store.fetchProfile()
  }
})
</script>

<style scoped lang="scss">
.profile-card {
  display: flex;
  align-items: center;
  gap: 20rpx;
  margin-top: 24rpx;
  padding: 28rpx;
}

.avatar {
  display: grid;
  width: 112rpx;
  height: 112rpx;
  place-items: center;
  border-radius: 50%;
  background: #24104f;
  color: #fff;
  font-size: 46rpx;
  font-weight: 900;
}

.profile-card__name {
  display: block;
  color: #24104f;
  font-size: 38rpx;
  font-weight: 900;
}

.profile-card__meta {
  display: block;
  margin-top: 10rpx;
  color: #7b6f98;
  font-size: 24rpx;
}

.menu-card {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  padding: 24rpx;
  text-align: left;
}

.menu-card__title {
  font-size: 30rpx;
  font-weight: 900;
}

.menu-card__desc {
  margin-top: 8rpx;
  color: #7b6f98;
  font-size: 24rpx;
}

.logout-button {
  width: 100%;
  min-height: 88rpx;
  margin-top: 30rpx;
  border: 3rpx solid #34205f;
  border-radius: 999rpx;
  background: #ffd6e1;
  color: #24104f;
  font-size: 28rpx;
  font-weight: 900;
}
</style>
