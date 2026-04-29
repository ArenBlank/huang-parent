<template>
  <view class="sl-topbar">
    <button v-if="showBack" class="sl-topbar__back" @click="handleBack">‹</button>
    <view class="sl-topbar__copy">
      <text class="sl-topbar__eyebrow" v-if="eyebrow">{{ eyebrow }}</text>
      <text class="sl-topbar__title">{{ title }}</text>
      <text v-if="subtitle" class="sl-topbar__subtitle">{{ subtitle }}</text>
    </view>
    <slot name="action" />
  </view>
</template>

<script setup>
import { goBack } from '../utils/navigation'

defineProps({
  title: {
    type: String,
    required: true
  },
  subtitle: {
    type: String,
    default: ''
  },
  eyebrow: {
    type: String,
    default: ''
  },
  showBack: {
    type: Boolean,
    default: false
  }
})

const handleBack = () => {
  goBack()
}
</script>

<style scoped lang="scss">
.sl-topbar {
  display: flex;
  align-items: center;
  gap: 18rpx;
  min-height: 92rpx;
}

.sl-topbar__back {
  display: grid;
  width: 64rpx;
  height: 64rpx;
  place-items: center;
  border: 3rpx solid #34205f;
  border-radius: 50%;
  background: #fff;
  color: #24104f;
  font-size: 52rpx;
  font-weight: 700;
  box-shadow: 0 8rpx 0 rgba(52, 32, 95, 0.08);
}

.sl-topbar__copy {
  flex: 1;
  min-width: 0;
}

.sl-topbar__eyebrow {
  display: block;
  color: #8b63ff;
  font-size: 20rpx;
  font-weight: 900;
  letter-spacing: 2rpx;
  text-transform: uppercase;
}

.sl-topbar__title {
  display: block;
  color: #24104f;
  font-size: 42rpx;
  font-weight: 900;
  line-height: 1.15;
}

.sl-topbar__subtitle {
  display: block;
  margin-top: 8rpx;
  color: #7b6f98;
  font-size: 24rpx;
  line-height: 1.45;
}
</style>
