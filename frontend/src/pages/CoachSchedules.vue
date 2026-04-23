<template>
  <div class="card">
    <div class="toolbar">
      <div>
        <h2>教练预约档期</h2>
        <p>先从教练名册里选人，再为教练创建某一天可预约的服务时间段。这里使用的是教练档案，不是角色管理里的角色 ID。</p>
      </div>
      <div class="toolbar-actions">
        <el-button @click="goBookingOps">前往预约订单处理</el-button>
        <el-button type="primary" @click="openCreateDrawer">新建可预约时间段</el-button>
      </div>
    </div>

    <el-alert
      type="info"
      :closable="false"
      show-icon
      title="列表中的“档案ID”才是排班、预约和评价链路里真正使用的教练标识，不是角色管理里的角色ID。"
      class="coach-alert"
    />

    <div class="summary-grid">
      <div class="summary-card">
        <div class="summary-label">可排班教练</div>
        <div class="summary-value">{{ coachOptions.length }}</div>
        <div class="summary-sub">已审核通过且可用于排班</div>
      </div>
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
        <div class="summary-sub">已有预约的档期不可删除</div>
      </div>
      <div class="summary-card">
        <div class="summary-label">未来可用</div>
        <div class="summary-value">{{ futureAvailableCount }}</div>
        <div class="summary-sub">启用且仍有余量</div>
      </div>
    </div>

    <div class="filter-panel">
      <div class="filter-panel__head">
        <div>
          <h3>筛选预约档期</h3>
          <p>教练必须从列表中选，不再要求手输数字。日期筛选的是“用户实际预约和到店服务的日期”。</p>
        </div>
        <el-button plain @click="openCoachPicker('filter')">查看教练名册</el-button>
      </div>

      <el-form :inline="true" class="filter-form">
        <el-form-item label="教练">
          <div class="coach-picker-field">
            <button type="button" class="coach-picker-trigger" @click="openCoachPicker('filter')">
              <span v-if="selectedFilterCoach">{{ coachOptionLabel(selectedFilterCoach) }}</span>
              <span v-else class="muted">从教练列表中选择</span>
            </button>
            <el-button
              v-if="queryForm.coachId !== null"
              text
              type="primary"
              @click="clearSelectedCoach('filter')"
            >
              清空
            </el-button>
          </div>
          <div class="field-help">当前共 {{ coachOptions.length }} 位可排班教练。显示名称来自教练档案，不是角色表。</div>
        </el-form-item>

        <el-form-item label="预约服务日期">
          <el-date-picker
            v-model="queryForm.scheduleDate"
            type="date"
            value-format="YYYY-MM-DD"
            placeholder="筛选某一天的预约档期"
            clearable
            style="width: 220px"
          />
          <div class="field-help">这一天就是用户实际预约和到店服务的日期，不是创建时间，也不是最晚预约时间。</div>
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
      <el-table-column prop="id" label="档期ID" width="88" />
      <el-table-column label="教练" min-width="240">
        <template #default="{ row }">
          <div class="coach-cell">
            <strong>{{ coachDisplayName(row.coachId) }}</strong>
            <span>档案ID {{ row.coachId }}</span>
            <span v-if="coachDisplayMeta(row.coachId)">{{ coachDisplayMeta(row.coachId) }}</span>
          </div>
        </template>
      </el-table-column>
      <el-table-column prop="scheduleDate" label="预约服务日期" width="140" />
      <el-table-column label="开始服务时间" width="140">
        <template #default="{ row }">
          {{ formatTime(row.startTime) }}
        </template>
      </el-table-column>
      <el-table-column label="结束服务时间" width="140">
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
    :title="drawerMode === 'create' ? '新建可预约时间段' : '编辑教练预约档期'"
    size="38%"
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
      <el-form-item label="教练" prop="coachId">
        <div class="coach-picker-field">
          <button
            type="button"
            class="coach-picker-trigger"
            :disabled="isReadonlyEdit"
            @click="openCoachPicker('form')"
          >
            <span v-if="selectedFormCoach">{{ coachOptionLabel(selectedFormCoach) }}</span>
            <span v-else class="muted">从教练列表中选择</span>
          </button>
          <el-button
            v-if="form.coachId !== null && !isReadonlyEdit"
            text
            type="primary"
            @click="clearSelectedCoach('form')"
          >
            清空
          </el-button>
        </div>
        <div class="field-help">创建档期前先选择教练档案。这里不再要求手动输入教练ID。</div>
      </el-form-item>

      <el-form-item label="预约服务日期" prop="scheduleDate">
        <el-date-picker
          v-model="form.scheduleDate"
          type="date"
          value-format="YYYY-MM-DD"
          placeholder="选择用户实际预约的服务日期"
          :disabled="isReadonlyEdit"
          style="width: 100%"
        />
        <div class="field-help">这一天就是用户会看到并下单预约的日期，不是创建时间，也不是截止时间。</div>
      </el-form-item>

      <div class="time-grid">
        <el-form-item label="开始服务时间" prop="startTime">
          <el-time-picker
            v-model="form.startTime"
            value-format="HH:mm:ss"
            placeholder="例如 18:00"
            :disabled="isReadonlyEdit"
            style="width: 100%"
          />
        </el-form-item>

        <el-form-item label="结束服务时间" prop="endTime">
          <el-time-picker
            v-model="form.endTime"
            value-format="HH:mm:ss"
            placeholder="例如 19:00"
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

  <el-dialog
    v-model="coachPickerVisible"
    :title="coachPickerMode === 'filter' ? '选择筛选教练' : '选择排班教练'"
    width="760px"
  >
    <div class="picker-head">
      <div>
        <strong>当前可选教练 {{ visibleCoachOptions.length }} 位</strong>
        <p>{{ coachOptionsHint }}</p>
      </div>
      <el-input
        v-model="coachKeyword"
        clearable
        placeholder="按昵称、账号、手机号或擅长领域筛选"
        style="width: 260px"
      />
    </div>

    <el-alert
      v-if="coachOptionsError"
      type="warning"
      :closable="false"
      show-icon
      :title="coachOptionsError"
      class="picker-alert"
    />

    <el-empty v-if="!visibleCoachOptions.length && !coachLoading" description="当前没有可选教练" />

    <div v-else class="coach-option-grid" v-loading="coachLoading">
      <button
        v-for="coach in visibleCoachOptions"
        :key="coach.coachId"
        type="button"
        class="coach-option-card"
        @click="selectCoachOption(coach)"
      >
        <div class="coach-option-card__top">
          <strong>{{ coachOptionLabel(coach) }}</strong>
          <span>档案ID {{ coach.coachId }}</span>
        </div>
        <div class="coach-option-card__meta">
          <span v-if="coach.expertise">{{ coach.expertise }}</span>
          <span v-if="coach.years !== null && coach.years !== undefined">教龄 {{ coach.years }} 年</span>
          <span v-if="coach.phone">{{ coach.phone }}</span>
        </div>
        <div class="coach-option-card__footer">
          <span>{{ coach.username || '未设置账号' }}</span>
          <span v-if="coach.price !== null && coach.price !== undefined">基础价 {{ coach.price }}</span>
        </div>
      </button>
    </div>
  </el-dialog>
