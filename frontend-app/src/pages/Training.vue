<template>
  <div class="page-stack">
    <section class="hero-panel">
      <div class="training-hero">
        <div>
          <p class="quest-kicker">Training Check-in</p>
          <h1 class="hero-title">把每日打卡变成持续进步曲线</h1>
          <p class="hero-subtitle">先选训练计划和动作，再填写本次训练记录，最后在下方查看最近打卡历史。</p>
          <div class="hero-badges">
            <span class="badge-pill is-dark">计划 {{ plans.length }}</span>
            <span class="badge-pill is-dark">动作 {{ planItems.length }}</span>
            <span class="badge-pill is-dark">记录 {{ records.length }}</span>
          </div>
        </div>
        <div class="progress-panel">
          <p class="progress-title">本周训练热度 {{ weeklyHeat }}%</p>
          <el-progress :percentage="weeklyHeat" />
          <p class="muted progress-note">根据本周打卡次数与训练时长估算，不再显示静态演示数字。</p>
        </div>
      </div>
    </section>

    <section class="metric-grid">
      <article class="metric-card">
        <p class="metric-label">本周打卡</p>
        <p class="metric-value">{{ stats?.checkinCount ?? stats?.totalCount ?? 0 }}</p>
        <p class="metric-note">本周累计训练次数</p>
      </article>
      <article class="metric-card">
        <p class="metric-label">训练时长</p>
        <p class="metric-value">{{ stats?.totalDurationMin ?? stats?.totalDuration ?? 0 }}</p>
        <p class="metric-note">单位：分钟</p>
      </article>
      <article class="metric-card">
        <p class="metric-label">消耗热量</p>
        <p class="metric-value">{{ stats?.totalCalories ?? 0 }}</p>
        <p class="metric-note">本周总热量消耗</p>
      </article>
    </section>

    <section class="card">
      <div class="toolbar">
        <div>
          <h2 class="section-title">今日打卡</h2>
          <p class="section-sub">完成计划动作选择后，填写时长、热量和训练感受。</p>
        </div>
        <div class="toolbar-actions">
          <el-button @click="loadStats" :loading="loading">刷新统计</el-button>
          <el-button type="success" :loading="submitting" :disabled="!plans.length" @click="submit">提交打卡</el-button>
        </div>
      </div>

      <el-empty v-if="!plans.length" description="请先去计划大厅订阅计划">
        <div class="empty-actions">
          <el-button @click="goPlans">去计划大厅订阅计划</el-button>
        </div>
      </el-empty>

      <div v-else class="training-grid">
        <article class="form-card">
          <el-form :model="form" label-position="top">
            <el-form-item label="训练计划">
              <el-select
                v-model="form.planId"
                filterable
                placeholder="选择训练计划"
                style="width: 100%"
                @change="handlePlanChange"
              >
                <el-option v-for="plan in plans" :key="plan.id" :label="plan.title" :value="plan.id" />
              </el-select>
            </el-form-item>

            <el-form-item label="计划动作">
              <el-select
                v-model="form.planItemId"
                filterable
                placeholder="选择动作"
                style="width: 100%"
                :loading="planItemLoading"
              >
                <el-option
                  v-for="item in planItems"
                  :key="item.id"
                  :label="`${item.actionName}（第 ${item.dayIndex} 天）`"
                  :value="item.id"
                />
              </el-select>
            </el-form-item>

            <el-form-item label="训练日期">
              <el-date-picker v-model="form.recordDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
            </el-form-item>

            <div class="split-grid">
              <el-form-item label="时长（分钟）">
                <el-input v-model.number="form.durationMin" />
              </el-form-item>
              <el-form-item label="热量消耗">
                <el-input v-model.number="form.calories" />
              </el-form-item>
            </div>

            <el-form-item label="训练感受">
              <el-input v-model="form.feeling" placeholder="例如：动作流畅，状态不错" />
            </el-form-item>
          </el-form>
        </article>

        <article class="quest-card quest-card--accent">
          <p class="quest-kicker">Action Preview</p>
          <h3 class="preview-title">{{ selectedItem ? selectedItem.actionName : '等待选择动作' }}</h3>
          <div v-if="selectedItem" class="preview-meta">
            <span class="preview-day-chip">DAY {{ selectedItem.dayIndex }}</span>
            <span class="muted">当前计划：{{ currentPlanTitle }}</span>
          </div>
          <p class="quest-copy">
            {{
              selectedItem
                ? '这次打卡将记录到该动作节点，提交后可在训练历史中回看。'
                : '选择动作后，这里会展示节奏信息和可用的教学视频。'
            }}
          </p>
          <div v-if="selectedItem" class="badge-row">
            <span class="tag">组数 {{ selectedItem.sets || 0 }}</span>
            <span class="tag">次数 {{ selectedItem.reps || 0 }}</span>
            <span class="tag">休息 {{ selectedItem.restSec || 0 }} 秒</span>
          </div>
          <div class="action-row">
            <el-button type="primary" :disabled="!selectedItem?.video?.playUrl" @click="openVideo(selectedItem)">
              {{ selectedItem?.video?.playUrl ? '查看教学视频' : '暂无视频' }}
            </el-button>
            <span class="muted">{{ selectedItem?.video?.title || '当前动作还没有绑定教学视频' }}</span>
          </div>
        </article>
      </div>
    </section>

    <section class="card">
      <div class="toolbar">
        <div>
          <h2 class="section-title">最近训练记录</h2>
          <p class="section-sub">展示最近 20 条打卡记录。</p>
        </div>
        <el-button type="primary" @click="loadRecords" :loading="recordsLoading">刷新记录</el-button>
      </div>

      <el-empty v-if="!records.length && !recordsLoading" description="暂无打卡记录">
        <div class="empty-actions">
          <el-button size="small" @click="loadRecords">重试</el-button>
          <el-button size="small" @click="goPlans">去计划大厅订阅计划</el-button>
        </div>
      </el-empty>

      <div v-else class="record-grid">
        <article v-for="record in records" :key="record.id" class="record-card">
          <div class="record-head">
            <strong>{{ record.recordDate }}</strong>
            <span class="tag">{{ record.durationMin || 0 }} 分钟</span>
          </div>
          <p class="muted">热量 {{ record.calories || 0 }}</p>
          <p class="muted">感受 {{ record.feeling || '无' }}</p>
        </article>
      </div>
    </section>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from "vue"
