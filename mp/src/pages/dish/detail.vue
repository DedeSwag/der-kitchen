<template>
  <view class="detail-page" v-if="dish">
    <!-- 大图 -->
    <view class="image-wrap" @tap="previewImage">
      <image class="dish-image" :src="dish.imageUrl || '/static/default-dish.png'" mode="aspectFill" />
      <view v-if="dish.status === 'out_of_stock'" class="stock-overlay">
        <text>暂时缺货</text>
      </view>
    </view>

    <!-- 信息区 -->
    <view class="info-section">
      <view class="title-row">
        <text class="dish-name">{{ dish.name }}</text>
        <view class="fav-btn" @tap="toggleFav">
          <text class="fav-icon">{{ isFav ? '❤️' : '🤍' }}</text>
        </view>
      </view>

      <view v-if="dish.description" class="dish-desc">
        <text>{{ dish.description }}</text>
      </view>

      <view v-if="dish.cookingTime" class="dish-time">
        <text>⏱️ 预计烹饪时长：约 {{ dish.cookingTime }} 分钟</text>
      </view>
    </view>

    <!-- 操作区 -->
    <view class="action-section">
      <view class="qty-row">
        <view v-if="quantity > 0" class="qty-btn minus" @tap="handleMinus"><text>−</text></view>
        <text v-if="quantity > 0" class="qty-num">{{ quantity }}</text>
        <view class="qty-btn plus" :class="{ disabled: isOutOfStock }" @tap="handlePlus"><text>＋</text></view>
      </view>

      <view
        class="add-btn"
        :class="{ disabled: isOutOfStock }"
        @tap="handleAddToCart"
      >
        <text>{{ isOutOfStock ? '暂时缺货' : (quantity > 0 ? '再加一份' : '加入购物车') }}</text>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { getDishDetail } from '@/api/dish'
import { addFavorite, removeFavorite } from '@/api/favorite'
import { useCartStore } from '@/stores/cart'
import type { Dish } from '@/types'

const cartStore = useCartStore()
const dish = ref<Dish | null>(null)
const isFav = ref(false)
let dishId = 0

const isOutOfStock = computed(() => dish.value?.status === 'out_of_stock')
const quantity = computed(() => dish.value ? cartStore.getQuantity(dish.value.id) : 0)

onLoad((query) => {
  dishId = Number(query?.id)
  if (dishId) loadDish()
})

async function loadDish() {
  try {
    dish.value = await getDishDetail(dishId)
  } catch {
    uni.showToast({ title: '菜品不存在', icon: 'none' })
    setTimeout(() => uni.navigateBack(), 1500)
  }
}

function previewImage() {
  if (dish.value?.imageUrl) {
    uni.previewImage({ urls: [dish.value.imageUrl], current: dish.value.imageUrl })
  }
}

function handlePlus() {
  if (isOutOfStock.value || !dish.value) return
  cartStore.addDish(dish.value)
}

function handleMinus() {
  if (!dish.value) return
  cartStore.minusDish(dish.value.id)
}

function handleAddToCart() {
  if (isOutOfStock.value || !dish.value) return
  cartStore.addDish(dish.value)
  uni.showToast({ title: '已加入', icon: 'success', duration: 1000 })
}

async function toggleFav() {
  if (!dish.value) return
  try {
    if (isFav.value) {
      await removeFavorite(dish.value.id)
      isFav.value = false
      uni.showToast({ title: '已取消收藏', icon: 'none', duration: 1000 })
    } else {
      await addFavorite(dish.value.id)
      isFav.value = true
      uni.showToast({ title: '已收藏 ❤️', icon: 'none', duration: 1000 })
    }
  } catch { /* 静默 */ }
}
</script>

<style lang="scss" scoped>
.detail-page {
  min-height: 100vh;
  background: var(--bg-page);
}

.image-wrap {
  position: relative;
  width: 100%;
  height: 400rpx;
}
.dish-image {
  width: 100%;
  height: 100%;
}
.stock-overlay {
  position: absolute;
  top: 0; left: 0; right: 0; bottom: 0;
  background: rgba(0,0,0,0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: var(--font-lg);
  font-weight: bold;
}

.info-section {
  background: var(--bg-card);
  padding: var(--spacing-lg);
  margin-bottom: var(--spacing-sm);
}

.title-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.dish-name {
  font-size: var(--font-xl);
  font-weight: bold;
}
.fav-btn { padding: 8rpx; }
.fav-icon { font-size: 44rpx; }

.dish-desc {
  margin-top: var(--spacing-md);
  font-size: var(--font-md);
  color: var(--text-regular);
  line-height: 1.6;
}

.dish-time {
  margin-top: var(--spacing-md);
  font-size: var(--font-sm);
  color: var(--text-secondary);
}

.action-section {
  background: var(--bg-card);
  padding: var(--spacing-lg);
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--spacing-lg);
}

.qty-row {
  display: flex;
  align-items: center;
  gap: 24rpx;
}
.qty-btn {
  width: 64rpx; height: 64rpx; border-radius: 50%;
  display: flex; align-items: center; justify-content: center;
  font-size: 36rpx;
  &.plus { background: var(--color-primary); color: #fff; }
  &.minus { background: #f0f0f0; color: var(--text-primary); }
  &.disabled { background: #ccc; }
}
.qty-num {
  font-size: var(--font-xl);
  font-weight: bold;
  min-width: 48rpx;
  text-align: center;
}

.add-btn {
  width: 100%;
  height: 88rpx;
  background: var(--color-primary);
  color: #fff;
  font-size: var(--font-lg);
  font-weight: bold;
  border-radius: 44rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  &.disabled { background: #ccc; }
  &:active:not(.disabled) { opacity: 0.85; }
}
</style>

