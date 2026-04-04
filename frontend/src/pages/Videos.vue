<template>
  <div class="card page-card">
    <div class="toolbar">
      <div>
        <h2>视频素材</h2>
        <p>上传、登记、上下架、删除、绑定训练计划项</p>
      </div>
      <div class="toolbar-actions">
        <el-button @click="openPanel('library')">素材库</el-button>
        <el-button @click="startCreate">新建素材</el-button>
        <el-button @click="openPanel('binding')">计划项绑定</el-button>
        <el-button type="primary" @click="loadVideos" :loading="loading">刷新</el-button>
      </div>
    </div>

    <div class="page-summary">
      <div class="summary-card">
        <div class="summary-label">素材总数</div>
        <div class="summary-value">{{ videos.length }}</div>
      </div>
      <div class="summary-card">
        <div class="summary-label">当前模式</div>
        <div class="summary-value summary-text">{{ editingId ? `编辑 #${editingId}` : '新建素材' }}</div>
      </div>
      <div class="summary-card">
        <div class="summary-label">最近上传</div>
        <div class="summary-value summary-text">{{ lastUpload?.objectPath || '暂无' }}</div>
      </div>
    </div>

    <el-collapse v-model="activePanel" accordion class="video-collapse">
      <el-collapse-item name="library">
        <template #title>
          <div class="collapse-title">
            <span>素材库</span>
            <span class="collapse-sub">查询、上传、浏览已有素材</span>
          </div>
        </template>

        <div ref="librarySection" class="panel-block">
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
            <div class="upload-main">
              <el-upload :auto-upload="false" :show-file-list="true" :on-change="handleFile">
                <el-button type="primary">选择文件</el-button>
              </el-upload>
              <el-button type="warning" :disabled="!file" @click="uploadVideo" :loading="uploading">上传</el-button>
              <span class="hint">上传成功后，MinIO 路径会自动带到“新建素材”里。</span>
            </div>

            <div v-if="lastUpload" class="upload-result compact">
              <div class="result-title">最近上传</div>
              <div class="result-row">路径：{{ lastUpload.objectPath }}</div>
              <div class="result-row">大小：{{ lastUpload.size }} bytes</div>
              <div class="result-row">类型：{{ lastUpload.contentType }}</div>
              <div class="result-actions">
                <el-button size="small" @click="copyPath">复制路径</el-button>
                <el-button size="small" type="primary" plain @click="startCreate">去新建素材</el-button>
              </div>
            </div>
          </div>

          <el-table :data="videos" style="width: 100%" v-loading="loading" max-height="420">
            <el-table-column prop="id" label="ID" width="70" />
            <el-table-column prop="title" label="标题" min-width="180" />
            <el-table-column prop="sourceSite" label="来源站点" width="120" />
            <el-table-column prop="authorName" label="作者" width="140" />
            <el-table-column prop="durationSec" label="时长" width="90" />
            <el-table-column prop="minioPath" label="MinIO 路径" min-width="260" show-overflow-tooltip />
            <el-table-column label="状态" width="90">
              <template #default="{ row }">
                <el-tag :type="row.status === 1 ? 'success' : 'info'">
                  {{ row.status === 1 ? '启用' : '停用' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="署名" width="90">
              <template #default="{ row }">
                {{ formatAttribution(row.attributionRequired) }}
              </template>
            </el-table-column>
            <el-table-column label="操作" width="380" fixed="right">
              <template #default="{ row }">
                <el-button size="small" @click="openEditor(row)">编辑</el-button>
                <el-button size="small" type="warning" @click="toggleStatus(row)">
                  {{ row.status === 1 ? '停用' : '启用' }}
                </el-button>
                <el-button size="small" type="danger" plain @click="deleteVideo(row)">删除</el-button>
                <el-button size="small" type="primary" plain @click="prefillBind(row)">绑定计划项</el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </el-collapse-item>

      <el-collapse-item name="editor">
        <template #title>
          <div class="collapse-title">
            <span>{{ editingId ? `编辑视频素材 #${editingId}` : '新建视频素材' }}</span>
            <span class="collapse-sub">上传后的路径会自动回填到这里</span>
          </div>
        </template>

        <div ref="editorSection" class="panel-block">
          <div class="editor-header">
            <div class="editor-tip">
              <span v-if="videoForm.minioPath">当前已回填 MinIO 路径</span>
              <span v-else>可以先去“素材库”上传文件，再回来创建素材。</span>
            </div>
            <div class="form-actions">
              <el-button @click="fillSample">填充示例</el-button>
              <el-button v-if="editingId" @click="resetVideoForm">切回新建</el-button>
              <el-button type="primary" @click="saveVideoAsset" :loading="saving">
                {{ editingId ? '保存修改' : '创建素材' }}
              </el-button>
            </div>
          </div>

          <el-form :model="videoForm" label-position="top" class="editor-form">
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
            <el-form-item label="MinIO 路径" class="form-span-2">
              <el-input v-model="videoForm.minioPath" placeholder="从上传返回值粘贴，或自动回填" />
            </el-form-item>
            <el-form-item label="状态">
              <el-select v-model.number="videoForm.status">
                <el-option label="停用" :value="0" />
                <el-option label="启用" :value="1" />
              </el-select>
            </el-form-item>
          </el-form>
        </div>
      </el-collapse-item>

      <el-collapse-item name="binding">
        <template #title>
          <div class="collapse-title">
            <span>计划项绑定</span>
            <span class="collapse-sub">把视频绑到训练计划里的某一个动作项</span>
          </div>
        </template>

        <div ref="bindingSection" class="panel-block">
          <div class="binding-toolbar">
            <div class="editor-tip">这里绑定的是“计划项 ID”，不是整套训练计划 ID。</div>
            <el-button size="small" @click="prefillBind({ id: videoFormIdHint })">带入当前编辑视频</el-button>
          </div>

          <div class="binding-grid">
            <div class="binding-card">
              <div class="binding-title">绑定计划项</div>
              <el-form :model="bindForm" label-position="top">
                <el-form-item label="计划项 ID">
                  <el-input v-model.number="bindForm.planItemId" placeholder="输入训练计划项 ID" />
                </el-form-item>
                <el-form-item label="视频 ID">
                  <el-input v-model.number="bindForm.videoId" placeholder="输入视频素材 ID" />
                </el-form-item>
                <div class="form-actions">
                  <el-button @click="fillBindSample">填充示例</el-button>
                  <el-button type="primary" @click="bindPlanItem" :loading="binding">绑定计划项</el-button>
                </div>
              </el-form>
            </div>

            <div class="binding-card">
              <div class="binding-title">解绑计划项</div>
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
          </div>
        </div>
      </el-collapse-item>
    </el-collapse>
  </div>
</template>

<script setup>
import { computed, nextTick, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
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
const activePanel = ref('library')

const librarySection = ref(null)
const editorSection = ref(null)
const bindingSection = ref(null)

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

const scrollToSection = async (panel) => {
  await nextTick()
  const map = {
    library: librarySection.value,
    editor: editorSection.value,
    binding: bindingSection.value
  }
  map[panel]?.scrollIntoView({ behavior: 'smooth', block: 'start' })
}

const openPanel = async (panel) => {
  activePanel.value = panel
  await scrollToSection(panel)
}

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
    ElMessage.success('上传成功，MinIO 路径已自动回填')
    await openPanel('editor')
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

const resetVideoForm = async () => {
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
    minioPath: lastUpload.value?.objectPath || '',
    status: 1
  })
  await openPanel('editor')
}

const startCreate = async () => {
  await resetVideoForm()
}

const openEditor = async (row) => {
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
  await openPanel('editor')
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
    minioPath: videoForm.minioPath || lastUpload.value?.objectPath || '',
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
    await resetVideoForm()
    await loadVideos()
    await openPanel('library')
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
    if (data.code !== 200) throw new Error(data.message || '状态更新失败')
    ElMessage.success('状态已更新')
    await loadVideos()
  } catch (err) {
    ElMessage.error(err.message || '状态更新失败')
  }
}

