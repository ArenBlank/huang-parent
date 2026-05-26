<template>
  <div class="page-stack">
    <section class="hero-panel">
      <div class="plan-hero">
        <div>
          <p class="quest-kicker">Plan Hub</p>
          <h1 class="hero-title">先选训练主线，再稳定推进每一天</h1>
          <p class="hero-subtitle">
            训练计划页现在分成“我的计划”和“发现大厅”两条线：左侧快速切换，右侧专注查看详情与动作节点，AI
            定制计划也会直接进入你的个人训练区。
          </p>
          <div class="hero-badges">
            <span class="badge-pill is-dark">我的计划 {{ myPlans.length }}</span>
            <span class="badge-pill is-dark">发现大厅 {{ libraryPlans.length }}</span>
            <span class="badge-pill is-dark">当前动作 {{ selected?.items?.length || 0 }}</span>
          </div>
        </div>

        <div class="progress-panel">
          <div class="progress-title-row">
            <p class="progress-title">订阅进度</p>
            <el-tooltip
              placement="top"
              effect="light"
              :width="340"
              content="当前进度条展示的是计划活跃度估算，不是视频观看进度。规则：未订阅时=18+动作数×6（无动作时为10），最高92%；已订阅时=38+动作数×8（无动作时为35），最高100%。例如已订阅且 5 个动作时为 78%，已订阅且 14 个动作时会封顶显示 100%。观看视频目前不会回写这个值。"
            >
              <span class="progress-help" aria-label="进度说明" tabindex="0" role="button">
                <el-icon><WarningFilled /></el-icon>
              </span>
            </el-tooltip>
          </div>
          <el-progress :percentage="itemProgress" />
          <p class="muted progress-note">
            {{
              selected?.subscription
                ? `已订阅，开始日期 ${selected.subscription.startDate}`
                : activeTab === 'mine'
                  ? '你还没有激活计划，去发现大厅挑一个开始吧'
                  : '当前计划尚未激活，设置开始日期后即可加入我的计划'
            }}
          </p>
          <p v-if="aiSubmitting" class="progress-ai-note">
            ✨ AI 教练正在后台编排新计划，你可以继续浏览当前内容。
          </p>
        </div>
      </div>
    </section>

    <section class="plan-grid">
      <article class="card">
        <div class="toolbar">
          <div>
            <h2 class="section-title">计划列表</h2>
            <p class="section-sub">
              “我的计划”只放你已经激活的训练主线；“发现大厅”则展示公共计划和你自己的未激活 AI 专属计划。
            </p>
          </div>
          <div class="toolbar-actions">
            <el-button class="ai-magic-button" type="primary" :loading="aiSubmitting" @click="openAiDialog">
              {{ aiSubmitting ? '✨ AI 定制中...' : '✨ AI 一键定制' }}
            </el-button>
            <el-button type="primary" plain @click="loadOverview" :loading="loading">刷新计划</el-button>
          </div>
        </div>

        <el-tabs v-model="activeTab" class="plan-tabs">
          <el-tab-pane :label="`我的计划 ${myPlans.length}`" name="mine">
            <el-empty v-if="!myPlans.length" description="你还没有激活训练计划">
              <div class="empty-actions">
                <el-button type="primary" size="small" @click="activeTab = 'library'">去发现大厅挑选</el-button>
              </div>
            </el-empty>

            <div v-else class="plan-list">
              <button
                v-for="plan in pagedMyPlans"
                :key="plan.id"
                type="button"
                class="plan-card eco-clickable"
                :class="{ 'plan-card--active': selected?.plan?.id === plan.id }"
                @click="selectPlan(plan)"
              >
                <div class="plan-head">
                  <strong>{{ plan.title }}</strong>
                  <span class="tag tag--active">已激活</span>
                </div>
                <p class="plan-goal">{{ plan.goal || '按周期推进训练目标' }}</p>
                <div class="plan-meta">
                  <span>开始于 {{ plan.startDate || '未设置' }}</span>
                  <span>周期 {{ plan.durationWeeks || 0 }} 周</span>
                  <span v-if="plan.planType === 'AI_PRIVATE'">AI 专属</span>
                </div>
              </button>
            </div>
            <el-pagination
              v-if="myPlans.length > planPageSize"
              v-model:current-page="myPlanPage"
              class="compact-pagination"
              small
              background
              layout="prev, pager, next"
              :page-size="planPageSize"
              :total="myPlans.length"
            />
          </el-tab-pane>

          <el-tab-pane :label="`发现大厅 ${libraryPlans.length}`" name="library">
            <el-empty v-if="!libraryPlans.length" description="当前没有新的可激活计划" />

            <div v-else class="plan-list">
              <button
                v-for="plan in pagedLibraryPlans"
                :key="plan.id"
                type="button"
                class="plan-card eco-clickable"
                :class="{ 'plan-card--active': selected?.plan?.id === plan.id }"
                @click="selectPlan(plan)"
              >
                <div class="plan-head">
                  <strong>{{ plan.title }}</strong>
                  <span class="tag">{{ plan.planType === 'AI_PRIVATE' ? 'AI 专属' : formatDifficulty(plan.level) }}</span>
                </div>
                <p class="plan-goal">{{ plan.goal || '按周期推进训练目标' }}</p>
                <div class="plan-meta">
                  <span>周期 {{ plan.durationWeeks || 0 }} 周</span>
                  <span>{{ plan.planType === 'AI_PRIVATE' ? '你的私有计划' : '公共计划' }}</span>
                </div>
              </button>
            </div>
            <el-pagination
              v-if="libraryPlans.length > planPageSize"
              v-model:current-page="libraryPlanPage"
              class="compact-pagination"
              small
              background
              layout="prev, pager, next"
              :page-size="planPageSize"
              :total="libraryPlans.length"
            />
          </el-tab-pane>
        </el-tabs>
      </article>

      <article class="card">
        <div class="toolbar">
          <div>
            <h2 class="section-title">计划详情</h2>
            <p class="section-sub">激活后计划会进入“我的计划”，动作节点支持直接进入视频播放页继续训练。</p>
          </div>
          <div class="detail-actions">
            <el-button
              v-if="selected?.subscription"
              type="danger"
              plain
              :disabled="!selected"
              :loading="unsubmitting"
              @click="cancelActivation"
            >
              取消激活
            </el-button>
            <el-button type="success" :disabled="!selected" :loading="submitting" @click="subscribe">
              {{ selected?.subscription ? '更新订阅日期' : '激活计划' }}
            </el-button>
          </div>
        </div>

        <el-empty v-if="!selected" description="请选择一个训练计划" />

        <div v-else class="detail-shell">
          <div class="summary-card">
            <div class="summary-head">
              <div>
                <h3>{{ selected.plan?.title }}</h3>
                <p>{{ selected.plan?.goal || '暂无目标描述' }}</p>
              </div>
              <span class="tag">
                {{
                  selected.planType === 'AI_PRIVATE'
                    ? 'AI 专属'
                    : selected?.subscription
                      ? '已激活'
                      : formatDifficulty(selected.plan?.level)
                }}
              </span>
            </div>
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
            <el-alert
              v-else
              type="info"
              :closable="false"
              show-icon
              :title="selected.planType === 'AI_PRIVATE' ? '这是你的 AI 专属计划，设置日期后即可重新激活' : '这是公共计划，设置日期后即可加入我的计划'"
            />
          </div>

          <section>
            <h3 class="section-title-sm">动作节点</h3>
            <p class="section-sub">训练动作按天展开，可快速查看时长、组数和绑定视频。</p>
            <el-empty v-if="!selected.items?.length" description="该计划还没有动作节点" />
            <div v-else class="timeline-list">
              <article v-for="item in pagedPlanItems" :key="item.id" class="timeline-item">
                <div class="timeline-rail" aria-hidden="true">
                  <span class="timeline-dot"></span>
                </div>
                <div class="timeline-content">
                  <div class="timeline-head">
                    <div class="timeline-title">
                      <span class="day-chip">DAY {{ item.dayIndex }}</span>
                      <strong>{{ item.actionName }}</strong>
                    </div>
                    <span class="tag">{{ item.durationMin || 0 }} 分钟</span>
                  </div>
                  <p class="muted">组数 {{ item.sets || 0 }} · 次数 {{ item.reps || 0 }} · 休息 {{ item.restSec || 0 }} 秒</p>
                  <div class="timeline-actions">
                    <el-button type="primary" :disabled="!item.video?.playUrl" @click="openVideo(item)">
                      {{ item.video?.playUrl ? '查看视频' : '暂无视频' }}
                    </el-button>
                    <span class="muted">{{ item.video?.title || '当前节点未绑定视频资源' }}</span>
                  </div>
                </div>
              </article>
            </div>
            <el-pagination
              v-if="planItems.length > itemPageSize"
              v-model:current-page="itemPage"
              class="compact-pagination"
              small
              background
              layout="prev, pager, next"
              :page-size="itemPageSize"
              :total="planItems.length"
            />
          </section>
        </div>
      </article>
    </section>

    <el-dialog
      v-model="aiDialogVisible"
      title="✨ 让 AI 为你定制专属计划"
      width="560px"
      destroy-on-close
      append-to-body
    >
      <div class="ai-dialog-copy">
        AI 会根据你的目标、器械条件和训练频率生成一份可直接落库的训练计划。提交后你仍然可以先关闭弹窗，继续浏览页面。
      </div>

      <el-input
        v-model="aiPrompt"
        type="textarea"
        :rows="5"
        maxlength="300"
        show-word-limit
        resize="none"
        :disabled="aiSubmitting"
        placeholder="请描述你的身体状况和目标，例如：我是新手男生，想在家里用哑铃减脂，每周练3天"
      />

      <div v-if="aiSubmitting" class="ai-status-note" aria-live="polite">
        <span class="ai-status-dot"></span>
        <span>⏳ AI 教练正在为您深度推演最优动作编排，通常需要 10~20 秒，请耐心等待...</span>
      </div>

      <template #footer>
        <div class="dialog-footer">
          <el-button @click="closeAiDialog">{{ aiSubmitting ? '先关闭' : '取消' }}</el-button>
          <el-button type="primary" :loading="aiSubmitting" :disabled="aiSubmitting" @click="submitAiPlan">
            提交
          </el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { WarningFilled } from '@element-plus/icons-vue'
