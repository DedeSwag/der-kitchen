import { http } from '@/utils/request'
import type { AdminNotification } from '@/types'

export interface SubscriptionConfig {
  enabled: boolean
  templateId?: string
}

export function getSubscriptionConfig() {
  return http.get<SubscriptionConfig>('/admin/notifications/subscription-config')
}

export function getAdminNotifications(params?: { afterId?: number; limit?: number }) {
  return http.get<AdminNotification[]>('/admin/notifications', params)
}

export function getUnreadNotificationCount() {
  return http.get<number>('/admin/notifications/unread-count')
}

export function markNotificationRead(id: number) {
  return http.post<void>(`/admin/notifications/${id}/read`)
}

export function markAllNotificationsRead() {
  return http.post<void>('/admin/notifications/read-all')
}
