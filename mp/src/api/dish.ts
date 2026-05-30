import { http } from '@/utils/request'
import type { Dish, PageResult } from '@/types'

/** 获取菜品列表（用户端，仅上架） */
export function getDishes(params?: { categoryId?: number; pageNum?: number; pageSize?: number }) {
  return http.get<PageResult<Dish>>('/dishes', params)
}

/** 获取菜品详情 */
export function getDishDetail(id: number) {
  return http.get<Dish>(`/dishes/${id}`)
}

/* ===== 管理端 ===== */

/** 获取全部菜品（含下架） */
export function getAdminDishes(params?: { pageNum?: number; pageSize?: number }) {
  return http.get<PageResult<Dish>>('/admin/dishes', params)
}

/** 单个菜品上下架 */
export function updateDishListing(id: number, isListed: boolean) {
  return http.put(`/admin/dishes/${id}/listing`, { isListed })
}

/** 批量上下架 */
export function batchDishListing(isListed: boolean) {
  return http.put('/admin/dishes/batch-listing', { isListed })
}
