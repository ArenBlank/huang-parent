<template>
  <div class="card">
    <div class="toolbar">
      <div>
        <h2>角色权限分配</h2>
        <p>基于权限码的资源级 / 操作级分配</p>
      </div>
      <el-button type="primary" @click="refreshAll" :loading="loading">刷新</el-button>
    </div>

    <div class="summary-grid">
      <div class="summary-card">
        <div class="summary-label">角色数</div>
        <div class="summary-value">{{ roles.length }}</div>
        <div class="summary-sub">已选择 {{ roleId || '-' }}</div>
      </div>
      <div class="summary-card">
        <div class="summary-label">权限总数</div>
        <div class="summary-value">{{ permissions.length }}</div>
        <div class="summary-sub">已勾选 {{ selectedPermCodes.length }}</div>
      </div>
      <div class="summary-card">
        <div class="summary-label">模块数</div>
        <div class="summary-value">{{ groupedPerms.length }}</div>
        <div class="summary-sub">筛选后 {{ filteredPermissions.length || permissions.length }}</div>
      </div>
    </div>

    <div class="filters">
      <el-select
        v-model="roleId"
        placeholder="选择角色"
        filterable
        style="width: 220px"
        @change="loadRolePerms"
      >
        <el-option
          v-for="role in roles"
          :key="role.id"
          :label="`${role.roleName} (${role.roleCode})`"
          :value="role.id"
        />
      </el-select>
      <el-select v-model="operation" placeholder="操作模式" style="width: 140px">
        <el-option label="替换" value="replace" />
        <el-option label="追加" value="add" />
        <el-option label="移除" value="remove" />
      </el-select>
      <el-input
        v-model="keyword"
        placeholder="搜索权限码 / 名称"
        style="width: 240px"
        clearable
      />
      <el-button size="small" @click="applyFilter">筛选</el-button>
      <el-button size="small" @click="selectAllFiltered" :disabled="!permissions.length">全选可见</el-button>
      <el-button size="small" @click="clearSelection" :disabled="!selectedPermCodes.length">清空选中</el-button>
      <el-button size="small" @click="exportPerms" :disabled="!roleId">导出权限</el-button>
      <el-button size="small" @click="openImport">导入权限</el-button>
    </div>

    <el-alert
      type="info"
      show-icon
      :closable="false"
      title="提示"
      description="替换：以当前勾选覆盖；追加/移除：仅对选中权限执行增删。"
      style="margin-bottom: 12px;"
    />

    <div v-if="groupedPerms.length === 0" class="empty">
      <el-empty description="暂无权限数据" />
    </div>
    <div v-else class="perm-grid">
      <div class="perm-group" v-for="group in groupedPerms" :key="group.module">
        <div class="group-title">
          <span>{{ group.module || 'default' }}</span>
          <el-button size="small" @click="toggleGroup(group, true)">全选</el-button>
          <el-button size="small" @click="toggleGroup(group, false)">清空</el-button>
        </div>
        <el-checkbox-group v-model="selectedPermCodes">
          <el-checkbox
            v-for="perm in group.items"
            :key="perm.permCode"
            :label="perm.permCode"
          >
            {{ perm.permName }} ({{ perm.permCode }})
          </el-checkbox>
        </el-checkbox-group>
      </div>
    </div>

    <div class="footer">
      <el-button type="primary" :disabled="!roleId" :loading="saving" @click="save">
        保存权限
      </el-button>
    </div>
  </div>

  <el-dialog v-model="importVisible" title="导入权限(JSON)" width="520px">
    <el-input
      v-model="importText"
      type="textarea"
      :rows="8"
      placeholder='{"roleId":1,"permCodes":["role:read"],"operation":"replace"}'
    />
    <template #footer>
      <el-button @click="importVisible = false">取消</el-button>
      <el-button type="primary" @click="applyImport">导入</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { computed, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { adminClient } from '../api/client'

const loading = ref(false)
const saving = ref(false)

const roles = ref([])
const permissions = ref([])
const roleId = ref(null)
const selectedPermCodes = ref([])
const operation = ref('replace')
const keyword = ref('')

const importVisible = ref(false)
const importText = ref('')

const filteredPermissions = ref([])

const groupedPerms = computed(() => {
  const groups = {}
  const list = filteredPermissions.value.length ? filteredPermissions.value : permissions.value
  list.forEach((perm) => {
    const module = perm.module || 'default'
    if (!groups[module]) {
      groups[module] = []
    }
    groups[module].push(perm)
  })
  return Object.keys(groups).map((module) => ({
    module,
    items: groups[module]
  }))
})

const refreshAll = async () => {
  loading.value = true
  await Promise.all([loadRoles(), loadPermissions()])
  if (!roleId.value && roles.value.length) {
    roleId.value = pickDefaultRoleId()
  }
  await loadRolePerms()
  loading.value = false
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

const loadRoles = async () => {
  try {
    const { data } = await adminClient.get('/admin/role/list')
    if (data.code !== 200) throw new Error(data.message || '加载失败')
    roles.value = data.data || []
    if (!roleId.value && roles.value.length) {
      roleId.value = pickDefaultRoleId()
    }
  } catch (err) {
    ElMessage.error(err.message || '加载失败')
  }
}

const loadPermissions = async () => {
  try {
    const { data } = await adminClient.get('/admin/role-permission/permissions')
    if (data.code !== 200) throw new Error(data.message || '加载失败')
    permissions.value = data.data || []
    filteredPermissions.value = []
  } catch (err) {
    ElMessage.error(err.message || '加载失败')
  }
}

const loadRolePerms = async () => {
  if (!roleId.value) return
  try {
    const { data } = await adminClient.get(`/admin/role-permission/role/${roleId.value}/permissions`)
    if (data.code !== 200) throw new Error(data.message || '加载失败')
    selectedPermCodes.value = (data.data || []).map((perm) => perm.permCode)
  } catch (err) {
    ElMessage.error(err.message || '加载失败')
  }
}

const applyFilter = () => {
  const key = keyword.value?.trim().toLowerCase()
  if (!key) {
    filteredPermissions.value = []
    return
  }
  filteredPermissions.value = permissions.value.filter((perm) => {
    const name = (perm.permName || '').toLowerCase()
    const code = (perm.permCode || '').toLowerCase()
    return name.includes(key) || code.includes(key)
  })
}

const selectAllFiltered = () => {
  const list = filteredPermissions.value.length ? filteredPermissions.value : permissions.value
  const codes = list.map((perm) => perm.permCode)
  selectedPermCodes.value = Array.from(new Set([...selectedPermCodes.value, ...codes]))
}

const clearSelection = () => {
  selectedPermCodes.value = []
}

const toggleGroup = (group, checked) => {
  const codes = group.items.map((item) => item.permCode)
  if (checked) {
    selectedPermCodes.value = Array.from(new Set([...selectedPermCodes.value, ...codes]))
  } else {
    selectedPermCodes.value = selectedPermCodes.value.filter((code) => !codes.includes(code))
  }
}

const save = async () => {
  if (!roleId.value) {
    ElMessage.warning('请选择角色')
    return
  }
  try {
    saving.value = true
    const { data } = await adminClient.post('/admin/role-permission/assign', {
      roleId: roleId.value,
      permCodes: selectedPermCodes.value,
      operation: operation.value
    })
    if (data.code !== 200) throw new Error(data.message || '保存失败')
    ElMessage.success('权限已更新')
  } catch (err) {
    ElMessage.error(err.message || '保存失败')
  } finally {
    saving.value = false
  }
}

const exportPerms = () => {
  if (!roleId.value) {
    ElMessage.warning('请选择角色')
    return
  }
  const payload = {
    roleId: roleId.value,
    permCodes: selectedPermCodes.value,
    operation: 'replace'
  }
  const text = JSON.stringify(payload, null, 2)
  if (navigator.clipboard?.writeText) {
    navigator.clipboard.writeText(text)
    ElMessage.success('已复制到剪贴板')
  } else {
    importText.value = text
    importVisible.value = true
  }
}

const openImport = () => {
  importText.value = ''
  importVisible.value = true
}

const applyImport = async () => {
  try {
    const payload = JSON.parse(importText.value || '{}')
    if (!payload.roleId || !Array.isArray(payload.permCodes)) {
      throw new Error('格式错误：需要 roleId 和 permCodes')
    }
    const { data } = await adminClient.post('/admin/role-permission/assign', {
      roleId: payload.roleId,
      permCodes: payload.permCodes,
      operation: payload.operation || 'replace'
    })
    if (data.code !== 200) throw new Error(data.message || '导入失败')
    ElMessage.success('导入成功')
    if (roleId.value === payload.roleId) {
      await loadRolePerms()
    }
    importVisible.value = false
  } catch (err) {
    ElMessage.error(err.message || '导入失败')
  }
}

refreshAll()
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

.filters {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 12px;
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

.perm-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
  gap: 12px;
}

.perm-group {
  background: rgba(255, 255, 255, 0.04);
  border: 1px solid rgba(255, 255, 255, 0.12);
  border-radius: 12px;
  padding: 12px;
}

.group-title {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
  font-weight: 600;
}

.footer {
  margin-top: 16px;
}
</style>
