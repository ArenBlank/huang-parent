<template>
  <div class="page-stack">
    <section class="hero-panel">
      <div class="apply-hero">
        <div>
          <p class="quest-kicker">Coach Application</p>
          <h1 class="hero-title">成为平台认证教练</h1>
          <p class="hero-subtitle">提交教练档案后可进入审核流程，页面会展示当前申请状态与最近更新时间。</p>
          <div class="hero-badges">
            <span class="badge-pill is-dark">当前状态 {{ statusText }}</span>
            <span class="badge-pill is-dark">申请编号 {{ applyInfo?.profileId || "-" }}</span>
          </div>
        </div>
        <el-button type="primary" @click="loadApply" :loading="loading">刷新状态</el-button>
      </div>
    </section>

    <section class="metric-grid">
      <article class="metric-card">
        <p class="metric-label">申请状态</p>
        <p class="metric-value">{{ statusText }}</p>
        <p class="metric-note">实时读取我的申请接口</p>
      </article>
      <article class="metric-card">
        <p class="metric-label">课时价格</p>
        <p class="metric-value">{{ applyInfo?.price || form.price || 0 }}</p>
        <p class="metric-note">单位：元 / 课时</p>
      </article>
      <article class="metric-card">
        <p class="metric-label">从业年限</p>
        <p class="metric-value">{{ applyInfo?.years || form.years || 0 }}</p>
        <p class="metric-note">认证审核关键字段之一</p>
      </article>
    </section>

    <section class="card">
      <div class="toolbar">
        <div>
          <h2 class="section-title">申请资料</h2>
          <p class="section-sub">先填写专业方向，再补齐经验与价格。</p>
        </div>
        <div class="toolbar-actions">
          <el-button @click="fillSample">填充示例</el-button>
          <el-button type="success" :loading="submitting" @click="submitApply">提交申请</el-button>
        </div>
      </div>

      <div class="apply-grid">
        <article class="form-shell">
          <el-form label-position="top" :model="form">
            <el-form-item label="个人简介">
              <el-input
                v-model="form.bio"
                type="textarea"
                :rows="4"
                placeholder="介绍你的训练背景、服务风格和擅长方向"
              />
            </el-form-item>
            <el-form-item label="擅长领域（逗号分隔）">
              <el-input v-model="form.expertise" placeholder="例如：减脂,增肌,力量训练" />
            </el-form-item>
            <div class="split-grid">
              <el-form-item label="从业年限">
                <el-input-number v-model="form.years" :min="0" :max="60" style="width: 100%" />
              </el-form-item>
              <el-form-item label="课时价格（元）">
                <el-input-number v-model="form.price" :min="0" :step="10" style="width: 100%" />
              </el-form-item>
            </div>
          </el-form>
        </article>

        <article class="quest-card quest-card--accent">
          <p class="quest-kicker">Preview</p>
          <h3 class="preview-title">教练档案预览</h3>
          <p class="quest-copy">审核端会优先查看你的领域标签、经验和定价信息。</p>
          <div class="badge-row">
            <span class="tag">{{ form.expertise || "待填写领域" }}</span>
            <span class="tag">{{ form.years || 0 }} 年经验</span>
            <span class="tag">￥{{ form.price || 0 }} / 课时</span>
          </div>
          <p class="muted preview-copy">{{ form.bio || "填写后这里会显示简介摘要。" }}</p>
        </article>
      </div>
    </section>

    <section class="card">
      <div class="toolbar">
        <div>
          <h2 class="section-title">申请记录</h2>
          <p class="section-sub">展示最近一次申请信息与更新时间。</p>
        </div>
      </div>

      <el-empty v-if="!applyInfo" description="暂无教练申请记录" />
      <div v-else class="record-card">
        <div class="badge-row">
          <span class="tag">申请编号 {{ applyInfo.profileId }}</span>
          <span class="tag">状态 {{ statusText }}</span>
          <span class="tag">更新时间 {{ applyInfo.updateTime || "-" }}</span>
        </div>
        <p class="muted">擅长领域：{{ applyInfo.expertise || "-" }}</p>
        <p class="muted">个人简介：{{ applyInfo.bio || "-" }}</p>
      </div>
    </section>
  </div>
</template>

<script setup>
import { computed, reactive, ref } from "vue"
import { ElMessage } from "element-plus"
import { appClient } from "../api/client"

const loading = ref(false)
const submitting = ref(false)
const applyInfo = ref(null)

const form = reactive({
  bio: "",
  expertise: "",
  years: 2,
  price: 199
})

const statusText = computed(() => {
  const status = applyInfo.value?.certStatus
  if (status === 1) return "已通过"
  if (status === 2) return "已驳回"
  if (status === 0) return "待审核"
  return applyInfo.value?.certStatusText || "未提交"
})

const loadApply = async () => {
  try {
    loading.value = true
    const { data } = await appClient.get("/app/coach/my-application")
    if (data.code !== 200) throw new Error(data.message || "加载申请信息失败")
    applyInfo.value = data.data || null
  } catch (err) {
    ElMessage.error(err.message || "加载申请信息失败")
  } finally {
    loading.value = false
  }
}

const submitApply = async () => {
  if (!form.bio || !form.expertise) {
    ElMessage.warning("请填写个人简介和擅长领域")
    return
  }
  try {
    submitting.value = true
    const payload = {
      bio: form.bio,
      expertise: form.expertise,
      years: form.years,
      price: form.price
    }
    const { data } = await appClient.post("/app/coach/apply", payload)
    if (data.code !== 200) throw new Error(data.message || "提交申请失败")
    ElMessage.success("申请已提交")
    await loadApply()
  } catch (err) {
    ElMessage.error(err.message || "提交申请失败")
  } finally {
    submitting.value = false
  }
}

const fillSample = () => {
  form.bio = "国家职业健身教练，擅长减脂增肌与动作矫正，注重训练计划和饮食建议协同。"
  form.expertise = "减脂,增肌,力量训练"
  form.years = 3
  form.price = 199
}

loadApply()
</script>

<style scoped>
.apply-hero {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: flex-end;
  flex-wrap: wrap;
}

.apply-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.15fr) minmax(280px, 0.85fr);
  gap: 12px;
}

.form-shell {
  border: 1px solid var(--eco-border);
  border-radius: 16px;
  background: #fffdf8;
  padding: 12px;
}

.preview-title {
  margin: 0;
  font-size: 24px;
  line-height: 1.1;
}

.preview-copy {
  margin-top: 8px;
  color: rgba(244, 255, 239, 0.88);
}

.record-card {
  border: 1px solid var(--eco-border);
  border-radius: 16px;
  background: #ffffff;
  padding: 12px;
}

@media (max-width: 980px) {
  .apply-grid {
    grid-template-columns: 1fr;
  }
}
</style>