import { useRouter } from "vue-router"
import { ElMessage } from "element-plus"
import { appClient } from "../api/client"
import { useTrainingStore } from "../stores/trainingStore"
import { createVideoPlaylist, saveVideoPlaylist } from "../utils/videoPlaylist"

const router = useRouter()
const trainingStore = useTrainingStore()
const form = trainingStore.draft

const stats = ref(null)
const loading = ref(false)
const submitting = ref(false)
const records = ref([])
const recordsLoading = ref(false)
const plans = ref([])
const planItems = ref([])
const planItemLoading = ref(false)

const isAvailablePlan = (plan) => Boolean(plan?.subscribed) && Number(plan?.status) === 1

const selectedItem = computed(() => {
  if (!form.planItemId) return null
  return planItems.value.find((item) => Number(item.id) === Number(form.planItemId)) || null
})

const currentPlanTitle = computed(() => {
  const current = plans.value.find((plan) => Number(plan.id) === Number(form.planId))
  return current?.title || "未选择"
})

const weeklyHeat = computed(() => {
  const count = Number(stats.value?.checkinCount ?? stats.value?.totalCount ?? 0)
  const duration = Number(stats.value?.totalDurationMin ?? stats.value?.totalDuration ?? 0)
  const score = count * 18 + duration * 0.6
  return Math.max(8, Math.min(Math.round(score), 100))
})

const loadPlans = async () => {
  try {
    const { data } = await appClient.get("/app/plan/list")
    if (data.code !== 200) throw new Error(data.message || "加载计划失败")

    plans.value = (data.data || []).filter(isAvailablePlan)
    if (!plans.value.length) {
      planItems.value = []
      trainingStore.resetWhenNoPlans()
      return
    }

    const matchedPlan = plans.value.find((plan) => Number(plan.id) === Number(form.planId))
    const nextPlanId = matchedPlan?.id || plans.value[0].id
    trainingStore.setPlanSelection(nextPlanId)
    await loadPlanDetail(nextPlanId)
  } catch (err) {
    ElMessage.error(err.message || "加载计划失败")
  }
}

const loadPlanDetail = async (planId, preferredItemId = form.planItemId) => {
  if (!planId) {
    planItems.value = []
    trainingStore.setActionSelection(null)
    return
  }

  try {
    planItemLoading.value = true
    const { data } = await appClient.get(`/app/plan/${planId}`)
    if (data.code !== 200) throw new Error(data.message || "加载计划详情失败")

    planItems.value = data.data?.items || []
    const matchedItem = planItems.value.find((item) => Number(item.id) === Number(preferredItemId))
    trainingStore.setActionSelection(matchedItem?.id || planItems.value[0]?.id || null)
  } catch (err) {
    ElMessage.error(err.message || "加载计划详情失败")
  } finally {
    planItemLoading.value = false
  }
}

const handlePlanChange = async (planId) => {
  trainingStore.setPlanSelection(planId)
  await loadPlanDetail(planId)
}

