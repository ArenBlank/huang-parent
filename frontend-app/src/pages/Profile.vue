<template>
  <div class="page-stack">
    <section class="hero-panel">
      <div class="profile-hero">
        <div>
          <p class="quest-kicker">Profile Center</p>
          <h1 class="hero-title">个人档案与账户安全</h1>
          <p class="hero-subtitle">资料完善度、头像管理、信息更新与密码更新集中在同一页，减少来回切换。</p>
          <div class="hero-badges">
            <span class="badge-pill is-dark">用户 {{ profile?.username || "-" }}</span>
            <span class="badge-pill is-dark">完善度 {{ completionRate }}%</span>
            <span class="badge-pill is-dark">{{ profile?.profileCompleted ? "已完善" : "待完善" }}</span>
          </div>
        </div>
        <el-button type="primary" @click="loadProfile" :loading="loading">刷新资料</el-button>
      </div>
    </section>

    <section class="card">
      <div class="toolbar">
        <div>
          <h2 class="section-title">资料概览</h2>
          <p class="section-sub">展示当前个人信息与资料完成情况。</p>
        </div>
      </div>

      <div class="profile-grid">
        <article class="avatar-card">
          <el-avatar :size="84" :src="avatarSrc">{{ avatarFallback }}</el-avatar>
          <div>
            <p class="metric-label">当前头像</p>
            <p class="muted">支持 jpg/png，建议小于 5MB</p>
          </div>
          <el-upload :show-file-list="false" :http-request="uploadAvatar" :before-upload="beforeAvatarUpload">
            <el-button type="primary" :loading="avatarUploading">上传头像</el-button>
          </el-upload>
        </article>

        <article class="progress-card">
          <p class="metric-label">资料完成度</p>
          <p class="progress-value">{{ completionRate }}%</p>
          <el-progress :percentage="completionRate" :status="completionRate >= 100 ? 'success' : 'warning'" />
        </article>
      </div>

      <el-descriptions :column="2" border style="margin-top: 12px">
        <el-descriptions-item label="用户名">{{ profile?.username || "-" }}</el-descriptions-item>
        <el-descriptions-item label="昵称">{{ profile?.nickname || "-" }}</el-descriptions-item>
        <el-descriptions-item label="手机">{{ profile?.phone || "-" }}</el-descriptions-item>
        <el-descriptions-item label="邮箱">{{ profile?.email || "-" }}</el-descriptions-item>
        <el-descriptions-item label="地址">{{ profile?.address || "-" }}</el-descriptions-item>
        <el-descriptions-item label="职业">{{ profile?.occupation || "-" }}</el-descriptions-item>
        <el-descriptions-item label="身高">{{ profile?.height || "-" }}</el-descriptions-item>
        <el-descriptions-item label="体重">{{ profile?.weight || "-" }}</el-descriptions-item>
        <el-descriptions-item label="简介" :span="2">{{ profile?.bio || "-" }}</el-descriptions-item>
      </el-descriptions>
    </section>

    <section class="card">
      <div class="toolbar">
        <div>
          <h2 class="section-title">更新资料</h2>
          <p class="section-sub">可按需更新昵称、联系方式和身体参数。</p>
        </div>
        <el-button type="success" @click="updateProfile" :loading="saving">保存资料</el-button>
      </div>

      <el-form :model="form" label-position="top">
        <div class="split-grid">
          <el-form-item label="昵称"><el-input v-model="form.nickname" /></el-form-item>
          <el-form-item label="邮箱"><el-input v-model="form.email" /></el-form-item>
        </div>
        <div class="split-grid">
          <el-form-item label="手机号"><el-input v-model="form.phone" /></el-form-item>
          <el-form-item label="性别">
            <el-select v-model="form.gender" placeholder="请选择">
              <el-option label="未知" :value="0" />
              <el-option label="男" :value="1" />
              <el-option label="女" :value="2" />
            </el-select>
          </el-form-item>
        </div>
        <div class="split-grid">
          <el-form-item label="出生日期">
            <el-date-picker v-model="form.birthDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
          </el-form-item>
          <el-form-item label="职业"><el-input v-model="form.occupation" /></el-form-item>
        </div>
        <div class="split-grid">
          <el-form-item label="身高（cm）"><el-input v-model.number="form.height" /></el-form-item>
          <el-form-item label="体重（kg）"><el-input v-model.number="form.weight" /></el-form-item>
        </div>
        <el-form-item label="地址"><el-input v-model="form.address" /></el-form-item>
        <el-form-item label="简介"><el-input v-model="form.bio" type="textarea" :rows="3" /></el-form-item>
      </el-form>
    </section>

    <section class="card">
      <div class="toolbar">
        <div>
          <h2 class="section-title">更新密码</h2>
          <p class="section-sub">提交前请确认两次输入的新密码一致。</p>
        </div>
        <el-button type="warning" @click="updatePassword" :loading="updatingPwd">更新密码</el-button>
      </div>

      <el-form :model="pwdForm" label-position="top">
        <el-form-item label="旧密码">
          <el-input v-model="pwdForm.oldPassword" type="password" show-password />
        </el-form-item>
        <div class="split-grid">
          <el-form-item label="新密码">
            <el-input v-model="pwdForm.newPassword" type="password" show-password />
          </el-form-item>
          <el-form-item label="确认新密码">
            <el-input v-model="pwdForm.confirmPassword" type="password" show-password />
          </el-form-item>
        </div>
      </el-form>
    </section>
  </div>
