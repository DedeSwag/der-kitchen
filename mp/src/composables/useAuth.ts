import { useUserStore } from '@/stores/user'

/**
 * 登录相关组合式函数
 * 提供登录状态检查和强制登录能力
 */
export function useAuth() {
  const userStore = useUserStore()

  /** 确保已登录，未登录则尝试静默登录 */
  async function ensureLogin(): Promise<boolean> {
    if (userStore.isLoggedIn()) return true
    await userStore.silentLogin()
    return userStore.isLoggedIn()
  }

  /** 检查是否为管理员 */
  function checkAdmin(): boolean {
    return userStore.isAdmin()
  }

  /** 需要登录的操作守卫 */
  async function withAuth<T>(fn: () => Promise<T>): Promise<T | null> {
    const ok = await ensureLogin()
    if (!ok) {
      uni.showToast({ title: '请先登录', icon: 'none' })
      return null
    }
    return fn()
  }

  return { ensureLogin, checkAdmin, withAuth }
}
