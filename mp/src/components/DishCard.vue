<template>
  <view class="dish-card pressable" :class="{ 'out-of-stock': dish.status === 'out_of_stock' }" @tap="goDetail">
    <!-- 图片 -->
    <view class="card-image-wrap">
      <image class="card-image" :src="dish.imageUrl || '/static/default-dish.png'" mode="aspectFill" />
      <view v-if="dish.status === 'out_of_stock'" class="stock-tag">缺货</view>
    </view>
    <!-- 信息 -->
    <view class="card-body">
      <view class="card-title-row">
        <text class="card-name ellipsis">{{ dish.name }}</text>
        <view class="fav-btn" @tap.stop="toggleFavorite">
          <text :class="['fav-icon', { active: isFavorited, 'fav-pop': favAnimating }]">{{ isFavorited ? '❤️' : '🤍' }}</text>
        </view>
      </view>
      <view v-if="dish.cookingTime" class="card-time">
        <text>⏱️ {{ dish.cookingTime }}分钟</text>
      </view>
      <!-- 加减按钮 -->
      <view class="card-actions">
        <view v-if="quantity > 0" class="qty-btn minus" @tap.stop="handleMinus">
          <text>−</text>
        </view>
        <text v-if="quantity > 0" :class="['qty-num', { 'qty-bounce': qtyAnimating }]">{{ quantity }}</text>
        <view class="qty-btn plus" :class="{ disabled: dish.status === 'out_of_stock' }" @tap.stop="handlePlus">
          <text>＋</text>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { useCartStore } from '@/stores/cart'
import { addFavorite, removeFavorite } from '@/api/favorite'
import type { Dish } from '@/types'

const props = defineProps<{
  dish: Dish
  favorited?: boolean
}>()

const emit = defineEmits<{
  (e: 'favChange', dishId: number, val: boolean): void
}>()

const cartStore = useCartStore()
const qtyAnimating = ref(false)
const favAnimating = ref(false)

const quantity = computed(() => cartStore.getQuantity(props.dish.id))
const isFavorited = computed(() => props.favorited || false)

function goDetail() {
  uni.navigateTo({ url: `/pages/dish/detail?id=${props.dish.id}` })
}

function handlePlus() {
  if (props.dish.status === 'out_of_stock') return
  cartStore.addDish(props.dish)
  triggerQtyAnim()
}

function handleMinus() {
  cartStore.minusDish(props.dish.id)
  triggerQtyAnim()
}

function triggerQtyAnim() {
  qtyAnimating.value = true
  setTimeout(() => { qtyAnimating.value = false }, 300)
}

async function toggleFavorite() {
  try {
    if (isFavorited.value) {
      await removeFavorite(props.dish.id)
      emit('favChange', props.dish.id, false)
    } else {
      await addFavorite(props.dish.id)
      emit('favChange', props.dish.id, true)
      favAnimating.value = true
      setTimeout(() => { favAnimating.value = false }, 400)
      uni.showToast({ title: '已收藏 ❤️', icon: 'none', duration: 1000 })
    }
  } catch { /* 静默 */ }
}
</script>

<style lang="scss" scoped>
.dish-card {
  background: var(--bg-card);
  border-radius: var(--radius-lg);
  overflow: hidden;
  width: 100%;

  &.out-of-stock {
    opacity: 0.6;
  }

  &:active {
    transform: scale(0.98);
  }
}

.card-image-wrap {
  position: relative;
  width: 100%;
  height: 240rpx;
}

.card-image {
  width: 100%;
  height: 100%;
}

.stock-tag {
  position: absolute;
  top: 12rpx;
  left: 12rpx;
  background: rgba(0, 0, 0, 0.6);
  color: #fff;
  font-size: var(--font-xs);
  padding: 4rpx 12rpx;
  border-radius: var(--radius-sm);
}

.card-body {
  padding: var(--spacing-sm);
}

.card-title-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.card-name {
  font-size: var(--font-md);
  font-weight: 500;
  flex: 1;
  max-width: 200rpx;
}

.fav-btn {
  padding: 8rpx;
}

.fav-icon {
  font-size: 32rpx;
  transition: transform 0.3s;
  &.active { transform: scale(1.2); }
}

.card-time {
  font-size: var(--font-xs);
  color: var(--text-secondary);
  margin-top: 8rpx;
}

.card-actions {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  margin-top: 12rpx;
  gap: 12rpx;
}

.qty-btn {
  width: 48rpx;
  height: 48rpx;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 32rpx;

  &.plus {
    background: var(--color-primary);
    color: #fff;
    &.disabled { background: #ccc; }
  }
  &.minus {
    background: #f0f0f0;
    color: var(--text-primary);
  }
}

.qty-num {
  font-size: var(--font-md);
  font-weight: bold;
  min-width: 32rpx;
  text-align: center;
}
</style>