import { isHandledBusinessError } from '../api/client'
import { fetchPlanDetail, fetchPlanOverview, generateAiPlan, subscribePlan, unsubscribePlan } from '../api/plan'
import { createVideoPlaylist, saveVideoPlaylist } from '../utils/videoPlaylist'

const router = useRouter()
const loading = ref(false)
const submitting = ref(false)
const unsubmitting = ref(false)
const aiDialogVisible = ref(false)
const aiPrompt = ref('')
const aiSubmitting = ref(false)
const activeTab = ref('mine')
const overview = ref({
  myPlans: [],
  libraryPlans: [],
  currentPlanId: null
})
const selected = ref(null)
const selectedMeta = ref(null)
const startDate = ref(formatDate(new Date()))
const syncingTab = ref(false)
const planPageSize = 3
const itemPageSize = 2
const myPlanPage = ref(1)
const libraryPlanPage = ref(1)
const itemPage = ref(1)

const myPlans = computed(() => overview.value.myPlans || [])
const libraryPlans = computed(() => overview.value.libraryPlans || [])
const pagedMyPlans = computed(() => {
  const start = (myPlanPage.value - 1) * planPageSize
  return myPlans.value.slice(start, start + planPageSize)
})
const pagedLibraryPlans = computed(() => {
  const start = (libraryPlanPage.value - 1) * planPageSize
  return libraryPlans.value.slice(start, start + planPageSize)
})
const planItems = computed(() => selected.value?.items || [])
const pagedPlanItems = computed(() => {
  const start = (itemPage.value - 1) * itemPageSize
  return planItems.value.slice(start, start + itemPageSize)
})