</template>

<script setup>
import { computed, reactive, ref } from "vue"
import { ElMessage } from "element-plus"
import { appClient } from "../api/client"

const profile = ref(null)
const loading = ref(false)
const saving = ref(false)
const updatingPwd = ref(false)
const avatarUploading = ref(false)

const form = reactive({
  nickname: "",
  email: "",
  phone: "",
  gender: null,
  birthDate: "",
  address: "",
  occupation: "",
  height: null,
  weight: null,
  bio: ""
})

const pwdForm = reactive({
  oldPassword: "",
  newPassword: "",
  confirmPassword: ""
})

const appBase = (import.meta.env.VITE_APP_BASE_URL || "").replace(/\/app\/?$/, "")

const avatarSrc = computed(() => {
  const raw = profile.value?.avatar
  if (!raw) return ""
  if (/^https?:\/\//.test(raw)) return raw
  return appBase ? `${appBase}${raw}` : raw
})

const avatarFallback = computed(() => {
  const name = profile.value?.nickname || profile.value?.username || ""
  return name ? name.slice(0, 1) : "U"
})

const completionRate = computed(() => {
  const raw = profile.value?.completionRate
  if (Number.isFinite(raw)) return Math.min(Math.max(Number(raw), 0), 100)
  return profile.value?.profileCompleted ? 100 : 0
})

const beforeAvatarUpload = (file) => {
  const isImage = file.type.startsWith("image/")
  if (!isImage) {
    ElMessage.error("请上传图片文件")
    return false
  }
  const sizeOk = file.size / 1024 / 1024 <= 5
  if (!sizeOk) {
    ElMessage.error("图片不能超过 5MB")
    return false
  }
  return true
}

const uploadAvatar = async ({ file }) => {
  if (!file) return
  try {
    avatarUploading.value = true
    const formData = new FormData()
    formData.append("file", file)
    const { data } = await appClient.post("/app/profile/avatar/upload", formData, {
      headers: { "Content-Type": "multipart/form-data" }
    })
    if (data.code !== 200) throw new Error(data.message || "头像上传失败")
    ElMessage.success("头像上传成功")
    await loadProfile()
  } catch (err) {
    ElMessage.error(err.message || "头像上传失败")
  } finally {
    avatarUploading.value = false
  }
}

const loadProfile = async () => {
  try {
    loading.value = true
    const { data } = await appClient.get("/app/profile/info")
    if (data.code !== 200) throw new Error(data.message || "加载资料失败")
    profile.value = data.data
    form.nickname = data.data?.nickname || ""
    form.email = data.data?.email || ""
    form.phone = data.data?.phone || ""
    form.gender = data.data?.gender ?? null
    form.birthDate = data.data?.birthDate || ""
    form.address = data.data?.address || ""
    form.occupation = data.data?.occupation || ""
    form.height = data.data?.height ?? null
    form.weight = data.data?.weight ?? null
    form.bio = data.data?.bio || ""
  } catch (err) {
    ElMessage.error(err.message || "加载资料失败")
  } finally {
    loading.value = false
  }
}

const updateProfile = async () => {
  try {
    saving.value = true
    const trimOrEmpty = (value) => {
      if (value === null || value === undefined) return undefined
      if (typeof value !== "string") return value
      const trimmed = value.trim()
      return trimmed.length ? trimmed : undefined
    }
    const isValidNumber = (value, min) => Number.isFinite(value) && (min === undefined || value >= min)
    const payload = {
      nickname: trimOrEmpty(form.nickname),
      email: trimOrEmpty(form.email),
      phone: trimOrEmpty(form.phone),
      gender: form.gender === 0 || form.gender === 1 || form.gender === 2 ? form.gender : undefined,
      birthDate: trimOrEmpty(form.birthDate),
      address: trimOrEmpty(form.address),
      occupation: trimOrEmpty(form.occupation),
      height: isValidNumber(form.height, 1) ? form.height : undefined,
      weight: isValidNumber(form.weight, 1) ? form.weight : undefined,
      bio: trimOrEmpty(form.bio)
    }
    Object.keys(payload).forEach((key) => {
      if (payload[key] === undefined) delete payload[key]
    })
    if (!Object.keys(payload).length) {
      ElMessage.warning("没有需要保存的修改")
      return
    }
    const { data } = await appClient.put("/app/profile/info", payload)
    if (data.code !== 200) throw new Error(data.message || "保存失败")
    ElMessage.success("资料已保存")
    await loadProfile()
  } catch (err) {
    ElMessage.error(err.message || "保存失败")
  } finally {
    saving.value = false
  }
}

const updatePassword = async () => {
  try {
    updatingPwd.value = true
    if (!pwdForm.oldPassword || !pwdForm.newPassword || !pwdForm.confirmPassword) {
      ElMessage.warning("请完整填写密码字段")
      return
    }
    if (pwdForm.newPassword !== pwdForm.confirmPassword) {
      ElMessage.error("两次输入的新密码不一致")
      return
    }
    const { data } = await appClient.put("/app/profile/password", pwdForm)
    if (data.code !== 200) throw new Error(data.message || "密码更新失败")
    ElMessage.success("密码已更新")
    pwdForm.oldPassword = ""
    pwdForm.newPassword = ""
    pwdForm.confirmPassword = ""
  } catch (err) {
    ElMessage.error(err.message || "密码更新失败")
  } finally {
    updatingPwd.value = false
  }
}

loadProfile()
</script>

<style scoped>
.profile-hero {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: flex-end;
  flex-wrap: wrap;
}

.profile-grid {
  display: grid;
  grid-template-columns: minmax(280px, 0.9fr) minmax(240px, 0.7fr);
  gap: 12px;
}

.avatar-card,
.progress-card {
  border: 1px solid var(--eco-border);
  border-radius: 16px;
  background: #ffffff;
  padding: 12px;
}

.avatar-card {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.progress-value {
  margin: 8px 0;
  font-size: 30px;
  line-height: 1;
  font-weight: 800;
}

:deep(.el-descriptions__label) {
  width: 90px;
}

@media (max-width: 900px) {
  .profile-grid {
    grid-template-columns: 1fr;
  }
}
</style>
