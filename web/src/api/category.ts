import request from '@/utils/request'
import type { Category } from '@/types'

export function getCategories() {
  return request.get<any, Category[]>('/api/v1/admin/categories')
}

export function createCategory(data: { name: string; sortOrder?: number }) {
  return request.post<any, Category>('/api/v1/admin/categories', data)
}

export function updateCategory(id: number, data: { name?: string; sortOrder?: number; status?: string }) {
  return request.put<any, Category>(`/api/v1/admin/categories/${id}`, data)
}

export function deleteCategory(id: number) {
  return request.delete(`/api/v1/admin/categories/${id}`)
}

export function batchSortCategories(sortList: { id: number; sortOrder: number }[]) {
  return request.put('/api/v1/admin/categories/sort', sortList)
}
