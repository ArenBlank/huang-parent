<template>
  <div class="card">
    <div class="toolbar">
      <div>
        <h2 class="section-title">训练打卡</h2>
        <p class="section-sub">记录训练数据，形成统计</p>
      </div>
      <el-button type="primary" @click="loadStats" :loading="loading">刷新</el-button>
    </div>

    <div class="stat-grid" v-if="stats">
      <div class="stat-card">
        <div class="stat-label">本周打卡</div>
        <div class="stat-value">{{ stats.checkinCount ?? stats.totalCount ?? 0 }}</div>
      </div>
      <div class="stat-card">
        <div class="stat-label">训练时长</div>
        <div class="stat-value">{{ stats.totalDurationMin ?? stats.totalDuration ?? 0 }}</div>
      </div>
      <div class="stat-card">
        <div class="stat-label">消耗卡路里</div>
        <div class="stat-value">{{ stats.totalCalories ?? 0 }}</div>
      </div>
    </div>
    <el-empty v-else description="暂无统计，可先完成一次训练打卡">
      <div class="empty-actions">
        <el-button size="small" @click="loadStats">刷新</el-button>
        <el-button size="small" @click="goPlans">去订阅计划</el-button>
      </div>
    </el-empty>
  </div>

  <div class="card" style="margin-top: 16px;">
    <div class="toolbar">
      <div>
        <h2 class="section-title">新增打卡</h2>
        <p class="section-sub">选择计划与训练动作</p>
      </div>
      <el-button type="success" :loading="submitting" :disabled="!plans.length" @click="submit">提交</el-button>
    </div>
    <el-empty v-if="!plans.length" description="暂无可用计划，可先订阅训练计划">
      <el-button size="small" @click="goPlans">去订阅计划</el-button>
    </el-empty>
    <el-form v-else :model="form" label-position="top">
      <el-form-item label="训练计划">
        <el-select v-model="form.planId" filterable placeholder="选择计划" style="width: 100%" @change="handlePlanChange">
          <el-option v-for="plan in plans" :key="plan.id" :label="plan.title" :value="plan.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="计划动作">
        <el-select v-model="form.planItemId" filterable placeholder="选择动作" style="width: 100%" :loading="planItemLoading">
          <el-option
            v-for="item in planItems"
            :key="item.id"
            :label="`${item.actionName}（第 ${item.dayIndex} 天）`"
            :value="item.id"
          />
        </el-select>
      </el-form-item>
      <el-empty v-if="plans.length && !planItems.length && !planItemLoading" description="该计划暂无动作">
        <div class="empty-actions">
          <el-button size="small" @click="handlePlanChange(form.planId)">刷新动作</el-button>
        </div>
      </el-empty>
      <el-form-item label="训练日期">
        <el-date-picker v-model="form.recordDate" type="date" placeholder="选择日期" value-format="YYYY-MM-DD" />
      </el-form-item>
      <el-form-item label="时长（分钟）">
        <el-input v-model.number="form.durationMin" />
      </el-form-item>
      <el-form-item label="卡路里">
        <el-input v-model.number="form.calories" />
      </el-form-item>
      <el-form-item label="感受">
        <el-input v-model="form.feeling" />
      </el-form-item>
    </el-form>

    <div v-if="selectedItem" class="preview-card">
      <div class="preview-title">动作预览</div>
      <div class="preview-row">
        <div class="preview-name">{{ selectedItem.actionName }}（第 {{ selectedItem.dayIndex }} 天）</div>
        <div class="preview-meta">计划：{{ currentPlanTitle }}</div>
      </div>
      <div class="preview-meta">
        {{ selectedItem.sets }} 组 × {{ selectedItem.reps }} 次 ｜ 休息 {{ selectedItem.restSec }} 秒
      </div>
      <div class="preview-actions">
        <el-button
          size="small"
          type="primary"
          :disabled="!selectedItem.video?.playUrl"
          @click="openVideo(selectedItem.video?.playUrl)"
        >
          观看教学视频
        </el-button>
        <span v-if="!selectedItem.video" class="muted">该动作未绑定视频</span>
      </div>
    </div>
  </div>

  <div class="card" style="margin-top: 16px;">
    <div class="toolbar">
      <div>
        <h2 class="section-title">我的打卡记录</h2>
        <p class="section-sub">最近 20 条</p>
      </div>
      <el-button type="primary" @click="loadRecords" :loading="recordsLoading">刷新</el-button>
    </div>
    <el-table v-if="records.length" :data="records" v-loading="recordsLoading" style="width: 100%">
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="recordDate" label="日期" />
      <el-table-column prop="durationMin" label="时长" width="100" />
      <el-table-column prop="calories" label="卡路里" width="100" />
      <el-table-column prop="feeling" label="感受" />
    </el-table>
    <el-empty v-else :description="recordsLoading ? '正在加载记录...' : '暂无打卡记录，可先完成一次打卡'">
      <div class="empty-actions">
        <el-button size="small" @click="loadRecords">刷新记录</el-button>
        <el-button size="small" @click="goPlans">去订阅计划</el-button>
      </div>
    </el-empty>
  </div>
