<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getDishes, updateDishListing, updateDishStatus, deleteDish } from '@/api/dish'
import { getCategories } from '@/api/category'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { Dish, Category } from '@/types'

const router = useRouter()

const filters = ref({
  categoryId: undefined as number | undefined,
  status: undefined as string | undefined,
  isListed: undefined as boolean | undefined,
})
const pageNum = ref(1)
const pageSize = ref(20)
const total = ref(0)
const dishes = ref<Dish[]>([])
const categories = ref<Category[]>([])
const loading = ref(false)

const statusOptions = [
  { label: '正常', value: 'normal' },
  { label: '缺货', value: 'out_of_stock' },
  { label: '暂不做', value: 'unavailable' },
]

function getStatusTagType(status: string) {
  const map: Record<string, string> = { normal: 'success', out_of_stock: 'warning', unavailable: 'info' }
  return (map[status] || 'info') as any
}

async function fetchDishes() {
  loading.value = true
  try {
    const res = await getDishes({ ...filters.value, pageNum: pageNum.value, pageSize: pageSize.value })
    dishes.value = res.records
    total.value = res.total
  } finally {
    loading.value = false
  }
}

async function fetchCategories() {
  categories.value = await getCategories()
}

function getCategoryName(id: number) {
  return categories.value.find(c => c.id === id)?.name || '-'
}

async function handleListingChange(dish: Dish) {
  try {
    await updateDishListing(dish.id, dish.isListed)
    ElMessage.success(dish.isListed ? '已上架' : '已下架')
  } catch { dish.isListed = !dish.isListed }
}

async function handleStatusChange(dish: Dish, cmd: string) {
  await updateDishStatus(dish.id, cmd)
  dish.status = cmd
  ElMessage.success('状态已更新')
}

async function handleDelete(dish: Dish) {
  await ElMessageBox.confirm(`确定删除「${dish.name}」吗？`, '提示', { type: 'warning' })
  await deleteDish(dish.id)
  ElMessage.success('已删除')
  fetchDishes()
}

function handleSearch() { pageNum.value = 1; fetchDishes() }
function handleReset() { filters.value = { categoryId: undefined, status: undefined, isListed: undefined }; handleSearch() }

onMounted(() => { fetchCategories(); fetchDishes() })
</script>

<template>
  <div class="dishes-page">
    <div class="filter-bar">
      <el-select v-model="filters.categoryId" placeholder="分类" clearable style="width:140px">
        <el-option v-for="c in categories" :key="c.id" :label="c.name" :value="c.id" />
      </el-select>
      <el-select v-model="filters.status" placeholder="状态" clearable style="width:120px">
        <el-option v-for="s in statusOptions" :key="s.value" :label="s.label" :value="s.value" />
      </el-select>
      <el-select v-model="filters.isListed" placeholder="上架" clearable style="width:120px">
        <el-option label="已上架" :value="true" />
        <el-option label="已下架" :value="false" />
      </el-select>
      <el-button type="primary" @click="handleSearch">查询</el-button>
      <el-button @click="handleReset">重置</el-button>
      <el-button type="primary" @click="router.push('/dishes/create')" style="margin-left:auto">+ 新增菜品</el-button>
    </div>

    <el-table :data="dishes" v-loading="loading" stripe>
      <el-table-column label="图片" width="80">
        <template #default="{ row }">
          <el-image v-if="row.imageUrl" :src="row.imageUrl" style="width:50px;height:50px;border-radius:6px" fit="cover" />
          <span v-else style="color:#ccc">无图</span>
        </template>
      </el-table-column>
      <el-table-column prop="name" label="菜品名称" min-width="140" />
      <el-table-column label="分类" width="100">
        <template #default="{ row }">{{ getCategoryName(row.categoryId) }}</template>
      </el-table-column>
      <el-table-column label="时长" width="90">
        <template #default="{ row }">{{ row.cookingTime ? `${row.cookingTime}分钟` : '-' }}</template>
      </el-table-column>
      <el-table-column label="状态" width="110">
        <template #default="{ row }">
          <el-dropdown trigger="click" @command="(cmd: string) => handleStatusChange(row, cmd)">
            <el-tag :type="getStatusTagType(row.status)" style="cursor:pointer">
              {{ statusOptions.find(s => s.value === row.status)?.label || row.status }}
            </el-tag>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item v-for="s in statusOptions" :key="s.value" :command="s.value">{{ s.label }}</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </template>
      </el-table-column>
      <el-table-column label="上架" width="80">
        <template #default="{ row }">
          <el-switch v-model="row.isListed" @change="handleListingChange(row)" />
        </template>
      </el-table-column>
      <el-table-column label="操作" width="140" fixed="right">
        <template #default="{ row }">
          <el-button text type="primary" size="small" @click="router.push(`/dishes/${row.id}`)">编辑</el-button>
          <el-button text type="danger" size="small" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <div style="margin-top:16px;display:flex;justify-content:flex-end">
      <el-pagination v-model:current-page="pageNum" v-model:page-size="pageSize" :total="total"
        :page-sizes="[10,20,50]" layout="total,sizes,prev,pager,next" @current-change="fetchDishes" @size-change="fetchDishes" />
    </div>
  </div>
</template>

<style scoped lang="scss">
.dishes-page { background:#fff; border-radius:8px; padding:20px; }
.filter-bar { display:flex; align-items:center; gap:12px; margin-bottom:16px; flex-wrap:wrap; }
</style>
