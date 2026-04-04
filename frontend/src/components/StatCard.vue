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
.stat-card {
  justify-content: space-between;
}

.stat-value {
  min-height: 28px;
}

.stat-note {
  color: var(--muted);
  font-size: 12px;
  line-height: 1.6;
}
</style>
