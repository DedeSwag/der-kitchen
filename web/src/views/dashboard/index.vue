<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getOverview } from '@/api/stats'
import { getOrders, transitOrderStatus } from '@/api/order'
import { useOrderRealtimeRefresh } from '@/composables/useOrderRealtimeRefresh'
import { ElMessage } from 'element-plus'
import type { StatsOverview, Order } from '@/types'

const router = useRouter()

const overview = ref<StatsOverview>({ todayOrders: 0, pendingOrders: 0, totalDishes: 0, weekOrders: 0 })
const pendingOrders = ref<Order[]>([])
const loading = ref(false)

async function fetchOverview() {
  overview.value = await getOverview()
}

async function fetchPendingOrders() {
  loading.value = true
  try {
    const res = await getOrders({ status: 'pending', pageNum: 1, pageSize: 10 })
    pendingOrders.value = res.records
  } finally {
    loading.value = false
  }
}

async function refreshDashboard() {
  await Promise.all([fetchOverview(), fetchPendingOrders()])
}

useOrderRealtimeRefresh(refreshDashboard)

async function handleTransit(orderId: number, status: string) {
  await transitOrderStatus(orderId, status)
  ElMessage.success('操作成功')
  fetchOverview()
  fetchPendingOrders()
}

function getMealTypeLabel(type: string) {
  const map: Record<string, string> = { breakfast: '早餐', lunch: '午餐', dinner: '晚餐' }
  return map[type] || type
}

onMounted(() => {
  void refreshDashboard()
})
</script>

<template>
  <div class="dashboard">
    <div class="stat-cards">
      <div class="stat-card pending">
        <div class="stat-value">{{ overview.pendingOrders }}</div>
        <div class="stat-label">待处理</div>
      </div>
      <div class="stat-card today">
        <div class="stat-value">{{ overview.todayOrders }}</div>
        <div class="stat-label">今日订单</div>
      </div>
      <div class="stat-card week">
        <div class="stat-value">{{ overview.weekOrders }}</div>
        <div class="stat-label">本周订单</div>
      </div>
      <div class="stat-card dishes">
        <div class="stat-value">{{ overview.totalDishes }}</div>
        <div class="stat-label">上架菜品</div>
      </div>
    </div>

    <div class="section">
      <div class="section-header">
        <h3>待处理订单</h3>
        <el-button text type="primary" @click="router.push('/orders')">查看全部</el-button>
      </div>
      <div v-loading="loading" class="order-cards">
        <el-empty v-if="pendingOrders.length === 0" description="暂无待处理订单 🎉" />
        <div v-for="order in pendingOrders" :key="order.id" class="order-card">
          <div class="order-card-header">
            <span class="order-id">#{{ order.id }}</span>
            <el-tag size="small">{{ getMealTypeLabel(order.mealType) }}</el-tag>
            <span class="order-date">{{ order.mealDate }}</span>
          </div>
          <div class="order-card-body">
            <p v-if="order.dietaryNotes" class="order-notes">📝 {{ order.dietaryNotes }}</p>
            <p v-if="order.specialRequests" class="order-notes">⭐ {{ order.specialRequests }}</p>
          </div>
          <div class="order-card-actions">
            <el-button type="primary" size="small" @click="handleTransit(order.id, 'preparing')">接单开做</el-button>
            <el-button size="small" @click="router.push(`/orders/${order.id}`)">查看详情</el-button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped lang="scss">
.stat-cards {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 24px;
}
.stat-card {
  background: #fff;
  border-radius: 8px;
  padding: 24px;
  text-align: center;
  box-shadow: 0 1px 3px rgba(0,0,0,0.06);
  .stat-value { font-size: 32px; font-weight: 700; margin-bottom: 4px; }
  .stat-label { color: var(--text-secondary); font-size: 14px; }
  &.pending .stat-value { color: #E6A23C; }
  &.today .stat-value { color: #FF6B35; }
  &.week .stat-value { color: #409EFF; }
  &.dishes .stat-value { color: #67C23A; }
}
.section {
  background: #fff;
  border-radius: 8px;
  padding: 20px;
  box-shadow: 0 1px 3px rgba(0,0,0,0.06);
}
.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  h3 { font-size: 16px; }
}
.order-cards {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 12px;
}
.order-card {
  border: 1px solid var(--border-color);
  border-radius: 8px;
  padding: 16px;
  &-header {
    display: flex; align-items: center; gap: 8px; margin-bottom: 8px;
    .order-id { font-weight: 600; }
    .order-date { margin-left: auto; color: var(--text-secondary); font-size: 12px; }
  }
  &-body { margin-bottom: 12px; min-height: 24px;
    .order-notes { font-size: 13px; color: var(--text-regular); margin-bottom: 4px; }
  }
  &-actions { display: flex; gap: 8px; }
}
</style>