const itemProgress = computed(() => {
  const total = selected.value?.items?.length || 0
  if (!total) return selected.value?.subscription ? 35 : 10
  return selected.value?.subscription ? Math.min(38 + total * 8, 100) : Math.min(18 + total * 6, 92)
})

function formatDate(date) {
  const local = new Date(date.getTime() - date.getTimezoneOffset() * 60000)
  return local.toISOString().slice(0, 10)
}

function formatDifficulty(level) {
  switch ((level || '').toLowerCase()) {
    case 'advanced':
      return '高阶'
    case 'intermediate':
      return '进阶'
    case 'beginner':
      return '入门'
    default:
      return '标准难度'
  }
}

function resolveRowById(planId) {
  if (!planId) return null
  return myPlans.value.find((item) => item.id === planId) || libraryPlans.value.find((item) => item.id === planId) || null
}

async function loadOverview(options = {}) {
  const { preferredId = null, preferredTab = null } = options
  try {
    loading.value = true
    const { data } = await fetchPlanOverview()
    if (data.code !== 200) {
      throw new Error(data.message || '加载计划概览失败')
    }

    overview.value = {
      myPlans: data.data?.myPlans || [],
      libraryPlans: data.data?.libraryPlans || [],
      currentPlanId: data.data?.currentPlanId || null
    }
    myPlanPage.value = Math.min(myPlanPage.value, Math.max(1, Math.ceil(myPlans.value.length / planPageSize)))
    libraryPlanPage.value = Math.min(libraryPlanPage.value, Math.max(1, Math.ceil(libraryPlans.value.length / planPageSize)))

    let nextTab = preferredTab || (myPlans.value.length ? 'mine' : 'library')
    if (nextTab === 'mine' && !myPlans.value.length) {
      nextTab = libraryPlans.value.length ? 'library' : 'mine'
    }
    if (nextTab === 'library' && !libraryPlans.value.length) {
      nextTab = myPlans.value.length ? 'mine' : 'library'
    }

    syncingTab.value = true
    activeTab.value = nextTab
    if (nextTab === 'mine') myPlanPage.value = 1
    if (nextTab === 'library') libraryPlanPage.value = 1
    itemPage.value = 1
    const targetId = preferredId || overview.value.currentPlanId
    await syncSelectedPlan(targetId, nextTab)
  } catch (err) {
    ElMessage.error(err.message || '加载计划概览失败')
  } finally {
    syncingTab.value = false
    loading.value = false
  }
}

