import request from '@/utils/request'
import type { StatsOverview } from '@/types'

export function getOverview() {
  return request.get<any, StatsOverview>('/api/v1/admin/stats/overview')
}

export function getTopDishes(params: { startDate: string; endDate: string; limit?: number }) {
  return request.get<any, { dishId: number; dishName: string; totalCount: number }[]>('/api/v1/admin/stats/top-dishes', { params })
}

export function getDailySummary(params: { startDate: string; endDate: string }) {
  return request.get<any, { mealDate: string; orderCount: number; dishCount: number }[]>('/api/v1/admin/stats/daily', { params })
}
