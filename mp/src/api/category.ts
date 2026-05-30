import { http } from '@/utils/request'
import type { Category } from '@/types'

/** 获取分类列表 */
export function getCategories() {
  return http.get<Category[]>('/categories')
}