async function syncSelectedPlan(preferredId = null, tab = activeTab.value) {
  const list = tab === 'mine' ? myPlans.value : libraryPlans.value
  if (!list.length) {
    selected.value = null
    selectedMeta.value = null
    startDate.value = formatDate(new Date())
    return
  }

  const currentId = selected.value?.plan?.id
  const target = list.find((item) => item.id === preferredId) || list.find((item) => item.id === currentId) || list[0]
  await selectPlan(target)
}

async function selectPlan(row) {
  if (!row?.id) {
    return
  }

  try {
    const { data } = await fetchPlanDetail(row.id)
    if (data.code !== 200) {
      throw new Error(data.message || '加载计划详情失败')
    }
    selectedMeta.value = row
    selected.value = data.data
    itemPage.value = 1
    if (selected.value?.subscription?.startDate) {
      startDate.value = selected.value.subscription.startDate
    } else {
      startDate.value = formatDate(new Date())
    }
  } catch (err) {
    ElMessage.error(err.message || '加载计划详情失败')
  }
}

async function subscribe() {
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
    const { data } = await subscribePlan({
      planId: selected.value.plan.id,
      startDate: startDate.value
    })
    if (data.code !== 200) {
      throw new Error(data.message || '激活计划失败')
    }
    ElMessage.success(`计划已保存，开始日期 ${startDate.value}`)
    await loadOverview({
      preferredTab: 'mine',
      preferredId: selected.value.plan.id
    })
  } catch (err) {
    ElMessage.error(err.message || '激活计划失败')
  } finally {
    submitting.value = false
  }
}

