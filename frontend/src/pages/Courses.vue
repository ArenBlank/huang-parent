<template>
  <div class="card">
    <div class="toolbar">
      <div>
        <h2>Courses</h2>
        <p>Manage course content and status</p>
      </div>
      <el-button type="primary" @click="drawerVisible = true">Create Course</el-button>
    </div>
    <el-table :data="courses" style="width: 100%" v-loading="loading">
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="title" label="Title" />
      <el-table-column prop="categoryId" label="Category" width="110" />
      <el-table-column prop="price" label="Price" width="120" />
      <el-table-column prop="status" label="Status" width="90" />
      <el-table-column label="Actions" width="160">
        <template #default="scope">
          <el-button size="small" type="warning" @click="publish(scope.row)">Publish</el-button>
        </template>
      </el-table-column>
    </el-table>
  </div>

  <el-drawer v-model="drawerVisible" title="Create Course" size="30%">
    <el-form :model="form" label-position="top">
      <el-form-item label="Category ID">
        <el-input v-model.number="form.categoryId" />
      </el-form-item>
      <el-form-item label="Title">
        <el-input v-model="form.title" />
      </el-form-item>
      <el-form-item label="Summary">
        <el-input v-model="form.summary" type="textarea" />
      </el-form-item>
      <el-form-item label="Cover URL">
        <el-input v-model="form.coverUrl" />
      </el-form-item>
      <el-form-item label="Level">
        <el-input v-model="form.level" />
      </el-form-item>
      <el-form-item label="Duration (min)">
        <el-input v-model.number="form.durationMin" />
      </el-form-item>
      <el-form-item label="Price">
        <el-input v-model.number="form.price" />
      </el-form-item>
      <el-form-item label="Status">
        <el-select v-model.number="form.status">
          <el-option label="Draft" :value="0" />
          <el-option label="Published" :value="1" />
        </el-select>
      </el-form-item>
      <el-button type="primary" @click="createCourse" :loading="creating">Submit</el-button>
    </el-form>
  </el-drawer>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { adminClient } from '../api/client'

const courses = ref([])
const loading = ref(false)
const creating = ref(false)
const drawerVisible = ref(false)

const form = reactive({
  categoryId: 1,
  title: '',
  summary: '',
  coverUrl: '',
  level: 'beginner',
  durationMin: 30,
  price: 199,
  status: 0
})

const loadCourses = async () => {
  try {
    loading.value = true
    const { data } = await adminClient.get('/admin/course/list')
    if (data.code !== 200) throw new Error(data.message || 'Load failed')
    courses.value = data.data || []
  } catch (err) {
    ElMessage.error(err.message || 'Load failed')
  } finally {
    loading.value = false
  }
}

const createCourse = async () => {
  try {
    creating.value = true
    const { data } = await adminClient.post('/admin/course', form)
    if (data.code !== 200) throw new Error(data.message || 'Create failed')
    ElMessage.success('Course created')
    drawerVisible.value = false
    await loadCourses()
  } catch (err) {
    ElMessage.error(err.message || 'Create failed')
  } finally {
    creating.value = false
  }
}

const publish = async (row) => {
  try {
    const { data } = await adminClient.put(`/admin/course/${row.id}/status`, null, {
      params: { status: 1 }
    })
    if (data.code !== 200) throw new Error(data.message || 'Publish failed')
    ElMessage.success('Published')
    await loadCourses()
  } catch (err) {
    ElMessage.error(err.message || 'Publish failed')
  }
}

onMounted(loadCourses)
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
