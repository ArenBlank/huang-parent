<template>
  <div class="card">
    <div class="page-heading">
      <div>
        <div class="eyebrow">Content Studio</div>
        <h2>运营内容</h2>
        <p>Banner、公告和系统配置集中在一个工坊里维护，顶部卡片只保留真正有用的数量信息。</p>
      </div>
      <div class="toolbar-actions">
        <span class="code-pill">BANNER · NOTICE · CONFIG</span>
        <el-button type="primary" @click="refreshAll" :loading="loading">刷新</el-button>
      </div>
    </div>

    <div class="metric-grid content-metrics">
      <div class="metric-card">
        <div class="metric-label">Banner</div>
        <div class="metric-value">{{ banners.length }}</div>
        <div class="metric-note">启用 {{ bannerActiveCount }}</div>
      </div>
      <div class="metric-card">
        <div class="metric-label">公告</div>
        <div class="metric-value">{{ notices.length }}</div>
        <div class="metric-note">启用 {{ noticeActiveCount }}</div>
      </div>
      <div class="metric-card">
        <div class="metric-label">系统配置</div>
        <div class="metric-value">{{ configs.length }}</div>
        <div class="metric-note">当前已配置项</div>
      </div>
    </div>

    <el-tabs v-model="activeTab">
      <el-tab-pane label="Banner" name="banner">
        <div class="tab-toolbar">
          <div class="filters">
            <el-select v-model="bannerStatus" placeholder="状态筛选" clearable style="width: 140px">
              <el-option label="启用" :value="1" />
              <el-option label="停用" :value="0" />
            </el-select>
            <el-button size="small" @click="loadBanners" :loading="bannerLoading">查询</el-button>
          </div>
          <el-button type="primary" @click="openBannerDrawer()">新建 Banner</el-button>
        </div>
        <div class="table-shell">
        <el-table :data="banners" style="width: 100%" v-loading="bannerLoading">
          <el-table-column prop="id" label="ID" width="70" />
          <el-table-column prop="title" label="标题" />
          <el-table-column label="图片" width="160">
            <template #default="scope">
              <el-image
                v-if="scope.row.imageUrl"
                :src="scope.row.imageUrl"
                fit="cover"
                style="width: 120px; height: 54px; border-radius: 8px"
              />
              <span v-else class="muted">无</span>
            </template>
          </el-table-column>
          <el-table-column prop="sort" label="排序" width="90" />
          <el-table-column prop="status" label="状态" width="90">
            <template #default="scope">
              <el-tag :type="scope.row.status === 1 ? 'success' : 'info'">
                {{ scope.row.status === 1 ? '启用' : '停用' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="220">
            <template #default="scope">
              <div class="table-action-row">
                <el-button size="small" @click="openBannerDrawer(scope.row)">编辑</el-button>
                <el-button size="small" type="warning" @click="toggleBanner(scope.row)">
                  {{ scope.row.status === 1 ? '停用' : '启用' }}
                </el-button>
                <el-button size="small" type="danger" @click="deleteBanner(scope.row)">删除</el-button>
              </div>
            </template>
          </el-table-column>
        </el-table>
        </div>
      </el-tab-pane>

      <el-tab-pane label="公告" name="notice">
        <div class="tab-toolbar">
          <div class="filters">
            <el-select v-model="noticeStatus" placeholder="状态筛选" clearable style="width: 140px">
              <el-option label="启用" :value="1" />
              <el-option label="停用" :value="0" />
            </el-select>
            <el-button size="small" @click="loadNotices" :loading="noticeLoading">查询</el-button>
          </div>
          <el-button type="primary" @click="openNoticeDrawer()">新建公告</el-button>
        </div>
        <div class="table-shell">
        <el-table :data="notices" style="width: 100%" v-loading="noticeLoading">
          <el-table-column prop="id" label="ID" width="70" />
          <el-table-column prop="title" label="标题" />
          <el-table-column prop="publishTime" label="发布时间" width="180" />
          <el-table-column prop="status" label="状态" width="90">
            <template #default="scope">
              <el-tag :type="scope.row.status === 1 ? 'success' : 'info'">
                {{ scope.row.status === 1 ? '启用' : '停用' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="220">
            <template #default="scope">
              <div class="table-action-row">
                <el-button size="small" @click="openNoticeDrawer(scope.row)">编辑</el-button>
                <el-button size="small" type="warning" @click="toggleNotice(scope.row)">
                  {{ scope.row.status === 1 ? '停用' : '启用' }}
                </el-button>
                <el-button size="small" type="danger" @click="deleteNotice(scope.row)">删除</el-button>
              </div>
            </template>
          </el-table-column>
        </el-table>
        </div>
      </el-tab-pane>

      <el-tab-pane label="系统配置" name="config">
        <div class="tab-toolbar">
          <div class="filters">
            <el-input v-model="configKey" placeholder="配置键筛选" style="width: 220px" clearable />
            <el-button size="small" @click="loadConfigs" :loading="configLoading">查询</el-button>
          </div>
          <el-button type="primary" @click="openConfigDrawer()">新建配置</el-button>
        </div>
        <div class="table-shell">
        <el-table :data="configs" style="width: 100%" v-loading="configLoading">
          <el-table-column prop="id" label="ID" width="70" />
          <el-table-column prop="configKey" label="配置键" width="200" />
          <el-table-column prop="configValue" label="配置值" />
          <el-table-column prop="remark" label="备注" />
          <el-table-column label="操作" width="160">
            <template #default="scope">
              <div class="table-action-row">
                <el-button size="small" @click="openConfigDrawer(scope.row)">编辑</el-button>
                <el-button size="small" type="danger" @click="deleteConfig(scope.row)">删除</el-button>
              </div>
            </template>
          </el-table-column>
        </el-table>
        </div>
      </el-tab-pane>
    </el-tabs>
  </div>

  <el-drawer v-model="bannerDrawerVisible" title="Banner" size="32%">
    <el-form :model="bannerForm" label-position="top">
      <el-form-item label="标题">
        <el-input v-model="bannerForm.title" />
      </el-form-item>
      <el-form-item label="图片 URL">
        <el-input v-model="bannerForm.imageUrl" />
      </el-form-item>
      <el-form-item label="链接 URL">
        <el-input v-model="bannerForm.linkUrl" />
      </el-form-item>
      <el-form-item label="排序">
        <el-input v-model.number="bannerForm.sort" />
      </el-form-item>
      <el-form-item label="状态">
        <el-select v-model.number="bannerForm.status">
          <el-option label="启用" :value="1" />
          <el-option label="停用" :value="0" />
        </el-select>
      </el-form-item>
      <div class="drawer-actions">
        <el-button @click="fillBannerSample">填充示例</el-button>
        <el-button type="primary" @click="saveBanner" :loading="bannerSaving">保存</el-button>
      </div>
    </el-form>
  </el-drawer>

  <el-drawer v-model="noticeDrawerVisible" title="公告" size="32%">
    <el-form :model="noticeForm" label-position="top">
      <el-form-item label="标题">
        <el-input v-model="noticeForm.title" />
      </el-form-item>
      <el-form-item label="内容">
        <el-input v-model="noticeForm.content" type="textarea" :rows="4" />
      </el-form-item>
      <el-form-item label="发布时间">
        <el-date-picker
          v-model="noticeForm.publishTime"
          type="datetime"
          value-format="YYYY-MM-DD HH:mm:ss"
          placeholder="可选"
        />
      </el-form-item>
      <el-form-item label="状态">
        <el-select v-model.number="noticeForm.status">
          <el-option label="启用" :value="1" />
          <el-option label="停用" :value="0" />
        </el-select>
      </el-form-item>
      <div class="drawer-actions">
        <el-button @click="fillNoticeSample">填充示例</el-button>
        <el-button type="primary" @click="saveNotice" :loading="noticeSaving">保存</el-button>
      </div>
    </el-form>
  </el-drawer>

  <el-drawer v-model="configDrawerVisible" title="系统配置" size="32%">
    <el-form :model="configForm" label-position="top">
      <el-form-item label="配置键">
        <el-input v-model="configForm.configKey" />
      </el-form-item>
      <el-form-item label="配置值">
        <el-input v-model="configForm.configValue" />
      </el-form-item>
      <el-form-item label="备注">
        <el-input v-model="configForm.remark" />
      </el-form-item>
      <div class="drawer-actions">
        <el-button @click="fillConfigSample">填充示例</el-button>
        <el-button type="primary" @click="saveConfig" :loading="configSaving">保存</el-button>
      </div>
    </el-form>
  </el-drawer>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { adminClient } from '../api/client'

const activeTab = ref('banner')
const loading = ref(false)

const banners = ref([])
const bannerStatus = ref()
const bannerLoading = ref(false)
const bannerSaving = ref(false)
const bannerDrawerVisible = ref(false)
const bannerEditingId = ref(null)
const bannerForm = reactive({
  title: '',
  imageUrl: '',
  linkUrl: '',
  sort: 0,
  status: 1
})

const notices = ref([])
const noticeStatus = ref()
const noticeLoading = ref(false)
const noticeSaving = ref(false)
const noticeDrawerVisible = ref(false)
const noticeEditingId = ref(null)
const noticeForm = reactive({
  title: '',
  content: '',
  publishTime: '',
  status: 1
})

const configs = ref([])
const configKey = ref('')
const configLoading = ref(false)
const configSaving = ref(false)
const configDrawerVisible = ref(false)
const configEditingId = ref(null)
const configForm = reactive({
  configKey: '',
  configValue: '',
  remark: ''
})

const bannerActiveCount = computed(() => banners.value.filter((item) => item.status === 1).length)
const noticeActiveCount = computed(() => notices.value.filter((item) => item.status === 1).length)

const refreshAll = async () => {
  loading.value = true
  await Promise.all([loadBanners(), loadNotices(), loadConfigs()])
  loading.value = false
}

const loadBanners = async () => {
  try {
    bannerLoading.value = true
    const { data } = await adminClient.get('/admin/banner/list', {
      params: { status: bannerStatus.value }
    })
    if (data.code !== 200) throw new Error(data.message || '加载失败')
    banners.value = data.data || []
  } catch (err) {
    ElMessage.error(err.message || '加载失败')
  } finally {
    bannerLoading.value = false
  }
}

const loadNotices = async () => {
  try {
    noticeLoading.value = true
    const { data } = await adminClient.get('/admin/notice/list', {
      params: { status: noticeStatus.value }
    })
    if (data.code !== 200) throw new Error(data.message || '加载失败')
    notices.value = data.data || []
  } catch (err) {
    ElMessage.error(err.message || '加载失败')
  } finally {
    noticeLoading.value = false
  }
}

const loadConfigs = async () => {
  try {
    configLoading.value = true
    const { data } = await adminClient.get('/admin/system-config/list', {
      params: { keyLike: configKey.value }
    })
    if (data.code !== 200) throw new Error(data.message || '加载失败')
    configs.value = data.data || []
  } catch (err) {
    ElMessage.error(err.message || '加载失败')
  } finally {
    configLoading.value = false
  }
}

const openBannerDrawer = (row) => {
  if (row) {
    bannerEditingId.value = row.id
    bannerForm.title = row.title || ''
    bannerForm.imageUrl = row.imageUrl || ''
    bannerForm.linkUrl = row.linkUrl || ''
    bannerForm.sort = row.sort ?? 0
    bannerForm.status = row.status ?? 1
  } else {
    bannerEditingId.value = null
    bannerForm.title = ''
    bannerForm.imageUrl = ''
    bannerForm.linkUrl = ''
    bannerForm.sort = 0
    bannerForm.status = 1
  }
  bannerDrawerVisible.value = true
}

const fillBannerSample = () => {
  bannerForm.title = '春季训练活动'
  bannerForm.imageUrl = 'https://images.unsplash.com/photo-1517836357463-d25dfeac3438?w=1200'
  bannerForm.linkUrl = 'https://example.com/activity'
  bannerForm.sort = 1
  bannerForm.status = 1
}

const saveBanner = async () => {
  try {
    bannerSaving.value = true
    const payload = { ...bannerForm }
    let data
    if (bannerEditingId.value) {
      ;({ data } = await adminClient.put(`/admin/banner/${bannerEditingId.value}`, payload))
    } else {
      ;({ data } = await adminClient.post('/admin/banner', payload))
    }
    if (data.code !== 200) throw new Error(data.message || '保存失败')
    ElMessage.success('保存成功')
    bannerDrawerVisible.value = false
    await loadBanners()
  } catch (err) {
    ElMessage.error(err.message || '保存失败')
  } finally {
    bannerSaving.value = false
  }
}

const toggleBanner = async (row) => {
  try {
    const status = row.status === 1 ? 0 : 1
    const { data } = await adminClient.put(`/admin/banner/${row.id}/status`, null, {
      params: { status }
    })
    if (data.code !== 200) throw new Error(data.message || '更新失败')
    ElMessage.success('状态已更新')
    await loadBanners()
  } catch (err) {
    ElMessage.error(err.message || '更新失败')
  }
}

const deleteBanner = async (row) => {
  try {
    await ElMessageBox.confirm('确定删除该 Banner 吗？', '提示', { type: 'warning' })
    const { data } = await adminClient.delete(`/admin/banner/${row.id}`)
    if (data.code !== 200) throw new Error(data.message || '删除失败')
    ElMessage.success('已删除')
    await loadBanners()
  } catch (err) {
    if (err !== 'cancel' && err !== 'close' && err?.message) {
      ElMessage.error(err.message || '删除失败')
    }
  }
}

const openNoticeDrawer = (row) => {
  if (row) {
    noticeEditingId.value = row.id
    noticeForm.title = row.title || ''
    noticeForm.content = row.content || ''
    noticeForm.publishTime = row.publishTime || ''
    noticeForm.status = row.status ?? 1
  } else {
    noticeEditingId.value = null
    noticeForm.title = ''
    noticeForm.content = ''
    noticeForm.publishTime = ''
    noticeForm.status = 1
  }
  noticeDrawerVisible.value = true
}

const fillNoticeSample = () => {
  noticeForm.title = '系统升级公告'
  noticeForm.content = '本周末进行系统升级，期间可能短暂不可用，请提前安排。'
  noticeForm.publishTime = ''
  noticeForm.status = 1
}

const saveNotice = async () => {
  try {
    noticeSaving.value = true
    const payload = { ...noticeForm }
    let data
    if (noticeEditingId.value) {
      ;({ data } = await adminClient.put(`/admin/notice/${noticeEditingId.value}`, payload))
    } else {
      ;({ data } = await adminClient.post('/admin/notice', payload))
    }
    if (data.code !== 200) throw new Error(data.message || '保存失败')
    ElMessage.success('保存成功')
    noticeDrawerVisible.value = false
    await loadNotices()
  } catch (err) {
    ElMessage.error(err.message || '保存失败')
  } finally {
    noticeSaving.value = false
  }
}

const toggleNotice = async (row) => {
  try {
    const status = row.status === 1 ? 0 : 1
    const { data } = await adminClient.put(`/admin/notice/${row.id}/status`, null, {
      params: { status }
    })
    if (data.code !== 200) throw new Error(data.message || '更新失败')
    ElMessage.success('状态已更新')
    await loadNotices()
  } catch (err) {
    ElMessage.error(err.message || '更新失败')
  }
}

const deleteNotice = async (row) => {
  try {
    await ElMessageBox.confirm('确定删除该公告吗？', '提示', { type: 'warning' })
    const { data } = await adminClient.delete(`/admin/notice/${row.id}`)
    if (data.code !== 200) throw new Error(data.message || '删除失败')
    ElMessage.success('已删除')
    await loadNotices()
  } catch (err) {
    if (err !== 'cancel' && err !== 'close' && err?.message) {
      ElMessage.error(err.message || '删除失败')
    }
  }
}

const openConfigDrawer = (row) => {
  if (row) {
    configEditingId.value = row.id
    configForm.configKey = row.configKey || ''
    configForm.configValue = row.configValue || ''
    configForm.remark = row.remark || ''
  } else {
    configEditingId.value = null
    configForm.configKey = ''
    configForm.configValue = ''
    configForm.remark = ''
  }
  configDrawerVisible.value = true
}

const fillConfigSample = () => {
  configForm.configKey = 'banner.rotation.interval'
  configForm.configValue = '5000'
  configForm.remark = '首页 Banner 自动轮播间隔（ms）'
}

const saveConfig = async () => {
  try {
    configSaving.value = true
    const payload = { ...configForm }
    let data
    if (configEditingId.value) {
      ;({ data } = await adminClient.put(`/admin/system-config/${configEditingId.value}`, payload))
    } else {
      ;({ data } = await adminClient.post('/admin/system-config', payload))
    }
    if (data.code !== 200) throw new Error(data.message || '保存失败')
    ElMessage.success('保存成功')
    configDrawerVisible.value = false
    await loadConfigs()
  } catch (err) {
    ElMessage.error(err.message || '保存失败')
  } finally {
    configSaving.value = false
  }
}

const deleteConfig = async (row) => {
  try {
    await ElMessageBox.confirm('确定删除该配置吗？', '提示', { type: 'warning' })
    const { data } = await adminClient.delete(`/admin/system-config/${row.id}`)
    if (data.code !== 200) throw new Error(data.message || '删除失败')
    ElMessage.success('已删除')
    await loadConfigs()
  } catch (err) {
    if (err !== 'cancel' && err !== 'close' && err?.message) {
      ElMessage.error(err.message || '删除失败')
    }
  }
}

onMounted(() => {
  refreshAll()
})
</script>

<style scoped>
.tab-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 14px;
}

.filters {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.drawer-actions {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.content-metrics {
  margin-bottom: 18px;
}

:deep(.el-tabs__item) {
  font-size: 16px;
  font-weight: 800;
}

:deep(.el-table .cell) {
  color: var(--text);
}
</style>
