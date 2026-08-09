// 请求基础配置
const BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api/v1'

export class ApiError extends Error {
  readonly code: number
  readonly status: number

  constructor(message: string, code: number, status: number) {
    super(message)
    this.name = 'ApiError'
    this.code = code
    this.status = status
  }
}

interface RequestOptions {
  url: string
  method?: 'GET' | 'POST' | 'PUT' | 'DELETE'
  data?: any
  header?: Record<string, string>
  showLoading?: boolean
  showError?: boolean
}

interface ApiResult<T = any> {
  code: number
  message: string
  data: T
}

// 获取 token
function getToken(): string {
  return uni.getStorageSync('token') || ''
}

// 请求封装
export function request<T = any>(options: RequestOptions): Promise<T> {
  const {
    url,
    method = 'GET',
    data,
    header = {},
    showLoading = false,
    showError = true,
  } = options

  if (showLoading) {
    uni.showLoading({ title: '加载中...', mask: true })
  }

  const token = getToken()
  if (token) {
    header['Authorization'] = `Bearer ${token}`
  }

  return new Promise((resolve, reject) => {
    uni.request({
      url: `${BASE_URL}${url}`,
      method,
      data,
      header: {
        'Content-Type': 'application/json',
        ...header,
      },
      success: (res) => {
        if (showLoading) uni.hideLoading()

        const statusCode = res.statusCode

        if (statusCode === 401) {
          // Token 过期，清除后重新登录
          uni.removeStorageSync('token')
          uni.removeStorageSync('userInfo')
          // 尝试静默重新登录
          reLogin()
          reject(new ApiError('登录已过期', 401, 401))
          return
        }

        if (statusCode >= 200 && statusCode < 300) {
          const result = res.data as ApiResult<T>
          if (result.code === 200) {
            resolve(result.data)
          } else {
            if (showError) {
              uni.showToast({ title: result.message || '请求失败', icon: 'none' })
            }
            reject(new ApiError(result.message, result.code, statusCode))
          }
        } else {
          const result = res.data as Partial<ApiResult<T>>
          const message = result?.message || `请求失败(${statusCode})`
          if (showError) {
            uni.showToast({ title: message, icon: 'none' })
          }
          reject(new ApiError(message, result?.code || statusCode, statusCode))
        }
      },
      fail: (err) => {
        if (showLoading) uni.hideLoading()
        if (showError) {
          uni.showToast({ title: '网络异常，请检查连接', icon: 'none' })
        }
        reject(err)
      },
    })
  })
}

// 静默重新登录
async function reLogin() {
  try {
    const [err, res] = await uni.login({ provider: 'weixin' }) as any
    if (err || !res?.code) return

    const data = await request<{ token: string }>({
      url: '/auth/wx-login',
      method: 'POST',
      data: { code: res.code },
      showError: false,
    })
    if (data?.token) {
      uni.setStorageSync('token', data.token)
    }
  } catch {
    // 静默失败不处理
  }
}

// 便捷方法
export const http = {
  get: <T = any>(url: string, data?: any) => request<T>({ url, method: 'GET', data }),
  post: <T = any>(url: string, data?: any) => request<T>({ url, method: 'POST', data }),
  put: <T = any>(url: string, data?: any) => request<T>({ url, method: 'PUT', data }),
  delete: <T = any>(url: string, data?: any) => request<T>({ url, method: 'DELETE', data }),
}
