<template>
  <div class="card">
    <div class="toolbar">
      <div>
        <h2>Course Schedules</h2>
        <p>Create and review schedule slots</p>
      </div>
      <el-button type="primary" @click="drawerVisible = true">Create Schedule</el-button>
    </div>
    <el-table :data="schedules" style="width: 100%" v-loading="loading">
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="courseId" label="Course" width="120" />
      <el-table-column prop="coachId" label="Coach" width="120" />
      <el-table-column prop="startTime" label="Start" />
      <el-table-column prop="endTime" label="End" />
      <el-table-column prop="capacity" label="Capacity" width="120" />
      <el-table-column prop="status" label="Status" width="90" />
    </el-table>
  </div>

  <el-drawer v-model="drawerVisible" title="Create Schedule" size="30%">
    <el-form :model="form" label-position="top">
      <el-form-item label="Course ID">
        <el-input v-model.number="form.courseId" />
      </el-form-item>
      <el-form-item label="Coach ID">
        <el-input v-model.number="form.coachId" />
      </el-form-item>
      <el-form-item label="Start Time (yyyy-MM-dd HH:mm:ss)">
        <el-input v-model="form.startTime" />
      </el-form-item>
      <el-form-item label="End Time (yyyy-MM-dd HH:mm:ss)">
        <el-input v-model="form.endTime" />
      </el-form-item>
      <el-form-item label="Capacity">
        <el-input v-model.number="form.capacity" />
      </el-form-item>
      <el-form-item label="Status">
        <el-select v-model.number="form.status">
          <el-option label="Inactive" :value="0" />
          <el-option label="Active" :value="1" />
        </el-select>
      </el-form-item>
      <el-button type="primary" @click="createSchedule" :loading="creating">Submit</el-button>
    </el-form>
  </el-drawer>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { adminClient } from '../api/client'

const schedules = ref([])
const loading = ref(false)
const creating = ref(false)
const drawerVisible = ref(false)

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
    if (data.code !== 200) throw new Error(data.message || 'Load failed')
    schedules.value = data.data || []
  } catch (err) {
    ElMessage.error(err.message || 'Load failed')
  } finally {
    loading.value = false
  }
}

const createSchedule = async () => {
  try {
    creating.value = true
    const { data } = await adminClient.post('/admin/course/schedule', form)
    if (data.code !== 200) throw new Error(data.message || 'Create failed')
    ElMessage.success('Schedule created')
    drawerVisible.value = false
    await loadSchedules()
  } catch (err) {
    ElMessage.error(err.message || 'Create failed')
  } finally {
    creating.value = false
  }
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
</style>
