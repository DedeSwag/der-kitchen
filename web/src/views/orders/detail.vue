<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getOrderDetail, transitOrderStatus } from '@/api/order'
import { ElMessage } from 'element-plus'
import type { OrderDetail } from '@/types'

const route = useRoute()
const router = useRouter()
const orderId = Number(route.params.id)

const order = ref<OrderDetail | null>(null)
const loading = ref(false)

const mealMap: Record<string, string> = { breakfast: '早餐', lunch: '午餐', dinner: '晚餐' }
const statusMap: Record<string, string> = { pending: '待处理', preparing: '制作中', completed: '已完成', cancelled: '已取消' }

function getStatusType(s: string) {
  const map: Record<string, any> = { pending: 'warning', preparing: '', completed: 'success', cancelled: 'info' }
  return map[s] || ''
}

async function fetchDetail() {
  loading.value = true
  try { order.value = await getOrderDetail(orderId) }
  finally { loading.value = false }
}

async function handleTransit(status: string) {
  await transitOrderStatus(orderId, status)
  ElMessage.success('操作成功')
  fetchDetail()
}

onMounted(fetchDetail)
</script>

<template>
  <div class="order-detail-page" v-loading="loading">
    <div class="page-header">
      <el-button @click="router.back()">← 返回</el-button>
      <h3>订单详情 #{{ orderId }}</h3>
    </div>

    <template v-if="order">
      <el-descriptions :column="2" border style="margin-bottom:20px">
        <el-descriptions-item label="状态">
          <el-tag :type="getStatusType(order.status)">{{ statusMap[order.status] || order.status }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="下单人">{{ order.userNickname }}</el-descriptions-item>
        <el-descriptions-item label="餐次">{{ mealMap[order.mealType] || order.mealType }}</el-descriptions-item>
        <el-descriptions-item label="用餐日期">{{ order.mealDate }}</el-descriptions-item>
        <el-descriptions-item label="下单时间">{{ order.createTime }}</el-descriptions-item>
        <el-descriptions-item label="口味偏好">{{ order.tasteTags || '-' }}</el-descriptions-item>
        <el-descriptions-item label="忌口">{{ order.dietaryNotes || '-' }}</el-descriptions-item>
        <el-descriptions-item label="特殊要求">{{ order.specialRequests || '-' }}</el-descriptions-item>
      </el-descriptions>

      <!-- 菜品明细 -->
      <h4 style="margin-bottom:12px">菜品明细</h4>
      <el-table :data="order.items" stripe size="small">
        <el-table-column prop="dishName" label="菜品" />
        <el-table-column prop="quantity" label="数量" width="80" />
        <el-table-column label="类型" width="100">
          <template #default="{ row }">
            <el-tag v-if="row.isExtra" type="warning" size="small">加菜</el-tag>
            <span v-else>原单</span>
          </template>
        </el-table-column>
      </el-table>

      <!-- 操作按钮 -->
      <div style="margin-top:20px;display:flex;gap:12px">
        <el-button v-if="order.status === 'pending'" type="primary" @click="handleTransit('preparing')">接单开做</el-button>
        <el-button v-if="order.status === 'preparing'" type="success" @click="handleTransit('completed')">完成上菜</el-button>
      </div>
    </template>
  </div>
</template>

<style scoped lang="scss">
.order-detail-page { background:#fff; border-radius:8px; padding:24px; }
.page-header { display:flex; align-items:center; gap:12px; margin-bottom:20px; h3 { font-size:18px; } }
</style>
