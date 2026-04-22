<template>
  <div class="academy-shell">
    <a class="skip-link" href="#admin-main">跳到主内容</a>

    <header class="academy-header">
      <div class="brand-cluster">
        <div class="brand-bubble">FP</div>
        <div>
          <div class="brand-title">健身学院控制台</div>
          <div class="brand-sub">把训练、内容、订单和权限做成一块更好用的运营画布。</div>
        </div>
      </div>

      <div class="header-actions">
        <div class="header-chip">
          <span>账号</span>
          <strong>{{ userLabel }}</strong>
        </div>
        <div class="header-chip">
          <span>角色</span>
          <strong>{{ roleLabel }}</strong>
        </div>
        <el-button type="danger" @click="handleLogout">退出登录</el-button>
      </div>
    </header>

    <section class="academy-hero">
      <div class="hero-copy">
        <div class="hero-badge">Playful Control Deck</div>
        <h1 class="hero-title">{{ pageTitle }}</h1>
        <p>{{ pageDescription }}</p>
        <div class="hero-actions">
          <button
            v-for="action in heroActions"
            :key="action.path"
            type="button"
            class="hero-pill"
            :class="{ active: route.path === action.path }"
            @click="router.push(action.path)"
          >
            <span class="hero-pill-code">{{ action.code }}</span>
            <span>{{ action.label }}</span>
          </button>
        </div>
      </div>

      <div class="hero-card">
        <div class="hero-card-head">
          <div>
            <div class="hero-card-kicker">Quick Radar</div>
            <div class="hero-card-title">Academy Radar</div>
          </div>
          <div class="hero-card-badge">{{ radarCards.length }} 个入口</div>
        </div>
        <div class="hero-card-copy">把课程、核销、训练、权限和订单入口压成一个更好扫读的工作区。</div>
        <div class="hero-card-grid">
          <button
            v-for="card in radarCards"
            :key="card.path"
            type="button"
            class="hero-mini-card radar-link"
            :class="[card.tone, { wide: card.wide }]"
            @click="router.push(card.path)"
          >
            <div class="hero-mini-card-top">
              <span>{{ card.title }}</span>
              <em>{{ card.code }}</em>
            </div>
            <strong>{{ card.headline }}</strong>
            <small>{{ card.note }}</small>
          </button>
        </div>
      </div>
    </section>

    <nav class="academy-nav">
      <div
        v-for="section in navSections"
        :key="section.title"
        class="nav-group"
      >
        <div class="nav-group-title">{{ section.title }}</div>
        <div class="nav-group-items">
          <button
            v-for="item in section.items"
            :key="item.path"
            type="button"
            class="nav-pill"
            :class="{ active: route.path === item.path }"
            @click="router.push(item.path)"
          >
            <span class="nav-code">{{ item.code }}</span>
            <span>{{ item.label }}</span>
          </button>
        </div>
      </div>
    </nav>

    <main id="admin-main" class="academy-main">
      <router-view />
    </main>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'

const navSections = [
  {
    title: '首页舞台',
    items: [
      { path: '/dashboard', label: '学院主舞台', code: 'HM' },
      { path: '/permission-center', label: '权限学院', code: 'AC' },
      { path: '/content', label: '内容工坊', code: 'CT' }
    ]
  },
  {
    title: '训练工作台',
    items: [
      { path: '/training-plans', label: '训练主线', code: 'PL' },
      { path: '/courses', label: '课程目录', code: 'CR' },
      { path: '/check-in-center', label: '前台核销', code: 'CI' },
      { path: '/schedules', label: '排期课表', code: 'SC' },
      { path: '/videos', label: '视频实验室', code: 'VD' }
    ]
  },
  {
    title: '增长运营',
    items: [
      { path: '/orders', label: '订单成绩单', code: 'OD' },
      { path: '/booking-ops', label: '预约大厅', code: 'BK' },
      { path: '/coach-schedules', label: '教练档期', code: 'CS' },
      { path: '/coach-apply', label: '教练招募', code: 'CP' },
      { path: '/operation-logs', label: '操作足迹', code: 'LG' }
    ]
  },
  {
    title: '角色工作室',
    items: [
      { path: '/permission-matrix', label: '权限矩阵', code: 'MX' },
      { path: '/role-permission', label: '角色许可', code: 'RP' },
      { path: '/role-scope', label: '范围边界', code: 'RS' },
      { path: '/user-roles', label: '角色档案', code: 'UR' }
    ]
  }
]

const pageMeta = {
  '/dashboard': {
    title: '学院主舞台',
    description: '先看课程目录、学员进度、口碑反馈和行动按钮，再往下进入管理动作。'
  },
  '/permission-center': {
    title: '权限学院',
    description: '把角色、矩阵、权限缺口和审计记录组织成更清楚的可视化工作区。'
  },
  '/content': {
    title: '内容工坊',
    description: '用更鲜活的方式管理 Banner、公告和系统配置，而不是普通后台表格。'
  },
  '/training-plans': {
    title: '训练主线编排室',
    description: '从课程宇宙的角度安排训练计划、动作节点和视频绑定。'
  },
  '/check-in-center': {
    title: '前台核销中心',
    description: '给门店前台和课程助教使用的极简核销工作台，支持扫码枪与手动输入。'
  },
  '/orders': {
    title: '订单成绩单',
    description: '把支付、退款和财务摘要拆成更好扫读的学习成绩卡。'
  },
  '/coach-schedules': {
    title: '教练档期管理台',
    description: '在这里编排教练可预约时间段；具体预约单的处理仍然放在预约大厅。'
  },
  '/videos': {
    title: '视频实验室',
    description: '上传、管理和绑定素材时，也保持同一套 playful 控制台体验。'
  }
}

