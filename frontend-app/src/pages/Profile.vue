<template>
  <div class="card">
    <div class="toolbar">
      <div>
        <h2 class="section-title">我的资料</h2>
        <p class="section-sub">查看并更新个人信息</p>
      </div>
      <el-button type="primary" @click="loadProfile" :loading="loading">刷新</el-button>
    </div>

    <div class="progress-row">
      <div class="progress-label">
        资料完成度：{{ completionRate }}%
        <el-tag v-if="profile?.profileCompleted" size="small" type="success">已完善</el-tag>
        <el-tag v-else size="small" type="warning">待完善</el-tag>
      </div>
      <el-progress :percentage="completionRate" :status="completionRate >= 100 ? 'success' : 'warning'" />
    </div>

    <el-descriptions :column="2" border>
      <el-descriptions-item label="用户名">{{ profile?.username }}</el-descriptions-item>
      <el-descriptions-item label="昵称">{{ profile?.nickname }}</el-descriptions-item>
      <el-descriptions-item label="手机">{{ profile?.phone }}</el-descriptions-item>
      <el-descriptions-item label="邮箱">{{ profile?.email }}</el-descriptions-item>
      <el-descriptions-item label="地址">{{ profile?.address }}</el-descriptions-item>
      <el-descriptions-item label="职业">{{ profile?.occupation }}</el-descriptions-item>
      <el-descriptions-item label="身高">{{ profile?.height }}</el-descriptions-item>
      <el-descriptions-item label="体重">{{ profile?.weight }}</el-descriptions-item>
      <el-descriptions-item label="简介" :span="2">{{ profile?.bio }}</el-descriptions-item>
    </el-descriptions>
  </div>

  <div class="card" style="margin-top: 16px;">
    <div class="toolbar">
      <div>
        <h2 class="section-title">头像管理</h2>
        <p class="section-sub">上传头像后会同步到个人资料</p>
      </div>
      <el-upload :show-file-list="false" :http-request="uploadAvatar" :before-upload="beforeAvatarUpload">
        <el-button type="primary" :loading="avatarUploading">上传头像</el-button>
      </el-upload>
    </div>
    <div class="avatar-row">
      <el-avatar :size="72" :src="avatarSrc">{{ avatarFallback }}</el-avatar>
      <div>
        <div class="muted">当前头像预览</div>
        <div class="hint">支持 jpg/png，建议小于 5MB</div>
      </div>
    </div>
  </div>

  <div class="card" style="margin-top: 16px;">
    <div class="toolbar">
      <div>
        <h2 class="section-title">修改资料</h2>
        <p class="section-sub">示例字段，按需补充</p>
      </div>
      <el-button type="success" @click="updateProfile" :loading="saving">保存</el-button>
    </div>
    <el-form :model="form" label-position="top">
      <el-form-item label="昵称">
        <el-input v-model="form.nickname" />
      </el-form-item>
      <el-form-item label="邮箱">
        <el-input v-model="form.email" />
      </el-form-item>
      <el-form-item label="性别">
        <el-select v-model="form.gender" placeholder="请选择">
          <el-option label="未知" :value="0" />
          <el-option label="男" :value="1" />
          <el-option label="女" :value="2" />
        </el-select>
      </el-form-item>
      <el-form-item label="出生日期">
        <el-date-picker v-model="form.birthDate" type="date" placeholder="选择日期" value-format="YYYY-MM-DD" />
      </el-form-item>
      <el-form-item label="地址">
        <el-input v-model="form.address" />
      </el-form-item>
      <el-form-item label="职业">
        <el-input v-model="form.occupation" />
      </el-form-item>
      <el-form-item label="身高（cm）">
        <el-input v-model.number="form.height" />
      </el-form-item>
      <el-form-item label="体重（kg）">
        <el-input v-model.number="form.weight" />
      </el-form-item>
      <el-form-item label="简介">
        <el-input v-model="form.bio" type="textarea" :rows="3" />
      </el-form-item>
    </el-form>
  </div>

  <div class="card" style="margin-top: 16px;">
    <div class="toolbar">
      <div>
        <h2 class="section-title">修改密码</h2>
        <p class="section-sub">请牢记新密码</p>
      </div>
      <el-button type="warning" @click="updatePassword" :loading="updatingPwd">更新密码</el-button>
    </div>
    <el-form :model="pwdForm" label-position="top">
      <el-form-item label="旧密码">
        <el-input v-model="pwdForm.oldPassword" type="password" show-password />
      </el-form-item>
      <el-form-item label="新密码">
        <el-input v-model="pwdForm.newPassword" type="password" show-password />
      </el-form-item>
      <el-form-item label="确认新密码">
        <el-input v-model="pwdForm.confirmPassword" type="password" show-password />
      </el-form-item>
    </el-form>
  </div>
