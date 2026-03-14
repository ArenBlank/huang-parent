<template>
  <div class="layout">
    <aside class="sidebar">
      <div class="brand">
        <div class="logo">FP</div>
        <div>
          <div class="title">Fitness Platform</div>
          <div class="subtitle">Admin Console</div>
        </div>
      </div>
      <el-menu
        :default-active="activePath"
        class="menu"
        background-color="transparent"
        text-color="#cfe3ee"
        active-text-color="#2dd4bf"
        router
      >
        <el-menu-item index="/dashboard">Dashboard</el-menu-item>
        <el-menu-item index="/courses">Courses</el-menu-item>
        <el-menu-item index="/schedules">Schedules</el-menu-item>
        <el-menu-item index="/orders">Orders</el-menu-item>
        <el-menu-item index="/videos">Videos</el-menu-item>
        <el-menu-item index="/permission-matrix">Permission Matrix</el-menu-item>
      </el-menu>
      <div class="sidebar-footer">
        <div class="tag">V3 Ready</div>
      </div>
    </aside>

    <main class="main">
      <header class="topbar">
        <div class="topbar-left">
          <span class="page-title">{{ pageTitle }}</span>
          <span class="page-sub">Stable, traceable, auditable</span>
        </div>
        <div class="topbar-right">
          <div class="user">{{ userLabel }}</div>
          <el-button type="danger" size="small" @click="handleLogout">Logout</el-button>
        </div>
      </header>
      <section class="page">
        <router-view />
      </section>
    </main>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

const activePath = computed(() => route.path)
const userLabel = computed(() => authStore.user?.username || 'Admin')

const pageTitle = computed(() => {
  const map = {
    '/dashboard': 'Dashboard',
    '/courses': 'Course Management',
    '/schedules': 'Course Schedules',
    '/orders': 'Orders & Refunds',
    '/videos': 'Video Assets',
    '/permission-matrix': 'Permission Matrix'
  }
  return map[route.path] || 'Console'
})

const handleLogout = () => {
  authStore.logout()
  router.push('/login')
}
</script>

<style scoped>
.layout {
  display: flex;
  min-height: 100vh;
}

.sidebar {
  width: 260px;
  padding: 24px 18px;
  background: rgba(5, 12, 16, 0.72);
  border-right: 1px solid rgba(255, 255, 255, 0.08);
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.brand {
  display: flex;
  gap: 12px;
  align-items: center;
}

.logo {
  width: 44px;
  height: 44px;
  border-radius: 12px;
  background: linear-gradient(135deg, #2dd4bf, #0891b2);
  display: grid;
  place-items: center;
  font-weight: 700;
  color: #03171a;
}

.title {
  font-size: 16px;
  font-weight: 600;
}

.subtitle {
  font-size: 12px;
  color: #8aa6b5;
}

.menu {
  border-right: none;
  background: transparent;
}

.sidebar-footer {
  margin-top: auto;
}

.main {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.topbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px 28px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
  background: rgba(10, 18, 22, 0.5);
  backdrop-filter: blur(8px);
}

.page-title {
  font-size: 20px;
  font-weight: 600;
}

.page-sub {
  margin-left: 12px;
  font-size: 12px;
  color: #86a4b3;
}

.topbar-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.user {
  padding: 6px 12px;
  background: rgba(45, 212, 191, 0.15);
  border-radius: 999px;
  font-size: 12px;
}

@media (max-width: 960px) {
  .layout {
    flex-direction: column;
  }

  .sidebar {
    width: 100%;
    flex-direction: row;
    flex-wrap: wrap;
  }

  .main {
    width: 100%;
  }
}
</style>
