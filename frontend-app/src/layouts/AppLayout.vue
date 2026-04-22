<template>
  <div class="app-shell">
    <header class="app-hero">
      <div class="hero-left">
        <p class="quest-kicker">Fitness Platform App</p>
        <h1 class="hero-title">把训练、课程、订单与成长放到同一控制台</h1>
        <p class="hero-subtitle">风格、配色和交互语言与 admin 端对齐，优先保证页面流畅度和操作清晰度。</p>
        <div class="hero-badges">
          <span class="badge-pill is-dark">当前页面 {{ currentSection }}</span>
          <span class="badge-pill">当前用户 {{ displayName }}</span>
        </div>
      </div>
      <aside class="hero-right">
        <div class="hero-stat hero-stat--compact">
          <span>训练身份</span>
          <strong>{{ identityLabel }}</strong>
        </div>
        <div class="hero-profile-card">
          <el-avatar :size="64" :src="avatarSrc">{{ avatarFallback }}</el-avatar>
          <strong class="hero-profile-name">{{ displayName }}</strong>
        </div>
        <el-button type="danger" @click="logout">退出登录</el-button>
      </aside>
    </header>

    <nav class="app-nav">
      <button
        v-for="item in navItems"
        :key="item.path"
        type="button"
        class="nav-pill"
        :class="{ active: activePath === item.path }"
        @click="goTo(item.path)"
      >
        <span class="nav-code">{{ item.code }}</span>
        <span>{{ item.label }}</span>
      </button>
    </nav>

    <main class="page">
      <router-view />
    </main>
  </div>
</template>

<script setup>
import { computed, nextTick, onMounted } from "vue"
import { useRoute, useRouter } from "vue-router"
import { useAppAuthStore } from "../stores/auth"

const route = useRoute()
const router = useRouter()
const store = useAppAuthStore()

const navItems = [
  { path: "/home", label: "首页舞台", code: "HM" },
  { path: "/plans", label: "训练计划", code: "PL" },
  { path: "/courses", label: "课程目录", code: "CR" },
  { path: "/booking", label: "预约大厅", code: "BK" },
  { path: "/training", label: "打卡成长", code: "TR" },
  { path: "/orders", label: "订单中心", code: "OD" },
  { path: "/coach-apply", label: "教练申请", code: "CP" },
  { path: "/profile", label: "个人档案", code: "ME" }
]

const sectionMap = Object.fromEntries(navItems.map((item) => [item.path, item.label]))
const activePath = computed(() => route.path)
const currentSection = computed(() => sectionMap[route.path] || "首页舞台")
const displayName = computed(() => store.user?.nickname || store.user?.username || "训练用户")
const identityLabel = computed(() => {
  const occupation = String(store.user?.occupation || "").trim()
  const age = Number(store.user?.age)
  if (occupation && Number.isFinite(age) && age > 0) return `${occupation} · ${age}岁`
  if (occupation) return occupation
  if (Number.isFinite(age) && age > 0) return `${age}岁`
  return "训练会员"
})
const appBase = (import.meta.env.VITE_APP_BASE_URL || "").replace(/\/app\/?$/, "")
const avatarSrc = computed(() => {
  const raw = store.user?.avatar
  if (!raw) return ""
  if (/^https?:\/\//.test(raw)) return raw
  return appBase ? `${appBase}${raw}` : raw
})
const avatarFallback = computed(() => {
  const name = displayName.value || ""
  return name ? name.slice(0, 1).toUpperCase() : "U"
})

const scrollNavIntoView = () => {
  requestAnimationFrame(() => {
    document.querySelector(".app-nav")?.scrollIntoView({ block: "start", behavior: "auto" })
  })
}

const goTo = async (path) => {
  if (route.path !== path) {
    await router.push(path)
  }
  await nextTick()
  scrollNavIntoView()
}

const logout = () => {
  store.logout()
  router.push("/login")
}

onMounted(async () => {
  if (!store.accessToken) return
  try {
    await store.fetchProfile()
  } catch {
    store.logout()
    router.push("/login")
  }
})
</script>

<style scoped>
.app-shell {
  min-height: 100vh;
  padding: 18px 0 28px;
  position: relative;
}

.app-hero,
.app-nav {
  width: min(var(--content-width), calc(100vw - 40px));
  margin: 0 auto 16px;
  position: relative;
  z-index: 1;
}

.app-hero {
  display: grid;
  grid-template-columns: minmax(0, 1.3fr) minmax(300px, 0.7fr);
  gap: 16px;
  padding: 24px;
  border-radius: 34px;
  border: 3px solid var(--eco-border);
  background: linear-gradient(135deg, #6d67ff 0%, #8ca5ff 44%, #ffd07a 100%);
  box-shadow: 0 16px 0 rgba(52, 45, 105, 0.12), 0 24px 34px rgba(91, 83, 255, 0.12);
}

.hero-left {
  color: #ffffff;
}

.hero-right {
  display: grid;
  gap: 10px;
  align-content: start;
}

.hero-stat,
.hero-profile-card {
  padding: 14px 16px;
  border-radius: 20px;
  border: 3px solid var(--eco-border);
  background: rgba(255, 255, 255, 0.9);
  color: var(--eco-text);
  box-shadow: 0 8px 0 rgba(52, 45, 105, 0.08);
}

.hero-stat span {
  display: block;
  color: var(--eco-text-soft);
  font-size: 11px;
  font-weight: 800;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.hero-stat strong {
  display: block;
  margin-top: 8px;
  font-family: "Fredoka", "Nunito", sans-serif;
  font-size: 30px;
  font-weight: 700;
}

.hero-stat--compact {
  padding: 10px 16px;
  border-radius: 18px;
}

.hero-stat--compact strong {
  margin-top: 4px;
  font-size: clamp(22px, 3vw, 30px);
}

.hero-profile-card {
  display: flex;
  align-items: center;
  gap: 18px;
  min-height: 104px;
  padding: 18px 20px;
}

.hero-profile-name {
  min-width: 0;
  font-family: "Fredoka", "Nunito", sans-serif;
  font-size: clamp(30px, 4vw, 42px);
  font-weight: 800;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.app-nav {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  padding: 16px;
  border-radius: 30px;
  border: 3px solid var(--eco-border);
  background: rgba(255, 255, 255, 0.9);
  box-shadow: var(--eco-shadow-soft);
}

.nav-pill {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  min-height: 50px;
  padding: 0 16px;
  border-radius: 999px;
  border: 3px solid var(--eco-border);
  background: #fffdf8;
  color: var(--eco-text);
  font-family: "Nunito", sans-serif;
  font-size: 13px;
  font-weight: 800;
  box-shadow: 0 8px 0 rgba(52, 45, 105, 0.08);
  transition: transform 0.18s ease, box-shadow 0.18s ease;
}

.nav-pill:hover {
  transform: translateY(-2px);
}

.nav-pill.active {
  background: linear-gradient(135deg, #fff0d7, #f5f0ff);
}

.nav-code {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  border: 2px solid var(--eco-border);
  background: #ffffff;
  display: grid;
  place-items: center;
  font-size: 11px;
}

@media (max-width: 1020px) {
  .app-hero {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 960px) {
  .app-hero,
  .app-nav {
    width: min(var(--content-width), calc(100vw - 20px));
  }

}
</style>
