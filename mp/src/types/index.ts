/** 公共类型定义 */

export interface UserInfo {
  id: number
  nickname: string
  avatarUrl: string
  role: 'admin' | 'user'
  openid?: string
}

export interface LoginResponse {
  token: string
  user: UserInfo
}

export interface Category {
  id: number
  name: string
  sortOrder: number
  status: string
}

export interface Dish {
  id: number
  name: string
  categoryId: number
  imageUrl: string
  description: string
  cookingTime: number | null
  status: string // normal | out_of_stock | unavailable
  isListed: boolean
}

export interface CartItem {
  dish: Dish
  quantity: number
}

export interface OrderItem {
  id: number
  dishId: number
  dishName: string
  dishImage: string
  quantity: number
  isExtra: boolean
}

export interface Order {
  id: number
  orderNo: string
  mealType: string
  status: string // pending | preparing | cooking | completed | cancelled
  flavorTags: string
  avoidNote: string
  specialNote: string
  items: OrderItem[]
  createTime: string
}

export interface PageResult<T> {
  records: T[]
  total: number
  pageNum: number
  pageSize: number
}
