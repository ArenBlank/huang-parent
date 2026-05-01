<template>
  <button class="sl-primary-button" :class="{ 'is-disabled': disabled || loading }" :disabled="disabled || loading" @click="emitClick">
    <text>{{ loading ? loadingText : text }}</text>
  </button>
</template>

<script setup>
const props = defineProps({
  text: {
    type: String,
    required: true
  },
  loadingText: {
    type: String,
    default: '处理中...'
  },
  loading: {
    type: Boolean,
    default: false
  },
  disabled: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['click'])

const emitClick = () => {
  if (props.disabled || props.loading) return
  emit('click')
}
</script>

<style scoped lang="scss">
.sl-primary-button {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  min-height: 104rpx;
  border: 4rpx solid #24104f;
  border-radius: 30rpx;
  background: linear-gradient(135deg, #8b63ff 0%, #b985ff 48%, #ff806f 100%);
  box-shadow: 0 12rpx 0 rgba(52, 32, 95, 0.16);
  color: #fff;
  font-size: 32rpx;
  font-weight: 900;
  line-height: 1;
  box-sizing: border-box;
}

.sl-primary-button text {
  line-height: 1;
}

.sl-primary-button.is-disabled {
  opacity: 0.68;
}
</style>
