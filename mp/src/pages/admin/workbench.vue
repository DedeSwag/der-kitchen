<template>
  <view class="workbench-page">
    <!-- 新订单横幅提醒 -->
    <view v-if="showBanner" class="new-order-banner" @tap="scrollToTop">
      <text>🔔 {{ bannerText }}</text>
      <text class="banner-close" @tap.stop="showBanner = false">✕</text>
    </view>

    <!-- 顶部统计 -->
    <view class="stats-row">
      <view class="stat-card pending">
        <text class="num">{{ pendingCount }}</text>
        <text class="label">待接单</text>
      </view>
      <view class="stat-card ongoing">
        <text class="num">{{ ongoingCount }}</text>
        <text class="label">进行中</text>
      </view>
    </view>

    <view v-if="subscriptionConfig?.enabled" class="subscribe-row">
      <text>微信离线提醒需要主动授权，每次授权可接收一条新订单通知。</text>
      <button class="subscribe-btn" size="mini" @tap="requestWechatSubscription">开启一次提醒</button>
    </view>

    <!-- Tab -->
    <view class="tabs">
      <view class="tab" :class="{ active: activeTab === 'pending' }" @tap="activeTab = 'pending'">
        <text>待处理</text>
      </view>
      <view class="tab" :class="{ active: activeTab === 'all' }" @tap="activeTab = 'all'">
        <text>今日全部</text>
      </view>
    </view>

    <!-- 订单列表 -->
    <scroll-view
      scroll-y
      class="order-scroll"
      refresher-enabled
      :refresher-triggered="refreshing"
      @refresherrefresh="onRefresh"
    >
      <view v-for="order in displayList" :key="order.id" class="admin-order-card">
        <!-- 头部 -->
        <view class="aoc-header">
          <text class="aoc-no">#{{ order.id }}</text>
          <text class="aoc-meal">{{ formatMealLabel(order.mealDate, order.mealType) }}</text>
          <view class="aoc-status" :class="order.status">
            <text>{{ statusMap[order.status]?.label }}</text>
          </view>
        </view>

        <!-- 菜品明细 -->
        <view class="aoc-dishes">
          <text v-for="item in order.items" :key="item.id" class="aoc-dish-item">
            {{ item.dishName }} ×{{ item.quantity }}
          </text>
        </view>

        <!-- 备注 -->
        <view v-if="order.tasteTags.length || order.dietaryNotes || order.specialRequests" class="aoc-notes">
          <text v-if="order.tasteTags.length" class="note-line">🏷️ {{ order.tasteTags.join('、') }}</text>
          <text v-if="order.dietaryNotes" class="note-line">🚫 {{ order.dietaryNotes }}</text>
          <text v-if="order.specialRequests" class="note-line">✨ {{ order.specialRequests }}</text>
        </view>

        <!-- 操作按钮 -->
        <view class="aoc-actions">
          <view v-if="order.status === 'pending'" class="action-btn primary" @tap="handleTransit(order, 'preparing')">
            <text>接单开做 👨‍🍳</text>
          </view>
          <view v-if="order.status === 'preparing'" class="action-btn cooking" @tap="handleTransit(order, 'cooking')">
            <text>开始烹饪 🔥</text>
          </view>
          <view v-if="order.status === 'cooking'" class="action-btn success" @tap="handleTransit(order, 'completed')">
            <text>完成上菜 ✅</text>
          </view>
          <view v-if="canCancel(order.status)" class="action-btn cancel" @tap="handleCancel(order)">
            <text>取消</text>
          </view>
        </view>
      </view>

      <!-- 空状态 -->
      <view v-if="displayList.length === 0" class="empty-state">
        <text class="empty-icon">{{ activeTab === 'pending' ? '🎉' : '📋' }}</text>
        <text class="empty-text">{{ activeTab === 'pending' ? '没有待处理订单' : '今日暂无订单' }}</text>
      </view>
    </scroll-view>
  </view>
</template>

<script setup lang="ts">
import { ref, computed, onUnmounted } from 'vue'
import { onShow, onHide } from '@dcloudio/uni-app'
import { getAdminOrders, transitOrderStatus, getPendingCount } from '@/api/order'
import {
  getAdminNotifications,
  getSubscriptionConfig,
  markNotificationRead,
  type SubscriptionConfig,
} from '@/api/notification'
import type { Order } from '@/types'
import { formatMealLabel, today } from '@/utils/order'

const activeTab = ref<'pending' | 'all'>('pending')
const pendingCount = ref(0)
const ongoingCount = ref(0)
const orderList = ref<Order[]>([])
const refreshing = ref(false)
const showBanner = ref(false)
const bannerText = ref('新订单来啦！')
const subscriptionConfig = ref<SubscriptionConfig | null>(null)
let pollTimer: any = null
let lastNotificationId: number | null = null

