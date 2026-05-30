<template>
  <view class="favorites-page">
    <view v-for="dish in favList" :key="dish.id" class="fav-item">
      <image class="fav-img" :src="dish.imageUrl || '/static/default-dish.png'" mode="aspectFill" @tap="goDetail(dish.id)" />
      <view class="fav-info">
        <text class="fav-name ellipsis">{{ dish.name }}</text>
        <text class="fav-meta">{{ dish.cookingTime ? `⏱️ ${dish.cookingTime}分钟` : '' }}</text>
        <view class="fav-bottom">
          <view class="qty-row">
            <view v-if="getQty(dish.id) > 0" class="qty-btn" @tap="cartStore.minusDish(dish.id)"><text>−</text></view>
            <text v-if="getQty(dish.id) > 0" class="qty-num">{{ getQty(dish.id) }}</text>
            <view class="qty-btn plus" @tap="cartStore.addDish(dish)"><text>＋</text></view>
          </view>
          <view class="unfav-btn" @tap="handleUnfav(dish.id)"><text>取消收藏</text></view>
        </view>
      </view>
    </view>

    <!-- 空状态 -->
    <view v-if="!loading && favList.length === 0" class="empty-state">
      <text class="empty-icon">🤍</text>
      <text class="empty-text">还没有收藏菜品，去逛逛~</text>
    </view>

    <!-- 购物车浮层 -->
    <CartBar />
  </view>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { getFavorites, removeFavorite } from '@/api/favorite'
import { useCartStore } from '@/stores/cart'
import type { Dish } from '@/types'
import CartBar from '@/components/CartBar.vue'

const cartStore = useCartStore()
const favList = ref<Dish[]>([])
const loading = ref(false)

function getQty(dishId: number) {
  return cartStore.getQuantity(dishId)
}

async function loadFavorites() {
  loading.value = true
  try {
    favList.value = await getFavorites()
  } catch { /* 忽略 */ }
  loading.value = false
}

async function handleUnfav(dishId: number) {
  try {
    await removeFavorite(dishId)
    favList.value = favList.value.filter(d => d.id !== dishId)
    uni.showToast({ title: '已取消收藏', icon: 'none', duration: 1000 })
  } catch { /* 忽略 */ }
}

function goDetail(id: number) {
  uni.navigateTo({ url: `/pages/dish/detail?id=${id}` })
}

onShow(() => { loadFavorites() })
</script>

<style lang="scss" scoped>
.favorites-page {
  min-height: 100vh;
  background: var(--bg-page);
  padding: var(--spacing-lg);
  padding-bottom: 200rpx;
}

.fav-item {
  display: flex;
  gap: var(--spacing-md);
  background: var(--bg-card);
  border-radius: var(--radius-lg);
  padding: var(--spacing-md);
  margin-bottom: var(--spacing-md);
}

.fav-img {
  width: 160rpx;
  height: 160rpx;
  border-radius: var(--radius-md);
  flex-shrink: 0;
}

.fav-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  min-width: 0;
}

.fav-name {
  font-size: var(--font-md);
  font-weight: 500;
}

.fav-meta {
  font-size: var(--font-xs);
  color: var(--text-secondary);
}

.fav-bottom {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.qty-row {
  display: flex;
  align-items: center;
  gap: 12rpx;
}
.qty-btn {
  width: 44rpx; height: 44rpx; border-radius: 50%;
  background: #f0f0f0; display: flex; align-items: center; justify-content: center;
  font-size: 28rpx;
  &.plus { background: var(--color-primary); color: #fff; }
}
.qty-num { font-size: var(--font-sm); font-weight: bold; min-width: 28rpx; text-align: center; }

.unfav-btn {
  font-size: var(--font-xs);
  color: var(--text-secondary);
  padding: 8rpx 16rpx;
  border: 1rpx solid var(--border-color);
  border-radius: 20rpx;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding-top: 200rpx;
  .empty-icon { font-size: 80rpx; margin-bottom: var(--spacing-md); }
  .empty-text { font-size: var(--font-md); color: var(--text-secondary); }
}
</style>

