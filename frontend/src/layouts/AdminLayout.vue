<template>
  <div class="layout">
    <aside class="sidebar">
      <div class="brand">
        <div class="logo">FP</div>
        <div>
          <div class="title">健身平台</div>
          <div class="subtitle">管理控制台</div>
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
        <el-menu-item index="/permission-center">权限中心</el-menu-item>
        <el-menu-item index="/dashboard">运营概览</el-menu-item>
        <el-menu-item index="/courses">课程</el-menu-item>
        <el-menu-item index="/schedules">排期</el-menu-item>
        <el-menu-item index="/orders">订单</el-menu-item>
        <el-menu-item index="/videos">视频</el-menu-item>
        <el-menu-item index="/content">运营内容</el-menu-item>
        <el-menu-item index="/permission-matrix">权限矩阵</el-menu-item>
        <el-menu-item index="/role-permission">角色权限分配</el-menu-item>
        <el-menu-item index="/role-scope">角色分类范围</el-menu-item>
        <el-menu-item index="/coach-apply">教练申请</el-menu-item>
        <el-menu-item index="/user-roles">用户与角色</el-menu-item>
        <el-menu-item index="/operation-logs">操作日志</el-menu-item>
      </el-menu>
      <div class="sidebar-footer">
        <div class="tag">V3 就绪</div>
      </div>
    </aside>

    <main class="main">
      <header class="topbar">
        <div class="topbar-left">
          <span class="page-title">{{ pageTitle }}</span>
          <span class="page-sub">稳定 / 可追踪 / 可审计</span>
        </div>
        <div class="topbar-right">
          <div class="user">{{ userLabel }}</div>
          <el-button type="danger" size="small" @click="handleLogout">退出登录</el-button>
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
const userLabel = computed(() => authStore.user?.username || '管理员')

const pageTitle = computed(() => {
  const map = {
    '/dashboard': '运营概览',
    '/courses': '课程管理',
    '/schedules': '课程排期',
    '/orders': '订单与退款',
    '/videos': '视频资产',
    '/content': '运营内容',
    '/permission-center': '权限中心',
    '/permission-matrix': '权限矩阵',
    '/role-permission': '角色权限分配',
    '/role-scope': '角色分类范围',
    '/coach-apply': '教练申请',
    '/user-roles': '用户与角色',
    '/operation-logs': '操作日志'
  }
  return map[route.path] || '控制台'
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
