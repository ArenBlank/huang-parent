<template>
  <div class="page-stack">
    <section class="hero-panel">
      <div class="plan-hero">
        <div>
          <p class="quest-kicker">Plan Hub</p>
          <h1 class="hero-title">先选训练主线，再稳定推进</h1>
          <p class="hero-subtitle">
            训练计划页重做为清晰双栏：左侧选主线，右侧看详情与动作节点，所有数据都来自后端接口。
          </p>
          <div class="hero-badges">
            <span class="badge-pill is-dark">可选计划 {{ plans.length }}</span>
            <span class="badge-pill is-dark">当前节点 {{ selected?.items?.length || 0 }}</span>
            <span class="badge-pill is-dark">进度 {{ itemProgress }}%</span>
          </div>
        </div>
        <div class="progress-panel">
          <p class="progress-title">订阅进度</p>
          <el-progress :percentage="itemProgress" />
          <p class="muted progress-note">
            {{ selected?.subscription ? `已订阅，开始日期 ${selected.subscription.startDate}` : "未订阅，选择开始日期后激活" }}
          </p>
        </div>
      </div>
    </section>

    <section class="plan-grid">
      <article class="card">
        <div class="toolbar">
          <div>
            <h2 class="section-title">计划列表</h2>
            <p class="section-sub">点击任意计划卡片即可加载详情，不再使用拥挤横向表格。</p>
          </div>
          <el-button type="primary" @click="loadPlans" :loading="loading">刷新计划</el-button>
        </div>

        <el-empty v-if="!plans.length" description="暂无训练计划">
          <div class="empty-actions">
            <el-button size="small" @click="loadPlans">重试</el-button>
            <el-button size="small" @click="goTo('/training')">去打卡</el-button>
          </div>
        </el-empty>

        <div v-else class="plan-list">
          <button
            v-for="plan in plans"
            :key="plan.id"
            type="button"
            class="plan-card eco-clickable"
            :class="{ 'plan-card--active': selected?.plan?.id === plan.id }"
            @click="selectPlan(plan)"
          >
            <div class="plan-head">
              <strong>{{ plan.title }}</strong>
              <span class="tag">{{ plan.level || "标准难度" }}</span>
            </div>
            <p class="plan-goal">{{ plan.goal || "按周期推进训练目标" }}</p>
            <div class="plan-meta">
              <span>周期 {{ plan.durationWeeks || 0 }} 周</span>
              <span>状态 {{ plan.subscribed ? "已订阅" : "未订阅" }}</span>
            </div>
          </button>
        </div>
      </article>

      <article class="card">
        <div class="toolbar">
          <div>
            <h2 class="section-title">计划详情</h2>
            <p class="section-sub">选择开始日期后可以直接激活或更新当前计划。</p>
          </div>
          <el-button type="success" :disabled="!selected" :loading="submitting" @click="subscribe">
            {{ selected?.subscription ? "更新订阅日期" : "激活计划" }}
          </el-button>
        </div>

        <el-empty v-if="!selected" description="请选择一个训练计划" />

        <div v-else class="detail-shell">
          <div class="summary-card">
            <h3>{{ selected.plan?.title }}</h3>
            <p>{{ selected.plan?.goal || "暂无目标描述" }}</p>
            <div class="badge-row">
              <span class="tag">计划ID {{ selected.plan?.id }}</span>
              <span class="tag">周期 {{ selected.plan?.durationWeeks || 0 }} 周</span>
              <span class="tag">动作 {{ selected.items?.length || 0 }} 个</span>
            </div>
          </div>

          <div class="subscribe-card">
            <el-form label-position="top">
              <el-form-item label="开始日期">
                <el-date-picker
                  v-model="startDate"
                  type="date"
                  value-format="YYYY-MM-DD"
                  placeholder="选择日期"
                  style="width: 100%"
                />
              </el-form-item>
            </el-form>
            <el-alert
              v-if="selected.subscription"
              type="success"
              :closable="false"
              show-icon
              :title="`当前订阅开始于 ${selected.subscription.startDate}`"
            />
            <el-alert v-else type="info" :closable="false" show-icon title="尚未订阅，设置日期后点击激活计划" />
          </div>

          <section>
            <h3 class="section-title-sm">动作节点</h3>
            <p class="section-sub">训练动作按天展示，可快速查看时长、组数和绑定视频。</p>
            <el-empty v-if="!selected.items?.length" description="该计划还没有动作节点" />
            <div v-else class="timeline-list">
              <article v-for="item in selected.items" :key="item.id" class="timeline-item">
                <div class="timeline-day">DAY {{ item.dayIndex }}</div>
                <div class="timeline-content">
                  <div class="timeline-head">
                    <strong>{{ item.actionName }}</strong>
                    <span class="tag">{{ item.durationMin || 0 }} 分钟</span>
                  </div>
                  <p class="muted">组数 {{ item.sets || 0 }} · 次数 {{ item.reps || 0 }} · 休息 {{ item.restSec || 0 }} 秒</p>
                  <div class="timeline-actions">
                    <el-button type="primary" :disabled="!item.video?.playUrl" @click="openVideo(item.video?.playUrl)">
                      {{ item.video?.playUrl ? "查看视频" : "暂无视频" }}
                    </el-button>
                    <span class="muted">{{ item.video?.title || "当前节点未绑定视频资源" }}</span>
                  </div>
                </div>
              </article>
            </div>
          </section>
        </div>
      </article>
    </section>
  </div>
