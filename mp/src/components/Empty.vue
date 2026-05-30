<template>
  <view class="empty-state">
    <text class="empty-icon">{{ icon }}</text>
    <text class="empty-text">{{ text }}</text>
    <view v-if="actionText" class="empty-action" @tap="$emit('action')">
      <text>{{ actionText }}</text>
    </view>
  </view>
</template>

<script setup lang="ts">
defineProps<{
  icon?: string
  text?: string
  actionText?: string
}>()

defineEmits<{
  (e: 'action'): void
}>()
</script>

<style lang="scss" scoped>
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 120rpx var(--spacing-lg);
}

.empty-icon {
  font-size: 80rpx;
  margin-bottom: var(--spacing-md);
}

.empty-text {
  font-size: var(--font-md);
  color: var(--text-secondary);
  margin-bottom: var(--spacing-lg);
}

.empty-action {
  padding: 16rpx 40rpx;
  background: var(--color-primary);
  color: #fff;
  border-radius: 32rpx;
  font-size: var(--font-md);
  &:active { opacity: 0.85; }
}
</style>
