<template>
  <div class="page-stack">
    <section class="card home-hero">
      <div class="toolbar">
        <div>
          <p class="quest-kicker">App Operations Deck</p>
          <h2 class="section-title">实时运营总览</h2>
          <p class="section-sub">风格与 admin 端统一，首页只保留高价值信息与入口。</p>
        </div>
        <el-button type="primary" :loading="loading" @click="loadAll">刷新首页数据</el-button>
      </div>

      <div class="metric-grid">
        <article class="metric-card">
          <p class="metric-label">计划数</p>
          <p class="metric-value">{{ plans.length }}</p>
          <p class="metric-note">当前可用训练计划</p>
        </article>
        <article class="metric-card">
          <p class="metric-label">课程数</p>
          <p class="metric-value">{{ courses.length }}</p>
          <p class="metric-note">可浏览课程目录</p>
        </article>
        <article class="metric-card">
          <p class="metric-label">订单数</p>
          <p class="metric-value">{{ orders.length }}</p>
          <p class="metric-note">最近订单记录</p>
        </article>
        <article class="metric-card">
          <p class="metric-label">打卡次数</p>
          <p class="metric-value">{{ weeklyCount }}</p>
          <p class="metric-note">来自周统计接口</p>
        </article>
      </div>
    </section>

    <section class="page-split">
      <article class="card surface-lilac">
        <div class="toolbar">
          <div>
            <h2 class="section-title-sm">核心入口</h2>
            <p class="section-sub">入口全部可点，hover 与 admin 保持一致。</p>
          </div>
        </div>
        <div class="entry-grid">
          <button
            v-for="entry in entries"
            :key="entry.path"
            type="button"
            class="entry-card eco-clickable"
            @click="go(entry.path)"
          >
            <span class="entry-code">{{ entry.code }}</span>
            <span class="entry-body">
              <strong>{{ entry.title }}</strong>
              <small>{{ entry.desc }}</small>
            </span>
          </button>
        </div>
      </article>

      <article class="card surface-peach">
        <div class="toolbar">
          <div>
            <h2 class="section-title-sm">训练影响指标</h2>
            <p class="section-sub">圆形进度 + 横向进度，显示训练活跃度变化。</p>
          </div>
          <span class="tag">Live</span>
        </div>
        <div class="impact-row">
          <div class="ring-progress" :style="ringStyle">
            <div class="ring-center">
              <div class="ring-value">{{ carbonFootprint }}</div>
              <div class="ring-unit">tons</div>
            </div>
          </div>
          <div class="impact-meta">
            <p class="metric-label">活跃提升</p>
            <p class="impact-delta">{{ changeRate > 0 ? `+${changeRate}%` : "0%" }}</p>
            <p class="metric-note">根据打卡时长、打卡次数、订单数量计算，数据不足时保持持平。</p>
          </div>
        </div>
        <div class="progress-list">
          <div class="progress-item">
            <span>训练时长</span>
            <div class="progress-track"><div class="progress-fill" :style="{ width: `${energyBar}%` }"></div></div>
            <div class="progress-num">{{ energyBar }}%</div>
          </div>
          <div class="progress-item">
            <span>打卡频率</span>
            <div class="progress-track"><div class="progress-fill" :style="{ width: `${transportBar}%` }"></div></div>
            <div class="progress-num">{{ transportBar }}%</div>
          </div>
          <div class="progress-item">
            <span>订单活跃</span>
            <div class="progress-track"><div class="progress-fill" :style="{ width: `${supplyBar}%` }"></div></div>
            <div class="progress-num">{{ supplyBar }}%</div>
          </div>
        </div>
      </article>
    </section>

    <section class="page-split">
      <article class="card surface-mint">
        <div class="toolbar">
          <div>
            <h2 class="section-title-sm">活动推荐</h2>
            <p class="section-sub">数据来自 `/app/banner/list`。</p>
          </div>
        </div>
        <el-empty v-if="!banners.length" description="暂无活动内容" />
        <div v-else class="signal-list">
          <button
            v-for="item in banners.slice(0, 5)"
            :key="item.id"
            class="signal-item eco-clickable"
            type="button"
            @click="openBanner(item)"
          >
            <span>{{ item.title || `活动 ${item.id}` }}</span>
            <small>{{ item.linkUrl ? "查看详情" : "未配置外链" }}</small>
          </button>
        </div>
      </article>

      <article class="card">
        <div class="toolbar">
          <div>
            <h2 class="section-title-sm">公告更新</h2>
            <p class="section-sub">数据来自 `/app/notice/list`。</p>
          </div>
        </div>
        <el-empty v-if="!notices.length" description="暂无公告" />
        <div v-else class="signal-list">
          <div v-for="notice in notices.slice(0, 5)" :key="notice.id" class="signal-item">
            <span>{{ notice.title || "系统公告" }}</span>
            <small>{{ notice.publishTime || "最近更新" }}</small>
          </div>
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

const router = useRouter()
const loading = ref(false)
const banners = ref([])
const notices = ref([])
const plans = ref([])
const courses = ref([])
const weekly = ref({})
const orders = ref([])

