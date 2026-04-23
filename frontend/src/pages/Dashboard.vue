<template>
  <div class="section-stack">
    <section class="card surface-lilac landing-hero">
      <div class="landing-copy">
        <div class="eyebrow">平台总览</div>
        <h2 class="display-title">把管理入口按业务链路收进一张更清楚的总览页</h2>
        <p>
          首页只保留真实可用的数据和入口，不放假占位。你可以从这里直接进入训练计划编排、前台内容运营和订单与退款。
        </p>
        <div class="hero-actions">
          <button type="button" class="hero-pill active" @click="router.push('/training-plans')">
            <span class="hero-pill-code">PL</span>
            <span>进入训练计划编排</span>
          </button>
          <button type="button" class="hero-pill" @click="router.push('/content')">
            <span class="hero-pill-code">CT</span>
            <span>打开前台内容运营</span>
          </button>
          <button type="button" class="hero-pill" @click="loadDashboardData">
            <span class="hero-pill-code">RF</span>
            <span>{{ loading ? '刷新中...' : '刷新实时数据' }}</span>
          </button>
        </div>
      </div>

      <div class="hero-metrics">
        <button type="button" class="hero-metric-card peach interactive-card" @click="router.push('/courses')">
          <span>课程管理</span>
          <strong>{{ metrics.courseCount }}</strong>
          <small>{{ metrics.courseCount === 0 ? '当前后端返回课程数为 0' : '当前课程资源数' }}</small>
        </button>
        <button type="button" class="hero-metric-card mint interactive-card" @click="router.push('/user-roles')">
          <span>学员规模</span>
          <strong>{{ metrics.userCount }}</strong>
          <small>{{ metrics.userCount === 0 ? '当前后端返回用户数为 0' : '平台当前用户数' }}</small>
        </button>
        <button type="button" class="hero-metric-card butter interactive-card" @click="router.push('/content')">
          <span>内容活动</span>
          <strong>{{ metrics.bannerCount + metrics.noticeCount }}</strong>
          <small>Banner 与公告总数</small>
        </button>
      </div>
    </section>

    <section class="dashboard-grid">
      <div class="card surface-peach">
        <div class="section-title-sm">高频业务入口</div>
        <div class="section-copy">按完整链路展示训练、订单和内容入口，让首页更像管理总览而不是概念包装页。</div>
        <div class="catalog-grid">
          <button
            v-for="item in catalogPreview"
            :key="item.label"
            type="button"
            class="catalog-card interactive-card"
            @click="router.push(item.path)"
          >
            <div class="catalog-top">
              <span class="catalog-badge">{{ item.badge }}</span>
              <strong>{{ item.label }}</strong>
            </div>
            <div class="catalog-value">{{ item.value }}</div>
            <div class="catalog-note">{{ item.note }}</div>
          </button>
        </div>
      </div>

      <div class="card surface-mint">
        <div class="section-title-sm">进度追踪</div>
        <div class="section-copy">这些比例全部来自真实接口汇总，不再是静态演示数字。</div>
        <div class="progress-stack">
          <div v-for="item in progressDemo" :key="item.label" class="progress-row">
            <div class="progress-head">
              <span>{{ item.label }}</span>
              <strong>{{ item.value }}</strong>
            </div>
            <div class="progress-track">
              <div class="progress-fill" :style="{ width: item.percent }"></div>
            </div>
            <div class="progress-note">{{ item.note }}</div>
          </div>
        </div>
      </div>

      <div class="card surface-mint">
        <div class="section-title-sm">运营动态标签</div>
        <div class="section-copy">每张标签都可点击，鼠标悬停会有轻微浮动，避免用户误判为静态内容。</div>
        <div class="tag-cloud">
          <button
            v-for="item in dynamicTags"
            :key="item.title"
            type="button"
            class="tag-card interactive-card"
            @click="router.push(item.path)"
          >
            <span>{{ item.kicker }}</span>
            <strong>{{ item.title }}</strong>
            <small>{{ item.note }}</small>
          </button>
        </div>
      </div>

      <div class="card cta-card">
        <div class="section-title-sm">行动入口</div>
        <div class="section-copy">保持 3 个最常用动作入口，所有卡片都统一悬浮反馈。</div>
        <div class="cta-stack">
          <button
            v-for="item in ctaLinks"
            :key="item.path"
            type="button"
            class="cta-link-card interactive-card"
            @click="router.push(item.path)"
          >
            <span>{{ item.kicker }}</span>
            <strong>{{ item.title }}</strong>
            <small>{{ item.note }}</small>
          </button>
        </div>
      </div>
    </section>

    <section class="card">
      <div class="toolbar">
        <div>
          <div class="section-title-sm">实时运营数据</div>
          <div class="section-copy">固定展示 6 项核心指标，全部由当前后端真实列表接口汇总得到。</div>
        </div>
      </div>
      <div class="metric-grid">
        <div v-for="item in fixedMetrics" :key="item.label" class="metric-card">
          <div class="metric-label">{{ item.label }}</div>
          <div class="metric-value">{{ item.value }}</div>
          <div class="metric-note">{{ item.note }}</div>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { adminClient } from '../api/client'

