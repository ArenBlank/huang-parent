<template>
  <div class="card">
    <div class="header">
      <div>
        <h2>Live Summary</h2>
        <p>Key operational numbers from backend</p>
      </div>
      <el-button type="primary" size="small" @click="loadSummary" :loading="loading">Refresh</el-button>
    </div>
    <div class="stat-grid" v-if="cards.length">
      <StatCard v-for="item in cards" :key="item.label" :label="item.label" :value="item.value" :note="item.note" />
    </div>
    <el-empty v-else description="No summary data" />
  </div>

  <div class="card" style="margin-top: 16px;">
    <h3>Raw Payload</h3>
    <pre class="payload">{{ summary }}</pre>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { adminClient } from '../api/client'
import StatCard from '../components/StatCard.vue'

const summary = ref(null)
const loading = ref(false)

const cards = computed(() => {
  if (!summary.value || typeof summary.value !== 'object') return []
  return Object.entries(summary.value).map(([key, value]) => ({
    label: key,
    value: value,
    note: Array.isArray(value) ? `${value.length} items` : ''
  }))
})

const loadSummary = async () => {
  try {
    loading.value = true
    const { data } = await adminClient.get('/admin/dashboard/summary')
    if (data.code !== 200) {
      throw new Error(data.message || 'Failed to load summary')
    }
    summary.value = data.data
  } catch (err) {
    ElMessage.error(err.message || 'Failed to load summary')
  } finally {
    loading.value = false
  }
}

onMounted(loadSummary)
</script>

<style scoped>
.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.header h2 {
  margin: 0 0 6px;
}

.header p {
  margin: 0;
  color: #8aa0af;
  font-size: 13px;
}

.payload {
  color: #bcd1dd;
  font-size: 12px;
  white-space: pre-wrap;
}
</style>
