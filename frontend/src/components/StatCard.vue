<template>
  <div class="stat-card">
    <div class="stat-label">{{ label }}</div>
    <div class="stat-value">{{ value }}</div>
    <div class="stat-note" v-if="note">{{ note }}</div>
  </div>
</template>

<script setup>
defineProps({
  label: { type: String, default: '' },
  value: { type: [String, Number], default: '-' },
  note: { type: String, default: '' }
})
</script>

<style scoped>
.stat-note {
  margin-top: 6px;
  font-size: 12px;
  color: #9bb0bd;
}
</style>
