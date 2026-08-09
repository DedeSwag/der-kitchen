import { defineStore } from 'pinia'
import { ref } from 'vue'
import { wxLogin, getMe } from '@/api/auth'
import type { UserInfo } from '@/types'

export const useUserStore = defineStore('user', () => {
  const token = ref(uni.getStorageSync('token') || '')
  const userInfo = ref<UserInfo | null>(JSON.parse(uni.getStorageSync('userInfo') || 'null'))

  const isLoggedIn = () => !!token.value
  const isAdmin = () => userInfo.value?.role === 'admin'

  /** 微信静默登录 */
  async function silentLogin() {
    // 已有 token，尝试获取用户信息
    if (token.value) {
      try {
        const info = await getMe()
        setUser(toUserInfo(info))
        switchTabBar()
        return
      } catch {
        // token 失效，清除后重新登录
        token.value = ''
        uni.removeStorageSync('token')
      }
    }

    // wx.login 获取 code
    try {
      const [err, res] = await uni.login({ provider: 'weixin' }) as any
      if (err || !res?.code) return

      const data = await wxLogin(res.code)
      token.value = data.token
      uni.setStorageSync('token', data.token)
      setUser(toUserInfo(data))
      switchTabBar()
    } catch (e) {
      console.warn('静默登录失败', e)
    }
  }

  function setUser(info: UserInfo) {
    userInfo.value = info
    uni.setStorageSync('userInfo', JSON.stringify(info))
  }

  function toUserInfo(data: { userId: number; nickname: string; avatarUrl?: string; role: 'admin' | 'user' }): UserInfo {
    return {
      id: data.userId,
      nickname: data.nickname,
      avatarUrl: data.avatarUrl || '',
      role: data.role,
    }
  }

  /** 根据角色切换 tabBar */
  function switchTabBar() {
    if (!userInfo.value) return

    if (userInfo.value.role === 'admin') {
      // 管理员：点菜/接单/我的
      uni.setTabBarItem({ index: 1, text: '接单', iconPath: '/static/tab/admin.png', selectedIconPath: '/static/tab/admin-active.png' })
    }
    // 普通用户保持默认 tabBar 即可
  }

  function logout() {
    token.value = ''
    userInfo.value = null
    uni.removeStorageSync('token')
    uni.removeStorageSync('userInfo')
    uni.reLaunch({ url: '/pages/home/index' })
  }

  return { token, userInfo, isLoggedIn, isAdmin, silentLogin, logout, switchTabBar }
})
