<template>
  <div class="section-stack">
    <section class="card surface-peach">
      <div class="page-heading">
        <div>
          <div class="eyebrow">Access Academy</div>
          <h2>权限学院</h2>
          <p>把权限矩阵、角色分配、缺口修复和审计记录做成一组更直观的学院任务卡。</p>
        </div>
        <div class="toolbar-actions">
          <span class="code-pill">FIX · SYNC · DEMO</span>
          <el-button type="primary" :loading="loading" @click="loadAll">刷新学院数据</el-button>
        </div>
      </div>

      <div class="stat-grid">
        <StatCard label="必需权限" :value="matrix?.required?.length || 0" note="系统声明需要的权限数量" />
        <StatCard label="已配置权限" :value="matrix?.config?.length || 0" note="已经接入矩阵的权限" />
        <StatCard label="缺失权限" :value="matrix?.missing?.length || 0" :note="missingNote" @click="openMissing" />
        <StatCard label="覆盖率" :value="coverageRate" note="已配置 / 必需" />
      </div>
    </section>

    <section class="split-layout">
      <div class="section-stack">
        <div class="card surface-lilac">
          <div class="toolbar">
            <div>
              <div class="section-title-sm">修复工作台</div>
              <div class="section-copy">先选择角色，再执行一键修复、同步或闭环演示。</div>
            </div>
          </div>

          <div class="workbench-grid">
            <div class="workbench-card">
              <div class="info-label">选择演示角色</div>
              <el-select
                v-model="fixRoleId"
                placeholder="选择角色用于一键修复"
                filterable
                style="width: 100%; margin-top: 10px;"
              >
                <el-option
                  v-for="role in roles"
                  :key="role.id"
                  :label="displayRoleLabel(role)"
                  :value="role.id"
                />
              </el-select>
            </div>

            <button type="button" class="action-card" @click="fixPermissions">
              <span>一键修复权限缺口</span>
              <strong>{{ fixing ? '处理中...' : 'Fix Access Gaps' }}</strong>
            </button>
            <button type="button" class="action-card mint" @click="demoInit">
              <span>演示初始化</span>
              <strong>{{ demoing ? '处理中...' : 'Demo Setup' }}</strong>
            </button>
            <button type="button" class="action-card butter" @click="runDemoFlow">
              <span>闭环演示</span>
              <strong>{{ demoFlowing ? '处理中...' : 'Run Demo Flow' }}</strong>
            </button>
          </div>

          <div v-if="demoHint" class="demo-banner">{{ demoHint }}</div>
        </div>

        <div class="card">
          <div class="toolbar">
            <div>
              <div class="section-title-sm">快速入口</div>
              <div class="section-copy">权限学院的几个核心入口都做成可点击任务卡。</div>
            </div>
          </div>
          <div class="quick-grid">
            <button type="button" class="quick-card" @click="go('/permission-matrix')">
              <span>权限矩阵</span>
              <strong>查看缺失并一键同步</strong>
            </button>
            <button type="button" class="quick-card" @click="go('/role-permission')">
              <span>角色权限分配</span>
              <strong>替换 / 追加 / 移除权限</strong>
            </button>
            <button type="button" class="quick-card" @click="go('/role-scope')">
              <span>角色分类范围</span>
              <strong>检查课程分类边界</strong>
            </button>
            <button type="button" class="quick-card" @click="go('/user-roles')">
              <span>用户与角色</span>
              <strong>查看账号归属与禁用状态</strong>
            </button>
          </div>
        </div>
      </div>

      <div class="section-stack">
        <div class="card surface-mint">
          <div class="section-title-sm">学院健康度</div>
          <div class="info-list" style="margin-top: 14px;">
            <div class="info-row">
              <div class="info-label">角色数</div>
              <div class="info-value">{{ roles.length }}</div>
            </div>
            <div class="info-row">
              <div class="info-label">权限总数</div>
              <div class="info-value">{{ permissions.length }}</div>
            </div>
            <div class="info-row">
              <div class="info-label">未分配权限</div>
              <div class="info-value">{{ unassignedCount }}</div>
            </div>
            <div class="info-row">
              <div class="info-label">最近摘要</div>
              <div class="info-value wrap">{{ lastFixSummary || '暂无最近修复记录' }}</div>
            </div>
          </div>
          <div class="toolbar-actions" style="margin-top: 16px;">
            <el-button @click="syncOnly" :loading="syncingOnly">同步缺失权限</el-button>
            <el-button @click="copySummary">复制权限摘要</el-button>
          </div>
        </div>

        <div class="card">
          <div class="section-title-sm">演示流程提示</div>
          <ol class="list-block">
            <li>选择管理员角色。</li>
            <li>执行一键修复权限缺口。</li>
            <li>进入角色权限分配页查看结果。</li>
            <li>在下方日志区确认审计记录生成。</li>
          </ol>
        </div>
      </div>
    </section>

    <section class="card">
      <div class="toolbar">
        <div>
          <div class="section-title-sm">权限修复日志</div>
          <div class="section-copy">同步与分配相关操作最近记录，继续保留真实数据能力。</div>
        </div>
        <el-button size="small" :loading="logLoading" @click="loadLogs">刷新日志</el-button>
      </div>
      <div class="table-shell">
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
    </section>

    <el-dialog v-model="missingVisible" title="缺失权限详情" width="620px">
      <div v-if="!matrix?.missing?.length">
        <el-empty description="暂无缺失权限" />
      </div>
      <div v-else class="missing-list">
        <el-tag v-for="perm in matrix.missing" :key="perm" type="danger">
          {{ displayPermissionCodeLabel(perm) }}
        </el-tag>
      </div>
      <template #footer>
        <el-button @click="missingVisible = false">关闭</el-button>
        <el-button type="primary" @click="go('/role-permission')">去分配</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="demoFlowVisible" title="演示闭环完成" width="520px">
      <div class="section-copy" style="margin-top: 0;">{{ demoFlowResult }}</div>
      <template #footer>
        <el-button @click="demoFlowVisible = false">关闭</el-button>
        <el-button type="primary" @click="go('/operation-logs')">查看日志</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { adminClient } from '../api/client'
import { displayPermissionCodeLabel, displayRoleLabel } from '../utils/rbacDisplay'
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

const missingNote = computed(() => {
  const count = matrix.value?.missing?.length || 0
  if (!count) {
    return '当前权限矩阵无缺失项'
  }
  return `点击查看 ${count} 项缺失权限`
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
.workbench-grid,
.quick-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 12px;
  margin-top: 16px;
}

.workbench-card,
.quick-card,
.action-card {
  padding: 18px;
  border-radius: 22px;
  border: 3px solid var(--border);
  background: #fffdf8;
  box-shadow: 0 8px 0 rgba(52, 45, 105, 0.08);
}

.quick-card,
.action-card {
  display: grid;
  gap: 8px;
  text-align: left;
}

.quick-card span,
.action-card span {
  color: var(--text-soft);
  font-size: 11px;
  font-weight: 800;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.quick-card strong,
.action-card strong {
  font-family: 'Fredoka', sans-serif;
  font-size: 18px;
  line-height: 1.45;
}

.action-card.mint {
  background: #ecfff8;
}

.action-card.butter {
  background: #fff6d8;
}

.demo-banner {
  margin-top: 14px;
  padding: 14px 16px;
  border-radius: 18px;
  border: 3px solid var(--border);
  background: #ffffff;
  font-size: 13px;
  font-weight: 700;
}

.list-block {
  margin: 14px 0 0;
  padding-left: 20px;
  color: var(--text-soft);
  line-height: 1.8;
}

.missing-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}
</style>
