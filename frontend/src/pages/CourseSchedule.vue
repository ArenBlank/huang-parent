<template>
  <div class="card">
    <div class="toolbar">
      <div>
        <h2>课程排期管理</h2>
        <p>统一管理课程的开课时间、授课教练与名额。开始和结束时间现在直接使用标准日期时间控件，不再手填格式。</p>
      </div>
      <el-button type="primary" @click="openCreateDrawer">新建排期</el-button>
    </div>

    <div class="summary-grid">
      <div class="summary-card">
        <div class="summary-label">排期总数</div>
        <div class="summary-value">{{ schedules.length }}</div>
        <div class="summary-sub">启用中 {{ activeCount }}</div>
      </div>
      <div class="summary-card">
        <div class="summary-label">最近一条</div>
        <div class="summary-value">{{ latestSchedule ? formatDateTime(latestSchedule.startTime) : '-' }}</div>
        <div class="summary-sub">课程 {{ latestSchedule?.courseId || '-' }}</div>
      </div>
      <div class="summary-card">
        <div class="summary-label">可选教练</div>
        <div class="summary-value">{{ coachOptions.length }}</div>
        <div class="summary-sub">来自教练档案名册</div>
      </div>
    </div>

    <el-table :data="schedules" style="width: 100%" v-loading="loading">
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="courseId" label="课程ID" width="100" />
      <el-table-column label="授课教练" min-width="180">
        <template #default="{ row }">
          <div class="coach-cell">
            <strong>{{ coachDisplayName(row.coachId) }}</strong>
            <span>档案ID {{ row.coachId }}</span>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="开始时间" min-width="180">
        <template #default="{ row }">
          {{ formatDateTime(row.startTime) }}
        </template>
      </el-table-column>
      <el-table-column label="结束时间" min-width="180">
        <template #default="{ row }">
          {{ formatDateTime(row.endTime) }}
        </template>
      </el-table-column>
      <el-table-column prop="capacity" label="容量" width="100" />
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'info'">
            {{ row.status === 1 ? '启用' : '停用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="160">
        <template #default="{ row }">
          <el-button size="small" type="warning" @click="toggleStatus(row)">
            {{ row.status === 1 ? '停用' : '启用' }}
          </el-button>
        </template>
      </el-table-column>
    </el-table>
  </div>

  <el-drawer v-model="drawerVisible" title="新建排期" size="34%" destroy-on-close>
    <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
      <el-form-item label="课程ID" prop="courseId">
        <el-input-number v-model="form.courseId" :min="1" controls-position="right" style="width: 100%" />
      </el-form-item>

      <el-form-item label="授课教练" prop="coachId">
        <el-select
          v-model="form.coachId"
          filterable
          clearable
          placeholder="请选择授课教练"
          style="width: 100%"
        >
          <el-option
            v-for="coach in coachOptions"
            :key="coach.coachId"
            :label="coachOptionLabel(coach)"
            :value="coach.coachId"
          />
        </el-select>
        <div class="field-help">这里用的是教练档案ID，不是角色管理里的角色ID。</div>
      </el-form-item>

      <el-form-item label="开始时间" prop="startTime">
        <el-date-picker
          v-model="form.startTime"
          type="datetime"
          value-format="YYYY-MM-DDTHH:mm:ss"
          placeholder="选择课程开始时间"
          style="width: 100%"
        />
        <div class="field-help">提交给后端的是标准 ISO 时间，例如 2026-04-24T18:00:00，不再要求手输格式。</div>
      </el-form-item>

      <el-form-item label="结束时间" prop="endTime">
        <el-date-picker
          v-model="form.endTime"
          type="datetime"
          value-format="YYYY-MM-DDTHH:mm:ss"
          placeholder="选择课程结束时间"
          style="width: 100%"
        />
      </el-form-item>

      <el-form-item label="容量" prop="capacity">
        <el-input-number v-model="form.capacity" :min="1" controls-position="right" style="width: 100%" />
      </el-form-item>

      <el-form-item label="状态" prop="status">
        <el-select v-model="form.status" style="width: 100%">
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
import { computed, nextTick, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { adminClient } from '../api/client'
import { listCoachScheduleCoachOptions } from '../api/coachSchedule'

const schedules = ref([])
const coachOptions = ref([])
const loading = ref(false)
const coachLoading = ref(false)
const creating = ref(false)
const drawerVisible = ref(false)
const formRef = ref(null)

const activeCount = computed(() => schedules.value.filter((item) => item.status === 1).length)
const latestSchedule = computed(() => (schedules.value.length ? schedules.value[0] : null))

const createDefaultForm = () => ({
  courseId: 108,
  coachId: null,
  startTime: '',
  endTime: '',
  capacity: 10,
  status: 1
})

const form = reactive(createDefaultForm())

const rules = {
  courseId: [{ required: true, message: '请填写课程ID', trigger: 'change' }],
  coachId: [{ required: true, message: '请选择授课教练', trigger: 'change' }],
  startTime: [{ required: true, message: '请选择开始时间', trigger: 'change' }],
  endTime: [
    { required: true, message: '请选择结束时间', trigger: 'change' },
    {
      validator: (_, value, callback) => {
        if (!value || !form.startTime) {
          callback()
          return
        }
        if (new Date(value).getTime() <= new Date(form.startTime).getTime()) {
          callback(new Error('结束时间必须晚于开始时间'))
          return
        }
        callback()
      },
      trigger: 'change'
    }
  ],
  capacity: [{ required: true, message: '请填写容量', trigger: 'change' }],
  status: [{ required: true, message: '请选择状态', trigger: 'change' }]
}

const coachOptionLabel = (coach) => {
  if (!coach) return '未命名教练'
  const name = coach.displayName || coach.nickname || coach.username || '教练'
  return `${name}（档案ID ${coach.coachId}）`
}

const coachDisplayName = (coachId) => {
  const coach = coachOptions.value.find((item) => Number(item.coachId) === Number(coachId))
  return coach ? coachOptionLabel(coach) : `教练档案 ${coachId || '-'}`
}

const formatDateTime = (value) => {
  if (!value) return '-'
  return String(value).replace('T', ' ')
}

const resetForm = () => {
  Object.assign(form, createDefaultForm())
}

const openCreateDrawer = async () => {
  resetForm()
  drawerVisible.value = true
  await nextTick()
  formRef.value?.clearValidate()
  if (!coachOptions.value.length) {
    await loadCoachOptions()
  }
}

const loadCoachOptions = async ({ silent = false } = {}) => {
  try {
    coachLoading.value = true
    const { data } = await listCoachScheduleCoachOptions()
    if (data.code !== 200) throw new Error(data.message || '加载教练名册失败')
    coachOptions.value = data.data || []
  } catch (error) {
    if (!silent) {
      ElMessage.error(error.message || '加载教练名册失败')
    }
  } finally {
    coachLoading.value = false
  }
}

const loadSchedules = async () => {
  try {
    loading.value = true
    const { data } = await adminClient.get('/admin/course/schedule/list')
    if (data.code !== 200) throw new Error(data.message || '加载失败')
    schedules.value = data.data || []
  } catch (error) {
    ElMessage.error(error.message || '加载失败')
  } finally {
    loading.value = false
  }
}

const createSchedule = async () => {
  if (!formRef.value) {
    return
  }
  try {
    await formRef.value.validate()
    creating.value = true
    const payload = {
      courseId: Number(form.courseId),
      coachId: Number(form.coachId),
      startTime: form.startTime,
      endTime: form.endTime,
      capacity: Number(form.capacity),
      status: Number(form.status)
    }
    const { data } = await adminClient.post('/admin/course/schedule', payload)
    if (data.code !== 200) throw new Error(data.message || '创建失败')
    ElMessage.success('排期已创建')
    drawerVisible.value = false
    await loadSchedules()
  } catch (error) {
    if (error?.message) {
      ElMessage.error(error.message || '创建失败')
    }
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
  } catch (error) {
    ElMessage.error(error.message || '更新失败')
  }
}

const fillSample = () => {
  form.courseId = 108
  form.coachId = coachOptions.value[0]?.coachId ?? null
  form.startTime = '2026-04-24T18:00:00'
  form.endTime = '2026-04-24T19:00:00'
  form.capacity = 10
  form.status = 1
}

onMounted(async () => {
  await loadSchedules()
  loadCoachOptions({ silent: true })
})
</script>

<style scoped>
.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  gap: 12px;
}

.toolbar h2 {
  margin: 0 0 6px;
}

.toolbar p {
  margin: 0;
  color: #8aa0af;
  font-size: 13px;
  line-height: 1.6;
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

.coach-cell {
  display: grid;
  gap: 4px;
}

.coach-cell strong {
  font-size: 14px;
}

.coach-cell span {
  color: #6b7280;
  font-size: 12px;
}

.field-help {
  margin-top: 6px;
  color: #6b7280;
  font-size: 12px;
  line-height: 1.5;
}

.form-actions {
  display: flex;
  gap: 8px;
}
</style>
