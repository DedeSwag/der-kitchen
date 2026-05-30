<template>
  <view class="mine-page">
    <view class="user-card">
      <image class="avatar" :src="userStore.userInfo?.avatarUrl || '/static/default-avatar.png'" mode="aspectFill" />
      <view class="info">
        <text class="nickname">{{ userStore.userInfo?.nickname || '未登录' }}</text>
        <text class="desc">{{ isAdmin ? '陈哥厨房 · 大厨 👨‍🍳' : '陈哥厨房 VIP 食客 👑' }}</text>
      </view>
    </view>

    <!-- 通用入口 -->
    <view class="menu-list">
      <view class="menu-item" @tap="navigateTo('/pages/mine/favorites')">
        <text>❤️ 我的收藏</text>
        <text class="arrow">→</text>
      </view>
      <view class="menu-item" @tap="goOrders">
        <text>📋 我的订单</text>
        <text class="arrow">→</text>
      </view>
    </view>

    <!-- 管理员专属 -->
    <view v-if="isAdmin" class="menu-list admin-section">
      <view class="section-label"><text>管理功能</text></view>
      <view class="menu-item" @tap="navigateTo('/pages/admin/listing')">
        <text>🍳 快速上下架</text>
        <text class="arrow">→</text>
      </view>
      <view class="menu-item" @tap="navigateTo('/pages/admin/workbench')">
        <text>📊 接单工作台</text>
        <text class="arrow">→</text>
      </view>
      <view class="menu-item" @tap="copyWebUrl">
        <text>💻 电脑端管理后台</text>
        <text class="arrow">复制链接</text>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()
const isAdmin = computed(() => userStore.userInfo?.role === 'admin')

function navigateTo(url: string) {
  uni.navigateTo({ url })
}

function goOrders() {
  uni.switchTab({ url: '/pages/order/list' })
}

function copyWebUrl() {
  const url = 'https://your-domain.com/admin'
  uni.setClipboardData({
    data: url,
    success: () => {
      uni.showToast({ title: '链接已复制', icon: 'success' })
    }
  })
}
</script>

<style lang="scss" scoped>
.mine-page {
  min-height: 100vh;
  background: var(--bg-page);
  padding-top: var(--spacing-lg);
}
.user-card {
  margin: 0 var(--spacing-lg);
  padding: var(--spacing-xl) var(--spacing-lg);
  background: var(--bg-card);
  border-radius: var(--radius-lg);
  display: flex;
  align-items: center;
  gap: var(--spacing-md);
  .avatar { width: 120rpx; height: 120rpx; border-radius: 50%; background: #eee; }
  .info { display: flex; flex-direction: column; gap: 8rpx; }
  .nickname { font-size: var(--font-lg); font-weight: bold; }
  .desc { font-size: var(--font-sm); color: var(--text-secondary); }
}
.menu-list {
  margin: var(--spacing-lg);
  background: var(--bg-card);
  border-radius: var(--radius-lg);
  overflow: hidden;
}
.menu-item {
  padding: var(--spacing-lg);
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-bottom: 1rpx solid var(--border-color);
  &:last-child { border-bottom: none; }
  &:active { background: #f5f5f5; }
  .arrow { color: var(--text-secondary); font-size: var(--font-sm); }
}
.admin-section {
  border: 1rpx solid #FFE58F;
}
.section-label {
  padding: var(--spacing-sm) var(--spacing-lg);
  background: #FFFBE6;
  font-size: var(--font-xs);
  color: #D48806;
  font-weight: bold;
}
</style>
