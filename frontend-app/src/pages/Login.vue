<template>
  <div class="login-wrapper">
    <el-card class="login-card">
      <div class="login-header">
        <div class="login-title">健身平台 App</div>
        <div class="login-sub">登录后查看计划与预约</div>
      </div>
      <el-form :model="form" label-position="top" @keyup.enter="submit">
        <el-form-item label="账号">
          <el-input v-model="form.account" placeholder="root / 手机号" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="form.password" type="password" show-password placeholder="请输入密码" />
        </el-form-item>
        <el-button type="primary" size="large" class="login-button" :loading="loading" @click="submit">
          登录
        </el-button>
      </el-form>
      <div class="quick-row">
        <el-button size="small" @click="fillAccount('root', 'root')">填充 root</el-button>
      </div>
      <div class="login-hint">建议账号：root（root）。其它账号以数据库实际密码为准。</div>
    </el-card>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useAppAuthStore } from '../stores/auth'

const router = useRouter()
const store = useAppAuthStore()
const loading = ref(false)

const form = reactive({
  account: '',
  password: ''
})

const fillAccount = (account, password) => {
  form.account = account
  form.password = password
}

const submit = async () => {
  if (!form.account || !form.password) {
    ElMessage.warning('请输入账号和密码')
    return
  }
  try {
    loading.value = true
    await store.login(form.account, form.password)
    ElMessage.success('登录成功')
    router.push('/home')
  } catch (err) {
    const message = err?.message || '登录失败'
    if (message.includes('无法连接后端')) {
      ElMessage.error('后端未启动，请先启动 app 服务（8081）')
    } else if (message.includes('请求超时')) {
      ElMessage.error('请求超时，请检查后端服务是否可用')
    } else if (message.includes('账号') || message.includes('密码')) {
      ElMessage.error('账号或密码错误')
    } else {
      ElMessage.error(message)
    }
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
  background: #ffffffee;
  border: 1px solid var(--border);
  box-shadow: var(--shadow);
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
  color: var(--muted);
}

.login-button {
  width: 100%;
  margin-top: 8px;
}

.login-hint {
  margin-top: 16px;
  font-size: 12px;
  color: var(--muted);
}
</style>
