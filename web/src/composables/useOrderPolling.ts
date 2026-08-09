import { ref, onMounted, onUnmounted } from 'vue'
import { getPendingCount } from '@/api/order'
import { getAdminNotifications, markNotificationRead } from '@/api/notification'
import { useAppStore } from '@/stores/app'

export function useOrderPolling(interval = 3000) {
  const appStore = useAppStore()
  let timer: ReturnType<typeof setInterval> | null = null
  const lastNotificationId = ref<number | null>(null)

  async function pollPendingCount() {
    try {
      appStore.setPendingCount(await getPendingCount())
    } catch {
      // 网络恢复后下次轮询会自动补齐
    }
  }

  async function pollNotifications() {
    try {
      const notifications = await getAdminNotifications({
        // 首次不传 afterId；空列表后游标为 0，后续按 id > afterId 增量拉取
        afterId: lastNotificationId.value ?? undefined,
        limit: 20,
      })
      if (lastNotificationId.value === null) {
        lastNotificationId.value = notifications.reduce((max, item) => Math.max(max, item.id), 0)
        return
      }
      if (notifications.length === 0) return
      lastNotificationId.value = Math.max(lastNotificationId.value, ...notifications.map(item => item.id))
      const latest = notifications[notifications.length - 1]
      notify(latest.title, latest.content)
      await Promise.all(notifications.map(item => markNotificationRead(item.id)))
    } catch {
      // 网络恢复后使用 afterId 继续增量获取
    }
  }

  function notify(title: string, content: string) {
    if ('Notification' in window && Notification.permission === 'granted') {
      new Notification(`🍳 ${title}`, {
        body: content,
        icon: '/favicon.ico',
      })
    }
    try {
      const audio = new Audio('/notification.mp3')
      audio.volume = 0.5
      audio.play()
    } catch {
      // 浏览器可能因未发生用户交互而禁止自动播放
    }
  }

  async function poll() {
    await Promise.all([pollPendingCount(), pollNotifications()])
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
    if ('Notification' in window && Notification.permission === 'default') {
      Notification.requestPermission()
    }
    start()
  })

  onUnmounted(stop)

  return { lastNotificationId, poll, start, stop }
}
