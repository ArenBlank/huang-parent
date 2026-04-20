<template>
  <div class="card">
    <div class="toolbar">
      <div>
        <h2>教练档期</h2>
        <p>这里管理的是教练可预约时间段，处理具体预约单请前往预约大厅。</p>
      </div>
      <div class="toolbar-actions">
        <el-button @click="goBookingOps">前往预约大厅</el-button>
        <el-button type="primary" @click="openCreateDrawer">新建可预约时间段</el-button>
      </div>
    </div>

    <div class="summary-grid">
      <div class="summary-card">
        <div class="summary-label">档期总数</div>
        <div class="summary-value">{{ tableData.length }}</div>
        <div class="summary-sub">当前筛选结果</div>
      </div>
      <div class="summary-card">
        <div class="summary-label">启用中</div>
        <div class="summary-value">{{ enabledCount }}</div>
        <div class="summary-sub">当前可见档期</div>
      </div>
      <div class="summary-card">
        <div class="summary-label">已有预约</div>
        <div class="summary-value">{{ bookedCount }}</div>
        <div class="summary-sub">不可删除档期</div>
      </div>
      <div class="summary-card">
        <div class="summary-label">未来可用</div>
        <div class="summary-value">{{ futureAvailableCount }}</div>
        <div class="summary-sub">启用且仍有余量</div>
      </div>
    </div>

    <div class="filter-panel">
      <el-form :inline="true" class="filter-form">
        <el-form-item label="教练 ID">
          <el-input
            v-model="queryForm.coachId"
            placeholder="输入教练档案 ID"
            clearable
            style="width: 180px"
          />
        </el-form-item>
        <el-form-item label="日期">
          <el-date-picker
            v-model="queryForm.scheduleDate"
            type="date"
            value-format="YYYY-MM-DD"
            placeholder="选择日期"
            clearable
            style="width: 180px"
          />
        </el-form-item>
        <el-form-item label="状态">
          <el-select
            v-model="queryForm.status"
            placeholder="全部状态"
            clearable
            style="width: 160px"
          >
            <el-option label="启用" :value="1" />
            <el-option label="停用" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="loadSchedules" :loading="loading">查询</el-button>
          <el-button @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>
    </div>

    <el-table :data="tableData" style="width: 100%" v-loading="loading">
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="coachId" label="教练ID" width="100" />
      <el-table-column prop="scheduleDate" label="日期" width="140" />
      <el-table-column label="开始时间" width="140">
        <template #default="{ row }">
          {{ formatTime(row.startTime) }}
        </template>
      </el-table-column>
      <el-table-column label="结束时间" width="140">
        <template #default="{ row }">
          {{ formatTime(row.endTime) }}
        </template>
      </el-table-column>
      <el-table-column prop="price" label="价格" width="120" />
      <el-table-column prop="capacity" label="容量" width="100" />
      <el-table-column prop="bookedCount" label="已预约人数" width="120" />
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'info'">
            {{ row.status === 1 ? '启用' : '停用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" min-width="260">
        <template #default="{ row }">
          <div class="table-actions">
            <el-button size="small" @click="openEditDrawer(row)">编辑</el-button>
            <el-button
              size="small"
              :type="row.status === 1 ? 'warning' : 'success'"
              :loading="statusLoadingMap[row.id] === true"
              @click="toggleStatus(row)"
            >
              {{ row.status === 1 ? '停用' : '启用' }}
            </el-button>
            <el-tooltip
              v-if="isBookedRow(row)"
              content="已有预约记录的档期不可删除"
              placement="top"
            >
              <span class="tooltip-button">
                <el-button size="small" type="danger" disabled>删除</el-button>
              </span>
            </el-tooltip>
            <el-button
              v-else
              size="small"
              type="danger"
              :loading="deleteLoadingMap[row.id] === true"
              @click="removeSchedule(row)"
            >
              删除
            </el-button>
          </div>
        </template>
      </el-table-column>
    </el-table>
  </div>

  <el-drawer
    v-model="drawerVisible"
    :title="drawerMode === 'create' ? '新建可预约时间段' : '编辑教练档期'"
    size="36%"
    destroy-on-close
  >
    <el-alert
      v-if="isReadonlyEdit"
      type="warning"
      :closable="false"
      show-icon
      title="该档期已有预约，核心字段不可编辑；启停请使用列表中的状态按钮。"
      class="drawer-alert"
    />

    <el-form
      ref="formRef"
      :model="form"
      :rules="rules"
      label-position="top"
      class="schedule-form"
    >
      <el-form-item label="教练 ID" prop="coachId">
        <el-input-number
          v-model="form.coachId"
          :min="1"
          :disabled="isReadonlyEdit"
          controls-position="right"
          style="width: 100%"
        />
      </el-form-item>

      <el-form-item label="日期" prop="scheduleDate">
        <el-date-picker
          v-model="form.scheduleDate"
          type="date"
          value-format="YYYY-MM-DD"
          placeholder="选择日期"
          :disabled="isReadonlyEdit"
          style="width: 100%"
        />
      </el-form-item>

      <div class="time-grid">
        <el-form-item label="开始时间" prop="startTime">
          <el-time-picker
            v-model="form.startTime"
            value-format="HH:mm:ss"
            placeholder="选择开始时间"
            :disabled="isReadonlyEdit"
            style="width: 100%"
          />
        </el-form-item>

        <el-form-item label="结束时间" prop="endTime">
          <el-time-picker
            v-model="form.endTime"
            value-format="HH:mm:ss"
            placeholder="选择结束时间"
            :disabled="isReadonlyEdit"
            style="width: 100%"
          />
        </el-form-item>
      </div>

      <div class="time-grid">
        <el-form-item label="价格" prop="price">
          <el-input-number
            v-model="form.price"
            :min="0"
            :precision="2"
            :disabled="isReadonlyEdit"
            controls-position="right"
            style="width: 100%"
          />
        </el-form-item>

        <el-form-item label="容量" prop="capacity">
          <el-input-number
            v-model="form.capacity"
            :min="1"
            :disabled="isReadonlyEdit"
            controls-position="right"
            style="width: 100%"
          />
        </el-form-item>
      </div>

      <el-form-item label="状态" prop="status">
        <el-select
          v-model="form.status"
          placeholder="选择状态"
          :disabled="isReadonlyEdit"
          style="width: 100%"
        >
          <el-option label="启用" :value="1" />
          <el-option label="停用" :value="0" />
        </el-select>
      </el-form-item>
    </el-form>

    <template #footer>
      <div class="drawer-actions">
        <el-button @click="drawerVisible = false">取消</el-button>
        <el-button
          type="primary"
          :loading="submitting"
          :disabled="isReadonlyEdit"
          @click="submitForm"
        >
          {{ drawerMode === 'create' ? '创建档期' : '保存修改' }}
        </el-button>
      </div>
    </template>
  </el-drawer>
</template>

<script setup>
import { computed, nextTick, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { useRouter } from 'vue-router'
import {
  createCoachSchedule,
  deleteCoachSchedule,
  listCoachSchedules,
  updateCoachSchedule,
  updateCoachScheduleStatus
} from '../api/coachSchedule'

const router = useRouter()
const loading = ref(false)
const submitting = ref(false)
const drawerVisible = ref(false)
const drawerMode = ref('create')
const formRef = ref(null)
const tableData = ref([])
const currentRow = ref(null)
const statusLoadingMap = reactive({})
const deleteLoadingMap = reactive({})

const queryForm = reactive({
  coachId: '',
  scheduleDate: '',
  status: undefined
})

const createDefaultForm = () => ({
  coachId: null,
  scheduleDate: '',
  startTime: '',
  endTime: '',
  price: 0,
  capacity: 1,
  status: 1
})

const form = reactive(createDefaultForm())

const enabledCount = computed(() => tableData.value.filter((item) => item.status === 1).length)
const bookedCount = computed(() => tableData.value.filter((item) => Number(item.bookedCount || 0) > 0).length)
const futureAvailableCount = computed(() => {
  const today = new Date().toISOString().slice(0, 10)
  return tableData.value.filter((item) => {
    const booked = Number(item.bookedCount || 0)
    const capacity = Number(item.capacity || 0)
    return item.status === 1 && item.scheduleDate >= today && capacity - booked > 0
  }).length
})
const isReadonlyEdit = computed(
  () => drawerMode.value === 'edit' && Number(currentRow.value?.bookedCount || 0) > 0
)

const rules = {
  coachId: [{ required: true, message: '请输入教练 ID', trigger: 'change' }],
  scheduleDate: [{ required: true, message: '请选择日期', trigger: 'change' }],
  startTime: [{ required: true, message: '请选择开始时间', trigger: 'change' }],
  endTime: [
    { required: true, message: '请选择结束时间', trigger: 'change' },
    {
      validator: (_, value, callback) => {
        if (!value || !form.startTime) {
          callback()
          return
        }
        if (parseTimeToSeconds(value) <= parseTimeToSeconds(form.startTime)) {
          callback(new Error('结束时间必须晚于开始时间'))
          return
        }
        callback()
      },
      trigger: 'change'
    }
  ],
  price: [
    { required: true, message: '请输入价格', trigger: 'change' },
    {
      validator: (_, value, callback) => {
        if (value === null || value === undefined || value < 0) {
          callback(new Error('价格不能小于 0'))
          return
        }
        callback()
      },
      trigger: 'change'
    }
  ],
  capacity: [
    { required: true, message: '请输入容量', trigger: 'change' },
    {
      validator: (_, value, callback) => {
        if (value === null || value === undefined || value <= 0) {
          callback(new Error('容量必须大于 0'))
          return
        }
        callback()
      },
      trigger: 'change'
    }
  ],
  status: [{ required: true, message: '请选择状态', trigger: 'change' }]
}

const buildQueryParams = () => ({
  coachId: queryForm.coachId !== '' ? Number(queryForm.coachId) : undefined,
  scheduleDate: queryForm.scheduleDate || undefined,
  status: queryForm.status ?? undefined
})

const resetForm = () => {
  Object.assign(form, createDefaultForm())
}

const normalizeDate = (value) => {
  if (!value) return ''
  if (Array.isArray(value)) {
    const [year = 0, month = 0, day = 0] = value
    return `${String(year).padStart(4, '0')}-${String(month).padStart(2, '0')}-${String(day).padStart(2, '0')}`
  }
  const raw = String(value)
  if (/^\d{4}-\d{2}-\d{2}$/.test(raw)) {
    return raw
  }
  if (raw.includes('T')) {
    return raw.slice(0, 10)
  }
  return raw.replace(/,/g, '-')
}

const normalizeTime = (value) => {
  if (!value) return ''
  if (Array.isArray(value)) {
    const [hours = 0, minutes = 0, seconds = 0] = value
    return `${String(hours).padStart(2, '0')}:${String(minutes).padStart(2, '0')}:${String(seconds).padStart(2, '0')}`
  }
  const raw = String(value)
  const parts = raw.split(':')
  if (parts.length === 2) {
    return `${parts[0].padStart(2, '0')}:${parts[1].padStart(2, '0')}:00`
  }
  if (parts.length === 3) {
    return `${parts[0].padStart(2, '0')}:${parts[1].padStart(2, '0')}:${parts[2].padStart(2, '0')}`
  }
  return raw
}

const parseTimeToSeconds = (value) => {
  const [hours = '0', minutes = '0', seconds = '0'] = normalizeTime(value).split(':')
  return Number(hours) * 3600 + Number(minutes) * 60 + Number(seconds)
}

const normalizeScheduleRow = (row) => ({
  ...row,
  scheduleDate: normalizeDate(row.scheduleDate),
  startTime: normalizeTime(row.startTime),
  endTime: normalizeTime(row.endTime)
})

const formatTime = (value) => normalizeTime(value) || '-'

const isBookedRow = (row) => Number(row?.bookedCount || 0) > 0

const loadSchedules = async () => {
  try {
    loading.value = true
    const { data } = await listCoachSchedules(buildQueryParams())
    if (data.code !== 200) {
      throw new Error(data.message || '加载档期失败')
    }
    tableData.value = (data.data || []).map(normalizeScheduleRow)
  } catch (err) {
    ElMessage.error(err.message || '加载档期失败')
  } finally {
    loading.value = false
  }
}

const resetQuery = async () => {
  queryForm.coachId = ''
  queryForm.scheduleDate = ''
  queryForm.status = undefined
  await loadSchedules()
}

const openCreateDrawer = async () => {
  drawerMode.value = 'create'
  currentRow.value = null
  resetForm()
  drawerVisible.value = true
  await nextTick()
  formRef.value?.clearValidate()
}

const openEditDrawer = async (row) => {
  drawerMode.value = 'edit'
  currentRow.value = row
  Object.assign(form, {
    coachId: row.coachId,
    scheduleDate: normalizeDate(row.scheduleDate),
    startTime: normalizeTime(row.startTime),
    endTime: normalizeTime(row.endTime),
    price: Number(row.price),
    capacity: row.capacity,
    status: row.status
  })
  drawerVisible.value = true
  await nextTick()
  formRef.value?.clearValidate()
  if (isBookedRow(row)) {
    ElMessage.warning('该档期已有预约，核心字段不可编辑；启停请使用列表中的状态按钮。')
  }
}

const submitForm = async () => {
  if (!formRef.value || isReadonlyEdit.value) {
    return
  }
  try {
    await formRef.value.validate()
    submitting.value = true
    const payload = {
      coachId: form.coachId,
      scheduleDate: form.scheduleDate,
      startTime: normalizeTime(form.startTime),
      endTime: normalizeTime(form.endTime),
      price: form.price,
      capacity: form.capacity,
      status: form.status
    }
    const { data } =
      drawerMode.value === 'create'
        ? await createCoachSchedule(payload)
        : await updateCoachSchedule(currentRow.value.id, payload)
    if (data.code !== 200) {
      throw new Error(data.message || '保存档期失败')
    }
    ElMessage.success(drawerMode.value === 'create' ? '档期已创建' : '档期已更新')
    drawerVisible.value = false
    await loadSchedules()
  } catch (err) {
    if (err?.message) {
      ElMessage.error(err.message || '保存档期失败')
    }
  } finally {
    submitting.value = false
  }
}

const toggleStatus = async (row) => {
  const nextStatus = row.status === 1 ? 0 : 1
  try {
    statusLoadingMap[row.id] = true
    const { data } = await updateCoachScheduleStatus(row.id, nextStatus)
    if (data.code !== 200) {
      throw new Error(data.message || '更新状态失败')
    }
    ElMessage.success(nextStatus === 1 ? '档期已启用' : '档期已停用')
    await loadSchedules()
  } catch (err) {
    ElMessage.error(err.message || '更新状态失败')
  } finally {
    statusLoadingMap[row.id] = false
  }
}

const removeSchedule = async (row) => {
  try {
    deleteLoadingMap[row.id] = true
    const { data } = await deleteCoachSchedule(row.id)
    if (data.code !== 200) {
      throw new Error(data.message || '删除档期失败')
    }
    ElMessage.success('档期已删除')
    await loadSchedules()
  } catch (err) {
    ElMessage.error(err.message || '删除档期失败')
  } finally {
    deleteLoadingMap[row.id] = false
  }
}

const goBookingOps = () => {
  router.push('/booking-ops')
}

onMounted(loadSchedules)
</script>

<style scoped>
.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
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

.toolbar-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.summary-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
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

.filter-panel {
  margin-bottom: 16px;
  padding: 16px;
  border-radius: 16px;
  border: 1px solid var(--border);
  background: #ffffffc7;
}

.filter-form {
  margin-bottom: -18px;
}

.table-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.tooltip-button {
  display: inline-flex;
}

.drawer-alert {
  margin-bottom: 16px;
}

.schedule-form {
  padding-top: 4px;
}

.time-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.drawer-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  width: 100%;
}

@media (max-width: 860px) {
  .toolbar {
    flex-direction: column;
    align-items: flex-start;
  }

  .time-grid {
    grid-template-columns: 1fr;
  }
}
</style>