const router = useRouter()
const loading = ref(false)

const metrics = reactive({
  userCount: 0,
  activeUserCount: 0,
  courseCount: 0,
  trainingPlanCount: 0,
  orderCount: 0,
  paidOrderCount: 0,
  refundedOrderCount: 0,
  bookingCount: 0,
  bannerCount: 0,
  noticeCount: 0
})

const normalizeList = (payload) => {
  if (Array.isArray(payload)) return payload
  if (Array.isArray(payload?.list)) return payload.list
  if (Array.isArray(payload?.records)) return payload.records
  if (Array.isArray(payload?.rows)) return payload.rows
  return []
}

const percentText = (numerator, denominator) => {
  if (!denominator) return '0%'
  return `${Math.round((numerator / denominator) * 100)}%`
}

const percentWidth = (numerator, denominator) => {
  if (!denominator) return '8%'
  return `${Math.max(8, Math.round((numerator / denominator) * 100))}%`
}

const catalogPreview = computed(() => [
  {
    badge: '主线',
    label: '训练计划编排',
    value: metrics.trainingPlanCount,
    note: metrics.trainingPlanCount === 0 ? '当前暂时没有训练计划' : '去编排训练计划和动作节点',
    path: '/training-plans'
  },
  {
    badge: '订单',
    label: '订单与退款',
    value: metrics.orderCount,
    note: metrics.orderCount === 0 ? '当前暂时没有订单样本' : '查看支付、关闭和退款情况',
    path: '/orders'
  },
  {
    badge: '内容',
    label: '前台内容运营',
    value: metrics.bannerCount + metrics.noticeCount,
    note: metrics.bannerCount + metrics.noticeCount === 0 ? '当前没有 Banner 或公告' : '维护 Banner、公告和配置',
    path: '/content'
  }
])

const progressDemo = computed(() => [
  {
    label: '活跃学员占比',
    value: percentText(metrics.activeUserCount, metrics.userCount),
    percent: percentWidth(metrics.activeUserCount, metrics.userCount),
    note: metrics.userCount === 0 ? '当前没有用户样本数据' : `启用用户 ${metrics.activeUserCount} / 总用户 ${metrics.userCount}`
  },
  {
    label: '预约转化热度',
    value: percentText(metrics.bookingCount, metrics.userCount),
    percent: percentWidth(metrics.bookingCount, metrics.userCount),
    note: metrics.userCount === 0 ? '当前没有用户样本数据' : `预约数 ${metrics.bookingCount} / 用户数 ${metrics.userCount}`
  },
  {
    label: '退款压力',
    value: percentText(metrics.refundedOrderCount, metrics.orderCount),
    percent: percentWidth(metrics.refundedOrderCount, metrics.orderCount),
    note: metrics.orderCount === 0 ? '当前没有订单样本数据' : `退款单 ${metrics.refundedOrderCount} / 订单数 ${metrics.orderCount}`
  }
])