const statusMap: Record<string, { label: string }> = {
  pending: { label: '待接单' },
  preparing: { label: '备菜中' },
  cooking: { label: '烹饪中' },
  completed: { label: '已完成' },
  cancelled: { label: '已取消' },
}

const displayList = computed(() => {
  if (activeTab.value === 'pending') {
    return orderList.value.filter(o => ['pending', 'preparing', 'cooking'].includes(o.status))
  }
  const currentDate = today()
  return orderList.value.filter(o => o.mealDate === currentDate)
})

function canCancel(status: string) {
  return ['pending', 'preparing', 'cooking'].includes(status)
}

async function loadOrders() {
  try {
    const res = await getAdminOrders({ pageNum: 1, pageSize: 50 })
    orderList.value = res.records
    pendingCount.value = orderList.value.filter(o => o.status === 'pending').length
    ongoingCount.value = orderList.value.filter(o => ['preparing', 'cooking'].includes(o.status)).length
  } catch { /* 忽略 */ }
}

async function pollPendingCount() {
  try {
    const count = await getPendingCount()
    pendingCount.value = count
  } catch { /* 忽略 */ }
}

async function pollNotifications() {
  try {
    const notifications = await getAdminNotifications({
      // 首次不传 afterId；空列表后游标为 0，后续按 id > afterId 增量拉取
      afterId: lastNotificationId ?? undefined,
      limit: 20,
    })
    if (lastNotificationId === null) {
      lastNotificationId = notifications.reduce((max, item) => Math.max(max, item.id), 0)
      return
    }
    if (notifications.length === 0) return
    lastNotificationId = Math.max(lastNotificationId, ...notifications.map(item => item.id))
    const latest = notifications[notifications.length - 1]
    bannerText.value = `${latest.title} · ${latest.content}`
    showBanner.value = true
    uni.vibrateShort({})
    setTimeout(() => { showBanner.value = false }, 3000)
    await Promise.all(notifications.map(item => markNotificationRead(item.id)))
    loadOrders()
  } catch { /* 忽略 */ }
}

async function loadSubscriptionConfig() {
  try {
    subscriptionConfig.value = await getSubscriptionConfig()
  } catch { /* 站内通知仍可正常工作 */ }
}

function requestWechatSubscription() {
  const templateId = subscriptionConfig.value?.templateId
  if (!templateId) return
  // #ifdef MP-WEIXIN
  uni.requestSubscribeMessage({
    tmplIds: [templateId],
    success: (result) => {
      const accepted = (result as unknown as Record<string, unknown>)[templateId] === 'accept'
      uni.showToast({ title: accepted ? '已开启一次提醒' : '未授权提醒', icon: 'none' })
    },
    fail: () => uni.showToast({ title: '订阅失败，请稍后重试', icon: 'none' }),
  })
  // #endif
  // #ifndef MP-WEIXIN
  uni.showToast({ title: '请在微信小程序中开启', icon: 'none' })
  // #endif
}

async function handleTransit(order: Order, action: string) {
  try {
    await transitOrderStatus(order.id, action)
    uni.showToast({ title: '操作成功', icon: 'success' })
    loadOrders()
  } catch (e: any) {
    uni.showToast({ title: e?.message || '操作失败', icon: 'none' })
  }
}

async function handleCancel(order: Order) {
  uni.showModal({
    title: '取消订单',
    content: `确定取消订单 #${order.id} 吗？`,
    confirmColor: '#FF4D4F',
    success: async (res) => {
      if (res.confirm) {
        try {
          await transitOrderStatus(order.id, 'cancelled')
          uni.showToast({ title: '已取消', icon: 'success' })
          loadOrders()
        } catch (e: any) {
          uni.showToast({ title: e?.message || '取消失败', icon: 'none' })
        }
      }
    }
  })
}

function onRefresh() {
  refreshing.value = true
  loadOrders().finally(() => { refreshing.value = false })
}

function scrollToTop() {
  showBanner.value = false
}

function startPoll() {
  stopPoll()
  pollTimer = setInterval(() => {
    pollPendingCount()
    pollNotifications()
  }, 3000)
}

function stopPoll() {
  if (pollTimer) { clearInterval(pollTimer); pollTimer = null }
}

onShow(() => {
  loadOrders()
  loadSubscriptionConfig()
  pollPendingCount()
  pollNotifications().finally(startPoll)
})

onHide(() => { stopPoll() })
onUnmounted(() => { stopPoll() })
</script>

