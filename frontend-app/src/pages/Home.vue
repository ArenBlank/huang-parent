<template>
  <div class="card">
    <div class="toolbar">
      <div>
        <h2 class="section-title">首页</h2>
        <p class="section-sub">今日推荐与最新公告</p>
      </div>
      <el-button type="primary" @click="loadAll" :loading="loading">刷新</el-button>
    </div>

    <div class="banner-grid" v-if="banners.length">
      <div v-for="item in banners" :key="item.id" class="banner-card">
        <div class="banner-title">{{ item.title || '运营Banner' }}</div>
        <div class="banner-sub">{{ item.subtitle || item.description || '点击查看更多' }}</div>
      </div>
    </div>
    <el-empty v-else description="暂无 Banner，可先刷新">
      <div class="empty-actions">
        <el-button size="small" @click="loadAll">刷新</el-button>
      </div>
    </el-empty>
  </div>

  <div class="card" style="margin-top: 16px;">
    <div class="toolbar">
      <div>
        <h2 class="section-title">公告</h2>
        <p class="section-sub">最近更新</p>
      </div>
      <span class="tag">{{ notices.length }} 条</span>
    </div>
    <el-timeline v-if="notices.length">
      <el-timeline-item v-for="notice in notices" :key="notice.id" :timestamp="notice.publishTime || ''">
        <div class="notice-title">{{ notice.title || '公告' }}</div>
        <div class="notice-content">{{ notice.content || notice.summary || '' }}</div>
      </el-timeline-item>
    </el-timeline>
    <el-empty v-else description="暂无公告，可先刷新">
      <div class="empty-actions">
        <el-button size="small" @click="loadAll">刷新公告</el-button>
      </div>
    </el-empty>
  </div>

  <div class="card" style="margin-top: 16px;">
    <div class="toolbar">
      <div>
        <h2 class="section-title">快速开始</h2>
        <p class="section-sub">一键进入核心功能</p>
      </div>
    </div>
    <div class="quick-grid">
      <div class="quick-card">
        <div class="quick-title">订阅训练计划</div>
        <div class="quick-desc">选择计划，开启训练</div>
        <el-button type="primary" size="small" @click="go('/plans')">去订阅</el-button>
      </div>
      <div class="quick-card">
        <div class="quick-title">报名课程</div>
        <div class="quick-desc">查看课程排期</div>
        <el-button type="success" size="small" @click="go('/courses')">去报名</el-button>
      </div>
      <div class="quick-card">
        <div class="quick-title">预约教练</div>
        <div class="quick-desc">挑选合适档期</div>
        <el-button type="warning" size="small" @click="go('/booking')">去预约</el-button>
      </div>
      <div class="quick-card">
        <div class="quick-title">查看订单</div>
        <div class="quick-desc">追踪支付与退款</div>
        <el-button size="small" @click="go('/orders')">去查看</el-button>
      </div>
      <div class="quick-card">
        <div class="quick-title">教练申请</div>
        <div class="quick-desc">提交资料等待审核</div>
        <el-button size="small" @click="go('/coach-apply')">去申请</el-button>
      </div>
    </div>
  </div>

  <div class="card" style="margin-top: 16px;">
    <div class="toolbar">
      <div>
        <h2 class="section-title">演示路径</h2>
        <p class="section-sub">建议按此顺序体验完整闭环</p>
      </div>
    </div>
    <div class="demo-flow">
      <div class="demo-step">
        <div class="demo-title">1. 订阅训练计划</div>
        <div class="demo-desc">选择计划并订阅</div>
        <el-button size="small" type="primary" @click="go('/plans')">去订阅</el-button>
      </div>
      <div class="demo-step">
        <div class="demo-title">2. 训练打卡</div>
        <div class="demo-desc">记录训练数据</div>
        <el-button size="small" type="success" @click="go('/training')">去打卡</el-button>
      </div>
      <div class="demo-step">
        <div class="demo-title">3. 预约/报名</div>
        <div class="demo-desc">预约教练或报名课程</div>
        <div class="demo-actions">
          <el-button size="small" @click="go('/booking')">去预约</el-button>
          <el-button size="small" @click="go('/courses')">去报名</el-button>
        </div>
      </div>
      <div class="demo-step">
        <div class="demo-title">4. 订单查看</div>
        <div class="demo-desc">查看支付与退款状态</div>
        <el-button size="small" @click="go('/orders')">去查看</el-button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { appClient } from '../api/client'

const banners = ref([])
const notices = ref([])
const loading = ref(false)
const router = useRouter()

const loadAll = async () => {
  try {
    loading.value = true
    const [bannerRes, noticeRes] = await Promise.all([
      appClient.get('/app/banner/list'),
      appClient.get('/app/notice/list', { params: { limit: 10 } })
    ])
    if (bannerRes.data.code !== 200) throw new Error(bannerRes.data.message || '加载Banner失败')
    if (noticeRes.data.code !== 200) throw new Error(noticeRes.data.message || '加载公告失败')
    banners.value = bannerRes.data.data || []
    notices.value = noticeRes.data.data || []
  } catch (err) {
    ElMessage.error(err.message || '加载失败')
  } finally {
    loading.value = false
  }
}

loadAll()

const go = (path) => {
  router.push(path)
}
</script>

<style scoped>
.banner-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 12px;
}

.banner-card {
  padding: 16px;
  border-radius: 16px;
  background: linear-gradient(135deg, rgba(59, 130, 246, 0.12), rgba(34, 197, 94, 0.12));
  border: 1px dashed rgba(59, 130, 246, 0.3);
}

.banner-title {
  font-size: 16px;
  font-weight: 600;
}

.banner-sub {
  font-size: 12px;
  color: var(--muted);
  margin-top: 6px;
}

.notice-title {
  font-weight: 600;
  margin-bottom: 4px;
}

.notice-content {
  font-size: 12px;
  color: var(--muted);
}

.quick-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 12px;
}

.quick-card {
  padding: 14px;
  border-radius: 14px;
  border: 1px solid var(--border);
  background: #ffffffcc;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.quick-title {
  font-weight: 600;
}

.quick-desc {
  font-size: 12px;
  color: var(--muted);
  margin-bottom: 4px;
}

.demo-flow {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 12px;
}

.demo-step {
  padding: 14px;
  border-radius: 14px;
  border: 1px dashed var(--border);
  background: #ffffffcc;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.demo-title {
  font-weight: 600;
}

.demo-desc {
  font-size: 12px;
  color: var(--muted);
  margin-bottom: 4px;
}

.demo-actions {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}
</style>
