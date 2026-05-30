<template>
  <view class="listing-page">
    <!-- 顶部快捷操作 -->
    <view class="quick-actions">
      <view class="quick-btn on" @tap="handleBatchListing(true)"><text>全部上架</text></view>
      <view class="quick-btn off" @tap="handleBatchListing(false)"><text>全部下架</text></view>
    </view>

    <!-- 按分类分组 -->
    <view v-for="group in groupedDishes" :key="group.categoryName" class="category-group">
      <view class="group-title">
        <text>{{ group.categoryName }}</text>
        <text class="group-count">{{ group.dishes.length }}道</text>
      </view>
      <view v-for="dish in group.dishes" :key="dish.id" class="dish-row">
        <image class="dish-img" :src="dish.imageUrl || '/static/default-dish.png'" mode="aspectFill" />
        <text class="dish-name ellipsis">{{ dish.name }}</text>
        <switch
          :checked="dish.isListed"
          color="#FF6B35"
          @change="handleToggle(dish)"
        />
      </view>
    </view>

    <!-- 空状态 -->
    <view v-if="!loading && groupedDishes.length === 0" class="empty-state">
      <text>暂无菜品</text>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { getAdminDishes, updateDishListing, batchDishListing } from '@/api/dish'
import { getCategories } from '@/api/category'
import type { Dish, Category } from '@/types'

const allDishes = ref<Dish[]>([])
const categories = ref<Category[]>([])
const loading = ref(false)

const groupedDishes = computed(() => {
  const catMap = new Map(categories.value.map(c => [c.id, c.name]))
  const groups: Record<number, { categoryName: string; dishes: Dish[] }> = {}

  allDishes.value.forEach(dish => {
    const catId = dish.categoryId
    if (!groups[catId]) {
      groups[catId] = { categoryName: catMap.get(catId) || '未分类', dishes: [] }
    }
    groups[catId].dishes.push(dish)
  })

  return Object.values(groups)
})

async function loadData() {
  loading.value = true
  try {
    const [catRes, dishRes] = await Promise.all([
      getCategories(),
      getAdminDishes({ pageNum: 1, pageSize: 200 }),
    ])
    categories.value = catRes
    allDishes.value = dishRes.records
  } catch { /* 忽略 */ }
  loading.value = false
}

async function handleToggle(dish: Dish) {
  const newVal = !dish.isListed
  try {
    await updateDishListing(dish.id, newVal)
    dish.isListed = newVal
    uni.showToast({ title: newVal ? '已上架' : '已下架', icon: 'none', duration: 800 })
  } catch {
    uni.showToast({ title: '操作失败', icon: 'none' })
  }
}

async function handleBatchListing(isListed: boolean) {
  uni.showModal({
    title: '确认操作',
    content: `确定将所有菜品${isListed ? '上架' : '下架'}吗？`,
    success: async (res) => {
      if (res.confirm) {
        try {
          await batchDishListing(isListed)
          allDishes.value.forEach(d => { d.isListed = isListed })
          uni.showToast({ title: `已全部${isListed ? '上架' : '下架'}`, icon: 'success' })
        } catch {
          uni.showToast({ title: '操作失败', icon: 'none' })
        }
      }
    }
  })
}

onShow(() => { loadData() })
</script>

<style lang="scss" scoped>
.listing-page {
  min-height: 100vh;
  background: var(--bg-page);
  padding-bottom: var(--spacing-xl);
}

.quick-actions {
  display: flex;
  gap: var(--spacing-md);
  padding: var(--spacing-lg);
}
.quick-btn {
  flex: 1;
  height: 72rpx;
  border-radius: 36rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: var(--font-md);
  font-weight: bold;
  &.on { background: #F6FFED; color: #52C41A; border: 1rpx solid #B7EB8F; }
  &.off { background: #FFF1F0; color: #FF4D4F; border: 1rpx solid #FFCCC7; }
  &:active { opacity: 0.8; }
}

.category-group {
  margin: 0 var(--spacing-lg) var(--spacing-md);
  background: var(--bg-card);
  border-radius: var(--radius-lg);
  overflow: hidden;
}

.group-title {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: var(--spacing-md) var(--spacing-lg);
  background: #fafafa;
  font-size: var(--font-md);
  font-weight: bold;
  border-bottom: 1rpx solid var(--border-color);
}
.group-count { font-size: var(--font-xs); color: var(--text-secondary); font-weight: normal; }

.dish-row {
  display: flex;
  align-items: center;
  padding: var(--spacing-sm) var(--spacing-lg);
  border-bottom: 1rpx solid var(--border-color);
  &:last-child { border-bottom: none; }
}

.dish-img {
  width: 64rpx;
  height: 64rpx;
  border-radius: var(--radius-sm);
  margin-right: var(--spacing-sm);
  flex-shrink: 0;
}

.dish-name {
  flex: 1;
  font-size: var(--font-md);
}

.empty-state {
  text-align: center;
  padding-top: 200rpx;
  color: var(--text-secondary);
}
</style>

