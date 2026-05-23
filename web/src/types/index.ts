export interface ApiResult<T = any> {
  code: number
  message: string
  data: T
}

export interface PageResult<T = any> {
  records: T[]
  total: number
  size: number
  current: number
}

export interface LoginResponse {
  token: string
  role: string
  nickname: string
  avatarUrl: string
}

export interface UserInfo {
  role: string
  nickname: string
  avatarUrl: string
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
  description: string
  imageUrl: string
  categoryId: number
  cookingTime: number
  status: string
  isListed: boolean
  createTime: string
}

export interface Order {
  id: number
  userId: number
  mealType: string
  mealDate: string
  status: string
  tasteTags: string
  dietaryNotes: string
  specialRequests: string
  createTime: string
}

export interface OrderItem {
  id: number
  orderId: number
  dishId: number
  dishName: string
  quantity: number
  isExtra: boolean
}

export interface OrderDetail extends Order {
  items: OrderItem[]
  userNickname: string
}

export interface StatsOverview {
  todayOrders: number
  pendingOrders: number
  totalDishes: number
  weekOrders: number
}
