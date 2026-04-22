<template>
  <div class="login-shell">
    <div class="login-poster">
      <div class="eyebrow">Fitness Platform Admin</div>
      <h1>让训练、订单和权限管理回到同一张控制台里。</h1>
      <p>
        这一版管理端采用更偏运营工作台的视觉语言，不再是普通后台模板。先登录，再进入新的控制台视图。
      </p>
      <div class="poster-grid">
        <div class="poster-card">
          <span class="poster-label">训练域</span>
          <strong>计划 / 课程 / 视频</strong>
        </div>
        <div class="poster-card">
          <span class="poster-label">运营域</span>
          <strong>订单 / 预约 / 内容</strong>
        </div>
        <div class="poster-card">
          <span class="poster-label">权限域</span>
          <strong>矩阵 / 角色 / 审计</strong>
        </div>
      </div>
    </div>

    <el-card class="login-card">
      <div class="login-header">
        <div class="eyebrow">Access</div>
        <div class="login-title">登录管理端</div>
        <div class="login-sub">输入账号后进入运营控制台。</div>
      </div>

      <el-form :model="form" label-position="top" @keyup.enter="submit">
        <el-form-item label="账号">
          <el-input v-model.trim="form.account" placeholder="root_admin / admin / ops_admin / audit_admin" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="form.password" type="password" show-password placeholder="请输入密码" />
        </el-form-item>
        <el-button type="primary" size="large" class="login-button" :loading="loading" @click="submit">
          进入控制台
        </el-button>
      </el-form>

      <div class="quick-row">
        <el-button size="small" @click="fillAccount('root_admin', 'root')">填充 root_admin</el-button>
        <el-button size="small" @click="fillAccount('ops_admin', 'ops_admin_123')">填充 ops_admin</el-button>
        <el-button size="small" @click="fillAccount('audit_admin', 'audit_admin_123')">填充 audit_admin</el-button>
      </div>

      <div class="login-hint">
        推荐账号：
        <span class="mono">root_admin / root</span>
        ，或
        <span class="mono">ops_admin / ops_admin_123</span>
      </div>
    </el-card>

    <AdminSliderCaptcha
      :visible="captchaVisible"
      @success="handleCaptchaSuccess"
      @close="captchaVisible = false"
    />
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import AdminSliderCaptcha from '../components/AdminSliderCaptcha.vue'
import { useAuthStore } from '../stores/auth'

const router = useRouter()
const authStore = useAuthStore()
const loading = ref(false)
const captchaVisible = ref(false)

const form = reactive({
  account: '',
  password: ''
})

const STORAGE_KEY = 'admin_last_account'

const fillAccount = (account, password) => {
  form.account = account
  form.password = password
}

const submit = async () => {
  if (!form.account || !form.password) {
    ElMessage.warning('请输入账号和密码')
    return
  }
  captchaVisible.value = true
}

const handleCaptchaSuccess = async (captchaVerification) => {
  captchaVisible.value = false
  try {
    loading.value = true
    await authStore.login(form.account, form.password, captchaVerification)
    ElMessage.success('登录成功')
    localStorage.setItem(STORAGE_KEY, form.account)
    await router.push('/dashboard')
  } catch (err) {
    ElMessage.error(err.message || '登录失败')
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  const cached = localStorage.getItem(STORAGE_KEY)
  if (cached) {
    form.account = cached
  }
})
</script>

<style scoped>
.login-shell {
  min-height: 100vh;
  display: grid;
  grid-template-columns: minmax(0, 1.2fr) minmax(360px, 420px);
  gap: 28px;
  align-items: stretch;
  padding: 28px;
}

.login-poster {
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  padding: 40px;
  border-radius: 32px;
  color: #ffffff;
  background:
    radial-gradient(circle at top right, rgba(255, 255, 255, 0.1), transparent 28%),
    linear-gradient(135deg, #102847 0%, #163d74 54%, #1f4aa8 100%);
  box-shadow: 0 24px 60px rgba(15, 23, 42, 0.18);
}

.login-poster h1 {
  margin: 14px 0 0;
  max-width: 680px;
  font-family: 'Fira Code', monospace;
  font-size: clamp(30px, 4vw, 54px);
  line-height: 1.08;
}

.login-poster p {
  max-width: 620px;
  margin: 18px 0 0;
  color: rgba(255, 255, 255, 0.8);
  font-size: 15px;
  line-height: 1.8;
}

.poster-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14px;
  margin-top: 32px;
}

.poster-card {
  min-height: 128px;
  padding: 18px;
  border-radius: 22px;
  border: 1px solid rgba(255, 255, 255, 0.12);
  background: rgba(255, 255, 255, 0.06);
  backdrop-filter: blur(8px);
}

.poster-label {
  display: block;
  color: rgba(255, 255, 255, 0.64);
  font-size: 11px;
  letter-spacing: 0.16em;
  text-transform: uppercase;
}

.poster-card strong {
  display: block;
  margin-top: 10px;
  font-size: 18px;
  line-height: 1.45;
}

.login-card {
  align-self: center;
  width: 100%;
  border-radius: 28px;
  border: 1px solid rgba(59, 130, 246, 0.12);
  background: rgba(255, 255, 255, 0.9);
  box-shadow: var(--shadow-lg);
  backdrop-filter: blur(16px);
}

.login-header {
  margin-bottom: 18px;
}

.login-title {
  margin-top: 8px;
  color: var(--text-strong);
  font-family: 'Fira Code', monospace;
  font-size: 26px;
  font-weight: 700;
}

.login-sub {
  margin-top: 8px;
  color: var(--muted);
  font-size: 13px;
}

.login-button {
  width: 100%;
  margin-top: 10px;
}

.quick-row {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 14px;
}

.login-hint {
  margin-top: 18px;
  color: var(--muted);
  font-size: 12px;
  line-height: 1.7;
}

@media (max-width: 1080px) {
  .login-shell {
    grid-template-columns: 1fr;
  }

  .poster-grid {
    grid-template-columns: 1fr;
  }
}
</style>