</template>

<script setup>
import { computed, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { appClient } from '../api/client'

const router = useRouter()
const stats = ref(null)
const loading = ref(false)
const submitting = ref(false)
const records = ref([])
const recordsLoading = ref(false)
const plans = ref([])
const planItems = ref([])
const planLoading = ref(false)
const planItemLoading = ref(false)

const selectedItem = computed(() => {
  if (!form.planItemId) return null
  return planItems.value.find((item) => item.id === form.planItemId) || null
})

const currentPlanTitle = computed(() => {
  const current = plans.value.find((plan) => plan.id === form.planId)
  return current?.title || '未选择'
})

const formatDate = (date) => {
  const local = new Date(date.getTime() - date.getTimezoneOffset() * 60000)
  return local.toISOString().slice(0, 10)
}

const form = reactive({
  planId: 1,
  planItemId: 1,
  recordDate: formatDate(new Date()),
  durationMin: 30,
  calories: 200,
  feeling: '轻松'
})

const loadPlans = async () => {
  try {
    planLoading.value = true
    const { data } = await appClient.get('/app/plan/list')
    if (data.code !== 200) throw new Error(data.message || '加载失败')
    plans.value = data.data || []
    if (plans.value.length) {
      form.planId = form.planId || plans.value[0].id
      await loadPlanDetail(form.planId)
    }
  } catch (err) {
    ElMessage.error(err.message || '加载失败')
  } finally {
    planLoading.value = false
  }
}

const loadPlanDetail = async (planId) => {
  if (!planId) return
  try {
    planItemLoading.value = true
    const { data } = await appClient.get(`/app/plan/${planId}`)
    if (data.code !== 200) throw new Error(data.message || '加载失败')
    planItems.value = data.data?.items || []
    if (planItems.value.length) {
      form.planItemId = planItems.value[0].id
    }
  } catch (err) {
    ElMessage.error(err.message || '加载失败')
  } finally {
    planItemLoading.value = false
  }
}

const handlePlanChange = async (planId) => {
  await loadPlanDetail(planId)
}

const loadStats = async () => {
  try {
    loading.value = true
    const { data } = await appClient.get('/app/record/my/weekly-stat')
    if (data.code !== 200) throw new Error(data.message || '加载失败')
    stats.value = data.data
  } catch (err) {
    ElMessage.error(err.message || '加载失败')
  } finally {
    loading.value = false
  }
}

const submit = async () => {
  if (!plans.value.length) {
    ElMessage.warning('暂无可用计划')
    return
  }
  if (!form.planId || !form.planItemId || !form.recordDate) {
    ElMessage.warning('请填写计划信息与日期')
    return
  }
  try {
    submitting.value = true
    const payload = {
      ...form,
      recordDate: form.recordDate
    }
    const { data } = await appClient.post('/app/record/checkin', payload)
    if (data.code !== 200) throw new Error(data.message || '打卡失败')
    ElMessage.success('打卡成功')
    await loadRecords()
    await loadStats()
  } catch (err) {
    ElMessage.error(err.message || '打卡失败')
  } finally {
    submitting.value = false
  }
}

const openVideo = (url) => {
  if (!url) return
  window.open(url, '_blank')
}

const goPlans = () => {
  router.push('/plans')
}

const loadRecords = async () => {
  try {
    recordsLoading.value = true
    const { data } = await appClient.get('/app/record/my/list', { params: { limit: 20 } })
    if (data.code !== 200) throw new Error(data.message || '加载失败')
    records.value = data.data || []
  } catch (err) {
    ElMessage.error(err.message || '加载失败')
  } finally {
    recordsLoading.value = false
  }
}

loadPlans()
loadStats()
loadRecords()
</script>

<style scoped>
.stat-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(160px, 1fr));
  gap: 12px;
}

.stat-card {
  background: rgba(59, 130, 246, 0.08);
  border: 1px solid rgba(59, 130, 246, 0.2);
  border-radius: 12px;
  padding: 12px;
}

.stat-label {
  font-size: 12px;
  color: var(--muted);
}

.stat-value {
  font-size: 20px;
  font-weight: 700;
  margin-top: 6px;
}

.preview-card {
  margin-top: 12px;
  padding: 12px;
  border-radius: 12px;
  border: 1px dashed var(--border);
  background: #ffffffcc;
}

.preview-title {
  font-weight: 600;
  margin-bottom: 6px;
}

.preview-row {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  gap: 12px;
}

.preview-name {
  font-weight: 600;
}

.preview-meta {
  font-size: 12px;
  color: var(--muted);
  margin-top: 4px;
}

.preview-actions {
  margin-top: 8px;
  display: flex;
  align-items: center;
  gap: 10px;
}
</style>
