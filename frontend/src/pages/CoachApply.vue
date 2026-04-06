<template>
  <div class="card">
    <div class="page-heading">
      <div>
        <div class="eyebrow">Coach Hub</div>
        <h2>教练申请</h2>
        <p>把申请审核也放到同一套可点、可扫读、可浮动反馈的控制台里。</p>
      </div>
      <div class="toolbar-actions">
        <span class="code-pill">APPLY · REVIEW · APPROVE</span>
        <el-button type="primary" @click="loadApplies" :loading="loading">刷新</el-button>
      </div>
    </div>

    <div class="metric-grid coach-metrics">
      <div class="metric-card">
        <div class="metric-label">申请总数</div>
        <div class="metric-value">{{ applies.length }}</div>
        <div class="metric-note">待审核 {{ pendingCount }}</div>
      </div>
      <div class="metric-card">
        <div class="metric-label">已通过</div>
        <div class="metric-value">{{ approvedCount }}</div>
        <div class="metric-note">已驳回 {{ rejectedCount }}</div>
      </div>
      <div class="metric-card">
        <div class="metric-label">最近申请</div>
        <div class="metric-value metric-value--latest">{{ latestApply?.nickname || latestApply?.username || '-' }}</div>
        <div class="metric-note">更新时间 {{ latestApply?.updateTime || '-' }}</div>
      </div>
    </div>

    <div class="filters">
      <el-select v-model="query.certStatus" placeholder="审核状态" clearable style="width: 160px">
        <el-option label="待审核" :value="0" />
        <el-option label="已通过" :value="1" />
        <el-option label="已驳回" :value="2" />
      </el-select>
      <el-input v-model="query.keyword" placeholder="姓名/手机号/账号" style="width: 220px" />
      <el-button size="small" @click="loadApplies" :loading="loading">查询</el-button>
      <el-button size="small" @click="fillSampleQuery">示例筛选</el-button>
    </div>

    <div class="table-shell">
      <el-table :data="applies" style="width: 100%" v-loading="loading">
      <el-table-column prop="profileId" label="档案ID" width="90" />
      <el-table-column prop="username" label="账号" width="120" />
      <el-table-column prop="nickname" label="昵称" min-width="140" show-overflow-tooltip />
      <el-table-column prop="phone" label="手机" width="140" />
      <el-table-column label="审核状态" width="140" align="center" header-align="center">
        <template #default="scope">
          <el-tag :type="scope.row.certStatus === 1 ? 'success' : scope.row.certStatus === 2 ? 'danger' : 'warning'">
            {{ scope.row.certStatusText || formatCertStatus(scope.row.certStatus) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="years" label="年限" width="90" />
      <el-table-column prop="price" label="课时价" width="100" />
      <el-table-column prop="updateTime" label="更新时间" width="190" />
      <el-table-column label="操作" width="320" align="center" header-align="center">
        <template #default="scope">
          <div class="table-action-row">
            <el-button size="small" @click="openDetail(scope.row)">详情</el-button>
            <el-button
              size="small"
              type="success"
              :disabled="scope.row.certStatus !== 0"
              @click="audit(scope.row, 1)"
            >
              通过
            </el-button>
            <el-button
              size="small"
              type="warning"
              :disabled="scope.row.certStatus !== 0"
              @click="audit(scope.row, 2)"
            >
              驳回
            </el-button>
          </div>
        </template>
      </el-table-column>
    </el-table>
    </div>
  </div>

  <el-drawer v-model="detailVisible" title="申请详情" size="36%">
    <el-descriptions :column="1" border>
      <el-descriptions-item label="账号">{{ detail?.username }}</el-descriptions-item>
      <el-descriptions-item label="昵称">{{ detail?.nickname }}</el-descriptions-item>
      <el-descriptions-item label="手机">{{ detail?.phone }}</el-descriptions-item>
      <el-descriptions-item label="擅长">{{ detail?.expertise }}</el-descriptions-item>
      <el-descriptions-item label="简介">{{ detail?.bio }}</el-descriptions-item>
      <el-descriptions-item label="年限">{{ detail?.years }}</el-descriptions-item>
      <el-descriptions-item label="课时价">{{ detail?.price }}</el-descriptions-item>
      <el-descriptions-item label="审核状态">{{ detail?.certStatusText }}</el-descriptions-item>
      <el-descriptions-item label="更新时间">{{ detail?.updateTime }}</el-descriptions-item>
    </el-descriptions>
    <div class="drawer-actions">
      <el-button type="success" :disabled="detail?.certStatus !== 0" @click="audit(detail, 1)">通过</el-button>
      <el-button type="warning" :disabled="detail?.certStatus !== 0" @click="audit(detail, 2)">驳回</el-button>
    </div>
  </el-drawer>
</template>

<script setup>
import { computed, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { adminClient } from '../api/client'

const applies = ref([])
const detail = ref(null)
const detailVisible = ref(false)
const loading = ref(false)

const query = reactive({
  certStatus: null,
  keyword: ''
})

const pendingCount = computed(() => applies.value.filter((item) => item.certStatus === 0).length)
const approvedCount = computed(() => applies.value.filter((item) => item.certStatus === 1).length)
const rejectedCount = computed(() => applies.value.filter((item) => item.certStatus === 2).length)
const latestApply = computed(() => (applies.value.length ? applies.value[0] : null))

const loadApplies = async () => {
  try {
    loading.value = true
    const { data } = await adminClient.get('/admin/coach-apply/list', {
      params: {
        certStatus: query.certStatus ?? undefined,
        keyword: query.keyword || undefined
      }
    })
    if (data.code !== 200) throw new Error(data.message || '加载失败')
    applies.value = data.data || []
  } catch (err) {
    ElMessage.error(err.message || '加载失败')
  } finally {
    loading.value = false
  }
}

const fillSampleQuery = () => {
  query.certStatus = 0
  query.keyword = ''
  loadApplies()
}

const formatCertStatus = (status) => {
  if (status === 1) return '已通过'
  if (status === 2) return '已驳回'
  return '待审核'
}

const openDetail = async (row) => {
  try {
    const { data } = await adminClient.get(`/admin/coach-apply/${row.profileId}`)
    if (data.code !== 200) throw new Error(data.message || '加载失败')
    detail.value = data.data
    detailVisible.value = true
  } catch (err) {
    ElMessage.error(err.message || '加载失败')
  }
}

const audit = async (row, certStatus) => {
  try {
    const label = certStatus === 1 ? '通过' : '驳回'
    await ElMessageBox.confirm(`确认${label}该申请吗？`, '提示', { type: 'warning' })
    const { data } = await adminClient.post('/admin/coach-apply/audit', {
      profileId: row.profileId,
      certStatus
    })
    if (data.code !== 200) throw new Error(data.message || '审核失败')
    ElMessage.success('已提交审核')
    await loadApplies()
  } catch (err) {
    if (err !== 'cancel' && err !== 'close' && err?.message) {
      ElMessage.error(err.message || '审核失败')
    }
  }
}

loadApplies()
</script>

<style scoped>
.coach-metrics .metric-card {
  justify-content: space-between;
  min-height: 180px;
}

.coach-metrics .metric-note {
  margin-top: auto;
}

.metric-value--latest {
  font-size: clamp(24px, 2.2vw, 40px);
  line-height: 1.15;
  word-break: break-word;
}

.filters {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;
  margin-bottom: 16px;
}

.drawer-actions {
  margin-top: 18px;
  display: flex;
  gap: 8px;
  flex-wrap: nowrap;
  justify-content: flex-end;
}

.table-action-row {
  justify-content: center;
  gap: 12px;
}

:deep(.el-table .cell) {
  line-height: 1.6;
}

:deep(.el-table .cell) {
  color: var(--text);
}

@media (max-width: 900px) {
  .drawer-actions {
    flex-wrap: wrap;
    justify-content: flex-start;
  }
}
</style>
