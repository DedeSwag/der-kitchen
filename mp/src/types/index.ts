/** 公共类型定义 */

export interface UserInfo {
  id: number
  nickname: string
  avatarUrl: string
  role: 'admin' | 'user'
  openid?: string
}

export interface LoginResponse {
  token?: string
  userId: number
  nickname: string
  avatarUrl?: string
  role: 'admin' | 'user'
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
  imageUrl?: string
  thumbnailUrl?: string
  imageFileId: number
  description?: string
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
  quantity: number
  isExtra: boolean
}

export interface Order {
  id: number
  userId: number
  userNickname?: string
  mealType: string
  mealDate: string
  status: string // pending | preparing | cooking | completed | cancelled
  tasteTags: string[]
  dietaryNotes?: string
  specialRequests?: string
  items: OrderItem[]
  createTime: string
  updateTime: string
}

export interface PageResult<T> {
  records: T[]
  total: number
  pageNum: number
  pageSize: number
}

export interface AdminNotification {
  id: number
  orderId: number
  type: 'new_order' | 'items_added' | 'order_cancelled'
  title: string
  content: string
  read: boolean
  createTime: string
}
