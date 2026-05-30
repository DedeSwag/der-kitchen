<template>
  <view v-if="visible" class="network-bar" @tap="retry">
    <text class="bar-icon">⚠️</text>
    <text class="bar-text">网络连接异常，点击重试</text>
  </view>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'

const visible = ref(false)
let checkTimer: any = null

const emit = defineEmits<{
  (e: 'retry'): void
}>()

function checkNetwork() {
  uni.getNetworkType({
    success: (res) => {
      visible.value = res.networkType === 'none'
    }
  })
}

function retry() {
  checkNetwork()
  emit('retry')
}

onMounted(() => {
  checkNetwork()
  // 监听网络状态变化
  uni.onNetworkStatusChange((res) => {
    visible.value = !res.isConnected
  })
  checkTimer = setInterval(checkNetwork, 10000)
})

onUnmounted(() => {
  if (checkTimer) clearInterval(checkTimer)
})
</script>

<style lang="scss" scoped>
.network-bar {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  height: 72rpx;
  background: #FFF1F0;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12rpx;
  z-index: 9999;
  border-bottom: 1rpx solid #FFCCC7;
  animation: slideDown 0.3s ease-out;
}

.bar-icon { font-size: 28rpx; }
.bar-text { font-size: var(--font-sm); color: #CF1322; }

@keyframes slideDown {
  from { transform: translateY(-100%); }
  to { transform: translateY(0); }
}
</style>
