import { onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { getPendingCount } from '@/api/order'
import { getRealtimeBootstrap } from '@/api/realtime'
import { adminRealtime } from '@/services/adminRealtime'
import { useAppStore } from '@/stores/app'
import { useUserStore } from '@/stores/user'
import type { AdminRealtimeEvent } from '@/types'

const PENDING_REFRESH_DELAY = 300

export function useAdminRealtime() {
  const router = useRouter()
  const appStore = useAppStore()
  const userStore = useUserStore()
  let pendingRefreshTimer: number | null = null
  let unsubscribeOrders: (() => void) | null = null
  let unsubscribeReset: (() => void) | null = null

  function schedulePendingCountRefresh() {
    if (pendingRefreshTimer !== null) window.clearTimeout(pendingRefreshTimer)
    pendingRefreshTimer = window.setTimeout(async () => {
      pendingRefreshTimer = null
      try {
        appStore.setPendingCount(await getPendingCount())
      } catch {
        // SSE 重连或页面重新聚焦时会再次同步。
      }
    }, PENDING_REFRESH_DELAY)
  }

  function notify(event: AdminRealtimeEvent) {
    if (!event.notification) return
    if ('Notification' in window && Notification.permission === 'granted') {
      new Notification(`🍳 ${event.notification.title}`, {
        body: event.notification.content,
        icon: '/favicon.ico',
        tag: `order-event-${event.eventId}`,
      })
    }
    const audio = new Audio('/notification.mp3')
    audio.volume = 0.5
    void audio.play().catch(() => {
      // 浏览器可能在用户首次交互前禁止自动播放。
    })
  }

  function handleUnauthorized() {
    userStore.logout()
    void router.push('/login')
  }

  async function start() {
    if (!userStore.token) return
    try {
      const bootstrap = await getRealtimeBootstrap()
      appStore.setPendingCount(bootstrap.pendingCount)
      adminRealtime.start({
        token: userStore.token,
        cursor: bootstrap.cursor,
        onUnauthorized: handleUnauthorized,
      })
    } catch {
      // 普通接口错误已由统一请求层处理；重新进入后台时会再次初始化。
    }
  }

  function handleVisibilityChange() {
    if (document.visibilityState === 'visible') schedulePendingCountRefresh()
  }

  onMounted(() => {
    unsubscribeOrders = adminRealtime.subscribeOrders(event => {
      notify(event)
      schedulePendingCountRefresh()
    })
    unsubscribeReset = adminRealtime.subscribeReset(schedulePendingCountRefresh)
    document.addEventListener('visibilitychange', handleVisibilityChange)
    void start()
  })

  onUnmounted(() => {
    adminRealtime.stop()
    unsubscribeOrders?.()
    unsubscribeReset?.()
    document.removeEventListener('visibilitychange', handleVisibilityChange)
    if (pendingRefreshTimer !== null) window.clearTimeout(pendingRefreshTimer)
  })
}
