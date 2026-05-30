<template>
  <view class="order-list-page">
    <!-- Tab -->
    <view class="tabs">
      <view class="tab" :class="{ active: activeTab === 'ongoing' }" @tap="activeTab = 'ongoing'">
        <text>进行中</text>
      </view>
      <view class="tab" :class="{ active: activeTab === 'history' }" @tap="activeTab = 'history'">
        <text>历史订单</text>
      </view>
    </view>

    <!-- 列表 -->
    <scroll-view
      scroll-y
      class="order-scroll"
      refresher-enabled
      :refresher-triggered="refreshing"
      @refresherrefresh="onRefresh"
      @scrolltolower="loadMore"
    >
      <OrderCard v-for="order in displayList" :key="order.id" :order="order" />

      <!-- 加载更多 -->
      <view v-if="activeTab === 'history' && hasMore" class="load-more">
        <text>加载中...</text>
      </view>

      <!-- 空状态 -->
      <Empty
        v-if="!loading && displayList.length === 0"
        :icon="activeTab === 'ongoing' ? '📋' : '🍜'"
        :text="activeTab === 'ongoing' ? '暂无进行中的订单' : '还没有订单哦，去点菜吧~'"
        :action-text="activeTab === 'history' ? '去点菜' : ''"
        @action="goHome"
      />
    </scroll-view>
  </view>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { getOrders } from '@/api/order'
import type { Order } from '@/types'
import OrderCard from '@/components/OrderCard.vue'
import Empty from '@/components/Empty.vue'

const activeTab = ref<'ongoing' | 'history'>('ongoing')
const ongoingList = ref<Order[]>([])
const historyList = ref<Order[]>([])
const loading = ref(false)
const refreshing = ref(false)
const historyPage = ref(1)
const hasMore = ref(true)

const displayList = computed(() => activeTab.value === 'ongoing' ? ongoingList.value : historyList.value)

async function loadOngoing() {
  try {
    const res = await getOrders({ status: 'ongoing', pageNum: 1, pageSize: 50 })
    ongoingList.value = res.records
  } catch { /* 忽略 */ }
}

async function loadHistory(reset = false) {
  if (reset) {
    historyPage.value = 1
    hasMore.value = true
    historyList.value = []
  }
  if (!hasMore.value) return
  try {
    const res = await getOrders({ status: 'history', pageNum: historyPage.value, pageSize: 10 })
    historyList.value.push(...res.records)
    hasMore.value = historyList.value.length < res.total
    historyPage.value++
  } catch { /* 忽略 */ }
}

async function fetchAll() {
  loading.value = true
  await Promise.all([loadOngoing(), loadHistory(true)])
  loading.value = false
}

function onRefresh() {
  refreshing.value = true
  fetchAll().finally(() => { refreshing.value = false })
}

function loadMore() {
  if (activeTab.value === 'history') loadHistory()
}

function goHome() {
  uni.switchTab({ url: '/pages/home/index' })
}

onShow(() => { fetchAll() })
</script>

<style lang="scss" scoped>
.order-list-page {
  min-height: 100vh;
  background: var(--bg-page);
  display: flex;
  flex-direction: column;
}

.tabs {
  display: flex;
  background: var(--bg-card);
  border-bottom: 1rpx solid var(--border-color);
}
.tab {
  flex: 1;
  height: 88rpx;
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
      position: absolute;
      bottom: 0;
      width: 60rpx;
      height: 6rpx;
      background: var(--color-primary);
      border-radius: 3rpx;
    }
  }
}

.order-scroll {
  flex: 1;
  padding: var(--spacing-lg);
}

.load-more {
  text-align: center;
  padding: var(--spacing-lg);
  color: var(--text-secondary);
  font-size: var(--font-sm);
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

