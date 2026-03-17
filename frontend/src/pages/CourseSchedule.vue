<template>
  <div class="card">
    <div class="toolbar">
      <div>
        <h2>课程排期</h2>
        <p>创建与查看排期</p>
      </div>
      <el-button type="primary" @click="drawerVisible = true">新建排期</el-button>
    </div>
    <div class="summary-grid">
      <div class="summary-card">
        <div class="summary-label">排期总数</div>
        <div class="summary-value">{{ schedules.length }}</div>
        <div class="summary-sub">启用 {{ activeCount }}</div>
      </div>
      <div class="summary-card">
        <div class="summary-label">最近排期</div>
        <div class="summary-value">{{ latestSchedule?.startTime || '-' }}</div>
        <div class="summary-sub">课程 {{ latestSchedule?.courseId || '-' }}</div>
      </div>
    </div>

    <el-table :data="schedules" style="width: 100%" v-loading="loading">
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="courseId" label="课程" width="120" />
      <el-table-column prop="coachId" label="教练" width="120" />
      <el-table-column prop="startTime" label="开始时间" />
      <el-table-column prop="endTime" label="结束时间" />
      <el-table-column prop="capacity" label="容量" width="120" />
      <el-table-column label="状态" width="90">
        <template #default="scope">
          <el-tag :type="scope.row.status === 1 ? 'success' : 'info'">
            {{ scope.row.status === 1 ? '启用' : '停用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="160">
        <template #default="scope">
          <el-button size="small" type="warning" @click="toggleStatus(scope.row)">
            {{ scope.row.status === 1 ? '停用' : '启用' }}
          </el-button>
        </template>
      </el-table-column>
    </el-table>
  </div>

  <el-drawer v-model="drawerVisible" title="新建排期" size="30%">
    <el-form :model="form" label-position="top">
      <el-form-item label="课程 ID">
        <el-input v-model.number="form.courseId" />
      </el-form-item>
      <el-form-item label="教练 ID">
        <el-input v-model.number="form.coachId" />
      </el-form-item>
      <el-form-item label="开始时间（yyyy-MM-dd HH:mm:ss）">
        <el-input v-model="form.startTime" />
      </el-form-item>
      <el-form-item label="结束时间（yyyy-MM-dd HH:mm:ss）">
        <el-input v-model="form.endTime" />
      </el-form-item>
      <el-form-item label="容量">
        <el-input v-model.number="form.capacity" />
      </el-form-item>
      <el-form-item label="状态">
        <el-select v-model.number="form.status">
          <el-option label="停用" :value="0" />
          <el-option label="启用" :value="1" />
        </el-select>
      </el-form-item>
      <div class="form-actions">
        <el-button @click="fillSample">填充示例</el-button>
        <el-button type="primary" @click="createSchedule" :loading="creating">提交</el-button>
      </div>
    </el-form>
  </el-drawer>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { adminClient } from '../api/client'

const schedules = ref([])
const loading = ref(false)
const creating = ref(false)
const drawerVisible = ref(false)

const activeCount = computed(() => schedules.value.filter((item) => item.status === 1).length)
const latestSchedule = computed(() => (schedules.value.length ? schedules.value[0] : null))

const form = reactive({
  courseId: 1,
  coachId: 1,
  startTime: '2026-04-01 18:00:00',
  endTime: '2026-04-01 19:00:00',
  capacity: 10,
  status: 1
})

const loadSchedules = async () => {
  try {
    loading.value = true
    const { data } = await adminClient.get('/admin/course/schedule/list')
    if (data.code !== 200) throw new Error(data.message || '加载失败')
    schedules.value = data.data || []
  } catch (err) {
    ElMessage.error(err.message || '加载失败')
  } finally {
    loading.value = false
  }
}

const createSchedule = async () => {
  try {
    creating.value = true
    const { data } = await adminClient.post('/admin/course/schedule', form)
    if (data.code !== 200) throw new Error(data.message || '创建失败')
    ElMessage.success('排期已创建')
    drawerVisible.value = false
    await loadSchedules()
  } catch (err) {
    ElMessage.error(err.message || '创建失败')
  } finally {
    creating.value = false
  }
}

const toggleStatus = async (row) => {
  try {
    const status = row.status === 1 ? 0 : 1
    const { data } = await adminClient.put(`/admin/course/schedule/${row.id}/status`, null, {
      params: { status }
    })
    if (data.code !== 200) throw new Error(data.message || '更新失败')
    ElMessage.success('状态已更新')
    await loadSchedules()
  } catch (err) {
    ElMessage.error(err.message || '更新失败')
  }
}

const fillSample = () => {
  form.courseId = 1
  form.coachId = 2
  form.startTime = '2026-04-01 18:00:00'
  form.endTime = '2026-04-01 19:00:00'
  form.capacity = 10
  form.status = 1
}

onMounted(loadSchedules)
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

.form-actions {
  display: flex;
  gap: 8px;
}
</style>
