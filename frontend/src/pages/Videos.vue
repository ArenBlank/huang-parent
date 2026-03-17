<template>
  <div class="card">
    <div class="toolbar">
      <div>
        <h2>视频素材</h2>
        <p>上传至 MinIO 并登记元数据</p>
      </div>
      <el-button type="primary" @click="loadVideos" :loading="loading">刷新</el-button>
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
      <div class="hint">上传成功会返回 objectPath，用于创建素材。</div>
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
      <el-table-column prop="title" label="标题" />
      <el-table-column label="状态" width="120">
        <template #default="scope">
          <el-tag :type="scope.row.status === 1 ? 'success' : 'info'">
            {{ scope.row.status === 1 ? '启用' : '停用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="minioPath" label="MinIO 路径" />
    </el-table>
  </div>

  <div class="card" style="margin-top: 16px;">
    <h3>新建视频素材</h3>
    <el-form :model="form" label-position="top">
      <el-form-item label="标题">
        <el-input v-model="form.title" />
      </el-form-item>
      <el-form-item label="来源站点">
        <el-input v-model="form.sourceSite" />
      </el-form-item>
      <el-form-item label="来源 URL">
        <el-input v-model="form.sourceUrl" />
      </el-form-item>
      <el-form-item label="许可证类型">
        <el-input v-model="form.licenseType" />
      </el-form-item>
      <el-form-item label="作者">
        <el-input v-model="form.authorName" />
      </el-form-item>
      <el-form-item label="时长（秒）">
        <el-input v-model.number="form.durationSec" />
      </el-form-item>
      <el-form-item label="标签">
        <el-input v-model="form.tags" />
      </el-form-item>
      <el-form-item label="MinIO 路径">
        <el-input v-model="form.minioPath" placeholder="从上传返回值填写" />
      </el-form-item>
      <el-form-item label="状态">
        <el-select v-model.number="form.status">
          <el-option label="停用" :value="0" />
          <el-option label="启用" :value="1" />
        </el-select>
      </el-form-item>
      <div class="form-actions">
        <el-button @click="fillSample">填充示例</el-button>
        <el-button type="primary" @click="createAsset" :loading="creating">提交</el-button>
      </div>
    </el-form>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { adminClient } from '../api/client'

const videos = ref([])
const loading = ref(false)
const uploading = ref(false)
const creating = ref(false)
const file = ref(null)
const lastUpload = ref(null)

const form = reactive({
  title: '',
  sourceSite: 'pexels',
  sourceUrl: '',
  licenseType: 'Pexels License',
  authorName: '',
  durationSec: 60,
  tags: '',
  minioPath: '',
  status: 1
})

const loadVideos = async () => {
  try {
    loading.value = true
    const { data } = await adminClient.get('/admin/video/list')
    if (data.code !== 200) throw new Error(data.message || '加载失败')
    videos.value = data.data || []
  } catch (err) {
    ElMessage.error(err.message || '加载失败')
  } finally {
    loading.value = false
  }
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
    form.minioPath = data.data.objectPath
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

const fillSample = () => {
  form.title = '深蹲教学-初级'
  form.sourceSite = 'pexels'
  form.sourceUrl = 'https://www.pexels.com/video/xxxx/'
  form.licenseType = 'Pexels License'
  form.authorName = 'Pexels Author'
  form.durationSec = 60
  form.tags = 'squat,legs,beginner'
  form.status = 1
}

const createAsset = async () => {
  try {
    creating.value = true
    const { data } = await adminClient.post('/admin/video', form)
    if (data.code !== 200) throw new Error(data.message || '创建失败')
    ElMessage.success('素材已创建')
    await loadVideos()
  } catch (err) {
    ElMessage.error(err.message || '创建失败')
  } finally {
    creating.value = false
  }
}

onMounted(loadVideos)
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

.upload-panel {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
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
}
</style>
