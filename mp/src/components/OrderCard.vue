<template>
  <view class="order-card" @tap="goDetail">
    <view class="card-header">
      <text class="meal-type">{{ mealLabel }}</text>
      <view class="status-tag" :class="order.status">
        <text>{{ statusIcon }} {{ statusLabel }}</text>
      </view>
    </view>
    <view class="card-body">
      <text class="dishes-summary ellipsis-2">{{ dishesSummary }}</text>
      <text class="order-time">{{ order.createTime }}</text>
    </view>
    <view class="card-footer">
      <view class="btn-secondary" @tap.stop="goDetail"><text>查看详情</text></view>
      <view v-if="canAddExtra" class="btn-secondary" @tap.stop="handleAddExtra"><text>临时加菜</text></view>
      <view v-if="order.status === 'completed'" class="btn-primary" @tap.stop="handleReorder"><text>再来一单</text></view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useCartStore } from '@/stores/cart'
import type { Order } from '@/types'

const props = defineProps<{ order: Order }>()

const statusMap: Record<string, { label: string; icon: string }> = {
  pending: { label: '待接单', icon: '⏳' },
  preparing: { label: '备菜中', icon: '🥬' },
  cooking: { label: '烹饪中', icon: '🔥' },
  completed: { label: '已完成', icon: '✓' },
  cancelled: { label: '已取消', icon: '✗' },
}

const mealMap: Record<string, string> = {
  today_lunch: '今日午餐',
  today_dinner: '今日晚餐',
  tomorrow_lunch: '明日午餐',
  tomorrow_dinner: '明日晚餐',
}

const statusLabel = computed(() => statusMap[props.order.status]?.label || props.order.status)
const statusIcon = computed(() => statusMap[props.order.status]?.icon || '')
const mealLabel = computed(() => mealMap[props.order.mealType] || props.order.mealType)

const dishesSummary = computed(() => {
  if (!props.order.items?.length) return '暂无菜品信息'
  const first2 = props.order.items.slice(0, 2).map(i => `${i.dishName}×${i.quantity}`)
  const rest = props.order.items.length > 2 ? `等${props.order.items.length}道` : ''
  return first2.join('、') + rest
})

const canAddExtra = computed(() => ['pending', 'preparing'].includes(props.order.status))

function goDetail() {
  uni.navigateTo({ url: `/pages/order/detail?id=${props.order.id}` })
}

function handleAddExtra() {
  const cartStore = useCartStore()
  cartStore.enterExtraMode(props.order.id)
  uni.switchTab({ url: '/pages/home/index' })
}

function handleReorder() {
  const cartStore = useCartStore()
  const dishes = props.order.items.map(i => ({
    dish: { id: i.dishId, name: i.dishName, imageUrl: i.dishImage, categoryId: 0, description: '', cookingTime: null, status: 'normal', isListed: true } as any,
    quantity: i.quantity,
  }))
  cartStore.batchAdd(dishes)
  uni.navigateTo({ url: '/pages/order/confirm' })
}
</script>

<style lang="scss" scoped>
.order-card {
  background: var(--bg-card);
  border-radius: var(--radius-lg);
  padding: var(--spacing-lg);
  margin-bottom: var(--spacing-md);
  &:active { opacity: 0.9; }
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: var(--spacing-sm);
}
.meal-type { font-size: var(--font-md); font-weight: bold; }

.status-tag {
  font-size: var(--font-xs);
  padding: 6rpx 16rpx;
  border-radius: 20rpx;
  &.pending { background: #FFF7E6; color: #FAAD14; }
  &.preparing { background: #E6F7FF; color: #1890FF; }
  &.cooking { background: #FFF1F0; color: #FF4D4F; }
  &.completed { background: #F6FFED; color: #52C41A; }
  &.cancelled { background: #F5F5F5; color: #999; }
}

.card-body {
  margin-bottom: var(--spacing-md);
}
.dishes-summary { font-size: var(--font-sm); color: var(--text-regular); display: block; }
.order-time { font-size: var(--font-xs); color: var(--text-secondary); margin-top: 8rpx; display: block; }

.card-footer {
  display: flex;
  gap: var(--spacing-sm);
  justify-content: flex-end;
}

.btn-secondary {
  padding: 12rpx 24rpx;
  border-radius: 24rpx;
  font-size: var(--font-sm);
  border: 1rpx solid var(--border-color);
  color: var(--text-regular);
}
.btn-primary {
  padding: 12rpx 24rpx;
  border-radius: 24rpx;
  font-size: var(--font-sm);
  background: var(--color-primary);
  color: #fff;
}
</style>
