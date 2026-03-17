<template>
  <div class="card">
    <div class="toolbar">
      <div>
        <h2>权限中心</h2>
        <p>权限矩阵、角色权限与资源范围的统一入口</p>
      </div>
      <el-button type="primary" @click="loadAll" :loading="loading">刷新</el-button>
    </div>

    <div class="stat-grid">
      <StatCard label="必需权限" :value="matrix?.required?.length || 0" />
      <StatCard label="已配置权限" :value="matrix?.config?.length || 0" />
      <StatCard label="数据库权限" :value="matrix?.database?.length || 0" />
      <div class="clickable" @click="openMissing">
        <StatCard
          label="缺失权限"
          :value="matrix?.missing?.length || 0"
          :note="matrix?.warning || ''"
        />
      </div>
      <StatCard label="角色数" :value="roles.length" />
      <StatCard label="权限总数" :value="permissions.length" />
      <StatCard label="未分配权限" :value="unassignedCount" />
      <StatCard label="覆盖率" :value="coverageRate" note="已配置 / 必需" />
    </div>

    <div class="fix-panel">
      <el-select
        v-model="fixRoleId"
        placeholder="选择角色用于一键修复"
        filterable
        style="width: 240px"
      >
        <el-option
          v-for="role in roles"
          :key="role.id"
          :label="`${role.roleName} (${role.roleCode})`"
          :value="role.id"
        />
      </el-select>
      <el-button type="primary" @click="fixPermissions" :loading="fixing">
        一键修复权限缺口
      </el-button>
      <el-button type="success" @click="demoInit" :loading="demoing">
        一键演示初始化
      </el-button>
      <el-button type="warning" @click="runDemoFlow" :loading="demoFlowing">
        一键闭环演示
      </el-button>
      <el-button @click="go('/role-permission')">去角色权限分配</el-button>
    </div>

    <div class="action-row">
      <el-button @click="syncOnly" :loading="syncingOnly">同步缺失权限</el-button>
      <el-button @click="go('/permission-matrix')">查看权限矩阵</el-button>
      <el-button @click="go('/role-scope')">角色分类范围</el-button>
      <el-button @click="go('/user-roles')">用户与角色</el-button>
      <el-button @click="copySummary">复制权限摘要</el-button>
    </div>
    <div v-if="demoHint" class="demo-hint">{{ demoHint }}</div>
  </div>

  <div class="card" style="margin-top: 16px;">
    <div class="section">
      <h3>快速入口</h3>
      <p class="section-sub">把权限闭环跑起来只需要几步</p>
      <div class="quick-grid">
        <div class="quick-card" @click="go('/permission-matrix')">
          <div class="quick-title">权限矩阵</div>
          <div class="quick-sub">查看缺失并一键同步</div>
        </div>
        <div class="quick-card" @click="go('/role-permission')">
          <div class="quick-title">角色权限分配</div>
          <div class="quick-sub">替换 / 追加 / 移除权限</div>
        </div>
        <div class="quick-card" @click="go('/role-scope')">
          <div class="quick-title">角色分类范围</div>
          <div class="quick-sub">课程分类的可见范围</div>
        </div>
        <div class="quick-card" @click="go('/user-roles')">
          <div class="quick-title">用户与角色</div>
          <div class="quick-sub">分配角色与禁用账号</div>
        </div>
      </div>
    </div>
  </div>

  <div class="card" style="margin-top: 16px;">
    <el-collapse v-model="collapseActive">
      <el-collapse-item title="演示流程提示" name="demo">
        <p class="section-sub">适合面试或作品集演示的最短路径</p>
        <ol class="demo-list">
          <li>选择角色（建议使用 root_admin 或管理员角色）。</li>
          <li>点击“一键修复权限缺口”，自动同步并追加缺失权限。</li>
          <li>进入“角色权限分配”查看权限已写入。</li>
          <li>进入“权限修复日志”确认审计记录生成。</li>
        </ol>
      </el-collapse-item>
      <el-collapse-item title="权限健康度说明" name="health">
        <ul class="health-list">
          <li>缺失权限：接口声明需要但系统未登记的权限码，需先同步。</li>
          <li>未分配权限：权限已存在但未赋给任何角色，用户访问会被拦截。</li>
          <li>覆盖率：已配置权限 / 必需权限的比例，越高越安全可用。</li>
        </ul>
      </el-collapse-item>
    </el-collapse>
  </div>

  <div class="card" style="margin-top: 16px;">
    <div class="toolbar">
      <div>
        <h3>权限修复日志</h3>
        <p class="section-sub">同步与分配相关操作最近记录</p>
      </div>
      <div class="summary" v-if="lastFixSummary">{{ lastFixSummary }}</div>
      <el-button size="small" @click="loadLogs" :loading="logLoading">刷新</el-button>
    </div>
    <el-table :data="logs" style="width: 100%" v-loading="logLoading">
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="module" label="模块" width="140" />
      <el-table-column prop="action" label="动作" width="140" />
      <el-table-column prop="detail" label="详情" />
      <el-table-column prop="operatorId" label="操作者" width="110" />
      <el-table-column prop="success" label="结果" width="90">
        <template #default="scope">
          <el-tag :type="scope.row.success === 1 ? 'success' : 'danger'">
            {{ scope.row.success === 1 ? '成功' : '失败' }}
          </el-tag>
        </template>
      </el-table-column>
    </el-table>
  </div>

  <el-dialog v-model="missingVisible" title="缺失权限详情" width="620px">
    <div v-if="!matrix?.missing?.length">
      <el-empty description="暂无缺失权限" />
    </div>
    <div v-else class="missing-list">
      <el-tag v-for="perm in matrix.missing" :key="perm" type="danger" class="tag-item">
        {{ perm }}
      </el-tag>
    </div>
    <template #footer>
      <el-button @click="missingVisible = false">关闭</el-button>
      <el-button type="primary" @click="go('/role-permission')">去分配</el-button>
    </template>
  </el-dialog>

  <el-dialog v-model="demoFlowVisible" title="演示闭环完成" width="520px">
    <div class="demo-flow-summary">
      <div>{{ demoFlowResult }}</div>
      <div class="muted">建议继续查看“角色权限分配”和“权限修复日志”。</div>
    </div>
    <template #footer>
      <el-button @click="demoFlowVisible = false">关闭</el-button>
      <el-button type="primary" @click="go('/operation-logs')">查看日志</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { adminClient } from '../api/client'
