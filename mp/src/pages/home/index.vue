<template>
  <view class="home-page">
    <!-- 网络异常提示 -->
    <NetworkBar @retry="refresh" />

    <!-- 顶部区域 -->
    <view class="header-section">
      <view class="header-top">
        <text class="title">🍳 陈哥厨房</text>
        <text class="date">{{ today }}</text>
      </view>
      <!-- 用餐时段 -->
      <view class="meal-tabs">
        <view
          v-for="m in mealOptions"
          :key="m.value"
          class="meal-tab"
          :class="{ active: appStore.selectedMealType === m.value }"
          @tap="appStore.selectedMealType = m.value"
        >
          <text>{{ m.label }}</text>
        </view>
      </view>
    </view>

    <!-- 加菜模式提示 -->
    <view v-if="cartStore.isExtraMode" class="extra-mode-banner">
      <text>🍽️ 加菜模式 · 为订单 #{{ cartStore.extraOrderId }} 加菜中</text>
      <view class="cancel-extra" @tap="cartStore.clear()"><text>取消</text></view>
    </view>

    <!-- 分类Tab -->
    <scroll-view scroll-x class="category-scroll" :show-scrollbar="false">
      <view class="category-tabs">
        <view
          v-for="cat in categoryList"
          :key="cat.id"
          class="cat-tab"
          :class="{ active: activeCategoryId === cat.id }"
          @tap="switchCategory(cat.id)"
        >
          <text>{{ cat.name }}</text>
        </view>
      </view>
    </scroll-view>

    <!-- 骨架屏 -->
    <Skeleton v-if="loading && allDishes.length === 0" />

    <!-- 菜品列表 -->
    <view v-else class="dish-grid">
      <DishCard
        v-for="dish in filteredDishes"
        :key="dish.id"
        :dish="dish"
        :favorited="favSet.has(dish.id)"
        @fav-change="handleFavChange"
      />
    </view>

    <!-- 空状态 -->
    <Empty
      v-if="!loading && allDishes.length > 0 && filteredDishes.length === 0"
      icon="🍜"
      text="该分类暂无菜品"
    />
    <Empty
      v-if="!loading && allDishes.length === 0"
      icon="🍳"
      text="暂无菜品～"
    />

    <!-- 购物车浮层 -->
    <CartBar />
  </view>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { useAppStore } from '@/stores/app'
import { useCartStore } from '@/stores/cart'
import { getCategories } from '@/api/category'
import { getDishes } from '@/api/dish'
import { getFavorites } from '@/api/favorite'
import type { Category, Dish } from '@/types'
import DishCard from '@/components/DishCard.vue'
import CartBar from '@/components/CartBar.vue'
import Skeleton from '@/components/Skeleton.vue'
import Empty from '@/components/Empty.vue'
import NetworkBar from '@/components/NetworkBar.vue'

const appStore = useAppStore()
const cartStore = useCartStore()

const today = new Date().toLocaleDateString('zh-CN', { month: 'long', day: 'numeric', weekday: 'long' })

const mealOptions = [
  { label: '今日午餐', value: 'today_lunch' },
  { label: '今日晚餐', value: 'today_dinner' },
  { label: '明日午餐', value: 'tomorrow_lunch' },
]

// 分类
const categoryList = ref<({ id: number; name: string })[]>([])
const activeCategoryId = ref<number>(0) // 0 = 全部

// 菜品
const allDishes = ref<Dish[]>([])
const loading = ref(false)

// 收藏集合
const favSet = ref<Set<number>>(new Set())

const filteredDishes = computed(() => {
  if (activeCategoryId.value === 0) return allDishes.value
  return allDishes.value.filter(d => d.categoryId === activeCategoryId.value)
})

function switchCategory(id: number) {
  activeCategoryId.value = id
}

async function loadCategories() {
  try {
    const list = await getCategories()
    categoryList.value = [{ id: 0, name: '全部' }, ...list]
  } catch { /* 忽略 */ }
}

async function loadDishes() {
  loading.value = true
  try {
    const res = await getDishes({ pageNum: 1, pageSize: 100 })
    allDishes.value = res.records
  } catch { /* 忽略 */ }
  loading.value = false
}

async function loadFavorites() {
  try {
    const list = await getFavorites()
    favSet.value = new Set(list.map(d => d.id))
  } catch { /* 忽略 */ }
}

function handleFavChange(dishId: number, val: boolean) {
  if (val) {
    favSet.value.add(dishId)
  } else {
    favSet.value.delete(dishId)
  }
  // 触发响应式
  favSet.value = new Set(favSet.value)
}

onMounted(() => {
  loadCategories()
  loadDishes()
  loadFavorites()
})

onShow(() => {
  // 每次展示时刷新菜品（可能有上下架变动）
  loadDishes()
})

function refresh() {
  loadCategories()
  loadDishes()
  loadFavorites()
}
</script>

<style lang="scss" scoped>
.home-page {
  min-height: 100vh;
  background: var(--bg-page);
  padding-bottom: 200rpx;
}

/* 顶部 */
.header-section {
  background: var(--bg-card);
  padding: var(--spacing-lg);
  padding-top: var(--spacing-md);
}

.header-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: var(--spacing-md);
}

.title {
  font-size: var(--font-xl);
  font-weight: bold;
}

.date {
  font-size: var(--font-sm);
  color: var(--text-secondary);
}

/* 用餐时段 */
.meal-tabs {
  display: flex;
  gap: var(--spacing-sm);
}

.meal-tab {
  padding: 12rpx 24rpx;
  border-radius: 32rpx;
  font-size: var(--font-sm);
  background: #f5f5f5;
  color: var(--text-regular);
  transition: all 0.2s;

  &.active {
    background: var(--color-primary);
    color: #fff;
  }
}

/* 加菜模式 */
.extra-mode-banner {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: var(--spacing-sm) var(--spacing-lg);
  background: #FFF7E6;
  border-bottom: 1rpx solid #FFE58F;
  font-size: var(--font-sm);
  color: #D48806;
}
.cancel-extra {
  padding: 8rpx 20rpx;
  background: #fff;
  border-radius: 20rpx;
  font-size: var(--font-xs);
  color: var(--text-secondary);
  border: 1rpx solid var(--border-color);
}

/* 分类滚动 */
.category-scroll {
  background: var(--bg-card);
  white-space: nowrap;
  border-bottom: 1rpx solid var(--border-color);
}

.category-tabs {
  display: inline-flex;
  padding: var(--spacing-sm) var(--spacing-lg);
  gap: var(--spacing-lg);
}

.cat-tab {
  font-size: var(--font-md);
  color: var(--text-regular);
  padding-bottom: 12rpx;
  position: relative;
  flex-shrink: 0;

  &.active {
    color: var(--color-primary);
    font-weight: bold;

    &::after {
      content: '';
      position: absolute;
      bottom: 0;
      left: 50%;
      transform: translateX(-50%);
      width: 40rpx;
      height: 6rpx;
      background: var(--color-primary);
      border-radius: 3rpx;
    }
  }
}

/* 菜品网格 */
.dish-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: var(--spacing-md);
  padding: var(--spacing-lg);
}

/* 空状态 */
.empty-state {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 300rpx;
  color: var(--text-secondary);
  font-size: var(--font-md);
}
</style>

