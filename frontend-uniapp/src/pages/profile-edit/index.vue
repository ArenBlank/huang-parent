<template>
  <SlPage>
    <view class="edit-topbar">
      <button class="back-button" aria-label="返回" @click="goBack">
        <image :src="icons.back" mode="aspectFit" />
      </button>
      <view class="title-block">
        <text class="eyebrow">PROFILE EDIT</text>
        <text class="page-title">编辑资料</text>
        <text class="page-sub">这些资料会用于首页展示、课程报名和个性化训练服务。</text>
      </view>
    </view>

    <view class="avatar-card sl-card">
      <button class="avatar-button" :disabled="uploading" @click="chooseAvatar">
        <image v-if="avatarSrc" class="avatar-image" :src="avatarSrc" mode="aspectFill" />
        <text v-else class="avatar-fallback">{{ avatarFallback }}</text>
        <view class="camera-badge">
          <image :src="icons.camera" mode="aspectFit" />
        </view>
      </button>
      <view class="avatar-copy">
        <text class="avatar-title">头像</text>
        <text class="avatar-desc">点击左侧头像上传，支持 jpg、png、webp，最大 5MB。</text>
        <text class="avatar-state">{{ uploading ? '正在上传头像...' : completionText }}</text>
      </view>
    </view>

    <view class="profile-form sl-card">
      <view class="form-head">
        <view>
          <text class="form-title">基础信息</text>
          <text class="form-sub">每一项都标明了用途，保存后会同步到 /app/profile/info。</text>
        </view>
        <view class="verified-pill">
          <image :src="icons.check" mode="aspectFit" />
          <text>{{ profile?.profileCompleted ? '已完善' : '待完善' }}</text>
        </view>
      </view>

      <view class="field-list">
        <view v-for="field in inputFields" :key="field.key" class="field-item">
          <view class="field-label-row">
            <view class="field-icon">
              <image :src="field.icon" mode="aspectFit" />
            </view>
            <view class="field-copy">
              <text class="field-label">{{ field.label }}</text>
              <text class="field-hint">{{ field.hint }}</text>
            </view>
          </view>
          <input
            v-model="form[field.key]"
            class="field-input"
            :type="field.type || 'text'"
            :placeholder="field.placeholder"
            :maxlength="field.maxlength || -1"
          />
        </view>

        <view class="field-item">
          <view class="field-label-row">
            <view class="field-icon">
              <image :src="icons.gender" mode="aspectFit" />
            </view>
            <view class="field-copy">
              <text class="field-label">性别</text>
              <text class="field-hint">用于资料展示，可选择不填写。</text>
            </view>
          </view>
          <picker :range="genderOptions" range-key="label" :value="genderIndex" @change="onGenderChange">
            <view class="field-input picker-value">{{ genderLabel }}</view>
          </picker>
        </view>

        <view class="field-item">
          <view class="field-label-row">
            <view class="field-icon">
              <image :src="icons.birthday" mode="aspectFit" />
            </view>
            <view class="field-copy">
              <text class="field-label">出生日期</text>
              <text class="field-hint">后端会据此计算年龄，首页不再显示死数据。</text>
            </view>
          </view>
          <picker mode="date" :value="form.birthDate" @change="onBirthDateChange">
            <view class="field-input picker-value">{{ form.birthDate || '请选择出生日期' }}</view>
          </picker>
        </view>

        <view class="field-item field-item--textarea">
          <view class="field-label-row">
            <view class="field-icon">
              <image :src="icons.bio" mode="aspectFit" />
            </view>
            <view class="field-copy">
              <text class="field-label">个人简介</text>
              <text class="field-hint">展示你的训练目标、偏好或补充说明。</text>
            </view>
          </view>
          <textarea
            v-model="form.bio"
            class="field-textarea"
            maxlength="300"
            placeholder="例如：希望提升体能、改善体态，偏好循序渐进训练。"
          ></textarea>
        </view>
      </view>

      <SlPrimaryButton class="submit-button" text="保存资料" :loading="saving" @click="save" />
    </view>
  </SlPage>
</template>

