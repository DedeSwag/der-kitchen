import { onActivated, onDeactivated, onMounted, onUnmounted } from 'vue'
import { adminRealtime } from '@/services/adminRealtime'
import type { AdminRealtimeEvent } from '@/types'

export function useOrderRealtimeRefresh(
  refresh: () => void | Promise<void>,
  matches: (event: AdminRealtimeEvent) => boolean = () => true,
  delay = 350,
) {
  let timer: number | null = null
  let unsubscribeOrder: (() => void) | null = null
  let unsubscribeReset: (() => void) | null = null

  function schedule() {
    if (timer !== null) window.clearTimeout(timer)
    timer = window.setTimeout(() => {
      timer = null
      void refresh()
    }, delay)
  }

  function attach() {
    if (unsubscribeOrder) return
    unsubscribeOrder = adminRealtime.subscribeOrders(event => {
      if (matches(event)) schedule()
    })
    unsubscribeReset = adminRealtime.subscribeReset(schedule)
  }

  function detach() {
    unsubscribeOrder?.()
    unsubscribeReset?.()
    unsubscribeOrder = null
    unsubscribeReset = null
    if (timer !== null) {
      window.clearTimeout(timer)
      timer = null
    }
  }

  onMounted(attach)
  onActivated(attach)
  onDeactivated(detach)
  onUnmounted(detach)
}
