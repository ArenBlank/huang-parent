<template>
  <div class="login-shell">
    <section class="login-stage hero-panel">
      <div class="stage-copy">
        <p class="quest-kicker">Sustainability Platform</p>
        <h1 class="hero-title">把训练旅程做成一片持续生长的森林</h1>
        <p class="hero-subtitle">
          训练计划、课程预约和打卡记录会沉淀成你的长期成长档案。登录前保持中性预览，登录后展示真实进度。
        </p>

        <div class="metric-grid">
          <article class="metric-card">
            <p class="metric-label">碳足迹预览</p>
            <p class="metric-value">0%</p>
            <p class="metric-note">新用户或样本不足时默认持平，不展示负向暗示。</p>
          </article>
          <article class="metric-card">
            <p class="metric-label">认证徽章</p>
            <p class="metric-value">03</p>
            <p class="metric-note">绿色训练流程、课程记录、订单数据可追踪。</p>
          </article>
          <article class="metric-card">
            <p class="metric-label">影响指标</p>
            <p class="metric-value">LIVE</p>
            <p class="metric-note">登录后查看训练进度、计划状态和关键业务数据。</p>
          </article>
        </div>
      </div>

      <div class="login-card">
        <div class="login-top">
          <p class="quest-kicker login-kicker">App Sign In</p>
          <h2 class="login-title">{{ isRegister ? "创建用户端账号" : "进入用户端" }}</h2>
          <p class="login-sub">
            账号密码通过后再完成滑块拼图验证，验证成功才会提交登录或注册请求。
          </p>
        </div>

        <div class="mode-switch" role="tablist" aria-label="登录注册切换">
          <button type="button" :class="{ active: !isRegister }" @click="switchMode(false)">登录</button>
          <button type="button" :class="{ active: isRegister }" @click="switchMode(true)">注册</button>
        </div>

        <el-form :model="form" label-position="top" @keyup.enter="submit">
          <el-form-item :label="isRegister ? '账号' : '账号'">
            <el-input v-model.trim="form.account" placeholder="请输入账号" />
          </el-form-item>

          <template v-if="isRegister">
            <el-form-item label="昵称">
              <el-input v-model.trim="form.nickname" placeholder="例如 Test Member" />
            </el-form-item>
            <el-form-item label="手机号">
              <el-input v-model.trim="form.phone" placeholder="请输入 11 位手机号" maxlength="11" />
            </el-form-item>
            <el-form-item label="邮箱">
              <el-input v-model.trim="form.email" placeholder="可选，用于接收通知" />
            </el-form-item>
          </template>

          <el-form-item label="密码">
            <el-input v-model="form.password" type="password" show-password placeholder="请输入密码" />
          </el-form-item>

          <el-form-item v-if="isRegister" label="确认密码">
            <el-input v-model="form.confirmPassword" type="password" show-password placeholder="请再次输入密码" />
          </el-form-item>

          <el-button type="primary" class="login-btn" :loading="loading" @click="submit">
            {{ isRegister ? "验证并注册" : "验证并登录" }}
          </el-button>
        </el-form>

        <div class="login-actions">
          <p class="muted">短信验证码入口已移除，登录与注册均需先完成滑块拼图验证。</p>
        </div>
      </div>
    </section>

    <AjSliderCaptcha
      :visible="captchaVisible"
      @success="handleCaptchaSuccess"
      @close="captchaVisible = false"
    />
  </div>
</template>

<script setup>
import { computed, reactive, ref } from "vue"
import { useRouter } from "vue-router"
import { ElMessage } from "element-plus"
import AjSliderCaptcha from "../components/AjSliderCaptcha.vue"
import { useAppAuthStore } from "../stores/auth"

const router = useRouter()
const store = useAppAuthStore()
const loading = ref(false)
const isRegister = ref(false)
const captchaVisible = ref(false)

const form = reactive({
  account: "",
  nickname: "",
  phone: "",
  email: "",
  password: "",
  confirmPassword: ""
})

const modeLabel = computed(() => (isRegister.value ? "注册" : "登录"))

const switchMode = (registerMode) => {
  isRegister.value = registerMode
  captchaVisible.value = false
}

const validateForm = () => {
  if (!form.account || !form.password) {
    ElMessage.warning("请输入账号和密码")
    return false
  }
  if (!isRegister.value) return true
  if (!form.nickname || !form.phone || !form.confirmPassword) {
    ElMessage.warning("请补全昵称、手机号和确认密码")
    return false
  }
  if (!/^1[3-9]\d{9}$/.test(form.phone)) {
    ElMessage.warning("请输入正确的手机号")
    return false
  }
  if (form.password !== form.confirmPassword) {
    ElMessage.warning("两次输入的密码不一致")
    return false
  }
  return true
}

const submit = () => {
  if (!validateForm()) return
  captchaVisible.value = true
}

const handleCaptchaSuccess = async (captchaVerification) => {
  captchaVisible.value = false
  loading.value = true
  try {
    if (isRegister.value) {
      await store.register({
        username: form.account,
        nickname: form.nickname,
        phone: form.phone,
        email: form.email || undefined,
        password: form.password,
        confirmPassword: form.confirmPassword,
        captchaVerification
      })
    } else {
      await store.login(form.account, form.password, captchaVerification)
    }
    ElMessage.success(`${modeLabel.value}成功`)
    await router.push("/home")
  } catch (err) {
    const message = err?.message || `${modeLabel.value}失败`
    if (message.includes("无法连接后端")) {
      ElMessage.error("后端未启动，请先启动 App 服务")
    } else if (message.includes("请求超时")) {
      ElMessage.error("请求超时，请检查后端状态")
    } else if (message.includes("account") || message.includes("password") || message.includes("密码")) {
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
  grid-template-columns: minmax(380px, 1.16fr) minmax(340px, 0.84fr);
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
  background: rgba(247, 255, 242, 0.94);
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

.mode-switch {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8px;
  padding: 5px;
  margin: 12px 0;
  border: 2px solid var(--eco-border);
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.7);
}

.mode-switch button {
  border: 0;
  border-radius: 999px;
  background: transparent;
  color: var(--eco-text-soft);
  font-weight: 900;
  padding: 10px 14px;
  cursor: pointer;
  transition: background 0.15s ease, color 0.15s ease;
}

.mode-switch button:hover {
  color: var(--eco-text);
  background: rgba(255, 255, 255, 0.7);
}

.mode-switch button:focus-visible {
  outline: 2px solid var(--eco-primary);
  outline-offset: -2px;
}

.mode-switch button:active {
  background: rgba(255, 255, 255, 0.5);
}

.mode-switch button.active {
  background: var(--eco-primary);
  color: #ffffff;
  box-shadow: 0 5px 0 rgba(52, 45, 105, 0.12);
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