</template>

<script setup>
import { computed, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { appClient } from '../api/client'

const profile = ref(null)
const loading = ref(false)
const saving = ref(false)
const updatingPwd = ref(false)
const avatarUploading = ref(false)

const form = reactive({
  nickname: '',
  email: '',
  gender: null,
  birthDate: '',
  address: '',
  occupation: '',
  height: null,
  weight: null,
  bio: ''
})

const pwdForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

const appBase = (import.meta.env.VITE_APP_BASE_URL || '').replace(/\/app\/?$/, '')

const avatarSrc = computed(() => {
  const raw = profile.value?.avatar
  if (!raw) return ''
  if (/^https?:\/\//.test(raw)) return raw
  return appBase ? `${appBase}${raw}` : raw
})

const avatarFallback = computed(() => {
  const name = profile.value?.nickname || profile.value?.username || ''
  return name ? name.slice(0, 1) : 'U'
})

const completionRate = computed(() => {
  const raw = profile.value?.completionRate
  if (Number.isFinite(raw)) {
    return Math.min(Math.max(Number(raw), 0), 100)
  }
  return profile.value?.profileCompleted ? 100 : 0
})

const beforeAvatarUpload = (file) => {
  const isImage = file.type.startsWith('image/')
  if (!isImage) {
    ElMessage.error('请上传图片文件')
    return false
  }
  const sizeOk = file.size / 1024 / 1024 <= 5
  if (!sizeOk) {
    ElMessage.error('图片不能超过 5MB')
    return false
  }
  return true
}

const uploadAvatar = async ({ file }) => {
  if (!file) return
  try {
    avatarUploading.value = true
    const formData = new FormData()
    formData.append('file', file)
    const { data } = await appClient.post('/app/profile/avatar/upload', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
    if (data.code !== 200) throw new Error(data.message || '上传失败')
    ElMessage.success('头像上传成功')
    await loadProfile()
  } catch (err) {
    ElMessage.error(err.message || '上传失败')
  } finally {
    avatarUploading.value = false
  }
}

const loadProfile = async () => {
  try {
    loading.value = true
    const { data } = await appClient.get('/app/profile/info')
    if (data.code !== 200) throw new Error(data.message || '加载失败')
    profile.value = data.data
    form.nickname = data.data?.nickname || ''
    form.email = data.data?.email || ''
    form.gender = data.data?.gender ?? null
    form.birthDate = data.data?.birthDate || ''
    form.address = data.data?.address || ''
    form.occupation = data.data?.occupation || ''
    form.height = data.data?.height ?? null
    form.weight = data.data?.weight ?? null
    form.bio = data.data?.bio || ''
  } catch (err) {
    ElMessage.error(err.message || '加载失败')
  } finally {
    loading.value = false
  }
}

const updateProfile = async () => {
  try {
    saving.value = true
    const payload = { ...form }
    Object.keys(payload).forEach((key) => {
      if (payload[key] === '' || payload[key] === null || payload[key] === undefined) {
        delete payload[key]
      }
    })
    if (!Object.keys(payload).length) {
      ElMessage.warning('没有需要保存的修改')
      return
    }
    const { data } = await appClient.put('/app/profile/info', payload)
    if (data.code !== 200) throw new Error(data.message || '保存失败')
    ElMessage.success('保存成功')
    await loadProfile()
  } catch (err) {
    ElMessage.error(err.message || '保存失败')
  } finally {
    saving.value = false
  }
}

const updatePassword = async () => {
  try {
    updatingPwd.value = true
    const { data } = await appClient.put('/app/profile/password', pwdForm)
    if (data.code !== 200) throw new Error(data.message || '更新失败')
    ElMessage.success('密码已更新')
  } catch (err) {
    ElMessage.error(err.message || '更新失败')
  } finally {
    updatingPwd.value = false
  }
}

loadProfile()
</script>

<style scoped>
:deep(.el-descriptions__label) {
  width: 90px;
}

.avatar-row {
  display: flex;
  align-items: center;
  gap: 12px;
  padding-top: 8px;
}

.muted {
  font-size: 12px;
  color: var(--muted);
}

.hint {
  font-size: 12px;
  color: #6b7280;
  margin-top: 4px;
}

.progress-row {
  margin-bottom: 12px;
}

.progress-label {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  margin-bottom: 6px;
}
</style>
