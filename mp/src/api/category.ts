import { http } from '@/utils/request'
import type { Category } from '@/types'

/** 获取分类列表 */
export function getCategories() {
  return http.get<Category[]>('/categories')
}

/** 获取管理端全部分类（包含隐藏分类） */
export function getAdminCategories() {
  return http.get<Category[]>('/admin/categories')
}
