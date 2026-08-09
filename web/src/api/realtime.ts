import request from '@/utils/request'
import type { RealtimeBootstrap } from '@/types'

export function getRealtimeBootstrap() {
  return request.get<any, RealtimeBootstrap>('/api/v1/admin/realtime/bootstrap')
}
