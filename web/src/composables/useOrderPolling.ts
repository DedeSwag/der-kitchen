import { ref, onMounted, onUnmounted } from 'vue'
import { getPendingCount } from '@/api/order'
import { useAppStore } from '@/stores/app'

export function useOrderPolling(interval = 5000) {
  const appStore = useAppStore()
  let timer: ReturnType<typeof setInterval> | null = null
  const lastCount = ref(0)

  async function poll() {
    try {
      const count = await getPendingCount()
      if (count > lastCount.value && lastCount.value > 0) {
        // 新订单来了
        notifyNewOrder(count)
      }
      lastCount.value = count
      appStore.setPendingCount(count)
    } catch {
      // ignore
    }
  }

  function notifyNewOrder(count: number) {
    // 浏览器通知
    if (Notification.permission === 'granted') {
      new Notification('🍳 新订单提醒', {
        body: `有 ${count} 个待处理订单`,
        icon: '/favicon.ico',
      })
    }
    // 提示音
    try {
      const audio = new Audio('/notification.mp3')
      audio.volume = 0.5
      audio.play()
    } catch {
      // ignore
    }
  }

  function start() {
    poll()
    timer = setInterval(poll, interval)
  }

  function stop() {
    if (timer) {
      clearInterval(timer)
      timer = null
    }
  }

  onMounted(() => {
    // 请求通知权限
    if ('Notification' in window && Notification.permission === 'default') {
      Notification.requestPermission()
    }
    start()
  })

  onUnmounted(() => stop())

  return { lastCount, poll, start, stop }
}