import StatCard from '../components/StatCard.vue'

const router = useRouter()
const loading = ref(false)
const matrix = ref(null)
const roles = ref([])
const permissions = ref([])
const fixRoleId = ref(null)
const fixing = ref(false)
const demoing = ref(false)
const demoFlowing = ref(false)
const demoHint = ref('')
const logs = ref([])
const logLoading = ref(false)
const missingVisible = ref(false)
const syncingOnly = ref(false)
const lastFixSummary = ref('')
const collapseActive = ref(['demo'])
const demoFlowVisible = ref(false)
const demoFlowResult = ref('')

const unassignedCount = computed(() => {
  if (!permissions.value.length) return 0
  const assigned = new Set(matrix.value?.config || [])
  return permissions.value.filter((perm) => !assigned.has(perm.permCode)).length
})

const coverageRate = computed(() => {
  const required = matrix.value?.required?.length || 0
  const configured = matrix.value?.config?.length || 0
  if (!required) return '0%'
  return `${Math.min(100, Math.round((configured / required) * 100))}%`
})

const go = (path) => {
  router.push(path)
}

const loadAll = async () => {
  try {
    loading.value = true
    const [matrixRes, roleRes, permRes] = await Promise.all([
      adminClient.get('/admin/permission/matrix'),
      adminClient.get('/admin/role/list'),
      adminClient.get('/admin/role-permission/permissions')
    ])
    if (matrixRes.data.code !== 200) throw new Error(matrixRes.data.message || '矩阵加载失败')
    if (roleRes.data.code !== 200) throw new Error(roleRes.data.message || '角色加载失败')
    if (permRes.data.code !== 200) throw new Error(permRes.data.message || '权限加载失败')
    matrix.value = matrixRes.data.data
    roles.value = roleRes.data.data || []
    permissions.value = permRes.data.data || []
    await loadLogs()
  } catch (err) {
    ElMessage.error(err.message || '加载失败')
  } finally {
    loading.value = false
  }
}

const loadLogs = async () => {
  try {
    logLoading.value = true
    const [permMatrixRes, rolePermRes] = await Promise.all([
      adminClient.get('/admin/operation-log/list', { params: { module: 'permission_matrix', limit: 20 } }),
      adminClient.get('/admin/operation-log/list', { params: { module: 'role_permission', limit: 20 } })
    ])
    if (permMatrixRes.data.code !== 200 || rolePermRes.data.code !== 200) {
      throw new Error('日志加载失败')
    }
    const merged = [...(permMatrixRes.data.data || []), ...(rolePermRes.data.data || [])]
    merged.sort((a, b) => (b.id || 0) - (a.id || 0))
    logs.value = merged.slice(0, 20)
    const latest = logs.value[0]
    if (latest) {
      lastFixSummary.value = `最近一次：${latest.module || '-'} / ${latest.action || '-'} / ${latest.success === 1 ? '成功' : '失败'}`
    }
  } catch (err) {
    ElMessage.error(err.message || '日志加载失败')
  } finally {
    logLoading.value = false
  }
}

const openMissing = () => {
  missingVisible.value = true
}