<style lang="scss" scoped>
.workbench-page {
  min-height: 100vh;
  background: var(--bg-page);
  display: flex;
  flex-direction: column;
}

/* 横幅 */
.new-order-banner {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: var(--spacing-sm) var(--spacing-lg);
  background: var(--color-primary);
  color: #fff;
  font-size: var(--font-md);
  font-weight: bold;
  animation: bannerSlide 0.3s ease-out;
}
@keyframes bannerSlide {
  from { transform: translateY(-100%); }
  to { transform: translateY(0); }
}
.banner-close { padding: 8rpx; font-size: var(--font-sm); }

/* 统计 */
.stats-row {
  display: flex;
  gap: var(--spacing-md);
  padding: var(--spacing-lg);
}
.subscribe-row {
  margin: 0 var(--spacing-lg) var(--spacing-md);
  padding: var(--spacing-sm) var(--spacing-md);
  border-radius: var(--radius-md);
  background: #fff7e6;
  color: #8c6d1f;
  font-size: var(--font-xs);
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
  text { flex: 1; }
  .subscribe-btn {
    margin: 0;
    color: #fff;
    background: var(--color-primary);
  }
}
.stat-card {
  flex: 1;
  background: var(--bg-card);
  border-radius: var(--radius-lg);
  padding: var(--spacing-lg);
  display: flex;
  flex-direction: column;
  align-items: center;
  .num { font-size: 56rpx; font-weight: bold; }
  .label { font-size: var(--font-sm); color: var(--text-secondary); margin-top: 8rpx; }
  &.pending .num { color: #FAAD14; }
  &.ongoing .num { color: #1890FF; }
}

/* Tab */
.tabs {
  display: flex;
  background: var(--bg-card);
  border-bottom: 1rpx solid var(--border-color);
}
.tab {
  flex: 1;
  height: 80rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: var(--font-md);
  color: var(--text-regular);
  position: relative;
  &.active {
    color: var(--color-primary);
    font-weight: bold;
    &::after {
      content: '';
      position: absolute; bottom: 0;
      width: 60rpx; height: 6rpx;
      background: var(--color-primary); border-radius: 3rpx;
    }
  }
}

/* 订单滚动 */
.order-scroll {
  flex: 1;
  padding: var(--spacing-lg);
}

/* 管理端订单卡片 */
.admin-order-card {
  background: var(--bg-card);
  border-radius: var(--radius-lg);
  padding: var(--spacing-lg);
  margin-bottom: var(--spacing-md);
}

.aoc-header {
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
  margin-bottom: var(--spacing-sm);
}
.aoc-no { font-size: var(--font-sm); color: var(--text-secondary); }
.aoc-meal { font-size: var(--font-md); font-weight: bold; flex: 1; }
.aoc-status {
  font-size: var(--font-xs);
  padding: 6rpx 16rpx;
  border-radius: 20rpx;
  &.pending { background: #FFF7E6; color: #FAAD14; }
  &.preparing { background: #E6F7FF; color: #1890FF; }
  &.cooking { background: #FFF1F0; color: #FF4D4F; }
  &.completed { background: #F6FFED; color: #52C41A; }
  &.cancelled { background: #F5F5F5; color: #999; }
}

.aoc-dishes {
  display: flex;
  flex-wrap: wrap;
  gap: 8rpx;
  margin-bottom: var(--spacing-sm);
}
.aoc-dish-item {
  font-size: var(--font-sm);
  background: #f5f5f5;
  padding: 6rpx 16rpx;
  border-radius: 16rpx;
  color: var(--text-regular);
}

.aoc-notes {
  padding: var(--spacing-sm);
  background: #FFFBE6;
  border-radius: var(--radius-md);
  margin-bottom: var(--spacing-sm);
}
.note-line {
  display: block;
  font-size: var(--font-sm);
  color: #8C6D1F;
  line-height: 1.6;
}

.aoc-actions {
  display: flex;
  gap: var(--spacing-sm);
  justify-content: flex-end;
  margin-top: var(--spacing-sm);
}
.action-btn {
  padding: 14rpx 28rpx;
  border-radius: 28rpx;
  font-size: var(--font-sm);
  font-weight: bold;
  &.primary { background: var(--color-primary); color: #fff; }
  &.cooking { background: #1890FF; color: #fff; }
  &.success { background: #52C41A; color: #fff; }
  &.cancel { background: #f5f5f5; color: var(--text-secondary); }
  &:active { opacity: 0.8; }
}

/* 空状态 */
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding-top: 150rpx;
  .empty-icon { font-size: 80rpx; margin-bottom: var(--spacing-md); }
  .empty-text { font-size: var(--font-md); color: var(--text-secondary); }
}
</style>
