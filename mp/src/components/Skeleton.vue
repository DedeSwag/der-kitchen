<template>
  <view class="skeleton-wrap">
    <!-- 模拟首页头部 -->
    <view class="sk-header">
      <view class="sk-line sk-title" />
      <view class="sk-line sk-date" />
    </view>
    <!-- 模拟分类Tab -->
    <view class="sk-tabs">
      <view v-for="i in 5" :key="i" class="sk-tab" />
    </view>
    <!-- 模拟菜品卡片网格 -->
    <view class="sk-grid">
      <view v-for="i in 6" :key="i" class="sk-card">
        <view class="sk-img" />
        <view class="sk-line sk-name" />
        <view class="sk-line sk-sub" />
      </view>
    </view>
  </view>
</template>

<style lang="scss" scoped>
.skeleton-wrap {
  padding: var(--spacing-lg);
}

.sk-header {
  margin-bottom: var(--spacing-lg);
}

.sk-line {
  background: linear-gradient(90deg, #f0f0f0 25%, #e0e0e0 50%, #f0f0f0 75%);
  background-size: 200% 100%;
  animation: shimmer 1.5s infinite;
  border-radius: 8rpx;
}

.sk-title { width: 200rpx; height: 40rpx; margin-bottom: 12rpx; }
.sk-date { width: 300rpx; height: 28rpx; }

.sk-tabs {
  display: flex;
  gap: var(--spacing-sm);
  margin-bottom: var(--spacing-lg);
}
.sk-tab {
  width: 120rpx;
  height: 56rpx;
  border-radius: 28rpx;
  background: linear-gradient(90deg, #f0f0f0 25%, #e0e0e0 50%, #f0f0f0 75%);
  background-size: 200% 100%;
  animation: shimmer 1.5s infinite;
}

.sk-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: var(--spacing-md);
}

.sk-card {
  background: var(--bg-card);
  border-radius: var(--radius-lg);
  overflow: hidden;
  padding-bottom: var(--spacing-sm);
}

.sk-img {
  width: 100%;
  height: 240rpx;
  background: linear-gradient(90deg, #f0f0f0 25%, #e0e0e0 50%, #f0f0f0 75%);
  background-size: 200% 100%;
  animation: shimmer 1.5s infinite;
}

.sk-name {
  width: 70%;
  height: 28rpx;
  margin: var(--spacing-sm) var(--spacing-sm) 8rpx;
}

.sk-sub {
  width: 40%;
  height: 24rpx;
  margin: 0 var(--spacing-sm);
}

@keyframes shimmer {
  0% { background-position: 200% 0; }
  100% { background-position: -200% 0; }
}
</style>
