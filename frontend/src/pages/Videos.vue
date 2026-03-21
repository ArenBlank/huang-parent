<template>
  <div class="card">
    <div class="toolbar">
      <div>
        <h2>视频素材</h2>
        <p>上传、登记、更新、上下架、绑定计划项</p>
      </div>
      <el-button type="primary" @click="loadVideos" :loading="loading">刷新</el-button>
    </div>

    <div class="filters">
      <el-input v-model="query.keyword" placeholder="关键词" style="width: 220px" clearable />
      <el-select v-model="query.status" placeholder="状态筛选" clearable style="width: 140px">
        <el-option label="启用" :value="1" />
        <el-option label="停用" :value="0" />
      </el-select>
      <el-button size="small" @click="loadVideos" :loading="loading">查询</el-button>
      <el-button size="small" @click="clearQuery">清空</el-button>
    </div>

    <div class="upload-panel">
      <el-upload
        :auto-upload="false"
        :show-file-list="true"
        :on-change="handleFile"
      >
        <el-button type="primary">选择文件</el-button>
      </el-upload>
      <el-button type="warning" :disabled="!file" @click="uploadVideo" :loading="uploading">上传</el-button>
      <div class="hint">上传成功会返回 objectPath，用于创建或更新素材。</div>
    </div>

    <div v-if="lastUpload" class="upload-result">
      <div class="result-title">最近上传</div>
      <div class="result-row">路径：{{ lastUpload.objectPath }}</div>
      <div class="result-row">大小：{{ lastUpload.size }} bytes</div>
      <div class="result-row">类型：{{ lastUpload.contentType }}</div>
      <el-button size="small" @click="copyPath">复制路径</el-button>
    </div>

    <el-table :data="videos" style="width: 100%" v-loading="loading">
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="title" label="标题" min-width="180" />
      <el-table-column prop="sourceSite" label="来源站点" width="120" />
      <el-table-column prop="authorName" label="作者" width="120" />
      <el-table-column prop="durationSec" label="时长" width="90" />
      <el-table-column prop="minioPath" label="MinIO 路径" min-width="220" />
      <el-table-column label="状态" width="90">
        <template #default="scope">
          <el-tag :type="scope.row.status === 1 ? 'success' : 'info'">
            {{ scope.row.status === 1 ? '启用' : '停用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="署名" width="90">
        <template #default="scope">
          {{ formatAttribution(scope.row.attributionRequired) }}
        </template>
      </el-table-column>
      <el-table-column label="操作" width="300">
        <template #default="scope">
          <el-button size="small" @click="openEditor(scope.row)">编辑</el-button>
          <el-button size="small" type="warning" @click="toggleStatus(scope.row)">
            {{ scope.row.status === 1 ? '停用' : '启用' }}
          </el-button>
          <el-button size="small" type="primary" plain @click="prefillBind(scope.row)">绑定计划项</el-button>
        </template>
      </el-table-column>
    </el-table>
  </div>

  <div class="card" style="margin-top: 16px;">
    <div class="toolbar">
      <div>
        <h3>{{ editingId ? `编辑视频素材 #${editingId}` : '新建视频素材' }}</h3>
        <p>保存后会立即回到列表刷新</p>
      </div>
      <div class="form-actions">
        <el-button @click="fillSample">填充示例</el-button>
        <el-button v-if="editingId" @click="startCreate">切回新建</el-button>
        <el-button type="primary" @click="saveVideoAsset" :loading="saving">
          {{ editingId ? '保存修改' : '创建素材' }}
        </el-button>
      </div>
    </div>

    <el-form :model="videoForm" label-position="top">
      <el-form-item label="标题">
        <el-input v-model="videoForm.title" />
      </el-form-item>
      <el-form-item label="来源站点">
        <el-input v-model="videoForm.sourceSite" />
      </el-form-item>
      <el-form-item label="来源 URL">
        <el-input v-model="videoForm.sourceUrl" />
      </el-form-item>
      <el-form-item label="许可证类型">
        <el-input v-model="videoForm.licenseType" />
      </el-form-item>
      <el-form-item label="是否要求署名">
        <el-select v-model.number="videoForm.attributionRequired">
          <el-option label="否" :value="0" />
          <el-option label="是" :value="1" />
        </el-select>
      </el-form-item>
      <el-form-item label="作者">
        <el-input v-model="videoForm.authorName" />
      </el-form-item>
      <el-form-item label="时长（秒）">
        <el-input v-model.number="videoForm.durationSec" />
      </el-form-item>
      <el-form-item label="标签">
        <el-input v-model="videoForm.tags" />
      </el-form-item>
      <el-form-item label="MinIO 路径">
        <el-input v-model="videoForm.minioPath" placeholder="从上传返回值填写" />
      </el-form-item>
      <el-form-item label="状态">
        <el-select v-model.number="videoForm.status">
          <el-option label="停用" :value="0" />
          <el-option label="启用" :value="1" />
        </el-select>
      </el-form-item>
    </el-form>
  </div>

  <div class="card" style="margin-top: 16px;">
    <div class="toolbar">
      <div>
        <h3>计划项绑定</h3>
        <p>只做现有接口的直接联动</p>
      </div>
      <el-button size="small" @click="prefillBind({ id: videoFormIdHint })">用当前编辑视频</el-button>
    </div>

    <el-form :model="bindForm" label-position="top">
      <el-form-item label="计划项 ID">
        <el-input v-model.number="bindForm.planItemId" placeholder="输入训练计划项 ID" />
      </el-form-item>
      <el-form-item label="视频 ID">
        <el-input v-model.number="bindForm.videoId" placeholder="输入视频 ID" />
      </el-form-item>
      <div class="form-actions">
        <el-button @click="fillBindSample">填充示例</el-button>
        <el-button type="primary" @click="bindPlanItem" :loading="binding">绑定计划项</el-button>
      </div>
    </el-form>

    <el-divider />

    <el-form :model="unbindForm" label-position="top">
      <el-form-item label="待解绑计划项 ID">
        <el-input v-model.number="unbindForm.planItemId" placeholder="输入计划项 ID" />
      </el-form-item>
      <div class="form-actions">
        <el-button @click="fillUnbindSample">填充示例</el-button>
        <el-button type="warning" @click="unbindPlanItem" :loading="unbinding">解绑计划项</el-button>
      </div>
    </el-form>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { adminClient } from '../api/client'

const videos = ref([])
const loading = ref(false)
const uploading = ref(false)
const saving = ref(false)
const binding = ref(false)
const unbinding = ref(false)
const file = ref(null)
const lastUpload = ref(null)
const editingId = ref(null)
const videoFormIdHint = computed(() => editingId.value || videos.value[0]?.id || 1)

const query = reactive({
  keyword: '',
  status: ''
})

const videoForm = reactive({
  title: '',
  sourceSite: 'pexels',
  sourceUrl: '',
  licenseType: 'Pexels License',
  attributionRequired: 0,
  authorName: '',
  durationSec: 60,
  tags: '',
  minioPath: '',
  status: 1
})

const bindForm = reactive({
  planItemId: '',
  videoId: ''
})

const unbindForm = reactive({
  planItemId: ''
})

const loadVideos = async () => {
  try {
    loading.value = true
    const { data } = await adminClient.get('/admin/video/list', {
      params: {
        status: query.status === '' ? undefined : query.status,
        keyword: query.keyword?.trim() || undefined
      }
    })
    if (data.code !== 200) throw new Error(data.message || '加载失败')
    videos.value = data.data || []
  } catch (err) {
    ElMessage.error(err.message || '加载失败')
  } finally {
    loading.value = false
  }
}

const clearQuery = () => {
  query.keyword = ''
  query.status = ''
  loadVideos()
}

const handleFile = (uploadFile) => {
  file.value = uploadFile.raw
}

const uploadVideo = async () => {
  if (!file.value) return
  try {
    uploading.value = true
    const formData = new FormData()
    formData.append('file', file.value)
    const { data } = await adminClient.post('/admin/video/upload', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
    if (data.code !== 200) throw new Error(data.message || '上传失败')
    videoForm.minioPath = data.data.objectPath
    lastUpload.value = data.data
    ElMessage.success('上传成功')
  } catch (err) {
    ElMessage.error(err.message || '上传失败')
  } finally {
    uploading.value = false
  }
}

const copyPath = async () => {
  if (!lastUpload.value?.objectPath) return
  try {
    await navigator.clipboard.writeText(lastUpload.value.objectPath)
    ElMessage.success('路径已复制')
  } catch (err) {
    ElMessage.error('复制失败')
  }
}

const resetVideoForm = () => {
  editingId.value = null
  Object.assign(videoForm, {
    title: '',
    sourceSite: 'pexels',
    sourceUrl: '',
    licenseType: 'Pexels License',
    attributionRequired: 0,
    authorName: '',
    durationSec: 60,
    tags: '',
    minioPath: '',
    status: 1
  })
}

const startCreate = () => {
  resetVideoForm()
}

const openEditor = (row) => {
  editingId.value = row.id
  Object.assign(videoForm, {
    title: row.title || '',
    sourceSite: row.sourceSite || 'pexels',
    sourceUrl: row.sourceUrl || '',
    licenseType: row.licenseType || 'Pexels License',
    attributionRequired: row.attributionRequired ?? 0,
    authorName: row.authorName || '',
    durationSec: row.durationSec ?? 60,
    tags: row.tags || '',
    minioPath: row.minioPath || '',
    status: row.status ?? 1
  })
}

const fillSample = () => {
  Object.assign(videoForm, {
    title: '深蹲教学-初级',
    sourceSite: 'pexels',
    sourceUrl: 'https://www.pexels.com/video/xxxx/',
    licenseType: 'Pexels License',
    attributionRequired: 0,
    authorName: 'Pexels Author',
    durationSec: 60,
    tags: 'squat,legs,beginner',
    status: 1
  })
}

const saveVideoAsset = async () => {
  try {
    saving.value = true
    const payload = { ...videoForm }
    let data
    if (editingId.value) {
      ;({ data } = await adminClient.put(`/admin/video/${editingId.value}`, payload))
    } else {
      ;({ data } = await adminClient.post('/admin/video', payload))
    }
    if (data.code !== 200) throw new Error(data.message || '保存失败')
    ElMessage.success(editingId.value ? '素材已更新' : '素材已创建')
    resetVideoForm()
    await loadVideos()
  } catch (err) {
    ElMessage.error(err.message || '保存失败')
  } finally {
    saving.value = false
  }
}

const toggleStatus = async (row) => {
  try {
    const status = row.status === 1 ? 0 : 1
    const { data } = await adminClient.put(`/admin/video/${row.id}/status`, null, {
      params: { status }
    })
    if (data.code !== 200) throw new Error(data.message || '更新失败')
    ElMessage.success('状态已更新')
    await loadVideos()
  } catch (err) {
    ElMessage.error(err.message || '更新失败')
  }
}

const prefillBind = (row) => {
  if (!row?.id) return
  bindForm.videoId = row.id
  if (!bindForm.planItemId) {
    bindForm.planItemId = ''
  }
}

const fillBindSample = () => {
  bindForm.planItemId = 1
  bindForm.videoId = videoFormIdHint.value
}

const bindPlanItem = async () => {
  if (!bindForm.planItemId || !bindForm.videoId) {
    ElMessage.warning('请先填写计划项ID和视频ID')
    return
  }
  try {
    binding.value = true
    const { data } = await adminClient.put('/admin/video/bind-plan-item', {
      planItemId: Number(bindForm.planItemId),
      videoId: Number(bindForm.videoId)
    })
    if (data.code !== 200) throw new Error(data.message || '绑定失败')
    ElMessage.success('绑定成功')
  } catch (err) {
    ElMessage.error(err.message || '绑定失败')
  } finally {
    binding.value = false
  }
}

const fillUnbindSample = () => {
  unbindForm.planItemId = 1
}

const unbindPlanItem = async () => {
  if (!unbindForm.planItemId) {
    ElMessage.warning('请先填写计划项ID')
    return
  }
  try {
    unbinding.value = true
    const { data } = await adminClient.put(`/admin/video/unbind-plan-item/${Number(unbindForm.planItemId)}`)
    if (data.code !== 200) throw new Error(data.message || '解绑失败')
    ElMessage.success('解绑成功')
  } catch (err) {
    ElMessage.error(err.message || '解绑失败')
  } finally {
    unbinding.value = false
  }
}

const formatAttribution = (value) => {
  return Number(value) === 1 ? '是' : '否'
}

onMounted(loadVideos)
</script>

<style scoped>
.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  gap: 12px;
}

.toolbar h2,
.toolbar h3 {
  margin: 0 0 6px;
}

.toolbar p {
  margin: 0;
  color: #8aa0af;
  font-size: 13px;
}

.filters {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 16px;
}

.upload-panel {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
  flex-wrap: wrap;
}

.hint {
  font-size: 12px;
  color: #8aa0af;
}

.upload-result {
  padding: 12px;
  border-radius: 12px;
  border: 1px dashed var(--border);
  background: #ffffffcc;
  margin-bottom: 16px;
}

.result-title {
  font-weight: 600;
  margin-bottom: 6px;
}

.result-row {
  font-size: 12px;
  color: #6b7280;
  margin-bottom: 4px;
}

.form-actions {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}
</style>
