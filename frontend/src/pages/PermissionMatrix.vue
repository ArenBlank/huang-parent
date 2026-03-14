<template>
  <div class="card">
    <div class="toolbar">
      <div>
        <h2>Permission Matrix</h2>
        <p>Required vs configured permissions</p>
      </div>
      <el-button type="primary" @click="loadMatrix" :loading="loading">Refresh</el-button>
    </div>
    <div class="stat-grid" v-if="matrix">
      <StatCard label="Required" :value="matrix.required?.length || 0" />
      <StatCard label="Config" :value="matrix.config?.length || 0" />
      <StatCard label="Database" :value="matrix.database?.length || 0" />
      <StatCard label="Missing" :value="matrix.missing?.length || 0" :note="matrix.warning || ''" />
    </div>
    <el-divider />
    <div class="matrix">
      <div class="matrix-block">
        <h4>Missing</h4>
        <el-tag v-for="item in matrix?.missing || []" :key="item" type="danger" class="tag-item">
          {{ item }}
        </el-tag>
      </div>
      <div class="matrix-block">
        <h4>Required</h4>
        <el-tag v-for="item in matrix?.required || []" :key="item" class="tag-item">
          {{ item }}
        </el-tag>
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { adminClient } from '../api/client'
import StatCard from '../components/StatCard.vue'

const matrix = ref(null)
const loading = ref(false)

const loadMatrix = async () => {
  try {
    loading.value = true
    const { data } = await adminClient.get('/admin/permission/matrix')
    if (data.code !== 200) throw new Error(data.message || 'Load failed')
    matrix.value = data.data
  } catch (err) {
    ElMessage.error(err.message || 'Load failed')
  } finally {
    loading.value = false
  }
}

onMounted(loadMatrix)
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

.matrix {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
  gap: 16px;
}

.matrix-block {
  background: rgba(255, 255, 255, 0.04);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 12px;
  padding: 12px;
}

.tag-item {
  margin: 4px 6px 4px 0;
}
</style>