async function cancelActivation() {
  if (!selected.value?.plan?.id) {
    return
  }

  try {
    await ElMessageBox.confirm(
      '取消激活后，该计划会从“我的计划”移回“发现大厅”，你仍然可以稍后再次激活。',
      '确认取消激活？',
      {
        type: 'warning',
        confirmButtonText: '确认取消',
        cancelButtonText: '再想想'
      }
    )
  } catch {
    return
  }

  try {
    unsubmitting.value = true
    const planId = selected.value.plan.id
    const { data } = await unsubscribePlan({ planId })
    if (data.code !== 200) {
      throw new Error(data.message || '取消激活失败')
    }
    ElMessage.success('已取消激活')
    await loadOverview({
      preferredTab: 'library',
      preferredId: planId
    })
  } catch (err) {
    if (err !== 'cancel') {
      ElMessage.error(err.message || '取消激活失败')
    }
  } finally {
    unsubmitting.value = false
  }
}

function openAiDialog() {
  aiDialogVisible.value = true
}

function closeAiDialog() {
  aiDialogVisible.value = false
}

async function submitAiPlan() {
  const prompt = aiPrompt.value.trim()
  if (!prompt) {
    ElMessage.warning('请先描述你的训练需求')
    return
  }
  if (aiSubmitting.value) {
    return
  }

  try {
    aiSubmitting.value = true
    const { data } = await generateAiPlan({ userPrompt: prompt })
    if (data.code !== 200) {
      throw new Error(data.message || '生成计划失败')
    }

    aiDialogVisible.value = false
    aiPrompt.value = ''
    await loadOverview({
      preferredTab: 'mine',
      preferredId: data.data?.planId || null
    })
    ElMessage.success('专属计划已生成！')
  } catch (err) {
    if (!isHandledBusinessError(err)) {
      ElMessage.error(err.message || '生成计划失败')
    }
  } finally {
    aiSubmitting.value = false
  }
}

function openVideo(item) {
  const playUrl = item?.video?.playUrl
  if (!playUrl) {
    return
  }

  const playlist = createVideoPlaylist({
    items: selected.value?.items || [],
    source: selected.value?.plan?.title || '训练计划',
    cover: selected.value?.plan?.coverUrl || '',
    fallbackTitle: '训练教学视频'
  })
  const playlistIndex = playlist.findIndex((entry) => entry.id === item?.id)
  const playlistKey = saveVideoPlaylist(playlist, `plan-${selected.value?.plan?.id || 'detail'}`)

  router.push({
    path: '/video-player',
    query: {
      playlistKey,
      index: String(playlistIndex >= 0 ? playlistIndex : 0),
      url: playUrl,
      title: item?.video?.title || item?.actionName || '训练教学视频',
      cover: selected.value?.plan?.coverUrl || '',
      source: selected.value?.plan?.title || '训练计划',
      action: item?.actionName || ''
    }
  })
}

watch(activeTab, async (tab) => {
  if (tab === 'mine') myPlanPage.value = 1
  if (tab === 'library') libraryPlanPage.value = 1
  itemPage.value = 1
  if (syncingTab.value) {
    return
  }
  await syncSelectedPlan(null, tab)
})

onMounted(() => {
  loadOverview()
})
</script>

<style scoped>
.plan-hero {
  display: grid;
  grid-template-columns: minmax(360px, 1.25fr) minmax(280px, 0.75fr);
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

.progress-title-row {
  display: flex;
  align-items: center;
  gap: 8px;
}

.progress-help {
  width: 20px;
  height: 20px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.18);
  color: rgba(255, 248, 188, 0.98);
  cursor: help;
  transition: transform 0.2s ease, background-color 0.2s ease;
}

.progress-help:hover {
  transform: translateY(-1px);
  background: rgba(255, 255, 255, 0.26);
}

