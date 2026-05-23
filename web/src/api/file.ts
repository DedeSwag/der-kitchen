import request from '@/utils/request'

export function uploadFile(file: File) {
  const formData = new FormData()
  formData.append('file', file)
  return request.post<any, { url: string }>('/api/v1/admin/files/upload', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })
}
