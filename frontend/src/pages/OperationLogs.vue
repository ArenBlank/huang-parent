<template>
  <div class="card">
    <div class="toolbar">
      <div>
        <h2>操作日志</h2>
        <p>关键操作的审计轨迹</p>
      </div>
      <el-button type="primary" @click="loadLogs" :loading="loading">刷新</el-button>
    </div>

    <div class="summary-grid">
      <div class="summary-card">
        <div class="summary-label">日志条数</div>
        <div class="summary-value">{{ logs.length }}</div>
        <div class="summary-sub">最近 {{ query.limit || 50 }} 条</div>
      </div>
      <div class="summary-card">
        <div class="summary-label">成功率</div>
        <div class="summary-value">{{ successRate }}</div>
        <div class="summary-sub">成功 {{ successCount }}</div>
      </div>
      <div class="summary-card">
        <div class="summary-label">最近模块</div>
        <div class="summary-value">{{ latestModule || '-' }}</div>
        <div class="summary-sub">最新一条日志</div>
      </div>
    </div>

    <div class="filters">
      <el-input v-model="query.module" placeholder="模块 (如 banner)" style="width: 180px" />
      <el-input v-model="query.action" placeholder="动作 (如 create)" style="width: 180px" />
      <el-input v-model.number="query.operatorId" placeholder="操作者ID" style="width: 140px" />
      <el-select v-model="query.success" placeholder="是否成功" clearable style="width: 140px">
        <el-option label="成功" :value="1" />
        <el-option label="失败" :value="0" />
      </el-select>
      <el-input v-model.number="query.limit" placeholder="条数" style="width: 110px" />
      <el-button size="small" @click="loadLogs" :loading="loading">查询</el-button>
      <el-button size="small" @click="fillQuerySample">示例筛选</el-button>
    </div>

    <el-table :data="logs" style="width: 100%" v-loading="loading">
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="module" label="模块" width="140" />
      <el-table-column prop="action" label="动作" width="140" />
      <el-table-column prop="detail" label="详情" />
      <el-table-column prop="operatorId" label="操作者" width="110" />
      <el-table-column prop="success" label="结果" width="90">
        <template #default="scope">
          <el-tag :type="scope.row.success === 1 ? 'success' : 'danger'">
            {{ scope.row.success === 1 ? '成功' : '失败' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="时间" width="180" />
    </el-table>
  </div>
</template>

<script setup>
import { computed, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { adminClient } from '../api/client'

const logs = ref([])
const loading = ref(false)
const query = reactive({
  module: '',
  action: '',
  operatorId: null,
  success: null,
  limit: 50
})

const successCount = computed(() => logs.value.filter((item) => item.success === 1).length)
const successRate = computed(() => {
  if (!logs.value.length) return '0%'
  return `${Math.round((successCount.value / logs.value.length) * 100)}%`
})
const latestModule = computed(() => logs.value[0]?.module || '')

const loadLogs = async () => {
  try {
    loading.value = true
    const { data } = await adminClient.get('/admin/operation-log/list', {
      params: {
        module: query.module || undefined,
        action: query.action || undefined,
        operatorId: query.operatorId || undefined,
        success: query.success ?? undefined,
        limit: query.limit || 50
      }
    })
    if (data.code !== 200) throw new Error(data.message || '加载失败')
    logs.value = data.data || []
  } catch (err) {
    ElMessage.error(err.message || '加载失败')
  } finally {
    loading.value = false
  }
}

const fillQuerySample = () => {
  query.module = 'role_permission'
  query.action = ''
  query.operatorId = null
  query.success = 1
  query.limit = 20
}

loadLogs()
</script>

<style scoped>
.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.toolbar h2 {
  margin: 0 0 6px;
}

.toolbar p {
  margin: 0;
  color: #8aa0af;
  font-size: 13px;
}

.filters {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 12px;
}

.summary-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 12px;
  margin-bottom: 16px;
}

.summary-card {
  padding: 12px;
  border-radius: 14px;
  border: 1px solid var(--border);
  background: #ffffffcc;
}

.summary-label {
  font-size: 12px;
  color: #8aa0af;
}

.summary-value {
  font-size: 22px;
  font-weight: 700;
  margin: 6px 0;
}

.summary-sub {
  font-size: 12px;
  color: #6b7280;
}
</style>
