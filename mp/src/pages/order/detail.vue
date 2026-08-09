<template>
  <view class="detail-page" v-if="order">
    <!-- 状态进度条 -->
    <view class="progress-section">
      <view class="progress-bar">
        <view v-for="(step, idx) in steps" :key="step.key" class="step" :class="{ done: stepIndex >= idx, current: stepIndex === idx }">
          <view class="step-dot" />
          <text class="step-label">{{ step.label }}</text>
        </view>
      </view>
      <view class="current-status">
        <text>当前状态：{{ statusLabel }} {{ statusIcon }}</text>
      </view>
    </view>

    <!-- 用餐信息 -->
    <view class="section">
      <view class="info-row"><text>📅 {{ mealLabel }}</text></view>
      <view class="info-row"><text>🕐 下单时间 {{ order.createTime }}</text></view>
    </view>

    <!-- 菜品明细 -->
    <view class="section">
      <text class="section-title">菜品明细</text>
      <view v-for="item in normalItems" :key="item.id" class="item-row">
        <image class="item-img" src="/static/default-dish.png" mode="aspectFill" />
        <text class="item-name ellipsis">{{ item.dishName }}</text>
        <text class="item-qty">×{{ item.quantity }}</text>
      </view>
      <!-- 加菜 -->
      <view v-if="extraItems.length > 0" class="extra-divider">
        <text>── 加菜 ──</text>
      </view>
      <view v-for="item in extraItems" :key="item.id" class="item-row extra">
        <image class="item-img" src="/static/default-dish.png" mode="aspectFill" />
        <text class="item-name ellipsis">{{ item.dishName }}</text>
        <text class="item-qty">×{{ item.quantity }}</text>
      </view>
    </view>

    <!-- 备注信息 -->
    <view v-if="order.tasteTags.length || order.dietaryNotes || order.specialRequests" class="section">
      <text class="section-title">备注信息</text>
      <view v-if="order.tasteTags.length" class="note-row"><text>🏷️ 口味：{{ order.tasteTags.join('、') }}</text></view>
      <view v-if="order.dietaryNotes" class="note-row"><text>🚫 忌口：{{ order.dietaryNotes }}</text></view>
      <view v-if="order.specialRequests" class="note-row"><text>✨ 特殊：{{ order.specialRequests }}</text></view>
    </view>

    <!-- 操作按钮 -->
    <view class="action-section" v-if="actionBtns.length > 0">
      <view
        v-for="btn in actionBtns"
        :key="btn.label"
        :class="['action-btn', btn.type]"
        @tap="btn.handler"
      >
        <text>{{ btn.label }}</text>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { onHide, onLoad, onShow, onUnload } from '@dcloudio/uni-app'
import { getOrderDetail, cancelOrder } from '@/api/order'
import { useCartStore } from '@/stores/cart'
import type { Order } from '@/types'
import { formatMealLabel } from '@/utils/order'

const order = ref<Order | null>(null)
let orderId = 0
let pollTimer: any = null

const steps = [
  { key: 'pending', label: '待接单' },
  { key: 'preparing', label: '备菜中' },
  { key: 'cooking', label: '烹饪中' },
  { key: 'completed', label: '已完成' },
]

const statusMap: Record<string, { label: string; icon: string }> = {
  pending: { label: '待接单', icon: '⏳' },
  preparing: { label: '备菜中', icon: '🥬' },
  cooking: { label: '烹饪中', icon: '🔥' },
  completed: { label: '已完成', icon: '✅' },
  cancelled: { label: '已取消', icon: '✗' },
}

const stepIndex = computed(() => {
  if (!order.value) return -1
  if (order.value.status === 'cancelled') return -1
  return steps.findIndex(s => s.key === order.value!.status)
})

const statusLabel = computed(() => statusMap[order.value?.status || '']?.label || '')
const statusIcon = computed(() => statusMap[order.value?.status || '']?.icon || '')
const mealLabel = computed(() => order.value
  ? formatMealLabel(order.value.mealDate, order.value.mealType)
  : '')

const normalItems = computed(() => order.value?.items?.filter(i => !i.isExtra) || [])
const extraItems = computed(() => order.value?.items?.filter(i => i.isExtra) || [])

const actionBtns = computed(() => {
  if (!order.value) return []
  const btns: { label: string; type: string; handler: () => void }[] = []
  const s = order.value.status
  if (s === 'pending' || s === 'preparing') {
    btns.push({ label: '临时加菜', type: 'secondary', handler: handleAddExtra })
  }
  if (s === 'pending') {
    btns.push({ label: '撤销订单', type: 'danger', handler: handleCancel })
  }
  if (s === 'completed') {
    btns.push({ label: '再来一单', type: 'primary', handler: handleReorder })
  }
  return btns
})

onLoad((query) => {
  orderId = Number(query?.id)
})

onShow(() => {
  if (orderId) loadDetail()
  startPoll()
})
onHide(stopPoll)
onUnload(stopPoll)

async function loadDetail() {
  try {
    order.value = await getOrderDetail(orderId)
    // 终态停止轮询
    if (order.value.status === 'completed' || order.value.status === 'cancelled') {
      stopPoll()
    }
  } catch { /* 忽略 */ }
}

function startPoll() {
  stopPoll()
  pollTimer = setInterval(loadDetail, 5000)
}

