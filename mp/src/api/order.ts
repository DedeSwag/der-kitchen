import { http } from '@/utils/request'
import type { Order, OrderItem, PageResult } from '@/types'

/** 创建订单 */
export function createOrder(data: {
  mealType: string
  items: { dishId: number; quantity: number }[]
  flavorTags?: string
  avoidNote?: string
  specialNote?: string
}) {
  return http.post<Order>('/orders', data)
}

/** 获取订单列表 */
export function getOrders(params?: { status?: string; pageNum?: number; pageSize?: number }) {
  return http.get<PageResult<Order>>('/orders', params)
}

/** 获取订单详情 */
export function getOrderDetail(id: number) {
  return http.get<Order>(`/orders/${id}`)
}

/** 加菜 */
export function addOrderItems(orderId: number, items: { dishId: number; quantity: number }[]) {
  return http.post<OrderItem[]>(`/orders/${orderId}/items`, { items })
}

/** 取消订单 */
export function cancelOrder(id: number) {
  return http.put(`/orders/${id}/cancel`)
}

/* ===== 管理端 ===== */

/** 管理端获取订单列表 */
export function getAdminOrders(params?: { status?: string; pageNum?: number; pageSize?: number }) {
  return http.get<PageResult<Order>>('/admin/orders', params)
}

/** 管理端推进订单状态 */
export function transitOrderStatus(id: number, action: string) {
  return http.put(`/admin/orders/${id}/transit`, { action })
}

/** 获取待处理订单数 */
export function getPendingCount() {
  return http.get<number>('/admin/orders/pending-count')
}
