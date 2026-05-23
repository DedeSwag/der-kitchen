import request from '@/utils/request'
import type { Order, OrderDetail, PageResult } from '@/types'

export function getOrders(params: {
  status?: string
  mealType?: string
  startDate?: string
  endDate?: string
  pageNum?: number
  pageSize?: number
}) {
  return request.get<any, PageResult<Order>>('/api/v1/admin/orders', { params })
}

export function getOrderDetail(id: number) {
  return request.get<any, OrderDetail>(`/api/v1/admin/orders/${id}`)
}

export function transitOrderStatus(id: number, status: string) {
  return request.put(`/api/v1/admin/orders/${id}/status`, { status })
}

export function getPendingCount() {
  return request.get<any, number>('/api/v1/admin/orders/pending-count')
}