const entries = [
  { path: "/plans", code: "PL", title: "训练计划", desc: "订阅与管理训练主线" },
  { path: "/training", code: "TR", title: "打卡成长", desc: "查看周训练记录和进度" },
  { path: "/courses", code: "CR", title: "课程目录", desc: "浏览课程并进入报名" },
  { path: "/booking", code: "BK", title: "预约大厅", desc: "选择可预约档期" },
  { path: "/orders", code: "OD", title: "订单中心", desc: "查看支付退款与明细" },
  { path: "/profile", code: "ME", title: "个人档案", desc: "维护资料与安全设置" }
]

const weeklyCount = computed(() => Number(weekly.value?.checkinCount ?? weekly.value?.totalCount ?? 0))
const weeklyDuration = computed(() => Number(weekly.value?.totalDurationMin ?? weekly.value?.totalDuration ?? 0))
const orderCount = computed(() => orders.value.length)

const energyBar = computed(() => Math.min(100, Math.round((weeklyDuration.value / 240) * 100)))
const transportBar = computed(() => Math.min(100, Math.round((weeklyCount.value / 14) * 100)))
const supplyBar = computed(() => Math.min(100, Math.round((orderCount.value / 20) * 100)))

const changeRate = computed(() => {
  const score = weeklyCount.value * 8 + Math.round(weeklyDuration.value / 10) + orderCount.value * 2
  return Math.min(35, Math.max(0, score))
})

const carbonFootprint = computed(() => {
  const baseline = 3.2
  const reduce = changeRate.value / 100
  return Math.max(0.8, Number((baseline * (1 - reduce)).toFixed(1)))
})

const ringStyle = computed(() => {
  const p = Math.min(100, Math.max(8, Math.round((changeRate.value / 35) * 100)))
  return { "--progress": p }
})

const go = (path) => router.push(path)

const openBanner = (item) => {
  if (item?.linkUrl) {
    window.open(item.linkUrl, "_blank")
    return
  }
  ElMessage.info("该活动暂未配置跳转链接")
}

const loadAll = async () => {
  try {
    loading.value = true
    const [bannerRes, noticeRes, planRes, courseRes, weeklyRes, orderRes] = await Promise.all([
      appClient.get("/app/banner/list"),
      appClient.get("/app/notice/list", { params: { limit: 10 } }),
      appClient.get("/app/plan/list"),
      appClient.get("/app/course/list"),
      appClient.get("/app/record/my/weekly-stat"),
      appClient.get("/app/order/my/list", { params: { limit: 20 } })
    ])

    if (bannerRes.data.code !== 200) throw new Error(bannerRes.data.message || "加载活动失败")
    if (noticeRes.data.code !== 200) throw new Error(noticeRes.data.message || "加载公告失败")
    if (planRes.data.code !== 200) throw new Error(planRes.data.message || "加载计划失败")
    if (courseRes.data.code !== 200) throw new Error(courseRes.data.message || "加载课程失败")
    if (weeklyRes.data.code !== 200) throw new Error(weeklyRes.data.message || "加载周统计失败")
    if (orderRes.data.code !== 200) throw new Error(orderRes.data.message || "加载订单失败")

    banners.value = bannerRes.data.data || []
    notices.value = noticeRes.data.data || []
    plans.value = planRes.data.data || []
    courses.value = courseRes.data.data || []
    weekly.value = weeklyRes.data.data || {}
    orders.value = orderRes.data.data || []
  } catch (err) {
    ElMessage.error(err.message || "首页数据加载失败")
  } finally {
    loading.value = false
  }
}

loadAll()
</script>

<style scoped>
.home-hero {
  background: linear-gradient(180deg, #fff5ea 0%, #fffdf8 100%);
}

.entry-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 10px;
}

.entry-card {
  border: 3px solid var(--eco-border);
  border-radius: 20px;
  background: #ffffff;
  padding: 12px;
  display: flex;
  align-items: center;
  gap: 10px;
  text-align: left;
  box-shadow: 0 8px 0 rgba(52, 45, 105, 0.08);
}

.entry-code {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  border: 2px solid var(--eco-border);
  background: #ffffff;
  display: grid;
  place-items: center;
  font-size: 11px;
  font-weight: 800;
}

.entry-body {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.entry-body strong {
  font-size: 16px;
  font-family: "Fredoka", "Nunito", sans-serif;
}

.entry-body small {
  color: var(--eco-text-soft);
  font-size: 12px;
}

.impact-row {
  display: grid;
  grid-template-columns: auto 1fr;
  gap: 14px;
  align-items: center;
}

.impact-delta {
  margin: 0;
  font-size: clamp(34px, 5vw, 50px);
  line-height: 1;
  color: var(--eco-primary);
  font-family: "Fredoka", "Nunito", sans-serif;
}

.signal-list {
  display: grid;
  gap: 10px;
}

.signal-item {
  border: 3px solid var(--eco-border);
  border-radius: 16px;
  background: #ffffff;
  padding: 10px 14px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  box-shadow: 0 6px 0 rgba(52, 45, 105, 0.06);
}

.signal-item span {
  font-weight: 700;
}

.signal-item small {
  color: var(--eco-text-soft);
  font-size: 12px;
}

@media (max-width: 1040px) {
  .entry-grid {
    grid-template-columns: 1fr;
  }

  .impact-row {
    grid-template-columns: 1fr;
  }
}
</style>
