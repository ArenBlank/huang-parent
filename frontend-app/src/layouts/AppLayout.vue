<template>
  <div class="layout">
    <header class="topbar">
      <div class="brand">
        <div class="logo">FP</div>
        <div>
          <div class="title">健身平台</div>
          <div class="subtitle">训练助手</div>
        </div>
      </div>
      <div class="user-area">
        <div class="tag">{{ userLabel }}</div>
        <el-button size="small" @click="logout">退出</el-button>
      </div>
    </header>

    <nav class="nav">
      <el-menu mode="horizontal" router :default-active="activePath">
        <el-menu-item index="/home">首页</el-menu-item>
        <el-menu-item index="/plans">计划</el-menu-item>
        <el-menu-item index="/courses">课程</el-menu-item>
        <el-menu-item index="/booking">预约</el-menu-item>
        <el-menu-item index="/training">打卡</el-menu-item>
        <el-menu-item index="/orders">订单</el-menu-item>
        <el-menu-item index="/coach-apply">教练申请</el-menu-item>
        <el-menu-item index="/profile">我的</el-menu-item>
      </el-menu>
    </nav>

    <main class="page">
      <router-view />
    </main>
  </div>
</template>

<script setup>
import { computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAppAuthStore } from '../stores/auth'

const route = useRoute()
const router = useRouter()
const store = useAppAuthStore()

const activePath = computed(() => route.path)
const userLabel = computed(() => store.user?.nickname || store.user?.username || '游客')

const logout = () => {
  store.logout()
  router.push('/login')
}

onMounted(async () => {
  if (!store.user && store.accessToken) {
    try {
      await store.fetchProfile()
    } catch (err) {
      store.logout()
      router.push('/login')
    }
  }
})
</script>

<style scoped>
.layout {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}

.topbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 18px 24px;
  background: #ffffffcc;
  backdrop-filter: blur(10px);
  border-bottom: 1px solid var(--border);
}

.brand {
  display: flex;
  align-items: center;
  gap: 12px;
}

.logo {
  width: 44px;
  height: 44px;
  border-radius: 14px;
  display: grid;
  place-items: center;
  background: linear-gradient(135deg, #3b82f6, #22c55e);
  color: #fff;
  font-weight: 700;
}

.title {
  font-size: 16px;
  font-weight: 600;
}

.subtitle {
  font-size: 12px;
  color: var(--muted);
}

.user-area {
  display: flex;
  align-items: center;
  gap: 10px;
}

.nav {
  background: #ffffffd9;
  border-bottom: 1px solid var(--border);
}

.el-menu {
  border-bottom: none;
}

@media (max-width: 900px) {
  .topbar {
    flex-direction: column;
    gap: 12px;
  }

  .nav {
    overflow-x: auto;
  }
}
</style>
