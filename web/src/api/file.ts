import request from '@/utils/request'
import type { FileUpload } from '@/types'

export function uploadFile(file: File) {
  const formData = new FormData()
  formData.append('files', file)
  return request.post<any, FileUpload[]>('/api/common/file/upload', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })
}
