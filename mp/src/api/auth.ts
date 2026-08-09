import { http } from '@/utils/request'
import type { LoginResponse } from '@/types'

/** 微信登录 */
export function wxLogin(code: string) {
  return http.post<LoginResponse>('/auth/wx-login', { code })
}

/** 获取当前用户信息 */
export function getMe() {
  return http.get<LoginResponse>('/auth/me')
}

/** 修改密码（管理员） */
export function changePassword(data: { oldPassword: string; newPassword: string }) {
  return http.post('/auth/change-password', data)
}
