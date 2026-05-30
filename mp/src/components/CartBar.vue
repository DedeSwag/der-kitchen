<template>
  <view v-if="cartStore.totalCount > 0" class="cart-bar" @tap="showDetail = true">
    <view class="cart-left">
      <view class="cart-icon-wrap">
        <text class="cart-icon">🛒</text>
        <view class="cart-badge">{{ cartStore.totalCount }}</view>
      </view>
      <text class="cart-text">已选 {{ cartStore.totalCount }} 道菜</text>
    </view>
    <view class="cart-btn" @tap.stop="goConfirm">
      <text>去下单 →</text>
    </view>
  </view>

  <!-- 已选菜品半屏弹窗 -->
  <view v-if="showDetail" class="cart-popup-mask" @tap="showDetail = false">
    <view class="cart-popup" @tap.stop>
      <view class="popup-header">
        <text class="popup-title">已选菜品</text>
        <text class="popup-clear" @tap="handleClear">清空</text>
      </view>
      <scroll-view scroll-y class="popup-list">
        <view v-for="item in cartStore.items" :key="item.dish.id" class="popup-item">
          <text class="item-name ellipsis">{{ item.dish.name }}</text>
          <view class="item-qty">
            <view class="qty-btn" @tap="cartStore.minusDish(item.dish.id)"><text>−</text></view>
            <text class="qty-num">{{ item.quantity }}</text>
            <view class="qty-btn" @tap="cartStore.addDish(item.dish)"><text>＋</text></view>
          </view>
        </view>
      </scroll-view>
      <view class="popup-footer">
        <text class="footer-summary">共 {{ cartStore.items.length }} 道菜，{{ cartStore.totalCount }} 份</text>
        <view class="cart-btn" @tap="goConfirm"><text>去下单 →</text></view>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useCartStore } from '@/stores/cart'

const cartStore = useCartStore()
const showDetail = ref(false)

function goConfirm() {
  showDetail.value = false
  if (cartStore.isExtraMode) {
    // 加菜模式：跳转加菜确认
    uni.navigateTo({ url: `/pages/order/confirm?extraOrderId=${cartStore.extraOrderId}` })
  } else {
    uni.navigateTo({ url: '/pages/order/confirm' })
  }
}

function handleClear() {
  cartStore.clear()
  showDetail.value = false
}
</script>

<style lang="scss" scoped>
.cart-bar {
  position: fixed;
  bottom: calc(env(safe-area-inset-bottom) + 120rpx);
  left: var(--spacing-lg);
  right: var(--spacing-lg);
  height: 96rpx;
  background: #333;
  border-radius: 48rpx;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 12rpx 0 24rpx;
  z-index: 999;
  box-shadow: 0 4rpx 20rpx rgba(0, 0, 0, 0.2);
  animation: slideUp 0.25s ease-out;
}

@keyframes slideUp {
  from { transform: translateY(100%); opacity: 0; }
  to { transform: translateY(0); opacity: 1; }
}

.cart-left {
  display: flex;
  align-items: center;
  gap: 16rpx;
}

.cart-icon-wrap {
  position: relative;
}
.cart-icon { font-size: 40rpx; }
.cart-badge {
  position: absolute;
  top: -8rpx;
  right: -12rpx;
  background: var(--color-primary);
  color: #fff;
  font-size: 20rpx;
  min-width: 28rpx;
  height: 28rpx;
  border-radius: 14rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0 6rpx;
}

.cart-text {
  color: #fff;
  font-size: var(--font-md);
}

.cart-btn {
  background: var(--color-primary);
  color: #fff;
  font-size: var(--font-md);
  font-weight: bold;
  padding: 16rpx 32rpx;
  border-radius: 36rpx;
}

/* 弹窗 */
.cart-popup-mask {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  z-index: 1000;
  display: flex;
  align-items: flex-end;
}

.cart-popup {
  width: 100%;
  max-height: 70vh;
  background: #fff;
  border-radius: 24rpx 24rpx 0 0;
  display: flex;
  flex-direction: column;
}

.popup-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: var(--spacing-lg);
  border-bottom: 1rpx solid var(--border-color);
}
.popup-title { font-size: var(--font-lg); font-weight: bold; }
.popup-clear { font-size: var(--font-sm); color: var(--text-secondary); }

.popup-list {
  flex: 1;
  max-height: 50vh;
  padding: 0 var(--spacing-lg);
}

.popup-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: var(--spacing-md) 0;
  border-bottom: 1rpx solid var(--border-color);
}
.item-name { flex: 1; font-size: var(--font-md); }
.item-qty {
  display: flex;
  align-items: center;
  gap: 16rpx;
  .qty-btn {
    width: 48rpx; height: 48rpx; border-radius: 50%;
    background: #f0f0f0; display: flex; align-items: center; justify-content: center;
  }
  .qty-num { font-size: var(--font-md); font-weight: bold; min-width: 32rpx; text-align: center; }
}

.popup-footer {
  padding: var(--spacing-lg);
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-top: 1rpx solid var(--border-color);
}
.footer-summary { font-size: var(--font-sm); color: var(--text-secondary); }
</style>