const heroActions = [
  { path: '/content', label: '去内容工坊', code: 'CT' },
  { path: '/videos', label: '去视频实验室', code: 'VD' },
  { path: '/booking-ops', label: '去预约大厅', code: 'BK' }
]

const radarCards = [
  { path: '/courses', title: '课程目录', headline: 'Catalog', note: '维护课程池与课程结构', tone: 'peach', code: 'CR' },
  { path: '/check-in-center', title: '前台核销', headline: 'Check-In', note: '扫码枪快速核销', tone: 'mint', code: 'CI' },
  { path: '/training-plans', title: '训练主线', headline: 'Progress', note: '编排计划节点', tone: 'mint', code: 'PL' },
  { path: '/permission-center', title: '权限学院', headline: 'Voices', note: '查看角色修复闭环', tone: 'butter', code: 'AC' },
  { path: '/orders', title: '订单成绩单', headline: 'Enroll', note: '追踪成交与退款', tone: 'lilac', code: 'OD', wide: true }
]

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

const userLabel = computed(() => authStore.user?.username || '管理员')
const roleLabel = computed(() => authStore.roles?.[0] || '未识别')
const pageTitle = computed(() => pageMeta[route.path]?.title || '健身学院控制台')
const pageDescription = computed(() => pageMeta[route.path]?.description || '把管理后台变成更友好、更有识别度的控制台。')

const handleLogout = () => {
  authStore.logout()
  router.push('/login')
}
</script>

<style scoped>
.academy-shell {
  min-height: 100vh;
  padding: 18px 0 34px;
}

.academy-header,
.academy-hero,
.academy-nav {
  width: min(1380px, calc(100vw - 40px));
  margin: 0 auto 18px;
}

.academy-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18px;
}

.brand-cluster {
  display: flex;
  align-items: center;
  gap: 16px;
}

