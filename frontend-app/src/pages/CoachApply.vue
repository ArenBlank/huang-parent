<template>
  <div class="card">
    <div class="toolbar">
      <div>
        <h2 class="section-title">教练申请</h2>
        <p class="section-sub">提交资料后等待审核</p>
      </div>
      <el-button type="primary" @click="loadApply" :loading="loading">刷新</el-button>
    </div>

    <el-empty v-if="!applyInfo" description="暂无申请记录">
      <div class="empty-actions">
        <el-button size="small" @click="fillSample">填充示例</el-button>
      </div>
    </el-empty>
    <div v-else class="apply-summary">
      <div class="row">
        <span class="label">申请编号</span>
        <span class="value">{{ applyInfo.profileId }}</span>
      </div>
      <div class="row">
        <span class="label">审核状态</span>
        <el-tag :type="statusTagType">{{ statusText }}</el-tag>
      </div>
      <div class="row">
        <span class="label">更新日期</span>
        <span class="value">{{ applyInfo.updateTime || '-' }}</span>
      </div>
      <div class="row">
        <span class="label">擅长领域</span>
        <span class="value">{{ applyInfo.expertise }}</span>
      </div>
      <div class="row">
        <span class="label">从业年限</span>
        <span class="value">{{ applyInfo.years }}</span>
      </div>
      <div class="row">
        <span class="label">课时价格</span>
        <span class="value">{{ applyInfo.price }}</span>
      </div>
    </div>
  </div>

  <div class="card" style="margin-top: 16px;">
    <div class="toolbar">
      <div>
        <h2 class="section-title">提交/更新申请</h2>
        <p class="section-sub">支持修改资料后重新提交</p>
      </div>
      <div class="actions">
        <el-button size="small" @click="fillSample">填充示例</el-button>
        <el-button type="success" :loading="submitting" @click="submitApply">提交申请</el-button>
      </div>
    </div>
    <el-form label-position="top" :model="form">
      <el-form-item label="个人简介">
        <el-input v-model="form.bio" type="textarea" :rows="3" placeholder="请简要介绍从业经验与风格" />
      </el-form-item>
      <el-form-item label="擅长领域（逗号分隔）">
        <el-input v-model="form.expertise" placeholder="减脂,增肌,力量训练" />
      </el-form-item>
      <el-form-item label="从业年限">
        <el-input-number v-model="form.years" :min="0" :max="60" />
      </el-form-item>
      <el-form-item label="课时价格（元）">
        <el-input-number v-model="form.price" :min="0" :step="10" />
      </el-form-item>
    </el-form>
  </div>
</template>

<script setup>
import { computed, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { appClient } from '../api/client'

const loading = ref(false)
const submitting = ref(false)
const applyInfo = ref(null)

const form = reactive({
  bio: '',
  expertise: '',
  years: 2,
  price: 199
})

const statusText = computed(() => {
  const status = applyInfo.value?.certStatus
  if (status === 1) return '已通过'
  if (status === 2) return '已驳回'
  if (status === 0) return '待审核'
  return applyInfo.value?.certStatusText || '未知'
})

const statusTagType = computed(() => {
  const status = applyInfo.value?.certStatus
  if (status === 1) return 'success'
  if (status === 2) return 'danger'
  if (status === 0) return 'warning'
  return 'info'
})

const loadApply = async () => {
  try {
    loading.value = true
    const { data } = await appClient.get('/app/coach/my-application')
    if (data.code !== 200) throw new Error(data.message || '加载失败')
    applyInfo.value = data.data || null
  } catch (err) {
    ElMessage.error(err.message || '加载失败')
  } finally {
    loading.value = false
  }
}

const submitApply = async () => {
  if (!form.bio || !form.expertise) {
    ElMessage.warning('请填写个人简介和擅长领域')
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
    const { data } = await appClient.post('/app/coach/apply', payload)
    if (data.code !== 200) throw new Error(data.message || '提交失败')
    ElMessage.success('申请已提交')
    await loadApply()
  } catch (err) {
    ElMessage.error(err.message || '提交失败')
  } finally {
    submitting.value = false
  }
}

const fillSample = () => {
  form.bio = '国家职业健身教练，擅长减脂增肌与动作矫正'
  form.expertise = '减脂,增肌,力量训练'
  form.years = 3
  form.price = 199
}

loadApply()
</script>

<style scoped>
.apply-summary {
  display: grid;
  gap: 8px;
  margin-top: 12px;
}

.row {
  display: flex;
  align-items: center;
  gap: 12px;
}

.label {
  font-size: 12px;
  color: var(--muted);
  min-width: 70px;
}

.value {
  font-weight: 600;
}

.actions {
  display: flex;
  gap: 8px;
}
</style>