.progress-help:focus-visible {
  outline: 2px solid rgba(255, 248, 188, 0.9);
  outline-offset: 2px;
}

.progress-note {
  margin: 8px 0 0;
  color: rgba(244, 255, 239, 0.9);
}

.progress-ai-note {
  margin: 10px 0 0;
  color: rgba(255, 248, 188, 0.95);
  font-size: 13px;
  line-height: 1.6;
}

.ai-magic-button {
  background: linear-gradient(135deg, #6d67ff 0%, #8f7dff 52%, #ffbc6d 100%);
  border: none;
  font-weight: 700;
  box-shadow: 0 8px 18px rgba(109, 103, 255, 0.24);
}

.plan-grid {
  display: grid;
  grid-template-columns: minmax(320px, 0.95fr) minmax(300px, 1.05fr);
  gap: 16px;
}

.plan-tabs {
  margin-top: 8px;
}

.plan-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.compact-pagination {
  margin-top: 12px;
  justify-content: center;
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

.tag--active {
  background: rgba(76, 196, 126, 0.12);
  color: #2c8a57;
  border-color: rgba(76, 196, 126, 0.35);
}

.detail-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  justify-content: flex-end;
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

.summary-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
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
  grid-template-columns: 26px 1fr;
  gap: 12px;
}

.timeline-rail {
  position: relative;
  display: flex;
  justify-content: center;
  padding-top: 6px;
}

.timeline-rail::after {
  content: "";
  position: absolute;
  top: 26px;
  bottom: -18px;
  width: 2px;
  border-radius: 999px;
  background: rgba(109, 103, 255, 0.18);
}

.timeline-item:last-child .timeline-rail::after {
  display: none;
}

.timeline-dot {
  position: relative;
  z-index: 1;
  width: 12px;
  height: 12px;
  border-radius: 999px;
  border: 3px solid #ffffff;
  background: var(--eco-primary);
  box-shadow: 0 0 0 2px rgba(109, 103, 255, 0.22);
}

.timeline-head {
  display: flex;
  justify-content: space-between;
  gap: 10px;
  align-items: center;
}

.timeline-title {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.day-chip {
  display: inline-flex;
  align-items: center;
  min-height: 26px;
  border-radius: 999px;
  border: 1px solid rgba(52, 45, 105, 0.22);
  background: #f2efff;
  padding: 0 10px;
  color: var(--eco-primary);
  font-size: 12px;
  font-weight: 800;
  letter-spacing: 0.04em;
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

.ai-dialog-copy {
  margin-bottom: 12px;
  color: var(--eco-text-soft);
  line-height: 1.7;
}

.ai-status-note {
  margin-top: 14px;
  display: flex;
  align-items: center;
  gap: 10px;
  border-radius: 14px;
  padding: 12px 14px;
  background: linear-gradient(135deg, rgba(109, 103, 255, 0.1), rgba(255, 188, 109, 0.12));
  color: var(--eco-primary);
  font-size: 13px;
  line-height: 1.6;
}

.ai-status-dot {
  width: 10px;
  height: 10px;
  border-radius: 999px;
  background: linear-gradient(135deg, #6d67ff, #ffbc6d);
  box-shadow: 0 0 0 0 rgba(109, 103, 255, 0.36);
  animation: ai-breathe 1.8s ease-in-out infinite;
  flex: 0 0 auto;
}

.empty-actions {
  display: flex;
  justify-content: center;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

@keyframes ai-breathe {
  0% {
    transform: scale(0.92);
    box-shadow: 0 0 0 0 rgba(109, 103, 255, 0.3);
  }

  50% {
    transform: scale(1.08);
    box-shadow: 0 0 0 10px rgba(109, 103, 255, 0);
  }

  100% {
    transform: scale(0.92);
    box-shadow: 0 0 0 0 rgba(109, 103, 255, 0);
  }
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
    grid-template-columns: 24px 1fr;
  }

  .summary-head {
    flex-direction: column;
  }

  .detail-actions {
    justify-content: flex-start;
  }
}
</style>
