<template>
  <div class="card">
    <div class="toolbar">
      <div>
        <h2 class="section-title">训练计划</h2>
        <p class="section-sub">选择适合你的计划并订阅</p>
      </div>
      <el-button type="primary" @click="loadPlans" :loading="loading">刷新</el-button>
    </div>

    <el-table v-if="plans.length" :data="plans" v-loading="loading" style="width: 100%" @row-click="selectPlan">
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="title" label="计划名称" />
      <el-table-column prop="level" label="难度" width="120" />
      <el-table-column prop="durationWeeks" label="周期（周）" width="120" />
      <el-table-column prop="status" label="状态" width="90" />
    </el-table>
    <el-empty v-else :description="loading ? '正在加载计划...' : '暂无计划，可联系管理员初始化计划数据'">
      <div class="empty-actions">
        <el-button size="small" @click="loadPlans">刷新</el-button>
        <el-button size="small" @click="goTo('/training')">去训练打卡</el-button>
      </div>
    </el-empty>
  </div>

  <div class="card" style="margin-top: 16px;">
    <div class="toolbar">
      <div>
        <h2 class="section-title">计划详情</h2>
        <p class="section-sub">点击上表查看详情</p>
      </div>
      <el-button type="success" :disabled="!selected" @click="subscribe" :loading="submitting">订阅计划</el-button>
    </div>
    <el-form :inline="true" label-position="top">
      <el-form-item label="开始日期">
        <el-date-picker v-model="startDate" type="date" placeholder="选择日期" value-format="YYYY-MM-DD" />
      </el-form-item>
    </el-form>
    <el-empty v-if="!selected" description="请选择计划">
      <div class="empty-actions">
        <el-button size="small" @click="loadPlans">刷新计划</el-button>
        <el-button size="small" @click="goTo('/training')">去训练打卡</el-button>
      </div>
    </el-empty>
    <div v-else class="detail">
      <div class="detail-title">{{ selected.plan?.title }}</div>
      <div class="detail-sub">目标：{{ selected.plan?.goal }} ｜ 难度：{{ selected.plan?.level }}</div>
      <el-divider />
      <div v-for="item in selected.items || []" :key="item.id" class="plan-item">
        <div>{{ item.actionName }}（第 {{ item.dayIndex }} 天）</div>
        <div class="muted">{{ item.sets }} 组 × {{ item.reps }} 次 ｜ 休息 {{ item.restSec }} 秒</div>
        <div class="muted" v-if="item.video?.playUrl">
          <el-link type="primary" @click.prevent="openVideo(item.video.playUrl)">观看视频</el-link>
        </div>
        <div class="muted" v-else>未绑定视频</div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { appClient } from '../api/client'

const plans = ref([])
const selected = ref(null)
const loading = ref(false)
const submitting = ref(false)
const router = useRouter()

const formatDate = (date) => {
  const local = new Date(date.getTime() - date.getTimezoneOffset() * 60000)
  return local.toISOString().slice(0, 10)
}

const startDate = ref(formatDate(new Date()))

const loadPlans = async () => {
  try {
    loading.value = true
    const { data } = await appClient.get('/app/plan/list')
    if (data.code !== 200) throw new Error(data.message || '加载失败')
    plans.value = data.data || []
    if (plans.value.length && !selected.value) {
      await selectPlan(plans.value[0])
    }
  } catch (err) {
    ElMessage.error(err.message || '加载失败')
  } finally {
    loading.value = false
  }
}

const selectPlan = async (row) => {
  try {
    const { data } = await appClient.get(`/app/plan/${row.id}`)
    if (data.code !== 200) throw new Error(data.message || '加载详情失败')
    selected.value = data.data
  } catch (err) {
    ElMessage.error(err.message || '加载详情失败')
  }
}

const subscribe = async () => {
  if (!selected.value?.plan?.id) {
    ElMessage.warning('请先选择计划')
    return
  }
  if (!startDate.value) {
    ElMessage.warning('请选择开始日期')
    return
  }
  try {
    submitting.value = true
    const { data } = await appClient.post('/app/plan/subscribe', {
      planId: selected.value.plan.id,
      startDate: startDate.value
    })
    if (data.code !== 200) throw new Error(data.message || '订阅失败')
    ElMessage.success('订阅成功')
  } catch (err) {
    ElMessage.error(err.message || '订阅失败')
  } finally {
    submitting.value = false
  }
}

const openVideo = (url) => {
  if (!url) return
  window.open(url, '_blank')
}

const goTo = (path) => {
  if (!path) return
  router.push(path)
}

loadPlans()
</script>

<style scoped>
.detail-title {
  font-size: 16px;
  font-weight: 600;
}

.detail-sub {
  font-size: 12px;
  color: var(--muted);
  margin-top: 6px;
}

.plan-item {
  padding: 8px 0;
  border-bottom: 1px dashed var(--border);
}

.muted {
  font-size: 12px;
  color: var(--muted);
}
</style>