const fixPermissions = async () => {
  if (!fixRoleId.value) {
    ElMessage.warning('请选择角色')
    return
  }
  try {
    fixing.value = true
    const syncRes = await adminClient.post('/admin/permission/matrix/sync')
    if (syncRes.data.code !== 200) throw new Error(syncRes.data.message || '同步失败')
    const missing = syncRes.data.data || matrix.value?.missing || []
    if (!missing.length) {
      ElMessage.success('没有缺失权限')
      await loadAll()
      return
    }
    const assignRes = await adminClient.post('/admin/role-permission/assign', {
      roleId: fixRoleId.value,
      permCodes: missing,
      operation: 'add'
    })
    if (assignRes.data.code !== 200) throw new Error(assignRes.data.message || '追加失败')
    ElMessage.success(`已补齐 ${missing.length} 个权限`)
    lastFixSummary.value = `刚刚修复：同步 ${missing.length} 个权限并追加到角色 ${fixRoleId.value}`
    demoHint.value = `本次修复角色：${fixRoleId.value}，追加权限 ${missing.length} 项`
    await loadAll()
  } catch (err) {
    ElMessage.error(err.message || '修复失败')
  } finally {
    fixing.value = false
  }
}

const pickDemoRole = () => {
  if (!roles.value.length) return null
  const adminRole = roles.value.find((role) => {
    const code = (role.roleCode || '').toLowerCase()
    const name = (role.roleName || '').toLowerCase()
    return code.includes('admin') || name.includes('管理员') || name.includes('admin')
  })
  return adminRole || roles.value[0]
}

const demoInit = async () => {
  try {
    demoing.value = true
    const role = pickDemoRole()
    if (!role) {
      ElMessage.warning('没有可用角色')
      return
    }
    fixRoleId.value = role.id
    await fixPermissions()
    if (fixRoleId.value) {
      demoHint.value = `演示初始化完成：角色 ${fixRoleId.value}`
    }
  } catch (err) {
    ElMessage.error(err.message || '演示初始化失败')
  } finally {
    demoing.value = false
  }
}

const syncOnly = async () => {
  try {
    syncingOnly.value = true
    const syncRes = await adminClient.post('/admin/permission/matrix/sync')
    if (syncRes.data.code !== 200) throw new Error(syncRes.data.message || '同步失败')
    const count = syncRes.data.data?.length || 0
    ElMessage.success(`已同步 ${count} 个权限`)
    lastFixSummary.value = `刚刚同步：${count} 个权限`
    await loadAll()
  } catch (err) {
    ElMessage.error(err.message || '同步失败')
  } finally {
    syncingOnly.value = false
  }
}

const copySummary = async () => {
  const payload = {
    required: matrix.value?.required?.length || 0,
    config: matrix.value?.config?.length || 0,
    database: matrix.value?.database?.length || 0,
    missing: matrix.value?.missing?.length || 0,
    roles: roles.value.length,
    permissions: permissions.value.length,
    coverage: coverageRate.value
  }
  const text = JSON.stringify(payload, null, 2)
  try {
    await navigator.clipboard.writeText(text)
    ElMessage.success('已复制权限摘要')
  } catch (err) {
    ElMessage.error('复制失败')
  }
}

const runDemoFlow = async () => {
  try {
    demoFlowing.value = true
    const role = pickDemoRole()
    if (!role) {
      ElMessage.warning('没有可用角色')
      return
    }
    fixRoleId.value = role.id
    await fixPermissions()
    await loadLogs()
    demoFlowResult.value = `闭环演示完成：角色 ${role.id} 已补齐权限，并生成日志记录。`
    demoFlowVisible.value = true
  } catch (err) {
    ElMessage.error(err.message || '演示闭环失败')
  } finally {
    demoFlowing.value = false
  }
}

loadAll()
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

.section h3 {
  margin: 0 0 4px;
}

.section-sub {
  margin: 0 0 12px;
  color: #8aa0af;
  font-size: 12px;
}

.quick-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 12px;
}

.quick-card {
  padding: 16px;
  border-radius: 14px;
  border: 1px dashed rgba(45, 212, 191, 0.4);
  background: rgba(45, 212, 191, 0.08);
  cursor: pointer;
  transition: transform 0.2s ease;
}

.quick-card:hover {
  transform: translateY(-2px);
}

.quick-title {
  font-weight: 600;
  margin-bottom: 6px;
}

.quick-sub {
  font-size: 12px;
  color: #9bb0bd;
}

.fix-panel {
  margin-top: 16px;
  display: flex;
  gap: 12px;
  align-items: center;
}

.action-row {
  margin-top: 12px;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.demo-hint {
  margin-top: 10px;
  font-size: 12px;
  color: #9bb0bd;
}

.health-list {
  margin: 0;
  padding-left: 18px;
  color: #9bb0bd;
  font-size: 13px;
  line-height: 1.7;
}

.demo-list {
  margin: 0;
  padding-left: 18px;
  color: #9bb0bd;
  font-size: 13px;
  line-height: 1.7;
}

.summary {
  font-size: 12px;
  color: #9bb0bd;
}

.clickable {
  cursor: pointer;
}

.missing-list {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.demo-flow-summary {
  font-size: 13px;
  color: #4b5563;
}

.muted {
  color: #9bb0bd;
  font-size: 12px;
  margin-top: 8px;
}
</style>