</template>

<script setup>
import { computed, nextTick, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { useRouter } from 'vue-router'
import {
  createCoachSchedule,
  deleteCoachSchedule,
  listCoachScheduleCoachOptions,
  listCoachSchedules,
  updateCoachSchedule,
  updateCoachScheduleStatus
} from '../api/coachSchedule'
import { isHandledBusinessError } from '../api/client'

const router = useRouter()
const loading = ref(false)
const coachLoading = ref(false)
const submitting = ref(false)
const drawerVisible = ref(false)
const drawerMode = ref('create')
const coachPickerVisible = ref(false)
const coachPickerMode = ref('filter')
const coachKeyword = ref('')
const coachOptionsError = ref('')
const formRef = ref(null)
const tableData = ref([])
const coachOptions = ref([])
const currentRow = ref(null)
const statusLoadingMap = reactive({})
const deleteLoadingMap = reactive({})

const queryForm = reactive({
  coachId: null,
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

const coachOptionMap = computed(() => {
  const map = new Map()
  coachOptions.value.forEach((item) => map.set(Number(item.coachId), item))
  return map
})

const selectedFilterCoach = computed(() => coachOptionMap.value.get(Number(queryForm.coachId)) || null)
const selectedFormCoach = computed(() => coachOptionMap.value.get(Number(form.coachId)) || null)
const visibleCoachOptions = computed(() => {
  const keyword = coachKeyword.value.trim().toLowerCase()
  if (!keyword) return coachOptions.value
  return coachOptions.value.filter((item) => {
    return [
      item.displayName,
      item.username,
      item.nickname,
      item.phone,
      item.expertise
    ].some((field) => String(field || '').toLowerCase().includes(keyword))
  })
})
const coachOptionsHint = computed(() => {
  if (coachOptionsError.value) return coachOptionsError.value
  return '这里展示的是已审核通过且可用于预约排班的教练档案。'
})

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
  coachId: [{ required: true, message: '请选择教练', trigger: 'change' }],
  scheduleDate: [{ required: true, message: '请选择预约服务日期', trigger: 'change' }],
  startTime: [{ required: true, message: '请选择开始服务时间', trigger: 'change' }],
  endTime: [
    { required: true, message: '请选择结束服务时间', trigger: 'change' },
    {
      validator: (_, value, callback) => {
        if (!value || !form.startTime) {
          callback()
          return
        }
        if (parseTimeToSeconds(value) <= parseTimeToSeconds(form.startTime)) {
          callback(new Error('结束服务时间必须晚于开始服务时间'))
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

const coachOptionLabel = (coach) => {
  if (!coach) return '未选择教练'
  return `${coach.displayName || coach.nickname || coach.username || '教练'}`
}

const coachDisplayName = (coachId) => {
  const coach = coachOptionMap.value.get(Number(coachId))
  return coach ? coachOptionLabel(coach) : `教练档案 ${coachId ?? '-'}`
}

const coachDisplayMeta = (coachId) => {
  const coach = coachOptionMap.value.get(Number(coachId))
  if (!coach) return ''
  const chunks = []
  if (coach.expertise) chunks.push(coach.expertise)
  if (coach.phone) chunks.push(coach.phone)
  return chunks.join(' / ')
}

const buildQueryParams = () => ({
  coachId: queryForm.coachId ?? undefined,
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

const loadCoachOptions = async ({ silent = false } = {}) => {
  try {
    coachLoading.value = true
    const { data } = await listCoachScheduleCoachOptions()
    if (data.code !== 200) {
      throw new Error(data.message || '加载教练名册失败')
    }
    coachOptions.value = data.data || []
    coachOptionsError.value = ''
  } catch (error) {
    coachOptionsError.value =
      !error?.message || error.message === '失败'
        ? '教练名册接口暂未生效。请重启管理端后端服务 web-admin，再重新打开此页面。'
        : error.message
    if (!silent && !isHandledBusinessError(error)) {
      ElMessage.error(coachOptionsError.value || '加载教练名册失败')
    }
  } finally {
    coachLoading.value = false
  }
}

const loadSchedules = async () => {
  try {
    loading.value = true
    const { data } = await listCoachSchedules(buildQueryParams())
    if (data.code !== 200) {
      throw new Error(data.message || '加载档期失败')
    }
    tableData.value = (data.data || []).map(normalizeScheduleRow)
  } catch (error) {
    ElMessage.error(error.message || '加载档期失败')
  } finally {
    loading.value = false
  }
}

const resetQuery = async () => {
  queryForm.coachId = null
  queryForm.scheduleDate = ''
  queryForm.status = undefined
  await loadSchedules()
}

const openCoachPicker = async (mode) => {
  coachPickerMode.value = mode
  coachKeyword.value = ''
  if (!coachOptions.value.length) {
    await loadCoachOptions()
  }
  coachPickerVisible.value = true
}

const selectCoachOption = (coach) => {
  const coachId = Number(coach?.coachId)
  if (!Number.isFinite(coachId)) return
  if (coachPickerMode.value === 'form') {
    form.coachId = coachId
    if ((form.price === null || form.price === undefined || Number(form.price) === 0) && coach.price !== null && coach.price !== undefined) {
      form.price = Number(coach.price)
    }
  } else {
    queryForm.coachId = coachId
  }
  coachPickerVisible.value = false
}

const clearSelectedCoach = (mode) => {
  if (mode === 'form') {
    form.coachId = null
    return
  }
  queryForm.coachId = null
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
    coachId: Number(row.coachId),
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
  } catch (error) {
    if (error?.message) {
      ElMessage.error(error.message || '保存档期失败')
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
  } catch (error) {
    ElMessage.error(error.message || '更新状态失败')
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
  } catch (error) {
    ElMessage.error(error.message || '删除档期失败')
  } finally {
    deleteLoadingMap[row.id] = false
  }
}

const goBookingOps = () => {
  router.push('/booking-ops')
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
  line-height: 1.6;
}

.toolbar-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.coach-alert {
  margin-bottom: 16px;
}

.summary-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  gap: 12px;
  margin-bottom: 16px;
}

.summary-card {
  padding: 14px;
  border-radius: 16px;
  border: 1px solid var(--border);
  background: #ffffffd9;
}

.summary-label {
  font-size: 12px;
  color: #8aa0af;
}

.summary-value {
  margin: 8px 0 4px;
  font-size: 24px;
  font-weight: 700;
}

.summary-sub {
  font-size: 12px;
  color: #6b7280;
}

.filter-panel {
  margin-bottom: 16px;
  padding: 16px;
  border-radius: 18px;
  border: 1px solid var(--border);
  background: #ffffffc7;
}

.filter-panel__head {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
  margin-bottom: 16px;
}

.filter-panel__head h3 {
  margin: 0 0 6px;
}

.filter-panel__head p {
  margin: 0;
  color: #6b7280;
  font-size: 13px;
  line-height: 1.6;
}

.filter-form {
  margin-bottom: -18px;
}

.coach-picker-field {
  display: flex;
  align-items: center;
  gap: 10px;
  min-height: 40px;
}

.coach-picker-trigger {
  width: 260px;
  min-height: 42px;
  padding: 0 14px;
  border: 1px solid var(--border);
  border-radius: 12px;
  background: #fff;
  color: var(--text);
  text-align: left;
  cursor: pointer;
  transition: border-color 0.2s ease, box-shadow 0.2s ease;
}

.coach-picker-trigger:hover {
  border-color: #6d67ff;
  box-shadow: 0 0 0 3px rgba(109, 103, 255, 0.08);
}

.coach-picker-trigger:disabled {
  cursor: not-allowed;
  color: #9ca3af;
  background: #f3f4f6;
}

.field-help {
  margin-top: 6px;
  color: #6b7280;
  font-size: 12px;
  line-height: 1.5;
  max-width: 360px;
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
  margin-bottom: 14px;
}

.schedule-form {
  padding-bottom: 12px;
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

.picker-head {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
  margin-bottom: 16px;
}

.picker-head strong {
  display: block;
  margin-bottom: 4px;
}

.picker-head p {
  margin: 0;
  color: #6b7280;
  font-size: 13px;
}

.picker-alert {
  margin-bottom: 16px;
}

.coach-option-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 12px;
  min-height: 120px;
}

.coach-option-card {
  display: grid;
  gap: 10px;
  padding: 14px;
  border: 1px solid var(--border);
  border-radius: 16px;
  background: linear-gradient(180deg, #ffffff, #faf7ff);
  text-align: left;
  cursor: pointer;
  transition: transform 0.18s ease, box-shadow 0.18s ease, border-color 0.18s ease;
}

.coach-option-card:hover {
  transform: translateY(-2px);
  border-color: #6d67ff;
  box-shadow: 0 12px 24px rgba(23, 17, 38, 0.08);
}

.coach-option-card__top,
.coach-option-card__footer {
  display: flex;
  justify-content: space-between;
  gap: 8px;
}

.coach-option-card__top span,
.coach-option-card__meta,
.coach-option-card__footer {
  color: #6b7280;
  font-size: 12px;
}

.coach-option-card__meta {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.muted {
  color: #9ca3af;
}

@media (max-width: 860px) {
  .toolbar,
  .filter-panel__head,
  .picker-head {
    flex-direction: column;
    align-items: flex-start;
  }

  .time-grid {
    grid-template-columns: 1fr;
  }

  .coach-picker-trigger {
    width: 100%;
  }
}
</style>