</template>

<script setup>
import { computed, ref } from "vue"
import { useRouter } from "vue-router"
import { ElMessage } from "element-plus"
import { appClient } from "../api/client"

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

const itemProgress = computed(() => {
  const total = selected.value?.items?.length || 0
  if (!total) return selected.value?.subscription ? 35 : 10
  return selected.value?.subscription ? Math.min(38 + total * 8, 100) : Math.min(18 + total * 6, 92)
})

const loadPlans = async () => {
  try {
    loading.value = true
    const { data } = await appClient.get("/app/plan/list")
    if (data.code !== 200) throw new Error(data.message || "加载计划失败")
    plans.value = data.data || []
    if (plans.value.length) {
      const currentId = selected.value?.plan?.id
      const target = plans.value.find((item) => item.id === currentId) || plans.value[0]
      await selectPlan(target)
    } else {
      selected.value = null
    }
  } catch (err) {
    ElMessage.error(err.message || "加载计划失败")
  } finally {
    loading.value = false
  }
}

const selectPlan = async (row) => {
  if (!row?.id) return
  try {
    const { data } = await appClient.get(`/app/plan/${row.id}`)
    if (data.code !== 200) throw new Error(data.message || "加载计划详情失败")
    selected.value = data.data
    if (selected.value?.subscription?.startDate) {
      startDate.value = selected.value.subscription.startDate
    }
  } catch (err) {
    ElMessage.error(err.message || "加载计划详情失败")
  }
}

const subscribe = async () => {
  if (!selected.value?.plan?.id) {
    ElMessage.warning("请先选择计划")
    return
  }
  if (!startDate.value) {
    ElMessage.warning("请选择开始日期")
    return
  }
  try {
    submitting.value = true
    const { data } = await appClient.post("/app/plan/subscribe", {
      planId: selected.value.plan.id,
      startDate: startDate.value
    })
    if (data.code !== 200) throw new Error(data.message || "订阅失败")
    ElMessage.success(`计划已保存，开始日期 ${startDate.value}`)
    await loadPlans()
    await selectPlan({ id: selected.value.plan.id })
  } catch (err) {
    ElMessage.error(err.message || "订阅失败")
  } finally {
    submitting.value = false
  }
}

const openVideo = (url) => {
  if (!url) return
  window.open(url, "_blank")
}

const goTo = (path) => {
  router.push(path)
}

loadPlans()
</script>

<style scoped>
.plan-hero {
  display: grid;
  grid-template-columns: minmax(0, 1.25fr) minmax(280px, 0.75fr);
  gap: 14px;
  align-items: end;
}

.progress-panel {
  border-radius: 18px;
  border: 1px solid rgba(255, 255, 255, 0.32);
  background: rgba(20, 56, 37, 0.34);
  padding: 14px;
}

.progress-title {
  margin: 0;
  font-size: 16px;
  font-weight: 700;
}

.progress-note {
  margin: 8px 0 0;
  color: rgba(244, 255, 239, 0.9);
}

.plan-grid {
  display: grid;
  grid-template-columns: minmax(320px, 0.95fr) minmax(0, 1.05fr);
  gap: 16px;
}

.plan-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.plan-card {
  border: 1px solid var(--eco-border);
  border-radius: 16px;
  background: #ffffff;
  padding: 12px;
  text-align: left;
}

.plan-card--active {
  border-color: var(--eco-primary);
  background: linear-gradient(180deg, #f6f1ff 0%, #f2efff 100%);
}

.plan-head {
  display: flex;
  justify-content: space-between;
  gap: 10px;
  align-items: center;
}

.plan-head strong {
  font-size: 16px;
}

.plan-goal {
  margin: 8px 0 0;
  color: var(--eco-text-soft);
  font-size: 13px;
  line-height: 1.6;
}

.plan-meta {
  margin-top: 8px;
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  color: var(--eco-text-soft);
  font-size: 12px;
}

.detail-shell {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.summary-card,
.subscribe-card {
  border: 1px solid var(--eco-border);
  border-radius: 16px;
  background: #fffdf8;
  padding: 14px;
}

.summary-card h3 {
  margin: 0;
  font-size: 24px;
  line-height: 1.1;
}

.summary-card p {
  margin: 8px 0 0;
  color: var(--eco-text-soft);
  line-height: 1.6;
}

.timeline-list {
  margin-top: 10px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.timeline-item {
  border: 1px solid var(--eco-border);
  border-radius: 16px;
  background: #ffffff;
  padding: 12px;
  display: grid;
  grid-template-columns: 90px 1fr;
  gap: 10px;
}

.timeline-day {
  border-radius: 12px;
  border: 1px solid var(--eco-border-strong);
  background: #f2efff;
  display: grid;
  place-items: center;
  font-size: 12px;
  font-weight: 800;
}

.timeline-head {
  display: flex;
  justify-content: space-between;
  gap: 10px;
  align-items: center;
}

.timeline-head strong {
  font-size: 16px;
}

.timeline-actions {
  margin-top: 8px;
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
}

@media (max-width: 1080px) {
  .plan-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 960px) {
  .plan-hero {
    grid-template-columns: 1fr;
  }

  .timeline-item {
    grid-template-columns: 1fr;
  }
}
</style>