const dynamicTags = computed(() => [
  {
    kicker: '用户',
    title: `学员规模 ${metrics.userCount}`,
    note: metrics.userCount === 0 ? '后端当前返回用户数为 0' : '点击查看角色与账号分配',
    path: '/user-roles'
  },
  {
    kicker: '课程',
    title: `课程资源 ${metrics.courseCount}`,
    note: metrics.courseCount === 0 ? '后端当前返回课程数为 0' : '点击进入课程基础管理',
    path: '/courses'
  },
  {
    kicker: '内容',
    title: `活动入口 ${metrics.bannerCount + metrics.noticeCount}`,
    note: metrics.bannerCount + metrics.noticeCount === 0 ? '当前没有 Banner 或公告' : '点击进入前台内容运营',
    path: '/content'
  }
])

const ctaLinks = [
  {
    kicker: '计划',
    title: '新建训练计划',
    note: '进入训练计划编排页开始维护',
    path: '/training-plans'
  },
  {
    kicker: '订单',
    title: '查看订单与退款',
    note: '进入订单与退款详情',
    path: '/orders'
  },
  {
    kicker: '权限',
    title: '进入权限治理总览',
    note: '查看角色权限与修复入口',
    path: '/permission-center'
  }
]

const fixedMetrics = computed(() => [
  { label: '预约数', value: metrics.bookingCount, note: '来自预约列表接口' },
  { label: '订单数', value: metrics.orderCount, note: '来自订单列表接口' },
  { label: '课程数', value: metrics.courseCount, note: '来自课程列表接口' },
  { label: '训练计划', value: metrics.trainingPlanCount, note: '来自训练计划接口' },
  { label: 'Banner 数', value: metrics.bannerCount, note: '来自 Banner 列表接口' },
  { label: '公告数', value: metrics.noticeCount, note: '来自公告列表接口' }
])

const loadDashboardData = async () => {
  try {
    loading.value = true
    const results = await Promise.allSettled([
      adminClient.get('/admin/user/list'),
      adminClient.get('/admin/course/list'),
      adminClient.get('/admin/training-plan/list'),
      adminClient.get('/admin/ops/order/list'),
      adminClient.get('/admin/ops/booking/list'),
      adminClient.get('/admin/banner/list'),
      adminClient.get('/admin/notice/list')
    ])

    const getData = (result) => {
      if (result.status !== 'fulfilled') return []
      const payload = result.value?.data
      if (payload?.code !== 200) return []
      return normalizeList(payload.data)
    }

    const users = getData(results[0])
    const courses = getData(results[1])
    const trainingPlans = getData(results[2])
    const orders = getData(results[3])
    const bookings = getData(results[4])
    const banners = getData(results[5])
    const notices = getData(results[6])

    metrics.userCount = users.length
    metrics.activeUserCount = users.filter((item) => item.status === 1).length
    metrics.courseCount = courses.length
    metrics.trainingPlanCount = trainingPlans.length
    metrics.orderCount = orders.length
    metrics.paidOrderCount = orders.filter((item) => item.payStatus === 'PAID').length
    metrics.refundedOrderCount = orders.filter(
      (item) => item.refundStatus === 'REFUNDED' || item.orderStatus === 'REFUNDED'
    ).length
    metrics.bookingCount = bookings.length
    metrics.bannerCount = banners.length
    metrics.noticeCount = notices.length
  } catch (err) {
    ElMessage.error(err.message || '加载首页数据失败')
  } finally {
    loading.value = false
  }
}

onMounted(loadDashboardData)
</script>

<style scoped>
.landing-hero {
  display: grid;
  grid-template-columns: minmax(0, 1.1fr) minmax(320px, 0.9fr);
  gap: 18px;
}

.display-title {
  margin-top: 18px;
  font-size: clamp(32px, 3.8vw, 50px);
  line-height: 1.06;
}

.hero-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-top: 22px;
}

