<template>
  <div class="card">
    <div class="header">
      <div>
        <h2>运营概览</h2>
        <p>来自后端的关键运营数据</p>
      </div>
      <el-button type="primary" size="small" @click="loadSummary" :loading="loading">刷新</el-button>
    </div>
    <div class="stat-grid" v-if="cards.length">
      <StatCard v-for="item in cards" :key="item.label" :label="item.label" :value="item.value" :note="item.note" />
    </div>
    <el-empty v-else description="暂无概览数据" />
  </div>

  <div class="card" style="margin-top: 16px;">
    <h3>原始数据</h3>
    <pre class="payload">{{ detailText }}</pre>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { adminClient } from '../api/client'
import StatCard from '../components/StatCard.vue'

const summary = ref(null)
const loading = ref(false)
const detailText = computed(() => (summary.value ? JSON.stringify(summary.value, null, 2) : ''))

const labelMap = {
  totalUsers: '用户总数',
  activeUsers: '活跃用户',
  orderCount: '订单数',
  bookingCount: '预约数',
  courseCount: '课程数',
  bannerCount: 'Banner 数',
  noticeCount: '公告数',
  refundCount: '退款数',
  revenue: '累计营收'
}

const cards = computed(() => {
  if (!summary.value || typeof summary.value !== 'object') return []
  return Object.entries(summary.value).map(([key, value]) => ({
    label: labelMap[key] || key,
    value: value,
    note: Array.isArray(value) ? `${value.length} 条` : ''
  }))
})

const loadSummary = async () => {
  try {
    loading.value = true
    const { data } = await adminClient.get('/admin/dashboard/summary')
    if (data.code !== 200) {
      throw new Error(data.message || '加载概览失败')
    }
    summary.value = data.data
  } catch (err) {
    ElMessage.error(err.message || '加载概览失败')
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
  color: #365062;
  font-size: 12px;
  white-space: pre-wrap;
}
</style>
