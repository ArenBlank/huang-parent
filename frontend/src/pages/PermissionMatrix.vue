<template>
  <div class="card">
    <div class="toolbar">
      <div>
        <h2>权限矩阵</h2>
        <p>必需权限 vs 已配置权限</p>
      </div>
      <div class="toolbar-actions">
        <el-button @click="syncMissing" :loading="syncing">同步缺失权限</el-button>
        <el-button type="primary" @click="loadMatrix" :loading="loading">刷新</el-button>
      </div>
    </div>
    <div class="stat-grid" v-if="matrix">
      <StatCard label="必需" :value="matrix.required?.length || 0" />
      <StatCard label="配置" :value="matrix.config?.length || 0" />
      <StatCard label="数据库" :value="matrix.database?.length || 0" />
      <StatCard label="缺失" :value="matrix.missing?.length || 0" :note="missingNote" />
    </div>
    <div class="action-row">
      <el-button size="small" @click="copyMissing" :disabled="!(matrix?.missing?.length)">复制缺失权限</el-button>
      <el-button size="small" @click="copyMatrixSummary" :disabled="!matrix">复制矩阵摘要</el-button>
    </div>
    <el-divider />
    <div class="matrix">
    <div class="matrix-block">
      <div class="block-header">
        <h4>缺失</h4>
        <div class="block-actions">
          <el-select v-model="targetRoleId" placeholder="选择角色" filterable style="width: 180px">
            <el-option
              v-for="role in roles"
              :key="role.id"
              :label="displayRoleLabel(role)"
              :value="role.id"
            />
          </el-select>
          <el-button size="small" @click="assignMissing" :loading="assigning">追加到角色</el-button>
          <el-button size="small" type="primary" @click="syncAndAssign" :loading="syncAssigning">
            同步并追加
          </el-button>
        </div>
      </div>
      <el-tag v-for="item in matrix?.missing || []" :key="item" type="danger" class="tag-item">
        {{ displayPermissionCodeLabel(item) }}
      </el-tag>
    </div>
      <div class="matrix-block">
        <h4>必需</h4>
        <el-tag v-for="item in matrix?.required || []" :key="item" class="tag-item">
          {{ displayPermissionCodeLabel(item) }}
        </el-tag>
      </div>
      <div class="matrix-block">
        <h4>已配置</h4>
        <el-tag v-for="item in matrix?.config || []" :key="item" type="success" class="tag-item">
          {{ displayPermissionCodeLabel(item) }}
        </el-tag>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { adminClient } from '../api/client'
import StatCard from '../components/StatCard.vue'
import { displayPermissionCodeLabel, displayRoleLabel } from '../utils/rbacDisplay'

const matrix = ref(null)
const loading = ref(false)
const syncing = ref(false)
const assigning = ref(false)
const syncAssigning = ref(false)
const roles = ref([])
const targetRoleId = ref(null)

const missingNote = computed(() => {
  const count = matrix.value?.missing?.length || 0
  if (!count) {
    return '当前权限矩阵无缺失项'
  }
  return `当前矩阵仍缺少 ${count} 项权限`
})

const loadMatrix = async () => {
  try {
    loading.value = true
    const { data } = await adminClient.get('/admin/permission/matrix')
    if (data.code !== 200) throw new Error(data.message || '加载失败')
    matrix.value = data.data
  } catch (err) {
    ElMessage.error(err.message || '加载失败')
  } finally {
    loading.value = false
  }
}

const loadRoles = async () => {
  try {
    const { data } = await adminClient.get('/admin/role/list')
    if (data.code !== 200) throw new Error(data.message || '加载失败')
    roles.value = data.data || []
    if (!targetRoleId.value && roles.value.length) {
      targetRoleId.value = pickDefaultRoleId()
    }
  } catch (err) {
    ElMessage.error(err.message || '加载失败')
  }
}

const pickDefaultRoleId = () => {
  if (!roles.value.length) return null
  const preferred = roles.value.find((role) => {
    const code = (role.roleCode || '').toLowerCase()
    const name = (role.roleName || '').toLowerCase()
    return code.includes('admin') || name.includes('管理员') || name.includes('admin')
  })
  return (preferred || roles.value[0]).id
}

const assignMissing = async () => {
  if (!targetRoleId.value) {
    ElMessage.warning('请选择角色')
    return
  }
  const missing = matrix.value?.missing || []
  if (!missing.length) {
    ElMessage.warning('暂无缺失权限')
    return
  }
  try {
    assigning.value = true
    const { data } = await adminClient.post('/admin/role-permission/assign', {
      roleId: targetRoleId.value,
      permCodes: missing,
      operation: 'add'
    })
    if (data.code !== 200) throw new Error(data.message || '追加失败')
    ElMessage.success('已追加权限到角色')
  } catch (err) {
    ElMessage.error(err.message || '追加失败')
  } finally {
    assigning.value = false
  }
}

const syncAndAssign = async () => {
  if (!targetRoleId.value) {
    ElMessage.warning('请选择角色')
    return
  }
  try {
    syncAssigning.value = true
    await syncMissing()
    await assignMissing()
  } catch (err) {
    ElMessage.error(err.message || '同步追加失败')
  } finally {
    syncAssigning.value = false
  }
}

onMounted(() => {
  loadMatrix()
  loadRoles()
})

const syncMissing = async () => {
  try {
    syncing.value = true
    const { data } = await adminClient.post('/admin/permission/matrix/sync')
    if (data.code !== 200) throw new Error(data.message || '同步失败')
    ElMessage.success(`已同步 ${data.data?.length || 0} 个权限`)
    await loadMatrix()
  } catch (err) {
    ElMessage.error(err.message || '同步失败')
  } finally {
    syncing.value = false
  }
}

const copyMissing = async () => {
  const list = matrix.value?.missing || []
  if (!list.length) return
  try {
    await navigator.clipboard.writeText(list.join('\n'))
    ElMessage.success('已复制缺失权限')
  } catch (err) {
    ElMessage.error('复制失败')
  }
}

const copyMatrixSummary = async () => {
  if (!matrix.value) return
  const payload = {
    required: matrix.value?.required?.length || 0,
    config: matrix.value?.config?.length || 0,
    database: matrix.value?.database?.length || 0,
    missing: matrix.value?.missing?.length || 0
  }
  try {
    await navigator.clipboard.writeText(JSON.stringify(payload, null, 2))
    ElMessage.success('已复制矩阵摘要')
  } catch (err) {
    ElMessage.error('复制失败')
  }
}
</script>

<style scoped>
.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
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

.action-row {
  margin: 12px 0 6px;
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
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

.block-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  margin-bottom: 8px;
}

.block-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.tag-item {
  margin: 4px 6px 4px 0;
}
</style>
