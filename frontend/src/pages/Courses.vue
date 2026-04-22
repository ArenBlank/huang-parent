<template>
  <div class="card">
    <div class="toolbar">
      <div>
        <h2>课程</h2>
        <p>统一管理课程内容、封面素材和上下架状态，运营人员无需再手动填写 MinIO 地址。</p>
      </div>
      <div class="toolbar-actions">
        <el-button :loading="loading" @click="loadCourses">刷新</el-button>
        <el-button type="primary" @click="openDrawer()">新建课程</el-button>
      </div>
    </div>

    <div class="summary-grid">
      <div class="summary-card">
        <div class="summary-label">课程总数</div>
        <div class="summary-value">{{ allCourses.length }}</div>
        <div class="summary-sub">当前已上架 {{ publishedCount }} 门</div>
      </div>
      <div class="summary-card">
        <div class="summary-label">未结束课程</div>
        <div class="summary-value">{{ unfinishedCount }}</div>
        <div class="summary-sub">优先关注仍可运营的课程</div>
      </div>
      <div class="summary-card">
        <div class="summary-label">已结束课程</div>
        <div class="summary-value">{{ endedCount }}</div>
        <div class="summary-sub">有报名记录的课程会自动保留</div>
      </div>
      <div class="summary-card">
        <div class="summary-label">无排期课程</div>
        <div class="summary-value">{{ noScheduleCount }}</div>
        <div class="summary-sub">适合继续补排期或清理草稿</div>
      </div>
    </div>

    <el-tabs v-model="activeTab" class="lifecycle-tabs">
      <el-tab-pane :label="`未结束 (${unfinishedCount})`" name="unfinished" />
      <el-tab-pane :label="`已结束 (${endedCount})`" name="ended" />
      <el-tab-pane :label="`无排期 (${noScheduleCount})`" name="no-schedule" />
      <el-tab-pane :label="`全部 (${allCourses.length})`" name="all" />
    </el-tabs>

    <el-table :data="visibleCourses" style="width: 100%" v-loading="loading">
      <el-table-column prop="id" label="ID" width="72" />
      <el-table-column label="封面" width="108">
        <template #default="{ row }">
          <div class="cover-cell">
            <el-image :src="row.coverUrl" fit="cover" class="cover-image">
              <template #error>
                <img :src="defaultCoverUrl" style="width: 100%; height: 100%; object-fit: cover;" />
              </template>
            </el-image>
          </div>
        </template>
      </el-table-column>
      <el-table-column prop="title" label="标题" min-width="220" />
      <el-table-column prop="categoryId" label="分类 ID" width="96" />
      <el-table-column label="难度" width="110">
        <template #default="{ row }">
          {{ getDifficultyLabel(row.level) }}
        </template>
      </el-table-column>
      <el-table-column label="目标" width="110">
        <template #default="{ row }">
          {{ getTargetLabel(getCourseTarget(row)) }}
        </template>
      </el-table-column>
      <el-table-column label="阶段" width="110">
        <template #default="{ row }">
          <el-tag :type="lifecycleTagType(row.lifecycleStatus)">
            {{ row.lifecycleLabel || '-' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="排期" width="120">
        <template #default="{ row }">
          {{ row.activeSchedules || 0 }} / {{ row.totalSchedules || 0 }}
        </template>
      </el-table-column>
      <el-table-column label="报名" width="110">
        <template #default="{ row }">
          <el-tag :type="row.hasEnrollment ? 'warning' : 'info'">
            {{ row.hasEnrollment ? '已有报名' : '暂无报名' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="price" label="价格" width="96" />
      <el-table-column label="状态" width="110">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'info'">
            {{ row.status === 1 ? '已上架' : '已下架' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="360" fixed="right">
        <template #default="{ row }">
          <el-button size="small" @click="openDrawer(row)">编辑</el-button>
          <el-button
            size="small"
            :type="row.status === 1 ? 'warning' : 'success'"
            :disabled="row.status !== 1 && row.lifecycleStatus === 'ENDED'"
            @click="togglePublish(row)"
          >
            {{ row.status === 1 ? '下架' : '上架' }}
          </el-button>
          <el-button
            v-if="!row.hasEnrollment"
            size="small"
            type="danger"
            plain
            @click="removeCourse(row)"
          >
            删除
          </el-button>
          <el-tooltip
            v-else
            content="已有报名记录的课程会保留用于追踪，不支持直接删除。"
            placement="top"
          >
            <span class="inline-block">
              <el-button size="small" type="danger" plain disabled>删除</el-button>
            </span>
          </el-tooltip>
        </template>
      </el-table-column>
    </el-table>
  </div>

  <el-drawer v-model="drawerVisible" :title="drawerTitle" size="34%">
    <el-alert
      v-if="isReadOnlyEdit"
      type="warning"
      :closable="false"
      show-icon
      class="drawer-alert"
      title="该课程已有报名记录，核心字段仅支持查看。若要调整运营状态，请在列表中直接上架或下架。"
    />

    <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
      <el-form-item label="分类 ID" prop="categoryId">
        <el-input-number v-model="form.categoryId" :min="1" :disabled="drawerFormDisabled" />
      </el-form-item>

      <el-form-item label="标题" prop="title">
        <el-input v-model.trim="form.title" :disabled="drawerFormDisabled" />
      </el-form-item>

      <el-form-item label="简介" prop="summary">
        <el-input
          v-model.trim="form.summary"
          type="textarea"
          :rows="3"
          :disabled="drawerFormDisabled"
        />
      </el-form-item>

      <el-form-item label="封面图片" prop="coverUrl">
        <div class="cover-upload-panel">
          <el-upload
            class="cover-uploader"
            :show-file-list="false"
            :http-request="uploadCoverImage"
            :before-upload="beforeCoverUpload"
            accept="image/png,image/jpeg,image/webp,image/gif"
            :disabled="drawerFormDisabled || uploadingCover"
          >
            <template v-if="form.coverUrl">
              <el-image :src="coverPreviewUrl" fit="cover" class="cover-preview">
                <template #error>
                  <img :src="defaultCoverUrl" style="width: 100%; height: 100%; object-fit: cover;" />
                </template>
              </el-image>
            </template>
            <template v-else>
              <div class="cover-uploader-placeholder">+</div>
            </template>
          </el-upload>

          <div class="cover-upload-meta">
            <div class="cover-upload-title">点击上传封面</div>
            <div class="cover-upload-tip">支持 JPG、PNG、WEBP、GIF，大小不超过 5MB</div>
            <div class="cover-upload-actions">
              <el-button text @click="useDefaultCover" :disabled="drawerFormDisabled">使用默认图</el-button>
              <el-button
                text
                @click="clearCover"
                :disabled="drawerFormDisabled || !form.coverUrl"
              >
                清空封面
              </el-button>
              <span v-if="uploadingCover" class="cover-upload-status">正在上传图片...</span>
            </div>
          </div>
        </div>
      </el-form-item>

      <el-form-item label="难度" prop="level">
        <el-select
          v-model="form.level"
          placeholder="请选择难度"
          :disabled="drawerFormDisabled"
          style="width: 100%"
        >
          <el-option
            v-for="option in difficultyOptions"
            :key="option.value"
            :label="option.label"
            :value="option.value"
          />
          <el-option
            v-if="showCustomDifficultyOption"
            :label="getDifficultyLabel(form.level)"
            :value="form.level"
          />
        </el-select>
      </el-form-item>

      <el-form-item label="时长（分钟）" prop="durationMin">
        <el-input-number v-model="form.durationMin" :min="1" :disabled="drawerFormDisabled" />
      </el-form-item>

      <el-form-item label="价格" prop="price">
        <el-input-number
          v-model="form.price"
          :min="0"
          :precision="2"
          :step="1"
          :disabled="drawerFormDisabled"
        />
      </el-form-item>

      <el-form-item label="状态" prop="status">
        <el-select v-model="form.status" :disabled="drawerFormDisabled" style="width: 100%">
          <el-option label="已下架" :value="0" />
          <el-option label="已上架" :value="1" />
        </el-select>
      </el-form-item>

      <div class="form-actions">
        <el-button @click="fillSample" :disabled="drawerFormDisabled">填充示例</el-button>
        <el-button @click="drawerVisible = false">取消</el-button>
        <el-button
          type="primary"
          :loading="submitting"
          :disabled="drawerFormDisabled"
          @click="submitCourse"
        >
          {{ isEditMode ? '保存修改' : '提交课程' }}
        </el-button>
      </div>
    </el-form>
  </el-drawer>
</template>

<script setup>
import { computed, nextTick, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { adminClient } from '../api/client'

const defaultCoverUrl = '/test.png'

const difficultyMap = {
  BEGINNER: '初级',
  INTERMEDIATE: '中级',
  ADVANCED: '高级'
}

const targetMap = {
  MUSCLE_GAIN: '增肌',
  WEIGHT_LOSS: '减脂',
  FAT_LOSS: '减脂',
  SHAPING: '塑形',
  BODY_SHAPING: '塑形',
  REHABILITATION: '康复',
  STRENGTH: '力量提升',
  ENDURANCE: '耐力提升',
  FLEXIBILITY: '柔韧性',
  CARDIO: '心肺训练',
  GENERAL_FITNESS: '综合体能'
}

const difficultyOptions = [
  { label: difficultyMap.BEGINNER, value: 'beginner' },
  { label: difficultyMap.INTERMEDIATE, value: 'intermediate' },
  { label: difficultyMap.ADVANCED, value: 'advanced' }
]

const allCourses = ref([])
const loading = ref(false)
const submitting = ref(false)
const uploadingCover = ref(false)
const drawerVisible = ref(false)
const editingId = ref(null)
const activeTab = ref('unfinished')
const formRef = ref()
const editingRow = ref(null)

const createEmptyForm = () => ({
  categoryId: 1,
  title: '',
  summary: '',
  coverUrl: '',
  level: 'beginner',
  durationMin: 30,
  price: 199,
  status: 0
})

const form = reactive(createEmptyForm())

const rules = {
  categoryId: [{ required: true, message: '请填写分类 ID', trigger: 'change' }],
  title: [{ required: true, message: '请填写课程标题', trigger: 'blur' }],
  level: [{ required: true, message: '请选择课程难度', trigger: 'change' }],
  durationMin: [{ required: true, message: '请填写课程时长', trigger: 'change' }],
  price: [
    { required: true, message: '请填写课程价格', trigger: 'change' },
    {
      validator: (_, value, callback) => {
        if (value == null || Number(value) < 0) {
          callback(new Error('课程价格不能小于 0'))
          return
        }
        callback()
      },
      trigger: 'change'
    }
  ],
  status: [{ required: true, message: '请选择课程状态', trigger: 'change' }]
}

const drawerTitle = computed(() => (isEditMode.value ? '编辑课程' : '新建课程'))
const isEditMode = computed(() => Boolean(editingId.value))
const isReadOnlyEdit = computed(() => Boolean(editingRow.value?.hasEnrollment))
const drawerFormDisabled = computed(() => isReadOnlyEdit.value)
const coverPreviewUrl = computed(() => form.coverUrl || defaultCoverUrl)
const showCustomDifficultyOption = computed(
  () => Boolean(form.level) && !difficultyOptions.some((option) => option.value === form.level)
)

const publishedCount = computed(() => allCourses.value.filter((item) => item.status === 1).length)
const unfinishedCount = computed(
  () =>
    allCourses.value.filter(
      (item) => item.lifecycleStatus === 'UPCOMING' || item.lifecycleStatus === 'ONGOING'
    ).length
)
const endedCount = computed(
  () => allCourses.value.filter((item) => item.lifecycleStatus === 'ENDED').length
)
const noScheduleCount = computed(
  () => allCourses.value.filter((item) => item.lifecycleStatus === 'NO_SCHEDULE').length
)

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
  return allCourses.value.filter(
    (item) => item.lifecycleStatus === 'UPCOMING' || item.lifecycleStatus === 'ONGOING'
  )
})

const normalizeEnumKey = (value) => String(value || '').trim().toUpperCase()

const normalizeLevelValue = (value) => {
  const normalized = String(value || '').trim().toLowerCase()
  if (normalized === 'beginner' || normalized === 'intermediate' || normalized === 'advanced') {
    return normalized
  }
  return normalized || 'beginner'
}

const getDifficultyLabel = (value) => {
  const key = normalizeEnumKey(value)
  return difficultyMap[key] || value || '-'
}

const getCourseTarget = (course) =>
  course?.target || course?.goal || course?.trainingTarget || course?.courseTarget || ''

const getTargetLabel = (value) => {
  const key = normalizeEnumKey(value)
  return targetMap[key] || value || '-'
}

const lifecycleTagType = (status) => {
  if (status === 'ONGOING') return 'success'
  if (status === 'UPCOMING') return 'warning'
  if (status === 'ENDED') return 'info'
  return ''
}

const resetForm = () => {
  Object.assign(form, createEmptyForm())
  editingId.value = null
  editingRow.value = null
}

const loadCourses = async () => {
  try {
    loading.value = true
    const { data } = await adminClient.get('/admin/course/list')
    if (data.code !== 200) {
      throw new Error(data.message || '课程加载失败')
    }
    allCourses.value = data.data || []
  } catch (error) {
    ElMessage.error(error.message || '课程加载失败')
  } finally {
    loading.value = false
  }
}

const openDrawer = async (row) => {
  resetForm()
  if (row) {
    editingId.value = row.id
    editingRow.value = row
    Object.assign(form, {
      categoryId: row.categoryId ?? 1,
      title: row.title || '',
      summary: row.summary || '',
      coverUrl: row.coverUrl || '',
      level: normalizeLevelValue(row.level),
      durationMin: row.durationMin ?? 30,
      price: row.price ?? 0,
      status: row.status ?? 0
    })
  }
  drawerVisible.value = true
  await nextTick()
  formRef.value?.clearValidate()
}

const fillSample = () => {
  Object.assign(form, {
    categoryId: 1,
    title: '燃脂入门体验课',
    summary: '适合新手的 30 分钟低门槛燃脂课程。',
    coverUrl: defaultCoverUrl,
    level: 'beginner',
    durationMin: 30,
    price: 99,
    status: 0
  })
}

const useDefaultCover = () => {
  form.coverUrl = defaultCoverUrl
}

const clearCover = () => {
  form.coverUrl = ''
}

const beforeCoverUpload = (file) => {
  const allowTypes = ['image/jpeg', 'image/png', 'image/webp', 'image/gif']
  const isImage = allowTypes.includes(file.type)
  if (!isImage) {
    ElMessage.error('仅支持上传 JPG、PNG、WEBP、GIF 图片')
    return false
  }
  const isLt5M = file.size / 1024 / 1024 < 5
  if (!isLt5M) {
    ElMessage.error('图片大小不能超过 5MB')
    return false
  }
  return true
}

const uploadCoverImage = async (options) => {
  try {
    uploadingCover.value = true
    const formData = new FormData()
    formData.append('file', options.file)
    const { data } = await adminClient.post('/admin/upload/image', formData, {
      headers: {
        'Content-Type': 'multipart/form-data'
      }
    })
    if (data.code !== 200 || !data.data) {
      throw new Error(data.message || '封面上传失败')
    }
    form.coverUrl = data.data
    ElMessage.success('封面上传成功')
    options.onSuccess?.(data.data)
  } catch (error) {
    ElMessage.error(error.message || '封面上传失败')
    options.onError?.(error)
  } finally {
    uploadingCover.value = false
  }
}

const submitCourse = async () => {
  if (!formRef.value) {
    return
  }
  try {
    await formRef.value.validate()
    submitting.value = true
    const payload = {
      categoryId: form.categoryId,
      title: form.title,
      summary: form.summary,
      coverUrl: form.coverUrl,
      level: form.level,
      durationMin: form.durationMin,
      price: form.price,
      status: form.status
    }
    const { data } = isEditMode.value
      ? await adminClient.put(`/admin/course/${editingId.value}`, payload)
      : await adminClient.post('/admin/course', payload)

    if (data.code !== 200) {
      throw new Error(data.message || '课程保存失败')
    }

    ElMessage.success(isEditMode.value ? '课程已更新' : '课程已创建')
    drawerVisible.value = false
    await loadCourses()
  } catch (error) {
    if (error?.message) {
      ElMessage.error(error.message)
    }
  } finally {
    submitting.value = false
  }
}

const togglePublish = async (row) => {
  const nextStatus = row.status === 1 ? 0 : 1
  if (nextStatus === 1 && row.lifecycleStatus === 'ENDED') {
    ElMessage.warning('已结束课程不能直接重新上架，请先补未来排期。')
    return
  }

  try {
    const { data } = await adminClient.put(`/admin/course/${row.id}/status`, null, {
      params: { status: nextStatus }
    })
    if (data.code !== 200) {
      throw new Error(data.message || '课程状态更新失败')
    }
    ElMessage.success(nextStatus === 1 ? '课程已上架' : '课程已下架')
    await loadCourses()
  } catch (error) {
    ElMessage.error(error.message || '课程状态更新失败')
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
    if (data.code !== 200) {
      throw new Error(data.message || '课程删除失败')
    }
    ElMessage.success('课程已删除')
    await loadCourses()
  } catch (error) {
    if (error === 'cancel' || error === 'close') {
      return
    }
    ElMessage.error(error.message || '课程删除失败')
  }
}

onMounted(loadCourses)
</script>

<style scoped>
.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
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
  margin: 6px 0;
  font-size: 22px;
  font-weight: 700;
}

.summary-sub {
  font-size: 12px;
  color: #6b7280;
}

.lifecycle-tabs {
  margin-bottom: 16px;
}

.cover-cell {
  width: 68px;
  height: 68px;
  overflow: hidden;
  border: 1px solid var(--border);
  border-radius: 12px;
  background: #f8fafc;
}

.cover-image {
  display: block;
  width: 100%;
  height: 100%;
}

.drawer-alert {
  margin-bottom: 16px;
}

.cover-upload-panel {
  display: flex;
  align-items: center;
  gap: 16px;
  flex-wrap: wrap;
}

.cover-uploader :deep(.el-upload) {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 160px;
  height: 160px;
  overflow: hidden;
  border: 1px dashed var(--border);
  border-radius: 18px;
  background: #f8fafc;
  transition: border-color 0.2s ease, transform 0.2s ease;
}

.cover-uploader :deep(.el-upload:hover) {
  border-color: var(--el-color-primary);
  transform: translateY(-1px);
}

.cover-preview {
  display: block;
  width: 160px;
  height: 160px;
}

.cover-uploader-placeholder {
  font-size: 42px;
  line-height: 1;
  color: #8aa0af;
}

.cover-upload-meta {
  display: flex;
  flex: 1;
  min-width: 220px;
  flex-direction: column;
  gap: 6px;
}

.cover-upload-title {
  font-size: 15px;
  font-weight: 600;
  color: #1f2937;
}

.cover-upload-tip {
  font-size: 12px;
  color: #8aa0af;
}

.cover-upload-actions {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.cover-upload-status {
  font-size: 12px;
  color: #8aa0af;
}

.form-actions {
  display: flex;
  gap: 8px;
}

.inline-block {
  display: inline-block;
}
</style>
