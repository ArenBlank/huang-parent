<template>
  <view class="sl-empty sl-card">
    <text class="sl-empty__title">{{ title }}</text>
    <text class="sl-empty__desc">{{ description }}</text>
    <button v-if="actionText" class="sl-empty__action" @click="$emit('action')">{{ actionText }}</button>
  </view>
</template>

<script setup>
defineProps({
  title: {
    type: String,
    default: '暂无数据'
  },
  description: {
    type: String,
    default: '刷新后再看看。'
  },
  actionText: {
    type: String,
    default: ''
  }
})

defineEmits(['action'])
</script>

<style scoped lang="scss">
.sl-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 14rpx;
  padding: 48rpx 30rpx;
  text-align: center;
}

.sl-empty__title {
  color: #24104f;
  font-size: 32rpx;
  font-weight: 900;
}

.sl-empty__desc {
  color: #7b6f98;
  font-size: 24rpx;
  line-height: 1.45;
}

.sl-empty__action {
  min-height: 64rpx;
  margin-top: 10rpx;
  padding: 0 28rpx;
  border: 2rpx solid #34205f;
  border-radius: 999rpx;
  background: #fff8ea;
  color: #24104f;
  font-size: 24rpx;
  font-weight: 800;
}
</style>