const deleteVideo = async (row) => {
  if (!row?.id) return
  try {
    await ElMessageBox.confirm(
      `确定删除视频素材 #${row.id}${row.title ? ` - ${row.title}` : ''} 吗？`,
      '删除确认',
      {
        type: 'warning',
        confirmButtonText: '删除',
        cancelButtonText: '取消'
      }
    )
    const { data } = await adminClient.delete(`/admin/video/${row.id}`)
    if (data.code !== 200) throw new Error(data.message || '删除失败')
    if (editingId.value === row.id) {
      await resetVideoForm()
    }
    ElMessage.success('删除成功')
    await loadVideos()
  } catch (err) {
    if (err === 'cancel' || err === 'close') return
    ElMessage.error(err.message || '删除失败')
  }
}

const prefillBind = async (row) => {
  if (!row?.id) return
  bindForm.videoId = row.id
  await openPanel('binding')
}

const fillBindSample = () => {
  bindForm.planItemId = 1
  bindForm.videoId = videoFormIdHint.value
}

const bindPlanItem = async () => {
  if (!bindForm.planItemId || !bindForm.videoId) {
    ElMessage.warning('请先填写计划项 ID 和视频 ID')
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
    ElMessage.warning('请先填写计划项 ID')
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
.page-card {
  overflow: hidden;
}

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

.toolbar-actions {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.page-summary {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  gap: 12px;
  margin-bottom: 16px;
}

.summary-card {
  padding: 12px 14px;
  border-radius: 14px;
  border: 1px solid var(--border);
  background: #ffffffcc;
}

.summary-label {
  font-size: 12px;
  color: #8aa0af;
}

.summary-value {
  margin-top: 8px;
  font-size: 24px;
  font-weight: 700;
}

.summary-text {
  font-size: 14px;
  font-weight: 600;
  word-break: break-all;
}

.video-collapse {
  border-top: none;
}

.collapse-title {
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding: 4px 0;
}

.collapse-sub {
  font-size: 12px;
  color: #8aa0af;
}

.panel-block {
  padding-top: 8px;
}

.filters {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 16px;
}

.upload-panel {
  display: grid;
  grid-template-columns: minmax(320px, 1fr) minmax(260px, 360px);
  gap: 12px;
  align-items: start;
  margin-bottom: 16px;
}

.upload-main {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
  min-height: 84px;
  padding: 12px 14px;
  border-radius: 14px;
  border: 1px dashed var(--border);
  background: #f8fbfd;
}

.hint {
  font-size: 12px;
  color: #8aa0af;
}

.upload-result {
  padding: 12px 14px;
  border-radius: 12px;
  border: 1px dashed var(--border);
  background: #ffffffcc;
}

.upload-result.compact {
  margin-bottom: 0;
}

.result-title {
  font-weight: 600;
  margin-bottom: 6px;
}

.result-row {
  font-size: 12px;
  color: #6b7280;
  margin-bottom: 4px;
  word-break: break-all;
}

.result-actions {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  margin-top: 10px;
}

.editor-header,
.binding-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
  flex-wrap: wrap;
}

.editor-tip {
  font-size: 12px;
  color: #64748b;
}

.editor-form {
  display: grid;
  grid-template-columns: repeat(2, minmax(240px, 1fr));
  gap: 0 16px;
}

.form-span-2 {
  grid-column: 1 / -1;
}

.binding-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(280px, 1fr));
  gap: 16px;
}

.binding-card {
  padding: 16px;
  border-radius: 14px;
  border: 1px solid var(--border);
  background: #ffffffcc;
}

.binding-title {
  font-size: 15px;
  font-weight: 600;
  margin-bottom: 12px;
}

.form-actions {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

@media (max-width: 1100px) {
  .upload-panel,
  .binding-grid,
  .editor-form {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  .toolbar,
  .editor-header,
  .binding-toolbar {
    align-items: flex-start;
  }
}
</style>