const loadStats = async () => {
  try {
    loading.value = true
    const { data } = await appClient.get("/app/record/my/weekly-stat")
    if (data.code !== 200) throw new Error(data.message || "加载统计失败")
    stats.value = data.data
  } catch (err) {
    ElMessage.error(err.message || "加载统计失败")
  } finally {
    loading.value = false
  }
}

const loadRecords = async () => {
  try {
    recordsLoading.value = true
    const { data } = await appClient.get("/app/record/my/list", { params: { limit: 20 } })
    if (data.code !== 200) throw new Error(data.message || "加载记录失败")
    records.value = data.data || []
  } catch (err) {
    ElMessage.error(err.message || "加载记录失败")
  } finally {
    recordsLoading.value = false
  }
}

const submit = async () => {
  if (!plans.value.length) {
    ElMessage.warning("请先去计划大厅订阅计划")
    return
  }
  if (!form.planId || !form.planItemId || !form.recordDate) {
    ElMessage.warning("请完整填写打卡信息")
    return
  }

  try {
    submitting.value = true
    const payload = {
      planId: form.planId,
      planItemId: form.planItemId,
      recordDate: form.recordDate,
      durationMin: form.durationMin,
      calories: form.calories,
      feeling: form.feeling
    }
    const { data } = await appClient.post("/app/record/checkin", payload)
    if (data.code !== 200) throw new Error(data.message || "打卡失败")

    ElMessage.success("打卡成功")
    await loadRecords()
    await loadStats()
    trainingStore.resetAfterSubmit()
  } catch (err) {
    ElMessage.error(err.message || "打卡失败")
  } finally {
    submitting.value = false
  }
}

const openVideo = (item) => {
  const playUrl = item?.video?.playUrl
  if (!playUrl) return

  const playlist = createVideoPlaylist({
    items: planItems.value,
    source: currentPlanTitle.value || "训练计划",
    cover: "",
    fallbackTitle: "训练教学视频"
  })
  const playlistIndex = playlist.findIndex((entry) => Number(entry.id) === Number(item?.id))
  const playlistKey = saveVideoPlaylist(playlist, `training-${form.planId || "checkin"}`)

  router.push({
    path: "/video-player",
    query: {
      playlistKey,
      index: String(playlistIndex >= 0 ? playlistIndex : 0),
      url: playUrl,
      title: item?.video?.title || item?.actionName || "训练教学视频",
      source: currentPlanTitle.value || "训练计划",
      action: item?.actionName || "",
      actionId: item?.id ? String(item.id) : "",
      planId: form.planId ? String(form.planId) : "",
      returnTo: "/training",
      cover: ""
    }
  })
}

const goPlans = () => {
  router.push("/plans")
}

onMounted(() => {
  loadPlans()
  loadStats()
  loadRecords()
})
</script>

<style scoped>
.training-hero {
  display: grid;
  grid-template-columns: minmax(0, 1.25fr) minmax(280px, 0.75fr);
  gap: 12px;
  align-items: end;
}

.progress-panel {
  border-radius: 16px;
  border: 1px solid rgba(255, 255, 255, 0.32);
  background: rgba(20, 56, 37, 0.34);
  padding: 12px;
}

.progress-title {
  margin: 0;
  font-size: 16px;
  color: #f4ffef;
  font-weight: 700;
}

.progress-note {
  margin-top: 8px;
  color: rgba(244, 255, 239, 0.9);
}

.training-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.15fr) minmax(300px, 0.85fr);
  gap: 12px;
}

.form-card {
  border: 1px solid var(--eco-border);
  border-radius: 18px;
  background: #fffdf8;
  padding: 14px;
}

.preview-title {
  margin: 0;
  font-size: 24px;
  line-height: 1.1;
}

.preview-meta {
  margin-top: 10px;
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
}

.preview-day-chip {
  display: inline-flex;
  align-items: center;
  min-height: 28px;
  border-radius: 999px;
  border: 1px solid rgba(255, 255, 255, 0.56);
  background: rgba(255, 255, 255, 0.22);
  padding: 0 12px;
  color: #ffffff;
  font-size: 12px;
  font-weight: 800;
  letter-spacing: 0.04em;
  box-shadow: inset 0 0 0 1px rgba(52, 45, 105, 0.12);
}

.record-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 10px;
}

.record-card {
  border: 1px solid var(--eco-border);
  border-radius: 16px;
  background: #ffffff;
  padding: 12px;
}

.record-head {
  display: flex;
  justify-content: space-between;
  gap: 8px;
  align-items: center;
}

.record-head strong {
  font-size: 16px;
}

@media (max-width: 960px) {
  .training-hero,
  .training-grid {
    grid-template-columns: 1fr;
  }
}
</style>
