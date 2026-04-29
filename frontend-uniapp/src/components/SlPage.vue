<template>
  <view class="sl-page">
    <slot />
  </view>
</template>

<style scoped lang="scss">
.sl-page {
  min-height: 100vh;
  padding: calc(var(--status-bar-height) + 24rpx) 28rpx 44rpx;
  background:
    radial-gradient(circle at 0% 0%, rgba(139, 99, 255, 0.22), transparent 34%),
    radial-gradient(circle at 100% 6%, rgba(255, 128, 111, 0.18), transparent 28%),
    linear-gradient(180deg, #f8ecff 0%, #fffaf4 46%, #fff0c8 100%);
}
</style>
