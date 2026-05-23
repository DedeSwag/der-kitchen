import request from '@/utils/request'
import type { Dish, PageResult } from '@/types'

export function getDishes(params: {
  categoryId?: number
  status?: string
  isListed?: boolean
  pageNum?: number
  pageSize?: number
}) {
  return request.get<any, PageResult<Dish>>('/api/v1/admin/dishes', { params })
}

export function getDishDetail(id: number) {
  return request.get<any, Dish>(`/api/v1/dishes/${id}`)
}

export function createDish(data: Partial<Dish>) {
  return request.post<any, Dish>('/api/v1/admin/dishes', data)
}

export function updateDish(id: number, data: Partial<Dish>) {
  return request.put<any, Dish>(`/api/v1/admin/dishes/${id}`, data)
}

export function deleteDish(id: number) {
  return request.delete(`/api/v1/admin/dishes/${id}`)
}

export function updateDishStatus(id: number, status: string) {
  return request.put(`/api/v1/admin/dishes/${id}/status`, { status })
}

export function updateDishListing(id: number, isListed: boolean) {
  return request.put(`/api/v1/admin/dishes/${id}/listing`, { isListed })
}

export function batchListing(ids: number[], isListed: boolean) {
  return request.put('/api/v1/admin/dishes/batch-listing', { ids, isListed })
}
