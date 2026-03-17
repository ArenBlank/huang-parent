<template>
  <div class="card">
    <div class="toolbar">
      <div>
        <h2>用户管理</h2>
        <p>用户状态与角色分配</p>
      </div>
      <el-button type="primary" @click="loadUsers" :loading="userLoading">刷新</el-button>
    </div>

    <div class="summary-grid">
      <div class="summary-card">
        <div class="summary-label">用户数</div>
        <div class="summary-value">{{ users.length }}</div>
        <div class="summary-sub">启用 {{ activeUserCount }}</div>
      </div>
      <div class="summary-card">
        <div class="summary-label">角色数</div>
        <div class="summary-value">{{ roles.length }}</div>
        <div class="summary-sub">启用 {{ activeRoleCount }}</div>
      </div>
    </div>

    <div class="filters">
      <el-select v-model="userQuery.status" placeholder="状态" clearable style="width: 140px">
        <el-option label="启用" :value="1" />
        <el-option label="禁用" :value="0" />
      </el-select>
      <el-input v-model="userQuery.keyword" placeholder="账号/昵称/手机" style="width: 240px" />
      <el-button size="small" @click="loadUsers" :loading="userLoading">查询</el-button>
    </div>

    <el-table :data="users" style="width: 100%" v-loading="userLoading">
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="username" label="账号" width="140" />
      <el-table-column prop="nickname" label="昵称" width="140" />
      <el-table-column prop="phone" label="手机" width="140" />
      <el-table-column prop="userType" label="类型" width="120" />
      <el-table-column prop="status" label="状态" width="90">
        <template #default="scope">
          <el-tag :type="scope.row.status === 1 ? 'success' : 'info'">
            {{ scope.row.status === 1 ? '启用' : '禁用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="220">
        <template #default="scope">
          <el-button size="small" @click="openAssignDrawer(scope.row)">分配角色</el-button>
          <el-button size="small" type="warning" @click="toggleUserStatus(scope.row)">
            {{ scope.row.status === 1 ? '禁用' : '启用' }}
          </el-button>
        </template>
      </el-table-column>
    </el-table>
  </div>

  <div class="card" style="margin-top: 16px;">
    <div class="toolbar">
      <div>
        <h2>角色管理</h2>
        <p>角色状态维护</p>
      </div>
      <el-button type="primary" @click="loadRoles" :loading="roleLoading">刷新</el-button>
    </div>

    <div class="filters">
      <el-select v-model="roleQuery.status" placeholder="状态" clearable style="width: 140px">
        <el-option label="启用" :value="1" />
        <el-option label="禁用" :value="0" />
      </el-select>
      <el-button size="small" @click="loadRoles" :loading="roleLoading">查询</el-button>
    </div>

    <el-table :data="roles" style="width: 100%" v-loading="roleLoading">
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="roleName" label="角色名称" />
      <el-table-column prop="roleCode" label="角色编码" width="160" />
      <el-table-column prop="status" label="状态" width="90">
        <template #default="scope">
          <el-tag :type="scope.row.status === 1 ? 'success' : 'info'">
            {{ scope.row.status === 1 ? '启用' : '禁用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="160">
        <template #default="scope">
          <el-button size="small" type="warning" @click="toggleRoleStatus(scope.row)">
            {{ scope.row.status === 1 ? '禁用' : '启用' }}
          </el-button>
        </template>
      </el-table-column>
    </el-table>
  </div>

  <el-drawer v-model="assignVisible" title="分配角色" size="30%">
    <div v-if="currentUser">
      <div class="assign-info">
        <div>用户：{{ currentUser.username }}</div>
        <div>昵称：{{ currentUser.nickname || '-' }}</div>
      </div>
      <el-form label-position="top">
        <el-form-item label="角色列表">
          <el-select v-model="selectedRoleIds" multiple filterable style="width: 100%">
            <el-option
              v-for="role in availableRoles"
              :key="role.id"
              :label="`${role.roleName} (${role.roleCode})`"
              :value="role.id"
            />
          </el-select>
        </el-form-item>
        <div class="form-actions">
          <el-button @click="fillDemoRoles">填充示例</el-button>
          <el-button type="primary" @click="saveUserRoles" :loading="assignLoading">保存</el-button>
        </div>
      </el-form>
    </div>
  </el-drawer>
</template>

<script setup>
import { computed, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { adminClient } from '../api/client'

const users = ref([])
const userLoading = ref(false)
const userQuery = reactive({
  status: null,
  keyword: ''
})

const roles = ref([])
const roleLoading = ref(false)
const roleQuery = reactive({
  status: null
})

const availableRoles = ref([])
const assignVisible = ref(false)
const assignLoading = ref(false)
const currentUser = ref(null)
const selectedRoleIds = ref([])

const activeUserCount = computed(() => users.value.filter((item) => item.status === 1).length)
const activeRoleCount = computed(() => roles.value.filter((item) => item.status === 1).length)

const loadUsers = async () => {
  try {
    userLoading.value = true
    const { data } = await adminClient.get('/admin/user/list', {
      params: {
        status: userQuery.status ?? undefined,
        keyword: userQuery.keyword || undefined
      }
    })
    if (data.code !== 200) throw new Error(data.message || '加载失败')
    users.value = data.data || []
  } catch (err) {
    ElMessage.error(err.message || '加载失败')
  } finally {
    userLoading.value = false
  }
}

const loadRoles = async () => {
  try {
    roleLoading.value = true
    const { data } = await adminClient.get('/admin/role/list', {
      params: { status: roleQuery.status ?? undefined }
    })
    if (data.code !== 200) throw new Error(data.message || '加载失败')
    roles.value = data.data || []
  } catch (err) {
    ElMessage.error(err.message || '加载失败')
  } finally {
    roleLoading.value = false
  }
}

const loadAvailableRoles = async () => {
  const { data } = await adminClient.get('/admin/role/available')
  if (data.code !== 200) throw new Error(data.message || '加载失败')
  availableRoles.value = data.data || []
}

const toggleUserStatus = async (row) => {
  try {
    const status = row.status === 1 ? 0 : 1
    const { data } = await adminClient.put('/admin/user/status', {
      userId: row.id,
      status
    })
    if (data.code !== 200) throw new Error(data.message || '更新失败')
    ElMessage.success('状态已更新')
    await loadUsers()
  } catch (err) {
    ElMessage.error(err.message || '更新失败')
  }
}

const toggleRoleStatus = async (row) => {
  try {
    const status = row.status === 1 ? 0 : 1
    const { data } = await adminClient.put('/admin/role/status', {
      roleId: row.id,
      status
    })
    if (data.code !== 200) throw new Error(data.message || '更新失败')
    ElMessage.success('状态已更新')
    await loadRoles()
  } catch (err) {
    ElMessage.error(err.message || '更新失败')
  }
}

const openAssignDrawer = async (row) => {
  try {
    currentUser.value = row
    if (!availableRoles.value.length) {
      await loadAvailableRoles()
    }
    const { data } = await adminClient.get(`/admin/user-role/user/${row.id}/roles`)
    if (data.code !== 200) throw new Error(data.message || '加载失败')
    selectedRoleIds.value = (data.data || []).map((item) => item.roleId)
    assignVisible.value = true
  } catch (err) {
    ElMessage.error(err.message || '加载失败')
  }
}

const fillDemoRoles = () => {
  if (availableRoles.value.length) {
    selectedRoleIds.value = availableRoles.value.slice(0, 2).map((item) => item.id)
  }
}

const saveUserRoles = async () => {
  if (!currentUser.value) return
  try {
    assignLoading.value = true
    const { data } = await adminClient.post('/admin/user/assign-roles', {
      userId: currentUser.value.id,
      roleIds: selectedRoleIds.value
    })
    if (data.code !== 200) throw new Error(data.message || '保存失败')
    ElMessage.success('角色已更新')
    assignVisible.value = false
  } catch (err) {
    ElMessage.error(err.message || '保存失败')
  } finally {
    assignLoading.value = false
  }
}

loadUsers()
loadRoles()
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

.assign-info {
  margin-bottom: 12px;
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
