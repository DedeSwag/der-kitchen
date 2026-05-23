import { defineStore } from 'pinia'
import { ref } from 'vue'
import { adminLogin as apiLogin, getMe } from '@/api/auth'
import type { LoginResponse } from '@/types'

export const useUserStore = defineStore('user', () => {
  const token = ref(localStorage.getItem('token') || '')
  const nickname = ref('')
  const role = ref('')
  const avatarUrl = ref('')

  async function login(username: string, password: string) {
    const res: LoginResponse = await apiLogin({ username, password })
    token.value = res.token
    nickname.value = res.nickname
    role.value = res.role
    avatarUrl.value = res.avatarUrl
    localStorage.setItem('token', res.token)
  }

  async function fetchUserInfo() {
    const res: LoginResponse = await getMe()
    nickname.value = res.nickname
    role.value = res.role
    avatarUrl.value = res.avatarUrl
  }

  function logout() {
    token.value = ''
    nickname.value = ''
    role.value = ''
    avatarUrl.value = ''
    localStorage.removeItem('token')
  }

  return { token, nickname, role, avatarUrl, login, fetchUserInfo, logout }
})
