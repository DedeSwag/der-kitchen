<template>
  <view v-if="show" class="confetti-overlay">
    <view v-for="i in 30" :key="i" class="confetti-piece" :style="getStyle(i)" />
  </view>
</template>

<script setup lang="ts">
import { ref } from 'vue'

const show = ref(false)

function getStyle(i: number) {
  const colors = ['#FF6B35', '#FFD700', '#FF4081', '#4CAF50', '#2196F3', '#9C27B0']
  const color = colors[i % colors.length]
  const left = Math.random() * 100
  const delay = Math.random() * 0.5
  const size = 8 + Math.random() * 8
  const rotation = Math.random() * 360
  return {
    background: color,
    left: `${left}%`,
    animationDelay: `${delay}s`,
    width: `${size}rpx`,
    height: `${size * 1.5}rpx`,
    transform: `rotate(${rotation}deg)`,
  }
}

function play() {
  show.value = true
  setTimeout(() => { show.value = false }, 2500)
}

defineExpose({ play })
</script>

<style lang="scss" scoped>
.confetti-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  z-index: 9999;
  pointer-events: none;
  overflow: hidden;
}

.confetti-piece {
  position: absolute;
  top: -20rpx;
  border-radius: 4rpx;
  animation: confettiFall 2.5s ease-out forwards;
}

@keyframes confettiFall {
  0% {
    transform: translateY(0) rotate(0deg);
    opacity: 1;
  }
  100% {
    transform: translateY(100vh) rotate(720deg);
    opacity: 0;
  }
}
</style>
