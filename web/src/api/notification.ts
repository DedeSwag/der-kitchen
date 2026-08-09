import request from '@/utils/request'
import type { AdminNotification } from '@/types'

export function getAdminNotifications(params?: { afterId?: number; limit?: number }) {
  return request.get<any, AdminNotification[]>('/api/v1/admin/notifications', { params })
}

export function getUnreadNotificationCount() {
  return request.get<any, number>('/api/v1/admin/notifications/unread-count')
}

export function markNotificationRead(id: number) {
  return request.post(`/api/v1/admin/notifications/${id}/read`)
}

export function markAllNotificationsRead() {
  return request.post('/api/v1/admin/notifications/read-all')
}
