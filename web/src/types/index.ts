export interface ApiResult<T = any> {
  code: number
  message: string
  data: T
}

export interface PageResult<T = any> {
  records: T[]
  total: number
  pageNum: number
  pageSize: number
}

export interface LoginResponse {
  token?: string
  userId: number
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
  description?: string
  imageUrl?: string
  thumbnailUrl?: string
  imageFileId: number
  categoryId: number
  cookingTime: number | null
  status: string
  isListed: boolean
  createTime: string
}

export interface FileUpload {
  fileId: number
  originalName: string
  url: string
  thumbnailUrl?: string
}

export interface Order {
  id: number
  userId: number
  mealType: string
  mealDate: string
  status: string
  userNickname?: string
  tasteTags: string[]
  dietaryNotes?: string
  specialRequests?: string
  items: OrderItem[]
  createTime: string
  updateTime: string
}

export interface OrderItem {
  id: number
  dishId: number
  dishName: string
  quantity: number
  isExtra: boolean
}

export interface OrderDetail extends Order {}

export interface AdminNotification {
  id: number
  orderId: number
  type: 'new_order' | 'items_added' | 'order_cancelled'
  title: string
  content: string
  read: boolean
  createTime: string
}

export interface StatsOverview {
  todayOrders: number
  pendingOrders: number
  totalDishes: number
  weekOrders: number
}
