<template>
  <div class="login-shell">
    <section class="login-stage hero-panel">
      <div class="stage-copy">
        <p class="quest-kicker">Sustainability Platform</p>
        <h1 class="hero-title">把训练旅程做成一片持续生长的森林</h1>
        <p class="hero-subtitle">
          训练计划、课程预约和打卡记录将形成你的长期成长档案。我们用自然色与有机结构重做了整个 app 端体验。
        </p>

        <div class="metric-grid">
          <article class="metric-card">
            <p class="metric-label">碳足迹预览</p>
            <p class="metric-value">-28%</p>
            <p class="metric-note">按每周规律训练估算，可减少无效出行与碎片化训练成本。</p>
          </article>
          <article class="metric-card">
            <p class="metric-label">认证徽章</p>
            <p class="metric-value">03</p>
            <p class="metric-note">绿色训练流程、课程记录可追踪、订单数据可核验。</p>
          </article>
          <article class="metric-card">
            <p class="metric-label">影响指标</p>
            <p class="metric-value">LIVE</p>
            <p class="metric-note">登录后可查看你的训练进度、计划状态和关键业务数据。</p>
          </article>
        </div>
      </div>

      <div class="login-card">
        <div class="login-top">
          <p class="quest-kicker login-kicker">App Sign In</p>
          <h2 class="login-title">进入用户端</h2>
          <p class="login-sub">登录后即可开始训练计划、课程报名、教练预约和打卡记录。</p>
        </div>

        <el-form :model="form" label-position="top" @keyup.enter="submit">
          <el-form-item label="账号">
            <el-input v-model="form.account" placeholder="请输入账号" />
          </el-form-item>
          <el-form-item label="密码">
            <el-input v-model="form.password" type="password" show-password placeholder="请输入密码" />
          </el-form-item>
          <el-button type="primary" class="login-btn" :loading="loading" @click="submit">登录并开始</el-button>
        </el-form>

        <div class="login-actions">
          <el-button @click="fillAccount('root', 'root')">填充测试账号</el-button>
          <p class="muted">常用测试账号：`root / root`。如果失败请确认 app 后端已启动。</p>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup>
import { reactive, ref } from "vue"
import { useRouter } from "vue-router"
import { ElMessage } from "element-plus"
import { useAppAuthStore } from "../stores/auth"

const router = useRouter()
const store = useAppAuthStore()
const loading = ref(false)

const form = reactive({
  account: "",
  password: ""
})

const fillAccount = (account, password) => {
  form.account = account
  form.password = password
}

const submit = async () => {
  if (!form.account || !form.password) {
    ElMessage.warning("请输入账号和密码")
    return
  }
  try {
    loading.value = true
    await store.login(form.account, form.password)
    ElMessage.success("登录成功")
    router.push("/home")
  } catch (err) {
    const message = err?.message || "登录失败"
    if (message.includes("无法连接后端")) {
      ElMessage.error("后端未启动，请先启动 app 服务（8081）")
    } else if (message.includes("请求超时")) {
      ElMessage.error("请求超时，请检查后端状态")
    } else if (message.includes("账号") || message.includes("密码")) {
      ElMessage.error("账号或密码错误")
    } else {
      ElMessage.error(message)
    }
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-shell {
  min-height: 100vh;
  padding: 18px;
  display: grid;
  place-items: center;
}

.login-stage {
  width: min(1220px, 100%);
  display: grid;
  grid-template-columns: minmax(0, 1.2fr) minmax(320px, 0.8fr);
  gap: 16px;
}

.stage-copy {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.login-card {
  border-radius: 24px;
  border: 1px solid rgba(255, 255, 255, 0.45);
  background: rgba(247, 255, 242, 0.92);
  color: var(--eco-text);
  padding: 18px;
  box-shadow: 0 20px 36px rgba(32, 64, 46, 0.2);
}

.login-top {
  margin-bottom: 10px;
}

.login-kicker {
  border-color: var(--eco-border);
  background: #f2f9ed;
  color: var(--eco-text);
}

.login-title {
  margin: 10px 0 0;
  font-size: 34px;
  line-height: 1.08;
  color: var(--eco-text);
}

.login-sub {
  margin: 8px 0 0;
  color: var(--eco-text-soft);
  line-height: 1.7;
  font-size: 13px;
}

.login-btn {
  width: 100%;
  margin-top: 8px;
}

.login-actions {
  margin-top: 12px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

@media (max-width: 960px) {
  .login-shell {
    padding: 10px;
  }

  .login-stage {
    grid-template-columns: 1fr;
  }
}
</style>
