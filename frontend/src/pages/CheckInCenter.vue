<template>
  <div class="checkin-shell">
    <section class="checkin-hero">
      <div class="checkin-hero__copy">
        <p class="section-eyebrow">Front Desk Check-In</p>
        <h2>前台核销工作台</h2>
        <p>
          这里是给门店前台或课程助教使用的极简核销页。支持扫码枪直接回车提交，也支持手动输入 6 位核销码。
        </p>
      </div>
      <div class="checkin-hero__badge">
        <span>当前状态</span>
        <strong>{{ submitting ? '处理中' : '待核销' }}</strong>
      </div>
    </section>

    <section class="checkin-stage">
      <div class="checkin-panel">
        <p class="checkin-panel__label">课程核销</p>
        <h1>请输入或扫描 6 位核销码</h1>
        <p class="checkin-panel__desc">
          扫码枪通常会在输入完成后自动附带回车，你可以直接把光标停留在输入框里连续核销下一位学员。
        </p>

        <div class="checkin-form">
          <el-input
            ref="codeInputRef"
            v-model="checkInCode"
            class="checkin-input"
            maxlength="6"
            clearable
            placeholder="请使用扫码枪或手动输入 6 位核销码"
            @input="handleInput"
            @keyup.enter="submitCheckIn"
          />
          <el-button
            type="primary"
            size="large"
            class="checkin-submit"
            :loading="submitting"
            @click="submitCheckIn"
          >
            确认核销
          </el-button>
        </div>

        <div class="checkin-tips">
          <span class="tag">自动转大写</span>
          <span class="tag">最长 6 位</span>
          <span class="tag">支持回车提交</span>
        </div>

        <div v-if="lastSuccess" class="checkin-result success">
          <span class="result-label">最近一次核销成功</span>
          <strong>{{ lastSuccess.code }}</strong>
          <p>{{ lastSuccess.at }}</p>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup>
import { nextTick, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { adminClient, isHandledBusinessError } from '../api/client'

const codeInputRef = ref(null)
const checkInCode = ref('')
const submitting = ref(false)
const lastSuccess = ref(null)

const focusInput = async () => {
  await nextTick()
  codeInputRef.value?.focus?.()
}

const selectInput = async () => {
  await nextTick()
  const inputEl = codeInputRef.value?.input
  inputEl?.focus?.()
  inputEl?.select?.()
}

const handleInput = (value) => {
  checkInCode.value = String(value || '')
    .toUpperCase()
    .replace(/[^A-Z0-9]/g, '')
    .slice(0, 6)
}

const formatNow = () => {
  const now = new Date()
  return `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-${String(now.getDate()).padStart(2, '0')} ${String(
    now.getHours()
  ).padStart(2, '0')}:${String(now.getMinutes()).padStart(2, '0')}:${String(now.getSeconds()).padStart(2, '0')}`
}

const submitCheckIn = async () => {
  const code = checkInCode.value.trim().toUpperCase()
  if (code.length !== 6) {
    ElMessage.warning('请输入完整的 6 位核销码')
    await selectInput()
    return
  }

  try {
    submitting.value = true
    await adminClient.post('/admin/course/check-in', {
      checkInCode: code
    })
    ElMessage.success('核销成功！欢迎上课')
    lastSuccess.value = {
      code,
      at: formatNow()
    }
    checkInCode.value = ''
    await focusInput()
  } catch (err) {
    if (!isHandledBusinessError(err)) {
      ElMessage.error(err.message || '核销失败，请检查核销码后重试')
    }
    await selectInput()
  } finally {
    submitting.value = false
  }
}

onMounted(() => {
  focusInput()
})
</script>

<style scoped>
.checkin-shell {
  display: grid;
  gap: 18px;
}

.checkin-hero {
  display: flex;
  justify-content: space-between;
  gap: 18px;
  align-items: stretch;
  padding: 24px;
  border-radius: 30px;
  border: 3px solid var(--border);
  background: linear-gradient(135deg, #6d67ff 0%, #8ca5ff 42%, #ffd07a 100%);
  box-shadow: 0 16px 0 rgba(52, 45, 105, 0.1), 0 24px 36px rgba(91, 83, 255, 0.12);
}

.checkin-hero__copy {
  color: #fff;
}

.checkin-hero__copy h2 {
  margin: 0;
  font-size: clamp(30px, 4vw, 48px);
  line-height: 1.05;
}

.checkin-hero__copy p:last-child {
  margin: 14px 0 0;
  max-width: 720px;
  color: rgba(255, 255, 255, 0.92);
  font-size: 15px;
  line-height: 1.8;
}

.checkin-hero__badge {
  min-width: 190px;
  align-self: flex-start;
  padding: 18px;
  border-radius: 26px;
  border: 3px solid var(--border);
  background: rgba(255, 255, 255, 0.9);
  box-shadow: 0 10px 0 rgba(52, 45, 105, 0.08);
  display: grid;
  gap: 8px;
}

.checkin-hero__badge span {
  color: var(--text-soft);
  font-size: 11px;
  font-weight: 800;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.checkin-hero__badge strong {
  font-size: 24px;
  font-family: 'Fredoka', sans-serif;
}

.checkin-stage {
  display: grid;
  place-items: center;
  min-height: 55vh;
}

.checkin-panel {
  width: min(820px, 100%);
  padding: 34px;
  border-radius: 34px;
  border: 3px solid var(--border);
  background: linear-gradient(180deg, #ffffff 0%, #fff9ef 100%);
  box-shadow: var(--shadow-card);
  display: grid;
  gap: 20px;
}

.checkin-panel__label {
  margin: 0;
  color: var(--text-soft);
  font-size: 11px;
  font-weight: 800;
  letter-spacing: 0.12em;
  text-transform: uppercase;
}

.checkin-panel h1 {
  margin: 0;
  font-size: clamp(30px, 4vw, 52px);
  line-height: 1.05;
}

.checkin-panel__desc {
  margin: 0;
  color: var(--text-soft);
  font-size: 15px;
  line-height: 1.8;
}

.checkin-form {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 180px;
  gap: 14px;
  align-items: stretch;
}

.checkin-input :deep(.el-input__wrapper) {
  min-height: 84px;
  border-radius: 24px;
  box-shadow: none;
  border: 3px solid rgba(52, 45, 105, 0.16);
  padding: 0 24px;
}

.checkin-input :deep(.el-input__inner) {
  font-size: clamp(28px, 4vw, 42px);
  font-weight: 800;
  letter-spacing: 0.18em;
  text-transform: uppercase;
}

.checkin-submit {
  min-height: 84px;
  border-radius: 24px;
  font-size: 20px;
  font-weight: 800;
}

.checkin-tips {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.checkin-result {
  padding: 18px 20px;
  border-radius: 24px;
  border: 3px solid rgba(46, 163, 122, 0.16);
  background: linear-gradient(135deg, rgba(119, 255, 190, 0.12), rgba(236, 255, 248, 0.82));
  display: grid;
  gap: 8px;
}

.checkin-result strong {
  font-size: 32px;
  letter-spacing: 0.14em;
}

.checkin-result p,
.result-label {
  margin: 0;
  color: var(--text-soft);
}

@media (max-width: 960px) {
  .checkin-hero {
    flex-direction: column;
  }

  .checkin-hero__badge {
    min-width: 0;
  }
}

@media (max-width: 720px) {
  .checkin-panel {
    padding: 24px;
  }

  .checkin-form {
    grid-template-columns: 1fr;
  }
}
</style>