.brand-bubble {
  width: 74px;
  height: 74px;
  border-radius: 28px;
  display: grid;
  place-items: center;
  border: 3px solid var(--border);
  background: linear-gradient(180deg, #fff3df, #ffd68f);
  box-shadow: var(--shadow-clay);
  font-family: 'Fredoka', sans-serif;
  font-size: 28px;
  font-weight: 700;
}

.brand-title {
  font-family: 'Fredoka', sans-serif;
  font-size: 30px;
  font-weight: 700;
}

.brand-sub {
  margin-top: 6px;
  color: var(--text-soft);
  font-size: 14px;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
  justify-content: flex-end;
}

.header-chip {
  display: grid;
  gap: 4px;
  min-width: 120px;
  padding: 12px 14px;
  border-radius: 22px;
  border: 3px solid var(--border);
  background: rgba(255, 255, 255, 0.86);
  box-shadow: 0 8px 0 rgba(52, 45, 105, 0.08);
}

.header-chip span {
  color: var(--text-soft);
  font-size: 11px;
  font-weight: 800;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.header-chip strong {
  font-size: 13px;
}

.academy-hero {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(520px, 1.04fr);
  align-items: start;
  gap: 16px;
  padding: 22px;
  border-radius: 38px;
  border: 3px solid var(--border);
  background:
    radial-gradient(circle at top right, rgba(255, 255, 255, 0.28), transparent 30%),
    linear-gradient(135deg, #6d67ff 0%, #8ca5ff 44%, #ffd07a 100%);
  box-shadow: 0 16px 0 rgba(52, 45, 105, 0.12), 0 30px 44px rgba(91, 83, 255, 0.14);
}

.hero-copy {
  color: #ffffff;
}

.hero-title {
  max-width: 10.5ch;
  margin-top: 14px;
  font-size: clamp(32px, 3.6vw, 50px);
  line-height: 0.98;
}

.hero-badge {
  display: inline-flex;
  align-items: center;
  min-height: 34px;
  padding: 0 14px;
  border-radius: 999px;
  border: 2px solid rgba(42, 35, 86, 0.74);
  background: rgba(255, 255, 255, 0.84);
  color: var(--text);
  font-size: 11px;
  font-weight: 800;
  letter-spacing: 0.16em;
  text-transform: uppercase;
  box-shadow: 0 5px 0 rgba(52, 45, 105, 0.08);
  user-select: none;
}

.hero-copy p {
  max-width: 560px;
  margin: 14px 0 0;
  color: rgba(255, 255, 255, 0.92);
  font-size: 14px;
  line-height: 1.72;
}

.hero-actions {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
  max-width: 760px;
  margin-top: 18px;
}

.hero-pill,
.nav-pill {
  display: inline-flex;
  align-items: center;
  justify-content: flex-start;
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
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.hero-pill {
  width: 100%;
}

.hero-pill:hover,
.nav-pill:hover {
  transform: translateY(-2px);
}

.hero-pill.active,
.nav-pill.active {
  background: linear-gradient(135deg, #fff0d7, #f5f0ff);
}

.hero-pill-code,
.nav-code {
  display: inline-grid;
  place-items: center;
  width: 28px;
  height: 28px;
  border-radius: 999px;
  border: 2px solid var(--border);
  background: #ffffff;
  font-size: 11px;
}

.hero-card {
  display: grid;
  align-content: start;
  gap: 12px;
  padding: 18px;
  border-radius: 30px;
  border: 3px solid var(--border);
  background: rgba(255, 255, 255, 0.9);
  box-shadow: 0 10px 0 rgba(52, 45, 105, 0.08);
}

.hero-card-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.hero-card-kicker {
  color: var(--text-soft);
  font-size: 11px;
  font-weight: 800;
  letter-spacing: 0.16em;
  text-transform: uppercase;
}

.hero-card-title {
  margin-top: 4px;
  font-family: 'Fredoka', sans-serif;
  font-size: 24px;
  font-weight: 700;
  line-height: 1.05;
}

.hero-card-badge {
  display: inline-flex;
  align-items: center;
  min-height: 34px;
  padding: 0 14px;
  border-radius: 999px;
  border: 2px solid rgba(42, 35, 86, 0.74);
  background: #fffaf1;
  color: var(--text);
  font-size: 11px;
  font-weight: 800;
  letter-spacing: 0.08em;
  white-space: nowrap;
  box-shadow: 0 4px 0 rgba(52, 45, 105, 0.08);
}

.hero-card-copy {
  color: #625b8c;
  font-size: 13px;
  line-height: 1.55;
}

.hero-card-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}

.hero-mini-card {
  display: grid;
  align-content: start;
  gap: 6px;
  text-align: left;
  min-height: 102px;
  padding: 14px 14px 16px;
  border-radius: 24px;
  border: 3px solid var(--border);
  background: #ffffff;
  box-shadow: 0 8px 0 rgba(52, 45, 105, 0.08);
  transition: transform 0.18s ease, box-shadow 0.18s ease;
}

.hero-mini-card.wide {
  grid-column: span 2;
}

.hero-mini-card:hover {
  transform: translateY(-3px) scale(1.01);
  box-shadow: 0 12px 0 rgba(52, 45, 105, 0.08);
}

.hero-mini-card-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.hero-mini-card span {
  color: var(--text-soft);
  font-size: 11px;
  font-weight: 800;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.hero-mini-card em {
  display: inline-grid;
  place-items: center;
  min-width: 34px;
  height: 28px;
  padding: 0 8px;
  border-radius: 999px;
  border: 2px solid rgba(42, 35, 86, 0.18);
  background: rgba(255, 255, 255, 0.72);
  color: #4a4275;
  font-size: 11px;
  font-style: normal;
  font-weight: 800;
  letter-spacing: 0.08em;
}

.hero-mini-card strong {
  font-family: 'Fredoka', sans-serif;
  font-size: 18px;
  line-height: 1.02;
}

.hero-mini-card small {
  color: #625b8c;
  font-size: 12px;
  line-height: 1.35;
}

.hero-mini-card.peach {
  background: #fff1e4;
}

.hero-mini-card.mint {
  background: #ecfff8;
}

.hero-mini-card.butter {
  background: #fff9dd;
}

.hero-mini-card.lilac {
  background: #f1edff;
}

.academy-nav {
  display: grid;
  gap: 14px;
  padding: 18px;
  border-radius: 32px;
  border: 3px solid var(--border);
  background: rgba(255, 255, 255, 0.88);
  box-shadow: var(--shadow-card);
}

.nav-group {
  display: grid;
  grid-template-columns: 152px 1fr;
  align-items: start;
  gap: 14px;
}

.nav-group-title {
  color: var(--text-soft);
  font-size: 11px;
  font-weight: 800;
  letter-spacing: 0.14em;
  text-transform: uppercase;
}

.nav-group-items {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.nav-pill {
  min-width: 176px;
}

.academy-main {
  min-height: 40vh;
}

@media (max-width: 1080px) {
  .academy-header,
  .academy-hero,
  .academy-nav {
    width: min(100vw - 20px, 1380px);
  }

  .academy-header,
  .academy-hero {
    grid-template-columns: 1fr;
    flex-direction: column;
    align-items: flex-start;
  }

  .hero-actions {
    grid-template-columns: repeat(2, minmax(0, 1fr));
    max-width: 560px;
  }

  .hero-card-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .nav-group {
    grid-template-columns: 1fr;
  }

  .header-actions {
    justify-content: flex-start;
  }
}

@media (max-width: 720px) {
  .hero-actions,
  .hero-card-grid {
    grid-template-columns: 1fr;
  }

  .hero-mini-card.wide {
    grid-column: auto;
  }

  .hero-card-head {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