<script setup>
import {
  faBriefcase,
  faCakeCandles,
  faCamera,
  faCheckCircle,
  faChevronLeft,
  faEnvelope,
  faLocationDot,
  faPenNib,
  faPhone,
  faRulerVertical,
  faUser,
  faVenusMars,
  faWeightScale
} from '@fortawesome/free-solid-svg-icons'
import { computed, reactive, ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import SlPage from '../../components/SlPage.vue'
import SlPrimaryButton from '../../components/SlPrimaryButton.vue'
import { getProfile, updateProfile, uploadAvatar } from '../../api/modules/profile'
import { ensureLogin } from '../../utils/authGuard'
import { toPublicUrl } from '../../utils/mediaUrl'
import { goBack as backToPrevious } from '../../utils/navigation'
import { useAppAuthStore } from '../../stores/auth'

const faIcon = (definition, color = '#24104f') => {
  const [width, height, , , pathData] = definition.icon
  const paths = Array.isArray(pathData)
    ? pathData.map((path) => `<path fill="${color}" d="${path}"/>`).join('')
    : `<path fill="${color}" d="${pathData}"/>`
  const svg = `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 ${width} ${height}">${paths}</svg>`
  return `data:image/svg+xml;charset=utf-8,${encodeURIComponent(svg)}`
}

const icons = {
  back: faIcon(faChevronLeft),
  bio: faIcon(faPenNib, '#8b63ff'),
  birthday: faIcon(faCakeCandles, '#8b63ff'),
  camera: faIcon(faCamera, '#ffffff'),
  check: faIcon(faCheckCircle, '#8b63ff'),
  email: faIcon(faEnvelope, '#8b63ff'),
  gender: faIcon(faVenusMars, '#8b63ff'),
  height: faIcon(faRulerVertical, '#8b63ff'),
  location: faIcon(faLocationDot, '#8b63ff'),
  occupation: faIcon(faBriefcase, '#8b63ff'),
  phone: faIcon(faPhone, '#8b63ff'),
  user: faIcon(faUser, '#8b63ff'),
  weight: faIcon(faWeightScale, '#8b63ff')
}

const store = useAppAuthStore()
const saving = ref(false)
const uploading = ref(false)
const profile = ref(null)
const appBase = (import.meta.env.VITE_APP_BASE_URL || '').replace(/\/app\/?$/, '').replace(/\/$/, '')

const form = reactive({
  nickname: '',
  email: '',
  phone: '',
  occupation: '',
  height: '',
  weight: '',
  address: '',
  gender: '',
  birthDate: '',
  bio: ''
})

const inputFields = [
  { key: 'nickname', label: '昵称', hint: '首页和我的页面展示的名字。', placeholder: '请输入昵称', icon: icons.user, maxlength: 30 },
  { key: 'email', label: '邮箱', hint: '用于账户通知和资料展示。', placeholder: '请输入邮箱', icon: icons.email, maxlength: 80 },
  { key: 'phone', label: '手机号', hint: '用于登录、联系和安全校验。', placeholder: '请输入手机号', icon: icons.phone, maxlength: 20 },
  { key: 'occupation', label: '职业', hint: '例如学生、上班族、自由职业。', placeholder: '请输入职业', icon: icons.occupation, maxlength: 30 },
  { key: 'height', label: '身高（cm）', hint: '辅助训练计划和体态分析。', placeholder: '请输入身高', icon: icons.height, type: 'number' },
  { key: 'weight', label: '体重（kg）', hint: '辅助课程和训练强度建议。', placeholder: '请输入体重', icon: icons.weight, type: 'number' },
  { key: 'address', label: '地址', hint: '可填写城市或常用训练区域。', placeholder: '请输入地址', icon: icons.location, maxlength: 80 }
]

const genderOptions = [
  { label: '暂不填写', value: '' },
  { label: '男', value: 1 },
  { label: '女', value: 2 },
  { label: '保密', value: 0 }
]

const resolveAssetUrl = (value) => toPublicUrl(value)

const avatarSrc = computed(() => resolveAssetUrl(profile.value?.avatar || store.user?.avatar || store.user?.avatarUrl))
const avatarFallback = computed(() => {
  const name = form.nickname || profile.value?.nickname || profile.value?.username || store.displayName || 'U'
  return String(name).slice(0, 1).toUpperCase()
})
const completionText = computed(() => {
  const rate = Number(profile.value?.completionRate)
  if (Number.isFinite(rate)) return `资料完成度 ${Math.min(Math.max(Math.round(rate), 0), 100)}%`
  return profile.value?.profileCompleted ? '资料已完善' : '继续补充资料会让服务更准确'
})
const genderIndex = computed(() => {
  const index = genderOptions.findIndex((item) => String(item.value) === String(form.gender))
  return index >= 0 ? index : 0
})
const genderLabel = computed(() => genderOptions[genderIndex.value]?.label || '暂不填写')

const fillForm = (data = {}) => {
  Object.keys(form).forEach((key) => {
    form[key] = data[key] == null ? '' : String(data[key])
  })
  form.gender = data.gender === null || data.gender === undefined ? '' : Number(data.gender)
  form.birthDate = data.birthDate || ''
}

const loadProfile = async () => {
  const { data } = await getProfile()
  profile.value = data || {}
  fillForm(profile.value)
}

const chooseImage = () =>
  new Promise((resolve, reject) => {
    uni.chooseImage({
      count: 1,
      sizeType: ['compressed'],
      sourceType: ['album', 'camera'],
      success: resolve,
      fail: reject
    })
  })

const chooseAvatar = async () => {
  try {
    const result = await chooseImage()
    const file = result.tempFiles?.[0]
    const filePath = file?.path || result.tempFilePaths?.[0]
    if (!filePath) return
    if (file?.size && file.size > 5 * 1024 * 1024) {
      uni.showToast({ title: '头像不能超过 5MB', icon: 'none' })
      return
    }
    uploading.value = true
    const payload = await uploadAvatar(filePath)
    const uploadedAvatar = payload?.data?.avatarUrl || payload?.data?.avatar
    if (uploadedAvatar) {
      profile.value = { ...(profile.value || {}), avatar: uploadedAvatar }
    }
    const latest = await store.fetchProfile()
    if (latest) {
      profile.value = latest
      fillForm(latest)
    }
    uni.showToast({ title: '头像已更新', icon: 'success' })
  } catch (error) {
    if (!String(error?.errMsg || error?.message || '').includes('cancel')) {
      uni.showToast({ title: error?.message || '头像上传失败', icon: 'none' })
    }
  } finally {
    uploading.value = false
  }
}

const trimOrUndefined = (value) => {
  if (value === null || value === undefined) return undefined
  if (typeof value !== 'string') return value
  const trimmed = value.trim()
  return trimmed ? trimmed : undefined
}

const numberOrUndefined = (value) => {
  if (value === null || value === undefined || value === '') return undefined
  const parsed = Number(value)
  return Number.isFinite(parsed) && parsed > 0 ? parsed : undefined
}

const save = async () => {
  try {
    saving.value = true
    const payload = {
      nickname: trimOrUndefined(form.nickname),
      email: trimOrUndefined(form.email),
      phone: trimOrUndefined(form.phone),
      occupation: trimOrUndefined(form.occupation),
      height: numberOrUndefined(form.height),
      weight: numberOrUndefined(form.weight),
      address: trimOrUndefined(form.address),
      gender: form.gender === '' ? undefined : Number(form.gender),
      birthDate: trimOrUndefined(form.birthDate),
      bio: trimOrUndefined(form.bio)
    }
    Object.keys(payload).forEach((key) => {
      if (payload[key] === undefined) delete payload[key]
    })
    if (!Object.keys(payload).length) {
      uni.showToast({ title: '没有需要保存的修改', icon: 'none' })
      return
    }
    await updateProfile(payload)
    const latest = await store.fetchProfile()
    if (latest) {
      profile.value = latest
      fillForm(latest)
    }
    uni.showToast({ title: '资料已保存', icon: 'success' })
  } catch (error) {
    uni.showToast({ title: error?.message || '保存失败', icon: 'none' })
  } finally {
    saving.value = false
  }
}

const onGenderChange = ({ detail }) => {
  form.gender = genderOptions[Number(detail.value)]?.value ?? ''
}

const onBirthDateChange = ({ detail }) => {
  form.birthDate = detail.value || ''
}

const goBack = () => {
  backToPrevious()
}

onShow(() => {
  if (ensureLogin()) {
    loadProfile()
  }
})
</script>

<style scoped lang="scss">
.edit-topbar {
  display: grid;
  grid-template-columns: 72rpx minmax(0, 1fr);
  gap: 18rpx;
  align-items: center;
}

button {
  margin: 0;
  padding: 0;
  border: 0;
  background: transparent;
  line-height: 1;
}

button::after {
  border: 0;
}

.back-button {
  display: grid;
  width: 68rpx;
  height: 68rpx;
  place-items: center;
  border: 3rpx solid #34205f;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.92);
  box-shadow: 0 8rpx 0 rgba(52, 32, 95, 0.08);
}

.back-button image {
  width: 32rpx;
  height: 32rpx;
}

.title-block {
  min-width: 0;
}

.eyebrow {
  display: block;
  color: #8b63ff;
  font-size: 20rpx;
  font-weight: 950;
  letter-spacing: 3rpx;
}

.page-title {
  display: block;
  margin-top: 4rpx;
  color: #24104f;
  font-size: 42rpx;
  font-weight: 950;
  line-height: 1.1;
}

.page-sub {
  display: block;
  margin-top: 8rpx;
  color: #7b6f98;
  font-size: 24rpx;
  line-height: 1.45;
}

.avatar-card,
.profile-form {
  margin-top: 24rpx;
  padding: 28rpx;
}

.avatar-card {
  display: grid;
  grid-template-columns: 132rpx minmax(0, 1fr);
  gap: 22rpx;
  align-items: center;
  border-color: rgba(52, 32, 95, 0.24);
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.96), rgba(244, 236, 255, 0.94));
}

