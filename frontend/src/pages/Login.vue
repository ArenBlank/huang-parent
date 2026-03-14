<template>
  <div class="login-wrapper">
    <el-card class="login-card">
      <div class="login-header">
        <div class="login-title">Fitness Platform Admin</div>
        <div class="login-sub">Sign in to manage operations</div>
      </div>
      <el-form :model="form" label-position="top" @keyup.enter="submit">
        <el-form-item label="Account">
          <el-input v-model="form.account" placeholder="admin / ops_admin / audit_admin" />
        </el-form-item>
        <el-form-item label="Password">
          <el-input v-model="form.password" type="password" show-password placeholder="Enter password" />
        </el-form-item>
        <el-button type="primary" size="large" class="login-button" :loading="loading" @click="submit">
          Sign In
        </el-button>
      </el-form>
      <div class="login-hint">
        Suggested: admin / ops_admin_123 / audit_admin_123 (based on test data)
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '../stores/auth'

const router = useRouter()
const authStore = useAuthStore()
const loading = ref(false)

const form = reactive({
  account: '',
  password: ''
})

const submit = async () => {
  if (!form.account || !form.password) {
    ElMessage.warning('Please enter account and password')
    return
  }
  try {
    loading.value = true
    await authStore.login(form.account, form.password)
    ElMessage.success('Login success')
    router.push('/dashboard')
  } catch (err) {
    ElMessage.error(err.message || 'Login failed')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-wrapper {
  min-height: 100vh;
  display: grid;
  place-items: center;
  padding: 24px;
}

.login-card {
  width: min(420px, 92vw);
  border-radius: 20px;
  background: rgba(8, 15, 18, 0.75);
  border: 1px solid rgba(255, 255, 255, 0.12);
  box-shadow: 0 25px 80px rgba(0, 0, 0, 0.4);
  backdrop-filter: blur(12px);
}

.login-header {
  text-align: center;
  margin-bottom: 16px;
}

.login-title {
  font-size: 20px;
  font-weight: 700;
}

.login-sub {
  font-size: 12px;
  color: #93a8b5;
}

.login-button {
  width: 100%;
  margin-top: 8px;
}

.login-hint {
  margin-top: 16px;
  font-size: 12px;
  color: #8aa0af;
}
</style>
