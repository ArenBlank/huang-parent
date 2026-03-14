<template>
  <div class="card">
    <div class="toolbar">
      <div>
        <h2>Orders</h2>
        <p>Orders, payments, and refund status</p>
      </div>
      <el-button type="primary" @click="loadOrders" :loading="loading">Refresh</el-button>
    </div>
    <el-table :data="orders" style="width: 100%" v-loading="loading" @row-click="loadDetail">
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="orderNo" label="Order No" />
      <el-table-column prop="totalAmount" label="Amount" width="120" />
      <el-table-column prop="payStatus" label="Pay Status" width="120" />
      <el-table-column prop="orderStatus" label="Order Status" width="140" />
    </el-table>
  </div>

  <div class="card" style="margin-top: 16px;">
    <h3>Order Detail</h3>
    <el-empty v-if="!detail" description="Select an order to view details" />
    <pre v-else class="payload">{{ detail }}</pre>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { adminClient } from '../api/client'

const orders = ref([])
const detail = ref(null)
const loading = ref(false)

const loadOrders = async () => {
  try {
    loading.value = true
    const { data } = await adminClient.get('/admin/ops/order/list')
    if (data.code !== 200) throw new Error(data.message || 'Load failed')
    orders.value = data.data || []
  } catch (err) {
    ElMessage.error(err.message || 'Load failed')
  } finally {
    loading.value = false
  }
}

const loadDetail = async (row) => {
  try {
    const { data } = await adminClient.get('/admin/ops/order/detail', {
      params: { orderId: row.id }
    })
    if (data.code !== 200) throw new Error(data.message || 'Detail failed')
    detail.value = data.data
  } catch (err) {
    ElMessage.error(err.message || 'Detail failed')
  }
}

onMounted(loadOrders)
</script>

<style scoped>
.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.toolbar h2 {
  margin: 0 0 6px;
}

.toolbar p {
  margin: 0;
  color: #8aa0af;
  font-size: 13px;
}

.payload {
  color: #bcd1dd;
  font-size: 12px;
  white-space: pre-wrap;
}
</style>