.avatar-button {
  position: relative;
  display: grid;
  width: 124rpx;
  height: 124rpx;
  overflow: visible;
  place-items: center;
  border: 4rpx solid #24104f;
  border-radius: 50%;
  background: linear-gradient(135deg, #fff0c8, #eee6ff);
}

.avatar-image {
  width: 100%;
  height: 100%;
  overflow: hidden;
  border-radius: 50%;
}

.avatar-fallback {
  color: #24104f;
  font-size: 48rpx;
  font-weight: 950;
}

.camera-badge {
  position: absolute;
  right: -4rpx;
  bottom: -4rpx;
  display: grid;
  width: 44rpx;
  height: 44rpx;
  place-items: center;
  border: 3rpx solid #ffffff;
  border-radius: 50%;
  background: #8b63ff;
  box-shadow: 0 8rpx 16rpx rgba(52, 32, 95, 0.18);
}

.camera-badge image {
  width: 22rpx;
  height: 22rpx;
}

.avatar-title {
  display: block;
  color: #24104f;
  font-size: 31rpx;
  font-weight: 950;
}

.avatar-desc,
.avatar-state {
  display: block;
  margin-top: 8rpx;
  color: #7b6f98;
  font-size: 23rpx;
  line-height: 1.45;
}

.avatar-state {
  color: #8b63ff;
  font-weight: 850;
}

.profile-form {
  display: flex;
  flex-direction: column;
  gap: 28rpx;
  border-color: rgba(52, 32, 95, 0.24);
}

.form-head {
  display: flex;
  justify-content: space-between;
  gap: 20rpx;
  align-items: flex-start;
}

.form-title {
  display: block;
  color: #24104f;
  font-size: 34rpx;
  font-weight: 950;
}

.form-sub {
  display: block;
  margin-top: 8rpx;
  color: #7b6f98;
  font-size: 23rpx;
  line-height: 1.45;
}

.verified-pill {
  display: inline-flex;
  align-items: center;
  flex: 0 0 auto;
  gap: 8rpx;
  min-height: 46rpx;
  padding: 0 16rpx;
  border-radius: 999rpx;
  background: #f0e7ff;
  color: #8b63ff;
  font-size: 22rpx;
  font-weight: 900;
}

.verified-pill image {
  width: 24rpx;
  height: 24rpx;
}

.field-list {
  display: flex;
  flex-direction: column;
  gap: 20rpx;
}

.field-item {
  padding: 20rpx;
  border: 2rpx solid rgba(52, 32, 95, 0.12);
  border-radius: 26rpx;
  background: #ffffff;
}

.field-label-row {
  display: flex;
  gap: 16rpx;
  align-items: center;
  margin-bottom: 16rpx;
}

.field-icon {
  display: grid;
  width: 52rpx;
  height: 52rpx;
  flex: 0 0 auto;
  place-items: center;
  border-radius: 16rpx;
  background: #eee6ff;
}

.field-icon image {
  width: 28rpx;
  height: 28rpx;
}

.field-copy {
  min-width: 0;
}

.field-label {
  display: block;
  color: #24104f;
  font-size: 27rpx;
  font-weight: 950;
}

.field-hint {
  display: block;
  margin-top: 4rpx;
  color: #8a7aa9;
  font-size: 21rpx;
  line-height: 1.35;
}

.field-input,
.picker-value {
  width: 100%;
  min-height: 76rpx;
  padding: 0 22rpx;
  border: 2rpx solid rgba(52, 32, 95, 0.16);
  border-radius: 22rpx;
  background: #fffdf9;
  color: #24104f;
  font-size: 27rpx;
  font-weight: 800;
}

.picker-value {
  display: flex;
  align-items: center;
}

.field-textarea {
  width: 100%;
  min-height: 190rpx;
  padding: 22rpx;
  border: 2rpx solid rgba(52, 32, 95, 0.16);
  border-radius: 22rpx;
  background: #fffdf9;
  color: #24104f;
  font-size: 27rpx;
  line-height: 1.5;
}

.submit-button {
  margin-top: 4rpx;
}
</style>
