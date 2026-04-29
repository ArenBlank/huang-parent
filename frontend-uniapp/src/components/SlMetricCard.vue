<template>
  <view class="sl-metric sl-card" :class="`sl-metric--${tone}`">
    <text class="sl-metric__label">{{ label }}</text>
    <text class="sl-metric__value">{{ value }}</text>
    <text v-if="note" class="sl-metric__note">{{ note }}</text>
  </view>
</template>

<script setup>
defineProps({
  label: {
    type: String,
    required: true
  },
  value: {
    type: [String, Number],
    required: true
  },
  note: {
    type: String,
    default: ''
  },
  tone: {
    type: String,
    default: 'violet'
  }
})
</script>

<style scoped lang="scss">
.sl-metric {
  min-height: 168rpx;
  padding: 22rpx;
}

.sl-metric__label,
.sl-metric__note {
  display: block;
  color: #7b6f98;
  font-size: 22rpx;
  line-height: 1.4;
}

.sl-metric__value {
  display: block;
  margin-top: 8rpx;
  color: #24104f;
  font-size: 44rpx;
  font-weight: 900;
  line-height: 1.1;
}

.sl-metric__note {
  margin-top: 8rpx;
}

.sl-metric--warm {
  background: linear-gradient(180deg, #fff 0%, #fff3dd 100%);
}

.sl-metric--mint {
  background: linear-gradient(180deg, #fff 0%, #effff8 100%);
}
</style>