function stopPoll() {
  if (pollTimer) { clearInterval(pollTimer); pollTimer = null }
}

function handleAddExtra() {
  const cartStore = useCartStore()
  cartStore.enterExtraMode(orderId)
  uni.switchTab({ url: '/pages/home/index' })
}

async function handleCancel() {
  uni.showModal({
    title: '确认撤销',
    content: '确定要撤销这个订单吗？',
    confirmText: '确定撤销',
    confirmColor: '#FF4D4F',
    success: async (res) => {
      if (res.confirm) {
        try {
          await cancelOrder(orderId)
          uni.showToast({ title: '订单已取消', icon: 'success' })
          loadDetail()
        } catch (e: any) {
          const msg = e?.message || '撤销失败'
          // 13.13 区分撤销失败原因
          if (msg.includes('已接单') || msg.includes('已开始')) {
            uni.showToast({ title: '订单已被接单，无法撤销', icon: 'none', duration: 2000 })
          } else {
            uni.showToast({ title: msg, icon: 'none' })
          }
          loadDetail()
        }
      }
    }
  })
}

function handleReorder() {
  if (!order.value) return
  const cartStore = useCartStore()
  const dishes = order.value.items.map(i => ({
    dish: { id: i.dishId, name: i.dishName, imageUrl: '', categoryId: 0, description: '', cookingTime: null, status: 'normal', isListed: true } as any,
    quantity: i.quantity,
  }))
  cartStore.batchAdd(dishes)
  uni.navigateTo({ url: '/pages/order/confirm' })
}
</script>

<style lang="scss" scoped>
.detail-page {
  min-height: 100vh;
  background: var(--bg-page);
  padding-bottom: 200rpx;
}

/* 进度条 */
.progress-section {
  background: var(--bg-card);
  padding: var(--spacing-xl) var(--spacing-lg);
  margin-bottom: var(--spacing-sm);
}

.progress-bar {
  display: flex;
  justify-content: space-between;
  position: relative;
  margin-bottom: var(--spacing-lg);

  &::before {
    content: '';
    position: absolute;
    top: 12rpx;
    left: 10%;
    right: 10%;
    height: 4rpx;
    background: var(--border-color);
  }
}

.step {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8rpx;
  position: relative;
  z-index: 1;
}

.step-dot {
  width: 24rpx;
  height: 24rpx;
  border-radius: 50%;
  background: var(--border-color);
  border: 4rpx solid #fff;
}

.step.done .step-dot { background: var(--color-primary); }
.step.current .step-dot {
  background: var(--color-primary);
  box-shadow: 0 0 0 6rpx rgba(255, 107, 53, 0.2);
  animation: glowPulse 1.5s ease-in-out infinite;
}

@keyframes glowPulse {
  0%, 100% { box-shadow: 0 0 0 6rpx rgba(255, 107, 53, 0.2); }
  50% { box-shadow: 0 0 0 12rpx rgba(255, 107, 53, 0.35); }
}

.step-label {
  font-size: var(--font-xs);
  color: var(--text-secondary);
}
.step.done .step-label,
.step.current .step-label {
  color: var(--color-primary);
  font-weight: bold;
}

.current-status {
  text-align: center;
  font-size: var(--font-lg);
  font-weight: bold;
  color: var(--color-primary);
}

/* 通用Section */
.section {
  background: var(--bg-card);
  padding: var(--spacing-lg);
  margin-bottom: var(--spacing-sm);
}
.section-title {
  font-size: var(--font-md);
  font-weight: bold;
  margin-bottom: var(--spacing-md);
  display: block;
}

.info-row {
  font-size: var(--font-md);
  color: var(--text-regular);
  margin-bottom: 8rpx;
}

/* 菜品行 */
.item-row {
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
  padding: var(--spacing-sm) 0;
  border-bottom: 1rpx solid var(--border-color);
  &:last-child { border-bottom: none; }
  &.extra { opacity: 0.85; }
}
.item-img { width: 72rpx; height: 72rpx; border-radius: var(--radius-sm); }
.item-name { flex: 1; font-size: var(--font-md); }
.item-qty { font-size: var(--font-md); color: var(--text-secondary); }

.extra-divider {
  text-align: center;
  padding: var(--spacing-md) 0;
  font-size: var(--font-xs);
  color: var(--text-secondary);
}

/* 备注 */
.note-row {
  font-size: var(--font-sm);
  color: var(--text-regular);
  margin-bottom: 12rpx;
  line-height: 1.5;
}

/* 操作区 */
.action-section {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  padding: var(--spacing-lg);
  padding-bottom: calc(var(--spacing-lg) + env(safe-area-inset-bottom));
  background: var(--bg-card);
  display: flex;
  gap: var(--spacing-md);
  box-shadow: 0 -2rpx 10rpx rgba(0,0,0,0.05);
}
.action-btn {
  flex: 1;
  height: 80rpx;
  border-radius: 40rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: var(--font-md);
  font-weight: bold;
  &.primary { background: var(--color-primary); color: #fff; }
  &.secondary { background: #f5f5f5; color: var(--text-primary); }
  &.danger { background: #FFF1F0; color: var(--color-danger); border: 1rpx solid var(--color-danger); }
  &:active { opacity: 0.85; }
}
</style>

