<template>
  <div class="card">
    <div class="toolbar">
      <div>
        <h2>课程</h2>
        <p>管理课程内容与上下架状态</p>
      </div>
      <el-button type="primary" @click="drawerVisible = true">新建课程</el-button>
    </div>
    <div class="summary-grid">
      <div class="summary-card">
        <div class="summary-label">课程总数</div>
        <div class="summary-value">{{ courses.length }}</div>
        <div class="summary-sub">上架 {{ publishedCount }}</div>
      </div>
      <div class="summary-card">
        <div class="summary-label">最新课程</div>
        <div class="summary-value">{{ latestCourse?.title || '-' }}</div>
        <div class="summary-sub">ID {{ latestCourse?.id || '-' }}</div>
      </div>
    </div>

    <el-table :data="courses" style="width: 100%" v-loading="loading">
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="title" label="标题" />
      <el-table-column prop="categoryId" label="分类" width="110" />
      <el-table-column prop="price" label="价格" width="120" />
      <el-table-column label="状态" width="120">
        <template #default="scope">
          <el-tag :type="scope.row.status === 1 ? 'success' : 'info'">
            {{ scope.row.status === 1 ? '已上架' : '草稿' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="220">
        <template #default="scope">
          <el-button size="small" @click="openDrawer(scope.row)">编辑</el-button>
          <el-button size="small" type="warning" @click="publish(scope.row)" :disabled="scope.row.status === 1">上架</el-button>
        </template>
      </el-table-column>
    </el-table>
  </div>

  <el-drawer v-model="drawerVisible" :title="drawerTitle" size="30%">
    <el-form :model="form" label-position="top">
      <el-form-item label="分类 ID">
        <el-input v-model.number="form.categoryId" />
      </el-form-item>
      <el-form-item label="标题">
        <el-input v-model="form.title" />
      </el-form-item>
      <el-form-item label="简介">
        <el-input v-model="form.summary" type="textarea" />
      </el-form-item>
      <el-form-item label="封面 URL">
        <el-input v-model="form.coverUrl" />
      </el-form-item>
      <el-form-item label="难度">
        <el-input v-model="form.level" />
      </el-form-item>
      <el-form-item label="时长（分钟）">
        <el-input v-model.number="form.durationMin" />
      </el-form-item>
      <el-form-item label="价格">
        <el-input v-model.number="form.price" />
      </el-form-item>
      <el-form-item label="状态">
        <el-select v-model.number="form.status">
          <el-option label="草稿" :value="0" />
          <el-option label="已上架" :value="1" />
        </el-select>
      </el-form-item>
      <div class="form-actions">
        <el-button @click="fillSample">填充示例</el-button>
        <el-button type="primary" @click="createCourse" :loading="creating">提交</el-button>
      </div>
    </el-form>
  </el-drawer>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { adminClient } from '../api/client'

const courses = ref([])
const loading = ref(false)
const creating = ref(false)
const drawerVisible = ref(false)
const editingId = ref(null)
const drawerTitle = computed(() => (editingId.value ? '编辑课程' : '新建课程'))

const publishedCount = computed(() => courses.value.filter((item) => item.status === 1).length)
const latestCourse = computed(() => (courses.value.length ? courses.value[0] : null))

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
    if (data.code !== 200) throw new Error(data.message || '加载失败')
    courses.value = data.data || []
  } catch (err) {
    ElMessage.error(err.message || '加载失败')
  } finally {
    loading.value = false
  }
}

const openDrawer = (row) => {
  if (row) {
    editingId.value = row.id
    form.categoryId = row.categoryId ?? 1
    form.title = row.title || ''
    form.summary = row.summary || ''
    form.coverUrl = row.coverUrl || ''
    form.level = row.level || 'beginner'
    form.durationMin = row.durationMin ?? 30
    form.price = row.price ?? 199
    form.status = row.status ?? 0
  } else {
    editingId.value = null
    form.categoryId = 1
    form.title = ''
    form.summary = ''
    form.coverUrl = ''
    form.level = 'beginner'
    form.durationMin = 30
    form.price = 199
    form.status = 0
  }
  drawerVisible.value = true
}

const createCourse = async () => {
  try {
    creating.value = true
    let data
    if (editingId.value) {
      ;({ data } = await adminClient.put(`/admin/course/${editingId.value}`, form))
    } else {
      ;({ data } = await adminClient.post('/admin/course', form))
    }
    if (data.code !== 200) throw new Error(data.message || '创建失败')
    ElMessage.success(editingId.value ? '课程已更新' : '课程已创建')
    drawerVisible.value = false
    await loadCourses()
  } catch (err) {
    ElMessage.error(err.message || '创建失败')
  } finally {
    creating.value = false
  }
}

const fillSample = () => {
  form.categoryId = 1
  form.title = '燃脂入门课'
  form.summary = '适合新手的 30 分钟燃脂课程'
  form.coverUrl = 'https://images.unsplash.com/photo-1517836357463-d25dfeac3438?w=1200'
  form.level = 'beginner'
  form.durationMin = 30
  form.price = 199
  form.status = 0
}

const publish = async (row) => {
  try {
    const { data } = await adminClient.put(`/admin/course/${row.id}/status`, null, {
      params: { status: 1 }
    })
    if (data.code !== 200) throw new Error(data.message || '上架失败')
    ElMessage.success('已上架')
    await loadCourses()
  } catch (err) {
    ElMessage.error(err.message || '上架失败')
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
