<template>
  <div class="card">
    <div class="toolbar">
      <div>
        <h2>Video Assets</h2>
        <p>Upload to MinIO and register metadata</p>
      </div>
      <el-button type="primary" @click="loadVideos" :loading="loading">Refresh</el-button>
    </div>

    <div class="upload-panel">
      <el-upload
        :auto-upload="false"
        :show-file-list="true"
        :on-change="handleFile"
      >
        <el-button type="primary">Select File</el-button>
      </el-upload>
      <el-button type="warning" :disabled="!file" @click="uploadVideo" :loading="uploading">Upload</el-button>
      <div class="hint">Upload returns objectPath used for metadata creation.</div>
    </div>

    <el-table :data="videos" style="width: 100%" v-loading="loading">
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="title" label="Title" />
      <el-table-column prop="status" label="Status" width="120" />
      <el-table-column prop="minioPath" label="MinIO Path" />
    </el-table>
  </div>

  <div class="card" style="margin-top: 16px;">
    <h3>Create Video Asset</h3>
    <el-form :model="form" label-position="top">
      <el-form-item label="Title">
        <el-input v-model="form.title" />
      </el-form-item>
      <el-form-item label="Source Site">
        <el-input v-model="form.sourceSite" />
      </el-form-item>
      <el-form-item label="Source URL">
        <el-input v-model="form.sourceUrl" />
      </el-form-item>
      <el-form-item label="License Type">
        <el-input v-model="form.licenseType" />
      </el-form-item>
      <el-form-item label="Author">
        <el-input v-model="form.authorName" />
      </el-form-item>
      <el-form-item label="Duration (sec)">
        <el-input v-model.number="form.durationSec" />
      </el-form-item>
      <el-form-item label="Tags">
        <el-input v-model="form.tags" />
      </el-form-item>
      <el-form-item label="MinIO Path">
        <el-input v-model="form.minioPath" placeholder="Fill from upload response" />
      </el-form-item>
      <el-form-item label="Status">
        <el-select v-model.number="form.status">
          <el-option label="Inactive" :value="0" />
          <el-option label="Active" :value="1" />
        </el-select>
      </el-form-item>
      <el-button type="primary" @click="createAsset" :loading="creating">Submit</el-button>
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
    if (data.code !== 200) throw new Error(data.message || 'Load failed')
    videos.value = data.data || []
  } catch (err) {
    ElMessage.error(err.message || 'Load failed')
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
    if (data.code !== 200) throw new Error(data.message || 'Upload failed')
    form.minioPath = data.data.objectPath
    ElMessage.success('Upload success')
  } catch (err) {
    ElMessage.error(err.message || 'Upload failed')
  } finally {
    uploading.value = false
  }
}

const createAsset = async () => {
  try {
    creating.value = true
    const { data } = await adminClient.post('/admin/video', form)
    if (data.code !== 200) throw new Error(data.message || 'Create failed')
    ElMessage.success('Video asset created')
    await loadVideos()
  } catch (err) {
    ElMessage.error(err.message || 'Create failed')
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
</style>
