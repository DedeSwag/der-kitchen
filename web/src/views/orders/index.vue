<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getOrders, transitOrderStatus } from '@/api/order'
import { ElMessage } from 'element-plus'
import type { Order } from '@/types'

const router = useRouter()

const filters = ref({
  status: undefined as string | undefined,
  mealType: undefined as string | undefined,
  dateRange: [] as string[],
})
const pageNum = ref(1)
const pageSize = ref(20)
const total = ref(0)
const orders = ref<Order[]>([])
const loading = ref(false)

const statusOptions = [
  { label: '待处理', value: 'pending' },
  { label: '制作中', value: 'preparing' },
  { label: '已完成', value: 'completed' },
  { label: '已取消', value: 'cancelled' },
]
const mealOptions = [
  { label: '早餐', value: 'breakfast' },
  { label: '午餐', value: 'lunch' },
  { label: '晚餐', value: 'dinner' },
]

function getStatusTagType(s: string) {
  const map: Record<string, any> = { pending: 'warning', preparing: '', completed: 'success', cancelled: 'info' }
  return map[s] || ''
}
function getStatusLabel(s: string) {
  return statusOptions.find(o => o.value === s)?.label || s
}
function getMealLabel(s: string) {
  return mealOptions.find(o => o.value === s)?.label || s
}

async function fetchOrders() {
  loading.value = true
  try {
    const params: any = { pageNum: pageNum.value, pageSize: pageSize.value }
    if (filters.value.status) params.status = filters.value.status
    if (filters.value.mealType) params.mealType = filters.value.mealType
    if (filters.value.dateRange?.length === 2) {
      params.startDate = filters.value.dateRange[0]
      params.endDate = filters.value.dateRange[1]
    }
    const res = await getOrders(params)
    orders.value = res.records
    total.value = res.total
  } finally { loading.value = false }
}

async function handleTransit(id: number, status: string) {
  await transitOrderStatus(id, status)
  ElMessage.success('操作成功')
  fetchOrders()
}

function getNextActions(status: string) {
  const map: Record<string, { label: string; status: string; type?: string }[]> = {
    pending: [{ label: '接单', status: 'preparing', type: 'primary' }],
    preparing: [{ label: '完成', status: 'completed', type: 'success' }],
  }
  return map[status] || []
}

function handleSearch() { pageNum.value = 1; fetchOrders() }
function handleReset() { filters.value = { status: undefined, mealType: undefined, dateRange: [] }; handleSearch() }

onMounted(fetchOrders)
</script>

<template>
  <div class="orders-page">
    <div class="filter-bar">
      <el-select v-model="filters.status" placeholder="状态" clearable style="width:120px">
        <el-option v-for="s in statusOptions" :key="s.value" :label="s.label" :value="s.value" />
      </el-select>
      <el-select v-model="filters.mealType" placeholder="餐次" clearable style="width:100px">
        <el-option v-for="m in mealOptions" :key="m.value" :label="m.label" :value="m.value" />
      </el-select>
      <el-date-picker v-model="filters.dateRange" type="daterange" range-separator="~"
        start-placeholder="开始日期" end-placeholder="结束日期" value-format="YYYY-MM-DD" style="width:240px" />
      <el-button type="primary" @click="handleSearch">查询</el-button>
      <el-button @click="handleReset">重置</el-button>
    </div>

    <el-table :data="orders" v-loading="loading" stripe>
      <el-table-column label="订单号" width="80">
        <template #default="{ row }">#{{ row.id }}</template>
      </el-table-column>
      <el-table-column label="餐次" width="80">
        <template #default="{ row }">{{ getMealLabel(row.mealType) }}</template>
      </el-table-column>
      <el-table-column prop="mealDate" label="用餐日期" width="110" />
      <el-table-column label="备注" min-width="180">
        <template #default="{ row }">
          <span v-if="row.dietaryNotes || row.specialRequests" class="notes-text">
            {{ row.dietaryNotes }}{{ row.specialRequests ? ` | ${row.specialRequests}` : '' }}
          </span>
          <span v-else style="color:#ccc">无</span>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="getStatusTagType(row.status)" size="small">{{ getStatusLabel(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="下单时间" width="170" />
      <el-table-column label="操作" width="160" fixed="right">
        <template #default="{ row }">
          <el-button text type="primary" size="small" @click="router.push(`/orders/${row.id}`)">详情</el-button>
          <el-button v-for="action in getNextActions(row.status)" :key="action.status"
            :type="action.type as any" size="small" @click="handleTransit(row.id, action.status)">
            {{ action.label }}
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <div style="margin-top:16px;display:flex;justify-content:flex-end">
      <el-pagination v-model:current-page="pageNum" v-model:page-size="pageSize" :total="total"
        :page-sizes="[10,20,50]" layout="total,sizes,prev,pager,next" @current-change="fetchOrders" @size-change="fetchOrders" />
    </div>
  </div>
</template>

<style scoped lang="scss">
.orders-page { background:#fff; border-radius:8px; padding:20px; }
.filter-bar { display:flex; align-items:center; gap:12px; margin-bottom:16px; flex-wrap:wrap; }
.notes-text { font-size:13px; color:var(--text-regular); }
</style>
