<template>
  <div class="section-stack">
    <section class="card">
      <div class="page-heading">
        <div>
          <div class="eyebrow">Training Ops</div>
          <h2>训练计划工作台</h2>
          <p>集中管理训练计划、计划项和视频绑定，让用户端训练主线始终有内容可推。</p>
        </div>
        <div class="toolbar-actions">
          <span class="code-pill mono">PLAN OPS / CRUD / BIND VIDEO</span>
          <el-button :loading="loading" @click="loadPlans">刷新列表</el-button>
          <el-button type="primary" @click="openPlanDrawer()">新建计划</el-button>
        </div>
      </div>

      <div class="metric-grid">
        <div class="metric-card">
          <div class="metric-label">计划总数</div>
          <div class="metric-value">{{ plans.length }}</div>
          <div class="metric-note">当前处于启用状态 {{ activePlanCount }} 套</div>
        </div>
        <div class="metric-card">
          <div class="metric-label">当前选中</div>
          <div class="metric-value">{{ selectedDetail?.plan?.id || '-' }}</div>
          <div class="metric-note">{{ selectedDetail?.plan?.title || '未选择训练计划' }}</div>
        </div>
        <div class="metric-card">
          <div class="metric-label">活跃订阅</div>
          <div class="metric-value">{{ selectedDetail?.activeSubscribeCount ?? 0 }}</div>
          <div class="metric-note">当前计划实时订阅人数</div>
        </div>
        <div class="metric-card">
          <div class="metric-label">计划项数量</div>
          <div class="metric-value">{{ selectedDetail?.items?.length || 0 }}</div>
          <div class="metric-note">建议保持每套计划至少有 3 个以上动作节点</div>
        </div>
      </div>

      <el-alert
        v-if="plans.length <= 1"
        type="info"
        :closable="false"
        show-icon
        style="margin-top: 16px;"
        title="当前计划较少。新增更多计划后，用户端才会出现更明显的训练主线切换体验。"
      />
    </section>

    <section class="split-layout">
      <div class="section-stack">
        <div class="card">
          <div class="toolbar">
            <div>
              <h3 class="section-title-sm">计划列表</h3>
              <div class="section-copy">左侧先看整体计划分布，再进入详情维护动作节点。</div>
            </div>
            <div class="chip-row">
              <span class="tag">已启用 {{ activePlanCount }}</span>
              <span class="tag">已停用 {{ inactivePlanCount }}</span>
            </div>
          </div>

          <div class="table-shell">
            <el-table
              :data="plans"
              style="width: 100%"
              v-loading="loading"
              highlight-current-row
              table-layout="auto"
              @row-click="selectPlan"
            >
              <el-table-column prop="id" label="ID" width="72" />
              <el-table-column prop="title" label="计划名称" min-width="220" />
              <el-table-column label="计划摘要" min-width="320">
                <template #default="{ row }">
                  <div class="plan-summary-cell">
                    <span class="tag">目标 {{ row.goal || '-' }}</span>
                    <span class="tag">难度 {{ row.level || '-' }}</span>
                    <span class="tag">周期 {{ row.durationWeeks || 0 }} 周</span>
                    <span class="tag">计划项 {{ row.itemCount || 0 }}</span>
                  </div>
                </template>
              </el-table-column>
              <el-table-column label="状态" width="92">
                <template #default="{ row }">
                  <el-tag :type="row.status === 1 ? 'success' : 'info'">
                    {{ row.status === 1 ? '启用' : '停用' }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column label="操作" width="280">
                <template #default="{ row }">
                  <div class="table-action-row">
                    <el-button size="small" @click.stop="openPlanDrawer(row)">编辑</el-button>
                    <el-button size="small" type="success" @click.stop="selectPlan(row)">查看详情</el-button>
                    <el-button size="small" type="danger" @click.stop="deletePlan(row)">删除</el-button>
                  </div>
                </template>
              </el-table-column>
            </el-table>
          </div>
        </div>

        <div class="card">
          <div class="toolbar">
            <div>
              <h3 class="section-title-sm">计划详情</h3>
              <div class="section-copy">计划详情会同步告诉你封面、订阅和动作节点是否完整。</div>
            </div>
            <div class="toolbar-actions">
              <el-button :disabled="!selectedPlanId" :loading="detailLoading" @click="reloadSelected">刷新详情</el-button>
              <el-button type="primary" :disabled="!selectedPlanId" @click="openItemDrawer()">新增计划项</el-button>
            </div>
          </div>

          <div v-if="selectedDetail" class="plan-hero">
            <div class="plan-copy">
              <div class="eyebrow">Plan #{{ selectedDetail.plan.id }}</div>
              <div class="plan-title">{{ selectedDetail.plan.title }}</div>
              <div class="chip-row">
                <span class="tag">目标 {{ selectedDetail.plan.goal || '-' }}</span>
                <span class="tag">难度 {{ selectedDetail.plan.level || '-' }}</span>
                <span class="tag">周期 {{ selectedDetail.plan.durationWeeks || 0 }} 周</span>
              </div>
              <div class="section-copy">
                当前有 {{ selectedDetail.items?.length || 0 }} 个动作节点，
                {{ selectedDetail.activeSubscribeCount || 0 }} 个活跃订阅，
                {{ selectedDetail.recordCount || 0 }} 条训练记录。
              </div>
            </div>
            <el-image
              v-if="selectedDetail.plan.coverUrl"
              :src="selectedDetail.plan.coverUrl"
              fit="cover"
              class="cover-preview"
            />
          </div>

          <div v-if="!selectedDetail" class="empty-block">
            <el-empty description="请选择一套训练计划">
              <el-button type="primary" @click="openPlanDrawer()">新建第一套计划</el-button>
            </el-empty>
          </div>

          <template v-else>
            <div class="table-shell">
              <el-table :data="selectedDetail.items || []" style="width: 100%" v-loading="detailLoading" table-layout="auto">
                <el-table-column prop="id" label="计划项 ID" width="96" />
                <el-table-column prop="dayIndex" label="训练日" width="86" />
                <el-table-column prop="actionName" label="动作名称" min-width="180" />
                <el-table-column label="训练参数" min-width="260">
                  <template #default="{ row }">
                    <div class="plan-summary-cell compact">
                      <span class="tag">{{ row.sets || 0 }} 组</span>
                      <span class="tag">{{ row.reps || 0 }} 次</span>
                      <span class="tag">{{ row.durationMin || 0 }} 分钟</span>
                      <span class="tag">休息 {{ row.restSec || 0 }} 秒</span>
                    </div>
                  </template>
                </el-table-column>
                <el-table-column label="绑定视频" min-width="180">
                  <template #default="{ row }">
                    {{ row.videoTitle || (row.videoId ? `视频 #${row.videoId}` : '未绑定') }}
                  </template>
                </el-table-column>
                <el-table-column prop="sort" label="排序" width="80" />
                <el-table-column label="操作" width="180">
                  <template #default="{ row }">
                    <div class="table-action-row">
                      <el-button size="small" @click="openItemDrawer(row)">编辑</el-button>
                      <el-button size="small" type="danger" @click="deleteItem(row)">删除</el-button>
                    </div>
                  </template>
                </el-table-column>
              </el-table>
            </div>
          </template>
        </div>
      </div>

      <div class="section-stack">
        <div class="card surface-muted">
          <div class="section-title-sm">维护建议</div>
          <div class="info-list">
            <div class="info-row">
              <div class="info-label">最低动作密度</div>
              <div class="info-value">每套计划至少 3 项</div>
            </div>
            <div class="info-row">
              <div class="info-label">视频绑定</div>
              <div class="info-value">{{ boundVideoCount }} / {{ selectedDetail?.items?.length || 0 }}</div>
            </div>
            <div class="info-row">
              <div class="info-label">当前风险</div>
              <div class="info-value">{{ riskHint }}</div>
            </div>
          </div>
        </div>

        <div class="card">
          <div class="section-title-sm">快速样例</div>
          <div class="section-copy">如果你需要先搭出完整链路，可以先用下面两组示例填充再保存。</div>
          <div class="toolbar-actions" style="margin-top: 14px;">
            <el-button @click="fillPlanSample">填充计划示例</el-button>
            <el-button @click="fillItemSample">填充计划项示例</el-button>
          </div>
        </div>
      </div>
    </section>

    <el-drawer v-model="planDrawerVisible" :title="planDrawerTitle" size="34%">
      <el-form :model="planForm" label-position="top">
        <el-form-item label="计划名称">
          <el-input v-model.trim="planForm.title" placeholder="例如：8 周减脂基础计划" />
        </el-form-item>
        <el-form-item label="目标">
          <el-input v-model.trim="planForm.goal" placeholder="例如 fat_loss / muscle_gain" />
        </el-form-item>
        <el-form-item label="难度">
          <el-input v-model.trim="planForm.level" placeholder="例如 beginner / intermediate" />
        </el-form-item>
        <el-form-item label="周期（周）">
          <el-input v-model.number="planForm.durationWeeks" />
        </el-form-item>
        <el-form-item label="封面 URL">
          <el-input v-model.trim="planForm.coverUrl" placeholder="建议填写可公开访问的图片地址" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model.number="planForm.status">
            <el-option label="停用" :value="0" />
            <el-option label="启用" :value="1" />
          </el-select>
        </el-form-item>
        <div class="toolbar-actions">
          <el-button @click="fillPlanSample">填充示例</el-button>
          <el-button type="primary" :loading="planSaving" @click="savePlan">保存计划</el-button>
        </div>
      </el-form>
    </el-drawer>

    <el-drawer v-model="itemDrawerVisible" :title="itemDrawerTitle" size="34%">
      <el-form :model="itemForm" label-position="top">
        <el-form-item label="第几天">
          <el-input v-model.number="itemForm.dayIndex" />
        </el-form-item>
        <el-form-item label="动作名称">
          <el-input v-model.trim="itemForm.actionName" placeholder="例如：哑铃深蹲" />
        </el-form-item>
        <el-form-item label="组数">
          <el-input v-model.number="itemForm.sets" />
        </el-form-item>
        <el-form-item label="次数">
          <el-input v-model.number="itemForm.reps" />
        </el-form-item>
        <el-form-item label="时长（分钟）">
          <el-input v-model.number="itemForm.durationMin" />
        </el-form-item>
        <el-form-item label="休息（秒）">
          <el-input v-model.number="itemForm.restSec" />
        </el-form-item>
        <el-form-item label="绑定视频">
          <el-select v-model="itemForm.videoId" clearable filterable placeholder="可不绑定">
            <el-option
              v-for="video in videoOptions"
              :key="video.id"
              :label="`#${video.id} ${video.title || '未命名视频'}`"
              :value="video.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="排序">
          <el-input v-model.number="itemForm.sort" />
        </el-form-item>
        <div class="toolbar-actions">
          <el-button @click="fillItemSample">填充示例</el-button>
          <el-button type="primary" :loading="itemSaving" @click="saveItem">保存计划项</el-button>
        </div>
      </el-form>
    </el-drawer>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { adminClient } from '../api/client'

const plans = ref([])
const loading = ref(false)
const detailLoading = ref(false)
const selectedPlanId = ref(null)
const selectedDetail = ref(null)
const videoOptions = ref([])

const planDrawerVisible = ref(false)
const itemDrawerVisible = ref(false)
const editingPlanId = ref(null)
const editingItemId = ref(null)
const planSaving = ref(false)
const itemSaving = ref(false)

const activePlanCount = computed(() => plans.value.filter((item) => item.status === 1).length)
const inactivePlanCount = computed(() => plans.value.filter((item) => item.status !== 1).length)
const boundVideoCount = computed(() => (selectedDetail.value?.items || []).filter((item) => !!item.videoId).length)
const riskHint = computed(() => {
  if (!selectedDetail.value) return '请选择计划'
  if (!(selectedDetail.value.items || []).length) return '当前计划没有动作节点'
  if (boundVideoCount.value < (selectedDetail.value.items || []).length) return '部分动作未绑定视频'
  return '结构完整'
})
const planDrawerTitle = computed(() => (editingPlanId.value ? '编辑训练计划' : '新建训练计划'))
const itemDrawerTitle = computed(() => (editingItemId.value ? '编辑计划项' : '新增计划项'))

const planForm = reactive({
  title: '',
  goal: '',
  level: 'beginner',
  durationWeeks: 4,
  coverUrl: '',
  status: 1
})

const itemForm = reactive({
  dayIndex: 1,
  actionName: '',
  sets: 4,
  reps: 12,
  durationMin: 15,
  restSec: 60,
  videoId: null,
  sort: 1
})

const resetPlanForm = () => {
  editingPlanId.value = null
  planForm.title = ''
  planForm.goal = ''
  planForm.level = 'beginner'
  planForm.durationWeeks = 4
  planForm.coverUrl = ''
  planForm.status = 1
}

const resetItemForm = () => {
  editingItemId.value = null
  itemForm.dayIndex = 1
  itemForm.actionName = ''
  itemForm.sets = 4
  itemForm.reps = 12
  itemForm.durationMin = 15
  itemForm.restSec = 60
  itemForm.videoId = null
  itemForm.sort = 1
}

const loadPlans = async () => {
  try {
    loading.value = true
    const { data } = await adminClient.get('/admin/training-plan/list')
    if (data.code !== 200) throw new Error(data.message || '加载训练计划失败')
    plans.value = data.data || []
    if (!plans.value.length) {
      selectedPlanId.value = null
      selectedDetail.value = null
      return
    }
    const targetId = plans.value.some((item) => item.id === selectedPlanId.value)
      ? selectedPlanId.value
      : plans.value[0].id
    await selectPlan({ id: targetId })
  } catch (err) {
    ElMessage.error(err.message || '加载训练计划失败')
  } finally {
    loading.value = false
  }
}

const loadVideos = async () => {
  try {
    const { data } = await adminClient.get('/admin/video/list', {
      params: { status: 1 }
    })
    if (data.code !== 200) throw new Error(data.message || '加载视频失败')
    videoOptions.value = data.data || []
  } catch (err) {
    ElMessage.error(err.message || '加载视频失败')
  }
}

const selectPlan = async (row) => {
  const planId = Number(row?.id)
  if (!planId) return
  try {
    detailLoading.value = true
    const { data } = await adminClient.get(`/admin/training-plan/${planId}`)
    if (data.code !== 200) throw new Error(data.message || '加载计划详情失败')
    selectedPlanId.value = planId
    selectedDetail.value = data.data
  } catch (err) {
    ElMessage.error(err.message || '加载计划详情失败')
  } finally {
    detailLoading.value = false
  }
}

const reloadSelected = async () => {
  if (!selectedPlanId.value) return
  await selectPlan({ id: selectedPlanId.value })
}

const openPlanDrawer = (row) => {
  if (row) {
    editingPlanId.value = row.id
    planForm.title = row.title || ''
    planForm.goal = row.goal || ''
    planForm.level = row.level || 'beginner'
    planForm.durationWeeks = row.durationWeeks ?? 4
    planForm.coverUrl = row.coverUrl || ''
    planForm.status = row.status ?? 1
  } else {
    resetPlanForm()
  }
  planDrawerVisible.value = true
}

const openItemDrawer = (row) => {
  if (!selectedPlanId.value) {
    ElMessage.warning('请先选择训练计划')
    return
  }
  if (row) {
    editingItemId.value = row.id
    itemForm.dayIndex = row.dayIndex ?? 1
    itemForm.actionName = row.actionName || ''
    itemForm.sets = row.sets ?? 4
    itemForm.reps = row.reps ?? 12
    itemForm.durationMin = row.durationMin ?? 15
    itemForm.restSec = row.restSec ?? 60
    itemForm.videoId = row.videoId ?? null
    itemForm.sort = row.sort ?? 1
  } else {
    resetItemForm()
  }
  itemDrawerVisible.value = true
}

const fillPlanSample = () => {
  planForm.title = '8周增肌基础计划'
  planForm.goal = 'muscle_gain'
  planForm.level = 'intermediate'
  planForm.durationWeeks = 8
  planForm.coverUrl = 'https://images.unsplash.com/photo-1517836357463-d25dfeac3438?w=1200'
  planForm.status = 1
}

const fillItemSample = () => {
  itemForm.dayIndex = 1
  itemForm.actionName = '哑铃深蹲'
  itemForm.sets = 4
  itemForm.reps = 10
  itemForm.durationMin = 20
  itemForm.restSec = 60
  itemForm.videoId = videoOptions.value[0]?.id ?? null
  itemForm.sort = 1
}

const savePlan = async () => {
  try {
    planSaving.value = true
    let data
    if (editingPlanId.value) {
      ;({ data } = await adminClient.put(`/admin/training-plan/${editingPlanId.value}`, { ...planForm }))
    } else {
      ;({ data } = await adminClient.post('/admin/training-plan', { ...planForm }))
    }
    if (data.code !== 200) throw new Error(data.message || '保存训练计划失败')
    const savedId = editingPlanId.value || data.data
    ElMessage.success(editingPlanId.value ? '训练计划已更新' : '训练计划已创建')
    planDrawerVisible.value = false
    await loadPlans()
    if (savedId) {
      await selectPlan({ id: savedId })
    }
  } catch (err) {
    ElMessage.error(err.message || '保存训练计划失败')
  } finally {
    planSaving.value = false
  }
}

const saveItem = async () => {
  if (!selectedPlanId.value) {
    ElMessage.warning('请先选择训练计划')
    return
  }
  try {
    itemSaving.value = true
    const payload = {
      ...itemForm,
      videoId: itemForm.videoId || null
    }
    let data
    if (editingItemId.value) {
      ;({ data } = await adminClient.put(`/admin/training-plan/item/${editingItemId.value}`, payload))
    } else {
      ;({ data } = await adminClient.post(`/admin/training-plan/${selectedPlanId.value}/item`, payload))
    }
    if (data.code !== 200) throw new Error(data.message || '保存计划项失败')
    ElMessage.success(editingItemId.value ? '计划项已更新' : '计划项已创建')
    itemDrawerVisible.value = false
    await loadPlans()
    await reloadSelected()
  } catch (err) {
    ElMessage.error(err.message || '保存计划项失败')
  } finally {
    itemSaving.value = false
  }
}

const deletePlan = async (row) => {
  try {
    await ElMessageBox.confirm(
      `确定删除训练计划 #${row.id} - ${row.title} 吗？`,
      '删除确认',
      {
        type: 'warning',
        confirmButtonText: '删除',
        cancelButtonText: '取消'
      }
    )
    const { data } = await adminClient.delete(`/admin/training-plan/${row.id}`)
    if (data.code !== 200) throw new Error(data.message || '删除训练计划失败')
    ElMessage.success('训练计划已删除')
    if (selectedPlanId.value === row.id) {
      selectedPlanId.value = null
      selectedDetail.value = null
    }
    await loadPlans()
  } catch (err) {
    if (err === 'cancel' || err === 'close') return
    ElMessage.error(err.message || '删除训练计划失败')
  }
}

const deleteItem = async (row) => {
  try {
    await ElMessageBox.confirm(
      `确定删除计划项 #${row.id} - ${row.actionName} 吗？`,
      '删除确认',
      {
        type: 'warning',
        confirmButtonText: '删除',
        cancelButtonText: '取消'
      }
    )
    const { data } = await adminClient.delete(`/admin/training-plan/item/${row.id}`)
    if (data.code !== 200) throw new Error(data.message || '删除计划项失败')
    ElMessage.success('计划项已删除')
    await loadPlans()
    await reloadSelected()
  } catch (err) {
    if (err === 'cancel' || err === 'close') return
    ElMessage.error(err.message || '删除计划项失败')
  }
}

onMounted(async () => {
  await Promise.all([loadPlans(), loadVideos()])
})
</script>

<style scoped>
.plan-hero {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 18px;
  margin-bottom: 18px;
  padding: 18px;
  border-radius: 22px;
  border: 3px solid var(--border);
  background: linear-gradient(180deg, #fffaf2 0%, #f4efff 100%);
  box-shadow: 0 10px 0 rgba(52, 45, 105, 0.08);
}

.plan-copy {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.plan-summary-cell {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.plan-summary-cell.compact {
  gap: 6px;
}

.plan-title {
  color: var(--text);
  font-family: 'Fredoka', 'Nunito', sans-serif;
  font-size: 28px;
  font-weight: 700;
}

.cover-preview {
  width: 210px;
  height: 124px;
  border-radius: 18px;
  overflow: hidden;
  border: 3px solid var(--border);
  flex-shrink: 0;
  box-shadow: 0 8px 0 rgba(52, 45, 105, 0.08);
}

@media (max-width: 900px) {
  .plan-hero {
    flex-direction: column;
  }

  .cover-preview {
    width: 100%;
    max-width: 360px;
  }
}
</style>