.hero-pill {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  min-height: 50px;
  padding: 0 16px;
  border: 3px solid var(--border);
  border-radius: 999px;
  background: #fffdf8;
  color: var(--text);
  font-family: 'Nunito', sans-serif;
  font-size: 13px;
  font-weight: 800;
  box-shadow: 0 8px 0 rgba(52, 45, 105, 0.08);
}

.hero-pill.active {
  background: linear-gradient(135deg, #fff0d7, #f5f0ff);
}

.hero-pill-code {
  display: inline-grid;
  place-items: center;
  width: 28px;
  height: 28px;
  border-radius: 999px;
  border: 2px solid var(--border);
  background: #ffffff;
  font-size: 11px;
}

.hero-metrics {
  display: grid;
  gap: 12px;
}

.hero-metric-card {
  text-align: left;
  min-height: 112px;
  padding: 16px 18px;
  border-radius: 24px;
  border: 3px solid var(--border);
  background: #ffffff;
  box-shadow: 0 10px 0 rgba(52, 45, 105, 0.08);
}

.hero-metric-card span,
.tag-card span,
.cta-link-card span {
  display: block;
  color: #6a638f;
  font-size: 11px;
  font-weight: 800;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.hero-metric-card strong,
.catalog-top strong,
.cta-link-card strong,
.tag-card strong {
  display: block;
  margin-top: 10px;
  color: var(--text);
  font-family: 'Fredoka', sans-serif;
  font-size: 28px;
  line-height: 1.1;
}

.hero-metric-card small,
.tag-card small,
.cta-link-card small {
  display: block;
  margin-top: 8px;
  color: #5f5887;
  font-size: 12px;
  line-height: 1.5;
}

.hero-metric-card.peach {
  background: #fff0e0;
}

.hero-metric-card.mint {
  background: #ebfff7;
}

.hero-metric-card.butter {
  background: #fff6d8;
}

.dashboard-grid {
  display: grid;
  grid-template-columns: 1.08fr 0.92fr;
  gap: 18px;
}

.catalog-grid,
.progress-stack,
.tag-cloud,
.cta-stack {
  display: grid;
  gap: 12px;
  margin-top: 16px;
}

.catalog-card,
.tag-card,
.cta-link-card {
  text-align: left;
  padding: 18px;
  border-radius: 22px;
  border: 3px solid var(--border);
  background: #fffdf8;
  box-shadow: 0 8px 0 rgba(52, 45, 105, 0.08);
}

.catalog-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.catalog-badge {
  display: inline-flex;
  align-items: center;
  min-height: 34px;
  padding: 0 14px;
  border-radius: 999px;
  border: 3px solid var(--border);
  background: #ffffff;
  color: #655d8c;
  font-size: 12px;
  font-weight: 800;
}

.catalog-value {
  margin-top: 16px;
  color: var(--text);
  font-family: 'Fredoka', sans-serif;
  font-size: 44px;
  line-height: 1;
}

.catalog-note {
  margin-top: 12px;
  color: #60598a;
  font-size: 14px;
  line-height: 1.6;
}

.progress-row {
  padding: 18px;
  border-radius: 22px;
  border: 3px solid var(--border);
  background: rgba(255, 255, 255, 0.94);
  box-shadow: 0 8px 0 rgba(52, 45, 105, 0.08);
}

.progress-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
  color: var(--text);
  font-family: 'Fredoka', sans-serif;
  font-size: 20px;
}

.progress-track {
  margin-top: 14px;
  height: 28px;
  padding: 3px;
  border-radius: 999px;
  border: 3px solid var(--border);
  background: #ffffff;
}

.progress-fill {
  height: 100%;
  border-radius: 999px;
  background: linear-gradient(90deg, #6b5cff, #ff8248);
}

.progress-note {
  margin-top: 12px;
  color: #60598a;
  font-size: 14px;
  line-height: 1.6;
}

@media (max-width: 1000px) {
  .landing-hero,
  .dashboard-grid {
    grid-template-columns: 1fr;
  }
}
</style>
