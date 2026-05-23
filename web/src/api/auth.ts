import request from '@/utils/request'
import type { LoginResponse } from '@/types'

export function adminLogin(data: { username: string; password: string }) {
  return request.post<any, LoginResponse>('/api/v1/auth/admin-login', data)
}

export function getMe() {
  return request.get<any, LoginResponse>('/api/v1/auth/me')
}

export function changePassword(data: { oldPassword: string; newPassword: string }) {
  return request.post('/api/v1/auth/change-password', data)
}
