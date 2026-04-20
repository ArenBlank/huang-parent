<template>
  <div class="card">
    <div class="toolbar">
      <div>
        <h2>课程</h2>
        <p>管理课程内容、上下架状态，并按课程生命周期快速整理视图。</p>
      </div>
      <div class="toolbar-actions">
        <el-button @click="loadCourses" :loading="loading">刷新</el-button>
        <el-button type="primary" @click="openDrawer()">新建课程</el-button>
      </div>
    </div>

    <div class="summary-grid">
      <div class="summary-card">
        <div class="summary-label">课程总数</div>
        <div class="summary-value">{{ allCourses.length }}</div>
        <div class="summary-sub">当前上架 {{ publishedCount }}</div>
      </div>
      <div class="summary-card">
        <div class="summary-label">未结束课程</div>
        <div class="summary-value">{{ unfinishedCount }}</div>
        <div class="summary-sub">默认只看这部分，减少运营干扰</div>
      </div>
      <div class="summary-card">
        <div class="summary-label">已结束课程</div>
        <div class="summary-value">{{ endedCount }}</div>
        <div class="summary-sub">有报名会自动下架，无报名会自动清理</div>
      </div>
      <div class="summary-card">
        <div class="summary-label">无排期课程</div>
        <div class="summary-value">{{ noScheduleCount }}</div>
        <div class="summary-sub">适合继续补排期或直接清理草稿</div>
      </div>
    </div>

    <el-tabs v-model="activeTab" class="lifecycle-tabs">
      <el-tab-pane :label="`未结束 (${unfinishedCount})`" name="unfinished" />
      <el-tab-pane :label="`已结束 (${endedCount})`" name="ended" />
      <el-tab-pane :label="`无排期 (${noScheduleCount})`" name="no-schedule" />
      <el-tab-pane :label="`全部 (${allCourses.length})`" name="all" />
    </el-tabs>

    <el-table :data="visibleCourses" style="width: 100%" v-loading="loading">
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="title" label="标题" min-width="240" />
      <el-table-column prop="categoryId" label="分类" width="110" />
      <el-table-column label="阶段" width="120">
        <template #default="scope">
          <el-tag :type="lifecycleTagType(scope.row.lifecycleStatus)">
            {{ scope.row.lifecycleLabel || '-' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="排期" width="120">
        <template #default="scope">
          {{ scope.row.activeSchedules || 0 }} / {{ scope.row.totalSchedules || 0 }}
        </template>
      </el-table-column>
      <el-table-column label="报名" width="120">
        <template #default="scope">
          <el-tag :type="scope.row.hasEnrollment ? 'warning' : 'info'">
            {{ scope.row.hasEnrollment ? '已有报名' : '暂无报名' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="price" label="价格" width="120" />
      <el-table-column label="状态" width="120">
        <template #default="scope">
          <el-tag :type="scope.row.status === 1 ? 'success' : 'info'">
            {{ scope.row.status === 1 ? '已上架' : '已下架' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="360">
        <template #default="scope">
          <el-button size="small" @click="openDrawer(scope.row)">编辑</el-button>
          <el-button
            size="small"
            :type="scope.row.status === 1 ? 'warning' : 'success'"
            @click="togglePublish(scope.row)"
            :disabled="scope.row.status !== 1 && scope.row.lifecycleStatus === 'ENDED'"
          >
            {{ scope.row.status === 1 ? '下架' : '上架' }}
          </el-button>
          <el-tooltip
            v-if="scope.row.hasEnrollment"
            content="已有报名记录的课程会保留用于追溯，结束后会自动下架并归类到已结束列表。"
            placement="top"
          >
            <span class="inline-block">
              <el-button size="small" type="danger" plain disabled>删除</el-button>
            </span>
          </el-tooltip>
          <el-button
            v-else
            size="small"
            type="danger"
            plain
            @click="removeCourse(scope.row)"
          >
            删除
          </el-button>
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
          <el-option label="已下架" :value="0" />
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
import { ElMessage, ElMessageBox } from 'element-plus'
import { adminClient } from '../api/client'

const allCourses = ref([])
const loading = ref(false)
const creating = ref(false)
const drawerVisible = ref(false)
const editingId = ref(null)
const activeTab = ref('unfinished')
const drawerTitle = computed(() => (editingId.value ? '编辑课程' : '新建课程'))

const publishedCount = computed(() => allCourses.value.filter((item) => item.status === 1).length)
const unfinishedCount = computed(
  () => allCourses.value.filter((item) => item.lifecycleStatus === 'UPCOMING' || item.lifecycleStatus === 'ONGOING').length
)
const endedCount = computed(() => allCourses.value.filter((item) => item.lifecycleStatus === 'ENDED').length)
const noScheduleCount = computed(() => allCourses.value.filter((item) => item.lifecycleStatus === 'NO_SCHEDULE').length)

const visibleCourses = computed(() => {
  if (activeTab.value === 'ended') {
    return allCourses.value.filter((item) => item.lifecycleStatus === 'ENDED')
  }
  if (activeTab.value === 'no-schedule') {
    return allCourses.value.filter((item) => item.lifecycleStatus === 'NO_SCHEDULE')
  }
  if (activeTab.value === 'all') {
    return allCourses.value
  }
  return allCourses.value.filter((item) => item.lifecycleStatus === 'UPCOMING' || item.lifecycleStatus === 'ONGOING')
})

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

const lifecycleTagType = (status) => {
  if (status === 'ONGOING') return 'success'
  if (status === 'UPCOMING') return 'warning'
  if (status === 'ENDED') return 'info'
  return ''
}

const loadCourses = async () => {
  try {
    loading.value = true
    const { data } = await adminClient.get('/admin/course/list')
    if (data.code !== 200) throw new Error(data.message || '加载失败')
    allCourses.value = data.data || []
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
    if (data.code !== 200) throw new Error(data.message || '提交失败')
    ElMessage.success(editingId.value ? '课程已更新' : '课程已创建')
    drawerVisible.value = false
    await loadCourses()
  } catch (err) {
    ElMessage.error(err.message || '提交失败')
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

const togglePublish = async (row) => {
  const nextStatus = row.status === 1 ? 0 : 1
  if (nextStatus === 1 && row.lifecycleStatus === 'ENDED') {
    ElMessage.warning('已结束课程不能重新上架，请先补新的未来排期')
    return
  }
  try {
    const { data } = await adminClient.put(`/admin/course/${row.id}/status`, null, {
      params: { status: nextStatus }
    })
    if (data.code !== 200) throw new Error(data.message || '状态更新失败')
    ElMessage.success(nextStatus === 1 ? '已上架' : '已下架')
    await loadCourses()
  } catch (err) {
    ElMessage.error(err.message || '状态更新失败')
  }
}

const removeCourse = async (row) => {
  try {
    await ElMessageBox.confirm(
      `确认删除课程《${row.title}》吗？没有任何报名记录的课程会被逻辑删除，并同步清理关联排期。`,
      '删除课程',
      {
        type: 'warning',
        confirmButtonText: '确认删除',
        cancelButtonText: '取消'
      }
    )
    const { data } = await adminClient.delete(`/admin/course/${row.id}`)
    if (data.code !== 200) throw new Error(data.message || '删除失败')
    ElMessage.success('课程已删除')
    await loadCourses()
  } catch (err) {
    if (err === 'cancel' || err === 'close') {
      return
    }
    ElMessage.error(err.message || '删除失败')
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
  gap: 12px;
}

.toolbar-actions {
  display: flex;
  gap: 8px;
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

.lifecycle-tabs {
  margin-bottom: 16px;
}

.form-actions {
  display: flex;
  gap: 8px;
}

.inline-block {
  display: inline-block;
}
</style>
