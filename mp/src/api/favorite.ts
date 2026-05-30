import { http } from '@/utils/request'
import type { Dish } from '@/types'

/** 获取收藏列表 */
export function getFavorites() {
  return http.get<Dish[]>('/favorites')
}

/** 添加收藏 */
export function addFavorite(dishId: number) {
  return http.post('/favorites', { dishId })
}

/** 取消收藏 */
export function removeFavorite(dishId: number) {
  return http.delete(`/favorites/${dishId}`)
}
