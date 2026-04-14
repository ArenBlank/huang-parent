<template>
  <div class="card">
    <div class="toolbar">
      <div>
        <h2>角色分类范围</h2>
        <p>角色可操作的课程分类范围</p>
      </div>
      <el-button type="primary" @click="refreshAll" :loading="loading">刷新</el-button>
    </div>

    <div class="summary-grid">
      <div class="summary-card">
        <div class="summary-label">角色数</div>
        <div class="summary-value">{{ roles.length }}</div>
        <div class="summary-sub">启用 {{ activeRoleCount }}</div>
      </div>
      <div class="summary-card">
        <div class="summary-label">分类数</div>
        <div class="summary-value">{{ categories.length }}</div>
        <div class="summary-sub">可分配范围</div>
      </div>
    </div>

    <el-tabs v-model="activeTab">
      <el-tab-pane label="单角色配置" name="single">
        <el-table :data="roles" style="width: 100%" v-loading="roleLoading">
          <el-table-column prop="id" label="角色ID" width="90" />
          <el-table-column label="角色名称">
            <template #default="scope">
              {{ displayRoleName(scope.row) }}
            </template>
          </el-table-column>
          <el-table-column prop="roleCode" label="角色编码" width="160" />
          <el-table-column prop="status" label="状态" width="90">
            <template #default="scope">
              <el-tag :type="scope.row.status === 1 ? 'success' : 'info'">
                {{ scope.row.status === 1 ? '启用' : '停用' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="160">
            <template #default="scope">
              <el-button size="small" @click="openRoleDrawer(scope.row)">配置范围</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <el-tab-pane label="批量更新" name="batch">
        <div class="batch-form">
          <el-form label-position="top">
            <el-form-item label="选择角色">
              <el-select v-model="batchRoleIds" multiple filterable style="width: 100%">
                <el-option
                  v-for="role in roles"
                  :key="role.id"
                  :label="displayRoleLabel(role)"
                  :value="role.id"
                />
              </el-select>
            </el-form-item>
            <el-form-item label="分类范围">
              <el-checkbox-group v-model="batchCategoryIds">
                <el-checkbox v-for="cat in categories" :key="cat.id" :label="cat.id">
                  {{ cat.name }}
                </el-checkbox>
              </el-checkbox-group>
            </el-form-item>
            <div class="form-actions">
              <el-button @click="fillBatchSample">填充示例</el-button>
              <el-button type="primary" @click="submitBatch" :loading="batchLoading">批量更新</el-button>
            </div>
          </el-form>
        </div>
      </el-tab-pane>
    </el-tabs>
  </div>

  <el-drawer v-model="drawerVisible" title="配置角色范围" size="32%">
    <div v-if="currentRole">
      <div class="role-meta">
        <div>角色：{{ displayRoleLabel(currentRole) }}</div>
      </div>
      <el-form label-position="top">
        <el-form-item label="分类范围">
          <el-checkbox-group v-model="selectedCategoryIds">
            <el-checkbox v-for="cat in categories" :key="cat.id" :label="cat.id">
              {{ cat.name }}
            </el-checkbox>
          </el-checkbox-group>
        </el-form-item>
        <div class="form-actions">
          <el-button @click="fillRoleSample">填充示例</el-button>
          <el-button type="primary" @click="saveScope" :loading="saving">保存</el-button>
        </div>
      </el-form>
    </div>
  </el-drawer>
</template>

<script setup>
import { computed, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { adminClient } from '../api/client'
import { displayRoleLabel, displayRoleName } from '../utils/rbacDisplay'

const activeTab = ref('single')
const loading = ref(false)

const roles = ref([])
const categories = ref([])
const roleLoading = ref(false)
const categoryLoading = ref(false)

const drawerVisible = ref(false)
const currentRole = ref(null)
const selectedCategoryIds = ref([])
const saving = ref(false)

const batchRoleIds = ref([])
const batchCategoryIds = ref([])
const batchLoading = ref(false)

const activeRoleCount = computed(() => roles.value.filter((item) => item.status === 1).length)

const refreshAll = async () => {
  loading.value = true
  await Promise.all([loadRoles(), loadCategories()])
  loading.value = false
}

const loadRoles = async () => {
  try {
    roleLoading.value = true
    const { data } = await adminClient.get('/admin/role/list')
    if (data.code !== 200) throw new Error(data.message || '加载失败')
    roles.value = data.data || []
  } catch (err) {
    ElMessage.error(err.message || '加载失败')
  } finally {
    roleLoading.value = false
  }
}

const loadCategories = async () => {
  try {
    categoryLoading.value = true
    const { data } = await adminClient.get('/admin/course-category/list', { params: { status: 1 } })
    if (data.code !== 200) throw new Error(data.message || '加载失败')
    categories.value = data.data || []
  } catch (err) {
    ElMessage.error(err.message || '加载失败')
  } finally {
    categoryLoading.value = false
  }
}

const openRoleDrawer = async (role) => {
  try {
    currentRole.value = role
    const { data } = await adminClient.get(`/admin/role/${role.id}/course-category-scope`)
    if (data.code !== 200) throw new Error(data.message || '加载失败')
    selectedCategoryIds.value = data.data || []
    drawerVisible.value = true
  } catch (err) {
    ElMessage.error(err.message || '加载失败')
  }
}

const fillRoleSample = () => {
  if (!categories.value.length) return
  selectedCategoryIds.value = categories.value.slice(0, 2).map((item) => item.id)
}

const saveScope = async () => {
  if (!currentRole.value) return
  try {
    saving.value = true
    const { data } = await adminClient.put(`/admin/role/${currentRole.value.id}/course-category-scope`, {
      categoryIds: selectedCategoryIds.value
    })
    if (data.code !== 200) throw new Error(data.message || '保存失败')
    ElMessage.success('已保存')
    drawerVisible.value = false
  } catch (err) {
    ElMessage.error(err.message || '保存失败')
  } finally {
    saving.value = false
  }
}

const fillBatchSample = () => {
  if (!roles.value.length || !categories.value.length) return
  batchRoleIds.value = roles.value.slice(0, 2).map((item) => item.id)
  batchCategoryIds.value = categories.value.slice(0, 2).map((item) => item.id)
}

const submitBatch = async () => {
  if (!batchRoleIds.value.length) {
    ElMessage.warning('请选择角色')
    return
  }
  try {
    batchLoading.value = true
    const items = batchRoleIds.value.map((roleId) => ({
      roleId,
      categoryIds: batchCategoryIds.value
    }))
    const { data } = await adminClient.put('/admin/role/course-category-scope/batch', { items })
    if (data.code !== 200) throw new Error(data.message || '批量更新失败')
    ElMessage.success('批量更新成功')
  } catch (err) {
    ElMessage.error(err.message || '批量更新失败')
  } finally {
    batchLoading.value = false
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

.role-meta {
  margin-bottom: 12px;
  color: #8aa0af;
  font-size: 13px;
}

.batch-form {
  max-width: 640px;
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
